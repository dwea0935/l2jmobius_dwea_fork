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
package org.l2jmobius.gameserver.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.FakePlayerData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.holders.npc.FakePlayerHolder;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Java 27 Tactical Utility-AI & Squad Synergy Engine for FakePlayers.
 * Executes a 250ms high-frequency micro-decision loop supporting:
 * <ul>
 *   <li>Archetype classification ({@link Archetype}: ARCHER, MAGE, DAGGER, HEALER, TANK, WARRIOR)</li>
 *   <li>Dynamic tactical postures ({@link Posture}: AGGRESSIVE, KITE_DEFENSIVE, ASSIST_PLAYER, GUARD)</li>
 *   <li>Squad follow formation & real-time target assist for any Player/GM</li>
 *   <li>Healer battle-heal triage, Tank hate-intercept, Archer/Mage GeoEngine standoff kiting, Dagger backstab flanking, and Second-Wind potion recovery</li>
 * </ul>
 */
public class FakePlayerAiEngine
{
	private static final Logger LOGGER = Logger.getLogger(FakePlayerAiEngine.class.getName());
	private static final long TICK_INTERVAL_MS = 250L;
	
	public enum Posture
	{
		AGGRESSIVE,
		KITE_DEFENSIVE,
		ASSIST_PLAYER,
		GUARD
	}
	
	public enum Archetype
	{
		ARCHER,
		MAGE,
		DAGGER,
		HEALER,
		TANK,
		WARRIOR
	}
	
	public static final class BotState
	{
		private final int _objectId;
		private final int _npcId;
		private final String _name;
		private final Archetype _archetype;
		private volatile Posture _posture;
		private volatile int _assistPlayerObjectId;
		private volatile int _squadSlot;
		private volatile long _lastHealSkillMs;
		private volatile long _lastPotionMs;
		private volatile long _lastKiteMs;
		private volatile long _lastSpecialSkillMs;
		private volatile String _lastTacticalAction = "Idle";
		
		public BotState(Npc npc, Archetype archetype, Posture initialPosture)
		{
			_objectId = npc.getObjectId();
			_npcId = npc.getId();
			_name = npc.getName();
			_archetype = archetype;
			_posture = initialPosture;
		}
		
		public int getObjectId()
		{
			return _objectId;
		}
		
		public int getNpcId()
		{
			return _npcId;
		}
		
		public String getName()
		{
			return _name;
		}
		
		public Archetype getArchetype()
		{
			return _archetype;
		}
		
		public Posture getPosture()
		{
			return _posture;
		}
		
		public void setPosture(Posture posture)
		{
			_posture = posture;
		}
		
		public int getAssistPlayerObjectId()
		{
			return _assistPlayerObjectId;
		}
		
		public void setAssistPlayerObjectId(int assistPlayerObjectId)
		{
			_assistPlayerObjectId = assistPlayerObjectId;
		}
		
		public int getSquadSlot()
		{
			return _squadSlot;
		}
		
		public void setSquadSlot(int squadSlot)
		{
			_squadSlot = squadSlot;
		}
		
		public long getLastHealSkillMs()
		{
			return _lastHealSkillMs;
		}
		
		public void setLastHealSkillMs(long lastHealSkillMs)
		{
			_lastHealSkillMs = lastHealSkillMs;
		}
		
		public long getLastPotionMs()
		{
			return _lastPotionMs;
		}
		
		public void setLastPotionMs(long lastPotionMs)
		{
			_lastPotionMs = lastPotionMs;
		}
		
		public long getLastKiteMs()
		{
			return _lastKiteMs;
		}
		
		public void setLastKiteMs(long lastKiteMs)
		{
			_lastKiteMs = lastKiteMs;
		}
		
		public long getLastSpecialSkillMs()
		{
			return _lastSpecialSkillMs;
		}
		
		public void setLastSpecialSkillMs(long lastSpecialSkillMs)
		{
			_lastSpecialSkillMs = lastSpecialSkillMs;
		}
		
		public String getLastTacticalAction()
		{
			return _lastTacticalAction;
		}
		
		public void setLastTacticalAction(String action)
		{
			_lastTacticalAction = action;
		}
	}
	
	private final Map<Integer, BotState> _botStates = new ConcurrentHashMap<>();
	private final Map<Integer, Npc> _trackedNpcs = new ConcurrentHashMap<>();
	private final AtomicInteger _squadSlotCounter = new AtomicInteger(0);
	private ScheduledFuture<?> _tickTask;
	
	protected FakePlayerAiEngine()
	{
		start();
	}
	
	public synchronized void start()
	{
		if (_tickTask != null)
		{
			_tickTask.cancel(false);
			_tickTask = null;
		}
		
		if (FakePlayersConfig.FAKE_PLAYERS_ENABLED)
		{
			_tickTask = ThreadPool.scheduleAtFixedRate(this::tick, 1000L, TICK_INTERVAL_MS);
			LOGGER.info(getClass().getSimpleName() + ": Tactical Utility-AI loop active (250ms tick, LLM bridge=" + FakePlayersConfig.FAKE_PLAYER_LLM_ENABLED + ").");
		}
	}
	
	/**
	 * Classifies a FakePlayer NPC into its combat archetype using Java 27 switch expressions on {@link PlayerClass}.
	 * @param npc the FakePlayer NPC
	 * @return the tactical {@link Archetype}
	 */
	public static Archetype classifyArchetype(Npc npc)
	{
		if ((npc == null) || (npc.getTemplate() == null))
		{
			return Archetype.WARRIOR;
		}
		
		final FakePlayerHolder fpInfo = npc.getTemplate().getFakePlayerInfo();
		final PlayerClass pc = (fpInfo != null) ? fpInfo.getPlayerClass() : null;
		if (pc == null)
		{
			return Archetype.WARRIOR;
		}
		
		return switch (pc)
		{
			case HAWKEYE, SILVER_RANGER, PHANTOM_RANGER, SAGITTARIUS, MOONLIGHT_SENTINEL, GHOST_SENTINEL, ARBALESTER, TRICKSTER -> Archetype.ARCHER;
			case BISHOP, CARDINAL, ELDER, EVA_SAINT, SHILLIEN_ELDER, SHILLIEN_SAINT, PROPHET, HIEROPHANT, CLERIC, ORACLE, SHILLIEN_ORACLE -> Archetype.HEALER;
			case PALADIN, PHOENIX_KNIGHT, DARK_AVENGER, HELL_KNIGHT, TEMPLE_KNIGHT, EVA_TEMPLAR, SHILLIEN_KNIGHT, SHILLIEN_TEMPLAR, KNIGHT, ELVEN_KNIGHT, PALUS_KNIGHT -> Archetype.TANK;
			case TREASURE_HUNTER, ADVENTURER, PLAINS_WALKER, WIND_RIDER, ABYSS_WALKER, GHOST_HUNTER, ROGUE, ELVEN_SCOUT, ASSASSIN -> Archetype.DAGGER;
			default -> pc.isMage() ? Archetype.MAGE : Archetype.WARRIOR;
		};
	}
	
	public BotState getOrCreateState(Npc npc)
	{
		_trackedNpcs.put(npc.getObjectId(), npc);
		return _botStates.computeIfAbsent(npc.getObjectId(), _ ->
		{
			final Archetype arch = classifyArchetype(npc);
			final Posture defaultPosture = !npc.isAttackable()
				? Posture.GUARD
				: ((arch == Archetype.ARCHER) || (arch == Archetype.MAGE)) ? Posture.KITE_DEFENSIVE : Posture.AGGRESSIVE;
			return new BotState(npc, arch, defaultPosture);
		});
	}
	
	public BotState getState(int objectId)
	{
		return _botStates.get(objectId);
	}
	
	public void registerBot(Npc npc)
	{
		if ((npc != null) && npc.isFakePlayer())
		{
			getOrCreateState(npc);
		}
	}
	
	/**
	 * Discovers all live spawned FakePlayer NPCs in the world across SpawnTable and dynamically spawned bots.
	 * @return list of active FakePlayer NPCs
	 */
	public List<Npc> getActiveFakePlayers()
	{
		for (int npcId : FakePlayerData.getInstance().getFakePlayerIds().values())
		{
			if (npcId <= 0)
			{
				continue;
			}
			final Set<Spawn> spawns = SpawnTable.getInstance().getSpawns(npcId);
			for (Spawn spawn : spawns)
			{
				if (spawn == null)
				{
					continue;
				}
				for (Npc npc : spawn.getSpawnedNpcs())
				{
					if ((npc != null) && npc.isSpawned() && !npc.isDead())
					{
						getOrCreateState(npc);
					}
				}
			}
		}
		
		final List<Npc> active = new ArrayList<>(_trackedNpcs.size());
		final var iter = _trackedNpcs.entrySet().iterator();
		while (iter.hasNext())
		{
			final var entry = iter.next();
			final Npc npc = entry.getValue();
			if ((npc == null) || !npc.isSpawned())
			{
				iter.remove();
				_botStates.remove(entry.getKey());
			}
			else
			{
				active.add(npc);
			}
		}
		return active;
	}
	
	public Npc findActiveFakePlayerByName(String name)
	{
		if ((name == null) || name.isBlank())
		{
			return null;
		}
		for (Npc npc : getActiveFakePlayers())
		{
			if (npc.getName().equalsIgnoreCase(name.trim()))
			{
				return npc;
			}
		}
		return null;
	}
	
	public void assignAssistPlayer(Npc npc, Player leader)
	{
		if ((npc == null) || (leader == null))
		{
			return;
		}
		final BotState state = getOrCreateState(npc);
		state.setAssistPlayerObjectId(leader.getObjectId());
		state.setPosture(Posture.ASSIST_PLAYER);
		state.setSquadSlot(_squadSlotCounter.getAndIncrement() % 6);
		state.setLastTacticalAction("Assisting " + leader.getName());
	}
	
	public void setPosture(Npc npc, Posture posture)
	{
		if ((npc == null) || (posture == null))
		{
			return;
		}
		final BotState state = getOrCreateState(npc);
		state.setPosture(posture);
		if (posture != Posture.ASSIST_PLAYER)
		{
			state.setAssistPlayerObjectId(0);
		}
		state.setLastTacticalAction("Posture -> " + posture.name());
	}
	
	private void tick()
	{
		if (!FakePlayersConfig.FAKE_PLAYERS_ENABLED || !FakePlayersConfig.FAKE_PLAYER_TACTICAL_AI)
		{
			return;
		}
		
		try
		{
			final long now = System.currentTimeMillis();
			final List<Npc> bots = getActiveFakePlayers();
			for (Npc npc : bots)
			{
				if ((npc == null) || npc.isDead() || !npc.isSpawned() || npc.isCastingNow() || npc.isMovementDisabled())
				{
					continue;
				}
				
				final BotState state = getOrCreateState(npc);
				evaluateBotTactics(npc, state, bots, now);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Exception in tactical AI tick: " + e.getMessage(), e);
		}
	}
	
	private void evaluateBotTactics(Npc npc, BotState state, Collection<Npc> allBots, long now)
	{
		// 1. Self-Preservation Second Wind (All Archetypes)
		if (npc.isInCombat() && (npc.getCurrentHp() < (npc.getMaxHp() * 0.35)) && ((now - state.getLastPotionMs()) >= 15000L))
		{
			state.setLastPotionMs(now);
			npc.broadcastPacket(new MagicSkillUse(npc, npc, 2037, 1, 100, 1000));
			npc.setCurrentHp(Math.min(npc.getMaxHp(), npc.getCurrentHp() + (npc.getMaxHp() * 0.30)));
			state.setLastTacticalAction("Second-Wind Potion (+30% HP)");
		}
		
		// 2. Healer Triage (HEALER archetype or ASSIST_PLAYER posture)
		if (((state.getArchetype() == Archetype.HEALER) || (state.getPosture() == Posture.ASSIST_PLAYER)) && ((now - state.getLastHealSkillMs()) >= 4000L))
		{
			Creature triageTarget = null;
			final int assistObjId = state.getAssistPlayerObjectId();
			if (assistObjId > 0)
			{
				final Player leader = World.getPlayer(assistObjId);
				if ((leader != null) && leader.isOnline() && !leader.isDead() && (npc.getInstanceId() == leader.getInstanceId()) && (npc.calculateDistance2D(leader) <= 750) && (leader.getCurrentHp() < (leader.getMaxHp() * 0.65)))
				{
					triageTarget = leader;
				}
			}
			
			if ((triageTarget == null) && (state.getArchetype() == Archetype.HEALER))
			{
				if (npc.getCurrentHp() < (npc.getMaxHp() * 0.65))
				{
					triageTarget = npc;
				}
				else
				{
					for (Npc ally : allBots)
					{
						if ((ally != null) && !ally.isDead() && (ally.getInstanceId() == npc.getInstanceId()) && (npc.calculateDistance2D(ally) <= 700) && (ally.getCurrentHp() < (ally.getMaxHp() * 0.65)))
						{
							triageTarget = ally;
							break;
						}
					}
				}
			}
			
			if (triageTarget != null)
			{
				state.setLastHealSkillMs(now);
				npc.broadcastPacket(new MagicSkillUse(npc, triageTarget, 1218, 1, 800, 1000));
				triageTarget.setCurrentHp(Math.min(triageTarget.getMaxHp(), triageTarget.getCurrentHp() + (triageTarget.getMaxHp() * 0.22)));
				state.setLastTacticalAction("Battle Heal -> " + triageTarget.getName());
				return;
			}
		}
		
		// 3. Squad Follow & Assist (Posture.ASSIST_PLAYER)
		if (state.getPosture() == Posture.ASSIST_PLAYER)
		{
			final Player leader = World.getPlayer(state.getAssistPlayerObjectId());
			if ((leader == null) || !leader.isOnline())
			{
				state.setPosture(Posture.AGGRESSIVE);
				state.setAssistPlayerObjectId(0);
				state.setLastTacticalAction("Leader offline -> AGGRESSIVE");
			}
			else if (!leader.isDead() && (npc.getInstanceId() == leader.getInstanceId()))
			{
				final Creature enemy = findLeaderCombatTarget(npc, leader);
				if ((enemy != null) && !enemy.isDead() && npc.isAttackable())
				{
					engageTarget(npc, enemy);
					state.setLastTacticalAction("Assisting vs " + enemy.getName());
					
					// Tank Intercept if enemy is focusing the assisted player
					if ((state.getArchetype() == Archetype.TANK) && ((now - state.getLastSpecialSkillMs()) >= 5000L) && (enemy.getTarget() == leader) && (npc.calculateDistance2D(enemy) <= 500))
					{
						state.setLastSpecialSkillMs(now);
						npc.broadcastPacket(new MagicSkillUse(npc, enemy, 28, 1, 500, 2000));
						if (enemy.isAttackable())
						{
							enemy.asAttackable().addDamageHate(npc, 0, 5000);
							enemy.setTarget(npc);
						}
						state.setLastTacticalAction("Aggro Intercept -> " + enemy.getName());
					}
				}
				else if (!npc.isInCombat())
				{
					final double dist = npc.calculateDistance2D(leader);
					if (dist > 2200)
					{
						npc.teleToLocation(leader.getX() + 60, leader.getY() + 60, leader.getZ(), leader.getHeading(), leader.getInstanceId());
						state.setLastTacticalAction("Squad Warp -> " + leader.getName());
						return;
					}
					else if (dist > 155)
					{
						final double angleRad = (state.getSquadSlot() * 2.0 * Math.PI) / 6.0;
						final int formX = leader.getX() + (int) Math.round(Math.cos(angleRad) * 95.0);
						final int formY = leader.getY() + (int) Math.round(Math.sin(angleRad) * 95.0);
						final int formZ = leader.getZ();
						if (GeoEngine.getInstance().canMoveToTarget(npc.getX(), npc.getY(), npc.getZ(), formX, formY, formZ, npc.getInstanceId()))
						{
							npc.setRunning();
							npc.getAI().setIntentionMoveTo(new Location(formX, formY, formZ));
							state.setLastTacticalAction("Following " + leader.getName());
						}
						return;
					}
				}
			}
		}
		
		// 4. GUARD Posture: hold position unless directly attacked
		if ((state.getPosture() == Posture.GUARD) && !npc.isInCombat())
		{
			return;
		}
		
		// Resolve active combat target for micro-tactics
		final Creature currentTarget = resolveCurrentCombatTarget(npc);
		if ((currentTarget == null) || currentTarget.isDead())
		{
			return;
		}
		
		final double targetDist = npc.calculateDistance2D(currentTarget);
		
		// 5. Tank Hate Intercept for low-HP allies in general combat
		if ((state.getArchetype() == Archetype.TANK) && ((now - state.getLastSpecialSkillMs()) >= 6000L) && (targetDist <= 500))
		{
			final WorldObject enemyTarget = currentTarget.getTarget();
			if ((enemyTarget instanceof Creature ally) && (ally != npc) && (ally.getCurrentHp() < (ally.getMaxHp() * 0.65)))
			{
				state.setLastSpecialSkillMs(now);
				npc.broadcastPacket(new MagicSkillUse(npc, currentTarget, 28, 1, 500, 2000));
				if (currentTarget.isAttackable())
				{
					currentTarget.asAttackable().addDamageHate(npc, 0, 4000);
					currentTarget.setTarget(npc);
				}
				state.setLastTacticalAction("Tank Taunt -> " + currentTarget.getName());
				return;
			}
		}
		
		// 6. Archer / Mage / KITE_DEFENSIVE Standoff Kiting
		if (((state.getArchetype() == Archetype.ARCHER) || (state.getArchetype() == Archetype.MAGE) || (state.getPosture() == Posture.KITE_DEFENSIVE))
			&& (targetDist < 175) && ((now - state.getLastKiteMs()) >= 2500L))
		{
			double dx = npc.getX() - currentTarget.getX();
			double dy = npc.getY() - currentTarget.getY();
			final double len = Math.hypot(dx, dy);
			if (len < 1.0)
			{
				dx = 1.0;
				dy = 0.0;
			}
			else
			{
				dx /= len;
				dy /= len;
			}
			
			final int kiteX = npc.getX() + (int) Math.round(dx * 300.0);
			final int kiteY = npc.getY() + (int) Math.round(dy * 300.0);
			final int kiteZ = npc.getZ();
			if (GeoEngine.getInstance().canMoveToTarget(npc.getX(), npc.getY(), npc.getZ(), kiteX, kiteY, kiteZ, npc.getInstanceId()))
			{
				state.setLastKiteMs(now);
				npc.setRunning();
				npc.getAI().setIntentionMoveTo(new Location(kiteX, kiteY, kiteZ));
				state.setLastTacticalAction("Kiting away from " + currentTarget.getName());
				ThreadPool.schedule(() ->
				{
					if (!npc.isDead() && !currentTarget.isDead() && npc.isSpawned())
					{
						engageTarget(npc, currentTarget);
					}
				}, 750L);
				return;
			}
		}
		
		// 7. Dagger Backstab Flanking
		if ((state.getArchetype() == Archetype.DAGGER) && (targetDist <= 140) && ((now - state.getLastSpecialSkillMs()) >= 4500L))
		{
			final double targetHeadingRad = Math.toRadians(LocationUtil.convertHeadingToDegree(currentTarget.getHeading()));
			final int behindX = currentTarget.getX() - (int) Math.round(Math.cos(targetHeadingRad) * 55.0);
			final int behindY = currentTarget.getY() - (int) Math.round(Math.sin(targetHeadingRad) * 55.0);
			final int behindZ = currentTarget.getZ();
			if (GeoEngine.getInstance().canMoveToTarget(npc.getX(), npc.getY(), npc.getZ(), behindX, behindY, behindZ, npc.getInstanceId()))
			{
				state.setLastSpecialSkillMs(now);
				npc.setXYZ(behindX, behindY, behindZ);
				npc.setHeading(LocationUtil.calculateHeadingFrom(behindX, behindY, currentTarget.getX(), currentTarget.getY()));
				npc.broadcastPacket(new MagicSkillUse(npc, currentTarget, 263, 1, 700, 1500));
				final double blowDmg = Math.max(150.0, npc.getPAtk(currentTarget) * 0.45);
				currentTarget.reduceCurrentHp(blowDmg, npc, null);
				state.setLastTacticalAction("Backstab Flank -> " + currentTarget.getName());
			}
		}
	}
	
	private Creature findLeaderCombatTarget(Npc npc, Player leader)
	{
		final WorldObject leaderTarget = leader.getTarget();
		if ((leaderTarget instanceof Creature c) && (c != leader) && (c != npc) && !c.isDead()
			&& (leader.isInCombat() || c.isMonster() || c.isAutoAttackable(leader)))
		{
			if (!c.isFakePlayer() || FakePlayersConfig.FAKE_PLAYER_AGGRO_FPC)
			{
				return c;
			}
		}
		
		return World.getNearestVisibleObjectInRange(leader, Attackable.class, 650,
			mob -> (mob != npc) && !mob.isDead() && !mob.isFakePlayer() && ((mob.getTarget() == leader) || (mob.getMostHated() == leader)));
	}
	
	private Creature resolveCurrentCombatTarget(Npc npc)
	{
		if (npc.isAttackable())
		{
			final Creature mostHated = npc.asAttackable().getMostHated();
			if ((mostHated != null) && !mostHated.isDead())
			{
				return mostHated;
			}
		}
		final WorldObject target = npc.getTarget();
		return (target instanceof Creature c) ? c : null;
	}
	
	public void engageTarget(Npc npc, Creature target)
	{
		if ((npc == null) || (target == null) || target.isDead() || !npc.isAttackable())
		{
			return;
		}
		npc.setRunning();
		npc.setTarget(target);
		npc.asAttackable().addDamageHate(target, 0, 500);
		npc.getAI().setIntentionAttack(target);
	}
	
	public void broadcastBotChat(Npc npc, String text)
	{
		if ((npc == null) || (text == null) || text.isBlank())
		{
			return;
		}
		npc.broadcastPacket(new CreatureSay(npc, ChatType.GENERAL, npc.getName(), text));
	}
	
	public static FakePlayerAiEngine getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final FakePlayerAiEngine INSTANCE = new FakePlayerAiEngine();
	}
}
