-- :name find-all
-- :doc Find all revisions
select * from revisions

-- :name find-all-by-uuid :? :*
-- :doc Find all revisions by id
SELECT `or_revisions`.*, "Revision" as `type` FROM `or_revisions`
INNER JOIN `or_files`    ON `or_files`.`id` = `or_revisions`.`file_id`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_files`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
WHERE `or_revisions`.`uuid` IN (:v*:ids)
  AND `or_revisions`.`archived` = :archived
  AND `or_revisions`.`organization_id` = :organization_id


-- :name find-all-by-rel :? :*
-- :doc Find all revisions by entity and rel
SELECT `or_revisions`.*, "Revision" as `type` FROM `or_revisions`
INNER JOIN `or_files`    ON `or_files`.`id` = `or_revisions`.`file_id`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_files`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `or_relations` ON `or_relations`.`child_entity_id` = `or_revisions`.`id`
  AND `or_relations`.`child_entity_type` = 'Revision'
  AND `or_relations`.`parent_entity_id` = :entity_id
  AND `or_relations`.`parent_entity_type` = :entity_type
  AND `or_relations`.`rel` = :rel
WHERE `or_revisions`.`archived` = :archived
  AND `or_revisions`.`organization_id` = :organization_id

