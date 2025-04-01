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
package quests.Q10430_KekropusLetterTrackingTheEvil;

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
 * Kekropus' Letter: Tracking The Evil (10430)
 * @author Stayway
 */
public class Q10430_KekropusLetterTrackingTheEvil extends LetterQuest
{
	// NPCs
	private static final int VISHOTSKY = 31981;
	private static final int JOKEL = 33868;
	private static final int INVISIBLE_NPC = 19543;
	// Items
	private static final int SOE_TOWN_OF_SCHUTTGART = 37123; // Scroll of Escape: Town of Schuttgart
	private static final int SOE_DEN_OF_EVIL = 37036; // Scroll of Escape: Den of Evil
	private static final int EWS = 959; // Scroll: Enchant Weapon (S-grade)
	private static final int EAS = 960; // Scroll: Enchant Armor (S-grade)
	// Location
	private static final Location TELEPORT_LOC = new Location(86404, -142221, -1341);
	// Misc
	private static final int MIN_LEVEL = 81;
	private static final int MAX_LEVEL = 84;
	
	public Q10430_KekropusLetterTrackingTheEvil()
	{
		super(10430);
		addTalkId(VISHOTSKY, JOKEL);
		addCreatureSeeId(INVISIBLE_NPC);
		addCondInCategory(CategoryType.MAGE_GROUP, "nocond.html");
		setIsErtheiaQuest(false);
		setLevel(MIN_LEVEL, MAX_LEVEL);
		setStartQuestSound("Npcdialog1.kekrops_quest_9");
		setStartLocation(SOE_TOWN_OF_SCHUTTGART, TELEPORT_LOC);
		registerQuestItems(SOE_TOWN_OF_SCHUTTGART, SOE_DEN_OF_EVIL);
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
			case "31981-02.html":
			{
				htmltext = event;
				break;
			}
			case "31981-03.html":
			{
				if (qs.isCond(1))
				{
					takeItems(player, SOE_TOWN_OF_SCHUTTGART, -1);
					giveItems(player, SOE_DEN_OF_EVIL, 1);
					qs.setCond(2, true);
					htmltext = event;
				}
				break;
			}
			case "33868-02.html":
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
			if ((npc.getId() == VISHOTSKY) && qs.isCond(1))
			{
				htmltext = "31981-01.html";
			}
			else if (qs.isCond(2))
			{
				htmltext = npc.getId() == VISHOTSKY ? "31981-04.html" : "33868-01.html";
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
				showOnScreenMsg(player, NpcStringId.DEN_OF_EVIL_IS_A_GOOD_HUNTING_ZONE_FOR_LV_81_OR_ABOVE, ExShowScreenMessage.TOP_CENTER, 6000);
			}
		}
	}
	
	@Override
	public boolean canShowTutorialMark(Player player)
	{
		return player.isInCategory(CategoryType.FIGHTER_GROUP);
	}
}