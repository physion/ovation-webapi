-- {
--   "_id": "string",
--   "organization_id": 0,
--   "type": "string",
--   "annotation_type": "string",
--   "entity": "string",
--   "user": "string",
--   "edited_at": "string",
--   "annotation": {
--     "text": "string",
--     "timestamp": "string"
--   },
--   "permissions": {},
--   "links": {
--     "_collaboration_roots": [
--       "string"
--     ]
--   }
-- }

-- :name create :insert
-- :doc Create new note
INSERT INTO `or_notes` (
  `uuid`,
  `organization_id`,
  `project_id`,
  `user_id`,
  `entity_id`,
  `entity_type`,
  `text`,
  `timestamp`,
  `created_at`,
  `updated_at`
)
VALUES (
  :_id,
  :organization_id,
  :project_id,
  :user_id,
  :entity_id,
  :entity_type,
  :text,
  :timestamp,
  :created-at,
  :updated-at
)

-- :name update :! :n
-- :doc Update note
UPDATE `or_notes`
SET
  `or_notes`.`text` = :text,
  `or_notes`.`timestamp` = :timestamp,
  `or_notes`.`updated_at` = :edited_at
WHERE `or_notes`.`uuid` = :_id
  AND `or_notes`.`organization_id` = :organization_id
  AND `or_notes`.`project_id` = :project_id

-- :name delete :! :n
-- :doc Delete note
DELETE FROM `or_notes`
WHERE `or_notes`.`uuid` = :_id
  AND `or_notes`.`organization_id` = :organization_id
  AND `or_notes`.`project_id` = :project_id

-- :name count :? :1
-- :doc Count notes
SELECT count(*) AS `count`
FROM `or_notes`

-- :name find-by-uuid :? :1
-- :doc Find first note with ID
SELECT
  `or_notes`.`id` AS `id`,
  `or_notes`.`uuid` AS `_id`,
  `or_notes`.`organization_id` AS `organization_id`,
  `or_projects`.`id` AS `project_id`,
  `or_projects`.`uuid` AS `project`,
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
  `or_notes`.`id` AS `id`,
  `or_notes`.`uuid` AS `_id`,
  `or_notes`.`organization_id` AS `organization_id`,
  `or_projects`.`id` AS `project_id`,
  `or_projects`.`uuid` AS `project`,
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

-- :name find-all-by-entity-uuid :? :*
-- :doc Find all notes by Entity ID
SELECT
  `or_notes`.`id` AS `id`,
  `or_notes`.`uuid` AS `_id`,
  `or_notes`.`organization_id` AS `organization_id`,
  `or_projects`.`id` AS `project_id`,
  `or_projects`.`uuid` AS `project`,
  `users`.`uuid` AS `user`,
  `entity_uuid`.`uuid` AS `entity`,
  `or_notes`.`text` AS `text`,
  `or_notes`.`timestamp` AS `timestamp`,
  `or_notes`.`updated_at` AS `edited_at`,
  "notes" AS `annotation_type`,
  "Annotation" AS `type`
FROM `or_notes`
INNER JOIN `uuids` AS `entity_uuid` ON `entity_uuid`.`entity_id` = `or_notes`.`entity_id`
  AND `entity_uuid`.`entity_type` = `or_notes`.`entity_type`
  AND `entity_uuid`.`uuid` IN (:v*:ids)
INNER JOIN `users` ON `users`.`id` = `or_notes`.`user_id`
LEFT JOIN `or_projects` ON `or_projects`.`id` = `or_notes`.`project_id`
LEFT JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
LEFT JOIN `or_sources`  ON `or_sources`.`id` = `or_notes`.`entity_id`
WHERE `or_notes`.`organization_id` = :organization_id
  AND (`teams`.`uuid` IN (:v*:team_uuids)
    OR (`entity_uuid`.`entity_type` = 'Source' AND `or_sources`.`owner_id` = :owner_id))

