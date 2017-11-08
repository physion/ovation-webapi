-- :name create :insert
-- :doc Create new project
INSERT INTO or_notes (uuid, organization_id)
VALUES (:uuid, :organization_id)


-- :name find-by-uuid :? :1
-- :doc Find first note with ID
SELECT `or_notes`.*, "Note" as `type` FROM `or_notes`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_notes`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
WHERE `or_notes`.`uuid` = :id
  AND `or_notes`.`organization_id` = :organization_id

-- :name find-all-by-uuid :? :*
-- :doc Find all notes by ID
SELECT `or_notes`.*, "Note" as `type` FROM `or_notes`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_notes`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
WHERE `or_notes`.`uuid` IN (:v*:ids)
  AND `or_notes`.`organization_id` = :organization_id

