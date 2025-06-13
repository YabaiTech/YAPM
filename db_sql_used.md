# For creating the table

```sql
CREATE TABLE `YAPM_TEST2`.`master_users_table` (`id` INT NOT NULL AUTO_INCREMENT , `username` VARCHAR(1024) NOT NULL , `email` VARCHAR(1024) NOT NULL , `hashed_password` VARCHAR(2048) NOT NULL , `salt` VARCHAR(1024) NOT NULL , `pwd_db_path` VARCHAR(2048) NOT NULL , `last_logged_in` BIGINT(64) NOT NULL , PRIMARY KEY (`id`), UNIQUE `username` (`username`)) ENGINE = InnoDB; 
```
