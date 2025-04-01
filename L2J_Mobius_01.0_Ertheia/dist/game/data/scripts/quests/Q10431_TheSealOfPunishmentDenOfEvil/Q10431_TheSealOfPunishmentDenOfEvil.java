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
package quests.Q10431_TheSealOfPunishmentDenOfEvil;

import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * The Seal of Punishment: Den of Evil (10431)
 * @author Stayway
 */
public class Q10431_TheSealOfPunishmentDenOfEvil extends Quest
{
	// NPCs
	private static final int JOKEL = 33868;
	private static final int CHAIREN = 32655;
	private static final int[] RAGNA_ORC =
	{
		22692, // Ragna Orc Warriors
		22693, // Ragna Orc Heroes
		22694, // Ragna Orc Commanders
		22695, // Ragna Orc Healers
		22696, // Ragna Orc Shamans
		22697, // Ragna Orc Priests
		22698, // Ragna Orc Archers
		22699, // Ragna Orc Snipers
		22701, // Varangka's Dre Vanuls
		22702, // Varangka's Destroyers
	};
	// Item
	private static final int EVIL_FREED_SOUL = 36715;
	// Misc
	private static final int MIN_LEVEL = 81;
	private static final int MAX_LEVEL = 84;
	
	public Q10431_TheSealOfPunishmentDenOfEvil()
	{
		super(10431);
		addStartNpc(JOKEL);
		addTalkId(JOKEL, CHAIREN);
		addKillId(RAGNA_ORC);
		registerQuestItems(EVIL_FREED_SOUL);
		addCondMaxLevel(MAX_LEVEL, "33868-06.html");
		addCondMinLevel(MIN_LEVEL, "33868-06.html");
		addCondNotRace(Race.ERTHEIA, "noErtheia.html");
		addCondInCategory(CategoryType.FOURTH_CLASS_GROUP, "nocond.html");
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return getNoQuestMsg(player);
		}
		String htmltext = event;
		switch (event)
		{
			case "32655-02.htm":
			case "33868-02.htm":
			case "33868-03.htm":
			{
				htmltext = event;
				break;
			}
			case "33868-04.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "32655-03.html":
			{
				qs.setCond(2, true);
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
				if (!qs.isCond(3))
				{
					break;
				}
				final int stoneId = Integer.parseInt(event.replaceAll("reward_", ""));
				giveItems(player, stoneId, 15);
				giveStoryQuestReward(player, 60);
				final long count = getQuestItemsCount(player, EVIL_FREED_SOUL);
				if ((count >= 50) && (count < 100))
				{
					addExpAndSp(player, 28240800, 6777);
				}
				else if ((count >= 100) && (count < 200))
				{
					addExpAndSp(player, 56481600, 13554);
				}
				else if ((count >= 200) && (count < 300))
				{
					addExpAndSp(player, 84722400, 20331);
				}
				else if ((count >= 300) && (count < 400))
				{
					addExpAndSp(player, 112963200, 27108);
				}
				else if ((count >= 400) && (count < 500))
				{
					addExpAndSp(player, 141204000, 33835);
				}
				else if ((count >= 500) && (count < 600))
				{
					addExpAndSp(player, 169444800, 40662);
				}
				else if ((count >= 600) && (count < 700))
				{
					addExpAndSp(player, 197685600, 47439);
				}
				else if ((count >= 700) && (count < 800))
				{
					addExpAndSp(player, 225926400, 54216);
				}
				else if ((count >= 800) && (count < 900))
				{
					addExpAndSp(player, 254167200, 60993);
				}
				else if (count >= 900)
				{
					addExpAndSp(player, 282408000, 67770);
				}
				qs.exitQuest(false, true);
				htmltext = "32655-06.html";
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
		switch (npc.getId())
		{
			case JOKEL:
			{
				if (qs.isCreated())
				{
					htmltext = "33868-01.htm";
				}
				else if (qs.isCond(1))
				{
					htmltext = "33868-05.html";
				}
				else if (qs.isCompleted())
				{
					htmltext = getAlreadyCompletedMsg(player);
				}
				break;
			}
			case CHAIREN:
			{
				if (qs.isCond(1))
				{
					htmltext = "32655-01.html";
				}
				else if (qs.isCond(2))
				{
					htmltext = "32655-04.html";
				}
				else if (qs.isCond(3))
				{
					htmltext = "32655-05.html";
				}
				break;
			}
		}
		return htmltext;
	}
	
	private void giveItem(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs != null)
		{
			giveItems(player, EVIL_FREED_SOUL, 1);
			if (qs.isCond(2))
			{
				if (getQuestItemsCount(player, EVIL_FREED_SOUL) >= 50)
				{
					qs.setCond(3, true);
				}
				else
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (killer.isInParty())
		{
			for (Player member : killer.getParty().getMembers())
			{
				if (LocationUtil.checkIfInRange(1500, npc, member, false))
				{
					giveItem(npc, member);
				}
			}
		}
		else
		{
			giveItem(npc, killer);
		}
	}
}