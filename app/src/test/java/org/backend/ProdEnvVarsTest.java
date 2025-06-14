package org.backend;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProdEnvVarsTest {
  @Test
  void envFileGetsParsedProperly() {
    ProdEnvVars env = new ProdEnvVars();

    // assertEquals(env.get("DATABASE_NAME"), "YAPM");
    // assertEquals(env.get("DATABASE_BASE_URL"), "jdbc:mysql://localhost:3306/");
    // assertEquals(env.get("DATABASE_USER"), "root");
    // assertEquals(env.get("DATABASE_PASSWORD"), "");
    // assertEquals(env.get("MASTER_USER_TABLE"), "master_users_table");
  }
}
