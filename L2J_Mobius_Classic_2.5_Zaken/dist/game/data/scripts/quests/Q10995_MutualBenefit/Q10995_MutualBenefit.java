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
package quests.Q10995_MutualBenefit;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Mutual Benefit (10995)
 * @author Stayway
 */
public class Q10995_MutualBenefit extends Quest
{
	// NPCs
	private static final int NEWBIE_GUIDE = 30601;
	private static final int REEP = 30516;
	// Items
	private static final int BALCK_WOLF_TOOTH = 90284;
	private static final int GOBLINS_NAVIGATION_DEVICE = 90285;
	private static final int UTUKU_ORC_AMULET = 90286;
	private static final int GOBLIN_BRIGANDS_OLD_SWORD = 90287;
	private static final int GARUM_WEREWOLF_TAIL = 90288;
	private static final int GOBLIN_BRIGAND_LIEUTENANT_NECKLACE = 90289;
	private static final int BOUNTY_POSTER = 90283;
	// Rewards
	private static final int SILVERSMITH_HAMMER = 49053;
	private static final int RING_NOVICE = 29497;
	private static final int NECKLACE_NOVICE = 49039;
	// Monsters
	private static final int BLACK_WOLF = 20317;
	private static final int GOBLIN_SNOOPER = 20327;
	private static final int UTUKU_ORC = 20446;
	private static final int UTUKU_ORC_ARCHER = 20447;
	private static final int GOBLIN_BRIGAND = 20322;
	private static final int GARUM_WEREWOLF = 20307;
	private static final int GOBLIN_BRIGAND_LIEUTENANT = 20324;
	// Misc
	private static final int MIN_LEVEL = 2;
	private static final int MAX_LEVEL = 20;
	
	public Q10995_MutualBenefit()
	{
		super(10995);
		addStartNpc(NEWBIE_GUIDE);
		addTalkId(NEWBIE_GUIDE, REEP);
		addKillId(BLACK_WOLF, GOBLIN_SNOOPER, UTUKU_ORC, UTUKU_ORC_ARCHER, GOBLIN_BRIGAND, GARUM_WEREWOLF, GOBLIN_BRIGAND_LIEUTENANT);
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "no-level.html");
		addCondRace(Race.DWARF, "no-race.html");
		registerQuestItems(BOUNTY_POSTER, BALCK_WOLF_TOOTH, GOBLINS_NAVIGATION_DEVICE, UTUKU_ORC_AMULET, GOBLIN_BRIGANDS_OLD_SWORD, GARUM_WEREWOLF_TAIL, GOBLIN_BRIGAND_LIEUTENANT_NECKLACE);
		setQuestNameNpcStringId(NpcStringId.LV_2_20_MUTUAL_BENEFIT);
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
			case "30601-02.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "reward1":
			{
				if (qs.isCond(7))
				{
					takeItems(player, BOUNTY_POSTER, 1);
					takeItems(player, BALCK_WOLF_TOOTH, 10);
					takeItems(player, GOBLINS_NAVIGATION_DEVICE, 10);
					takeItems(player, UTUKU_ORC_AMULET, 10);
					takeItems(player, GOBLIN_BRIGANDS_OLD_SWORD, 10);
					takeItems(player, GARUM_WEREWOLF_TAIL, 10);
					giveItems(player, SILVERSMITH_HAMMER, 1);
					giveItems(player, RING_NOVICE, 2);
					giveItems(player, NECKLACE_NOVICE, 1);
					addExpAndSp(player, 70000, 0);
					qs.exitQuest(false, true);
					htmltext = "30516-03.html";
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
		if ((qs != null) && qs.isStarted() && LocationUtil.checkIfInRange(1500, npc, killer, true))
		{
			switch (npc.getId())
			{
				case BLACK_WOLF:
				{
					if ((qs.isCond(2) && (getQuestItemsCount(killer, BALCK_WOLF_TOOTH) < 10)))
					{
						giveItems(killer, BALCK_WOLF_TOOTH, 1);
						if ((getQuestItemsCount(killer, BALCK_WOLF_TOOTH) >= 10) && (getQuestItemsCount(killer, GOBLINS_NAVIGATION_DEVICE) >= 10))
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							showOnScreenMsg(killer, NpcStringId.YOU_HAVE_KILLED_ENOUGH_BLACK_WOLVES_AND_GOBLIN_SNOOPERS_NGO_HUNTING_AND_KILL_UTUKU_ORCS_AND_UTUKU_ORC_ARCHERS, ExShowScreenMessage.TOP_CENTER, 10000);
							qs.setCond(3);
						}
						else
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				}
				case GOBLIN_SNOOPER:
				{
					if ((qs.isCond(2) && (getQuestItemsCount(killer, GOBLINS_NAVIGATION_DEVICE) < 10)))
					{
						giveItems(killer, GOBLINS_NAVIGATION_DEVICE, 1);
						if ((getQuestItemsCount(killer, BALCK_WOLF_TOOTH) >= 10) && (getQuestItemsCount(killer, GOBLINS_NAVIGATION_DEVICE) >= 10))
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							showOnScreenMsg(killer, NpcStringId.YOU_HAVE_KILLED_ENOUGH_BLACK_WOLVES_AND_GOBLIN_SNOOPERS_NGO_HUNTING_AND_KILL_UTUKU_ORCS_AND_UTUKU_ORC_ARCHERS, ExShowScreenMessage.TOP_CENTER, 10000);
							qs.setCond(3);
						}
						else
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				}
				case UTUKU_ORC:
				case UTUKU_ORC_ARCHER:
				{
					if (qs.isCond(3) && (getQuestItemsCount(killer, UTUKU_ORC_AMULET) < 10))
					{
						giveItems(killer, UTUKU_ORC_AMULET, 1);
						if (getQuestItemsCount(killer, UTUKU_ORC_AMULET) >= 10)
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							showOnScreenMsg(killer, NpcStringId.YOU_HAVE_KILLED_ENOUGH_UTUKU_ORCS_AND_UTUKU_ORC_ARCHERS_NGO_HUNTING_AND_KILL_GOBLIN_BRIGANDS, ExShowScreenMessage.TOP_CENTER, 10000);
							qs.setCond(4);
						}
						else
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				}
				case GOBLIN_BRIGAND:
				{
					if (qs.isCond(4) && (getQuestItemsCount(killer, GOBLIN_BRIGANDS_OLD_SWORD) < 10))
					{
						giveItems(killer, GOBLIN_BRIGANDS_OLD_SWORD, 1);
						if (getQuestItemsCount(killer, GOBLIN_BRIGANDS_OLD_SWORD) >= 10)
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							showOnScreenMsg(killer, NpcStringId.YOU_HAVE_KILLED_ENOUGH_GOBLIN_BRIGANDS_N_GO_HUNTING_AND_KILL_GARUM_WEREWOLVES, ExShowScreenMessage.TOP_CENTER, 10000);
							qs.setCond(5);
						}
						else
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				}
				case GARUM_WEREWOLF:
				{
					if (qs.isCond(5) && (getQuestItemsCount(killer, GARUM_WEREWOLF_TAIL) < 10))
					{
						giveItems(killer, GARUM_WEREWOLF_TAIL, 1);
						if (getQuestItemsCount(killer, GARUM_WEREWOLF_TAIL) >= 10)
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							showOnScreenMsg(killer, NpcStringId.YOU_HAVE_KILLED_ENOUGH_GARUM_WEREWOLVES_N_GO_HUNTING_AND_KILL_GOBLIN_BRIGAND_LIEUTENANTS, ExShowScreenMessage.TOP_CENTER, 10000);
							qs.setCond(6);
						}
						else
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				}
				case GOBLIN_BRIGAND_LIEUTENANT:
				{
					if (qs.isCond(6) && (getQuestItemsCount(killer, GOBLIN_BRIGAND_LIEUTENANT_NECKLACE) < 10))
					{
						giveItems(killer, GOBLIN_BRIGAND_LIEUTENANT_NECKLACE, 1);
						if (getQuestItemsCount(killer, GOBLIN_BRIGAND_LIEUTENANT_NECKLACE) >= 10)
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							showOnScreenMsg(killer, NpcStringId.YOU_HAVE_KILLED_ENOUGH_GOBLIN_BRIGAND_LIEUTENANTS_NRETURN_TO_WEAPON_MERCHANT_REEP, ExShowScreenMessage.TOP_CENTER, 10000);
							qs.setCond(7);
						}
						else
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				}
			}
		}
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		final QuestState qs = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		switch (qs.getState())
		{
			case State.CREATED:
			{
				if (npc.getId() == NEWBIE_GUIDE)
				{
					htmltext = "30601-01.html";
				}
				break;
			}
			case State.STARTED:
			{
				if (npc.getId() == NEWBIE_GUIDE)
				{
					switch (qs.getCond())
					{
						case 1:
						{
							htmltext = "30601-02a.html";
							break;
						}
					}
					break;
				}
				else if (npc.getId() == REEP)
				{
					switch (qs.getCond())
					{
						case 1:
						{
							htmltext = "30516-01.htm";
							qs.setCond(2, true);
							showOnScreenMsg(talker, NpcStringId.GO_HUNTING_AND_KILL_BLACK_WOLVES_AND_GOBLIN_SNOOPERS, ExShowScreenMessage.TOP_CENTER, 10000);
							giveItems(talker, BOUNTY_POSTER, 1);
							break;
						}
						case 2:
						{
							htmltext = "30516-01a.html";
							break;
						}
						case 7:
						{
							htmltext = "30516-02.html";
							break;
						}
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
		return htmltext;
	}
}
