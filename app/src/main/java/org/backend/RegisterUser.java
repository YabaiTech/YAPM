package org.backend;

import org.utils.InputValidator;
import org.vault.*;

import java.io.File;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class RegisterUser {
  private final DBOperations localDbOps;
  private final DBOperations cloudDbOps;
  private String username;
  private String email;
  private String plaintextPassword;
  private String hashedPassword;
  private String hashSaltBase64;
  private String dbFileName;

  public RegisterUser(DatabaseConnection loaclDb, DatabaseConnection cloudDb) {
    this.localDbOps = new DBOperations(loaclDb);
    this.cloudDbOps = new DBOperations(cloudDb);
  }

  public BackendError setUsername(String uname) {
    if (InputValidator.isValidUsername(uname)) {
      this.username = uname;
      return null;
    }

    return new BackendError(BackendError.ErrorTypes.InvalidUserName,
        "[RegisterUser.setUsername] Username contains characters that are not alphabets or numbers");
  }

  public BackendError setEmail(String email) {
    if (InputValidator.isValidEmail(email)) {
      this.email = email;
      return null;
    }

    return new BackendError(BackendError.ErrorTypes.InvalidEmail, "[RegisterUser.setEmail] Invalid email provided");
  }

  public BackendError setPassword(String pwd) {
    BackendError pwdProblems = InputValidator.isValidPassword(pwd);
    if (pwdProblems == null) {
      this.plaintextPassword = pwd;
      return null;
    }

    return pwdProblems;
  }

  private BackendError isEverythingSet() {
    String errorTag = "[RegisterUser.isEverythingSet] ";
    if (this.username == null) {
      return new BackendError(BackendError.ErrorTypes.UsernameNotProvided, errorTag + "Username not provided");
    }
    if (this.email == null) {
      return new BackendError(BackendError.ErrorTypes.EmailNotProvided, errorTag + "Email not provided");
    }
    if (this.plaintextPassword == null) {
      return new BackendError(BackendError.ErrorTypes.PasswordNotProvided, errorTag + "Password not provided");
    }
    if (this.hashedPassword == null) {
      return new BackendError(BackendError.ErrorTypes.HashedPasswordNotGenerated,
          errorTag + "Hashed password not generated");
    }
    if (this.hashSaltBase64 == null) {
      return new BackendError(BackendError.ErrorTypes.SaltForHashNotGenerated,
          errorTag + "The salt for the hash is not generated");
    }
    if (this.dbFileName == null) {
      return new BackendError(BackendError.ErrorTypes.DbFileNameNotSet, errorTag + "Local database filename not set");
    }

    return null;
  }

  private BackendError setDbFilename() {
    boolean isOk = FileHandler.createDbStoreDirIfNotExisting();
    if (!isOk) {
      return new BackendError(BackendError.ErrorTypes.FailedToCreateDbDir,
          "[RegisterUser.setDbFilename] Failed to create `YAPM` directory to store local DB files");
    }

    this.dbFileName = this.username + UUID.randomUUID().toString() + ".db";
    return null;
  }

  private BackendError createLocalDb(String dbName) {
    try (VaultManager vm = new VaultManager(FileHandler.getFullPath(dbName), this.plaintextPassword)) {
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

  private boolean isUsernameTaken(String username) {
    try {
      UserInfo cloudFetchedUser = this.cloudDbOps.getUserInfo(username);

      // check the sentinel value to decide whether the user exists
      return cloudFetchedUser.lastLoggedInTime != -1;
    } catch (Exception e) {
      return false;
    }
  }

  private boolean isEmailAlreadyUsed(String email) {
    try {
      UserInfo cloudFetchedUser = this.cloudDbOps.getUserInfoByEmail(email);

      // check the sentinel value to decide whether the user exists
      return cloudFetchedUser.lastLoggedInTime != -1;
    } catch (Exception e) {
      System.err.println("[RegisterUser.isEmailAlreadyUsed] Failed to check if the email is already used: " + e);
      return false;
    }
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

      this.hashSaltBase64 = Base64.getEncoder().encodeToString(salt);
      this.hashedPassword = Base64.getEncoder().encodeToString(hash);
    } catch (Exception e) {
      System.err.println(
          "[RegisterUser.generatePasswordHash] Either the PBKDF2WithHmacSHA1 hashing algorithm is not available or the provided PBEKeySpec is wrong: "
              + e);
      System.exit(1);
    }
  }

  public BackendError register() {
    generatePasswordHash();
    setDbFilename();

    BackendError response = isEverythingSet();
    if (response != null) {
      return response;
    }

    // check if the provided username is already taken
    if (isUsernameTaken(this.username)) {
      return new BackendError(BackendError.ErrorTypes.UsernameAlreadyExists,
          "[RegisterUser.register] A user with that username already exists");
    }

    // check if the provided email is already taken
    if (isEmailAlreadyUsed(this.email)) {
      return new BackendError(BackendError.ErrorTypes.EmailAlreadyExists,
          "[RegisterUser.register] A user is already registered using that email");
    }

    try {
      response = createLocalDb(this.dbFileName);
      if (response != null) {
        return response;
      }
    } catch (Exception e) {
      return new BackendError(BackendError.ErrorTypes.FailedToCreateDbDir,
          "[RegisterUser.register] Failed to create the YAPM directory. Given exception: " + e);
    }

    try {
      BackendError resp = this.localDbOps.addUser(this.username, this.email, this.hashedPassword, this.hashSaltBase64,
          // FileHandler.getFullPath(this.dbFileName),
          this.dbFileName,
          System.currentTimeMillis());
      if (resp != null) {
        return resp;
      }

      resp = this.cloudDbOps.addUser(this.username, this.email, this.hashedPassword, this.hashSaltBase64,
          // FileHandler.getFullPath(this.dbFileName),
          this.dbFileName,
          System.currentTimeMillis());
      if (resp != null) {
        this.localDbOps.deleteUser(this.username);
        return resp;
      }
    } catch (Exception e) {
      return new BackendError(BackendError.ErrorTypes.DbTransactionError,
          "[RegisterUser.register] Failed to add user to the database. Given exception: " + e);
    }

    SupabaseUtils supaUtils = new SupabaseUtils();
    String localDbFilePath = FileHandler.getFullPath(this.dbFileName);
    boolean isOk = supaUtils.uploadVault(Path.of(localDbFilePath), new File(localDbFilePath).getName());
    if (!isOk) {
      return new BackendError(BackendError.ErrorTypes.FailedToUploadDbFile,
          "[LoginUser.login] Failed to upload DB file to the cloud");
    }

    return null;
  }

}
