package org.backend;

import java.sql.*;

class DBConnection {
  public Connection con;

  DBConnection() {
    try {
      con = DriverManager.getConnection(EnvVars.DATABASE_URL, EnvVars.DATABASE_USER, EnvVars.DATABASE_PASSWORD);

      System.out.println("Connection established successfully!");
    } catch (Exception e) {
      System.err.println("[DBConnection.DBConnection] Failed to connect to DB: ");
      e.printStackTrace();

      System.exit(1);
    }
  }
}
