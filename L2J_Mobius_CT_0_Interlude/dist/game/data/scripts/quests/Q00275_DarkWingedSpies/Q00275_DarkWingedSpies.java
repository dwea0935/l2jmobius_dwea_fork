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
package quests.Q00275_DarkWingedSpies;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

public class Q00275_DarkWingedSpies extends Quest
{
	// Monsters
	private static final int DARKWING_BAT = 20316;
	private static final int VARANGKA_TRACKER = 27043;
	// Items
	private static final int DARKWING_BAT_FANG = 1478;
	private static final int VARANGKA_PARASITE = 1479;
	
	public Q00275_DarkWingedSpies()
	{
		super(275, "Dark Winged Spies");
		registerQuestItems(DARKWING_BAT_FANG, VARANGKA_PARASITE);
		addStartNpc(30567); // Tantus
		addTalkId(30567);
		addKillId(DARKWING_BAT, VARANGKA_TRACKER);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = getQuestState(player, false);
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equals("30567-03.htm"))
		{
			st.startQuest();
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = getQuestState(player, true);
		
		switch (st.getState())
		{
			case State.CREATED:
			{
				if (player.getRace() != Race.ORC)
				{
					htmltext = "30567-00.htm";
				}
				else if (player.getLevel() < 11)
				{
					htmltext = "30567-01.htm";
				}
				else
				{
					htmltext = "30567-02.htm";
				}
				break;
			}
			case State.STARTED:
			{
				if (st.isCond(1))
				{
					htmltext = "30567-04.htm";
				}
				else
				{
					htmltext = "30567-05.htm";
					takeItems(player, DARKWING_BAT_FANG, -1);
					takeItems(player, VARANGKA_PARASITE, -1);
					giveAdena(player, 4200, true);
					st.exitQuest(true, true);
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		final QuestState st = getQuestState(player, false);
		if ((st == null) || !st.isCond(1))
		{
			return;
		}
		
		switch (npc.getId())
		{
			case DARKWING_BAT:
			{
				giveItems(player, DARKWING_BAT_FANG, 1);
				if (getQuestItemsCount(player, DARKWING_BAT_FANG) < 70)
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				else
				{
					st.setCond(2, true);
				}
				
				if ((getRandom(100) < 10) && (getQuestItemsCount(player, DARKWING_BAT_FANG) > 10) && (getQuestItemsCount(player, DARKWING_BAT_FANG) < 66))
				{
					// Spawn of Varangka Tracker on the npc position.
					addSpawn(VARANGKA_TRACKER, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 0);
					giveItems(player, VARANGKA_PARASITE, 1);
				}
				break;
			}
			case VARANGKA_TRACKER:
			{
				if (hasQuestItems(player, VARANGKA_PARASITE))
				{
					takeItems(player, VARANGKA_PARASITE, -1);
					if (getQuestItemsCount(player, DARKWING_BAT_FANG) < 70)
					{
						giveItems(player, DARKWING_BAT_FANG, 5);
						if (getQuestItemsCount(player, DARKWING_BAT_FANG) < 70)
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
						else
						{
							st.setCond(2, true);
						}
					}
				}
				break;
			}
		}
	}
}
