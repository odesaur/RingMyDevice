-- db_settings
CREATE TABLE IF NOT EXISTS `db_settings` (
  `id` integer PRIMARY KEY AUTOINCREMENT,
  `setting` text,
  `value` text
);
CREATE UNIQUE INDEX IF NOT EXISTS `idx_db_settings_setting` ON `db_settings` (`setting`);

-- rmd_users
CREATE TABLE IF NOT EXISTS `rmd_users` (
  `id` integer PRIMARY KEY AUTOINCREMENT,
  `uid` text,
  `salt` text,
  `hashed_password` text,
  `private_key` text,
  `public_key` text,
  `command_to_user` text,
  `command_time` integer,
  `command_sig` text,
  `push_url` text
);
CREATE UNIQUE INDEX IF NOT EXISTS `idx_rmd_users_uid` ON `rmd_users` (`uid`);

-- locations
CREATE TABLE IF NOT EXISTS `locations` (
  `id` integer PRIMARY KEY AUTOINCREMENT,
  `user_id` integer,
  `position` text,
  CONSTRAINT `fk_rmd_users_locations` FOREIGN KEY (`user_id`) REFERENCES `rmd_users` (`id`) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS `idx_rmd_locations_user_id` ON `locations` (`user_id`);

-- pictures
CREATE TABLE IF NOT EXISTS `pictures` (
  `id` integer PRIMARY KEY AUTOINCREMENT,
  `user_id` integer,
  `content` text,
  CONSTRAINT `fk_rmd_users_pictures` FOREIGN KEY (`user_id`) REFERENCES `rmd_users` (`id`) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS `idx_rmd_pictures_user_id` ON `pictures` (`user_id`);
