package org.YAPM;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class VaultManager {
  private final static String JDBC_PREFIX = "jdbc:sqlite:";
  private final static String VERIFICATION_TEXT = "vault_verification";

  public static void createVault(String dbPath, String masterPasswd) throws Exception {
    if (dbPath.isEmpty() || masterPasswd.isEmpty()) {
      throw new IllegalArgumentException("[VaultManager] ERROR: db path or master password is empty.");
    }
    if (new File(dbPath).exists()) {
      throw new IllegalArgumentException("[VaultManager] ERROR: db already exists.");
    }

    String url = JDBC_PREFIX + dbPath;

    try {
      Connection connection = DriverManager.getConnection(url);
      connection.setAutoCommit(false);

      Statement statement = connection.createStatement();
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

      EncryptedData encryptedVerificationText = CryptoUtils.encrypt(VERIFICATION_TEXT, masterPasswd);
      String verTextB64 = encryptedVerificationText.getCipherText() + ":" + encryptedVerificationText.getIV();

      PreparedStatement preparedStatement = connection
          .prepareStatement("INSERT INTO metadata(salt,verification) VALUES(?,?)");
      preparedStatement.setString(1, encryptedVerificationText.getSalt());
      preparedStatement.setString(2, verTextB64);
      preparedStatement.executeUpdate();

      connection.commit();
    } catch (SQLException e) {
      e.printStackTrace();
      throw new IllegalStateException("[VaultManager] ERROR: failed to create vault.");
    }
  }

  public static List<Entry> openVault(String dbPath, String masterPasswd) throws Exception {
    if (dbPath.isEmpty() || masterPasswd.isEmpty()) {
      throw new IllegalArgumentException("[VaultManager] ERROR: db path or master password is empty.");
    }
    if (!new File(dbPath).exists()) {
      throw new IllegalStateException("[VaultManager] ERROR: db doesn't exist.");
    }

    String url = JDBC_PREFIX + dbPath;
    try (Connection connection = DriverManager.getConnection(url)) {
      connection.setAutoCommit(false);

      String saltB64 = Base64.getEncoder().encodeToString(verifyMasterPasswd(connection, masterPasswd));

      List<Entry> entries = new ArrayList<>();
      try (PreparedStatement preparedStatement = connection
          .prepareStatement("SELECT url, username, password, iv FROM entries")) {
        ResultSet resultSet = preparedStatement.executeQuery();

        while (resultSet.next()) {
          String urlField = resultSet.getString("url");
          String usernameField = resultSet.getString("username");
          String passwdField = resultSet.getString("password");
          String ivField = resultSet.getString("iv");

          EncryptedData passwd = new EncryptedData(passwdField, ivField, saltB64);
          String plainPasswd = CryptoUtils.decrypt(passwd, masterPasswd);

          entries.add(new Entry(urlField, usernameField, plainPasswd));
        }
      }

      connection.commit();
      return entries;
    } catch (SQLException e) {
      System.out.println("[VaultManager] ERROR: ");
      e.printStackTrace();
      return null;
    }
  }

  public static void addEntry(String dbPath, String masterPasswd, String urlField, String usernameField,
      String plainPasswdField) throws Exception {
    if (dbPath.isEmpty() || masterPasswd.isEmpty() || urlField.isEmpty() || usernameField.isEmpty()
        || plainPasswdField.isEmpty()) {
      throw new IllegalArgumentException(
          "[VaultManager] ERROR: db path, master password, URL field, username, or the password field is empty.");
    }

    String url = JDBC_PREFIX + dbPath;
    try (Connection connection = DriverManager.getConnection(url)) {
      connection.setAutoCommit(false);

      byte[] salt = verifyMasterPasswd(connection, masterPasswd);

      EncryptedData encrypted = CryptoUtils.encrypt(plainPasswdField, masterPasswd, salt);
      String cipherB64 = encrypted.getCipherText();
      String ivB64 = encrypted.getIV();

      try (PreparedStatement preparedStatement = connection
          .prepareStatement("INSERT INTO entries(url, username, password, iv) VALUES(?,?,?,?)")) {
        preparedStatement.setString(1, urlField);
        preparedStatement.setString(2, usernameField);
        preparedStatement.setString(3, cipherB64);
        preparedStatement.setString(4, ivB64);

        preparedStatement.executeUpdate();
      }

      connection.commit();
    } catch (SQLException e) {
      e.printStackTrace();

      throw new IllegalStateException("[VaultManager] ERROR: failed to add entry.");
    }
  }

  public static void deleteEntry(String dbPath, String masterPasswd, int entryID)
      throws Exception, IllegalArgumentException {
    if (dbPath.isEmpty() || masterPasswd.isEmpty()) {
      throw new IllegalArgumentException("[VaultManager] ERROR: db path or master password is empty.");
    }

    String url = JDBC_PREFIX + dbPath;
    try (Connection connection = DriverManager.getConnection(url)) {
      connection.setAutoCommit(false);
      verifyMasterPasswd(connection, masterPasswd);

      try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM entries WHERE id = ?")) {
        preparedStatement.setInt(1, entryID);
        int affected = preparedStatement.executeUpdate();
        if (affected == 0) {
          throw new IllegalArgumentException("No entry found with ID: " + entryID);
        }
      }

      connection.commit();
    } catch (SQLException e) {
      System.out.println("[VaultManager] ERROR: ");
      e.printStackTrace();
    }
  }

  private static byte[] verifyMasterPasswd(Connection connection, String masterPasswd) throws Exception {
    String saltB64, verPayload;

    try (Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT salt, verification FROM metadata LIMIT 1")) {
      if (!resultSet.next()) {
        throw new IllegalStateException("[VaultManager] ERROR: metadata table is missing or corrupted.");
      }

      saltB64 = resultSet.getString("salt");
      verPayload = resultSet.getString("verification");
    } catch (SQLException e) {
      System.out.println("[VaultManager] ERROR: ");
      e.printStackTrace();
      return null;
    }

    String[] segments = verPayload.split(":");
    if (segments.length != 2) {
      throw new IllegalStateException("[VaultManager] ERROR: bad verification format.");
    }

    String cipherMasterPasswd = segments[0];
    String ivB64 = segments[1];

    EncryptedData verificationData = new EncryptedData(cipherMasterPasswd, ivB64, saltB64);
    String decrypted = CryptoUtils.decrypt(verificationData, masterPasswd);
    if (!decrypted.equals(VERIFICATION_TEXT)) {
      throw new SecurityException("[VaultManager] ERROR: incorrect master password.");
    }

    return Base64.getDecoder().decode(saltB64);
  }
}
