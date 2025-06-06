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
package ai.bosses.Core;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;

import ai.AbstractNpcAI;

/**
 * Core AI.
 * @author DrLecter, Emperorc, Mobius
 */
public class Core extends AbstractNpcAI
{
	// NPCs
	private static final int CORE = 29006;
	private static final int DEATH_KNIGHT = 29007;
	private static final int DOOM_WRAITH = 29008;
	private static final int SUSCEPTOR = 29011;
	// Spawns
	private static final Map<Integer, Location> MINNION_SPAWNS = new HashMap<>();
	static
	{
		MINNION_SPAWNS.put(DEATH_KNIGHT, new Location(17191, 109298, -6488));
		MINNION_SPAWNS.put(DEATH_KNIGHT, new Location(17564, 109548, -6488));
		MINNION_SPAWNS.put(DEATH_KNIGHT, new Location(17855, 109552, -6488));
		MINNION_SPAWNS.put(DEATH_KNIGHT, new Location(18280, 109202, -6488));
		MINNION_SPAWNS.put(DEATH_KNIGHT, new Location(18784, 109253, -6488));
		MINNION_SPAWNS.put(DEATH_KNIGHT, new Location(18059, 108314, -6488));
		MINNION_SPAWNS.put(DEATH_KNIGHT, new Location(17300, 108444, -6488));
		MINNION_SPAWNS.put(DEATH_KNIGHT, new Location(17148, 110071, -6648));
		MINNION_SPAWNS.put(DEATH_KNIGHT, new Location(18318, 110077, -6648));
		MINNION_SPAWNS.put(DEATH_KNIGHT, new Location(17726, 110391, -6648));
		MINNION_SPAWNS.put(DOOM_WRAITH, new Location(17113, 110970, -6648));
		MINNION_SPAWNS.put(DOOM_WRAITH, new Location(17496, 110880, -6648));
		MINNION_SPAWNS.put(DOOM_WRAITH, new Location(18061, 110990, -6648));
		MINNION_SPAWNS.put(DOOM_WRAITH, new Location(18384, 110698, -6648));
		MINNION_SPAWNS.put(DOOM_WRAITH, new Location(17993, 111458, -6584));
		MINNION_SPAWNS.put(SUSCEPTOR, new Location(17297, 111470, -6584));
		MINNION_SPAWNS.put(SUSCEPTOR, new Location(17893, 110198, -6648));
		MINNION_SPAWNS.put(SUSCEPTOR, new Location(17706, 109423, -6488));
		MINNION_SPAWNS.put(SUSCEPTOR, new Location(17849, 109388, -6480));
	}
	// Misc
	private static final byte ALIVE = 0;
	private static final byte DEAD = 1;
	private static final Collection<Attackable> _minions = ConcurrentHashMap.newKeySet();
	private static boolean _firstAttacked;
	
	private Core()
	{
		registerMobs(CORE, DEATH_KNIGHT, DOOM_WRAITH, SUSCEPTOR);
		_firstAttacked = false;
		final StatSet info = GrandBossManager.getInstance().getStatSet(CORE);
		if (GrandBossManager.getInstance().getStatus(CORE) == DEAD)
		{
			// Load the unlock date and time for Core from DB.
			final long temp = info.getLong("respawn_time") - System.currentTimeMillis();
			// If Core is locked until a certain time, mark it so and start the unlock timer the unlock time has not yet expired.
			if (temp > 0)
			{
				startQuestTimer("core_unlock", temp, null, null);
			}
			else
			{
				// The time has already expired while the server was offline. Immediately spawn Core.
				final GrandBoss core = (GrandBoss) addSpawn(CORE, 17726, 108915, -6480, 0, false, 0);
				GrandBossManager.getInstance().setStatus(CORE, ALIVE);
				spawnBoss(core);
			}
		}
		else
		{
			final boolean test = GlobalVariablesManager.getInstance().getBoolean("Core_Attacked", false);
			if (test)
			{
				_firstAttacked = true;
			}
			final int loc_x = info.getInt("loc_x");
			final int loc_y = info.getInt("loc_y");
			final int loc_z = info.getInt("loc_z");
			final int heading = info.getInt("heading");
			final double hp = info.getDouble("currentHP");
			final double mp = info.getDouble("currentMP");
			final GrandBoss core = (GrandBoss) addSpawn(CORE, loc_x, loc_y, loc_z, heading, false, 0);
			core.setCurrentHpMp(hp, mp);
			spawnBoss(core);
		}
	}
	
	@Override
	public void onSave()
	{
		GlobalVariablesManager.getInstance().set("Core_Attacked", _firstAttacked);
	}
	
	public void spawnBoss(GrandBoss npc)
	{
		GrandBossManager.getInstance().addBoss(npc);
		npc.broadcastPacket(new PlaySound(1, "BS01_A", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
		// Spawn minions
		Attackable mob;
		Location spawnLocation;
		for (Entry<Integer, Location> spawn : MINNION_SPAWNS.entrySet())
		{
			spawnLocation = spawn.getValue();
			mob = addSpawn(spawn.getKey(), spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ(), getRandom(61794), false, 0).asAttackable();
			mob.setIsRaidMinion(true);
			_minions.add(mob);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("core_unlock"))
		{
			final GrandBoss core = (GrandBoss) addSpawn(CORE, 17726, 108915, -6480, 0, false, 0);
			GrandBossManager.getInstance().setStatus(CORE, ALIVE);
			spawnBoss(core);
		}
		else if (event.equalsIgnoreCase("spawn_minion"))
		{
			final Attackable mob = addSpawn(npc.getId(), npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), false, 0).asAttackable();
			mob.setIsRaidMinion(true);
			_minions.add(mob);
		}
		else if (event.equalsIgnoreCase("despawn_minions"))
		{
			_minions.forEach(Attackable::decayMe);
			_minions.clear();
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (npc.getId() == CORE)
		{
			if (_firstAttacked)
			{
				if (getRandom(100) == 0)
				{
					npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.REMOVING_INTRUDERS);
				}
			}
			else
			{
				_firstAttacked = true;
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.A_NON_PERMITTED_TARGET_HAS_BEEN_DISCOVERED);
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.INTRUDER_REMOVAL_SYSTEM_INITIATED);
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.getId() == CORE)
		{
			npc.broadcastPacket(new PlaySound(1, "BS02_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.A_FATAL_ERROR_HAS_OCCURRED);
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.SYSTEM_IS_BEING_SHUT_DOWN);
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.EMPTY);
			_firstAttacked = false;
			addSpawn(900103, 16502, 110165, -6394, 0, false, 900000);
			addSpawn(900103, 18948, 110166, -6397, 0, false, 900000);
			GrandBossManager.getInstance().setStatus(CORE, DEAD);
			
			final long baseIntervalMillis = Config.CORE_SPAWN_INTERVAL * 3600000;
			final long randomRangeMillis = Config.CORE_SPAWN_RANDOM * 3600000;
			final long respawnTime = baseIntervalMillis + getRandom(-randomRangeMillis, randomRangeMillis);
			startQuestTimer("core_unlock", respawnTime, null, null);
			// Also save the respawn time so that the info is maintained past reboots.
			final StatSet info = GrandBossManager.getInstance().getStatSet(CORE);
			info.set("respawn_time", System.currentTimeMillis() + respawnTime);
			GrandBossManager.getInstance().setStatSet(CORE, info);
			startQuestTimer("despawn_minions", 20000, null, null);
			cancelQuestTimers("spawn_minion");
		}
		else if ((GrandBossManager.getInstance().getStatus(CORE) == ALIVE) && _minions.contains(npc))
		{
			_minions.remove(npc);
			startQuestTimer("spawn_minion", 60000, npc, null);
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.getId() == CORE)
		{
			npc.setImmobilized(true);
		}
	}
	
	public static void main(String[] args)
	{
		new Core();
	}
}
