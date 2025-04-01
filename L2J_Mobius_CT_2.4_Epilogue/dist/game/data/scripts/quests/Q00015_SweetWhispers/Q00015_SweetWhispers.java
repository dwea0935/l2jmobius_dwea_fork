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
package quests.Q00015_SweetWhispers;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Sweet Whisper (15)<br>
 * Original jython script by disKret.
 * @author nonom
 */
public class Q00015_SweetWhispers extends Quest
{
	// NPCs
	private static final int VLADIMIR = 31302;
	private static final int HIERARCH = 31517;
	private static final int M_NECROMANCER = 31518;
	
	public Q00015_SweetWhispers()
	{
		super(15, "Sweet Whispers");
		addStartNpc(VLADIMIR);
		addTalkId(VLADIMIR, HIERARCH, M_NECROMANCER);
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
			case "31302-01.html":
			{
				qs.startQuest();
				break;
			}
			case "31518-01.html":
			{
				if (qs.isCond(1))
				{
					qs.setCond(2);
				}
				break;
			}
			case "31517-01.html":
			{
				if (qs.isCond(2))
				{
					addExpAndSp(player, 350531, 28204);
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
		final int npcId = npc.getId();
		
		switch (qs.getState())
		{
			case State.COMPLETED:
			{
				htmltext = getAlreadyCompletedMsg(player);
				break;
			}
			case State.CREATED:
			{
				if (npcId == VLADIMIR)
				{
					htmltext = (player.getLevel() >= 60) ? "31302-00.htm" : "31302-00a.html";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npcId)
				{
					case VLADIMIR:
					{
						if (qs.isCond(1))
						{
							htmltext = "31302-01a.html";
						}
						break;
					}
					case M_NECROMANCER:
					{
						final int cond = qs.getCond();
						if (cond == 1)
						{
							htmltext = "31518-00.html";
						}
						else if (cond == 2)
						{
							htmltext = "31518-01a.html";
						}
						break;
					}
					case HIERARCH:
					{
						if (qs.isCond(2))
						{
							htmltext = "31517-00.html";
						}
						break;
					}
				}
				break;
			}
		}
		return htmltext;
	}
}
