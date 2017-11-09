-- :name find-by-uuid :? :1
-- :doc Find first tag with ID
SELECT
  `or_tags`.`uuid` AS `_id`,
  `or_tags`.`organization_id` AS `organization_id`,
  `users`.`uuid` AS `user`,
  `entity_uuid`.`uuid` AS `entity`,
  `or_tags`.`tag` AS `name`,
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
  `or_tags`.`tag` AS `name`,
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

