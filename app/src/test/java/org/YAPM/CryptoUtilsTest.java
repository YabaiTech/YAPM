package org.YAPM;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CryptoUtilsTest {
  @Test
  void passwordsMatchAfterGoingThroughEncryptionAndDecryption() {
    String masterPasswd = "abcdefgh12345678";
    String plaintext = "aquickbrownfoxjumpsoveralazydog69420";

    try {
      EncryptedData encrypted = CryptoUtils.encrypt(plaintext, masterPasswd);
      String decrypted = CryptoUtils.decrypt(encrypted, masterPasswd);

      System.out.println(plaintext);
      System.out.println(decrypted);
      assertEquals(plaintext, decrypted, "Plaintext password should be the same before and after encryption.");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
