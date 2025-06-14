package org.backend;

import io.github.cdimascio.dotenv.Dotenv;

class ProdEnvVars {
  private Dotenv dotenv;

  public ProdEnvVars() {
    try {
      this.dotenv = Dotenv.load();
    } catch (Exception e) {
      System.out.println("You might have forgotten to copy the .env file into the /app directory");
      System.exit(1);
    }
  }

  public String get(String varName) {
    return this.dotenv.get(varName);
  }
}
