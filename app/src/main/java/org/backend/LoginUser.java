package org.backend;

import org.vault.*;

import java.io.File;
import java.nio.file.Path;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class LoginUser {
  private final DBOperations localDbOps;
  private final DBOperations cloudDbOps;
  private String username;
  private String email;
  private final String plaintextPassword;
  private String hashedPassword;
  private UserInfo fetchedUser;
  private UserInfo cloudFetchedUser;

  public LoginUser(DatabaseConnection localDb, DatabaseConnection cloudDb, String accountIdentifier, String pwd) {
    this.localDbOps = new DBOperations(localDb);
    this.cloudDbOps = new DBOperations(cloudDb);

    if (isValidUsername(accountIdentifier)) {
      this.username = accountIdentifier;
    } else {
      this.email = accountIdentifier;
    }

    this.plaintextPassword = pwd;
  }

  public BackendError login() {
    try {
      if (this.username != null) {
        if (this.username.isEmpty()) {
          return new BackendError(BackendError.ErrorTypes.InvalidLoginCredentials,
              "[Login.login] Provided username/email can't be empty");
        }

        this.fetchedUser = this.localDbOps.getUserInfo(this.username);
        this.cloudFetchedUser = this.cloudDbOps.getUserInfo(this.username);
      } else {
        if (this.email.isEmpty()) {
          return new BackendError(BackendError.ErrorTypes.InvalidLoginCredentials,
              "[Login.login] Provided username/email can't be empty");
        }

        this.fetchedUser = this.localDbOps.getUserInfoByEmail(this.email);
        this.cloudFetchedUser = this.cloudDbOps.getUserInfoByEmail(this.email);
      }
    } catch (Exception e) {
      return new BackendError(BackendError.ErrorTypes.DbTransactionError,
          "[LoginUser.login] Failed to get master user info from database. Given exception: " + e);
    }

    // registered in no DB
    if ((this.cloudFetchedUser.lastLoggedInTime == -1) && (this.fetchedUser.lastLoggedInTime == -1)) {
      return new BackendError(BackendError.ErrorTypes.UserDoesNotExist,
          "[LoginUser.login] Failed to login. No user registered using that username/email.");
    }

    BackendError resp;
    SupabaseUtils supaUtils = new SupabaseUtils();

    // registered in the cloud, but not on local -> Sync with cloud DB
    if ((this.cloudFetchedUser.lastLoggedInTime != -1) && (this.fetchedUser.lastLoggedInTime == -1)) {
      resp = syncWithCloudDb();
      if (resp != null) {
        return resp;
      }

      String pathStr = FileHandler.getFullPath(this.cloudFetchedUser.passwordDbName);
      boolean isOk = supaUtils.downloadVault(new File(pathStr).getName(),
          Path.of(pathStr));
      if (!isOk) {
        return new BackendError(BackendError.ErrorTypes.FailedToDownloadDbFile,
            "[LoginUser.login] Failed to download DB file from the cloud");
      }
    }

    // registered in the local, but not on cloud -> Sync with local DB
    if ((this.cloudFetchedUser.lastLoggedInTime == -1) && (this.fetchedUser.lastLoggedInTime != -1)) {
      resp = syncWithLocalDb();
      if (resp != null) {
        return resp;
      }

      String pathStr = FileHandler.getFullPath(this.fetchedUser.passwordDbName);
      boolean isOk = supaUtils.uploadVault(Path.of(pathStr), new File(pathStr).getName());
      if (!isOk) {
        return new BackendError(BackendError.ErrorTypes.FailedToUploadDbFile,
            "[LoginUser.login] Failed to upload DB file to the cloud");
      }
    }

    // registered in both
    if ((this.cloudFetchedUser.lastLoggedInTime != -1) && (this.fetchedUser.lastLoggedInTime != -1)) {
      boolean conflictingUsername = !fetchedUser.username.equals(cloudFetchedUser.username);
      boolean conflictingEmail = !fetchedUser.email.equals(cloudFetchedUser.email);
      boolean conflictingHashedPassword = !fetchedUser.hashedPassword.equals(cloudFetchedUser.hashedPassword);

      // the entries in the two DBs conflict with each other -> Delete the local entry
      if (conflictingUsername || conflictingEmail || conflictingHashedPassword) {
        resp = resoveDbConflict();
        if (resp != null) {
          return resp;
        }
      }

      String pathStr = FileHandler.getFullPath(this.fetchedUser.passwordDbName);
      boolean isOk = supaUtils.downloadVault(new File(pathStr).getName(),
          Path.of(pathStr.concat("_for_merging")));
      if (!isOk) {
        return new BackendError(BackendError.ErrorTypes.FailedToDownloadDbFile,
            "[LoginUser.login] Failed to download DB file from the cloud");
      }

    }

    generatePasswordHash();
    if ((!this.hashedPassword.equals(this.fetchedUser.hashedPassword))) {
      // failed to log in
      this.fetchedUser = null;
      return new BackendError(BackendError.ErrorTypes.InvalidLoginCredentials,
          "[LoginUser.login] Login credentials don't match");
    }

    // successful login
    try {
      this.localDbOps.updateLastLoginTime(this.username, System.currentTimeMillis());
      this.cloudDbOps.updateLastLoginTime(this.username, System.currentTimeMillis());
    } catch (Exception e) {
      // it's ok even if it fails to update. Just let it know in the logs
      System.err.println("[LoginUser.login] Failed to update the last login time: " + e);
    }

    // merge DB files if there were copies of them in local disk and cloud
    String dbPath = FileHandler.getFullPath(this.fetchedUser.passwordDbName);
    File dbInLocalDisk = new File(dbPath);
    File dbToMerge = new File(dbPath.concat("_for_merging"));

    if (!dbInLocalDisk.exists()) {
      dbToMerge.renameTo(dbInLocalDisk);
      return null;
    }

    if (dbToMerge.exists()) {
      try (VaultManager vm = new VaultManager(dbPath, this.plaintextPassword)) {
        VaultStatus status = vm.connectToDB();
        if (status != VaultStatus.DBConnectionSuccess) {
          return new BackendError(BackendError.ErrorTypes.LocalDBCreationFailed,
              "[LoginUser.login] Failed to connect to the local database for merging");
        }

        status = vm.merge(new VaultManager(dbPath.concat("_for_merging"),
            this.plaintextPassword));
        if (status != VaultStatus.DBMergeSuccess) {
          return new BackendError(BackendError.ErrorTypes.FailedToMergeDbFiles,
              "[LoginUser.login] Failed to merge the cloud and local database files");
        }

        dbToMerge.delete();
        boolean isOk = supaUtils.uploadVault(Path.of(dbPath), new File(dbPath).getName());
        if (!isOk) {
          return new BackendError(BackendError.ErrorTypes.FailedToUploadDbFile,
              "[LoginUser.login] Failed to upload the merged DB file to the cloud");
        }
      }
    }

    return null;
  }

  private BackendError syncWithCloudDb() {
    try {
      BackendError response = this.localDbOps.addUser(cloudFetchedUser.username, cloudFetchedUser.email,
          cloudFetchedUser.hashedPassword,
          cloudFetchedUser.salt, cloudFetchedUser.passwordDbName, cloudFetchedUser.lastLoggedInTime);

      if (response != null) {
        return new BackendError(BackendError.ErrorTypes.FailedToSyncWithCloud,
            "[LoginUser.login] Failed to add the cloud user entry to the local database: " + response.getErrorType()
                + " -> " + response.getContext());
      }

      this.fetchedUser = this.cloudFetchedUser;
    } catch (Exception e) {
      return new BackendError(BackendError.ErrorTypes.FailedToSyncWithCloud,
          "[LoginUser.login] Failed to add the cloud user entry to the local database: " + e);
    }

    return null;
  }

  private BackendError syncWithLocalDb() {
    try {
      BackendError response = this.cloudDbOps.addUser(fetchedUser.username, fetchedUser.email,
          fetchedUser.hashedPassword,
          fetchedUser.salt, fetchedUser.passwordDbName, fetchedUser.lastLoggedInTime);

      if (response != null) {
        return new BackendError(BackendError.ErrorTypes.FailedToSyncWithLocal,
            "[LoginUser.login] Failed to add the local user entry to the cloud database: " + response.getErrorType()
                + " -> " + response.getContext());
      }

      this.cloudFetchedUser = this.fetchedUser;
    } catch (Exception e) {
      return new BackendError(BackendError.ErrorTypes.FailedToSyncWithLocal,
          "[LoginUser.login] Failed to add the local user entry to the cloud database: " + e);
    }

    return null;
  }

  private BackendError resoveDbConflict() {
    try {
      BackendError response = this.localDbOps.deleteUser(this.fetchedUser.username);

      if (response != null) {
        return new BackendError(BackendError.ErrorTypes.FailedToRemoveLocalConflict,
            "[LoginUser.login] Failed to remove the local conflicting entry: " + response.getErrorType()
                + " -> " + response.getContext());
      }
    } catch (Exception e) {
      return new BackendError(BackendError.ErrorTypes.FailedToRemoveLocalConflict,
          "[LoginUser.login] Failed to remove the local conflicting entry: " + e);
    }

    return null;
  }

  /*
   * A valid username can only include alphanumeric characters.
   */
  private boolean isValidUsername(String uname) {
    for (int i = 0; i < uname.length(); i++) {
      char c = uname.charAt(i);

      boolean isLowercase = (c >= 'a') && (c <= 'z');
      boolean isUppercase = (c >= 'A') && (c <= 'Z');
      boolean isNumeric = (c >= '0') && (c <= '9');

      if (!isLowercase && !isUppercase && !isNumeric) {
        return false;
      }
    }

    return true;
  }

  private void generatePasswordHash() {
    if (this.fetchedUser == null) {
      return;
    }
    // For salt
    byte[] salt = Base64.getDecoder().decode(this.fetchedUser.salt);

    // For the hash (+salt)
    KeySpec spec = new PBEKeySpec(this.plaintextPassword.toCharArray(), salt, 65536, 128);
    SecretKeyFactory factory;
    byte[] hash;

    try {
      factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      hash = factory.generateSecret(spec).getEncoded();

      this.hashedPassword = Base64.getEncoder().encodeToString(hash);
    } catch (Exception e) {
      System.err.println(
          "[RegisterUser] Either the PBKDF2WithHmacSHA1 hashing algorithm is not available or the provided PBEKeySpec is wrong: "
              + e);
      System.exit(1);
    }
  }

  public String getDbFilePath() {
    // redundant code for clarity
    if (this.fetchedUser.passwordDbName == null) {
      return null;
    }

    return FileHandler.getFullPath(this.fetchedUser.passwordDbName);
  }

  public String getPlaintextPassword() {
    return this.plaintextPassword;
  }
}
