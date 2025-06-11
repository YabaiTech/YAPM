package org.backend;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Random;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class RegisterUser {
  private String username;
  private String email;
  private String plaintextPassword;
  private String hashedPassword;

  public RegisterUser() {
  }

  public BackendError setUsername(String uname) {
    if (isValidUsername(uname)) {
      this.username = uname;
      return null;
    }

    return new BackendError(BackendError.AllErrorCodes.InvalidUserName,
        "Username contains characters that are not alphabets or numbers", "Backend.setUsername");
  }

  /*
   * A valid username can only include alphanumeric characters.
   */
  private boolean isValidUsername(String uname) {
    for (int i = 0; i < uname.length(); i++) {
      char c = uname.charAt(i);

      boolean isLowercase = ((c >= 'a') && (c <= 'z')) ? true : false;
      boolean isUppercase = ((c >= 'A') && (c <= 'Z')) ? true : false;
      boolean isNumeric = ((c >= '0') && (c <= '9')) ? true : false;

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

    return new BackendError(BackendError.AllErrorCodes.InvalidEmail, "Invalid email provided", "setEmail");
  }

  private boolean isValidEmail(String email) {
    // placeholder
    return true;
  }

  public BackendError setPassword(String pwd) {
    BackendError pwdValidity = isValidPassword(pwd);
    if (pwdValidity == null) {
      this.plaintextPassword = pwd;
      return null;
    }

    return pwdValidity;
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
    boolean isAtleast8Chars = false;
    boolean hasAtleast1Lowercase = false;
    boolean hasAtleast1Uppercase = false;
    boolean hasAtleast1Number = false;
    boolean hasAtleast1Special = false;

    if (pwd.length() >= 8) {
      isAtleast8Chars = true;
    } else {
      return new BackendError(BackendError.AllErrorCodes.PasswordNeedsToBeAtleast8Chars,
          "Password needs to be at least 8 characters long", "isValidPassword");
    }

    for (int i = 0; i < pwd.length(); i++) {
      char c = pwd.charAt(i);

      boolean isLowercase = ((c >= 'a') && (c <= 'z')) ? true : false;
      boolean isUppercase = ((c >= 'A') && (c <= 'Z')) ? true : false;
      boolean isNumeric = ((c >= '0') && (c <= '9')) ? true : false;
      boolean specialCondition = ((c >= '!') && (c <= '/')) || ((c >= ':') && (c <= '@')) || ((c >= '[') && (c <= '`'))
          || ((c >= '{') && (c <= '~'));
      boolean isSpecialChar = (specialCondition) ? true : false;

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
        return new BackendError(BackendError.AllErrorCodes.PasswordContainsUnallowedChars,
            "Password can only have characters that are lowercase or uppercase alphabets, numbers, special characters",
            "isValidPassword");
      }

      if (isAtleast8Chars && hasAtleast1Lowercase && hasAtleast1Uppercase && hasAtleast1Number && hasAtleast1Special) {
        return null;
      }
    }

    if (!hasAtleast1Lowercase) {
      return new BackendError(BackendError.AllErrorCodes.PasswordNeedsAtleast1Lowercase,
          "Password needs to have at least 1 lowercase letter", "isValidPassword");
    }
    if (!hasAtleast1Uppercase) {
      return new BackendError(BackendError.AllErrorCodes.PasswordNeedsAtleast1Uppercase,
          "Password needs to have at least 1 uppercase letter", "isValidPassword");
    }
    if (!hasAtleast1Number) {
      return new BackendError(BackendError.AllErrorCodes.PasswordNeedsAtleast1Number,
          "Password needs to have at least 1 number", "isValidPassword");
    }
    if (!hasAtleast1Special) {
      return new BackendError(BackendError.AllErrorCodes.PasswordNeedsAtleast1SpecialChar,
          "Password needs to have at least 1 special character", "isValidPassword");
    }

    return null; // unreachable
  }

  private BackendError isEverythingSet() {
    if (this.username == null) {
      return new BackendError(BackendError.AllErrorCodes.UsernameNotProvided, "Username not provided",
          "isEverythingSet");
    }
    if (this.email == null) {
      return new BackendError(BackendError.AllErrorCodes.EmailNotProvided, "Email not provided", "isEverythingSet");
    }
    if (this.plaintextPassword == null) {
      return new BackendError(BackendError.AllErrorCodes.PasswordNotProvided, "Password not provided",
          "isEverythingSet");
    }
    if (this.hashedPassword == null) {
      return new BackendError(BackendError.AllErrorCodes.HashedPasswordNotGenerated, "Hashed password not generated",
          "isEverythingSet");
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

  public String getValidDbFilePath() {
    // If in Linux, store in `/home/<<current_user>>`
    // If in Window, store in the documents folder
    // Other OS, store it in the `pwd = present working directory`

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
        System.exit(1);
      }
    }

    // now the YAPM directory exists, just need to generate a suitable name for the
    // db file
    dbFileName = this.username + getRandomNum() + ".db";
    return new File(dbStoreDirectory, dbFileName).toString();
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

  public BackendError register() {
    generatePasswordHash();
    BackendError response = isEverythingSet();
    if (response != null) {
      return response;
    }

    // save to the DB
    DBConnection db = new DBConnection();

    try {
      String dbFilePath = getValidDbFilePath();
      db.addUser(this.username, this.email, this.hashedPassword, dbFilePath, System.currentTimeMillis());
    } catch (Exception e) {
      return new BackendError(BackendError.AllErrorCodes.DbTransactionError, "Failed to add user to the database",
          "register()");
    }

    return null;
  }

}
