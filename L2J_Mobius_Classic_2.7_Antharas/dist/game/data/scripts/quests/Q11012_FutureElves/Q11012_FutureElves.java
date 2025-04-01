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
package quests.Q11012_FutureElves;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;

import quests.Q11011_NewPotionDevelopment3.Q11011_NewPotionDevelopment3;

/**
 * Future: Future Elves (11012)
 * @author Stayway
 */
public class Q11012_FutureElves extends Quest
{
	// NPCs
	private static final int HERBIEL = 30150;
	private static final int SORIUS = 30327;
	private static final int REISA = 30328;
	private static final int ROSELLA = 30414;
	private static final int MANUEL = 30293;
	// Items
	private static final int FIRST_CLASS_BUFF_SCROLL = 29654;
	private static final int IMPROVED_SOE = 49087;
	// Misc
	private static final int MIN_LEVEL = 19;
	
	public Q11012_FutureElves()
	{
		super(11012);
		addStartNpc(HERBIEL);
		addTalkId(HERBIEL, SORIUS, REISA, ROSELLA, MANUEL);
		addCondMinLevel(MIN_LEVEL, "no-level.html"); // Custom
		addCondRace(Race.ELF, "no-race.html"); // Custom
		addCondCompletedQuest(Q11011_NewPotionDevelopment3.class.getSimpleName(), "30150-04.html");
		setQuestNameNpcStringId(NpcStringId.LV_19_FUTURE_ELVES);
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
			case "30150-02.htm":
			case "30150-02a.htm":
			case "f_knight.html":
			case "f_scout.html":
			case "m_wizard.html":
			case "m_oracle.html":
			{
				htmltext = event;
				break;
			}
			case "a_knight.html":
			{
				qs.startQuest();
				qs.setCond(2, true);
				htmltext = event;
				break;
			}
			case "a_scout.html":
			{
				qs.startQuest();
				qs.setCond(3, true);
				htmltext = event;
				break;
			}
			case "a_wizard.html":
			{
				qs.startQuest();
				qs.setCond(4, true);
				htmltext = event;
				break;
			}
			case "a_oracle.html":
			{
				qs.startQuest();
				qs.setCond(5, true);
				htmltext = event;
				break;
			}
			case "30327-02.html":
			case "30328-02.html": // Custom html
			case "30414-02.html":
			case "30293-02.html": // Custom html
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
				if ((npc.getId() == HERBIEL) && (talker.getPlayerClass() == PlayerClass.ELVEN_FIGHTER))
				{
					htmltext = "30150-01.html";
				}
				else if (talker.getPlayerClass() == PlayerClass.ELVEN_MAGE)
				{
					htmltext = "30150-01a.html";
				}
				break;
			}
			case State.STARTED:
			{
				if (npc.getId() == HERBIEL)
				{
					if (qs.getCond() >= 1)
					{
						htmltext = "30150-03.html";
					}
					break;
				}
				if ((npc.getId() == SORIUS) && (talker.getPlayerClass() != PlayerClass.ELVEN_KNIGHT))
				{
					if (qs.isCond(2))
					{
						htmltext = "30327-01.html"; // Custom html
					}
					break;
				}
				if ((npc.getId() == REISA) && (talker.getPlayerClass() != PlayerClass.ELVEN_SCOUT))
				{
					if (qs.isCond(3))
					{
						htmltext = "30328-01.html";
					}
					break;
				}
				if ((npc.getId() == ROSELLA) && (talker.getPlayerClass() != PlayerClass.ELVEN_WIZARD))
				{
					if (qs.isCond(4))
					{
						htmltext = "30414-01.html";
					}
					break;
				}
				if ((npc.getId() == MANUEL) && (talker.getPlayerClass() != PlayerClass.ORACLE))
				{
					if (qs.isCond(5))
					{
						htmltext = "30293-01.html"; // Custom html
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