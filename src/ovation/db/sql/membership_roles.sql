-- :name create :insert
-- :doc Create Membership role
INSERT INTO `membership_roles` (
  `team_id`,
  `membership_id`,
  `role_id`
)
VALUES (
  :team_id,
  :membership_id,
  :role_id
)

-- :name find-by :? :1
-- :doc Find membership role by team_id, membership_id and role_id
SELECT
  `membership_roles`.`id`,
  `membership_roles`.`team_id`,
  `membership_roles`.`membership_id`,
  `membership_roles`.`role_id`,
  `membership_roles`.`created_at` AS `created-at`,
  `membership_roles`.`updated_at` AS `updated-at`,
FROM `membership_roles`
WHERE `membership_roles`.`team_id` = :team_id
  AND `membership_roles`.`membership_id` = :membership_id
  AND `membership_roles`.`role_id` = :role_id

