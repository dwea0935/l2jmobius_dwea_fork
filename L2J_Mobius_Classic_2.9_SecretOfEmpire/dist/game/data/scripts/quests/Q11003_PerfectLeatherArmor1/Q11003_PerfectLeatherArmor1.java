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
package quests.Q11003_PerfectLeatherArmor1;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

import quests.Q11002_HelpWithTempleRestoration.Q11002_HelpWithTempleRestoration;

/**
 * Perfect Leather Armor (1/3) (11003)
 * @author Stayway
 */
public class Q11003_PerfectLeatherArmor1 extends Quest
{
	// NPCs
	private static final int HARRYS = 30035;
	private static final int LECTOR = 30001;
	// Items
	private static final int COBWEB = 90209;
	private static final int ESSENCE_OF_WATER = 90210;
	private static final int LECTORS_NOTES = 90208;
	// Rewards
	private static final int SCROLL_OF_ESCAPE = 10650;
	private static final int HEALING_POTION = 1073;
	private static final int MP_RECOVERY_POTION = 90310;
	private static final int SOULSHOTS_NO_GRADE = 5789;
	private static final int SPIRITSHOT_NO_GRADE = 5790;
	// Monsters
	private static final int GIANT_SPIDER = 20103;
	private static final int GIANT_FANG_SPIDER = 20106;
	private static final int GIANT_BLADE_SPIDER = 20108;
	private static final int UNDINE = 20110;
	private static final int UNDINE_ELDER = 20113;
	private static final int UNDINE_NOBLE = 20115;
	// Misc
	private static final int MIN_LEVEL = 15;
	private static final int MAX_LEVEL = 20;
	
	public Q11003_PerfectLeatherArmor1()
	{
		super(11003);
		addStartNpc(HARRYS);
		addTalkId(HARRYS, LECTOR);
		addKillId(GIANT_SPIDER, GIANT_FANG_SPIDER, GIANT_BLADE_SPIDER, UNDINE, UNDINE_ELDER, UNDINE_NOBLE);
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "no-level.html"); // Custom
		addCondRace(Race.HUMAN, "no-race.html"); // Custom
		addCondCompletedQuest(Q11002_HelpWithTempleRestoration.class.getSimpleName(), "no-quest.html");
		registerQuestItems(LECTORS_NOTES, COBWEB, ESSENCE_OF_WATER);
		setQuestNameNpcStringId(NpcStringId.LV_15_20_PERFECT_LEATHER_ARMOR_1_3);
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
			case "30035-02.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "reward1":
			{
				if (qs.isCond(4))
				{
					takeItems(player, LECTORS_NOTES, 1);
					takeItems(player, COBWEB, 25);
					takeItems(player, ESSENCE_OF_WATER, 20);
					giveItems(player, SCROLL_OF_ESCAPE, 5);
					giveItems(player, HEALING_POTION, 40);
					giveItems(player, MP_RECOVERY_POTION, 40);
					giveItems(player, SOULSHOTS_NO_GRADE, 1000);
					addExpAndSp(player, 70000, 3600);
					qs.exitQuest(false, true);
					htmltext = "30001-03.html";
				}
				break;
			}
			case "reward2":
			{
				if (qs.isCond(4))
				{
					takeItems(player, LECTORS_NOTES, 1);
					takeItems(player, COBWEB, 25);
					takeItems(player, ESSENCE_OF_WATER, 20);
					giveItems(player, SCROLL_OF_ESCAPE, 5);
					giveItems(player, HEALING_POTION, 40);
					giveItems(player, MP_RECOVERY_POTION, 40);
					giveItems(player, SPIRITSHOT_NO_GRADE, 1000);
					addExpAndSp(player, 70000, 3600);
					qs.exitQuest(false, true);
					htmltext = "30001-04.html";
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
				if (npc.getId() == HARRYS)
				{
					htmltext = "30035-01.html";
				}
				break;
			}
			case State.STARTED:
			{
				if (npc.getId() == HARRYS)
				{
					if (qs.isCond(1))
					{
						htmltext = "30035-02a.html";
					}
				}
				else if (npc.getId() == LECTOR)
				{
					switch (qs.getCond())
					{
						case 1:
						{
							htmltext = "30001-01.htm";
							qs.setCond(2, true);
							showOnScreenMsg(talker, NpcStringId.LECTOR_WANTS_YOU_TO_BRING_HIM_MATERIALS_FOR_NEW_ARMOR_N_GO_HUNTING_AND_KILL_GIANT_SPIDERS, ExShowScreenMessage.TOP_CENTER, 10000);
							giveItems(talker, LECTORS_NOTES, 1);
							break;
						}
						case 2:
						{
							htmltext = "30001-01a.html";
							break;
						}
						case 4:
						{
							htmltext = "30001-02.html";
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
				case GIANT_SPIDER:
				case GIANT_BLADE_SPIDER:
				case GIANT_FANG_SPIDER:
				{
					if (qs.isCond(2) && (getQuestItemsCount(killer, COBWEB) < 25) && (getRandom(100) < 87))
					{
						giveItems(killer, COBWEB, 1);
						if (getQuestItemsCount(killer, COBWEB) >= 25)
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							showOnScreenMsg(killer, NpcStringId.YOU_HAVE_KILLED_ENOUGH_GIANT_SPIDERS_N_GO_HUNTING_AND_KILL_UNDINES, ExShowScreenMessage.TOP_CENTER, 10000);
							qs.setCond(3);
						}
						else
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				}
				case UNDINE:
				case UNDINE_ELDER:
				case UNDINE_NOBLE:
				{
					if (qs.isCond(3) && (getQuestItemsCount(killer, ESSENCE_OF_WATER) < 20) && (getRandom(100) < 100))
					{
						giveItems(killer, ESSENCE_OF_WATER, 1);
						if (getQuestItemsCount(killer, ESSENCE_OF_WATER) >= 20)
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							showOnScreenMsg(killer, NpcStringId.YOU_HAVE_ALL_OF_THE_ITEMS_LECTOR_REQUESTED_RETURN_TO_HIM, ExShowScreenMessage.TOP_CENTER, 10000);
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
