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
package quests.Q11008_PreparationForDungeon;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

import quests.Q11007_NoiseInWoods.Q11007_NoiseInWoods;

/**
 * Preparation for Dungeon (11008)
 * @author Stayway
 */
public class Q11008_PreparationForDungeon extends Quest
{
	// NPCs
	private static final int KENDELL = 30218;
	private static final int STARDEN = 30220;
	// Items
	private static final int ORCS_BANDAGE = 90223;
	private static final int DRYADS_CRIMSON_HERB = 90224;
	private static final int SPIDER_VENOM = 90225;
	private static final int NOTE_ABOUT_REQUIRED_INGREDIENTS = 90222; // Need finish htm
	// Rewards
	private static final int WARRIORS_ARMOR = 90306;
	private static final int WARRIORS_GAITERS = 90307;
	private static final int MEDIUMS_TUNIC = 90308;
	private static final int MEDIUMS_STOCKINGS = 90309;
	private static final int EARRING_NOVICE = 29486;
	// Monsters
	private static final int KABOO_ORC_WARRIOR_CAPTAIN = 20472;
	private static final int KABOO_ORC_WARRIOR_LIEUTENANT = 20473;
	private static final int KABOO_ORC_WARRIOR = 20471;
	private static final int DRYAD = 20013;
	private static final int DRYAD_ELDER = 20019;
	private static final int HOOK_SPIDER = 20308;
	private static final int CRIMSON_SPIDER = 20460;
	private static final int PINCER_SPIDER = 20466;
	// Misc
	private static final int MIN_LEVEL = 11;
	private static final int MAX_LEVEL = 20;
	
	public Q11008_PreparationForDungeon()
	{
		super(11008);
		addStartNpc(KENDELL);
		addTalkId(KENDELL, STARDEN);
		addKillId(KABOO_ORC_WARRIOR, KABOO_ORC_WARRIOR_CAPTAIN, KABOO_ORC_WARRIOR_LIEUTENANT, DRYAD, DRYAD_ELDER, HOOK_SPIDER, CRIMSON_SPIDER, PINCER_SPIDER);
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "no-level.html"); // Custom
		addCondRace(Race.ELF, "no-race.html"); // Custom
		addCondCompletedQuest(Q11007_NoiseInWoods.class.getSimpleName(), "no-quest.html");
		registerQuestItems(NOTE_ABOUT_REQUIRED_INGREDIENTS, ORCS_BANDAGE, DRYADS_CRIMSON_HERB, SPIDER_VENOM);
		setQuestNameNpcStringId(NpcStringId.LV_11_20_PREPARATION_FOR_DUNGEON);
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
			case "30218-02.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "reward1":
			{
				if (qs.isCond(5))
				{
					takeItems(player, NOTE_ABOUT_REQUIRED_INGREDIENTS, 1);
					takeItems(player, ORCS_BANDAGE, 20);
					takeItems(player, DRYADS_CRIMSON_HERB, 20);
					takeItems(player, SPIDER_VENOM, 20);
					giveItems(player, WARRIORS_ARMOR, 1);
					giveItems(player, WARRIORS_GAITERS, 1);
					giveItems(player, EARRING_NOVICE, 2);
					addExpAndSp(player, 80000, 0);
					qs.exitQuest(false, true);
					htmltext = "30220-03.html";
				}
				break;
			}
			case "reward2":
			{
				if (qs.isCond(5))
				{
					takeItems(player, NOTE_ABOUT_REQUIRED_INGREDIENTS, 1);
					takeItems(player, ORCS_BANDAGE, 20);
					takeItems(player, DRYADS_CRIMSON_HERB, 20);
					takeItems(player, SPIDER_VENOM, 20);
					giveItems(player, MEDIUMS_TUNIC, 1);
					giveItems(player, MEDIUMS_STOCKINGS, 1);
					giveItems(player, EARRING_NOVICE, 2);
					addExpAndSp(player, 80000, 0);
					qs.exitQuest(false, true);
					htmltext = "30220-04.html"; // Custom
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
				if (npc.getId() == KENDELL)
				{
					htmltext = "30218-01.html";
				}
				break;
			}
			case State.STARTED:
			{
				if (npc.getId() == KENDELL)
				{
					if (qs.isCond(1))
					{
						htmltext = "30218-02a.html";
					}
					break;
				}
				else if (npc.getId() == STARDEN)
				{
					switch (qs.getCond())
					{
						case 1:
						{
							htmltext = "30220-01.htm";
							qs.setCond(2, true);
							showOnScreenMsg(talker, NpcStringId.GO_HUNTING_AND_KILL_KABOO_ORC_WARRIOR_LIEUTENANTS_AND_KABOO_ORC_WARRIOR_CAPTAINS, ExShowScreenMessage.TOP_CENTER, 10000);
							giveItems(talker, NOTE_ABOUT_REQUIRED_INGREDIENTS, 1);
							break;
						}
						case 2:
						{
							htmltext = "30220-01a.html";
							break;
						}
						case 5:
						{
							htmltext = "30220-02.html";
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
				case KABOO_ORC_WARRIOR:
				case KABOO_ORC_WARRIOR_CAPTAIN:
				case KABOO_ORC_WARRIOR_LIEUTENANT:
				{
					if (qs.isCond(2) && (getQuestItemsCount(killer, ORCS_BANDAGE) < 20) && (getRandom(100) < 90))
					{
						giveItems(killer, ORCS_BANDAGE, 1);
						if (getQuestItemsCount(killer, ORCS_BANDAGE) >= 20)
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							showOnScreenMsg(killer, NpcStringId.YOU_HAVE_KILLED_ENOUGH_KABOO_ORC_WARRIOR_LIEUTENANTS_AND_KABOO_ORC_WARRIOR_CAPTAINS_N_GO_HUNTING_AND_KILL_DRYADS, ExShowScreenMessage.TOP_CENTER, 10000);
							qs.setCond(3);
						}
						else
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				}
				case DRYAD:
				case DRYAD_ELDER:
				{
					if (qs.isCond(3) && (getQuestItemsCount(killer, DRYADS_CRIMSON_HERB) < 20) && (getRandom(100) < 90))
					{
						giveItems(killer, DRYADS_CRIMSON_HERB, 1);
						if (getQuestItemsCount(killer, DRYADS_CRIMSON_HERB) >= 20)
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							showOnScreenMsg(killer, NpcStringId.YOU_HAVE_KILLED_ENOUGH_DRYADS_N_GO_HUNTING_AND_KILL_CRIMSON_SPIDERS_HOOK_SPIDERS_AND_PINCER_SPIDERS, ExShowScreenMessage.TOP_CENTER, 10000);
							qs.setCond(4);
						}
						else
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				}
				case HOOK_SPIDER:
				case CRIMSON_SPIDER:
				case PINCER_SPIDER:
				{
					if (qs.isCond(4) && (getQuestItemsCount(killer, SPIDER_VENOM) < 20) && (getRandom(100) < 90))
					{
						giveItems(killer, SPIDER_VENOM, 1);
						if ((getQuestItemsCount(killer, SPIDER_VENOM) >= 20))
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							showOnScreenMsg(killer, NpcStringId.RETURN_TO_SENTINEL_STARDEN, ExShowScreenMessage.TOP_CENTER, 10000);
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
