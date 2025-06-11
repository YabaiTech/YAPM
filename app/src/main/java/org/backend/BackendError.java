package org.backend;

public class BackendError {
  public static enum AllErrorCodes {
    DbTransactionError,

    InvalidUserName,
    InvalidEmail,

    PasswordContainsUnallowedChars,
    PasswordNeedsToBeAtleast8Chars,
    PasswordNeedsAtleast1Lowercase,
    PasswordNeedsAtleast1Uppercase,
    PasswordNeedsAtleast1Number,
    PasswordNeedsAtleast1SpecialChar,

    UsernameNotProvided,
    EmailNotProvided,
    PasswordNotProvided,
    HashedPasswordNotGenerated,

    InvalidLoginCredentials
  }

  private AllErrorCodes errorCode;
  private String errorMessage;
  private String errorCreator;

  BackendError(AllErrorCodes errCode, String errMsg, String errCreator) {
    this.errorCode = errCode;
    this.errorMessage = errMsg;
    this.errorCreator = errCreator;
  }

  public AllErrorCodes getErrorCode() {
    return this.errorCode;
  }

  public String getErrorMessage() {
    return this.errorMessage;
  }

  public String getErrorCreator() {
    return this.errorCreator;
  }
}
