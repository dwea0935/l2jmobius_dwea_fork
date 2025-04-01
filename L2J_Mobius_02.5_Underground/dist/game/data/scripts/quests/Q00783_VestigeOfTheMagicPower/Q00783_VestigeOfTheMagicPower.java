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
package quests.Q00783_VestigeOfTheMagicPower;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.QuestType;
import org.l2jmobius.gameserver.model.quest.State;

import quests.Q10455_ElikiasLetter.Q10455_ElikiasLetter;

/**
 * Vestige of the Magic Power (783)
 * @URL https://l2wiki.com/Vestige_of_the_Magic_Power
 * @author Gigi
 */
public class Q00783_VestigeOfTheMagicPower extends Quest
{
	// NPC's
	private static final int LEONA_BLACKBIRD = 31595;
	// Monster's
	private static final int[] MONSTERS =
	{
		23384, // Smaug
		23385, // Lunatikan
		23386, // Jabberwok
		23387, // Kanzaroth
		23388, // Kandiloth
		23395, // Garion
		23396, // Garion Neti
		23397, // Desert Wendigo
		23398, // Koraza
		23399 // Bend Beetle
	};
	// Misc
	private static final int MIN_LEVEL = 99;
	private static final int HIGH_GRADE_FRAGMENT_OF_CHAOS = 46557;
	private static final int LEONAS_REWARD_BOX = 46558;
	private static final int BLOODIED_DEMONIC_TOME = 37893;
	
	public Q00783_VestigeOfTheMagicPower()
	{
		super(783);
		addStartNpc(LEONA_BLACKBIRD);
		addTalkId(LEONA_BLACKBIRD);
		addKillId(MONSTERS);
		registerQuestItems(HIGH_GRADE_FRAGMENT_OF_CHAOS);
		addCondMinLevel(MIN_LEVEL, "31595-00.htm");
		addCondCompletedQuest(Q10455_ElikiasLetter.class.getSimpleName(), "31595-00.htm");
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
			case "31595-02.htm":
			case "31595-03.htm":
			{
				htmltext = event;
				break;
			}
			case "31595-04.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "31595-07.html":
			{
				if ((getQuestItemsCount(player, HIGH_GRADE_FRAGMENT_OF_CHAOS) >= 250) && (getQuestItemsCount(player, HIGH_GRADE_FRAGMENT_OF_CHAOS) < 500))
				{
					addExpAndSp(player, 3876316782L, 9303137);
					giveItems(player, LEONAS_REWARD_BOX, 1);
					takeItems(player, HIGH_GRADE_FRAGMENT_OF_CHAOS, -1);
					giveItems(player, BLOODIED_DEMONIC_TOME, 1);
					qs.exitQuest(QuestType.REPEATABLE, true);
					htmltext = event;
					break;
				}
				else if ((getQuestItemsCount(player, HIGH_GRADE_FRAGMENT_OF_CHAOS) >= 500) && (getQuestItemsCount(player, HIGH_GRADE_FRAGMENT_OF_CHAOS) < 750))
				{
					addExpAndSp(player, 7752633564L, 18606274);
					giveItems(player, LEONAS_REWARD_BOX, 2);
					takeItems(player, HIGH_GRADE_FRAGMENT_OF_CHAOS, -1);
					giveItems(player, BLOODIED_DEMONIC_TOME, 1);
					qs.exitQuest(QuestType.REPEATABLE, true);
					htmltext = event;
					break;
				}
				else if ((getQuestItemsCount(player, HIGH_GRADE_FRAGMENT_OF_CHAOS) >= 750) && (getQuestItemsCount(player, HIGH_GRADE_FRAGMENT_OF_CHAOS) < 1000))
				{
					addExpAndSp(player, 11628950346L, 27909411);
					giveItems(player, LEONAS_REWARD_BOX, 3);
					takeItems(player, HIGH_GRADE_FRAGMENT_OF_CHAOS, -1);
					giveItems(player, BLOODIED_DEMONIC_TOME, 1);
					qs.exitQuest(QuestType.REPEATABLE, true);
					htmltext = event;
					break;
				}
				else if (getQuestItemsCount(player, HIGH_GRADE_FRAGMENT_OF_CHAOS) >= 1000)
				{
					addExpAndSp(player, 15505267128L, 37212548);
					giveItems(player, LEONAS_REWARD_BOX, 4);
					takeItems(player, HIGH_GRADE_FRAGMENT_OF_CHAOS, -1);
					giveItems(player, BLOODIED_DEMONIC_TOME, 1);
					qs.exitQuest(QuestType.REPEATABLE, true);
					htmltext = event;
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
		if (npc.getId() == LEONA_BLACKBIRD)
		{
			switch (qs.getState())
			{
				case State.CREATED:
				{
					htmltext = "31595-01.htm";
					break;
				}
				case State.STARTED:
				{
					if (qs.isCond(1))
					{
						htmltext = "31595-05.html";
					}
					else if (qs.isCond(2))
					{
						htmltext = "31595-06.html";
					}
					break;
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isStarted() && (getQuestItemsCount(player, HIGH_GRADE_FRAGMENT_OF_CHAOS) < 1000))
		{
			giveItems(player, HIGH_GRADE_FRAGMENT_OF_CHAOS, 1);
			if (getQuestItemsCount(player, HIGH_GRADE_FRAGMENT_OF_CHAOS) >= 250)
			{
				qs.setCond(2, true);
			}
			else
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
		}
	}
}