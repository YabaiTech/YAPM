package org.backend;

class LoginUser {
  private String username;
  private String password;
  private String dbPath;

  LoginUser(String uname, String pwd) {
    this.username = uname;
    this.password = pwd;

  }

  public BackendError login() {
    // try to get the SQLite file
    getDbFilePath();

    return null;
  }

  private String generateHash(String input) {
    // placeholder
    return input;
  }

  private BackendError getDbFilePath() {
    // use `this.username` and the gernerated hash from `this.password` to
    // successfully login and retrieve dbFilePath
    this.dbPath = "";

    return null;
  }
}
