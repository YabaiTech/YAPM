package org.backend;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

class DBConnectionTest {
  int getRandomNum() {
    // generate a random number from 1 to 90,000
    final int MIN = 1;
    final int MAX = 90000;
    Random rand = new Random();
    int randNum = rand.nextInt(MAX - MIN) + MIN;

    return randNum;
  }

  @Test
  void establishesDbConnectionProperly() {
    DBConnection db = new DBConnection();

    assert (db != null);
  }

  @Test
  void addUserToDb() {
    DBConnection db = new DBConnection();

    // generate a random number from 1 to 90,000
    int randNum = getRandomNum();

    assert (db != null);

    try {
      db.addUser("ninja" + randNum, "ninja@gmail.com" + randNum, "meh" + randNum, "/home/ninja" + randNum,
          System.currentTimeMillis());
    } catch (Exception e) {
      throw new UnknownError("Failed to add user to DB: " + e.toString());
    }
  }
}
