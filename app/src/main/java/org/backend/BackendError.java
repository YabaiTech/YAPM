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

    InvalidLoginCredentials,

    DbFileDoesNotExist,

    UserNotLoggedIn,

    FileSystemError,
  }

  private AllErrorCodes errorType;
  private String additionalContext;

  BackendError(AllErrorCodes errCode, String errMsg) {
    this.errorType = errCode;
    this.additionalContext = errMsg;
  }

  public AllErrorCodes getErrorCode() {
    return this.errorType;
  }

  public String getContext() {
    return this.additionalContext;
  }
}
