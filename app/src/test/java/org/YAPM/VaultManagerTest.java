package org.YAPM;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;

import javax.crypto.BadPaddingException;

class VaultManagerTest {
  private static final String MASTER_PASSWD = "testMaster123";
  private static final String WRONG_PASSWD = "testMaster121";

  @TempDir
  Path tmpDir;
  private String dbPath;

  @BeforeEach
  public void setUp() {
    dbPath = tmpDir.resolve("testVault.db").toString();
  }

  @Test
  void testCreateAndOpenEmptyVault() throws Exception {
    VaultManager.createVault(dbPath, MASTER_PASSWD);
    List<Entry> entries = VaultManager.openVault(dbPath, MASTER_PASSWD);

    assertNotNull(entries, "Entries shouldn't be null.");
    assertTrue(entries.isEmpty(), "However, it should be empty.");
  }

  @Test
  void testOpenVaultWithWrongPasswordThrows() throws Exception {
    VaultManager.createVault(dbPath, MASTER_PASSWD);

    assertThrows(BadPaddingException.class, () -> {
      VaultManager.openVault(dbPath, WRONG_PASSWD);
    }, "Opening vault with incorrect password should throw BadPaddingException (or SecurityException if it passes that).");
  }

  @Test
  void testCreateVaultWithEmptyPathThrows() {
    assertThrows(IllegalArgumentException.class, () -> {
      VaultManager.createVault("", MASTER_PASSWD);
    }, "Opening vault with empty path should throw IllegalArgument Exception.");
  }

  @Test
  void testOpenVaultWithEmptyPasswordThrows() {
    assertDoesNotThrow(() -> VaultManager.createVault(dbPath, MASTER_PASSWD),
        "Creating vault with proper parameters should not throw any error.");

    assertThrows(IllegalArgumentException.class, () -> {
      VaultManager.openVault(dbPath, "");
    }, "However, opening vault with no password should throw.");
  }
}
