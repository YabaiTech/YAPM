package org.backend;

import org.vault.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
  private String dbPath;
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
        this.fetchedUser = this.localDbOps.getUserInfo(this.username);
        this.cloudFetchedUser = this.cloudDbOps.getUserInfo(this.username);
      } else {
        this.fetchedUser = this.localDbOps.getUserInfoByEmail(this.email);
        this.cloudFetchedUser = this.cloudDbOps.getUserInfoByEmail(this.email);
      }
    } catch (Exception e) {
      return new BackendError(BackendError.ErrorTypes.DbTransactionError,
          "[LoginUser.login] Failed to get master user info from database. Given exception: " + e);
    }

    BackendError response;

    // registered in no DB
    if ((this.cloudFetchedUser.lastLoggedInTime == -1) && (this.fetchedUser.lastLoggedInTime == -1)) {
      System.out.println("I'm inside none");
      return new BackendError(BackendError.ErrorTypes.UserDoesNotExist,
          "[LoginUser.login] Failed to login. No user registered using that username/email.");
    }

    // registered in the cloud, but not on local
    if ((this.cloudFetchedUser.lastLoggedInTime != -1) && (this.fetchedUser.lastLoggedInTime == -1)) {
      System.out.println("I'm only inside cloud");
      try {
        response = this.localDbOps.addUser(cloudFetchedUser.username, cloudFetchedUser.email,
            cloudFetchedUser.hashedPassword,
            cloudFetchedUser.salt, cloudFetchedUser.passwordDbPath, cloudFetchedUser.lastLoggedInTime);

        if (response != null) {
          return new BackendError(BackendError.ErrorTypes.FailedToSyncWithCloud,
              "[LoginUser.login] Failed to add the cloud user entry to the local database: " + response.getErrorType()
                  + " -> " + response.getContext());
        }
      } catch (Exception e) {
        return new BackendError(BackendError.ErrorTypes.FailedToSyncWithCloud,
            "[LoginUser.login] Failed to add the cloud user entry to the local database: " + e);
      }
    }

    // registered in the local, but not on cloud
    if ((this.cloudFetchedUser.lastLoggedInTime == -1) && (this.fetchedUser.lastLoggedInTime != -1)) {
      System.out.println("I'm only inside local");
      try {
        response = this.cloudDbOps.addUser(fetchedUser.username, fetchedUser.email, fetchedUser.hashedPassword,
            fetchedUser.salt, fetchedUser.passwordDbPath, fetchedUser.lastLoggedInTime);

        if (response != null) {
          return new BackendError(BackendError.ErrorTypes.FailedToSyncWithLocal,
              "[LoginUser.login] Failed to add the local user entry to the cloud database: " + response.getErrorType()
                  + " -> " + response.getContext());
        }
      } catch (Exception e) {
        return new BackendError(BackendError.ErrorTypes.FailedToSyncWithLocal,
            "[LoginUser.login] Failed to add the local user entry to the cloud database: " + e);
      }
    }

    // registered in both
    if ((this.cloudFetchedUser.lastLoggedInTime != -1) && (this.fetchedUser.lastLoggedInTime != -1)) {
      System.out.println("I'm inside both");
      // different entry on local and cloud DB -> delete local user
      boolean conflictingUsername = fetchedUser.username != cloudFetchedUser.username;
      boolean conflictingEmail = fetchedUser.email != cloudFetchedUser.email;
      boolean conflictingHashedPassword = fetchedUser.hashedPassword != cloudFetchedUser.hashedPassword;

      try {
        if (conflictingUsername || conflictingEmail || conflictingHashedPassword) {
          response = this.localDbOps.deleteUser(this.fetchedUser.username);

          if (response != null) {
            return new BackendError(BackendError.ErrorTypes.FailedToRemoveLocalConflict,
                "[LoginUser.login] Failed to remove the local conflicting entry: " + response.getErrorType()
                    + " -> " + response.getContext());
          }
        }
      } catch (Exception e) {
        return new BackendError(BackendError.ErrorTypes.FailedToRemoveLocalConflict,
            "[LoginUser.login] Failed to remove the local conflicting entry: " + e);
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

  public BackendError verifyDbFilePath() {
    // if the user isn't logged in, return BackendError
    if (this.fetchedUser == null) {
      return new BackendError(BackendError.ErrorTypes.UserNotLoggedIn,
          "[LoginUser.verifyDbFilePath] User not logged in");
    }
    // verify if the Db file is stored in the path saved in the DB
    Path dirPath = Paths.get(this.fetchedUser.passwordDbPath);
    if (!Files.exists(dirPath)) {
      return new BackendError(BackendError.ErrorTypes.DbFileDoesNotExist,
          "[LoginUser.verifyDbFilePath] The database file does not exist in the saved directory");
    }

    this.dbPath = dirPath.toString();
    return null;
  }

  private String getRandomUUID() {
    return UUID.randomUUID().toString();
  }

  private BackendError createLocalDb(String dbPath) {
    try (VaultManager vm = new VaultManager(dbPath, this.plaintextPassword)) {
      VaultStatus resp = vm.connectToDB();
      if (resp != VaultStatus.DBConnectionSuccess) {
        return new BackendError(BackendError.ErrorTypes.LocalDBCreationFailed,
            "[RegisterUser.createLocalDb] Failed to create vault. Provided error: " + resp);
      }

      resp = vm.createVault();
      if (resp != VaultStatus.DBCreateVaultSuccess) {
        return new BackendError(BackendError.ErrorTypes.LocalDBCreationFailed,
            "[RegisterUser.createLocalDb] Failed to create vault. Provided error: " + resp);
      }

      return null;
    }
  }

  public BackendError getNewDbFilePath() {
    // if the user isn't logged in, return BackendError
    if (this.fetchedUser == null) {
      return new BackendError(BackendError.ErrorTypes.UserNotLoggedIn,
          "[LoginUser.verifyDbFilePath] User not logged in");
    }

    // the db file will be stored in the `YAPM` directory inside the user's home
    // directory in their OS
    String os = System.getProperty("os.name");
    String homeDir = System.getProperty("user.home");
    String dbStoreDirectory;
    String dbFileName;

    if (os.equalsIgnoreCase("windows")) {
      dbStoreDirectory = homeDir + "\\YAPM";
    } else {
      dbStoreDirectory = homeDir + "/YAPM";
    }
    Path dirPath = Paths.get(dbStoreDirectory);
    if (!Files.exists(dirPath)) {
      File newDir = new File(homeDir, "YAPM");
      if (newDir.mkdir()) {
        System.out.println("[RegisterUser] Created the YAPM directory");
      } else {
        System.err.println("[RegisterUser] Failed to create the YAPM directory");

        return new BackendError(BackendError.ErrorTypes.FileSystemError,
            "[LoginUser.generateNewDbFile] Failed to create the YAPM directory");
      }
    }

    // now the YAPM directory exists, just need to generate a suitable name for the
    // db file
    dbFileName = this.username + getRandomUUID() + ".db";
    String newDbPath = new File(dbStoreDirectory, dbFileName).toString();

    BackendError response = createLocalDb(newDbPath);
    if (response != null) {
      return response;
    }
    this.dbPath = newDbPath;

    return null;
  }

  public String getDbFilePath() {
    // redundant code for clarity
    if (this.dbPath == null) {
      return null;
    }

    return this.dbPath;
  }

  public String getPlaintextPassword() {
    return this.plaintextPassword;
  }

}
