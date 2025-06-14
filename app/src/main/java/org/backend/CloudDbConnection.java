package org.backend;

import java.sql.*;

public class CloudDbConnection implements AutoCloseable {
  public Connection con;

  public CloudDbConnection() {
    try {
      ProdEnvVars penv = new ProdEnvVars();
      String dbURL = penv.get("DATABASE_BASE_URL") + penv.get("DATABASE_NAME");
      String dbUser = penv.get("DATABASE_USER");
      String dbPwd = penv.get("DATABASE_PASSWORD");

      con = DriverManager.getConnection(dbURL, dbUser, dbPwd);
    } catch (Exception e) {
      System.err.println("[CloudDbConnection.CloudDbConnection] Failed to connect to the cloud DB: ");
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
      System.err
          .println(
              "[CloudDbConnection.close] Error occured while closing the cloud databse connection: " + e.toString());
    }
  }
}
