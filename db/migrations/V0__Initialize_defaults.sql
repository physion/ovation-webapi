-- MySQL dump 10.13  Distrib 5.6.27, for osx10.11 (x86_64)
--
-- Host: localhost    Database: ovation_development
-- ------------------------------------------------------
-- Server version	5.6.27

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `activities`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `activities` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) DEFAULT NULL,
  `owner_id` int(11) DEFAULT NULL,
  `source_id` int(11) DEFAULT NULL,
  `source_type` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `uuid` varchar(36) COLLATE utf8_unicode_ci DEFAULT NULL,
  `activity_type` varchar(64) COLLATE utf8_unicode_ci DEFAULT NULL,
  `source_location` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `source_position` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `operation` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `destination_location` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `destination_position` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `destination_target` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `plating_configuration_id` int(11) DEFAULT NULL,
  `assay` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `workflow_activity_id` int(11) DEFAULT NULL,
  `custom_attributes` text COLLATE utf8_unicode_ci,
  `processed_resources` text COLLATE utf8_unicode_ci,
  `notes` text COLLATE utf8_unicode_ci,
  PRIMARY KEY (`id`),
  KEY `index_activities_on_uuid` (`uuid`),
  KEY `index_activities_on_activity_type` (`activity_type`)
) ENGINE=InnoDB AUTO_INCREMENT=495 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `admin_reports`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `admin_reports` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `organization_id` int(11) NOT NULL,
  `resource_id` int(11) DEFAULT NULL,
  `request_date` datetime NOT NULL,
  `report_type` varchar(255) NOT NULL,
  `search_params` varchar(255) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `status` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_admin_reports_on_user_id` (`user_id`),
  KEY `index_admin_reports_on_organization_id` (`organization_id`),
  KEY `index_admin_reports_on_resource_id` (`resource_id`),
  CONSTRAINT `fk_rails_667486d0b5` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_rails_7e2c7f60d3` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`id`),
  CONSTRAINT `fk_rails_92762aa62c` FOREIGN KEY (`resource_id`) REFERENCES `resources` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `app_settings`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `app_settings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) DEFAULT NULL,
  `value` text,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_app_settings_on_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `assay_results`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `assay_results` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `workflow_sample_result_id` int(11) DEFAULT NULL,
  `result` longtext,
  `removed` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `workflow_activity_id` int(11) DEFAULT NULL,
  `status` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_assay_results_on_workflow_sample_result_id` (`workflow_sample_result_id`),
  KEY `index_assay_results_on_removed` (`removed`),
  KEY `index_assay_results_on_workflow_activity_id` (`workflow_activity_id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `assets`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `assets` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `owner_id` int(11) NOT NULL,
  `team_id` int(11) NOT NULL,
  `project_uuid` varchar(36) COLLATE utf8_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `project_name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_assets_on_team_id_and_project_uuid` (`team_id`,`project_uuid`),
  KEY `index_assets_on_owner_id` (`owner_id`),
  KEY `index_assets_on_team_id` (`team_id`),
  KEY `index_assets_on_project_uuid` (`project_uuid`)
) ENGINE=InnoDB AUTO_INCREMENT=147 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `assigned_trainers`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `assigned_trainers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `training_pack_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_assigned_trainers_on_training_pack_id_and_user_id` (`training_pack_id`,`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `associated_report_configurations`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `associated_report_configurations` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `report_configuration_id` int(11) DEFAULT NULL,
  `project_id` int(11) DEFAULT NULL,
  `configuration` text,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `requisition_template_id` int(11) DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_project_report_template` (`project_id`,`report_configuration_id`,`requisition_template_id`),
  KEY `index_associated_report_configurations_on_project_id` (`project_id`),
  KEY `fk_rails_f9585052a9` (`report_configuration_id`),
  KEY `fk_rails_e61f4f618b` (`requisition_template_id`),
  KEY `index_associated_report_configurations_on_deleted_at` (`deleted_at`),
  CONSTRAINT `fk_rails_0668c613fc` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`),
  CONSTRAINT `fk_rails_e61f4f618b` FOREIGN KEY (`requisition_template_id`) REFERENCES `requisition_templates` (`id`),
  CONSTRAINT `fk_rails_f9585052a9` FOREIGN KEY (`report_configuration_id`) REFERENCES `report_configurations` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `audit_entries`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `audit_entries` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `type` varchar(64) COLLATE utf8_unicode_ci DEFAULT NULL,
  `url` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `method` varchar(16) COLLATE utf8_unicode_ci DEFAULT NULL,
  `user_id` int(11) NOT NULL DEFAULT '0',
  `class_name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `model_changes` mediumtext COLLATE utf8_unicode_ci,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `ip_addr` varchar(15) COLLATE utf8_unicode_ci DEFAULT NULL,
  `query_params` text COLLATE utf8_unicode_ci,
  `model_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_audit_entries_on_type` (`type`),
  KEY `index_audit_entries_on_url_and_method` (`url`,`method`),
  KEY `index_audit_entries_on_user_id` (`user_id`),
  KEY `index_audit_entries_on_class_name` (`class_name`),
  KEY `index_audit_entries_on_created_at` (`created_at`),
  KEY `index_audit_entries_on_model_id` (`model_id`)
) ENGINE=InnoDB AUTO_INCREMENT=116727 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `barcode_templates`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `barcode_templates` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `organization_id` int(11) DEFAULT NULL,
  `template` text COLLATE utf8_unicode_ci,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `format` varchar(16) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_barcode_templates_on_organization_id` (`organization_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `billing_informations`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `billing_informations` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `requisition_id` int(11) NOT NULL,
  `bill_to` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `date_of_injury` date DEFAULT NULL,
  `street` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `city` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `state` varchar(64) COLLATE utf8_unicode_ci DEFAULT NULL,
  `zip` varchar(10) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_billing_informations_on_requisition_id` (`requisition_id`)
) ENGINE=InnoDB AUTO_INCREMENT=140 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `box_integration_audits`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `box_integration_audits` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `resource_id` int(11) DEFAULT NULL,
  `box_integration_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `box_integration_mapping_id` int(11) DEFAULT NULL,
  `report_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_box_integration_audits_on_resource_id` (`resource_id`),
  KEY `index_box_integration_audits_on_box_integration_id` (`box_integration_id`),
  KEY `index_box_integration_audits_on_box_integration_mapping_id` (`box_integration_mapping_id`),
  KEY `index_box_integration_audits_on_report_id` (`report_id`),
  CONSTRAINT `fk_rails_547d86f261` FOREIGN KEY (`box_integration_mapping_id`) REFERENCES `box_integration_mappings` (`id`),
  CONSTRAINT `fk_rails_689f6e55af` FOREIGN KEY (`resource_id`) REFERENCES `resources` (`id`),
  CONSTRAINT `fk_rails_6fd2e5c721` FOREIGN KEY (`box_integration_id`) REFERENCES `box_integrations` (`id`),
  CONSTRAINT `fk_rails_76344c2fba` FOREIGN KEY (`report_id`) REFERENCES `reports` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `box_integration_mappings`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `box_integration_mappings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `panel_code` varchar(255) DEFAULT NULL,
  `requisition_template_id` int(11) DEFAULT NULL,
  `project_id` int(11) DEFAULT NULL,
  `owner_id` int(11) DEFAULT NULL,
  `configuration` text,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `organization_id` int(11) DEFAULT NULL,
  `mark_as_received` tinyint(1) DEFAULT NULL,
  `box_integration_id` int(11) DEFAULT NULL,
  `test_panel_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_box_integration_mappings_on_requisition_template_id` (`requisition_template_id`),
  KEY `index_box_integration_mappings_on_project_id` (`project_id`),
  KEY `index_box_integration_mappings_on_owner_id` (`owner_id`),
  KEY `index_box_integration_mappings_on_organization_id` (`organization_id`),
  KEY `index_box_integration_mappings_on_box_integration_id` (`box_integration_id`),
  KEY `index_box_integration_mappings_on_test_panel_id` (`test_panel_id`),
  CONSTRAINT `fk_rails_166a9b1e2b` FOREIGN KEY (`requisition_template_id`) REFERENCES `requisition_templates` (`id`),
  CONSTRAINT `fk_rails_1fa2b68ed9` FOREIGN KEY (`test_panel_id`) REFERENCES `test_panels` (`id`),
  CONSTRAINT `fk_rails_30d311304c` FOREIGN KEY (`box_integration_id`) REFERENCES `box_integrations` (`id`),
  CONSTRAINT `fk_rails_7ec07c182c` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_rails_b1a768ab21` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`),
  CONSTRAINT `fk_rails_dc1ab0e226` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `box_integrations`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `box_integrations` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) DEFAULT NULL,
  `organization_partner_id` int(11) DEFAULT NULL,
  `box_user_email` varchar(255) DEFAULT NULL,
  `box_user_name` varchar(255) DEFAULT NULL,
  `root_folder_id` varchar(255) DEFAULT NULL,
  `import_format` varchar(255) DEFAULT NULL,
  `export_format` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_box_integrations_on_organization_id` (`organization_id`),
  KEY `index_box_integrations_on_organization_partner_id` (`organization_partner_id`),
  CONSTRAINT `fk_rails_4bc447ac7a` FOREIGN KEY (`organization_partner_id`) REFERENCES `organization_partners` (`id`),
  CONSTRAINT `fk_rails_f00210e25c` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `contacts`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `contacts` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `email` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `containers`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `containers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) NOT NULL,
  `owner_id` int(11) NOT NULL,
  `uuid` varchar(36) COLLATE utf8_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `number_of_wells` int(11) NOT NULL,
  `current_location` varchar(64) COLLATE utf8_unicode_ci DEFAULT NULL,
  `type` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `barcode_label` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `origin_id` int(11) DEFAULT NULL,
  `origin_type` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `activity_name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `container_id` int(11) DEFAULT NULL,
  `samples_count` int(11) NOT NULL DEFAULT '0',
  `position` varchar(6) COLLATE utf8_unicode_ci DEFAULT NULL,
  `internal_position` int(11) DEFAULT NULL,
  `suffix` int(11) NOT NULL DEFAULT '0',
  `label` varchar(64) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_containers_on_container_id_and_internal_position` (`container_id`,`internal_position`),
  KEY `index_containers_on_organization_id` (`organization_id`),
  KEY `index_containers_on_type` (`type`),
  KEY `index_containers_on_origin_id` (`origin_id`),
  KEY `index_containers_on_organization_id_and_barcode_label_and_suffix` (`organization_id`,`barcode_label`,`suffix`),
  KEY `index_containers_on_container_id` (`container_id`)
) ENGINE=InnoDB AUTO_INCREMENT=634 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `default_organizations_for_users`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `default_organizations_for_users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `delayed_jobs`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `delayed_jobs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `priority` int(11) NOT NULL DEFAULT '0',
  `attempts` int(11) NOT NULL DEFAULT '0',
  `handler` longtext COLLATE utf8_unicode_ci NOT NULL,
  `last_error` longtext COLLATE utf8_unicode_ci,
  `run_at` datetime DEFAULT NULL,
  `locked_at` datetime DEFAULT NULL,
  `failed_at` datetime DEFAULT NULL,
  `locked_by` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `queue` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `owner_id` int(11) DEFAULT NULL,
  `owner_type` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `delayed_jobs_priority` (`priority`,`run_at`),
  KEY `index_delayed_jobs_on_owner_type_and_owner_id` (`owner_type`,`owner_id`)
) ENGINE=InnoDB AUTO_INCREMENT=136 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `deliveries`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `deliveries` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `project_id` int(11) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_deliveries_on_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `document_references`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `document_references` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `project_id` int(11) DEFAULT NULL,
  `document_id` int(11) DEFAULT NULL,
  `revision_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `entity_id` int(11) DEFAULT NULL,
  `entity_type` varchar(64) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_document_references_on_project_id` (`project_id`),
  KEY `index_document_references_on_entity_id_and_entity_type` (`entity_id`,`entity_type`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `documents`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `documents` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `type` varchar(64) COLLATE utf8_unicode_ci DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8_unicode_ci DEFAULT NULL,
  `entity_id` int(11) DEFAULT NULL,
  `entity_type` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `rev` int(11) NOT NULL DEFAULT '0',
  `owner_id` int(11) DEFAULT NULL,
  `resource_id` int(11) DEFAULT NULL,
  `resource_version` text COLLATE utf8_unicode_ci,
  `admin_signature_required` tinyint(1) NOT NULL DEFAULT '0',
  `customer_signature_required` tinyint(1) NOT NULL DEFAULT '0',
  `admin_signature` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `admin_signature_at` datetime DEFAULT NULL,
  `customer_signature` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `customer_signature_at` datetime DEFAULT NULL,
  `approved` tinyint(1) NOT NULL DEFAULT '0',
  `meta_data` text COLLATE utf8_unicode_ci,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `revision` tinyint(1) NOT NULL DEFAULT '0',
  `head_id` int(11) DEFAULT NULL,
  `author` varchar(64) COLLATE utf8_unicode_ci DEFAULT NULL,
  `effective` date DEFAULT NULL,
  `organization_id` int(11) DEFAULT NULL,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  `status` varchar(32) COLLATE utf8_unicode_ci NOT NULL DEFAULT 'no-signatures-required',
  PRIMARY KEY (`id`),
  KEY `index_documents_entity` (`entity_id`,`entity_type`),
  KEY `index_documents_approved` (`entity_id`,`entity_type`,`approved`),
  KEY `index_documents_on_head_id` (`head_id`),
  KEY `index_documents_on_organization_id` (`organization_id`),
  KEY `index_documents_on_deleted` (`deleted`),
  KEY `index_documents_on_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=552 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `downloads`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `downloads` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `url` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `os` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_downloads_on_active_and_os` (`active`,`os`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `event_processed_receipts`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `event_processed_receipts` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `event_route_id` int(11) DEFAULT NULL,
  `trigger` varchar(255) DEFAULT NULL,
  `args` text,
  `organization_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `success` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_event_processed_receipts_on_event_route_id` (`event_route_id`),
  KEY `index_event_processed_receipts_on_organization_id` (`organization_id`),
  CONSTRAINT `fk_rails_222e48ec18` FOREIGN KEY (`event_route_id`) REFERENCES `event_routes` (`id`),
  CONSTRAINT `fk_rails_dee4871c7a` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `event_routes`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `event_routes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `action_class` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `trigger` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `parameters` text COLLATE utf8_unicode_ci,
  `organization_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `github_updated_at` datetime DEFAULT NULL,
  `last_commit` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `removed_from_github` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `index_event_routes_on_trigger` (`trigger`),
  KEY `index_event_routes_on_organization_id` (`organization_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `events`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `events` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `event` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `data` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `timestamp` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_events_on_user_id` (`user_id`),
  KEY `index_events_on_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=20122 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `experiment_relations`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `experiment_relations` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `project_uuid` varchar(36) COLLATE utf8_unicode_ci NOT NULL,
  `experiment_uuid` varchar(36) COLLATE utf8_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_experiment_relations_on_project_uuid_and_experiment_uuid` (`project_uuid`,`experiment_uuid`),
  KEY `index_experiment_relations_on_project_uuid` (`project_uuid`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fax_requests`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `fax_requests` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `fax_destination` varchar(255) DEFAULT NULL,
  `document_id` int(11) DEFAULT NULL,
  `fax_credential` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_fax_requests_on_document_id` (`document_id`),
  CONSTRAINT `fk_rails_c62eeaed42` FOREIGN KEY (`document_id`) REFERENCES `documents` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `icd_codes`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `icd_codes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `code` varchar(16) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_icd_codes_on_organization_id_and_code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=69824 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `insurance_informations`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `insurance_informations` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `billing_information_id` int(11) NOT NULL,
  `insurance` varchar(128) DEFAULT NULL,
  `id_number` varchar(64) DEFAULT NULL,
  `group_number` varchar(64) DEFAULT NULL,
  `name_of_person_insured` varchar(255) DEFAULT NULL,
  `relationship_to_insured` varchar(32) DEFAULT NULL,
  `insurance_type` varchar(16) DEFAULT NULL,
  `dob_of_insured` date DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `insurance_provider_id` int(11) DEFAULT NULL,
  `subscriber_number` varchar(10) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_insurance_informations_on_billing_information_id` (`billing_information_id`),
  KEY `index_insurance_informations_on_insurance_provider_id` (`insurance_provider_id`),
  CONSTRAINT `fk_rails_faf86333f3` FOREIGN KEY (`insurance_provider_id`) REFERENCES `insurance_providers` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `insurance_providers`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `insurance_providers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) DEFAULT NULL,
  `code` varchar(16) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `clearing_house_payer_id` varchar(255) DEFAULT NULL,
  `kareo_id` int(11) DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `clearing_house` varchar(255) DEFAULT NULL,
  `kareo_payer_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_insurance_providers_on_organization_id_and_code` (`organization_id`,`code`),
  KEY `index_insurance_providers_on_kareo_id` (`kareo_id`),
  KEY `index_insurance_providers_on_active` (`active`),
  KEY `index_insurance_providers_on_clearing_house` (`clearing_house`),
  KEY `index_insurance_providers_on_clearing_house_payer_id` (`clearing_house_payer_id`)
) ENGINE=InnoDB AUTO_INCREMENT=122 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `invoice_items`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `invoice_items` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `invoice_id` int(11) DEFAULT NULL,
  `quantity` int(11) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `charged` tinyint(1) NOT NULL DEFAULT '0',
  `tier_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `unit_price_cents` int(11) DEFAULT NULL,
  `unit_price_currency` varchar(255) NOT NULL DEFAULT 'USD',
  `total_cents` int(11) DEFAULT NULL,
  `total_currency` varchar(255) NOT NULL DEFAULT 'USD',
  PRIMARY KEY (`id`),
  KEY `index_invoice_items_on_invoice_id` (`invoice_id`),
  KEY `index_invoice_items_on_tier_id` (`tier_id`),
  CONSTRAINT `fk_rails_25bf3d2c5e` FOREIGN KEY (`invoice_id`) REFERENCES `invoices` (`id`),
  CONSTRAINT `fk_rails_2d44816914` FOREIGN KEY (`tier_id`) REFERENCES `tiers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `invoices`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `invoices` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(255) DEFAULT NULL,
  `organization_id` int(11) DEFAULT NULL,
  `billing_contact_email` varchar(255) DEFAULT NULL,
  `date` date DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `total_cents` int(11) DEFAULT NULL,
  `total_currency` varchar(255) NOT NULL DEFAULT 'USD',
  `start_date` datetime DEFAULT NULL,
  `end_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_invoices_on_organization_id` (`organization_id`),
  CONSTRAINT `fk_rails_3a303bf667` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `line_items`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `line_items` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) DEFAULT NULL,
  `requisition_id` int(11) DEFAULT NULL,
  `activity_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `category` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `invoice_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_line_items_on_organization_id` (`organization_id`),
  KEY `index_line_items_on_requisition_id` (`requisition_id`),
  KEY `index_line_items_on_activity_id` (`activity_id`),
  KEY `index_line_items_on_category` (`category`),
  KEY `index_line_items_on_invoice_id` (`invoice_id`),
  CONSTRAINT `fk_rails_17929d70c4` FOREIGN KEY (`invoice_id`) REFERENCES `invoices` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `membership_roles`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `membership_roles` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `membership_id` int(11) DEFAULT NULL,
  `role_id` int(11) DEFAULT NULL,
  `team_id` int(11) DEFAULT NULL,
  `expires` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_user_roles_unique` (`membership_id`,`role_id`),
  KEY `index_user_roles_on_user_id` (`membership_id`),
  KEY `index_user_roles_on_role_id` (`role_id`),
  KEY `index_user_roles_on_team_id` (`team_id`)
) ENGINE=InnoDB AUTO_INCREMENT=318 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `memberships`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `memberships` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `team_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `license` tinyint(1) DEFAULT '0',
  `license_accepted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_memberships_on_team_id_and_user_id` (`team_id`,`user_id`),
  KEY `index_memberships_on_team_id` (`team_id`),
  KEY `index_memberships_on_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1352 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `menus`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `menus` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `route` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_menus_on_route` (`route`)
) ENGINE=InnoDB AUTO_INCREMENT=72 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notes`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `notes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `document_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `date` date DEFAULT NULL,
  `body` text COLLATE utf8_unicode_ci,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_notes_on_document_id` (`document_id`)
) ENGINE=InnoDB AUTO_INCREMENT=56 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notification_settings`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `notification_settings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `support_email` varchar(255) DEFAULT NULL,
  `physician_portal_url` varchar(255) DEFAULT NULL,
  `email_signature` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `logo_file_name` varchar(255) DEFAULT NULL,
  `logo_content_type` varchar(255) DEFAULT NULL,
  `logo_file_size` int(11) DEFAULT NULL,
  `logo_updated_at` datetime DEFAULT NULL,
  `organization_id` int(11) DEFAULT NULL,
  `smtp_address` varchar(255) DEFAULT NULL,
  `smtp_port` varchar(255) DEFAULT NULL,
  `smtp_username` varchar(255) DEFAULT NULL,
  `encrypted_smtp_password` varchar(255) DEFAULT NULL,
  `smtp_domain` varchar(255) DEFAULT NULL,
  `encrypted_smtp_password_salt` varchar(255) DEFAULT NULL,
  `encrypted_smtp_password_iv` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_notification_settings_on_organization_id` (`organization_id`),
  CONSTRAINT `fk_rails_16a5d87ff9` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notifications`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `notifications` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `notification_type` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `user_id` int(11) DEFAULT NULL,
  `body` text COLLATE utf8_unicode_ci,
  `read` tinyint(1) DEFAULT '0',
  `url` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `removed` tinyint(1) NOT NULL DEFAULT '0',
  `originator_id` int(11) DEFAULT NULL,
  `organization_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_notifications_on_user_id` (`user_id`),
  KEY `index_notifications_on_organization_id` (`organization_id`),
  CONSTRAINT `fk_rails_394d9847aa` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`id`),
  CONSTRAINT `fk_rails_b080fb4855` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `organization_group_memberships`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `organization_group_memberships` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_group_id` int(11) DEFAULT NULL,
  `organization_membership_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_organization_group_memberships_unique` (`organization_membership_id`,`organization_group_id`),
  KEY `idx_group_id_on_group_memberships` (`organization_group_id`),
  KEY `idx_membership_id_on_group_memberships` (`organization_membership_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `organization_groups`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `organization_groups` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) DEFAULT NULL,
  `name` varchar(50) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `logo_image` mediumtext,
  PRIMARY KEY (`id`),
  KEY `index_organization_groups_on_organization_id` (`organization_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `organization_memberships`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `organization_memberships` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `email` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `role` varchar(32) COLLATE utf8_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `job_title` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `contact_information` text COLLATE utf8_unicode_ci,
  `emergency_contact` text COLLATE utf8_unicode_ci,
  `status` varchar(8) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_organization_memberships_on_organization_id_and_user_id` (`organization_id`,`user_id`),
  UNIQUE KEY `index_organization_memberships_access` (`organization_id`,`user_id`,`role`),
  KEY `index_organization_memberships_on_organization_id` (`organization_id`),
  KEY `index_organization_memberships_on_email` (`email`),
  KEY `index_organization_memberships_on_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=40 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `organization_partners`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `organization_partners` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_organization_partners_on_organization_id` (`organization_id`),
  CONSTRAINT `fk_rails_01c9791cea` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `organization_report_configurations`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `organization_report_configurations` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) DEFAULT NULL,
  `report_configuration_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `org_report_configuration_id` (`organization_id`,`report_configuration_id`),
  KEY `index_organization_report_configurations_on_organization_id` (`organization_id`),
  KEY `fk_rails_84ecba040f` (`report_configuration_id`),
  CONSTRAINT `fk_rails_525ae165de` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`id`),
  CONSTRAINT `fk_rails_84ecba040f` FOREIGN KEY (`report_configuration_id`) REFERENCES `report_configurations` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `organizations`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `organizations` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) COLLATE utf8_unicode_ci NOT NULL,
  `name` varchar(128) COLLATE utf8_unicode_ci NOT NULL,
  `owner_id` int(11) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `default_barcode_template_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `custom_attributes` text COLLATE utf8_unicode_ci,
  `logo_image` mediumtext COLLATE utf8_unicode_ci,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  `deleted_at` datetime DEFAULT NULL,
  `deleted_by_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_organizations_on_uuid` (`uuid`),
  KEY `index_organizations_on_default_barcode_template_id` (`default_barcode_template_id`),
  KEY `index_organizations_on_user_id` (`user_id`),
  KEY `index_organizations_on_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `patients`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `patients` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `project_id` int(11) NOT NULL,
  `owner_id` int(11) NOT NULL,
  `patient_uuid` varchar(36) COLLATE utf8_unicode_ci DEFAULT NULL,
  `identifier` varchar(128) COLLATE utf8_unicode_ci DEFAULT NULL,
  `last_name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `first_name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `street_address` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `city` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `state` varchar(64) COLLATE utf8_unicode_ci DEFAULT NULL,
  `zip_code` varchar(10) COLLATE utf8_unicode_ci DEFAULT NULL,
  `date_of_birth` date DEFAULT NULL,
  `phone_number` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `gender` text COLLATE utf8_unicode_ci,
  `height` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `weight` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `ethnicity` text COLLATE utf8_unicode_ci,
  `physician_office_phone` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `office_contact` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `physician_npi` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `email` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `original_order_id` varchar(64) COLLATE utf8_unicode_ci DEFAULT NULL,
  `encrypted_pin` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `encrypted_pin_iv` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `encrypted_pin_salt` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `patient_id` varchar(64) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_patients_on_project_id` (`project_id`)
) ENGINE=InnoDB AUTO_INCREMENT=140 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pending_memberships`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `pending_memberships` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `team_id` int(11) NOT NULL,
  `email` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `role_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_pending_memberships_on_team_id_and_email` (`team_id`,`email`),
  KEY `index_pending_memberships_on_team_id` (`team_id`),
  KEY `index_pending_memberships_on_email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `permissions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `permissions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `tag_id` int(11) DEFAULT NULL,
  `role_id` int(11) DEFAULT NULL,
  `view` tinyint(1) NOT NULL DEFAULT '0',
  `read` tinyint(1) NOT NULL DEFAULT '0',
  `write` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `admin` tinyint(1) NOT NULL DEFAULT '0',
  `menu_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_permissions_on_tag_id_and_role_id` (`tag_id`,`role_id`),
  KEY `index_permissions_on_role_id_and_view` (`role_id`,`view`),
  KEY `index_permissions_on_role_id_and_read` (`role_id`,`read`),
  KEY `index_permissions_on_role_id_and_write` (`role_id`,`write`),
  KEY `index_permissions_on_admin` (`admin`),
  KEY `index_permissions_on_menu_id` (`menu_id`)
) ENGINE=InnoDB AUTO_INCREMENT=199 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `physician_contacts`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `physician_contacts` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) NOT NULL,
  `sales_account_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_physician_contacts_on_organization_id` (`organization_id`),
  KEY `index_physician_contacts_on_sales_account_id` (`sales_account_id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `physicians`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `physicians` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) NOT NULL,
  `office_phone` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `contact` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `npi` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `email` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `sales_account_id` int(11) DEFAULT NULL,
  `physician_contact_id` int(11) DEFAULT NULL,
  `archived` tinyint(1) NOT NULL DEFAULT '0',
  `source_type` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `source_id` int(11) DEFAULT NULL,
  `facility_name` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
  `facility_identifier` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_physicians_on_sales_account_id` (`sales_account_id`),
  KEY `index_physicians_on_organization_id_and_archived` (`organization_id`,`archived`),
  CONSTRAINT `fk_rails_36a5e8ee93` FOREIGN KEY (`sales_account_id`) REFERENCES `sales_accounts` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=76 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `plating_configurations`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `plating_configurations` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `configuration` mediumtext COLLATE utf8_unicode_ci,
  `organization_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `plate_type` varchar(64) COLLATE utf8_unicode_ci DEFAULT NULL,
  `well_position_column` varchar(64) COLLATE utf8_unicode_ci DEFAULT NULL,
  `sample_name_column` varchar(64) COLLATE utf8_unicode_ci DEFAULT NULL,
  `setup_skip_lines` int(11) NOT NULL DEFAULT '0',
  `uuid` varchar(36) COLLATE utf8_unicode_ci DEFAULT NULL,
  `archived_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_plating_configurations_on_uuid` (`uuid`),
  KEY `index_plating_configurations_on_organization_id` (`organization_id`),
  KEY `index_plating_configurations_on_archived_at` (`archived_at`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_memberships`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `project_memberships` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `project_id` varchar(36) COLLATE utf8_unicode_ci DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `role` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_project_memberships_on_project_id_and_user_id` (`project_id`,`user_id`),
  UNIQUE KEY `index_project_memberships_access` (`project_id`,`user_id`,`role`),
  KEY `index_project_memberships_on_project_id` (`project_id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_requisition_template_associations`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `project_requisition_template_associations` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) DEFAULT NULL,
  `project_id` int(11) DEFAULT NULL,
  `requisition_template_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_project_requisition_template_associations_unique` (`project_id`,`requisition_template_id`),
  KEY `index_project_requisition_template_associations_on_project` (`project_id`),
  KEY `index_project_requisition_template_associations_on_template` (`requisition_template_id`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_transfer_requests`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `project_transfer_requests` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `status` varchar(32) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_project_transfer_requests_on_organization_id` (`organization_id`),
  KEY `index_project_transfer_requests_on_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_transfers`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `project_transfers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `project_uuid` varchar(36) COLLATE utf8_unicode_ci DEFAULT NULL,
  `owner_uuid` varchar(36) COLLATE utf8_unicode_ci DEFAULT NULL,
  `new_owner_uuid` varchar(36) COLLATE utf8_unicode_ci DEFAULT NULL,
  `status` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `details` text COLLATE utf8_unicode_ci,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `status_details` text COLLATE utf8_unicode_ci,
  `temp_uuid` varchar(48) COLLATE utf8_unicode_ci DEFAULT NULL,
  `current_step` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `projects`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `projects` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `couch_project_id` varchar(36) COLLATE utf8_unicode_ci DEFAULT NULL,
  `organization_id` int(11) DEFAULT NULL,
  `owner_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `rev` int(11) DEFAULT NULL,
  `custom_attributes` text COLLATE utf8_unicode_ci,
  `source_project_id` int(11) DEFAULT NULL,
  `archived_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_meta_projects_on_project_id` (`couch_project_id`),
  KEY `index_projects_on_name` (`name`),
  KEY `index_projects_on_source_project_id` (`source_project_id`),
  KEY `index_projects_on_organization_id_and_archived_at` (`organization_id`,`archived_at`)
) ENGINE=InnoDB AUTO_INCREMENT=90 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `queue_assignments`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `queue_assignments` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `sample_id` int(11) NOT NULL,
  `name` varchar(50) DEFAULT NULL,
  `status` varchar(50) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_queue_assignments_on_sample_id_and_name` (`sample_id`,`name`),
  KEY `index_queue_assignments_on_sample_id` (`sample_id`),
  KEY `index_queue_assignments_on_name_and_status` (`name`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=588 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `queued_jobs`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `queued_jobs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) NOT NULL,
  `status` varchar(255) NOT NULL,
  `progress` int(11) DEFAULT NULL,
  `progress_max` int(11) DEFAULT NULL,
  `message` text,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `entity_type` varchar(255) DEFAULT NULL,
  `entity_id` int(11) DEFAULT NULL,
  `label` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_queued_jobs_on_organization_id` (`organization_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reimbursement_requests`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `reimbursement_requests` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `submitted` tinyint(1) DEFAULT '0',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `requisition_id` int(11) DEFAULT NULL,
  `adjusted_charges_cents` int(11) DEFAULT NULL,
  `adjusted_charges_currency` varchar(255) NOT NULL DEFAULT 'USD',
  `balance_cents` int(11) DEFAULT NULL,
  `balance_currency` varchar(255) NOT NULL DEFAULT 'USD',
  `posting_date` date DEFAULT NULL,
  `primary_insurance_plan_name` varchar(255) DEFAULT NULL,
  `primary_insurance_first_bill_date` date DEFAULT NULL,
  `primary_insurance_payment_posting_date` date DEFAULT NULL,
  `secondary_insurance_plan_name` varchar(255) DEFAULT NULL,
  `secondary_insurance_first_bill_date` date DEFAULT NULL,
  `secondary_insurance_payment_posting_date` date DEFAULT NULL,
  `patient_first_bill_date` date DEFAULT NULL,
  `patient_payment_posting_date` date DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `patient_payment_amount_cents` int(11) DEFAULT NULL,
  `patient_payment_amount_currency` varchar(255) NOT NULL DEFAULT 'USD',
  `patient_balance_cents` int(11) DEFAULT NULL,
  `patient_balance_currency` varchar(255) NOT NULL DEFAULT 'USD',
  `insurance_balance_cents` int(11) DEFAULT NULL,
  `insurance_balance_currency` varchar(255) NOT NULL DEFAULT 'USD',
  `primary_insurance_insurance_payment_cents` int(11) DEFAULT NULL,
  `primary_insurance_insurance_payment_currency` varchar(255) NOT NULL DEFAULT 'USD',
  `secondary_insurance_insurance_payment_cents` int(11) DEFAULT NULL,
  `secondary_insurance_insurance_payment_currency` varchar(255) NOT NULL DEFAULT 'USD',
  `charges_cents` int(11) DEFAULT NULL,
  `charges_currency` varchar(255) NOT NULL DEFAULT 'USD',
  `receipts_cents` int(11) DEFAULT NULL,
  `receipts_currency` varchar(255) NOT NULL DEFAULT 'USD',
  PRIMARY KEY (`id`),
  KEY `index_reimbursement_requests_on_submitted` (`submitted`),
  KEY `index_reimbursement_requests_on_requisition_id` (`requisition_id`),
  CONSTRAINT `fk_rails_20f35b766f` FOREIGN KEY (`requisition_id`) REFERENCES `requisitions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `remote_function_usages`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `remote_function_usages` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `remote_function_id` int(11) DEFAULT NULL,
  `entity_id` int(11) DEFAULT NULL,
  `entity_type` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_remote_function_usages_on_remote_function_id` (`remote_function_id`),
  CONSTRAINT `fk_rails_777e39e23c` FOREIGN KEY (`remote_function_id`) REFERENCES `remote_functions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `remote_functions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `remote_functions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `type` varchar(50) NOT NULL,
  `name` varchar(100) NOT NULL,
  `params` varchar(255) DEFAULT NULL,
  `function_url` varchar(200) NOT NULL,
  `description` text NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `report_configurations`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `report_configurations` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `interactor` varchar(255) DEFAULT NULL,
  `configuration` text,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `required` text,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `report_downloads`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `report_downloads` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `report_id` int(11) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_report_downloads_on_user_id` (`user_id`),
  KEY `index_report_downloads_on_report_id` (`report_id`),
  CONSTRAINT `fk_rails_a9c95d387c` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_rails_c1ba8c05ac` FOREIGN KEY (`report_id`) REFERENCES `reports` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `report_generation_requests`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `report_generation_requests` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `sample_id` int(11) NOT NULL,
  `request` text NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `report_generation_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_report_generation_requests_on_sample_id` (`sample_id`),
  KEY `index_report_generation_requests_on_report_generation_id` (`report_generation_id`),
  CONSTRAINT `fk_rails_75007c496e` FOREIGN KEY (`report_generation_id`) REFERENCES `report_generations` (`id`),
  CONSTRAINT `fk_rails_7f9ffebd69` FOREIGN KEY (`sample_id`) REFERENCES `samples` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `report_generations`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `report_generations` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `status` varchar(255) NOT NULL,
  `response` text,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `source_id` int(11) NOT NULL,
  `source_type` varchar(255) NOT NULL,
  `interactor_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_report_generations_on_source_id` (`source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reports`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `reports` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) DEFAULT NULL,
  `requisition_id` int(11) DEFAULT NULL,
  `status` varchar(30) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `resource_id` int(4) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_reports_on_organization_id` (`organization_id`),
  KEY `index_reports_on_requisition_id` (`requisition_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `requisition_template_associations`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `requisition_template_associations` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) DEFAULT NULL,
  `test_panel_id` int(11) DEFAULT NULL,
  `requisition_template_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_requisition_template_associations_uniqueness` (`test_panel_id`,`requisition_template_id`),
  KEY `index_requisition_template_associations_on_test_panel_id` (`test_panel_id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `requisition_templates`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `requisition_templates` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) DEFAULT NULL,
  `name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `description` text COLLATE utf8_unicode_ci,
  `template` text COLLATE utf8_unicode_ci,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `workflow_definition_id` int(11) DEFAULT NULL,
  `sample_type` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `project_id` int(11) DEFAULT NULL,
  `billing_interactor` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `billing_configuration` text COLLATE utf8_unicode_ci,
  `clinical` tinyint(1) DEFAULT '0',
  `component_name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `github_updated_at` datetime DEFAULT NULL,
  `last_commit` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `removed_from_github` tinyint(1) DEFAULT '0',
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_requisition_templates_on_organization_id` (`organization_id`),
  KEY `index_requisition_templates_on_workflow_definition_id` (`workflow_definition_id`),
  KEY `index_requisition_templates_on_project_id` (`project_id`),
  KEY `index_requisition_templates_on_clinical` (`clinical`),
  KEY `index_requisition_templates_on_deleted_at` (`deleted_at`),
  CONSTRAINT `fk_rails_a00194a5aa` FOREIGN KEY (`workflow_definition_id`) REFERENCES `workflow_definitions` (`id`),
  CONSTRAINT `fk_rails_dfb6e572fc` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `requisitions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `requisitions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `owner_id` int(11) NOT NULL,
  `uuid` varchar(36) COLLATE utf8_unicode_ci DEFAULT NULL,
  `identifier` varchar(128) COLLATE utf8_unicode_ci DEFAULT NULL,
  `sample_collection_date` date DEFAULT NULL,
  `requested_tests` text COLLATE utf8_unicode_ci,
  `diagnosis` text COLLATE utf8_unicode_ci,
  `contact_email` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `portal` tinyint(1) NOT NULL DEFAULT '0',
  `sample_type` varchar(128) COLLATE utf8_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `physician_id` int(11) DEFAULT NULL,
  `organization_id` int(11) DEFAULT NULL,
  `couch_project_id` varchar(36) COLLATE utf8_unicode_ci DEFAULT NULL,
  `medications` text COLLATE utf8_unicode_ci,
  `transferrer_id` int(11) DEFAULT NULL,
  `status` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `requisition_template_id` int(11) DEFAULT NULL,
  `complete` tinyint(1) DEFAULT '0',
  `sample_collected_by` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `sample_collection_time` varchar(8) COLLATE utf8_unicode_ci DEFAULT NULL,
  `medications_pt` text COLLATE utf8_unicode_ci,
  `custom_attributes` text COLLATE utf8_unicode_ci,
  `delivered_at` datetime DEFAULT NULL,
  `pregnant` tinyint(1) DEFAULT NULL,
  `pregnancy_due_date` date DEFAULT NULL,
  `breast_feeding` tinyint(1) DEFAULT NULL,
  `lifestyle_factors` text COLLATE utf8_unicode_ci,
  `recurrent_conditions` text COLLATE utf8_unicode_ci,
  `project_id` int(11) DEFAULT NULL,
  `accession_status` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `processing_status` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `reporting_status` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `billing_status` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `source_type` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `source_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_requisitions_on_identifier_and_organization_id` (`identifier`,`organization_id`),
  KEY `index_requisitions_on_transferrer_id` (`transferrer_id`),
  KEY `index_requisitions_on_status` (`status`),
  KEY `index_requisitions_on_requisition_template_id` (`requisition_template_id`),
  KEY `index_requisitions_on_complete` (`complete`),
  KEY `index_requisitions_on_organization_id` (`organization_id`),
  KEY `index_requisitions_on_project_id` (`project_id`),
  KEY `index_requisitions_on_created_at` (`created_at`),
  KEY `index_requisitions_on_delivered_at` (`delivered_at`),
  CONSTRAINT `fk_rails_32d082110b` FOREIGN KEY (`requisition_template_id`) REFERENCES `requisition_templates` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=181 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `research_plans`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `research_plans` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `plan_code` varchar(20) NOT NULL,
  `price_per_user_cents` int(11) NOT NULL,
  `price_per_hipaa_user_cents` int(11) NOT NULL,
  `base_data_allowed_tb` int(11) NOT NULL,
  `price_per_extra_tb_cents` int(11) NOT NULL,
  `num_free_trial_days` int(11) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `research_subscriptions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `research_subscriptions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) DEFAULT NULL,
  `research_plan_id` int(11) DEFAULT NULL,
  `start_date` date NOT NULL,
  `billing_contact_email` varchar(255) NOT NULL,
  `requires_hipaa` tinyint(1) NOT NULL DEFAULT '0',
  `num_users` int(11) NOT NULL,
  `num_extra_data_tb` int(11) NOT NULL DEFAULT '0',
  `status` varchar(255) NOT NULL,
  `payment_method` varchar(255) NOT NULL,
  `payment_gateway` varchar(255) DEFAULT NULL,
  `payment_gateway_subscription_uuid` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `credit_card_on_file` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `index_research_subscriptions_on_organization_id` (`organization_id`),
  KEY `index_research_subscriptions_on_research_plan_id` (`research_plan_id`),
  CONSTRAINT `fk_rails_20e2d51715` FOREIGN KEY (`research_plan_id`) REFERENCES `research_plans` (`id`),
  CONSTRAINT `fk_rails_638fa85739` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `resource_groups`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `resource_groups` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `resource_group_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `activity_id` varchar(36) DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  `validated` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `index_resource_groups_on_resource_group_id` (`resource_group_id`),
  KEY `index_resource_groups_on_activity_id` (`activity_id`),
  KEY `index_resource_groups_on_deleted_and_validated` (`deleted`,`validated`),
  CONSTRAINT `fk_rails_c3a00051ae` FOREIGN KEY (`resource_group_id`) REFERENCES `resource_groups` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=86 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `resources`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `resources` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `entity_id` varchar(36) COLLATE utf8_unicode_ci NOT NULL,
  `path` text COLLATE utf8_unicode_ci,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `version` text COLLATE utf8_unicode_ci,
  `label` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `source_id` int(11) DEFAULT NULL,
  `origin_id` int(11) DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  `resource_group_id` int(11) DEFAULT NULL,
  `validated` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `index_resources_on_user_id` (`user_id`),
  KEY `index_resources_on_origin_id` (`origin_id`),
  KEY `index_resources_on_resource_group_id` (`resource_group_id`),
  KEY `index_resources_on_entity_id_and_origin_id` (`entity_id`,`origin_id`),
  KEY `index_resources_on_deleted_and_validated` (`deleted`,`validated`),
  CONSTRAINT `fk_rails_6b4ece530c` FOREIGN KEY (`resource_group_id`) REFERENCES `resource_groups` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1106 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `retests`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `retests` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `sample_id` int(11) DEFAULT NULL,
  `requested_test` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `notes` text COLLATE utf8_unicode_ci,
  `status` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `requester_id` int(11) DEFAULT NULL,
  `organization_id` int(11) DEFAULT NULL,
  `preamp_sample_id` int(11) DEFAULT NULL,
  `sample_state_id` int(11) DEFAULT NULL,
  `preamp_sample_state_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_retests_on_sample_id` (`sample_id`),
  KEY `index_retests_on_status` (`status`),
  KEY `index_retests_on_requester_id` (`requester_id`),
  KEY `index_retests_on_organization_id` (`organization_id`),
  KEY `index_retests_on_preamp_sample_id` (`preamp_sample_id`),
  KEY `index_retests_on_sample_state_id` (`sample_state_id`),
  KEY `index_retests_on_preamp_sample_state_id` (`preamp_sample_state_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `roles`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `roles` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) DEFAULT NULL,
  `name` varchar(64) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_roles_name` (`organization_id`,`name`),
  KEY `index_roles_on_organization_id` (`organization_id`)
) ENGINE=InnoDB AUTO_INCREMENT=220 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sales_accounts`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `sales_accounts` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `primary_contact_name` varchar(255) DEFAULT NULL,
  `primary_contact_phone` varchar(32) DEFAULT NULL,
  `primary_contact_fax` varchar(32) DEFAULT NULL,
  `primary_contact_email` varchar(255) DEFAULT NULL,
  `secondary_contact_name` varchar(255) DEFAULT NULL,
  `secondary_contact_phone` varchar(32) DEFAULT NULL,
  `secondary_contact_fax` varchar(32) DEFAULT NULL,
  `secondary_contact_email` varchar(255) DEFAULT NULL,
  `street_address` varchar(255) DEFAULT NULL,
  `street_address_city` varchar(255) DEFAULT NULL,
  `street_address_state` varchar(32) DEFAULT NULL,
  `mailing_address` varchar(255) DEFAULT NULL,
  `mailing_address_city` varchar(255) DEFAULT NULL,
  `mailing_address_state` varchar(32) DEFAULT NULL,
  `mailing_address_zip_code` varchar(32) DEFAULT NULL,
  `street_address_zip_code` varchar(255) DEFAULT NULL,
  `organization_id` int(11) DEFAULT NULL,
  `sales_rep_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_sales_accounts_on_organization_id` (`organization_id`),
  KEY `index_sales_accounts_on_sales_rep_id` (`sales_rep_id`),
  CONSTRAINT `fk_rails_39473ffdf4` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`id`),
  CONSTRAINT `fk_rails_4f5e3b05e1` FOREIGN KEY (`sales_rep_id`) REFERENCES `sales_reps` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sales_reps`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `sales_reps` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `first_name` varchar(255) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `phone_number` varchar(32) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `organization_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_sales_reps_on_user_id` (`user_id`),
  KEY `index_sales_reps_on_organization_id` (`organization_id`),
  CONSTRAINT `fk_rails_74c3442db1` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`id`),
  CONSTRAINT `fk_rails_c4fab156ee` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sample_states`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `sample_states` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `container_id` int(11) NOT NULL,
  `sample_id` int(11) NOT NULL,
  `source_id` int(11) DEFAULT NULL,
  `activity_id` int(11) DEFAULT NULL,
  `position` varchar(12) DEFAULT NULL,
  `internal_position` int(11) DEFAULT NULL,
  `label` varchar(64) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `assay` varchar(255) DEFAULT NULL,
  `pool_index` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_sample_states_on_container_position` (`container_id`,`internal_position`,`pool_index`),
  KEY `index_sample_states_on_container_id` (`container_id`),
  KEY `index_sample_states_on_sample_id` (`sample_id`),
  KEY `index_sample_states_on_source_id` (`source_id`),
  KEY `index_sample_states_on_activity_id` (`activity_id`),
  KEY `index_sample_states_on_label` (`label`)
) ENGINE=InnoDB AUTO_INCREMENT=10267 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `samples`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `samples` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `owner_id` int(11) NOT NULL,
  `identifier` varchar(128) COLLATE utf8_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `organization_id` int(11) DEFAULT NULL,
  `requisition_id` int(11) DEFAULT NULL,
  `lot` varchar(64) COLLATE utf8_unicode_ci DEFAULT NULL,
  `result` text COLLATE utf8_unicode_ci,
  `patient_id` int(11) DEFAULT NULL,
  `control` tinyint(1) DEFAULT '0',
  `control_type` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `date_received` date DEFAULT NULL,
  `custom_attributes` text COLLATE utf8_unicode_ci,
  `received` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_samples_on_organization_id` (`organization_id`),
  KEY `index_samples_on_requisition_id` (`requisition_id`),
  KEY `index_samples_on_patient_id` (`patient_id`),
  KEY `index_samples_on_control` (`control`),
  KEY `index_samples_on_control_type` (`control_type`)
) ENGINE=InnoDB AUTO_INCREMENT=1356 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `schema_migrations`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `schema_migrations` (
  `version` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  UNIQUE KEY `unique_schema_migrations` (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `secure_host_credentials`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `secure_host_credentials` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `host` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `username` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `encrypted_password` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `encrypted_password_salt` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `encrypted_password_iv` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `organization_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_secure_host_credentials_on_organization_id` (`organization_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `signatures`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `signatures` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `document_id` int(11) DEFAULT NULL,
  `role_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `signed_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_signatures_on_document_id` (`document_id`)
) ENGINE=InnoDB AUTO_INCREMENT=185 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `signup_tokens`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `signup_tokens` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `token` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `active` tinyint(1) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_signup_tokens_on_token` (`token`),
  KEY `index_signup_tokens_on_active` (`active`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `site_settings`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `site_settings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) COLLATE utf8_unicode_ci DEFAULT NULL,
  `value` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_site_settings_on_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `subscriptions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `subscriptions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) DEFAULT NULL,
  `start_date` date NOT NULL,
  `billing_contact_email` varchar(255) NOT NULL,
  `status` varchar(255) NOT NULL,
  `payment_gateway` varchar(255) DEFAULT NULL,
  `payment_gateway_subscription_uuid` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `min_monthly_fee_cents` int(11) DEFAULT NULL,
  `min_monthly_fee_currency` varchar(255) NOT NULL DEFAULT 'USD',
  `price_per_box_integration_cents` int(11) DEFAULT NULL,
  `price_per_box_integration_currency` varchar(255) NOT NULL DEFAULT 'USD',
  PRIMARY KEY (`id`),
  KEY `index_subscriptions_on_organization_id` (`organization_id`),
  CONSTRAINT `fk_rails_364213cc3e` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `taggings`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `taggings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `entity_id` int(11) DEFAULT NULL,
  `entity_type` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `tag_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_taggings_entity_tag` (`entity_id`,`entity_type`,`tag_id`)
) ENGINE=InnoDB AUTO_INCREMENT=568 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tags`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `tags` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) DEFAULT NULL,
  `name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_tags_name` (`organization_id`,`name`)
) ENGINE=InnoDB AUTO_INCREMENT=120 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `team_groups`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `team_groups` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `team_id` int(11) NOT NULL,
  `organization_group_id` int(11) NOT NULL,
  `role_id` int(11) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_team_groups_on_team_id_and_organization_group_id` (`team_id`,`organization_group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `teams`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `teams` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `owner_id` int(11) NOT NULL,
  `name` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `description` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `uuid` varchar(36) COLLATE utf8_unicode_ci NOT NULL,
  `license_file_name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `license_content_type` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `license_file_size` int(11) DEFAULT NULL,
  `license_updated_at` datetime DEFAULT NULL,
  `entity_id` int(11) DEFAULT NULL,
  `entity_type` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `db_version` varchar(12) COLLATE utf8_unicode_ci DEFAULT '',
  `notifications` tinyint(1) NOT NULL DEFAULT '1',
  `database_provisioned` tinyint(1) NOT NULL DEFAULT '0',
  `replication` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_teams_on_uuid` (`uuid`),
  KEY `index_teams_on_owner_id` (`owner_id`),
  KEY `index_teams_on_name` (`name`),
  KEY `index_teams_entity` (`entity_id`,`entity_type`)
) ENGINE=InnoDB AUTO_INCREMENT=1247 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `template_user_roles`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `template_user_roles` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `role_id` int(11) DEFAULT NULL,
  `organization_id` int(11) DEFAULT NULL,
  `project_type` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `requisition_template_id` int(11) DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_template_user_roles_on_user_id` (`user_id`),
  KEY `index_template_user_roles_on_role_id` (`role_id`),
  KEY `index_template_user_roles_on_organization_id` (`organization_id`),
  KEY `index_template_user_roles_on_project_type` (`project_type`),
  KEY `index_template_user_roles_on_requisition_template_id` (`requisition_template_id`),
  KEY `index_template_user_roles_on_deleted_at` (`deleted_at`),
  CONSTRAINT `fk_rails_94eae982f3` FOREIGN KEY (`requisition_template_id`) REFERENCES `requisition_templates` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `test_gene_associations`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `test_gene_associations` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `test_panel_id` int(11) DEFAULT NULL,
  `test_gene_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `organization_id` int(11) DEFAULT NULL,
  `archived_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_test_gene_associations_panel_gene_uniqueness` (`test_panel_id`,`test_gene_id`),
  KEY `index_test_gene_associations_on_test_panel_id` (`test_panel_id`),
  KEY `index_test_gene_associations_on_test_gene_id` (`test_gene_id`),
  KEY `index_test_gene_associations_on_archived_at` (`archived_at`)
) ENGINE=InnoDB AUTO_INCREMENT=81 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `test_genes`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `test_genes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) NOT NULL,
  `name` varchar(64) DEFAULT NULL,
  `key` varchar(64) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `code` varchar(64) NOT NULL,
  `cpt_code` varchar(255) DEFAULT NULL,
  `archived_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_test_genes_on_organization_id_and_code` (`organization_id`,`code`),
  UNIQUE KEY `index_test_genes_on_organization_id_and_key` (`organization_id`,`key`),
  KEY `index_test_genes_on_organization_id` (`organization_id`),
  KEY `index_test_genes_on_archived_at` (`archived_at`)
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `test_panels`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `test_panels` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) NOT NULL,
  `name` varchar(64) DEFAULT NULL,
  `key` varchar(64) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `code` varchar(64) NOT NULL,
  `cpt_code` varchar(255) DEFAULT NULL,
  `z_code` varchar(255) DEFAULT NULL,
  `archived_at` datetime DEFAULT NULL,
  `delivery_attributes` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_test_panels_on_organization_id_and_code` (`organization_id`,`code`),
  UNIQUE KEY `index_test_panels_on_organization_id_and_key` (`organization_id`,`key`),
  KEY `index_test_panels_on_organization_id` (`organization_id`),
  KEY `index_test_panels_on_archived_at` (`archived_at`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tiers`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `tiers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) DEFAULT NULL,
  `min_samples` int(11) DEFAULT NULL,
  `max_samples` int(11) DEFAULT NULL,
  `sample_type` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `price_cents` int(11) DEFAULT NULL,
  `price_currency` varchar(255) NOT NULL DEFAULT 'USD',
  `number` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_tiers_on_organization_id` (`organization_id`),
  CONSTRAINT `fk_rails_b788c86919` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tokens`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `tokens` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `bearer` text NOT NULL,
  `expires_at` datetime DEFAULT NULL,
  `revoked` tinyint(1) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `label` varchar(255) NOT NULL,
  `token_scope` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_tokens_on_user_id` (`user_id`),
  KEY `index_tokens_on_revoked` (`revoked`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `training_packs`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `training_packs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `entity_id` int(11) NOT NULL,
  `entity_type` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `document_id` int(11) DEFAULT NULL,
  `document_revision_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `expires` date DEFAULT NULL,
  `recurring` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8_unicode_ci DEFAULT NULL,
  `organization_id` int(11) DEFAULT NULL,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  `role_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_training_packs_on_uuid` (`uuid`),
  KEY `index_training_packs_on_entity_id_and_entity_type` (`entity_id`,`entity_type`),
  KEY `index_training_packs_on_recurring` (`recurring`),
  KEY `index_training_packs_on_organization_id` (`organization_id`),
  KEY `index_training_packs_on_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=79 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `training_requirements`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `training_requirements` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `training_pack_id` int(11) NOT NULL,
  `role_id` int(11) NOT NULL,
  `read` tinyint(1) NOT NULL DEFAULT '0',
  `demonstrate` tinyint(1) NOT NULL DEFAULT '0',
  `perform` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_training_requirements_on_training_pack_id` (`training_pack_id`)
) ENGINE=InnoDB AUTO_INCREMENT=102 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `trainings`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `trainings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uid` varchar(36) COLLATE utf8_unicode_ci DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `training_pack_id` int(11) DEFAULT NULL,
  `role_id` int(11) DEFAULT NULL,
  `co_signed_at` datetime DEFAULT NULL,
  `co_signer_id` int(11) DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `organization_id` int(11) DEFAULT NULL,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_trainings_on_user_id_and_training_pack_id_and_role_id` (`user_id`,`training_pack_id`,`role_id`),
  KEY `index_trainings_on_user_id` (`user_id`),
  KEY `index_trainings_on_training_pack_id` (`training_pack_id`),
  KEY `index_trainings_on_co_signer_id` (`co_signer_id`),
  KEY `index_trainings_on_organization_id` (`organization_id`),
  KEY `index_trainings_on_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_preferences`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `user_preferences` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `send_mention_email` tinyint(1) NOT NULL DEFAULT '1',
  `send_daily_mention_email` tinyint(1) NOT NULL DEFAULT '0',
  `send_membership_changes_email` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `index_user_preferences_on_user_id` (`user_id`),
  CONSTRAINT `fk_rails_a69bfcfd81` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `email` varchar(255) COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
  `encrypted_password` varchar(255) COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
  `reset_password_token` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `reset_password_sent_at` datetime DEFAULT NULL,
  `remember_created_at` datetime DEFAULT NULL,
  `sign_in_count` int(11) DEFAULT '0',
  `current_sign_in_at` datetime DEFAULT NULL,
  `last_sign_in_at` datetime DEFAULT NULL,
  `current_sign_in_ip` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `last_sign_in_ip` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `confirmation_token` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `confirmed_at` datetime DEFAULT NULL,
  `confirmation_sent_at` datetime DEFAULT NULL,
  `unconfirmed_email` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `authentication_token` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `uuid` varchar(36) COLLATE utf8_unicode_ci DEFAULT NULL,
  `cloudant_key` varchar(64) COLLATE utf8_unicode_ci DEFAULT NULL,
  `cloudant_password` varchar(128) COLLATE utf8_unicode_ci DEFAULT NULL,
  `s3_access_key_id` varchar(128) COLLATE utf8_unicode_ci DEFAULT NULL,
  `s3_secret_access_key` varchar(128) COLLATE utf8_unicode_ci DEFAULT NULL,
  `s3_bucket_url` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `admin` tinyint(1) DEFAULT '0',
  `cloudant_read` tinyint(1) DEFAULT '0',
  `cloudant_write` tinyint(1) DEFAULT '0',
  `cloudant_create` tinyint(1) DEFAULT '0',
  `cloudant_admin` tinyint(1) DEFAULT '0',
  `password_salt` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `signup_token` varchar(64) COLLATE utf8_unicode_ci DEFAULT NULL,
  `first_ovation_download_at` datetime DEFAULT NULL,
  `last_ovation_download_at` datetime DEFAULT NULL,
  `first_cloudant_document_at` datetime DEFAULT NULL,
  `first_s3_document_at` datetime DEFAULT NULL,
  `locked_at` datetime DEFAULT NULL,
  `priority_access` tinyint(1) NOT NULL DEFAULT '0',
  `activated_at` datetime DEFAULT NULL,
  `winnebago` tinyint(1) NOT NULL DEFAULT '0',
  `db_version_identifier` varchar(12) COLLATE utf8_unicode_ci DEFAULT '',
  `password_changed_at` datetime DEFAULT NULL,
  `verified` tinyint(1) NOT NULL DEFAULT '0',
  `first_name` varchar(128) COLLATE utf8_unicode_ci DEFAULT NULL,
  `last_name` varchar(128) COLLATE utf8_unicode_ci DEFAULT NULL,
  `location` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `auth0_id` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `active_organization` text COLLATE utf8_unicode_ci,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_users_on_email` (`email`),
  UNIQUE KEY `index_users_on_reset_password_token` (`reset_password_token`),
  UNIQUE KEY `index_users_on_confirmation_token` (`confirmation_token`),
  UNIQUE KEY `index_users_on_authentication_token` (`authentication_token`),
  UNIQUE KEY `index_users_on_uuid` (`uuid`),
  UNIQUE KEY `index_users_on_auth0_id` (`auth0_id`),
  KEY `index_users_on_password_changed_at` (`password_changed_at`)
) ENGINE=InnoDB AUTO_INCREMENT=59 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `uuids`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `uuids` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `entity_id` int(11) DEFAULT NULL,
  `entity_type` varchar(36) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_uuids_on_uuid` (`uuid`)
) ENGINE=InnoDB AUTO_INCREMENT=3571 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `workflow_activities`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `workflow_activities` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `workflow_id` int(11) NOT NULL,
  `status` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `activity_name` varchar(64) DEFAULT NULL,
  `activity_type` varchar(128) DEFAULT NULL,
  `configuration` text,
  `label` varchar(64) DEFAULT NULL,
  `parent_labels` text,
  `removed_at` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_workflow_activities_on_workflow_label_status_removed_at` (`workflow_id`,`label`,`status`,`removed_at`),
  KEY `index_workflow_activities_on_workflow_id` (`workflow_id`),
  CONSTRAINT `fk_rails_2aab817c4a` FOREIGN KEY (`workflow_id`) REFERENCES `workflows` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=769 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `workflow_definitions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `workflow_definitions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `configuration` text NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `workflow_type` varchar(50) NOT NULL,
  `github_updated_at` datetime DEFAULT NULL,
  `last_commit` varchar(255) DEFAULT NULL,
  `removed_from_github` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_workflow_definitions_on_organization_id_and_workflow_type` (`organization_id`,`workflow_type`),
  KEY `index_workflow_definitions_on_organization_id` (`organization_id`),
  CONSTRAINT `fk_rails_8d004f53cf` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `workflow_sample_results`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `workflow_sample_results` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `workflow_sample_id` int(11) DEFAULT NULL,
  `result_type` varchar(50) NOT NULL,
  `status` varchar(50) DEFAULT NULL,
  `result` longtext,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `workflow_activity_id` int(11) DEFAULT NULL,
  `removed` tinyint(1) NOT NULL DEFAULT '0',
  `routing` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_workflow_sample_results_on_workflow_sample_id` (`workflow_sample_id`),
  KEY `index_workflow_sample_results_on_removed` (`removed`),
  KEY `index_workflow_sample_results_on_workflow_activity_id` (`workflow_activity_id`),
  CONSTRAINT `fk_rails_331ab4d47b` FOREIGN KEY (`workflow_sample_id`) REFERENCES `workflow_samples` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=125 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `workflow_samples`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `workflow_samples` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `workflow_id` int(11) NOT NULL,
  `sample_id` int(11) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_workflow_samples_on_workflow_id` (`workflow_id`),
  KEY `index_workflow_samples_on_sample_id` (`sample_id`),
  CONSTRAINT `fk_rails_38776b5457` FOREIGN KEY (`workflow_id`) REFERENCES `workflows` (`id`),
  CONSTRAINT `fk_rails_51363af456` FOREIGN KEY (`sample_id`) REFERENCES `samples` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=226 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `workflows`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `workflows` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) NOT NULL,
  `workflow_definition_id` int(11) NOT NULL,
  `status` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `archived_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_workflows_on_organization_id` (`organization_id`),
  KEY `index_workflows_on_workflow_definition_id` (`workflow_definition_id`),
  KEY `index_workflows_on_archived_at` (`archived_at`),
  CONSTRAINT `fk_rails_18ef8c3b59` FOREIGN KEY (`workflow_definition_id`) REFERENCES `workflow_definitions` (`id`),
  CONSTRAINT `fk_rails_477b8ee18d` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=83 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2017-10-11 23:31:48
