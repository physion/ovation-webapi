-- {
--   "_id": "string",
--   "organization_id": 0,
--   "type": "string",
--   "annotation_type": "string",
--   "entity": "string",
--   "user": "string",
--   "annotation": {
--     "name": "string",
--     "notes": "string",
--     "start": "string",
--     "end": "string"
--   },
--   "permissions": {},
--   "links": {
--     "_collaboration_roots": [
--       "string"
--     ]
--   }
-- }

-- :name create :insert
-- :doc Create new property
INSERT INTO `or_timeline_events` (
  `uuid`,
  `organization_id`,
  `project_id`,
  `user_id`,
  `entity_id`,
  `entity_type`,
  `name`,
  `notes`,
  `start`,
  `end`,
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
  :name,
  :notes,
  :start,
  :end,
  :created-at,
  :updated-at
)

-- :name update :! :n
-- :doc Update timeline event
UPDATE `or_timeline_events`
SET
  `or_timeline_events`.`name` = :name,
  `or_timeline_events`.`notes` = :notes,
  `or_timeline_events`.`start` = :start,
  `or_timeline_events`.`end` = :end,
  `or_timeline_events`.`updated_at` = now()
WHERE `or_timeline_events`.`uuid` = :_id
  AND `or_timeline_events`.`organization_id` = :organization_id
  AND `or_timeline_events`.`project_id` = :project_id

-- :name delete :! :n
-- :doc Delete timeline event
DELETE FROM `or_timeline_events`
WHERE `or_timeline_events`.`uuid` = :_id
  AND `or_timeline_events`.`organization_id` = :organization_id
  AND `or_timeline_events`.`project_id` = :project_id

-- :name count :? :1
-- :doc Count timeline events
SELECT COUNT(*) AS `count`
FROM `or_timeline_events`

-- :name find-by-uuid :? :1
-- :doc Find first tag with ID
SELECT
  `or_timeline_events`.`id` AS `id`,
  `or_timeline_events`.`uuid` AS `_id`,
  `or_timeline_events`.`organization_id` AS `organization_id`,
  `or_projects`.`id` AS `project_id`,
  `or_projects`.`uuid` AS `project`,
  `users`.`uuid` AS `user`,
  `entity_uuid`.`uuid` AS `entity`,
  `or_timeline_events`.`name` AS `name`,
  `or_timeline_events`.`notes` AS `notes`,
  `or_timeline_events`.`start` AS `start`,
  `or_timeline_events`.`end` AS `end`,
  "timeline_events" AS `annotation_type`,
  "Annotation" AS `type`
FROM `or_timeline_events`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_timeline_events`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `uuids` AS `entity_uuid` ON `entity_uuid`.`entity_id` = `or_timeline_events`.`entity_id`
  AND `entity_uuid`.`entity_type` = `or_timeline_events`.`entity_type`
INNER JOIN `users` ON `users`.`id` = `or_timeline_events`.`user_id`
WHERE `or_timeline_events`.`uuid` = :id
  AND `or_timeline_events`.`organization_id` = :organization_id

-- :name find-all-by-uuid :? :*
-- :doc Find all timeline_events by ID
SELECT
  `or_timeline_events`.`id` AS `id`,
  `or_timeline_events`.`uuid` AS `_id`,
  `or_timeline_events`.`organization_id` AS `organization_id`,
  `or_projects`.`id` AS `project_id`,
  `or_projects`.`uuid` AS `project`,
  `users`.`uuid` AS `user`,
  `entity_uuid`.`uuid` AS `entity`,
  `or_timeline_events`.`name` AS `name`,
  `or_timeline_events`.`notes` AS `notes`,
  `or_timeline_events`.`start` AS `start`,
  `or_timeline_events`.`end` AS `end`,
  "timeline_events" AS `annotation_type`,
  "Annotation" AS `type`
FROM `or_timeline_events`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_timeline_events`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `uuids` AS `entity_uuid` ON `entity_uuid`.`entity_id` = `or_timeline_events`.`entity_id`
  AND `entity_uuid`.`entity_type` = `or_timeline_events`.`entity_type`
INNER JOIN `users` ON `users`.`id` = `or_timeline_events`.`user_id`
WHERE `or_timeline_events`.`uuid` IN (:v*:ids)
  AND `or_timeline_events`.`organization_id` = :organization_id

-- :name find-all-by-entity-uuid :? :*
-- :doc Find all timeline_events by Entity ID
SELECT
  `or_timeline_events`.`id` AS `id`,
  `or_timeline_events`.`uuid` AS `_id`,
  `or_timeline_events`.`organization_id` AS `organization_id`,
  `or_projects`.`id` AS `project_id`,
  `or_projects`.`uuid` AS `project`,
  `users`.`uuid` AS `user`,
  `entity_uuid`.`uuid` AS `entity`,
  `or_timeline_events`.`name` AS `name`,
  `or_timeline_events`.`notes` AS `notes`,
  `or_timeline_events`.`start` AS `start`,
  `or_timeline_events`.`end` AS `end`,
  "timeline_events" AS `annotation_type`,
  "Annotation" AS `type`
FROM `or_timeline_events`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_timeline_events`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `uuids` AS `entity_uuid` ON `entity_uuid`.`entity_id` = `or_timeline_events`.`entity_id`
  AND `entity_uuid`.`entity_type` = `or_timeline_events`.`entity_type`
  AND `entity_uuid`.`uuid` IN (:v*:ids)
INNER JOIN `users` ON `users`.`id` = `or_timeline_events`.`user_id`
WHERE `or_timeline_events`.`organization_id` = :organization_id

