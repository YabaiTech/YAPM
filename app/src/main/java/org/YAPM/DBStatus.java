package org.YAPM;

public enum DBStatus {
  DBConnectionSuccess,
  DBConnectionFailure,
  DBCreateVaultSuccess,
  DBCreateVaultFailure,
  DBOpenVaultSuccess,
  DBOpenVaultFailure,
  DBAddEntrySuccess,
  DBAddEntryFailureException,
  DBAddEntryFailureEmptyParameter,
  DBDeleteEntrySuccess,
  DBDeleteEntryFailureException,
  DBDeleteEntryFailureInvalidID,
  DBBadVerificationFormat,
  DBWrongMasterPasswd,
  DBCloseSuccess,
  DBCloseFailure
}
