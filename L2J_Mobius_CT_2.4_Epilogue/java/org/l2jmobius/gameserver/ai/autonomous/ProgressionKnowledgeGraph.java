/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.ai.autonomous;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;

/**
 * World &amp; Progression Knowledge Graph ({@code Lv 1 -> 85}) for Autonomous Citizens.
 * <p>
 * Indexes the 6 racial starting villages, level-bracketed hunting grounds from {@code Lv 1} to {@code Lv 85},
 * town return hubs, class transfer pathways ({@code Lv 20 / 40 / 76}), and fair-play equipment progression
 * loadouts capped at the Classic Luxury Shop B-Grade ceiling.
 * </p>
 */
public final class ProgressionKnowledgeGraph
{
	public record VillageHub(
		Race race,
		String villageName,
		Location spawnPoint,
		Location guildMasterPoint,
		Location gatekeeperPoint)
	{
	}
	
	public record HuntingZone(
		String name,
		int minLevel,
		int maxLevel,
		Race preferredRace,
		Location huntingCenter,
		String townName,
		Location townCenter,
		boolean isSevenSignsCatacomb)
	{
		public HuntingZone(
			String name,
			int minLevel,
			int maxLevel,
			Race preferredRace,
			Location huntingCenter,
			String townName,
			Location townCenter)
		{
			this(name, minLevel, maxLevel, preferredRace, huntingCenter, townName, townCenter, false);
		}
	}
	
	public record GearLoadout(
		int gradeTier, // 0 = NG, 1 = D, 2 = C, 3 = B (Shop Cap), 4 = A-Craft, 5 = S-SA
		String gradeLabel,
		int minLevel,
		long adenaCost,
		int weaponId,
		int shieldOrSigilId,
		int helmetId,
		int chestId,
		int legsId,
		int glovesId,
		int bootsId,
		int necklaceId,
		int earringId,
		int ringId,
		int soulshotId,
		int spiritshotId,
		int arrowId)
	{
	}
	
	private static final List<VillageHub> STARTING_VILLAGES = List.of(
		new VillageHub(
			Race.HUMAN,
			"Talking Island Village",
			new Location(-84136, 244579, -3730),
			new Location(-84466, 243176, -3728),
			new Location(-84181, 244619, -3729)),
		new VillageHub(
			Race.ELF,
			"Elven Village",
			new Location(45873, 49288, -3064),
			new Location(47362, 50068, -2992),
			new Location(46934, 51467, -2977)),
		new VillageHub(
			Race.DARK_ELF,
			"Dark Elven Village",
			new Location(12428, 16551, -4588),
			new Location(10842, 17909, -4560),
			new Location(9745, 15606, -4574)),
		new VillageHub(
			Race.ORC,
			"Orc Village",
			new Location(-44211, -113521, -241),
			new Location(-45916, -113626, -240),
			new Location(-45158, -112583, -240)),
		new VillageHub(
			Race.DWARF,
			"Dwarven Village",
			new Location(116551, -182493, -1525),
			new Location(115717, -183422, -1483),
			new Location(115113, -178212, -901)),
		new VillageHub(
			Race.KAMAEL,
			"Kamael Village",
			new Location(-116934, 46616, 368),
			new Location(-118082, 45602, 360),
			new Location(-117251, 46771, 360)));
	
	private static final Location GLUDIO_TOWN = new Location(-14280, 123600, -3116);
	private static final Location DION_TOWN = new Location(15670, 142983, -2705);
	private static final Location GIRAN_TOWN = new Location(83400, 147943, -3404);
	private static final Location OREN_TOWN = new Location(82956, 53162, -1495);
	private static final Location ADEN_TOWN = new Location(146783, 25808, -2008);
	private static final Location GODDARD_TOWN = new Location(147928, -55273, -2734);
	private static final Location RUNE_TOWN = new Location(43799, -47727, -798);
	
	private static final List<HuntingZone> HUNTING_ZONES = List.of(
		// Lv 1 - 10 Racial Starting Outskirts
		new HuntingZone("Talking Island Outskirts", 1, 10, Race.HUMAN, new Location(-81200, 240150, -3560), "Talking Island Village", new Location(-84136, 244579, -3730)),
		new HuntingZone("Elven Forest Glade", 1, 10, Race.ELF, new Location(46900, 41600, -3490), "Elven Village", new Location(45873, 49288, -3064)),
		new HuntingZone("Dark Forest Altar", 1, 10, Race.DARK_ELF, new Location(25800, 10950, -3720), "Dark Elven Village", new Location(12428, 16551, -4588)),
		new HuntingZone("Immortal Plateau North", 1, 10, Race.ORC, new Location(-50200, -113600, -670), "Orc Village", new Location(-44211, -113521, -241)),
		new HuntingZone("Frozen Valley Strip Mine", 1, 10, Race.DWARF, new Location(108500, -174000, -400), "Dwarven Village", new Location(116551, -182493, -1525)),
		new HuntingZone("Isle of Souls Outskirts", 1, 10, Race.KAMAEL, new Location(-122300, 52100, -1250), "Kamael Village", new Location(-116934, 46616, 368)),
		// Lv 10 - 20 Early Hunting Grounds
		new HuntingZone("Ruins of Agony", 10, 20, null, new Location(-42200, 118500, -3520), "Gludio Castle Town", GLUDIO_TOWN),
		new HuntingZone("Elven Fortress Approaches", 10, 20, null, new Location(42000, 24500, -3600), "Gludio Castle Town", GLUDIO_TOWN),
		// Lv 20 - 30 First Class Hunting Grounds + Seven Signs Necropolis
		new HuntingZone("Ruins of Despair", 20, 30, null, new Location(-20100, 138800, -3890), "Gludio Castle Town", GLUDIO_TOWN),
		new HuntingZone("Execution Grounds", 20, 32, null, new Location(45500, 147800, -3690), "Dion Castle Town", DION_TOWN),
		new HuntingZone("Necropolis of Sacrifice", 20, 30, null, new Location(-41560, 209800, -5085), "Gludio Castle Town", GLUDIO_TOWN, true),
		// Lv 30 - 40 Mid-Dion / Early-Giran + Seven Signs Necropolis
		new HuntingZone("Death Pass", 30, 40, null, new Location(70500, 118200, -3680), "Giran Castle Town", GIRAN_TOWN),
		new HuntingZone("Plains of Dion", 30, 40, null, new Location(18500, 162500, -3520), "Dion Castle Town", DION_TOWN),
		new HuntingZone("Pilgrim's Necropolis", 30, 40, null, new Location(45250, 123900, -5410), "Giran Castle Town", GIRAN_TOWN, true),
		// Lv 40 - 52 Second Class Hunting Grounds + Seven Signs Catacomb
		new HuntingZone("Sea of Spores", 40, 52, null, new Location(64200, 26800, -3760), "Oren Castle Town", OREN_TOWN),
		new HuntingZone("Outlaw Forest", 40, 52, null, new Location(91400, 12200, -3660), "Oren Castle Town", OREN_TOWN),
		new HuntingZone("Catacomb of the Branded", 40, 52, null, new Location(46500, 170300, -4980), "Giran Castle Town", GIRAN_TOWN, true),
		// Lv 52 - 61 B-Grade Progression Grounds + Seven Signs Catacomb
		new HuntingZone("The Cemetery", 52, 61, null, new Location(172100, 20300, -3320), "Aden Castle Town", ADEN_TOWN),
		new HuntingZone("Forsaken Plains", 52, 61, null, new Location(179500, 41200, -4150), "Aden Castle Town", ADEN_TOWN),
		new HuntingZone("Catacomb of the Apostate", 52, 61, null, new Location(77300, 78400, -5125), "Oren Castle Town", OREN_TOWN, true),
		// Lv 61 - 75 Late Progression Grounds + Seven Signs Catacomb
		new HuntingZone("Wall of Argos", 61, 75, null, new Location(168200, -49600, -3530), "Goddard Castle Town", GODDARD_TOWN),
		new HuntingZone("Hot Springs", 61, 75, null, new Location(144800, -112500, -2100), "Goddard Castle Town", GODDARD_TOWN),
		new HuntingZone("Catacomb of the Witch", 61, 75, null, new Location(140400, 79600, -5425), "Aden Castle Town", ADEN_TOWN, true),
		// Lv 75 - 85 Endgame Third-Class Grounds + Seven Signs Necropolis/Catacomb
		new HuntingZone("Varka Silenos Outpost", 75, 85, null, new Location(125500, -40900, -3720), "Goddard Castle Town", GODDARD_TOWN),
		new HuntingZone("Ketra Orc Outpost", 75, 85, null, new Location(146900, -67200, -3660), "Goddard Castle Town", GODDARD_TOWN),
		new HuntingZone("Catacomb of Dark Omens", 72, 80, null, new Location(-19500, 13500, -4900), "Oren Castle Town", OREN_TOWN, true),
		new HuntingZone("Disciples Necropolis", 78, 85, null, new Location(172200, -17600, -4900), "Aden Castle Town", ADEN_TOWN, true));
	
	private ProgressionKnowledgeGraph()
	{
	}
	
	public static VillageHub getStartingVillage(Race race)
	{
		if (race != null)
		{
			for (VillageHub hub : STARTING_VILLAGES)
			{
				if (hub.race() == race)
				{
					return hub;
				}
			}
		}
		return STARTING_VILLAGES.get(0);
	}
	
	public static HuntingZone selectHuntingZone(int level, Race race)
	{
		final List<HuntingZone> candidates = new ArrayList<>();
		for (HuntingZone zone : HUNTING_ZONES)
		{
			if ((level >= zone.minLevel()) && (level <= zone.maxLevel()))
			{
				if ((zone.preferredRace() == null) || (zone.preferredRace() == race))
				{
					candidates.add(zone);
				}
			}
		}
		if (!candidates.isEmpty())
		{
			return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
		}
		// Fallback to closest level bracket
		HuntingZone best = HUNTING_ZONES.get(0);
		int bestDiff = Integer.MAX_VALUE;
		for (HuntingZone zone : HUNTING_ZONES)
		{
			final int mid = (zone.minLevel() + zone.maxLevel()) / 2;
			final int diff = Math.abs(level - mid);
			if (diff < bestDiff)
			{
				bestDiff = diff;
				best = zone;
			}
		}
		return best;
	}
	
	public static final PlayerClass[] ENDGAME_ASPIRATIONS =
	{
		PlayerClass.FORTUNE_SEEKER,
		PlayerClass.MAESTRO,
		PlayerClass.DUELIST,
		PlayerClass.DREADNOUGHT,
		PlayerClass.PHOENIX_KNIGHT,
		PlayerClass.HELL_KNIGHT,
		PlayerClass.SAGITTARIUS,
		PlayerClass.ADVENTURER,
		PlayerClass.ARCHMAGE,
		PlayerClass.SOULTAKER,
		PlayerClass.ARCANA_LORD,
		PlayerClass.CARDINAL,
		PlayerClass.HIEROPHANT,
		PlayerClass.EVA_TEMPLAR,
		PlayerClass.SWORD_MUSE,
		PlayerClass.WIND_RIDER,
		PlayerClass.MOONLIGHT_SENTINEL,
		PlayerClass.MYSTIC_MUSE,
		PlayerClass.ELEMENTAL_MASTER,
		PlayerClass.EVA_SAINT,
		PlayerClass.SHILLIEN_TEMPLAR,
		PlayerClass.SPECTRAL_DANCER,
		PlayerClass.GHOST_HUNTER,
		PlayerClass.GHOST_SENTINEL,
		PlayerClass.STORM_SCREAMER,
		PlayerClass.SPECTRAL_MASTER,
		PlayerClass.SHILLIEN_SAINT,
		PlayerClass.TITAN,
		PlayerClass.GRAND_KHAVATARI,
		PlayerClass.DOMINATOR,
		PlayerClass.DOOMCRYER,
		PlayerClass.DOOMBRINGER,
		PlayerClass.MALE_SOUL_HOUND,
		PlayerClass.FEMALE_SOUL_HOUND,
		PlayerClass.TRICKSTER
	};
	
	public static PlayerClass selectRandomEndgameAspiration()
	{
		return ENDGAME_ASPIRATIONS[ThreadLocalRandom.current().nextInt(ENDGAME_ASPIRATIONS.length)];
	}
	
	public static PlayerClass getBaseStartingClassFor(PlayerClass aspiration)
	{
		if (aspiration == null)
		{
			return PlayerClass.FIGHTER;
		}
		PlayerClass cur = aspiration;
		while (cur.getParent() != null)
		{
			cur = cur.getParent();
		}
		return cur;
	}
	
	public static HuntingZone selectClassTransferQuestZone(int classTier)
	{
		return selectClassTransferQuestZone(classTier, 1);
	}
	
	public static HuntingZone selectClassTransferQuestZone(int classTier, int subStage)
	{
		if (classTier == 1)
		{
			return switch (subStage)
			{
				case 1 -> new HuntingZone("Ruins of Agony (1st Class Path S1)", 19, 25, null, new Location(-42200, 123600, -3520), "Gludio Castle Town", GLUDIO_TOWN);
				case 2 -> new HuntingZone("Ruins of Despair (1st Class Path S2)", 19, 25, null, new Location(-20100, 138800, -3890), "Gludio Castle Town", GLUDIO_TOWN);
				default -> new HuntingZone("Gludio Outskirts (1st Class Path S3)", 19, 25, null, new Location(-14500, 128200, -3120), "Gludio Castle Town", GLUDIO_TOWN);
			};
		}
		if (classTier == 2)
		{
			return switch (subStage)
			{
				case 1 -> new HuntingZone("Death Pass (2nd Class Trial Mark #1)", 39, 45, null, new Location(70500, 118200, -3680), "Giran Castle Town", GIRAN_TOWN);
				case 2 -> new HuntingZone("Cruma Marshlands (2nd Class Testimony Mark #2)", 39, 45, null, new Location(17200, 114300, -3440), "Dion Castle Town", DION_TOWN);
				default -> new HuntingZone("Breka's Stronghold (2nd Class Test Mark #3)", 39, 45, null, new Location(80200, 132600, -3610), "Giran Castle Town", GIRAN_TOWN);
			};
		}
		return switch (subStage)
		{
			case 1 -> new HuntingZone("Hot Springs (Saga Tablet #1)", 75, 80, null, new Location(149500, -112500, -2065), "Goddard Castle Town", GODDARD_TOWN);
			case 2 -> new HuntingZone("Valley of Saints (Saga Tablet #2)", 75, 80, null, new Location(68200, -72100, -3720), "Goddard Castle Town", GODDARD_TOWN);
			case 3 -> new HuntingZone("Wall of Argos (Saga Tablet #3)", 75, 80, null, new Location(168200, -49600, -3530), "Goddard Castle Town", GODDARD_TOWN);
			case 4 -> new HuntingZone("Ketra Orc Outpost (Saga Tablet #4 Divine Stone)", 75, 80, null, new Location(146800, -67200, -3640), "Goddard Castle Town", GODDARD_TOWN);
			case 5 -> new HuntingZone("Shrine of the Loyal (Saga Tablet #5 Archon of Halisha)", 75, 80, null, new Location(184200, -75600, -2780), "Goddard Castle Town", GODDARD_TOWN);
			default -> new HuntingZone("Forbidden Gateway (Saga Tablet #6 Final)", 75, 80, null, new Location(185800, 20600, -3260), "Aden Castle Town", ADEN_TOWN);
		};
	}
	
	public static HuntingZone selectSubclassQuestZone(int stage)
	{
		return switch (stage)
		{
			case 1 -> new HuntingZone("The Cemetery (Cabrio Coffer)", 75, 80, null, new Location(180112, 18480, -3160), "Aden Castle Town", ADEN_TOWN);
			case 2 -> new HuntingZone("Tower of Insolence (Hallate/Kernon/Golkonda)", 75, 80, null, new Location(115200, 16600, -3660), "Aden Castle Town", ADEN_TOWN);
			case 3 -> new HuntingZone("Tower of Insolence Upper (Pipette Trial)", 75, 80, null, new Location(114600, 16200, 950), "Aden Castle Town", ADEN_TOWN);
			default -> new HuntingZone("Silent Valley (Mimir's Elixir)", 75, 80, null, new Location(179500, 41200, -4150), "Aden Castle Town", ADEN_TOWN);
		};
	}
	
	public static HuntingZone selectNoblesseQuestZone(int stage)
	{
		return switch (stage)
		{
			case 1 -> new HuntingZone("Dragon Valley & Forsaken Plains (Q241 Precious Soul 1)", 75, 82, null, new Location(73000, 118500, -3680), "Aden Castle Town", ADEN_TOWN);
			case 2 -> new HuntingZone("Swamp of Screams (Q242 Precious Soul 2)", 76, 82, null, new Location(70000, -49800, -3280), "Goddard Castle Town", GODDARD_TOWN);
			case 3 -> new HuntingZone("Valley of Saints & Barakiel Shrine (Q246 Precious Soul 3)", 76, 85, null, new Location(91008, -85904, -2736), "Goddard Castle Town", GODDARD_TOWN);
			default -> new HuntingZone("Coliseum & Goddard Lake (Q247 Precious Soul 4)", 76, 85, null, new Location(147450, 27064, -2205), "Aden Castle Town", ADEN_TOWN);
		};
	}
	
	public static int[] getClassTransferStageItemIds(PlayerClass playerClass, PlayerClass targetEndgameClass, int classTier)
	{
		final boolean isMage = (playerClass != null) && playerClass.isMage();
		final String asp = (targetEndgameClass != null) ? targetEndgameClass.name() : "";
		if (classTier == 1)
		{
			if (asp.equals("FORTUNE_SEEKER"))
			{
				return new int[] { 1640, 1641, 1642 }; // Raven's Feather -> Bead Parcel -> Ring of Raven
			}
			if (asp.equals("MAESTRO"))
			{
				return new int[] { 1633, 1634, 1635 }; // Ratman Tooth -> Big Tooth -> Final Pass Certificate
			}
			if (isMage)
			{
				return new int[] { 1158, 1159, 1161 }; // Gem of Aiken -> Flame Eruptor -> Sword of Ritual
			}
			return new int[] { 1143, 1144, 1145 }; // Warrior Guild Mark -> Rusted Bronze Sword -> Medallion of Warrior
		}
		if (classTier == 2)
		{
			// 3 separate Occupation Marks (Trial Mark #1, Testimony Mark #2, Test Mark #3)
			if (asp.equals("FORTUNE_SEEKER"))
			{
				return new int[] { 3119, 3238, 2809 }; // Mark of Guildsman + Mark of Prosperity + Mark of Searcher
			}
			if (asp.equals("MAESTRO"))
			{
				return new int[] { 3119, 3238, 2867 }; // Mark of Guildsman + Mark of Prosperity + Mark of Maestro
			}
			if (asp.equals("DUELIST") || asp.equals("GRAND_KHAVATARI") || asp.equals("SWORD_MUSE") || asp.equals("SPECTRAL_DANCER"))
			{
				return new int[] { 2627, 2734, 2762 }; // Mark of Challenger + Mark of Trust + Mark of Duelist
			}
			if (asp.equals("DREADNOUGHT") || asp.equals("TITAN"))
			{
				return new int[] { 2627, 2734, 2721 }; // Mark of Challenger + Mark of Trust + Mark of Champion
			}
			if (asp.contains("SAGITTARIUS") || asp.contains("SENTINEL") || asp.contains("TRICKSTER"))
			{
				return new int[] { 2809, 2734, 3293 }; // Mark of Searcher + Mark of Trust + Mark of Sagittarius
			}
			if (isMage)
			{
				return new int[] { 3140, 2734, 2840 }; // Mark of Scholar + Mark of Trust + Mark of Magus
			}
			return new int[] { 2627, 2734, 2809 }; // Mark of Challenger + Mark of Trust + Mark of Searcher
		}
		// Tier 3: All 6 Resonance Amulets (7284..7289) for the 6 Tablets of Vision
		return new int[] { 7284, 7285, 7286, 7287, 7288, 7289 };
	}
	
	public static int getClassTransferQuestMarkId(PlayerClass playerClass, int classTier)
	{
		final int[] stageItems = getClassTransferStageItemIds(playerClass, null, classTier);
		return stageItems[stageItems.length - 1];
	}
	
	/**
	 * Maps every 1st-Class Path (Lv 20), 2nd-Class 3-Mark Trial/Testimony/Test (Lv 40), and 3rd-Class 6-Tablet Saga (Lv 76)
	 * stage to its real server {@link org.l2jmobius.gameserver.mechanics.script.Quest} ID in {@code ScriptManager}.
	 */
	public static int[] getClassTransferQuestIds(PlayerClass playerClass, PlayerClass targetEndgameClass, int classTier)
	{
		final PlayerClass nextClass = selectNextClassTowards(playerClass, targetEndgameClass);
		final String nextName = (nextClass != null) ? nextClass.name() : "";
		final String asp = (targetEndgameClass != null) ? targetEndgameClass.name() : nextName;
		final Race race = (playerClass != null) ? playerClass.getRace() : Race.HUMAN;
		
		if (classTier == 1)
		{
			final int q1 = switch (nextName)
			{
				case "WARRIOR" -> 401;
				case "KNIGHT" -> 402;
				case "ROGUE" -> 403;
				case "WIZARD" -> 404;
				case "CLERIC" -> 405;
				case "ELVEN_KNIGHT" -> 406;
				case "ELVEN_SCOUT" -> 407;
				case "ELVEN_WIZARD" -> 408;
				case "ORACLE" -> 409;
				case "PALUS_KNIGHT" -> 410;
				case "ASSASSIN" -> 411;
				case "DARK_WIZARD" -> 412;
				case "SHILLIEN_ORACLE" -> 413;
				case "ORC_RAIDER" -> 414;
				case "ORC_MONK" -> 415;
				case "ORC_SHAMAN" -> 416;
				case "SCAVENGER" -> 417;
				case "ARTISAN" -> 418;
				case "TROOPER" -> 62;
				case "WARDER" -> 63;
				default -> 401;
			};
			return new int[] { q1, q1, q1 };
		}
		if (classTier == 2)
		{
			if (race == Race.KAMAEL)
			{
				final int kq = nextName.contains("BERSERKER") ? 64 : (nextName.contains("ARBALESTER") ? 66 : 65);
				return new int[] { kq, kq, kq };
			}
			final int trialId;
			if (asp.equals("FORTUNE_SEEKER") || asp.equals("MAESTRO"))
			{
				trialId = 216; // TrialOfTheGuildsman
			}
			else if (asp.contains("KNIGHT") || asp.contains("TEMPLAR"))
			{
				trialId = 212; // TrialOfDuty
			}
			else if (asp.equals("ADVENTURER") || asp.equals("WIND_RIDER") || asp.equals("GHOST_HUNTER") || asp.contains("SAGITTARIUS") || asp.contains("SENTINEL"))
			{
				trialId = 213; // TrialOfTheSearcher
			}
			else if (asp.equals("ARCHMAGE") || asp.equals("SOULTAKER") || asp.equals("ARCANA_LORD") || asp.equals("MYSTIC_MUSE") || asp.equals("ELEMENTAL_MASTER") || asp.equals("STORM_SCREAMER") || asp.equals("SPECTRAL_MASTER"))
			{
				trialId = 214; // TrialOfTheScholar
			}
			else if (asp.equals("CARDINAL") || asp.equals("HIEROPHANT") || asp.equals("EVA_SAINT") || asp.equals("SHILLIEN_SAINT") || asp.equals("DOMINATOR") || asp.equals("DOOMCRYER"))
			{
				trialId = 215; // TrialOfThePilgrim
			}
			else
			{
				trialId = 211; // TrialOfTheChallenger
			}
			
			final int testimonyId = switch (race)
			{
				case ELF -> 218; // TestimonyOfLife
				case DARK_ELF -> 219; // TestimonyOfFate
				case ORC -> 220; // TestimonyOfGlory
				case DWARF -> 221; // TestimonyOfProsperity
				default -> 217; // TestimonyOfTrust
			};
			
			final int testId = switch (asp)
			{
				case "DUELIST", "GRAND_KHAVATARI", "SWORD_MUSE", "SPECTRAL_DANCER" -> 222; // TestOfTheDuelist
				case "DREADNOUGHT", "TITAN" -> 223; // TestOfTheChampion
				case "SAGITTARIUS", "MOONLIGHT_SENTINEL", "GHOST_SENTINEL" -> 224; // TestOfSagittarius
				case "ADVENTURER", "WIND_RIDER", "GHOST_HUNTER", "FORTUNE_SEEKER" -> 225; // TestOfTheSearcher
				case "PHOENIX_KNIGHT", "EVA_TEMPLAR", "CARDINAL", "EVA_SAINT" -> 226; // TestOfTheHealer
				case "HIEROPHANT", "SHILLIEN_SAINT" -> 227; // TestOfTheReformer
				case "ARCHMAGE", "MYSTIC_MUSE", "STORM_SCREAMER" -> 228; // TestOfMagus
				case "SOULTAKER", "SHILLIEN_TEMPLAR", "HELL_KNIGHT" -> 229; // TestOfWitchcraft
				case "ARCANA_LORD", "ELEMENTAL_MASTER", "SPECTRAL_MASTER" -> 230; // TestOfTheSummoner
				case "MAESTRO" -> 231; // TestOfTheMaestro
				case "DOMINATOR" -> 232; // TestOfTheLord
				case "DOOMCRYER" -> 233; // TestOfTheWarSpirit
				default -> 222;
			};
			return new int[] { trialId, testimonyId, testId };
		}
		
		// Tier 3: Saga of the 6 Tablets of Vision (Q00067..Q00100)
		final int sagaId = switch (asp)
		{
			case "DOOMBRINGER" -> 67;
			case "MALE_SOUL_HOUND" -> 68;
			case "FEMALE_SOUL_HOUND", "TRICKSTER" -> 69;
			case "PHOENIX_KNIGHT" -> 70;
			case "EVA_TEMPLAR" -> 71;
			case "SWORD_MUSE" -> 72;
			case "DUELIST" -> 73;
			case "DREADNOUGHT" -> 74;
			case "TITAN" -> 75;
			case "GRAND_KHAVATARI" -> 76;
			case "DOMINATOR" -> 77;
			case "DOOMCRYER" -> 78;
			case "ADVENTURER" -> 79;
			case "WIND_RIDER" -> 80;
			case "GHOST_HUNTER" -> 81;
			case "SAGITTARIUS" -> 82;
			case "MOONLIGHT_SENTINEL" -> 83;
			case "GHOST_SENTINEL" -> 84;
			case "CARDINAL" -> 85;
			case "HIEROPHANT" -> 86;
			case "EVA_SAINT" -> 87;
			case "ARCHMAGE" -> 88;
			case "MYSTIC_MUSE" -> 89;
			case "STORM_SCREAMER" -> 90;
			case "ARCANA_LORD" -> 91;
			case "ELEMENTAL_MASTER" -> 92;
			case "SPECTRAL_MASTER" -> 93;
			case "SOULTAKER" -> 94;
			case "HELL_KNIGHT" -> 95;
			case "SPECTRAL_DANCER" -> 96;
			case "SHILLIEN_TEMPLAR" -> 97;
			case "SHILLIEN_SAINT" -> 98;
			case "FORTUNE_SEEKER" -> 99;
			case "MAESTRO" -> 100;
			default -> 73;
		};
		return new int[] { sagaId, sagaId, sagaId, sagaId, sagaId, sagaId };
	}
	
	public static int getSubclassQuestId(Race race, int stage)
	{
		if (stage <= 3)
		{
			return (race == Race.KAMAEL) ? 236 : 234; // Q00236_SeedsOfChaos or Q00234_FatesWhisper
		}
		return 235; // Q00235_MimirsElixir
	}
	
	public static int getNoblesseQuestId(int stage)
	{
		return switch (stage)
		{
			case 1 -> 241; // Q00241_PossessorOfAPreciousSoul1
			case 2 -> 242; // Q00242_PossessorOfAPreciousSoul2
			case 3 -> 246; // Q00246_PossessorOfAPreciousSoul3
			default -> 247; // Q00247_PossessorOfAPreciousSoul4
		};
	}
	
	public static PlayerClass selectNextClassTransfer(PlayerClass currentClass)
	{
		return selectNextClassTowards(currentClass, null);
	}
	
	public static PlayerClass selectNextClassTowards(PlayerClass currentClass, PlayerClass targetEndgameClass)
	{
		if (currentClass == null)
		{
			return null;
		}
		final Set<PlayerClass> nextClasses = currentClass.getNextClasses();
		if ((nextClasses == null) || nextClasses.isEmpty())
		{
			return null;
		}
		if (targetEndgameClass != null)
		{
			for (PlayerClass candidate : nextClasses)
			{
				PlayerClass ptr = targetEndgameClass;
				while (ptr != null)
				{
					if (ptr == candidate)
					{
						return candidate;
					}
					ptr = ptr.getParent();
				}
			}
		}
		final List<PlayerClass> list = new ArrayList<>(nextClasses);
		return list.get(ThreadLocalRandom.current().nextInt(list.size()));
	}
	
	public static int getTargetGradeTierForLevel(int level)
	{
		return getTargetGradeTierForLevel(level, false);
	}
	
	public static int getTargetGradeTierForLevel(int level, boolean canAffordCraftedEndgame)
	{
		if (canAffordCraftedEndgame && (level >= 76))
		{
			return 5; // S-Grade Crafted + SA
		}
		if (canAffordCraftedEndgame && (level >= 61))
		{
			return 4; // A-Grade Crafted / Mammon Unsealed
		}
		if (level >= 52)
		{
			return 3; // B-Grade Luxury Shop ceiling
		}
		if (level >= 40)
		{
			return 2; // C-Grade
		}
		if (level >= 20)
		{
			return 1; // D-Grade
		}
		return 0; // No-Grade
	}
	
	public static GearLoadout getFairPlayLoadout(PlayerClass playerClass, int gradeTier)
	{
		final boolean isMage = (playerClass != null) && playerClass.isMage();
		final String className = (playerClass != null) ? playerClass.name() : "FIGHTER";
		final boolean isArcher = className.contains("RANGER") || className.contains("HAWKEYE") || className.contains("SAGITTARIUS") || className.contains("SENTINEL") || className.contains("ARBALESTER") || className.contains("TRICKSTER");
		final boolean isDagger = className.contains("ROGUE") || className.contains("ASSASSIN") || className.contains("SCOUT") || className.contains("HUNTER") || className.contains("WALKER") || className.contains("ADVENTURER") || className.contains("RIDER");
		final boolean isPolearm = className.contains("WARLORD") || className.contains("DREADNOUGHT");
		final boolean isDual = className.contains("GLADIATOR") || className.contains("DUELIST") || className.contains("DANCER") || className.contains("MUSE");
		final boolean isFist = className.contains("MONK") || className.contains("TYRANT") || className.contains("KHAVATARI");
		final boolean twoHandedMelee = isPolearm || isDual || isFist || isArcher;
		
		return switch (gradeTier)
		{
			case 1 -> // D-Grade (Lv 20+)
				new GearLoadout(
					1,
					"D-Grade",
					20,
					150_000L,
					isMage ? 189 : (isArcher ? 275 : (isDagger ? 222 : (isPolearm ? 292 : (isFist ? 262 : 129)))), // Staff of Life / Light Crossbow / Poniard Dagger / Pike / Scallop Jamadhar / Sword of Revolution
					(isMage || twoHandedMelee) ? 0 : 627, // Aspis
					47, // Helmet
					isMage ? 437 : (isArcher || isDagger || isFist ? 395 : 352), // Tunic of Knowledge / Manticore Skin Shirt / Brigandine Tunic
					isMage ? 469 : (isArcher || isDagger || isFist ? 417 : 379), // Hose of Knowledge / Manticore Skin Gaiters / Brigandine Gaiters
					605, // Leather Gloves
					553, // Iron Boots
					908, // Necklace of Wisdom
					846, // Coral Earring
					877, // Ring of Wisdom
					1463, // Soulshot: D-grade
					3948, // Blessed Spiritshot: D-grade
					1341); // Bone Arrow
			case 2 -> // C-Grade (Lv 40+)
				new GearLoadout(
					2,
					"C-Grade",
					40,
					850_000L,
					isMage ? 206 : (isArcher ? 281 : (isDagger ? 231 : (isPolearm ? 298 : (isDual ? 2560 : (isFist ? 265 : 135))))), // Demon's Staff / Crystallized Ice Bow / Grace Dagger / Orcish Glaive / Stormbringer*Stormbringer / Fisted Blade / Samurai Longsword
					(isMage || twoHandedMelee) ? 0 : 631, // Composite Shield
					517, // Composite Helmet
					isMage ? 441 : (isArcher || isDagger || isFist ? 398 : 356), // Karmian Tunic / Plated Leather Mail / Full Plate Armor
					isMage ? 473 : (isArcher || isDagger || isFist ? 420 : 0), // Karmian Stockings / Plated Leather Gaiters
					2454, // Karmian Gloves / Mithril Gauntlets
					2431, // Plated Leather Boots
					913, // Blessed Necklace
					850, // Moonstone Earring
					881, // Aquastone Ring
					1464, // Soulshot: C-grade
					3949, // Blessed Spiritshot: C-grade
					1342); // Fine Steel Arrow
			case 3 -> // B-Grade Luxury Shop Ceiling (Lv 52+)
				new GearLoadout(
					3,
					"B-Grade",
					52,
					3_500_000L,
					isMage ? 7892 : (isArcher ? 287 : (isDagger ? 234 : (isPolearm ? 300 : (isDual ? 2626 : (isFist ? 268 : 7883))))), // Spell Breaker / Bow of Peril / Demon's Dagger / Great Axe / Samurai*Samurai / Bellion Cestus / Guardian Sword
					(isMage || twoHandedMelee) ? 0 : 633, // Zubei's Shield
					503, // Zubei's Helmet
					isMage ? 2400 : (isArcher || isDagger || isFist ? 2390 : 357), // Avadon Robe / Blue Wolf Leather Armor / Zubei's Breastplate
					isMage ? 0 : (isArcher || isDagger || isFist ? 0 : 383), // Zubei's Gaiters
					5718, // Avadon / Zubei Gloves
					5734, // Avadon / Zubei Boots
					916, // Necklace of Black Ore
					854, // Earring of Black Ore
					885, // Ring of Black Ore
					1465, // Soulshot: B-grade
					3950, // Blessed Spiritshot: B-grade
					1343); // Silver Arrow
			case 4 -> // A-Grade Crafted & Mammon Unsealed (Lv 61+)
				new GearLoadout(
					4,
					"A-Craft",
					61,
					8_000_000L,
					isMage ? 151 : (isArcher ? 288 : (isDagger ? 236 : (isPolearm ? 305 : (isDual ? 5706 : (isFist ? 270 : 80))))), // Sword of Miracles / Carnage Bow / Soul Separator / Tallum Glaive / Keshanberk*Damascus / Dragon Grinder / Tallum Blade
					(isMage || twoHandedMelee) ? 0 : 2498, // Dark Crystal Shield
					547, // Tallum / Majestic Helmet
					isMage ? 2409 : (isArcher || isDagger || isFist ? 2395 : 2382), // Majestic Robe / Majestic Leather / Tallum Plate Armor
					0,
					isMage ? 5774 : (isArcher || isDagger || isFist ? 5772 : 5768), // Majestic / Tallum Gloves
					isMage ? 5786 : (isArcher || isDagger || isFist ? 5784 : 5780), // Majestic / Tallum Boots
					924, // Majestic Necklace
					862, // Majestic Earring
					893, // Majestic Ring
					1466, // Soulshot: A-grade
					3951, // Blessed Spiritshot: A-grade
					1344); // Mithril Arrow
			case 5 -> // S-Grade Crafted + Mammon SA (Lv 76+)
				new GearLoadout(
					5,
					"S-SA",
					76,
					25_000_000L,
					isMage ? 6579 : (isArcher ? 7577 : (isDagger ? 6590 : (isPolearm ? 6599 : (isDual ? 6580 : (isFist ? 6602 : 6583))))), // Arcana Mace [Acumen] / Draconic Bow [Focus] / Angel Slayer [Haste] / Saint Spear [Health] / Tallum*Dark Legion / Demon Splinter [Focus] / Basalt Battlehammer [Health]
					(isMage || twoHandedMelee) ? 0 : 6377, // Imperial Crusader Shield
					isMage ? 6386 : (isArcher || isDagger || isFist ? 6382 : 6378), // Major Arcana Circlet / Draconic Leather Helmet / Imperial Crusader Helmet
					isMage ? 6383 : (isArcher || isDagger || isFist ? 6379 : 6373), // Major Arcana Robe / Draconic Leather Armor / Imperial Crusader Breastplate
					isMage ? 0 : (isArcher || isDagger || isFist ? 0 : 6374), // Imperial Crusader Gaiters
					isMage ? 6384 : (isArcher || isDagger || isFist ? 6380 : 6375), // Major Arcana / Draconic / Imperial Crusader Gloves
					isMage ? 6385 : (isArcher || isDagger || isFist ? 6381 : 6376), // Major Arcana / Draconic / Imperial Crusader Boots
					920, // Tateossian Necklace
					858, // Tateossian Earring
					889, // Tateossian Ring
					1467, // Soulshot: S-grade
					3952, // Blessed Spiritshot: S-grade
					1345); // Shining Arrow
			default -> // No-Grade (Lv 1+)
				new GearLoadout(
					0,
					"No-Grade",
					1,
					2_000L,
					isMage ? 179 : (isArcher ? 272 : (isDagger ? 218 : (isPolearm ? 15 : (isFist ? 256 : 2)))), // Mace of Prayer / Forest Bow / Throwing Knife / Short Spear / Cestus / Long Sword
					0,
					45, // Bone Helmet
					isMage ? 432 : 23, // Tunic of Devotion / Wooden Breastplate
					isMage ? 465 : 2386, // Hose of Devotion / Wooden Gaiters
					51, // Short Leather Gloves
					39, // Boots
					906, // Necklace of Magic
					845, // Cat's Eye Earring
					875, // Ring of Magic
					1835, // Soulshot: No-grade
					2509, // Spiritshot: No-grade
					17); // Wooden Arrow
		};
	}
}
