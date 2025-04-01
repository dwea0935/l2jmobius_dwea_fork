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
package quests.Q10797_CrossingFate;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.util.LocationUtil;

import quests.Q10796_TheEyeThatDefiedTheGods.Q10796_TheEyeThatDefiedTheGods;

/**
 * Crossing Fate (10797)
 * @URL https://l2wiki.com/Crossing_Fate
 * @author Gigi
 */
public class Q10797_CrossingFate extends Quest
{
	// NPCs
	private static final int EYE_OF_ARGOS = 31683;
	private static final int DAIMON_THE_WHITE_EYED = 27499;
	// Items
	private static final int EAA = 730;
	// Misc
	private static final int MIN_LEVEL = 70;
	private static final int MAX_LEVEL = 75;
	
	public Q10797_CrossingFate()
	{
		super(10797);
		addStartNpc(EYE_OF_ARGOS);
		addTalkId(EYE_OF_ARGOS);
		addKillId(DAIMON_THE_WHITE_EYED);
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "no_level.html");
		addCondRace(Race.ERTHEIA, "noErtheia.html");
		addCondCompletedQuest(Q10796_TheEyeThatDefiedTheGods.class.getSimpleName(), "restriction.html");
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "31683-02.htm":
			case "31683-03.htm":
			{
				htmltext = event;
				break;
			}
			case "31683-04.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "31683-07.html":
			{
				if (qs.isCond(2))
				{
					addExpAndSp(player, 2721600, 653);
					giveStoryQuestReward(player, 26);
					giveItems(player, EAA, 5);
					qs.exitQuest(false, true);
					htmltext = event;
					break;
				}
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
			htmltext = "31683-01.htm";
		}
		else if (qs.isCond(1))
		{
			htmltext = "31683-05.html";
		}
		else if (qs.isCond(2))
		{
			htmltext = "31683-06.html";
		}
		else if (qs.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && qs.isCond(1) && LocationUtil.checkIfInRange(1500, npc, qs.getPlayer(), false))
		{
			qs.setCond(2, true);
		}
	}
}