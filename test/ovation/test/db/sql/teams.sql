-- :name create :insert
-- :doc Create team
INSERT INTO `teams` (
  `name`,
  `owner_id`,
  `uuid`,
  `created_at`,
  `updated_at`
)
VALUES (
  :name,
  :owner_id,
  :_id,
  now(),
  now()
)
