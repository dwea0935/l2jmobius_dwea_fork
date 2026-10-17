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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.autonomous.AutonomousCitizen.CitizenMacroState;
import org.l2jmobius.gameserver.ai.autonomous.ProgressionKnowledgeGraph.VillageHub;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.data.xml.InitialEquipmentData;
import org.l2jmobius.gameserver.data.xml.PlayerTemplateData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.instance.RaidBoss;
import org.l2jmobius.gameserver.entity.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.entity.actor.templates.PlayerTemplate;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.groups.PartyDistributionType;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.InitialEquipment;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.RaidBossSpawnManager;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.network.serverpackets.JoinParty;

/**
 * Singleton Manager for Real-Player Autonomous Citizens ({@code Lv 1 -> 85}), Inter-Citizen Dwarven
 * Spoil/Craft Commissioning, MariaDB Cold Storage &amp; Auto-Restore Persistence, and Self-Reporting QA/Economy Engine.
 */
public final class AutonomousPlayerManager
{
	private static final Logger LOGGER = Logger.getLogger(AutonomousPlayerManager.class.getName());
	public static final String CITIZEN_ACCOUNT = "autonomous_qa";
	private static final long TICK_INTERVAL_MS = 500L;
	
	public record ColdStorageCitizenSummary(
		int charId,
		String name,
		int level,
		PlayerClass playerClass,
		String aspiration,
		String phenotypeBadge)
	{
	}
	
	/**
	 * Balanced Endgame Class Aspiration rotation — prioritizes Fortune Seeker (Spoiler) and Maestro (Crafter)
	 * early so the server economy always has active Dwarven Artisans for inter-citizen A/S-Grade trade.
	 */
	private static final PlayerClass[] ASPIRATION_ROTATION =
	{
		PlayerClass.FORTUNE_SEEKER, // Dwarf Spoiler (Scavenger -> Bounty Hunter -> Fortune Seeker)
		PlayerClass.MAESTRO, // Dwarf Crafter (Artisan -> Warsmith -> Maestro)
		PlayerClass.DUELIST,
		PlayerClass.DREADNOUGHT,
		PlayerClass.ARCHMAGE,
		PlayerClass.SAGITTARIUS,
		PlayerClass.CARDINAL,
		PlayerClass.PHOENIX_KNIGHT,
		PlayerClass.MYSTIC_MUSE,
		PlayerClass.GHOST_HUNTER,
		PlayerClass.STORM_SCREAMER,
		PlayerClass.TITAN,
		PlayerClass.GRAND_KHAVATARI,
		PlayerClass.DOOMCRYER,
		PlayerClass.DOOMBRINGER,
		PlayerClass.TRICKSTER
	};
	
	private static final String[] CITIZEN_NAME_POOL =
	{
		// Elven & Sylvan Names (56)
		"Aeloria", "Kaelen", "Thalorin", "Vaelis", "Elowen", "Lyris", "Miriel", "Caelum",
		"Sylvana", "Faelar", "Aerith", "Lirael", "Caladrel", "Elenion", "Galathil", "Nimloth",
		"Celeborn", "Idril", "Luthien", "Finduilas", "Aredhel", "Glorfindel", "Erestor", "Lindir",
		"Haldir", "Orophin", "Rumil", "Amroth", "Nimrodel", "Celebrindal", "Tauriel", "Thranduil",
		"Aegnor", "Angrod", "Finrod", "Orodreth", "Earwen", "Anaire", "Amarie", "Vana",
		"Nessa", "Yavanna", "Estel", "Gilraen", "Ivorwen", "Lothiriel", "Morwen", "Nienor",
		"Rian", "Tuor", "Voronwe", "Ecthelion", "Rog", "Agalad", "Beleg", "Mablung",
		// Dark Elven & Shadow Names (56)
		"Nyxara", "Draven", "Valkor", "Seraphis", "Malakor", "Vespera", "Zariel", "Noctis",
		"Lilith", "Morrigan", "Shalia", "Xullrae", "Viconia", "Phaere", "Quenthel", "Alisstra",
		"Waelan", "Zauvirr", "Jarlaxle", "Zaknafein", "Dinin", "Berginyon", "Rizzen", "Pharaun",
		"Ryld", "Gromph", "Soluun", "Narbondel", "Veldrin", "Valas", "Kimmuriel", "RaiGuy",
		"Seldszar", "Nym", "Tebryn", "Urlryn", "Vorn", "Kelnozz", "Malaggar", "Sorn",
		"Briza", "Maya", "Vierna", "Sabrina", "Dahlia", "Sylphrena", "Kaladin", "Shallan",
		"Jasnah", "Adolin", "Renarin", "Szeth", "Vasher", "Vivenna", "Susebron", "Lightsong",
		// Human Chivalric, Mage & Rogue Names (64)
		"Bramwell", "Garrick", "Isolde", "Ronan", "Dorian", "Talia", "Astrid", "Soren",
		"Alistair", "Caspian", "Evander", "Gideon", "Hadrian", "Julian", "Leander", "Lucian",
		"Marcus", "Nathaniel", "Oberon", "Percival", "Quentin", "Raphael", "Sebastian", "Tristan",
		"Valerian", "Xavier", "Zachary", "Adelaide", "Beatrix", "Cordelia", "Delphine", "Eleanor",
		"Genevieve", "Helena", "Imogen", "Juliet", "Katarina", "Lavinia", "Madeline", "Octavia",
		"Penelope", "Rosalind", "Seraphina", "Theodora", "Valeria", "Vivienne", "Winifred", "Yvaine",
		"Siegfried", "Roland", "Galahad", "Lancelot", "Gawain", "Bedivere", "Kay", "Bors",
		"Pellinore", "Lamorak", "Gareth", "Agravain", "Mordred", "Guinevere", "Morgana", "Nimue",
		// Orcish War & Shamanic Names (52)
		"Torvald", "Fenris", "Korgath", "Grommash", "Durotan", "Orgrim", "Nazgrel", "Broxigar",
		"Varok", "Garrosh", "Rexxar", "Thrall", "DrekThar", "Zuluhed", "Kargath", "Kilrogg",
		"Nerzhul", "Guldan", "ChoGall", "Teron", "Dentarg", "Tagar", "Dalrend", "Maim",
		"Draka", "Geyah", "Aggra", "Garona", "Griselda", "Karga", "Shakia", "Zogosh",
		"Uruk", "Azog", "Bolg", "Gothmog", "Lurtz", "Shagrat", "Gorbag", "Grishnakh",
		"Ugluk", "Mauhur", "Snaga", "Muzgash", "Lagduf", "Radbug", "Ufthak", "Golfimbul",
		"PaagrioSon", "KashaFang", "AtubaClaw", "NerugaWar",
		// Dwarven Artisan & Spoiler Names (52)
		"Korin", "Thorin", "Balin", "Dwalin", "Fili", "Kili", "Oin", "Gloin",
		"Bifur", "Bofur", "Bombur", "Dori", "Nori", "Ori", "Gimli", "Dain",
		"Thrain", "Thror", "Nain", "Fundin", "Groin", "Borin", "Farin", "Gror",
		"Frerin", "Dis", "Telchar", "Azaghal", "Mim", "Ibun", "Khim", "Durin",
		"Bruenor", "Thibbledorf", "Athrogate", "Amber", "Torgar", "Baelnor", "Krag", "Bardin",
		"Gotrek", "Snorri", "Malakai", "Josef", "Belegar", "Thorek", "Ungrim", "Thorgrim",
		"Burlok", "Grombrindal", "Alrik", "Byrrnoth",
		// Kamael & Gracia Epilogue Retail Vibe Names (56)
		"Kekropus", "Katenar", "Harkilgamed", "Rodelia", "Spicula", "Metusela", "Einkin", "Nornil",
		"Zephyra", "Valkyrie", "Enyo", "Bellona", "Nemesis", "Alecto", "Megaera", "Tisiphone",
		"Hyperion", "Iapetus", "Coeus", "Crius", "Cronus", "Oceanus", "Tethys", "Theia",
		"Phoebe", "Mnemosyne", "Themis", "Dione", "Atlas", "Prometheus", "Epimetheus", "Menoetius",
		"Orion", "Perseus", "Bellerophon", "Cadmus", "Theseus", "Jason", "Achilles", "Hector",
		"Ajax", "Odysseus", "Diomedes", "Patroclus", "Aeneas", "Sarpedon", "Memnon", "Cygnus",
		"Penthesilea", "Hippolyta", "Antiope", "Melanippe", "Atalanta", "Cassandra", "Andromache", "Briseis"
	};
	
	private final Map<Integer, AutonomousCitizen> _citizens = new ConcurrentHashMap<>();
	private final AtomicInteger _spawnCounter = new AtomicInteger(0);
	private final AtomicBoolean _bootRestoreDone = new AtomicBoolean(false);
	private volatile ScheduledFuture<?> _tickTask;
	
	private AutonomousPlayerManager()
	{
		start();
		ThreadPool.schedule(this::restoreActiveCitizensOnBoot, 5000L);
	}
	
	public synchronized void start()
	{
		if ((_tickTask == null) || _tickTask.isCancelled())
		{
			_tickTask = ThreadPool.scheduleAtFixedRate(this::tickAll, 1000L, TICK_INTERVAL_MS);
		}
	}
	
	private void tickAll()
	{
		if (_citizens.isEmpty())
		{
			return;
		}
		for (AutonomousCitizen citizen : _citizens.values())
		{
			try
			{
				citizen.tick();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "AutonomousPlayerManager: Error ticking citizen " + citizen.getPlayer().getName() + ": " + e.getMessage(), e);
			}
		}
	}
	
	/**
	 * Enforces exact client {@code CharacterCreateMenuWnd} ({@code interface.u}) and {@code Engine.dll}
	 * ({@code User::GetCharType} @ {@code VA 0x2036ec40}) appearance bounds so no citizen ever triggers a bald
	 * head mesh ({@code hairStyle > 4} on Male) or Kamael hairColor index fault ({@code hairColor > 2}).
	 */
	public static void sanitizeCitizenAppearance(Player player)
	{
		if ((player == null) || (player.getAppearance() == null))
		{
			return;
		}
		final PlayerAppearance app = player.getAppearance();
		final Race race = player.getRace();
		final PlayerClass pc = player.getPlayerClass();
		if (race == Race.KAMAEL && (pc != null))
		{
			if ((pc == PlayerClass.MALE_SOLDIER) || (pc == PlayerClass.TROOPER) || (pc == PlayerClass.BERSERKER)
				|| (pc == PlayerClass.MALE_SOULBREAKER) || (pc == PlayerClass.DOOMBRINGER) || (pc == PlayerClass.MALE_SOUL_HOUND))
			{
				app.setMale();
			}
			else if ((pc == PlayerClass.FEMALE_SOLDIER) || (pc == PlayerClass.WARDER) || (pc == PlayerClass.ARBALESTER)
				|| (pc == PlayerClass.FEMALE_SOULBREAKER) || (pc == PlayerClass.TRICKSTER) || (pc == PlayerClass.FEMALE_SOUL_HOUND))
			{
				app.setFemale();
			}
		}
		if ((app.getFace() < 0) || (app.getFace() > 2))
		{
			app.setFace(ThreadLocalRandom.current().nextInt(0, 3));
		}
		final int maxHairColor = (race == Race.KAMAEL) ? 2 : 3;
		if ((app.getHairColor() < 0) || (app.getHairColor() > maxHairColor))
		{
			app.setHairColor(ThreadLocalRandom.current().nextInt(0, maxHairColor + 1));
		}
		final int maxHairStyle = app.isFemale() ? 6 : 4;
		if ((app.getHairStyle() < 0) || (app.getHairStyle() > maxHairStyle))
		{
			app.setHairStyle(ThreadLocalRandom.current().nextInt(0, maxHairStyle + 1));
		}
	}
	
	public AutonomousCitizen spawnCitizen()
	{
		return spawnCitizen(null, null);
	}
	
	public AutonomousCitizen spawnCitizen(Player sponsor)
	{
		return spawnCitizen(null, sponsor);
	}
	
	public int spawnCitizens(int count)
	{
		return spawnCitizens(count, null);
	}
	
	public int spawnCitizens(int count, Player sponsor)
	{
		int spawned = 0;
		for (int i = 0; i < count; i++)
		{
			if (spawnCitizen(null, sponsor) != null)
			{
				spawned++;
			}
		}
		return spawned;
	}
	
	public AutonomousCitizen spawnCitizen(PlayerClass requestedClassOrAspiration)
	{
		return spawnCitizen(requestedClassOrAspiration, null);
	}
	
	public AutonomousCitizen spawnCitizen(PlayerClass requestedClassOrAspiration, Player sponsor)
	{
		try
		{
			final PlayerClass aspiration;
			final PlayerClass chosenStartClass;
			if (requestedClassOrAspiration == null)
			{
				aspiration = ASPIRATION_ROTATION[_spawnCounter.getAndIncrement() % ASPIRATION_ROTATION.length];
				chosenStartClass = ProgressionKnowledgeGraph.getBaseStartingClassFor(aspiration);
			}
			else if (requestedClassOrAspiration.level() == 3)
			{
				aspiration = requestedClassOrAspiration;
				chosenStartClass = ProgressionKnowledgeGraph.getBaseStartingClassFor(aspiration);
			}
			else
			{
				chosenStartClass = ProgressionKnowledgeGraph.getBaseStartingClassFor(requestedClassOrAspiration);
				PlayerClass resolvedAspiration = null;
				for (PlayerClass cand : ProgressionKnowledgeGraph.ENDGAME_ASPIRATIONS)
				{
					if ((cand == requestedClassOrAspiration) || cand.childOf(requestedClassOrAspiration))
					{
						resolvedAspiration = cand;
						break;
					}
				}
				aspiration = (resolvedAspiration != null) ? resolvedAspiration : ProgressionKnowledgeGraph.selectRandomEndgameAspiration();
			}
			
			final PlayerTemplate template = PlayerTemplateData.getInstance().getTemplate(chosenStartClass);
			if (template == null)
			{
				return null;
			}
			
			// Exact CharacterCreateMenuWnd (interface.u) & Engine.dll (User::GetCharType) bounds:
			// - Sex: Kamael locked by MALE_SOLDIER/FEMALE_SOLDIER; all other races 50/50 Male/Female
			// - Face: 0..2 (3 types)
			// - HairColor: 0..2 for Kamael (3 colors), 0..3 for all other races (4 colors)
			// - HairStyle: 0..4 for Male (5 styles), 0..6 for Female (7 styles)
			final boolean isFemale = (chosenStartClass == PlayerClass.FEMALE_SOLDIER)
				|| ((chosenStartClass != PlayerClass.MALE_SOLDIER) && ThreadLocalRandom.current().nextBoolean());
			final byte face = (byte) ThreadLocalRandom.current().nextInt(0, 3);
			final int hairColorBound = (template.getRace() == Race.KAMAEL) ? 3 : 4;
			final byte hairColor = (byte) ThreadLocalRandom.current().nextInt(0, hairColorBound);
			final int hairStyleBound = isFemale ? 7 : 5;
			final byte hairStyle = (byte) ThreadLocalRandom.current().nextInt(0, hairStyleBound);
			final PlayerAppearance appearance = new PlayerAppearance(face, hairColor, hairStyle, isFemale);
			
			final String charName = generateUniqueCitizenName();
			final Player player;
			synchronized (CharInfoTable.getInstance())
			{
				player = Player.create(template, CITIZEN_ACCOUNT, charName, appearance);
			}
			if (player == null)
			{
				return null;
			}
			
			final VillageHub hub = ProgressionKnowledgeGraph.getStartingVillage(template.getRace());
			final int spawnX;
			final int spawnY;
			final int spawnZ;
			if (sponsor != null)
			{
				final int offsetX = ThreadLocalRandom.current().nextInt(-75, 76);
				final int offsetY = ThreadLocalRandom.current().nextInt(-75, 76);
				spawnX = sponsor.getX() + offsetX;
				spawnY = sponsor.getY() + offsetY;
				spawnZ = sponsor.getZ() + 5;
				player.setInstanceId(sponsor.getInstanceId());
				player.setHeading(sponsor.getHeading());
			}
			else
			{
				final Location spawnLoc = hub.spawnPoint();
				final int offsetX = ThreadLocalRandom.current().nextInt(-90, 91);
				final int offsetY = ThreadLocalRandom.current().nextInt(-90, 91);
				spawnX = spawnLoc.getX() + offsetX;
				spawnY = spawnLoc.getY() + offsetY;
				spawnZ = spawnLoc.getZ();
			}
			
			player.setXYZInvisible(spawnX, spawnY, spawnZ);
			player.setTitle("");
			player.setVitalityPoints(PlayerStat.MAX_VITALITY_POINTS, true);
			player.addAdena(ItemProcessType.REWARD, 25_000L, null, false);
			
			final Collection<InitialEquipment> classEquipment = InitialEquipmentData.getInstance().getClassEquipment(player.getPlayerClass());
			if (classEquipment != null)
			{
				for (InitialEquipment equipment : classEquipment)
				{
					final Item item = player.getInventory().addItem(ItemProcessType.REWARD, equipment.getId(), equipment.getCount(), player, null);
					if ((item != null) && item.isEquipable() && equipment.isEquipped())
					{
						player.getInventory().equipItem(item);
					}
				}
			}
			
			for (SkillLearn skillLearn : SkillTreeData.getInstance().getAvailableSkills(player, player.getPlayerClass(), false, true))
			{
				final Skill skill = SkillData.getInstance().getSkill(skillLearn.getSkillId(), skillLearn.getSkillLevel());
				if (skill != null)
				{
					player.addSkill(skill, true);
				}
			}
			
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			player.setCurrentCp(player.getMaxCp());
			player.setOfflinePlay(true);
			player.setOnlineStatus(true, true);
			player.spawnMe(player.getX(), player.getY(), player.getZ());
			player.setRunning();
			player.broadcastUserInfo();
			if (sponsor != null)
			{
				player.sendInfo(sponsor);
			}
			
			final AutonomousCitizen citizen = new AutonomousCitizen(player, aspiration);
			if (sponsor != null)
			{
				citizen.holdNearSponsor(sponsor.getName(), 15_000L);
			}
			player.storeMe();
			_citizens.put(player.getObjectId(), citizen);
			start();
			LOGGER.info("AutonomousPlayerManager: Spawned Lv 1 Citizen " + player.getName() + " [" + chosenStartClass + " -> " + aspiration + " " + citizen.getPhenotypeBadge() + "] at " + ((sponsor != null) ? ("GM " + sponsor.getName()) : hub.villageName()));
			return citizen;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "AutonomousPlayerManager: Failed to spawn citizen: " + e.getMessage(), e);
			return null;
		}
	}
	
	/**
	 * Teleports all active citizens around the requesting GM, guarantees world spawn visibility,
	 * and holds them in {@code SPAWN_AND_ORIENT} for 15 seconds so the GM can inspect or party them.
	 */
	public int recallAllToPlayer(Player sponsor)
	{
		if (sponsor == null)
		{
			return 0;
		}
		int recalled = 0;
		final List<AutonomousCitizen> list = getActiveCitizens();
		for (int i = 0; i < list.size(); i++)
		{
			final AutonomousCitizen citizen = list.get(i);
			final Player cp = (citizen != null) ? citizen.getPlayer() : null;
			if (cp == null)
			{
				continue;
			}
			final double angle = (2.0 * Math.PI * i) / Math.max(1, list.size());
			final int ox = (int) Math.round(Math.cos(angle) * 75.0);
			final int oy = (int) Math.round(Math.sin(angle) * 75.0);
			citizen.holdNearSponsor(sponsor.getName(), 15_000L);
			cp.teleToLocation(sponsor.getX() + ox, sponsor.getY() + oy, sponsor.getZ() + 5, sponsor.getHeading(), sponsor.getInstanceId());
			if (!cp.isSpawned())
			{
				cp.onTeleported();
			}
			cp.sendInfo(sponsor);
			cp.broadcastUserInfo();
			recalled++;
		}
		return recalled;
	}
	
	/**
	 * Checks whether at least one Dwarven Crafter (Artisan/Warsmith/Maestro) or Spoiler (Scavenger/Bounty Hunter/Fortune Seeker)
	 * citizen is active in the world to fulfill A-Grade / S-Grade crafting &amp; spoil commissions.
	 */
	public boolean hasActiveDwarvenArtisan()
	{
		for (AutonomousCitizen c : _citizens.values())
		{
			if ((c != null) && (c.isCrafterCitizen() || c.isSpoilerCitizen()))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Executes an authentic inter-citizen Dwarven Craft &amp; Spoil trade commission:
	 * transfers real Adena from {@code buyer}'s {@code PcInventory} to an active Dwarven Crafter/Spoiler citizen's
	 * {@code PcInventory}, recording both sides in {@link EconomyProgressionAuditor}.
	 */
	public boolean commissionDwarvenCraft(AutonomousCitizen buyer, int targetTier, long commissionCost)
	{
		if ((buyer == null) || (buyer.getPlayer() == null))
		{
			return false;
		}
		
		AutonomousCitizen bestDwarf = null;
		for (AutonomousCitizen candidate : _citizens.values())
		{
			if ((candidate == null) || (candidate == buyer) || (candidate.getPlayer() == null))
			{
				continue;
			}
			if (candidate.isCrafterCitizen())
			{
				bestDwarf = candidate;
				break;
			}
			if (candidate.isSpoilerCitizen() && (bestDwarf == null))
			{
				bestDwarf = candidate;
			}
		}
		
		if (bestDwarf == null)
		{
			// Auto-spawn a Dwarven Crafter (Maestro aspiration) if none is currently online so the server economy stays self-sustaining
			bestDwarf = spawnCitizen(PlayerClass.MAESTRO);
			if (bestDwarf == null)
			{
				return false;
			}
		}
		
		final Player buyerPlayer = buyer.getPlayer();
		final Player dwarfPlayer = bestDwarf.getPlayer();
		final String gradeName = (targetTier >= 5) ? "S-Grade + SA" : "A-Grade Crafted";
		final long actualTransfer = Math.max(0L, Math.min(buyerPlayer.getAdena(), commissionCost));
		if (actualTransfer > 0L)
		{
			buyerPlayer.reduceAdena(ItemProcessType.TRANSFER, actualTransfer, dwarfPlayer, false);
			dwarfPlayer.addAdena(ItemProcessType.TRANSFER, actualTransfer, buyerPlayer, false);
			EconomyProgressionAuditor.getInstance().recordDwarvenCraft(dwarfPlayer, buyerPlayer, gradeName, actualTransfer);
		}
		
		bestDwarf.addSpoilAndCraftPoints(3);
		bestDwarf.setLastAction("Crafted & sold " + gradeName + " set to " + buyerPlayer.getName() + " (+" + actualTransfer + " Adena)");
		buyer.setLastAction("Commissioned " + gradeName + " craft from Dwarf " + dwarfPlayer.getName());
		return true;
	}
	
	/**
	 * Groups {@code requestorCitizen} with other active citizens into a real {@link Party}
	 * to contest Subclass Raid Bosses (Cabrio, Hallate, Kernon, Golkonda) or Noblesse Raid Boss (Barakiel).
	 */
	public void joinOrFormQuestRaidParty(AutonomousCitizen requestorCitizen)
	{
		if ((requestorCitizen == null) || (requestorCitizen.getPlayer() == null))
		{
			return;
		}
		final Player leader = requestorCitizen.getPlayer();
		Party party = leader.getParty();
		if (party == null)
		{
			party = new Party(leader, PartyDistributionType.FINDERS_KEEPERS);
			leader.setParty(party);
		}
		for (AutonomousCitizen other : _citizens.values())
		{
			if ((other == null) || (other == requestorCitizen) || (party.getMemberCount() >= 9))
			{
				continue;
			}
			final Player op = other.getPlayer();
			if ((op != null) && !op.isDead() && !op.isInParty() && (op.getLevel() >= 55))
			{
				op.joinParty(party);
			}
		}
	}
	
	private String generateUniqueCitizenName()
	{
		for (int attempt = 0; attempt < 96; attempt++)
		{
			final String base = CITIZEN_NAME_POOL[ThreadLocalRandom.current().nextInt(CITIZEN_NAME_POOL.length)];
			final String candidate = (attempt < 80)
				? base
				: (base + ThreadLocalRandom.current().nextInt(10, 99));
			if (!CharInfoTable.getInstance().doesCharNameExist(candidate) && (World.getPlayer(candidate) == null))
			{
				return candidate;
			}
		}
		return "Citizen" + (1000 + ThreadLocalRandom.current().nextInt(9000));
	}
	
	/**
	 * Flushes all currently active citizens and their {@code character_variables} to MariaDB with
	 * {@code AUTONOMOUS_ACTIVE = true} so they automatically survive a server restart.
	 */
	public int storeAllCitizens()
	{
		int stored = 0;
		for (AutonomousCitizen citizen : _citizens.values())
		{
			if ((citizen == null) || (citizen.getPlayer() == null))
			{
				continue;
			}
			try
			{
				citizen.saveStateToVariables(true);
				citizen.getPlayer().storeMe();
				stored++;
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "AutonomousPlayerManager: Failed to store citizen " + citizen.getPlayer().getName() + ": " + e.getMessage());
			}
		}
		if (stored > 0)
		{
			LOGGER.info("AutonomousPlayerManager: Stored " + stored + " active Autonomous Citizen(s) to MariaDB for restart persistence.");
		}
		return stored;
	}
	
	/**
	 * Automatically restores all {@code autonomous_qa} characters from MariaDB whose {@code AUTONOMOUS_ACTIVE}
	 * variable is {@code true}, while leaving hibernated characters in MariaDB Cold Storage.
	 */
	public synchronized void restoreActiveCitizensOnBoot()
	{
		if (!_bootRestoreDone.compareAndSet(false, true))
		{
			return;
		}
		final List<Integer> activeCharIds = new ArrayList<>();
		final String sql = "SELECT c.charId, v.val FROM characters c "
			+ "LEFT JOIN character_variables v ON c.charId = v.charId AND v.var = 'AUTONOMOUS_ACTIVE' "
			+ "WHERE c.account_name = ?";
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(sql))
		{
			ps.setString(1, CITIZEN_ACCOUNT);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final int charId = rs.getInt("charId");
					final String activeVal = rs.getString("val");
					// Restore if AUTONOMOUS_ACTIVE is explicitly "true" or "1"
					if ("true".equalsIgnoreCase(activeVal) || "1".equals(activeVal))
					{
						activeCharIds.add(charId);
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "AutonomousPlayerManager: Error querying active citizens on boot: " + e.getMessage(), e);
			return;
		}
		
		int restored = 0;
		for (int charId : activeCharIds)
		{
			if (wakeCitizenFromColdStorage(charId, null) != null)
			{
				restored++;
			}
		}
		if (restored > 0)
		{
			LOGGER.info("AutonomousPlayerManager: Auto-restored " + restored + " active Autonomous Citizen(s) from MariaDB on server startup.");
		}
	}
	
	/**
	 * Queries MariaDB for all {@code autonomous_qa} citizens currently resting in Cold Storage (not active in {@code _citizens}).
	 */
	public List<ColdStorageCitizenSummary> getColdStorageCitizens()
	{
		final List<ColdStorageCitizenSummary> result = new ArrayList<>();
		final String sql = "SELECT c.charId, c.char_name, c.level, c.classid, "
			+ "v_asp.val AS asp, v_ipd.val AS ipd, v_egt.val AS egt, v_k.val AS k, v_svo.val AS svo "
			+ "FROM characters c "
			+ "LEFT JOIN character_variables v_asp ON c.charId = v_asp.charId AND v_asp.var = 'AUTONOMOUS_ASPIRATION' "
			+ "LEFT JOIN character_variables v_ipd ON c.charId = v_ipd.charId AND v_ipd.var = 'AUTONOMOUS_IPD' "
			+ "LEFT JOIN character_variables v_egt ON c.charId = v_egt.charId AND v_egt.var = 'AUTONOMOUS_EGT' "
			+ "LEFT JOIN character_variables v_k ON c.charId = v_k.charId AND v_k.var = 'AUTONOMOUS_LEVEL_K' "
			+ "LEFT JOIN character_variables v_svo ON c.charId = v_svo.charId AND v_svo.var = 'AUTONOMOUS_SVO' "
			+ "WHERE c.account_name = ? ORDER BY c.level DESC, c.charId DESC";
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(sql))
		{
			ps.setString(1, CITIZEN_ACCOUNT);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final int charId = rs.getInt("charId");
					if (_citizens.containsKey(charId) || (World.getPlayer(charId) != null))
					{
						continue;
					}
					final String name = rs.getString("char_name");
					final int level = rs.getInt("level");
					final int classId = rs.getInt("classid");
					PlayerClass pc = PlayerClass.getPlayerClass(classId);
					if (pc == null)
					{
						pc = PlayerClass.FIGHTER;
					}
					final String asp = (rs.getString("asp") != null) ? rs.getString("asp") : pc.name();
					final String ipd = (rs.getString("ipd") != null) ? rs.getString("ipd") : "TFT";
					final String egt = (rs.getString("egt") != null) ? rs.getString("egt") : "Bourg";
					final String k = (rs.getString("k") != null) ? rs.getString("k") : "L1";
					final String svo = (rs.getString("svo") != null) ? rs.getString("svo") : "Pro";
					result.add(new ColdStorageCitizenSummary(charId, name, level, pc, asp, "[" + ipd + "/" + egt + "/" + k + "/" + svo + "]"));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "AutonomousPlayerManager: Failed to query Cold Storage citizens: " + e.getMessage());
		}
		return result;
	}
	
	/**
	 * Wakes a citizen from MariaDB Cold Storage by {@code charId}, sanitizes its appearance bounds,
	 * restores all learned skills/items/4D phenotype variables, and spawns it into the world.
	 */
	public AutonomousCitizen wakeCitizenFromColdStorage(int charId, Player sponsor)
	{
		final AutonomousCitizen alreadyActive = _citizens.get(charId);
		if (alreadyActive != null)
		{
			return alreadyActive;
		}
		Player player = null;
		try
		{
			player = Player.load(charId);
			if (player == null)
			{
				return null;
			}
			sanitizeCitizenAppearance(player);
			if ((player.getTitle() != null) && player.getTitle().startsWith("->"))
			{
				player.setTitle("");
			}
			if (sponsor != null)
			{
				final int ox = ThreadLocalRandom.current().nextInt(-75, 76);
				final int oy = ThreadLocalRandom.current().nextInt(-75, 76);
				player.setXYZInvisible(sponsor.getX() + ox, sponsor.getY() + oy, sponsor.getZ() + 5);
				player.setInstanceId(sponsor.getInstanceId());
				player.setHeading(sponsor.getHeading());
			}
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			player.setCurrentCp(player.getMaxCp());
			player.setOfflinePlay(true);
			player.setOnlineStatus(true, true);
			player.restoreEffects();
			player.spawnMe(player.getX(), player.getY(), player.getZ());
			player.setRunning();
			player.broadcastUserInfo();
			if (sponsor != null)
			{
				player.sendInfo(sponsor);
			}
			
			final AutonomousCitizen citizen = new AutonomousCitizen(player);
			if (sponsor != null)
			{
				citizen.holdNearSponsor(sponsor.getName(), 15_000L);
			}
			citizen.saveStateToVariables(true);
			player.storeMe();
			_citizens.put(player.getObjectId(), citizen);
			start();
			LOGGER.info("AutonomousPlayerManager: Woke Citizen " + player.getName() + " (Lv " + player.getLevel() + " " + player.getPlayerClass() + " -> " + citizen.getTargetEndgameClass() + " " + citizen.getPhenotypeBadge() + ") from MariaDB Cold Storage.");
			return citizen;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "AutonomousPlayerManager: Error waking citizen charId=" + charId + " from Cold Storage: " + e.getMessage(), e);
			if (player != null)
			{
				cleanupPlayer(player);
			}
			return null;
		}
	}
	
	/**
	 * Wakes up to {@code maxCount} citizens from MariaDB Cold Storage and recalls them to {@code sponsor} (if non-null).
	 */
	public int wakeAllFromColdStorage(int maxCount, Player sponsor)
	{
		final List<ColdStorageCitizenSummary> coldList = getColdStorageCitizens();
		int woken = 0;
		for (ColdStorageCitizenSummary summary : coldList)
		{
			if (woken >= maxCount)
			{
				break;
			}
			if (wakeCitizenFromColdStorage(summary.charId(), sponsor) != null)
			{
				woken++;
			}
		}
		return woken;
	}
	
	public boolean isAutonomousCitizen(Player player)
	{
		return (player != null) && _citizens.containsKey(player.getObjectId());
	}
	
	public boolean onPartyInvite(Player requestor, Player target, int distributionTypeId)
	{
		if ((requestor == null) || (target == null))
		{
			return false;
		}
		final AutonomousCitizen citizen = _citizens.get(target.getObjectId());
		if (citizen == null)
		{
			return false;
		}
		
		Party party = requestor.getParty();
		if (party == null)
		{
			PartyDistributionType distType = PartyDistributionType.findById(distributionTypeId);
			if (distType == null)
			{
				distType = PartyDistributionType.FINDERS_KEEPERS;
			}
			party = new Party(requestor, distType);
			requestor.setParty(party);
		}
		else if (party.getMemberCount() >= 9)
		{
			return false;
		}
		
		requestor.sendPacket(new JoinParty(1));
		target.joinParty(party);
		citizen.setMacroState(CitizenMacroState.PARTY_COOP);
		return true;
	}
	
	public String formRaidBossExpedition(Player gmTrigger)
	{
		final List<AutonomousCitizen> active = getActiveCitizens();
		if (active.isEmpty())
		{
			return "No active Autonomous Citizens spawned. Spawn citizens first!";
		}
		
		int avgLvl = 0;
		for (AutonomousCitizen c : active)
		{
			avgLvl += c.getPlayer().getLevel();
		}
		avgLvl = Math.max(20, avgLvl / active.size());
		
		RaidBoss bestBoss = null;
		int bestDiff = Integer.MAX_VALUE;
		for (RaidBoss boss : RaidBossSpawnManager.getInstance().getBosses().values())
		{
			if ((boss == null) || boss.isAlikeDead() || !boss.isSpawned())
			{
				continue;
			}
			final int diff = Math.abs(boss.getLevel() - avgLvl);
			if (diff < bestDiff)
			{
				bestDiff = diff;
				bestBoss = boss;
			}
		}
		
		if (bestBoss == null)
		{
			return "No live open-world Raid Boss currently spawned in RaidBossSpawnManager.";
		}
		
		Player leader = null;
		Party expeditionParty = null;
		int deployed = 0;
		for (AutonomousCitizen citizen : active)
		{
			final Player cp = citizen.getPlayer();
			if ((cp == null) || cp.isDead())
			{
				continue;
			}
			if (leader == null)
			{
				leader = cp;
				if (!leader.isInParty())
				{
					expeditionParty = new Party(leader, PartyDistributionType.FINDERS_KEEPERS);
					leader.setParty(expeditionParty);
				}
				else
				{
					expeditionParty = leader.getParty();
				}
			}
			else if (!cp.isInParty() && (expeditionParty != null) && (expeditionParty.getMemberCount() < 9))
			{
				cp.joinParty(expeditionParty);
			}
			citizen.assignRaidBossExpedition(bestBoss);
			deployed++;
			if (deployed >= 9)
			{
				break;
			}
		}
		
		return "Formed " + deployed + "-citizen Raid Expedition targeting " + bestBoss.getName() + " (Lv " + bestBoss.getLevel() + ").";
	}
	
	/**
	 * Moves an active citizen into MariaDB Cold Storage ({@code AUTONOMOUS_ACTIVE = false}),
	 * preserving all character progress, items, skills, and 4D behavioral phenotype variables.
	 */
	public boolean despawnCitizen(int objectId)
	{
		final AutonomousCitizen citizen = _citizens.remove(objectId);
		if (citizen == null)
		{
			return false;
		}
		citizen.saveStateToVariables(false);
		cleanupPlayer(citizen.getPlayer());
		return true;
	}
	
	/**
	 * Moves all active citizens into MariaDB Cold Storage ({@code AUTONOMOUS_ACTIVE = false}).
	 */
	public int despawnAll()
	{
		int count = 0;
		for (Integer objId : new ArrayList<>(_citizens.keySet()))
		{
			if (despawnCitizen(objId))
			{
				count++;
			}
		}
		return count;
	}
	
	private void cleanupPlayer(Player player)
	{
		if (player == null)
		{
			return;
		}
		try
		{
			if (player.isInParty())
			{
				player.leaveParty();
			}
			player.setOfflinePlay(false);
			player.setOnlineStatus(false, true);
			player.decayMe();
			player.storeMe();
			World.removeObject(player);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.FINE, "AutonomousPlayerManager: Error cleaning up citizen " + player.getName() + ": " + e.getMessage());
		}
	}
	
	public List<AutonomousCitizen> getActiveCitizens()
	{
		return new ArrayList<>(_citizens.values());
	}
	
	public AutonomousCitizen getCitizen(int objectId)
	{
		return _citizens.get(objectId);
	}
	
	public static AutonomousPlayerManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AutonomousPlayerManager INSTANCE = new AutonomousPlayerManager();
	}
}
