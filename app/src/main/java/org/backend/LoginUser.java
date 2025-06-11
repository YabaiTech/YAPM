package org.backend;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

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

    if ((this.hashedPassword != this.fetchedUser.hashedPassword) || (this.email != this.fetchedUser.email)) {
      return new BackendError(BackendError.AllErrorCodes.InvalidLoginCredentials, "Login credentials don't match",
          "LoginUser.login");
    }

    // successful login; now decrypt the local db file

    return null;
  }

  private void generatePasswordHash() {
    // For salt
    SecureRandom random = new SecureRandom();
    byte[] salt = new byte[16];
    random.nextBytes(salt);

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

  private BackendError getDbFilePath() {
    // use `this.username` and the gernerated hash from `this.password` to
    // successfully login and retrieve dbFilePath
    this.dbPath = "";

    return null;
  }
}
