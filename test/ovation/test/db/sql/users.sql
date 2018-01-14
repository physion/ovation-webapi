-- :name create :insert
-- :doc Create user
INSERT INTO `users` (
  `uuid`,
  `first_name`,
  `last_name`,
  `created_at`,
  `updated_at`
)
VALUES (
  :_id,
  :first_name,
  :last_name,
  now(),
  now()
)
