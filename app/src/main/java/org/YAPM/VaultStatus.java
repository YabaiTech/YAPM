package org.YAPM;

public enum VaultStatus {
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
