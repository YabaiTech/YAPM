package org.backend;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

class RegisterUserTest {
  DBConnection db = new DBConnection();

  int getRandomNum() {
    // generate a random number from 1 to 90,000
    final int MIN = 1;
    final int MAX = 90000;
    Random rand = new Random();
    int randNum = rand.nextInt(MAX - MIN) + MIN;

    return randNum;
  }

  @Test
  void properUsernameGetsAccepted() {
    RegisterUser reg = new RegisterUser(this.db);
    BackendError response = reg.setUsername("dipta123");

    assertNull(response);
  }

  @Test
  void properPasswordGetsAccepted() {
    RegisterUser reg = new RegisterUser(this.db);
    BackendError response = reg.setPassword("aBc123#!");

    assertNull(response);
  }

  @Test
  void nonUTF8PasswordsGetRejected() {
    RegisterUser reg = new RegisterUser(this.db);
    BackendError response = reg.setPassword("🙂aBc123#!");

    BackendError.ErrorTypes expected = BackendError.ErrorTypes.PasswordContainsUnallowedChars;
    assert (response != null);
    assertEquals(expected, response.getErrorType());
  }

  @Test
  void passwordsWithoutLowercaseGetsRejected() {
    RegisterUser reg = new RegisterUser(this.db);
    BackendError response = reg.setPassword("XYZ123#!");

    BackendError.ErrorTypes expected = BackendError.ErrorTypes.PasswordNeedsAtleast1Lowercase;
    assert (response != null);
    assertEquals(expected, response.getErrorType());
  }

  @Test
  void addsUserIfEverythingIsValid() {
    RegisterUser reg = new RegisterUser(this.db);

    // generate a random number from 1 to 90,000
    int randNum = getRandomNum();

    BackendError usrRes = reg.setUsername("dipta" + randNum);
    BackendError emailRes = reg.setEmail("dipta@gmail.com" + randNum);
    BackendError pwdRes = reg.setPassword("xYZ123#!" + randNum);
    assertNull(usrRes);
    assertNull(emailRes);
    assertNull(pwdRes);

    BackendError addUserRes = reg.register();
    assertNull(addUserRes);
  }
}
