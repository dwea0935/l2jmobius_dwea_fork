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
package ai.areas.TalkingIsland;

import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.util.LocationUtil;

import ai.AbstractNpcAI;

/**
 * Eleve AI.
 * @author St3eT
 */
public class Eleve extends AbstractNpcAI
{
	// NPC
	private static final int ELEVE = 33246;
	
	private Eleve()
	{
		addSpawnId(ELEVE);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("NPC_MOVE"))
		{
			if (getRandomBoolean())
			{
				final Location randomLoc = LocationUtil.getRandomLocation(npc.getSpawn().getLocation(), 0, 500);
				addMoveToDesire(npc, GeoEngine.getInstance().getValidLocation(npc.getLocation().getX(), npc.getLocation().getY(), npc.getLocation().getZ(), randomLoc.getX(), randomLoc.getY(), randomLoc.getZ(), npc.getInstanceWorld()), 23);
			}
			startQuestTimer("NPC_MOVE", (10 + getRandom(5)) * 1000, npc, null);
		}
		else if (event.equals("NPC_SHOUT"))
		{
			final int rand = getRandom(3);
			if (rand == 0)
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.DON_T_KNOW_WHAT_TO_DO_LOOK_AT_THE_MAP);
			}
			else if (rand == 1)
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.DO_YOU_SEE_A_SCROLL_ICON_GO_THAT_LOCATION);
			}
			startQuestTimer("NPC_SHOUT", (10 + getRandom(5)) * 1000, npc, null);
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		startQuestTimer("NPC_MOVE", (10 + getRandom(5)) * 1000, npc, null);
		startQuestTimer("NPC_SHOUT", (10 + getRandom(5)) * 1000, npc, null);
	}
	
	public static void main(String[] args)
	{
		new Eleve();
	}
}