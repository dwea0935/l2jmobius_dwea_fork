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
package ai.bosses.Orfen;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;

import ai.AbstractNpcAI;

/**
 * Orfen's AI
 * @author Emperorc, Mobius
 */
public class Orfen extends AbstractNpcAI
{
	private static final Location SPAWN_LOCATION = new Location(43728, 17220, -4342);
	
	private static final NpcStringId[] TEXT =
	{
		NpcStringId.S1_STOP_KIDDING_YOURSELF_ABOUT_YOUR_OWN_POWERLESSNESS,
		NpcStringId.S1_YOU_WILL_LEARN_WHAT_THE_TRUE_FEAR_IS,
		NpcStringId.YOU_RE_REALLY_STUPID_TO_HAVE_CHALLENGED_ME_S1_GET_READY,
		NpcStringId.S1_DO_YOU_THINK_THAT_S_GOING_TO_WORK
	};
	
	private static final int ORFEN = 29325;
	private static final int ARIMA = 29326;
	private static final int ARIMUS = 29327;
	
	private static final byte ALIVE = 0;
	private static final byte DEAD = 1;
	
	private static final SkillHolder BLOW = new SkillHolder(4067, 4);
	private static final SkillHolder ORFEN_HEAL = new SkillHolder(4516, 1);
	private static final SkillHolder ORFEN_SLASHER = new SkillHolder(32486, 1);
	private static final SkillHolder ORFEN_FATAL_SLASHER = new SkillHolder(32487, 1);
	private static final SkillHolder ORFEN_ENERGY_SCATTER = new SkillHolder(32488, 1);
	private static final SkillHolder ORFEN_FURY_ENERGY_WAVE = new SkillHolder(32489, 1);
	private static final SkillHolder YOKE_OF_ORFEN = new SkillHolder(32490, 1);
	private static final SkillHolder ORFEN_BLOW_UP = new SkillHolder(32491, 1);
	private static final SkillHolder ORFEN_FATAL_STAMP = new SkillHolder(32492, 1);
	private static final SkillHolder ORFEN_RAISE_SPORE = new SkillHolder(32493, 1);
	private static final SkillHolder HALLUCINATING_DUST = new SkillHolder(32494, 1);
	private static final SkillHolder ORFEN_RAGE = new SkillHolder(32495, 1);
	
	private static Set<Attackable> _minions = ConcurrentHashMap.newKeySet();
	
	private Orfen()
	{
		final int[] mobs =
		{
			ORFEN,
			ARIMA,
			ARIMUS
		};
		registerMobs(mobs);
		final StatSet info = GrandBossManager.getInstance().getStatSet(ORFEN);
		final int status = GrandBossManager.getInstance().getStatus(ORFEN);
		if (status == DEAD)
		{
			// load the unlock date and time for Orfen from DB
			final long temp = info.getLong("respawn_time") - System.currentTimeMillis();
			// if Orfen is locked until a certain time, mark it so and start the unlock timer
			// the unlock time has not yet expired.
			if (temp > 0)
			{
				startQuestTimer("orfen_unlock", temp, null, null);
			}
			else
			{
				// the time has already expired while the server was offline. Immediately spawn Orfen.
				final GrandBoss orfen = (GrandBoss) addSpawn(ORFEN, SPAWN_LOCATION, false, 0);
				GrandBossManager.getInstance().setStatus(ORFEN, ALIVE);
				spawnBoss(orfen);
				cancelQuestTimer("DISTANCE_CHECK", orfen, null);
				startQuestTimer("DISTANCE_CHECK", 5000, orfen, null, true);
			}
		}
		else
		{
			final int loc_x = info.getInt("loc_x");
			final int loc_y = info.getInt("loc_y");
			final int loc_z = info.getInt("loc_z");
			final int heading = info.getInt("heading");
			final double hp = info.getDouble("currentHP");
			final double mp = info.getDouble("currentMP");
			final GrandBoss orfen = (GrandBoss) addSpawn(ORFEN, loc_x, loc_y, loc_z, heading, false, 0);
			orfen.setCurrentHpMp(hp, mp);
			spawnBoss(orfen);
			cancelQuestTimer("DISTANCE_CHECK", orfen, null);
			startQuestTimer("DISTANCE_CHECK", 5000, orfen, null, true);
		}
	}
	
	public void spawnBoss(GrandBoss npc)
	{
		GrandBossManager.getInstance().addBoss(npc);
		npc.broadcastPacket(new PlaySound(1, "BS01_A", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
		// Spawn minions
		final int x = npc.getX();
		final int y = npc.getY();
		Attackable mob;
		mob = addSpawn(ARIMA, x + 100, y + 100, npc.getZ(), 0, false, 0).asAttackable();
		mob.setIsRaidMinion(true);
		_minions.add(mob);
		mob = addSpawn(ARIMA, x + 100, y - 100, npc.getZ(), 0, false, 0).asAttackable();
		mob.setIsRaidMinion(true);
		_minions.add(mob);
		mob = addSpawn(ARIMA, x - 100, y + 100, npc.getZ(), 0, false, 0).asAttackable();
		mob.setIsRaidMinion(true);
		_minions.add(mob);
		mob = addSpawn(ARIMA, x - 100, y - 100, npc.getZ(), 0, false, 0).asAttackable();
		mob.setIsRaidMinion(true);
		_minions.add(mob);
		startQuestTimer("check_minion_loc", 10000, npc, null, true);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "orfen_unlock":
			{
				final GrandBoss orfen = (GrandBoss) addSpawn(ORFEN, SPAWN_LOCATION, false, 0);
				GrandBossManager.getInstance().setStatus(ORFEN, ALIVE);
				spawnBoss(orfen);
				cancelQuestTimer("DISTANCE_CHECK", orfen, null);
				startQuestTimer("DISTANCE_CHECK", 5000, orfen, null, true);
				break;
			}
			case "check_minion_loc":
			{
				for (Attackable mob : _minions)
				{
					if (!npc.isInsideRadius2D(mob, 3000))
					{
						mob.teleToLocation(npc.getLocation());
						npc.asAttackable().clearAggroList();
						npc.getAI().setIntention(Intention.IDLE, null, null);
					}
				}
				break;
			}
			case "despawn_minions":
			{
				for (Attackable mob : _minions)
				{
					mob.decayMe();
				}
				_minions.clear();
				break;
			}
			case "spawn_minion":
			{
				final Attackable mob = addSpawn(ARIMA, npc.getX(), npc.getY(), npc.getZ(), 0, false, 0).asAttackable();
				mob.setIsRaidMinion(true);
				_minions.add(mob);
				break;
			}
			case "DISTANCE_CHECK":
			{
				if ((npc == null) || npc.isDead())
				{
					cancelQuestTimers("DISTANCE_CHECK");
				}
				else if (npc.calculateDistance2D(npc.getSpawn()) > 10000)
				{
					npc.asAttackable().clearAggroList();
					npc.getAI().setIntention(Intention.MOVE_TO, SPAWN_LOCATION);
				}
				break;
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onFactionCall(Npc npc, Npc caller, Player attacker, boolean isSummon)
	{
		if ((caller == null) || (npc == null) || npc.isCastingNow(SkillCaster::isAnyNormalType))
		{
			return;
		}
		
		final int npcId = npc.getId();
		final int callerId = caller.getId();
		if ((npcId == ARIMA) && (getRandom(20) == 0))
		{
			npc.setTarget(attacker);
			npc.doCast(BLOW.getSkill());
		}
		else if (npcId == ARIMUS)
		{
			int chance = 1;
			if (callerId == ORFEN)
			{
				chance = 9;
			}
			if ((callerId != ARIMUS) && (caller.getCurrentHp() < (caller.getMaxHp() / 2.0)) && (getRandom(10) < chance))
			{
				npc.getAI().setIntention(Intention.IDLE, null, null);
				npc.setTarget(caller);
				npc.doCast(ORFEN_HEAL.getSkill());
			}
		}
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final int npcId = npc.getId();
		if (npcId == ORFEN)
		{
			if (manageSkills(npc) && npc.isInsideRadius2D(attacker, 1000) && (getRandom(10) == 0))
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, TEXT[getRandom(3)], attacker.getName());
				npc.setTarget(attacker);
				npc.doCast(ORFEN_FATAL_SLASHER.getSkill());
			}
			else if (manageSkills(npc) && npc.isInsideRadius2D(attacker, 1000) && (getRandom(10) == 0))
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, TEXT[getRandom(3)], attacker.getName());
				npc.setTarget(attacker);
				npc.doCast(ORFEN_ENERGY_SCATTER.getSkill());
			}
			else if (manageSkills(npc) && npc.isInsideRadius2D(attacker, 1000) && (getRandom(10) == 0))
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, TEXT[getRandom(3)], attacker.getName());
				npc.setTarget(attacker);
				npc.doCast(ORFEN_FURY_ENERGY_WAVE.getSkill());
			}
			else if (manageSkills(npc) && npc.isInsideRadius2D(attacker, 1000) && (getRandom(10) == 0))
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, TEXT[getRandom(3)], attacker.getName());
				npc.setTarget(attacker);
				npc.doCast(YOKE_OF_ORFEN.getSkill());
			}
			else if (manageSkills(npc) && npc.isInsideRadius2D(attacker, 1000) && (getRandom(10) == 0))
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, TEXT[getRandom(3)], attacker.getName());
				npc.setTarget(attacker);
				npc.doCast(ORFEN_BLOW_UP.getSkill());
			}
			else if (manageSkills(npc) && npc.isInsideRadius2D(attacker, 1000) && (getRandom(10) == 0))
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, TEXT[getRandom(3)], attacker.getName());
				npc.setTarget(attacker);
				npc.doCast(ORFEN_FATAL_STAMP.getSkill());
			}
			else if (manageSkills(npc) && npc.isInsideRadius2D(attacker, 1000) && (getRandom(10) == 0))
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, TEXT[getRandom(3)], attacker.getName());
				npc.setTarget(attacker);
				npc.doCast(ORFEN_RAISE_SPORE.getSkill());
			}
			else if (manageSkills(npc) && npc.isInsideRadius2D(attacker, 1000) && (getRandom(10) == 0))
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, TEXT[getRandom(3)], attacker.getName());
				npc.setTarget(attacker);
				npc.doCast(HALLUCINATING_DUST.getSkill());
			}
			else if (manageSkills(npc) && npc.isInsideRadius2D(attacker, 1000) && (getRandom(10) == 0) && (npc.getCurrentHp() <= (npc.getMaxHp() * 0.5)))
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, TEXT[getRandom(3)], attacker.getName());
				npc.setTarget(attacker);
				npc.doCast(ORFEN_RAGE.getSkill());
			}
			else if (manageSkills(npc) && npc.isInsideRadius2D(attacker, 1000))
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, TEXT[getRandom(3)], attacker.getName());
				npc.setTarget(attacker);
				npc.doCast(ORFEN_SLASHER.getSkill());
			}
		}
		else if (npcId == ARIMUS)
		{
			if (!npc.isCastingNow(SkillCaster::isAnyNormalType) && ((npc.getCurrentHp() - damage) < (npc.getMaxHp() / 2.0)))
			{
				npc.setTarget(attacker);
				npc.doCast(ORFEN_HEAL.getSkill());
			}
		}
	}
	
	private boolean manageSkills(Npc npc)
	{
		if (npc.isCastingNow())
		{
			return false;
		}
		return true;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.getId() == ORFEN)
		{
			npc.broadcastPacket(new PlaySound(1, "BS02_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
			GrandBossManager.getInstance().setStatus(ORFEN, DEAD);
			
			// Calculate Min and Max respawn times randomly.
			long respawnTime = Config.ORFEN_SPAWN_INTERVAL + getRandom(-Config.ORFEN_SPAWN_RANDOM, Config.ORFEN_SPAWN_RANDOM);
			respawnTime *= 3600000;
			startQuestTimer("orfen_unlock", respawnTime, null, null);
			
			// Also save the respawn time so that the info is maintained past reboots.
			final StatSet info = GrandBossManager.getInstance().getStatSet(ORFEN);
			info.set("respawn_time", System.currentTimeMillis() + respawnTime);
			GrandBossManager.getInstance().setStatSet(ORFEN, info);
			cancelQuestTimer("check_minion_loc", npc, null);
			startQuestTimer("despawn_minions", 20000, null, null);
			cancelQuestTimers("spawn_minion");
			
			// Stop distance check task.
			cancelQuestTimers("DISTANCE_CHECK");
		}
		else if ((GrandBossManager.getInstance().getStatus(ORFEN) == ALIVE) && (npc.getId() == ARIMA))
		{
			_minions.remove(npc);
			startQuestTimer("spawn_minion", 360000, npc, null);
		}
	}
	
	public static void main(String[] args)
	{
		new Orfen();
	}
}
