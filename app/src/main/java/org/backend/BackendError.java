package org.backend;

public class BackendError {
  public enum ErrorTypes {
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

    FailedToSyncWithCloud,
    FailedToSyncWithLocal,
    FailedToRemoveLocalConflict,
  }

  private final ErrorTypes errorType;
  private final String additionalContext;

  BackendError(ErrorTypes errType, String errMsg) {
    this.errorType = errType;
    this.additionalContext = errMsg;
  }

  public ErrorTypes getErrorType() {
    return this.errorType;
  }

  public String getContext() {
    return this.additionalContext;
  }
}
