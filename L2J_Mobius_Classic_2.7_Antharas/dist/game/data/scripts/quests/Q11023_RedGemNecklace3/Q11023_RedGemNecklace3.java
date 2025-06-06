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
package quests.Q11023_RedGemNecklace3;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

import quests.Q11022_RedGemNecklace2.Q11022_RedGemNecklace2;

/**
 * Red Gem Necklace (3/3) (11023)
 * @author Stayway
 */
public class Q11023_RedGemNecklace3 extends Quest
{
	// NPCs
	private static final int USKA = 30560;
	// Items
	private static final int HARD_LENS = 90282;
	private static final int RED_STONE = 90281;
	private static final int NECKLACE_MATERIALS_TICKET = 90280;
	// Rewards
	private static final int SCROLL_OF_ESCAPE = 10650;
	private static final int HEALING_POTION = 1073;
	private static final int MP_RECOVERY_POTION = 90310;
	private static final int SOULSHOTS_NO_GRADE = 5789;
	private static final int SPIRITSHOT_NO_GRADE = 5790;
	// Monsters
	private static final int EVIL_EYE_SEER = 21257;
	private static final int KASHA_IMP = 21117;
	// Misc
	private static final int MIN_LEVEL = 15;
	private static final int MAX_LEVEL = 20;
	
	public Q11023_RedGemNecklace3()
	{
		super(11023);
		addStartNpc(USKA);
		addTalkId(USKA);
		addKillId(EVIL_EYE_SEER, KASHA_IMP);
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "no-level.html"); // Custom
		addCondRace(Race.ORC, "no-race.html"); // Custom
		addCondCompletedQuest(Q11022_RedGemNecklace2.class.getSimpleName(), "30560-06.html");
		registerQuestItems(NECKLACE_MATERIALS_TICKET, HARD_LENS, RED_STONE);
		setQuestNameNpcStringId(NpcStringId.LV_15_RED_GEM_NECKLACE_3_3);
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
			case "30560-02.htm":
			{
				qs.startQuest();
				qs.setCond(2);
				showOnScreenMsg(player, NpcStringId.GO_HUNTING_AND_KILL_EVIL_EYE_SEERS, ExShowScreenMessage.TOP_CENTER, 10000);
				giveItems(player, NECKLACE_MATERIALS_TICKET, 1);
				htmltext = event;
				break;
			}
			case "reward1":
			{
				if (qs.isCond(4))
				{
					takeItems(player, NECKLACE_MATERIALS_TICKET, 1);
					takeItems(player, HARD_LENS, 20);
					takeItems(player, RED_STONE, 20);
					giveItems(player, SCROLL_OF_ESCAPE, 5);
					giveItems(player, HEALING_POTION, 40);
					giveItems(player, MP_RECOVERY_POTION, 40);
					giveItems(player, SOULSHOTS_NO_GRADE, 1000);
					addExpAndSp(player, 70000, 3600);
					qs.exitQuest(false, true);
					htmltext = "30560-04.html";
				}
				break;
			}
			case "reward2":
			{
				if (qs.isCond(4))
				{
					takeItems(player, NECKLACE_MATERIALS_TICKET, 1);
					takeItems(player, HARD_LENS, 20);
					takeItems(player, RED_STONE, 20);
					giveItems(player, SCROLL_OF_ESCAPE, 5);
					giveItems(player, HEALING_POTION, 40);
					giveItems(player, MP_RECOVERY_POTION, 40);
					giveItems(player, SPIRITSHOT_NO_GRADE, 1000);
					addExpAndSp(player, 70000, 3600);
					qs.exitQuest(false, true);
					htmltext = "30560-05.html";
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
				htmltext = "30560-01.html";
				break;
			}
			case State.STARTED:
			{
				if (qs.isCond(2))
				{
					htmltext = "30560-02a.html";
				}
				else if (qs.isCond(4))
				{
					htmltext = "30560-03.html";
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
				case EVIL_EYE_SEER:
				{
					if (qs.isCond(2) && (getQuestItemsCount(killer, HARD_LENS) < 20) && (getRandom(100) < 92))
					{
						giveItems(killer, HARD_LENS, 1);
						if (getQuestItemsCount(killer, HARD_LENS) >= 20)
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							showOnScreenMsg(killer, NpcStringId.YOU_HAVE_KILLED_ENOUGH_EVIL_EYE_SEERS_N_GO_HUNTING_AND_KILL_KASHA_IMPS, ExShowScreenMessage.TOP_CENTER, 10000);
							qs.setCond(3);
						}
						else
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				}
				case KASHA_IMP:
				{
					if (qs.isCond(3) && (getQuestItemsCount(killer, RED_STONE) < 20) && (getRandom(100) < 91))
					{
						giveItems(killer, RED_STONE, 1);
						if (getQuestItemsCount(killer, RED_STONE) >= 20)
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							showOnScreenMsg(killer, NpcStringId.YOU_HAVE_KILLED_ENOUGH_KASHA_IMPS_NRETURN_TO_ACCESSORY_MERCHANT_USKA, ExShowScreenMessage.TOP_CENTER, 10000);
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
