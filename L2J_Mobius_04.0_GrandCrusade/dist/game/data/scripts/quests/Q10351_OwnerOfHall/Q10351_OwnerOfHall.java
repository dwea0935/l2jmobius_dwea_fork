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
package quests.Q10351_OwnerOfHall;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

import quests.Q10318_DecayingDarkness.Q10318_DecayingDarkness;

/**
 * Owner of Hall (10351)
 * @URL https://l2wiki.com/Owner_of_Hall
 * @author Gigi
 */
public class Q10351_OwnerOfHall extends Quest
{
	// NPCs
	private static final int LYDIA = 32892;
	private static final int OCTAVIS = 29194; // Octavis common mode
	// Item
	private static final int OCTAVIS_BRACELET = 19461;
	// Misc
	private static final int MIN_LEVEL = 95;
	
	public Q10351_OwnerOfHall()
	{
		super(10351);
		addStartNpc(LYDIA);
		addTalkId(LYDIA);
		addKillId(OCTAVIS);
		addCondMinLevel(MIN_LEVEL, "32892-00.htm");
		addCondCompletedQuest(Q10318_DecayingDarkness.class.getSimpleName(), "32892-00a.htm");
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
			case "32892-02.htm":
			case "32892-03.htm":
			case "32892-04.htm":
			{
				htmltext = event;
				break;
			}
			case "32892-05.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "32892-08.html":
			{
				if (qs.isCond(2))
				{
					giveAdena(player, 23655000, false);
					addExpAndSp(player, 897850000, 215484);
					giveItems(player, OCTAVIS_BRACELET, 1);
					qs.exitQuest(false, true);
					htmltext = getHtm(player, "32892-08.html").replace("%name%", player.getName());
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
				htmltext = "32892-01.htm";
				break;
			}
			case State.STARTED:
			{
				if (qs.isCond(1))
				{
					htmltext = "32892-06.html";
				}
				else if (qs.isCond(2))
				{
					htmltext = "32892-07.html";
				}
				break;
			}
			case State.COMPLETED:
			{
				htmltext = "Complete.html";
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
		if ((qs != null) && qs.isStarted() && player.isInsideRadius3D(npc, Config.ALT_PARTY_RANGE))
		{
			qs.setCond(2, true);
		}
	}
}