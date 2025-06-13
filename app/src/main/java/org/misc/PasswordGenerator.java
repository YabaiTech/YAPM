package org.misc;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;

public class PasswordGenerator {
  private final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private final String LOWER = "abcdefghijklmnopqrstuvwxyz";
  private final String DIGITS = "0123456789";
  private final static String SPECIAL = "#$%&^`~.,:;\"'\\|/_-<>*+!?[{}()]=@";

  private final SecureRandom random = new SecureRandom();

  private final boolean useUpper;
  private final boolean useLower;
  private final boolean useDigits;
  private final boolean useSpecial;

  public PasswordGenerator(boolean useUpper, boolean useLower, boolean useDigits, boolean useSpecial) {
    int enabled = 0;
    if (useUpper)
      enabled++;
    if (useLower)
      enabled++;
    if (useDigits)
      enabled++;
    if (useSpecial)
      enabled++;
    if (enabled < 1 || enabled > 4) {
      enabled = 1;
      useLower = true;
    }

    this.useUpper = useUpper;
    this.useLower = useLower;
    this.useDigits = useDigits;
    this.useSpecial = useSpecial;
  }

  public boolean usingUpper() {
    return this.useUpper;
  }

  public boolean usingLower() {
    return this.useLower;
  }

  public boolean usingDigits() {
    return this.useDigits;
  }

  public boolean usingSpecial() {
    return this.useSpecial;
  }

  public static String getSpecialChars() {
    return SPECIAL;
  }

  public String generate(int length) {
    ArrayList<Character> passwdChars = new ArrayList<Character>(length);
    StringBuilder pool = new StringBuilder();

    if (this.useUpper) {
      passwdChars.add(randomCharFrom(this.UPPER));
      pool.append(this.UPPER);
    }
    if (this.useLower) {
      passwdChars.add(randomCharFrom(this.LOWER));
      pool.append(this.LOWER);
    }
    if (this.useDigits) {
      passwdChars.add(randomCharFrom(this.DIGITS));
      pool.append(this.DIGITS);
    }
    if (this.useSpecial) {
      passwdChars.add(randomCharFrom(this.SPECIAL));
      pool.append(this.SPECIAL);
    }

    if (length < passwdChars.size()) {
      return null;
    }
    for (int i = passwdChars.size(); i < length; ++i) {
      passwdChars.add(randomCharFrom(pool.toString()));
    }

    Collections.shuffle(passwdChars, random);

    StringBuilder res = new StringBuilder(length);
    for (char c : passwdChars) {
      res.append(c);
    }
    return res.toString();
  }

  private char randomCharFrom(String s) {
    return s.charAt(random.nextInt(s.length()));
  }
}
