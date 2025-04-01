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
package quests.Q00127_FishingSpecialistsRequest;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;

/**
 * Fishing Specialist's Request (127)
 * @author Mobius
 */
public class Q00127_FishingSpecialistsRequest extends Quest
{
	// NPCs
	private static final int PIERRE = 30013;
	private static final int FERMA = 30015;
	private static final int BAIKAL = 30016;
	// Items
	private static final int PIERRE_LETTER = 49510;
	private static final int FISH_REPORT = 49504;
	private static final int SEALED_BOTTLE = 49505;
	private static final int FISHING_ROD_CHEST = 49507;
	// Location
	private static final Location TELEPORT_LOC = new Location(105276, 162500, -3600);
	// Misc
	private static final int MIN_LEVEL = 20;
	
	public Q00127_FishingSpecialistsRequest()
	{
		super(127);
		addStartNpc(PIERRE);
		addTalkId(PIERRE, FERMA, BAIKAL);
		addCondMinLevel(MIN_LEVEL, "30013-00.htm");
		registerQuestItems(PIERRE_LETTER, FISH_REPORT, SEALED_BOTTLE);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = null;
		switch (event)
		{
			case "30013-02.html":
			{
				qs.startQuest();
				giveItems(player, PIERRE_LETTER, 1);
				htmltext = event;
				break;
			}
			case "teleport_to_ferma":
			{
				player.teleToLocation(TELEPORT_LOC);
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		if (qs.isCreated())
		{
			if (npc.getId() == PIERRE)
			{
				htmltext = player.getLevel() < MIN_LEVEL ? "30013-00.htm" : "30013-01.htm";
			}
		}
		else if (qs.isStarted())
		{
			switch (npc.getId())
			{
				case PIERRE:
				{
					switch (qs.getCond())
					{
						case 1:
						case 2:
						{
							htmltext = "30013-03.html";
							break;
						}
						case 3:
						{
							takeItems(player, -1, SEALED_BOTTLE);
							giveItems(player, FISHING_ROD_CHEST, 1);
							qs.exitQuest(false, true);
							htmltext = "30013-04.html";
							break;
						}
					}
					break;
				}
				case FERMA:
				{
					switch (qs.getCond())
					{
						case 1:
						{
							takeItems(player, -1, PIERRE_LETTER);
							giveItems(player, FISH_REPORT, 1);
							qs.setCond(2, true);
							htmltext = "30015-01.html";
							break;
						}
						case 2:
						{
							htmltext = "30015-02.html";
							break;
						}
						case 3:
						{
							htmltext = "30015-03.html";
							break;
						}
					}
					break;
				}
				case BAIKAL:
				{
					switch (qs.getCond())
					{
						case 2:
						{
							takeItems(player, -1, FISH_REPORT);
							giveItems(player, SEALED_BOTTLE, 1);
							qs.setCond(3, true);
							htmltext = "30016-01.html";
							break;
						}
						case 3:
						{
							htmltext = "30016-02.html";
							break;
						}
					}
					break;
				}
			}
		}
		else if (qs.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
		}
		return htmltext;
	}
}
