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
package quests.Q00775_RetrievingTheChaosFragment;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Faction;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.QuestType;
import org.l2jmobius.gameserver.model.quest.State;

import quests.Q10455_ElikiasLetter.Q10455_ElikiasLetter;

/**
 * Retrieving the Fragment of Chaos (775)
 * @URL https://l2wiki.com/Retrieving_the_Fragment_of_Chaos
 * @author Dmitri, CostyKiller
 */
public class Q00775_RetrievingTheChaosFragment extends Quest
{
	// NPCs
	private static final int LEONA_BLACKBIRD = 31595;
	// Monsters
	private static final int[] MONSTERS =
	{
		// Desert Quarry
		23811, // Cantera Tanya
		23812, // Cantera Deathmoz
		23813, // Cantera Floxis
		23814, // Cantera Belika
		23815, // Cantera Bridget
		
		// Beleth Magic Circle
		23354, // Decay Hannibal
		23355, // Armor Beast
		23356, // Klein Soldier
		23357, // Disorder Warrior
		23360, // Bizuard
		23361, // Mutated Fly
	};
	// Misc
	private static final int MIN_LEVEL = 99;
	// Items
	private static final int CHAOS_FRAGMENT = 37766;
	private static final int BASIC_SUPPLY_BOX = 47172;
	private static final int INTERMEDIATE_SUPPLY_BOX = 47173;
	private static final int ADVANCED_SUPPLY_BOX = 47174;
	
	public Q00775_RetrievingTheChaosFragment()
	{
		super(775);
		addStartNpc(LEONA_BLACKBIRD);
		addTalkId(LEONA_BLACKBIRD);
		addKillId(MONSTERS);
		registerQuestItems(CHAOS_FRAGMENT);
		addCondMinLevel(MIN_LEVEL, "31595-00.htm");
		addCondCompletedQuest(Q10455_ElikiasLetter.class.getSimpleName(), "31595-00.htm");
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
			case "31595-02.htm":
			case "31595-03.htm":
			case "31595-04.htm":
			case "31595-04a.htm":
			case "31595-04b.htm":
			case "31595-06.html":
			case "31595-06a.html":
			case "31595-06b.html":
			{
				htmltext = event;
				break;
			}
			case "select_mission":
			{
				qs.startQuest();
				if ((player.getFactionLevel(Faction.BLACKBIRD_CLAN) >= 1) && (player.getFactionLevel(Faction.BLACKBIRD_CLAN) < 2))
				{
					htmltext = "31595-04a.htm";
					break;
				}
				else if (player.getFactionLevel(Faction.BLACKBIRD_CLAN) >= 2)
				{
					htmltext = "31595-04b.htm";
					break;
				}
				htmltext = "31595-04.htm";
				break;
			}
			case "return":
			{
				if ((player.getFactionLevel(Faction.BLACKBIRD_CLAN) >= 1) && (player.getFactionLevel(Faction.BLACKBIRD_CLAN) < 2))
				{
					htmltext = "31595-04a.htm";
					break;
				}
				else if (player.getFactionLevel(Faction.BLACKBIRD_CLAN) >= 2)
				{
					htmltext = "31595-04b.htm";
					break;
				}
				htmltext = "31595-04.htm";
				break;
			}
			case "31595-07.html":
			{
				qs.setCond(2, true);
				htmltext = event;
				break;
			}
			case "31595-07a.html":
			{
				qs.setCond(3, true);
				htmltext = event;
				break;
			}
			case "31595-07b.html":
			{
				qs.setCond(4, true);
				htmltext = event;
				break;
			}
			case "31595-10.html":
			{
				final int chance = getRandom(100);
				switch (qs.getCond())
				{
					case 5:
					{
						if ((getQuestItemsCount(player, CHAOS_FRAGMENT) == 250) && (player.getLevel() >= MIN_LEVEL))
						{
							if (chance < 2)
							{
								giveItems(player, ADVANCED_SUPPLY_BOX, 1);
							}
							else if (chance < 20)
							{
								giveItems(player, INTERMEDIATE_SUPPLY_BOX, 1);
							}
							else if (chance < 100)
							{
								giveItems(player, BASIC_SUPPLY_BOX, 1);
							}
							addExpAndSp(player, 12_113_489_880L, 12_113_460);
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
						if ((getQuestItemsCount(player, CHAOS_FRAGMENT) == 500) && (player.getLevel() >= MIN_LEVEL))
						{
							if (chance < 2)
							{
								giveItems(player, ADVANCED_SUPPLY_BOX, 1);
							}
							else if (chance < 20)
							{
								giveItems(player, BASIC_SUPPLY_BOX, 1);
							}
							else if (chance < 100)
							{
								giveItems(player, INTERMEDIATE_SUPPLY_BOX, 1);
							}
							addExpAndSp(player, 24_226_979_760L, 24_226_920);
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
						if ((getQuestItemsCount(player, CHAOS_FRAGMENT) == 750) && (player.getLevel() >= MIN_LEVEL))
						{
							if (chance < 2)
							{
								giveItems(player, BASIC_SUPPLY_BOX, 1);
							}
							else if (chance < 20)
							{
								giveItems(player, INTERMEDIATE_SUPPLY_BOX, 1);
							}
							else if (chance < 100)
							{
								giveItems(player, ADVANCED_SUPPLY_BOX, 1);
							}
							addExpAndSp(player, 36_340_469_640L, 36_340_380);
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
				htmltext = "31595-01.htm";
				// fallthrough?
			}
			case State.STARTED:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						if ((player.getFactionLevel(Faction.BLACKBIRD_CLAN) >= 1) && (player.getFactionLevel(Faction.BLACKBIRD_CLAN) < 2))
						{
							htmltext = "31595-04a.htm";
							break;
						}
						else if (player.getFactionLevel(Faction.BLACKBIRD_CLAN) >= 2)
						{
							htmltext = "31595-04b.htm";
							break;
						}
						htmltext = "31595-04.htm";
						break;
					}
					case 2:
					{
						htmltext = "31595-08.html";
						break;
					}
					case 3:
					{
						htmltext = "31595-08a.html";
						break;
					}
					case 4:
					{
						htmltext = "31595-08b.html";
						break;
					}
					case 5:
					case 6:
					case 7:
					{
						htmltext = "31595-09.html";
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
					htmltext = "31595-01.htm";
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
					if (giveItemRandomly(player, npc, CHAOS_FRAGMENT, 1, 250, 1, true))
					{
						qs.setCond(5, true);
					}
					break;
				}
				case 3:
				{
					if (giveItemRandomly(player, npc, CHAOS_FRAGMENT, 1, 500, 1, true))
					{
						qs.setCond(6, true);
					}
					break;
				}
				case 4:
				{
					if (giveItemRandomly(player, npc, CHAOS_FRAGMENT, 1, 750, 1, true))
					{
						qs.setCond(7, true);
					}
					break;
				}
			}
		}
	}
}