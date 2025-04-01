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
package quests.Q10309_DreamlandsMysteries;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * @author Serenitty
 */
public class Q10309_DreamlandsMysteries extends Quest
{
	// NPC
	private static final int DREAM_PRIESTESS = 34304;
	// Monsters
	private static final int[] MONSTERS =
	{
		18678,
		18679,
		18682,
		18683,
		18683,
		18684,
		18685,
	};
	// Items
	private static final ItemHolder BOOST_ATK_SCROLL = new ItemHolder(94269, 10);
	private static final ItemHolder BOOST_DEF_SCROLL = new ItemHolder(94271, 10);
	private static final ItemHolder BERSERKER_SCROLL = new ItemHolder(94777, 10);
	// Misc
	private static final int MIN_LEVEL = 76;
	
	public Q10309_DreamlandsMysteries()
	{
		super(10309);
		addStartNpc(DREAM_PRIESTESS);
		addTalkId(DREAM_PRIESTESS);
		addKillId(MONSTERS);
		addCondMinLevel(MIN_LEVEL, "34304-06.htm");
		setQuestNameNpcStringId(NpcStringId.LV_76_DREAMLAND_S_MYSTERIES);
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
			case "34304-01.htm":
			case "34304-02.htm":
			case "34304-03.htm":
			case "34304-05.htm":
			{
				htmltext = event;
				break;
			}
			case "34304-04.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "34304-08.html":
			{
				if (qs.isStarted())
				{
					addExpAndSp(player, 100000000, 27000000);
					giveItems(player, BOOST_ATK_SCROLL);
					giveItems(player, BOOST_DEF_SCROLL);
					giveItems(player, BERSERKER_SCROLL);
					qs.exitQuest(false, true);
				}
				htmltext = event;
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
			htmltext = "34304-01.htm";
		}
		else if (qs.isStarted())
		{
			if (qs.isCond(1))
			{
				htmltext = "34304-07.htm";
			}
			else if (qs.isCond(2))
			{
				htmltext = "34304-05.htm";
			}
		}
		else if (qs.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && qs.isCond(1))
		{
			qs.setCond(2, true);
			showOnScreenMsg(killer, NpcStringId.LV_76_DREAMLAND_S_MYSTERIES_COMPLETED, ExShowScreenMessage.TOP_CENTER, 10000);
		}
	}
}
