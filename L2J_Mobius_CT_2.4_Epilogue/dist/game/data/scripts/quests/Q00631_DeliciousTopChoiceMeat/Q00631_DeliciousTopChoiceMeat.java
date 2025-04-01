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
package quests.Q00631_DeliciousTopChoiceMeat;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

public class Q00631_DeliciousTopChoiceMeat extends Quest
{
	// NPC
	private static final int TUNATUN = 31537;
	
	// Item
	private static final int TOP_QUALITY_MEAT = 7546;
	
	// Drop chances
	private static final Map<Integer, Double> CHANCES = new HashMap<>();
	static
	{
		CHANCES.put(21460, 0.600);
		CHANCES.put(21461, 0.480);
		CHANCES.put(21462, 0.447);
		CHANCES.put(21463, 0.808);
		CHANCES.put(21464, 0.447);
		CHANCES.put(21465, 0.808);
		CHANCES.put(21466, 0.447);
		CHANCES.put(21467, 0.808);
		CHANCES.put(21479, 0.477);
		CHANCES.put(21480, 0.863);
		CHANCES.put(21481, 0.477);
		CHANCES.put(21482, 0.863);
		CHANCES.put(21483, 0.477);
		CHANCES.put(21484, 0.863);
		CHANCES.put(21485, 0.477);
		CHANCES.put(21486, 0.863);
		CHANCES.put(21498, 0.509);
		CHANCES.put(21499, 0.920);
		CHANCES.put(21500, 0.509);
		CHANCES.put(21501, 0.920);
		CHANCES.put(21502, 0.509);
		CHANCES.put(21503, 0.920);
		CHANCES.put(21504, 0.509);
		CHANCES.put(21505, 0.920);
	}
	
	// Rewards
	private static final int[][] REWARDS =
	{
		{
			4039,
			15
		},
		{
			4043,
			15
		},
		{
			4044,
			15
		},
		{
			4040,
			10
		},
		{
			4042,
			10
		},
		{
			4041,
			5
		}
	};
	
	public Q00631_DeliciousTopChoiceMeat()
	{
		super(631, "Delicious Top Choice Meat");
		
		registerQuestItems(TOP_QUALITY_MEAT);
		
		addStartNpc(TUNATUN);
		addTalkId(TUNATUN);
		
		for (int npcId : CHANCES.keySet())
		{
			addKillId(npcId);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equals("31537-03.htm"))
		{
			if (player.getLevel() >= 65)
			{
				st.startQuest();
			}
			else
			{
				htmltext = "31537-02.htm";
				st.exitQuest(true);
			}
		}
		else if (StringUtil.isNumeric(event))
		{
			if (getQuestItemsCount(player, TOP_QUALITY_MEAT) >= 120)
			{
				htmltext = "31537-06.htm";
				takeItems(player, TOP_QUALITY_MEAT, -1);
				
				final int[] reward = REWARDS[Integer.parseInt(event)];
				rewardItems(player, reward[0], reward[1]);
				playSound(player, QuestSound.ITEMSOUND_QUEST_FINISH);
				st.exitQuest(true);
			}
			else
			{
				st.setCond(1);
				htmltext = "31537-07.htm";
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState st = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (st.getState())
		{
			case State.CREATED:
				htmltext = "31537-01.htm";
				break;
			
			case State.STARTED:
				final int cond = st.getCond();
				if (cond == 1)
				{
					htmltext = "31537-03a.htm";
				}
				else if (cond == 2)
				{
					if (getQuestItemsCount(player, TOP_QUALITY_MEAT) >= 120)
					{
						htmltext = "31537-04.htm";
					}
					else
					{
						st.setCond(1);
						htmltext = "31537-03a.htm";
					}
				}
				break;
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		final Player partyMember = getRandomPartyMember(player, 1);
		if (partyMember == null)
		{
			return;
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st == null)
		{
			return;
		}
		
		if (giveItemRandomly(partyMember, npc, TOP_QUALITY_MEAT, 1, 120, CHANCES.get(npc.getId()), true))
		{
			st.setCond(2);
		}
	}
}