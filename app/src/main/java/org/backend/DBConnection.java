package org.backend;

import java.sql.*;

class DBConnection {
  public static Connection con;

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

  public void addUser(String username, String email, String hashedPassword, String saltB64, String dbFilePath,
      long lastUnixTimestamp)
      throws SQLException {
    String query = "INSERT INTO `" + EnvVars.MASTER_USER_TABLE
        + "` (username, email, hashed_password, salt, pwd_db_path, last_logged_in) VALUES (?,?,?,?,?,?)";

    PreparedStatement ps = con.prepareStatement(query);

    ps.setString(1, username);
    ps.setString(2, email);
    ps.setString(3, hashedPassword);
    ps.setString(4, saltB64);
    ps.setString(5, dbFilePath);
    ps.setLong(6, lastUnixTimestamp);

    int opStat = ps.executeUpdate();

    if (opStat == 1) {
      System.out.println("Successfully added the user!");
    } else {
      System.out.println("Failed to add the user!");
    }
  }

  public UserInfo getUserInfo(String username) throws SQLException {
    String query = "SELECT * FROM `" + EnvVars.MASTER_USER_TABLE + "` WHERE `username`=?";

    PreparedStatement ps = con.prepareStatement(query);

    ps.setString(1, String.valueOf(username));
    ResultSet res = ps.executeQuery();

    UserInfo fetchedUser = new UserInfo();

    if (res.next()) {
      fetchedUser.username = res.getString("username");
      fetchedUser.email = res.getString("email");
      fetchedUser.hashedPassword = res.getString("hashed_password");
      fetchedUser.salt = res.getString("salt");
      fetchedUser.passwordDbPath = res.getString("pwd_db_path");
      fetchedUser.lastLoggedInTime = res.getLong("last_logged_in");
    }

    return fetchedUser;
  }

  public boolean updateLastLoginTime(String username, long time) throws SQLException {
    String query = "UPDATE `" + EnvVars.MASTER_USER_TABLE + "` SET `last_logged_in`=? WHERE `username`=?";

    PreparedStatement ps = con.prepareStatement(query);
    ps.setLong(1, time);
    ps.setString(2, username);

    int opStat = ps.executeUpdate();

    if (opStat == 1) {
      return true;
    }
    return false;
  }
}
