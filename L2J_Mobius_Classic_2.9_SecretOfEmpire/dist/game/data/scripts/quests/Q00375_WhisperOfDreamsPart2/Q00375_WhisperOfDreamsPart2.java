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
package quests.Q00375_WhisperOfDreamsPart2;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Whisper Of Dreams Part2 (375)
 * @author Stayway
 */
public class Q00375_WhisperOfDreamsPart2 extends Quest
{
	// NPCs
	private static final int VANUTU = 30938;
	// Monsters
	private static final int LIMAL_KARINNESS = 20628;
	private static final int KARIK = 20629;
	// Items
	private static final int KARIK_HORN = 5888;
	private static final int LIMAL_KARINESS_BLOOD = 5889;
	private static final int MYSTERIOUS_STONE = 5887;
	// Rewards
	private static final int SCROLL_PART_EW = 49474;
	private static final int REFINED_SCROLL_PART_EW = 49476;
	private static final int ENCHANT_WEAPON_B = 947;
	private static final int IMPROVED_ENCHANT_WEAPON_B = 33808;
	// Misc
	private static final int MIN_LEVEL = 68;
	private static final int MAX_LEVEL = 82;
	
	public Q00375_WhisperOfDreamsPart2()
	{
		super(375);
		addStartNpc(VANUTU);
		addTalkId(VANUTU);
		addKillId(LIMAL_KARINNESS, KARIK);
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "30938-02.html");
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		String htmltext = null;
		if (qs != null)
		{
			switch (event)
			{
				case "30938-03.htm":
				{
					qs.startQuest();
					htmltext = event;
					break;
				}
				case "30938-07.html":
				{
					qs.setCond(1);
					htmltext = event;
					break;
				}
				case "30938-08.html":
				{
					qs.exitQuest(true, true);
					htmltext = event;
					break;
				}
				case "reward1":
				{
					if (qs.isCond(2) && (getQuestItemsCount(player, KARIK_HORN) >= 325) && (getQuestItemsCount(player, LIMAL_KARINESS_BLOOD) >= 325))
					{
						giveItems(player, SCROLL_PART_EW, 1);
						takeItems(player, KARIK_HORN, 325);
						takeItems(player, LIMAL_KARINESS_BLOOD, 325);
						giveAdena(player, 9000, true);
						htmltext = "30938-06.html";
					}
					break;
				}
				case "reward2":
				{
					if (qs.isCond(2) && (getQuestItemsCount(player, KARIK_HORN) >= 325) && (getQuestItemsCount(player, LIMAL_KARINESS_BLOOD) >= 325))
					{
						giveItems(player, REFINED_SCROLL_PART_EW, 1);
						takeItems(player, KARIK_HORN, 325);
						takeItems(player, LIMAL_KARINESS_BLOOD, 325);
						giveAdena(player, 9000, true);
						htmltext = "30938-06.html";
					}
					break;
				}
				case "reward3":
				{
					if (qs.isCond(2) && (getQuestItemsCount(player, KARIK_HORN) >= 325) && (getQuestItemsCount(player, LIMAL_KARINESS_BLOOD) >= 325))
					{
						giveItems(player, ENCHANT_WEAPON_B, 1);
						takeItems(player, KARIK_HORN, 325);
						takeItems(player, LIMAL_KARINESS_BLOOD, 325);
						giveAdena(player, 9000, true);
						htmltext = "30938-06.html";
					}
					break;
				}
				case "reward4":
				{
					if (qs.isCond(2) && (getQuestItemsCount(player, KARIK_HORN) >= 325) && (getQuestItemsCount(player, LIMAL_KARINESS_BLOOD) >= 325))
					{
						giveItems(player, IMPROVED_ENCHANT_WEAPON_B, 1);
						takeItems(player, KARIK_HORN, 325);
						takeItems(player, LIMAL_KARINESS_BLOOD, 325);
						giveAdena(player, 9000, true);
						htmltext = "30938-06.html";
					}
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
		switch (qs.getState())
		{
			case State.CREATED:
			{
				if (getQuestItemsCount(player, MYSTERIOUS_STONE) >= 1)
				{
					takeItems(player, MYSTERIOUS_STONE, 1);
					htmltext = "30938-01.htm";
				}
				else
				{
					htmltext = "30938-05.html";
				}
				break;
			}
			case State.STARTED:
			{
				if (qs.isCond(1))
				{
					htmltext = "30938-04.html";
				}
				else if (qs.isCond(2))
				{
					htmltext = (getQuestItemsCount(player, KARIK_HORN) >= 325) && (getQuestItemsCount(player, LIMAL_KARINESS_BLOOD) >= 325) ? "30938-05.html" : "30938-06.html";
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getRandomPartyMemberState(killer, -1, 3, npc);
		if (qs != null)
		{
			switch (npc.getId())
			{
				case KARIK:
				{
					if (qs.isCond(1) && qs.isStarted())
					{
						giveItemRandomly(killer, npc, KARIK_HORN, 1, 325, 0.95, true);
					}
					break;
				}
				case LIMAL_KARINNESS:
				{
					if (qs.isCond(1) && qs.isStarted())
					{
						giveItemRandomly(killer, npc, LIMAL_KARINESS_BLOOD, 1, 325, 0.95, true);
					}
					break;
				}
			}
			if (qs.isCond(1) && (getQuestItemsCount(killer, LIMAL_KARINESS_BLOOD) >= 325) && (getQuestItemsCount(killer, KARIK_HORN) >= 325))
			{
				qs.setCond(2, true);
			}
		}
	}
}
