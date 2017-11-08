-- :name find-by-uuid :? :1
-- :doc Find first property with ID
SELECT `or_properties`.*, "Property" as `type` FROM `or_properties`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_properties`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
WHERE `or_properties`.`uuid` = :id
  AND `or_properties`.`organization_id` = :organization_id

-- :name find-all-by-uuid :? :*
-- :doc Find all properties by ID
SELECT `or_properties`.*, "Property" as `type` FROM `or_properties`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_properties`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
WHERE `or_properties`.`uuid` IN (:v*:ids)
  AND `or_properties`.`organization_id` = :organization_id

