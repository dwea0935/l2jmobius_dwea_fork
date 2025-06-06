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
package quests.Q10591_NobleMaterial;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.util.LocationUtil;

import quests.Q10590_ReawakenedFate.Q10590_ReawakenedFate;

/**
 * Noble Material (10591)
 * @URL https://www.youtube.com/watch?v=HCd784Gnguw
 * @author NightBR, Mobius
 */
public class Q10591_NobleMaterial extends Quest
{
	// NPCs
	private static final int JOACHIM = 34513;
	private static final int HARP_ZU_HESTUI = 34014;
	private static final int HERPA = 34362;
	private static final int LIONEL_HUNTER = 33907;
	private static final int[] MONSTERS =
	{
		23487, // Magma Ailith
		23489, // Lava Wyrm
		23490, // Lava Drake
		23491, // Lava Wendigo
		23492, // Lavastone Golem
		23493, // Lava Leviah
		23494, // Magma Salamander
		23495, // Magma Dre Vanul
		23499, // Flame Preta
		23500, // Flame Crow
		23501, // Flame Rael
		23502, // Flame Salamander
		23503, // Flame Drake
		23504, // Flame Votis
	};
	// Item
	private static final int FLAME_ENERGY = 80856; // Flame Energy - monster drop
	// Rewards
	private static final int ADENA_AMOUNT = 5050;
	private static final int ACHIEVEMENT_BOX_LV_100 = 80910;
	private static final int ACQUIRE_NOBLESSE_PRIVILEGES = 34983;
	private static final int WARRIOR_CICLET_BOX_LV5 = 80911;
	private static final int WIZARD_CICLET_BOX_LV5 = 80912;
	private static final int KNIGHT_CICLET_BOX_LV5 = 80913;
	// Misc
	private static final int MIN_LEVEL = 100;
	private static final SkillHolder NOBLESSE_PRESENTATION = new SkillHolder(18176, 1);
	// Location
	private static final Location BURNING_MARSH = new Location(152754, -15142, -4400);
	private static final Location HEIN = new Location(111257, 221071, -3550);
	
	public Q10591_NobleMaterial()
	{
		super(10591);
		addStartNpc(JOACHIM);
		addTalkId(JOACHIM, HARP_ZU_HESTUI, HERPA, LIONEL_HUNTER);
		addKillId(MONSTERS);
		registerQuestItems(FLAME_ENERGY);
		addCondMinLevel(99, "34513-16.html");
		addCondCompletedQuest(Q10590_ReawakenedFate.class.getSimpleName(), "34513-16.html");
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
			case "34513-02.htm":
			case "34513-07.html":
			case "34513-04.html":
			case "34014-03.html":
			case "34362-03.html":
			case "33907-03.html":
			{
				htmltext = event;
				break;
			}
			case "34513-03.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "teleport":
			{
				if (qs.isCond(1))
				{
					player.teleToLocation(BURNING_MARSH);
				}
				break;
			}
			case "34014-02.html":
			{
				qs.setCond(2, true);
				htmltext = event;
				break;
			}
			case "34513-06.html":
			{
				qs.setCond(4, true);
				htmltext = event;
				break;
			}
			case "34362-02.html":
			{
				qs.setCond(5, true);
				htmltext = event;
				break;
			}
			case "teleport_s":
			{
				if (qs.isCond(5))
				{
					player.teleToLocation(HEIN);
				}
				break;
			}
			case "33907-02.html":
			{
				qs.setCond(6, true);
				htmltext = event;
				break;
			}
			case "33907-05.html":
			{
				if (qs.isCond(7) && (player.getLevel() >= MIN_LEVEL))
				{
					// Reward #1
					giveAdena(player, ADENA_AMOUNT, false);
					giveItems(player, ACHIEVEMENT_BOX_LV_100, 1);
					giveItems(player, ACQUIRE_NOBLESSE_PRIVILEGES, 1);
					giveItems(player, WARRIOR_CICLET_BOX_LV5, 1);
					player.setNobleLevel(1);
					player.broadcastInfo();
					showOnScreenMsg(player, NpcStringId.CONGRATULATIONS_YOU_ARE_NOW_A_NOBLESSE, ExShowScreenMessage.TOP_CENTER, 10000);
					player.doCast(NOBLESSE_PRESENTATION.getSkill());
					qs.exitQuest(false, true);
					htmltext = event;
				}
				break;
			}
			case "33907-06.html":
			{
				if (qs.isCond(7) && (player.getLevel() >= MIN_LEVEL))
				{
					// Reward #2
					giveAdena(player, ADENA_AMOUNT, false);
					giveItems(player, ACHIEVEMENT_BOX_LV_100, 1);
					giveItems(player, ACQUIRE_NOBLESSE_PRIVILEGES, 1);
					giveItems(player, WIZARD_CICLET_BOX_LV5, 1);
					player.setNobleLevel(1);
					player.broadcastInfo();
					showOnScreenMsg(player, NpcStringId.CONGRATULATIONS_YOU_ARE_NOW_A_NOBLESSE, ExShowScreenMessage.TOP_CENTER, 10000);
					player.doCast(NOBLESSE_PRESENTATION.getSkill());
					qs.exitQuest(false, true);
					htmltext = event;
				}
				break;
			}
			case "33907-07.html":
			{
				if (qs.isCond(7) && (player.getLevel() >= MIN_LEVEL))
				{
					// Reward #3
					giveAdena(player, ADENA_AMOUNT, false);
					giveItems(player, ACHIEVEMENT_BOX_LV_100, 1);
					giveItems(player, ACQUIRE_NOBLESSE_PRIVILEGES, 1);
					giveItems(player, KNIGHT_CICLET_BOX_LV5, 1);
					player.setNobleLevel(1);
					player.broadcastInfo();
					showOnScreenMsg(player, NpcStringId.CONGRATULATIONS_YOU_ARE_NOW_A_NOBLESSE, ExShowScreenMessage.TOP_CENTER, 10000);
					player.doCast(NOBLESSE_PRESENTATION.getSkill());
					qs.exitQuest(false, true);
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
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (qs.getState())
		{
			case State.CREATED:
			{
				if (npc.getId() == JOACHIM)
				{
					htmltext = "34513-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case JOACHIM:
					{
						if ((getQuestItemsCount(player, FLAME_ENERGY) >= 1000) && (player.getLevel() >= MIN_LEVEL))
						{
							qs.setCond(3, true);
						}
						
						if (qs.isCond(1))
						{
							htmltext = "34513-04.htm";
						}
						else if (qs.isCond(2))
						{
							htmltext = "34513-15.html";
						}
						else if (qs.isCond(3))
						{
							htmltext = "34513-05.html";
						}
						else if (qs.isCond(4))
						{
							htmltext = "34513-07.html";
						}
						break;
					}
					case HARP_ZU_HESTUI:
					{
						if (qs.isCond(1))
						{
							htmltext = "34014-01.html";
						}
						else if (qs.isCond(2))
						{
							htmltext = "34014-03.html";
						}
						break;
					}
					case HERPA:
					{
						if (qs.isCond(4))
						{
							htmltext = "34362-01.html";
						}
						else if (qs.isCond(5))
						{
							htmltext = "34362-03.html";
						}
						break;
					}
					case LIONEL_HUNTER:
					{
						if (qs.isCond(5))
						{
							htmltext = "33907-01.html";
						}
						else if (qs.isCond(6))
						{
							htmltext = "33907-03.html";
						}
						else if (qs.isCond(7))
						{
							htmltext = "33907-04.html";
						}
						break;
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				htmltext = getAlreadyCompletedMsg(player);
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void actionForEachPlayer(Player player, Npc npc, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(2) && LocationUtil.checkIfInRange(Config.ALT_PARTY_RANGE, npc, player, false))
		{
			if ((getQuestItemsCount(player, FLAME_ENERGY) < 1000) && (getRandom(100) < 90))
			{
				giveItems(player, FLAME_ENERGY, 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
			if ((getQuestItemsCount(player, FLAME_ENERGY) >= 1000) && (player.getLevel() >= MIN_LEVEL))
			{
				qs.setCond(3, true);
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		executeForEachPlayer(killer, npc, isSummon, true, false);
	}
}
