package org.vault;

public class Entry {
  private final int id;
  private final String url;
  private final String username;
  private final String passwd;

  Entry(int id, String url, String username, String passwd) {
    this.id = id;
    this.url = url;
    this.username = username;
    this.passwd = passwd;
  }

  public int getID() {
    return this.id;
  }

  public String getURL() {
    return this.url;
  }

  public String getUsername() {
    return this.username;
  }

  public String getPasswd() {
    return this.passwd;
  }
}
