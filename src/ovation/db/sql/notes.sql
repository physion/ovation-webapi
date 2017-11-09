-- :name create :insert
-- :doc Create new project
INSERT INTO or_notes (uuid, organization_id)
VALUES (:uuid, :organization_id)


-- :name find-by-uuid :? :1
-- :doc Find first note with ID
SELECT
  `or_notes`.`uuid` AS `_id`,
  `users`.`uuid` AS `user`,
  `entity_uuid`.`uuid` AS `entity`,
  `or_notes`.`text` AS `text`,
  `or_notes`.`timestamp` AS `timestamp`,
  `or_notes`.`updated_at` AS `edited_at`,
  "notes" AS `annotation_type`,
  "Annotation" AS `type`
FROM `or_notes`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_notes`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `uuids` AS `entity_uuid` ON `entity_uuid`.`entity_id` = `or_notes`.`entity_id`
  AND `entity_uuid`.`entity_type` = `or_notes`.`entity_type`
INNER JOIN `users` ON `users`.`id` = `or_notes`.`user_id`
WHERE `or_notes`.`uuid` = :id
  AND `or_notes`.`organization_id` = :organization_id

-- :name find-all-by-uuid :? :*
-- :doc Find all notes by ID
SELECT
  `or_notes`.`uuid` AS `_id`,
  `users`.`uuid` AS `user`,
  `entity_uuid`.`uuid` AS `entity`,
  `or_notes`.`text` AS `text`,
  `or_notes`.`timestamp` AS `timestamp`,
  `or_notes`.`updated_at` AS `edited_at`,
  "notes" AS `annotation_type`,
  "Annotation" AS `type`
FROM `or_notes`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_notes`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `uuids` AS `entity_uuid` ON `entity_uuid`.`entity_id` = `or_notes`.`entity_id`
  AND `entity_uuid`.`entity_type` = `or_notes`.`entity_type`
INNER JOIN `users` ON `users`.`id` = `or_notes`.`user_id`
WHERE `or_notes`.`uuid` IN (:v*:ids)
  AND `or_notes`.`organization_id` = :organization_id

