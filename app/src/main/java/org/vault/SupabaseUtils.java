package org.vault;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.backend.ProdEnvVars;

public class SupabaseUtils {
  private static String SUPABASE_URL;
  private static String SUPABASE_API_KEY;
  private static String BUCKET_NAME;

  private static final HttpClient client = HttpClient.newHttpClient();

  public SupabaseUtils() {
    if (SUPABASE_URL != null) {
      return;
    }

    ProdEnvVars dotenv = new ProdEnvVars();
    SUPABASE_URL = dotenv.get("SUPABASE_URL");
    SUPABASE_API_KEY = dotenv.get("SUPABASE_API_KEY");
    BUCKET_NAME = dotenv.get("BUCKET_NAME");
  }

  public boolean uploadVault(Path localFile, String remotePath) {
    try {
      String uploadUrl = String.format(
          "%sstorage/v1/object/%s/%s",
          SUPABASE_URL,
          BUCKET_NAME,
          URLEncoder.encode(remotePath, StandardCharsets.UTF_8));

      HttpRequest req = HttpRequest.newBuilder(URI.create(uploadUrl))
          .header("apikey", SUPABASE_API_KEY)
          .header("Authorization", "Bearer " + SUPABASE_API_KEY)
          .header("x-upsert", "true")
          .POST(BodyPublishers.ofFile(localFile))
          .build();

      HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
      int code = res.statusCode();

      if (code / 100 == 2) {
        System.out.println("[Supabase.uploadVault] Success: " + res.body());
        return true;
      } else {
        System.err.println("[Supabase.uploadVault] Failed [" + code + "]: " + res.body());
        return false;
      }
    } catch (Exception e) {
      System.err.println("[Supabase.uploadVault] ERROR: ");
      e.printStackTrace();
      return false;
    }
  }

  public boolean downloadVault(String remotePath, Path localDest) {
    try {
      Path parent = localDest.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      if (Files.exists(localDest)) {
        Files.delete(localDest);
      }

      String downloadUrl = String.format(
          "%sstorage/v1/object/public/%s/%s",
          SUPABASE_URL,
          BUCKET_NAME,
          URLEncoder.encode(remotePath, StandardCharsets.UTF_8));

      HttpRequest req = HttpRequest.newBuilder(URI.create(downloadUrl))
          .header("apikey", SUPABASE_API_KEY)
          .header("Authorization", "Bearer " + SUPABASE_API_KEY)
          .GET()
          .build();

      HttpResponse<Path> res = client.send(req, BodyHandlers.ofFile(localDest));
      int code = res.statusCode();

      if (code / 100 == 2) {
        System.out.println("[Supabase.downloadVault] Success: wrote to " +
            localDest);
        return true;
      } else {
        System.err.println("[Supabase.downloadVault] Failed [" + code + "]" + res.body());
        return false;
      }
    } catch (Exception e) {
      System.err.println("[Supabase.downloadVault] ERROR:");
      e.printStackTrace();
      return false;
    }
  }
}
