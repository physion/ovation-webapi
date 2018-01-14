-- :name create :insert
-- :doc Create new Team
INSERT INTO `teams` (
  `owner_id`,
  `name`,
  `uuid`,
  `entity_id`,
  `entity_type`,
  `created_at`,
  `updated_at`
)
VALUES (
  :owner_id,
  :name,
  :uuid,
  :entity_id,
  :entity_type,
  :created-at,
  :updated-at
)
