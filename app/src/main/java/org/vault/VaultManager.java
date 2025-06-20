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

        Entry entry = new Entry(id, plainUrl, plainUsername, plainPasswd);
        entries.add(entry);
      }
      System.out.println("\nOPENED " + this.dbPath + ":\n");
      for (Entry e : entries) {
        e.display();
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

    try (PreparedStatement preparedStatement = this.connection.prepareStatement(
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

  public static VaultStatus merge(String newDbPath, VaultManager v1, VaultManager v2) {
    if (!v1.masterPasswd.equals(v2.masterPasswd)) {
      return VaultStatus.DBMergeDifferentMasterPasswd;
    }
    if (v1.connection == null) {
      if (v1.connectToDB() != VaultStatus.DBConnectionSuccess) {
        return VaultStatus.DBConnectionFailure;
      }
    }
    if (v2.connection == null) {
      if (v2.connectToDB() != VaultStatus.DBConnectionSuccess) {
        return VaultStatus.DBParameterVaultConnectionFailure;
      }
    }

    byte[] salt1, salt2;
    try {
      salt1 = v1.verifyMasterPasswd(v1.masterPasswd);
      salt2 = v2.verifyMasterPasswd(v2.masterPasswd);

      if (salt1 == null || salt2 == null) {
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

    try {
      VaultManager nv = new VaultManager(newDbPath, v1.masterPasswd);
      nv.connectToDB();
      try (Statement stmt = nv.connection.createStatement()) {
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS metadata (" +
            "  salt TEXT NOT NULL," +
            "  verification TEXT NOT NULL" +
            ");");
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS entries (" +
            "  id TEXT PRIMARY KEY," +
            "  url TEXT NOT NULL," +
            "  username TEXT NOT NULL," +
            "  password TEXT NOT NULL," +
            "  iv TEXT NOT NULL," +
            "  timestamp INTEGER NOT NULL" +
            ");");
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS deleted (" +
            "  id TEXT PRIMARY KEY," +
            "  deleted_at INTEGER NOT NULL" +
            ");");

        stmt.executeUpdate("DELETE FROM metadata;");
      }

      try (ResultSet rs = v1.connection.createStatement().executeQuery(
          "SELECT salt, verification FROM metadata LIMIT 1");
          PreparedStatement ps = nv.connection.prepareStatement(
              "INSERT INTO metadata(salt,verification) VALUES(?,?)")) {
        if (!rs.next()) {
          return VaultStatus.DBBadVerificationFormat;
        }
        ps.setString(1, rs.getString("salt"));
        ps.setString(2, rs.getString("verification"));

        ps.executeUpdate();
      }

      // load all unique IDs from both v1 and v2 and both both entries and deleted
      // tables
      PreparedStatement allIds = nv.connection.prepareStatement(
          "SELECT id FROM entries UNION SELECT id FROM deleted");
      nv.connection.createStatement().execute("ATTACH DATABASE '" + v1.dbPath + "' AS v1;");
      nv.connection.createStatement().execute("ATTACH DATABASE '" + v2.dbPath + "' AS v2;");

      ResultSet rsAll = nv.connection.createStatement().executeQuery(
          "SELECT id FROM v1.entries UNION SELECT id FROM v2.entries " +
              "UNION SELECT id FROM v1.deleted UNION SELECT id FROM v2.deleted");

      while (rsAll.next()) {
        String id = rsAll.getString("id");
        Record rec = new Record();
        // load local and remote entries and deletes
        Record r1 = rec.loadRecord(nv.connection, "v1", id);
        Record r2 = rec.loadRecord(nv.connection, "v2", id);

        // decide latest state
        // if both are deleted, pick max deletedAt
        long finalDeleted = -1;
        if (r1.deletedAt != null || r2.deletedAt != null) {
          finalDeleted = Math.max(
              r1.deletedAt == null ? -1 : r1.deletedAt,
              r2.deletedAt == null ? -1 : r2.deletedAt);
        }

        // find latest entry ts
        long latestEntryTs = Math.max(r1.timestamp, r2.timestamp);

        // apply tombstone wins if delete is newer than entry
        if (finalDeleted > latestEntryTs) {
          // write tombstone only and delete the entry from new vault
          rec.upsertDeleted(nv.connection, id, finalDeleted);
          rec.deleteEntryRow(nv.connection, id);
        } else {
          // pick newer record to write and remove the entry from table deleted
          Record chosen = (r1.timestamp >= r2.timestamp) ? r1 : r2;

          rec.upsertEntry(nv.connection, chosen);
          rec.deleteDeletedRow(nv.connection, id);
        }
      }

      nv.connection.commit();
      nv.connection.createStatement().execute("DETACH DATABASE v1;");
      nv.connection.createStatement().execute("DETACH DATABASE v2;");
      ArrayList<Entry> entries = new ArrayList<>();
      nv.openVault(entries);

      System.out.println("\nMERGED " + v1.dbPath + " + " + v2.dbPath + " = " + nv.dbPath + ":\n");
      for (Entry e : entries) {
        e.display();
      }
      nv.close();

      return VaultStatus.DBMergeSuccess;
    } catch (SQLException e) {
      System.out.println("[VaultManager.merge] ERROR: ");
      e.printStackTrace();
      return VaultStatus.DBMergeFailureException;
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

  public static String computeId(String url, String username) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    md.update((url + ":" + username).getBytes(StandardCharsets.UTF_8));

    return Base64.getEncoder().encodeToString(md.digest());
  }
}
