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
package quests.custom.Q10529_IvoryTowersResearchFloatingSeaJournal;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.NpcLogListHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.QuestType;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.util.ArrayUtil;

/**
 * Ivory Tower's Research - Floating Sea Journal (10529)
 * @URL https://l2wiki.com/Ivory_Tower%27s_Research_-_Floating_Sea_Journal
 * @author Mobius
 */
public class Q10529_IvoryTowersResearchFloatingSeaJournal extends Quest
{
	// NPCs
	private static final int START_NPC = 33846;
	private static final int[] MONSTERS_1 =
	{
		24226,
		24227,
		24228,
		24229,
		24230,
		24231,
		24232,
		24233,
		24234,
		24235,
		24236,
		24237,
		24238,
		24239,
	};
	private static final int[] MONSTERS_2 =
	{
		24232,
		24239,
	};
	// Item
	private static final int MONSTER_DROP_1 = 48836;
	private static final int MONSTER_DROP_2 = 48837;
	// Misc
	private static final int REQUIRED_DROP_COUNT_1 = 100;
	private static final int REQUIRED_DROP_COUNT_2 = 1;
	private static final int KILLING_NPCSTRING_ID = NpcStringId.LV_106_IVORY_TOWER_S_RESEARCH_SEA_OF_SPORES_JOURNAL_IN_PROGRESS.getId();
	private static final QuestType QUEST_TYPE = QuestType.ONE_TIME; // REPEATABLE, ONE_TIME, DAILY
	private static final boolean PARTY_QUEST = false;
	private static final int KILLING_COND_1 = 1;
	private static final int FINISH_COND_1 = 2;
	private static final int KILLING_COND_2 = 3;
	private static final int FINISH_COND_2 = 4;
	private static final int MIN_LEVEL = 106;
	
	public Q10529_IvoryTowersResearchFloatingSeaJournal()
	{
		super(10529);
		addStartNpc(START_NPC);
		addTalkId(START_NPC);
		addKillId(MONSTERS_1);
		addKillId(MONSTERS_2);
		registerQuestItems(MONSTER_DROP_1, MONSTER_DROP_2);
		addCondMinLevel(MIN_LEVEL, getNoQuestMsg(null));
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		switch (event)
		{
			case "accept_1.htm":
			{
				if (qs.isCreated())
				{
					qs.startQuest();
					qs.setCond(KILLING_COND_1);
				}
				break;
			}
			case "accept_2.html":
			{
				if (qs.isCond(FINISH_COND_1) && (getQuestItemsCount(player, MONSTER_DROP_1) >= REQUIRED_DROP_COUNT_1))
				{
					takeItems(player, MONSTER_DROP_1, -1);
					qs.setCond(KILLING_COND_2, true);
				}
				break;
			}
			case "reward.html":
			{
				if (qs.isCond(FINISH_COND_2) && (getQuestItemsCount(player, MONSTER_DROP_2) >= REQUIRED_DROP_COUNT_2))
				{
					takeItems(player, MONSTER_DROP_2, -1);
					// Reward.
					addExpAndSp(player, 99527685300L, 99527580);
					rewardItems(player, 19448, 1);
					qs.exitQuest(QUEST_TYPE, true);
				}
				break;
			}
			default:
			{
				return null;
			}
		}
		return event;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		if (npc.getId() == START_NPC)
		{
			switch (qs.getState())
			{
				case State.CREATED:
				{
					htmltext = "start.htm";
					break;
				}
				case State.STARTED:
				{
					if (qs.isCond(KILLING_COND_1))
					{
						htmltext = "accept.htm";
					}
					else if (qs.isCond(FINISH_COND_1))
					{
						htmltext = "finish_1.html";
					}
					else if (qs.isCond(KILLING_COND_2))
					{
						htmltext = "accept_2.html";
					}
					else if (qs.isCond(FINISH_COND_2))
					{
						htmltext = "finish_2.html";
					}
					break;
				}
				case State.COMPLETED:
				{
					if (qs.isNowAvailable())
					{
						qs.setState(State.CREATED);
						htmltext = "start.htm";
					}
					else
					{
						htmltext = getAlreadyCompletedMsg(player, QUEST_TYPE);
					}
					break;
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = PARTY_QUEST ? getRandomPartyMemberState(killer, -1, 3, npc) : getQuestState(killer, false);
		if (qs != null)
		{
			final Player player = qs.getPlayer();
			if (qs.isCond(KILLING_COND_1) && ArrayUtil.contains(MONSTERS_1, npc.getId()))
			{
				if (giveItemRandomly(player, npc, MONSTER_DROP_1, 1, REQUIRED_DROP_COUNT_1, 1, true))
				{
					qs.setCond(FINISH_COND_1, true);
				}
				sendNpcLogList(player);
			}
			else if (qs.isCond(KILLING_COND_2) && ArrayUtil.contains(MONSTERS_2, npc.getId()))
			{
				if (giveItemRandomly(player, npc, MONSTER_DROP_2, 1, REQUIRED_DROP_COUNT_2, 1, true))
				{
					qs.setCond(FINISH_COND_2, true);
				}
			}
		}
	}
	
	@Override
	public Set<NpcLogListHolder> getNpcLogList(Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(KILLING_COND_1))
		{
			final Set<NpcLogListHolder> holder = new HashSet<>();
			holder.add(new NpcLogListHolder(KILLING_NPCSTRING_ID, false, (int) getQuestItemsCount(player, MONSTER_DROP_1)));
			return holder;
		}
		return super.getNpcLogList(player);
	}
}
