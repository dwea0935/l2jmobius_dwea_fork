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
package quests.Q10897_ShowYourAbility;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Faction;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

import quests.Q10896_VisitTheAdventureGuild.Q10896_VisitTheAdventureGuild;

/**
 * Show Your Ability (10897)
 * @URL https://l2wiki.com/Show_Your_Ability
 * @author Dmitri
 */
public class Q10897_ShowYourAbility extends Quest
{
	// NPCs
	private static final int PENNY = 34413;
	// Misc
	private static final int MIN_LEVEL = 100;
	
	public Q10897_ShowYourAbility()
	{
		super(10897);
		addStartNpc(PENNY);
		addTalkId(PENNY);
		addCondMinLevel(MIN_LEVEL, "nolevel.html");
		addCondCompletedQuest(Q10896_VisitTheAdventureGuild.class.getSimpleName(), "34413-00.htm");
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
			case "34413-07.html":
			{
				// Rewards
				addExpAndSp(player, 20763540000L, 20763540);
				addFactionPoints(player, Faction.ADVENTURE_GUILD, 100); // add FP points to ADVENTURE_GUILD Faction
				qs.exitQuest(false, true);
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
						if ((qs.isCond(1)) && (player.getFactionLevel(Faction.ADVENTURE_GUILD) >= 2))
						{
							final QuestState st = player.getQuestState("Q00682_TheStrongInTheClosedSpace");
							if ((st != null) && st.isCompleted())
							{
								qs.setCond(2, true);
								htmltext = "34413-06.html";
							}
							else
							{
								htmltext = "34413-05.html";
							}
						}
						if (qs.isCond(2))
						{
							htmltext = "34413-06.html";
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