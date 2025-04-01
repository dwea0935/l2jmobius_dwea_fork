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
package quests.Q10993_FutureDwarves;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;

import quests.Q10999_LoserPriest3.Q10999_LoserPriest3;

/**
 * Future: Dwarves (10993)
 * @author Stayway
 */
public class Q10993_FutureDwarves extends Quest
{
	// NPCs
	private static final int GERALD = 30650;
	private static final int PIPPI = 30524;
	private static final int SILVERA = 30527;
	
	// Items
	private static final int FIRST_CLASS_BUFF_SCROLL = 29654;
	private static final int IMPROVED_SOE = 49087;
	
	// Misc
	private static final int MIN_LEVEL = 19;
	
	public Q10993_FutureDwarves()
	{
		super(10993);
		addStartNpc(GERALD);
		addTalkId(PIPPI, GERALD, SILVERA);
		addCondMinLevel(MIN_LEVEL, "no-level.html"); // Custom
		addCondRace(Race.DWARF, "no-race.html"); // Custom
		addCondCompletedQuest(Q10999_LoserPriest3.class.getSimpleName(), "30650-04.html");
		setQuestNameNpcStringId(NpcStringId.LV_19_FUTURE_DWARVES);
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
			case "30650-02.htm":
			case "f_scavenger.html":
			case "f_artisan.html":
			{
				htmltext = event;
				break;
			}
			case "a_scavenger.html":
			{
				qs.startQuest();
				qs.setCond(2, true);
				htmltext = event;
				break;
			}
			case "a_artisan.html": // Custom html
			{
				qs.startQuest();
				qs.setCond(3, true);
				htmltext = event;
				break;
			}
			case "30524-02.html":
			case "30527-02.html":
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
				if ((npc.getId() == GERALD))
				{
					htmltext = "30650-01.html";
				}
				break;
			}
			case State.STARTED:
			{
				if (npc.getId() == GERALD)
				{
					if (qs.getCond() >= 1)
					{
						htmltext = "30650-03.html";
					}
					break;
				}
				if ((npc.getId() == PIPPI) && (talker.getPlayerClass() != PlayerClass.SCAVENGER))
				{
					if (qs.isCond(2))
					{
						htmltext = "30524-01.html";
					}
					break;
				}
				if ((npc.getId() == SILVERA) && (talker.getPlayerClass() != PlayerClass.ARTISAN))
				{
					if (qs.isCond(3))
					{
						htmltext = "30527-01.html"; // Custom Html
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