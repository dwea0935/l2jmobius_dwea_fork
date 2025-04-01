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
package quests.custom.Q00529_RegularBarrierMaintenance;

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

import quests.custom.Q10529_IvoryTowersResearchFloatingSeaJournal.Q10529_IvoryTowersResearchFloatingSeaJournal;

/**
 * Regular Barrier Maintenance (529)
 * @URL https://l2wiki.com/Regular_Barrier_Maintenance
 * @author Mobius
 */
public class Q00529_RegularBarrierMaintenance extends Quest
{
	// NPCs
	private static final int START_NPC = 34448;
	private static final int[] MONSTERS =
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
	// Item
	private static final int MONSTER_DROP = 48838;
	// Misc
	private static final int REQUIRED_DROP_COUNT = 200;
	private static final int KILLING_NPCSTRING_ID = NpcStringId.LV_106_REGULAR_BARRIER_MAINTENANCE_IN_PROGRESS.getId();
	private static final QuestType QUEST_TYPE = QuestType.DAILY; // REPEATABLE, ONE_TIME, DAILY
	private static final boolean PARTY_QUEST = true;
	private static final int KILLING_COND = 1;
	private static final int FINISH_COND = 2;
	private static final int MIN_LEVEL = 106;
	
	public Q00529_RegularBarrierMaintenance()
	{
		super(529);
		addStartNpc(START_NPC);
		addTalkId(START_NPC);
		addKillId(MONSTERS);
		registerQuestItems(MONSTER_DROP);
		addCondMinLevel(MIN_LEVEL, getNoQuestMsg(null));
		addCondCompletedQuest(Q10529_IvoryTowersResearchFloatingSeaJournal.class.getSimpleName(), getNoQuestMsg(null));
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
			case "accept.htm":
			{
				if (qs.isCreated())
				{
					qs.startQuest();
					qs.setCond(KILLING_COND);
				}
				break;
			}
			case "reward.html":
			{
				if (qs.isCond(FINISH_COND) && (getQuestItemsCount(player, MONSTER_DROP) >= REQUIRED_DROP_COUNT))
				{
					takeItems(player, MONSTER_DROP, -1);
					// Reward.
					addExpAndSp(player, 49763842650L, 49763790);
					giveAdena(player, 3225882, false);
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
					if (qs.isCond(KILLING_COND))
					{
						htmltext = "accept.htm";
					}
					else if (qs.isCond(FINISH_COND))
					{
						htmltext = "finish.html";
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
		if ((qs != null) && qs.isCond(KILLING_COND))
		{
			final Player player = qs.getPlayer();
			if (giveItemRandomly(player, npc, MONSTER_DROP, 1, REQUIRED_DROP_COUNT, 1, true))
			{
				qs.setCond(FINISH_COND, true);
			}
			sendNpcLogList(player);
		}
	}
	
	@Override
	public Set<NpcLogListHolder> getNpcLogList(Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(KILLING_COND))
		{
			final Set<NpcLogListHolder> holder = new HashSet<>();
			holder.add(new NpcLogListHolder(KILLING_NPCSTRING_ID, true, (int) getQuestItemsCount(player, MONSTER_DROP)));
			return holder;
		}
		return super.getNpcLogList(player);
	}
}
