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
package quests.Q00027_ChestCaughtWithABaitOfWind;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

import quests.Q00050_LanoscosSpecialBait.Q00050_LanoscosSpecialBait;

/**
 * Chest Caught With A Bait Of Wind (27)<br>
 * Original Jython script by DooMIta.
 * @author nonom
 */
public class Q00027_ChestCaughtWithABaitOfWind extends Quest
{
	// NPCs
	private static final int LANOSCO = 31570;
	private static final int SHALING = 31434;
	// Items
	private static final int BLUE_TREASURE_BOX = 6500;
	private static final int STRANGE_BLUESPRINT = 7625;
	private static final int BLACK_PEARL_RING = 880;
	
	public Q00027_ChestCaughtWithABaitOfWind()
	{
		super(27, "Chest caught with a bait of wind");
		addStartNpc(LANOSCO);
		addTalkId(LANOSCO, SHALING);
		registerQuestItems(STRANGE_BLUESPRINT);
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
		
		switch (event)
		{
			case "31570-03.htm":
			{
				st.startQuest();
				break;
			}
			case "31570-05.htm":
			{
				if (st.isCond(1) && hasQuestItems(player, BLUE_TREASURE_BOX))
				{
					htmltext = "31570-06.htm";
					st.setCond(2, true);
					giveItems(player, STRANGE_BLUESPRINT, 1);
					takeItems(player, BLUE_TREASURE_BOX, -1);
				}
				break;
			}
			case "31434-02.htm":
			{
				if (st.isCond(2) && hasQuestItems(player, STRANGE_BLUESPRINT))
				{
					giveItems(player, BLACK_PEARL_RING, 1);
					st.exitQuest(false, true);
					htmltext = "31434-01.htm";
				}
				break;
			}
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
			case State.COMPLETED:
			{
				htmltext = getAlreadyCompletedMsg(player);
				break;
			}
			case State.CREATED:
			{
				final QuestState qs = player.getQuestState(Q00050_LanoscosSpecialBait.class.getSimpleName());
				if (npc.getId() == LANOSCO)
				{
					htmltext = "31570-02.htm";
					if (qs != null)
					{
						htmltext = ((player.getLevel() >= 27) && qs.isCompleted()) ? "31570-01.htm" : htmltext;
					}
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case LANOSCO:
					{
						if (st.isCond(1))
						{
							if (hasQuestItems(player, BLUE_TREASURE_BOX))
							{
								htmltext = "31570-04.htm";
							}
							else
							{
								htmltext = "31570-05.htm";
							}
						}
						else
						{
							htmltext = "31570-07.htm";
						}
						break;
					}
					case SHALING:
					{
						if (st.isCond(2))
						{
							htmltext = "31434-00.htm";
						}
						break;
					}
				}
			}
		}
		return htmltext;
	}
}
