package org.misc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordEntropyCalculatorTest {
  private static final double DELTA = 0.0001;

  @Test
  void testEmptyPassword() {
    String pwd = "";
    double entropy = PasswordEntropyCalculator.calculateEntropy(pwd);
    assertEquals(0.0, entropy, DELTA, "Empty password should have 0 entropy");
  }

  @Test
  void testLowercaseOnly() {
    String pwd = "abcdef";
    double expected = 6 * (Math.log(26) / Math.log(2));
    double entropy = PasswordEntropyCalculator.calculateEntropy(pwd);
    assertEquals(expected, entropy, DELTA, "Entropy for lowercase-only mismatch");
  }

  @Test
  void testUppercaseOnly() {
    String pwd = "ABCDEF";
    double expected = 6 * (Math.log(26) / Math.log(2));
    double entropy = PasswordEntropyCalculator.calculateEntropy(pwd);
    assertEquals(expected, entropy, DELTA, "Entropy for uppercase-only mismatch");
  }

  @Test
  void testDigitsOnly() {
    String pwd = "012345";
    double expected = 6 * (Math.log(10) / Math.log(2));
    double entropy = PasswordEntropyCalculator.calculateEntropy(pwd);
    assertEquals(expected, entropy, DELTA, "Entropy for digits-only mismatch");
  }

  @Test
  void testSymbolsOnly() {
    String pwd = "!@#$%^";
    double expected = 6 * (Math.log(32) / Math.log(2));
    double entropy = PasswordEntropyCalculator.calculateEntropy(pwd);
    assertEquals(expected, entropy, DELTA, "Entropy for symbols-only mismatch");
  }

  @Test
  void testLowerAndUpper() {
    String pwd = "aBcDeF";
    double expected = 6 * (Math.log(52) / Math.log(2));
    double entropy = PasswordEntropyCalculator.calculateEntropy(pwd);
    assertEquals(expected, entropy, DELTA, "Entropy for mixed case mismatch");
  }

  @Test
  void testLettersDigitsSymbols() {
    String pwd = "aB3$dE7!";
    double expected = 8 * (Math.log(94) / Math.log(2));
    double entropy = PasswordEntropyCalculator.calculateEntropy(pwd);
    assertEquals(expected, entropy, DELTA, "Entropy for full charset mismatch");
  }

  @Test
  void testShortVsLongMixed() {
    String shortPwd = "aB3$";
    String longPwd = "aB3$aB3$";
    double entropyShort = PasswordEntropyCalculator.calculateEntropy(shortPwd);
    double entropyLong = PasswordEntropyCalculator.calculateEntropy(longPwd);
    assertTrue(entropyLong > entropyShort,
        "Longer password should have strictly greater entropy");
  }
}
