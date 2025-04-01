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
package quests.Q11034_ResurrectedOne;

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
import org.l2jmobius.gameserver.network.serverpackets.ExTutorialShowId;

import quests.Q11035_DeathlyMischief.Q11035_DeathlyMischief;

/**
 * Resurrected One (11034)
 * @URL https://l2wiki.com/Resurrected_One
 * @author Dmitri
 */
public class Q11034_ResurrectedOne extends Quest
{
	// NPCs
	private static final int KALESIN = 33177;
	private static final int ZENATH = 33509;
	private static final int SKELETON_SCOUT = 24386;
	private static final int SKELETON_ARCHER = 24387;
	private static final int SKELETON_WARRIOR = 24388;
	// Items
	private static final ItemHolder SOE_ZENATH = new ItemHolder(80680, 1);
	// Location
	private static final Location TRAINING_GROUNDS_TELEPORT = new Location(-46169, 110937, -3808);
	// Misc
	private static final String KILL_COUNT_VAR = "KillCount";
	private static final int MIN_LEVEL = 27;
	
	public Q11034_ResurrectedOne()
	{
		super(11034);
		addStartNpc(KALESIN);
		addTalkId(KALESIN, ZENATH);
		addKillId(SKELETON_SCOUT, SKELETON_ARCHER, SKELETON_WARRIOR);
		registerQuestItems(SOE_ZENATH.getId());
		addCondMinLevel(MIN_LEVEL, "33177-05.html");
		setQuestNameNpcStringId(NpcStringId.LV_20_40_THE_RESURRECTED_ONE);
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
			case "33177-02.html":
			case "33177-03.html":
			{
				htmltext = event;
				break;
			}
			case "33177-04.html":
			{
				qs.startQuest();
				player.sendPacket(new ExTutorialShowId(19)); // Adventurers Guide
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
			case "33509-02.html":
			{
				if (qs.isCond(2))
				{
					addExpAndSp(player, 1640083, 1476);
					qs.exitQuest(false, true);
					htmltext = event;
					
					// Initialize next quest.
					final Quest nextQuest = QuestManager.getInstance().getQuest(Q11035_DeathlyMischief.class.getSimpleName());
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
				if (npc.getId() == KALESIN)
				{
					htmltext = "33177-01.html";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case KALESIN:
					{
						if (qs.isCond(1))
						{
							htmltext = "33177-04.html";
						}
						break;
					}
					case ZENATH:
					{
						if (qs.isCond(2))
						{
							htmltext = "33509-01.html";
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
				giveItems(killer, SOE_ZENATH);
				showOnScreenMsg(killer, NpcStringId.USE_SCROLL_OF_ESCAPE_ZENATH_IN_YOUR_INVENTORY_NTALK_TO_ZENATH_TO_COMPLETE_THE_QUEST, ExShowScreenMessage.TOP_CENTER, 10000);
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
			holder.add(new NpcLogListHolder(NpcStringId.DEFEAT_SKELETONS_3.getId(), true, qs.getInt(KILL_COUNT_VAR)));
			return holder;
		}
		return super.getNpcLogList(player);
	}
}
