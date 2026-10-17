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
import org.l2jmobius.gameserver.ai.neural.FakePlayerNeuralPolicy;
import org.l2jmobius.gameserver.ai.neural.FakePlayerNeuralPolicy.CurriculumStage;
import org.l2jmobius.gameserver.ai.neural.FakePlayerNeuralPolicy.TacticalAction;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.FakePlayerData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.holders.npc.FakePlayerHolder;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Java 27 Tactical Utility-AI, 5-Stage Neural Curriculum RL &amp; Squad Synergy Engine for FakePlayers.
 * Executes a 250ms high-frequency micro-decision loop supporting:
 * <ul>
 *   <li>Archetype classification ({@link Archetype}: ARCHER, MAGE, DAGGER, HEALER, TANK, WARRIOR)</li>
 *   <li>Dynamic tactical postures ({@link Posture}: AGGRESSIVE, KITE_DEFENSIVE, ASSIST_PLAYER, GUARD)</li>
 *   <li>In-JVM Java 27 SIMD-accelerated {@link FakePlayerNeuralPolicy} (`16 -> 32 -> 16 -> 8`) with 5-Stage Progressive Curriculum RL</li>
 *   <li>Squad follow formation &amp; real-time target assist for any Player/GM</li>
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
		private volatile int _rivalObjectId;
		private volatile int _squadSlot;
		private volatile long _lastHealSkillMs;
		private volatile long _lastPotionMs;
		private volatile long _lastKiteMs;
		private volatile long _lastSpecialSkillMs;
		private volatile String _lastTacticalAction = "Idle";
		private final float[] _stateVec = new float[FakePlayerNeuralPolicy.INPUT_DIM];
		private final float[] _actionProbs = new float[FakePlayerNeuralPolicy.ACTION_DIM];
		
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
		
		public int getRivalObjectId()
		{
			return _rivalObjectId;
		}
		
		public void setRivalObjectId(int rivalObjectId)
		{
			_rivalObjectId = rivalObjectId;
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
		
		public float[] getStateVec()
		{
			return _stateVec;
		}
		
		public float[] getActionProbs()
		{
			return _actionProbs;
		}
	}
	
	private final Map<Integer, BotState> _botStates = new ConcurrentHashMap<>();
	private final Map<Integer, Npc> _trackedNpcs = new ConcurrentHashMap<>();
	private final AtomicInteger _squadSlotCounter = new AtomicInteger(0);
	private final FakePlayerNeuralPolicy _neuralPolicy = new FakePlayerNeuralPolicy();
	private ScheduledFuture<?> _tickTask;
	
	protected FakePlayerAiEngine()
	{
		start();
	}
	
	public FakePlayerNeuralPolicy getNeuralPolicy()
	{
		return _neuralPolicy;
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
			LOGGER.info(getClass().getSimpleName() + ": Tactical Utility-AI & Neural Curriculum (" + _neuralPolicy.getStage().getTitle() + ") active (250ms tick, LLM bridge=" + FakePlayersConfig.FAKE_PLAYER_LLM_ENABLED + ").");
		}
	}
	
	/**
	 * Spawns a single FakePlayer NPC at the given coordinates without prematurely zeroing the spawn count.
	 * @param npcId template ID (e.g. 81002..81006)
	 * @param x world X
	 * @param y world Y
	 * @param z world Z
	 * @param heading heading
	 * @param instanceId instance ID
	 * @return spawned {@link Npc} or {@code null}
	 */
	public Npc spawnFakePlayer(int npcId, int x, int y, int z, int heading, int instanceId)
	{
		try
		{
			final NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
			if (template == null)
			{
				LOGGER.warning(getClass().getSimpleName() + ": NpcTemplate not found for FakePlayer ID " + npcId);
				return null;
			}
			final Spawn spawn = new Spawn(template);
			spawn.setXYZ(x, y, z);
			spawn.setHeading(heading);
			spawn.setInstanceId(instanceId);
			spawn.setAmount(1);
			final Npc npc = spawn.doSpawn(false);
			spawn.stopRespawn();
			SpawnTable.getInstance().addSpawn(spawn);
			if (npc != null)
			{
				npc.setRunning();
				registerBot(npc);
			}
			return npc;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed to spawn FakePlayer " + npcId + ": " + e.getMessage(), e);
			return null;
		}
	}
	
	/**
	 * Spawns a 1v1 self-play curriculum duel (Dagger vs Archer) near the GM to train the neural policy online.
	 * @param gm the invoking admin player
	 * @return number of duelists spawned
	 */
	public int spawnTrainingDuel(Player gm)
	{
		if (gm == null)
		{
			return 0;
		}
		final Npc botA = spawnFakePlayer(81005, gm.getX() + 90, gm.getY() - 80, gm.getZ(), gm.getHeading(), gm.getInstanceId()); // Vex (Dagger)
		final Npc botB = spawnFakePlayer(81002, gm.getX() + 260, gm.getY() + 80, gm.getZ(), gm.getHeading(), gm.getInstanceId()); // Lyra (Archer)
		if ((botA != null) && (botB != null))
		{
			final BotState stateA = getOrCreateState(botA);
			final BotState stateB = getOrCreateState(botB);
			stateA.setPosture(Posture.AGGRESSIVE);
			stateB.setPosture(Posture.KITE_DEFENSIVE);
			stateA.setRivalObjectId(botB.getObjectId());
			stateB.setRivalObjectId(botA.getObjectId());
			engageTarget(botA, botB);
			engageTarget(botB, botA);
			return 2;
		}
		return (botA != null ? 1 : 0) + (botB != null ? 1 : 0);
	}
	
	/**
	 * Spawns a 3v3 tactical squad simulation near the GM (Tank + Healer + Archer vs Dagger + Mage + Tank)
	 * to train Stage 4/5 Squad Synergy and PvP Arena policy weights.
	 * @param gm the invoking admin player
	 * @return number of bots spawned
	 */
	public int spawnSquadSim(Player gm)
	{
		if (gm == null)
		{
			return 0;
		}
		final int[] teamAlphaIds = { 81004, 81003, 81002 }; // Aegis, Seraphina, Lyra
		final int[] teamBravoIds = { 81005, 81006, 81004 }; // Vex, Zephyr, Aegis
		final List<Npc> teamAlpha = new ArrayList<>(3);
		final List<Npc> teamBravo = new ArrayList<>(3);
		
		for (int i = 0; i < 3; i++)
		{
			final Npc a = spawnFakePlayer(teamAlphaIds[i], gm.getX() + 110, gm.getY() - 120 + (i * 95), gm.getZ(), gm.getHeading(), gm.getInstanceId());
			if (a != null)
			{
				teamAlpha.add(a);
			}
			final Npc b = spawnFakePlayer(teamBravoIds[i], gm.getX() + 330, gm.getY() - 120 + (i * 95), gm.getZ(), gm.getHeading(), gm.getInstanceId());
			if (b != null)
			{
				teamBravo.add(b);
			}
		}
		
		for (int i = 0; i < teamAlpha.size(); i++)
		{
			final Npc a = teamAlpha.get(i);
			final Npc rival = teamBravo.isEmpty() ? null : teamBravo.get(i % teamBravo.size());
			if (rival != null)
			{
				getOrCreateState(a).setRivalObjectId(rival.getObjectId());
				engageTarget(a, rival);
			}
		}
		for (int i = 0; i < teamBravo.size(); i++)
		{
			final Npc b = teamBravo.get(i);
			final Npc rival = teamAlpha.isEmpty() ? null : teamAlpha.get(i % teamAlpha.size());
			if (rival != null)
			{
				getOrCreateState(b).setRivalObjectId(rival.getObjectId());
				engageTarget(b, rival);
			}
		}
		return teamAlpha.size() + teamBravo.size();
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
		final int stageId = _neuralPolicy.getStage().getId();
		final float[] s = state.getStateVec();
		final float[] probs = state.getActionProbs();
		
		// Resolve leader & triage target
		Player leader = null;
		if (state.getAssistPlayerObjectId() > 0)
		{
			leader = World.getPlayer(state.getAssistPlayerObjectId());
			if ((leader == null) || !leader.isOnline())
			{
				state.setPosture(Posture.AGGRESSIVE);
				state.setAssistPlayerObjectId(0);
				state.setLastTacticalAction("Leader offline -> AGGRESSIVE");
				leader = null;
			}
		}
		
		Creature triageTarget = findTriageTarget(npc, state, leader, allBots);
		Creature currentTarget = resolveCurrentCombatTarget(npc, state, leader);
		
		buildFeatureVector(npc, state, triageTarget, currentTarget, leader, now, s);
		final TacticalAction chosen = _neuralPolicy.selectAction(s, state.getArchetype(), probs);
		
		// 1. Self-Preservation Second Wind (All Archetypes, unlocked Stage 1+)
		if ((chosen == TacticalAction.SECOND_WIND_POTION) || (npc.isInCombat() && (npc.getCurrentHp() < (npc.getMaxHp() * 0.35)) && ((now - state.getLastPotionMs()) >= 15000L)))
		{
			state.setLastPotionMs(now);
			npc.broadcastPacket(new MagicSkillUse(npc, npc, 2037, 1, 100, 1000));
			npc.setCurrentHp(Math.min(npc.getMaxHp(), npc.getCurrentHp() + (npc.getMaxHp() * 0.30)));
			state.setLastTacticalAction("NN[" + stageId + "] Second-Wind (+30% HP)");
			_neuralPolicy.recordOutcome(s, TacticalAction.SECOND_WIND_POTION, probs, 1.35f);
		}
		
		// 2. Healer Triage (unlocked Stage 3+)
		if ((stageId >= 3) && (triageTarget != null) && ((chosen == TacticalAction.BATTLE_HEAL_TRIAGE) || (state.getArchetype() == Archetype.HEALER) || (state.getPosture() == Posture.ASSIST_PLAYER)) && ((now - state.getLastHealSkillMs()) >= 4000L))
		{
			state.setLastHealSkillMs(now);
			npc.broadcastPacket(new MagicSkillUse(npc, triageTarget, 1218, 1, 800, 1000));
			triageTarget.setCurrentHp(Math.min(triageTarget.getMaxHp(), triageTarget.getCurrentHp() + (triageTarget.getMaxHp() * 0.22)));
			state.setLastTacticalAction("NN[" + stageId + "] Battle Heal -> " + triageTarget.getName());
			_neuralPolicy.recordOutcome(s, TacticalAction.BATTLE_HEAL_TRIAGE, probs, 1.50f);
			return;
		}
		
		// 3. Squad Follow & Assist (Posture.ASSIST_PLAYER)
		if ((state.getPosture() == Posture.ASSIST_PLAYER) && (leader != null) && !leader.isDead() && (npc.getInstanceId() == leader.getInstanceId()))
		{
			final Creature enemy = findLeaderCombatTarget(npc, leader);
			if ((enemy != null) && !enemy.isDead() && npc.isAttackable())
			{
				engageTarget(npc, enemy);
				currentTarget = enemy;
				state.setLastTacticalAction("NN[" + stageId + "] Assist vs " + enemy.getName());
				
				// Tank Intercept if enemy is focusing the assisted player (Stage 3+)
				if ((stageId >= 3) && (state.getArchetype() == Archetype.TANK) && ((now - state.getLastSpecialSkillMs()) >= 5000L) && (enemy.getTarget() == leader) && (npc.calculateDistance2D(enemy) <= 500))
				{
					state.setLastSpecialSkillMs(now);
					npc.broadcastPacket(new MagicSkillUse(npc, enemy, 28, 1, 500, 2000));
					if (enemy.isAttackable())
					{
						enemy.asAttackable().addDamageHate(npc, 0, 5000);
						enemy.setTarget(npc);
					}
					state.setLastTacticalAction("NN[" + stageId + "] Aggro Intercept -> " + enemy.getName());
					_neuralPolicy.recordOutcome(s, TacticalAction.TANK_HATE_INTERCEPT, probs, 1.45f);
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
						state.setLastTacticalAction("NN[" + stageId + "] Regroup -> " + leader.getName());
						_neuralPolicy.recordOutcome(s, TacticalAction.SQUAD_REGROUP_FOLLOW, probs, 0.85f);
					}
					return;
				}
			}
		}
		
		// 4. GUARD Posture: hold position unless directly attacked
		if ((state.getPosture() == Posture.GUARD) && !npc.isInCombat() && (state.getRivalObjectId() == 0))
		{
			return;
		}
		
		if ((currentTarget == null) || currentTarget.isDead())
		{
			if (state.getRivalObjectId() != 0)
			{
				state.setRivalObjectId(0);
				_neuralPolicy.incrementDuelsCompleted();
				_neuralPolicy.recordOutcome(s, chosen, probs, 2.0f);
				state.setLastTacticalAction("NN[" + stageId + "] Duel Victory!");
			}
			return;
		}
		
		final double targetDist = npc.calculateDistance2D(currentTarget);
		
		// 5. Tank Hate Intercept for low-HP allies in general combat (Stage 3+)
		if ((stageId >= 3) && (state.getArchetype() == Archetype.TANK) && ((now - state.getLastSpecialSkillMs()) >= 6000L) && (targetDist <= 500))
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
				state.setLastTacticalAction("NN[" + stageId + "] Tank Taunt -> " + currentTarget.getName());
				_neuralPolicy.recordOutcome(s, TacticalAction.TANK_HATE_INTERCEPT, probs, 1.40f);
				return;
			}
		}
		
		// 6. Archer / Mage / KITE_DEFENSIVE Standoff Kiting (Stage 2+)
		if ((stageId >= 2) && ((chosen == TacticalAction.STANDOFF_KITE) || (state.getArchetype() == Archetype.ARCHER) || (state.getArchetype() == Archetype.MAGE) || (state.getPosture() == Posture.KITE_DEFENSIVE))
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
				state.setLastTacticalAction("NN[" + stageId + "] Standoff Kite <- " + currentTarget.getName());
				_neuralPolicy.recordOutcome(s, TacticalAction.STANDOFF_KITE, probs, 1.30f);
				final Creature resumeTarget = currentTarget;
				ThreadPool.schedule(() ->
				{
					if (!npc.isDead() && !resumeTarget.isDead() && npc.isSpawned())
					{
						engageTarget(npc, resumeTarget);
					}
				}, 750L);
				return;
			}
		}
		
		// 7. Dagger Backstab Flanking (Stage 2+)
		if ((stageId >= 2) && (state.getArchetype() == Archetype.DAGGER) && (targetDist <= 140) && ((now - state.getLastSpecialSkillMs()) >= 4500L))
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
				state.setLastTacticalAction("NN[" + stageId + "] Backstab Flank -> " + currentTarget.getName());
				_neuralPolicy.recordOutcome(s, TacticalAction.BACKSTAB_FLANK, probs, 1.55f);
				return;
			}
		}
		
		// 8. Primary Combat Engagement (Stage 1+)
		if (!npc.isAttackingNow() && !npc.isCastingNow())
		{
			engageTarget(npc, currentTarget);
		}
		final float hpAdvantage = (float) ((npc.getCurrentHp() / Math.max(1.0, npc.getMaxHp())) - (currentTarget.getCurrentHp() / Math.max(1.0, currentTarget.getMaxHp())));
		_neuralPolicy.recordOutcome(s, chosen, probs, 0.25f + (0.45f * hpAdvantage));
	}
	
	private void buildFeatureVector(Npc npc, BotState state, Creature triageTarget, Creature enemy, Player leader, long now, float[] s)
	{
		s[0] = (float) (npc.getCurrentHp() / Math.max(1.0, npc.getMaxHp()));
		s[1] = (float) (npc.getCurrentMp() / Math.max(1.0, npc.getMaxMp()));
		s[2] = (enemy != null) ? (float) (enemy.getCurrentHp() / Math.max(1.0, enemy.getMaxHp())) : 1.0f;
		s[3] = (enemy != null) ? (float) Math.min(1.0, npc.calculateDistance2D(enemy) / 1000.0) : 1.0f;
		s[4] = ((now - state.getLastPotionMs()) >= 15000L) ? 1.0f : 0.0f;
		s[5] = ((now - state.getLastHealSkillMs()) >= 4000L) ? 1.0f : 0.0f;
		s[6] = ((now - state.getLastSpecialSkillMs()) >= 4500L) ? 1.0f : 0.0f;
		s[7] = ((now - state.getLastKiteMs()) >= 2500L) ? 1.0f : 0.0f;
		s[8] = (triageTarget != null) ? 1.0f : 0.0f;
		s[9] = ((enemy != null) && (enemy.getTarget() != null) && (enemy.getTarget() != npc)) ? 1.0f : 0.0f;
		final Archetype arch = state.getArchetype();
		s[10] = ((arch == Archetype.ARCHER) || (arch == Archetype.MAGE)) ? 1.0f : 0.0f;
		s[11] = (arch == Archetype.DAGGER) ? 1.0f : 0.0f;
		s[12] = (arch == Archetype.HEALER) ? 1.0f : 0.0f;
		s[13] = (arch == Archetype.TANK) ? 1.0f : 0.0f;
		s[14] = (arch == Archetype.WARRIOR) ? 1.0f : 0.0f;
		s[15] = ((leader != null) && !npc.isInCombat() && (npc.calculateDistance2D(leader) > 155.0)) ? 1.0f : 0.0f;
	}
	
	private Creature findTriageTarget(Npc npc, BotState state, Player leader, Collection<Npc> allBots)
	{
		if ((state.getArchetype() != Archetype.HEALER) && (state.getPosture() != Posture.ASSIST_PLAYER))
		{
			return null;
		}
		if ((leader != null) && !leader.isDead() && (npc.getInstanceId() == leader.getInstanceId()) && (npc.calculateDistance2D(leader) <= 750) && (leader.getCurrentHp() < (leader.getMaxHp() * 0.65)))
		{
			return leader;
		}
		if (state.getArchetype() == Archetype.HEALER)
		{
			if (npc.getCurrentHp() < (npc.getMaxHp() * 0.65))
			{
				return npc;
			}
			for (Npc ally : allBots)
			{
				if ((ally != null) && !ally.isDead() && (ally.getInstanceId() == npc.getInstanceId()) && (npc.calculateDistance2D(ally) <= 700) && (ally.getCurrentHp() < (ally.getMaxHp() * 0.65)))
				{
					return ally;
				}
			}
		}
		return null;
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
	
	private Creature resolveCurrentCombatTarget(Npc npc, BotState state, Player leader)
	{
		if (state.getRivalObjectId() != 0)
		{
			final Npc rival = _trackedNpcs.get(state.getRivalObjectId());
			if ((rival != null) && rival.isSpawned() && !rival.isDead())
			{
				return rival;
			}
		}
		if (npc.isAttackable())
		{
			final Creature mostHated = npc.asAttackable().getMostHated();
			if ((mostHated != null) && !mostHated.isDead())
			{
				return mostHated;
			}
		}
		final WorldObject target = npc.getTarget();
		if ((target instanceof Creature c) && !c.isDead())
		{
			return c;
		}
		if (leader != null)
		{
			return findLeaderCombatTarget(npc, leader);
		}
		return null;
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
