-- {
--   "_id": "4b6de6e8-502b-40ed-95eb-4e4f3fc0ab40",
--   "permissions": {
--     "create": true,
--     "update": false,
--     "delete": false
--   },
--   "_rev": "5-43ee32b0e39865961e48e9c22a3c3043",
--   "organization_id": 0,
--   "type": "Activity",
--   "api_version": 4,
--   "relationships": {
--     "inputs": {
--       "self": "/api/v1/o/0/activities/4b6de6e8-502b-40ed-95eb-4e4f3fc0ab40/links/inputs/relationships",
--       "related": "/api/v1/o/0/activities/4b6de6e8-502b-40ed-95eb-4e4f3fc0ab40/links/inputs"
--     },
--     "outputs": {
--       "self": "/api/v1/o/0/activities/4b6de6e8-502b-40ed-95eb-4e4f3fc0ab40/links/outputs/relationships",
--       "related": "/api/v1/o/0/activities/4b6de6e8-502b-40ed-95eb-4e4f3fc0ab40/links/outputs"
--     },
--     "actions": {
--       "self": "/api/v1/o/0/activities/4b6de6e8-502b-40ed-95eb-4e4f3fc0ab40/links/actions/relationships",
--       "related": "/api/v1/o/0/activities/4b6de6e8-502b-40ed-95eb-4e4f3fc0ab40/links/actions"
--     },
--     "operators": {
--       "self": "/api/v1/o/0/activities/4b6de6e8-502b-40ed-95eb-4e4f3fc0ab40/links/operators/relationships",
--       "related": "/api/v1/o/0/activities/4b6de6e8-502b-40ed-95eb-4e4f3fc0ab40/links/operators"
--     },
--     "parents": {
--       "self": "/api/v1/o/0/activities/4b6de6e8-502b-40ed-95eb-4e4f3fc0ab40/links/parents/relationships",
--       "related": "/api/v1/o/0/activities/4b6de6e8-502b-40ed-95eb-4e4f3fc0ab40/links/parents"
--     }
--   },
--   "attributes": {
--     "name": "New rev",
--     "created-at": "2016-01-26T16:12:55.280Z",
--     "updated-at": "2016-01-26T16:39:13.815Z"
--   },
--   "owner": "15cab930-1e24-0131-026c-22000a977b96",
--   "links": {
--     "_collaboration_roots": [
--       "2dbd4384-d9ca-4fbd-aa18-43655271917b"
--     ],
--     "notes": "/api/v1/o/0/entities/4b6de6e8-502b-40ed-95eb-4e4f3fc0ab40/annotations/notes",
--     "properties": "/api/v1/o/0/entities/4b6de6e8-502b-40ed-95eb-4e4f3fc0ab40/annotations/properties",
--     "tags": "/api/v1/o/0/entities/4b6de6e8-502b-40ed-95eb-4e4f3fc0ab40/annotations/tags",
--     "zip": "/api/v1/o/0/zip/activities/4b6de6e8-502b-40ed-95eb-4e4f3fc0ab40",
--     "self": "/api/v1/o/0/activities/4b6de6e8-502b-40ed-95eb-4e4f3fc0ab40",
--     "timeline-events": "/api/v1/o/0/entities/4b6de6e8-502b-40ed-95eb-4e4f3fc0ab40/annotations/timeline_events"
--   }
-- }


-- :name create :insert
-- :doc Create activities
INSERT INTO `or_activities` (
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
-- :doc Update activity
UPDATE `or_activities`
SET
  `or_activities`.`name` = :name,
  `or_activities`.`attributes` = :attributes,
  `or_activities`.`archived` = :archived,
  `or_activities`.`archived_at` = :archived_at,
  `or_activities`.`archived_by_user_id` = :archived_by_user_id,
  `or_activities`.`updated_at` = :updated-at
WHERE `or_activities`.`uuid` = :_id
  AND `or_activities`.`organization_id` = :organization_id
  AND `or_activities`.`project_id` = :project_id

-- :name delete :! :n
-- :doc Delete activity
DELETE FROM `or_activities`
WHERE `or_activities`.`uuid` = :_id
  AND `or_activities`.`organization_id` = :organization_id
  AND `or_activities`.`project_id` = :project_id

-- :name count :? :1
-- :doc Count activities
SELECT COUNT(*) AS `count`
FROM `or_activities`

-- :name find-all :? :*
-- :doc Find all activities
SELECT
  `or_activities`.`uuid` AS `_id`,
  `or_activities`.`organization_id` AS `organization_id`,
  `or_projects`.`uuid` AS `project`,
  `users`.`uuid` AS `owner`,
  `or_activities`.`name` AS `name`,
  `or_activities`.`created_at` AS `created-at`,
  `or_activities`.`updated_at` AS `updated-at`,
  `or_activities`.`attributes` AS `attributes`,
  "Activity" AS `type`
FROM `or_activities`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_activities`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `users` ON `users`.`id` = `or_activities`.`owner_id`
WHERE `or_activities`.`archived` = :archived
  AND `or_activities`.`organization_id` = :organization_id


-- :name find-all-by-uuid :? :*
-- :doc Find all activities by id
SELECT
  `or_activities`.`uuid` AS `_id`,
  `or_activities`.`organization_id` AS `organization_id`,
  `or_projects`.`uuid` AS `project`,
  `users`.`uuid` AS `owner`,
  `or_activities`.`name` AS `name`,
  `or_activities`.`created_at` AS `created-at`,
  `or_activities`.`updated_at` AS `updated-at`,
  `or_activities`.`attributes` AS `attributes`,
  "Activity" AS `type`
FROM `or_activities`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_activities`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `users` ON `users`.`id` = `or_activities`.`owner_id`
WHERE `or_activities`.`uuid` IN (:v*:ids)
  AND `or_activities`.`archived` = :archived
  AND `or_activities`.`organization_id` = :organization_id


-- :name find-all-by-rel :? :*
-- :doc Find all activities by entity and rel
SELECT
  `or_activities`.`uuid` AS `_id`,
  `or_activities`.`organization_id` AS `organization_id`,
  `or_projects`.`uuid` AS `project`,
  `users`.`uuid` AS `owner`,
  `or_activities`.`name` AS `name`,
  `or_activities`.`created_at` AS `created-at`,
  `or_activities`.`updated_at` AS `updated-at`,
  `or_activities`.`attributes` AS `attributes`,
  "Activity" AS `type`
FROM `or_activities`
INNER JOIN `or_projects` ON `or_projects`.`id` = `or_activities`.`project_id`
INNER JOIN `teams`       ON `teams`.`id` = `or_projects`.`team_id`
  AND `teams`.`uuid` IN (:v*:team_uuids)
INNER JOIN `users` ON `users`.`id` = `or_activities`.`owner_id`
INNER JOIN `or_relations` ON `or_relations`.`child_entity_id` = `or_activities`.`id`
  AND `or_relations`.`child_entity_type` = 'Activity'
  AND `or_relations`.`parent_entity_id` = :entity_id
  AND `or_relations`.`parent_entity_type` = :entity_type
  AND `or_relations`.`rel` = :rel
WHERE `or_activities`.`archived` = :archived
  AND `or_activities`.`organization_id` = :organization_id

