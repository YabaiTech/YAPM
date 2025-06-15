package org.backend;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

class RegisterUserTest {
  DBConnection localDb = new DBConnection();
  CloudDbConnection cloudDb = new CloudDbConnection();

  int getRandomNum() {
    // generate a random number from 1 to 90,000
    final int MIN = 1;
    final int MAX = 90000;
    Random rand = new Random();
    int randNum = rand.nextInt(MAX - MIN) + MIN;

    return randNum;
  }

  @Test
  void validUsernameGetsAccepted() {
    RegisterUser reg = new RegisterUser(this.localDb, this.cloudDb);
    BackendError response = reg.setUsername("dipta123");

    assertNull(response);
  }

  @Test
  void invalidUsernameGetsAccepted() {
    RegisterUser reg = new RegisterUser(this.localDb, this.cloudDb);
    BackendError response = reg.setUsername("atr-ues");

    assertEquals(BackendError.ErrorTypes.InvalidUserName, response.getErrorType());
  }

  @Test
  void validPasswordGetsAccepted() {
    RegisterUser reg = new RegisterUser(this.localDb, this.cloudDb);
    BackendError response = reg.setPassword("aBc123#!");

    assertNull(response);
  }

  @Test
  void lessThan8CharPasswordsGetRejected() {
    RegisterUser reg = new RegisterUser(this.localDb, this.cloudDb);
    BackendError response = reg.setPassword("aB9!");

    assertEquals(BackendError.ErrorTypes.PasswordNeedsToBeAtleast8Chars, response.getErrorType());
  }

  @Test
  void passwordsWithUnallowedCharsGetRejected() {
    RegisterUser reg = new RegisterUser(this.localDb, this.cloudDb);
    BackendError response = reg.setPassword("🙂aBc123#!");

    BackendError.ErrorTypes expected = BackendError.ErrorTypes.PasswordContainsUnallowedChars;
    assert (response != null);
    assertEquals(expected, response.getErrorType());
  }

  @Test
  void passwordsWithoutLowercaseGetsRejected() {
    RegisterUser reg = new RegisterUser(this.localDb, this.cloudDb);
    BackendError response = reg.setPassword("XYZ123#!");

    assertEquals(BackendError.ErrorTypes.PasswordNeedsAtleast1Lowercase, response.getErrorType());
  }

  @Test
  void passwordsWithoutUppercaseGetsRejected() {
    RegisterUser reg = new RegisterUser(this.localDb, this.cloudDb);
    BackendError response = reg.setPassword("xyz123#!");

    assertEquals(BackendError.ErrorTypes.PasswordNeedsAtleast1Uppercase, response.getErrorType());
  }

  @Test
  void passwordsWithoutNumberGetsRejected() {
    RegisterUser reg = new RegisterUser(this.localDb, this.cloudDb);
    BackendError response = reg.setPassword("xyzAbc#!");

    assertEquals(BackendError.ErrorTypes.PasswordNeedsAtleast1Number, response.getErrorType());
  }

  @Test
  void passwordsWithoutSpecialCharGetsRejected() {
    RegisterUser reg = new RegisterUser(this.localDb, this.cloudDb);
    BackendError response = reg.setPassword("xyzAbc123");

    assertEquals(BackendError.ErrorTypes.PasswordNeedsAtleast1SpecialChar, response.getErrorType());
  }

  @Test
  void addsUserIfEverythingIsValid() {
    RegisterUser reg = new RegisterUser(this.localDb, this.cloudDb);

    // generate a random number from 1 to 90,000
    int randNum = getRandomNum();

    BackendError usrRes = reg.setUsername("anindya" + randNum);
    BackendError emailRes = reg.setEmail("and@gmail.com" + randNum);
    BackendError pwdRes = reg.setPassword("xYZ123#!");
    assertNull(usrRes);
    assertNull(emailRes);
    assertNull(pwdRes);

    BackendError addUserRes = reg.register();
    assertNull(addUserRes);
  }
}
