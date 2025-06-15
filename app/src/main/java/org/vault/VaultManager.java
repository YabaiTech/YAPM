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
          "  iv TEXT NOT NULL" +
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

        EncryptedData passwd = new EncryptedData(passwdField, ivField, saltB64);
        String plainPasswd;
        try {
          plainPasswd = CryptoUtils.decrypt(passwd, masterPasswd);
        } catch (Exception e) {
          System.out.println("[VaultManager.openVault] ERROR: ");
          e.printStackTrace();
          return VaultStatus.DBOpenVaultFailure;
        }

        entries.add(new Entry(id, urlField, usernameField, plainPasswd));
      }

      this.connection.commit();
      return VaultStatus.DBOpenVaultSuccess;
    } catch (SQLException e) {
      System.out.println("[VaultManager.openVault] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBOpenVaultFailure;
    }
  }

  public VaultStatus addEntry(String urlField, String usernameField, String plainPasswdField) {
    if (urlField.isEmpty() || usernameField.isEmpty() || plainPasswdField.isEmpty()) {
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

    EncryptedData encrypted;
    try {
      encrypted = CryptoUtils.encrypt(plainPasswdField, this.masterPasswd, salt);
    } catch (Exception e) {
      System.out.println("[VaultManager.addEntry] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBAddEntryFailureException;
    }
    String cipherB64 = encrypted.getCipherText();
    String ivB64 = encrypted.getIV();

    try (PreparedStatement preparedStatement = this.connection
        .prepareStatement("INSERT INTO entries(url, username, password, iv) VALUES(?,?,?,?)")) {
      preparedStatement.setString(1, urlField);
      preparedStatement.setString(2, usernameField);
      preparedStatement.setString(3, cipherB64);
      preparedStatement.setString(4, ivB64);

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

  public VaultStatus editEntry(int entryID, String newUrl, String newUsername, String newPlainPasswd) {
    if (newUrl.isEmpty() || newUsername.isEmpty() || newPlainPasswd.isEmpty()) {
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

    EncryptedData encrypted;
    try {
      encrypted = CryptoUtils.encrypt(newPlainPasswd, this.masterPasswd, salt);
    } catch (Exception e) {
      System.out.println("[VaultManager.editEntry] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBEditEntryFailureException;
    }
    String cipherB64 = encrypted.getCipherText();
    String ivB64 = encrypted.getIV();

    try (PreparedStatement ps = connection.prepareStatement(
        "UPDATE entries " +
            "SET url = ?, username = ?, password = ?, iv = ? " +
            "WHERE id = ?")) {

      ps.setString(1, newUrl);
      ps.setString(2, newUsername);
      ps.setString(3, cipherB64);
      ps.setString(4, ivB64);
      ps.setInt(5, entryID);

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
