package org.utils;

import org.backend.*;

public class InputValidator {
  /*
   * A valid username can only include alphanumeric characters.
   */
  public static boolean isValidUsername(String uname) {
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

  public static boolean isValidEmail(String email) {
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

  /*
   * A valid password has to meet the following criteria:
   * 1) At least 8 characters long
   * 2) At least 1 lowercase letter
   * 3) At least 1 uppercase letter
   * 4) At least 1 numberical digit
   * 5) At least 1 special characters/symbols (Make sure UTF-8)
   * 6) No characters are outside the above catagories are allowed
   */
  public static BackendError isValidPassword(String pwd) {
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
}
