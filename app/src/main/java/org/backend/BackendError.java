package org.backend;

public class BackendError {
  public static enum ErrorTypes {
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
    SaltForHashNotGenerated,

    UsernameAlreadyExists,
    EmailAlreadyExists,

    FailedToCreateDbDir,

    InvalidLoginCredentials,

    DbFileDoesNotExist,

    UserNotLoggedIn,

    FileSystemError,

    LocalDBCreationFailed,
  }

  private final ErrorTypes errorType;
  private final String additionalContext;

  BackendError(ErrorTypes errCode, String errMsg) {
    this.errorType = errCode;
    this.additionalContext = errMsg;
  }

  public ErrorTypes getErrorType() {
    return this.errorType;
  }

  public String getContext() {
    return this.additionalContext;
  }
}
