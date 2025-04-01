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
package quests.Q10432_ChasingVarangka;

import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

import quests.Q10431_TheSealOfPunishmentDenOfEvil.Q10431_TheSealOfPunishmentDenOfEvil;

/**
 * Chasing Varangka (10432)
 * @URL https://l2wiki.com/Chasing_Varangka
 * @author Gigi
 */
public class Q10432_ChasingVarangka extends Quest
{
	// NPCs
	private static final int CHAIREN = 32655;
	private static final int JOKEL = 33868;
	private static final int DARK_SHAMAN_VARANGKA = 18808;
	// Misc
	private static final int MIN_LEVEL = 81;
	private static final int MAX_LEVEL = 84;
	// Rewards
	private static final int EAS = 960;
	
	public Q10432_ChasingVarangka()
	{
		super(10432);
		addStartNpc(CHAIREN);
		addTalkId(CHAIREN, JOKEL);
		addKillId(DARK_SHAMAN_VARANGKA);
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "32655-00.htm");
		addCondNotRace(Race.ERTHEIA, "noErtheia.html");
		addCondInCategory(CategoryType.FOURTH_CLASS_GROUP, "32655-00.htm");
		addCondCompletedQuest(Q10431_TheSealOfPunishmentDenOfEvil.class.getSimpleName(), "32655-00.htm");
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
			case "32655-02.htm":
			case "32655-03.htm":
			case "33868-02.html":
			{
				htmltext = event;
				break;
			}
			case "32655-04.htm":
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
					htmltext = "33868-03.html";
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
				if (npc.getId() == CHAIREN)
				{
					htmltext = "32655-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case CHAIREN:
					{
						if (qs.isCond(1))
						{
							htmltext = "32655-05.html";
						}
						else if (qs.isCond(2))
						{
							htmltext = "32655-06.html";
						}
						break;
					}
					case JOKEL:
					{
						if (qs.isCond(2))
						{
							htmltext = "33868-01.html";
						}
						break;
					}
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