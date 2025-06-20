package org.vault;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class Record {
  String id, url, username, password, iv;
  long timestamp;
  Long deletedAt;

  public Record loadRecord(Connection conn, String dbAlias, String id) throws SQLException {
    Record r = new Record();

    r.id = id;

    // entry
    try (ResultSet re = conn.createStatement().executeQuery(
        "SELECT url,username,password,iv,timestamp FROM " + dbAlias + ".entries WHERE id='" + id + "'")) {
      if (re.next()) {
        r.url = re.getString("url");
        r.username = re.getString("username");
        r.password = re.getString("password");
        r.iv = re.getString("iv");
        r.timestamp = re.getLong("timestamp");
      }
    }
    // deleted
    try (ResultSet rd = conn.createStatement().executeQuery(
        "SELECT deleted_at FROM " + dbAlias + ".deleted WHERE id='" + id + "'")) {
      if (rd.next()) {
        r.deletedAt = rd.getLong("deleted_at");
      }
    }
    return r;
  }

  public void upsertEntry(Connection conn, Record rec) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement(
        "INSERT OR REPLACE INTO entries(id,url,username,password,iv,timestamp) VALUES(?,?,?,?,?,?)")) {
      ps.setString(1, rec.id);
      ps.setString(2, rec.url);
      ps.setString(3, rec.username);
      ps.setString(4, rec.password);
      ps.setString(5, rec.iv);
      ps.setLong(6, rec.timestamp);
      ps.executeUpdate();
    }
  }

  public void upsertDeleted(Connection conn, String id, long ts) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement(
        "INSERT OR REPLACE INTO deleted(id,deleted_at) VALUES(?,?)")) {
      ps.setString(1, id);
      ps.setLong(2, ts);
      ps.executeUpdate();
    }
  }

  public void deleteEntryRow(Connection conn, String id) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement(
        "DELETE FROM entries WHERE id = ?")) {
      ps.setString(1, id);
      ps.executeUpdate();
    }
  }

  public void deleteDeletedRow(Connection conn, String id) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement(
        "DELETE FROM deleted WHERE id = ?")) {
      ps.setString(1, id);
      ps.executeUpdate();
    }
  }
}
