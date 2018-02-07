-- :name create :insert
-- :doc Create organization
INSERT INTO `organizations` (
  `name`,
  `uuid`,
  `owner_id`,
  `created_at`,
  `updated_at`
)
VALUES (
  :name,
  :_id,
  :owner_id,
  now(),
  now()
)
