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
package quests.Q10958_ExploringNewOpportunities;

import java.util.HashSet;
import java.util.Set;

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

import quests.Q10957_TheLifeOfADeathKnight.Q10957_TheLifeOfADeathKnight;

/**
 * @author Mobius
 */
public class Q10958_ExploringNewOpportunities extends Quest
{
	// NPCs
	private static final int KILREMANGE = 34138;
	private static final int MATHORN = 34139;
	private static final int SKELETON_ARCHER = 22184;
	private static final int SKELETON_WARRIOR = 22185;
	// Items
	private static final ItemHolder SOE_MATHORN = new ItemHolder(93319, 1);
	private static final ItemHolder SOE_NOVICE = new ItemHolder(10650, 10);
	private static final ItemHolder RING_NOVICE = new ItemHolder(49041, 2);
	private static final ItemHolder EARRING_NOVICE = new ItemHolder(49040, 2);
	private static final ItemHolder NECKLACE_NOVICE = new ItemHolder(49039, 1);
	// Location
	private static final Location TRAINING_GROUNDS = new Location(39554, 149715, -3896);
	// Misc
	private static final String KILL_COUNT_VAR = "KillCount";
	
	public Q10958_ExploringNewOpportunities()
	{
		super(10958);
		addStartNpc(KILREMANGE);
		addTalkId(KILREMANGE, MATHORN);
		addKillId(SKELETON_ARCHER, SKELETON_WARRIOR);
		addCondCompletedQuest(Q10957_TheLifeOfADeathKnight.class.getSimpleName(), "34138-02.htm");
		registerQuestItems(SOE_MATHORN.getId());
		setQuestNameNpcStringId(NpcStringId.LV_2_15_EXPLORING_NEW_OPPORTUNITIES);
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
			case "34138-00.html":
			case "34138-01.htm":
			case "34138-02.htm":
			case "34138-03.htm":
			{
				htmltext = event;
				break;
			}
			case "34138-04.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "teleport":
			{
				if (qs.isCond(1))
				{
					giveStoryBuffReward(npc, player);
					player.teleToLocation(TRAINING_GROUNDS);
				}
				break;
			}
			case "34139-02.html":
			{
				if (qs.isCond(2))
				{
					showOnScreenMsg(player, NpcStringId.YOU_WILL_RECEIVE_REWARDS_FOR_COMPLETING_QUESTS_NCLICK_THE_QUEST_BUTTON_IN_THE_RIGHT_BOTTOM_CORNER_OF_YOUR_SCREEN_TO_OPEN_QUEST_WINDOW, ExShowScreenMessage.TOP_CENTER, 10000);
					giveStoryBuffReward(npc, player); // ?
					giveItems(player, SOE_NOVICE);
					giveItems(player, RING_NOVICE);
					giveItems(player, EARRING_NOVICE);
					giveItems(player, NECKLACE_NOVICE);
					giveStoryBuffReward(npc, player);
					addExpAndSp(player, 260000, 6000);
					qs.exitQuest(false, true);
				}
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
			case State.CREATED:
			{
				htmltext = "34138-01.htm";
				break;
			}
			case State.STARTED:
			{
				if (npc.getId() == KILREMANGE)
				{
					if (qs.isCond(1))
					{
						htmltext = "34138-04.htm";
					}
				}
				else if (npc.getId() == MATHORN)
				{
					if (qs.isCond(2))
					{
						htmltext = "34139-01.html";
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
			if (killCount < 20)
			{
				qs.set(KILL_COUNT_VAR, killCount);
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				sendNpcLogList(killer);
			}
			else
			{
				qs.setCond(2, true);
				qs.unset(KILL_COUNT_VAR);
				giveItems(killer, SOE_MATHORN);
				showOnScreenMsg(killer, NpcStringId.THE_TRAINING_IN_OVER_NUSE_A_SCROLL_OF_ESCAPE_IN_YOUR_INVENTORY_TO_GO_BACK_TO_QUARTERMASTER_MATHORN, ExShowScreenMessage.TOP_CENTER, 10000);
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
			holder.add(new NpcLogListHolder(NpcStringId.CLEAN_THE_FIRST_TRAINING_GROUNDS_UP.getId(), true, qs.getInt(KILL_COUNT_VAR)));
			return holder;
		}
		return super.getNpcLogList(player);
	}
}
