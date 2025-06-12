package org.YAPM;

public class VaultConst {
  public static final String ENCRYPTION_ALGO = "AES/CBC/PKCS5Padding";
  public static final String KEY_DERIVATION_FUNCTION = "PBKDF2WithHmacSHA256";
  public static final int ITERATIONS = 65536;
  public static final int KEY_LENGTH = 256;
  public static final int IV_LENGTH = 16;
  public static final int SALT_LENGTH = 16;
}
