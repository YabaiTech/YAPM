package org.backend;

class EnvVars {
  public static String DATABASE_NAME = "YAPM";
  public static String DATABASE_BASE_URL = "jdbc:mysql://localhost:3306/";
  public static String DATABASE_URL = DATABASE_BASE_URL + DATABASE_NAME;
  public static String DATABASE_USER = "root";
  public static String DATABASE_PASSWORD = "";

  public static String MASTER_USER_TABLE = "master_users";

  private static final String TABLE_CREATION_SQL1 = "CREATE TABLE IF NOT EXISTS `";
  private static final String TABLE_CREATION_SQL2 = """
      ` (`id` INT NOT NULL AUTO_INCREMENT, `username` VARCHAR(512) NOT NULL,
      `email` VARCHAR(1024) NOT NULL, `hashed_password` VARCHAR(2048) NOT NULL,
      `salt` VARCHAR(1024) NOT NULL, `pwd_db_path` VARCHAR(2048) NOT NULL,
      `last_logged_in` BIGINT(64) NOT NULL , PRIMARY KEY (`id`), UNIQUE `username` (`username`)) ENGINE = InnoDB;
          """;

  public static String getTableCreationSQL(String tableName) {
    return TABLE_CREATION_SQL1 + tableName + TABLE_CREATION_SQL2;
  }
}
