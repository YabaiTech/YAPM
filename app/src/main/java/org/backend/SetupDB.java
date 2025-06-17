package org.backend;

import java.sql.*;

public class SetupDB implements AutoCloseable {
  private static Connection localCon;
  private static Connection cloudCon;

  private static ProdEnvVars prodEnv = new ProdEnvVars();

  public static boolean init() {
    try {
      localCon = DriverManager.getConnection(EnvVars.DATABASE_BASE_URL, EnvVars.DATABASE_USER,
          EnvVars.DATABASE_PASSWORD);

      String dbBaseURL = prodEnv.get("DATABASE_BASE_URL");
      String dbUser = prodEnv.get("DATABASE_USER");
      String dbPwd = prodEnv.get("DATABASE_PASSWORD");

      cloudCon = DriverManager.getConnection(dbBaseURL, dbUser, dbPwd);

      createDatabaseIfNotExisting();
      createTableIfNotExisting();
    } catch (Exception e) {
      System.err.println("[SetupDB.SetupDB] Failed to connect to DB: ");
      e.printStackTrace();

      System.exit(1);
    }

    return true;
  }

  private static void createDatabaseIfNotExisting() {
    try (Statement stmtLocal = localCon.createStatement(); Statement stmtCloud = cloudCon.createStatement()) {
      String localDbCreationSQL = "CREATE DATABASE IF NOT EXISTS " + EnvVars.DATABASE_NAME;
      String cloudDbCreationSQL = "CREATE DATABASE IF NOT EXISTS " + prodEnv.get("DATABASE_NAME");
      stmtLocal.execute(localDbCreationSQL);
      stmtCloud.execute(cloudDbCreationSQL);
    } catch (Exception e) {
      System.err.println("[SetupDB.createMasterUserTable] Failed to create the YAPM database: ");
      e.printStackTrace();

      System.exit(1);
    }
  }

  private static void createTableIfNotExisting() {
    try {
      localCon = DriverManager.getConnection(EnvVars.DATABASE_URL, EnvVars.DATABASE_USER, EnvVars.DATABASE_PASSWORD);

      String dbBaseURL = prodEnv.get("DATABASE_BASE_URL");
      String dbName = prodEnv.get("DATABASE_NAME");
      String dbUser = prodEnv.get("DATABASE_USER");
      String dbPwd = prodEnv.get("DATABASE_PASSWORD");
      cloudCon = DriverManager.getConnection(dbBaseURL + dbName, dbUser, dbPwd);

      Statement stmtLocal = localCon.createStatement();
      Statement stmtCloud = cloudCon.createStatement();

      stmtLocal.execute(EnvVars.getTableCreationSQL(EnvVars.MASTER_USER_TABLE));
      stmtCloud.execute(EnvVars.getTableCreationSQL(prodEnv.get("MASTER_USER_TABLE")));
    } catch (Exception e) {
      System.err.println("[SetupDB.createMasterUserTable] Failed to create master user table: " + e);

      System.exit(1);
    }
  }

  @Override
  public void close() {
    try {
      localCon.close();
      cloudCon.close();
    } catch (Exception e) {
      // If it errors, just let it know using a log
      System.err.println("[DBConnection.close] Error occured while closing the databse connection: " + e.toString());
    }
  }
}
