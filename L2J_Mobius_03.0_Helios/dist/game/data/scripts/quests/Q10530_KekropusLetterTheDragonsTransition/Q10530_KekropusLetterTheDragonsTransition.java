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
package quests.Q10530_KekropusLetterTheDragonsTransition;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

import quests.LetterQuest;

/**
 * Kekropus' Letter: Belos' Whereabouts (10424)
 * @author Stayway
 */
public class Q10530_KekropusLetterTheDragonsTransition extends LetterQuest
{
	// NPCs
	private static final int JERONIN = 30121;
	private static final int NAMO = 33973;
	private static final int INVISIBLE_NPC = 19543;
	// Items
	private static final int SOE_TOWN_OF_GIRAN = 46733; // Scroll of Escape: Town of GIRAN
	private static final int SOE_DRAGON_VALLEY = 46734; // Scroll of Escape: Dragon Valley
	// Location
	private static final Location TELEPORT_LOC = new Location(84015, 147219, -3395);
	// Rewards
	private static final int XP = 1533168;
	private static final int SP = 306;
	// Misc
	private static final int MIN_LEVEL = 81;
	private static final int MAX_LEVEL = 84;
	
	public Q10530_KekropusLetterTheDragonsTransition()
	{
		super(10530);
		addTalkId(JERONIN, NAMO);
		addCreatureSeeId(INVISIBLE_NPC);
		setIsErtheiaQuest(false);
		setLevel(MIN_LEVEL, MAX_LEVEL);
		setStartQuestSound("Npcdialog1.kekrops_quest_15");
		setStartLocation(SOE_TOWN_OF_GIRAN, TELEPORT_LOC);
		registerQuestItems(SOE_TOWN_OF_GIRAN, SOE_DRAGON_VALLEY);
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
			case "30121-02.htm":
			case "33973-02.html":
			{
				htmltext = event;
				break;
			}
			case "30121-03.htm":
			{
				if (qs.isCond(2))
				{
					takeItems(player, SOE_TOWN_OF_GIRAN, -1);
					giveItems(player, SOE_DRAGON_VALLEY, 1);
					qs.setCond(3, true);
					htmltext = event;
				}
				break;
			}
			case "33973-03.html":
			{
				if (qs.isCond(3))
				{
					qs.exitQuest(false, true);
					if (player.getLevel() >= MIN_LEVEL)
					{
						addExpAndSp(player, XP, SP);
					}
					showOnScreenMsg(player, NpcStringId.YOU_HAVE_FINISHED_ALL_OF_KEKROPUS_LETTERS_GROW_STRONGER_HERE_UNTIL_YOU_RECEIVE_LETTERS_FROM_A_MINSTREL_AT_LV_85, ExShowScreenMessage.TOP_CENTER, 6000);
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
			if ((npc.getId() == JERONIN) && qs.isCond(2))
			{
				htmltext = "30121-01.htm";
			}
			else if (qs.isCond(3))
			{
				htmltext = npc.getId() == JERONIN ? "30121-04.htm" : "33973-01.html";
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
			if ((qs != null) && qs.isCond(3))
			{
				showOnScreenMsg(player, NpcStringId.DEN_OF_EVIL_IS_A_GOOD_HUNTING_ZONE_FOR_LV_81_OR_ABOVE, ExShowScreenMessage.TOP_CENTER, 6000);
			}
		}
	}
}