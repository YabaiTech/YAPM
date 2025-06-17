package org.backend;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DBConnectionTest {
  @BeforeAll
  void setup() {
    SetupDB.init();
  }

  int getRandomNum() {
    // generate a random number from 1 to 90,000
    final int MIN = 1;
    final int MAX = 90000;
    Random rand = new Random();

    return rand.nextInt(MAX - MIN) + MIN;
  }

  @Test
  void establishesDbConnectionProperly() {
    assertDoesNotThrow(() -> {
      try (DBConnection db = new DBConnection()) {
        assertNotNull(db);
      }
    });
  }

  @Test
  void addUserToDb() {
    DBConnection db = new DBConnection();
    DBOperations ops = new DBOperations(db);

    // generate a random number from 1 to 90,000
    int randNum = getRandomNum();

    try {
      BackendError resp = ops.addUser("test" + randNum, "test@gmail.com" + randNum, "test" + randNum, "my_test_salt",
          "/home/ninja" + randNum,
          System.currentTimeMillis());

      if (resp != null) {
        throw new IllegalStateException(
            "Failed to add user to the database. Response error context: " + resp.getContext());
      }

    } catch (Exception e) {
      throw new UnknownError("Failed to add user to DB: " + e);
    }
  }
}
