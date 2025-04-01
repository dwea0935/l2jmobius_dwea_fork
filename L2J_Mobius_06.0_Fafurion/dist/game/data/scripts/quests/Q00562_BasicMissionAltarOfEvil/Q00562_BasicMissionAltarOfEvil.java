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
package quests.Q00562_BasicMissionAltarOfEvil;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Faction;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.QuestType;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Q00562_BasicMissionAltarOfEvil
 * @URL https://l2wiki.com/Basic_Mission:_Altar_of_Evil
 * @author NightBR
 */
public class Q00562_BasicMissionAltarOfEvil extends Quest
{
	// NPCs
	private static final int PENNY = 34413;
	private static final int LAPATHIA = 34414;
	private static final int ELISA = 30848;
	private static final int BELLA = 30256;
	private static final int DE_VILLAGE_TELEPORT_DEVICE = 30134;
	// Rewards
	private static final long EXP = 231860550;
	private static final int SP = 231840;
	private static final int FP = 200; // Faction points
	private static final int SCROLL_OF_ESCAPE_BLOODY_SWAMPLAND = 39494;
	private static final int SCROLL_OF_ESCAPE_TOWN_OF_ADEN = 48413;
	// Misc
	private static final int MIN_LEVEL = 85;
	private static final int MAX_LEVEL = 88;
	// Location
	private static final Location TOWN_OF_ADEN = new Location(146632, 26760, -2213);
	
	public Q00562_BasicMissionAltarOfEvil()
	{
		super(562);
		addStartNpc(PENNY);
		addTalkId(PENNY, LAPATHIA, ELISA, BELLA, DE_VILLAGE_TELEPORT_DEVICE);
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
			case "34413-06.html":
			case "30256-02.html":
			case "34414-03.html":
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
			case "34413-05.html":
			{
				qs.setCond(5, true);
				htmltext = event;
				break;
			}
			case "34413-09.html":
			{
				qs.setCond(2, true);
				htmltext = event;
				break;
			}
			case "34413-07.html":
			{
				final StringBuilder str = new StringBuilder("00");
				checkQuestCompleted(player, str); // Initialize the array with all quests completed
				if (str.indexOf("11") != -1) // verify if all quests completed
				{
					giveItems(player, SCROLL_OF_ESCAPE_BLOODY_SWAMPLAND, 1);
					addExpAndSp(player, EXP, SP);
					addFactionPoints(player, Faction.ADVENTURE_GUILD, FP); // add FP points to ADVENTURE_GUILD Faction
					qs.exitQuest(QuestType.DAILY, true);
					htmltext = event;
				}
				else
				{
					htmltext = "34413-08.html";
				}
				break;
			}
			case "30848-02.html": // ELISA
			{
				qs.setCond(3, true);
				htmltext = event;
				break;
			}
			case "30256-03.html": // BELLA
			{
				qs.setCond(4, true);
				htmltext = event;
				break;
			}
			case "34414-04.html": // LEPATHIA
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
				qs.setCond(8, true);
				break;
			}
			case "keepscroll":
			{
				qs.setCond(8, true);
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
						if (qs.getCond() == 1)
						{
							htmltext = "34413-04.htm";
						}
						else if (qs.getCond() == 2)
						{
							// htmltext = qs.getCond() == 2 ? "34413-10.htm" : "34413-06.html";
							htmltext = "33509-10.htm";
						}
						else if (qs.getCond() == 5)
						{
							// htmltext = qs.getCond() == 5 ? "34413-08.htm" : "34413-06.html";
							htmltext = "34413-08.html";
						}
						else
						{
							htmltext = "34413-06.html";
						}
						break;
					}
					case ELISA:
					{
						htmltext = "30848-01.html";
						break;
					}
					case BELLA:
					{
						htmltext = "30256-01.html";
						break;
					}
					case DE_VILLAGE_TELEPORT_DEVICE:
					{
						qs.setCond(5, true);
						htmltext = "30134-01.html";
						break;
					}
					case LAPATHIA:
					{
						if (qs.getCond() == 5)
						{
							qs.setCond(6, true);
							htmltext = "34414-01.html";
						}
						else
						{
							final StringBuilder str = new StringBuilder("00");
							checkQuestCompleted(player, str); // Initialize the array with all quests completed
							if (str.indexOf("11") != -1) // verify if all quests completed
							{
								qs.setCond(7, true);
								htmltext = "34414-02.html";
							}
							else
							{
								htmltext = "34414-01.html";
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
		final QuestState st1 = player.getQuestState("Q00581_ThePurificationRitual");
		if ((st1 != null) && st1.isCompleted())
		{
			index = 0;
			string.setCharAt(index, ch);
		}
		final QuestState st2 = player.getQuestState("Q00582_WashBloodWithBlood");
		if ((st2 != null) && st2.isCompleted())
		{
			index = 1;
			string.setCharAt(index, ch);
		}
		return string;
	}
}
