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
package quests.Q10518_SucceedingThePriestess;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.Id;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcMenuSelect;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.quest.NpcLogListHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.QuestType;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.util.ArrayUtil;

import quests.Q10516_UnveiledFafurionTemple.Q10516_UnveiledFafurionTemple;

/**
 * Succeeding the Priestess (10518)
 * @author Kazumi
 */
public final class Q10518_SucceedingThePriestess extends Quest
{
	// NPCs
	private static final int LIONEL = 33907;
	// MOBs
	private static final int[] FIELD_MOBS =
	{
		24304, // Groz Kropiora
		24305, // Groz Krotania
		24306, // Groz Krophy
		24307, // Groz Krotany
		24308, // Water Drake
		24309, // Krophy
		24310, // Krotany
		24311, // Kropiora
		24312, // Krotania
		24313, // Spiz Krophy
		24314, // Spiz Krotany
		24315, // Spiz Kropiora
		24316, // Spiz Krotania
	};
	private static final int[] FAFURION_MOBS =
	{
		24318, // Temple Guard Captain
		24319, // Elite Guardian Warrior
		24320, // Elite Guardian Archer
		24321, // Temple Patrol Guard
		24322, // Temple Knight Recruit
		24323, // Temple Guard
		24324, // Temple Guardian Warrior
		24325, // Temple Wizard
		24326, // Temple Guardian Wizard
		24327, // Temple Priest
		24328, // Temple Guardian Priest
		24329, // Starving Water Dragon
	};
	// Item
	private static final int DEFORMED_CLAW = 80325;
	private static final int BROKEN_CONTRACT_FRAGMENTS = 80326;
	private static final int TOP_GRADE_SPIRIT_STONE = 45932;
	// Misc
	private static final int MIN_LEVEL = 110;
	private static final int REQUIRED_DROP_COUNT_1 = 10;
	private static final int REQUIRED_DROP_COUNT_2 = 30;
	private static final int REQUIRED_KILL_COUNT = 50;
	private static final String KILL_COUNT_VAR = "KillCount_10518";
	
	public Q10518_SucceedingThePriestess()
	{
		super(10518);
		addStartNpc(LIONEL);
		addTalkId(LIONEL);
		addKillId(FIELD_MOBS);
		addKillId(FAFURION_MOBS);
		registerQuestItems(DEFORMED_CLAW, BROKEN_CONTRACT_FRAGMENTS);
		addCondMinLevel(MIN_LEVEL, "rupicia_q10517_02.htm");
		addCondCompletedQuest(Q10516_UnveiledFafurionTemple.class.getSimpleName(), "rupicia_q10517_02.htm");
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
			htmltext = "lionel_hunter_q10518_04.htm";
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
					htmltext = "lionel_hunter_q10518_01.htm";
					break;
				}
				break;
			}
			case State.STARTED:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						htmltext = "lionel_hunter_q10518_05.htm";
						break;
					}
					case 2:
					{
						htmltext = "lionel_hunter_q10518_07.htm";
						break;
					}
					case 3:
					{
						htmltext = "lionel_hunter_q10518_10.htm";
						break;
					}
					case 4:
					{
						htmltext = "lionel_hunter_q10518_11.htm";
						break;
					}
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
	
	@RegisterEvent(EventType.ON_NPC_MENU_SELECT)
	@RegisterType(ListenerRegisterType.NPC)
	@Id(LIONEL)
	public final void onNpcMenuSelect(OnNpcMenuSelect event)
	{
		final Player player = event.getTalker();
		final QuestState qs = getQuestState(player, false);
		final Npc npc = event.getNpc();
		final int ask = event.getAsk();
		final int reply = event.getReply();
		
		if (ask == 10518)
		{
			switch (reply)
			{
				case 1:
				{
					showHtmlFile(player, "lionel_hunter_q10518_02.htm", npc);
					break;
				}
				case 2:
				{
					showHtmlFile(player, "lionel_hunter_q10518_03.htm", npc);
					break;
				}
				case 3:
				{
					showHtmlFile(player, "lionel_hunter_q10518_08.htm", npc);
					break;
				}
				case 4:
				{
					qs.setCond(3);
					showHtmlFile(player, "lionel_hunter_q10518_09.htm", npc);
					break;
				}
				case 10:
				{
					if (player.getLevel() >= MIN_LEVEL)
					{
						if (qs.isCond(2))
						{
							qs.exitQuest(QuestType.ONE_TIME, true);
							giveItems(player, TOP_GRADE_SPIRIT_STONE, 1);
							addExpAndSp(player, 500056821000L, 500056740);
							showHtmlFile(player, "lionel_hunter_q10518_12.htm", npc);
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
		if (qs != null)
		{
			if (qs.isCond(1) && ArrayUtil.contains(FIELD_MOBS, npc.getId()))
			{
				giveItemRandomly(player, npc, DEFORMED_CLAW, 1, REQUIRED_DROP_COUNT_1, 0.5, true);
				
				final int killCount = qs.getInt(KILL_COUNT_VAR) + 1;
				if (killCount <= REQUIRED_KILL_COUNT)
				{
					qs.set(KILL_COUNT_VAR, killCount);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				
				if ((killCount >= REQUIRED_KILL_COUNT) && (getQuestItemsCount(player, DEFORMED_CLAW) >= REQUIRED_DROP_COUNT_1))
				{
					qs.setCond(2, true);
				}
				
				sendNpcLogList(player);
			}
			else if (qs.isCond(3) && ArrayUtil.contains(FAFURION_MOBS, npc.getId()))
			{
				if (giveItemRandomly(player, npc, BROKEN_CONTRACT_FRAGMENTS, 1, REQUIRED_DROP_COUNT_2, 1, true))
				{
					qs.setCond(4, true);
				}
			}
		}
	}
	
	@Override
	public Set<NpcLogListHolder> getNpcLogList(Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(1))
		{
			final Set<NpcLogListHolder> holder = new HashSet<>();
			holder.add(new NpcLogListHolder(551810, true, qs.getInt(KILL_COUNT_VAR))); // Defeat Krofins
			holder.add(new NpcLogListHolder(80325, true, (int) getQuestItemsCount(player, DEFORMED_CLAW))); // Deformed Claw
			return holder;
		}
		return super.getNpcLogList(player);
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public final void onLogin(OnPlayerLogin evt)
	{
		final Player player = evt.getPlayer();
		sendNpcLogList(player);
	}
}
