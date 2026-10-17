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
		Location townCenter)
	{
	}
	
	public record GearLoadout(
		int gradeTier, // 0 = NG, 1 = D, 2 = C, 3 = B
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
		// Lv 20 - 30 First Class Hunting Grounds
		new HuntingZone("Ruins of Despair", 20, 30, null, new Location(-20100, 138800, -3890), "Gludio Castle Town", GLUDIO_TOWN),
		new HuntingZone("Execution Grounds", 20, 32, null, new Location(45500, 147800, -3690), "Dion Castle Town", DION_TOWN),
		// Lv 30 - 40 Mid-Dion / Early-Giran
		new HuntingZone("Death Pass", 30, 40, null, new Location(70500, 118200, -3680), "Giran Castle Town", GIRAN_TOWN),
		new HuntingZone("Plains of Dion", 30, 40, null, new Location(18500, 162500, -3520), "Dion Castle Town", DION_TOWN),
		// Lv 40 - 52 Second Class Hunting Grounds
		new HuntingZone("Sea of Spores", 40, 52, null, new Location(64200, 26800, -3760), "Oren Castle Town", OREN_TOWN),
		new HuntingZone("Outlaw Forest", 40, 52, null, new Location(91400, 12200, -3660), "Oren Castle Town", OREN_TOWN),
		// Lv 52 - 61 B-Grade Progression Grounds
		new HuntingZone("The Cemetery", 52, 61, null, new Location(172100, 20300, -3320), "Aden Castle Town", ADEN_TOWN),
		new HuntingZone("Forsaken Plains", 52, 61, null, new Location(179500, 41200, -4150), "Aden Castle Town", ADEN_TOWN),
		// Lv 61 - 75 Late Progression Grounds
		new HuntingZone("Wall of Argos", 61, 75, null, new Location(168200, -49600, -3530), "Goddard Castle Town", GODDARD_TOWN),
		new HuntingZone("Hot Springs", 61, 75, null, new Location(144800, -112500, -2100), "Goddard Castle Town", GODDARD_TOWN),
		// Lv 75 - 85 Endgame Third-Class Grounds
		new HuntingZone("Varka Silenos Outpost", 75, 85, null, new Location(125500, -40900, -3720), "Goddard Castle Town", GODDARD_TOWN),
		new HuntingZone("Ketra Orc Outpost", 75, 85, null, new Location(146900, -67200, -3660), "Goddard Castle Town", GODDARD_TOWN));
	
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
	
	public static PlayerClass selectNextClassTransfer(PlayerClass currentClass)
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
		final List<PlayerClass> list = new ArrayList<>(nextClasses);
		return list.get(ThreadLocalRandom.current().nextInt(list.size()));
	}
	
	public static int getTargetGradeTierForLevel(int level)
	{
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
		
		return switch (gradeTier)
		{
			case 1 -> // D-Grade (Lv 20+)
				new GearLoadout(
					1,
					"D-Grade",
					20,
					150_000L,
					isMage ? 189 : (isArcher ? 275 : (isDagger ? 222 : 129)), // Staff of Life / Light Crossbow / Poniard Dagger / Sword of Revolution
					isMage ? 0 : 627, // Aspis
					47, // Helmet
					isMage ? 437 : (isArcher || isDagger ? 395 : 352), // Tunic of Knowledge / Manticore Skin Shirt / Brigandine Tunic
					isMage ? 469 : (isArcher || isDagger ? 417 : 379), // Hose of Knowledge / Manticore Skin Gaiters / Brigandine Gaiters
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
					isMage ? 206 : (isArcher ? 281 : (isDagger ? 231 : 135)), // Demon's Staff / Crystallized Ice Bow / Grace Dagger / Samurai Longsword
					isMage ? 0 : 631, // Composite Shield
					517, // Composite Helmet
					isMage ? 441 : (isArcher || isDagger ? 398 : 356), // Karmian Tunic / Plated Leather Mail / Full Plate Armor
					isMage ? 473 : (isArcher || isDagger ? 420 : 0), // Karmian Stockings / Plated Leather Gaiters
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
					isMage ? 7892 : (isArcher ? 287 : (isDagger ? 234 : 7883)), // Spell Breaker / Bow of Peril / Demon's Dagger / Guardian Sword
					isMage ? 0 : 633, // Zubei's Shield
					503, // Zubei's Helmet
					isMage ? 2400 : (isArcher || isDagger ? 2390 : 357), // Avadon Robe / Blue Wolf Leather Armor / Zubei's Breastplate
					isMage ? 0 : (isArcher || isDagger ? 0 : 383), // Zubei's Gaiters
					5718, // Avadon / Zubei Gloves
					5734, // Avadon / Zubei Boots
					916, // Necklace of Black Ore
					854, // Earring of Black Ore
					885, // Ring of Black Ore
					1465, // Soulshot: B-grade
					3950, // Blessed Spiritshot: B-grade
					1343); // Silver Arrow
			default -> // No-Grade (Lv 1+)
				new GearLoadout(
					0,
					"No-Grade",
					1,
					2_000L,
					isMage ? 179 : (isArcher ? 272 : (isDagger ? 218 : 2)), // Mace of Prayer / Forest Bow / Throwing Knife / Long Sword
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
