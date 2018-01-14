ALTER TABLE `or_sources`
  ADD COLUMN `project_id` INT,
  ADD INDEX `fk_project_id_idx` (`project_id` ASC);
