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
package quests.Q00943_FillingTheEnergyOfDestruction;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.QuestType;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Filling the Energy of Destruction (943)
 * @author karma12
 */
public class Q00943_FillingTheEnergyOfDestruction extends Quest
{
	// NPC
	private static final int SEED_TALISMAN_MANAGER = 33715;
	// Raids
	private static final int ISTINA_EASY = 29195;
	private static final int ISTINA_HARD = 29196;
	private static final int OCTAVIS_EASY = 29194;
	private static final int OCTAVIS_HARD = 29212;
	private static final int SPEZION_EASY = 25867;
	private static final int SPEZION_HARD = 25868;
	private static final int BAYLOR = 29213;
	private static final int BALOK = 29218;
	private static final int RON = 25825;
	private static final int TAUTI_1 = 29236;
	private static final int TAUTI_2 = 29237;
	private static final int TAUTI_3 = 29238;
	// Item
	private static final int CORE_OF_TWISTED_MAGIC = 35668;
	// Rewards
	private static final int ENERGY_OF_DESTRUCTION = 35562;
	
	public Q00943_FillingTheEnergyOfDestruction()
	{
		super(943);
		addStartNpc(SEED_TALISMAN_MANAGER);
		addTalkId(SEED_TALISMAN_MANAGER);
		addKillId(ISTINA_EASY, ISTINA_HARD, OCTAVIS_EASY, OCTAVIS_HARD, SPEZION_EASY, SPEZION_HARD, BAYLOR, BALOK, RON, TAUTI_1, TAUTI_2, TAUTI_3);
		registerQuestItems(CORE_OF_TWISTED_MAGIC);
	}
	
	@Override
	public void actionForEachPlayer(Player player, Npc npc, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(1) && LocationUtil.checkIfInRange(1500, npc, player, false))
		{
			giveItems(player, CORE_OF_TWISTED_MAGIC, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			qs.setCond(2, true);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "33715-03.htm":
			{
				qs.startQuest();
				break;
			}
			case "33715-06.html":
			{
				takeItems(player, CORE_OF_TWISTED_MAGIC, 1);
				giveItems(player, ENERGY_OF_DESTRUCTION, 1);
				if (player.getLevel() >= 99)
				{
					addExpAndSp(player, 0, 5371901);
				}
				qs.exitQuest(QuestType.DAILY, true);
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		executeForEachPlayer(killer, npc, isSummon, true, true);
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (qs.getState())
		{
			case State.COMPLETED:
			{
				if (qs.isNowAvailable())
				{
					qs.setState(State.CREATED);
					htmltext = (player.getLevel() >= 90) ? "33715-01.htm" : "33715-00.htm";
				}
				else
				{
					htmltext = "33715-07.html";
				}
				break;
			}
			case State.CREATED:
			{
				htmltext = (player.getLevel() >= 90) ? "33715-01.htm" : "33715-00.htm";
				break;
			}
			case State.STARTED:
			{
				if (qs.isCond(1))
				{
					htmltext = "33715-04.html";
				}
				else if (qs.isCond(2))
				{
					if (player.getLevel() < 90)
					{
						htmltext = "33715-00a.html";
					}
					else
					{
						htmltext = "33715-05.html";
					}
				}
				break;
			}
		}
		return htmltext;
	}
}
