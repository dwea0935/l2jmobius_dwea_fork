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
package quests.Q10589_WhereFatesIntersect;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.NpcLogListHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.QuestType;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExTutorialShowId;

import quests.Q11027_PathOfDestinyOvercome.Q11027_PathOfDestinyOvercome;

/**
 * Where Fates Intersect (10589)
 * @URL https://l2wiki.com/Where_Fates_Intersect
 * @author NightBR
 */
public class Q10589_WhereFatesIntersect extends Quest
{
	// NPCs
	private static final int TARTI = 34505;
	private static final int HERPHAH = 34362;
	private static final int VOLLODOS = 30137;
	private static final int JOACHIM = 34513;
	private static final int[] MONSTERS =
	{
		24452, // Doom Soldier
		24453, // Doom Servant
		24454, // Doom Berserker
		24455, // Doom Seer
	};
	// Items
	private static final int MONSTER_DROP = 80853; // Undead Blood
	// Misc
	private static final int REQUIRED_DROP_COUNT = 200;
	private static final int KILLING_NPCSTRING_ID1 = NpcStringId.LV_85_WHERE_FATES_INTERSECT_IN_PROGRESS.getId();
	private static final int KILLING_NPCSTRING_ID2 = NpcStringId.LV_85_WHERE_FATES_INTERSECT_2.getId();
	private static final int REACH_LV_95 = NpcStringId.REACH_LV_95.getId();
	private static final QuestType QUEST_TYPE = QuestType.ONE_TIME; // REPEATABLE, ONE_TIME, DAILY
	private static final int KILLING_COND = 3;
	private static final int FINISH_COND = 4;
	// Rewards
	private static final int REWARD_ITEM1 = 80908; // Lv. 95 Achievement Reward Box
	private static final int REWARD_ITEM1_AMOUNT = 1;
	// Location
	private static final Location TOWN_OF_ADEN = new Location(146568, 26808, -2208);
	private static final Location ALTAR_OF_EVIL = new Location(-14088, 22168, -3626);
	
	public Q10589_WhereFatesIntersect()
	{
		super(10589);
		addStartNpc(TARTI);
		addTalkId(TARTI, HERPHAH, VOLLODOS, JOACHIM);
		addKillId(MONSTERS);
		registerQuestItems(MONSTER_DROP);
		addCondCompletedQuest(Q11027_PathOfDestinyOvercome.class.getSimpleName(), getNoQuestMsg(null));
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
			case "34505-01.htm":
			case "34505-02.htm":
			case "34505-04.html":
			case "34362-02.html":
			case "34362-05.html":
			case "30137-02.html":
			case "34513-02.html":
			{
				htmltext = event;
				break;
			}
			case "34505-03.htm":
			{
				// Show Service/Help/View Map page
				player.sendPacket(new ExTutorialShowId(12));
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "34362-03.html":
			{
				// Show Service/Help/Adventure's Guide page
				player.sendPacket(new ExTutorialShowId(25));
				qs.setCond(2, true);
				htmltext = event;
				break;
			}
			case "34362-06.html":
			{
				if (qs.isCond(4))
				{
					// Check player level it must be 95+
					qs.setCond(5, true);
					htmltext = event;
				}
				break;
			}
			case "30137-03.html":
			{
				qs.setCond(3, true);
				htmltext = event;
				break;
			}
			case "34513-03.html":
			{
				if (qs.isCond(5))
				{
					takeItems(player, MONSTER_DROP, -1);
					// Reward.
					rewardItems(player, REWARD_ITEM1, REWARD_ITEM1_AMOUNT);
					qs.exitQuest(QUEST_TYPE, true);
				}
				break;
			}
			case "townofaden":
			{
				giveStoryBuffReward(npc, player);
				player.teleToLocation(TOWN_OF_ADEN); // Town of Aden near Npc Herphah
				break;
			}
			case "altarofevil":
			{
				giveStoryBuffReward(npc, player);
				player.teleToLocation(ALTAR_OF_EVIL); // Altar of Evil near Npc Vollodos
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
				htmltext = "34505-00.htm";
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case TARTI:
					{
						if (qs.isCond(1))
						{
							htmltext = "34505-04.html";
						}
						break;
					}
					case HERPHAH:
					{
						if (qs.isCond(1))
						{
							htmltext = "34362-01.html";
						}
						else if (qs.isCond(2))
						{
							htmltext = "34362-07.html";
						}
						else if (qs.isCond(4))
						{
							htmltext = "34362-04.html";
						}
						else if (qs.isCond(5))
						{
							htmltext = "34362-06.html";
						}
						break;
					}
					case VOLLODOS:
					{
						if (qs.isCond(2))
						{
							htmltext = "30137-01.html";
						}
						else if (qs.isCond(3))
						{
							htmltext = "30137-04.html";
						}
						break;
					}
					case JOACHIM:
					{
						if (qs.isCond(5))
						{
							htmltext = "34513-01.html";
							break;
						}
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				if (qs.isNowAvailable())
				{
					qs.setState(State.CREATED);
					htmltext = "34505-00.htm";
				}
				else
				{
					htmltext = getAlreadyCompletedMsg(player, QUEST_TYPE);
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		executeForEachPlayer(player, npc, isSummon, true, false);
	}
	
	@Override
	public void actionForEachPlayer(Player player, Npc npc, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(KILLING_COND) && player.isInsideRadius3D(npc, Config.ALT_PARTY_RANGE))
		{
			if (getQuestItemsCount(player, MONSTER_DROP) < REQUIRED_DROP_COUNT)
			{
				giveItems(player, MONSTER_DROP, 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
			if ((getQuestItemsCount(player, MONSTER_DROP) >= REQUIRED_DROP_COUNT) && (player.getLevel() >= 95))
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
			if (player.getLevel() >= 95)
			{
				holder.add(new NpcLogListHolder(REACH_LV_95, true, 1));
			}
			holder.add(new NpcLogListHolder(KILLING_NPCSTRING_ID1, true, (int) getQuestItemsCount(player, MONSTER_DROP)));
			holder.add(new NpcLogListHolder(KILLING_NPCSTRING_ID2, true, (int) getQuestItemsCount(player, MONSTER_DROP)));
			return holder;
		}
		return super.getNpcLogList(player);
	}
}
