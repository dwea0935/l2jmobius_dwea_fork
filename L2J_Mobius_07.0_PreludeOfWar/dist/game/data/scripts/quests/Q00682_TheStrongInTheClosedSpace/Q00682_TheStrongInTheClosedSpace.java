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
package quests.Q00682_TheStrongInTheClosedSpace;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Faction;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.QuestType;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * The Strong in the Closed Space (682)
 * @URL https://l2wiki.com/The_Strong_in_the_Closed_Space
 * @author Dmitri
 */
public class Q00682_TheStrongInTheClosedSpace extends Quest
{
	// NPCs
	private static final int PENNY = 34413;
	// MiniBoss
	private static final int BURNSTEIN = 26136; // Burnstein (Command Post)
	private static final int ISADORA = 26220; // Isadora
	private static final int MALISS = 26219; // Maliss
	private static final int HELIOS = 26335; // Helios
	private static final int QEEN_KROSHA = 26390; // Queen Krosha
	// Misc
	private static final int MIN_LEVEL = 100;
	
	public Q00682_TheStrongInTheClosedSpace()
	{
		super(682);
		addStartNpc(PENNY);
		addTalkId(PENNY);
		addKillId(BURNSTEIN, ISADORA, MALISS, HELIOS, QEEN_KROSHA);
		addCondMinLevel(MIN_LEVEL, "nolevel.html");
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
			case "34413-02.htm":
			case "34413-03.htm":
			case "34413-06.html":
			{
				htmltext = event;
				break;
			}
			case "34413-04.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "34413-07.html":
			{
				// Rewards
				giveAdena(player, 3559950, true);
				addExpAndSp(player, 14831100000L, 14831100);
				addFactionPoints(player, Faction.ADVENTURE_GUILD, 100); // add FP points to ADVENTURE_GUILD Faction
				qs.exitQuest(QuestType.DAILY, true);
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
		switch (qs.getState())
		{
			case State.CREATED:
			{
				if (npc.getId() == PENNY)
				{
					htmltext = "34413-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case PENNY:
					{
						if (qs.isCond(1))
						{
							htmltext = "34413-04.htm";
						}
						else if (qs.isCond(2))
						{
							htmltext = "34413-05.html";
						}
						break;
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				if (qs.isNowAvailable())
				{
					qs.setState(State.CREATED);
					htmltext = "34413-01.htm";
				}
				else
				{
					htmltext = getAlreadyCompletedMsg(player, QuestType.DAILY);
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		executeForEachPlayer(player, npc, isSummon, true, false);
	}
	
	@Override
	public void actionForEachPlayer(Player player, Npc npc, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(1) && LocationUtil.checkIfInRange(Config.ALT_PARTY_RANGE, npc, player, false))
		{
			qs.setCond(2, true);
		}
	}
}
