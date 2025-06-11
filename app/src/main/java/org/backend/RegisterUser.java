package org.backend;

import java.nio.charset.StandardCharsets;

class RegisterUser {
  private String username;
  private String plaintextPassword;

  RegisterUser() {
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

  public BackendError setPassword(String pwd) {
    if (isValidPassword(pwd)) {
      this.plaintextPassword = pwd;
      return null;
    }

    return new BackendError(BackendError.AllErrorCodes.InvalidPassword, "Password doesn't fulfill the criterias",
        "setPassword");
  }

  /*
   * A valid password has to meet the following criteria:
   * 1) At least 8 characters long
   * 2) At least 1 lowercase letter
   * 3) At least 1 uppercase letter
   * 4) At least 1 numberical digit
   * 5) At least 1 special characters/symbols (Make sure UTF-8)
   * 6) No characters are outside UTF-8
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

    // check whether the string is valid UTF-8. If they enter emoji, it's not.
    if (!isUTF8(pwd)) {
      return new BackendError(BackendError.AllErrorCodes.PasswordStringIsNotUTF8,
          "Password must only contain UTF-8 characters", "isValidPassword");
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
      }
      if (isUppercase) {
        hasAtleast1Uppercase = true;
      }
      if (isNumeric) {
        hasAtleast1Number = true;
      }
      if (isSpecialChar) {
        hasAtleast1Special = true;
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

  private boolean isUTF8(String str) {
    return StandardCharsets.UTF_8.newEncoder().canEncode(str);
  }

  public BackendError register() {
    // save to the DB

    return null;
  }

}
