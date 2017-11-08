-- :name find-all
-- :doc Find all timeline_events
select * from timeline_events

-- :name find-by-uuid :? :1
-- :doc Find first tag with ID
SELECT
  `or_timeline_events`.`uuid` AS `_id`,
  `or_timeline_events`.`organization_id` AS `organization_id`,
  `users`.`uuid` AS `user`,
  `entity_uuid`.`uuid` AS `entity`,
  `or_timeline_events`.`name` AS `name`,
  `or_timeline_events`.`notes` AS `notes`,
  `or_timeline_events`.`start` AS `start`,
  `or_timeline_events`.`end` AS `end`,
  "TimelineEvent" AS `annotation_type`,
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
  `or_timeline_events`.`uuid` AS `_id`,
  `or_timeline_events`.`organization_id` AS `organization_id`,
  `users`.`uuid` AS `user`,
  `entity_uuid`.`uuid` AS `entity`,
  `or_timeline_events`.`name` AS `name`,
  `or_timeline_events`.`notes` AS `notes`,
  `or_timeline_events`.`start` AS `start`,
  `or_timeline_events`.`end` AS `end`,
  "TimelineEvent" AS `annotation_type`,
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

