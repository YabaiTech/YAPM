package org.vault;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Path;

class SupabaseUtilsTest {
  @TempDir
  Path tmpDir;

  @Test
  void shouldUploadFile() {
    SupabaseUtils supaUtils = new SupabaseUtils();

    String pathString = System.getProperty("user.dir") + "/../sample.env";
    Path source = Path.of(pathString);
    assertEquals(true, supaUtils.uploadVault(source, "sample.env"));
  }

  @Test
  void shouldDownloadFile() {
    SupabaseUtils supaUtils = new SupabaseUtils();

    Path dest = Path.of(tmpDir.toString() + "/downloaded_sample.env");
    assertEquals(true, supaUtils.downloadVault("sample.env", dest));

    File downloadedFile = new File(dest.toString());
    assertEquals(true, downloadedFile.exists());
  }

}
