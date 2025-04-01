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
package quests.Q00647_InfluxOfMachines;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

public class Q00647_InfluxOfMachines extends Quest
{
	// Item
	private static final int DESTROYED_GOLEM_SHARD = 8100;
	
	// NPC
	private static final int GUTENHAGEN = 32069;
	
	public Q00647_InfluxOfMachines()
	{
		super(647, "Influx of Machines");
		
		registerQuestItems(DESTROYED_GOLEM_SHARD);
		
		addStartNpc(GUTENHAGEN);
		addTalkId(GUTENHAGEN);
		
		for (int i = 22052; i < 22079; i++)
		{
			addKillId(i);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equals("32069-02.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("32069-06.htm"))
		{
			takeItems(player, DESTROYED_GOLEM_SHARD, -1);
			giveItems(player, Rnd.get(4963, 4972), 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_FINISH);
			st.exitQuest(true);
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState st = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (st.getState())
		{
			case State.CREATED:
				htmltext = (player.getLevel() < 46) ? "32069-03.htm" : "32069-01.htm";
				break;
			
			case State.STARTED:
				final int cond = st.getCond();
				if (cond == 1)
				{
					htmltext = "32069-04.htm";
				}
				else if (cond == 2)
				{
					htmltext = "32069-05.htm";
				}
				break;
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		final Player partyMember = getRandomPartyMember(player, 1);
		if (partyMember == null)
		{
			return;
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st == null)
		{
			return;
		}
		
		if (giveItemRandomly(partyMember, npc, DESTROYED_GOLEM_SHARD, 1, 500, 0.3, true))
		{
			st.setCond(2);
		}
	}
}