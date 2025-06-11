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

  @Test
  public void testAddEntryAndOpen() throws Exception {
    VaultManager.createVault(dbPath, MASTER_PASSWD);

    String url = "https://example.com";
    String username = "alice";
    String passwd = "s3cr3t";
    VaultManager.addEntry(dbPath, MASTER_PASSWD, url, username, passwd);

    List<Entry> entries = VaultManager.openVault(dbPath, MASTER_PASSWD);
    assertEquals(1, entries.size(), "Should have one entry.");

    Entry e = entries.get(0);
    assertEquals(url, e.getURL());
    assertEquals(username, e.getUsername());
    assertEquals(passwd, e.getPasswd());
  }

  @Test
  public void testDeleteEntry() throws Exception {
    VaultManager.createVault(dbPath, MASTER_PASSWD);

    VaultManager.addEntry(dbPath, MASTER_PASSWD, "url1", "user1", "pwd1");
    VaultManager.addEntry(dbPath, MASTER_PASSWD, "url2", "user2", "pwd2");

    List<Entry> entries = VaultManager.openVault(dbPath, MASTER_PASSWD);
    assertEquals(2, entries.size());

    VaultManager.deleteEntry(dbPath, MASTER_PASSWD, 1);
    List<Entry> afterDelete = VaultManager.openVault(dbPath, MASTER_PASSWD);
    assertEquals(1, afterDelete.size(), "One entry should remain after deletion.");
    assertEquals("url2", afterDelete.get(0).getURL());
  }
}
