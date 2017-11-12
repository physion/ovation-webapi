-- {
--   "rel": "files",
--   "_id": "cd553b43-68bc-409c-9dc5-795af00e1c41--files-->41ee30a5-2965-4492-8335-0640a763de63",
--   "_rev": "3-4b912ae8a45aa15967fa486bb829b4ab",
--   "organization_id": 0,
--   "type": "Relation",
--   "inverse_rel": "parents",
--   "target_id": "41ee30a5-2965-4492-8335-0640a763de63",
--   "user_id": "15cab930-1e24-0131-026c-22000a977b96",
--   "source_id": "cd553b43-68bc-409c-9dc5-795af00e1c41",
--   "links": {
--     "_collaboration_roots": [
--       "cd553b43-68bc-409c-9dc5-795af00e1c41"
--     ],
--     "self": "/api/v1/o/0/relationships/cd553b43-68bc-409c-9dc5-795af00e1c41--files-->41ee30a5-2965-4492-8335-0640a763de63"
--   }
-- }


-- :name find-all
-- :doc Find all relations
select * from relations

-- :name find-all-by-uuid :? :*
-- :doc Find all relations by id
SELECT
  `or_relations`.`uuid` AS `_id`,
  `or_relations`.`organization_id` AS `organization_id`,
  `or_projects`.`uuid` AS `project`,
  `or_relations`.`rel`,
  `or_relations`.`inverse_rel`,
  `parent_entity_uuid`.`uuid` AS `source_id`,
  `child_entity_uuid`.`uuid` AS `target_id`,
  `users`.`uuid` AS `user_id`,
  "Relation" as `type`
FROM `or_relations`
INNER JOIN `uuids` AS `parent_entity_uuid` ON `parent_entity_uuid`.`entity_id` = `or_relations`.`parent_entity_id`
  AND `parent_entity_uuid`.`entity_type` = `or_relations`.`parent_entity_type`
INNER JOIN `uuids` AS `child_entity_uuid` ON `child_entity_uuid`.`entity_id` = `or_relations`.`child_entity_id`
  AND `child_entity_uuid`.`entity_type` = `or_relations`.`child_entity_type`
INNER JOIN `users` ON `users`.`id` = `or_relations`.`user_id`
LEFT JOIN `or_projects` ON `or_projects`.`id` = `or_relations`.`project_id`
WHERE `or_relations`.`uuid` IN (:v*:ids)

-- :name find-all-by-parent-entity-rel :? :*
-- :doc Find all relations by parent entity and rel
SELECT
  `or_relations`.`uuid` AS `_id`,
  `or_relations`.`organization_id` AS `organization_id`,
  `or_projects`.`uuid` AS `project`,
  `or_relations`.`rel`,
  `or_relations`.`inverse_rel`,
  `parent_entity_uuid`.`uuid` AS `source_id`,
  `child_entity_uuid`.`uuid` AS `target_id`,
  `users`.`uuid` AS `user_id`,
  "Relation" as `type`
FROM `or_relations`
INNER JOIN `uuids` AS `parent_entity_uuid` ON `parent_entity_uuid`.`entity_id` = `or_relations`.`parent_entity_id`
  AND `parent_entity_uuid`.`entity_type` = `or_relations`.`parent_entity_type`
INNER JOIN `uuids` AS `child_entity_uuid` ON `child_entity_uuid`.`entity_id` = `or_relations`.`child_entity_id`
  AND `child_entity_uuid`.`entity_type` = `or_relations`.`child_entity_type`
INNER JOIN `users` ON `users`.`id` = `or_relations`.`user_id`
LEFT JOIN `or_projects` ON `or_projects`.`id` = `or_relations`.`project_id`
WHERE `or_relations`.`parent_entity_id` = :entity_id
  AND `or_relations`.`parent_entity_type` = :entity_type
  AND `or_relations`.`rel` = :rel

