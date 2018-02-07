-- :name create :insert
-- :doc Create new UUID
INSERT INTO `uuids` (
  `uuid`,
  `created_at`,
  `updated_at`
)
VALUES (
  :uuid,
  :created-at,
  :updated-at
)

-- :name update-entity :! :n
-- :doc Update entity
UPDATE `uuids`
SET
  `uuids`.`entity_id` = :entity_id,
  `uuids`.`entity_type` = :entity_type,
  `uuids`.`updated_at` = :updated-at
WHERE
  `uuids`.`uuid` = :uuid

-- :name find-by-uuid :? :1
-- :doc Find record by uuid
SELECT
  `entity_id`,
  `entity_type`
FROM `uuids`
WHERE
  `uuids`.`uuid` = :uuid
