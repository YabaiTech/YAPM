package org.misc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PasswordGeneratorTest {
  private PasswordGenerator allSetsGen;
  private PasswordGenerator onlyLowerGen;
  private PasswordGenerator onlySpecialGen;
  private final String SPECIAL = "#$%&^`~.,:;\"'\\|/_-<>*+!?[{}()]=@";

  @BeforeEach
  public void setUp() {
    allSetsGen = new PasswordGenerator(true, true, true, true);
    onlyLowerGen = new PasswordGenerator(false, true, false, false);
    onlySpecialGen = new PasswordGenerator(false, false, false, true);
  }

  // @Test
  // public void testIllegalToggleCountTooFew() {
  // // setting everything to false makes the generator only use lowercase
  // alphabets
  // PasswordGenerator pg = new PasswordGenerator(false, false, false, false);
  // assertEquals(32, SPECIAL.length());
  //
  // assertFalse(pg.usingUpper());
  // assertTrue(pg.usingLower());
  // assertFalse(pg.usingDigits());
  // assertFalse(pg.usingSpecial());
  // }

  @Test
  public void testLengthLessThanSets() {
    // when length < number, null is returned
    PasswordGenerator pg = new PasswordGenerator(true, false, true, false);

    assertNull(pg.generate(1));
  }

  @RepeatedTest(5)
  public void testGenerateAllSetsContainsEachType() {
    String pwd = allSetsGen.generate(12);
    StringBuilder cls = new StringBuilder("^[");

    for (char c : SPECIAL.toCharArray()) {
      if (c == '\\' || c == '[' || c == ']' || c == '-' || c == '^') {
        cls.append('\\');
      }
      cls.append(c);
    }

    cls.append("]+$");
    String onlySpecialRegex = cls.toString();

    assertTrue(pwd.matches(".*[A-Z].*"), "Should contain at least one uppercase.");
    assertTrue(pwd.matches(".*[a-z].*"), "Should contain at least one lowercase.");
    assertTrue(pwd.matches(".*\\d.*"), "Should contain at least one digit.");
    assertTrue(pwd.matches(".*[" + onlySpecialRegex + "].*"),
        "Should contain at least one special.");
    assertEquals(12, pwd.length());
  }

  @RepeatedTest(5)
  public void testOnlyLowercase() {
    String pwd = onlyLowerGen.generate(8);
    assertTrue(pwd.matches("^[a-z]+$"), "Should contain only lowercase letters.");
    assertEquals(8, pwd.length());
  }

  @RepeatedTest(5)
  public void testOnlySpecial() {
    StringBuilder cls = new StringBuilder("^[");

    for (char c : SPECIAL.toCharArray()) {
      if (c == '\\' || c == '[' || c == ']' || c == '-' || c == '^') {
        cls.append('\\');
      }
      cls.append(c);
    }

    cls.append("]+$");
    String onlySpecialRegex = cls.toString();
    String pwd = onlySpecialGen.generate(6);

    assertTrue(pwd.matches(onlySpecialRegex), "Should contain only special characters");
    assertEquals(6, pwd.length());
  }

  @Test
  public void testRandomness() {
    String p1 = allSetsGen.generate(16);
    String p2 = allSetsGen.generate(16);
    assertNotEquals(p1, p2, "Two generated passwords should differ");
  }
}
