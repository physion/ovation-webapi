-- :name create :insert
-- :doc Create folders
INSERT INTO `or_folders` (
  `uuid`,
  `organization_id`,
  `project_id`,
  `owner_id`,
  `name`,
  `attributes`,
  `archived`,
  `created_at`,
  `updated_at`
)
VALUES (
  :_id,
  :organization_id,
  :project_id,
  :owner_id,
  :name,
  :attributes,
  false,
  :created-at,
  :updated-at
)

-- :name update :! :n
-- :doc Update folder
UPDATE `or_folders`
SET
  `or_folders`.`name` = :name,
  `or_folders`.`attributes` = :attributes,
  `or_folders`.`updated_at` = :updated-at
WHERE `or_folders`.`uuid` = :_id
  AND `or_folders`.`organization_id` = :organization_id
  AND `or_folders`.`project_id` = :project_id

-- :name update-project-id :! :n
-- :doc Update folder project
UPDATE `or_folders`
SET
  `or_folders`.`project_id` = :project_id
WHERE `or_folders`.`uuid` = :_id
  AND `or_folders`.`organization_id` = :organization_id

-- :name archive :! :n
-- :doc Archive folder
UPDATE `or_folders`
SET
  `or_folders`.`archived` = :archived,
  `or_folders`.`archived_at` = :archived_at,
  `or_folders`.`archived_by_user_id` = :archived_by_user_id,
  `or_folders`.`updated_at` = :updated-at
WHERE `or_folders`.`uuid` = :_id
  AND `or_folders`.`organization_id` = :organization_id
  AND `or_folders`.`project_id` = :project_id

-- :name unarchive :! :n
-- :doc Unarchive folder
UPDATE `or_folders`
SET
  `or_folders`.`archived` = false,
  `or_folders`.`archived_at` = NULL,
  `or_folders`.`archived_by_user_id` = NULL,
  `or_folders`.`updated_at` = :updated-at
WHERE `or_folders`.`uuid` = :_id
  AND `or_folders`.`organization_id` = :organization_id
  AND `or_folders`.`project_id` = :project_id

-- :name delete :! :n
-- :doc Delete folder
DELETE FROM `or_folders`
WHERE `or_folders`.`uuid` = :_id
  AND `or_folders`.`organization_id` = :organization_id
  AND `or_folders`.`project_id` = :project_id

-- :name count :? :1
-- :doc Count folders
SELECT COUNT(*) AS `count`
FROM `or_folders`

-- :name find-all :? :*
-- :doc Find all folders
SELECT
  `or_folders`.`id` AS `id`,
  `or_folders`.`uuid` AS `_id`,
  `or_folders`.`organization_id` AS `organization_id`,
  `or_projects`.`id` AS `project_id`,
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
  `or_folders`.`id` AS `id`,
  `or_folders`.`uuid` AS `_id`,
  `or_folders`.`organization_id` AS `organization_id`,
  `or_projects`.`id` AS `project_id`,
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
  `or_folders`.`id` AS `id`,
  `or_folders`.`uuid` AS `_id`,
  `or_folders`.`organization_id` AS `organization_id`,
  `or_projects`.`id` AS `project_id`,
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
LEFT JOIN `or_relations` AS `rel` ON `rel`.`child_entity_id` = `or_folders`.`id`
  AND `rel`.`child_entity_type` = 'Folder'
  AND `rel`.`parent_entity_id` = :entity_id
  AND `rel`.`parent_entity_type` = :entity_type
  AND `rel`.`rel` = :rel
LEFT JOIN `or_relations` AS `inverse_rel` ON `inverse_rel`.`parent_entity_id` = `or_folders`.`id`
  AND `inverse_rel`.`parent_entity_type` = 'Folder'
  AND `inverse_rel`.`child_entity_id` = :entity_id
  AND `inverse_rel`.`child_entity_type` = :entity_type
  AND `inverse_rel`.`inverse_rel` = :rel
WHERE `or_folders`.`archived` = :archived
  AND `or_folders`.`organization_id` = :organization_id
  AND (`rel`.`id` OR `inverse_rel`.`id`)

