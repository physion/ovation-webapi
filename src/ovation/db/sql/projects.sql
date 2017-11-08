-- :name create :insert
-- :doc Create new project
INSERT INTO or_projects (uuid, organization_id, name, team_id, created_at, updated_at, owner_id)
VALUES (:uuid, :organization_id, :name, :team_id, now(), now(), :owner_id)


-- :name update :! :n
-- :doc Update project
UPDATE or_projects
SET name = :name,
    attributes = :attributes,
    updated_at = :updated_at
WHERE id = :id


-- :name find-by-uuid :? :1
-- :doc Find project by id
SELECT `or_projects`.*, "Project" as `type` FROM `or_projects`
INNER JOIN `teams` ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
WHERE `or_projects`.`uuid` = :id
  AND `or_projects`.`organization_id` = :organization_id


-- :name find-all-by-uuid :? :*
-- :doc Find all projects by id
SELECT `or_projects`.*, "Project" as `type` FROM `or_projects`
INNER JOIN `teams` on `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
WHERE `or_projects`.`uuid` IN (:v*:ids)
  AND `or_projects`.`archived` = :archived
  AND `or_projects`.`organization_id` = :organization_id


-- :name find-all :? :*
-- :doc Find all projects
SELECT `or_projects`.*, "Project" as `type` FROM `or_projects`
INNER JOIN `teams` on `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
WHERE `or_projects`.`archived` = :archived
  AND `or_projects`.`organization_id` = :organization_id


-- :name find-all-by-rel :? :*
-- :doc Find all projects by entity and rel
SELECT `or_projects`.*, "Project" as `type` FROM `or_projects`
INNER JOIN `teams` ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `or_relations` ON `or_relations`.`child_entity_id` = `or_projects`.`id`
  AND `or_relations`.`child_entity_type` = 'Project'
  AND `or_relations`.`parent_entity_id` = :entity_id
  AND `or_relations`.`parent_entity_type` = :entity_type
  AND `or_relations`.`rel` = :rel
WHERE `or_projects`.`archived` = :archived
  AND `or_projects`.`organization_id` = :organization_id

