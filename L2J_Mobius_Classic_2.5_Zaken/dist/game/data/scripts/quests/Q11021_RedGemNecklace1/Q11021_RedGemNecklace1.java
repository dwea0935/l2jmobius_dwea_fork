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
package quests.Q11021_RedGemNecklace1;

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
 * Red Gem Necklace (1/3) (11021)
 * @author Stayway
 */
public class Q11021_RedGemNecklace1 extends Quest
{
	// NPCs
	private static final int SUMARI = 30564;
	private static final int USKA = 30560;
	// Items
	private static final int BEARS_SHIN_BONE = 90275;
	private static final int SHARP_SPIDER_LEG = 90276;
	private static final int LIST_OF_MATERIALS = 90274;
	// Rewards
	private static final int SCROLL_OF_ESCAPE = 10650;
	private static final int HEALING_POTION = 1073;
	private static final int MP_RECOVERY_POTION = 90310;
	private static final int SOULSHOTS_NO_GRADE = 5789;
	private static final int SPIRITSHOT_NO_GRADE = 5790;
	// Monsters
	private static final int KASHA_BEAR = 20479;
	private static final int KASHA_SPIDER = 20474;
	private static final int KASHA_FANG_SPIDER = 20476;
	private static final int KASHA_BLADE_SPIDER = 20478;
	// Misc
	private static final int MIN_LEVEL = 15;
	private static final int MAX_LEVEL = 20;
	
	public Q11021_RedGemNecklace1()
	{
		super(11021);
		addStartNpc(SUMARI);
		addTalkId(SUMARI, USKA);
		addKillId(KASHA_BEAR, KASHA_SPIDER, KASHA_FANG_SPIDER, KASHA_BLADE_SPIDER);
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "no-level.html"); // Custom
		addCondRace(Race.ORC, "no-race.html"); // Custom
		registerQuestItems(LIST_OF_MATERIALS, BEARS_SHIN_BONE, SHARP_SPIDER_LEG);
		setQuestNameNpcStringId(NpcStringId.LV_15_20_RED_GEM_NECKLACE_1_3);
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
			case "30564-02.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "reward1":
			{
				if (qs.isCond(4))
				{
					takeItems(player, LIST_OF_MATERIALS, 1);
					takeItems(player, BEARS_SHIN_BONE, 20);
					takeItems(player, SHARP_SPIDER_LEG, 30);
					giveItems(player, SCROLL_OF_ESCAPE, 5);
					giveItems(player, HEALING_POTION, 40);
					giveItems(player, MP_RECOVERY_POTION, 40);
					giveItems(player, SOULSHOTS_NO_GRADE, 1000);
					addExpAndSp(player, 70000, 3600);
					qs.exitQuest(false, true);
					htmltext = "30560-03.html";
				}
				break;
			}
			case "reward2":
			{
				if (qs.isCond(4))
				{
					takeItems(player, LIST_OF_MATERIALS, 1);
					takeItems(player, BEARS_SHIN_BONE, 20);
					takeItems(player, SHARP_SPIDER_LEG, 30);
					giveItems(player, SCROLL_OF_ESCAPE, 5);
					giveItems(player, HEALING_POTION, 40);
					giveItems(player, MP_RECOVERY_POTION, 40);
					giveItems(player, SPIRITSHOT_NO_GRADE, 1000);
					addExpAndSp(player, 70000, 3600);
					qs.exitQuest(false, true);
					htmltext = "30560-04.html";
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
				if (npc.getId() == SUMARI)
				{
					htmltext = "30564-01.html";
				}
				break;
			}
			case State.STARTED:
			{
				if (npc.getId() == SUMARI)
				{
					if (qs.isCond(1))
					{
						htmltext = "30564-02a.html";
					}
					break;
				}
				else if (npc.getId() == USKA)
				{
					switch (qs.getCond())
					{
						case 1:
						{
							htmltext = "30560-01.htm";
							qs.setCond(2, true);
							showOnScreenMsg(talker, NpcStringId.GO_HUNTING_AND_KILL_KASHA_BEAR_2, ExShowScreenMessage.TOP_CENTER, 10000);
							giveItems(talker, LIST_OF_MATERIALS, 1);
							break;
						}
						case 2:
						{
							htmltext = "30560-01a.html";
							break;
						}
						case 4:
						{
							htmltext = "30560-02.html";
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
				case KASHA_BEAR:
				{
					if (qs.isCond(2) && (getQuestItemsCount(killer, BEARS_SHIN_BONE) < 20) && (getRandom(100) < 92))
					{
						giveItems(killer, BEARS_SHIN_BONE, 1);
						if (getQuestItemsCount(killer, BEARS_SHIN_BONE) >= 20)
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							showOnScreenMsg(killer, NpcStringId.YOU_HAVE_KILLED_ENOUGH_KASHA_BEARS_N_GO_HUNTING_AND_KILL_KASHA_SPIDERS, ExShowScreenMessage.TOP_CENTER, 10000);
							qs.setCond(3);
						}
						else
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				}
				case KASHA_SPIDER:
				case KASHA_FANG_SPIDER:
				case KASHA_BLADE_SPIDER:
				{
					if (qs.isCond(3) && (getQuestItemsCount(killer, SHARP_SPIDER_LEG) < 30) && (getRandom(100) < 89))
					{
						giveItems(killer, SHARP_SPIDER_LEG, 1);
						if (getQuestItemsCount(killer, SHARP_SPIDER_LEG) >= 30)
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							showOnScreenMsg(killer, NpcStringId.YOU_HAVE_KILLED_ENOUGH_KASHA_SPIDERS_NRETURN_TO_ACCESSORY_MERCHANT_USKA, ExShowScreenMessage.TOP_CENTER, 10000);
							qs.setCond(4);
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
