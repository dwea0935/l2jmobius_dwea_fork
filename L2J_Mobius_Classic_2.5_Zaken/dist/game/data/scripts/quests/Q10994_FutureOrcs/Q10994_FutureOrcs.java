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
package quests.Q10994_FutureOrcs;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;

import quests.Q11023_RedGemNecklace3.Q11023_RedGemNecklace3;

/**
 * Future: Future Orcs (10994)
 * @author Stayway
 */
public class Q10994_FutureOrcs extends Quest
{
	// NPCs
	private static final int USKA = 30560;
	private static final int KARUKIA = 30570;
	private static final int GANTAKAI = 30587;
	private static final int HESTUI = 30585;
	// Items
	private static final int SCROLL_OF_BLOOD_MELODY = 49772;
	private static final int IMPROVED_SOE = 49087;
	// Misc
	private static final int MIN_LEVEL = 19;
	
	public Q10994_FutureOrcs()
	{
		super(10994);
		addStartNpc(USKA);
		addTalkId(USKA, KARUKIA, GANTAKAI, HESTUI);
		addCondMinLevel(MIN_LEVEL, "no-level.html"); // Custom
		addCondRace(Race.ORC, "no-race.html"); // Custom
		addCondCompletedQuest(Q11023_RedGemNecklace3.class.getSimpleName(), "30560-04.html");
		setQuestNameNpcStringId(NpcStringId.LV_19_FUTURE_ORCS);
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
			case "30560-02.htm":
			case "30560-02a.htm":
			case "f_raider.html":
			case "f_monk.html":
			case "m_shaman.html":
			{
				htmltext = event;
				break;
			}
			case "a_raider.html":
			{
				qs.startQuest();
				qs.setCond(2, true);
				htmltext = event;
				break;
			}
			case "a_monk.html":
			{
				qs.startQuest();
				qs.setCond(3, true);
				htmltext = event;
				break;
			}
			case "a_shaman.html":
			{
				qs.startQuest();
				qs.setCond(4, true);
				htmltext = event;
				break;
			}
			case "30570-02.html": // Custom html
			case "30587-02.html":
			case "30585-02.html":
			{
				if (qs.getCond() > 1)
				{
					giveItems(player, SCROLL_OF_BLOOD_MELODY, 2);
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
				if ((npc.getId() == USKA) && (talker.getPlayerClass() == PlayerClass.ORC_FIGHTER))
				{
					htmltext = "30560-01.html";
				}
				else if (talker.getPlayerClass() == PlayerClass.ORC_MAGE)
				{
					htmltext = "30560-01a.html";
				}
				break;
			}
			case State.STARTED:
			{
				if (npc.getId() == USKA)
				{
					if (qs.getCond() >= 1)
					{
						htmltext = "30560-03.html";
					}
					break;
				}
				if ((npc.getId() == KARUKIA) && (talker.getPlayerClass() != PlayerClass.ORC_RAIDER))
				{
					if (qs.isCond(2))
					{
						htmltext = "30570-01.html"; // Custom html
					}
					break;
				}
				if ((npc.getId() == GANTAKAI) && (talker.getPlayerClass() != PlayerClass.ORC_MONK))
				{
					if (qs.isCond(3))
					{
						htmltext = "30587-01.html";
					}
					break;
				}
				if ((npc.getId() == HESTUI) && (talker.getPlayerClass() != PlayerClass.ORC_SHAMAN))
				{
					if (qs.isCond(4))
					{
						htmltext = "30585-01.html";
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