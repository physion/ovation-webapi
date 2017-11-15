-- :name find-by-uuid :? :1
-- :doc Find record by uuid
SELECT
  `entity_id`,
  `entity_type`
FROM `uuids`
WHERE
  `uuids`.`uuid` = :uuid
