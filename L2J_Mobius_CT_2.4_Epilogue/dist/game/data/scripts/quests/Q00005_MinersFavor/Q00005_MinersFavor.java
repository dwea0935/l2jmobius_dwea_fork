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
package quests.Q00005_MinersFavor;

import org.l2jmobius.gameserver.managers.QuestManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

import ai.others.NewbieGuide.NewbieGuide;

/**
 * Miner's Favor (5)
 * @author malyelfik
 */
public class Q00005_MinersFavor extends Quest
{
	// NPCs
	private static final int BOLTER = 30554;
	private static final int SHARI = 30517;
	private static final int GARITA = 30518;
	private static final int REED = 30520;
	private static final int BRUNON = 30526;
	// Items
	private static final int BOLTERS_LIST = 1547;
	private static final int MINING_BOOTS = 1548;
	private static final int MINERS_PICK = 1549;
	private static final int BOOMBOOM_POWDER = 1550;
	private static final int REDSTONE_BEER = 1551;
	private static final int BOLTERS_SMELLY_SOCKS = 1552;
	private static final int NECKLACE = 906;
	// Misc
	private static final int MIN_LEVEL = 2;
	private static final int GUIDE_MISSION = 41;
	
	public Q00005_MinersFavor()
	{
		super(5, "Miner's Favor");
		addStartNpc(BOLTER);
		addTalkId(BOLTER, SHARI, GARITA, REED, BRUNON);
		registerQuestItems(BOLTERS_LIST, MINING_BOOTS, MINERS_PICK, BOOMBOOM_POWDER, REDSTONE_BEER, BOLTERS_SMELLY_SOCKS);
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
			case "30554-03.htm":
			{
				qs.startQuest();
				giveItems(player, BOLTERS_LIST, 1);
				giveItems(player, BOLTERS_SMELLY_SOCKS, 1);
				break;
			}
			case "30526-02.html":
			{
				if (!hasQuestItems(player, BOLTERS_SMELLY_SOCKS))
				{
					return "30526-04.html";
				}
				takeItems(player, BOLTERS_SMELLY_SOCKS, -1);
				giveItems(player, MINERS_PICK, 1);
				checkProgress(player, qs);
				break;
			}
			case "30554-05.html":
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
			case BOLTER:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getLevel() >= MIN_LEVEL) ? "30554-02.htm" : "30554-01.html";
						break;
					}
					case State.STARTED:
					{
						if (qs.isCond(1))
						{
							htmltext = "30554-04.html";
						}
						else
						{
							giveItems(player, NECKLACE, 1);
							addExpAndSp(player, 5672, 446);
							giveAdena(player, 2466, true);
							qs.exitQuest(false, true);
							
							// Newbie Guide.
							final Quest newbieGuide = QuestManager.getInstance().getQuest(NewbieGuide.class.getSimpleName());
							if (newbieGuide != null)
							{
								final QuestState newbieGuideQs = newbieGuide.getQuestState(player, true);
								if (!haveNRMemo(newbieGuideQs, GUIDE_MISSION))
								{
									setNRMemo(newbieGuideQs, GUIDE_MISSION);
									setNRMemoState(newbieGuideQs, GUIDE_MISSION, 1);
									showOnScreenMsg(player, "Delivery duty complete. \\n Go find the Newbie Guide.", 2, 5000);
								}
								else if ((getNRMemoState(newbieGuideQs, GUIDE_MISSION) % 10) != 1)
								{
									setNRMemo(newbieGuideQs, GUIDE_MISSION);
									setNRMemoState(newbieGuideQs, GUIDE_MISSION, getNRMemoState(newbieGuideQs, GUIDE_MISSION) + 1);
									showOnScreenMsg(player, "Delivery duty complete. \\n Go find the Newbie Guide.", 2, 5000);
								}
							}
							
							htmltext = "30554-06.html";
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
			case BRUNON:
			{
				if (qs.isStarted())
				{
					htmltext = (hasQuestItems(player, MINERS_PICK)) ? "30526-03.html" : "30526-01.html";
				}
				break;
			}
			case REED:
			{
				htmltext = giveItem(player, qs, npc.getId(), REDSTONE_BEER);
				break;
			}
			case SHARI:
			{
				htmltext = giveItem(player, qs, npc.getId(), BOOMBOOM_POWDER);
				break;
			}
			case GARITA:
			{
				htmltext = giveItem(player, qs, npc.getId(), MINING_BOOTS);
				break;
			}
		}
		return htmltext;
	}
	
	private static void checkProgress(Player player, QuestState qs)
	{
		if (hasQuestItems(player, BOLTERS_LIST, MINING_BOOTS, MINERS_PICK, BOOMBOOM_POWDER, REDSTONE_BEER))
		{
			qs.setCond(2, true);
		}
	}
	
	private static String giveItem(Player player, QuestState qs, int npcId, int itemId)
	{
		if (!qs.isStarted())
		{
			return getNoQuestMsg(qs.getPlayer());
		}
		else if (hasQuestItems(player, itemId))
		{
			return npcId + "-02.html";
		}
		giveItems(player, itemId, 1);
		playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		checkProgress(player, qs);
		return npcId + "-01.html";
	}
}