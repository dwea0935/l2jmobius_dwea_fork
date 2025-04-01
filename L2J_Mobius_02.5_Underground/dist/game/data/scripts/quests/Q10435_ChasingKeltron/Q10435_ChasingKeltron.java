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
package quests.Q10435_ChasingKeltron;

import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

import quests.Q10434_TheSealOfPunishmentSelMahumTrainingGrounds.Q10434_TheSealOfPunishmentSelMahumTrainingGrounds;

/**
 * Chasing Keltron (10435)
 * @URL https://l2wiki.com/Chasing_Keltron
 * @author Gigi
 */
public class Q10435_ChasingKeltron extends Quest
{
	// NPCs
	private static final int RUA = 33841;
	private static final int SEL_MAHUM_CHIEF_KELTRON = 27498;
	// Reward
	private static final int EAS = 960;
	// Misc
	private static final int MIN_LEVEL = 81;
	
	public Q10435_ChasingKeltron()
	{
		super(10435);
		addStartNpc(RUA);
		addTalkId(RUA);
		addKillId(SEL_MAHUM_CHIEF_KELTRON);
		addCondMinLevel(MIN_LEVEL, "33841-00.htm");
		addCondNotRace(Race.ERTHEIA, "33841-00.htm");
		addCondInCategory(CategoryType.WEAPON_MASTER, "33841-00.htm");
		addCondCompletedQuest(Q10434_TheSealOfPunishmentSelMahumTrainingGrounds.class.getSimpleName(), "33841-00.htm");
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
			case "33841-02.htm":
			case "33841-03.htm":
			{
				htmltext = event;
				break;
			}
			case "33841-04.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "reward_9546":
			case "reward_9547":
			case "reward_9548":
			case "reward_9549":
			case "reward_9550":
			case "reward_9551":
			{
				if (qs.isCond(2))
				{
					final int stoneId = Integer.parseInt(event.replaceAll("reward_", ""));
					giveItems(player, stoneId, 15);
					giveItems(player, EAS, 2);
					giveStoryQuestReward(player, 30);
					addExpAndSp(player, 14120400, 3388);
					qs.exitQuest(false, true);
					htmltext = "33841-07.html";
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
				if (npc.getId() == RUA)
				{
					htmltext = "33841-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				if ((qs.isCond(1)) && (npc.getId() == RUA))
				{
					htmltext = "33841-05.html";
					break;
				}
				else if (qs.isCond(2))
				{
					htmltext = "33841-06.html";
				}
				break;
			}
			case State.COMPLETED:
			{
				htmltext = getNoQuestMsg(player);
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && qs.isCond(1))
		{
			qs.setCond(2, true);
		}
	}
}