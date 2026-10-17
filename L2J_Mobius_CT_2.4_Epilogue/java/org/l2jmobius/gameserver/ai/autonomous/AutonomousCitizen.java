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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.l2jmobius.gameserver.ai.autonomous.EconomyProgressionAuditor.EconomySink;
import org.l2jmobius.gameserver.ai.autonomous.EconomyProgressionAuditor.EconomySource;
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
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.actor.instance.RaidBoss;
import org.l2jmobius.gameserver.entity.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;

/**
 * Real-Player Autonomous Citizen ({@code Lv 1 -> 85}) with a Hierarchical Macro-Lifecycle State Machine,
 * Endgame Class Aspiration, Authentic Dwarven Spoil/Sweep/Craft Economy, Multi-Mark / 6-Tablet Class Transfer
 * Trials, Multi-Stage Subclass &amp; 4-Part Noblesse Campaigns, Deep 4-Archetype Combat Doctrines + UCB1
 * Contextual Toolkit Exploration Memory, and Self-Reporting QA &amp; Economy Auditing.
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
	
	private final Player _player;
	private final PlayerClass _targetEndgameClass;
	private volatile CitizenMacroState _macroState = CitizenMacroState.SPAWN_AND_ORIENT;
	private volatile HuntingZone _currentZone;
	private volatile String _lastAction = "Spawned in Starting Village";
	private volatile String _lastDecisionReason = "Initializing tactical doctrine";
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
	private int _classTransferSubStage = 0; // 1..3 for 1st/2nd Class (3 Marks), 1..6 for 3rd Class (6 Tablets of Vision)
	private int _questKillsCurrent = 0;
	private int _questKillsTarget = 10;
	private int _spoilAndCraftPoints = 0;
	private boolean _sevenSignsRegistered = false;
	private int _completedPailakaTier = 0; // 1 = Lv36 Q128, 2 = Lv61 Q129, 3 = Lv73 Q144
	private int _subclassQuestStage = 0; // 1=Cabrio, 2=ToI Scepters, 3=Red Pipette+Star, 4=Mimir's Elixir, 5=Leveling Sub 40->75, 6=Certified
	private int _subClassLevel = 0;
	private boolean _subclassCertified = false;
	private int _noblesseQuestStage = 0; // 0=None, 1=Q241 Done(7677), 2=Q242 Done(7678), 3=Q246 Done(7679), 4=Q247 Noblesse Done
	private boolean _noblesseStageInProgress = false;
	private volatile RaidBoss _expeditionRaidBoss = null;
	private final Set<Integer> _blacklistedTargets = ConcurrentHashMap.newKeySet();
	
	// UCB1 Contextual Toolkit Exploration & Strategy Optimization Memory (16 situation buckets x learned skillIds)
	private final Map<Long, Double> _contextualSkillQ = new ConcurrentHashMap<>();
	private final Map<Long, Integer> _contextualSkillTrials = new ConcurrentHashMap<>();
	private final int[] _bucketTotalTrials = new int[16];
	private int _lastCastSkillId = 0;
	private int _lastCastBucket = 0;
	private int _lastCastTargetObjId = 0;
	private double _lastCastTargetHpBefore = 0.0;
	private long _lastCastTimestampMs = 0L;
	
	public AutonomousCitizen(Player player)
	{
		this(player, ProgressionKnowledgeGraph.selectRandomEndgameAspiration());
	}
	
	public AutonomousCitizen(Player player, PlayerClass targetEndgameClass)
	{
		_player = player;
		_targetEndgameClass = (targetEndgameClass != null) ? targetEndgameClass : ProgressionKnowledgeGraph.selectRandomEndgameAspiration();
		final VillageHub hub = ProgressionKnowledgeGraph.getStartingVillage(player.getRace());
		_currentZone = ProgressionKnowledgeGraph.selectHuntingZone(player.getLevel(), player.getRace());
		_lastAction = "Oriented at " + hub.villageName() + " (-> " + _targetEndgameClass.name() + ")";
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
	
	public PlayerClass getTargetEndgameClass()
	{
		return _targetEndgameClass;
	}
	
	public String getLastDecisionReason()
	{
		return _lastDecisionReason;
	}
	
	public boolean isSpoilerCitizen()
	{
		final PlayerClass pc = _player.getPlayerClass();
		return (_targetEndgameClass == PlayerClass.FORTUNE_SEEKER)
			|| (pc == PlayerClass.SCAVENGER)
			|| (pc == PlayerClass.BOUNTY_HUNTER)
			|| (pc == PlayerClass.FORTUNE_SEEKER);
	}
	
	public boolean isCrafterCitizen()
	{
		final PlayerClass pc = _player.getPlayerClass();
		return (_targetEndgameClass == PlayerClass.MAESTRO)
			|| (pc == PlayerClass.ARTISAN)
			|| (pc == PlayerClass.WARSMITH)
			|| (pc == PlayerClass.MAESTRO);
	}
	
	public boolean isArcherCitizen()
	{
		final PlayerClass pc = _player.getPlayerClass();
		final String name = (pc != null) ? pc.name() : "";
		final String asp = (_targetEndgameClass != null) ? _targetEndgameClass.name() : "";
		return name.contains("RANGER") || name.contains("HAWKEYE") || name.contains("SAGITTARIUS") || name.contains("SENTINEL") || name.contains("ARBALESTER") || name.contains("TRICKSTER")
			|| ((pc != null) && (pc.level() >= 1) && (asp.contains("SAGITTARIUS") || asp.contains("SENTINEL") || asp.contains("TRICKSTER")));
	}
	
	public boolean isMomentumFighterCitizen()
	{
		final PlayerClass pc = _player.getPlayerClass();
		return (_targetEndgameClass == PlayerClass.DUELIST)
			|| (_targetEndgameClass == PlayerClass.GRAND_KHAVATARI)
			|| (pc == PlayerClass.GLADIATOR)
			|| (pc == PlayerClass.DUELIST)
			|| (pc == PlayerClass.ORC_MONK)
			|| (pc == PlayerClass.TYRANT)
			|| (pc == PlayerClass.GRAND_KHAVATARI);
	}
	
	public boolean isPolearmTrainCitizen()
	{
		final PlayerClass pc = _player.getPlayerClass();
		return (_targetEndgameClass == PlayerClass.DREADNOUGHT)
			|| (pc == PlayerClass.WARLORD)
			|| (pc == PlayerClass.DREADNOUGHT);
	}
	
	public void addSpoilAndCraftPoints(int points)
	{
		_spoilAndCraftPoints += points;
	}
	
	public int getSpoilAndCraftPoints()
	{
		return _spoilAndCraftPoints;
	}
	
	public void setLastAction(String action)
	{
		_lastAction = action;
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
		final StringBuilder sb = new StringBuilder(48);
		final PlayerClass pc = _player.getPlayerClass();
		final int classLvl = (pc != null) ? pc.level() : 0;
		sb.append(switch (classLvl)
		{
			case 1 -> "[1st]";
			case 2 -> "[2nd]";
			case 3 -> "[3rd]";
			default -> "[Base]";
		});
		if (isSpoilerCitizen())
		{
			sb.append("[Spoil:").append(_spoilAndCraftPoints).append("]");
		}
		else if (isCrafterCitizen())
		{
			sb.append("[Craft:").append(_spoilAndCraftPoints).append("]");
		}
		if (_sevenSignsRegistered)
		{
			sb.append("[7S]");
		}
		if ((_subClassLevel > 0) && !_subclassCertified)
		{
			sb.append("[SubLv").append(_subClassLevel).append("]");
		}
		if (_subclassCertified)
		{
			sb.append("[SubCert75]");
		}
		if ((_noblesseQuestStage > 0) && !_player.isNoble())
		{
			sb.append("[NobQ").append(_noblesseQuestStage).append("]");
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
		
		// 3. Party Co-op override whenever grouped with a non-raid leader outside quest campaigns
		if ((_macroState != CitizenMacroState.RAID_BOSS_EXPEDITION)
			&& (_macroState != CitizenMacroState.SUBCLASS_AND_CERTIFICATION)
			&& (_macroState != CitizenMacroState.NOBLESSE_AND_OLYMPIAD)
			&& _player.isInParty()
			&& (_player.getParty().getLeader() != _player))
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
	
	private boolean hasAllItemsInInventory(int[] requiredItemIds)
	{
		for (int itemId : requiredItemIds)
		{
			final Item item = _player.getInventory().getItemByItemId(itemId);
			if ((item == null) || (item.getCount() <= 0))
			{
				return false;
			}
		}
		return true;
	}
	
	private void tickSpawnAndOrient()
	{
		ensureFairPlayGearAndConsumables(true);
		applyTownSupportBuffs();
		_macroState = CitizenMacroState.TOWN_ROUTINE;
		_lastAction = "Oriented & equipped starter kit (-> " + _targetEndgameClass.name() + ")";
	}
	
	private boolean isEligibleForNextNoblesseQuestPart()
	{
		if (_player.isNoble())
		{
			return false;
		}
		return ((_subClassLevel >= 50) && (_noblesseQuestStage == 0))
			|| ((_subClassLevel >= 60) && (_noblesseQuestStage == 1))
			|| ((_subClassLevel >= 65) && (_noblesseQuestStage == 2))
			|| ((_subClassLevel >= 75) && _subclassCertified && (_noblesseQuestStage == 3));
	}
	
	private void tickTownRoutine()
	{
		final PlayerClass pc = _player.getPlayerClass();
		final int classLvl = (pc != null) ? pc.level() : 0;
		final int lvl = _player.getLevel();
		
		// 1. Check Class Transfer eligibility (Lv 20 3-Stage Path, Lv 40 3-Mark Trials, Lv 76 6-Tablet Saga)
		if (((lvl >= 20) && (classLvl == 0)) || ((lvl >= 40) && (classLvl == 1)) || ((lvl >= 76) && (classLvl == 2)))
		{
			if ((pc != null) && !pc.getNextClasses().isEmpty())
			{
				final int targetClassTier = classLvl + 1;
				final int[] requiredStageItems = ProgressionKnowledgeGraph.getClassTransferStageItemIds(pc, _targetEndgameClass, targetClassTier);
				if (hasAllItemsInInventory(requiredStageItems))
				{
					_macroState = CitizenMacroState.CLASS_TRANSFER;
				}
				else
				{
					// Determine which sub-stage (1..3 for 1st/2nd Class, 1..6 for 3rd Class Saga) the citizen needs next
					int nextSubStage = 1;
					for (int i = 0; i < requiredStageItems.length; i++)
					{
						if (_player.getInventory().getItemByItemId(requiredStageItems[i]) == null)
						{
							nextSubStage = i + 1;
							break;
						}
					}
					_classTransferSubStage = nextSubStage;
					_questKillsCurrent = 0;
					_questKillsTarget = (targetClassTier == 3) ? 6 : 8;
					_currentZone = ProgressionKnowledgeGraph.selectClassTransferQuestZone(targetClassTier, _classTransferSubStage);
					_player.teleToLocation(_currentZone.huntingCenter(), true);
					_macroState = CitizenMacroState.CLASS_TRANSFER_QUEST;
					_lastAction = "Class Transfer Tier " + targetClassTier + " Stage [" + _classTransferSubStage + "/" + requiredStageItems.length + "] at " + _currentZone.name();
				}
				return;
			}
		}
		
		// 2. Check Subclass Campaign (Q234 Fate's Whisper + Q235 Mimir's Elixir) & Subclass Lv 75 Master Certification
		if ((lvl >= 76) && (classLvl >= 3) && !_subclassCertified)
		{
			if ((_subclassQuestStage < 5) || (_subClassLevel >= 75))
			{
				_macroState = CitizenMacroState.SUBCLASS_AND_CERTIFICATION;
				return;
			}
		}
		
		// 3. Check 4-Part Noblesse Campaign (Possessor of a Precious Soul 1 at SubLv50, 2 at SubLv60, 3 at SubLv65 + Barakiel, 4 at SubLv75)
		if (isEligibleForNextNoblesseQuestPart())
		{
			_noblesseStageInProgress = false;
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
		
		// 7. Upgrade gear (NG -> D -> C -> B Shop Cap -> Dwarven A-Craft -> Dwarven S-SA) & restock consumables
		ensureFairPlayGearAndConsumables(false);
		
		// 8. Refresh buffs in town
		applyTownSupportBuffs();
		
		// 9. Select level-appropriate hunting zone (including Seven Signs Catacombs/Necropolises) and travel
		_currentZone = ProgressionKnowledgeGraph.selectHuntingZone(_player.getLevel(), _player.getRace());
		_macroState = CitizenMacroState.TRAVEL_TO_HUNTING_ZONE;
		if (!_lastAction.startsWith("Disappointed:") && !_lastAction.startsWith("Commissioned"))
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
	
	/**
	 * Multi-Stage Profession Quest Chains:
	 * <ul>
	 *   <li><b>1st Class (Lv 20, 3 Stages):</b> Collects 2 intermediate Path quest items + final 1st-Class Occupation Mark across 3 zones.</li>
	 *   <li><b>2nd Class (Lv 40, 3-Mark Trial Chain):</b> Completes Trial (Mark #1), Testimony (Mark #2), and Test (Mark #3) across Death Pass, Cruma Marshlands, and Breka's Stronghold.</li>
	 *   <li><b>3rd Class (Lv 76, 6-Tablet Saga Chain):</b> Collects all 6 Resonance Amulets (7284..7289) &amp; communes with all 6 Tablets of Vision across Hot Springs, Valley of Saints, Wall of Argos, Ketra Outpost, Shrine of the Loyal (Archon of Halisha), and Forbidden Gateway.</li>
	 * </ul>
	 */
	private void tickClassTransferQuest()
	{
		final PlayerClass pc = _player.getPlayerClass();
		final int classLvl = (pc != null) ? pc.level() : 0;
		final int targetClassTier = classLvl + 1;
		final int[] stageItems = ProgressionKnowledgeGraph.getClassTransferStageItemIds(pc, _targetEndgameClass, targetClassTier);
		if (_classTransferSubStage < 1)
		{
			_classTransferSubStage = 1;
		}
		
		if (_questKillsCurrent >= _questKillsTarget)
		{
			final int earnedItemId = stageItems[Math.min(_classTransferSubStage - 1, stageItems.length - 1)];
			_player.getInventory().addItem(ItemProcessType.QUEST, earnedItemId, 1, _player, null);
			
			if (_classTransferSubStage < stageItems.length)
			{
				_classTransferSubStage++;
				_questKillsCurrent = 0;
				_questKillsTarget = (targetClassTier == 3) ? 6 : 8;
				_currentZone = ProgressionKnowledgeGraph.selectClassTransferQuestZone(targetClassTier, _classTransferSubStage);
				_player.teleToLocation(_currentZone.huntingCenter(), true);
				_lastAction = "Earned Quest Item (" + earnedItemId + ") -> Stage [" + _classTransferSubStage + "/" + stageItems.length + "] at " + _currentZone.name();
				return;
			}
			
			// Completed all stages (3/3 Marks or 6/6 Tablets of Vision)!
			if (targetClassTier >= 3)
			{
				_player.getInventory().addItem(ItemProcessType.QUEST, 6622, 1, _player, null); // Giant's Codex
			}
			final long questRewardAdena = targetClassTier * 85_000L;
			_player.addAdena(ItemProcessType.QUEST, questRewardAdena, _player, false);
			EconomyProgressionAuditor.getInstance().recordInflow(_player, EconomySource.QUEST_REWARD_ADENA, questRewardAdena, getCurrentZoneName());
			_lastAdenaSnapshot = _player.getAdena();
			_macroState = CitizenMacroState.RETURN_TO_TOWN;
			_lastAction = "Completed all " + stageItems.length + " Class Transfer stages -> Returning to Grand Master";
			return;
		}
		
		huntQuestMonstersInCurrentZone("Class Trial [" + _classTransferSubStage + "/" + stageItems.length + "]");
	}
	
	private void tickClassTransfer()
	{
		final PlayerClass currentClass = _player.getPlayerClass();
		final PlayerClass nextClass = ProgressionKnowledgeGraph.selectNextClassTowards(currentClass, _targetEndgameClass);
		if (nextClass == null)
		{
			GameplayAnomalyAuditor.getInstance().report(
				_player,
				AnomalyType.SKILL_TREE_GAP,
				getCurrentZoneName(),
				"No child PlayerClass found for class transfer from " + currentClass + " towards " + _targetEndgameClass + " at Lv " + _player.getLevel());
			_macroState = CitizenMacroState.TOWN_ROUTINE;
			return;
		}
		
		// Consume all earned Class Transfer Marks / Resonance Amulets (3 Marks for 2nd Class, 6 Resonance Amulets for 3rd Class)
		final int targetTier = ((currentClass != null) ? currentClass.level() : 0) + 1;
		final int[] stageItems = ProgressionKnowledgeGraph.getClassTransferStageItemIds(currentClass, _targetEndgameClass, targetTier);
		consumeItemIfPresent(stageItems);
		_classTransferSubStage = 0;
		
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
		_lastAction = "Completed Class Transfer -> " + nextClass.name() + " (Goal: " + _targetEndgameClass.name() + ")";
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
			if (isSpoilerCitizen() || isCrafterCitizen())
			{
				_spoilAndCraftPoints += 5;
			}
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
		final double engageDist = isArcherCitizen() ? Math.max(750.0, _player.getPhysicalAttackRange() - 40.0) : (isMage ? 650.0 : 120.0);
		if (dist > engageDist)
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
	
	/**
	 * Authentic Multi-Stage Subclass Campaign:
	 * <ul>
	 *   <li>Stage 1: Q00234 Fate's Whisper — Contest Raid Boss Shilen's Messenger Cabrio (25035) at The Cemetery -> Reiria's Soul Orb (4666)</li>
	 *   <li>Stage 2: Q00234 Fate's Whisper — Contest ToI Raid Bosses Hallate (25220), Kernon (25054), Golkonda (25126) -> Infernium Scepters (4667, 4668, 4669)</li>
	 *   <li>Stage 3: Q00234 Fate's Whisper — Stain Pipette Knife (4665 -> 4673) + B-Grade Weapon &amp; 984 B-Crystals (1460) -> Star of Destiny (5011)</li>
	 *   <li>Stage 4: Q00235 Mimir's Elixir — Hunt in Silent Valley for Pure Silver (6320), True Gold (6321), Blood Fire (6318) -> Mimir's Elixir (6319) -> Unlock Subclass at Lv 40</li>
	 *   <li>Stage 5: Level Subclass from Lv 40 -> 75 through real monster kills in HUNT_AND_FARM (Emergent 10280 at Lv 65, Master 10281 + Cert Skills at Lv 75)</li>
	 * </ul>
	 */
	private void tickSubclassAndCertification()
	{
		if (_subclassQuestStage == 0)
		{
			_subclassQuestStage = 1;
			_questKillsCurrent = 0;
			_questKillsTarget = 8;
			AutonomousPlayerManager.getInstance().joinOrFormQuestRaidParty(this);
			_currentZone = ProgressionKnowledgeGraph.selectSubclassQuestZone(1);
			_player.teleToLocation(_currentZone.huntingCenter(), true);
			_lastAction = "Q234 Fate's Whisper S1: Contesting Cabrio (25035) at The Cemetery";
			return;
		}
		
		if (_subclassQuestStage == 1)
		{
			if (_questKillsCurrent >= _questKillsTarget)
			{
				_player.getInventory().addItem(ItemProcessType.QUEST, 4666, 1, _player, null); // Reiria's Soul Orb
				_subclassQuestStage = 2;
				_questKillsCurrent = 0;
				_questKillsTarget = 12;
				_currentZone = ProgressionKnowledgeGraph.selectSubclassQuestZone(2);
				_player.teleToLocation(_currentZone.huntingCenter(), true);
				_lastAction = "Q234 S2: Earned Reiria's Soul Orb (4666) -> Contesting ToI Raid Bosses";
				return;
			}
			huntQuestMonstersOrRaidBossInCurrentZone("Q234 S1 Cabrio", 25035);
			return;
		}
		
		if (_subclassQuestStage == 2)
		{
			if ((_questKillsCurrent == 4) && (_player.getInventory().getItemByItemId(4667) == null))
			{
				_player.getInventory().addItem(ItemProcessType.QUEST, 4667, 1, _player, null); // Hallate's Infernium Scepter
			}
			else if ((_questKillsCurrent == 8) && (_player.getInventory().getItemByItemId(4668) == null))
			{
				_player.getInventory().addItem(ItemProcessType.QUEST, 4668, 1, _player, null); // Kernon's Infernium Scepter
			}
			if (_questKillsCurrent >= _questKillsTarget)
			{
				_player.getInventory().addItem(ItemProcessType.QUEST, 4669, 1, _player, null); // Golkonda's Infernium Scepter
				_player.getInventory().addItem(ItemProcessType.QUEST, 4665, 1, _player, null); // Pipette Knife
				_subclassQuestStage = 3;
				_questKillsCurrent = 0;
				_questKillsTarget = 5;
				_currentZone = ProgressionKnowledgeGraph.selectSubclassQuestZone(3);
				_player.teleToLocation(_currentZone.huntingCenter(), true);
				_lastAction = "Q234 S3: Obtained 3 Infernium Scepters -> Staining Pipette Knife (4665)";
				return;
			}
			huntQuestMonstersOrRaidBossInCurrentZone("Q234 S2 ToI Raids", 25220, 25054, 25126);
			return;
		}
		
		if (_subclassQuestStage == 3)
		{
			if (_questKillsCurrent >= _questKillsTarget)
			{
				consumeItemIfPresent(4665); // Pipette Knife
				consumeItemIfPresent(4666, 4667, 4668, 4669);
				_player.getInventory().addItem(ItemProcessType.QUEST, 4673, 1, _player, null); // Red Pipette Knife
				
				// B-Grade weapon + 984 Crystal (B-Grade) (1460) sink for Maestro Reorin
				final long bWeaponAndCrystalsCost = Math.min(_player.getAdena() / 2, 3_500_000L);
				if (bWeaponAndCrystalsCost > 0)
				{
					_player.reduceAdena(ItemProcessType.QUEST, bWeaponAndCrystalsCost, _player, false);
					EconomyProgressionAuditor.getInstance().recordSink(_player, EconomySink.SINK_CLASS_AND_MAMMON, bWeaponAndCrystalsCost, "Q234 B-Grade Weapon + 984 B-Crystals");
					_lastAdenaSnapshot = _player.getAdena();
				}
				consumeItemIfPresent(4673);
				_player.getInventory().addItem(ItemProcessType.QUEST, 5011, 1, _player, null); // Star of Destiny
				
				_subclassQuestStage = 4;
				_questKillsCurrent = 0;
				_questKillsTarget = 12;
				_currentZone = ProgressionKnowledgeGraph.selectSubclassQuestZone(4);
				_player.teleToLocation(_currentZone.huntingCenter(), true);
				_lastAction = "Q235 Mimir's Elixir S4: Earned Star of Destiny (5011) -> Silent Valley";
				return;
			}
			huntQuestMonstersInCurrentZone("Q234 Stain Pipette Knife");
			return;
		}
		
		if (_subclassQuestStage == 4)
		{
			if ((_questKillsCurrent == 4) && (_player.getInventory().getItemByItemId(6320) == null))
			{
				_player.getInventory().addItem(ItemProcessType.QUEST, 6320, 1, _player, null); // Pure Silver
			}
			else if ((_questKillsCurrent == 8) && (_player.getInventory().getItemByItemId(6321) == null))
			{
				_player.getInventory().addItem(ItemProcessType.QUEST, 6321, 1, _player, null); // True Gold
			}
			if (_questKillsCurrent >= _questKillsTarget)
			{
				_player.getInventory().addItem(ItemProcessType.QUEST, 6318, 1, _player, null); // Blood Fire
				_player.getInventory().addItem(ItemProcessType.QUEST, 6319, 1, _player, null); // Mimir's Elixir
				consumeItemIfPresent(5011, 6320, 6321, 6318, 6319);
				_subClassLevel = 40;
				_subclassQuestStage = 5;
				_macroState = CitizenMacroState.RETURN_TO_TOWN;
				_lastAction = "Completed Mimir's Elixir (6319) -> Subclass unlocked at Lv 40 (Leveling 40->75)";
				return;
			}
			huntQuestMonstersInCurrentZone("Q235 Mimir's Elixir");
			return;
		}
		
		// Stage 5 complete: _subClassLevel >= 75 -> Award Master & Class-Specific Certification Skills
		if (_subClassLevel >= 75)
		{
			_subclassCertified = true;
			_subclassQuestStage = 6;
			_player.getInventory().addItem(ItemProcessType.QUEST, 10280, 1, _player, null); // Certificate - Emergent Ability
			_player.getInventory().addItem(ItemProcessType.QUEST, 10281, 1, _player, null); // Certificate - Master Ability
			
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
			_lastAction = "Subclass reached Lv 75 -> Certified " + learned + " Master/Class SubCert skills!";
			_macroState = CitizenMacroState.TOWN_ROUTINE;
		}
		else
		{
			_macroState = CitizenMacroState.TOWN_ROUTINE;
		}
	}
	
	/**
	 * Full 4-Part Noblesse Quest Chain Integrity ({@code Possessor of a Precious Soul 1 -> 2 -> 3 -> 4}):
	 * <ul>
	 *   <li>Part 1 ({@code Q00241_PossessorOfAPreciousSoul_1}, Subclass {@code Lv 50+}): Hunts Malruk Succubus (20244) for Malruk Succubus Claw (7597), Baraham (27113) for Legend of Seventeen (7587), Taik Orc Supply Officer (20669) for Echo Crystal - Forgotten Song (7589), and Crimson Drake (20670) for Crimson Moss (7598) -> Virgil's Letter (7677).</li>
	 *   <li>Part 2 ({@code Q00242_PossessorOfAPreciousSoul_2}, Subclass {@code Lv 60+}): Consumes Virgil's Letter (7677), hunts Restless Dead &amp; Matriarch (27317) in Swamp of Screams for 4 Orbs of Binding (7595), rescues Fallen Unicorn -> Caradine's Letter (7678).</li>
	 *   <li>Part 3 ({@code Q00246_PossessorOfAPreciousSoul_3}, Subclass {@code Lv 65+}): Consumes Caradine's Letter (7678), hunts Pilgrim of Splendor (21541) for Ring of Goddess: Waterbinder (7591) &amp; Judge of Splendor (21544) for Necklace of Goddess: Evergreen (7592), forms a Full Raid Party to contest Raid Boss Flame of Splendor Barakiel (25325) for Rain Song (7593), exchanges for Relic Box (7594) &amp; Caradine's Letter (7679).</li>
	 *   <li>Part 4 ({@code Q00247_PossessorOfAPreciousSoul_4}, Subclass {@code Lv 75+}): Consumes Caradine's Letter (7679), travels to Lady of the Lake (31745) at Coliseum/Goddard -> Noblesse Tiara (7694), {@code setNoble(true)}, &amp; Blessing of Noblesse (1323).</li>
	 * </ul>
	 */
	private void tickNoblesseAndOlympiad()
	{
		// Part 1: Q00241_PossessorOfAPreciousSoul_1 (Subclass Lv 50+)
		if (_noblesseQuestStage == 0)
		{
			if (!_noblesseStageInProgress)
			{
				_noblesseStageInProgress = true;
				_questKillsCurrent = 0;
				_questKillsTarget = 12;
				_currentZone = ProgressionKnowledgeGraph.selectNoblesseQuestZone(1);
				_player.teleToLocation(_currentZone.huntingCenter(), true);
				_lastAction = "Q241 Precious Soul Pt.1 (SubLv" + _subClassLevel + "): Hunting Malruk Succubus & Crimson Drake (0/12)";
				return;
			}
			if ((_questKillsCurrent == 3) && (_player.getInventory().getItemByItemId(7597) == null))
			{
				_player.getInventory().addItem(ItemProcessType.QUEST, 7597, 1, _player, null); // Malruk Succubus Claw
				_player.getInventory().addItem(ItemProcessType.QUEST, 7587, 1, _player, null); // Legend of Seventeen
			}
			else if ((_questKillsCurrent == 7) && (_player.getInventory().getItemByItemId(7589) == null))
			{
				_player.getInventory().addItem(ItemProcessType.QUEST, 7589, 1, _player, null); // Echo Crystal - Forgotten Song
			}
			if (_questKillsCurrent >= _questKillsTarget)
			{
				_player.getInventory().addItem(ItemProcessType.QUEST, 7598, 5, _player, null); // Crimson Moss
				consumeItemIfPresent(7597, 7587, 7589, 7598);
				_player.getInventory().addItem(ItemProcessType.QUEST, 7677, 1, _player, null); // Virgil's Letter
				_noblesseQuestStage = 1;
				_noblesseStageInProgress = false;
				_macroState = CitizenMacroState.RETURN_TO_TOWN;
				_lastAction = "Completed Q241 Precious Soul Pt.1 -> Earned Virgil's Letter (7677)";
				return;
			}
			huntQuestMonstersInCurrentZone("Q241 Precious Soul Pt.1");
			return;
		}
		
		// Part 2: Q00242_PossessorOfAPreciousSoul_2 (Subclass Lv 60+)
		if (_noblesseQuestStage == 1)
		{
			if (!_noblesseStageInProgress)
			{
				_noblesseStageInProgress = true;
				consumeItemIfPresent(7677); // Virgil's Letter
				_questKillsCurrent = 0;
				_questKillsTarget = 12;
				_currentZone = ProgressionKnowledgeGraph.selectNoblesseQuestZone(2);
				_player.teleToLocation(_currentZone.huntingCenter(), true);
				_lastAction = "Q242 Precious Soul Pt.2 (SubLv" + _subClassLevel + "): Collecting 4 Orbs of Binding (7595) in Swamp of Screams";
				return;
			}
			if ((_questKillsCurrent > 0) && ((_questKillsCurrent % 3) == 0))
			{
				_player.getInventory().addItem(ItemProcessType.QUEST, 7595, 1, _player, null); // Orb of Binding
			}
			if (_questKillsCurrent >= _questKillsTarget)
			{
				consumeItemIfPresent(7595);
				_player.getInventory().addItem(ItemProcessType.QUEST, 7678, 1, _player, null); // Caradine's Letter
				_noblesseQuestStage = 2;
				_noblesseStageInProgress = false;
				_macroState = CitizenMacroState.RETURN_TO_TOWN;
				_lastAction = "Completed Q242 Precious Soul Pt.2 -> Rescued Fallen Unicorn & Earned Caradine's Letter (7678)";
				return;
			}
			huntQuestMonstersInCurrentZone("Q242 Precious Soul Pt.2");
			return;
		}
		
		// Part 3: Q00246_PossessorOfAPreciousSoul_3 (Subclass Lv 65+ & Raid Boss Flame of Splendor Barakiel 25325)
		if (_noblesseQuestStage == 2)
		{
			if (!_noblesseStageInProgress)
			{
				_noblesseStageInProgress = true;
				consumeItemIfPresent(7678); // Caradine's Letter
				_questKillsCurrent = 0;
				_questKillsTarget = 15;
				AutonomousPlayerManager.getInstance().joinOrFormQuestRaidParty(this);
				_currentZone = ProgressionKnowledgeGraph.selectNoblesseQuestZone(3);
				_player.teleToLocation(_currentZone.huntingCenter(), true);
				_lastAction = "Q246 Precious Soul Pt.3 (SubLv" + _subClassLevel + "): Contesting Barakiel (25325) in Valley of Saints";
				return;
			}
			if ((_questKillsCurrent == 5) && (_player.getInventory().getItemByItemId(7591) == null))
			{
				_player.getInventory().addItem(ItemProcessType.QUEST, 7591, 1, _player, null); // Ring of Goddess: Waterbinder
			}
			else if ((_questKillsCurrent == 10) && (_player.getInventory().getItemByItemId(7592) == null))
			{
				_player.getInventory().addItem(ItemProcessType.QUEST, 7592, 1, _player, null); // Necklace of Goddess: Evergreen
			}
			if (_questKillsCurrent >= _questKillsTarget)
			{
				_player.getInventory().addItem(ItemProcessType.QUEST, 7593, 1, _player, null); // Rain Song (Barakiel's Staff)
				consumeItemIfPresent(7591, 7592, 7593);
				_player.getInventory().addItem(ItemProcessType.QUEST, 7594, 1, _player, null); // Relic Box
				consumeItemIfPresent(7594);
				_player.getInventory().addItem(ItemProcessType.QUEST, 7679, 1, _player, null); // Caradine's Letter (Final)
				_noblesseQuestStage = 3;
				_noblesseStageInProgress = false;
				_macroState = CitizenMacroState.RETURN_TO_TOWN;
				_lastAction = "Completed Q246 Precious Soul Pt.3 -> Earned Barakiel's Rain Song (7593) & Caradine's Letter (7679)";
				return;
			}
			huntQuestMonstersOrRaidBossInCurrentZone("Q246 Barakiel / Valley of Saints", 25325);
			return;
		}
		
		// Part 4: Q00247_PossessorOfAPreciousSoul_4 (Subclass Lv 75+ Ordination with Lady of the Lake 31745)
		_currentZone = ProgressionKnowledgeGraph.selectNoblesseQuestZone(4);
		_player.teleToLocation(_currentZone.huntingCenter(), true);
		consumeItemIfPresent(7679); // Caradine's Letter
		_noblesseQuestStage = 4;
		_noblesseStageInProgress = false;
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
		_lastAction = "Completed Q247 Precious Soul Pt.4 -> Ascended to Noblesse (Tiara 7694 + Blessing 1323)" + (olyActive ? " & Olympiad Ready" : "");
		_macroState = CitizenMacroState.RETURN_TO_TOWN;
	}
	
	private void huntQuestMonstersOrRaidBossInCurrentZone(String questLabel, int... raidBossIds)
	{
		if (_player.isCastingNow())
		{
			return;
		}
		
		// Check if current target died
		final WorldObject currentObj = _player.getTarget();
		if ((currentObj != null) && currentObj.isCreature())
		{
			final Creature currentTarget = currentObj.asCreature();
			if (currentTarget.isAlikeDead())
			{
				if (currentTarget.isRaid())
				{
					_questKillsCurrent = _questKillsTarget;
				}
				else
				{
					_questKillsCurrent++;
				}
				onTargetDefeated(currentTarget);
				_player.setTarget(null);
				_lastAction = questLabel + " progress (" + _questKillsCurrent + "/" + _questKillsTarget + ")";
				return;
			}
		}
		
		// First check if one of the target Raid Bosses is spawned & alive nearby
		RaidBoss liveBoss = null;
		for (int bossId : raidBossIds)
		{
			final RaidBoss candidate = World.getNearestVisibleObjectInRange(_player, RaidBoss.class, 3000, rb -> (rb.getId() == bossId) && !rb.isAlikeDead() && rb.isSpawned());
			if (candidate != null)
			{
				liveBoss = candidate;
				break;
			}
		}
		
		if (liveBoss != null)
		{
			_player.setTarget(liveBoss);
			final boolean isMage = (_player.getPlayerClass() != null) && _player.getPlayerClass().isMage();
			final double dist = _player.calculateDistance2D(liveBoss);
			final double engageDist = isArcherCitizen() ? Math.max(750.0, _player.getPhysicalAttackRange() - 40.0) : (isMage ? 650.0 : 120.0);
			if (dist > engageDist)
			{
				if (!_player.isMoving())
				{
					_player.getAI().setIntentionMoveTo(liveBoss);
				}
				_lastAction = questLabel + ": Approaching Raid Boss " + liveBoss.getName();
				return;
			}
			engageTarget(liveBoss, isMage);
			_lastAction = questLabel + ": Contesting Raid Boss " + liveBoss.getName() + " (" + _questKillsCurrent + "/" + _questKillsTarget + ")";
			return;
		}
		
		huntQuestMonstersInCurrentZone(questLabel);
	}
	
	private void huntQuestMonstersInCurrentZone(String questLabel)
	{
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
				_lastAction = questLabel + " kill (" + _questKillsCurrent + "/" + _questKillsTarget + ")";
				return;
			}
		}
		
		Creature target = null;
		if ((_player.getTarget() != null) && _player.getTarget().isCreature())
		{
			final Creature candidate = _player.getTarget().asCreature();
			if (!candidate.isAlikeDead() && isValidQuestTarget(candidate))
			{
				target = candidate;
			}
		}
		if (target == null)
		{
			target = World.getNearestVisibleObjectInRange(_player, Creature.class, 2500, this::isValidQuestTarget);
			if (target != null)
			{
				_player.setTarget(target);
			}
		}
		if (target == null)
		{
			_questKillsCurrent++;
			_lastAction = questLabel + " scouting zone (" + _questKillsCurrent + "/" + _questKillsTarget + ")";
			return;
		}
		
		final PlayerClass pc = _player.getPlayerClass();
		final boolean isMage = (pc != null) && pc.isMage();
		final double dist = _player.calculateDistance2D(target);
		final double engageDist = isArcherCitizen() ? Math.max(750.0, _player.getPhysicalAttackRange() - 40.0) : (isMage ? 650.0 : 110.0);
		if (dist > engageDist)
		{
			if (!_player.isMoving())
			{
				_player.getAI().setIntentionMoveTo(target);
			}
			_lastAction = questLabel + ": Pursuing " + target.getName() + " (" + _questKillsCurrent + "/" + _questKillsTarget + ")";
			return;
		}
		
		engageTarget(target, isMage);
	}
	
	private boolean isValidQuestTarget(Creature creature)
	{
		if ((creature == null) || creature.isAlikeDead() || !creature.isMonster() || creature.isRaid())
		{
			return false;
		}
		if (!creature.isAutoAttackable(_player))
		{
			return false;
		}
		return (Math.abs(_player.getZ() - creature.getZ()) < 750)
			&& GeoEngine.getInstance().canSeeTarget(_player, creature)
			&& GeoEngine.getInstance().canMoveToTarget(_player.getX(), _player.getY(), _player.getZ(), creature.getX(), creature.getY(), creature.getZ(), _player.getInstanceId());
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
			EconomyProgressionAuditor.getInstance().recordInflow(_player, EconomySource.SEVEN_SIGNS_ANCIENT_ADENA, totalAncientAdena, getCurrentZoneName());
			if (isCrafterCitizen() || isSpoilerCitizen())
			{
				_spoilAndCraftPoints += 2;
			}
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
			EconomyProgressionAuditor.getInstance().recordSink(_player, EconomySink.SINK_TELEPORTS, 5_000L, _currentZone.name());
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
		if (isCrafterCitizen() || isSpoilerCitizen())
		{
			return (_spoilAndCraftPoints >= 3) || (ancientAdena >= 50_000L) || (_player.getAdena() >= 3_500_000L);
		}
		// Regular citizens cannot spoil or craft themselves: they must afford a Dwarven Craft Commission AND have an active Dwarf Artisan in the world
		final boolean hasWealth = (ancientAdena >= 50_000L) || (_player.getAdena() >= 3_500_000L);
		return hasWealth && AutonomousPlayerManager.getInstance().hasActiveDwarvenArtisan();
	}
	
	private void tickHuntAndFarm()
	{
		final int lvl = _player.getLevel();
		final PlayerClass pc = _player.getPlayerClass();
		final int classLvl = (pc != null) ? pc.level() : 0;
		
		// 1. Return to town if ready for class transfer, Pailaka, Subclass Campaign/Certification, 4-Part Noblesse Milestone, or gear tier upgrade
		final boolean readyForSubCampaign = (lvl >= 76) && (classLvl >= 3) && !_subclassCertified && ((_subclassQuestStage < 5) || (_subClassLevel >= 75));
		if ((((lvl >= 20) && (classLvl == 0)) || ((lvl >= 40) && (classLvl == 1)) || ((lvl >= 76) && (classLvl == 2)))
			|| readyForSubCampaign
			|| isEligibleForNextNoblesseQuestPart()
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
				evaluatePreviousSkillOutcome(currentTarget);
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
		
		// 5. GeoEngine stuck detection while pursuing target (Archers use full 850-1100 bow range!)
		final double dist = _player.calculateDistance2D(target);
		final boolean isMage = (_player.getPlayerClass() != null) && _player.getPlayerClass().isMage();
		final double engageRange = isArcherCitizen()
			? Math.max(750.0, _player.getPhysicalAttackRange() - 40.0)
			: (isMage ? 650.0 : 110.0);
		
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
		
		// 6. Execute deep archetype combat doctrine + UCB1 contextual toolkit optimization
		evaluatePreviousSkillOutcome(target);
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
	
	/**
	 * Encodes the current combat situation into a 4-bit contextual bucket ({@code 0..15}):
	 * <ul>
	 *   <li>Bit 0 ({@code 1}): Target High HP (&gt; 60%)</li>
	 *   <li>Bit 1 ({@code 2}): Target Debuffed (Surrender/Vortex/Gloom/Hex active)</li>
	 *   <li>Bit 2 ({@code 4}): Multi-Mob Cluster (2+ live monsters within 200u)</li>
	 *   <li>Bit 3 ({@code 8}): Close Range (&lt;= 180u)</li>
	 * </ul>
	 */
	private int computeSituationBucket(Creature target)
	{
		if (target == null)
		{
			return 0;
		}
		int bucket = 0;
		if ((target.getCurrentHp() / Math.max(1.0, target.getMaxHp())) > 0.60)
		{
			bucket |= 1;
		}
		if (target.isAffectedBySkill(1083) || target.isAffectedBySkill(1071) || target.isAffectedBySkill(1074)
			|| target.isAffectedBySkill(1169) || target.isAffectedBySkill(1339) || target.isAffectedBySkill(1340) || target.isAffectedBySkill(1341))
		{
			bucket |= 2;
		}
		int nearbyMobs = 0;
		for (Monster m : World.getVisibleObjectsInRange(_player, Monster.class, 200))
		{
			if ((m != null) && !m.isAlikeDead())
			{
				nearbyMobs++;
				if (nearbyMobs >= 2)
				{
					bucket |= 4;
					break;
				}
			}
		}
		if (_player.calculateDistance2D(target) <= 180.0)
		{
			bucket |= 8;
		}
		return bucket;
	}
	
	private static long ucbKey(int bucket, int skillId)
	{
		return ((long) (bucket & 0xF) << 32) | (skillId & 0xFFFFFFFFL);
	}
	
	private void evaluatePreviousSkillOutcome(Creature currentTarget)
	{
		if ((_lastCastSkillId <= 0) || (currentTarget == null) || (currentTarget.getObjectId() != _lastCastTargetObjId))
		{
			return;
		}
		final Skill castSkill = _player.getKnownSkill(_lastCastSkillId);
		final double hpNow = currentTarget.isAlikeDead() ? 0.0 : currentTarget.getCurrentHp();
		final double hpDelta = Math.max(0.0, _lastCastTargetHpBefore - hpNow);
		final double elapsedSec = Math.max(0.5, (System.currentTimeMillis() - _lastCastTimestampMs) / 1000.0);
		final double dpsUtility = (hpDelta / Math.max(1.0, currentTarget.getMaxHp())) * 100.0 / elapsedSec;
		
		double tacticalBonus = 0.0;
		if (castSkill != null)
		{
			final String name = castSkill.getName().toLowerCase(Locale.ROOT);
			if (name.contains("surrender") || name.contains("gloom") || name.contains("vortex"))
			{
				tacticalBonus += 35.0;
			}
			if (name.contains("sonic") || name.contains("force") || name.contains("focus"))
			{
				tacticalBonus += 25.0 + (_player.getCharges() * 6.0);
			}
			if ((_lastCastBucket & 4) != 0 && (name.contains("storm") || name.contains("blast") || name.contains("quake") || name.contains("whirlwind") || name.contains("buster")))
			{
				tacticalBonus += 40.0;
			}
		}
		
		final double observedUtility = dpsUtility + tacticalBonus;
		final long key = ucbKey(_lastCastBucket, _lastCastSkillId);
		final int prevTrials = _contextualSkillTrials.getOrDefault(key, 0);
		final double prevQ = _contextualSkillQ.getOrDefault(key, 20.0);
		final double updatedQ = (prevTrials == 0) ? observedUtility : ((0.75 * prevQ) + (0.25 * observedUtility));
		_contextualSkillTrials.put(key, prevTrials + 1);
		_contextualSkillQ.put(key, updatedQ);
		_bucketTotalTrials[_lastCastBucket & 0xF]++;
		_lastCastSkillId = 0;
	}
	
	private double scoreSkillWithUCB1(int bucket, Skill skill, Creature target)
	{
		final long key = ucbKey(bucket, skill.getId());
		final int trials = _contextualSkillTrials.getOrDefault(key, 0);
		final double qVal = _contextualSkillQ.getOrDefault(key, Math.max(15.0, skill.getMagicLevel() * 0.6));
		final int totalBucket = Math.max(1, _bucketTotalTrials[bucket & 0xF]);
		final double explorationBonus = 18.0 * Math.sqrt((2.0 * Math.log(totalBucket + 2.0)) / (trials + 1.0));
		
		// Penalize approach time if target is outside skill castRange
		double rangePenalty = 0.0;
		if ((target != null) && (skill.getCastRange() > 0))
		{
			final double dist = _player.calculateDistance2D(target);
			if (dist > skill.getCastRange())
			{
				rangePenalty = (dist - skill.getCastRange()) * 0.12;
			}
		}
		return qVal + explorationBonus - rangePenalty;
	}
	
	private void castTacticalSkill(Skill skill, Creature target, boolean ctrlPressed, String doctrineReason)
	{
		if (target != null)
		{
			_lastCastBucket = computeSituationBucket(target);
			_lastCastSkillId = skill.getId();
			_lastCastTargetObjId = target.getObjectId();
			_lastCastTargetHpBefore = target.getCurrentHp();
			_lastCastTimestampMs = System.currentTimeMillis();
		}
		_player.useMagic(skill, ctrlPressed, false);
		_lastDecisionReason = doctrineReason;
		_lastAction = doctrineReason + ((target != null && target != _player) ? (" -> " + target.getName()) : "");
	}
	
	/**
	 * Deep 4-Archetype Combat Combo &amp; Tactical Doctrine Engine + UCB1 Contextual Toolkit Exploration:
	 * <ol>
	 *   <li><b>Dwarven Spoiler Pre-Hook:</b> Scavenger/Bounty Hunter/Fortune Seeker casts {@code Spoil (254)} via {@code useMagic}.</li>
	 *   <li><b>Elemental Mage Combo Doctrine:</b> {@code Surrender to Element / Gloom} -> {@code Elemental Vortex (1339-1343)} -> {@code Vortex Buster/Crusher/Slug (1451-1456)} -> UCB1 Primary Elemental Nuke.</li>
	 *   <li><b>Gladiator / Duelist &amp; Grand Khavatari Momentum Charge Doctrine:</b> Builds charges via {@code Sonic Rage (345)} / {@code Raging Force (346)} / {@code Sonic Focus (8)}, releases high-multiplier finishers ({@code Triple Sonic Slash 261}, {@code Hurricane Assault 284}, {@code Double Sonic Slash 5}), and weaves Auto-Attacks when finishers are within {@code <= 2.5s} of cooldown.</li>
	 *   <li><b>Warlord / Dreadnought Polearm Monster-Train &amp; Vampiric Cleave Doctrine:</b> Pulls {@code 3-8} monsters into a stacked cluster, maintains {@code Fell Swoop (421)} / {@code Thrill Fight (130)}, locks down with {@code Shock Blast (361)} / {@code Thunder Storm (48)}, and prioritizes Polearm Auto-Attack Cleave when HP &lt; 85% to multiply Vampiric Rage life-steal across up to 12 targets.</li>
	 *   <li><b>Archer Range-Preservation &amp; Auto-Attack Supremacy Doctrine:</b> Stands at full {@code 850-1100} bow range, strictly discards any skill whose {@code castRange < distanceToTarget} (refusing to waste DPS running closer), and relies on Soulshot Critical Auto-Attacks + long-range {@code Lethal Shot (343)} / {@code Seven Arrow (924)}.</li>
	 * </ol>
	 */
	private void engageTarget(Creature target, boolean isMage)
	{
		// 0. Authentic Dwarven Spoil (254): ONLY Scavenger -> Bounty Hunter -> Fortune Seeker citizens cast Spoil via useMagic
		if (target.isMonster() && isSpoilerCitizen())
		{
			final Monster monster = target.asMonster();
			if ((monster != null) && !monster.isSpoiled() && (monster.getSpoilerObjectId() == 0))
			{
				final Skill spoilSkill = getReadySkill(254);
				if (spoilSkill != null)
				{
					castTacticalSkill(spoilSkill, monster, false, "Spoiler Doctrine: Casting Spoil (254)");
					return;
				}
			}
		}
		
		// 1. Elemental Mage Combo Doctrine (Surrender -> Vortex -> Vortex Slug/Buster -> UCB1 Elemental Nuke)
		if (isMage)
		{
			executeMageComboDoctrine(target);
			return;
		}
		
		// 2. Archer Range-Preservation & Auto-Attack Supremacy Doctrine
		if (isArcherCitizen())
		{
			executeArcherRangeDoctrine(target);
			return;
		}
		
		// 3. Gladiator / Duelist & Tyrant / Grand Khavatari Momentum Charge-and-Release Doctrine
		if (isMomentumFighterCitizen())
		{
			executeMomentumChargeDoctrine(target);
			return;
		}
		
		// 4. Warlord / Dreadnought Polearm Monster-Train & Vampiric Cleave Doctrine
		if (isPolearmTrainCitizen())
		{
			executeDreadnoughtTrainDoctrine(target);
			return;
		}
		
		// 5. General Melee / Dagger / Knight Tactical Rotation with UCB1 Toolkit Exploration
		final Skill offensiveSkill = selectBestOffensiveSkill(target, false);
		if (offensiveSkill != null)
		{
			castTacticalSkill(offensiveSkill, target, true, "UCB1 Tactical Skill: " + offensiveSkill.getName());
			return;
		}
		
		_player.getAI().setIntentionAttack(target);
		_lastDecisionReason = "Melee Auto-Attack weave";
		_lastAction = "Attacking " + target.getName() + " (Lv " + target.getLevel() + ")";
	}
	
	private void executeMageComboDoctrine(Creature target)
	{
		final double targetHpRatio = target.getCurrentHp() / Math.max(1.0, target.getMaxHp());
		final double mpRatio = _player.getCurrentMp() / Math.max(1.0, _player.getMaxMp());
		final double hpRatio = _player.getCurrentHp() / Math.max(1.0, _player.getMaxHp());
		
		// Sustain weave: Body To Mind (1157) when MP is low and HP is healthy
		if ((mpRatio < 0.45) && (hpRatio > 0.72))
		{
			final Skill btm = getReadySkill(1157);
			if (btm != null)
			{
				castTacticalSkill(btm, _player, false, "Mage Sustain: Body To Mind (1157) MP recovery");
				return;
			}
		}
		
		// Step 1: Elemental Vulnerability Debuff (Surrender to Fire 1083 / Water 1071 / Wind 1074 / Gloom 1169) when target HP > 40%
		if (targetHpRatio > 0.40)
		{
			final int[] surrenderIds = { 1169, 1083, 1071, 1074, 1222 }; // Curse Gloom, Surrender to Fire, Water, Wind, Curse Chaos
			for (int debuffId : surrenderIds)
			{
				if (!target.isAffectedBySkill(debuffId))
				{
					final Skill debuff = getReadySkill(debuffId);
					if (debuff != null)
					{
						castTacticalSkill(debuff, target, true, "Mage Combo [1/3]: Debuff " + debuff.getName() + " (-Elemental Res)");
						return;
					}
				}
			}
		}
		
		// Step 2: Detonate active Elemental Vortex with Vortex Buster (1451) / Ice Vortex Crusher (1453) / Wind Vortex Slug (1456)
		final int[][] vortexToDetonator =
		{
			{ 1339, 1451 }, // Fire Vortex -> Fire Vortex Buster
			{ 1340, 1453 }, // Ice Vortex -> Ice Vortex Crusher
			{ 1341, 1456 } // Wind Vortex -> Wind Vortex Slug
		};
		for (int[] pair : vortexToDetonator)
		{
			if (target.isAffectedBySkill(pair[0]))
			{
				final Skill detonator = getReadySkill(pair[1]);
				if (detonator != null)
				{
					castTacticalSkill(detonator, target, true, "Mage Combo [3/3]: Detonate " + detonator.getName() + " on active Vortex!");
					return;
				}
			}
		}
		
		// Step 3: Place Elemental Vortex (Fire Vortex 1339 / Ice Vortex 1340 / Wind Vortex 1341 / Dark Vortex 1343 / Light Vortex 1342)
		final int[] vortexIds = { 1339, 1340, 1341, 1343, 1342 };
		for (int vortexId : vortexIds)
		{
			if (!target.isAffectedBySkill(vortexId))
			{
				final Skill vortex = getReadySkill(vortexId);
				if (vortex != null)
				{
					castTacticalSkill(vortex, target, true, "Mage Combo [2/3]: Cast " + vortex.getName() + " (Multi-Debuff + DoT)");
					return;
				}
			}
		}
		
		// Step 4: Select highest UCB1-scored Primary Elemental Nuke across the citizen's learned spellbook
		final int bucket = computeSituationBucket(target);
		final int[] primaryNukes = { 1234, 1230, 1235, 1239, 1148, 1265, 1267, 1178, 1184, 1181, 1177 };
		Skill bestNuke = null;
		double bestScore = Double.NEGATIVE_INFINITY;
		for (int nukeId : primaryNukes)
		{
			if ((nukeId == 1234) && (hpRatio > 0.80))
			{
				continue; // Save Vampiric Claw for when HP < 80%
			}
			final Skill nuke = getReadySkill(nukeId);
			if (nuke != null)
			{
				final double score = scoreSkillWithUCB1(bucket, nuke, target);
				if (score > bestScore)
				{
					bestScore = score;
					bestNuke = nuke;
				}
			}
		}
		if (bestNuke != null)
		{
			castTacticalSkill(bestNuke, target, true, String.format(Locale.US, "Mage UCB1 Nuke [B%d Q=%.1f]: %s", bucket, bestScore, bestNuke.getName()));
			return;
		}
		
		final Skill fallback = selectBestOffensiveSkill(target, true);
		if (fallback != null)
		{
			castTacticalSkill(fallback, target, true, "Mage UCB1 Fallback: " + fallback.getName());
			return;
		}
		_player.getAI().setIntentionAttack(target);
	}
	
	private void executeMomentumChargeDoctrine(Creature target)
	{
		final int charges = _player.getCharges();
		
		// Maintain Duelist / Khavatari self-buffs: Duelist Spirit (297), War Cry (78), Bison/Ogre/Puma Totem (292/109/282)
		for (int buffId : new int[] { 297, 78, 292, 109, 282 })
		{
			if (!_player.isAffectedBySkill(buffId))
			{
				final Skill selfBuff = getReadySkill(buffId);
				if (selfBuff != null)
				{
					castTacticalSkill(selfBuff, _player, false, "Momentum Buff: Activating " + selfBuff.getName());
					return;
				}
			}
		}
		
		// Priority 1: If charges >= 3, release top-tier 3rd/2nd class Finisher (Triple Sonic Slash 261 / Hurricane Assault 284)
		if (charges >= 3)
		{
			for (int finisherId : new int[] { 261, 284 })
			{
				final Skill finisher = getReadySkill(finisherId);
				if (finisher != null)
				{
					castTacticalSkill(finisher, target, true, "Momentum Release [Charges=" + charges + "]: Finisher " + finisher.getName());
					return;
				}
			}
		}
		
		// Priority 2: Build charges via 3rd-class Ranged Builder (Sonic Rage 345 / Raging Force 346) which deals damage AND grants +1 charge!
		if (charges < 5)
		{
			for (int rageBuilderId : new int[] { 345, 346 })
			{
				final Skill builder = getReadySkill(rageBuilderId);
				if (builder != null)
				{
					castTacticalSkill(builder, target, true, "Momentum Builder [Charges=" + charges + "->+1]: " + builder.getName());
					return;
				}
			}
		}
		
		// Priority 3: Manual Charge Generation (Sonic Focus 8 / Focused Force 50) if charges < 3
		if (charges < 3)
		{
			for (int focusId : new int[] { 8, 50 })
			{
				final Skill focus = getReadySkill(focusId);
				if (focus != null)
				{
					castTacticalSkill(focus, _player, false, "Momentum Charge [Charges=" + charges + "->+1]: Manual " + focus.getName());
					return;
				}
			}
		}
		
		// Priority 4: Cooldown vs. Auto-Attack Weave Decision (Anti-Spammer Guard!)
		// If Triple Sonic Slash (261) or Hurricane Assault (284) is learned and coming off cooldown in <= 2500ms, HOLD charges and Auto-Attack!
		for (int topFinisherId : new int[] { 261, 284 })
		{
			final Skill knownFinisher = _player.getKnownSkill(topFinisherId);
			if (knownFinisher != null)
			{
				final long remainingCd = _player.getSkillRemainingReuseTime(knownFinisher.getReuseHashCode());
				if ((remainingCd > 0L) && (remainingCd <= 2500L) && (charges >= 2))
				{
					_player.getAI().setIntentionAttack(target);
					_lastDecisionReason = "Momentum Weave [Charges=" + charges + "]: Holding charges for " + knownFinisher.getName() + " CD (" + remainingCd + "ms) -> Auto-Attack";
					_lastAction = _lastDecisionReason;
					return;
				}
			}
		}
		
		// Priority 5: Spend 2+ charges on UCB1-scored Double Sonic Slash (5) / Sonic Blaster (6) / Force Blaster (35) / Sonic Buster (9)
		if (charges >= 2)
		{
			final int bucket = computeSituationBucket(target);
			Skill bestMid = null;
			double bestScore = Double.NEGATIVE_INFINITY;
			for (int midSkillId : new int[] { 5, 6, 35, 9, 17 })
			{
				final Skill midSkill = getReadySkill(midSkillId);
				if (midSkill != null)
				{
					final double s = scoreSkillWithUCB1(bucket, midSkill, target);
					if (s > bestScore)
					{
						bestScore = s;
						bestMid = midSkill;
					}
				}
			}
			if (bestMid != null)
			{
				castTacticalSkill(bestMid, target, true, "Momentum UCB1 Release [Charges=" + charges + "]: " + bestMid.getName());
				return;
			}
		}
		
		// Priority 6: Early-level 1st/2nd class skills (Triple Slash 1, Iron Punch 29) or Dual-Sword Auto-Attack Weave
		for (int earlyId : new int[] { 1, 29 })
		{
			final Skill early = getReadySkill(earlyId);
			if ((early != null) && (ThreadLocalRandom.current().nextInt(100) < 45))
			{
				castTacticalSkill(early, target, true, "Momentum Early Combo: " + early.getName());
				return;
			}
		}
		
		_player.getAI().setIntentionAttack(target);
		_lastDecisionReason = "Momentum Auto-Attack Weave [Charges=" + charges + "] (Vampiric sustain + CD wait)";
		_lastAction = "Dual/Fist Auto-Attacking " + target.getName() + " [Charges=" + charges + "]";
	}
	
	private void executeDreadnoughtTrainDoctrine(Creature target)
	{
		// Maintain Polearm Multi-Target Stances & Buffs: Fell Swoop (421), Thrill Fight (130), Battle Roar (121)
		for (int buffId : new int[] { 421, 130, 121 })
		{
			if (!_player.isAffectedBySkill(buffId))
			{
				final Skill buff = getReadySkill(buffId);
				if (buff != null)
				{
					castTacticalSkill(buff, _player, false, "Dreadnought Polearm Stance: " + buff.getName() + " (+Multi-Target Cleave)");
					return;
				}
			}
		}
		
		// Count stacked monsters in melee cleave radius (<= 165) vs unaggroed monsters in train pull range (170..650)
		int stackedInMelee = 0;
		Monster nextTrainPullCandidate = null;
		for (Monster m : World.getVisibleObjectsInRange(_player, Monster.class, 650))
		{
			if ((m == null) || m.isAlikeDead() || m.isRaid() || !m.isAutoAttackable(_player))
			{
				continue;
			}
			final double d = _player.calculateDistance2D(m);
			if (d <= 165.0)
			{
				stackedInMelee++;
			}
			else if ((nextTrainPullCandidate == null) && (m.getTarget() != _player))
			{
				nextTrainPullCandidate = m;
			}
		}
		
		final double hpRatio = _player.getCurrentHp() / Math.max(1.0, _player.getMaxHp());
		
		// Phase 1: Monster Train Pulling Phase (if < 4 mobs stacked in melee and HP > 65% and more mobs nearby)
		if ((stackedInMelee < 4) && (nextTrainPullCandidate != null) && (hpRatio > 0.65))
		{
			final Skill provoke = getReadySkill(286); // Provoke (AoE agro pull)
			if (provoke != null)
			{
				castTacticalSkill(provoke, _player, false, "Dreadnought Train [" + stackedInMelee + " stacked]: Casting Provoke (286) to pull monster train!");
				return;
			}
			_player.setTarget(nextTrainPullCandidate);
			_player.getAI().setIntentionAttack(nextTrainPullCandidate);
			_lastDecisionReason = "Dreadnought Train [" + stackedInMelee + " stacked]: Tagging " + nextTrainPullCandidate.getName() + " into cleave stack";
			_lastAction = _lastDecisionReason;
			return;
		}
		
		// Phase 2: AoE Crowd-Control Stun on Stacked Train (Shock Blast 361 / Thunder Storm 48)
		if (stackedInMelee >= 2)
		{
			for (int stunId : new int[] { 361, 48 })
			{
				final Skill aoeStun = getReadySkill(stunId);
				if (aoeStun != null)
				{
					castTacticalSkill(aoeStun, target, true, "Dreadnought Cleave [" + stackedInMelee + " mobs]: AoE Stun " + aoeStun.getName());
					return;
				}
			}
		}
		
		// Phase 3: Vampiric Auto-Attack Cleave vs. AoE Skill Weave
		// When HP < 85%, Polearm Auto-Attacks hit up to 11-12 targets simultaneously and multiply Vampiric Rage life-steal!
		if (hpRatio < 0.85)
		{
			_player.getAI().setIntentionAttack(target);
			_lastDecisionReason = "Dreadnought Vampiric Cleave [" + stackedInMelee + " mobs, HP " + (int) Math.round(hpRatio * 100) + "%]: Polearm Auto-Attack multi-target life-steal";
			_lastAction = _lastDecisionReason;
			return;
		}
		
		// When HP >= 85%, weave UCB1-scored AoE Polearm Cleave skills (Earthquake 347, Whirlwind 36, Wrath 320)
		final int bucket = computeSituationBucket(target);
		for (int cleaveId : new int[] { 347, 320, 36 })
		{
			final Skill cleave = getReadySkill(cleaveId);
			if (cleave != null)
			{
				castTacticalSkill(cleave, target, true, "Dreadnought AoE Skill [B" + bucket + ", " + stackedInMelee + " mobs]: " + cleave.getName());
				return;
			}
		}
		
		_player.getAI().setIntentionAttack(target);
		_lastDecisionReason = "Dreadnought Polearm Cleave [" + stackedInMelee + " mobs]: Multi-target Auto-Attack";
		_lastAction = _lastDecisionReason;
	}
	
	private void executeArcherRangeDoctrine(Creature target)
	{
		// Maintain Archer Self-Buffs: Rapid Shot (99), Hawk Eye (314)
		for (int buffId : new int[] { 99, 314 })
		{
			if (!_player.isAffectedBySkill(buffId))
			{
				final Skill buff = getReadySkill(buffId);
				if (buff != null)
				{
					castTacticalSkill(buff, _player, false, "Archer Stance: Activating " + buff.getName());
					return;
				}
			}
		}
		
		final double dist = _player.calculateDistance2D(target);
		final int atkRange = Math.max(700, _player.getPhysicalAttackRange());
		
		// Emergency Peel: If monster closed within < 240 range, cast Stun Shot (101) or Hamstring Shot (354)
		if (dist < 240.0)
		{
			for (int peelId : new int[] { 101, 354 })
			{
				final Skill peel = getReadySkill(peelId);
				if (peel != null)
				{
					castTacticalSkill(peel, target, true, "Archer Peel [" + (int) dist + "u]: " + peel.getName() + " against close target");
					return;
				}
			}
		}
		
		// Strict Range-Preservation Filter:
		// Evaluate high-power bow skills (Seven Arrow 924, Lethal Shot 343, Double Shot 19),
		// but DISCARD any skill whose castRange < dist so the Archer NEVER wastes time running closer!
		final int[] bowSkills = { 924, 343, 19 };
		for (int skillId : bowSkills)
		{
			final Skill skill = getReadySkill(skillId);
			if (skill == null)
			{
				continue;
			}
			final int skillRange = skill.getCastRange();
			if ((dist > skillRange) && (dist <= atkRange))
			{
				// Discard short-range skill and fire full-range Soulshot Critical Auto-Attacks!
				_player.getAI().setIntentionAttack(target);
				_lastDecisionReason = "Archer Range Doctrine [" + (int) dist + "u/" + atkRange + "u]: Discarded short-range " + skill.getName() + " (" + skillRange + "u) -> Full-Range Crit Auto-Attack";
				_lastAction = _lastDecisionReason;
				return;
			}
			// Only cast Double Shot (19) if below Lv 70 and target HP > 65%; at endgame, only Lethal Shot (343) / Seven Arrow (924) beat Bow Auto-Attack DPS!
			if ((skillId == 19) && ((_player.getLevel() >= 70) || (target.getCurrentHp() < (target.getMaxHp() * 0.65))))
			{
				continue;
			}
			if (dist <= skillRange)
			{
				castTacticalSkill(skill, target, true, "Archer High-Power Skill [" + (int) dist + "u]: " + skill.getName());
				return;
			}
		}
		
		// Default: Full-Range Soulshot Critical Auto-Attack Supremacy
		_player.getAI().setIntentionAttack(target);
		_lastDecisionReason = "Archer Auto-Attack Supremacy [" + (int) dist + "u/" + atkRange + "u]: Full-Range Soulshot Crit Bow Auto-Attack";
		_lastAction = "Bow Crit Auto-Attacking " + target.getName() + " (" + (int) dist + "u)";
	}
	
	private Skill getReadySkill(int skillId)
	{
		final Skill skill = _player.getKnownSkill(skillId);
		if ((skill == null) || _player.isSkillDisabled(skill))
		{
			return null;
		}
		if (_player.getCurrentMp() < (skill.getMpConsume() + skill.getMpInitialConsume() + 5))
		{
			return null;
		}
		return skill;
	}
	
	private Skill selectBestOffensiveSkill(Creature target, boolean isMage)
	{
		final int bucket = computeSituationBucket(target);
		Skill best = null;
		double bestScore = Double.NEGATIVE_INFINITY;
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
			final String nameLower = skill.getName().toLowerCase(Locale.ROOT);
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
			if (!isMage && (ThreadLocalRandom.current().nextInt(100) >= 55))
			{
				continue;
			}
			final double score = scoreSkillWithUCB1(bucket, skill, target);
			if (score >= bestScore)
			{
				bestScore = score;
				best = skill;
			}
		}
		return best;
	}
	
	private void onTargetDefeated(Creature defeated)
	{
		// 1. Authentic Dwarven Sweeper (42): ONLY Spoiler citizens sweep real XML <spoil> drops via monster.takeSweep()
		if (defeated.isMonster())
		{
			final Monster monster = defeated.asMonster();
			if ((monster != null) && isSpoilerCitizen() && monster.isSpoiled() && monster.checkSpoilOwner(_player, false))
			{
				final Skill sweeperSkill = _player.getKnownSkill(42);
				if (sweeperSkill != null)
				{
					_player.broadcastPacket(new MagicSkillUse(_player, monster, 42, sweeperSkill.getLevel(), 0, 0));
					final Collection<ItemHolder> sweptItems = monster.takeSweep();
					if ((sweptItems != null) && !sweptItems.isEmpty())
					{
						long totalSweptCount = 0L;
						for (ItemHolder item : sweptItems)
						{
							_player.addItem(ItemProcessType.SWEEP, item, _player, true);
							totalSweptCount += item.getCount();
						}
						_spoilAndCraftPoints += sweptItems.size();
						EconomyProgressionAuditor.getInstance().recordSpoiledMaterials(_player, totalSweptCount, getCurrentZoneName());
						_lastAction = "Swept " + sweptItems.size() + " real spoil drop(s) from " + monster.getName() + " (Spoil Pts: " + _spoilAndCraftPoints + ")";
					}
				}
			}
			else if ((monster != null) && isCrafterCitizen() && (_player.getKnownSkill(172) != null))
			{
				// Crafter citizens (Artisan -> Warsmith -> Maestro) accumulate craft proficiency from real monster salvage & Create Item (172)
				_spoilAndCraftPoints++;
			}
			
			// 2. Seven Signs Seal Stone collection in Catacombs & Necropolises
			if ((_currentZone != null) && _currentZone.isSevenSignsCatacomb())
			{
				final int sealStoneId = 6360 + ThreadLocalRandom.current().nextInt(3); // 6360 Blue, 6361 Green, 6362 Red
				final long stoneCount = 150L * Math.max(1, _player.getLevel() / 15);
				_player.getInventory().addItem(ItemProcessType.LOOT, sealStoneId, stoneCount, _player, monster);
			}
			
			// 3. Subclass Leveling (Stage 5: Subclass Lv 40 -> 75 through real monster kills, triggering 4-Part Noblesse at SubLv 50/60/65/75!)
			if ((_subclassQuestStage == 5) && (_subClassLevel >= 40) && (_subClassLevel < 75))
			{
				_subClassLevel++;
				if ((_subClassLevel == 65) && (_player.getInventory().getItemByItemId(10280) == null))
				{
					_player.getInventory().addItem(ItemProcessType.QUEST, 10280, 1, _player, null); // Certificate - Emergent Ability
					final Skill boostCp = SkillData.getInstance().getSkill(6563, 1);
					if (boostCp != null)
					{
						_player.addSkill(boostCp, true);
					}
					_lastAction = "Subclass reached Lv 65 -> Earned Emergent Ability Certificate (10280)";
				}
				else if (_subClassLevel >= 75)
				{
					_lastAction = "Subclass reached Lv 75! Ready for Master & Class Certification in town";
				}
			}
		}
		
		// 4. Verify XP/Adena progression, record Economy inflow & run starvation QA check
		final long currentExp = _player.getExp();
		final long currentAdena = _player.getAdena();
		if (currentAdena > _lastAdenaSnapshot)
		{
			EconomyProgressionAuditor.getInstance().recordInflow(_player, EconomySource.MOB_DROP_ADENA, currentAdena - _lastAdenaSnapshot, getCurrentZoneName());
		}
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
			if (targetTier >= 4)
			{
				// Tier 4 (A-Craft) & Tier 5 (S-SA): ONLY Dwarven Crafters/Spoilers can self-craft; Regular citizens MUST commission an active Dwarf citizen!
				if (!isCrafterCitizen() && !isSpoilerCitizen())
				{
					final long commissionCost = Math.min(Math.max(1_500_000L, _player.getAdena() / 2), loadout.adenaCost());
					final boolean commissioned = AutonomousPlayerManager.getInstance().commissionDwarvenCraft(this, targetTier, commissionCost);
					if (!commissioned)
					{
						// Cannot upgrade above B-Shop without an active Dwarven Crafter/Spoiler transaction
						return;
					}
				}
				else
				{
					// Dwarf Crafter / Spoiler uses Create Item (172) + Mammon Ancient Adena unsealing
					final Skill createItem = _player.getKnownSkill(172);
					if (createItem != null)
					{
						_player.broadcastPacket(new MagicSkillUse(_player, _player, 172, createItem.getLevel(), 0, 0));
					}
					final long unsealCost = Math.min(_player.getAdena() / 3, loadout.adenaCost() / 2);
					if (unsealCost > 0)
					{
						_player.reduceAdena(ItemProcessType.FEE, unsealCost, _player, false);
						EconomyProgressionAuditor.getInstance().recordSink(_player, EconomySink.SINK_CLASS_AND_MAMMON, unsealCost, "Mammon Unseal " + loadout.gradeLabel());
						_lastAdenaSnapshot = _player.getAdena();
					}
					_spoilAndCraftPoints += 3;
					_lastAction = "Dwarven Forge: Crafted & Unsealed " + loadout.gradeLabel() + " equipment set";
				}
			}
			else if (!isInitialSpawn && (_player.getAdena() < loadout.adenaCost()) && (targetTier <= 3))
			{
				GameplayAnomalyAuditor.getInstance().report(
					_player,
					AnomalyType.MULTISELL_MISSING_INGREDIENT,
					getCurrentZoneName(),
					"Citizen reached Lv " + _player.getLevel() + " (" + loadout.gradeLabel() + ") with " + _player.getAdena() + " Adena < required " + loadout.adenaCost());
			}
			else if (!isInitialSpawn && (loadout.adenaCost() > 0) && (_player.getAdena() >= loadout.adenaCost()))
			{
				_player.reduceAdena(ItemProcessType.BUY, loadout.adenaCost(), _player, false);
				EconomyProgressionAuditor.getInstance().recordSink(_player, EconomySink.SINK_GEAR_MERCHANT, loadout.adenaCost(), "Purchased " + loadout.gradeLabel());
				_lastAdenaSnapshot = _player.getAdena();
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
		ensureInventoryStock(loadout.soulshotId(), 2000, isInitialSpawn);
		ensureInventoryStock(loadout.spiritshotId(), 2000, isInitialSpawn);
		if (loadout.arrowId() > 0)
		{
			ensureInventoryStock(loadout.arrowId(), 2000, isInitialSpawn);
		}
		ensureInventoryStock(1061, 100, isInitialSpawn); // Greater Healing Potion
		ensureInventoryStock(1539, 100, isInitialSpawn); // Major Healing Potion
		ensureInventoryStock(728, 100, isInitialSpawn); // Mana Potion (400 MP / 2s)
		ensureInventoryStock(736, 10, isInitialSpawn); // Scroll of Escape
		
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
	
	private void ensureInventoryStock(int itemId, long targetCount, boolean isInitialSpawn)
	{
		if ((itemId <= 0) || (ItemData.getInstance().getTemplate(itemId) == null))
		{
			return;
		}
		final Item existing = _player.getInventory().getItemByItemId(itemId);
		final long current = (existing != null) ? existing.getCount() : 0L;
		if (current < (targetCount / 3))
		{
			final long buyQty = targetCount - current;
			if (!isInitialSpawn && (_player.getAdena() >= 5_000L))
			{
				final long fee = Math.min(2_500L, _player.getAdena() / 10);
				if (fee > 0)
				{
					_player.reduceAdena(ItemProcessType.BUY, fee, _player, false);
					EconomyProgressionAuditor.getInstance().recordSink(_player, EconomySink.SINK_CONSUMABLES, fee, "Restock itemId=" + itemId);
					_lastAdenaSnapshot = _player.getAdena();
				}
			}
			_player.getInventory().addItem(ItemProcessType.BUY, itemId, buyQty, _player, null);
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
