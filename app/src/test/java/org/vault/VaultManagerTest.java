package org.vault;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.ArrayList;

class VaultManagerTest {
  private static final String MASTER_PASSWD = "testMaster123";
  private static final String WRONG_PASSWD = "testMaster121";
  private VaultManager vm;

  @TempDir
  Path tmpDir;
  private String dbPath;

  @BeforeEach
  public void setUp() throws Exception {
    dbPath = tmpDir.resolve("testVault.db").toString();
    vm = new VaultManager(dbPath, MASTER_PASSWD);
    assertEquals(VaultStatus.DBConnectionSuccess, vm.connectToDB(), "Should connect successfully");
    assertEquals(VaultStatus.DBCreateVaultSuccess, vm.createVault(), "Vault creation should succeed");
  }

  @AfterEach
  public void cleanup() throws Exception {
    assertEquals(VaultStatus.DBCloseSuccess, vm.closeDB(), "Should close DB cleanly");
  }

  @Test
  void testCreateAndOpenEmptyVault() throws Exception {
    ArrayList<Entry> entries = new ArrayList<Entry>();
    assertEquals(VaultStatus.DBOpenVaultSuccess, vm.openVault(entries));
    assertTrue(entries.isEmpty(), "Vault should be empty upon creation.");
  }

  @Test
  public void testAddEntryAndRetrieve() throws Exception {
    assertEquals(VaultStatus.DBAddEntrySuccess, vm.addEntry("http://example.com", "user1", "pass1"));

    ArrayList<Entry> entries = new ArrayList<Entry>();
    assertEquals(VaultStatus.DBOpenVaultSuccess, vm.openVault(entries));
    assertEquals(1, entries.size());

    Entry e = entries.get(0);
    assertEquals("http://example.com", e.getURL());
    assertEquals("user1", e.getUsername());
    assertEquals("pass1", e.getPasswd());
  }

  @Test
  public void testDeleteEntrySuccess() throws Exception {
    vm.addEntry("url", "user", "pwd");
    vm.addEntry("url1", "user1", "pwd1");

    ArrayList<Entry> entries = new ArrayList<Entry>();

    vm.openVault(entries);
    int idToDelete = 2;
    assertEquals(VaultStatus.DBDeleteEntrySuccess, vm.deleteEntry(idToDelete));

    entries.clear();
    vm.openVault(entries);
    Entry e = entries.get(0);
    assertTrue(!entries.isEmpty(), "Vault should not be empty after deletion");
    assertEquals("url", e.getURL());
    assertEquals("user", e.getUsername());
    assertEquals("pwd", e.getPasswd());
  }

  @Test
  public void testDeleteEntryInvalidID() throws Exception {
    assertEquals(VaultStatus.DBDeleteEntryFailureInvalidID, vm.deleteEntry(999));
  }

  @Test
  public void testWrongMasterPasswd() throws Exception {
    vm.closeDB();

    VaultManager vmWrong = new VaultManager(dbPath, WRONG_PASSWD);
    vmWrong.connectToDB();

    ArrayList<Entry> entries = new ArrayList<Entry>();
    assertEquals(VaultStatus.DBConnectionSuccess, vmWrong.connectToDB(),
        "Should connect successfully even with wrong password.");
    ;
    assertEquals(VaultStatus.DBWrongMasterPasswd, vmWrong.openVault(entries),
        "Opening vault with wrong master password should return DBWrongPassword.");
    assertEquals(VaultStatus.DBCloseSuccess, vmWrong.closeDB(), "Should close DB cleanly");
  }

  @Test
  public void testAddEmptyParameter() throws Exception {
    assertEquals(VaultStatus.DBAddEntryFailureEmptyParameter, vm.addEntry("", "user", "pwd"));
    assertEquals(VaultStatus.DBAddEntryFailureEmptyParameter, vm.addEntry("url", "", "pwd"));
    assertEquals(VaultStatus.DBAddEntryFailureEmptyParameter, vm.addEntry("url", "user", ""));
  }

  @Test
  public void testEditEntrySuccess() throws Exception {
    assertEquals(VaultStatus.DBAddEntrySuccess, vm.addEntry("siteA", "alice", "passA"));

    VaultStatus status = vm.editEntry(1, "siteB", "bob", "passB");
    assertEquals(VaultStatus.DBEditEntrySuccess, status, "Editing existing entry should succeed");

    ArrayList<Entry> entries = new ArrayList<Entry>();
    assertEquals(VaultStatus.DBOpenVaultSuccess, vm.openVault(entries));
    assertEquals(1, entries.size());

    Entry e = entries.get(0);
    assertEquals("siteB", e.getURL());
    assertEquals("bob", e.getUsername());
    assertEquals("passB", e.getPasswd());
  }

  @Test
  public void testEditEntryEmptyParameter() throws Exception {
    vm.addEntry("url", "user", "pwd");

    assertEquals(VaultStatus.DBEditEntryFailureEmptyParameter,
        vm.editEntry(1, "", "user2", "pwd2"));
    assertEquals(VaultStatus.DBEditEntryFailureEmptyParameter,
        vm.editEntry(1, "url2", "", "pwd2"));
    assertEquals(VaultStatus.DBEditEntryFailureEmptyParameter,
        vm.editEntry(1, "url2", "user2", ""));
  }

  @Test
  public void testEditEntryInvalidID() throws Exception {
    assertEquals(VaultStatus.DBEditEntryFailureInvalidID,
        vm.editEntry(1, "url", "user", "pwd"));
  }

  @Test
  public void testEditEntryWrongMasterPasswd() throws Exception {
    vm.addEntry("origUrl", "origUser", "origPwd");
    vm.closeDB();

    VaultManager vmWrong = new VaultManager(dbPath, WRONG_PASSWD);
    assertEquals(VaultStatus.DBConnectionSuccess, vmWrong.connectToDB());

    VaultStatus status = vmWrong.editEntry(1, "newUrl", "newUser", "newPwd");
    assertEquals(VaultStatus.DBWrongMasterPasswd, status,
        "Editing with wrong master password should return DBWrongMasterPasswd");
    assertEquals(VaultStatus.DBCloseSuccess, vmWrong.closeDB());
  }
}
