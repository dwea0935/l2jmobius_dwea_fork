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
package quests.Q11011_NewPotionDevelopment3;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

import quests.Q11010_NewPotionDevelopment2.Q11010_NewPotionDevelopment2;

/**
 * New Potion Development (3/3) (11011)
 * @author Stayway
 */
public class Q11011_NewPotionDevelopment3 extends Quest
{
	// NPCs
	private static final int HERBIEL = 30150;
	// Items
	private static final int ANTIDOTE = 90235;
	private static final int ARACHNID_TRACKER_THORN = 90236;
	private static final int MEDICATIONS_RESEARCH = 90234;
	// Rewards
	private static final int SCROLL_OF_ESCAPE = 10650;
	private static final int HEALING_POTION = 1073;
	private static final int MP_RECOVERY_POTION = 90310;
	private static final int SOULSHOTS_NO_GRADE = 5789;
	private static final int SPIRITSHOT_NO_GRADE = 5790;
	// Monsters
	private static final int RATMAN_SCAVENGER = 20039;
	private static final int ARACHNID_TRACKER = 20043;
	// Misc
	private static final int MIN_LEVEL = 15;
	private static final int MAX_LEVEL = 20;
	
	public Q11011_NewPotionDevelopment3()
	{
		super(11011);
		addStartNpc(HERBIEL);
		addTalkId(HERBIEL);
		addKillId(RATMAN_SCAVENGER, ARACHNID_TRACKER);
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "no-level.html"); // Custom
		addCondRace(Race.ELF, "no-race.html"); // Custom
		addCondCompletedQuest(Q11010_NewPotionDevelopment2.class.getSimpleName(), "30150-05.html");
		registerQuestItems(MEDICATIONS_RESEARCH, ANTIDOTE, ARACHNID_TRACKER_THORN);
		setQuestNameNpcStringId(NpcStringId.LV_15_NEW_POTION_DEVELOPMENT_3_3);
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
			case "30150-02.htm":
			{
				qs.startQuest();
				qs.setCond(2, true);
				showOnScreenMsg(player, NpcStringId.GO_HUNTING_AND_KILL_RATMAN_SCAVENGERS, ExShowScreenMessage.TOP_CENTER, 10000);
				giveItems(player, MEDICATIONS_RESEARCH, 1);
				htmltext = event;
				break;
			}
			case "reward1":
			{
				if (qs.isCond(4))
				{
					takeItems(player, MEDICATIONS_RESEARCH, 1);
					takeItems(player, ANTIDOTE, 20);
					takeItems(player, ARACHNID_TRACKER_THORN, 20);
					giveItems(player, SCROLL_OF_ESCAPE, 5);
					giveItems(player, HEALING_POTION, 40);
					giveItems(player, MP_RECOVERY_POTION, 40);
					giveItems(player, SOULSHOTS_NO_GRADE, 1000);
					addExpAndSp(player, 70000, 3600);
					qs.exitQuest(false, true);
					htmltext = "30150-04.html";
				}
				break;
			}
			case "reward2":
			{
				if (qs.isCond(4))
				{
					takeItems(player, MEDICATIONS_RESEARCH, 1);
					takeItems(player, ANTIDOTE, 20);
					takeItems(player, ARACHNID_TRACKER_THORN, 20);
					giveItems(player, SCROLL_OF_ESCAPE, 5);
					giveItems(player, HEALING_POTION, 40);
					giveItems(player, MP_RECOVERY_POTION, 40);
					giveItems(player, SPIRITSHOT_NO_GRADE, 1000);
					addExpAndSp(player, 70000, 3600);
					qs.exitQuest(false, true);
					htmltext = "30150-04.html";
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
				htmltext = "30150-01.html";
				break;
			}
			case State.STARTED:
			{
				if (qs.isCond(2))
				{
					htmltext = "30150-02a.html";
				}
				else if (qs.isCond(4))
				{
					htmltext = "30150-03.html";
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
				case RATMAN_SCAVENGER:
				{
					if (qs.isCond(2) && (getQuestItemsCount(killer, ANTIDOTE) < 20) && (getRandom(100) < 95))
					{
						giveItems(killer, ANTIDOTE, 1);
						if (getQuestItemsCount(killer, ANTIDOTE) >= 20)
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							showOnScreenMsg(killer, NpcStringId.YOU_HAVE_KILLED_ENOUGH_RATMAN_SCAVENGERS_N_GO_HUNTING_AND_KILL_ARACHNID_TRACKERS, ExShowScreenMessage.TOP_CENTER, 10000);
							qs.setCond(3);
						}
						else
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				}
				case ARACHNID_TRACKER:
				{
					if (qs.isCond(3) && (getQuestItemsCount(killer, ARACHNID_TRACKER_THORN) < 20) && (getRandom(100) < 90))
					{
						giveItems(killer, ARACHNID_TRACKER_THORN, 1);
						if (getQuestItemsCount(killer, ARACHNID_TRACKER_THORN) >= 20)
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							showOnScreenMsg(killer, NpcStringId.RETURN_TO_GROCER_HERBIEL_3, ExShowScreenMessage.TOP_CENTER, 10000);
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
