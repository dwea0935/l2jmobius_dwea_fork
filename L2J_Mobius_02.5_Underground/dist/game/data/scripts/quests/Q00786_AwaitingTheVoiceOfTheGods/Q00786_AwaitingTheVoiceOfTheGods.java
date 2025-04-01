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
package quests.Q00786_AwaitingTheVoiceOfTheGods;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.QuestType;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * Awaiting the Voice of the Gods (786)
 * @URL https://l2wiki.com/Awaiting_the_Voice_of_the_Gods
 * @author Gigi
 */
public class Q00786_AwaitingTheVoiceOfTheGods extends Quest
{
	// NPC
	private static final int HERMIT = 31616;
	// Monsters
	private static final int[] MONSTERS =
	{
		21294, // Canyon Antelope
		21295, // Canyon Antelope Slave
		21296, // Canyon Bandersnatch
		21297, // Canyon Bandersnatch Slave
		21299, // Valley Buffalo Slave
		21304, // Valley Grendel Slave
		21313, // Sly Hound Dog
		21312, // Valley Grendel
		21311, // Valley Buffalo
		21301, // Gaze of Nightmare
		21302, // Eye of Watchman
		21305, // Eye of Pilgrim
		21298, // Eye of Restrainer
		21303, // Homunculus
		21307 // Elder Homunculus
	};
	// Items
	private static final int EYE_OF_DARKNESS = 39734; // min 50
	private static final int DARK_MALICE = 39735; // max 900
	private static final int EMISSARY_REWARD_BOX = 39727; // Emissary's Reward Box (Mid-grade)
	// Misc
	private static final int MIN_LEVEL = 70;
	private static final int MAX_LEVEL = 75;
	
	public Q00786_AwaitingTheVoiceOfTheGods()
	{
		super(786);
		addStartNpc(HERMIT);
		addTalkId(HERMIT);
		addKillId(MONSTERS);
		registerQuestItems(EYE_OF_DARKNESS, DARK_MALICE);
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "no_level.html");
		addCondRace(Race.ERTHEIA, "noErtheya.html");
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return getNoQuestMsg(player);
		}
		switch (event)
		{
			case "31616-02.htm":
			case "31616-03.htm":
			case "31616-07.html":
			case "31616-08.html":
			{
				htmltext = event;
				break;
			}
			case "31616-04.htm":
			{
				qs.startQuest();
				break;
			}
			case "31616-09.html":
			{
				if ((getQuestItemsCount(player, EYE_OF_DARKNESS) >= 50) && (getQuestItemsCount(player, DARK_MALICE) < 100))
				{
					takeItems(player, EYE_OF_DARKNESS, -1);
					takeItems(player, DARK_MALICE, -1);
					addExpAndSp(player, 14140350, 3393);
					giveItems(player, EMISSARY_REWARD_BOX, 1);
					qs.exitQuest(QuestType.DAILY, true);
					break;
				}
				else if ((getQuestItemsCount(player, EYE_OF_DARKNESS) >= 50) && ((getQuestItemsCount(player, DARK_MALICE) >= 100) && (getQuestItemsCount(player, DARK_MALICE) <= 199)))
				{
					takeItems(player, EYE_OF_DARKNESS, -1);
					takeItems(player, DARK_MALICE, -1);
					addExpAndSp(player, 28280700, 6786);
					giveItems(player, EMISSARY_REWARD_BOX, 2);
					qs.exitQuest(QuestType.DAILY, true);
					break;
				}
				else if ((getQuestItemsCount(player, EYE_OF_DARKNESS) >= 50) && ((getQuestItemsCount(player, DARK_MALICE) >= 200) && (getQuestItemsCount(player, DARK_MALICE) <= 299)))
				{
					takeItems(player, EYE_OF_DARKNESS, -1);
					takeItems(player, DARK_MALICE, -1);
					addExpAndSp(player, 42421050, 10179);
					giveItems(player, EMISSARY_REWARD_BOX, 3);
					qs.exitQuest(QuestType.DAILY, true);
					break;
				}
				else if ((getQuestItemsCount(player, EYE_OF_DARKNESS) >= 50) && ((getQuestItemsCount(player, DARK_MALICE) >= 300) && (getQuestItemsCount(player, DARK_MALICE) <= 399)))
				{
					takeItems(player, EYE_OF_DARKNESS, -1);
					takeItems(player, DARK_MALICE, -1);
					addExpAndSp(player, 56561400, 13572);
					giveItems(player, EMISSARY_REWARD_BOX, 4);
					qs.exitQuest(QuestType.DAILY, true);
					break;
				}
				else if ((getQuestItemsCount(player, EYE_OF_DARKNESS) >= 50) && ((getQuestItemsCount(player, DARK_MALICE) >= 400) && (getQuestItemsCount(player, DARK_MALICE) <= 499)))
				{
					takeItems(player, EYE_OF_DARKNESS, -1);
					takeItems(player, DARK_MALICE, -1);
					addExpAndSp(player, 70701750, 16965);
					giveItems(player, EMISSARY_REWARD_BOX, 5);
					qs.exitQuest(QuestType.DAILY, true);
					break;
				}
				else if ((getQuestItemsCount(player, EYE_OF_DARKNESS) >= 50) && ((getQuestItemsCount(player, DARK_MALICE) >= 500) && (getQuestItemsCount(player, DARK_MALICE) <= 599)))
				{
					takeItems(player, EYE_OF_DARKNESS, -1);
					takeItems(player, DARK_MALICE, -1);
					addExpAndSp(player, 84842100, 20358);
					giveItems(player, EMISSARY_REWARD_BOX, 6);
					qs.exitQuest(QuestType.DAILY, true);
					break;
				}
				else if ((getQuestItemsCount(player, EYE_OF_DARKNESS) >= 50) && ((getQuestItemsCount(player, DARK_MALICE) >= 600) && (getQuestItemsCount(player, DARK_MALICE) <= 699)))
				{
					takeItems(player, EYE_OF_DARKNESS, -1);
					takeItems(player, DARK_MALICE, -1);
					addExpAndSp(player, 98982450, 23751);
					giveItems(player, EMISSARY_REWARD_BOX, 7);
					qs.exitQuest(QuestType.DAILY, true);
					break;
				}
				else if ((getQuestItemsCount(player, EYE_OF_DARKNESS) >= 50) && ((getQuestItemsCount(player, DARK_MALICE) >= 700) && (getQuestItemsCount(player, DARK_MALICE) <= 799)))
				{
					takeItems(player, EYE_OF_DARKNESS, -1);
					takeItems(player, DARK_MALICE, -1);
					addExpAndSp(player, 113122800, 27144);
					giveItems(player, EMISSARY_REWARD_BOX, 8);
					qs.exitQuest(QuestType.DAILY, true);
					break;
				}
				else if ((getQuestItemsCount(player, EYE_OF_DARKNESS) >= 50) && ((getQuestItemsCount(player, DARK_MALICE) >= 800) && (getQuestItemsCount(player, DARK_MALICE) <= 899)))
				{
					takeItems(player, EYE_OF_DARKNESS, -1);
					takeItems(player, DARK_MALICE, -1);
					addExpAndSp(player, 127263150, 30537);
					giveItems(player, EMISSARY_REWARD_BOX, 9);
					qs.exitQuest(QuestType.DAILY, true);
					break;
				}
				if ((getQuestItemsCount(player, EYE_OF_DARKNESS) >= 50) && (getQuestItemsCount(player, DARK_MALICE) >= 900))
				{
					takeItems(player, EYE_OF_DARKNESS, -1);
					takeItems(player, DARK_MALICE, -1);
					addExpAndSp(player, 141403500, 33930);
					giveItems(player, EMISSARY_REWARD_BOX, 10);
					qs.exitQuest(QuestType.DAILY, true);
					break;
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		if (npc.getId() == HERMIT)
		{
			switch (qs.getState())
			{
				case State.COMPLETED:
				{
					if (!qs.isNowAvailable())
					{
						htmltext = "31616-10.html";
						break;
					}
					qs.setState(State.CREATED);
					break;
				}
				case State.CREATED:
				{
					htmltext = "31616-01.htm";
					break;
				}
				case State.STARTED:
				{
					if (qs.isCond(1))
					{
						htmltext = "31616-05.html";
					}
					else if (qs.isStarted() && qs.isCond(2))
					{
						htmltext = "31616-06.html";
					}
					break;
				}
			}
		}
		else if (qs.isCompleted() && !qs.isNowAvailable())
		{
			htmltext = "31616-10.html";
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && (qs.isCond(1)))
		{
			if (giveItemRandomly(killer, npc, EYE_OF_DARKNESS, 1, 50, 0.15, true))
			{
				showOnScreenMsg(killer, NpcStringId.YOU_CAN_GATHER_MORE_POWERFUL_DARK_MALICE, ExShowScreenMessage.TOP_CENTER, 5000);
				qs.setCond(2, true);
			}
		}
		else if ((qs != null) && (qs.isCond(2)))
		{
			if (giveItemRandomly(killer, npc, DARK_MALICE, 1, 900, 0.3, true))
			{
				qs.setCond(2, true);
			}
		}
	}
}