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
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.instance.Chest;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.actor.instance.RaidBoss;
import org.l2jmobius.gameserver.entity.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Real-Player Autonomous Citizen ({@code Lv 1 -> 85}) with a Hierarchical Macro-Lifecycle State Machine,
 * Endgame Class Aspiration, Authentic Dwarven Spoil/Sweep/Craft Economy, Genuine Server-Side {@link Quest} &amp; {@link QuestState}
 * Validation, Deep 7-Archetype Combat Doctrines (+ Treasure Chest Unlock Gambling) + UCB1 Contextual Toolkit Exploration Memory,
 * and Self-Reporting QA &amp; Economy Auditing.
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
	private int _activeQuestId = 0;
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
	
	public boolean isDaggerRogueCitizen()
	{
		if (isArcherCitizen())
		{
			return false;
		}
		final PlayerClass pc = _player.getPlayerClass();
		final String name = (pc != null) ? pc.name() : "";
		final String asp = (_targetEndgameClass != null) ? _targetEndgameClass.name() : "";
		return (pc == PlayerClass.TREASURE_HUNTER)
			|| (pc == PlayerClass.ADVENTURER)
			|| (pc == PlayerClass.PLAINS_WALKER)
			|| (pc == PlayerClass.WIND_RIDER)
			|| (pc == PlayerClass.ABYSS_WALKER)
			|| (pc == PlayerClass.GHOST_HUNTER)
			|| ((name.equals("ROGUE") || name.equals("ELVEN_SCOUT") || name.equals("ASSASSIN") || ((pc != null) && (pc.level() >= 0)))
				&& (asp.equals("ADVENTURER") || asp.equals("WIND_RIDER") || asp.equals("GHOST_HUNTER")));
	}
	
	public boolean isTankCitizen()
	{
		final PlayerClass pc = _player.getPlayerClass();
		final String name = (pc != null) ? pc.name() : "";
		final String asp = (_targetEndgameClass != null) ? _targetEndgameClass.name() : "";
		return (pc == PlayerClass.PALADIN)
			|| (pc == PlayerClass.PHOENIX_KNIGHT)
			|| (pc == PlayerClass.DARK_AVENGER)
			|| (pc == PlayerClass.HELL_KNIGHT)
			|| (pc == PlayerClass.TEMPLE_KNIGHT)
			|| (pc == PlayerClass.EVA_TEMPLAR)
			|| (pc == PlayerClass.SHILLIEN_KNIGHT)
			|| (pc == PlayerClass.SHILLIEN_TEMPLAR)
			|| ((name.contains("KNIGHT") || ((pc != null) && (pc.level() >= 0)))
				&& (asp.equals("PHOENIX_KNIGHT") || asp.equals("HELL_KNIGHT") || asp.equals("EVA_TEMPLAR") || asp.equals("SHILLIEN_TEMPLAR")));
	}
	
	public boolean isOverlordCitizen()
	{
		final PlayerClass pc = _player.getPlayerClass();
		return (_targetEndgameClass == PlayerClass.DOMINATOR)
			|| (pc == PlayerClass.OVERLORD)
			|| (pc == PlayerClass.DOMINATOR)
			|| ((pc == PlayerClass.ORC_SHAMAN) && (_targetEndgameClass == PlayerClass.DOMINATOR));
	}
	
	public boolean isSupportOrOverlordCitizen()
	{
		final PlayerClass pc = _player.getPlayerClass();
		final String name = (pc != null) ? pc.name() : "";
		final String asp = (_targetEndgameClass != null) ? _targetEndgameClass.name() : "";
		return isOverlordCitizen()
			|| name.equals("CLERIC") || name.equals("BISHOP") || name.equals("CARDINAL")
			|| name.equals("PROPHET") || name.equals("HIEROPHANT")
			|| name.equals("ORACLE") || name.equals("ELDER") || name.equals("EVA_SAINT")
			|| name.equals("SHILLIEN_ORACLE") || name.equals("SHILLIEN_ELDER") || name.equals("SHILLIEN_SAINT")
			|| name.equals("ORC_SHAMAN") || name.equals("WARCRYER") || name.equals("DOOMCRYER")
			|| name.equals("SWORDSINGER") || name.equals("SWORD_MUSE")
			|| name.equals("BLADEDANCER") || name.equals("SPECTRAL_DANCER")
			|| asp.equals("CARDINAL") || asp.equals("EVA_SAINT") || asp.equals("SHILLIEN_SAINT")
			|| asp.equals("HIEROPHANT") || asp.equals("DOMINATOR") || asp.equals("DOOMCRYER")
			|| asp.equals("SWORD_MUSE") || asp.equals("SPECTRAL_DANCER");
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
	
	private long _holdAtSpawnUntilMs = 0L;
	
	public void holdNearSponsor(String sponsorName, long durationMs)
	{
		_holdAtSpawnUntilMs = System.currentTimeMillis() + durationMs;
		_macroState = CitizenMacroState.SPAWN_AND_ORIENT;
		_lastAction = "Near " + sponsorName + " -> Orienting & gearing up";
	}
	
	private void tickSpawnAndOrient()
	{
		ensureFairPlayGearAndConsumables(true);
		applyTownSupportBuffs();
		final long now = System.currentTimeMillis();
		if (now < _holdAtSpawnUntilMs)
		{
			final long remSec = Math.max(1L, ((_holdAtSpawnUntilMs - now) + 999L) / 1000L);
			_lastAction = "Oriented & buffed (-> " + _targetEndgameClass.name() + ", departing in " + remSec + "s)";
			return;
		}
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
	 * Locates a live spawned {@link Npc} registered on the given server {@link Quest} so the citizen
	 * can invoke genuine {@link Quest#notifyTalk(Npc, Player)} and {@link Quest#notifyEvent(String, Npc, Player)} transitions.
	 */
	private Npc findSpawnedQuestNpc(Quest quest)
	{
		if (quest == null)
		{
			return null;
		}
		final Set<Integer> npcIds = quest.getRegisteredIds(ListenerRegisterType.NPC);
		if ((npcIds == null) || npcIds.isEmpty())
		{
			return null;
		}
		for (Npc nearby : World.getVisibleObjects(_player, Npc.class))
		{
			if ((nearby != null) && !nearby.isMonster() && npcIds.contains(nearby.getId()))
			{
				return nearby;
			}
		}
		for (int npcId : npcIds)
		{
			final Spawn spawn = SpawnTable.getInstance().getAnySpawn(npcId);
			if ((spawn != null) && (spawn.getLastSpawn() != null) && !spawn.getLastSpawn().isMonster())
			{
				return spawn.getLastSpawn();
			}
		}
		return null;
	}
	
	/**
	 * Starts or resumes a genuine server-side {@link Quest} and {@link QuestState} via {@link ScriptManager#getQuest(int)},
	 * persisting the active quest to {@code character_quests} in MariaDB and setting {@code cond = 1}.
	 */
	private QuestState ensureServerQuestStarted(int questId)
	{
		_activeQuestId = questId;
		if (questId <= 0)
		{
			return null;
		}
		final Quest quest = ScriptManager.getInstance().getQuest(questId);
		if (quest == null)
		{
			return null;
		}
		final QuestState qs = quest.getQuestState(_player, true);
		if ((qs != null) && qs.isCreated())
		{
			final Npc npc = findSpawnedQuestNpc(quest);
			if (npc != null)
			{
				try
				{
					quest.notifyTalk(npc, _player);
					quest.notifyEvent("ACCEPT", npc, _player);
				}
				catch (Exception ignored)
				{
				}
			}
			if (qs.isCreated())
			{
				qs.startQuest();
			}
		}
		return qs;
	}
	
	/**
	 * Invokes the server's genuine {@link Quest#onKill(Npc, Player, boolean)} hook on monster kill and awards
	 * intermediate registered {@code _questItemIds} through {@link Quest#giveItems(Player, int, long)} (which validates
	 * {@code ItemTemplate.isQuestItem()} and applies {@code RatesConfig.QUEST_ITEM_DROP_AMOUNT_MULTIPLIER}).
	 */
	private void notifyServerQuestKill(int questId, Creature killed)
	{
		if (questId <= 0)
		{
			return;
		}
		final Quest quest = ScriptManager.getInstance().getQuest(questId);
		if (quest == null)
		{
			return;
		}
		final QuestState qs = ensureServerQuestStarted(questId);
		if ((killed != null) && killed.isNpc())
		{
			try
			{
				quest.onKill(killed.asNpc(), _player, false);
			}
			catch (Exception ignored)
			{
			}
		}
		final int[] regItems = quest.getRegisteredItemIds();
		if ((regItems != null) && (regItems.length > 0))
		{
			final int interItemId = regItems[Math.abs(_questKillsCurrent) % regItems.length];
			if (interItemId > 0)
			{
				Quest.giveItems(_player, interItemId, 1);
			}
		}
		if ((qs != null) && qs.isStarted())
		{
			qs.setCond(Math.min(30, Math.max(1, qs.getCond() + 1)), true);
		}
	}
	
	/**
	 * Completes a quest stage with a genuine Quest NPC interaction ({@link Quest#notifyTalk(Npc, Player)}),
	 * exchanges intermediate {@code _questItemIds} via {@link Quest#takeItems(Player, int, long)}, awards the stage
	 * Occupation Mark / Resonance Amulet / Quest Item via {@link Quest#giveItems(Player, int, long)}, awards quest
	 * Adena &amp; XP/SP via {@link Quest#giveAdena(Player, long, boolean)} and {@link Quest#addExpAndSp(Player, long, int)},
	 * and completes the {@link QuestState} via {@link QuestState#exitQuest(boolean, boolean)}.
	 */
	private void completeServerQuestStageWithNpc(int questId, int rewardItemId, long rewardCount, long baseAdena, long baseExp, int baseSp, boolean exitQuestState)
	{
		final Quest quest = (questId > 0) ? ScriptManager.getInstance().getQuest(questId) : null;
		if (quest != null)
		{
			final QuestState qs = ensureServerQuestStarted(questId);
			final Npc npc = findSpawnedQuestNpc(quest);
			if (npc != null)
			{
				try
				{
					quest.notifyTalk(npc, _player);
				}
				catch (Exception ignored)
				{
				}
			}
			final int[] regItems = quest.getRegisteredItemIds();
			if (regItems != null)
			{
				for (int regId : regItems)
				{
					if ((regId != rewardItemId) && Quest.hasQuestItems(_player, regId))
					{
						Quest.takeItems(_player, regId, -1);
					}
				}
			}
			if (exitQuestState && (qs != null) && qs.isStarted())
			{
				qs.exitQuest(false, true);
			}
			else if ((qs != null) && qs.isStarted())
			{
				qs.setCond(Math.min(30, Math.max(1, qs.getCond() + 1)), true);
			}
		}
		if ((rewardItemId > 0) && (rewardCount > 0))
		{
			Quest.giveItems(_player, rewardItemId, rewardCount);
		}
		if (baseAdena > 0)
		{
			Quest.giveAdena(_player, baseAdena, true);
			EconomyProgressionAuditor.getInstance().recordInflow(_player, EconomySource.QUEST_REWARD_ADENA, baseAdena, getCurrentZoneName());
			_lastAdenaSnapshot = _player.getAdena();
		}
		if ((baseExp > 0) || (baseSp > 0))
		{
			Quest.addExpAndSp(_player, baseExp, baseSp);
		}
	}
	
	/**
	 * Multi-Stage Profession Quest Chains (Validated via Server {@link Quest} &amp; {@link QuestState}):
	 * <ul>
	 *   <li><b>1st Class (Lv 20, 3 Stages, Q401..Q418 / Q62..Q63):</b> Collects 2 intermediate Path quest items + final 1st-Class Occupation Mark across 3 zones.</li>
	 *   <li><b>2nd Class (Lv 40, 3-Mark Trial Chain, Q211..Q233 / Q64..Q66):</b> Completes Trial (Mark #1), Testimony (Mark #2), and Test (Mark #3) across Death Pass, Cruma Marshlands, and Breka's Stronghold.</li>
	 *   <li><b>3rd Class (Lv 76, 6-Tablet Saga Chain, Q67..Q100):</b> Collects all 6 Resonance Amulets (7284..7289) &amp; communes with all 6 Tablets of Vision across Hot Springs, Valley of Saints, Wall of Argos, Ketra Outpost, Shrine of the Loyal (Archon of Halisha), and Forbidden Gateway.</li>
	 * </ul>
	 */
	private void tickClassTransferQuest()
	{
		final PlayerClass pc = _player.getPlayerClass();
		final int classLvl = (pc != null) ? pc.level() : 0;
		final int targetClassTier = classLvl + 1;
		final int[] stageItems = ProgressionKnowledgeGraph.getClassTransferStageItemIds(pc, _targetEndgameClass, targetClassTier);
		final int[] questIds = ProgressionKnowledgeGraph.getClassTransferQuestIds(pc, _targetEndgameClass, targetClassTier);
		if (_classTransferSubStage < 1)
		{
			_classTransferSubStage = 1;
		}
		final int stageIdx = Math.min(_classTransferSubStage - 1, stageItems.length - 1);
		final int questId = questIds[Math.min(stageIdx, questIds.length - 1)];
		ensureServerQuestStarted(questId);
		
		if (_questKillsCurrent >= _questKillsTarget)
		{
			final int earnedItemId = stageItems[stageIdx];
			final boolean isFinalStageOfQuest = (_classTransferSubStage >= stageItems.length) || (targetClassTier == 2);
			completeServerQuestStageWithNpc(questId, earnedItemId, 1, 0L, 0L, 0, isFinalStageOfQuest);
			
			if (_classTransferSubStage < stageItems.length)
			{
				_classTransferSubStage++;
				_questKillsCurrent = 0;
				_questKillsTarget = (targetClassTier == 3) ? 6 : 8;
				_currentZone = ProgressionKnowledgeGraph.selectClassTransferQuestZone(targetClassTier, _classTransferSubStage);
				_player.teleToLocation(_currentZone.huntingCenter(), true);
				_lastAction = "Quest Q" + questId + " Stage Done (" + earnedItemId + ") -> Stage [" + _classTransferSubStage + "/" + stageItems.length + "] at " + _currentZone.name();
				return;
			}
			
			// Completed all stages (3/3 Marks or 6/6 Tablets of Vision)!
			if (targetClassTier >= 3)
			{
				Quest.giveItems(_player, 6622, 1); // Giant's Codex
			}
			final long questRewardAdena = targetClassTier * 85_000L;
			final long questRewardExp = targetClassTier * 150_000L;
			final int questRewardSp = targetClassTier * 18_000;
			completeServerQuestStageWithNpc(questId, 0, 0, questRewardAdena, questRewardExp, questRewardSp, true);
			_activeQuestId = 0;
			_macroState = CitizenMacroState.RETURN_TO_TOWN;
			_lastAction = "Completed all " + stageItems.length + " Class Transfer stages (Q" + questId + ") -> Returning to Grand Master";
			return;
		}
		
		huntQuestMonstersInCurrentZone("Q" + questId + " Trial [" + _classTransferSubStage + "/" + stageItems.length + "]");
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
		
		// Consume all earned Class Transfer Marks / Resonance Amulets via Quest.takeItems (3 Marks for 2nd Class, 6 Resonance Amulets for 3rd Class)
		final int targetTier = ((currentClass != null) ? currentClass.level() : 0) + 1;
		final int[] stageItems = ProgressionKnowledgeGraph.getClassTransferStageItemIds(currentClass, _targetEndgameClass, targetTier);
		Quest.takeItems(_player, -1, stageItems);
		_classTransferSubStage = 0;
		_activeQuestId = 0;
		
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
			ensureServerQuestStarted(144); // Q00144_PailakaInjuredDragon
			completeServerQuestStageWithNpc(144, 13296, 1, 0L, 10_800_000L, 950_000, true);
			_player.setVitalityPoints(PlayerStat.MAX_VITALITY_POINTS, true);
			equipItemIfValid(13296); // Pailaka Shirt (A-Grade)
			_lastAction = "Cleared Q144 Pailaka (Injured Dragon Lv73) -> Full Vitality & Pailaka Shirt";
		}
		else if ((lvl >= 61) && (_completedPailakaTier < 2))
		{
			_completedPailakaTier = 2;
			ensureServerQuestStarted(129); // Q00129_PailakaDevilsLegacy
			completeServerQuestStageWithNpc(129, 13033, 1, 0L, 4_200_000L, 420_000, true);
			_player.setVitalityPoints(PlayerStat.MAX_VITALITY_POINTS, true);
			equipItemIfValid(13033); // Pailaka Bracelet / Earring
			_lastAction = "Cleared Q129 Pailaka (Devil's Legacy Lv61) -> Full Vitality & Pailaka Earring";
		}
		else if ((lvl >= 36) && (_completedPailakaTier < 1))
		{
			_completedPailakaTier = 1;
			ensureServerQuestStarted(128); // Q00128_PailakaSongOfIceAndFire
			completeServerQuestStageWithNpc(128, 13032, 1, 0L, 810_000L, 75_000, true);
			_player.setVitalityPoints(PlayerStat.MAX_VITALITY_POINTS, true);
			equipItemIfValid(13032); // Pailaka Ring
			_lastAction = "Cleared Q128 Pailaka (Song of Ice & Fire Lv36) -> Full Vitality & Pailaka Ring";
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
	 * Authentic Multi-Stage Subclass Campaign (Validated via Server {@link Quest} Q234/Q236 &amp; Q235):
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
		final int q234Id = ProgressionKnowledgeGraph.getSubclassQuestId(_player.getRace(), 1);
		if (_subclassQuestStage == 0)
		{
			_subclassQuestStage = 1;
			_questKillsCurrent = 0;
			_questKillsTarget = 8;
			ensureServerQuestStarted(q234Id);
			AutonomousPlayerManager.getInstance().joinOrFormQuestRaidParty(this);
			_currentZone = ProgressionKnowledgeGraph.selectSubclassQuestZone(1);
			_player.teleToLocation(_currentZone.huntingCenter(), true);
			_lastAction = "Q" + q234Id + " Fate's Whisper S1: Contesting Cabrio (25035) at The Cemetery";
			return;
		}
		
		if (_subclassQuestStage == 1)
		{
			ensureServerQuestStarted(q234Id);
			if (_questKillsCurrent >= _questKillsTarget)
			{
				completeServerQuestStageWithNpc(q234Id, 4666, 1, 0L, 0L, 0, false); // Reiria's Soul Orb
				_subclassQuestStage = 2;
				_questKillsCurrent = 0;
				_questKillsTarget = 12;
				_currentZone = ProgressionKnowledgeGraph.selectSubclassQuestZone(2);
				_player.teleToLocation(_currentZone.huntingCenter(), true);
				_lastAction = "Q" + q234Id + " S2: Earned Reiria's Soul Orb (4666) -> Contesting ToI Raid Bosses";
				return;
			}
			huntQuestMonstersOrRaidBossInCurrentZone("Q" + q234Id + " S1 Cabrio", 25035);
			return;
		}
		
		if (_subclassQuestStage == 2)
		{
			ensureServerQuestStarted(q234Id);
			if ((_questKillsCurrent == 4) && !Quest.hasQuestItems(_player, 4667))
			{
				Quest.giveItems(_player, 4667, 1); // Hallate's Infernium Scepter
			}
			else if ((_questKillsCurrent == 8) && !Quest.hasQuestItems(_player, 4668))
			{
				Quest.giveItems(_player, 4668, 1); // Kernon's Infernium Scepter
			}
			if (_questKillsCurrent >= _questKillsTarget)
			{
				Quest.giveItems(_player, 4669, 1); // Golkonda's Infernium Scepter
				completeServerQuestStageWithNpc(q234Id, 4665, 1, 0L, 0L, 0, false); // Pipette Knife from Reorin
				_subclassQuestStage = 3;
				_questKillsCurrent = 0;
				_questKillsTarget = 5;
				_currentZone = ProgressionKnowledgeGraph.selectSubclassQuestZone(3);
				_player.teleToLocation(_currentZone.huntingCenter(), true);
				_lastAction = "Q" + q234Id + " S3: Obtained 3 Infernium Scepters -> Staining Pipette Knife (4665)";
				return;
			}
			huntQuestMonstersOrRaidBossInCurrentZone("Q" + q234Id + " S2 ToI Raids", 25220, 25054, 25126);
			return;
		}
		
		if (_subclassQuestStage == 3)
		{
			ensureServerQuestStarted(q234Id);
			if (_questKillsCurrent >= _questKillsTarget)
			{
				Quest.takeItems(_player, -1, 4665, 4666, 4667, 4668, 4669);
				Quest.giveItems(_player, 4673, 1); // Red Pipette Knife
				
				// B-Grade weapon + 984 Crystal (B-Grade) (1460) sink for Maestro Reorin
				final long bWeaponAndCrystalsCost = Math.min(_player.getAdena() / 2, 3_500_000L);
				if (bWeaponAndCrystalsCost > 0)
				{
					_player.reduceAdena(ItemProcessType.QUEST, bWeaponAndCrystalsCost, _player, false);
					EconomyProgressionAuditor.getInstance().recordSink(_player, EconomySink.SINK_CLASS_AND_MAMMON, bWeaponAndCrystalsCost, "Q234 B-Grade Weapon + 984 B-Crystals");
					_lastAdenaSnapshot = _player.getAdena();
				}
				Quest.takeItems(_player, 4673, -1);
				completeServerQuestStageWithNpc(q234Id, 5011, 1, 240_000L, 0L, 0, true); // Star of Destiny
				
				_subclassQuestStage = 4;
				_questKillsCurrent = 0;
				_questKillsTarget = 12;
				ensureServerQuestStarted(235); // Q00235_MimirsElixir
				_currentZone = ProgressionKnowledgeGraph.selectSubclassQuestZone(4);
				_player.teleToLocation(_currentZone.huntingCenter(), true);
				_lastAction = "Q235 Mimir's Elixir S4: Earned Star of Destiny (5011) -> Silent Valley";
				return;
			}
			huntQuestMonstersInCurrentZone("Q" + q234Id + " Stain Pipette Knife");
			return;
		}
		
		if (_subclassQuestStage == 4)
		{
			ensureServerQuestStarted(235);
			if ((_questKillsCurrent == 4) && !Quest.hasQuestItems(_player, 6320))
			{
				Quest.giveItems(_player, 6320, 1); // Pure Silver
			}
			else if ((_questKillsCurrent == 8) && !Quest.hasQuestItems(_player, 6321))
			{
				Quest.giveItems(_player, 6321, 1); // True Gold
			}
			if (_questKillsCurrent >= _questKillsTarget)
			{
				Quest.giveItems(_player, 6318, 1); // Blood Fire
				completeServerQuestStageWithNpc(235, 6319, 1, 0L, 0L, 0, true); // Mimir's Elixir
				Quest.takeItems(_player, -1, 5011, 6320, 6321, 6318, 6319);
				_activeQuestId = 0;
				_subClassLevel = 40;
				_subclassQuestStage = 5;
				_macroState = CitizenMacroState.RETURN_TO_TOWN;
				_lastAction = "Completed Q235 Mimir's Elixir (6319) -> Subclass unlocked at Lv 40 (Leveling 40->75)";
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
			ensureServerQuestStarted(136); // Q00136_MoreThanMeetsTheEye (Subclass Certification prerequisite)
			completeServerQuestStageWithNpc(136, 10280, 1, 0L, 0L, 0, true); // Certificate - Emergent Ability
			Quest.giveItems(_player, 10281, 1); // Certificate - Master Ability
			
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
	 *   <li>Part 1 ({@code Q00241_PossessorOfAPreciousSoul1}, Subclass {@code Lv 50+}): Hunts Malruk Succubus (20244) for Malruk Succubus Claw (7597), Baraham (27113) for Legend of Seventeen (7587), Taik Orc Supply Officer (20669) for Echo Crystal - Forgotten Song (7589), and Crimson Drake (20670) for Crimson Moss (7598) -> Virgil's Letter (7677).</li>
	 *   <li>Part 2 ({@code Q00242_PossessorOfAPreciousSoul2}, Subclass {@code Lv 60+}): Consumes Virgil's Letter (7677), hunts Restless Dead &amp; Matriarch (27317) in Swamp of Screams for 4 Orbs of Binding (7595), rescues Fallen Unicorn -> Caradine's Letter (7678).</li>
	 *   <li>Part 3 ({@code Q00246_PossessorOfAPreciousSoul3}, Subclass {@code Lv 65+}): Consumes Caradine's Letter (7678), hunts Pilgrim of Splendor (21541) for Ring of Goddess: Waterbinder (7591) &amp; Judge of Splendor (21544) for Necklace of Goddess: Evergreen (7592), forms a Full Raid Party to contest Raid Boss Flame of Splendor Barakiel (25325) for Rain Song (7593), exchanges for Relic Box (7594) &amp; Caradine's Letter (7679).</li>
	 *   <li>Part 4 ({@code Q00247_PossessorOfAPreciousSoul4}, Subclass {@code Lv 75+}): Consumes Caradine's Letter (7679), travels to Lady of the Lake (31745) at Coliseum/Goddard -> Noblesse Tiara (7694), {@code setNoble(true)}, &amp; Blessing of Noblesse (1323).</li>
	 * </ul>
	 */
	private void tickNoblesseAndOlympiad()
	{
		// Part 1: Q00241_PossessorOfAPreciousSoul1 (Subclass Lv 50+)
		if (_noblesseQuestStage == 0)
		{
			final int qId = ProgressionKnowledgeGraph.getNoblesseQuestId(1);
			ensureServerQuestStarted(qId);
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
			if ((_questKillsCurrent == 3) && !Quest.hasQuestItems(_player, 7597))
			{
				Quest.giveItems(_player, 7597, 1); // Malruk Succubus Claw
				Quest.giveItems(_player, 7587, 1); // Legend of Seventeen
			}
			else if ((_questKillsCurrent == 7) && !Quest.hasQuestItems(_player, 7589))
			{
				Quest.giveItems(_player, 7589, 1); // Echo Crystal - Forgotten Song
			}
			if (_questKillsCurrent >= _questKillsTarget)
			{
				Quest.giveItems(_player, 7598, 5); // Crimson Moss
				Quest.takeItems(_player, -1, 7597, 7587, 7589, 7598);
				completeServerQuestStageWithNpc(qId, 7677, 1, 0L, 263_043L, 0, true); // Virgil's Letter
				_activeQuestId = 0;
				_noblesseQuestStage = 1;
				_noblesseStageInProgress = false;
				_macroState = CitizenMacroState.RETURN_TO_TOWN;
				_lastAction = "Completed Q241 Precious Soul Pt.1 -> Earned Virgil's Letter (7677)";
				return;
			}
			huntQuestMonstersInCurrentZone("Q241 Precious Soul Pt.1");
			return;
		}
		
		// Part 2: Q00242_PossessorOfAPreciousSoul2 (Subclass Lv 60+)
		if (_noblesseQuestStage == 1)
		{
			final int qId = ProgressionKnowledgeGraph.getNoblesseQuestId(2);
			ensureServerQuestStarted(qId);
			if (!_noblesseStageInProgress)
			{
				_noblesseStageInProgress = true;
				Quest.takeItems(_player, 7677, -1); // Virgil's Letter
				_questKillsCurrent = 0;
				_questKillsTarget = 12;
				_currentZone = ProgressionKnowledgeGraph.selectNoblesseQuestZone(2);
				_player.teleToLocation(_currentZone.huntingCenter(), true);
				_lastAction = "Q242 Precious Soul Pt.2 (SubLv" + _subClassLevel + "): Collecting 4 Orbs of Binding (7595) in Swamp of Screams";
				return;
			}
			if ((_questKillsCurrent > 0) && ((_questKillsCurrent % 3) == 0))
			{
				Quest.giveItems(_player, 7595, 1); // Orb of Binding
			}
			if (_questKillsCurrent >= _questKillsTarget)
			{
				Quest.takeItems(_player, 7595, -1);
				completeServerQuestStageWithNpc(qId, 7678, 1, 0L, 455_764L, 0, true); // Caradine's Letter
				_activeQuestId = 0;
				_noblesseQuestStage = 2;
				_noblesseStageInProgress = false;
				_macroState = CitizenMacroState.RETURN_TO_TOWN;
				_lastAction = "Completed Q242 Precious Soul Pt.2 -> Rescued Fallen Unicorn & Earned Caradine's Letter (7678)";
				return;
			}
			huntQuestMonstersInCurrentZone("Q242 Precious Soul Pt.2");
			return;
		}
		
		// Part 3: Q00246_PossessorOfAPreciousSoul3 (Subclass Lv 65+ & Raid Boss Flame of Splendor Barakiel 25325)
		if (_noblesseQuestStage == 2)
		{
			final int qId = ProgressionKnowledgeGraph.getNoblesseQuestId(3);
			ensureServerQuestStarted(qId);
			if (!_noblesseStageInProgress)
			{
				_noblesseStageInProgress = true;
				Quest.takeItems(_player, 7678, -1); // Caradine's Letter
				_questKillsCurrent = 0;
				_questKillsTarget = 15;
				AutonomousPlayerManager.getInstance().joinOrFormQuestRaidParty(this);
				_currentZone = ProgressionKnowledgeGraph.selectNoblesseQuestZone(3);
				_player.teleToLocation(_currentZone.huntingCenter(), true);
				_lastAction = "Q246 Precious Soul Pt.3 (SubLv" + _subClassLevel + "): Contesting Barakiel (25325) in Valley of Saints";
				return;
			}
			if ((_questKillsCurrent == 5) && !Quest.hasQuestItems(_player, 7591))
			{
				Quest.giveItems(_player, 7591, 1); // Ring of Goddess: Waterbinder
			}
			else if ((_questKillsCurrent == 10) && !Quest.hasQuestItems(_player, 7592))
			{
				Quest.giveItems(_player, 7592, 1); // Necklace of Goddess: Evergreen
			}
			if (_questKillsCurrent >= _questKillsTarget)
			{
				Quest.giveItems(_player, 7593, 1); // Rain Song (Barakiel's Staff)
				Quest.takeItems(_player, -1, 7591, 7592, 7593);
				Quest.giveItems(_player, 7594, 1); // Relic Box
				Quest.takeItems(_player, 7594, -1);
				completeServerQuestStageWithNpc(qId, 7679, 1, 0L, 719_843L, 0, true); // Caradine's Letter (Final)
				_activeQuestId = 0;
				_noblesseQuestStage = 3;
				_noblesseStageInProgress = false;
				_macroState = CitizenMacroState.RETURN_TO_TOWN;
				_lastAction = "Completed Q246 Precious Soul Pt.3 -> Earned Barakiel's Rain Song (7593) & Caradine's Letter (7679)";
				return;
			}
			huntQuestMonstersOrRaidBossInCurrentZone("Q246 Barakiel / Valley of Saints", 25325);
			return;
		}
		
		// Part 4: Q00247_PossessorOfAPreciousSoul4 (Subclass Lv 75+ Ordination with Lady of the Lake 31745)
		final int q247Id = ProgressionKnowledgeGraph.getNoblesseQuestId(4);
		ensureServerQuestStarted(q247Id);
		_currentZone = ProgressionKnowledgeGraph.selectNoblesseQuestZone(4);
		_player.teleToLocation(_currentZone.huntingCenter(), true);
		Quest.takeItems(_player, 7679, -1); // Caradine's Letter
		completeServerQuestStageWithNpc(q247Id, 7694, 1, 0L, 93_836L, 0, true); // Noblesse Tiara
		_activeQuestId = 0;
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
				notifyServerQuestKill(_activeQuestId, currentTarget);
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
				notifyServerQuestKill(_activeQuestId, currentTarget);
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
			notifyServerQuestKill(_activeQuestId, null);
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
	 * Deep 7-Archetype Combat Combo &amp; Tactical Doctrine Engine + UCB1 Contextual Toolkit Exploration:
	 * <ol>
	 *   <li><b>Dwarven Spoiler Pre-Hook:</b> Scavenger/Bounty Hunter/Fortune Seeker casts {@code Spoil (254)} via {@code useMagic}.</li>
	 *   <li><b>Doctrine 1 — Elemental Mage Combo:</b> {@code Surrender to Element / Gloom} -> {@code Elemental Vortex (1339-1343)} -> {@code Vortex Buster/Crusher/Slug (1451-1456)} -> UCB1 Primary Elemental Nuke.</li>
	 *   <li><b>Doctrine 2 — Archer Range-Preservation &amp; Auto-Attack Supremacy:</b> Stands at full {@code 850-1100} bow range, strictly discards any skill whose {@code castRange < distanceToTarget} (refusing to waste DPS running closer), and relies on Soulshot Critical Auto-Attacks + long-range {@code Lethal Shot (343)} / {@code Seven Arrow (924)}.</li>
	 *   <li><b>Doctrine 3 — Gladiator / Duelist &amp; Grand Khavatari Momentum Charge:</b> Builds charges via {@code Sonic Rage (345)} / {@code Raging Force (346)} / {@code Sonic Focus (8)}, releases high-multiplier finishers ({@code Triple Sonic Slash 261}, {@code Hurricane Assault 284}, {@code Double Sonic Slash 5}), and weaves Auto-Attacks when finishers are within {@code <= 2.5s} of cooldown.</li>
	 *   <li><b>Doctrine 4 — Warlord / Dreadnought Polearm Monster-Train &amp; Vampiric Cleave:</b> Pulls {@code 3-8} monsters into a stacked cluster, maintains {@code Fell Swoop (421)} / {@code Thrill Fight (130)}, locks down with {@code Shock Blast (361)} / {@code Thunder Storm (48)}, and prioritizes Polearm Auto-Attack Cleave when HP &lt; 85% to multiply Vampiric Rage life-steal across up to 12 targets.</li>
	 *   <li><b>Doctrine 5 — Rogue / Dagger Positional Flank + Treasure Chest Unlock Gambling:</b> Restocks Thief/Deluxe Chest Keys, opens World Treasure Chests ({@code 18265..18298}) with {@code Unlock (27)} / {@code Deluxe Key (2229)}, maintains {@code Focus Death/Chance/Power (355-357)}, flanks behind target via {@code Bluff (358)} / {@code Shadow Step (821)} / {@code Trick (11)} / {@code Switch (12)} / geometric back-step, and chains {@code Backstab (30)} -> {@code Lethal Blow (344)} -> {@code Critical/Blinding Blow (409/321)} -> {@code Deadly Blow (263)}.</li>
	 *   <li><b>Doctrine 6 — Tank Group Healer-Protector Taunt &amp; Solo Vanguard Stance:</b> In party/low-HP mode, scans party Healers/allies for aggro threats and fires {@code Aggression (28)} (Top of Aggro + N in PvE, 3s Forced Target-Lock in PvP) or {@code Aura of Hate (18)}, weaving {@code Tribunal (400)} / {@code Judgment (401)} / {@code Shield Slam (353)} / {@code Touch of Life (341)} / {@code Shield of Faith (529)}. In solo mode (HP &gt;= 55%), activates {@code Vanguard (810-813)} stance and {@code Boost Morale (956)} / {@code Blade Hurricane (814)}.</li>
	 *   <li><b>Doctrine 7 — Support, Healer &amp; Overlord:</b> Overlord/Dominator restores party CP ({@code Ritual of Life 1306}, {@code Honor of Pa'agrio 1305}), maintains {@code Victories of Pa'agrio (1414)} / {@code Pa'agrio's Fist (1416)}, and casts AoE Seals + {@code Steal Essence (1245)}. Cardinal/Eva's Saint/Shillien Saint triages HP ({@code Balance Life 1335}, {@code Major Heal 1401}), recharges MP ({@code Mana Gain 1460} + {@code Recharge 1013}), maintains 81-83 buffs ({@code Lord of Vampire 1507}, {@code Stigma of Shillien 1539}, {@code Prophecy of Water 1355}), and solo farms with {@code Inquisitor (1520)} / {@code Might of Heaven (1028)}. Hierophant/Doomcryer/Sword Muse/Spectral Dancer maintain {@code Counter Critical (1542)}, {@code Great Fury (1543)}, {@code Chant of Protection (1461)}, and Songs/Dances.</li>
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
		
		// Doctrine 7: Support, Healer & Overlord (Dominator, Cardinal, Eva's/Shillien Saint, Hierophant, Doomcryer, Sword Muse, Spectral Dancer)
		if (isSupportOrOverlordCitizen())
		{
			executeSupportAndOverlordDoctrine(target);
			return;
		}
		
		// Doctrine 1: Elemental Mage Combo (Surrender -> Vortex -> Vortex Slug/Buster -> UCB1 Elemental Nuke)
		if (isMage)
		{
			executeMageComboDoctrine(target);
			return;
		}
		
		// Doctrine 2: Archer Range-Preservation & Auto-Attack Supremacy
		if (isArcherCitizen())
		{
			executeArcherRangeDoctrine(target);
			return;
		}
		
		// Doctrine 3: Gladiator / Duelist & Tyrant / Grand Khavatari Momentum Charge-and-Release
		if (isMomentumFighterCitizen())
		{
			executeMomentumChargeDoctrine(target);
			return;
		}
		
		// Doctrine 4: Warlord / Dreadnought Polearm Monster-Train & Vampiric Cleave
		if (isPolearmTrainCitizen())
		{
			executeDreadnoughtTrainDoctrine(target);
			return;
		}
		
		// Doctrine 5: Rogue / Dagger Positional Flank + Treasure Chest Unlock Gambling
		if (isDaggerRogueCitizen())
		{
			executeDaggerRogueDoctrine(target);
			return;
		}
		
		// Doctrine 6: Tank Group Healer-Protector Taunt & Solo Vanguard Stance
		if (isTankCitizen())
		{
			executeTankVanguardAndTauntDoctrine(target);
			return;
		}
		
		// Fallback Melee / Kamael Tactical Rotation with UCB1 Toolkit Exploration
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
	
	/**
	 * Doctrine 5 — Rogue / Dagger Positional Flank + World Treasure Chest {@code Unlock (27)} Gambling:
	 * <ul>
	 *   <li>Scans {@code World.getVisibleObjectsInRange(_player, Chest.class, 900)} for unopened Treasure Chests within {@code |chest.getLevel() - _player.getLevel()| <= 6} and casts {@code Unlock (27)} or {@code Deluxe Chest Key (2229)}.</li>
	 *   <li>Maintains Thief sub-fantasy buffs: {@code Focus Death (355)} (AW/GH), {@code Focus Chance (356)} (PW/WR), {@code Focus Power (357)} (TH/Adv), {@code Mortal Strike (410)}, {@code Esprit (171)}.</li>
	 *   <li>Flanks behind the target via {@code Bluff (358)} / {@code Shadow Step (821)} (3rd Class) or {@code Switch (12)} / {@code Trick (11)} / {@code Veil (65)} / {@code Lure (51)} / geometric back-step (1st/2nd Class).</li>
	 *   <li>Applies Mana Preservation vs. Danger Triage and chains {@code Backstab (30)} -> {@code Lethal Blow (344)} -> {@code Critical Blow (409)} / {@code Blinding Blow (321)} -> {@code Deadly Blow (263)}.</li>
	 * </ul>
	 */
	private void executeDaggerRogueDoctrine(Creature target)
	{
		// 1. World Treasure Chest Gambling (Unlock 27 & Deluxe Chest Key 2229 on Chest 18265..18298, 21671..21822)
		Chest chestTarget = (target instanceof Chest c && !c.isDead() && !c.isInteracted()) ? c : null;
		if (chestTarget == null)
		{
			for (Chest chest : World.getVisibleObjectsInRange(_player, Chest.class, 900))
			{
				if ((chest != null) && !chest.isDead() && !chest.isInteracted() && (Math.abs(chest.getLevel() - _player.getLevel()) <= 6))
				{
					chestTarget = chest;
					break;
				}
			}
		}
		if (chestTarget != null)
		{
			_player.setTarget(chestTarget);
			if (_player.calculateDistance2D(chestTarget) > 40.0)
			{
				if (!_player.isMoving())
				{
					_player.getAI().setIntentionMoveTo(chestTarget);
				}
				_lastDecisionReason = "Dagger Treasure Gamble: Approaching " + chestTarget.getName() + " (Lv " + chestTarget.getLevel() + ")";
				_lastAction = _lastDecisionReason;
				return;
			}
			Skill unlockSkill = getReadySkill(27); // Unlock (27)
			if (unlockSkill == null)
			{
				final int keySkillLvl = Math.min(8, Math.max(1, (_player.getLevel() - 10) / 10));
				unlockSkill = SkillData.getInstance().getSkill(2229, keySkillLvl); // Deluxe Chest Key (2229)
			}
			if (unlockSkill != null)
			{
				castTacticalSkill(unlockSkill, chestTarget, true, "Dagger Treasure Gamble: Casting " + unlockSkill.getName() + " on " + chestTarget.getName());
				return;
			}
		}
		
		// 2. Maintain Thief Sub-Fantasy Self-Buffs: Focus Death (355), Focus Chance (356), Focus Power (357), Mortal Strike (410), Stealth/Esprit (171)
		for (int buffId : new int[] { 355, 356, 357, 410, 171 })
		{
			if (!_player.isAffectedBySkill(buffId))
			{
				final Skill buff = getReadySkill(buffId);
				if (buff != null)
				{
					castTacticalSkill(buff, _player, false, "Dagger Stance: Activating " + buff.getName());
					return;
				}
			}
		}
		
		// 3. Positional Back/Side Flank + Bluff (358) / Shadow Step (821) / Switch (12) / Trick (11) / Geometric Back-Step
		final boolean isBehindTarget = _player.isBehind(target);
		if (!isBehindTarget && (target.getTarget() == _player))
		{
			for (int flankSkillId : new int[] { 358, 821, 12, 11, 65, 51 })
			{
				final Skill flankSkill = getReadySkill(flankSkillId);
				if (flankSkill != null)
				{
					castTacticalSkill(flankSkill, target, true, "Dagger Flank Setup: Casting " + flankSkill.getName() + " to expose enemy back!");
					return;
				}
			}
			if ((ThreadLocalRandom.current().nextInt(100) < 40) && !_player.isMoving())
			{
				final double rad = Math.toRadians(LocationUtil.convertHeadingToDegree(target.getHeading()));
				final int backX = target.getX() - (int) Math.round(Math.cos(rad) * 45.0);
				final int backY = target.getY() - (int) Math.round(Math.sin(rad) * 45.0);
				final Location flankLoc = GeoEngine.getInstance().getValidLocation(_player.getX(), _player.getY(), _player.getZ(), backX, backY, target.getZ(), _player.getInstanceId());
				_player.getAI().setIntentionMoveTo(flankLoc);
				_lastDecisionReason = "Dagger Positional Flank: Stepping 45u behind " + target.getName() + " for Backstab";
				_lastAction = _lastDecisionReason;
				return;
			}
		}
		
		// 4. Mana Preservation vs. Danger Triage
		final double hpRatio = _player.getCurrentHp() / Math.max(1.0, _player.getMaxHp());
		final double mpRatio = _player.getCurrentMp() / Math.max(1.0, _player.getMaxMp());
		int nearbyMobs = 0;
		for (Monster m : World.getVisibleObjectsInRange(_player, Monster.class, 220))
		{
			if ((m != null) && !m.isAlikeDead())
			{
				nearbyMobs++;
			}
		}
		final boolean isMimicOrHighDanger = (target instanceof Chest) || (nearbyMobs >= 2) || (hpRatio <= 0.75);
		
		if (!isMimicOrHighDanger && (mpRatio < 0.50))
		{
			// Low Danger & Low MP: Conserve MP with fast Dagger Auto-Attacks, only firing Backstab (30) when strictly behind target!
			if (isBehindTarget)
			{
				final Skill backstab = getReadySkill(30);
				if (backstab != null)
				{
					castTacticalSkill(backstab, target, true, "Dagger MP-Conserve Backstab [Behind]: " + backstab.getName());
					return;
				}
			}
			_player.getAI().setIntentionAttack(target);
			_lastDecisionReason = "Dagger MP Preservation [HP " + (int) Math.round(hpRatio * 100) + "%, MP " + (int) Math.round(mpRatio * 100) + "%]: Fast Crit Auto-Attack";
			_lastAction = _lastDecisionReason;
			return;
		}
		
		// High Danger / Evasion Cooldowns: Exciting Adventure (768), Wind Riding (769), Ghost Walking (770), Ultimate Evasion (111), Mirage (430), Dodge (487)
		if (isMimicOrHighDanger)
		{
			for (int evaId : new int[] { 768, 769, 770, 111, 430, 487 })
			{
				if (!_player.isAffectedBySkill(evaId))
				{
					final Skill eva = getReadySkill(evaId);
					if (eva != null)
					{
						castTacticalSkill(eva, _player, false, "Dagger Evasion Triage: Popping " + eva.getName());
						return;
					}
				}
			}
		}
		
		// Apply Hex (122) / Bleed (96) / Sting (223) (Abyss Walker / Ghost Hunter)
		if ((target.getCurrentHp() > (target.getMaxHp() * 0.45)))
		{
			for (int debuffId : new int[] { 122, 96, 223 })
			{
				if (!target.isAffectedBySkill(debuffId))
				{
					final Skill debuff = getReadySkill(debuffId);
					if (debuff != null)
					{
						castTacticalSkill(debuff, target, true, "Dagger Opener Debuff: " + debuff.getName());
						return;
					}
				}
			}
		}
		
		// Unleash full Blow Chain: Backstab (30) [when behind/side] -> Lethal Blow (344) -> Critical Blow (409) / Blinding Blow (321) -> Deadly Blow (263) / Mortal Blow (16)
		if (isBehindTarget || !_player.isInFrontOf(target))
		{
			final Skill backstab = getReadySkill(30);
			if (backstab != null)
			{
				castTacticalSkill(backstab, target, true, "Dagger Blow Chain [Behind]: Backstab (30)");
				return;
			}
		}
		for (int blowId : new int[] { 344, 409, 321, 263, 16 })
		{
			final Skill blow = getReadySkill(blowId);
			if (blow != null)
			{
				castTacticalSkill(blow, target, true, "Dagger Blow Chain: " + blow.getName());
				return;
			}
		}
		
		_player.getAI().setIntentionAttack(target);
		_lastDecisionReason = "Dagger Fast Auto-Attack Weave";
		_lastAction = "Dagger Attacking " + target.getName();
	}
	
	/**
	 * Doctrine 6 — Tank Group Healer-Protector Taunt &amp; Solo Vanguard Stance ({@code Phoenix Knight}, {@code Hell Knight}, {@code Eva's Templar}, {@code Shillien Templar}):
	 * <ul>
	 *   <li><b>Emergency Survival (&lt; 40% HP):</b> Activates {@code Touch of Life (341)}, {@code Shield of Faith (529)}, {@code Vengeance (368)}, or {@code Ultimate Defense (110)}.</li>
	 *   <li><b>Group / Healer-Protector Mode ({@code isInParty()} or HP &lt; 50%):</b> Scans party members for attackers targeting Healers/allies and fires {@code Aura of Hate (18)} (AoE Taunt) or {@code Aggression (28)} (Top of Aggro + N in PvE, 3s Forced Target-Lock in PvP), maintaining {@code Guard Stance (288)} / {@code Aegis Stance (318)} / {@code Deflect Arrow (112)} / {@code Iron Shield (527)} and weaving {@code Tribunal (400)} / {@code Judgment (401)} / {@code Shield Slam (353)} / {@code Shield Bash (352)} / {@code Shackle (402)}.</li>
	 *   <li><b>Solo Vanguard Offensive Mode ({@code !isInParty()} and HP &gt;= 55%):</b> Activates Epilogue {@code Vanguard (810..813)} stance + {@code Boost Morale (956)} / {@code Seed of Revenge (761)} / {@code Spirit of Shillien (789)} and weaves {@code Blade Hurricane (814)} / {@code Blade Slash (815)} / {@code Double Strike (816)}.</li>
	 * </ul>
	 */
	private void executeTankVanguardAndTauntDoctrine(Creature target)
	{
		final double hpRatio = _player.getCurrentHp() / Math.max(1.0, _player.getMaxHp());
		
		// 1. Emergency Survival Cooldowns (< 40% HP): Touch of Life (341), Shield of Faith (529), Vengeance (368), Ultimate Defense (110)
		if (hpRatio < 0.40)
		{
			for (int defId : new int[] { 341, 529, 368, 110 })
			{
				if (!_player.isAffectedBySkill(defId))
				{
					final Skill defSkill = getReadySkill(defId);
					if (defSkill != null)
					{
						castTacticalSkill(defSkill, _player, false, "Tank Emergency Survival [HP " + (int) Math.round(hpRatio * 100) + "%]: " + defSkill.getName());
						return;
					}
				}
			}
		}
		
		// 2. Group / Healer-Protector Mode (_player.isInParty() or HP < 50%)
		if (_player.isInParty() || (hpRatio < 0.50))
		{
			// Maintain Tank Shield Stances & Buffs: Iron Shield (527), Deflect Arrow (112), Majesty (231), Guard Stance (288), Aegis Stance (318)
			for (int buffId : new int[] { 527, 112, 231, 288, 318 })
			{
				if (!_player.isAffectedBySkill(buffId))
				{
					final Skill buff = getReadySkill(buffId);
					if (buff != null)
					{
						castTacticalSkill(buff, _player, false, "Tank Protector Stance: " + buff.getName());
						return;
					}
				}
			}
			
			// Scan nearby hostiles threatening party allies (prioritizing Healers/Mages!)
			Creature looseAttacker = null;
			int looseCount = 0;
			final Party party = _player.getParty();
			for (Creature nearby : World.getVisibleObjectsInRange(_player, Creature.class, 600))
			{
				if ((nearby == null) || nearby.isAlikeDead() || (nearby == _player))
				{
					continue;
				}
				if (nearby.isAttackable())
				{
					final Attackable mob = nearby.asAttackable();
					final Creature mostHated = mob.getMostHated();
					if ((mostHated != null) && (mostHated != _player))
					{
						looseCount++;
						if (looseAttacker == null)
						{
							looseAttacker = mob;
						}
					}
				}
				else if ((party != null) && (nearby.getTarget() instanceof Player ally) && (ally != _player) && party.containsPlayer(ally) && nearby.isAutoAttackable(_player))
				{
					looseCount++;
					looseAttacker = nearby;
				}
			}
			
			if (looseCount >= 2)
			{
				final Skill auraOfHate = getReadySkill(18); // Aura of Hate (AoE Taunt: TopOfAggro+N in PvE, 3s Target-Lock in PvP)
				if (auraOfHate != null)
				{
					castTacticalSkill(auraOfHate, _player, false, "Tank Healer-Protector [" + looseCount + " attackers]: Casting Aura of Hate (18) AoE Taunt!");
					return;
				}
			}
			
			final Creature tauntTarget = (looseAttacker != null) ? looseAttacker : target;
			if ((tauntTarget != null) && ((tauntTarget.getTarget() != _player) || (tauntTarget.isAttackable() && (tauntTarget.asAttackable().getMostHated() != _player))))
			{
				final Skill aggression = getReadySkill(28); // Aggression (Single-Target Taunt: TopOfAggro+N in PvE, 3s Target-Lock in PvP)
				if (aggression != null)
				{
					_player.setTarget(tauntTarget);
					castTacticalSkill(aggression, tauntTarget, true, "Tank Healer-Protector: Taunting " + tauntTarget.getName() + " with Aggression (28) [TopAggro+N / 3s Lock]!");
					return;
				}
			}
			
			// Tank Offensive/Debuff Weave: Tribunal (400), Judgment (401), Shield Slam (353), Shield Bash (352), Shackle (402), Arrest (403)
			for (int tankSkillId : new int[] { 400, 401, 353, 352, 402, 403 })
			{
				final Skill tankSkill = getReadySkill(tankSkillId);
				if (tankSkill != null)
				{
					castTacticalSkill(tankSkill, target, true, "Tank Protector Strike: " + tankSkill.getName());
					return;
				}
			}
			
			_player.getAI().setIntentionAttack(target);
			_lastDecisionReason = "Tank Healer-Protector: Holding top aggro on " + target.getName();
			_lastAction = _lastDecisionReason;
			return;
		}
		
		// 3. Solo Vanguard Offensive Mode (!_player.isInParty() and HP >= 55%)
		for (int vanguardBuffId : new int[] { 810, 811, 812, 813, 956, 761, 789, 785, 787 })
		{
			if (!_player.isAffectedBySkill(vanguardBuffId))
			{
				final Skill vBuff = getReadySkill(vanguardBuffId);
				if (vBuff != null)
				{
					castTacticalSkill(vBuff, _player, false, "Tank Solo Vanguard: Activating " + vBuff.getName());
					return;
				}
			}
		}
		
		for (int vanguardStrikeId : new int[] { 814, 815, 816, 817, 400, 401, 70 })
		{
			final Skill vStrike = getReadySkill(vanguardStrikeId);
			if (vStrike != null)
			{
				castTacticalSkill(vStrike, target, true, "Tank Solo Vanguard Strike: " + vStrike.getName());
				return;
			}
		}
		
		_player.getAI().setIntentionAttack(target);
		_lastDecisionReason = "Tank Solo Vanguard: High-P.Atk Auto-Attack weave";
		_lastAction = "Vanguard Attacking " + target.getName();
	}
	
	/**
	 * Doctrine 7 — Support, Healer &amp; Overlord ({@code Dominator/Overlord}, {@code Cardinal/Bishop}, {@code Eva's Saint}, {@code Shillien Saint}, {@code Hierophant/Prophet}, {@code Doomcryer}, {@code Sword Muse}, {@code Spectral Dancer}):
	 * <ul>
	 *   <li><b>Overlord / Dominator:</b> Restores CP via {@code Honor of Pa'agrio (1305)} / {@code Ritual of Life (1306)} when CP &lt; 80%, maintains {@code Victories of Pa'agrio (1414)} / {@code Pa'agrio's Fist (1416)} / {@code Pa'agrio's Emblem (1415)} / {@code Eye of Pa'agrio (1364)} / {@code Soul of Pa'agrio (1365)} / {@code Soul Guard (1311)}, casts AoE Seals ({@code 1509, 1248, 1462, 1104, 1108, 1097}), and drains with {@code Steal Essence (1245)}.</li>
	 *   <li><b>Cardinal / Eva's Saint / Shillien Saint:</b> Triages HP ({@code Balance Life 1335}, {@code Major Group Heal 1402}, {@code Major Heal 1401}, {@code Greater Battle Heal 1217}) and {@code Cleanse (1409)}, acts as Elven/Dark-Elven Mana Battery ({@code Mana Gain 1460} + {@code Recharge 1013} when ally MP &lt; 65%), maintains exclusive 81-83 buffs/debuffs ({@code Lord of Vampire 1507}, {@code Stigma of Shillien 1539}, {@code Prophecy of Water 1355}, {@code Divine Protection 1353}, {@code Celestial Shield 1418}), and solo farms with {@code Inquisitor (1520)} ({@code Divine Punishment 1523}, {@code Divine Flash 1528}) or {@code Might of Heaven (1028)}.</li>
	 *   <li><b>Hierophant / Doomcryer / Sword Muse / Spectral Dancer:</b> Maintains exclusive 79-83 buffs ({@code Counter Critical 1542}, {@code Great Fury 1543}, {@code Chant of Protection 1461}, {@code Prophecy of Fire 1356}, {@code Prophecy of Wind 1357}, {@code Chant of Victory 1363}), Songs ({@code 349, 364, 764, 269, 264}), and Dances ({@code 915, 365, 367, 275, 274}).</li>
	 * </ul>
	 */
	private void executeSupportAndOverlordDoctrine(Creature target)
	{
		// 1. Overlord / Dominator Branch
		if (isOverlordCitizen())
		{
			// Exclusive CP Restoration: Scan self and nearby party/clan allies for CP < 80%
			Player lowCpAlly = (_player.getCurrentCp() < (_player.getMaxCp() * 0.80)) ? _player : null;
			if (lowCpAlly == null)
			{
				for (Player ally : World.getVisibleObjectsInRange(_player, Player.class, 600))
				{
					if ((ally != null) && !ally.isAlikeDead() && !ally.isAutoAttackable(_player) && (ally.getCurrentCp() < (ally.getMaxCp() * 0.80)))
					{
						lowCpAlly = ally;
						break;
					}
				}
			}
			if (lowCpAlly != null)
			{
				for (int cpHealId : new int[] { 1305, 1306 }) // Honor of Pa'agrio (1305 AoE CP), Ritual of Life (1306 Single CP)
				{
					final Skill cpHeal = getReadySkill(cpHealId);
					if (cpHeal != null)
					{
						castTacticalSkill(cpHeal, (cpHealId == 1306) ? lowCpAlly : _player, false, "Overlord CP Restore: " + cpHeal.getName() + " -> " + lowCpAlly.getName());
						return;
					}
				}
			}
			
			// HP Restoration: Heart of Pa'agrio (1256) when HP < 70%
			if (_player.getCurrentHp() < (_player.getMaxHp() * 0.70))
			{
				final Skill heart = getReadySkill(1256);
				if (heart != null)
				{
					castTacticalSkill(heart, _player, false, "Overlord Sustain: Heart of Pa'agrio (1256)");
					return;
				}
			}
			
			// Endgame Pa'agrio Buffs (Lv 76-85): Victories of Pa'agrio (1414), Pa'agrio's Fist (1416), Pa'agrio's Emblem (1415), Eye of Pa'agrio (1364), Soul of Pa'agrio (1365), Soul Guard (1311)
			for (int paagrioBuffId : new int[] { 1414, 1416, 1415, 1364, 1365, 1311, 1260, 1282 })
			{
				if (!_player.isAffectedBySkill(paagrioBuffId))
				{
					final Skill pBuff = getReadySkill(paagrioBuffId);
					if (pBuff != null)
					{
						castTacticalSkill(pBuff, _player, false, "Overlord Pa'agrio Buff: " + pBuff.getName());
						return;
					}
				}
			}
			
			// AoE Seals & Debuffs: Seal of Limit (1509), Seal of Suspension (1248), Seal of Blockade (1462), Seal of Winter (1104), Seal of Flame (1108), Seal of Gloom (1097)
			if (target.getCurrentHp() > (target.getMaxHp() * 0.50))
			{
				for (int sealId : new int[] { 1509, 1248, 1462, 1104, 1108, 1097 })
				{
					if (!target.isAffectedBySkill(sealId))
					{
						final Skill seal = getReadySkill(sealId);
						if (seal != null)
						{
							castTacticalSkill(seal, target, true, "Overlord AoE Seal: " + seal.getName());
							return;
						}
					}
				}
			}
			
			// Primary Overlord Life-Drain Combat: Steal Essence (1245) / Soul Chill (1090)
			for (int drainId : new int[] { 1245, 1090 })
			{
				final Skill drain = getReadySkill(drainId);
				if (drain != null)
				{
					castTacticalSkill(drain, target, true, "Overlord Life-Drain: " + drain.getName());
					return;
				}
			}
			
			_player.getAI().setIntentionAttack(target);
			_lastDecisionReason = "Overlord Melee/Staff Weave";
			_lastAction = "Overlord Attacking " + target.getName();
			return;
		}
		
		// 2. HP Triage for Cardinal / Eva's Saint / Shillien Saint & 1st/2nd Class Healers (when self or party ally HP < 70%)
		Player injuredAlly = (_player.getCurrentHp() < (_player.getMaxHp() * 0.70)) ? _player : null;
		if ((injuredAlly == null) && _player.isInParty())
		{
			for (Player member : _player.getParty().getMembers())
			{
				if ((member != null) && !member.isAlikeDead() && (member.getCurrentHp() < (member.getMaxHp() * 0.70)) && (_player.calculateDistance2D(member) <= 600))
				{
					injuredAlly = member;
					break;
				}
			}
		}
		if (injuredAlly != null)
		{
			for (int healId : new int[] { 1335, 1402, 1401, 1217, 1218, 1015, 1011 })
			{
				final Skill heal = getReadySkill(healId);
				if (heal != null)
				{
					castTacticalSkill(heal, injuredAlly, false, "Healer Triage: Casting " + heal.getName() + " on " + injuredAlly.getName());
					return;
				}
			}
		}
		
		// Cleanse (1409) on debuffed self or party ally
		if (_player.getEffectList().hasDebuffs())
		{
			final Skill cleanse = getReadySkill(1409);
			if (cleanse != null)
			{
				castTacticalSkill(cleanse, _player, false, "Cardinal Triage: Cleanse (1409)");
				return;
			}
		}
		
		// 3. Elven & Dark Elven Mana Battery (Eva's Saint, Shillien Saint, Elder, Shillien Elder): Mana Gain (1460) + Recharge (1013) on ally with MP < 65%
		if (_player.isInParty())
		{
			for (Player member : _player.getParty().getMembers())
			{
				if ((member != null) && (member != _player) && !member.isAlikeDead() && (member.getCurrentMp() < (member.getMaxMp() * 0.65)) && (_player.calculateDistance2D(member) <= 600))
				{
					if (!member.isAffectedBySkill(1460))
					{
						final Skill manaGain = getReadySkill(1460);
						if (manaGain != null)
						{
							castTacticalSkill(manaGain, member, false, "Saint Mana Battery: Mana Gain (1460) -> " + member.getName());
							return;
						}
					}
					final Skill recharge = getReadySkill(1013);
					if (recharge != null)
					{
						castTacticalSkill(recharge, member, false, "Saint Mana Battery: Recharge (1013) -> " + member.getName());
						return;
					}
				}
			}
		}
		
		// 4. Exclusive 79-83 Endgame Buffs, Songs & Dances (Not in Town Buffer)
		final int[] exclusiveSupportBuffs =
		{
			1507, // Lord of Vampire (Shillien Saint)
			1355, // Prophecy of Water (Eva's Saint)
			1353, // Divine Protection (Cardinal)
			1418, // Celestial Shield (Cardinal)
			1542, // Counter Critical (Hierophant)
			1543, // Great Fury (Doomcryer)
			1461, // Chant of Protection
			1356, // Prophecy of Fire (Hierophant)
			1357, // Prophecy of Wind (Shillien Saint)
			1363, // Chant of Victory (Doomcryer)
			1413, // Magnus' Chant (Doomcryer)
			349, // Song of Renewal (Sword Muse)
			364, // Song of Champion (Sword Muse)
			764, // Song of Wind Storm (Sword Muse)
			269, // Song of Hunter (Sword Muse)
			264, // Song of Earth (Sword Muse)
			915, // Dance of Berserker (Spectral Dancer)
			365, // Dance of Siren (Spectral Dancer)
			275, // Dance of Fury (Spectral Dancer)
			274 // Dance of Fire (Spectral Dancer)
		};
		for (int buffId : exclusiveSupportBuffs)
		{
			if ((buffId == 1418) && (_player.getCurrentHp() > (_player.getMaxHp() * 0.45)))
			{
				continue; // Save Celestial Shield for low HP
			}
			if (!_player.isAffectedBySkill(buffId))
			{
				final Skill sBuff = getReadySkill(buffId);
				if (sBuff != null)
				{
					castTacticalSkill(sBuff, _player, false, "Endgame Support Buff: " + sBuff.getName());
					return;
				}
			}
		}
		
		// 5. Stigma of Shillien (1539) (-40 Melee Weapon Resistance on target!) & Dance of Medusa (367)
		if (!target.isAffectedBySkill(1539))
		{
			final Skill stigma = getReadySkill(1539);
			if (stigma != null)
			{
				castTacticalSkill(stigma, target, true, "Shillien Saint Debuff: Stigma of Shillien (1539)");
				return;
			}
		}
		
		// 6. Solo Farm / Inquisitor (1520, 1521, 1522 -> Divine Punishment 1523, Divine Flash 1528, Surrender to the Holy 1524) & Might of Heaven (1028)
		for (int inqStanceId : new int[] { 1520, 1521, 1522 })
		{
			if (!_player.isAffectedBySkill(inqStanceId) && !_player.isInParty())
			{
				final Skill inq = getReadySkill(inqStanceId);
				if (inq != null)
				{
					castTacticalSkill(inq, _player, false, "Healer Solo Stance: Activating " + inq.getName());
					return;
				}
			}
		}
		for (int offensiveId : new int[] { 1524, 1523, 1528, 1028, 1031, 1245, 401, 366, 1177 })
		{
			if ((offensiveId == 1524) && target.isAffectedBySkill(1524))
			{
				continue;
			}
			final Skill atkSkill = getReadySkill(offensiveId);
			if (atkSkill != null)
			{
				castTacticalSkill(atkSkill, target, true, "Support Offensive Weave: " + atkSkill.getName());
				return;
			}
		}
		
		_player.getAI().setIntentionAttack(target);
		_lastDecisionReason = "Support Auto-Attack Weave";
		_lastAction = "Support Attacking " + target.getName();
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
				if ((_subClassLevel == 65) && !Quest.hasQuestItems(_player, 10280))
				{
					Quest.giveItems(_player, 10280, 1); // Certificate - Emergent Ability
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
		if (creature instanceof Chest chest)
		{
			if (!chest.isInteracted() && !isDaggerRogueCitizen())
			{
				return false;
			}
			if (!chest.isInteracted() && (Math.abs(chest.getLevel() - _player.getLevel()) > 6))
			{
				return false;
			}
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
		
		// Ensure consumables (Soulshots, Spiritshots, Arrows, Healing Potions, Mana Potions 728, Escape Scrolls, and Thief/Deluxe Chest Keys for Daggers)
		ensureInventoryStock(loadout.soulshotId(), 2000, isInitialSpawn);
		ensureInventoryStock(loadout.spiritshotId(), 2000, isInitialSpawn);
		if (loadout.arrowId() > 0)
		{
			ensureInventoryStock(loadout.arrowId(), 2000, isInitialSpawn);
		}
		if (isDaggerRogueCitizen())
		{
			ensureInventoryStock(1661, 100, isInitialSpawn); // Thief Key (for Unlock 27)
			final int deluxeKeyId = 6665 + Math.min(7, Math.max(0, (_player.getLevel() - 20) / 10)); // Deluxe Chest Key Grade 1..8 (6665..6672)
			ensureInventoryStock(deluxeKeyId, 30, isInitialSpawn);
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
