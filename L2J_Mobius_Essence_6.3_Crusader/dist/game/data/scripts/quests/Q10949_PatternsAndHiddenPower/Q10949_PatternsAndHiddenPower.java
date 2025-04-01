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
package quests.Q10949_PatternsAndHiddenPower;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.network.NpcStringId;

/**
 * @author Serenitty
 */
public class Q10949_PatternsAndHiddenPower extends Quest
{
	// NPC
	private static final int ORVEN = 30857;
	// Items
	private static final ItemHolder ADVENTURE_DYE = new ItemHolder(97878, 1);
	private static final ItemHolder ADVENTURE_DYE_POWDER = new ItemHolder(97982, 1);
	// Misc
	private static final int MIN_LEVEL = 40;
	
	public Q10949_PatternsAndHiddenPower()
	{
		super(10949);
		addStartNpc(ORVEN);
		addTalkId(ORVEN);
		addCondMinLevel(MIN_LEVEL, "no_lvl.html");
		setQuestNameNpcStringId(NpcStringId.LV_40_PATTERNS_AND_HIDDEN_POWER);
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
			case "30857.htm":
			case "30857-01.htm":
			case "30857-02.htm":
			case "30857-03.htm":
			case "30857-04.htm":
			case "30857-05.htm":
			{
				htmltext = event;
				break;
			}
			case "StartPatterns":
			{
				qs.startQuest();
				htmltext = "30857-03.htm";
				break;
			}
			case "takeReward":
			{
				if (qs.isStarted())
				{
					giveItems(player, ADVENTURE_DYE);
					giveItems(player, ADVENTURE_DYE_POWDER);
					qs.exitQuest(false, true);
					htmltext = "30857-05.htm";
					break;
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		if (qs.isCreated())
		{
			htmltext = "30857.htm";
		}
		else if (qs.isStarted())
		{
			htmltext = "30857-04.htm";
		}
		else if (qs.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
		}
		return htmltext;
	}
}
