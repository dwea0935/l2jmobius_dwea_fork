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
package quests.Q10978_MissingPets;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.network.NpcStringId;

/**
 * @author QuangNguyen
 */
public class Q10978_MissingPets extends Quest
{
	// NPCs
	private static final int LEMPER = 30869;
	private static final int COOPER = 30829;
	// Items
	private static final ItemHolder SOULSHOT_TICKET = new ItemHolder(90907, 100);
	private static final ItemHolder PET_GUIDE = new ItemHolder(94118, 1);
	// Misc
	private static final int MIN_LEVEL = 76;
	
	public Q10978_MissingPets()
	{
		super(10978);
		addStartNpc(LEMPER);
		addTalkId(LEMPER, COOPER);
		addCondMinLevel(MIN_LEVEL, "no_lvl.html");
		setQuestNameNpcStringId(NpcStringId.LV_76_MISSING_PETS);
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
			case "30869.htm":
			case "30869-01.htm":
			case "30869-02.htm":
			case "30829.html":
			case "30829-01.html":
			{
				htmltext = event;
				break;
			}
			case "30869-03.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "30829-02.html":
			{
				htmltext = event;
				break;
			}
			case "reward":
			{
				if (qs.isCond(1))
				{
					giveItems(player, SOULSHOT_TICKET);
					giveItems(player, PET_GUIDE);
					htmltext = "30829-03.htm";
					qs.exitQuest(false, true);
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
			htmltext = "30869.htm";
		}
		else if (qs.isStarted())
		{
			switch (npc.getId())
			{
				case LEMPER:
				{
					if (qs.isCond(1))
					{
						htmltext = "30869-01.htm";
					}
					break;
				}
				case COOPER:
				{
					if (qs.isCond(1))
					{
						htmltext = "30829.html";
					}
					break;
				}
			}
		}
		else if (qs.isCompleted())
		{
			if (npc.getId() == LEMPER)
			{
				htmltext = getAlreadyCompletedMsg(player);
			}
		}
		return htmltext;
	}
}
