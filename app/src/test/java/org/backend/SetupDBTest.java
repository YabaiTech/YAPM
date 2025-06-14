package org.backend;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SetupDBTest {
  @Test
  void createsDatabaseAndTableIfNotExisting() {
    // it shouldn't throw if they exists or not
    assertDoesNotThrow(() -> {
      SetupDB.init();
    });
  }
}
