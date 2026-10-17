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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

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
 * Spoil/Craft Commissioning &amp; Self-Reporting QA/Economy Engine.
 */
public final class AutonomousPlayerManager
{
	private static final Logger LOGGER = Logger.getLogger(AutonomousPlayerManager.class.getName());
	private static final String CITIZEN_ACCOUNT = "autonomous_qa";
	private static final long TICK_INTERVAL_MS = 500L;
	
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
		"Aeloria", "Kaelen", "Thalorin", "Vaelis", "Draven", "Morwen",
		"Korin", "Bramwell", "Lyris", "Soren", "Elowen", "Garrick",
		"Nyxara", "Torvald", "Seraphis", "Caelum", "Valkor", "Isolde",
		"Ronan", "Miriel", "Dorian", "Talia", "Fenris", "Astrid"
	};
	
	private final Map<Integer, AutonomousCitizen> _citizens = new ConcurrentHashMap<>();
	private final AtomicInteger _spawnCounter = new AtomicInteger(0);
	private volatile ScheduledFuture<?> _tickTask;
	
	private AutonomousPlayerManager()
	{
		start();
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
	
	public AutonomousCitizen spawnCitizen()
	{
		final int index = _spawnCounter.getAndIncrement();
		final PlayerClass aspiration = ASPIRATION_ROTATION[index % ASPIRATION_ROTATION.length];
		return spawnCitizen(aspiration);
	}
	
	public int spawnCitizens(int count)
	{
		int spawned = 0;
		for (int i = 0; i < count; i++)
		{
			if (spawnCitizen() != null)
			{
				spawned++;
			}
		}
		return spawned;
	}
	
	public AutonomousCitizen spawnCitizen(PlayerClass requestedClassOrAspiration)
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
			
			final boolean isFemale = (chosenStartClass == PlayerClass.FEMALE_SOLDIER)
				|| ((chosenStartClass != PlayerClass.MALE_SOLDIER) && ThreadLocalRandom.current().nextBoolean());
			final byte face = (byte) ThreadLocalRandom.current().nextInt(0, 3);
			final byte hairColor = (byte) ThreadLocalRandom.current().nextInt(0, 4);
			final byte hairStyle = (byte) ThreadLocalRandom.current().nextInt(0, 4);
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
			final Location spawnLoc = hub.spawnPoint();
			final int offsetX = ThreadLocalRandom.current().nextInt(-90, 91);
			final int offsetY = ThreadLocalRandom.current().nextInt(-90, 91);
			
			World.addObject(player);
			player.setXYZInvisible(spawnLoc.getX() + offsetX, spawnLoc.getY() + offsetY, spawnLoc.getZ());
			player.setTitle("->" + aspiration.name());
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
			
			final AutonomousCitizen citizen = new AutonomousCitizen(player, aspiration);
			_citizens.put(player.getObjectId(), citizen);
			start();
			LOGGER.info("AutonomousPlayerManager: Spawned Lv 1 Citizen " + player.getName() + " [" + chosenStartClass + " -> " + aspiration + "] at " + hub.villageName());
			return citizen;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "AutonomousPlayerManager: Failed to spawn citizen: " + e.getMessage(), e);
			return null;
		}
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
		for (int attempt = 0; attempt < 50; attempt++)
		{
			final String base = CITIZEN_NAME_POOL[ThreadLocalRandom.current().nextInt(CITIZEN_NAME_POOL.length)];
			final String candidate = (attempt < CITIZEN_NAME_POOL.length)
				? base
				: (base + ThreadLocalRandom.current().nextInt(10, 99));
			if (!CharInfoTable.getInstance().doesCharNameExist(candidate) && (World.getPlayer(candidate) == null))
			{
				return candidate;
			}
		}
		return "Citizen" + (1000 + ThreadLocalRandom.current().nextInt(9000));
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
	
	public boolean despawnCitizen(int objectId)
	{
		final AutonomousCitizen citizen = _citizens.remove(objectId);
		if (citizen == null)
		{
			return false;
		}
		cleanupPlayer(citizen.getPlayer());
		return true;
	}
	
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
