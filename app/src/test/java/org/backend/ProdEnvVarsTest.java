package org.backend;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProdEnvVarsTest {
  @Test
  void envFileGetsParsedProperly() {
    assertDoesNotThrow(() -> {
      ProdEnvVars env = new ProdEnvVars();

      assert (!env.get("DATABASE_NAME").isEmpty());
      assert (!env.get("DATABASE_BASE_URL").isEmpty());
      assert (!env.get("DATABASE_USER").isEmpty());
      assert (!env.get("MASTER_USER_TABLE").isEmpty());
    });
  }
}
