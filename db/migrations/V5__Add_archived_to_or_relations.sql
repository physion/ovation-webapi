ALTER TABLE `or_relations` ADD COLUMN `archived` TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE `or_relations` ADD COLUMN `archived_at` DATETIME NULL;
ALTER TABLE `or_relations` ADD COLUMN `archived_by_user_id` INT NULL;
