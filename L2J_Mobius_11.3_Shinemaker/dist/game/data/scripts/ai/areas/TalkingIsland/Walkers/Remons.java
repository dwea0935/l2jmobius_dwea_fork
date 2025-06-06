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
package ai.areas.TalkingIsland.Walkers;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;

import ai.AbstractNpcAI;

/**
 * Remons AI.
 * @author Gladicek
 */
public class Remons extends AbstractNpcAI
{
	// NPC
	private static final int REMONS = 33570;
	private static final int SOROS = 33218;
	// Distances
	private static final int MIN_DISTANCE = 70;
	private static final int MAX_DISTANCE = 200;
	
	private Remons()
	{
		addSpawnId(REMONS);
	}
	
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		switch (event)
		{
			case "NPC_SHOUT":
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.MAY_THE_VILLAGE_ALSO_BE_IN_DANGER);
				getTimers().addTimer("NPC_SHOUT", (10 + getRandom(5)) * 1000, npc, null);
				break;
			}
			case "WALK_AROUND_SOROS":
			{
				followNpc(npc, SOROS, 240, MIN_DISTANCE, MAX_DISTANCE);
				break;
			}
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		followNpc(npc, SOROS, 240, MIN_DISTANCE, MAX_DISTANCE);
		getTimers().addTimer("NPC_SHOUT", (10 + getRandom(5)) * 1000, npc, null);
		getTimers().addRepeatingTimer("WALK_AROUND_SOROS", 1000, npc, null);
	}
	
	public static void main(String[] args)
	{
		new Remons();
	}
}