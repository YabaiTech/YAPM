package org.misc;

public class PasswordEntropyCalculator {
  private static final int LOWER_POOL = 26;
  private static final int UPPER_POOL = 26;
  private static final int DIGIT_POOL = 10;
  private static final int SPECIAL_POOL = 32;

  public static double calculateEntropy(String passwd) {
    int poolSize = 0;
    boolean hasLower = false, hasUpper = false, hasDigit = false, hasSpecial = false;

    for (char c : passwd.toCharArray()) {
      if (Character.isLowerCase(c))
        hasLower = true;
      else if (Character.isUpperCase(c))
        hasUpper = true;
      else if (Character.isDigit(c))
        hasDigit = true;
      else
        hasSpecial = true;
    }

    if (hasLower)
      poolSize += LOWER_POOL;
    if (hasUpper)
      poolSize += UPPER_POOL;
    if (hasDigit)
      poolSize += DIGIT_POOL;
    if (hasSpecial)
      poolSize += SPECIAL_POOL;
    if (poolSize == 0) {
      return 0;
    }

    return passwd.length() * (Math.log(poolSize) / Math.log(2));
  }

  public static PasswordStrength getPasswdStrength(String passwd) {
    double entropy = calculateEntropy(passwd);
    if (entropy < 28)
      return PasswordStrength.POOR;
    else if (entropy < 36)
      return PasswordStrength.WEAK;
    else if (entropy < 60)
      return PasswordStrength.REASONABLE;
    else if (entropy < 128)
      return PasswordStrength.GOOD;
    else
      return PasswordStrength.GREAT;
  }
}
