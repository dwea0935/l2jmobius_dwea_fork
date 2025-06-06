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
package quests.custom.Q00684_DisturbedFields;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.NpcLogListHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.QuestType;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;

/**
 * Disturbed Fields (684)
 * @URL https://l2wiki.com/Disturbed_Fields
 * @author Liamxroy
 */
public class Q00684_DisturbedFields extends Quest
{
	// NPCs
	private static final int START_NPC_1 = 33907;
	private static final int START_NPC_2 = 34481;
	private static final int[] MONSTERS =
	{
		24304,
		24305,
		24306,
		24307,
		24308,
	};
	// Misc
	private static final int REQUIRED_KILL_COUNT = 150;
	private static final String KILL_COUNT_VAR = "KillCount";
	private static final int KILLING_NPCSTRING_ID = NpcStringId.DEFEAT_MONSTERS_IN_THE_FIELD_OF_WHISPERS_2.getId();
	private static final QuestType QUEST_TYPE = QuestType.DAILY; // REPEATABLE, ONE_TIME, DAILY
	private static final boolean PARTY_QUEST = true;
	private static final int KILLING_COND = 1;
	private static final int FINISH_COND = 2;
	private static final int MIN_LEVEL = 108;
	
	public Q00684_DisturbedFields()
	{
		super(684);
		addStartNpc(START_NPC_1, START_NPC_2);
		addTalkId(START_NPC_1, START_NPC_2);
		addKillId(MONSTERS);
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
			case "accept.htm":
			case "accept2.htm":
			{
				if (qs.isCreated())
				{
					qs.startQuest();
					qs.setCond(KILLING_COND);
				}
				break;
			}
			case "reward.html":
			case "reward2.html":
			{
				if (qs.isCond(FINISH_COND) && (qs.getInt(KILL_COUNT_VAR) >= REQUIRED_KILL_COUNT))
				{
					// Reward.
					addExpAndSp(player, 74386893330L, 74386890);
					giveAdena(player, 4027449, false);
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
		if (npc.getId() == START_NPC_1)
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
		
		if (npc.getId() == START_NPC_2)
		{
			switch (qs.getState())
			{
				case State.CREATED:
				{
					htmltext = "start2.htm";
					break;
				}
				case State.STARTED:
				{
					if (qs.isCond(KILLING_COND))
					{
						htmltext = "accept2.htm";
					}
					else if (qs.isCond(FINISH_COND))
					{
						htmltext = "finish2.html";
					}
					break;
				}
				case State.COMPLETED:
				{
					if (qs.isNowAvailable())
					{
						qs.setState(State.CREATED);
						htmltext = "start2.htm";
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
			final int killCount = qs.getInt(KILL_COUNT_VAR) + 1;
			if (killCount <= REQUIRED_KILL_COUNT)
			{
				qs.set(KILL_COUNT_VAR, killCount);
				if (killCount >= REQUIRED_KILL_COUNT)
				{
					qs.setCond(FINISH_COND, true);
				}
				else // if (killCount < REQUIRED_KILL_COUNT)
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					sendNpcLogList(player);
				}
			}
		}
	}
	
	@Override
	public Set<NpcLogListHolder> getNpcLogList(Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(KILLING_COND))
		{
			final Set<NpcLogListHolder> holder = new HashSet<>();
			holder.add(new NpcLogListHolder(KILLING_NPCSTRING_ID, true, qs.getInt(KILL_COUNT_VAR)));
			return holder;
		}
		return super.getNpcLogList(player);
	}
}
