package org.backend;

import java.io.File;
import java.nio.file.Path;

public class FileHandler {
  public static String getDbStoreDir() {
    String homeDir = System.getProperty("user.home");
    Path fullPath = Path.of(homeDir, "YAPM");

    return fullPath.toString();
  }

  public static String getFullPath(String dbFileName) {
    Path fullPath = Path.of(getDbStoreDir(), dbFileName);

    return fullPath.toString();
  }

  public static boolean createDbStoreDirIfNotExisting() {
    File dbStoreDir = new File(getDbStoreDir());
    if (!dbStoreDir.exists()) {
      return dbStoreDir.mkdir();
    }

    return true;
  }
}
