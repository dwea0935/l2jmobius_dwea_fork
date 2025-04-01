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
package ai.areas.Gracia.AI.NPC.Seyo;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.enums.ChatType;

import ai.AbstractNpcAI;

/**
 * Seyo AI.
 * @author St3eT
 */
public class Seyo extends AbstractNpcAI
{
	// NPC
	private static final int SEYO = 32737;
	// Item
	private static final int STONE_FRAGMENT = 15486; // Spirit Stone Fragment
	// Misc
	private static final String[] TEXT =
	{
		"No one else? Don't worry~ I don't bite. Haha~!",
		"OK~ Master of luck? That's you? Haha~! Well, anyone can come after all.",
		"Shedding blood is a given on the battlefield. At least it's safe here.",
		"OK~ Who's next? It all depends on your fate and luck, right? At least come and take a look.",
		"There was someone who won 10,000 from me. A warrior shouldn't just be good at fighting, right? You've gotta be good in everything."
	};
	
	public Seyo()
	{
		addStartNpc(SEYO);
		addTalkId(SEYO);
		addFirstTalkId(SEYO);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		if (npc == null)
		{
			return htmltext;
		}
		switch (event)
		{
			case "TRICKERY_TIMER":
			{
				if (npc.isScriptValue(1))
				{
					npc.setScriptValue(0);
					npc.broadcastSay(ChatType.NPC_GENERAL, getRandomEntry(TEXT));
				}
				break;
			}
			case "give1":
			{
				if (npc.isScriptValue(1))
				{
					htmltext = "32737-04.html";
				}
				else if (!hasQuestItems(player, STONE_FRAGMENT))
				{
					htmltext = "32737-01.html";
				}
				else
				{
					npc.setScriptValue(1);
					takeItems(player, STONE_FRAGMENT, 1);
					if (getRandom(100) == 0)
					{
						giveItems(player, STONE_FRAGMENT, 100);
						npc.broadcastSay(ChatType.NPC_GENERAL, "Amazing. " + player.getName() + " took 100 of these soul stone fragments. What a complete swindler.");
					}
					else
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, "Hmm? Hey, did you give " + player.getName() + " something? But it was just 1. Haha.");
					}
					startQuestTimer("TRICKERY_TIMER", 5000, npc, null);
				}
				break;
			}
			case "give5":
			{
				if (npc.isScriptValue(1))
				{
					htmltext = "32737-04.html";
				}
				else if (getQuestItemsCount(player, STONE_FRAGMENT) < 5)
				{
					htmltext = "32737-02.html";
				}
				else
				{
					npc.setScriptValue(1);
					takeItems(player, STONE_FRAGMENT, 5);
					final int chance = getRandom(100);
					if (chance < 20)
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, "Ahem~! " + player.getName() + " has no luck at all. Try praying.");
					}
					else if (chance < 80)
					{
						giveItems(player, STONE_FRAGMENT, 1);
						npc.broadcastSay(ChatType.NPC_GENERAL, "It's better than losing it all, right? Or does this feel worse?");
					}
					else
					{
						final int itemCount = getRandom(10, 16);
						giveItems(player, STONE_FRAGMENT, itemCount);
						npc.broadcastSay(ChatType.NPC_GENERAL, player.getName() + " pulled one with " + itemCount + " digits. Lucky~ Not bad~");
					}
					startQuestTimer("TRICKERY_TIMER", 5000, npc, null);
				}
				break;
			}
			case "give20":
			{
				if (npc.isScriptValue(1))
				{
					htmltext = "32737-04.html";
				}
				else if (getQuestItemsCount(player, STONE_FRAGMENT) < 20)
				{
					htmltext = "32737-03.html";
				}
				else
				{
					npc.setScriptValue(1);
					takeItems(player, STONE_FRAGMENT, 20);
					final int chance = getRandom(10000);
					if (chance == 0)
					{
						giveItems(player, STONE_FRAGMENT, 10000);
						npc.broadcastSay(ChatType.NPC_GENERAL, "Ah... It's over. What kind of guy is that? Damn... Fine, you " + player.getName() + ", take it and get outta here.");
					}
					else if (chance < 10)
					{
						giveItems(player, STONE_FRAGMENT, 1);
						npc.broadcastSay(ChatType.NPC_GENERAL, "You don't feel bad, right? Are you sad? But don't cry~");
					}
					else
					{
						giveItems(player, STONE_FRAGMENT, getRandom(1, 100));
						npc.broadcastSay(ChatType.NPC_GENERAL, "A big piece is made up of little pieces. So here's a little piece~");
					}
					startQuestTimer("TRICKERY_TIMER", 5000, npc, null);
				}
				break;
			}
		}
		return htmltext;
	}
}