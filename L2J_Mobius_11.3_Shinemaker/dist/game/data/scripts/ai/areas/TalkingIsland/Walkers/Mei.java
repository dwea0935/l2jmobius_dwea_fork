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
 * Mei AI.
 * @author Gladicek
 */
public class Mei extends AbstractNpcAI
{
	// NPC
	private static final int MEI = 33280;
	private static final int ROTINA = 33027;
	
	private Mei()
	{
		addSpawnId(MEI);
	}
	
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		if (event.equals("NPC_SHOUT"))
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.IF_YOU_IGNORE_THE_TRAINING_GROUNDS_YOU_LL_REGRET_IT);
			getTimers().addTimer("NPC_SHOUT", (10 + getRandom(5)) * 1000, npc, null);
		}
		else if (event.equals("NPC_FOLLOW"))
		{
			addSpawn(ROTINA, npc.getX() + 10, npc.getY() + 10, npc.getZ() + 20, 0, false, 0);
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		getTimers().addTimer("NPC_SHOUT", (10 + getRandom(5)) * 1000, npc, null);
		getTimers().addTimer("NPC_FOLLOW", 100, npc, null);
	}
	
	public static void main(String[] args)
	{
		new Mei();
	}
}