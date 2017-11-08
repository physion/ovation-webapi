-- {
--   "_id": "4cf67ade-49dd-463c-83eb-07e7d4c54f7e",
--   "permissions": {
--     "create": true,
--     "update": false,
--     "delete": false
--   },
--   "_rev": "5-4e7b2634cab369d7f4c6b3e21a9c48fc",
--   "organization_id": 0,
--   "type": "File",
--   "revisions": {},
--   "api_version": 4,
--   "relationships": {
--     "revisions": {
--       "self": "/api/v1/o/0/files/4cf67ade-49dd-463c-83eb-07e7d4c54f7e/links/revisions/relationships",
--       "related": "/api/v1/o/0/files/4cf67ade-49dd-463c-83eb-07e7d4c54f7e/links/revisions"
--     },
--     "head": {
--       "self": "/api/v1/o/0/files/4cf67ade-49dd-463c-83eb-07e7d4c54f7e/links/head/relationships",
--       "related": "/api/v1/o/0/files/4cf67ade-49dd-463c-83eb-07e7d4c54f7e/links/head"
--     },
--     "parents": {
--       "self": "/api/v1/o/0/files/4cf67ade-49dd-463c-83eb-07e7d4c54f7e/links/parents/relationships",
--       "related": "/api/v1/o/0/files/4cf67ade-49dd-463c-83eb-07e7d4c54f7e/links/parents"
--     },
--     "sources": {
--       "self": "/api/v1/o/0/files/4cf67ade-49dd-463c-83eb-07e7d4c54f7e/links/sources/relationships",
--       "related": "/api/v1/o/0/files/4cf67ade-49dd-463c-83eb-07e7d4c54f7e/links/sources"
--     }
--   },
--   "attributes": {
--     "name": "HealthTech DFT-P03 HL7 Specification - Billing Data.docx",
--     "created-at": "2015-11-20T18:03:28.344Z",
--     "updated-at": "2015-11-20T18:03:33.973Z"
--   },
--   "owner": "15cab930-1e24-0131-026c-22000a977b96",
--   "links": {
--     "heads": "/api/v1/o/0/files/4cf67ade-49dd-463c-83eb-07e7d4c54f7e/heads",
--     "_collaboration_roots": [
--       "cd553b43-68bc-409c-9dc5-795af00e1c41"
--     ],
--     "notes": "/api/v1/o/0/entities/4cf67ade-49dd-463c-83eb-07e7d4c54f7e/annotations/notes",
--     "properties": "/api/v1/o/0/entities/4cf67ade-49dd-463c-83eb-07e7d4c54f7e/annotations/properties",
--     "tags": "/api/v1/o/0/entities/4cf67ade-49dd-463c-83eb-07e7d4c54f7e/annotations/tags",
--     "self": "/api/v1/o/0/files/4cf67ade-49dd-463c-83eb-07e7d4c54f7e",
--     "timeline-events": "/api/v1/o/0/entities/4cf67ade-49dd-463c-83eb-07e7d4c54f7e/annotations/timeline_events"
--   }
-- }


-- :name find-all :? :*
-- :doc Find all files
SELECT
  `or_files`.`uuid` AS `_id`,
  `or_files`.`organization_id` AS `organization_id`,
  `or_projects`.`uuid` AS `project`,
  `or_files`.`name` AS `name`,
  `or_files`.`created_at` AS `created-at`,
  `or_files`.`updated_at` AS `updated-at`,
  `or_files`.`attributes` AS `attributes`,
  `users`.`uuid` AS `owner`,
  "File" as `type`
FROM `or_files`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_files`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `users` ON `users`.`id` = `or_files`.`owner_id`
WHERE `or_files`.`archived` = :archived
  AND `or_files`.`organization_id` = :organization_id


-- :name find-all-by-uuid :? :*
-- :doc Find all files by id
SELECT
  `or_files`.`uuid` AS `_id`,
  `or_files`.`organization_id` AS `organization_id`,
  `or_projects`.`uuid` AS `project`,
  `or_files`.`name` AS `name`,
  `or_files`.`created_at` AS `created-at`,
  `or_files`.`updated_at` AS `updated-at`,
  `or_files`.`attributes` AS `attributes`,
  `users`.`uuid` AS `owner`,
  "File" as `type`
FROM `or_files`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_files`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `users` ON `users`.`id` = `or_files`.`owner_id`
WHERE `or_files`.`uuid` IN (:v*:ids)
  AND `or_files`.`archived` = :archived
  AND `or_files`.`organization_id` = :organization_id


-- :name find-all-by-rel :? :*
-- :doc Find all files by entity and rel
SELECT
  `or_files`.`uuid` AS `_id`,
  `or_files`.`organization_id` AS `organization_id`,
  `or_projects`.`uuid` AS `project`,
  `or_files`.`name` AS `name`,
  `or_files`.`created_at` AS `created-at`,
  `or_files`.`updated_at` AS `updated-at`,
  `or_files`.`attributes` AS `attributes`,
  `users`.`uuid` AS `owner`,
  "File" as `type`
FROM `or_files`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_files`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `users` ON `users`.`id` = `or_files`.`owner_id`
INNER JOIN `or_relations` ON `or_relations`.`child_entity_id` = `or_files`.`id`
  AND `or_relations`.`child_entity_type` = 'File'
  AND `or_relations`.`parent_entity_id` = :entity_id
  AND `or_relations`.`parent_entity_type` = :entity_type
  AND `or_relations`.`rel` = :rel
WHERE `or_files`.`archived` = :archived
  AND `or_files`.`organization_id` = :organization_id

