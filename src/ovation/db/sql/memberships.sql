-- :name create :insert
-- :doc Create Membership
INSERT INTO `memberships` (
  `team_id`,
  `user_id`,
  `created_at`,
  `updated_at`
)
VALUES (
  :team_id,
  :user_id,
  :created-at,
  :updated-at
)

-- :name find-by :? :1
-- :doc Find membership by team_id and user_id
SELECT
  `memberships`.`id`,
  `memberships`.`team_id`,
  `memberships`.`user_id`,
  `memberships`.`created_at` AS `created-at`,
  `memberships`.`updated_at` AS `updated-at`,
FROM `memberships`
WHERE `memberships`.`team_id` = :team_id
  AND `memberships`.`user_id` = :user_id

