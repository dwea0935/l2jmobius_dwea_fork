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

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.ai.autonomous.GameplayAnomalyAuditor.AnomalyType;
import org.l2jmobius.gameserver.ai.autonomous.ProgressionKnowledgeGraph.GearLoadout;
import org.l2jmobius.gameserver.ai.autonomous.ProgressionKnowledgeGraph.HuntingZone;
import org.l2jmobius.gameserver.ai.autonomous.ProgressionKnowledgeGraph.VillageHub;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;

/**
 * Real-Player Autonomous Citizen ({@code Lv 1 -> 85}) with a Hierarchical Macro-Lifecycle State Machine
 * and Self-Reporting QA Anomaly Detection.
 */
public final class AutonomousCitizen
{
	public enum CitizenMacroState
	{
		SPAWN_AND_ORIENT,
		TOWN_ROUTINE,
		GUILD_SKILL_LEARN,
		CLASS_TRANSFER,
		TRAVEL_TO_HUNTING_ZONE,
		HUNT_AND_FARM,
		RETURN_TO_TOWN,
		PARTY_COOP
	}
	
	private static final int[] TOWN_BUFF_SKILL_IDS =
	{
		1204, // Wind Walk
		1040, // Shield
		1036, // Magic Barrier
		1045, // Bless the Body
		1048, // Bless the Soul
		1068, // Might
		1086, // Haste
		1059, // Empower
		1085, // Acumen
		1268 // Vampiric Rage
	};
	
	private final Player _player;
	private volatile CitizenMacroState _macroState = CitizenMacroState.SPAWN_AND_ORIENT;
	private volatile HuntingZone _currentZone;
	private volatile String _lastAction = "Spawned in Starting Village";
	private int _equippedGradeTier = -1;
	private int _lastX;
	private int _lastY;
	private int _lastZ;
	private int _stuckTicks = 0;
	private long _lastExpSnapshot = 0L;
	private long _lastAdenaSnapshot = 0L;
	private int _killsWithoutReward = 0;
	private int _lastTrackedTargetId = 0;
	private double _lastTrackedTargetHp = 0.0;
	private long _lastPotionMs = 0L;
	private long _lastManaPotionMs = 0L;
	private final Set<Integer> _blacklistedTargets = ConcurrentHashMap.newKeySet();
	
	public AutonomousCitizen(Player player)
	{
		_player = player;
		final VillageHub hub = ProgressionKnowledgeGraph.getStartingVillage(player.getRace());
		_currentZone = ProgressionKnowledgeGraph.selectHuntingZone(player.getLevel(), player.getRace());
		_lastAction = "Oriented at " + hub.villageName();
		_lastX = player.getX();
		_lastY = player.getY();
		_lastZ = player.getZ();
		_lastExpSnapshot = player.getExp();
		_lastAdenaSnapshot = player.getAdena();
	}
	
	public Player getPlayer()
	{
		return _player;
	}
	
	public CitizenMacroState getMacroState()
	{
		return _macroState;
	}
	
	public void setMacroState(CitizenMacroState state)
	{
		_macroState = state;
	}
	
	public String getCurrentZoneName()
	{
		return (_currentZone != null) ? _currentZone.name() : "Town";
	}
	
	public String getLastAction()
	{
		return _lastAction;
	}
	
	public int getEquippedGradeTier()
	{
		return Math.max(0, _equippedGradeTier);
	}
	
	public void tick()
	{
		if ((_player == null) || !_player.isOnline())
		{
			return;
		}
		
		// 1. Death recovery -> revive to town and run TOWN_ROUTINE
		if (_player.isDead())
		{
			_player.doRevive();
			_player.setCurrentHp(_player.getMaxHp());
			_player.setCurrentMp(_player.getMaxMp());
			_player.setCurrentCp(_player.getMaxCp());
			final Location townLoc = (_currentZone != null)
				? _currentZone.townCenter()
				: ProgressionKnowledgeGraph.getStartingVillage(_player.getRace()).spawnPoint();
			_player.teleToLocation(townLoc, true);
			_macroState = CitizenMacroState.TOWN_ROUTINE;
			_lastAction = "Revived -> Returned to town";
			return;
		}
		
		// 2. Auto-sustain potions (Healing Potion 1061 & Mana Potion 728)
		sustainVitalsAndShots();
		
		// 3. Party Co-op override whenever grouped with a leader
		if (_player.isInParty() && (_player.getParty().getLeader() != _player))
		{
			_macroState = CitizenMacroState.PARTY_COOP;
			tickPartyCoop();
			return;
		}
		else if (_macroState == CitizenMacroState.PARTY_COOP)
		{
			_macroState = CitizenMacroState.TOWN_ROUTINE;
		}
		
		switch (_macroState)
		{
			case SPAWN_AND_ORIENT -> tickSpawnAndOrient();
			case TOWN_ROUTINE -> tickTownRoutine();
			case GUILD_SKILL_LEARN -> tickGuildSkillLearn();
			case CLASS_TRANSFER -> tickClassTransfer();
			case TRAVEL_TO_HUNTING_ZONE -> tickTravelToHuntingZone();
			case HUNT_AND_FARM -> tickHuntAndFarm();
			case RETURN_TO_TOWN -> tickReturnToTown();
			default -> tickTownRoutine();
		}
	}
	
	private void sustainVitalsAndShots()
	{
		final long now = System.currentTimeMillis();
		final double hpRatio = _player.getCurrentHp() / Math.max(1.0, _player.getMaxHp());
		final double mpRatio = _player.getCurrentMp() / Math.max(1.0, _player.getMaxMp());
		
		if ((hpRatio < 0.68) && ((now - _lastPotionMs) >= 3000L))
		{
			_lastPotionMs = now;
			consumeItemIfPresent(1539, 1061, 1060);
			_player.setCurrentHp(Math.min(_player.getMaxHp(), _player.getCurrentHp() + (_player.getMaxHp() * 0.25)));
			_player.broadcastPacket(new MagicSkillUse(_player, _player, 2037, 1, 0, 0));
		}
		
		if ((mpRatio < 0.62) && ((now - _lastManaPotionMs) >= 2000L))
		{
			_lastManaPotionMs = now;
			if (consumeItemIfPresent(728, 726))
			{
				_player.setCurrentMp(Math.min(_player.getMaxMp(), _player.getCurrentMp() + 400.0));
				_player.broadcastPacket(new MagicSkillUse(_player, _player, 2288, 1, 0, 0));
			}
		}
		
		_player.rechargeShots(true, true);
	}
	
	private boolean consumeItemIfPresent(int... itemIds)
	{
		for (int itemId : itemIds)
		{
			final Item item = _player.getInventory().getItemByItemId(itemId);
			if ((item != null) && (item.getCount() > 0))
			{
				_player.destroyItem(ItemProcessType.NONE, item.getObjectId(), 1, _player, false);
				return true;
			}
		}
		return false;
	}
	
	private void tickSpawnAndOrient()
	{
		ensureFairPlayGearAndConsumables(true);
		applyTownSupportBuffs();
		_macroState = CitizenMacroState.TOWN_ROUTINE;
		_lastAction = "Oriented & equipped No-Grade starter kit";
	}
	
	private void tickTownRoutine()
	{
		// 1. Check Class Transfer eligibility (Lv 20, Lv 40, Lv 76)
		final PlayerClass pc = _player.getPlayerClass();
		final int classLvl = (pc != null) ? pc.level() : 0;
		final int lvl = _player.getLevel();
		if (((lvl >= 20) && (classLvl == 0)) || ((lvl >= 40) && (classLvl == 1)) || ((lvl >= 76) && (classLvl == 2)))
		{
			if ((pc != null) && !pc.getNextClasses().isEmpty())
			{
				_macroState = CitizenMacroState.CLASS_TRANSFER;
				return;
			}
		}
		
		// 2. Check Guild Master skill learning
		final Collection<SkillLearn> learnable = SkillTreeData.getInstance().getAvailableSkills(_player, _player.getPlayerClass(), false, false);
		if ((learnable != null) && !learnable.isEmpty())
		{
			_macroState = CitizenMacroState.GUILD_SKILL_LEARN;
			return;
		}
		
		// 3. Upgrade gear up to B-Grade Luxury Shop ceiling & restock consumables
		ensureFairPlayGearAndConsumables(false);
		
		// 4. Refresh buffs in town
		applyTownSupportBuffs();
		
		// 5. Select level-appropriate hunting zone and travel
		_currentZone = ProgressionKnowledgeGraph.selectHuntingZone(_player.getLevel(), _player.getRace());
		_macroState = CitizenMacroState.TRAVEL_TO_HUNTING_ZONE;
		_lastAction = "Town routine complete -> Heading to " + _currentZone.name();
	}
	
	private void tickGuildSkillLearn()
	{
		final Collection<SkillLearn> learnable = SkillTreeData.getInstance().getAvailableSkills(_player, _player.getPlayerClass(), false, false);
		if ((learnable == null) || learnable.isEmpty())
		{
			_macroState = CitizenMacroState.TOWN_ROUTINE;
			return;
		}
		
		int learnedCount = 0;
		for (SkillLearn sl : learnable)
		{
			final Skill skill = SkillData.getInstance().getSkill(sl.getSkillId(), sl.getSkillLevel());
			if (skill == null)
			{
				GameplayAnomalyAuditor.getInstance().report(
					_player,
					AnomalyType.SKILL_TREE_GAP,
					getCurrentZoneName(),
					"Missing SkillData entry for skillId=" + sl.getSkillId() + " lvl=" + sl.getSkillLevel());
				continue;
			}
			final long spCost = sl.getLevelUpSp();
			if (_player.getSp() >= spCost)
			{
				_player.setSp(_player.getSp() - spCost);
			}
			_player.addSkill(skill, true);
			learnedCount++;
		}
		
		if (learnedCount > 0)
		{
			_player.sendSkillList();
			_lastAction = "Learned " + learnedCount + " Guild Master skill(s)";
		}
		_macroState = CitizenMacroState.TOWN_ROUTINE;
	}
	
	private void tickClassTransfer()
	{
		final PlayerClass currentClass = _player.getPlayerClass();
		final PlayerClass nextClass = ProgressionKnowledgeGraph.selectNextClassTransfer(currentClass);
		if (nextClass == null)
		{
			GameplayAnomalyAuditor.getInstance().report(
				_player,
				AnomalyType.SKILL_TREE_GAP,
				getCurrentZoneName(),
				"No child PlayerClass found for class transfer from " + currentClass + " at Lv " + _player.getLevel());
			_macroState = CitizenMacroState.TOWN_ROUTINE;
			return;
		}
		
		_player.setPlayerClass(nextClass.getId());
		if (!_player.isSubClassActive())
		{
			_player.setBaseClass(nextClass.getId());
		}
		
		for (SkillLearn sl : SkillTreeData.getInstance().getAvailableSkills(_player, nextClass, false, true))
		{
			final Skill skill = SkillData.getInstance().getSkill(sl.getSkillId(), sl.getSkillLevel());
			if (skill != null)
			{
				_player.addSkill(skill, true);
			}
		}
		
		_player.broadcastUserInfo();
		_lastAction = "Completed Class Transfer -> " + nextClass.name();
		_macroState = CitizenMacroState.GUILD_SKILL_LEARN;
	}
	
	private void tickTravelToHuntingZone()
	{
		if (_currentZone == null)
		{
			_currentZone = ProgressionKnowledgeGraph.selectHuntingZone(_player.getLevel(), _player.getRace());
		}
		
		final Location center = _currentZone.huntingCenter();
		final int scatterX = ThreadLocalRandom.current().nextInt(-220, 221);
		final int scatterY = ThreadLocalRandom.current().nextInt(-220, 221);
		final Location dest = new Location(center.getX() + scatterX, center.getY() + scatterY, center.getZ());
		
		// Gatekeeper teleport fee above Lv 40
		if ((_player.getLevel() > 40) && (_player.getAdena() >= 5_000L))
		{
			_player.reduceAdena(ItemProcessType.FEE, 5_000L, _player, false);
		}
		
		_player.teleToLocation(dest, true);
		_player.setRunning();
		_blacklistedTargets.clear();
		_stuckTicks = 0;
		_lastX = _player.getX();
		_lastY = _player.getY();
		_lastZ = _player.getZ();
		_lastExpSnapshot = _player.getExp();
		_lastAdenaSnapshot = _player.getAdena();
		_macroState = CitizenMacroState.HUNT_AND_FARM;
		_lastAction = "Arrived at " + _currentZone.name() + " (Lv " + _currentZone.minLevel() + "-" + _currentZone.maxLevel() + ")";
	}
	
	private void tickReturnToTown()
	{
		final Location townLoc = (_currentZone != null)
			? _currentZone.townCenter()
			: ProgressionKnowledgeGraph.getStartingVillage(_player.getRace()).spawnPoint();
		consumeItemIfPresent(736); // Scroll of Escape
		_player.teleToLocation(townLoc, true);
		_macroState = CitizenMacroState.TOWN_ROUTINE;
		_lastAction = "Returned to " + ((_currentZone != null) ? _currentZone.townName() : "Town");
	}
	
	private void tickHuntAndFarm()
	{
		final int lvl = _player.getLevel();
		final PlayerClass pc = _player.getPlayerClass();
		final int classLvl = (pc != null) ? pc.level() : 0;
		
		// 1. Return to town if ready for class transfer or outgrew current hunting zone
		if ((((lvl >= 20) && (classLvl == 0)) || ((lvl >= 40) && (classLvl == 1)) || ((lvl >= 76) && (classLvl == 2)))
			|| ((_currentZone != null) && (lvl > (_currentZone.maxLevel() + 2)) && (lvl < 85))
			|| (ProgressionKnowledgeGraph.getTargetGradeTierForLevel(lvl) > _equippedGradeTier))
		{
			_macroState = CitizenMacroState.RETURN_TO_TOWN;
			_lastAction = "Milestone Lv " + lvl + " reached -> Returning to town";
			return;
		}
		
		if (_player.isCastingNow())
		{
			return;
		}
		
		// 2. Pick up nearby dropped items
		final Item droppedItem = World.getFirstVisibleObjectInRange(_player, Item.class, 220, item ->
			item.isSpawned()
			&& (!item.isProtected() || (item.getOwnerId() == _player.getObjectId()))
			&& GeoEngine.getInstance().canMoveToTarget(_player.getX(), _player.getY(), _player.getZ(), item.getX(), item.getY(), item.getZ(), _player.getInstanceId()));
		if (droppedItem != null)
		{
			if (_player.calculateDistance2D(droppedItem) > 65)
			{
				if (!_player.isMoving())
				{
					_player.getAI().setIntentionMoveTo(droppedItem);
				}
			}
			else
			{
				_player.doPickupItem(droppedItem);
				_lastAction = "Picked up " + droppedItem.getName();
			}
			return;
		}
		
		// 3. Check if current target died -> verify XP/Adena progression & starvation QA check
		final WorldObject currentObj = _player.getTarget();
		if ((currentObj != null) && currentObj.isCreature())
		{
			final Creature currentTarget = currentObj.asCreature();
			if (currentTarget.isAlikeDead())
			{
				onTargetDefeated(currentTarget);
				_player.setTarget(null);
			}
		}
		
		// 4. Acquire or validate live monster target
		Creature target = null;
		if ((_player.getTarget() != null) && _player.getTarget().isCreature())
		{
			final Creature candidate = _player.getTarget().asCreature();
			if (!candidate.isAlikeDead() && isValidHuntTarget(candidate))
			{
				target = candidate;
			}
		}
		
		if (target == null)
		{
			target = World.getNearestVisibleObjectInRange(_player, Creature.class, 2000, this::isValidHuntTarget);
			if (target != null)
			{
				_player.setTarget(target);
				_lastTrackedTargetId = target.getObjectId();
				_lastTrackedTargetHp = target.getCurrentHp();
				_stuckTicks = 0;
			}
		}
		
		if (target == null)
		{
			// Roam around hunting zone center
			if (!_player.isMoving() && (_currentZone != null))
			{
				final Location c = _currentZone.huntingCenter();
				final int rx = c.getX() + ThreadLocalRandom.current().nextInt(-450, 451);
				final int ry = c.getY() + ThreadLocalRandom.current().nextInt(-450, 451);
				final Location roamLoc = GeoEngine.getInstance().getValidLocation(_player.getX(), _player.getY(), _player.getZ(), rx, ry, c.getZ(), _player.getInstanceId());
				_player.getAI().setIntentionMoveTo(roamLoc);
				_lastAction = "Patrolling " + _currentZone.name();
			}
			return;
		}
		
		// 5. GeoEngine stuck detection while pursuing target
		final double dist = _player.calculateDistance2D(target);
		final boolean isMage = (_player.getPlayerClass() != null) && _player.getPlayerClass().isMage();
		final double engageRange = isMage ? 650.0 : 110.0;
		
		if (dist > engageRange)
		{
			final int movedDistSq = ((_player.getX() - _lastX) * (_player.getX() - _lastX))
				+ ((_player.getY() - _lastY) * (_player.getY() - _lastY));
			_lastX = _player.getX();
			_lastY = _player.getY();
			_lastZ = _player.getZ();
			
			if (movedDistSq < 25)
			{
				_stuckTicks++;
				if (_stuckTicks >= 4)
				{
					GameplayAnomalyAuditor.getInstance().report(
						_player,
						AnomalyType.GEO_STUCK_LOOP,
						getCurrentZoneName(),
						"Stuck for " + _stuckTicks + " ticks pursuing " + target.getName() + " at (" + target.getX() + "," + target.getY() + "," + target.getZ() + ")");
					_blacklistedTargets.add(target.getObjectId());
					_player.setTarget(null);
					_stuckTicks = 0;
					if (_currentZone != null)
					{
						final Location c = _currentZone.huntingCenter();
						_player.getAI().setIntentionMoveTo(new Location(c.getX() + ThreadLocalRandom.current().nextInt(-300, 301), c.getY() + ThreadLocalRandom.current().nextInt(-300, 301), c.getZ()));
					}
					return;
				}
			}
			else
			{
				_stuckTicks = 0;
			}
			
			if (!_player.isMoving())
			{
				_player.getAI().setIntentionMoveTo(target);
			}
			_lastAction = "Approaching " + target.getName() + " (" + (int) dist + "u)";
			return;
		}
		
		// 6. Execute offensive skill or physical attack
		engageTarget(target, isMage);
	}
	
	private void tickPartyCoop()
	{
		final Party party = _player.getParty();
		if (party == null)
		{
			_macroState = CitizenMacroState.TOWN_ROUTINE;
			return;
		}
		final Player leader = party.getLeader();
		if ((leader == null) || (leader == _player))
		{
			_macroState = CitizenMacroState.TOWN_ROUTINE;
			return;
		}
		
		if (_player.calculateDistance3D(leader) > 2500)
		{
			_player.teleToLocation(leader.getX() + 60, leader.getY() + 60, leader.getZ(), leader.getHeading(), leader.getInstanceId());
			_lastAction = "Regrouped with party leader " + leader.getName();
			return;
		}
		
		final WorldObject leaderTargetObj = leader.getTarget();
		if ((leaderTargetObj != null) && leaderTargetObj.isCreature())
		{
			final Creature leaderTarget = leaderTargetObj.asCreature();
			if (!leaderTarget.isAlikeDead() && (leaderTarget.isAttackable() || leaderTarget.isAutoAttackable(leader)))
			{
				_player.setTarget(leaderTarget);
				final boolean isMage = (_player.getPlayerClass() != null) && _player.getPlayerClass().isMage();
				engageTarget(leaderTarget, isMage);
				_lastAction = "Assisting " + leader.getName() + " -> " + leaderTarget.getName();
				return;
			}
		}
		
		if ((_player.calculateDistance2D(leader) > 160) && !_player.isMoving())
		{
			_player.getAI().setIntentionFollow(leader);
			_lastAction = "Following party leader " + leader.getName();
		}
	}
	
	private void engageTarget(Creature target, boolean isMage)
	{
		final Skill offensiveSkill = selectBestOffensiveSkill(target, isMage);
		if (offensiveSkill != null)
		{
			final double hpBefore = target.getCurrentHp();
			_player.useMagic(offensiveSkill, true, false);
			_lastAction = "Casting " + offensiveSkill.getName() + " on " + target.getName();
			
			// Check zero-damage anomaly if same target has not lost HP after repeated casts
			if ((_lastTrackedTargetId == target.getObjectId()) && (_lastTrackedTargetHp > 0.0) && (Math.abs(hpBefore - _lastTrackedTargetHp) < 0.1) && !_player.isCastingNow())
			{
				// Allow physical fallback if magic is blocked/resisted
				_player.getAI().setIntentionAttack(target);
			}
			_lastTrackedTargetId = target.getObjectId();
			_lastTrackedTargetHp = hpBefore;
			return;
		}
		
		_player.getAI().setIntentionAttack(target);
		_lastAction = "Attacking " + target.getName() + " (Lv " + target.getLevel() + ")";
	}
	
	private Skill selectBestOffensiveSkill(Creature target, boolean isMage)
	{
		Skill best = null;
		int bestPower = -1;
		for (Skill skill : _player.getAllSkills())
		{
			if ((skill == null) || skill.isPassive() || skill.isToggle() || _player.isSkillDisabled(skill))
			{
				continue;
			}
			if (_player.getCurrentMp() < (skill.getMpConsume() + skill.getMpInitialConsume() + 10))
			{
				continue;
			}
			final int castRange = skill.getCastRange();
			if (castRange <= 0)
			{
				continue;
			}
			final String nameLower = skill.getName().toLowerCase();
			final boolean isOffensiveName = nameLower.contains("strike")
				|| nameLower.contains("shot")
				|| nameLower.contains("wind")
				|| nameLower.contains("blaze")
				|| nameLower.contains("aqua")
				|| nameLower.contains("twister")
				|| nameLower.contains("prominence")
				|| nameLower.contains("hydro")
				|| nameLower.contains("hurricane")
				|| nameLower.contains("vampiric")
				|| nameLower.contains("drain")
				|| nameLower.contains("blow")
				|| nameLower.contains("slash")
				|| nameLower.contains("sonic")
				|| nameLower.contains("stun")
				|| nameLower.contains("soul");
			if (!isOffensiveName)
			{
				continue;
			}
			if (!isMage && (ThreadLocalRandom.current().nextInt(100) >= 40))
			{
				continue;
			}
			final int pwr = Math.max(1, skill.getMagicLevel());
			if (pwr >= bestPower)
			{
				bestPower = pwr;
				best = skill;
			}
		}
		return best;
	}
	
	private void onTargetDefeated(Creature defeated)
	{
		final long currentExp = _player.getExp();
		final long currentAdena = _player.getAdena();
		if ((currentExp > _lastExpSnapshot) || (currentAdena > _lastAdenaSnapshot))
		{
			_killsWithoutReward = 0;
			_lastExpSnapshot = currentExp;
			_lastAdenaSnapshot = currentAdena;
		}
		else
		{
			_killsWithoutReward++;
			if (_killsWithoutReward >= 10)
			{
				GameplayAnomalyAuditor.getInstance().report(
					_player,
					AnomalyType.DROP_OR_XP_STARVATION,
					getCurrentZoneName(),
					"Defeated 10 monsters (last: " + defeated.getName() + " Lv " + defeated.getLevel() + ") without XP or Adena gain");
				_killsWithoutReward = 0;
			}
		}
	}
	
	private boolean isValidHuntTarget(Creature creature)
	{
		if ((creature == null) || creature.isAlikeDead() || !creature.isMonster() || creature.isRaid())
		{
			return false;
		}
		if (_blacklistedTargets.contains(creature.getObjectId()))
		{
			return false;
		}
		if (!creature.isAutoAttackable(_player))
		{
			return false;
		}
		final int lvlDiff = Math.abs(creature.getLevel() - _player.getLevel());
		if ((lvlDiff > 9) && (_player.getLevel() > 10))
		{
			return false;
		}
		return (Math.abs(_player.getZ() - creature.getZ()) < 650)
			&& GeoEngine.getInstance().canSeeTarget(_player, creature)
			&& GeoEngine.getInstance().canMoveToTarget(_player.getX(), _player.getY(), _player.getZ(), creature.getX(), creature.getY(), creature.getZ(), _player.getInstanceId());
	}
	
	private void ensureFairPlayGearAndConsumables(boolean isInitialSpawn)
	{
		final int targetTier = ProgressionKnowledgeGraph.getTargetGradeTierForLevel(_player.getLevel());
		final GearLoadout loadout = ProgressionKnowledgeGraph.getFairPlayLoadout(_player.getPlayerClass(), targetTier);
		
		if (targetTier > _equippedGradeTier)
		{
			if (!isInitialSpawn && (_player.getAdena() < loadout.adenaCost()))
			{
				// Fair-play check: report if citizen reached grade milestone without enough Adena, then grant partial stipend only if starved
				GameplayAnomalyAuditor.getInstance().report(
					_player,
					AnomalyType.MULTISELL_MISSING_INGREDIENT,
					getCurrentZoneName(),
					"Citizen reached Lv " + _player.getLevel() + " (" + loadout.gradeLabel() + ") with " + _player.getAdena() + " Adena < required " + loadout.adenaCost());
			}
			else if (!isInitialSpawn && (loadout.adenaCost() > 0))
			{
				_player.reduceAdena(ItemProcessType.BUY, Math.min(_player.getAdena(), loadout.adenaCost()), _player, false);
			}
			
			equipItemIfValid(loadout.weaponId());
			equipItemIfValid(loadout.shieldOrSigilId());
			equipItemIfValid(loadout.helmetId());
			equipItemIfValid(loadout.chestId());
			equipItemIfValid(loadout.legsId());
			equipItemIfValid(loadout.glovesId());
			equipItemIfValid(loadout.bootsId());
			equipItemIfValid(loadout.necklaceId());
			equipItemIfValid(loadout.earringId());
			equipItemIfValid(loadout.ringId());
			
			_equippedGradeTier = targetTier;
			_player.broadcastUserInfo();
		}
		
		// Ensure consumables (Soulshots, Spiritshots, Arrows, Healing Potions, Mana Potions 728, Escape Scrolls)
		ensureInventoryStock(loadout.soulshotId(), 2000);
		ensureInventoryStock(loadout.spiritshotId(), 2000);
		if (loadout.arrowId() > 0)
		{
			ensureInventoryStock(loadout.arrowId(), 2000);
		}
		ensureInventoryStock(1061, 100); // Greater Healing Potion
		ensureInventoryStock(1539, 100); // Major Healing Potion
		ensureInventoryStock(728, 100); // Mana Potion (400 MP / 2s)
		ensureInventoryStock(736, 10); // Scroll of Escape
		
		_player.addAutoSoulShot(loadout.soulshotId());
		_player.addAutoSoulShot(loadout.spiritshotId());
		_player.rechargeShots(true, true);
	}
	
	private void equipItemIfValid(int itemId)
	{
		if ((itemId <= 0) || (ItemData.getInstance().getTemplate(itemId) == null))
		{
			return;
		}
		Item item = _player.getInventory().getItemByItemId(itemId);
		if (item == null)
		{
			item = _player.getInventory().addItem(ItemProcessType.BUY, itemId, 1, _player, null);
		}
		if ((item != null) && item.isEquipable() && !item.isEquipped())
		{
			_player.getInventory().equipItem(item);
		}
	}
	
	private void ensureInventoryStock(int itemId, long targetCount)
	{
		if ((itemId <= 0) || (ItemData.getInstance().getTemplate(itemId) == null))
		{
			return;
		}
		final Item existing = _player.getInventory().getItemByItemId(itemId);
		final long current = (existing != null) ? existing.getCount() : 0L;
		if (current < (targetCount / 3))
		{
			_player.getInventory().addItem(ItemProcessType.BUY, itemId, targetCount - current, _player, null);
		}
	}
	
	private void applyTownSupportBuffs()
	{
		for (int skillId : TOWN_BUFF_SKILL_IDS)
		{
			final int maxLvl = SkillData.getInstance().getMaxLevel(skillId);
			if (maxLvl > 0)
			{
				final Skill buff = SkillData.getInstance().getSkill(skillId, maxLvl);
				if (buff != null)
				{
					buff.applyEffects(_player, _player);
				}
			}
		}
		_player.setCurrentHp(_player.getMaxHp());
		_player.setCurrentMp(_player.getMaxMp());
		_player.setCurrentCp(_player.getMaxCp());
	}
}
