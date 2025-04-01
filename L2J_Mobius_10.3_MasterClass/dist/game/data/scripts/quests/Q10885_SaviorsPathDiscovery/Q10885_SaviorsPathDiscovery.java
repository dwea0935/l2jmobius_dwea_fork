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
package quests.Q10885_SaviorsPathDiscovery;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.enums.Movie;

/**
 * Savior's Path - Discovery (10885)
 * @URL https://l2wiki.com/Savior%27s_Path_-_Discovery
 * @author CostyKiller
 */
public class Q10885_SaviorsPathDiscovery extends Quest
{
	// NPCs
	private static final int LEONA_BLACKBIRD = 34425;
	private static final int ELIKIA = 34057;
	// Item
	private static final int LEONA_BLACKBIRDS_MESSAGE = 48545;
	// Misc
	private static final int MIN_LEVEL = 103;
	
	public Q10885_SaviorsPathDiscovery()
	{
		super(10885);
		addStartNpc(ELIKIA);
		addTalkId(LEONA_BLACKBIRD, ELIKIA);
		addCondMinLevel(MIN_LEVEL, "34057-00.html");
		registerQuestItems(LEONA_BLACKBIRDS_MESSAGE);
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
			case "34057-02.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "34057-03.htm":
			{
				if (qs.isCond(1))
				{
					qs.setCond(2);
				}
				htmltext = event;
				break;
			}
			case "34057-04.html":
			{
				if (qs.isCond(2))
				{
					giveItems(player, LEONA_BLACKBIRDS_MESSAGE, 1, true);
					playMovie(player, Movie.EP5_ASTATINE_QST_START);
					qs.setCond(3);
				}
				htmltext = event;
				break;
			}
			case "34425-03.html":
			{
				if (qs.isCond(3))
				{
					if ((player.getLevel() >= MIN_LEVEL))
					{
						takeItems(player, -1, LEONA_BLACKBIRDS_MESSAGE);
						addExpAndSp(player, 90638, 82);
						qs.exitQuest(false, true);
						htmltext = event;
					}
					else
					{
						htmltext = getNoQuestLevelRewardMsg(player);
					}
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
				if (npc.getId() == ELIKIA)
				{
					htmltext = "34057-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case LEONA_BLACKBIRD:
					{
						if (qs.isCond(3))
						{
							htmltext = "34425-01.htm";
						}
						else
						{
							htmltext = "34425-02.html";
						}
						break;
					}
					case ELIKIA:
					{
						if (qs.isCond(1))
						{
							htmltext = "34057-01.htm";
						}
						else if (qs.isCond(2))
						{
							htmltext = "34057-03.htm";
						}
						else if (qs.isCond(3))
						{
							htmltext = "34057-04.html";
						}
						break;
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				htmltext = getAlreadyCompletedMsg(player);
				break;
			}
		}
		return htmltext;
	}
}
