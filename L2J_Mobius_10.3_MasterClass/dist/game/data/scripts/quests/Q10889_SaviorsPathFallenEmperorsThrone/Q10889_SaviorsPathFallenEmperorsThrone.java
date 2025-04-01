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
package quests.Q10889_SaviorsPathFallenEmperorsThrone;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

import quests.Q10888_SaviorsPathDefeatTheEmbryo.Q10888_SaviorsPathDefeatTheEmbryo;

/**
 * Savior's Path - Fallen Emperor's Throne
 * @URL https://l2wiki.com/Savior%27s_Path_-_Fallen_Emperor%27s_Throne
 * @author CostyKiller
 */
public class Q10889_SaviorsPathFallenEmperorsThrone extends Quest
{
	// NPC
	private static final int LEONA_BLACKBIRD = 34425;
	// Monsters
	private static final int FE_HELIOS = 26335;
	// Items
	private static final int ORIGIN_OF_GIANTS = 48548;
	// Misc
	private static final int MIN_LEVEL = 103;
	private static final int ORIGIN_OF_GIANTS_NEEDED = 5;
	
	public Q10889_SaviorsPathFallenEmperorsThrone()
	{
		super(10889);
		addStartNpc(LEONA_BLACKBIRD);
		addTalkId(LEONA_BLACKBIRD);
		addKillId(FE_HELIOS);
		addCondMinLevel(MIN_LEVEL, "34425-00.html");
		addCondCompletedQuest(Q10888_SaviorsPathDefeatTheEmbryo.class.getSimpleName(), "34425-00.html");
		registerQuestItems(ORIGIN_OF_GIANTS);
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
			case "34425-02.htm":
			case "34425-03.htm":
			case "34425-06.htm":
			{
				htmltext = event;
				break;
			}
			case "34425-04.html":
			{
				if ((player.getLevel() >= MIN_LEVEL))
				{
					qs.startQuest();
					htmltext = event;
				}
				break;
			}
			case "34425-07.html":
			{
				if (qs.isCond(2))
				{
					if ((player.getLevel() >= MIN_LEVEL))
					{
						addExpAndSp(player, 27191624, 24472);
						giveAdena(player, 30773010, true);
						qs.exitQuest(false, true);
						htmltext = event;
					}
					else
					{
						htmltext = getNoQuestLevelRewardMsg(player);
					}
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
				htmltext = "34425-01.htm";
				break;
			}
			case State.STARTED:
			{
				if (qs.isCond(1) && (getQuestItemsCount(player, ORIGIN_OF_GIANTS) < ORIGIN_OF_GIANTS_NEEDED))
				{
					htmltext = "34425-05.html";
				}
				else if (qs.isCond(2) && (getQuestItemsCount(player, ORIGIN_OF_GIANTS) >= ORIGIN_OF_GIANTS_NEEDED))
				{
					htmltext = "34425-06.htm";
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
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		executeForEachPlayer(player, npc, isSummon, true, true);
	}
	
	@Override
	public void actionForEachPlayer(Player player, Npc npc, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(1) && player.isInsideRadius3D(npc, Config.ALT_PARTY_RANGE))
		{
			if (getQuestItemsCount(player, ORIGIN_OF_GIANTS) < ORIGIN_OF_GIANTS_NEEDED)
			{
				giveItems(player, ORIGIN_OF_GIANTS, 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
			if (getQuestItemsCount(player, ORIGIN_OF_GIANTS) >= ORIGIN_OF_GIANTS_NEEDED)
			{
				qs.setCond(2, true);
			}
		}
	}
}