-- :name find-all
-- :doc Find all relations
select * from relations

-- :name find-all-by-uuid :? :*
-- :doc Find all relations by id
SELECT `or_relations`.`uuid` AS `_id`,
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
WHERE `or_relations`.`uuid` IN (:v*:ids)

-- :name find-all-by-parent-entity-rel :? :*
-- :doc Find all relations by parent entity and rel
SELECT `or_relations`.`uuid` AS `_id`,
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
WHERE `or_relations`.`parent_entity_id` = :entity_id
  AND `or_relations`.`parent_entity_type` = :entity_type
  AND `or_relations`.`rel` = :rel


