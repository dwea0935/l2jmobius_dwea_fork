/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package quests.Q00004_LongLiveThePaagrioLord;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;

/**
 * Long Live the Pa'agrio Lord (4)
 * @author malyelfik
 */
public class Q00004_LongLiveThePaagrioLord extends Quest
{
	// NPCs
	private static final int KUNAI = 30559;
	private static final int USKA = 30560;
	private static final int GROOKIN = 30562;
	private static final int VARKEES = 30566;
	private static final int NAKUSIN = 30578;
	private static final int HESTUI = 30585;
	private static final int URUTU = 30587;
	// Items
	private static final int HONEY_KHANDAR = 1541;
	private static final int BEAR_FUR_CLOAK = 1542;
	private static final int BLOODY_AXE = 1543;
	private static final int ANCESTOR_SKULL = 1544;
	private static final int SPIDER_DUST = 1545;
	private static final int DEEP_SEA_ORB = 1546;
	// Rewards
	private static final int CLUB = 49051; // Club (Novice)
	// Misc
	private static final int MIN_LEVEL = 2;
	private static final int MAX_LEVEL = 5;
	
	public Q00004_LongLiveThePaagrioLord()
	{
		super(4);
		addStartNpc(NAKUSIN);
		addTalkId(NAKUSIN, VARKEES, URUTU, HESTUI, KUNAI, USKA, GROOKIN);
		registerQuestItems(HONEY_KHANDAR, BEAR_FUR_CLOAK, BLOODY_AXE, ANCESTOR_SKULL, SPIDER_DUST, DEEP_SEA_ORB);
		addCondMaxLevel(MAX_LEVEL, "30578-01.htm");
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = event;
		switch (event)
		{
			case "30578-03.htm":
			{
				qs.startQuest();
				break;
			}
			case "30578-05.html":
			{
				break;
			}
			default:
			{
				htmltext = null;
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
		switch (npc.getId())
		{
			case NAKUSIN:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getRace() != Race.ORC) ? "30578-00.htm" : (player.getLevel() >= MIN_LEVEL) ? "30578-02.htm" : "30578-01.htm";
						break;
					}
					case State.STARTED:
					{
						if (qs.isCond(1))
						{
							htmltext = "30578-04.html";
						}
						else
						{
							// Newbie Guide
							showOnScreenMsg(player, NpcStringId.DELIVERY_DUTY_COMPLETE_GO_FIND_THE_NEWBIE_HELPER, 2, 5000);
							rewardItems(player, CLUB, 1);
							qs.exitQuest(false, true);
							htmltext = "30578-06.html";
						}
						break;
					}
					case State.COMPLETED:
					{
						htmltext = getAlreadyCompletedMsg(player);
						break;
					}
				}
				break;
			}
			case VARKEES:
			{
				htmltext = giveItem(player, qs, npc.getId(), HONEY_KHANDAR, getRegisteredItemIds());
				break;
			}
			case URUTU:
			{
				htmltext = giveItem(player, qs, npc.getId(), DEEP_SEA_ORB, getRegisteredItemIds());
				break;
			}
			case HESTUI:
			{
				htmltext = giveItem(player, qs, npc.getId(), BEAR_FUR_CLOAK, getRegisteredItemIds());
				break;
			}
			case KUNAI:
			{
				htmltext = giveItem(player, qs, npc.getId(), SPIDER_DUST, getRegisteredItemIds());
				break;
			}
			case USKA:
			{
				htmltext = giveItem(player, qs, npc.getId(), ANCESTOR_SKULL, getRegisteredItemIds());
				break;
			}
			case GROOKIN:
			{
				htmltext = giveItem(player, qs, npc.getId(), BLOODY_AXE, getRegisteredItemIds());
				break;
			}
		}
		return htmltext;
	}
	
	private static String giveItem(Player player, QuestState qs, int npcId, int itemId, int... items)
	{
		if (!qs.isStarted())
		{
			return getNoQuestMsg(player);
		}
		else if (hasQuestItems(player, itemId))
		{
			return npcId + "-02.html";
		}
		giveItems(player, itemId, 1);
		playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		if (hasQuestItems(player, items))
		{
			qs.setCond(2, true);
		}
		return npcId + "-01.html";
	}
}