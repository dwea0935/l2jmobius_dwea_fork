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
package ai.areas.DungeonOfAbyss.SoulTracker;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.QuestState;

import ai.AbstractNpcAI;

/**
 * @author QuangNguyen
 */
public class Magrit extends AbstractNpcAI
{
	// NPC
	private static final int SOUL_TRACKER_MARGIT = 31774;
	// Item
	private static final int KEY_OF_WEST_WING = 90010;
	// Locations
	private static final Map<String, Location> LOCATIONS = new HashMap<>();
	static
	{
		LOCATIONS.put("1", new Location(-119440, -182464, -6752)); // Join Room from Magrit
		LOCATIONS.put("2", new Location(-120394, -179651, -6751)); // Move to West Wing 2nd
		LOCATIONS.put("3", new Location(-116963, -181492, -6575)); // Go to the Condemned of Abyss Prison
		LOCATIONS.put("4", new Location(146945, 26764, -2200)); // Return to Aden
	}
	
	private Magrit()
	{
		addStartNpc(SOUL_TRACKER_MARGIT);
		addTalkId(SOUL_TRACKER_MARGIT);
		addFirstTalkId(SOUL_TRACKER_MARGIT);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".htm";
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (npc.getId() == SOUL_TRACKER_MARGIT)
		{
			final QuestState qs = player.getQuestState("Q00933_ExploringTheWestWingOfTheDungeonOfAbyss");
			switch (event)
			{
				case "1":
				{
					if ((qs != null) && qs.isStarted())
					{
						player.teleToLocation(LOCATIONS.get(event), false); // Join Room from Magrit
					}
					else
					{
						return "no_enter.htm";
					}
					break;
				}
				case "2":
				{
					player.teleToLocation(LOCATIONS.get(event), false); // Move to West Wing 2nd
					break;
				}
				case "3":
				{
					if (!hasQuestItems(player, KEY_OF_WEST_WING))
					{
						return "no_key.htm";
					}
					player.teleToLocation(LOCATIONS.get(event), false); // Go to the Condemned of Abyss Prison
					break;
				}
				case "4":
				{
					player.teleToLocation(LOCATIONS.get(event), false); // Return to Aden
					break;
				}
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	public static void main(String[] args)
	{
		new Magrit();
	}
}