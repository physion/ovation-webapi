-- :name find-all :? :*
-- :doc Find all files
SELECT `or_files`.*, "File" as `type` FROM `or_files`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_files`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
WHERE `or_files`.`archived` = :archived
  AND `or_files`.`organization_id` = :organization_id


-- :name find-all-by-uuid :? :*
-- :doc Find all files by id
SELECT `or_files`.*, "File" as `type` FROM `or_files`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_files`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
WHERE `or_files`.`uuid` IN (:v*:ids)
  AND `or_files`.`archived` = :archived
  AND `or_files`.`organization_id` = :organization_id


-- :name find-all-by-rel :? :*
-- :doc Find all files by entity and rel
SELECT `or_files`.*, "File" as `type` FROM `or_files`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_files`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `or_relations` ON `or_relations`.`child_entity_id` = `or_files`.`id`
  AND `or_relations`.`child_entity_type` = 'File'
  AND `or_relations`.`parent_entity_id` = :entity_id
  AND `or_relations`.`parent_entity_type` = :entity_type
  AND `or_relations`.`rel` = :rel
WHERE `or_files`.`archived` = :archived
  AND `or_files`.`organization_id` = :organization_id

