-- {
--   "_id": "5162f46e-f79d-43d6-ba9b-73b1198119f0",
--   "permissions": {
--     "update": false,
--     "delete": false
--   },
--   "_rev": "1-29d6d50d42151f358d3f4375219010ee",
--   "organization_id": 292,
--   "type": "Annotation",
--   "annotation_type": "tags",
--   "entity": "c946977e-52ab-473f-b231-3efbb4ba46b5",
--   "user": "5b8c8e20-d642-0130-2f6c-22000aec9fab",
--   "annotation": {
--     "tag": "Version tag"
--   },
--   "links": {
--     "_collaboration_roots": [
--       "aa511810-a28e-489b-810e-8d52d48e8673"
--     ],
--     "self": "/api/v1/o/292/entities/c946977e-52ab-473f-b231-3efbb4ba46b5/annotations/tags/5162f46e-f79d-43d6-ba9b-73b1198119f0"
--   }
-- }


-- :name create :insert
-- :doc Create new property
INSERT INTO `or_tags` (
  `uuid`,
  `organization_id`,
  `project_id`,
  `user_id`,
  `entity_id`,
  `entity_type`,
  `tag`,
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
  :tag,
  now(),
  now()
)

-- :name update :! :n
-- :doc Update tag
UPDATE `or_tags`
SET
  `or_tags`.`tag` = :tag,
  `or_tags`.`updated_at` = now()
WHERE `or_tags`.`uuid` = :_id
  AND `or_tags`.`organization_id` = :organization_id
  AND `or_tags`.`project_id` = :project_id

-- :name delete :! :n
-- :doc Delete tag
DELETE FROM `or_tags`
WHERE `or_tags`.`uuid` = :_id
  AND `or_tags`.`organization_id` = :organization_id
  AND `or_tags`.`project_id` = :project_id

-- :name count :? :1
-- :doc Count tags
SELECT COUNT(*) AS `count`
FROM `or_tags`

-- :name find-by-uuid :? :1
-- :doc Find first tag with ID
SELECT
  `or_tags`.`uuid` AS `_id`,
  `or_tags`.`organization_id` AS `organization_id`,
  `users`.`uuid` AS `user`,
  `entity_uuid`.`uuid` AS `entity`,
  `or_tags`.`tag` AS `tag`,
  "tags" AS `annotation_type`,
  "Annotation" AS `type`
FROM `or_tags`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_tags`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `uuids` AS `entity_uuid` ON `entity_uuid`.`entity_id` = `or_tags`.`entity_id`
  AND `entity_uuid`.`entity_type` = `or_tags`.`entity_type`
INNER JOIN `users` ON `users`.`id` = `or_tags`.`user_id`
WHERE `or_tags`.`uuid` = :id
  AND `or_tags`.`organization_id` = :organization_id

-- :name find-all-by-uuid :? :*
-- :doc Find all tags by ID
SELECT
  `or_tags`.`uuid` AS `_id`,
  `or_tags`.`organization_id` AS `organization_id`,
  `users`.`uuid` AS `user`,
  `entity_uuid`.`uuid` AS `entity`,
  `or_tags`.`tag` AS `tag`,
  "tags" AS `annotation_type`,
  "Annotation" AS `type`
FROM `or_tags`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_tags`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `uuids` AS `entity_uuid` ON `entity_uuid`.`entity_id` = `or_tags`.`entity_id`
  AND `entity_uuid`.`entity_type` = `or_tags`.`entity_type`
INNER JOIN `users` ON `users`.`id` = `or_tags`.`user_id`
WHERE `or_tags`.`uuid` IN (:v*:ids)
  AND `or_tags`.`organization_id` = :organization_id

