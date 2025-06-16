package org.vault;

public enum VaultStatus {
  DBConnectionSuccess,
  DBConnectionFailure,
  DBOtherConnectionFailure,

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

  DBEditEntrySuccess,
  DBEditEntryFailureEmptyParameter,
  DBEditEntryFailureInvalidID,
  DBEditEntryFailureException,

  DBBadVerificationFormat,
  DBWrongMasterPasswd,

  DBMergeSuccess,
  DBMergeFailureException,
  DBMergeDifferentMasterPasswd,

  DBCloseSuccess,
  DBCloseFailure
}
