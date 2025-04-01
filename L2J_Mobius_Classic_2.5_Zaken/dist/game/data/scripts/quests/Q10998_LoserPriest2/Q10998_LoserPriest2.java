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
package quests.Q10998_LoserPriest2;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

import quests.Q10997_LoserPriest1.Q10997_LoserPriest1;

/**
 * Loser Priest (2/3) (10998)
 * @author Stayway
 */
public class Q10998_LoserPriest2 extends Quest
{
	// NPCs
	private static final int GERALD = 30650;
	// Items
	private static final int HUNTER_TARANTULA_VENOM = 90300;
	private static final int PLUNDER_TARANTULA_KIDNEY = 90301;
	private static final int MAINTENANCE_REQUEST = 90299;
	// Rewards
	private static final int SCROLL_OF_ESCAPE = 10650;
	private static final int HEALING_POTION = 1073;
	private static final int MP_RECOVERY_POTION = 90310;
	private static final int SOULSHOTS_NO_GRADE = 5789;
	private static final int SPIRITSHOT_NO_GRADE = 5790;
	// Monsters
	private static final int HUNTER_TARANTULA = 20403;
	private static final int PLUNDER_TARANTULA = 20508;
	// Misc
	private static final int MIN_LEVEL = 15;
	private static final int MAX_LEVEL = 20;
	
	public Q10998_LoserPriest2()
	{
		super(10998);
		addStartNpc(GERALD);
		addTalkId(GERALD);
		addKillId(PLUNDER_TARANTULA, HUNTER_TARANTULA);
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "no-level.html"); // Custom
		addCondRace(Race.DWARF, "no-race.html"); // Custom
		addCondCompletedQuest(Q10997_LoserPriest1.class.getSimpleName(), "30650-06.html");
		registerQuestItems(MAINTENANCE_REQUEST, HUNTER_TARANTULA_VENOM, PLUNDER_TARANTULA_KIDNEY);
		setQuestNameNpcStringId(NpcStringId.LV_15_LOSER_PRIEST_2_3);
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
			case "abort.html":
			{
				htmltext = event;
				break;
			}
			case "30650-02.htm":
			{
				qs.startQuest();
				qs.setCond(1);
				qs.setCond(2);
				showOnScreenMsg(player, NpcStringId.GO_HUNTING_AND_KILL_HUNTER_TARANTULAS, ExShowScreenMessage.TOP_CENTER, 10000);
				giveItems(player, MAINTENANCE_REQUEST, 1);
				htmltext = event;
				break;
			}
			case "reward1":
			{
				if (qs.isCond(4))
				{
					takeItems(player, MAINTENANCE_REQUEST, 1);
					takeItems(player, HUNTER_TARANTULA_VENOM, 20);
					takeItems(player, PLUNDER_TARANTULA_KIDNEY, 20);
					giveItems(player, SCROLL_OF_ESCAPE, 5);
					giveItems(player, HEALING_POTION, 40);
					giveItems(player, MP_RECOVERY_POTION, 40);
					giveItems(player, SOULSHOTS_NO_GRADE, 1000);
					addExpAndSp(player, 70000, 3600);
					qs.exitQuest(false, true);
					htmltext = "30650-04.html";
				}
				break;
			}
			case "reward2":
			{
				if (qs.isCond(4))
				{
					takeItems(player, MAINTENANCE_REQUEST, 1);
					takeItems(player, HUNTER_TARANTULA_VENOM, 20);
					takeItems(player, PLUNDER_TARANTULA_KIDNEY, 20);
					giveItems(player, SCROLL_OF_ESCAPE, 5);
					giveItems(player, HEALING_POTION, 40);
					giveItems(player, MP_RECOVERY_POTION, 40);
					giveItems(player, SPIRITSHOT_NO_GRADE, 1000);
					addExpAndSp(player, 70000, 3600);
					qs.exitQuest(false, true);
					htmltext = "30650-05.html";
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
				htmltext = "30650-01.html";
				break;
			}
			case State.STARTED:
			{
				if (qs.isCond(2))
				{
					htmltext = "30650-02a.html";
				}
				else if (qs.isCond(4))
				{
					htmltext = "30650-03.html";
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
				case HUNTER_TARANTULA:
				{
					if (qs.isCond(2) && (getQuestItemsCount(killer, HUNTER_TARANTULA_VENOM) < 20) && (getRandom(100) < 94))
					{
						giveItems(killer, HUNTER_TARANTULA_VENOM, 1);
						if (getQuestItemsCount(killer, HUNTER_TARANTULA_VENOM) >= 20)
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							showOnScreenMsg(killer, NpcStringId.YOU_HAVE_KILLED_ENOUGH_HUNTER_TARANTULAS_N_GO_HUNTING_AND_KILL_PLUNDER_TARANTULAS, ExShowScreenMessage.TOP_CENTER, 10000);
							qs.setCond(3);
						}
						else
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				}
				case PLUNDER_TARANTULA:
				{
					if (qs.isCond(3) && (getQuestItemsCount(killer, PLUNDER_TARANTULA_KIDNEY) < 20) && (getRandom(100) < 94))
					{
						giveItems(killer, PLUNDER_TARANTULA_KIDNEY, 1);
						if (getQuestItemsCount(killer, PLUNDER_TARANTULA_KIDNEY) >= 20)
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							showOnScreenMsg(killer, NpcStringId.YOU_HAVE_KILLED_ENOUGH_PLUNDER_TARANTULAS_NRETURN_TO_PRIEST_OF_THE_EARTH_GERALD, ExShowScreenMessage.TOP_CENTER, 10000);
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
