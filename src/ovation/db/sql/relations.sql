-- {
--   "_id": "cd553b43-68bc-409c-9dc5-795af00e1c41--files-->41ee30a5-2965-4492-8335-0640a763de63",
--   "organization_id": 0,
--   "type": "Relation",
--   "user_id": "15cab930-1e24-0131-026c-22000a977b96",
--   "rel": "files",
--   "inverse_rel": "parents",
--   "target_id": "41ee30a5-2965-4492-8335-0640a763de63",
--   "source_id": "cd553b43-68bc-409c-9dc5-795af00e1c41",
--   "links": {
--     "_collaboration_roots": [
--       "cd553b43-68bc-409c-9dc5-795af00e1c41"
--     ],
--     "self": "/api/v1/o/0/relationships/cd553b43-68bc-409c-9dc5-795af00e1c41--files-->41ee30a5-2965-4492-8335-0640a763de63"
--   }
-- }

-- :name create :insert
-- :doc Create new relation
INSERT INTO `or_relations` (
  `uuid`,
  `organization_id`,
  `project_id`,
  `user_id`,
  `rel`,
  `inverse_rel`,
  `source_id`,
  `source_type`,
  `target_id`,
  `target_type`
)
VALUES (
  :_id,
  :organization_id,
  :project_id,
  :user_id,
  :rel,
  :inverse_rel,
  :source_id,
  :source_type,
  :target_id,
  :target_type
)

-- :name update :! :n
-- :doc Update relation
UPDATE `or_relations`
SET
  `or_relations`.`rel` = :rel,
  `or_relations`.`inverse_rel` = :inverse_rel
WHERE `or_relations`.`uuid` = :_id
  AND `or_relations`.`organization_id` = :organization_id
  AND `or_relations`.`project_id` = :project_id

-- :name archive-by-entity :! :n
-- :doc Archive all relations involving entity
UPDATE `or_relations`
SET
  `or_relations`.`archived` = :archived,
  `or_relations`.`archived_at` = :archived_at,
  `or_relations`.`archived_by_user_id` = :archived_by_user_id
WHERE
  (
        `or_relations`.`source_id` = :id
    AND `or_relations`.`source_type` = :type
  )
  OR
  (
        `or_relations`.`target_id` = :id
    AND `or_relations`.`target_type` = :type
  )

-- :name unarchive-by-entity :! :n
-- :doc Unarchive all relations involving entity
UPDATE `or_relations`
SET
  `or_relations`.`archived` = false,
  `or_relations`.`archived_at` = NULL,
  `or_relations`.`archived_by_user_id` = NULL
WHERE
  (
        `or_relations`.`source_id` = :id
    AND `or_relations`.`source_type` = :type
  )
  OR
  (
        `or_relations`.`target_id` = :id
    AND `or_relations`.`target_type` = :type
  )

-- :name delete :! :n
-- :doc Delete relation
DELETE FROM `or_relations`
WHERE `or_relations`.`uuid` = :_id
  AND `or_relations`.`organization_id` = :organization_id

-- :name count :? :1
-- :doc Count relations
SELECT COUNT(*) AS `count`
FROM `or_relations`

-- :name find-all-by-uuid :? :*
-- :doc Find all relations by id
SELECT
  `or_relations`.`id` AS `id`,
  `or_relations`.`uuid` AS `_id`,
  `or_relations`.`organization_id` AS `organization_id`,
  `or_relations`.`rel`,
  `or_relations`.`inverse_rel`,
  `source_uuid`.`uuid` AS `source_id`,
  `target_uuid`.`uuid` AS `target_id`,
  `users`.`uuid` AS `user_id`,
  `or_projects`.`uuid` AS `project`,
  "Relation" as `type`
FROM `or_relations`
INNER JOIN `uuids` AS `source_uuid` ON `source_uuid`.`entity_id` = `or_relations`.`source_id`
  AND `source_uuid`.`entity_type` = `or_relations`.`source_type`
INNER JOIN `uuids` AS `target_uuid` ON `target_uuid`.`entity_id` = `or_relations`.`target_id`
  AND `target_uuid`.`entity_type` = `or_relations`.`target_type`
INNER JOIN `users` ON `users`.`id` = `or_relations`.`user_id`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_relations`.`project_id`
WHERE `or_relations`.`uuid` IN (:v*:ids)

-- :name find-all-by-parent-entity-rel :? :*
-- :doc Find all relations by parent entity and rel
SELECT
  `or_relations`.`id` AS `id`,
  `or_relations`.`uuid` AS `_id`,
  `or_relations`.`organization_id` AS `organization_id`,
  `or_relations`.`rel`,
  `or_relations`.`inverse_rel`,
  `source_uuid`.`uuid` AS `source_id`,
  `target_uuid`.`uuid` AS `target_id`,
  `users`.`uuid` AS `user_id`,
  `or_projects`.`uuid` AS `project`,
  "Relation" as `type`
FROM `or_relations`
INNER JOIN `uuids` AS `source_uuid` ON `source_uuid`.`entity_id` = `or_relations`.`source_id`
  AND `source_uuid`.`entity_type` = `or_relations`.`source_type`
INNER JOIN `uuids` AS `target_uuid` ON `target_uuid`.`entity_id` = `or_relations`.`target_id`
  AND `target_uuid`.`entity_type` = `or_relations`.`target_type`
INNER JOIN `users` ON `users`.`id` = `or_relations`.`user_id`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_relations`.`project_id`
WHERE `or_relations`.`source_id` = :entity_id
  AND `or_relations`.`source_type` = :entity_type
  AND `or_relations`.`rel` = :rel
  AND `or_relations`.`archived` = false


