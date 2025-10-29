USE `openisle`;
SET NAMES utf8mb4;

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `approved` bit(1) DEFAULT NULL,
  `avatar` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `display_medal` varchar(255) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `experience` int DEFAULT NULL,
  `introduction` text,
  `password` varchar(255) NOT NULL,
  `password_reset_code` varchar(255) DEFAULT NULL,
  `point` int DEFAULT NULL,
  `register_reason` text,
  `role` varchar(20) DEFAULT 'USER',
  `username` varchar(50) NOT NULL,
  `verification_code` varchar(255) DEFAULT NULL,
  `verified` bit(1) DEFAULT NULL,
  `is_bot` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_users_email` (`email`),
  UNIQUE KEY `UK_users_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` text,
  `icon` varchar(255) DEFAULT NULL,
  `name` varchar(50) NOT NULL,
  `small_icon` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_categories_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `tags` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `approved` bit(1) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text,
  `icon` varchar(255) DEFAULT NULL,
  `name` varchar(50) NOT NULL,
  `small_icon` varchar(255) DEFAULT NULL,
  `creator_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_tags_name` (`name`),
  KEY `FK_tags_creator` (`creator_id`),
  CONSTRAINT `FK_tags_creator` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
