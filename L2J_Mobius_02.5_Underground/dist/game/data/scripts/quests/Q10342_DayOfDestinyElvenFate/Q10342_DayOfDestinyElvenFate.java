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
package quests.Q10342_DayOfDestinyElvenFate;

import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

import quests.ThirdClassTransferQuest;

/**
 * Day of Destiny: Elven Fate (10342)
 * @author St3eT
 */
public class Q10342_DayOfDestinyElvenFate extends ThirdClassTransferQuest
{
	// NPC
	private static final int WINONIN = 30856;
	// Misc
	private static final int MIN_LEVEL = 76;
	private static final Race START_RACE = Race.ELF;
	
	public Q10342_DayOfDestinyElvenFate()
	{
		super(10342, MIN_LEVEL, START_RACE);
		addStartNpc(WINONIN);
		addTalkId(WINONIN);
		addCondMinLevel(MIN_LEVEL, "30856-11.html");
		addCondRace(START_RACE, "30856-11.html");
		addCondInCategory(CategoryType.THIRD_CLASS_GROUP, "30856-12.html");
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
			case "30856-02.htm":
			case "30856-03.htm":
			case "30856-04.htm":
			case "30856-08.html":
			{
				htmltext = event;
				break;
			}
			case "30856-05.htm":
			{
				qs.startQuest();
				qs.set("STARTED_CLASS", player.getPlayerClass().getId());
				htmltext = event;
				break;
			}
			default:
			{
				htmltext = super.onEvent(event, npc, player);
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState qs = getQuestState(player, true);
		if (npc.getId() == WINONIN)
		{
			if (qs.getState() == State.CREATED)
			{
				htmltext = "30856-01.htm";
			}
			else if (qs.getState() == State.STARTED)
			{
				if (qs.isCond(1))
				{
					htmltext = "30856-06.html";
				}
				else if (qs.isCond(13))
				{
					htmltext = "30856-07.html";
				}
			}
		}
		return (!htmltext.equals(getNoQuestMsg(player)) ? htmltext : super.onTalk(npc, player));
	}
}