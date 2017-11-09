-- :name find-by-uuid :? :1
-- :doc Find first property with ID
SELECT
  `or_properties`.`uuid` AS `_id`,
  `or_properties`.`organization_id` AS `organization_id`,
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
  `or_properties`.`uuid` AS `_id`,
  `or_properties`.`organization_id` AS `organization_id`,
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

