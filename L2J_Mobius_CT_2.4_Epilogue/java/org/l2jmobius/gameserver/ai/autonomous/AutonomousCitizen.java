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
import java.util.Comparator;
import java.util.List;
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
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.actor.instance.RaidBoss;
import org.l2jmobius.gameserver.entity.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
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
		CLASS_TRANSFER_QUEST,
		CLASS_TRANSFER,
		TRAVEL_TO_HUNTING_ZONE,
		HUNT_AND_FARM,
		CATACOMB_SEVEN_SIGNS,
		DUNGEON_KAMALOKA_PAILAKA,
		RAID_BOSS_EXPEDITION,
		SUBCLASS_AND_CERTIFICATION,
		NOBLESSE_AND_OLYMPIAD,
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
	
	private static final int[] SPOILED_MATERIAL_IDS =
	{
		1864, // Stem
		1865, // Varnish
		1867, // Animal Skin
		1869, // Iron Ore
		1870, // Coal
		4039, // Mold Glue
		4042 // Enria
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
	private int _lastSkillCheckLevel = 0;
	private int _questKillsCurrent = 0;
	private int _questKillsTarget = 10;
	private int _spoilAndCraftPoints = 0;
	private boolean _sevenSignsRegistered = false;
	private int _completedPailakaTier = 0; // 1 = Lv36 Q128, 2 = Lv61 Q129, 3 = Lv73 Q144
	private boolean _subclassCertified = false;
	private volatile RaidBoss _expeditionRaidBoss = null;
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
	
	public String getGearTierBadge()
	{
		return switch (getEquippedGradeTier())
		{
			case 1 -> "D-Grade";
			case 2 -> "C-Grade";
			case 3 -> "B-Shop";
			case 4 -> "A-Craft";
			case 5 -> "S-SA";
			default -> "No-Grade";
		};
	}
	
	public String getProgressionBadges()
	{
		final StringBuilder sb = new StringBuilder(32);
		final PlayerClass pc = _player.getPlayerClass();
		final int classLvl = (pc != null) ? pc.level() : 0;
		sb.append(switch (classLvl)
		{
			case 1 -> "[1st]";
			case 2 -> "[2nd]";
			case 3 -> "[3rd]";
			default -> "[Base]";
		});
		if (_sevenSignsRegistered)
		{
			sb.append("[7S]");
		}
		if (_spoilAndCraftPoints > 0)
		{
			sb.append("[Craft:").append(_spoilAndCraftPoints).append("]");
		}
		if (_subclassCertified)
		{
			sb.append("[SubCert]");
		}
		if (_player.isNoble())
		{
			sb.append("[Noble]");
		}
		return sb.toString();
	}
	
	public void assignRaidBossExpedition(RaidBoss raidBoss)
	{
		_expeditionRaidBoss = raidBoss;
		_macroState = CitizenMacroState.RAID_BOSS_EXPEDITION;
		_lastAction = "Joined Raid Expedition -> " + ((raidBoss != null) ? raidBoss.getName() : "Raid Boss");
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
		
		// 3. Party Co-op override whenever grouped with a non-raid leader
		if ((_macroState != CitizenMacroState.RAID_BOSS_EXPEDITION) && _player.isInParty() && (_player.getParty().getLeader() != _player))
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
			case CLASS_TRANSFER_QUEST -> tickClassTransferQuest();
			case CLASS_TRANSFER -> tickClassTransfer();
			case TRAVEL_TO_HUNTING_ZONE -> tickTravelToHuntingZone();
			case HUNT_AND_FARM, CATACOMB_SEVEN_SIGNS -> tickHuntAndFarm();
			case DUNGEON_KAMALOKA_PAILAKA -> tickDungeonKamalokaPailaka();
			case RAID_BOSS_EXPEDITION -> tickRaidBossExpedition();
			case SUBCLASS_AND_CERTIFICATION -> tickSubclassAndCertification();
			case NOBLESSE_AND_OLYMPIAD -> tickNoblesseAndOlympiad();
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
		final PlayerClass pc = _player.getPlayerClass();
		final int classLvl = (pc != null) ? pc.level() : 0;
		final int lvl = _player.getLevel();
		
		// 1. Check Class Transfer eligibility (Lv 20, Lv 40, Lv 76) -> requires Quest Mark in PcInventory first!
		if (((lvl >= 20) && (classLvl == 0)) || ((lvl >= 40) && (classLvl == 1)) || ((lvl >= 76) && (classLvl == 2)))
		{
			if ((pc != null) && !pc.getNextClasses().isEmpty())
			{
				final int targetClassTier = classLvl + 1;
				final int requiredMarkId = ProgressionKnowledgeGraph.getClassTransferQuestMarkId(pc, targetClassTier);
				if (_player.getInventory().getItemByItemId(requiredMarkId) != null)
				{
					_macroState = CitizenMacroState.CLASS_TRANSFER;
				}
				else
				{
					_questKillsCurrent = 0;
					_questKillsTarget = targetClassTier * 10; // 10 kills (1st), 20 kills (2nd), 30 kills (3rd)
					_currentZone = ProgressionKnowledgeGraph.selectClassTransferQuestZone(targetClassTier);
					_player.teleToLocation(_currentZone.huntingCenter(), true);
					_macroState = CitizenMacroState.CLASS_TRANSFER_QUEST;
					_lastAction = "Started Class Transfer Trial (0/" + _questKillsTarget + ") at " + _currentZone.name();
				}
				return;
			}
		}
		
		// 2. Check Subclass & Certification Skills (Lv 76+ after 3rd Class Transfer)
		if ((lvl >= 76) && (classLvl >= 3) && !_subclassCertified)
		{
			_macroState = CitizenMacroState.SUBCLASS_AND_CERTIFICATION;
			return;
		}
		
		// 3. Check Noblesse & Grand Olympiad (Lv 78+ after Subclass Certification)
		if ((lvl >= 78) && _subclassCertified && !_player.isNoble())
		{
			_macroState = CitizenMacroState.NOBLESSE_AND_OLYMPIAD;
			return;
		}
		
		// 4. Check Pailaka / Kamaloka Vitality Dungeon milestones (Lv 36 Q128, Lv 61 Q129, Lv 73 Q144)
		if (((lvl >= 36) && (_completedPailakaTier < 1))
			|| ((lvl >= 61) && (_completedPailakaTier < 2))
			|| ((lvl >= 73) && (_completedPailakaTier < 3)))
		{
			_macroState = CitizenMacroState.DUNGEON_KAMALOKA_PAILAKA;
			return;
		}
		
		// 5. Exchange Seven Signs Seal Stones (6360, 6361, 6362) for Ancient Adena (5575) with Priest of Dawn
		exchangeSevenSignsSealStones();
		
		// 6. Check Guild Master skill learning (only if at least one skill is affordable or citizen leveled up since last check)
		final Collection<SkillLearn> learnable = SkillTreeData.getInstance().getAvailableSkills(_player, _player.getPlayerClass(), false, false);
		if ((learnable != null) && !learnable.isEmpty())
		{
			boolean hasAffordable = false;
			final long currentSp = _player.getSp();
			for (SkillLearn sl : learnable)
			{
				if (currentSp >= sl.getLevelUpSp())
				{
					hasAffordable = true;
					break;
				}
			}
			if (hasAffordable || (lvl > _lastSkillCheckLevel))
			{
				_macroState = CitizenMacroState.GUILD_SKILL_LEARN;
				return;
			}
		}
		
		// 7. Upgrade gear (NG -> D -> C -> B Shop Cap -> A-Craft -> S-SA) & restock consumables
		ensureFairPlayGearAndConsumables(false);
		
		// 8. Refresh buffs in town
		applyTownSupportBuffs();
		
		// 9. Select level-appropriate hunting zone (including Seven Signs Catacombs/Necropolises) and travel
		_currentZone = ProgressionKnowledgeGraph.selectHuntingZone(_player.getLevel(), _player.getRace());
		_macroState = CitizenMacroState.TRAVEL_TO_HUNTING_ZONE;
		if (!_lastAction.startsWith("Disappointed:"))
		{
			_lastAction = "Town routine complete -> Heading to " + _currentZone.name();
		}
	}
	
	private void tickGuildSkillLearn()
	{
		_lastSkillCheckLevel = _player.getLevel();
		final Collection<SkillLearn> learnable = SkillTreeData.getInstance().getAvailableSkills(_player, _player.getPlayerClass(), false, false);
		if ((learnable == null) || learnable.isEmpty())
		{
			_macroState = CitizenMacroState.TOWN_ROUTINE;
			return;
		}
		
		final List<SkillLearn> sortedSkills = new ArrayList<>(learnable);
		sortedSkills.sort(Comparator.comparingInt(SkillLearn::getLevelUpSp));
		
		int learnedCount = 0;
		String missingSkillName = null;
		long missingSp = 0L;
		for (SkillLearn sl : sortedSkills)
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
				_player.addSkill(skill, true);
				learnedCount++;
			}
			else if (missingSkillName == null)
			{
				missingSkillName = skill.getName();
				missingSp = spCost - _player.getSp();
			}
		}
		
		if (learnedCount > 0)
		{
			_player.sendSkillList();
		}
		
		if (missingSkillName != null)
		{
			_lastAction = "Disappointed: Short " + missingSp + " SP for " + missingSkillName + " -> Farming SP";
		}
		else if (learnedCount > 0)
		{
			_lastAction = "Learned " + learnedCount + " Guild Master skill(s)";
		}
		_macroState = CitizenMacroState.TOWN_ROUTINE;
	}
	
	private void tickClassTransferQuest()
	{
		final PlayerClass pc = _player.getPlayerClass();
		final int classLvl = (pc != null) ? pc.level() : 0;
		final int targetClassTier = classLvl + 1;
		final int requiredMarkId = ProgressionKnowledgeGraph.getClassTransferQuestMarkId(pc, targetClassTier);
		
		if (_questKillsCurrent >= _questKillsTarget)
		{
			_player.getInventory().addItem(ItemProcessType.QUEST, requiredMarkId, 1, _player, null);
			if (targetClassTier >= 3)
			{
				_player.getInventory().addItem(ItemProcessType.QUEST, 6622, 1, _player, null); // Giant's Codex
			}
			_macroState = CitizenMacroState.RETURN_TO_TOWN;
			_lastAction = "Completed Class Transfer Trial (" + _questKillsCurrent + "/" + _questKillsTarget + ") -> Returning to Guild Master";
			return;
		}
		
		if (_player.isCastingNow())
		{
			return;
		}
		
		final WorldObject currentObj = _player.getTarget();
		if ((currentObj != null) && currentObj.isCreature())
		{
			final Creature currentTarget = currentObj.asCreature();
			if (currentTarget.isAlikeDead())
			{
				_questKillsCurrent++;
				onTargetDefeated(currentTarget);
				_player.setTarget(null);
				_lastAction = "Class Transfer Trial kill (" + _questKillsCurrent + "/" + _questKillsTarget + ")";
				return;
			}
		}
		
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
			target = World.getNearestVisibleObjectInRange(_player, Creature.class, 2200, this::isValidHuntTarget);
			if (target != null)
			{
				_player.setTarget(target);
			}
		}
		if (target == null)
		{
			// If zone has no nearby monsters, count trial exploration step so citizen never softlocks
			_questKillsCurrent++;
			_lastAction = "Searching Class Transfer Trial zone (" + _questKillsCurrent + "/" + _questKillsTarget + ")";
			return;
		}
		
		final boolean isMage = (pc != null) && pc.isMage();
		final double dist = _player.calculateDistance2D(target);
		if (dist > (isMage ? 650.0 : 110.0))
		{
			if (!_player.isMoving())
			{
				_player.getAI().setIntentionMoveTo(target);
			}
			_lastAction = "Pursuing trial monster " + target.getName() + " (" + _questKillsCurrent + "/" + _questKillsTarget + ")";
			return;
		}
		
		engageTarget(target, isMage);
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
		
		// Consume the earned Class Transfer Mark
		final int targetTier = ((currentClass != null) ? currentClass.level() : 0) + 1;
		final int markId = ProgressionKnowledgeGraph.getClassTransferQuestMarkId(currentClass, targetTier);
		consumeItemIfPresent(markId);
		
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
	
	private void tickDungeonKamalokaPailaka()
	{
		final int lvl = _player.getLevel();
		if ((lvl >= 73) && (_completedPailakaTier < 3))
		{
			_completedPailakaTier = 3;
			_player.setVitalityPoints(PlayerStat.MAX_VITALITY_POINTS, true);
			equipItemIfValid(13296); // Pailaka Shirt (A-Grade)
			_player.addExpAndSp(10_800_000L, 950_000);
			_lastAction = "Cleared Pailaka (Injured Dragon Lv73) -> Full Vitality & Pailaka Shirt";
		}
		else if ((lvl >= 61) && (_completedPailakaTier < 2))
		{
			_completedPailakaTier = 2;
			_player.setVitalityPoints(PlayerStat.MAX_VITALITY_POINTS, true);
			equipItemIfValid(13033); // Pailaka Bracelet / Earring
			_player.addExpAndSp(4_200_000L, 420_000);
			_lastAction = "Cleared Pailaka (Devil's Legacy Lv61) -> Full Vitality & Pailaka Earring";
		}
		else if ((lvl >= 36) && (_completedPailakaTier < 1))
		{
			_completedPailakaTier = 1;
			_player.setVitalityPoints(PlayerStat.MAX_VITALITY_POINTS, true);
			equipItemIfValid(13032); // Pailaka Ring
			_player.addExpAndSp(810_000L, 75_000);
			_lastAction = "Cleared Pailaka (Song of Ice & Fire Lv36) -> Full Vitality & Pailaka Ring";
		}
		_macroState = CitizenMacroState.TOWN_ROUTINE;
	}
	
	private void tickRaidBossExpedition()
	{
		final RaidBoss boss = _expeditionRaidBoss;
		if ((boss == null) || boss.isAlikeDead() || !boss.isSpawned())
		{
			_expeditionRaidBoss = null;
			_spoilAndCraftPoints += 5;
			_macroState = CitizenMacroState.RETURN_TO_TOWN;
			_lastAction = "Raid Boss Expedition victorious -> Returning to town";
			return;
		}
		
		if (_player.calculateDistance3D(boss) > 1800)
		{
			final int ox = ThreadLocalRandom.current().nextInt(-140, 141);
			final int oy = ThreadLocalRandom.current().nextInt(-140, 141);
			_player.teleToLocation(boss.getX() + ox, boss.getY() + oy, boss.getZ(), boss.getHeading(), boss.getInstanceId());
			_lastAction = "Deployed to Raid Boss " + boss.getName() + " (Lv " + boss.getLevel() + ")";
			return;
		}
		
		_player.setTarget(boss);
		final boolean isMage = (_player.getPlayerClass() != null) && _player.getPlayerClass().isMage();
		final double dist = _player.calculateDistance2D(boss);
		if (dist > (isMage ? 650.0 : 120.0))
		{
			if (!_player.isMoving())
			{
				_player.getAI().setIntentionMoveTo(boss);
			}
			return;
		}
		
		engageTarget(boss, isMage);
		_lastAction = "Raiding " + boss.getName() + " (HP " + (int) Math.round((boss.getCurrentHp() * 100.0) / Math.max(1.0, boss.getMaxHp())) + "%)";
	}
	
	private void tickSubclassAndCertification()
	{
		_subclassCertified = true;
		// Award Emergent, Master & Class-Specific Certificates into PcInventory
		_player.getInventory().addItem(ItemProcessType.QUEST, 10280, 1, _player, null); // Emergent Ability
		_player.getInventory().addItem(ItemProcessType.QUEST, 10281, 1, _player, null); // Master Ability
		
		final boolean isMage = (_player.getPlayerClass() != null) && _player.getPlayerClass().isMage();
		final int[] certSkillIds = isMage
			? new int[] { 6577, 6576, 6566, 6563 } // Counter Barrier, Counter Spirit, Counter Defense, Boost CP
			: new int[] { 6565, 6575, 6564, 6563 }; // Counter Haste, Counter Critical, Resist Trait, Boost CP
		
		int learned = 0;
		for (int skillId : certSkillIds)
		{
			final Skill certSkill = SkillData.getInstance().getSkill(skillId, 1);
			if (certSkill != null)
			{
				_player.addSkill(certSkill, true);
				learned++;
			}
		}
		_player.sendSkillList();
		_lastAction = "Completed Subclass Lv75 Certification (" + learned + " SubCert skills learned)";
		_macroState = CitizenMacroState.TOWN_ROUTINE;
	}
	
	private void tickNoblesseAndOlympiad()
	{
		_player.setNoble(true);
		equipItemIfValid(7694); // Noblesse Tiara
		final Skill blessingOfNoblesse = SkillData.getInstance().getSkill(1323, 1);
		if (blessingOfNoblesse != null)
		{
			_player.addSkill(blessingOfNoblesse, true);
			blessingOfNoblesse.applyEffects(_player, _player);
		}
		_player.broadcastUserInfo();
		final boolean olyActive = Olympiad.getInstance().inCompPeriod();
		_lastAction = "Ascended to Noblesse (Tiara + Blessing 1323)" + (olyActive ? " & Olympiad Ready" : "");
		_macroState = CitizenMacroState.TOWN_ROUTINE;
	}
	
	private void exchangeSevenSignsSealStones()
	{
		long totalAncientAdena = 0L;
		final int[] sealStoneIds = { 6360, 6361, 6362 }; // Blue, Green, Red Seal Stones
		final long[] rates = { 3L, 5L, 10L };
		for (int i = 0; i < sealStoneIds.length; i++)
		{
			final Item stones = _player.getInventory().getItemByItemId(sealStoneIds[i]);
			if ((stones != null) && (stones.getCount() > 0))
			{
				final long count = stones.getCount();
				_player.destroyItem(ItemProcessType.NONE, stones.getObjectId(), count, _player, false);
				totalAncientAdena += count * rates[i];
			}
		}
		if (totalAncientAdena > 0L)
		{
			_player.getInventory().addItem(ItemProcessType.REWARD, 5575, totalAncientAdena, _player, null);
			_spoilAndCraftPoints += 2;
			_lastAction = "Exchanged Seal Stones for " + totalAncientAdena + " Ancient Adena (Mammon)";
		}
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
		if (_currentZone.isSevenSignsCatacomb())
		{
			_sevenSignsRegistered = true;
			_macroState = CitizenMacroState.CATACOMB_SEVEN_SIGNS;
			_lastAction = "Entered Seven Signs " + _currentZone.name() + " (Farming Seal Stones)";
		}
		else
		{
			_macroState = CitizenMacroState.HUNT_AND_FARM;
			_lastAction = "Arrived at " + _currentZone.name() + " (Lv " + _currentZone.minLevel() + "-" + _currentZone.maxLevel() + ")";
		}
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
	
	private boolean canAffordCraftedEndgame()
	{
		final Item aa = _player.getInventory().getItemByItemId(5575);
		final long ancientAdena = (aa != null) ? aa.getCount() : 0L;
		return (_spoilAndCraftPoints >= 3) || (ancientAdena >= 50_000L) || (_player.getAdena() >= 8_000_000L);
	}
	
	private void tickHuntAndFarm()
	{
		final int lvl = _player.getLevel();
		final PlayerClass pc = _player.getPlayerClass();
		final int classLvl = (pc != null) ? pc.level() : 0;
		
		// 1. Return to town if ready for class transfer, Pailaka, Subclass Certification, or gear tier upgrade
		if ((((lvl >= 20) && (classLvl == 0)) || ((lvl >= 40) && (classLvl == 1)) || ((lvl >= 76) && (classLvl == 2)))
			|| ((lvl >= 76) && (classLvl >= 3) && !_subclassCertified)
			|| ((lvl >= 78) && _subclassCertified && !_player.isNoble())
			|| ((_currentZone != null) && (lvl > (_currentZone.maxLevel() + 2)) && (lvl < 85))
			|| (ProgressionKnowledgeGraph.getTargetGradeTierForLevel(lvl, canAffordCraftedEndgame()) > _equippedGradeTier))
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
		// Dwarven Spoil (254) before finishing monster
		if (target.isMonster() && (_player.getRace() == Race.DWARF))
		{
			final Monster monster = target.asMonster();
			if ((monster != null) && !monster.isSpoiled())
			{
				final Skill spoilSkill = _player.getKnownSkill(254);
				if (spoilSkill != null)
				{
					monster.setSpoilerObjectId(_player.getObjectId());
					_player.useMagic(spoilSkill, true, false);
					_lastAction = "Casting Spoil (254) on " + monster.getName();
					return;
				}
			}
		}
		
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
		// 1. Dwarven Sweeper (42) & Crafting Material Harvest
		if (defeated.isMonster())
		{
			final Monster monster = defeated.asMonster();
			if ((monster != null) && (monster.isSpoiled() || (_player.getRace() == Race.DWARF)))
			{
				_player.broadcastPacket(new MagicSkillUse(_player, monster, 42, 1, 0, 0)); // Sweeper (42)
				final int matId = SPOILED_MATERIAL_IDS[ThreadLocalRandom.current().nextInt(SPOILED_MATERIAL_IDS.length)];
				_player.getInventory().addItem(ItemProcessType.SWEEP, matId, ThreadLocalRandom.current().nextInt(2, 7), _player, monster);
				_spoilAndCraftPoints++;
				monster.setSpoilerObjectId(0);
				_lastAction = "Swept spoiled materials from " + monster.getName() + " (Craft Pts: " + _spoilAndCraftPoints + ")";
			}
			
			// 2. Seven Signs Seal Stone collection in Catacombs & Necropolises
			if ((_currentZone != null) && _currentZone.isSevenSignsCatacomb())
			{
				final int sealStoneId = 6360 + ThreadLocalRandom.current().nextInt(3); // 6360 Blue, 6361 Green, 6362 Red
				final long stoneCount = 150L * Math.max(1, _player.getLevel() / 15);
				_player.getInventory().addItem(ItemProcessType.LOOT, sealStoneId, stoneCount, _player, monster);
			}
		}
		
		// 3. Verify XP/Adena progression & starvation QA check
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
		final int targetTier = ProgressionKnowledgeGraph.getTargetGradeTierForLevel(_player.getLevel(), canAffordCraftedEndgame());
		final GearLoadout loadout = ProgressionKnowledgeGraph.getFairPlayLoadout(_player.getPlayerClass(), targetTier);
		
		if (targetTier > _equippedGradeTier)
		{
			if (!isInitialSpawn && (_player.getAdena() < loadout.adenaCost()) && (targetTier <= 3))
			{
				// Fair-play check: report if citizen reached shop grade milestone without enough Adena
				GameplayAnomalyAuditor.getInstance().report(
					_player,
					AnomalyType.MULTISELL_MISSING_INGREDIENT,
					getCurrentZoneName(),
					"Citizen reached Lv " + _player.getLevel() + " (" + loadout.gradeLabel() + ") with " + _player.getAdena() + " Adena < required " + loadout.adenaCost());
			}
			else if (!isInitialSpawn && (loadout.adenaCost() > 0) && (_player.getAdena() >= loadout.adenaCost()))
			{
				_player.reduceAdena(ItemProcessType.BUY, loadout.adenaCost(), _player, false);
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
			if (targetTier >= 4)
			{
				_lastAction = "Crafted & Unsealed " + loadout.gradeLabel() + " equipment set";
			}
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
		if (_player.isNoble())
		{
			final Skill blessing = SkillData.getInstance().getSkill(1323, 1);
			if (blessing != null)
			{
				blessing.applyEffects(_player, _player);
			}
		}
		_player.setCurrentHp(_player.getMaxHp());
		_player.setCurrentMp(_player.getMaxMp());
		_player.setCurrentCp(_player.getMaxCp());
	}
}
