-- {
--   "_id": "74b5498a-6560-4a00-bb56-9111bbab6296",
--   "permissions": {
--     "create": true,
--     "update": false,
--     "delete": false
--   },
--   "_rev": "4-310dbaa958762681d5c90ce7b9feff68",
--   "organization_id": 0,
--   "type": "Revision",
--   "api_version": 4,
--   "relationships": {
--     "file": {
--       "self": "/api/v1/o/0/revisions/74b5498a-6560-4a00-bb56-9111bbab6296/links/file/relationships",
--       "related": "/api/v1/o/0/revisions/74b5498a-6560-4a00-bb56-9111bbab6296/links/file"
--     },
--     "activities": {
--       "self": "/api/v1/o/0/revisions/74b5498a-6560-4a00-bb56-9111bbab6296/links/activities/relationships",
--       "related": "/api/v1/o/0/revisions/74b5498a-6560-4a00-bb56-9111bbab6296/links/activities"
--     },
--     "origins": {
--       "self": "/api/v1/o/0/revisions/74b5498a-6560-4a00-bb56-9111bbab6296/links/origins/relationships",
--       "related": "/api/v1/o/0/revisions/74b5498a-6560-4a00-bb56-9111bbab6296/links/origins"
--     },
--     "procedures": {
--       "self": "/api/v1/o/0/revisions/74b5498a-6560-4a00-bb56-9111bbab6296/links/procedures/relationships",
--       "related": "/api/v1/o/0/revisions/74b5498a-6560-4a00-bb56-9111bbab6296/links/procedures"
--     }
--   },
--   "attributes": {
--     "created-at": "2015-11-20T18:03:32.244Z",
--     "content_type": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
--     "name": "HealthTech DFT-P03 HL7 Specification - Billing Data.docx",
--     "file_id": "4cf67ade-49dd-463c-83eb-07e7d4c54f7e",
--     "updated-at": "2015-11-20T18:03:33.973Z",
--     "previous": [],
--     "url": "https://dev.ovation.io/resources/3350"
--   },
--   "owner": "15cab930-1e24-0131-026c-22000a977b96",
--   "links": {
--     "_collaboration_roots": [
--       "cd553b43-68bc-409c-9dc5-795af00e1c41"
--     ],
--     "notes": "/api/v1/o/0/entities/74b5498a-6560-4a00-bb56-9111bbab6296/annotations/notes",
--     "properties": "/api/v1/o/0/entities/74b5498a-6560-4a00-bb56-9111bbab6296/annotations/properties",
--     "tags": "/api/v1/o/0/entities/74b5498a-6560-4a00-bb56-9111bbab6296/annotations/tags",
--     "upload-complete": "/api/v1/o/0/revisions/74b5498a-6560-4a00-bb56-9111bbab6296/upload-complete",
--     "upload-failed": "/api/v1/o/0/revisions/74b5498a-6560-4a00-bb56-9111bbab6296/upload-failed",
--     "self": "/api/v1/o/0/revisions/74b5498a-6560-4a00-bb56-9111bbab6296",
--     "timeline-events": "/api/v1/o/0/entities/74b5498a-6560-4a00-bb56-9111bbab6296/annotations/timeline_events"
--   }
-- }


-- :name create :insert
-- :doc Create revision
INSERT INTO `or_revisions` (
  `uuid`,
  `organization_id`,
  `owner_id`,
  `file_id`,
  `resource_id`,
  `name`,
  `version`,
  `content_type`,
  `content_length`,
  `upload_status`,
  `url`,
  `attributes`,
  `archived`,
  `created_at`,
  `updated_at`
)
VALUES (
  :_id,
  :organization_id,
  :owner_id,
  :file_id,
  :resource_id,
  :name,
  :version,
  :content_type,
  :content_length,
  :upload-status,
  :url,
  :attributes,
  false,
  :created-at,
  :updated-at
)

-- :name update :! :n
-- :doc Update revision
UPDATE `or_revisions`
SET
  `or_revisions`.`name` = :name,
  `or_revisions`.`content_type` = :content_type,
  `or_revisions`.`content_length` = :content_length,
  `or_revisions`.`upload_status` = :upload-status,
  `or_revisions`.`url` = :url,
  `or_revisions`.`attributes` = :attributes,
  `or_revisions`.`updated_at` = :updated-at
WHERE
  `or_revisions`.`uuid` = :_id

-- :name archive :! :n
-- :doc Archive revision
UPDATE `or_revisions`
SET
  `or_revisions`.`archived` = :archived,
  `or_revisions`.`archived_at` = :archived_at,
  `or_revisions`.`archived_by_user_id` = :archived_by_user_id,
  `or_revisions`.`updated_at` = :updated-at
WHERE
  `or_revisions`.`uuid` = :_id

-- :name unarchive :! :n
-- :doc Unarchive revision
UPDATE `or_revisions`
SET
  `or_revisions`.`archived` = false,
  `or_revisions`.`archived_at` = NULL,
  `or_revisions`.`archived_by_user_id` = NULL,
  `or_revisions`.`updated_at` = :updated-at
WHERE
  `or_revisions`.`uuid` = :_id

-- :name update-resource-id :! :n
-- :doc Update resource id
UPDATE `or_revisions`
SET
  `or_revisions`.`resource_id` = :resource_id
WHERE `or_revisions`.`uuid` = :_id
  AND `or_revisions`.`organization_id` = :organization_id

-- :name delete :! :n
-- :doc Delete resource
DELETE FROM `or_revisions`
WHERE `or_revisions`.`uuid` = :_id
  AND `or_revisions`.`organization_id` = :organization_id

-- :name count :? :1
-- :doc Count revisions
SELECT COUNT(*) AS `count`
FROM `or_revisions`

-- :name find-all
-- :doc Find all revisions
select * from revisions

-- :name find-all-by-uuid :? :*
-- :doc Find all revisions by id
SELECT
  `or_revisions`.`id` AS `id`,
  `or_revisions`.`uuid` AS `_id`,
  `or_revisions`.`organization_id` AS `organization_id`,
  `or_projects`.`uuid` AS `project`,
  `users`.`uuid` AS `owner`,
  `or_revisions`.`name` AS `name`,
  `or_revisions`.`version` AS `version`,
  `or_revisions`.`content_type` AS `content_type`,
  `or_revisions`.`content_length` AS `content_length`,
  `or_revisions`.`upload_status` AS `upload-status`,
  `or_revisions`.`url` AS `url`,
  `or_revisions`.`created_at` AS `created-at`,
  `or_revisions`.`updated_at` AS `updated-at`,
  `or_revisions`.`attributes` AS `attributes`,
  `or_files`.`uuid` AS `file_id`,
  "Revision" as `type`
FROM `or_revisions`
INNER JOIN `or_files`    ON `or_files`.`id` = `or_revisions`.`file_id`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_files`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `users` ON `users`.`id` = `or_revisions`.`owner_id`
WHERE `or_revisions`.`uuid` IN (:v*:ids)
  AND `or_revisions`.`archived` = :archived
  AND `or_revisions`.`organization_id` = :organization_id


-- :name find-all-by-rel :? :*
-- :doc Find all revisions by entity and rel
SELECT
  `or_revisions`.`id` AS `id`,
  `or_revisions`.`uuid` AS `_id`,
  `or_revisions`.`organization_id` AS `organization_id`,
  `or_projects`.`uuid` AS `project`,
  `users`.`uuid` AS `owner`,
  `or_revisions`.`name` AS `name`,
  `or_revisions`.`version` AS `version`,
  `or_revisions`.`content_type` AS `content_type`,
  `or_revisions`.`content_length` AS `content_length`,
  `or_revisions`.`upload_status` AS `upload-status`,
  `or_revisions`.`url` AS `url`,
  `or_revisions`.`created_at` AS `created-at`,
  `or_revisions`.`updated_at` AS `updated-at`,
  `or_revisions`.`attributes` AS `attributes`,
  `or_files`.`uuid` AS `file_id`,
  "Revision" as `type`
FROM `or_revisions`
INNER JOIN `or_files`    ON `or_files`.`id` = `or_revisions`.`file_id`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_files`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `users` ON `users`.`id` = `or_revisions`.`owner_id`
INNER JOIN `or_relations` ON `or_relations`.`child_entity_id` = `or_revisions`.`id`
  AND `or_relations`.`child_entity_type` = 'Revision'
  AND `or_relations`.`parent_entity_id` = :entity_id
  AND `or_relations`.`parent_entity_type` = :entity_type
  AND `or_relations`.`rel` = :rel
WHERE `or_revisions`.`archived` = :archived
  AND `or_revisions`.`organization_id` = :organization_id


-- :name storage-by-project-for-public-org :? :*
-- :doc Returns storage by project for public organization
SELECT
  `or_projects`.`uuid` AS `id`,
  `or_revisions`.`organization_id` AS `organization_id`,
  COALESCE(SUM(`or_revisions`.`content_length`), 0) AS `usage`
FROM `or_revisions`
INNER JOIN `or_files`    ON `or_files`.`id` = `or_revisions`.`file_id`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_files`.`project_id`
  AND `or_projects`.`owner_id` = :owner_id
WHERE `or_revisions`.`archived` = false
  AND `or_revisions`.`organization_id` = 0
GROUP BY `or_projects`.`uuid`


-- :name storage-by-project-for-private-org :? :*
-- :doc Returns storage by project for private organization
SELECT
  `or_projects`.`uuid` AS `id`,
  `or_revisions`.`organization_id` AS `organization_id`,
  COALESCE(SUM(`or_revisions`.`content_length`), 0) AS `usage`
FROM `or_revisions`
INNER JOIN `or_files`    ON `or_files`.`id` = `or_revisions`.`file_id`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_files`.`project_id`
WHERE `or_revisions`.`archived` = false
  AND `or_revisions`.`organization_id` = :organization_id
GROUP BY `or_projects`.`uuid`


-- :name find-head-by-file-id :? :*
-- :doc Find HEAD revision for given file ID
SELECT
  `or_revisions`.`id` AS `id`,
  `or_revisions`.`uuid` AS `_id`,
  `or_revisions`.`organization_id` AS `organization_id`,
  `or_projects`.`uuid` AS `project`,
  `users`.`uuid` AS `owner`,
  `or_revisions`.`name` AS `name`,
  `or_revisions`.`version` AS `version`,
  `or_revisions`.`content_type` AS `content_type`,
  `or_revisions`.`content_length` AS `content_length`,
  `or_revisions`.`upload_status` AS `upload-status`,
  `or_revisions`.`url` AS `url`,
  `or_revisions`.`created_at` AS `created-at`,
  `or_revisions`.`updated_at` AS `updated-at`,
  `or_revisions`.`attributes` AS `attributes`,
  `or_files`.`uuid` AS `file_id`,
  "Revision" as `type`
FROM `or_revisions`
INNER JOIN `or_files`    ON `or_files`.`id` = `or_revisions`.`file_id`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_files`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `users` ON `users`.`id` = `or_revisions`.`owner_id`
WHERE `or_files`.`uuid` = :file_id
  AND `or_revisions`.`id` = `or_files`.`head_revision_id`
  AND `or_revisions`.`archived` = false
  AND `or_revisions`.`organization_id` = :organization_id
ORDER BY `or_revisions`.`id` DESC
LIMIT 1

