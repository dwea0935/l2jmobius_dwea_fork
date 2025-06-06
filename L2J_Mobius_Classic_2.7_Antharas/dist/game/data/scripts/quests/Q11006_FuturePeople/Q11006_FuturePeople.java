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
package quests.Q11006_FuturePeople;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;

import quests.Q11005_PerfectLeatherArmor3.Q11005_PerfectLeatherArmor3;

/**
 * Future People (11016)
 * @author Stayway
 */
public class Q11006_FuturePeople extends Quest
{
	// NPCs
	private static final int LECTOR = 30001;
	private static final int PAYNE = 30136;
	private static final int PARINA = 30391;
	private static final int ZIGAUNT = 30022;
	private static final int AURON = 30010;
	private static final int KLAUS_VASPER = 30417;
	private static final int BEZIQUE = 30379;
	
	// Items
	private static final int FIRST_CLASS_BUFF_SCROLL = 29654;
	private static final int IMPROVED_SOE = 49087;
	
	// Misc
	private static final int MIN_LEVEL = 19;
	
	public Q11006_FuturePeople()
	{
		super(11006);
		addStartNpc(LECTOR);
		addTalkId(PAYNE, LECTOR, PARINA, ZIGAUNT, AURON, KLAUS_VASPER, BEZIQUE);
		addCondMinLevel(MIN_LEVEL, "no-level.html"); // Custom
		addCondRace(Race.HUMAN, "no-race.html"); // Custom
		addCondCompletedQuest(Q11005_PerfectLeatherArmor3.class.getSimpleName(), "30001-04.html");
		setQuestNameNpcStringId(NpcStringId.LV_19_FUTURE_PEOPLE);
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
			case "30001-02.htm":
			case "30001-02a.htm":
			case "f_warrior.html":
			case "f_knight.html":
			case "f_rogue.html":
			case "m_wizard.html":
			case "m_cleric.html":
			{
				htmltext = event;
				break;
			}
			case "a_warrior.html":
			{
				qs.startQuest();
				qs.setCond(2, true);
				htmltext = event;
				break;
			}
			case "a_knight.html":
			{
				qs.startQuest();
				qs.setCond(3, true);
				htmltext = event;
				break;
			}
			case "a_rogue.html":
			{
				qs.startQuest();
				qs.setCond(4, true);
				htmltext = event;
				break;
			}
			case "a_wizard.html":
			{
				qs.startQuest();
				qs.setCond(5, true);
				htmltext = event;
				break;
			}
			case "a_cleric.html": // Custom html
			{
				qs.startQuest();
				qs.setCond(5, true);
				htmltext = event;
				break;
			}
			case "30391-02.html":
			case "30022-02.html":
			case "30010-02.html":
			case "30417-02.html":
			case "30379-02.html": // Custom Html
			{
				if (qs.getCond() > 1)
				{
					giveItems(player, FIRST_CLASS_BUFF_SCROLL, 5);
					giveItems(player, IMPROVED_SOE, 1);
					qs.exitQuest(false, true);
					htmltext = event;
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		final QuestState qs = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		switch (qs.getState())
		{
			case State.CREATED:
			{
				if ((npc.getId() == LECTOR) && (talker.getPlayerClass() == PlayerClass.FIGHTER))
				{
					htmltext = "30001-01.html";
				}
				else if (talker.getPlayerClass() == PlayerClass.MAGE)
				{
					htmltext = "30001-01a.html";
				}
				break;
			}
			case State.STARTED:
			{
				if (npc.getId() == LECTOR)
				{
					if (qs.getCond() >= 1)
					{
						htmltext = "30001-03.html";
					}
					break;
				}
				if ((npc.getId() == PARINA) && (talker.getPlayerClass() != PlayerClass.WIZARD))
				{
					if (qs.isCond(5))
					{
						htmltext = "30391-01.html";
					}
					break;
				}
				if ((npc.getId() == ZIGAUNT) && (talker.getPlayerClass() != PlayerClass.CLERIC))
				{
					if (qs.isCond(6))
					{
						htmltext = "30022-01.html"; // Custom Html
					}
					break;
				}
				if ((npc.getId() == AURON) && (talker.getPlayerClass() != PlayerClass.WARRIOR))
				{
					if (qs.isCond(2))
					{
						htmltext = "30010-01.html";
					}
					break;
				}
				if ((npc.getId() == KLAUS_VASPER) && (talker.getPlayerClass() != PlayerClass.WARRIOR))
				{
					if (qs.isCond(3))
					{
						htmltext = "30417-01.html";
					}
					break;
				}
				if ((npc.getId() == BEZIQUE) && (talker.getPlayerClass() != PlayerClass.WARRIOR))
				{
					if (qs.isCond(4))
					{
						htmltext = "30379-01.html"; // Custom Html
					}
					break;
				}
				break;
			}
			case State.COMPLETED:
			{
				htmltext = getAlreadyCompletedMsg(talker);
				break;
			}
		}
		return htmltext;
	}
}