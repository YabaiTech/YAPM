package org.backend;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SetupDBTest {
  @Test
  void createsDatabaseAndTableIfNotExisting() {
    // it shouldn't throw under any circumstances
    boolean isSuccess = SetupDB.init();
    assertEquals(true, isSuccess);
  }
}
