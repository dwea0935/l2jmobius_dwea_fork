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
package quests.Q11040_AttackOfTheEnragedForest;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.managers.QuestManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.quest.NpcLogListHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

import quests.Q11026_PathOfDestinyConviction.Q11026_PathOfDestinyConviction;
import quests.Q11039_CommunicationBreakdown.Q11039_CommunicationBreakdown;

/**
 * Attack of the Enraged Forest (11040)
 * @URL https://l2wiki.com/Attack_of_the_Enraged_Forest
 * @author Dmitri, Mobius
 */
public class Q11040_AttackOfTheEnragedForest extends Quest
{
	// NPCs
	private static final int PIO = 33963;
	private static final int TARTI = 34505;
	private static final int TINY_WHIRLWIND = 24401;
	private static final int GIANT_WHIRLWIND = 24402;
	// Items
	private static final ItemHolder SOE_TARTI = new ItemHolder(80677, 1);
	// Location
	private static final Location TRAINING_GROUNDS_TELEPORT = new Location(-91374, 92270, -3360);
	// Misc
	private static final String KILL_COUNT_VAR = "KillCount";
	private static final int MIN_LEVEL = 66;
	
	public Q11040_AttackOfTheEnragedForest()
	{
		super(11040);
		addStartNpc(PIO);
		addTalkId(PIO, TARTI);
		addKillId(TINY_WHIRLWIND, GIANT_WHIRLWIND);
		registerQuestItems(SOE_TARTI.getId());
		addCondMinLevel(MIN_LEVEL, "33963-06.html");
		addCondCompletedQuest(Q11039_CommunicationBreakdown.class.getSimpleName(), "33963-06");
		setQuestNameNpcStringId(NpcStringId.LV_40_76_ATTACK_OF_THE_ENRAGED_FOREST);
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
			case "33963-03.html":
			case "33963-04.html":
			case "33963-05.html":
			{
				htmltext = event;
				break;
			}
			case "33963-02.html":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "teleport":
			{
				if (qs.isCond(1))
				{
					player.teleToLocation(TRAINING_GROUNDS_TELEPORT);
				}
				break;
			}
			case "34505-02.html":
			{
				if (qs.isCond(2))
				{
					addExpAndSp(player, 834929477, 595042);
					giveAdena(player, 240000, true);
					qs.exitQuest(false, true);
					htmltext = event;
					
					// Initialize next quest.
					final Quest nextQuest = QuestManager.getInstance().getQuest(Q11026_PathOfDestinyConviction.class.getSimpleName());
					if (nextQuest != null)
					{
						nextQuest.newQuestState(player);
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
				if (npc.getId() == PIO)
				{
					htmltext = "33963-01.html";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case PIO:
					{
						if (qs.isCond(1))
						{
							htmltext = "33963-02.html";
						}
						break;
					}
					case TARTI:
					{
						if (qs.isCond(2))
						{
							htmltext = "34505-01.html";
						}
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
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && qs.isCond(1))
		{
			final int killCount = qs.getInt(KILL_COUNT_VAR) + 1;
			if (killCount < 30)
			{
				qs.set(KILL_COUNT_VAR, killCount);
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				sendNpcLogList(killer);
			}
			else
			{
				qs.setCond(2, true);
				qs.unset(KILL_COUNT_VAR);
				giveItems(killer, SOE_TARTI);
				showOnScreenMsg(killer, NpcStringId.USE_SCROLL_OF_ESCAPE_TARTI_IN_YOUR_INVENTORY_NTALK_TO_TARTI_TO_COMPLETE_THE_QUEST, ExShowScreenMessage.TOP_CENTER, 10000);
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
			holder.add(new NpcLogListHolder(NpcStringId.DEFEAT_THE_PACK_OF_WINDIMA_2.getId(), true, qs.getInt(KILL_COUNT_VAR)));
			return holder;
		}
		return super.getNpcLogList(player);
	}
}
