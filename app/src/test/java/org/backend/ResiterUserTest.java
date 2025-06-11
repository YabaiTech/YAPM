package org.backend;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AppTest {
  @Test
  void properUsernameGetsAccepted() {
    RegisterUser reg = new RegisterUser();
    BackendError response = reg.setUsername("dipta123");

    assertNull(response);
  }

  @Test
  void properPasswordGetsAccepted() {
    RegisterUser reg = new RegisterUser();
    BackendError response = reg.setPassword("aBc123#!");

    assertNull(response);
  }
}
