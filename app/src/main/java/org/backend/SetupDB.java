package org.backend;

import java.sql.*;

public class SetupDB implements AutoCloseable {
  private static Connection con;

  public static void init() {
    try {
      con = DriverManager.getConnection(EnvVars.DATABASE_BASE_URL, EnvVars.DATABASE_USER, EnvVars.DATABASE_PASSWORD);

      createDatabaseIfNotExisting();
      createTableIfNotExisting();
    } catch (Exception e) {
      System.err.println("[SetupDB.SetupDB] Failed to connect to DB: ");
      e.printStackTrace();

      System.exit(1);
    }
  }

  private static void createDatabaseIfNotExisting() {
    try {
      Statement stmt = con.createStatement();
      String dbCreationSQL = "CREATE DATABASE IF NOT EXISTS " + EnvVars.DATABASE_NAME;
      stmt.execute(dbCreationSQL);
    } catch (Exception e) {
      System.err.println("[SetupDB.createMasterUserTable] Failed to create the YAPM database: ");
      e.printStackTrace();

      System.exit(1);
    }
  }

  private static void createTableIfNotExisting() {
    try {
      Statement stmnt = con.createStatement();
      stmnt.execute(EnvVars.TABLE_CREATION_SQL);
    } catch (Exception e) {
      System.err.println("[SetupDB.createMasterUserTable] Failed to create master user table: " + e);
    }
  }

  @Override
  public void close() {
    try {
      con.close();
    } catch (Exception e) {
      // If it errors, just let it know using a log
      System.err.println("[DBConnection.close] Error occured while closing the databse connection: " + e.toString());
    }
  }
}
