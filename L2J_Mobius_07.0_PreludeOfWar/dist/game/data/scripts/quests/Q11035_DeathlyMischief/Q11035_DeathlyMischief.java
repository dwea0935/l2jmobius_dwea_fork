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
package quests.Q11035_DeathlyMischief;

import org.l2jmobius.gameserver.managers.QuestManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.ExTutorialShowId;

import quests.Q11025_PathOfDestinyProving.Q11025_PathOfDestinyProving;

/**
 * Deathly Mischief (11035)
 * @URL https://l2wiki.com/Deathly_Mischief
 * @author Dmitri
 */
public class Q11035_DeathlyMischief extends Quest
{
	// NPCs
	private static final int ZENATH = 33509;
	private static final int TARTI = 34505;
	private static final int PHANTOM_SKELETON_SOLDIER = 24389;
	private static final int SKELETON_BERSERKER = 24390;
	// Items
	private static final int BREATH_OF_DEATH = 80672;
	private static final ItemHolder SOE_TARTI = new ItemHolder(80677, 1);
	// Location
	private static final Location TRAINING_GROUNDS_TELEPORT = new Location(-51130, 110053, -3664);
	// Misc
	private static final int MIN_LEVEL = 33;
	
	public Q11035_DeathlyMischief()
	{
		super(11035);
		addStartNpc(ZENATH);
		addTalkId(ZENATH, TARTI);
		addKillId(PHANTOM_SKELETON_SOLDIER, SKELETON_BERSERKER);
		registerQuestItems(SOE_TARTI.getId(), BREATH_OF_DEATH);
		addCondMinLevel(MIN_LEVEL, "33509-05.html");
		setQuestNameNpcStringId(NpcStringId.LV_20_40_DEATHLY_MISCHIEF);
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
			case "33509-02.html":
			case "33509-03.html":
			{
				htmltext = event;
				break;
			}
			case "33509-04.html":
			{
				qs.startQuest();
				player.sendPacket(new ExTutorialShowId(17)); // Adventurers Guide
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
					addExpAndSp(player, 4952686, 4457);
					giveAdena(player, 165000, true);
					showOnScreenMsg(player, NpcStringId.SECOND_CLASS_TRANSFER_IS_AVAILABLE_NGO_SEE_TARTI_IN_THE_TOWN_OF_GLUDIO_TO_START_THE_CLASS_TRANSFER, ExShowScreenMessage.TOP_CENTER, 10000);
					qs.exitQuest(false, true);
					htmltext = event;
					
					// Initialize next quest.
					final Quest nextQuest = QuestManager.getInstance().getQuest(Q11025_PathOfDestinyProving.class.getSimpleName());
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
				if (npc.getId() == ZENATH)
				{
					htmltext = "33509-01.html";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case ZENATH:
					{
						if (qs.isCond(1))
						{
							htmltext = "33509-04.html";
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
		if ((qs != null) && qs.isCond(1) && giveItemRandomly(killer, BREATH_OF_DEATH, 1, 15, 0.5, true))
		{
			qs.setCond(2, true);
			giveItems(killer, SOE_TARTI);
			showOnScreenMsg(killer, NpcStringId.USE_SCROLL_OF_ESCAPE_TARTI_IN_YOUR_INVENTORY_NTALK_TO_TARTI_TO_COMPLETE_THE_QUEST, ExShowScreenMessage.TOP_CENTER, 10000);
		}
	}
}
