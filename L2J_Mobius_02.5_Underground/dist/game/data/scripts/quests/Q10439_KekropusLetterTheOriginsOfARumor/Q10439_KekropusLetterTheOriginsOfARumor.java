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
package quests.Q10439_KekropusLetterTheOriginsOfARumor;

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
 * Kekropus' Letter: The Origins of a Rumor (10439)
 * @author Stayway
 */
public class Q10439_KekropusLetterTheOriginsOfARumor extends LetterQuest
{
	// NPCs
	private static final int GOSTA = 30916;
	private static final int HELVETICA = 32641;
	private static final int ATHENIA = 32643;
	private static final int INVISIBLE_NPC = 19543;
	// Items
	private static final int SOE_HEINE = 37112; // Scroll of Escape: Heine
	private static final int SOE_FIELD_OF_SILENCE = 37039; // Scroll of Escape: Field of Silence
	private static final int SOE_FIELD_OF_WISPERS = 37040; // Scroll of Escape: Field of Wispers
	private static final int EWS = 959; // Scroll: Enchant Weapon (S-grade)
	private static final int EAS = 960; // Scroll: Enchant Armor (S-grade)
	// Location
	private static final Location TELEPORT_LOC = new Location(108457, 221649, -3598);
	// Misc
	private static final int MIN_LEVEL = 81;
	private static final int MAX_LEVEL = 84;
	
	public Q10439_KekropusLetterTheOriginsOfARumor()
	{
		super(10439);
		addTalkId(GOSTA, HELVETICA, ATHENIA);
		addCreatureSeeId(INVISIBLE_NPC);
		addCondInCategory(CategoryType.MAGE_CLOACK, "nocond.html");
		setIsErtheiaQuest(false);
		setLevel(MIN_LEVEL, MAX_LEVEL);
		setStartQuestSound("Npcdialog1.kekrops_quest_9");
		setStartLocation(SOE_HEINE, TELEPORT_LOC);
		registerQuestItems(SOE_HEINE, SOE_FIELD_OF_SILENCE, SOE_FIELD_OF_WISPERS);
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
			case "30916-02.html":
			case "30916-03.html":
			{
				htmltext = event;
				break;
			}
			case "30916-04.html":
			{
				if (qs.isCond(1))
				{
					giveItems(player, SOE_FIELD_OF_SILENCE, 1);
					qs.setCond(2, true);
					htmltext = event;
				}
				break;
			}
			case "30916-06.html":
			{
				if (qs.isCond(1))
				{
					giveItems(player, SOE_FIELD_OF_WISPERS, 1);
					qs.setCond(3, true);
					htmltext = event;
				}
				break;
			}
			case "32641-02.html":
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
			case "32642-02.html":
			{
				if (qs.isCond(3))
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
			giveItems(player, SOE_HEINE, 1);
			if ((npc.getId() == GOSTA) && qs.isCond(1))
			{
				htmltext = "30916-01.html";
			}
			else if (qs.isCond(2))
			{
				htmltext = npc.getId() == GOSTA ? "30916-05.html" : "32641-01.html";
			}
			else if (qs.isCond(3))
			{
				htmltext = npc.getId() == GOSTA ? "30917-07.html" : "32642-01.html";
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
			if (qs != null)
			{
				if (qs.isCond(2))
				{
					showOnScreenMsg(player, NpcStringId.FIELD_OF_SILENCE_AND_FIELD_OR_WHISPERS_ARE_GOOD_HUNTING_ZONES_FOR_LV_81_OR_ABOVE, ExShowScreenMessage.TOP_CENTER, 6000);
				}
				else if (qs.isCond(3))
				{
					showOnScreenMsg(player, NpcStringId.FIELD_OF_SILENCE_AND_FIELD_OR_WHISPERS_ARE_GOOD_HUNTING_ZONES_FOR_LV_81_OR_ABOVE, ExShowScreenMessage.TOP_CENTER, 6000);
				}
			}
		}
	}
	
	@Override
	public boolean canShowTutorialMark(Player player)
	{
		return player.isInCategory(CategoryType.MAGE_CLOACK);
	}
}