-- {
--   "_id": "754f5543-062c-408e-990f-d13b2041c5ef",
--   "permissions": {
--     "create": true,
--     "update": false,
--     "delete": false
--   },
--   "_rev": "2-1d2d836008c876182b8b0be63ef1ae09",
--   "organization_id": 0,
--   "type": "Source",
--   "api_version": 5,
--   "relationships": {
--     "children": {
--       "self": "/api/v1/o/0/sources/754f5543-062c-408e-990f-d13b2041c5ef/links/children/relationships",
--       "related": "/api/v1/o/0/sources/754f5543-062c-408e-990f-d13b2041c5ef/links/children"
--     },
--     "parents": {
--       "self": "/api/v1/o/0/sources/754f5543-062c-408e-990f-d13b2041c5ef/links/parents/relationships",
--       "related": "/api/v1/o/0/sources/754f5543-062c-408e-990f-d13b2041c5ef/links/parents"
--     },
--     "files": {
--       "self": "/api/v1/o/0/sources/754f5543-062c-408e-990f-d13b2041c5ef/links/files/relationships",
--       "related": "/api/v1/o/0/sources/754f5543-062c-408e-990f-d13b2041c5ef/links/files"
--     },
--     "revisions": {
--       "self": "/api/v1/o/0/sources/754f5543-062c-408e-990f-d13b2041c5ef/links/revisions/relationships",
--       "related": "/api/v1/o/0/sources/754f5543-062c-408e-990f-d13b2041c5ef/links/revisions"
--     },
--     "activities": {
--       "self": "/api/v1/o/0/sources/754f5543-062c-408e-990f-d13b2041c5ef/links/activities/relationships",
--       "related": "/api/v1/o/0/sources/754f5543-062c-408e-990f-d13b2041c5ef/links/activities"
--     },
--     "origins": {
--       "self": "/api/v1/o/0/sources/754f5543-062c-408e-990f-d13b2041c5ef/links/origins/relationships",
--       "related": "/api/v1/o/0/sources/754f5543-062c-408e-990f-d13b2041c5ef/links/origins"
--     }
--   },
--   "attributes": {
--     "name": "PQ Source 2",
--     "created-at": "2017-07-10T16:42:28.961Z",
--     "updated-at": "2017-07-10T16:43:33.856Z"
--   },
--   "owner": "a5e3e1f0-093f-4ab5-bee2-93079dcfd13e",
--   "links": {
--     "_collaboration_roots": [
--       "bb7a0827-c2f6-45d6-b55b-3bc3c038103f"
--     ],
--     "notes": "/api/v1/o/0/entities/754f5543-062c-408e-990f-d13b2041c5ef/annotations/notes",
--     "properties": "/api/v1/o/0/entities/754f5543-062c-408e-990f-d13b2041c5ef/annotations/properties",
--     "tags": "/api/v1/o/0/entities/754f5543-062c-408e-990f-d13b2041c5ef/annotations/tags",
--     "self": "/api/v1/o/0/sources/754f5543-062c-408e-990f-d13b2041c5ef",
--     "timeline-events": "/api/v1/o/0/entities/754f5543-062c-408e-990f-d13b2041c5ef/annotations/timeline_events"
--   }
-- }


-- :name create :insert
-- :doc Create source
INSERT INTO `or_sources` ()
VALUES ()


-- :name find-all :? :*
-- :doc Find all sources
-- :note ACL owner_id, project_id
SELECT
  `or_sources`.`uuid` AS `_id`,
  `or_sources`.`organization_id` AS `organization_id`,
  `users`.`uuid` AS `owner`,
  `or_sources`.`name` AS `name`,
  `or_sources`.`created_at` AS `created-at`,
  `or_sources`.`updated_at` AS `updated-id`,
  `or_sources`.`attributes` AS `attributes`,
  "Source" as `type`
FROM `or_sources`
INNER JOIN `users` ON `users`.`id` = `or_sources`.`owner_id`
LEFT JOIN `or_source_projects` ON `or_source_projects`.`source_id` = `or_sources`.`id`
LEFT JOIN `or_projects` ON `or_projects`.`id` = `or_source_projects`.`project_id`
LEFT JOIN `teams` ON `teams`.`id` = `or_projects`.`team_id`
WHERE `or_sources`.`archived` = :archived
  AND `or_sources`.`organization_id` = :organization_id
  AND (`teams`.`uuid` IN (:v*:team_uuids) OR `or_sources`.`owner_id` = :owner_id)


-- :name find-all-by-uuid :? :*
-- :doc Find all sources by id
SELECT
  `or_sources`.`uuid` AS `_id`,
  `or_sources`.`organization_id` AS `organization_id`,
  `users`.`uuid` AS `owner`,
  `or_sources`.`name` AS `name`,
  `or_sources`.`created_at` AS `created-at`,
  `or_sources`.`updated_at` AS `updated-id`,
  `or_sources`.`attributes` AS `attributes`,
  "Source" as `type`
FROM `or_sources`
INNER JOIN `users` ON `users`.`id` = `or_sources`.`owner_id`
LEFT JOIN `or_source_projects` ON `or_source_projects`.`source_id` = `or_sources`.`id`
LEFT JOIN `or_projects` ON `or_projects`.`id` = `or_source_projects`.`project_id`
LEFT JOIN `teams` ON `teams`.`id` = `or_projects`.`team_id`
WHERE `or_sources`.`uuid` IN (:v*:ids)
  AND `or_sources`.`archived` = :archived
  AND `or_sources`.`organization_id` = :organization_id
  AND (`teams`.`uuid` IN (:v*:team_uuids) OR `or_sources`.`owner_id` = :owner_id)


-- :name find-all-by-rel :? :*
-- :doc Find all sources by entity and rel
SELECT
  `or_sources`.`uuid` AS `_id`,
  `or_sources`.`organization_id` AS `organization_id`,
  `users`.`uuid` AS `owner`,
  `or_sources`.`name` AS `name`,
  `or_sources`.`created_at` AS `created-at`,
  `or_sources`.`updated_at` AS `updated-id`,
  `or_sources`.`attributes` AS `attributes`,
  "Source" as `type`
FROM `or_sources`
INNER JOIN `users` ON `users`.`id` = `or_sources`.`owner_id`
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
