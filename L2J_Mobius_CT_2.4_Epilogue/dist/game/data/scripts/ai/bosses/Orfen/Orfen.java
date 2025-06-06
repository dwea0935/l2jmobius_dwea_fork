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
package ai.bosses.Orfen;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.Config;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.zone.type.BossZone;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;

import ai.AbstractNpcAI;

/**
 * Orfen's AI
 * @author Emperorc
 */
public class Orfen extends AbstractNpcAI
{
	private static final Location[] POS =
	{
		new Location(43728, 17220, -4342),
		new Location(55024, 17368, -5412),
		new Location(53504, 21248, -5486),
		new Location(53248, 24576, -5262)
	};
	
	private static final String[] TEXT =
	{
		"$s1. Stop kidding yourself about your own powerlessness!",
		"$s1. I'll make you feel what true fear is!",
		"You're really stupid to have challenged me. $s1! Get ready!",
		"$s1. Do you think that's going to work?!"
	};
	
	private static final int ORFEN = 29014;
	// private static final int RAIKEL = 29015;
	private static final int RAIKEL_LEOS = 29016;
	// private static final int RIBA = 29017;
	private static final int RIBA_IREN = 29018;
	
	private static boolean _hasTeleported;
	private static final Collection<Attackable> MINIONS = ConcurrentHashMap.newKeySet();
	private static BossZone ZONE;
	
	private static final byte ALIVE = 0;
	private static final byte DEAD = 1;
	
	private Orfen()
	{
		final int[] mobs =
		{
			ORFEN,
			RAIKEL_LEOS,
			RIBA_IREN
		};
		registerMobs(mobs);
		_hasTeleported = false;
		ZONE = GrandBossManager.getInstance().getZone(POS[0]);
		final StatSet info = GrandBossManager.getInstance().getStatSet(ORFEN);
		if (GrandBossManager.getInstance().getStatus(ORFEN) == DEAD)
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
				final int i = getRandom(10);
				Location loc;
				if (i < 4)
				{
					loc = POS[1];
				}
				else if (i < 7)
				{
					loc = POS[2];
				}
				else
				{
					loc = POS[3];
				}
				final GrandBoss orfen = (GrandBoss) addSpawn(ORFEN, loc, false, 0);
				GrandBossManager.getInstance().setStatus(ORFEN, ALIVE);
				spawnBoss(orfen);
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
		}
	}
	
	public void setSpawnPoint(Npc npc, int index)
	{
		npc.asAttackable().clearAggroList();
		npc.getAI().setIntention(Intention.IDLE, null, null);
		final Spawn spawn = npc.getSpawn();
		spawn.setLocation(POS[index]);
		npc.teleToLocation(POS[index], false);
	}
	
	public void spawnBoss(GrandBoss npc)
	{
		GrandBossManager.getInstance().addBoss(npc);
		npc.broadcastPacket(new PlaySound(1, "BS01_A", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
		startQuestTimer("check_orfen_pos", 10000, npc, null, true);
		// Spawn minions
		final int x = npc.getX();
		final int y = npc.getY();
		Attackable mob;
		mob = addSpawn(RAIKEL_LEOS, x + 100, y + 100, npc.getZ(), 0, false, 0).asAttackable();
		mob.setIsRaidMinion(true);
		MINIONS.add(mob);
		mob = addSpawn(RAIKEL_LEOS, x + 100, y - 100, npc.getZ(), 0, false, 0).asAttackable();
		mob.setIsRaidMinion(true);
		MINIONS.add(mob);
		mob = addSpawn(RAIKEL_LEOS, x - 100, y + 100, npc.getZ(), 0, false, 0).asAttackable();
		mob.setIsRaidMinion(true);
		MINIONS.add(mob);
		mob = addSpawn(RAIKEL_LEOS, x - 100, y - 100, npc.getZ(), 0, false, 0).asAttackable();
		mob.setIsRaidMinion(true);
		MINIONS.add(mob);
		startQuestTimer("check_minion_loc", 10000, npc, null, true);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("orfen_unlock"))
		{
			final int i = getRandom(10);
			Location loc;
			if (i < 4)
			{
				loc = POS[1];
			}
			else if (i < 7)
			{
				loc = POS[2];
			}
			else
			{
				loc = POS[3];
			}
			final GrandBoss orfen = (GrandBoss) addSpawn(ORFEN, loc, false, 0);
			GrandBossManager.getInstance().setStatus(ORFEN, ALIVE);
			spawnBoss(orfen);
		}
		else if (event.equalsIgnoreCase("check_orfen_pos"))
		{
			if ((_hasTeleported && (npc.getCurrentHp() > (npc.getMaxHp() * 0.95))) || (!ZONE.isInsideZone(npc) && !_hasTeleported))
			{
				setSpawnPoint(npc, getRandom(3) + 1);
				_hasTeleported = false;
			}
			else if (_hasTeleported && !ZONE.isInsideZone(npc))
			{
				setSpawnPoint(npc, 0);
			}
		}
		else if (event.equalsIgnoreCase("check_minion_loc"))
		{
			for (Attackable mob : MINIONS)
			{
				if (!npc.isInsideRadius2D(mob, 3000))
				{
					mob.teleToLocation(npc.getLocation());
					npc.asAttackable().clearAggroList();
					npc.getAI().setIntention(Intention.IDLE, null, null);
				}
			}
		}
		else if (event.equalsIgnoreCase("despawn_minions"))
		{
			for (Attackable mob : MINIONS)
			{
				if (mob != null)
				{
					mob.decayMe();
				}
			}
			MINIONS.clear();
		}
		else if (event.equalsIgnoreCase("spawn_minion"))
		{
			final Attackable mob = addSpawn(RAIKEL_LEOS, npc.getX(), npc.getY(), npc.getZ(), 0, false, 0).asAttackable();
			mob.setIsRaidMinion(true);
			MINIONS.add(mob);
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onSkillSee(Npc npc, Player caster, Skill skill, List<WorldObject> targets, boolean isSummon)
	{
		if (npc.getId() == ORFEN)
		{
			final Creature originalCaster = isSummon ? caster.getSummon() : caster;
			if ((skill.getEffectPoint() > 0) && (getRandom(5) == 0) && npc.isInsideRadius2D(originalCaster, 1000))
			{
				final NpcSay packet = new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), TEXT[getRandom(4)].replace("$s1", caster.getName()));
				npc.broadcastPacket(packet);
				originalCaster.teleToLocation(npc.getLocation());
				npc.setTarget(originalCaster);
				npc.doCast(SkillData.getInstance().getSkill(4064, 1));
			}
		}
	}
	
	@Override
	public void onFactionCall(Npc npc, Npc caller, Player attacker, boolean isSummon)
	{
		if ((caller == null) || (npc == null) || npc.isCastingNow())
		{
			return;
		}
		
		final int npcId = npc.getId();
		final int callerId = caller.getId();
		if ((npcId == RAIKEL_LEOS) && (getRandom(20) == 0))
		{
			npc.setTarget(attacker);
			npc.doCast(SkillData.getInstance().getSkill(4067, 4));
		}
		else if (npcId == RIBA_IREN)
		{
			int chance = 1;
			if (callerId == ORFEN)
			{
				chance = 9;
			}
			if ((callerId != RIBA_IREN) && (caller.getCurrentHp() < (caller.getMaxHp() / 2.0)) && (getRandom(10) < chance))
			{
				npc.getAI().setIntention(Intention.IDLE, null, null);
				npc.setTarget(caller);
				npc.doCast(SkillData.getInstance().getSkill(4516, 1));
			}
		}
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final int npcId = npc.getId();
		if (npcId == ORFEN)
		{
			if (!_hasTeleported && ((npc.getCurrentHp() - damage) < (npc.getMaxHp() / 2)))
			{
				_hasTeleported = true;
				setSpawnPoint(npc, 0);
			}
			else if (npc.isInsideRadius2D(attacker, 1000) && !npc.isInsideRadius2D(attacker, 300) && (getRandom(10) == 0))
			{
				final NpcSay packet = new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npcId, TEXT[getRandom(3)].replace("$s1", attacker.getName()));
				npc.broadcastPacket(packet);
				attacker.teleToLocation(npc.getLocation());
				npc.setTarget(attacker);
				npc.doCast(SkillData.getInstance().getSkill(4064, 1));
			}
		}
		else if ((npcId == RIBA_IREN) && !npc.isCastingNow() && ((npc.getCurrentHp() - damage) < (npc.getMaxHp() / 2.0)))
		{
			npc.setTarget(attacker);
			npc.doCast(SkillData.getInstance().getSkill(4516, 1));
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.getId() == ORFEN)
		{
			npc.broadcastPacket(new PlaySound(1, "BS02_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
			GrandBossManager.getInstance().setStatus(ORFEN, DEAD);
			
			final long baseIntervalMillis = Config.ORFEN_SPAWN_INTERVAL * 3600000;
			final long randomRangeMillis = Config.ORFEN_SPAWN_RANDOM * 3600000;
			final long respawnTime = baseIntervalMillis + getRandom(-randomRangeMillis, randomRangeMillis);
			
			// Next respawn time.
			final long nextRespawnTime = System.currentTimeMillis() + respawnTime;
			LOGGER.info("Orfen will respawn at: " + TimeUtil.getDateTimeString(nextRespawnTime));
			
			startQuestTimer("orfen_unlock", respawnTime, null, null);
			// also save the respawn time so that the info is maintained past reboots
			final StatSet info = GrandBossManager.getInstance().getStatSet(ORFEN);
			info.set("respawn_time", System.currentTimeMillis() + respawnTime);
			GrandBossManager.getInstance().setStatSet(ORFEN, info);
			cancelQuestTimer("check_minion_loc", npc, null);
			cancelQuestTimer("check_orfen_pos", npc, null);
			startQuestTimer("despawn_minions", 20000, null, null);
			cancelQuestTimers("spawn_minion");
		}
		else if ((GrandBossManager.getInstance().getStatus(ORFEN) == ALIVE) && (npc.getId() == RAIKEL_LEOS))
		{
			MINIONS.remove(npc);
			startQuestTimer("spawn_minion", 360000, npc, null);
		}
	}
	
	public static void main(String[] args)
	{
		new Orfen();
	}
}
