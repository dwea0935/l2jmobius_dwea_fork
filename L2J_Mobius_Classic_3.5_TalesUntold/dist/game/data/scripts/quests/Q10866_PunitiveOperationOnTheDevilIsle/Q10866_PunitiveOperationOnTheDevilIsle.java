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
package quests.Q10866_PunitiveOperationOnTheDevilIsle;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.network.NpcStringId;

/**
 * Punitive Operation on the Devil Isle (10866)
 * @author Stayway
 */
public class Q10866_PunitiveOperationOnTheDevilIsle extends Quest
{
	// NPCs
	private static final int RODEMAI = 30756;
	private static final int EIN = 34017;
	private static final int FETHIN = 34019;
	private static final int NIKIA = 34020;
	// Misc
	private static final int MIN_LEVEL = 70;
	
	public Q10866_PunitiveOperationOnTheDevilIsle()
	{
		super(10866);
		addStartNpc(RODEMAI);
		addTalkId(RODEMAI, EIN, FETHIN, NIKIA);
		setQuestNameNpcStringId(NpcStringId.LV_70_PUNITIVE_OPERATION_ON_THE_DEVIL_S_ISLE);
	}
	
	@Override
	public boolean checkPartyMember(Player member, Npc npc)
	{
		final QuestState qs = getQuestState(member, false);
		return ((qs != null) && qs.isStarted());
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
			case "30756-02.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "34017-02.html":
			{
				qs.setCond(2, true);
				htmltext = event;
				break;
			}
			case "34019-02.html":
			{
				qs.setCond(3, true);
				htmltext = event;
				break;
			}
			case "34020-02.html":
			{
				if (qs.isStarted())
				{
					addExpAndSp(player, 150000, 4500);
					giveAdena(player, 13136, true);
					qs.exitQuest(false, true);
					htmltext = event;
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
			htmltext = (player.getLevel() >= MIN_LEVEL) ? "30756-01.htm" : "no_lvl.html";
		}
		else if (qs.isStarted())
		{
			switch (npc.getId())
			{
				case RODEMAI:
				{
					if (qs.isCond(1))
					{
						htmltext = "30756-02.html";
					}
					break;
				}
				case EIN:
				{
					if (qs.isCond(1))
					{
						htmltext = "34017-01.html";
					}
					else if (qs.isCond(2))
					{
						htmltext = "34017-02.html";
					}
					break;
				}
				case FETHIN:
				{
					if (qs.isCond(2))
					{
						htmltext = "34019-01.html";
					}
					else if (qs.isCond(3))
					{
						htmltext = "34019-02.html";
					}
					break;
				}
				case NIKIA:
				{
					if (qs.isCond(3))
					{
						htmltext = "34020-01.html";
					}
					break;
				}
			}
		}
		else if (qs.isCompleted())
		{
			if (npc.getId() == RODEMAI)
			{
				htmltext = getAlreadyCompletedMsg(player);
			}
		}
		return htmltext;
	}
}