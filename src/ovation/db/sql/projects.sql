-- {
--   "_id": "cd553b43-68bc-409c-9dc5-795af00e1c41",
--   "permissions": {
--     "create": true,
--     "update": false,
--     "delete": false
--   },
--   "_rev": "31-ad92a29fe71ca894971656f5ac3eb35b",
--   "organization_id": 0,
--   "type": "Project",
--   "api_version": 4,
--   "relationships": {
--     "folders": {
--       "self": "/api/v1/o/0/projects/cd553b43-68bc-409c-9dc5-795af00e1c41/links/folders/relationships",
--       "related": "/api/v1/o/0/projects/cd553b43-68bc-409c-9dc5-795af00e1c41/links/folders"
--     },
--     "files": {
--       "self": "/api/v1/o/0/projects/cd553b43-68bc-409c-9dc5-795af00e1c41/links/files/relationships",
--       "related": "/api/v1/o/0/projects/cd553b43-68bc-409c-9dc5-795af00e1c41/links/files"
--     },
--     "activities": {
--       "self": "/api/v1/o/0/projects/cd553b43-68bc-409c-9dc5-795af00e1c41/links/activities/relationships",
--       "related": "/api/v1/o/0/projects/cd553b43-68bc-409c-9dc5-795af00e1c41/links/activities"
--     }
--   },
--   "attributes": {
--     "description": "HL7 integration",
--     "created-at": "2015-11-12T15:16:55.496Z",
--     "updated-at": "2015-11-20T18:03:30.755Z",
--     "name": "HealthTech integration"
--   },
--   "owner": "15cab930-1e24-0131-026c-22000a977b96",
--   "links": {
--     "_collaboration_roots": [],
--     "notes": "/api/v1/o/0/entities/cd553b43-68bc-409c-9dc5-795af00e1c41/annotations/notes",
--     "properties": "/api/v1/o/0/entities/cd553b43-68bc-409c-9dc5-795af00e1c41/annotations/properties",
--     "tags": "/api/v1/o/0/entities/cd553b43-68bc-409c-9dc5-795af00e1c41/annotations/tags",
--     "team": "/api/v1/o/0/teams/cd553b43-68bc-409c-9dc5-795af00e1c41",
--     "self": "/api/v1/o/0/projects/cd553b43-68bc-409c-9dc5-795af00e1c41",
--     "timeline-events": "/api/v1/o/0/entities/cd553b43-68bc-409c-9dc5-795af00e1c41/annotations/timeline_events"
--   }
-- }


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


-- :name find-all :? :*
-- :doc Find all projects
SELECT
  `or_projects`.`uuid` AS `_id`,
  `or_projects`.`organization_id` AS `organization_id`,
  `users`.`uuid` AS `owner`,
  `or_projects`.`name` AS `name`,
  `or_projects`.`created_at` AS `created-at`,
  `or_projects`.`updated_at` AS `updated-at`,
  `or_projects`.`attributes` AS `attributes`,
  "Project" AS `type`
FROM `or_projects`
INNER JOIN `teams` on `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `users` ON `users`.`id` = `or_projects`.`owner_id`
WHERE `or_projects`.`archived` = :archived
  AND `or_projects`.`organization_id` = :organization_id


-- :name find-by-uuid :? :1
-- :doc Find project by id
SELECT
  `or_projects`.`uuid` AS `_id`,
  `or_projects`.`organization_id` AS `organization_id`,
  `users`.`uuid` AS `owner`,
  `or_projects`.`name` AS `name`,
  `or_projects`.`created_at` AS `created-at`,
  `or_projects`.`updated_at` AS `updated-at`,
  `or_projects`.`attributes` AS `attributes`,
  "Project" AS `type`
FROM `or_projects`
INNER JOIN `teams` ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `users` ON `users`.`id` = `or_projects`.`owner_id`
WHERE `or_projects`.`uuid` = :id
  AND `or_projects`.`organization_id` = :organization_id


-- :name find-all-by-uuid :? :*
-- :doc Find all projects by id
SELECT
  `or_projects`.`uuid` AS `_id`,
  `or_projects`.`organization_id` AS `organization_id`,
  `users`.`uuid` AS `owner`,
  `or_projects`.`name` AS `name`,
  `or_projects`.`created_at` AS `created-at`,
  `or_projects`.`updated_at` AS `updated-at`,
  `or_projects`.`attributes` AS `attributes`,
  "Project" AS `type`
FROM `or_projects`
INNER JOIN `teams` on `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `users` ON `users`.`id` = `or_projects`.`owner_id`
WHERE `or_projects`.`uuid` IN (:v*:ids)
  AND `or_projects`.`archived` = :archived
  AND `or_projects`.`organization_id` = :organization_id


-- :name find-all-by-rel :? :*
-- :doc Find all projects by entity and rel
SELECT
  `or_projects`.`uuid` AS `_id`,
  `or_projects`.`organization_id` AS `organization_id`,
  `users`.`uuid` AS `owner`,
  `or_projects`.`name` AS `name`,
  `or_projects`.`created_at` AS `created-at`,
  `or_projects`.`updated_at` AS `updated-at`,
  `or_projects`.`attributes` AS `attributes`,
  "Project" AS `type`
FROM `or_projects`
INNER JOIN `teams` ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `users` ON `users`.`id` = `or_projects`.`owner_id`
INNER JOIN `or_relations` ON `or_relations`.`child_entity_id` = `or_projects`.`id`
  AND `or_relations`.`child_entity_type` = 'Project'
  AND `or_relations`.`parent_entity_id` = :entity_id
  AND `or_relations`.`parent_entity_type` = :entity_type
  AND `or_relations`.`rel` = :rel
WHERE `or_projects`.`archived` = :archived
  AND `or_projects`.`organization_id` = :organization_id
