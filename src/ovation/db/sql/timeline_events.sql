-- :name find-all
-- :doc Find all timeline_events
select * from timeline_events

-- :name find-by-uuid :? :1
-- :doc Find first tag with ID
SELECT `or_timeline_events`.*, "TimelineEvent" as `type` FROM `or_timeline_events`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_timeline_events`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
WHERE `or_timeline_events`.`uuid` = :id
  AND `or_timeline_events`.`organization_id` = :organization_id

-- :name find-all-by-uuid :? :*
-- :doc Find all timeline_events by ID
SELECT `or_timeline_events`.*, "TimelineEvent" as `type` FROM `or_timeline_events`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_timeline_events`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
WHERE `or_timeline_events`.`uuid` IN (:v*:ids)
  AND `or_timeline_events`.`organization_id` = :organization_id

