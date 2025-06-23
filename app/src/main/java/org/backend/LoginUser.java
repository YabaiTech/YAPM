package org.backend;

import org.utils.InputValidator;
import org.vault.*;

import java.io.File;
import java.nio.file.Path;
import java.security.spec.KeySpec;
import java.util.Base64;

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

    if (InputValidator.isValidUsername(accountIdentifier)) {
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
    String localDbPath = FileHandler.getFullPath(this.fetchedUser.passwordDbName);
    String cloudDbPath = localDbPath.concat("_for_merging");

    File localDbFile = new File(localDbPath);
    File cloudDbFile = new File(cloudDbPath);

    if (!localDbFile.exists()) {
      cloudDbFile.renameTo(localDbFile);
      return null;
    }

    if (cloudDbFile.exists()) {

      String mergedDbTempName = System.currentTimeMillis() + ".db";
      String mergedDbTempPath = FileHandler.getFullPath(mergedDbTempName);
      File newlyMergedDbFile = new File(mergedDbTempPath);

      try (VaultManager vm = new VaultManager(localDbPath, this.plaintextPassword);
          VaultManager otherVm = new VaultManager(localDbPath.concat("_for_merging"), this.plaintextPassword)) {

        VaultStatus status = VaultManager.merge(mergedDbTempPath, vm, otherVm);
        if (status != VaultStatus.DBMergeSuccess) {
          return new BackendError(BackendError.ErrorTypes.FailedToMergeDbFiles,
              "[LoginUser.login] Failed to merge the cloud and local database files");
        }

        vm.close();
        otherVm.close();

        cloudDbFile.delete();
        boolean isOk = localDbFile.delete();
        if (!isOk) {
          return new BackendError(BackendError.ErrorTypes.FileSystemError,
              "[LoginUser.sync] Failed to delete the old DB file");
        }

        isOk = newlyMergedDbFile.renameTo(localDbFile);
        if (!isOk) {
          return new BackendError(BackendError.ErrorTypes.FileSystemError,
              "[LoginUser.sync] Failed to rename the newly merged DB file to the assigned name");
        }

        isOk = supaUtils.uploadVault(Path.of(localDbPath), localDbFile.getName());
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
          "[LoginUser.generatePasswordHash] Either the PBKDF2WithHmacSHA1 hashing algorithm is not available or the provided PBEKeySpec is wrong: "
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

  public BackendError sync() {

    if (this.fetchedUser == null) {
      return new BackendError(BackendError.ErrorTypes.UserNotLoggedIn, "[LoginUser.logout] User isn't logged in");
    }

    String localDbPath = FileHandler.getFullPath(this.fetchedUser.passwordDbName);
    String cloudDbPath = localDbPath.concat("_for_merging");

    SupabaseUtils supaUtils = new SupabaseUtils();
    boolean isOk = supaUtils.downloadVault(new File(localDbPath).getName(), Path.of(cloudDbPath));
    if (!isOk) {
      return new BackendError(BackendError.ErrorTypes.FailedToDownloadDbFile,
          "[LoginUser.login] Failed to download DB file from the cloud");
    }

    // merge DBs
    File cloudDbFile = new File(cloudDbPath);
    File localDbFile = new File(localDbPath);

    try (VaultManager vm = new VaultManager(localDbPath, this.plaintextPassword);
        VaultManager otherVm = new VaultManager(cloudDbPath, this.plaintextPassword);) {

      String mergedDbTempName = System.currentTimeMillis() + ".db";
      String mergedDbTempPath = FileHandler.getFullPath(mergedDbTempName);
      File newlyMergedDb = new File(mergedDbTempPath);

      VaultStatus status = VaultManager.merge(mergedDbTempPath, vm, otherVm);
      if (status != VaultStatus.DBMergeSuccess) {
        return new BackendError(BackendError.ErrorTypes.FailedToMergeDbFiles,
            "[LoginUser.login] Failed to merge the cloud and local database files");
      }

      cloudDbFile.delete();
      isOk = localDbFile.delete();
      if (!isOk) {
        return new BackendError(BackendError.ErrorTypes.FileSystemError,
            "[LoginUser.sync] Failed to delete the old DB file");
      }

      isOk = newlyMergedDb.renameTo(localDbFile);
      if (!isOk) {
        return new BackendError(BackendError.ErrorTypes.FileSystemError,
            "[LoginUser.sync] Failed to rename the newly merged DB file to the assigned name");
      }

      isOk = supaUtils.uploadVault(Path.of(localDbPath), localDbFile.getName());
      if (!isOk) {
        return new BackendError(BackendError.ErrorTypes.FailedToUploadDbFile,
            "[LoginUser.login] Failed to upload the merged DB file to the cloud");
      }
    }

    return null;
  }

  public BackendError logout() {
    BackendError err = sync();
    if (err != null) {
      return err;
    }

    // attempt to update the last logged in time (failing isn't a fatal error)
    try {
      this.localDbOps.updateLastLoginTime(this.username, System.currentTimeMillis());
      this.cloudDbOps.updateLastLoginTime(this.username, System.currentTimeMillis());
    } catch (Exception e) {
      // it's ok even if it fails to update. Just let it know in the logs
      System.err.println("[LoginUser.login] Failed to update the last login time: " + e);
    }

    return null;
  }
}
