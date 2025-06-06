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
package quests.Q00108_JumbleTumbleDiamondFuss;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.managers.QuestManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.util.LocationUtil;

import ai.others.NewbieGuide.NewbieGuide;

/**
 * Jumble, Tumble, Diamond Fuss (108)
 * @author Janiko
 */
public class Q00108_JumbleTumbleDiamondFuss extends Quest
{
	// NPCs
	private static final int COLLECTOR_GOUPH = 30523;
	private static final int TRADER_REEP = 30516;
	private static final int CARRIER_TOROCCO = 30555;
	private static final int MINER_MARON = 30529;
	private static final int BLACKSMITH_BRUNON = 30526;
	private static final int WAREHOUSE_KEEPER_MURDOC = 30521;
	private static final int WAREHOUSE_KEEPER_AIRY = 30522;
	// Monsters
	private static final int GOBLIN_BRIGAND_LEADER = 20323;
	private static final int GOBLIN_BRIGAND_LIEUTENANT = 20324;
	private static final int BLADE_BAT = 20480;
	// Items
	private static final int GOUPHS_CONTRACT = 1559;
	private static final int REEPS_CONTRACT = 1560;
	private static final int ELVEN_WINE = 1561;
	private static final int BRUNONS_DICE = 1562;
	private static final int BRUNONS_CONTRACT = 1563;
	private static final int AQUAMARINE = 1564;
	private static final int CHRYSOBERYL = 1565;
	private static final int GEM_BOX = 1566;
	private static final int COAL_PIECE = 1567;
	private static final int BRUNONS_LETTER = 1568;
	private static final int BERRY_TART = 1569;
	private static final int BAT_DIAGRAM = 1570;
	private static final int STAR_DIAMOND = 1571;
	// Rewards
	private static final ItemHolder[] REWARDS =
	{
		new ItemHolder(1060, 100), // Lesser Healing Potion
		new ItemHolder(4412, 10), // Echo Crystal - Theme of Battle
		new ItemHolder(4413, 10), // Echo Crystal - Theme of Love
		new ItemHolder(4414, 10), // Echo Crystal - Theme of Solitude
		new ItemHolder(4415, 10), // Echo Crystal - Theme of Feast
		new ItemHolder(4416, 10), // Echo Crystal - Theme of Celebration
	};
	private static final int SILVERSMITH_HAMMER = 1511;
	private static final ItemHolder SPIRITSHOTS_NO_GRADE_FOR_ROOKIES = new ItemHolder(5790, 3000);
	private static final ItemHolder SOULSHOTS_NO_GRADE_FOR_ROOKIES = new ItemHolder(5789, 7000);
	// Misc
	private static final int MIN_LEVEL = 10;
	private static final int MAX_GEM_COUNT = 10;
	private static final Map<Integer, Double> GOBLIN_DROP_CHANCES = new HashMap<>();
	static
	{
		GOBLIN_DROP_CHANCES.put(GOBLIN_BRIGAND_LEADER, 0.8);
		GOBLIN_DROP_CHANCES.put(GOBLIN_BRIGAND_LIEUTENANT, 0.6);
	}
	private static final int GUIDE_MISSION = 41;
	
	public Q00108_JumbleTumbleDiamondFuss()
	{
		super(108);
		addStartNpc(COLLECTOR_GOUPH);
		addTalkId(COLLECTOR_GOUPH, TRADER_REEP, CARRIER_TOROCCO, MINER_MARON, BLACKSMITH_BRUNON, WAREHOUSE_KEEPER_MURDOC, WAREHOUSE_KEEPER_AIRY);
		addKillId(GOBLIN_BRIGAND_LEADER, GOBLIN_BRIGAND_LIEUTENANT, BLADE_BAT);
		registerQuestItems(GOUPHS_CONTRACT, REEPS_CONTRACT, ELVEN_WINE, BRUNONS_DICE, BRUNONS_CONTRACT, AQUAMARINE, CHRYSOBERYL, GEM_BOX, COAL_PIECE, BRUNONS_LETTER, BERRY_TART, BAT_DIAGRAM, STAR_DIAMOND);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		String htmltext = null;
		if (qs == null)
		{
			return htmltext;
		}
		switch (event)
		{
			case "30523-04.htm":
			{
				if (qs.isCreated())
				{
					qs.startQuest();
					giveItems(player, GOUPHS_CONTRACT, 1);
					htmltext = event;
				}
				break;
			}
			case "30555-02.html":
			{
				if (qs.isCond(2) && hasQuestItems(player, REEPS_CONTRACT))
				{
					takeItems(player, REEPS_CONTRACT, -1);
					giveItems(player, ELVEN_WINE, 1);
					qs.setCond(3, true);
					htmltext = event;
				}
				break;
			}
			case "30526-02.html":
			{
				if (qs.isCond(4) && hasQuestItems(player, BRUNONS_DICE))
				{
					takeItems(player, BRUNONS_DICE, -1);
					giveItems(player, BRUNONS_CONTRACT, 1);
					qs.setCond(5, true);
					htmltext = event;
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		final QuestState qs = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		
		switch (npc.getId())
		{
			case COLLECTOR_GOUPH:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						if (talker.getRace() != Race.DWARF)
						{
							htmltext = "30523-01.htm";
						}
						else if (talker.getLevel() < MIN_LEVEL)
						{
							htmltext = "30523-02.htm";
						}
						else
						{
							htmltext = "30523-03.htm";
						}
						break;
					}
					case State.STARTED:
					{
						switch (qs.getCond())
						{
							case 1:
							{
								if (hasQuestItems(talker, GOUPHS_CONTRACT))
								{
									htmltext = "30523-05.html";
								}
								break;
							}
							case 2:
							case 3:
							case 4:
							case 5:
							case 6:
							{
								if (hasAtLeastOneQuestItem(talker, REEPS_CONTRACT, ELVEN_WINE, BRUNONS_DICE, BRUNONS_CONTRACT))
								{
									htmltext = "30523-06.html";
								}
								break;
							}
							case 7:
							{
								if (hasQuestItems(talker, GEM_BOX))
								{
									takeItems(talker, GEM_BOX, -1);
									giveItems(talker, COAL_PIECE, 1);
									qs.setCond(8, true);
									htmltext = "30523-07.html";
								}
								break;
							}
							case 8:
							case 9:
							case 10:
							case 11:
							{
								if (hasAtLeastOneQuestItem(talker, COAL_PIECE, BRUNONS_LETTER, BERRY_TART, BAT_DIAGRAM))
								{
									htmltext = "30523-08.html";
								}
								break;
							}
							case 12:
							{
								if (hasQuestItems(talker, STAR_DIAMOND))
								{
									if (talker.getLevel() < 25)
									{
										if (talker.isMageClass())
										{
											giveItems(talker, SPIRITSHOTS_NO_GRADE_FOR_ROOKIES);
											playSound(talker, "tutorial_voice_027");
										}
										else
										{
											giveItems(talker, SOULSHOTS_NO_GRADE_FOR_ROOKIES);
											playSound(talker, "tutorial_voice_026");
										}
									}
									
									// Newbie Guide.
									final Quest newbieGuide = QuestManager.getInstance().getQuest(NewbieGuide.class.getSimpleName());
									if (newbieGuide != null)
									{
										final QuestState newbieGuideQs = newbieGuide.getQuestState(talker, true);
										if (!haveNRMemo(newbieGuideQs, GUIDE_MISSION))
										{
											setNRMemo(newbieGuideQs, GUIDE_MISSION);
											setNRMemoState(newbieGuideQs, GUIDE_MISSION, 100000);
											showOnScreenMsg(talker, NpcStringId.ACQUISITION_OF_RACE_SPECIFIC_WEAPON_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
										}
										else
										{
											if (((getNRMemoState(newbieGuideQs, GUIDE_MISSION) % 1000000) / 100000) != 1)
											{
												setNRMemo(newbieGuideQs, GUIDE_MISSION);
												setNRMemoState(newbieGuideQs, GUIDE_MISSION, getNRMemoState(newbieGuideQs, GUIDE_MISSION) + 100000);
												showOnScreenMsg(talker, NpcStringId.ACQUISITION_OF_RACE_SPECIFIC_WEAPON_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
											}
										}
									}
									
									addExpAndSp(talker, 34565, 2962);
									giveAdena(talker, 14666, true);
									for (ItemHolder reward : REWARDS)
									{
										giveItems(talker, reward);
									}
									giveItems(talker, SILVERSMITH_HAMMER, 1);
									qs.exitQuest(false, true);
									talker.sendPacket(new SocialAction(talker.getObjectId(), 3));
									htmltext = "30523-09.html";
								}
								break;
							}
						}
						break;
					}
					case State.COMPLETED:
					{
						htmltext = getAlreadyCompletedMsg(talker);
						break;
					}
				}
				break;
			}
			case TRADER_REEP:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						if (hasQuestItems(talker, GOUPHS_CONTRACT))
						{
							takeItems(talker, GOUPHS_CONTRACT, -1);
							giveItems(talker, REEPS_CONTRACT, 1);
							qs.setCond(2, true);
							htmltext = "30516-01.html";
						}
						break;
					}
					case 2:
					{
						if (hasQuestItems(talker, REEPS_CONTRACT))
						{
							htmltext = "30516-02.html";
						}
						break;
					}
					default:
					{
						if (qs.getCond() > 2)
						{
							htmltext = "30516-02.html";
						}
						break;
					}
				}
				break;
			}
			case CARRIER_TOROCCO:
			{
				switch (qs.getCond())
				{
					case 2:
					{
						if (hasQuestItems(talker, REEPS_CONTRACT))
						{
							htmltext = "30555-01.html";
						}
						break;
					}
					case 3:
					{
						if (hasQuestItems(talker, ELVEN_WINE))
						{
							htmltext = "30555-03.html";
						}
						break;
					}
					case 7:
					{
						if (hasQuestItems(talker, GEM_BOX))
						{
							htmltext = "30555-04.html";
						}
						break;
					}
					default:
					{
						if (qs.isStarted())
						{
							htmltext = "30555-05.html";
						}
						break;
					}
				}
				break;
			}
			case MINER_MARON:
			{
				switch (qs.getCond())
				{
					case 3:
					{
						if (hasQuestItems(talker, ELVEN_WINE))
						{
							takeItems(talker, ELVEN_WINE, -1);
							giveItems(talker, BRUNONS_DICE, 1);
							qs.setCond(4, true);
							htmltext = "30529-01.html";
						}
						break;
					}
					case 4:
					{
						if (hasQuestItems(talker, BRUNONS_DICE))
						{
							htmltext = "30529-02.html";
						}
						break;
					}
					default:
					{
						if (qs.getCond() > 4)
						{
							htmltext = "30529-03.html";
						}
						break;
					}
				}
				break;
			}
			case BLACKSMITH_BRUNON:
			{
				switch (qs.getCond())
				{
					case 4:
					{
						if (hasQuestItems(talker, BRUNONS_DICE))
						{
							htmltext = "30526-01.html";
						}
						break;
					}
					case 5:
					{
						if (hasQuestItems(talker, BRUNONS_CONTRACT))
						{
							htmltext = "30526-03.html";
						}
						break;
					}
					case 6:
					{
						if (hasQuestItems(talker, BRUNONS_CONTRACT) && (getQuestItemsCount(talker, AQUAMARINE) >= MAX_GEM_COUNT) && (getQuestItemsCount(talker, CHRYSOBERYL) >= MAX_GEM_COUNT))
						{
							takeItems(talker, -1, BRUNONS_CONTRACT, AQUAMARINE, CHRYSOBERYL);
							giveItems(talker, GEM_BOX, 1);
							qs.setCond(7, true);
							htmltext = "30526-04.html";
						}
						break;
					}
					case 7:
					{
						if (hasQuestItems(talker, GEM_BOX))
						{
							htmltext = "30526-05.html";
						}
						break;
					}
					case 8:
					{
						if (hasQuestItems(talker, COAL_PIECE))
						{
							takeItems(talker, COAL_PIECE, -1);
							giveItems(talker, BRUNONS_LETTER, 1);
							qs.setCond(9, true);
							htmltext = "30526-06.html";
						}
						break;
					}
					case 9:
					{
						if (hasQuestItems(talker, BRUNONS_LETTER))
						{
							htmltext = "30526-07.html";
						}
						break;
					}
					case 10:
					case 11:
					case 12:
					{
						if (hasAtLeastOneQuestItem(talker, BERRY_TART, BAT_DIAGRAM, STAR_DIAMOND))
						{
							htmltext = "30526-08.html";
						}
						break;
					}
				}
				break;
			}
			case WAREHOUSE_KEEPER_MURDOC:
			{
				switch (qs.getCond())
				{
					case 9:
					{
						if (hasQuestItems(talker, BRUNONS_LETTER))
						{
							takeItems(talker, BRUNONS_LETTER, -1);
							giveItems(talker, BERRY_TART, 1);
							qs.setCond(10, true);
							htmltext = "30521-01.html";
						}
						break;
					}
					case 10:
					{
						if (hasQuestItems(talker, BERRY_TART))
						{
							htmltext = "30521-02.html";
						}
						break;
					}
					case 11:
					case 12:
					{
						htmltext = "30521-03.html";
						break;
					}
				}
				break;
			}
			case WAREHOUSE_KEEPER_AIRY:
			{
				switch (qs.getCond())
				{
					case 10:
					{
						if (hasQuestItems(talker, BERRY_TART))
						{
							takeItems(talker, BERRY_TART, -1);
							giveItems(talker, BAT_DIAGRAM, 1);
							qs.setCond(11, true);
							htmltext = "30522-01.html";
						}
						break;
					}
					case 11:
					{
						if (hasQuestItems(talker, BAT_DIAGRAM))
						{
							htmltext = "30522-02.html";
						}
						break;
					}
					case 12:
					{
						if (hasQuestItems(talker, STAR_DIAMOND))
						{
							htmltext = "30522-03.html";
						}
						break;
					}
					default:
					{
						if (qs.isStarted())
						{
							htmltext = "30522-04.html";
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
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && LocationUtil.checkIfInRange(Config.ALT_PARTY_RANGE, npc, killer, true))
		{
			switch (npc.getId())
			{
				case GOBLIN_BRIGAND_LEADER:
				case GOBLIN_BRIGAND_LIEUTENANT:
				{
					if (qs.isCond(5) && hasQuestItems(killer, BRUNONS_CONTRACT))
					{
						final double dropChance = GOBLIN_DROP_CHANCES.get(npc.getId());
						boolean playSound = false;
						if (giveItemRandomly(killer, npc, AQUAMARINE, 1, MAX_GEM_COUNT, dropChance, false))
						{
							if (getQuestItemsCount(killer, CHRYSOBERYL) >= MAX_GEM_COUNT)
							{
								qs.setCond(6, true);
								break;
							}
							
							playSound = true;
						}
						if (giveItemRandomly(killer, npc, CHRYSOBERYL, 1, MAX_GEM_COUNT, dropChance, false))
						{
							if (getQuestItemsCount(killer, AQUAMARINE) >= MAX_GEM_COUNT)
							{
								qs.setCond(6, true);
								break;
							}
							
							playSound = true;
						}
						
						if (playSound)
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				}
				case BLADE_BAT:
				{
					if (qs.isCond(11) && hasQuestItems(killer, BAT_DIAGRAM) && giveItemRandomly(killer, npc, STAR_DIAMOND, 1, 1, 0.2, true))
					{
						takeItems(killer, BAT_DIAGRAM, -1);
						qs.setCond(12);
					}
					break;
				}
			}
		}
	}
}