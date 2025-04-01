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
package quests.Q00577_BasicMissionSilentValley;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Faction;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.QuestType;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Basic Mission: Silent Valley (577)
 * @URL https://l2wiki.com/Basic_Mission:_Silent_Valley
 * @author Dmitri
 */
public class Q00577_BasicMissionSilentValley extends Quest
{
	// NPCs
	private static final int PENNY = 34413;
	private static final int CORZET = 34424;
	// Rewards
	private static final int SCROLL_OF_ESCAPE_TOWN_OF_ADEN = 48413;
	// Misc
	private static final int MIN_LEVEL = 95;
	private static final int MAX_LEVEL = 97;
	// Location
	private static final Location TOWN_OF_ADEN = new Location(146632, 26760, -2213);
	
	public Q00577_BasicMissionSilentValley()
	{
		super(577);
		addStartNpc(PENNY);
		addTalkId(PENNY, CORZET);
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "nolevel.html");
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
			case "34413-02.htm":
			case "34413-03.htm":
			case "33780-02.html":
			{
				htmltext = event;
				break;
			}
			case "34413-04.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "33780-03.html": // CORZET
			{
				qs.setCond(2, true);
				htmltext = event;
				break;
			}
			case "33780-05.html": // CORZET
			{
				giveItems(player, SCROLL_OF_ESCAPE_TOWN_OF_ADEN, 1);
				htmltext = event;
				break;
			}
			case "usescroll":
			{
				// TODO: force player to use item SCROLL_OF_ESCAPE_TOWN_OF_ADEN
				player.teleToLocation(TOWN_OF_ADEN); // Town of Aden near Npc Penny - temp solution
				takeItems(player, SCROLL_OF_ESCAPE_TOWN_OF_ADEN, -1); // remove SOE - temp solution
				break;
			}
			case "34413-07.html":
			{
				final StringBuilder str = new StringBuilder("00");
				checkQuestCompleted(player, str); // Initialize the array with all quests completed
				if (str.indexOf("11") != -1) // verify if all quests completed
				{
					addExpAndSp(player, 1793099880L, 1793070);
					addFactionPoints(player, Faction.ADVENTURE_GUILD, 200); // add FP points to ADVENTURE_GUILD Faction
					qs.exitQuest(QuestType.DAILY, true);
					htmltext = event;
				}
				else
				{
					htmltext = "34413-05.html";
				}
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
		switch (qs.getState())
		{
			case State.CREATED:
			{
				if (npc.getId() == PENNY)
				{
					htmltext = "34413-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case PENNY:
					{
						if ((qs.getCond() >= 1) && (qs.getCond() <= 2))
						{
							htmltext = "34413-05.html";
						}
						else if (qs.getCond() == 3)
						{
							htmltext = "34413-06.html";
						}
						break;
					}
					case CORZET:
					{
						if (qs.getCond() == 1)
						{
							htmltext = "33780-01.html";
						}
						else if (qs.getCond() == 2)
						{
							final StringBuilder str = new StringBuilder("00");
							checkQuestCompleted(player, str); // Initialize the array with all quests completed
							if (str.indexOf("11") != -1) // verify if all quests completed
							{
								qs.setCond(3, true);
								htmltext = "33780-04.html";
							}
							else
							{
								htmltext = "33780-03.html";
							}
						}
						break;
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				if (qs.isNowAvailable())
				{
					qs.setState(State.CREATED);
					htmltext = "34413-01.htm";
				}
				else
				{
					htmltext = getAlreadyCompletedMsg(player, QuestType.DAILY);
				}
				break;
			}
		}
		return htmltext;
	}
	
	private StringBuilder checkQuestCompleted(Player player, StringBuilder string)
	{
		int index = 0;
		final char ch = '1';
		final QuestState st1 = player.getQuestState("Q00589_ASecretChange");
		if ((st1 != null) && st1.isCompleted())
		{
			index = 0;
			string.setCharAt(index, ch);
		}
		final QuestState st2 = player.getQuestState("Q00590_ToEachTheirOwn");
		if ((st2 != null) && st2.isCompleted())
		{
			index = 1;
			string.setCharAt(index, ch);
		}
		return string;
	}
}
