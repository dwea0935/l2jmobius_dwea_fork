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
package ai.others;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.util.MinionList;

import ai.AbstractNpcAI;

/**
 * Manages minion's spawn, idle despawn and Teleportation Cube spawn.
 * @author GKR
 */
public class Epidos extends AbstractNpcAI
{
	private static final int[] EPIDOSES =
	{
		25609,
		25610,
		25611,
		25612
	};
	
	private static final int[] MINIONS =
	{
		25605,
		25606,
		25607,
		25608
	};
	
	private static final int[] MINIONS_COUNT =
	{
		3,
		6,
		11
	};
	
	private final Map<Integer, Double> _lastHp = new ConcurrentHashMap<>();
	
	private Epidos()
	{
		addKillId(EPIDOSES);
		addSpawnId(EPIDOSES);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("check_minions"))
		{
			if ((getRandom(1000) > 250) && _lastHp.containsKey(npc.getObjectId()))
			{
				final int hpDecreasePercent = (int) (((_lastHp.get(npc.getObjectId()) - npc.getCurrentHp()) * 100) / npc.getMaxHp());
				int minionsCount = 0;
				final int spawnedMinions = npc.asMonster().getMinionList().countSpawnedMinions();
				
				if ((hpDecreasePercent > 5) && (hpDecreasePercent <= 15) && (spawnedMinions <= 9))
				{
					minionsCount = MINIONS_COUNT[0];
				}
				else if ((((hpDecreasePercent > 1) && (hpDecreasePercent <= 5)) || ((hpDecreasePercent > 15) && (hpDecreasePercent <= 30))) && (spawnedMinions <= 6))
				{
					minionsCount = MINIONS_COUNT[1];
				}
				else if (spawnedMinions == 0)
				{
					minionsCount = MINIONS_COUNT[2];
				}
				
				for (int i = 0; i < minionsCount; i++)
				{
					MinionList.spawnMinion(npc.asMonster(), MINIONS[Arrays.binarySearch(EPIDOSES, npc.getId())]);
				}
				
				_lastHp.put(npc.getObjectId(), npc.getCurrentHp());
			}
			
			startQuestTimer("check_minions", 10000, npc, null);
		}
		else if (event.equalsIgnoreCase("check_idle"))
		{
			if (npc.getAI().getIntention() == Intention.ACTIVE)
			{
				npc.deleteMe();
			}
			else
			{
				startQuestTimer("check_idle", 600000, npc, null);
			}
		}
		return null;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.isInsideRadius3D(-45474, 247450, -13994, 2000))
		{
			addSpawn(32376, -45482, 246277, -14184, 0, false, 0, false);
		}
		
		_lastHp.remove(npc.getObjectId());
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		startQuestTimer("check_minions", 10000, npc, null);
		startQuestTimer("check_idle", 600000, npc, null);
		_lastHp.put(npc.getObjectId(), (double) npc.getMaxHp());
	}
	
	public static void main(String[] args)
	{
		new Epidos();
	}
}
