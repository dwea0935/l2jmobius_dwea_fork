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
package quests.Q00697_DefendTheHallOfErosion;

import org.l2jmobius.gameserver.managers.SoIManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

public class Q00697_DefendTheHallOfErosion extends Quest
{
	private static final int TEPIOS = 32603;
	private static final int VESPER_STONE = 14052;
	
	public Q00697_DefendTheHallOfErosion()
	{
		super(697, "Defend the Hall of Erosion");
		addStartNpc(TEPIOS);
		addTalkId(TEPIOS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState qs = player.getQuestState(getName());
		if (qs == null)
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("32603-03.html"))
		{
			qs.startQuest();
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (qs.getState())
		{
			case State.CREATED:
			{
				if (player.getLevel() < 75)
				{
					htmltext = "32603-00.html";
					qs.exitQuest(true);
					break;
				}
				if (SoIManager.getCurrentStage() != 4)
				{
					htmltext = "32603-00a.html";
					qs.exitQuest(true);
					break;
				}
				htmltext = "32603-01.htm";
				break;
			}
			case State.STARTED:
			{
				if (qs.isCond(1) && (qs.getInt("defenceDone") == 0))
				{
					htmltext = "32603-04.html";
				}
				else if (qs.isCond(1) && (qs.getInt("defenceDone") != 0))
				{
					giveItems(player, VESPER_STONE, getRandom(12, 20));
					qs.unset("defenceDone");
					qs.exitQuest(true);
					htmltext = "32603-05.html";
				}
				break;
			}
		}
		return htmltext;
	}
}