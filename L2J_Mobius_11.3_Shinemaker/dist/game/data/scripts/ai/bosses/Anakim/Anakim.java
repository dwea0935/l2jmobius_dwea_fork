/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ai.bosses.Anakim;

import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.ArrayUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Anakim Manager AI - version 2023
 * @author Notorion
 */
public class Anakim extends Script
{
	// Status
	private static final int ALIVE = 0;
	private static final int FIGHTING = 1;
	private static final int DEAD = 2;
	
	// NPCs
	private static final int ANAKIM = 29348;
	private static final int EXIST_CUBIC = 31109;
	private static final int ANAKIM_CUBIC = 31101;
	
	// General Configs
	private static final int MIN_LEVEL_TO_ATTACK = 110;
	
	// Minions
	// @formatter:off
	private static final int[] ANAKIM_MINIONS = {29349, 29350, 29351};
	// @formatter:on
	private static final int[] ALL_MOBS =
	{
		ANAKIM,
		ANAKIM_MINIONS[0],
		ANAKIM_MINIONS[1],
		ANAKIM_MINIONS[2]
	};
	
	// Locations
	private static final Location ENTER_ANAKIM_LOC = new Location(185078, -12375, -5488);
	private static final Location SPAWN_LOC = new Location(185076, -13295, -5488, 16384);
	private static final Location KICK_LOC = new Location(183505, -15903, -2712);
	
	private static final ZoneType BOSS_ZONE = ZoneManager.getInstance().getZoneById(12003);
	
	// Barrier Skills
	private static final SkillHolder BARRIER_INITIAL = new SkillHolder(29518, 1);
	private static final SkillHolder BARRIER_TIMED = new SkillHolder(29515, 1);
	
	// Barrier Configuration
	private static final int HITS_TO_BREAK_INITIAL = 2000; // Hits to break immunity
	private static final int HITS_TO_BREAK_TIMED = 2000; // Hits to break timed barrier
	private static final long TIME_VULNERABLE = 3 * 60000; // Vulnerable duration (3 min)
	private static final long TIME_BARRIER_LIMIT = 10 * 60000; // Max barrier duration (10 min)
	private static final long TIME_RESET_INACTIVITY = 10 * 60000; // Reset if inactive
	
	// Variables
	private static long _lastAction;
	private static Npc _anakimBoss;
	
	// Barrier Controls
	private boolean _isInitialBarrier = false;
	private boolean _isTimedBarrier = false;
	private final Map<Npc, Integer> _hitCounter = new ConcurrentHashMap<>();
	private long _lastHitTime = 0;
	
	public Anakim()
	{
		addTalkId(EXIST_CUBIC, ANAKIM_CUBIC);
		addStartNpc(EXIST_CUBIC, ANAKIM_CUBIC);
		addFirstTalkId(EXIST_CUBIC, ANAKIM_CUBIC);
		addAttackId(ALL_MOBS);
		addKillId(ALL_MOBS);
		addSkillSeeId(ALL_MOBS);
		
		final StatSet info = GrandBossManager.getInstance().getStatSet(ANAKIM);
		if (GrandBossManager.getInstance().getStatus(ANAKIM) == DEAD)
		{
			long respawnTime = info.getLong("respawn_time");
			if (respawnTime == 0)
			{
				respawnTime = getNextRespawnTime();
				info.set("respawn_time", respawnTime);
				GrandBossManager.getInstance().setStatSet(ANAKIM, info);
			}
			
			final long delay = respawnTime - System.currentTimeMillis();
			if (delay > 0)
			{
				startQuestTimer("unlock_anakim", delay, null, null);
			}
			else
			{
				spawnAnakim();
			}
		}
		else
		{
			spawnAnakim();
		}
	}
	
	private void spawnAnakim()
	{
		GrandBossManager.getInstance().setStatus(ANAKIM, ALIVE);
		
		if ((_anakimBoss != null) && !_anakimBoss.isDead())
		{
			return;
		}
		
		_anakimBoss = addSpawn(ANAKIM, SPAWN_LOC, false, 0);
		GrandBossManager.getInstance().addBoss((GrandBoss) _anakimBoss);
		
		// Immobilize Boss
		_anakimBoss.setRandomWalking(false);
		_anakimBoss.setRandomAnimation(false);
		
		applyInitialBarrier(_anakimBoss);
		
		_lastAction = System.currentTimeMillis();
		startQuestTimer("check_activity_task", 60000, null, null);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "unlock_anakim":
			{
				spawnAnakim();
				break;
			}
			case "check_activity_task":
			{
				// Reset logic
				if ((_lastAction + TIME_RESET_INACTIVITY) < System.currentTimeMillis())
				{
					if ((_anakimBoss != null) && !_anakimBoss.isDead())
					{
						if ((GrandBossManager.getInstance().getStatus(ANAKIM) == FIGHTING) || (_anakimBoss.getCurrentHp() < _anakimBoss.getMaxHp()))
						{
							resetAnakim(_anakimBoss);
						}
					}
				}
				else
				{
					// Minions gather around an idle boss while players are near.
					if ((_anakimBoss != null) && !_anakimBoss.isDead() && (GrandBossManager.getInstance().getStatus(ANAKIM) != FIGHTING) && (BOSS_ZONE != null) && !BOSS_ZONE.getPlayersInside().isEmpty())
					{
						manageMinions(_anakimBoss);
					}
					
					startQuestTimer("check_activity_task", 60000, null, null);
				}
				break;
			}
			case "END_VULNERABILITY":
			{
				if ((npc != null) && !npc.isDead())
				{
					applyTimedBarrier(npc);
				}
				break;
			}
			case "END_TIMED_BARRIER":
			{
				if ((npc != null) && !npc.isDead() && _isTimedBarrier)
				{
					applyTimedBarrier(npc);
				}
				break;
			}
			case "cancel_timers":
			{
				cancelQuestTimer("check_activity_task", null, null);
				cancelQuestTimers("END_VULNERABILITY");
				cancelQuestTimers("END_TIMED_BARRIER");
				break;
			}
			case "exist":
			{
				player.teleToLocation(TeleportWhereType.TOWN);
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	private void manageMinions(Npc boss)
	{
		if (BOSS_ZONE == null)
		{
			return;
		}
		
		for (Creature c : BOSS_ZONE.getCharactersInside())
		{
			if ((c != null) && c.isNpc() && !c.isDead())
			{
				if (ArrayUtil.contains(ANAKIM_MINIONS, c.getId()))
				{
					// Pull back minions that wandered off.
					if (boss.calculateDistance3D(c) > 500)
					{
						c.teleToLocation(boss.getX() + getRandom(-200, 200), boss.getY() + getRandom(-200, 200), boss.getZ());
					}
				}
			}
		}
	}
	
	private void resetAnakim(Npc npc)
	{
		if (GrandBossManager.getInstance().getStatus(ANAKIM) != ALIVE)
		{
			GrandBossManager.getInstance().setStatus(ANAKIM, ALIVE);
		}
		
		npc.setCurrentHp(npc.getMaxHp());
		npc.setCurrentMp(npc.getMaxMp());
		npc.teleToLocation(SPAWN_LOC, true);
		npc.stopAllEffects();
		
		if (npc.isAttackable())
		{
			npc.asAttackable().clearAggroList();
		}
		
		if (BOSS_ZONE != null)
		{
			for (Creature c : BOSS_ZONE.getCharactersInside())
			{
				if ((c != null) && c.isPlayer())
				{
					c.teleToLocation(MapRegionData.getInstance().getTeleToLocation(c, TeleportWhereType.TOWN));
				}
			}
		}
		
		applyInitialBarrier(npc);
		_lastAction = System.currentTimeMillis();
		startQuestTimer("check_activity_task", 60000, null, null);
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		if (npc.getId() == ANAKIM_CUBIC)
		{
			player.teleToLocation(ENTER_ANAKIM_LOC, true);
			return null;
		}
		
		return super.onTalk(npc, player);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isPet)
	{
		_lastAction = System.currentTimeMillis();
		
		// Level Check
		if (attacker.getLevel() < MIN_LEVEL_TO_ATTACK)
		{
			attacker.teleToLocation(KICK_LOC, true);
			return;
		}
		
		if (npc.getId() == ANAKIM)
		{
			if (GrandBossManager.getInstance().getStatus(ANAKIM) != FIGHTING)
			{
				GrandBossManager.getInstance().setStatus(ANAKIM, FIGHTING);
			}
			
			// Wake up Minions
			if (BOSS_ZONE != null)
			{
				for (Creature c : BOSS_ZONE.getCharactersInside())
				{
					if ((c != null) && c.isNpc() && !c.isDead())
					{
						if (ArrayUtil.contains(ANAKIM_MINIONS, c.getId()))
						{
							c.asAttackable().addDamageHate(attacker, 0, 999);
							c.getAI().setIntentionAttack(attacker);
						}
					}
				}
			}
			
			if (_isInitialBarrier || _isTimedBarrier)
			{
				if ((System.currentTimeMillis() - _lastHitTime) > 60000)
				{
					_hitCounter.put(npc, 0);
				}
				
				_lastHitTime = System.currentTimeMillis();
				
				final int hits = _hitCounter.merge(npc, 1, Integer::sum);
				if (hits >= (_isInitialBarrier ? HITS_TO_BREAK_INITIAL : HITS_TO_BREAK_TIMED))
				{
					enterVulnerableState(npc);
				}
			}
		}
		
		// Anti-Kite
		if (npc.isMinion() || npc.isRaid())
		{
			if ((BOSS_ZONE != null) && !BOSS_ZONE.isInsideZone(attacker))
			{
				attacker.doDie(null);
			}
			
			if ((BOSS_ZONE != null) && !BOSS_ZONE.isInsideZone(npc))
			{
				final Spawn spawn = npc.getSpawn();
				if (spawn != null)
				{
					npc.teleToLocation(spawn.getX(), spawn.getY(), spawn.getZ());
				}
				else
				{
					npc.teleToLocation(SPAWN_LOC, true);
				}
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isPet)
	{
		if (npc.getId() == ANAKIM)
		{
			notifyEvent("cancel_timers", null, null);
			
			// The exit cubic appears where the boss died.
			addSpawn(EXIST_CUBIC, npc.getLocation(), false, 900000);
			
			GrandBossManager.getInstance().setStatus(ANAKIM, DEAD);
			
			final long respawnTime = getNextRespawnTime();
			final StatSet info = GrandBossManager.getInstance().getStatSet(ANAKIM);
			info.set("respawn_time", respawnTime);
			GrandBossManager.getInstance().setStatSet(ANAKIM, info);
			
			startQuestTimer("unlock_anakim", respawnTime - System.currentTimeMillis(), null, null);
			
			_anakimBoss = null;
		}
	}
	
	@Override
	public void onSkillSee(Npc npc, Player caster, Skill skill, Collection<WorldObject> targets, boolean isPet)
	{
		if (ArrayUtil.contains(ANAKIM_MINIONS, npc.getId()) && getRandomBoolean())
		{
			if (skill.getAbnormalType() == AbnormalType.HP_RECOVER)
			{
				if (!npc.isCastingNow())
				{
					final WorldObject target = npc.getTarget();
					if ((target != npc) && (target != caster) && (target != _anakimBoss))
					{
						npc.asAttackable().clearAggroList();
						npc.setTarget(caster);
						npc.asAttackable().addDamageHate(caster, 500, 99999);
						npc.getAI().setIntentionAttack(caster);
					}
				}
			}
		}
	}
	
	private void applyInitialBarrier(Npc npc)
	{
		cleanBarriers(npc);
		_isInitialBarrier = true;
		_isTimedBarrier = false;
		_hitCounter.put(npc, 0);
		
		npc.setInvul(true);
		final Skill barrier = BARRIER_INITIAL.getSkill();
		if (barrier != null)
		{
			barrier.applyEffects(npc, npc);
		}
		
		cancelQuestTimer("END_VULNERABILITY", npc, null);
		cancelQuestTimer("END_TIMED_BARRIER", npc, null);
	}
	
	private void applyTimedBarrier(Npc npc)
	{
		cleanBarriers(npc);
		_isInitialBarrier = false;
		_isTimedBarrier = true;
		_hitCounter.put(npc, 0);
		
		npc.setInvul(true);
		final Skill barrier = BARRIER_TIMED.getSkill();
		if (barrier != null)
		{
			barrier.applyEffects(npc, npc);
		}
		
		startQuestTimer("END_TIMED_BARRIER", TIME_BARRIER_LIMIT, npc, null);
	}
	
	private void enterVulnerableState(Npc npc)
	{
		cleanBarriers(npc);
		_isInitialBarrier = false;
		_isTimedBarrier = false;
		_hitCounter.put(npc, 0);
		
		npc.setInvul(false);
		startQuestTimer("END_VULNERABILITY", TIME_VULNERABLE, npc, null);
	}
	
	private void cleanBarriers(Npc npc)
	{
		final Skill initialSkill = BARRIER_INITIAL.getSkill();
		if (initialSkill != null)
		{
			npc.stopSkillEffects(initialSkill);
		}
		
		final Skill timedSkill = BARRIER_TIMED.getSkill();
		if (timedSkill != null)
		{
			npc.stopSkillEffects(timedSkill);
		}
		
		npc.setInvul(false);
	}
	
	// Respawns every Friday at 20:00.
	private long getNextRespawnTime()
	{
		// Set to Friday 20:00
		final Calendar nextSpawn = Calendar.getInstance();
		nextSpawn.set(Calendar.HOUR_OF_DAY, 20);
		nextSpawn.set(Calendar.MINUTE, 0);
		nextSpawn.set(Calendar.SECOND, 0);
		nextSpawn.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
		
		// If Friday 20:00 already passed this week, move to next week
		if (nextSpawn.getTimeInMillis() < System.currentTimeMillis())
		{
			nextSpawn.add(Calendar.WEEK_OF_YEAR, 1);
		}
		
		return nextSpawn.getTimeInMillis();
	}
	
	public static void main(String[] args)
	{
		new Anakim();
	}
}
