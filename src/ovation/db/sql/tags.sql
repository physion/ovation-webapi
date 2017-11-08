-- :name find-by-uuid :? :1
-- :doc Find first tag with ID
SELECT `or_tags`.*, "Tag" as `type` FROM `or_tags`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_tags`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
WHERE `or_tags`.`uuid` = :id
  AND `or_tags`.`organization_id` = :organization_id

-- :name find-all-by-uuid :? :*
-- :doc Find all tags by ID
SELECT `or_tags`.*, "Tag" as `type` FROM `or_tags`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_tags`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
WHERE `or_tags`.`uuid` IN (:v*:ids)
  AND `or_tags`.`organization_id` = :organization_id

