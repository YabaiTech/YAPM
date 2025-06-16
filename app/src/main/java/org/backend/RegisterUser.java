package org.backend;

import org.vault.*;

import java.io.File;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
  private String localDbFilePath;

  public RegisterUser(DatabaseConnection loaclDb, DatabaseConnection cloudDb) {
    this.localDbOps = new DBOperations(loaclDb);
    this.cloudDbOps = new DBOperations(cloudDb);
  }

  public BackendError setUsername(String uname) {
    if (isValidUsername(uname)) {
      this.username = uname;
      return null;
    }

    return new BackendError(BackendError.ErrorTypes.InvalidUserName,
        "[RegisterUser.setUsername] Username contains characters that are not alphabets or numbers");
  }

  /*
   * A valid username can only include alphanumeric characters.
   */
  private boolean isValidUsername(String uname) {
    if (uname.isEmpty()) {
      return false;
    }

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

  public BackendError setEmail(String email) {
    if (isValidEmail(email)) {
      this.email = email;
      return null;
    }

    return new BackendError(BackendError.ErrorTypes.InvalidEmail, "[RegisterUser.setEmail] Invalid email provided");
  }

  private boolean isValidEmail(String email) {
    if (email.isEmpty()) {
      return false;
    }

    String[] emailsParts = email.split("@");
    if (emailsParts.length != 2 || emailsParts[0].isEmpty() ||
        emailsParts[1].isEmpty()) {
      return false;
    }

    String[] urlParts = emailsParts[1].split("\\.");
    if (urlParts.length < 2 || urlParts[0].isEmpty() || urlParts[1].isEmpty()) {
      return false;
    }

    return true;
  }

  public BackendError setPassword(String pwd) {
    BackendError pwdProblems = isValidPassword(pwd);
    if (pwdProblems == null) {
      this.plaintextPassword = pwd;
      return null;
    }

    return pwdProblems;
  }

  /*
   * A valid password has to meet the following criteria:
   * 1) At least 8 characters long
   * 2) At least 1 lowercase letter
   * 3) At least 1 uppercase letter
   * 4) At least 1 numberical digit
   * 5) At least 1 special characters/symbols (Make sure UTF-8)
   * 6) No characters are outside the above catagories are allowed
   */
  private BackendError isValidPassword(String pwd) {
    String errorTag = "[RegisterUser.isValidPassword] ";

    boolean isAtleast8Chars = false;
    boolean hasAtleast1Lowercase = false;
    boolean hasAtleast1Uppercase = false;
    boolean hasAtleast1Number = false;
    boolean hasAtleast1Special = false;

    if (pwd.length() >= 8) {
      isAtleast8Chars = true;
    } else {
      return new BackendError(BackendError.ErrorTypes.PasswordNeedsToBeAtleast8Chars,
          errorTag + "Password needs to be at least 8 characters long");
    }

    for (int i = 0; i < pwd.length(); i++) {
      char c = pwd.charAt(i);

      boolean isLowercase = (c >= 'a') && (c <= 'z');
      boolean isUppercase = (c >= 'A') && (c <= 'Z');
      boolean isNumeric = (c >= '0') && (c <= '9');
      boolean specialCondition = ((c >= '!') && (c <= '/')) || ((c >= ':') && (c <= '@')) || ((c >= '[') && (c <= '`'))
          || ((c >= '{') && (c <= '~'));
      boolean isSpecialChar = specialCondition;

      if (isLowercase) {
        hasAtleast1Lowercase = true;
      } else if (isUppercase) {
        hasAtleast1Uppercase = true;
      } else if (isNumeric) {
        hasAtleast1Number = true;
      } else if (isSpecialChar) {
        hasAtleast1Special = true;
      } else {
        // contains characters that are outside the allowed characters
        return new BackendError(BackendError.ErrorTypes.PasswordContainsUnallowedChars,
            errorTag
                + "Password can only have characters that are lowercase or uppercase alphabets, numbers, special characters");
      }

      if (isAtleast8Chars && hasAtleast1Lowercase && hasAtleast1Uppercase && hasAtleast1Number && hasAtleast1Special) {
        return null;
      }
    }

    if (!hasAtleast1Lowercase) {
      return new BackendError(BackendError.ErrorTypes.PasswordNeedsAtleast1Lowercase,
          errorTag + "Password needs to have at least 1 lowercase letter");
    }
    if (!hasAtleast1Uppercase) {
      return new BackendError(BackendError.ErrorTypes.PasswordNeedsAtleast1Uppercase,
          errorTag + "Password needs to have at least 1 uppercase letter");
    }
    if (!hasAtleast1Number) {
      return new BackendError(BackendError.ErrorTypes.PasswordNeedsAtleast1Number,
          errorTag + "Password needs to have at least 1 number");
    }
    if (!hasAtleast1Special) {
      return new BackendError(BackendError.ErrorTypes.PasswordNeedsAtleast1SpecialChar,
          errorTag + "Password needs to have at least 1 special character");
    }

    return null; // Unreachable code. For LSP.
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

    return null;
  }

  private String getRandomUUID() {
    return UUID.randomUUID().toString();
  }

  private String getDbFilePath() throws FileSystemException {
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

        throw new FileSystemException("[RegisterUser.getDbFilePath] Failed to create the YAPM directory");
      }
    }

    // now the YAPM directory exists, just need to generate a suitable name for the
    // db file
    dbFileName = this.username + getRandomUUID() + ".db";

    return new File(dbStoreDirectory, dbFileName).toString();
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
          "[RegisterUser] Either the PBKDF2WithHmacSHA1 hashing algorithm is not available or the provided PBEKeySpec is wrong: "
              + e);
      System.exit(1);
    }
  }

  public BackendError register() {
    generatePasswordHash();
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
      this.localDbFilePath = getDbFilePath();
      response = createLocalDb(this.localDbFilePath);
      if (response != null) {
        return response;
      }
    } catch (Exception e) {
      return new BackendError(BackendError.ErrorTypes.FailedToCreateDbDir,
          "[RegisterUser.register] Failed to create the YAPM directory. Given exception: " + e);
    }

    try {
      BackendError resp = this.localDbOps.addUser(this.username, this.email, this.hashedPassword, this.hashSaltBase64,
          this.localDbFilePath,
          System.currentTimeMillis());
      if (resp != null) {
        return resp;
      }

      resp = this.cloudDbOps.addUser(this.username, this.email, this.hashedPassword, this.hashSaltBase64,
          this.localDbFilePath,
          System.currentTimeMillis());
      if (resp != null) {
        this.localDbOps.deleteUser(this.username);
        return resp;
      }
    } catch (Exception e) {
      return new BackendError(BackendError.ErrorTypes.DbTransactionError,
          "[RegisterUser.register] Failed to add user to the database. Given exception: " + e);
    }

    return null;
  }

}
