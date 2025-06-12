package org.backend;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Random;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class LoginUser {
  private String username;
  private String email;
  private String plaintextPassword;
  private String hashedPassword;
  private String dbPath;
  private UserInfo fetchedUser;

  public LoginUser(String uname, String email, String pwd) {
    this.username = uname;
    this.email = email;
    this.plaintextPassword = pwd;
  }

  public BackendError login() {
    DBConnection db = new DBConnection();

    try {
      this.fetchedUser = db.getUserInfo(this.username);

    } catch (Exception e) {
      System.err.println("[LoginUser] Failed to get user info from database");
      return new BackendError(BackendError.AllErrorCodes.DbTransactionError, "Failed to get user info from database",
          "login");
    }

    generatePasswordHash();
    if ((!this.hashedPassword.equals(this.fetchedUser.hashedPassword))
        || (!this.email.equals(this.fetchedUser.email))) {
      // failed to log in
      this.fetchedUser = null;
      return new BackendError(BackendError.AllErrorCodes.InvalidLoginCredentials, "Login credentials don't match",
          "LoginUser.login");
    }

    // successful login; now decrypt the local db file
    try {
      db.updateLastLoginTime(this.username, System.currentTimeMillis());
    } catch (Exception e) {
      System.err.println("[LoginUser] Failed to update the last login time: " + e.toString());
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
          "[RegisterUser] Either the PBKDF2WithHmacSHA1 hashing algorithm is not available or the provided PBEKeySpec is wrong: "
              + e.toString());
      System.exit(1);
    }
  }

  public BackendError verifyDbFilePath() {
    // if the user isn't logged in, return BackendError
    if (this.fetchedUser == null) {
      return new BackendError(BackendError.AllErrorCodes.UserNotLoggedIn,
          "[LoginUser.verifyDbFilePath] User not logged in",
          "LoginUser.verifyDbFilePath");
    }
    // verify if the Db file is stored in the path saved in the DB
    Path dirPath = Paths.get(this.fetchedUser.passwordDbPath);
    if (!Files.exists(dirPath)) {
      return new BackendError(BackendError.AllErrorCodes.DbFileDoesNotExist,
          "[LoginUser.verifyDbFilePath] The database file does not exist in the saved directory",
          "LoginUser.verifyDbFilePath");
    }

    return null;
  }

  private int getRandomNum() {
    // generate a random number from 1 to 90,000
    final int MIN = 1;
    final int MAX = 90000;
    Random rand = new Random();
    int randNum = rand.nextInt(MAX - MIN) + MIN;

    return randNum;
  }

  public BackendError generateNewDbFile() {
    // if the user isn't logged in, return BackendError
    if (this.fetchedUser == null) {
      return new BackendError(BackendError.AllErrorCodes.UserNotLoggedIn,
          "[LoginUser.verifyDbFilePath] User not logged in",
          "LoginUser.verifyDbFilePath");
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

        return new BackendError(BackendError.AllErrorCodes.FileSystemError,
            "[LoginUser.generateNewDbFile] Failed to create the YAPM directory", "LoginUser.generateNewDbFile");
      }
    }

    // now the YAPM directory exists, just need to generate a suitable name for the
    // db file
    dbFileName = this.username + getRandomNum() + ".db";
    this.dbPath = new File(dbStoreDirectory, dbFileName).toString();

    return null;
  }

  public String getDbFilePath() {
    // redundant code for clarity
    if (this.dbPath == null) {
      return null;
    }

    return this.dbPath;
  }
}
