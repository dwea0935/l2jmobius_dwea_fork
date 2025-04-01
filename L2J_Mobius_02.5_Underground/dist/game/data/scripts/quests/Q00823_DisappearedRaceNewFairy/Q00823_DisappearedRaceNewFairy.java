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
package quests.Q00823_DisappearedRaceNewFairy;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.QuestType;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Disappeared Race, New Fairy (00823)
 * @URL https://l2wiki.com/Disappeared_Race,_New_Fairy
 * @author Gigi
 */
public class Q00823_DisappearedRaceNewFairy extends Quest
{
	// NPCs
	private static final int MIMYU = 30747;
	// Monsters
	private static final int[] MONSTERS =
	{
		23566, // Nymph Rose
		23567, // Nymph Rose
		23568, // Nymph Lily
		23569, // Nymph Lily
		23570, // Nymph Tulip
		23571, // Nymph Tulip
		23572, // Nymph Cosmos
		23573, // Nymph Cosmos
		23578 // Nymph Guardian
	};
	// Item's
	private static final int NYMPH_STAMEN = 46258;
	private static final int MIMIUS_REWARD_BOX = 46259;
	// Misc
	private static final int MIN_LEVEL = 100;
	
	public Q00823_DisappearedRaceNewFairy()
	{
		super(823);
		addStartNpc(MIMYU);
		addTalkId(MIMYU);
		addKillId(MONSTERS);
		registerQuestItems(NYMPH_STAMEN);
		addCondMinLevel(MIN_LEVEL, "30747-00.htm");
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
			case "30747-02.htm":
			case "30747-03.htm":
			case "30747-04.htm":
			case "30747-09.html":
			{
				htmltext = event;
				break;
			}
			case "30747-05.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "30747-10.html":
			{
				if ((getQuestItemsCount(player, NYMPH_STAMEN) >= 300) && (getQuestItemsCount(player, NYMPH_STAMEN) < 600))
				{
					addExpAndSp(player, 3045319200L, 7308474);
					giveItems(player, MIMIUS_REWARD_BOX, 1);
				}
				else if ((getQuestItemsCount(player, NYMPH_STAMEN) >= 600) && (getQuestItemsCount(player, NYMPH_STAMEN) < 900))
				{
					addExpAndSp(player, 6090638400L, 14617495);
					giveItems(player, MIMIUS_REWARD_BOX, 2);
				}
				else if ((getQuestItemsCount(player, NYMPH_STAMEN) >= 900) && (getQuestItemsCount(player, NYMPH_STAMEN) < 1200))
				{
					addExpAndSp(player, 9135957600L, 21926243);
					giveItems(player, MIMIUS_REWARD_BOX, 3);
				}
				else if ((getQuestItemsCount(player, NYMPH_STAMEN) >= 1200) && (getQuestItemsCount(player, NYMPH_STAMEN) < 1500))
				{
					addExpAndSp(player, 12181276800L, 29233986);
					giveItems(player, MIMIUS_REWARD_BOX, 4);
				}
				else if ((getQuestItemsCount(player, NYMPH_STAMEN) >= 1500) && (getQuestItemsCount(player, NYMPH_STAMEN) < 1800))
				{
					addExpAndSp(player, 15226596000L, 36542370);
					giveItems(player, MIMIUS_REWARD_BOX, 5);
				}
				else if (getQuestItemsCount(player, NYMPH_STAMEN) >= 1800)
				{
					addExpAndSp(player, 18271915200L, 43852486);
					giveItems(player, MIMIUS_REWARD_BOX, 6);
				}
				qs.exitQuest(QuestType.DAILY, true);
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
			case State.COMPLETED:
			{
				if (!qs.isNowAvailable())
				{
					htmltext = "30747-11.html";
					break;
				}
				qs.setState(State.CREATED);
			}
			case State.CREATED:
			{
				htmltext = "30747-01.htm";
				break;
			}
			case State.STARTED:
			{
				if (qs.isCond(1))
				{
					htmltext = "30747-06.html";
				}
				else if (qs.isCond(2) && (getQuestItemsCount(player, NYMPH_STAMEN) < 1800))
				{
					htmltext = "30747-07.html";
				}
				else
				{
					htmltext = "30747-08.html";
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && (qs.getCond() > 0) && (getQuestItemsCount(player, NYMPH_STAMEN) < 1800))
		{
			giveItems(player, NYMPH_STAMEN, 1);
			if (getQuestItemsCount(player, NYMPH_STAMEN) == 300)
			{
				qs.setCond(2, true);
			}
			else
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
		}
	}
}