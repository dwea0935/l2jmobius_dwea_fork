/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.model.actor.enums.player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.l2jmobius.gameserver.model.actor.enums.creature.Race;

/**
 * <p>
 * This enum defines all the classes that a player can choose in the game. Each class has a unique ID, a flag indicating if it is a mage class, a flag indicating if it is a summoner class, a race associated with it and a parent class from which it is derived.
 * </p>
 * <p>
 * The class hierarchy is structured such that each class can have a parent class, forming a tree-like structure.<br>
 * This allows for class progression and specialization as players advance in the game.
 * </p>
 * It also utility methods to retrieve information, check relationships and determine possible class transfers.
 */
public enum PlayerClass
{
	FIGHTER(0, false, Race.HUMAN, null),
	
	WARRIOR(1, false, Race.HUMAN, FIGHTER),
	GLADIATOR(2, false, Race.HUMAN, WARRIOR),
	WARLORD(3, false, Race.HUMAN, WARRIOR),
	KNIGHT(4, false, Race.HUMAN, FIGHTER),
	PALADIN(5, false, Race.HUMAN, KNIGHT),
	DARK_AVENGER(6, false, Race.HUMAN, KNIGHT),
	ROGUE(7, false, Race.HUMAN, FIGHTER),
	TREASURE_HUNTER(8, false, Race.HUMAN, ROGUE),
	HAWKEYE(9, false, Race.HUMAN, ROGUE),
	
	MAGE(10, true, Race.HUMAN, null),
	WIZARD(11, true, Race.HUMAN, MAGE),
	SORCERER(12, true, Race.HUMAN, WIZARD),
	NECROMANCER(13, true, Race.HUMAN, WIZARD),
	WARLOCK(14, true, true, Race.HUMAN, WIZARD),
	CLERIC(15, true, Race.HUMAN, MAGE),
	BISHOP(16, true, Race.HUMAN, CLERIC),
	PROPHET(17, true, Race.HUMAN, CLERIC),
	
	ELVEN_FIGHTER(18, false, Race.ELF, null),
	ELVEN_KNIGHT(19, false, Race.ELF, ELVEN_FIGHTER),
	TEMPLE_KNIGHT(20, false, Race.ELF, ELVEN_KNIGHT),
	SWORDSINGER(21, false, Race.ELF, ELVEN_KNIGHT),
	ELVEN_SCOUT(22, false, Race.ELF, ELVEN_FIGHTER),
	PLAINS_WALKER(23, false, Race.ELF, ELVEN_SCOUT),
	SILVER_RANGER(24, false, Race.ELF, ELVEN_SCOUT),
	
	ELVEN_MAGE(25, true, Race.ELF, null),
	ELVEN_WIZARD(26, true, Race.ELF, ELVEN_MAGE),
	SPELLSINGER(27, true, Race.ELF, ELVEN_WIZARD),
	ELEMENTAL_SUMMONER(28, true, true, Race.ELF, ELVEN_WIZARD),
	ORACLE(29, true, Race.ELF, ELVEN_MAGE),
	ELDER(30, true, Race.ELF, ORACLE),
	
	DARK_FIGHTER(31, false, Race.DARK_ELF, null),
	PALUS_KNIGHT(32, false, Race.DARK_ELF, DARK_FIGHTER),
	SHILLIEN_KNIGHT(33, false, Race.DARK_ELF, PALUS_KNIGHT),
	BLADEDANCER(34, false, Race.DARK_ELF, PALUS_KNIGHT),
	ASSASSIN(35, false, Race.DARK_ELF, DARK_FIGHTER),
	ABYSS_WALKER(36, false, Race.DARK_ELF, ASSASSIN),
	PHANTOM_RANGER(37, false, Race.DARK_ELF, ASSASSIN),
	
	DARK_MAGE(38, true, Race.DARK_ELF, null),
	DARK_WIZARD(39, true, Race.DARK_ELF, DARK_MAGE),
	SPELLHOWLER(40, true, Race.DARK_ELF, DARK_WIZARD),
	PHANTOM_SUMMONER(41, true, true, Race.DARK_ELF, DARK_WIZARD),
	SHILLIEN_ORACLE(42, true, Race.DARK_ELF, DARK_MAGE),
	SHILLIEN_ELDER(43, true, Race.DARK_ELF, SHILLIEN_ORACLE),
	
	ORC_FIGHTER(44, false, Race.ORC, null),
	ORC_RAIDER(45, false, Race.ORC, ORC_FIGHTER),
	DESTROYER(46, false, Race.ORC, ORC_RAIDER),
	ORC_MONK(47, false, Race.ORC, ORC_FIGHTER),
	TYRANT(48, false, Race.ORC, ORC_MONK),
	
	ORC_MAGE(49, true, Race.ORC, null),
	ORC_SHAMAN(50, true, Race.ORC, ORC_MAGE),
	OVERLORD(51, true, Race.ORC, ORC_SHAMAN),
	WARCRYER(52, true, Race.ORC, ORC_SHAMAN),
	
	DWARVEN_FIGHTER(53, false, Race.DWARF, null),
	SCAVENGER(54, false, Race.DWARF, DWARVEN_FIGHTER),
	BOUNTY_HUNTER(55, false, Race.DWARF, SCAVENGER),
	ARTISAN(56, false, Race.DWARF, DWARVEN_FIGHTER),
	WARSMITH(57, false, Race.DWARF, ARTISAN),
	
	DUELIST(88, false, Race.HUMAN, GLADIATOR),
	DREADNOUGHT(89, false, Race.HUMAN, WARLORD),
	PHOENIX_KNIGHT(90, false, Race.HUMAN, PALADIN),
	HELL_KNIGHT(91, false, Race.HUMAN, DARK_AVENGER),
	SAGITTARIUS(92, false, Race.HUMAN, HAWKEYE),
	ADVENTURER(93, false, Race.HUMAN, TREASURE_HUNTER),
	ARCHMAGE(94, true, Race.HUMAN, SORCERER),
	SOULTAKER(95, true, Race.HUMAN, NECROMANCER),
	ARCANA_LORD(96, true, true, Race.HUMAN, WARLOCK),
	CARDINAL(97, true, Race.HUMAN, BISHOP),
	HIEROPHANT(98, true, Race.HUMAN, PROPHET),
	
	EVA_TEMPLAR(99, false, Race.ELF, TEMPLE_KNIGHT),
	SWORD_MUSE(100, false, Race.ELF, SWORDSINGER),
	WIND_RIDER(101, false, Race.ELF, PLAINS_WALKER),
	MOONLIGHT_SENTINEL(102, false, Race.ELF, SILVER_RANGER),
	MYSTIC_MUSE(103, true, Race.ELF, SPELLSINGER),
	ELEMENTAL_MASTER(104, true, true, Race.ELF, ELEMENTAL_SUMMONER),
	EVA_SAINT(105, true, Race.ELF, ELDER),
	
	SHILLIEN_TEMPLAR(106, false, Race.DARK_ELF, SHILLIEN_KNIGHT),
	SPECTRAL_DANCER(107, false, Race.DARK_ELF, BLADEDANCER),
	GHOST_HUNTER(108, false, Race.DARK_ELF, ABYSS_WALKER),
	GHOST_SENTINEL(109, false, Race.DARK_ELF, PHANTOM_RANGER),
	STORM_SCREAMER(110, true, Race.DARK_ELF, SPELLHOWLER),
	SPECTRAL_MASTER(111, true, true, Race.DARK_ELF, PHANTOM_SUMMONER),
	SHILLIEN_SAINT(112, true, Race.DARK_ELF, SHILLIEN_ELDER),
	
	TITAN(113, false, Race.ORC, DESTROYER),
	GRAND_KHAVATARI(114, false, Race.ORC, TYRANT),
	DOMINATOR(115, true, Race.ORC, OVERLORD),
	DOOMCRYER(116, true, Race.ORC, WARCRYER),
	
	FORTUNE_SEEKER(117, false, Race.DWARF, BOUNTY_HUNTER),
	MAESTRO(118, false, Race.DWARF, WARSMITH),
	
	MALE_SOLDIER(123, false, Race.KAMAEL, null),
	FEMALE_SOLDIER(124, false, Race.KAMAEL, null),
	TROOPER(125, false, Race.KAMAEL, MALE_SOLDIER),
	WARDER(126, false, Race.KAMAEL, FEMALE_SOLDIER),
	BERSERKER(127, false, Race.KAMAEL, TROOPER),
	MALE_SOULBREAKER(128, false, Race.KAMAEL, TROOPER),
	FEMALE_SOULBREAKER(129, false, Race.KAMAEL, WARDER),
	ARBALESTER(130, false, Race.KAMAEL, WARDER),
	DOOMBRINGER(131, false, Race.KAMAEL, BERSERKER),
	MALE_SOUL_HOUND(132, false, Race.KAMAEL, MALE_SOULBREAKER),
	FEMALE_SOUL_HOUND(133, false, Race.KAMAEL, FEMALE_SOULBREAKER),
	TRICKSTER(134, false, Race.KAMAEL, ARBALESTER),
	INSPECTOR(135, false, Race.KAMAEL, WARDER),
	JUDICATOR(136, false, Race.KAMAEL, INSPECTOR),
	
	SIGEL_KNIGHT(139, false, null, null),
	TYRR_WARRIOR(140, false, null, null),
	OTHELL_ROGUE(141, false, null, null),
	YUL_ARCHER(142, false, null, null),
	FEOH_WIZARD(143, false, null, null),
	ISS_ENCHANTER(144, false, null, null),
	WYNN_SUMMONER(145, false, null, null),
	AEORE_HEALER(146, false, null, null),
	
	SIGEL_PHOENIX_KNIGHT(148, false, Race.HUMAN, PHOENIX_KNIGHT),
	SIGEL_HELL_KNIGHT(149, false, Race.HUMAN, HELL_KNIGHT),
	SIGEL_EVA_TEMPLAR(150, false, Race.ELF, EVA_TEMPLAR),
	SIGEL_SHILLIEN_TEMPLAR(151, false, Race.DARK_ELF, SHILLIEN_TEMPLAR),
	TYRR_DUELIST(152, false, Race.HUMAN, DUELIST),
	TYRR_DREADNOUGHT(153, false, Race.HUMAN, DREADNOUGHT),
	TYRR_TITAN(154, false, Race.ORC, TITAN),
	TYRR_GRAND_KHAVATARI(155, false, Race.ORC, GRAND_KHAVATARI),
	TYRR_MAESTRO(156, false, Race.DWARF, MAESTRO),
	TYRR_DOOMBRINGER(157, false, Race.KAMAEL, DOOMBRINGER),
	OTHELL_ADVENTURER(158, false, Race.HUMAN, ADVENTURER),
	OTHELL_WIND_RIDER(159, false, Race.ELF, WIND_RIDER),
	OTHELL_GHOST_HUNTER(160, false, Race.DARK_ELF, GHOST_HUNTER),
	OTHELL_FORTUNE_SEEKER(161, false, Race.DWARF, FORTUNE_SEEKER),
	YUL_SAGITTARIUS(162, false, Race.HUMAN, SAGITTARIUS),
	YUL_MOONLIGHT_SENTINEL(163, false, Race.ELF, MOONLIGHT_SENTINEL),
	YUL_GHOST_SENTINEL(164, false, Race.DARK_ELF, GHOST_SENTINEL),
	YUL_TRICKSTER(165, false, Race.KAMAEL, TRICKSTER),
	FEOH_ARCHMAGE(166, true, Race.HUMAN, ARCHMAGE),
	FEOH_SOULTAKER(167, true, Race.HUMAN, SOULTAKER),
	FEOH_MYSTIC_MUSE(168, true, Race.ELF, MYSTIC_MUSE),
	FEOH_STORM_SCREAMER(169, true, Race.DARK_ELF, STORM_SCREAMER),
	FEOH_SOUL_HOUND(170, true, Race.KAMAEL, MALE_SOUL_HOUND), // fix me ?
	ISS_HIEROPHANT(171, true, Race.HUMAN, HIEROPHANT),
	ISS_SWORD_MUSE(172, false, Race.ELF, SWORD_MUSE),
	ISS_SPECTRAL_DANCER(173, false, Race.DARK_ELF, SPECTRAL_DANCER),
	ISS_DOMINATOR(174, true, Race.ORC, DOMINATOR),
	ISS_DOOMCRYER(175, true, Race.ORC, DOOMCRYER),
	WYNN_ARCANA_LORD(176, true, true, Race.HUMAN, ARCANA_LORD),
	WYNN_ELEMENTAL_MASTER(177, true, true, Race.ELF, ELEMENTAL_MASTER),
	WYNN_SPECTRAL_MASTER(178, true, true, Race.DARK_ELF, SPECTRAL_MASTER),
	AEORE_CARDINAL(179, true, Race.HUMAN, CARDINAL),
	AEORE_EVA_SAINT(180, true, Race.ELF, EVA_SAINT),
	AEORE_SHILLIEN_SAINT(181, true, Race.DARK_ELF, SHILLIEN_SAINT),
	
	ERTHEIA_FIGHTER(182, false, Race.ERTHEIA, null),
	ERTHEIA_WIZARD(183, true, Race.ERTHEIA, null),
	
	MARAUDER(184, false, Race.ERTHEIA, ERTHEIA_FIGHTER),
	CLOUD_BREAKER(185, true, Race.ERTHEIA, ERTHEIA_WIZARD),
	
	RIPPER(186, false, Race.ERTHEIA, MARAUDER),
	STRATOMANCER(187, true, Race.ERTHEIA, CLOUD_BREAKER),
	
	EVISCERATOR(188, false, Race.ERTHEIA, RIPPER),
	SAYHA_SEER(189, true, Race.ERTHEIA, STRATOMANCER),
	
	DEATH_SOLDIER(212, false, Race.HUMAN, null),
	DEATH_WARRIOR(213, false, Race.HUMAN, DEATH_SOLDIER),
	DEATH_BERSERKER(214, false, Race.HUMAN, DEATH_WARRIOR),
	DEATH_KNIGHT(215, false, Race.HUMAN, DEATH_BERSERKER),
	SIGEL_DEATH_KNIGHT(216, false, Race.HUMAN, DEATH_KNIGHT),
	
	DWARF_MAKER(231, true, Race.DWARF, null),
	EARTH_MAKER(232, true, Race.DWARF, DWARF_MAKER),
	WIND_MAKER(233, true, Race.DWARF, EARTH_MAKER),
	SOUL_MAKER(234, true, Race.DWARF, WIND_MAKER),
	SHINE_MAKER(235, true, Race.DWARF, SOUL_MAKER);
	
	/** The Identifier of the Class */
	private final int _id;
	
	/** True if the class is a mage class */
	private final boolean _isMage;
	
	/** True if the class is a summoner class */
	private final boolean _isSummoner;
	
	/** The Race object of the class */
	private final Race _race;
	
	/** The parent PlayerClass or null if this class is a root */
	private final PlayerClass _parentClass;
	
	/** List of available Class for next transfer **/
	private final Set<PlayerClass> _nextClasses = new HashSet<>(1);
	
	/** Map to store all PlayerClass instances by their ID */
	private static Map<Integer, PlayerClass> _classMap = new HashMap<>(PlayerClass.values().length);
	static
	{
		for (PlayerClass playerClass : PlayerClass.values())
		{
			_classMap.put(playerClass.getId(), playerClass);
		}
	}
	
	/**
	 * Constructs a new PlayerClass instance.
	 * @param id the class ID.
	 * @param isMage {@code true} if the class is a mage class.
	 * @param race the race associated with the class.
	 * @param parent the parent PlayerClass, or {@code null} if this is a root class.
	 */
	private PlayerClass(int id, boolean isMage, Race race, PlayerClass parent)
	{
		_id = id;
		_isMage = isMage;
		_isSummoner = false;
		_race = race;
		_parentClass = parent;
		
		if (_parentClass != null)
		{
			_parentClass.addNextClass(this);
		}
	}
	
	/**
	 * Constructs a new PlayerClass instance with summoner flag.
	 * @param id the class ID.
	 * @param isMage {@code true} if the class is a mage class.
	 * @param isSummoner {@code true} if the class is a summoner class.
	 * @param race the race associated with the class.
	 * @param parent the parent PlayerClass, or {@code null} if this is a root class.
	 */
	private PlayerClass(int id, boolean isMage, boolean isSummoner, Race race, PlayerClass parent)
	{
		_id = id;
		_isMage = isMage;
		_isSummoner = isSummoner;
		_race = race;
		_parentClass = parent;
		
		if (_parentClass != null)
		{
			_parentClass.addNextClass(this);
		}
	}
	
	/**
	 * Retrieves the PlayerClass instance by its ID.
	 * @param id the ID of the class to retrieve.
	 * @return the PlayerClass instance corresponding to the given ID, or {@code null} if not found.
	 */
	public static PlayerClass getPlayerClass(int id)
	{
		return _classMap.get(id);
	}
	
	/**
	 * Gets the ID of the class.
	 * @return the ID of the class.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Checks if the class is a mage class.
	 * @return {@code true} if the class is a mage class.
	 */
	public boolean isMage()
	{
		return _isMage;
	}
	
	/**
	 * Checks if the class is a summoner class.
	 * @return {@code true} if the class is a summoner class.
	 */
	public boolean isSummoner()
	{
		return _isSummoner;
	}
	
	/**
	 * Gets the race associated with the class.
	 * @return the Race object of the class.
	 */
	public Race getRace()
	{
		return _race;
	}
	
	/**
	 * Checks if this class is a child of the specified class.
	 * @param id the parent PlayerClass to check.
	 * @return {@code true} if this class is a child of the specified class.
	 */
	public boolean childOf(PlayerClass id)
	{
		if (_parentClass == null)
		{
			return false;
		}
		
		if (_parentClass == id)
		{
			return true;
		}
		
		return _parentClass.childOf(id);
	}
	
	/**
	 * Checks if this class is equal to or a child of the specified class.
	 * @param playerClass the PlayerClass to check.
	 * @return {@code true} if this class is equal to or a child of the specified class.
	 */
	public boolean equalsOrChildOf(PlayerClass playerClass)
	{
		return (this == playerClass) || childOf(playerClass);
	}
	
	/**
	 * Gets the level of this class in the class hierarchy.
	 * @return the child level of this PlayerClass (0=root, 1=first class, etc.).
	 */
	public int level()
	{
		if (_parentClass == null)
		{
			return 0;
		}
		
		return 1 + _parentClass.level();
	}
	
	/**
	 * Gets the parent class of this class.
	 * @return the parent PlayerClass, or {@code null} if this is a root class.
	 */
	public PlayerClass getParent()
	{
		return _parentClass;
	}
	
	/**
	 * Gets the root class of this class.
	 * @return the root PlayerClass of this class.
	 */
	public PlayerClass getRootClass()
	{
		if (_parentClass != null)
		{
			return _parentClass.getRootClass();
		}
		
		return this;
	}
	
	/**
	 * Gets the set of possible class transfers for this class.
	 * @return a set of PlayerClass instances representing possible class transfers.
	 */
	public Set<PlayerClass> getNextClasses()
	{
		return _nextClasses;
	}
	
	/**
	 * Adds a class to the list of possible class transfers for this class.
	 * @param playerClass the PlayerClass to add.
	 */
	private void addNextClass(PlayerClass playerClass)
	{
		_nextClasses.add(playerClass);
	}
}
