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
INSERT INTO `or_sources` (
  `uuid`,
  `organization_id`,
  `project_id`,
  `owner_id`,
  `name`,
  `attributes`,
  `archived`,
  `created_at`,
  `updated_at`
)
VALUES (
  :_id,
  :organization_id,
  :project_id,
  :owner_id,
  :name,
  :attributes,
  false,
  :created-at,
  :updated-at
)

-- :name update :! :n
-- :doc Update source
UPDATE `or_sources`
SET
  `or_sources`.`name` = :name,
  `or_sources`.`attributes` = :attributes,
  `or_sources`.`updated_at` = :updated-at
WHERE `or_sources`.`uuid` = :_id
  AND `or_sources`.`organization_id` = :organization_id
  AND `or_sources`.`project_id` = :project_id

-- :name archive :! :n
-- :doc Archive source
UPDATE `or_sources`
SET
  `or_sources`.`archived` = :archived,
  `or_sources`.`archived_at` = :archived_at,
  `or_sources`.`archived_by_user_id` = :archived_by_user_id,
  `or_sources`.`updated_at` = :updated-at
WHERE `or_sources`.`uuid` = :_id
  AND `or_sources`.`organization_id` = :organization_id
  AND `or_sources`.`project_id` = :project_id

-- :name unarchive :! :n
-- :doc Unarchive source
UPDATE `or_sources`
SET
  `or_sources`.`archived` = false,
  `or_sources`.`archived_at` = NULL,
  `or_sources`.`archived_by_user_id` = NULL,
  `or_sources`.`updated_at` = :updated-at
WHERE `or_sources`.`uuid` = :_id
  AND `or_sources`.`organization_id` = :organization_id
  AND `or_sources`.`project_id` = :project_id

-- :name delete :! :n
-- :doc Delete source
DELETE FROM `or_sources`
WHERE `or_sources`.`uuid` = :_id
  AND `or_sources`.`organization_id` = :organization_id
  AND `or_sources`.`project_id` = :project_id

-- :name count :? :1
-- :doc Count sources
SELECT COUNT(*) AS `count`
FROM `or_sources`

-- :name find-all :? :*
-- :doc Find all sources
-- :note ACL owner_id, project_id
SELECT
  `or_sources`.`id` AS `id`,
  `or_sources`.`uuid` AS `_id`,
  `or_sources`.`organization_id` AS `organization_id`,
  `or_projects`.`id` AS `project_id`,
  `or_projects`.`uuid` AS `project`,
  `users`.`uuid` AS `owner`,
  `or_sources`.`name` AS `name`,
  `or_sources`.`created_at` AS `created-at`,
  `or_sources`.`updated_at` AS `updated-at`,
  `or_sources`.`attributes` AS `attributes`,
  "Source" as `type`
FROM `or_sources`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_sources`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND (`teams`.`uuid` IN (:v*:team_uuids) OR 1 = :service_account)
INNER JOIN `users` ON `users`.`id` = `or_sources`.`owner_id`
WHERE `or_sources`.`archived` = :archived
  AND `or_sources`.`organization_id` = :organization_id


-- :name find-all-by-uuid :? :*
-- :doc Find all sources by id
SELECT
  `or_sources`.`id` AS `id`,
  `or_sources`.`uuid` AS `_id`,
  `or_sources`.`organization_id` AS `organization_id`,
  `or_projects`.`id` AS `project_id`,
  `or_projects`.`uuid` AS `project`,
  `users`.`uuid` AS `owner`,
  `or_sources`.`name` AS `name`,
  `or_sources`.`created_at` AS `created-at`,
  `or_sources`.`updated_at` AS `updated-at`,
  `or_sources`.`attributes` AS `attributes`,
  "Source" as `type`
FROM `or_sources`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_sources`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND (`teams`.`uuid` IN (:v*:team_uuids) OR 1 = :service_account)
INNER JOIN `users` ON `users`.`id` = `or_sources`.`owner_id`
WHERE `or_sources`.`uuid` IN (:v*:ids)
  AND `or_sources`.`archived` = :archived
  AND `or_sources`.`organization_id` = :organization_id


-- :name find-all-by-rel :? :*
-- :doc Find all sources by entity and rel
SELECT
  `or_sources`.`id` AS `id`,
  `or_sources`.`uuid` AS `_id`,
  `or_sources`.`organization_id` AS `organization_id`,
  `or_projects`.`id` AS `project_id`,
  `or_projects`.`uuid` AS `project`,
  `users`.`uuid` AS `owner`,
  `or_sources`.`name` AS `name`,
  `or_sources`.`created_at` AS `created-at`,
  `or_sources`.`updated_at` AS `updated-at`,
  `or_sources`.`attributes` AS `attributes`,
  "Source" as `type`
FROM `or_sources`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_sources`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND (`teams`.`uuid` IN (:v*:team_uuids) OR 1 = :service_account)
INNER JOIN `users` ON `users`.`id` = `or_sources`.`owner_id`
LEFT JOIN `or_relations` AS `rel` ON `rel`.`target_id` = `or_sources`.`id`
  AND `rel`.`target_type` = 'Source'
  AND `rel`.`source_id` = :entity_id
  AND `rel`.`source_type` = :entity_type
  AND `rel`.`rel` = :rel
LEFT JOIN `or_relations` AS `inverse_rel` ON `inverse_rel`.`source_id` = `or_sources`.`id`
  AND `inverse_rel`.`source_type` = 'Source'
  AND `inverse_rel`.`target_id` = :entity_id
  AND `inverse_rel`.`target_type` = :entity_type
  AND `inverse_rel`.`inverse_rel` = :rel
WHERE `or_sources`.`archived` = :archived
  AND `or_sources`.`organization_id` = :organization_id
  AND (`rel`.`id` OR `inverse_rel`.`id`)

