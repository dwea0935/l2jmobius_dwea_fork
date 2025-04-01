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
package quests.Q10436_KekropusLetterTheSealOfPunishment;

import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

import quests.LetterQuest;

/**
 * Kekropus' Letter: The Seal of Punishment (10436)
 * @author Stayway
 */
public class Q10436_KekropusLetterTheSealOfPunishment extends LetterQuest
{
	// NPCs
	private static final int MOUEN = 30196;
	private static final int LAKI = 32742;
	private static final int INVISIBLE_NPC = 19543;
	// Items
	private static final int SOE_TOWN_OF_OREN = 37114; // Scroll of Escape: Town of Oren
	private static final int EWS = 959; // Scroll: Enchant Weapon (S-grade)
	private static final int EAS = 960; // Scroll: Enchant Armor (S-grade)
	// Location
	private static final Location TELEPORT_LOC = new Location(86404, -142221, -1341);
	// Misc
	private static final int MIN_LEVEL = 81;
	private static final int MAX_LEVEL = 84;
	
	public Q10436_KekropusLetterTheSealOfPunishment()
	{
		super(10436);
		addTalkId(MOUEN, LAKI);
		addCreatureSeeId(INVISIBLE_NPC);
		addCondInCategory(CategoryType.BOW_MASTER, "nocond.html");
		setIsErtheiaQuest(false);
		setLevel(MIN_LEVEL, MAX_LEVEL);
		setStartQuestSound("Npcdialog1.kekrops_quest_9");
		setStartLocation(SOE_TOWN_OF_OREN, TELEPORT_LOC);
		registerQuestItems(SOE_TOWN_OF_OREN);
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
			case "30196-02.html":
			{
				htmltext = event;
				break;
			}
			case "30196-03.html":
			{
				if (qs.isCond(1))
				{
					giveItems(player, SOE_TOWN_OF_OREN, 1);
					qs.setCond(2, true);
					htmltext = event;
				}
				break;
			}
			case "32472-02.html":
			{
				if (qs.isCond(2))
				{
					qs.exitQuest(false, true);
					giveItems(player, EWS, 1);
					giveItems(player, EAS, 10);
					giveStoryQuestReward(player, 235);
					if ((player.getLevel() >= MIN_LEVEL) && (player.getLevel() <= MAX_LEVEL))
					{
						addExpAndSp(player, 1_412_040, 338);
					}
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
		String htmltext = getNoQuestMsg(player);
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return htmltext;
		}
		
		if (qs.isStarted())
		{
			if ((npc.getId() == MOUEN) && qs.isCond(1))
			{
				htmltext = "30196-01.html";
			}
			else if (qs.isCond(2))
			{
				htmltext = npc.getId() == MOUEN ? "30196-04.html" : "32472-01.html";
			}
		}
		return htmltext;
	}
	
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			final QuestState qs = getQuestState(player, false);
			if ((qs != null) && qs.isCond(2))
			{
				showOnScreenMsg(player, NpcStringId.PLAINS_OF_THE_LIZARDMEN_IS_A_GOOD_HUNTING_ZONE_FOR_LV_81_OR_ABOVE, ExShowScreenMessage.TOP_CENTER, 6000);
			}
		}
	}
	
	@Override
	public boolean canShowTutorialMark(Player player)
	{
		return player.isInCategory(CategoryType.BOW_MASTER);
	}
}