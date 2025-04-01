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
package quests.Q10519_ControllingYourTemper;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.Id;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcMenuSelect;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.QuestType;
import org.l2jmobius.gameserver.model.quest.State;

import quests.Q10518_SucceedingThePriestess.Q10518_SucceedingThePriestess;

/**
 * Controlling Your Temper (10519)
 * @author Kazumi
 */
public final class Q10519_ControllingYourTemper extends Quest
{
	// NPCs
	private static final int OKAYTI = 34490;
	// MOBs
	private static final int[] FAFURIONS =
	{
		29361, // Fafurion
		29362, // Fafurion
		29363, // Fafurion
		29364, // Fafurion
		29365, // Fafurion
		29366, // Fafurion
		29367, // Fafurion
	};
	// Item
	private static final int SUPERIOR_GIANT_CODEX_CHAPTER_1 = 46151;
	// Misc
	private static final int MIN_LEVEL = 110;
	
	public Q10519_ControllingYourTemper()
	{
		super(10519);
		addStartNpc(OKAYTI);
		addTalkId(OKAYTI);
		addKillId(FAFURIONS);
		addCondMinLevel(MIN_LEVEL, "okayti_q10519_02.htm");
		addCondCompletedQuest(Q10518_SucceedingThePriestess.class.getSimpleName(), "okayti_q10519_02.htm");
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		if (event.equals("quest_accept"))
		{
			qs.startQuest();
			htmltext = "okayti_q10519_05.htm";
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
				if (player.getLevel() >= MIN_LEVEL)
				{
					htmltext = "okayti_q10519_01.htm";
					break;
				}
				break;
			}
			case State.STARTED:
			{
				if (qs.getCond() == 1)
				{
					htmltext = "okayti_q10519_06.htm";
					break;
				}
				htmltext = "okayti_q10519_07.htm";
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
	
	@RegisterEvent(EventType.ON_NPC_MENU_SELECT)
	@RegisterType(ListenerRegisterType.NPC)
	@Id(OKAYTI)
	public final void onNpcMenuSelect(OnNpcMenuSelect event)
	{
		final Player player = event.getTalker();
		final QuestState qs = getQuestState(player, false);
		final Npc npc = event.getNpc();
		final int ask = event.getAsk();
		final int reply = event.getReply();
		
		if (ask == 10519)
		{
			switch (reply)
			{
				case 1:
				{
					showHtmlFile(player, "okayti_q10519_03.htm", npc);
					break;
				}
				case 2:
				{
					showHtmlFile(player, "okayti_q10519_04.htm", npc);
					break;
				}
				case 10:
				{
					if (player.getLevel() >= MIN_LEVEL)
					{
						if (qs.isCond(2))
						{
							qs.exitQuest(QuestType.ONE_TIME, true);
							giveItems(player, SUPERIOR_GIANT_CODEX_CHAPTER_1, 1);
							addExpAndSp(player, 333371214000L, 333371160);
							showHtmlFile(player, "okayti_q10519_08.htm", npc);
							break;
						}
						break;
					}
					getNoQuestLevelRewardMsg(player);
					break;
				}
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final Party party = player.getParty();
		if (party != null)
		{
			party.getMembers().forEach(p -> onKill(npc, p));
		}
		else
		{
			onKill(npc, player);
		}
	}
	
	public void onKill(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(1))
		{
			qs.setCond(2, true);
		}
	}
}
