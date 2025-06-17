package org.vault;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;

public class VaultManager implements AutoCloseable {
  private final String JDBC_PREFIX = "jdbc:sqlite:";
  private final String VERIFICATION_TEXT = "vault_verification";
  private final String dbPath;
  private final String masterPasswd;
  private Connection connection;

  public VaultManager(String dbPath, String masterPasswd) {
    this.dbPath = dbPath;
    this.masterPasswd = masterPasswd;
  }

  @Override
  public void close() {
    this.closeDB();
  }

  public VaultStatus createVault() {
    try (Statement statement = this.connection.createStatement()) {
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS metadata (" +
          "  salt TEXT NOT NULL," +
          "  verification TEXT NOT NULL" +
          ");");
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS entries (" +
          "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
          "  url TEXT NOT NULL," +
          "  username TEXT NOT NULL," +
          "  password TEXT NOT NULL," +
          "  iv TEXT NOT NULL," +
          "  timestamp INTEGER NOT NULL" +
          ");");
      statement.executeUpdate("DELETE FROM metadata;");

      EncryptedData encryptedVerificationText;
      try {
        encryptedVerificationText = CryptoUtils.encrypt(VERIFICATION_TEXT, masterPasswd);
      } catch (Exception e) {
        System.out.println("[VaultManager.createVault] ERROR: ");
        e.printStackTrace();
        return VaultStatus.DBCreateVaultFailure;
      }
      String verTextB64 = encryptedVerificationText.getCipherText() + ":" + encryptedVerificationText.getIV();

      try (PreparedStatement preparedStatement = this.connection
          .prepareStatement("INSERT INTO metadata(salt,verification) VALUES(?,?)")) {
        preparedStatement.setString(1, encryptedVerificationText.getSalt());
        preparedStatement.setString(2, verTextB64);
        preparedStatement.executeUpdate();
      }

      this.connection.commit();
      return VaultStatus.DBCreateVaultSuccess;
    } catch (SQLException e) {
      System.out.println("[VaultManager.createVault] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBCreateVaultFailure;
    }
  }

  public VaultStatus openVault(ArrayList<Entry> entries) {
    byte[] salt;
    try {
      salt = verifyMasterPasswd(this.masterPasswd);
      if (salt == null) {
        return VaultStatus.DBOpenVaultFailure;
      }
    } catch (IllegalStateException e) {
      System.out.println("[VaultManager.openVault] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBBadVerificationFormat;
    } catch (SecurityException e) {
      System.out.println("[VaultManager.openVault] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBWrongMasterPasswd;
    } catch (Exception e) {
      System.out.println("[VaultManager.openVault] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBOpenVaultFailure;
    }

    try (PreparedStatement preparedStatement = this.connection
        .prepareStatement("SELECT id, url, username, password, iv FROM entries")) {
      String saltB64 = Base64.getEncoder().encodeToString(salt);
      ResultSet resultSet = preparedStatement.executeQuery();

      while (resultSet.next()) {
        int id = resultSet.getInt("id");
        String urlField = resultSet.getString("url");
        String usernameField = resultSet.getString("username");
        String passwdField = resultSet.getString("password");
        String ivField = resultSet.getString("iv");

        EncryptedData url = new EncryptedData(urlField, ivField, saltB64);
        EncryptedData username = new EncryptedData(usernameField, ivField, saltB64);
        EncryptedData passwd = new EncryptedData(passwdField, ivField, saltB64);

        String plainUrl, plainUsername, plainPasswd;
        try {
          plainUrl = CryptoUtils.decrypt(url, masterPasswd);
          plainUsername = CryptoUtils.decrypt(username, masterPasswd);
          plainPasswd = CryptoUtils.decrypt(passwd, masterPasswd);
        } catch (Exception e) {
          System.out.println("[VaultManager.openVault] ERROR: ");
          e.printStackTrace();
          return VaultStatus.DBOpenVaultFailure;
        }

        entries.add(new Entry(id, plainUrl, plainUsername, plainPasswd));
      }

      this.connection.commit();
      return VaultStatus.DBOpenVaultSuccess;
    } catch (SQLException e) {
      System.out.println("[VaultManager.openVault] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBOpenVaultFailure;
    }
  }

  public VaultStatus addEntry(String urlField, String usernameField, String passwdField) {
    if (urlField.isEmpty() || usernameField.isEmpty() || passwdField.isEmpty()) {
      return VaultStatus.DBAddEntryFailureEmptyParameter;
    }

    byte[] salt;
    try {
      salt = verifyMasterPasswd(this.masterPasswd);
      if (salt == null) {
        return VaultStatus.DBAddEntryFailureException;
      }
    } catch (IllegalStateException e) {
      System.out.println("[VaultManager.addEntry] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBBadVerificationFormat;
    } catch (SecurityException e) {
      System.out.println("[VaultManager.addEntry] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBWrongMasterPasswd;
    } catch (Exception e) {
      System.out.println("[VaultManager.addEntry] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBAddEntryFailureException;
    }

    EncryptedData encryptedUrl, encryptedUsername, encryptedPasswd;
    try {
      encryptedUrl = CryptoUtils.encrypt(urlField, this.masterPasswd, salt);

      final byte[] iv = Base64.getDecoder().decode(encryptedUrl.getIV());
      encryptedUsername = CryptoUtils.encrypt(usernameField, this.masterPasswd, salt, iv);
      encryptedPasswd = CryptoUtils.encrypt(passwdField, this.masterPasswd, salt, iv);
    } catch (Exception e) {
      System.out.println("[VaultManager.addEntry] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBAddEntryFailureException;
    }
    String cipherUrl = encryptedUrl.getCipherText();
    String cipherUsername = encryptedUsername.getCipherText();
    String cipherPasswd = encryptedPasswd.getCipherText();
    String ivB64 = encryptedUrl.getIV();

    try (PreparedStatement preparedStatement = this.connection
        .prepareStatement("INSERT INTO entries(url, username, password, iv, timestamp) VALUES(?,?,?,?,?)")) {
      preparedStatement.setString(1, cipherUrl);
      preparedStatement.setString(2, cipherUsername);
      preparedStatement.setString(3, cipherPasswd);
      preparedStatement.setString(4, ivB64);
      preparedStatement.setLong(5, System.currentTimeMillis());

      preparedStatement.executeUpdate();
      this.connection.commit();

      return VaultStatus.DBAddEntrySuccess;
    } catch (SQLException e) {
      System.out.println("[VaultManager.addEntry] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBAddEntryFailureException;
    }
  }

  public VaultStatus deleteEntry(int entryID) {
    try {
      byte[] salt = verifyMasterPasswd(this.masterPasswd);
      if (salt == null) {
        return VaultStatus.DBDeleteEntryFailureException;
      }
    } catch (IllegalStateException e) {
      System.out.println("[VaultManager.deleteEntry] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBBadVerificationFormat;
    } catch (SecurityException e) {
      System.out.println("[VaultManager.deleteEntry] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBWrongMasterPasswd;
    } catch (Exception e) {
      System.out.println("[VaultManager.deleteEntry] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBDeleteEntryFailureException;
    }

    try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM entries WHERE id = ?")) {
      preparedStatement.setInt(1, entryID);
      int affected = preparedStatement.executeUpdate();
      if (affected == 0) {
        return VaultStatus.DBDeleteEntryFailureInvalidID;
      }

      this.connection.commit();
      return VaultStatus.DBDeleteEntrySuccess;
    } catch (SQLException e) {
      System.out.println("[VaultManager.deleteEntry] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBDeleteEntryFailureException;
    }
  }

  public VaultStatus editEntry(int entryID, String newUrl, String newUsername, String newPasswd) {
    if (newUrl.isEmpty() || newUsername.isEmpty() || newPasswd.isEmpty()) {
      return VaultStatus.DBEditEntryFailureEmptyParameter;
    }

    byte[] salt;
    try {
      salt = verifyMasterPasswd(this.masterPasswd);
      if (salt == null) {
        return VaultStatus.DBEditEntryFailureException;
      }
    } catch (IllegalStateException e) {
      System.out.println("[VaultManager.editEntry] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBBadVerificationFormat;
    } catch (SecurityException e) {
      System.out.println("[VaultManager.editEntry] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBWrongMasterPasswd;
    } catch (Exception e) {
      System.out.println("[VaultManager.editEnty] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBEditEntryFailureException;
    }

    EncryptedData encryptedUrl, encryptedUsername, encryptedPasswd;
    try {
      encryptedUrl = CryptoUtils.encrypt(newUrl, this.masterPasswd, salt);

      final byte[] iv = Base64.getDecoder().decode(encryptedUrl.getIV());
      encryptedUsername = CryptoUtils.encrypt(newUsername, this.masterPasswd, salt, iv);
      encryptedPasswd = CryptoUtils.encrypt(newPasswd, this.masterPasswd, salt, iv);
    } catch (Exception e) {
      System.out.println("[VaultManager.editEntry] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBEditEntryFailureException;
    }
    String cipherUrl = encryptedUrl.getCipherText();
    String cipherUsername = encryptedUsername.getCipherText();
    String cipherPasswd = encryptedPasswd.getCipherText();
    String ivB64 = encryptedUrl.getIV();

    try (PreparedStatement ps = connection.prepareStatement(
        "UPDATE entries " +
            "SET url = ?, username = ?, password = ?, iv = ?, timestamp = ? " +
            "WHERE id = ?")) {

      ps.setString(1, cipherUrl);
      ps.setString(2, cipherUsername);
      ps.setString(3, cipherPasswd);
      ps.setString(4, ivB64);
      ps.setLong(5, System.currentTimeMillis());
      ps.setInt(6, entryID);

      int affected = ps.executeUpdate();
      if (affected == 0) {
        return VaultStatus.DBEditEntryFailureInvalidID;
      }

      this.connection.commit();
      return VaultStatus.DBEditEntrySuccess;
    } catch (SQLException e) {
      System.out.println("[VaultManager.editEntry] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBEditEntryFailureException;
    }
  }

  private byte[] verifyMasterPasswd(String masterPasswd) throws Exception {
    String saltB64, verPayload;

    try (Statement statement = this.connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT salt, verification FROM metadata LIMIT 1")) {
      if (!resultSet.next()) {
        throw new IllegalStateException(
            "[VaultManager.verifyMasterPasswd] ERROR: metadata table is missing or corrupted.");
      }

      saltB64 = resultSet.getString("salt");
      verPayload = resultSet.getString("verification");
    } catch (SQLException e) {
      System.out.println("[VaultManager.verifyMasterPasswd] ERROR: ");
      e.printStackTrace();
      return null;
    }

    String[] segments = verPayload.split(":");
    if (segments.length != 2) {
      throw new IllegalStateException("[VaultManager.verifyMasterPasswd] ERROR: bad verification format.");
    }

    String cipherMasterPasswd = segments[0];
    String ivB64 = segments[1];

    EncryptedData verificationData = new EncryptedData(cipherMasterPasswd, ivB64, saltB64);
    String decrypted;
    try {
      decrypted = CryptoUtils.decrypt(verificationData, masterPasswd);
    } catch (Exception e) {
      throw new SecurityException("[VaultManager.verifyMasterPasswd] ERROR: incorrect master password.", e);
    }
    if (!decrypted.equals(VERIFICATION_TEXT)) {
      throw new SecurityException("[VaultManager.verifyMasterPasswd] ERROR: incorrect master password.");
    }

    return Base64.getDecoder().decode(saltB64);
  }

  public VaultStatus merge(VaultManager other) {
    if (!this.masterPasswd.equals(other.masterPasswd)) {
      return VaultStatus.DBMergeDifferentMasterPasswd;
    }
    if (this.connection == null) {
      if (this.connectToDB() != VaultStatus.DBConnectionSuccess) {
        return VaultStatus.DBConnectionFailure;
      }
    }
    if (other.connection == null) {
      if (other.connectToDB() != VaultStatus.DBConnectionSuccess) {
        return VaultStatus.DBParameterVaultConnectionFailure;
      }
    }

    byte[] salt, otherSalt;
    try {
      salt = this.verifyMasterPasswd(this.masterPasswd);
      otherSalt = other.verifyMasterPasswd(other.masterPasswd);

      if (salt == null || otherSalt == null) {
        return VaultStatus.DBMergeFailureException;
      }
    } catch (IllegalStateException e) {
      System.out.println("[VaultManager.merge] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBBadVerificationFormat;
    } catch (SecurityException e) {
      System.out.println("[VaultManager.merge] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBWrongMasterPasswd;
    } catch (Exception e) {
      System.out.println("[VaultManager.merge] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBMergeFailureException;
    }

    try (
        PreparedStatement psOther = other.connection
            .prepareStatement("SELECT id, url, username, password, iv, timestamp FROM entries");
        ResultSet rsOther = psOther.executeQuery()) {
      String lookupSql = "SELECT timestamp FROM entries WHERE id = ?";
      String insertSql = "INSERT INTO entries(url, username, password, iv, timestamp) VALUES(?,?,?,?,?)";
      String updateSql = "UPDATE entries " +
          "SET url = ?, username = ?, password = ?, iv = ?, timestamp = ? " +
          "WHERE id = ?";

      try (PreparedStatement psLookup = this.connection.prepareStatement(lookupSql);
          PreparedStatement psInsert = this.connection.prepareStatement(insertSql);
          PreparedStatement psUpdate = this.connection.prepareStatement(updateSql)) {
        while (rsOther.next()) {
          int otherId = rsOther.getInt("id");
          String otherUrl = rsOther.getString("url");
          String otherUsername = rsOther.getString("username");
          String otherPasswd = rsOther.getString("password");
          String otherIvB64 = rsOther.getString("iv");
          long otherTimestamp = rsOther.getLong("timestamp");
          String otherSaltB64 = Base64.getEncoder().encodeToString(otherSalt);

          EncryptedData otherUrlData = new EncryptedData(otherUrl, otherIvB64, otherSaltB64);
          EncryptedData otherUsernameData = new EncryptedData(otherUsername, otherIvB64, otherSaltB64);
          EncryptedData otherPasswdData = new EncryptedData(otherPasswd, otherIvB64, otherSaltB64);
          String plainUrl, plainUsername, plainPasswd;

          try {
            plainUrl = CryptoUtils.decrypt(otherUrlData, other.masterPasswd);
            plainUsername = CryptoUtils.decrypt(otherUsernameData, other.masterPasswd);
            plainPasswd = CryptoUtils.decrypt(otherPasswdData, other.masterPasswd);
          } catch (Exception e) {
            System.out.println("[VaultManager.merge] ERROR: ");
            e.printStackTrace();
            return VaultStatus.DBMergeFailureException;
          }

          EncryptedData newUrlData, newUsernameData, newPasswdData;
          byte[] newIvBytes;
          try {
            newUrlData = CryptoUtils.encrypt(plainUrl, this.masterPasswd, salt);

            newIvBytes = Base64.getDecoder().decode(newUrlData.getIV());
            newUsernameData = CryptoUtils.encrypt(plainUsername, this.masterPasswd, salt, newIvBytes);
            newPasswdData = CryptoUtils.encrypt(plainPasswd, this.masterPasswd, salt, newIvBytes);
          } catch (Exception e) {
            System.out.println("[VaultManager.merge] ERROR: ");
            e.printStackTrace();
            return VaultStatus.DBMergeFailureException;
          }
          String newUrlCipherB64 = newUrlData.getCipherText();
          String newUsernameCipherB64 = newUsernameData.getCipherText();
          String newPasswdCipherB64 = newPasswdData.getCipherText();

          psLookup.setInt(1, otherId);
          try (ResultSet rsSelf = psLookup.executeQuery()) {
            if (rsSelf.next()) {
              long selfTimestamp = rsSelf.getLong("timestamp");
              if (otherTimestamp > selfTimestamp) {
                psUpdate.setString(1, newUrlCipherB64);
                psUpdate.setString(2, newUsernameCipherB64);
                psUpdate.setString(3, newPasswdCipherB64);
                psUpdate.setString(4, Base64.getEncoder().encodeToString(newIvBytes));
                psUpdate.setLong(5, otherTimestamp);
                psUpdate.setInt(6, otherId);
                psUpdate.executeUpdate();
              }
            } else {
              psInsert.setString(1, newUrlCipherB64);
              psInsert.setString(2, newUsernameCipherB64);
              psInsert.setString(3, newPasswdCipherB64);
              psInsert.setString(4, Base64.getEncoder().encodeToString(newIvBytes));
              psInsert.setLong(5, otherTimestamp);
              psInsert.executeUpdate();
            }
          }
        }
      }

      this.connection.commit();
      return VaultStatus.DBMergeSuccess;
    } catch (SQLException e) {
      System.out.println("[VaultManager.merge] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBMergeFailureException;
    }
  }

  public VaultStatus connectToDB() {
    try {
      this.connection = DriverManager.getConnection(JDBC_PREFIX + dbPath);
      this.connection.setAutoCommit(false);

      return VaultStatus.DBConnectionSuccess;
    } catch (SQLException e) {
      System.out.println("[VaultManager.connectToDB] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBConnectionFailure;
    }
  }

  public VaultStatus closeDB() {
    try {
      this.connection.close();

      return VaultStatus.DBCloseSuccess;
    } catch (SQLException e) {
      System.out.println("[VaultManager.closeDB] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBCloseFailure;
    }
  }
}
