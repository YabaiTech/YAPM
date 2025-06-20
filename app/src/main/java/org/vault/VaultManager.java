package org.vault;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
          "  id TEXT PRIMARY KEY," +
          "  url TEXT NOT NULL," +
          "  username TEXT NOT NULL," +
          "  password TEXT NOT NULL," +
          "  iv TEXT NOT NULL," +
          "  timestamp INTEGER NOT NULL" +
          ");");
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS deleted (" +
          "  id TEXT PRIMARY KEY," +
          "  deleted_at INTEGER NOT NULL" +
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

    String saltB64 = Base64.getEncoder().encodeToString(salt);
    try (PreparedStatement preparedStatement = this.connection
        .prepareStatement("SELECT e.id, e.url, e.username, e.password, e.iv " +
            " FROM entries e " +
            " LEFT JOIN deleted d ON e.id = d.id " +
            " WHERE d.id IS NULL;")) {
      ResultSet resultSet = preparedStatement.executeQuery();

      while (resultSet.next()) {
        String id = resultSet.getString("id");
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
    String id;
    try {
      salt = verifyMasterPasswd(this.masterPasswd);
      id = computeId(urlField, usernameField);

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
        .prepareStatement(
            "INSERT OR REPLACE INTO entries(id, url, username, password, iv, timestamp) VALUES(?,?,?,?,?,?)")) {
      preparedStatement.setString(1, id);
      preparedStatement.setString(2, cipherUrl);
      preparedStatement.setString(3, cipherUsername);
      preparedStatement.setString(4, cipherPasswd);
      preparedStatement.setString(5, ivB64);
      preparedStatement.setLong(6, System.currentTimeMillis());

      preparedStatement.executeUpdate();
      this.connection.commit();

      return VaultStatus.DBAddEntrySuccess;
    } catch (SQLException e) {
      System.out.println("[VaultManager.addEntry] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBAddEntryFailureException;
    }
  }

  public VaultStatus deleteEntry(String entryID) {
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

    try {
      try (PreparedStatement preparedStatement = this.connection.prepareStatement("DELETE FROM entries WHERE id = ?")) {
        preparedStatement.setString(1, entryID);
        int affected = preparedStatement.executeUpdate();
        if (affected == 0) {
          return VaultStatus.DBDeleteEntryFailureInvalidID;
        }
      }
      try (PreparedStatement preparedStatement = this.connection
          .prepareStatement("INSERT OR REPLACE INTO deleted(id,deleted_at) VALUES(?,?)")) {
        preparedStatement.setString(1, entryID);
        preparedStatement.setLong(2, System.currentTimeMillis());

        preparedStatement.executeUpdate();
      }

      this.connection.commit();
      return VaultStatus.DBDeleteEntrySuccess;
    } catch (SQLException e) {
      System.out.println("[VaultManager.deleteEntry] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBDeleteEntryFailureException;
    }
  }

  public VaultStatus editEntry(String entryID, String newUrl, String newUsername, String newPasswd) {
    if (newUrl.isEmpty() || newUsername.isEmpty() || newPasswd.isEmpty()) {
      return VaultStatus.DBEditEntryFailureEmptyParameter;
    }

    byte[] salt;
    String id;
    try {
      salt = verifyMasterPasswd(this.masterPasswd);
      id = computeId(newUrl, newUsername);

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

    try {
      try (PreparedStatement ps = this.connection.prepareStatement(
          "DELETE FROM entries WHERE id = ?")) {
        ps.setString(1, entryID);

        int affected = ps.executeUpdate();
        if (affected == 0) {
          return VaultStatus.DBEditEntryFailureInvalidID;
        }
      }

      try (PreparedStatement ps = this.connection.prepareStatement(
          "INSERT OR REPLACE INTO deleted(id,deleted_at) VALUES(?,?)")) {
        ps.setString(1, entryID);
        ps.setLong(2, System.currentTimeMillis());
        ps.executeUpdate();
      }

      try (PreparedStatement ps = this.connection.prepareStatement("DELETE FROM deleted WHERE id = ?")) {
        ps.setString(1, id);
        ps.executeUpdate();
      }

      try (PreparedStatement ps = connection
          .prepareStatement("INSERT INTO entries(id,url,username,password,iv,timestamp) VALUES(?,?,?,?,?,?)")) {

        ps.setString(1, id);
        ps.setString(2, cipherUrl);
        ps.setString(3, cipherUsername);
        ps.setString(4, cipherPasswd);
        ps.setString(5, ivB64);
        ps.setLong(6, System.currentTimeMillis());

        ps.executeUpdate();
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

    Set<String> allIds = new HashSet<>();
    try (
        Statement a = this.connection.createStatement();
        ResultSet ra1 = a.executeQuery("SELECT id FROM entries");
        ResultSet ra2 = a.executeQuery("SELECT id FROM deleted");
        Statement b = other.connection.createStatement();
        ResultSet rb1 = b.executeQuery("SELECT id FROM entries");
        ResultSet rb2 = b.executeQuery("SELECT id FROM deleted")) {
      while (ra1.next())
        allIds.add(ra1.getString("id"));
      while (ra2.next())
        allIds.add(ra2.getString("id"));
      while (rb1.next())
        allIds.add(rb1.getString("id"));
      while (rb2.next())
        allIds.add(rb2.getString("id"));
    } catch (SQLException e) {
      e.printStackTrace();
      return VaultStatus.DBMergeFailureException;
    }

    try {
      PreparedStatement lookupEntryA = connection.prepareStatement(
          "SELECT url,username,password,iv,timestamp FROM entries WHERE id=?");
      PreparedStatement lookupDelA = connection.prepareStatement(
          "SELECT deleted_at FROM deleted WHERE id=?");
      PreparedStatement deleteLocal = connection.prepareStatement(
          "DELETE FROM entries WHERE id=?");
      PreparedStatement tombstoneLocal = connection.prepareStatement(
          "INSERT OR REPLACE INTO deleted(id,deleted_at) VALUES(?,?)");
      PreparedStatement upsertLocal = connection.prepareStatement(
          "INSERT OR REPLACE INTO entries(id,url,username,password,iv,timestamp) VALUES(?,?,?,?,?,?)");

      PreparedStatement lookupEntryB = other.connection.prepareStatement(
          "SELECT url,username,password,iv,timestamp FROM entries WHERE id=?");
      PreparedStatement lookupDelB = other.connection.prepareStatement(
          "SELECT deleted_at FROM deleted WHERE id=?");

      for (String id : allIds) {
        Long tsEA = null, tsDA = null, tsEB = null, tsDB = null;
        String urlA = null, userA = null, passA = null, ivA = null;
        String urlB = null, userB = null, passB = null, ivB = null;

        lookupEntryA.setString(1, id);
        try (ResultSet r = lookupEntryA.executeQuery()) {
          if (r.next()) {
            tsEA = r.getLong("timestamp");
            urlA = r.getString("url");
            userA = r.getString("username");
            passA = r.getString("password");
            ivA = r.getString("iv");
          }
        }
        lookupDelA.setString(1, id);
        try (ResultSet r = lookupDelA.executeQuery()) {
          if (r.next())
            tsDA = r.getLong("deleted_at");
        }

        lookupEntryB.setString(1, id);
        try (ResultSet r = lookupEntryB.executeQuery()) {
          if (r.next()) {
            tsEB = r.getLong("timestamp");
            urlB = r.getString("url");
            userB = r.getString("username");
            passB = r.getString("password");
            ivB = r.getString("iv");
          }
        }
        lookupDelB.setString(1, id);
        try (ResultSet r = lookupDelB.executeQuery()) {
          if (r.next())
            tsDB = r.getLong("deleted_at");
        }

        long maxDel = Math.max(tsDA == null ? 0L : tsDA,
            tsDB == null ? 0L : tsDB);
        long maxEnt = Math.max(tsEA == null ? 0L : tsEA,
            tsEB == null ? 0L : tsEB);

        if (maxDel > maxEnt) {
          deleteLocal.setString(1, id);

          deleteLocal.executeUpdate();

          tombstoneLocal.setString(1, id);
          tombstoneLocal.setLong(2, maxDel);

          tombstoneLocal.executeUpdate();
        } else {
          boolean pickA = tsEA != null && (tsEB == null || tsEA >= tsEB);
          String srcUrl = pickA ? urlA : urlB;
          String srcUser = pickA ? userA : userB;
          String srcPass = pickA ? passA : passB;
          String srcIv = pickA ? ivA : ivB;
          long srcTs = pickA ? tsEA : tsEB;

          upsertLocal.setString(1, id);
          upsertLocal.setString(2, srcUrl);
          upsertLocal.setString(3, srcUser);
          upsertLocal.setString(4, srcPass);
          upsertLocal.setString(5, srcIv);
          upsertLocal.setLong(6, srcTs);
          upsertLocal.executeUpdate();
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

  public static String computeId(String url, String username) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    md.update((url + ":" + username).getBytes(StandardCharsets.UTF_8));

    return Base64.getEncoder().encodeToString(md.digest());
  }
}
