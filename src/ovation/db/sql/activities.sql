-- :name create :insert
-- :doc Create activities
INSERT INTO `or_activities` ()
VALUES ()


-- :name find-all :? :*
-- :doc Find all activities
SELECT `or_activities`.*, "Activity" as `type` FROM `or_activities`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_activities`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
WHERE `or_activities`.`archived` = :archived
  AND `or_activities`.`organization_id` = :organization_id


-- :name find-all-by-uuid :? :*
-- :doc Find all activities by id
SELECT `or_activities`.*, "Activity" as `type` FROM `or_activities`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_activities`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
WHERE `or_activities`.`uuid` IN (:v*:ids)
  AND `or_activities`.`archived` = :archived
  AND `or_activities`.`organization_id` = :organization_id


-- :name find-all-by-rel :? :*
-- :doc Find all activities by entity and rel
SELECT `or_activities`.*, "Activity" as `type` FROM `or_activities`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_activities`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `or_relations` ON `or_relations`.`child_entity_id` = `or_activities`.`id`
  AND `or_relations`.`child_entity_type` = 'Activity'
  AND `or_relations`.`parent_entity_id` = :entity_id
  AND `or_relations`.`parent_entity_type` = :entity_type
  AND `or_relations`.`rel` = :rel
WHERE `or_activities`.`archived` = :archived
  AND `or_activities`.`organization_id` = :organization_id

