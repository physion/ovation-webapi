DROP USER IF EXISTS 'webapi';
CREATE USER 'webapi'@'%';

DROP DATABASE IF EXISTS ovation_development;
CREATE DATABASE IF NOT EXISTS ovation_development DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;
GRANT ALL ON ovation_development.* TO 'webapi'@'%' IDENTIFIED BY 'webapipass';
