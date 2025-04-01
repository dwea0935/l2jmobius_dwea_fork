DROP TABLE IF EXISTS `character_costume_shortcuts`;
CREATE TABLE `character_costume_shortcuts`  (
  `player_id` int(11) NOT NULL,
  `page` int(11) NOT NULL,
  `slot_index` int(11) NOT NULL,
  `costume_id` int(11) NOT NULL
) DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;