/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package quests.Q00602_ShadowOfLight;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Shadow of Light (602)<br>
 * Original Jython script by disKret.
 * @author malyelfik
 */
public class Q00602_ShadowOfLight extends Quest
{
	// NPC
	private static final int EYE_OF_ARGOS = 31683;
	// Item
	private static final int EYE_OF_DARKNESS = 7189;
	// Monsters
	private static final int[] MOBS =
	{
		21299,
		21304
	};
	
	// Reward
	private static final int[][] REWARD =
	{
		{
			6699,
			40000,
			120000,
			20000
		},
		{
			6698,
			60000,
			110000,
			15000
		},
		{
			6700,
			40000,
			150000,
			10000
		},
		{
			0,
			100000,
			140000,
			11250
		}
	};
	
	public Q00602_ShadowOfLight()
	{
		super(602, "Shadow of Light");
		addStartNpc(EYE_OF_ARGOS);
		addTalkId(EYE_OF_ARGOS);
		addKillId(MOBS);
		registerQuestItems(EYE_OF_DARKNESS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = event;
		switch (event)
		{
			case "31683-02.htm":
			{
				qs.startQuest();
				break;
			}
			case "31683-05.html":
			{
				if (getQuestItemsCount(player, EYE_OF_DARKNESS) < 100)
				{
					return "31683-06.html";
				}
				final int i = getRandom(4);
				if (i < 3)
				{
					giveItems(player, REWARD[i][0], 3);
				}
				giveAdena(player, REWARD[i][1], true);
				addExpAndSp(player, REWARD[i][2], REWARD[i][3]);
				qs.exitQuest(true, true);
				break;
			}
			default:
			{
				htmltext = null;
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return;
		}
		
		final int chance = (npc.getId() == MOBS[0]) ? 560 : 800;
		if (qs.isCond(1) && (getRandom(1000) < chance))
		{
			giveItems(player, EYE_OF_DARKNESS, 1);
			if (getQuestItemsCount(player, EYE_OF_DARKNESS) == 100)
			{
				qs.setCond(2, true);
			}
			else
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
		}
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
				htmltext = (player.getLevel() >= 68) ? "31683-01.htm" : "31683-00.htm";
				break;
			}
			case State.STARTED:
			{
				htmltext = (qs.isCond(1)) ? "31683-03.html" : "31683-04.html";
				break;
			}
		}
		return htmltext;
	}
}