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
package quests.Q11002_HelpWithTempleRestoration;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * Help with Temple Restoration (11002)
 * @author Stayway
 */
public class Q11002_HelpWithTempleRestoration extends Quest
{
	// NPCs
	private static final int ALTRAN = 30283;
	private static final int HARRYS = 30035;
	// Items
	private static final int WOODEN_POLE = 90205;
	private static final int WOODEN_DOOR_PANEL = 90206;
	private static final int STONE_POWDER = 90207;
	private static final int INVENTORY_BOOK = 90204;
	// Rewards
	private static final int WARRIORS_ARMOR = 90306;
	private static final int WARRIORS_GAITERS = 90307;
	private static final int MEDIUMS_TUNIC = 90308;
	private static final int MEDIUMS_STOCKINGS = 90309;
	private static final int EARRING_NOVICE = 29486;
	// Monsters
	private static final int ORC_LIEUTENANT = 20096;
	private static final int ORC_CAPTAIN = 20098;
	private static final int WEREWOLF_CHIEFTAIN = 20342;
	private static final int WEREWOLF_HUMTER = 20343;
	private static final int STONE_GOLEM = 20016;
	private static final int CRASHER = 20101;
	// Misc
	private static final int MIN_LEVEL = 11;
	private static final int MAX_LEVEL = 20;
	
	public Q11002_HelpWithTempleRestoration()
	{
		super(11002);
		addStartNpc(ALTRAN);
		addTalkId(HARRYS, ALTRAN);
		addKillId(ORC_CAPTAIN, ORC_LIEUTENANT, WEREWOLF_HUMTER, WEREWOLF_CHIEFTAIN, STONE_GOLEM, CRASHER);
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "no-level.html"); // Custom
		addCondRace(Race.HUMAN, "no-race.html"); // Custom
		registerQuestItems(INVENTORY_BOOK, WOODEN_POLE, WOODEN_DOOR_PANEL, STONE_POWDER);
		setQuestNameNpcStringId(NpcStringId.LV_11_20_HELP_WITH_TEMPLE_RESTORATION);
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
			case "30283-02.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "reward1":
			{
				if (qs.isCond(5))
				{
					takeItems(player, INVENTORY_BOOK, 1);
					takeItems(player, WOODEN_POLE, 20);
					takeItems(player, WOODEN_DOOR_PANEL, 25);
					takeItems(player, STONE_POWDER, 20);
					giveItems(player, WARRIORS_ARMOR, 1);
					giveItems(player, WARRIORS_GAITERS, 1);
					giveItems(player, EARRING_NOVICE, 2);
					addExpAndSp(player, 80000, 0);
					qs.exitQuest(false, true);
					htmltext = "30035-03.html";
				}
				break;
			}
			case "reward2":
			{
				if (qs.isCond(5))
				{
					takeItems(player, INVENTORY_BOOK, 1);
					takeItems(player, WOODEN_POLE, 20);
					takeItems(player, WOODEN_DOOR_PANEL, 25);
					takeItems(player, STONE_POWDER, 20);
					giveItems(player, MEDIUMS_TUNIC, 1);
					giveItems(player, MEDIUMS_STOCKINGS, 1);
					giveItems(player, EARRING_NOVICE, 2);
					addExpAndSp(player, 80000, 0);
					qs.exitQuest(false, true);
					htmltext = "30035-04.html";
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
		switch (qs.getState())
		{
			case State.CREATED:
			{
				if (npc.getId() == ALTRAN)
				{
					htmltext = "30283-01.html";
				}
				break;
			}
			case State.STARTED:
			{
				if (npc.getId() == ALTRAN)
				{
					if (qs.isCond(1))
					{
						htmltext = "30283-02a.html";
					}
					break;
				}
				else if (npc.getId() == HARRYS)
				{
					switch (qs.getCond())
					{
						case 1:
						{
							htmltext = "30035-01.htm";
							qs.setCond(2, true);
							showOnScreenMsg(talker, NpcStringId.YOU_HAVE_TALKED_TO_HARRYS_NGO_HUNTING_AND_KILL_ORC_LIEUTENANTS_AND_ORC_CAPTAINS, ExShowScreenMessage.TOP_CENTER, 10000);
							giveItems(talker, INVENTORY_BOOK, 1);
							break;
						}
						case 2:
						{
							htmltext = "30035-01a.html";
							break;
						}
						case 5:
						{
							htmltext = "30035-02.html";
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
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if (qs != null)
		{
			switch (npc.getId())
			{
				case ORC_CAPTAIN:
				case ORC_LIEUTENANT:
				{
					if (qs.isCond(2) && (getQuestItemsCount(killer, WOODEN_POLE) < 20) && (getRandom(100) < 84))
					{
						giveItems(killer, WOODEN_POLE, 1);
						if (getQuestItemsCount(killer, WOODEN_POLE) >= 20)
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							showOnScreenMsg(killer, NpcStringId.YOU_HAVE_KILLED_ENOUGH_ORC_LIEUTENANTS_AND_ORC_CAPTAINS_N_GO_HUNTING_AND_KILL_WEREWOLF_HUNTERS_AND_WEREWOLF_CHIEFTAINS, ExShowScreenMessage.TOP_CENTER, 10000);
							qs.setCond(3);
						}
						else
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				}
				case WEREWOLF_HUMTER:
				case WEREWOLF_CHIEFTAIN:
				{
					if (qs.isCond(3) && (getQuestItemsCount(killer, WOODEN_DOOR_PANEL) < 25) && (getRandom(100) < 87))
					{
						giveItems(killer, WOODEN_DOOR_PANEL, 1);
						if (getQuestItemsCount(killer, WOODEN_DOOR_PANEL) >= 25)
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							showOnScreenMsg(killer, NpcStringId.YOU_HAVE_KILLED_ENOUGH_WEREWOLF_HUNTERS_AND_WEREWOLF_CHIEFTAINS_N_GO_HUNTING_AND_KILL_STONE_GOLEMS_AND_CRASHERS, ExShowScreenMessage.TOP_CENTER, 10000);
							qs.setCond(4);
						}
						else
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				}
				case CRASHER:
				case STONE_GOLEM:
				{
					if (qs.isCond(4) && (getQuestItemsCount(killer, STONE_POWDER) < 20) && (getRandom(100) < 84))
					{
						giveItems(killer, STONE_POWDER, 1);
						if ((getQuestItemsCount(killer, STONE_POWDER) >= 20) && (getQuestItemsCount(killer, STONE_POWDER) >= 10))
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							showOnScreenMsg(killer, NpcStringId.YOU_HAVE_ALL_OF_THE_ITEMS_HARRYS_REQUESTED_RETURN_TO_HIM, ExShowScreenMessage.TOP_CENTER, 10000);
							qs.setCond(5);
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
}
