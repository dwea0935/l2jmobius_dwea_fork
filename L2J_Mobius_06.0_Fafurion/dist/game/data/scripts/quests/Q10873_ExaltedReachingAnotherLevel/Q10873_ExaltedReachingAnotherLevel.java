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
package quests.Q10873_ExaltedReachingAnotherLevel;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

import quests.Q10823_ExaltedOneWhoShattersTheLimit.Q10823_ExaltedOneWhoShattersTheLimit;

/**
 * Exalted, Reaching Another Level (10873)
 * @URL https://l2wiki.com/Exalted,_Reaching_Another_Level
 * @author Dmitri
 */
public class Q10873_ExaltedReachingAnotherLevel extends Quest
{
	// NPC
	private static final int LIONEL = 33907;
	// Items
	private static final int SPORCHA_CERTIFICATE = 47830;
	private static final int KRENAHT_CERTIFICATE = 47831;
	private static final int SANTIAGO_SERTIFICATE = 47832;
	private static final int ARCTURUS_CERTIFICATE = 47833;
	private static final int LIONEL_MISSION_LIST_4 = 47829;
	// Rewards
	private static final int VITALITY_OF_THE_EXALTED = 47854;
	private static final int DIGNITY_OF_THE_EXALTED = 47852;
	// Misc
	private static final int MIN_LEVEL = 103;
	private static final int MIN_COMPLETE_LEVEL = 104;
	
	public Q10873_ExaltedReachingAnotherLevel()
	{
		super(10873);
		addStartNpc(LIONEL);
		addTalkId(LIONEL);
		addCondMinLevel(MIN_LEVEL, "33907-00.htm");
		addCondCompletedQuest(Q10823_ExaltedOneWhoShattersTheLimit.class.getSimpleName(), "33907-00.htm");
		registerQuestItems(LIONEL_MISSION_LIST_4, SPORCHA_CERTIFICATE, KRENAHT_CERTIFICATE, SANTIAGO_SERTIFICATE, ARCTURUS_CERTIFICATE);
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
			case "33907-03.htm":
			case "33907-04.htm":
			{
				htmltext = event;
				break;
			}
			case "33907-05.html":
			{
				if (qs.isCreated())
				{
					giveItems(player, LIONEL_MISSION_LIST_4, 1);
					qs.startQuest();
					htmltext = event;
				}
				break;
			}
			case "33907-08.html":
			{
				if (hasQuestItems(player, SPORCHA_CERTIFICATE, KRENAHT_CERTIFICATE, SANTIAGO_SERTIFICATE, ARCTURUS_CERTIFICATE) && (player.getLevel() >= MIN_COMPLETE_LEVEL))
				{
					giveItems(player, VITALITY_OF_THE_EXALTED, 1);
					giveItems(player, DIGNITY_OF_THE_EXALTED, 1);
					qs.exitQuest(false, true);
					htmltext = event;
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState qs = getQuestState(player, true);
		switch (qs.getState())
		{
			case State.CREATED:
			{
				htmltext = "33907-01.htm";
				break;
			}
			case State.STARTED:
			{
				if (hasQuestItems(player, SPORCHA_CERTIFICATE, KRENAHT_CERTIFICATE, SANTIAGO_SERTIFICATE, ARCTURUS_CERTIFICATE) && (player.getLevel() >= MIN_COMPLETE_LEVEL))
				{
					htmltext = "33907-07.html";
				}
				else
				{
					htmltext = "33907-06.html";
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
