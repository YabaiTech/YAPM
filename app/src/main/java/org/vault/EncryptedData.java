package org.vault;

public class EncryptedData {
  private final String cipherText;
  private final String iv;
  private final String salt;

  public EncryptedData(String cipherText, String iv, String salt) {
    this.cipherText = cipherText;
    this.iv = iv;
    this.salt = salt;
  }

  public String getCipherText() {
    return this.cipherText;
  }

  public String getIV() {
    return this.iv;
  }

  public String getSalt() {
    return this.salt;
  }
}
