package org.backend;

import org.vault.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Random;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoginUserTest {
  DBConnection localDb = new DBConnection();
  CloudDbConnection cloudDb = new CloudDbConnection();

  UserInfo mockUser;
  ArrayList<UserInfo> createdMockUsers = new ArrayList<UserInfo>();
  String commonPlaintextPassword = "genUser123#!";
  String hashSaltBase64;
  String hashedPassword;

  int getRandomNum() {
    // generate a random number from 1 to 90,000
    final int MIN = 1;
    final int MAX = 90000;
    Random rand = new Random();
    int randNum = rand.nextInt(MAX - MIN) + MIN;

    return randNum;
  }

  void createLocalDbFile(String dbName) {
    try (VaultManager vm = new VaultManager(FileHandler.getFullPath(dbName), this.commonPlaintextPassword)) {
      VaultStatus resp = vm.connectToDB();
      if (resp != VaultStatus.DBConnectionSuccess) {
        throw new IllegalStateException(
            "[LoginUserTest.createLocalDb] Failed to connect to vault for mock user: " + resp);
      }

      resp = vm.createVault();
      if (resp != VaultStatus.DBCreateVaultSuccess) {
        throw new IllegalStateException("[LoginUserTest.createLocalDb] Failed to create vault for mock user: " + resp);
      }
    }
  }

  void generateUser() {
    int randNum = getRandomNum();

    // allocate a new user
    mockUser = new UserInfo();

    mockUser.username = "genUser" + randNum;
    mockUser.email = "genUser@gmail.com" + randNum;
    mockUser.passwordDbName = "genUser" + randNum + ".db";
    mockUser.lastLoggedInTime = System.currentTimeMillis();

    // For salt
    SecureRandom random = new SecureRandom();
    byte[] salt = new byte[16];
    random.nextBytes(salt);

    // For the hash (+salt)
    KeySpec spec = new PBEKeySpec(this.commonPlaintextPassword.toCharArray(), salt, 65536, 128);
    SecretKeyFactory factory;
    byte[] hash;

    try {
      factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      hash = factory.generateSecret(spec).getEncoded();

      mockUser.salt = Base64.getEncoder().encodeToString(salt);
      mockUser.hashedPassword = Base64.getEncoder().encodeToString(hash);
    } catch (Exception e) {
      System.err.println(
          "[RegisterUser] Either the PBKDF2WithHmacSHA1 hashing algorithm is not available or the provided PBEKeySpec is wrong: "
              + e);
      System.exit(1);
    }

    this.createdMockUsers.add(this.mockUser);
  }

  BackendError addToLocalDb(UserInfo usr) {
    try {
      DBOperations localOps = new DBOperations(this.localDb);

      BackendError resp = localOps.addUser(usr.username, usr.email, usr.hashedPassword, usr.salt,
          usr.passwordDbName, usr.lastLoggedInTime);
      if (resp != null) {
        return resp;
      }
    } catch (Exception e) {
      return new BackendError(BackendError.ErrorTypes.DbTransactionError,
          "[RegisterUser.addToLocalDb] Failed to add user to the database. Given exception: " + e);
    }

    return null;
  }

  BackendError addToCloudDb(UserInfo usr) {
    try {
      DBOperations cloudOps = new DBOperations(this.cloudDb);

      BackendError resp = cloudOps.addUser(usr.username, usr.email, usr.hashedPassword, usr.salt, usr.passwordDbName,
          usr.lastLoggedInTime);
      if (resp != null) {
        return resp;
      }
    } catch (Exception e) {
      return new BackendError(BackendError.ErrorTypes.DbTransactionError,
          "[RegisterUser.addToCloudDb] Failed to add user to the database. Given exception: " + e);
    }

    return null;
  }

  @BeforeAll
  void setup() {
    RegisterUser reg = new RegisterUser(this.localDb, this.cloudDb);

    // 0 -> Will be used in username and email based login check
    generateUser();
    reg.setUsername(createdMockUsers.get(0).username);
    reg.setEmail(createdMockUsers.get(0).email);
    reg.setPassword(this.commonPlaintextPassword);

    BackendError response = reg.register();
    if (response != null) {
      System.err.println("[LoginUserTest.setup] Failed to register for login test: " + response.getErrorType() + " -> "
          + response.getContext());

      throw new IllegalStateException("[LoginUserTest.setup] Failed to register a user to DB for login testing");
    }

    // To test the syncing edge-cases
    generateUser(); // to test syncing with cloud DB
    generateUser(); // to test syncing with local DB

    generateUser(); // to test syncing conflict

    // Case-1: User registered only in cloud DB
    response = addToCloudDb(this.createdMockUsers.get(1));
    if (response != null) {
      System.err.println("[LoginUserTest.setup] Failed to seed for syncing test: " + response.getErrorType() + " -> "
          + response.getContext());

      throw new IllegalStateException("[LoginUserTest.setup] Failed to seed a user to DB for login testing");
    }
    createLocalDbFile(this.createdMockUsers.get(1).passwordDbName);
    String localDbPath = FileHandler.getFullPath(this.createdMockUsers.get(1).passwordDbName);
    File localDbFileRef = new File(localDbPath);
    SupabaseUtils supaUtils = new SupabaseUtils();
    boolean isUploaded = supaUtils.uploadVault(Path.of(localDbPath), this.createdMockUsers.get(1).passwordDbName);
    assert (localDbFileRef.delete());
    assert (isUploaded);

    // Case-2: User registered only in local DB
    response = addToLocalDb(this.createdMockUsers.get(2));
    if (response != null) {
      System.err.println("[LoginUserTest.setup] Failed to seed for syncing test: " + response.getErrorType() + " -> "
          + response.getContext());

      throw new IllegalStateException("[LoginUserTest.setup] Failed to seed a user to DB for login testing");
    }
    createLocalDbFile(this.createdMockUsers.get(2).passwordDbName);
    localDbPath = FileHandler.getFullPath(this.createdMockUsers.get(2).passwordDbName);
    localDbFileRef = new File(localDbPath);
    assert (localDbFileRef.exists());

    // Case-3: Got tested with `validCredentialLogsInUsingUsername()` and
    // `validCredentialLogsInUsingEmail()`

    // Case-4: User info is conflicting in both DBs
    response = addToCloudDb(this.createdMockUsers.get(3));
    if (response != null) {
      System.err.println("[LoginUserTest.setup] Failed to seed for syncing test: " + response.getErrorType() + " -> "
          + response.getContext());

      throw new IllegalStateException("[LoginUserTest.setup] Failed to seed a user to DB for login testing");
    }

    // we change the email to cause the conflict between the DSs
    this.createdMockUsers.get(3).email = "conflicting_email@gmail.com" + System.currentTimeMillis();

    response = addToLocalDb(this.createdMockUsers.get(3));
    if (response != null) {
      System.err.println("[LoginUserTest.setup] Failed to seed for syncing test: " + response.getErrorType() + " -> "
          + response.getContext());

      throw new IllegalStateException("[LoginUserTest.setup] Failed to seed a user to DB for login testing");
    }
    createLocalDbFile(this.createdMockUsers.get(3).passwordDbName);
    localDbPath = FileHandler.getFullPath(this.createdMockUsers.get(3).passwordDbName);
    localDbFileRef = new File(localDbPath);
    isUploaded = supaUtils.uploadVault(Path.of(localDbPath), this.createdMockUsers.get(1).passwordDbName);
    assert (isUploaded);
    assert (localDbFileRef.exists());
  }

  @Test
  void validCredentialLogsInUsingUsername() {
    UserInfo u0 = this.createdMockUsers.get(0);
    LoginUser auth = new LoginUser(this.localDb, this.cloudDb, u0.username, this.commonPlaintextPassword);

    assertDoesNotThrow(() -> {
      BackendError resp = auth.login();
      assertEquals(null, resp);
    });
  }

  @Test
  void validCredentialLogsInUsingEmail() {
    UserInfo u0 = this.createdMockUsers.get(0);
    LoginUser auth = new LoginUser(this.localDb, this.cloudDb, u0.email, this.commonPlaintextPassword);

    assertDoesNotThrow(() -> {
      BackendError resp = auth.login();
      assertEquals(null, resp);
    });
  }

  @Test
  void unmatchingPasswordFailsToLogIn() {
    UserInfo u0 = this.createdMockUsers.get(0);
    LoginUser auth = new LoginUser(this.localDb, this.cloudDb, u0.username, "invalid_Password123@!");

    BackendError resp = auth.login();
    assertEquals(BackendError.ErrorTypes.InvalidLoginCredentials, resp.getErrorType());
  }

  @Test
  void unregisteredUsernameFailsToLogIn() {
    LoginUser auth = new LoginUser(this.localDb, this.cloudDb, "invalidUsername", this.commonPlaintextPassword);

    BackendError resp = auth.login();
    assertEquals(BackendError.ErrorTypes.UserDoesNotExist, resp.getErrorType());
  }

  @Test
  void unregisteredEmailFailsToLogIn() {
    LoginUser auth = new LoginUser(this.localDb, this.cloudDb, "invalid_email@gmail.com", this.commonPlaintextPassword);

    BackendError resp = auth.login();
    assertEquals(BackendError.ErrorTypes.UserDoesNotExist, resp.getErrorType());
  }

  @Test
  void emptyUsernameOrEmailFailsToLogIn() {
    LoginUser auth = new LoginUser(this.localDb, this.cloudDb, "", this.commonPlaintextPassword);

    BackendError resp = auth.login();
    assertEquals(BackendError.ErrorTypes.InvalidLoginCredentials, resp.getErrorType());
  }

  @Test
  void shouldSyncWithCloud() {
    UserInfo u1 = this.createdMockUsers.get(1);
    LoginUser auth = new LoginUser(this.localDb, this.cloudDb, u1.username,
        this.commonPlaintextPassword);

    assertDoesNotThrow(() -> {
      DBOperations localOps = new DBOperations(this.localDb);
      UserInfo retrieved = localOps.getUserInfo(u1.username);
      assertEquals(-1, retrieved.lastLoggedInTime); // shouldn't exist before syncing

      BackendError resp = auth.login();
      assertEquals(null, resp);

      retrieved = localOps.getUserInfo(u1.username);
      assertEquals(u1.email, retrieved.email);
    });
  }

  @Test
  void shouldSyncWithLocal() {
    UserInfo u2 = this.createdMockUsers.get(2);
    LoginUser auth = new LoginUser(this.localDb, this.cloudDb, u2.username,
        this.commonPlaintextPassword);

    assertDoesNotThrow(() -> {
      DBOperations cloudOps = new DBOperations(this.cloudDb);
      UserInfo retrieved = cloudOps.getUserInfo(u2.username);
      assertEquals(-1, retrieved.lastLoggedInTime); // shouldn't exist before syncing

      BackendError resp = auth.login();
      assertEquals(null, resp);

      retrieved = cloudOps.getUserInfo(u2.username);
      assertEquals(u2.email, retrieved.email);
    });
  }

  @Test
  void shouldHandleDbConflict() {
    UserInfo u3 = this.createdMockUsers.get(2);
    LoginUser auth = new LoginUser(this.localDb, this.cloudDb, u3.username,
        this.commonPlaintextPassword);

    assertDoesNotThrow(() -> {
      DBOperations localOps = new DBOperations(this.localDb);

      BackendError resp = auth.login();
      assertEquals(null, resp);

      // the user entry in the local DB should get deleted
      UserInfo retrieved = localOps.getUserInfo(u3.username);
      assertNotEquals(-1, retrieved.lastLoggedInTime);
    });
  }

  @AfterAll
  void remove() {
    DBOperations localOps = new DBOperations(this.localDb);
    DBOperations cloudOps = new DBOperations(this.cloudDb);

    try {
      for (int i = 0; i <= 3; i++) {
        localOps.deleteUser(this.createdMockUsers.get(i).username);
        cloudOps.deleteUser(this.createdMockUsers.get(i).username);
      }
    } catch (Exception e) {
      System.err.println("[LoginUserTest.remove] (Non-fatal) Failed to remove the mock user from DBs: " + e);
    }
  }
}
