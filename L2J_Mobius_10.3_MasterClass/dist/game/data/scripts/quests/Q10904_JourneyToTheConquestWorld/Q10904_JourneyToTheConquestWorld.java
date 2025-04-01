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
package quests.Q10904_JourneyToTheConquestWorld;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * @author CostyKiller
 */
public class Q10904_JourneyToTheConquestWorld extends Quest
{
	// NPCs
	private static final int ENTROPY = 34599;
	private static final int CHLOE = 34600;
	// Items
	private static final int CONQUEST_NAME_CHANGE_COUPON = 81979;
	private static final int CHLOE_INVITATION = 82176;
	// Misc
	private static final int MIN_LEVEL = 110;
	
	public Q10904_JourneyToTheConquestWorld()
	{
		super(10904);
		addStartNpc(CHLOE);
		addTalkId(CHLOE, ENTROPY);
		addCondMinLevel(MIN_LEVEL, "34600-00.html");
		registerQuestItems(CHLOE_INVITATION);
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
			case "34600-02.html":
			{
				if ((player.getLevel() >= MIN_LEVEL))
				{
					qs.startQuest();
					giveItems(player, CHLOE_INVITATION, 1);
					htmltext = event;
				}
				break;
			}
			case "34599-02.html":
			{
				if (qs.isCond(1) && (hasQuestItems(player, CHLOE_INVITATION)))
				{
					takeItems(player, -1, CHLOE_INVITATION);
					qs.setCond(2);
					htmltext = event;
				}
				break;
			}
			case "34599-04.html":
			{
				if (qs.isCond(2))
				{
					giveItems(player, CONQUEST_NAME_CHANGE_COUPON, 1);
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
		switch (qs.getState())
		{
			case State.CREATED:
			{
				if (npc.getId() == CHLOE)
				{
					htmltext = "34600-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case CHLOE:
					{
						if (qs.isCond(1))
						{
							htmltext = "34600-02.html";
						}
						break;
					}
					case ENTROPY:
					{
						if (qs.isCond(1))
						{
							htmltext = "34599-01.htm";
						}
						else if (qs.isCond(2))
						{
							htmltext = "34599-03.htm";
						}
						break;
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				htmltext = getAlreadyCompletedMsg(player);
				break;
			}
		}
		return htmltext;
	}
}
