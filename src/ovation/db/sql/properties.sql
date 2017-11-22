-- {
--   "_id": "string",
--   "organization_id": 0,
--   "type": "string",
--   "annotation_type": "string",
--   "entity": "string",
--   "user": "string",
--   "annotation": {
--     "key": "string",
--     "value": {}
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
INSERT INTO `or_properties` (
  `uuid`,
  `organization_id`,
  `project_id`,
  `user_id`,
  `entity_id`,
  `entity_type`,
  `property_key`,
  `property_value`,
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
  :key,
  :value,
  :created-at,
  :updated-at
)

-- :name update :! :n
-- :doc Update note
UPDATE `or_properties`
SET
  `or_properties`.`property_key` = :key,
  `or_properties`.`property_value` = :value,
  `or_properties`.`updated_at` = :updated-at
WHERE `or_properties`.`uuid` = :_id
  AND `or_properties`.`organization_id` = :organization_id
  AND `or_properties`.`project_id` = :project_id

-- :name delete :! :n
-- :doc Delete property
DELETE FROM `or_properties`
WHERE `or_properties`.`uuid` = :_id
  AND `or_properties`.`organization_id` = :organization_id
  AND `or_properties`.`project_id` = :project_id

-- :name count :? :1
-- :doc Count properties
SELECT COUNT(*) AS `count`
FROM `or_properties`

-- :name find-by-uuid :? :1
-- :doc Find first property with ID
SELECT
  `or_properties`.`id` AS `id`,
  `or_properties`.`uuid` AS `_id`,
  `or_properties`.`organization_id` AS `organization_id`,
  `or_projects`.`uuid` AS `project`,
  `users`.`uuid` AS `user`,
  `entity_uuid`.`uuid` AS `entity`,
  `or_properties`.`property_key` AS `key`,
  `or_properties`.`property_value` AS `value`,
  "properties" AS `annotation_type`,
  "Annotation" AS `type`
FROM `or_properties`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_properties`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `uuids` AS `entity_uuid` ON `entity_uuid`.`entity_id` = `or_properties`.`entity_id`
  AND `entity_uuid`.`entity_type` = `or_properties`.`entity_type`
INNER JOIN `users` ON `users`.`id` = `or_properties`.`user_id`
WHERE `or_properties`.`uuid` = :id
  AND `or_properties`.`organization_id` = :organization_id

-- :name find-all-by-uuid :? :*
-- :doc Find all properties by ID
SELECT
  `or_properties`.`id` AS `id`,
  `or_properties`.`uuid` AS `_id`,
  `or_properties`.`organization_id` AS `organization_id`,
  `or_projects`.`uuid` AS `project`,
  `users`.`uuid` AS `user`,
  `entity_uuid`.`uuid` AS `entity`,
  `or_properties`.`property_key` AS `key`,
  `or_properties`.`property_value` AS `value`,
  "properties" AS `annotation_type`,
  "Annotation" AS `type`
FROM `or_properties`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_properties`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `uuids` AS `entity_uuid` ON `entity_uuid`.`entity_id` = `or_properties`.`entity_id`
  AND `entity_uuid`.`entity_type` = `or_properties`.`entity_type`
INNER JOIN `users` ON `users`.`id` = `or_properties`.`user_id`
WHERE `or_properties`.`uuid` IN (:v*:ids)
  AND `or_properties`.`organization_id` = :organization_id

