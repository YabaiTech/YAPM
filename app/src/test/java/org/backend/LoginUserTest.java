package org.backend;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoginUserTest {
  DBConnection localDb = new DBConnection();
  CloudDbConnection cloudDb = new CloudDbConnection();

  String uname;
  String email;
  String plaintextPwd;

  int getRandomNum() {
    // generate a random number from 1 to 90,000
    final int MIN = 1;
    final int MAX = 90000;
    Random rand = new Random();
    int randNum = rand.nextInt(MAX - MIN) + MIN;

    return randNum;
  }

  @BeforeAll
  void setup() {
    RegisterUser reg = new RegisterUser(this.localDb, this.cloudDb);
    int randNum = getRandomNum();

    this.uname = "syncTest" + randNum;
    this.email = "syncTest@gmail.com" + randNum;
    this.plaintextPwd = "xYZ123#!";

    reg.setUsername(this.uname);
    reg.setEmail(this.email);
    reg.setPassword(this.plaintextPwd);

    BackendError response = reg.register();
    if (response != null) {
      System.err.println("[LoginUserTest.setup] Failed to register for syncing test: " + response.getErrorType());

      throw new IllegalStateException("[LoginUserTest.setup] Failed to seed a user to DB for login testing");
    }

  }

  @Test
  void validCredentialLogsInUsingUsername() {
    LoginUser auth = new LoginUser(this.localDb, this.cloudDb, this.uname, this.plaintextPwd);

    assertDoesNotThrow(() -> {
      BackendError resp = auth.login();
      assertEquals(null, resp);
    });
  }

  @Test
  void validCredentialLogsInUsingEmail() {
    LoginUser auth = new LoginUser(this.localDb, this.cloudDb, this.email, this.plaintextPwd);

    assertDoesNotThrow(() -> {
      BackendError resp = auth.login();
      assertEquals(null, resp);
    });
  }

  @AfterAll
  void remove() {
    DBOperations localOps = new DBOperations(this.localDb);
    DBOperations cloudOps = new DBOperations(this.cloudDb);

    try {
      localOps.deleteUser(this.uname);
      cloudOps.deleteUser(this.uname);
    } catch (Exception e) {
      System.err.println("[LoginUserTest.remove] (Non-fatal) Failed to remove test user from DBs: " + e);
    }
  }
}
