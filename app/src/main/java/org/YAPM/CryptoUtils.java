package org.YAPM;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

public class CryptoUtils {
  public static EncryptedData encrypt(String plaintext, String passwd) throws Exception {
    byte[] salt = generateRandomBytes(VaultConst.SALT_LENGTH);
    SecretKeySpec key = deriveKeyFromPasswd(passwd, salt);

    Cipher cipher = Cipher.getInstance(VaultConst.ENCRYPTION_ALGO);
    byte[] iv = generateRandomBytes(VaultConst.IV_LENGTH);
    cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

    byte[] encrypted = cipher.doFinal(plaintext.getBytes());
    return new EncryptedData(Base64.getEncoder().encodeToString(encrypted), Base64.getEncoder().encodeToString(iv),
        Base64.getEncoder().encodeToString(salt));
  }

  public static EncryptedData encrypt(String plaintext, String passwd, byte[] salt) throws Exception {
    SecretKeySpec key = deriveKeyFromPasswd(passwd, salt);

    Cipher cipher = Cipher.getInstance(VaultConst.ENCRYPTION_ALGO);
    byte[] iv = generateRandomBytes(VaultConst.IV_LENGTH);
    cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

    byte[] encrypted = cipher.doFinal(plaintext.getBytes());
    return new EncryptedData(Base64.getEncoder().encodeToString(encrypted), Base64.getEncoder().encodeToString(iv),
        Base64.getEncoder().encodeToString(salt));
  }

  public static String decrypt(EncryptedData data, String passwd) throws Exception {
    byte[] cipherText = Base64.getDecoder().decode(data.getCipherText());
    byte[] iv = Base64.getDecoder().decode(data.getIV());
    byte[] salt = Base64.getDecoder().decode(data.getSalt());

    SecretKeySpec key = deriveKeyFromPasswd(passwd, salt);
    Cipher cipher = Cipher.getInstance(VaultConst.ENCRYPTION_ALGO);
    cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

    byte[] decrypted = cipher.doFinal(cipherText);
    return new String(decrypted, StandardCharsets.UTF_8);
  }

  public static SecretKeySpec deriveKeyFromPasswd(String passwd, byte[] salt) throws Exception {
    SecretKeyFactory factory = SecretKeyFactory.getInstance(VaultConst.KEY_DERIVATION_FUNCTION);
    KeySpec spec = new PBEKeySpec(passwd.toCharArray(), salt, VaultConst.ITERATIONS, VaultConst.KEY_LENGTH);
    SecretKey res = factory.generateSecret(spec);

    return new SecretKeySpec(res.getEncoded(), "AES");
  }

  public static byte[] generateRandomBytes(int length) {
    byte[] res = new byte[length];
    new SecureRandom().nextBytes(res);

    return res;
  }
}
