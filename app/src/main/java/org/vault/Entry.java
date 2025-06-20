package org.vault;

public class Entry {
  private final String id;
  private final String url;
  private final String username;
  private final String passwd;

  Entry(String id, String url, String username, String passwd) {
    this.id = id;
    this.url = url;
    this.username = username;
    this.passwd = passwd;
  }

  public String getID() {
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
