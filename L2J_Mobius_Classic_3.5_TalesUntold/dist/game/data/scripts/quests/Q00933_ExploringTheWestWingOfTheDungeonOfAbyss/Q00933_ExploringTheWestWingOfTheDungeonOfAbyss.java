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
package quests.Q00933_ExploringTheWestWingOfTheDungeonOfAbyss;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.QuestType;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * @author QuangNguyen
 */
public class Q00933_ExploringTheWestWingOfTheDungeonOfAbyss extends Quest
{
	// NPCs
	private static final int MAGRIT = 31774;
	private static final int INGRIT = 31775;
	// Monsters
	public int MERTT = 21638;
	public int DUHT = 21639;
	public int PRIZT = 21640;
	public int KOVART = 21641;
	// Items
	public ItemHolder OSKZLA = new ItemHolder(90008, 1);
	public ItemHolder POD = new ItemHolder(90136, 1);
	
	public Q00933_ExploringTheWestWingOfTheDungeonOfAbyss()
	{
		super(933);
		addStartNpc(MAGRIT, INGRIT);
		addTalkId(MAGRIT, INGRIT);
		addKillId(MERTT, DUHT, PRIZT, KOVART);
		registerQuestItems(OSKZLA.getId());
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
			case "31774-01.htm":
			case "31774-02.htm":
			case "31774-03.htm":
			case "31775-01.htm":
			case "31775-02.htm":
			case "31775-03.htm":
			{
				htmltext = event;
				break;
			}
			
			case "31774-04.htm":
			{
				if (player.getLevel() >= 40)
				{
					qs.startQuest();
					htmltext = event;
				}
				break;
			}
			case "31775-04.htm":
			{
				if (player.getLevel() >= 40)
				{
					qs.startQuest();
					htmltext = event;
				}
				break;
			}
			case "end.htm":
			{
				player.addExpAndSp(250000, 7700);
				rewardItems(player, POD);
				qs.exitQuest(QuestType.DAILY, true);
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		final QuestState qs = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		switch (qs.getState())
		{
			case State.CREATED:
			{
				htmltext = (talker.getLevel() < 40) ? "nolvl.htm" : "31774-01.htm";
				break;
			}
			case State.STARTED:
			{
				if (npc.getId() == MAGRIT)
				{
					switch (qs.getCond())
					{
						case 0:
						{
							if ((qs.getPlayer().getLevel() >= 40) && (qs.getPlayer().getLevel() <= 46))
							{
								htmltext = "31774-01.htm";
							}
							else
							{
								htmltext = "31774-01a.htm";
							}
							break;
						}
						case 1:
						{
							htmltext = "31774-04.htm";
							break;
						}
						case 2:
						{
							htmltext = "31774-05.htm";
							break;
						}
					}
					break;
				}
				else if (npc.getId() == INGRIT)
				{
					switch (qs.getCond())
					{
						case 0:
						{
							if ((qs.getPlayer().getLevel() >= 40) && (qs.getPlayer().getLevel() <= 46))
							{
								htmltext = "31775-01.htm";
								qs.startQuest();
							}
							else
							{
								htmltext = "31775-01a.htm";
							}
							break;
						}
						case 1:
						{
							htmltext = "31775-04.htm";
							break;
						}
						case 2:
						{
							htmltext = "31775-05.htm";
							break;
						}
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				if (qs.isNowAvailable())
				{
					qs.setState(State.CREATED);
					if ((npc.getId() == MAGRIT) && (qs.getPlayer().getLevel() < 40))
					{
						htmltext = "31774-01.htm";
					}
					else if ((npc.getId() == INGRIT) && (qs.getPlayer().getLevel() < 40))
					{
						htmltext = "31775-01.htm";
					}
					else
					{
						htmltext = "nolvl.htm";
					}
				}
				else
				{
					htmltext = getAlreadyCompletedMsg(talker);
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && (qs.getCond() == 1))
		{
			if (getQuestItemsCount(killer, OSKZLA.getId()) < 50)
			{
				giveItems(killer, OSKZLA);
			}
			if (getQuestItemsCount(killer, OSKZLA.getId()) >= 50)
			{
				qs.setCond(2);
			}
		}
	}
}