DROP TABLE IF EXISTS `character_costume_collection`;
CREATE TABLE `character_costume_collection`  (
  `player_id` int(10) UNSIGNED NOT NULL,
  `id` smallint(6) NOT NULL DEFAULT 0,
  `reuse` bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`player_id`, `id`) USING BTREE,
  CONSTRAINT `character_costume_collection_ibfk_1` FOREIGN KEY (`player_id`) REFERENCES `characters` (`charId`) ON DELETE CASCADE ON UPDATE RESTRICT
) DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;