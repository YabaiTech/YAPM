package org.vault;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Paths;

class SupabaseUtilsTest {
  // @Test
  // void shouldUploadFile() {
  // SupabaseUtils supaUtils = new SupabaseUtils();
  //
  // Path source =
  // Paths.get("/home/twaha/YAPM/anindya483324ee8d800-1ca5-420f-bbea-71750dfb1841.db");
  // assertEquals(true, supaUtils.uploadVault(source,
  // "anindya483324ee8d800-1ca5-420f-bbea-71750dfb1841.db"));
  // }

  // @Test
  // void shouldDownloadFile() {
  // SupabaseUtils supaUtils = new SupabaseUtils();
  //
  // Path dest = Path.of("/home/twaha/YAPM/gen.db");
  // assertEquals(true,
  // supaUtils.downloadVault("anindya483324ee8d800-1ca5-420f-bbea-71750dfb1841.db",
  // dest));
  // }

  @Test
  void testtube() {
    SupabaseUtils supaUtils = new SupabaseUtils();

    boolean ret1 = supaUtils.uploadVault(Path.of("/home/twaha/YAPM/simple.txt"),
        "simple.txt");
    boolean ret2 = supaUtils.downloadVault("simple.txt",
        Path.of("/home/twaha/YAPM/simply_downloaded.txt"));

    System.out.println(ret1 + " | " + ret2);
  }
}
