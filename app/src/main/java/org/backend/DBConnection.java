package org.backend;

import java.sql.*;

public class DBConnection implements AutoCloseable {
  public Connection con;

  public DBConnection() {
    try {
      con = DriverManager.getConnection(EnvVars.DATABASE_URL, EnvVars.DATABASE_USER, EnvVars.DATABASE_PASSWORD);
    } catch (Exception e) {
      System.err.println("[DBConnection.DBConnection] Failed to connect to DB: ");
      e.printStackTrace();

      System.exit(1);
    }
  }

  @Override
  public void close() {
    try {
      con.close();
    } catch (Exception e) {
      // If it errors, just let it know using a log
      System.err.println("[DBConnection.close] Error occured while closing the databse connection: " + e);
    }
  }
}
