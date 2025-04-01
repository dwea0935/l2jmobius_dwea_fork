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
package quests.Q00495_IncarnationOfJealousyPellineGroup;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.QuestType;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Incarnation of Jealousy Pelline (Group) (495)
 * @author Mobius
 */
public class Q00495_IncarnationOfJealousyPellineGroup extends Quest
{
	// NPC
	private static final int KARTIA_RESEARCHER = 33647;
	// Item
	private static final int DIMENSION_KEEPER_RED_BOX = 34928;
	// Boss
	private static final int BOSS = 25883; // Pelline (Group 90)
	// Misc
	private static final int MIN_LEVEL = 90;
	private static final int MAX_LEVEL = 94;
	
	public Q00495_IncarnationOfJealousyPellineGroup()
	{
		super(495);
		addStartNpc(KARTIA_RESEARCHER);
		addTalkId(KARTIA_RESEARCHER);
		addKillId(BOSS);
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "33647-00.htm");
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		final String htmltext = event;
		if (event.equals("33647-03.htm"))
		{
			qs.startQuest();
		}
		else if (event.equals("33647-06.html") && qs.isCond(2))
		{
			rewardItems(player, DIMENSION_KEEPER_RED_BOX, 1);
			qs.exitQuest(QuestType.DAILY, true);
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
				htmltext = "33647-01.htm";
				break;
			}
			case State.STARTED:
			{
				if (qs.isCond(1))
				{
					htmltext = "33647-04.html";
				}
				else if (qs.isCond(2))
				{
					if ((player.getLevel() < MIN_LEVEL) || (player.getLevel() > MAX_LEVEL))
					{
						htmltext = "33647-00a.html";
					}
					else
					{
						htmltext = "33647-05.html";
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				if (!qs.isNowAvailable())
				{
					htmltext = "33647-07.html";
				}
				else
				{
					qs.setState(State.CREATED);
					htmltext = "33647-01.htm";
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		executeForEachPlayer(player, npc, isSummon, true, false);
	}
	
	@Override
	public void actionForEachPlayer(Player player, Npc npc, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(1))
		{
			qs.setCond(2, true);
		}
	}
}