-- :name find-admin-role :? :1
-- :doc Finds admin role for org zero
SELECT
  `roles`.`id`,
  `roles`.`name`
FROM `roles`
WHERE `roles`.`name` = 'Admin'
  AND `roles`.`organization_id` = 0

