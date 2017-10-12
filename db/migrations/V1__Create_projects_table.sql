-- -----------------------------------------------------
-- Table `or_projects`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `or_projects` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `uuid` VARCHAR(36) NOT NULL,
  `organization_id` INT NOT NULL,
  `name` VARCHAR(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `team_id` INT NOT NULL,
  `attributes` JSON NULL,
  `archived` TINYINT(1) NULL,
  `archived_at` DATETIME NULL,
  `archived_by_user_id` INT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  `owner_id` INT NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_team_id_idx` (`team_id` ASC),
  INDEX `fk_organization_id_idx` (`organization_id` ASC),
  INDEX `fk_archived_by_user_id_idx` (`archived_by_user_id` ASC),
  INDEX `fk_owner_id_idx` (`owner_id` ASC),
  CONSTRAINT `fk_team_id_or_projects`
  FOREIGN KEY (`team_id`)
  REFERENCES `ovation_development`.`teams` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_organization_id_or_projects`
  FOREIGN KEY (`organization_id`)
  REFERENCES `ovation_development`.`organizations` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_archived_by_user_id_or_projects`
  FOREIGN KEY (`archived_by_user_id`)
  REFERENCES `ovation_development`.`users` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_owner_id_or_projects`
  FOREIGN KEY (`owner_id`)
  REFERENCES `ovation_development`.`users` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
  ENGINE = InnoDB;
