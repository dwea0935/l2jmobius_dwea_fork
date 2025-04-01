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
package quests.Q00036_MakeASewingKit;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Make a Sewing Kit (36)
 * @author malyelfik, CostyKiller
 */
public class Q00036_MakeASewingKit extends Quest
{
	// NPC
	private static final int FERRIS = 30847;
	// Monster
	private static final int LOST_STEEL_GOLEM = 22091;
	private static final int FROST_STEEL_GOLEM = 22092;
	// Items
	private static final int IRON_ORE = 36521;
	private static final int COKES = 36561;
	private static final int SEWING_KIT = 7078;
	private static final int REINFORCED_IRON_PIECE = 7163;
	// Misc
	private static final int MIN_LEVEL = 85;
	private static final int REINFORCED_IRON_PIECE_COUNT = 5;
	private static final int IRON_ORE_COUNT = 180;
	private static final int COKES_COUNT = 360;
	
	public Q00036_MakeASewingKit()
	{
		super(36);
		addStartNpc(FERRIS);
		addTalkId(FERRIS);
		addKillId(LOST_STEEL_GOLEM, FROST_STEEL_GOLEM);
		registerQuestItems(REINFORCED_IRON_PIECE);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = event;
		switch (event)
		{
			case "30847-03.htm":
			{
				qs.startQuest();
				break;
			}
			case "30847-06.html":
			{
				if (getQuestItemsCount(player, REINFORCED_IRON_PIECE) < REINFORCED_IRON_PIECE_COUNT)
				{
					return getNoQuestMsg(player);
				}
				takeItems(player, REINFORCED_IRON_PIECE, -1);
				qs.setCond(3, true);
				break;
			}
			case "30847-09.html":
			{
				if ((getQuestItemsCount(player, IRON_ORE) >= IRON_ORE_COUNT) && (getQuestItemsCount(player, COKES) >= COKES_COUNT))
				{
					takeItems(player, IRON_ORE, 180);
					takeItems(player, COKES, 360);
					giveItems(player, SEWING_KIT, 1);
					qs.exitQuest(false, true);
				}
				else
				{
					htmltext = "30847-10.html";
				}
				break;
			}
			default:
			{
				htmltext = null;
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player member = getRandomPartyMember(player, 1);
		if ((member != null) && getRandomBoolean())
		{
			giveItems(player, REINFORCED_IRON_PIECE, 1);
			if (getQuestItemsCount(player, REINFORCED_IRON_PIECE) >= REINFORCED_IRON_PIECE_COUNT)
			{
				getQuestState(member, false).setCond(2, true);
			}
			else
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
		}
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState qs = getQuestState(player, true);
		
		switch (qs.getState())
		{
			case State.CREATED:
			{
				htmltext = (player.getLevel() >= MIN_LEVEL) ? "30847-01.htm" : "30847-02.html";
				break;
			}
			case State.STARTED:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						htmltext = "30847-04.html";
						break;
					}
					case 2:
					{
						htmltext = "30847-05.html";
						break;
					}
					case 3:
					{
						htmltext = ((getQuestItemsCount(player, IRON_ORE) >= IRON_ORE_COUNT) && (getQuestItemsCount(player, COKES) >= COKES_COUNT)) ? "30847-07.html" : "30847-08.html";
						break;
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				htmltext = getAlreadyCompletedMsg(player);
				break;
			}
		}
		return htmltext;
	}
}