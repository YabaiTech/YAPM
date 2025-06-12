package org.backend;

import java.sql.*;

public class DBConnection {
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
}
