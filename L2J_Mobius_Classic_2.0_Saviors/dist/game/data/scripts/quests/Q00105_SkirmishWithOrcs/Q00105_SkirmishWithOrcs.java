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
package quests.Q00105_SkirmishWithOrcs;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Skimirish with Orcs (105)
 * @author Janiko
 */
public class Q00105_SkirmishWithOrcs extends Quest
{
	// NPC
	private static final int KENDNELL = 30218;
	// Items
	private static final int KENDELLS_1ST_ORDER = 1836;
	private static final int KENDELLS_2ND_ORDER = 1837;
	private static final int KENDELLS_3RD_ORDER = 1838;
	private static final int KENDELLS_4TH_ORDER = 1839;
	private static final int KENDELLS_5TH_ORDER = 1840;
	private static final int KENDELLS_6TH_ORDER = 1841;
	private static final int KENDELLS_7TH_ORDER = 1842;
	private static final int KENDELLS_8TH_ORDER = 1843;
	private static final int KABOO_CHIEFS_1ST_TORQUE = 1844;
	private static final int KABOO_CHIEFS_2ST_TORQUE = 1845;
	private static final Map<Integer, Integer> MONSTER_DROP = new HashMap<>();
	static
	{
		MONSTER_DROP.put(27059, KENDELLS_1ST_ORDER); // Uoph (Kaboo Chief)
		MONSTER_DROP.put(27060, KENDELLS_2ND_ORDER); // Kracha (Kaboo Chief)
		MONSTER_DROP.put(27061, KENDELLS_3RD_ORDER); // Batoh (Kaboo Chief)
		MONSTER_DROP.put(27062, KENDELLS_4TH_ORDER); // Tanukia (Kaboo Chief)
		MONSTER_DROP.put(27064, KENDELLS_5TH_ORDER); // Turel (Kaboo Chief)
		MONSTER_DROP.put(27065, KENDELLS_6TH_ORDER); // Roko (Kaboo Chief)
		MONSTER_DROP.put(27067, KENDELLS_7TH_ORDER); // Kamut (Kaboo Chief)
		MONSTER_DROP.put(27068, KENDELLS_8TH_ORDER); // Murtika (Kaboo Chief)
	}
	private static final int[] KENDNELLS_ORDERS =
	{
		KENDELLS_1ST_ORDER,
		KENDELLS_2ND_ORDER,
		KENDELLS_3RD_ORDER,
		KENDELLS_4TH_ORDER,
		KENDELLS_5TH_ORDER,
		KENDELLS_6TH_ORDER,
		KENDELLS_7TH_ORDER,
		KENDELLS_8TH_ORDER
	};
	// Rewards
	private static final int SWORD_OF_SUNSET = 49046;
	private static final int STAFF_OF_SUNSET = 49045;
	// Misc
	private static final int MIN_LEVEL = 10;
	private static final int MAX_LEVEL = 15;
	
	public Q00105_SkirmishWithOrcs()
	{
		super(105);
		addStartNpc(KENDNELL);
		addTalkId(KENDNELL);
		addKillId(MONSTER_DROP.keySet());
		registerQuestItems(KENDNELLS_ORDERS);
		addCondMaxLevel(MAX_LEVEL, "30218-02.htm");
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
			case "30218-04.html":
			{
				if (qs.isCreated())
				{
					qs.startQuest();
					giveItems(player, KENDNELLS_ORDERS[getRandom(0, 3)], 1);
					htmltext = event;
				}
				break;
			}
			case "30218-05.html":
			{
				htmltext = event;
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && LocationUtil.checkIfInRange(Config.ALT_PARTY_RANGE, npc, killer, true))
		{
			switch (npc.getId())
			{
				case 27059:
				case 27060:
				case 27061:
				case 27062:
				{
					if (qs.isCond(1) && hasQuestItems(killer, MONSTER_DROP.get(npc.getId())))
					{
						giveItems(killer, KABOO_CHIEFS_1ST_TORQUE, 1);
						qs.setCond(2, true);
					}
					break;
				}
				case 27064:
				case 27065:
				case 27067:
				case 27068:
				{
					if (qs.isCond(3) && hasQuestItems(killer, MONSTER_DROP.get(npc.getId())))
					{
						giveItems(killer, KABOO_CHIEFS_2ST_TORQUE, 1);
						qs.setCond(4, true);
					}
					break;
				}
			}
		}
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
				if (talker.getRace() == Race.ELF)
				{
					htmltext = (talker.getLevel() >= MIN_LEVEL) ? "30218-03.htm" : "30218-02.htm";
				}
				else
				{
					htmltext = "30218-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				if (hasAtLeastOneQuestItem(talker, KENDELLS_1ST_ORDER, KENDELLS_2ND_ORDER, KENDELLS_3RD_ORDER, KENDELLS_4TH_ORDER))
				{
					htmltext = "30218-06.html";
				}
				if (qs.isCond(2) && hasQuestItems(talker, KABOO_CHIEFS_1ST_TORQUE))
				{
					for (int i = 0; i < 4; i++)
					{
						takeItems(talker, KENDNELLS_ORDERS[i], -1);
					}
					takeItems(talker, KABOO_CHIEFS_1ST_TORQUE, 1);
					giveItems(talker, KENDNELLS_ORDERS[getRandom(4, 7)], 1);
					qs.setCond(3, true);
					htmltext = "30218-07.html";
				}
				if (hasAtLeastOneQuestItem(talker, KENDELLS_5TH_ORDER, KENDELLS_6TH_ORDER, KENDELLS_7TH_ORDER, KENDELLS_8TH_ORDER))
				{
					htmltext = "30218-08.html";
				}
				if (qs.isCond(4) && hasQuestItems(talker, KABOO_CHIEFS_2ST_TORQUE))
				{
					// Q00281_HeadForTheHills.giveNewbieReward(talker);
					if (talker.isMageClass())
					{
						giveItems(talker, STAFF_OF_SUNSET, 1);
					}
					else
					{
						giveItems(talker, SWORD_OF_SUNSET, 1);
					}
					talker.sendPacket(new SocialAction(talker.getObjectId(), 3));
					qs.exitQuest(false, true);
					htmltext = "30218-09.html";
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