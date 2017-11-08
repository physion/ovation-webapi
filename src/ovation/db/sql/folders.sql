-- :name create :insert
-- :doc Create folders
INSERT INTO `or_folders` ()
VALUES ()


-- :name find-all :? :*
-- :doc Find all folders
SELECT
  `or_folders`.`uuid` AS `_id`,
  `or_folders`.`organization_id` AS `organization_id`,
  `or_projects`.`uuid` AS `project`,
  `or_folders`.`name` AS `name`,
  `or_folders`.`created_at` AS `created-at`,
  `or_folders`.`updated_at` AS `updated-at`,
  `or_folders`.`attributes` AS `attributes`,
  `users`.`uuid` AS `owner`,
  "Folder" as `type`
FROM `or_folders`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_folders`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `users` ON `users`.`id` = `or_folders`.`owner_id`
WHERE `or_folders`.`archived` = :archived
  AND `or_folders`.`organization_id` = :organization_id


-- :name find-all-by-uuid :? :*
-- :doc Find all folders by id
SELECT
  `or_folders`.`uuid` AS `_id`,
  `or_folders`.`organization_id` AS `organization_id`,
  `or_projects`.`uuid` AS `project`,
  `or_folders`.`name` AS `name`,
  `or_folders`.`created_at` AS `created-at`,
  `or_folders`.`updated_at` AS `updated-at`,
  `or_folders`.`attributes` AS `attributes`,
  `users`.`uuid` AS `owner`,
  "Folder" as `type`
FROM `or_folders`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_folders`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `users` ON `users`.`id` = `or_folders`.`owner_id`
WHERE `or_folders`.`uuid` IN (:v*:ids)
  AND `or_folders`.`archived` = :archived
  AND `or_folders`.`organization_id` = :organization_id


-- :name find-all-by-rel :? :*
-- :doc Find all folders by entity and rel
SELECT
  `or_folders`.`uuid` AS `_id`,
  `or_folders`.`organization_id` AS `organization_id`,
  `or_projects`.`uuid` AS `project`,
  `or_folders`.`name` AS `name`,
  `or_folders`.`created_at` AS `created-at`,
  `or_folders`.`updated_at` AS `updated-at`,
  `or_folders`.`attributes` AS `attributes`,
  `users`.`uuid` AS `owner`,
  "Folder" as `type`
FROM `or_folders`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_folders`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `users` ON `users`.`id` = `or_folders`.`owner_id`
INNER JOIN `or_relations` ON `or_relations`.`child_entity_id` = `or_folders`.`id`
  AND `or_relations`.`child_entity_type` = 'Folder'
  AND `or_relations`.`parent_entity_id` = :entity_id
  AND `or_relations`.`parent_entity_type` = :entity_type
  AND `or_relations`.`rel` = :rel
WHERE `or_folders`.`archived` = :archived
  AND `or_folders`.`organization_id` = :organization_id

