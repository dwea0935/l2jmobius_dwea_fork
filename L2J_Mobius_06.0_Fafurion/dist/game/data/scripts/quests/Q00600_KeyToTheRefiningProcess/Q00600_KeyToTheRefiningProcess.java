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
package quests.Q00600_KeyToTheRefiningProcess;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Faction;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.QuestType;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Key to the Refining Process (600)
 * @URL https://l2wiki.com/Key_to_the_Refining_Process
 * @author Dmitri
 */
public class Q00600_KeyToTheRefiningProcess extends Quest
{
	// NPCs
	private static final int DEVIANNE = 34427;
	// Monsters
	private static final int[] MONSTERS =
	{
		24148, // Death Pondus 105
		24153, // Devil Varos 105
		24158, // Demonic Weiss 105
		24159, // Atelia Yuyurina 104
		24160, // Atelia Popobena 105
		24161, // Harke 105
		24162, // Ergalion 105
		24163, // Spira 106
	};
	// Items
	private static final int FRAGMENT_OF_CONDENSED_ENERGY = 48549;
	// Misc
	private static final int MIN_LEVEL = 103;
	
	public Q00600_KeyToTheRefiningProcess()
	{
		super(600);
		addStartNpc(DEVIANNE);
		addTalkId(DEVIANNE);
		addKillId(MONSTERS);
		registerQuestItems(FRAGMENT_OF_CONDENSED_ENERGY);
		addCondMinLevel(MIN_LEVEL, "34427-00.htm");
		addFactionLevel(Faction.BLACKBIRD_CLAN, 4, "34427-00.htm");
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
			case "34427-02.htm":
			case "34427-03.htm":
			case "34427-04.htm":
			case "34427-04a.htm":
			case "34427-04b.htm":
			case "34427-06.html":
			case "34427-06a.html":
			case "34427-06b.html":
			{
				htmltext = event;
				break;
			}
			case "select_mission":
			{
				qs.startQuest();
				if ((player.getFactionLevel(Faction.BLACKBIRD_CLAN) >= 8) && (player.getFactionLevel(Faction.BLACKBIRD_CLAN) < 9))
				{
					htmltext = "34427-04a.htm";
					break;
				}
				else if (player.getFactionLevel(Faction.BLACKBIRD_CLAN) >= 9)
				{
					htmltext = "34427-04b.htm";
					break;
				}
				htmltext = "34427-04.htm";
				break;
			}
			case "return":
			{
				if ((player.getFactionLevel(Faction.BLACKBIRD_CLAN) >= 8) && (player.getFactionLevel(Faction.BLACKBIRD_CLAN) < 9))
				{
					htmltext = "34427-04a.htm";
					break;
				}
				else if (player.getFactionLevel(Faction.BLACKBIRD_CLAN) >= 9)
				{
					htmltext = "34427-04b.htm";
					break;
				}
				htmltext = "34427-04.htm";
				break;
			}
			case "34427-07.html":
			{
				qs.setCond(2, true);
				htmltext = event;
				break;
			}
			case "34427-07a.html":
			{
				qs.setCond(3, true);
				htmltext = event;
				break;
			}
			case "34427-07b.html":
			{
				qs.setCond(4, true);
				htmltext = event;
				break;
			}
			case "34427-10.html":
			{
				switch (qs.getCond())
				{
					case 5:
					{
						if ((getQuestItemsCount(player, FRAGMENT_OF_CONDENSED_ENERGY) == 20) && (player.getLevel() >= MIN_LEVEL))
						{
							// if (chance < 30)
							// {
							// giveItems(player, SAVIORS_WISH, 2);
							// }
							// else if (chance < 100)
							// {
							// giveItems(player, SAVIORS_WISH, 1);
							// }
							giveAdena(player, 2495659, true);
							addExpAndSp(player, 27191624760L, 27191610);
							addFactionPoints(player, Faction.BLACKBIRD_CLAN, 100);
							qs.exitQuest(QuestType.DAILY, true);
							htmltext = event;
						}
						else
						{
							htmltext = getNoQuestLevelRewardMsg(player);
						}
						break;
					}
					case 6:
					{
						if ((getQuestItemsCount(player, FRAGMENT_OF_CONDENSED_ENERGY) == 40) && (player.getLevel() >= MIN_LEVEL))
						{
							// if (chance < 30)
							// {
							// giveItems(player, SAVIORS_WISH, 2);
							// }
							// else if (chance < 100)
							// {
							// giveItems(player, SAVIORS_WISH, 1);
							// }
							giveAdena(player, 4991318, true);
							addExpAndSp(player, 54383249520L, 54383220);
							addFactionPoints(player, Faction.BLACKBIRD_CLAN, 200);
							qs.exitQuest(QuestType.DAILY, true);
							htmltext = event;
						}
						else
						{
							htmltext = getNoQuestLevelRewardMsg(player);
						}
						break;
					}
					case 7:
					{
						if ((getQuestItemsCount(player, FRAGMENT_OF_CONDENSED_ENERGY) == 60) && (player.getLevel() >= MIN_LEVEL))
						{
							// if (chance < 30)
							// {
							// giveItems(player, SAVIORS_WISH, 2);
							// }
							// else if (chance < 100)
							// {
							// giveItems(player, SAVIORS_WISH, 1);
							// }
							giveAdena(player, 7486978, true);
							addExpAndSp(player, 81574874280L, 81574830);
							addFactionPoints(player, Faction.BLACKBIRD_CLAN, 300);
							qs.exitQuest(QuestType.DAILY, true);
							htmltext = event;
						}
						else
						{
							htmltext = getNoQuestLevelRewardMsg(player);
						}
						break;
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
				htmltext = "34427-01.htm";
				// fallthrough
			}
			case State.STARTED:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						if ((player.getFactionLevel(Faction.BLACKBIRD_CLAN) >= 8) && (player.getFactionLevel(Faction.BLACKBIRD_CLAN) < 9))
						{
							htmltext = "34427-04a.htm";
							break;
						}
						else if (player.getFactionLevel(Faction.BLACKBIRD_CLAN) >= 9)
						{
							htmltext = "34427-04b.htm";
							break;
						}
						htmltext = "34427-04.htm";
						break;
					}
					case 2:
					{
						htmltext = "34427-08.html";
						break;
					}
					case 3:
					{
						htmltext = "34427-08a.html";
						break;
					}
					case 4:
					{
						htmltext = "34427-08b.html";
						break;
					}
					case 5:
					case 6:
					case 7:
					{
						htmltext = "34427-09.html";
						break;
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				if (!qs.isNowAvailable())
				{
					htmltext = getAlreadyCompletedMsg(player, QuestType.DAILY);
				}
				else
				{
					qs.setState(State.CREATED);
					htmltext = "34427-01.htm";
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
		if ((qs != null) && (qs.getCond() > 1) && player.isInsideRadius3D(npc, Config.ALT_PARTY_RANGE))
		{
			switch (qs.getCond())
			{
				case 2:
				{
					if (giveItemRandomly(player, npc, FRAGMENT_OF_CONDENSED_ENERGY, 1, 20, 1, true))
					{
						qs.setCond(5, true);
					}
					break;
				}
				case 3:
				{
					if (giveItemRandomly(player, npc, FRAGMENT_OF_CONDENSED_ENERGY, 1, 40, 1, true))
					{
						qs.setCond(6, true);
					}
					break;
				}
				case 4:
				{
					if (giveItemRandomly(player, npc, FRAGMENT_OF_CONDENSED_ENERGY, 1, 60, 1, true))
					{
						qs.setCond(7, true);
					}
					break;
				}
			}
		}
	}
}
