DROP TABLE IF EXISTS `character_costumes`;
CREATE TABLE `character_costumes`  (
  `player_id` int(10) UNSIGNED NOT NULL,
  `id` smallint(6) NOT NULL DEFAULT 0,
  `amount` smallint(6) NOT NULL DEFAULT 0,
  `locked` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`player_id`, `id`) USING BTREE,
  CONSTRAINT `character_costumes_ibfk_1` FOREIGN KEY (`player_id`) REFERENCES `characters` (`charId`) ON DELETE CASCADE ON UPDATE RESTRICT
) DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;