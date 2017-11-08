-- :name create :insert
-- :doc Create source
INSERT INTO `or_sources` ()
VALUES ()


-- :name find-all :? :*
-- :doc Find all sources
-- :note ACL owner_id, project_id
SELECT `or_sources`.*, "Source" as `type` FROM `or_sources`
LEFT JOIN `or_source_projects` ON `or_source_projects`.`source_id` = `or_sources`.`id`
LEFT JOIN `or_projects` ON `or_projects`.`id` = `or_source_projects`.`project_id`
LEFT JOIN `teams` ON `teams`.`id` = `or_projects`.`team_id`
WHERE `or_sources`.`archived` = :archived
  AND `or_sources`.`organization_id` = :organization_id
  AND (`teams`.`uuid` IN (:v*:team_uuids) OR `or_sources`.`owner_id` = :owner_id)


-- :name find-all-by-uuid :? :*
-- :doc Find all sources by id
SELECT `or_sources`.*, "Source" as `type` FROM `or_sources`
LEFT JOIN `or_source_projects` ON `or_source_projects`.`source_id` = `or_sources`.`id`
LEFT JOIN `or_projects` ON `or_projects`.`id` = `or_source_projects`.`project_id`
LEFT JOIN `teams` ON `teams`.`id` = `or_projects`.`team_id`
WHERE `or_sources`.`uuid` IN (:v*:ids)
  AND `or_sources`.`archived` = :archived
  AND `or_sources`.`organization_id` = :organization_id
  AND (`teams`.`uuid` IN (:v*:team_uuids) OR `or_sources`.`owner_id` = :owner_id)


-- :name find-all-by-rel :? :*
-- :doc Find all sources by entity and rel
SELECT `or_sources`.*, "Source" as `type` FROM `or_sources`
LEFT JOIN `or_source_projects` ON `or_source_projects`.`source_id` = `or_sources`.`id`
LEFT JOIN `or_projects` ON `or_projects`.`id` = `or_source_projects`.`project_id`
LEFT JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
INNER JOIN `or_relations` ON `or_relations`.`child_entity_id` = `or_sources`.`id`
  AND `or_relations`.`child_entity_type` = 'Source'
  AND `or_relations`.`parent_entity_id` = :entity_id
  AND `or_relations`.`parent_entity_type` = :entity_type
  AND `or_relations`.`rel` = :rel
WHERE `or_sources`.`archived` = :archived
  AND `or_sources`.`organization_id` = :organization_id
  AND (`teams`.`uuid` IN (:v*:team_uuids) OR `or_sources`.`owner_id` = :owner_id)
