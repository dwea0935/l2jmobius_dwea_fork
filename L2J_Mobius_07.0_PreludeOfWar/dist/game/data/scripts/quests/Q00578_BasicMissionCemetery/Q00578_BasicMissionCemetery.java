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
package quests.Q00578_BasicMissionCemetery;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Faction;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.QuestType;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Basic Mission: Cemetery (578)
 * @URL https://l2wiki.com/Basic_Mission:_Cemetery
 * @author Dmitri
 */
public class Q00578_BasicMissionCemetery extends Quest
{
	// NPCs
	private static final int PENNY = 34413;
	private static final int QUTAERMASTER = 33407;
	// Rewards
	private static final int SCROLL_OF_ESCAPE_CEMETERY = 47062;
	private static final int SCROLL_OF_ESCAPE_TOWN_OF_ADEN = 48413;
	// Misc
	private static final int MIN_LEVEL = 97;
	private static final int MAX_LEVEL = 99;
	
	public Q00578_BasicMissionCemetery()
	{
		super(578);
		addStartNpc(PENNY);
		addTalkId(PENNY, QUTAERMASTER);
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
			case "33407-02.html":
			{
				qs.setCond(2, true);
				htmltext = event;
				break;
			}
			case "34413-07.html":
			{
				// Rewards
				giveItems(player, SCROLL_OF_ESCAPE_CEMETERY, 1);
				addExpAndSp(player, 1346064975L, 1346055);
				addFactionPoints(player, Faction.ADVENTURE_GUILD, 140); // add FP points to ADVENTURE_GUILD Faction
				qs.exitQuest(QuestType.DAILY, true);
				htmltext = event;
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
							htmltext = "33509-05.html";
						}
						else if (qs.getCond() == 3)
						{
							htmltext = "34413-06.html";
						}
						break;
					}
					case QUTAERMASTER:
					{
						if (qs.getCond() == 1)
						{
							htmltext = "33407-01.html";
						}
						else if (qs.getCond() == 2)
						{
							final QuestState st = player.getQuestState("Q00758_TheFallenKingsMen");
							if ((st != null) && st.isCompleted())
							{
								qs.setCond(3, true);
								giveItems(player, SCROLL_OF_ESCAPE_TOWN_OF_ADEN, 1);
								htmltext = null;
							}
							else
							{
								htmltext = "33407-03.html";
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
}
