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

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;

import ai.AbstractNpcAI;

/**
 * Oris AI.
 * @author Gladicek
 */
public class Oris extends AbstractNpcAI
{
	// NPCs
	private static final int ORIS = 33116;
	
	private Oris()
	{
		addSpawnId(ORIS);
		addStartNpc(ORIS);
		addTalkId(ORIS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("SOCIAL_ACTION_1"))
		{
			npc.broadcastSocialAction(6);
			startQuestTimer("SOCIAL_ACTION_2", 2500, npc, null);
		}
		else if (event.equals("SOCIAL_ACTION_2"))
		{
			npc.broadcastSocialAction(7);
		}
		else if (event.equals("SPAM_TEXT") && (npc != null))
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.I_HAVEN_T_FELT_THIS_GOOD_IN_AGES, 1000);
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		npc.setRandomAnimation(false);
		startQuestTimer("SOCIAL_ACTION_1", 6500, npc, null, true);
		startQuestTimer("SPAM_TEXT", 10000, npc, null, true);
	}
	
	public static void main(String[] args)
	{
		new Oris();
	}
}