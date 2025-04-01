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
package quests.Q11018_FutureDarkElves;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;

import quests.Q11017_PrepareForTrade3.Q11017_PrepareForTrade3;

/**
 * Future: Dark Elves (11018)
 * @author Stayway
 */
public class Q11018_FutureDarkElves extends Quest
{
	// NPCs
	private static final int VOLLODOS = 30137;
	private static final int VIRGIL = 30329;
	private static final int TRISKEL = 30416;
	private static final int VARIKA = 30421;
	private static final int SIDRA = 30330;
	
	// Items
	private static final int FIRST_CLASS_BUFF_SCROLL = 29654;
	private static final int IMPROVED_SOE = 49087;
	
	// Misc
	private static final int MIN_LEVEL = 19;
	
	public Q11018_FutureDarkElves()
	{
		super(11018);
		addStartNpc(VOLLODOS);
		addTalkId(VIRGIL, VOLLODOS, TRISKEL, VARIKA, SIDRA);
		addCondMinLevel(MIN_LEVEL, "no-level.html"); // Custom
		addCondRace(Race.DARK_ELF, "no-race.html"); // Custom
		addCondCompletedQuest(Q11017_PrepareForTrade3.class.getSimpleName(), "30137-04.html");
		setQuestNameNpcStringId(NpcStringId.LV_19_FUTURE_DARK_ELVES);
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
			case "30137-02.htm":
			case "30137-02a.htm":
			case "f_PalusKnight.html":
			case "f_assassin.html":
			case "m_wizard.html":
			case "m_shillien.html":
			{
				htmltext = event;
				break;
			}
			case "a_PalusKnight.html":
			{
				qs.startQuest();
				qs.setCond(2, true);
				htmltext = event;
				break;
			}
			case "a_assassin.html": // Custom html
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
			case "a_shillien.html": // Custom html
			{
				qs.startQuest();
				qs.setCond(5, true);
				htmltext = event;
				break;
			}
			case "30329-02.html":
			case "30416-02.html":
			case "30421-02.html":
			case "30330-02.html":
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
				if ((npc.getId() == VOLLODOS) && (talker.getPlayerClass() == PlayerClass.DARK_FIGHTER))
				{
					htmltext = "30137-01.html";
				}
				else if (talker.getPlayerClass() == PlayerClass.DARK_MAGE)
				{
					htmltext = "30137-01a.html";
				}
				break;
			}
			case State.STARTED:
			{
				if (npc.getId() == VOLLODOS)
				{
					if (qs.getCond() >= 1)
					{
						htmltext = "30137-03.html";
					}
					break;
				}
				if ((npc.getId() == VIRGIL) && (talker.getPlayerClass() != PlayerClass.PALUS_KNIGHT))
				{
					if (qs.isCond(2))
					{
						htmltext = "30329-01.html";
					}
					break;
				}
				if ((npc.getId() == TRISKEL) && (talker.getPlayerClass() != PlayerClass.ASSASSIN))
				{
					if (qs.isCond(3))
					{
						htmltext = "30416-01.html"; // Custom Html
					}
					break;
				}
				if ((npc.getId() == VARIKA) && (talker.getPlayerClass() != PlayerClass.DARK_WIZARD))
				{
					if (qs.isCond(4))
					{
						htmltext = "30421-01.html";
					}
					break;
				}
				if ((npc.getId() == SIDRA) && (talker.getPlayerClass() != PlayerClass.DARK_WIZARD))
				{
					if (qs.isCond(5))
					{
						htmltext = "30330-01.html"; // Custom html
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