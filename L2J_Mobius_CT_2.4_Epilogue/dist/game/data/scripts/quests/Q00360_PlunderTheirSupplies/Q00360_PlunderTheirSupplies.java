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
package quests.Q00360_PlunderTheirSupplies;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Plunder Supplies (360)
 * @author netvirus
 */
public class Q00360_PlunderTheirSupplies extends Quest
{
	// Npc
	private static final int COLEMAN = 30873;
	// Misc
	private static final int MIN_LEVEL = 52;
	// Monsters
	private static final Map<Integer, Integer> MONSTER_DROP_CHANCES = new HashMap<>();
	// Items
	private static final int RECIPE_OF_SUPPLY = 5870;
	private static final int SUPPLY_ITEMS = 5872;
	private static final int SUSPICIOUS_DOCUMENT_PIECE = 5871;
	static
	{
		MONSTER_DROP_CHANCES.put(20666, 50); // Taik Orc Seeker
		MONSTER_DROP_CHANCES.put(20669, 75); // Taik Orc Supply Leader
	}
	
	public Q00360_PlunderTheirSupplies()
	{
		super(360, "Plunder Their Supplies");
		addStartNpc(COLEMAN);
		addTalkId(COLEMAN);
		addKillId(MONSTER_DROP_CHANCES.keySet());
		registerQuestItems(SUPPLY_ITEMS, SUSPICIOUS_DOCUMENT_PIECE, RECIPE_OF_SUPPLY);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		String htmltext = null;
		if (qs == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "30873-03.htm":
			case "30873-09.html":
			{
				htmltext = event;
				break;
			}
			case "30873-04.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "30873-10.html":
			{
				qs.exitQuest(false, true);
				htmltext = event;
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isPet)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs == null) || !LocationUtil.checkIfInRange(Config.ALT_PARTY_RANGE, npc, killer, false))
		{
			return;
		}
		
		if (getRandom(100) < MONSTER_DROP_CHANCES.get(npc.getId()))
		{
			giveItems(killer, SUPPLY_ITEMS, 1);
			playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
		
		if (getRandom(100) < 10)
		{
			if (getQuestItemsCount(killer, SUSPICIOUS_DOCUMENT_PIECE) < 4)
			{
				giveItems(killer, SUSPICIOUS_DOCUMENT_PIECE, 1);
			}
			else
			{
				giveItems(killer, RECIPE_OF_SUPPLY, 1);
				takeItems(killer, SUSPICIOUS_DOCUMENT_PIECE, -1);
			}
			playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
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
				htmltext = (player.getLevel() >= MIN_LEVEL) ? "30873-02.htm" : "30873-01.html";
				break;
			}
			case State.STARTED:
			{
				final long supplyCount = getQuestItemsCount(player, SUPPLY_ITEMS);
				final long recipeCount = getQuestItemsCount(player, RECIPE_OF_SUPPLY);
				if (supplyCount == 0)
				{
					if (recipeCount == 0)
					{
						htmltext = "30873-05.html";
					}
					else
					{
						giveAdena(player, (recipeCount * 6000), true);
						takeItems(player, RECIPE_OF_SUPPLY, -1);
						htmltext = "30873-08.html";
					}
				}
				else
				{
					if (recipeCount == 0)
					{
						giveAdena(player, ((supplyCount * 100) + 6000), true);
						takeItems(player, SUPPLY_ITEMS, -1);
						htmltext = "30873-06.html";
					}
					else
					{
						giveAdena(player, (((supplyCount * 100) + 6000) + (recipeCount * 6000)), true);
						takeItems(player, SUPPLY_ITEMS, -1);
						takeItems(player, RECIPE_OF_SUPPLY, -1);
						htmltext = "30873-07.html";
					}
				}
				break;
			}
		}
		return htmltext;
	}
}
