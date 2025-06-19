package org.backend;

import io.github.cdimascio.dotenv.Dotenv;

public class ProdEnvVars {
  private static Dotenv dotenv;

  public ProdEnvVars() {
    if (dotenv != null) {
      return;
    }

    try {
      dotenv = Dotenv.load();
    } catch (Exception e) {
      System.out.println("You might have forgotten to copy the .env file into the /app directory");
      System.exit(1);
    }
  }

  public String get(String varName) {
    return dotenv.get(varName);
  }
}
