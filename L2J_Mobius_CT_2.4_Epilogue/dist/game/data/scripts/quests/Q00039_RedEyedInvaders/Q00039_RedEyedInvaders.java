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
package quests.Q00039_RedEyedInvaders;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;

/**
 * Red-eyed Invaders (39)
 * @author Janiko
 */
public class Q00039_RedEyedInvaders extends Quest
{
	// NPCs
	private static final int CAPTAIN_BATHIA = 30332;
	private static final int GUARD_BABENCO = 30334;
	// Monsters
	private static final int MALE_LIZARDMAN = 20919;
	private static final int MALE_LIZARDMAN_SCOUT = 20920;
	private static final int MALE_LIZARDMAN_GUARD = 20921;
	private static final int GIANT_ARANE = 20925;
	// Items
	private static final ItemHolder LIZ_NECKLACE_A = new ItemHolder(7178, 100);
	private static final ItemHolder LIZ_NECKLACE_B = new ItemHolder(7179, 100);
	private static final ItemHolder LIZ_PERFUME = new ItemHolder(7180, 30);
	private static final ItemHolder LIZ_GEM = new ItemHolder(7181, 30);
	// Rewards
	private static final ItemHolder GREEN_HIGH_LURE = new ItemHolder(6521, 60);
	private static final ItemHolder BABYDUCK_ROD = new ItemHolder(6529, 1);
	private static final ItemHolder FISHING_SHOT_NONE = new ItemHolder(6535, 500);
	// Misc
	private static final int MIN_LEVEL = 20;
	
	public Q00039_RedEyedInvaders()
	{
		super(39, "Red-Eyed Invaders");
		addStartNpc(GUARD_BABENCO);
		addTalkId(GUARD_BABENCO, CAPTAIN_BATHIA);
		addKillId(MALE_LIZARDMAN_GUARD, MALE_LIZARDMAN_SCOUT, MALE_LIZARDMAN, GIANT_ARANE);
		registerQuestItems(LIZ_NECKLACE_A.getId(), LIZ_NECKLACE_B.getId(), LIZ_PERFUME.getId(), LIZ_GEM.getId());
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
			case "30334-03.htm":
			{
				if (qs.isCreated())
				{
					qs.startQuest();
					htmltext = event;
				}
				break;
			}
			case "30332-02.html":
			{
				if (qs.isCond(1))
				{
					qs.setCond(2, true);
					htmltext = event;
				}
				break;
			}
			case "30332-05.html":
			{
				if (qs.isCond(3))
				{
					if (hasAllItems(player, true, LIZ_NECKLACE_A, LIZ_NECKLACE_B))
					{
						qs.setCond(4, true);
						takeAllItems(player, LIZ_NECKLACE_A, LIZ_NECKLACE_B);
						htmltext = event;
					}
					else
					{
						htmltext = "30332-06.html";
					}
				}
				break;
			}
			case "30332-09.html":
			{
				if (qs.isCond(5))
				{
					if (hasAllItems(player, true, LIZ_PERFUME, LIZ_GEM))
					{
						rewardItems(player, GREEN_HIGH_LURE);
						rewardItems(player, BABYDUCK_ROD);
						rewardItems(player, FISHING_SHOT_NONE);
						addExpAndSp(player, 62366, 2783);
						qs.exitQuest(false, true);
						htmltext = event;
					}
					else
					{
						htmltext = "30332-10.html";
					}
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
		switch (npc.getId())
		{
			case CAPTAIN_BATHIA:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						htmltext = "30332-01.html";
						break;
					}
					case 2:
					{
						htmltext = "30332-03.html";
						break;
					}
					case 3:
					{
						htmltext = "30332-04.html";
						break;
					}
					case 4:
					{
						htmltext = "30332-07.html";
						break;
					}
					case 5:
					{
						htmltext = "30332-08.html";
						break;
					}
				}
				break;
			}
			case GUARD_BABENCO:
			{
				if (qs.isCreated())
				{
					htmltext = (talker.getLevel() >= MIN_LEVEL) ? "30334-01.htm" : "30334-02.htm";
				}
				else if (qs.isStarted() && qs.isCond(1))
				{
					htmltext = "30334-04.html";
				}
				else if (qs.isCompleted())
				{
					htmltext = getAlreadyCompletedMsg(talker);
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		switch (npc.getId())
		{
			case MALE_LIZARDMAN:
			{
				final QuestState qs = getRandomPartyMemberState(killer, 2, 3, npc);
				if ((qs != null) && giveItemRandomly(qs.getPlayer(), npc, LIZ_NECKLACE_A.getId(), 1, LIZ_NECKLACE_A.getCount(), 0.5, true) && hasItem(qs.getPlayer(), LIZ_NECKLACE_B))
				{
					qs.setCond(3);
				}
				break;
			}
			case MALE_LIZARDMAN_SCOUT:
			{
				if (getRandomBoolean())
				{
					final QuestState qs = getRandomPartyMemberState(killer, 2, 3, npc);
					if ((qs != null) && giveItemRandomly(qs.getPlayer(), npc, LIZ_NECKLACE_A.getId(), 1, LIZ_NECKLACE_A.getCount(), 0.5, true) && hasItem(qs.getPlayer(), LIZ_NECKLACE_B))
					{
						qs.setCond(3);
					}
				}
				else
				{
					final QuestState qs = getRandomPartyMemberState(killer, 4, 3, npc);
					if ((qs != null) && giveItemRandomly(qs.getPlayer(), npc, LIZ_PERFUME.getId(), 1, LIZ_PERFUME.getCount(), 0.25, true) && hasItem(qs.getPlayer(), LIZ_GEM))
					{
						qs.setCond(5);
					}
				}
				break;
			}
			case MALE_LIZARDMAN_GUARD:
			{
				if (getRandomBoolean())
				{
					final QuestState qs = getRandomPartyMemberState(killer, 2, 3, npc);
					if ((qs != null) && giveItemRandomly(qs.getPlayer(), npc, LIZ_NECKLACE_B.getId(), 1, LIZ_NECKLACE_B.getCount(), 0.5, true) && hasItem(qs.getPlayer(), LIZ_NECKLACE_A))
					{
						qs.setCond(3);
					}
				}
				else
				{
					final QuestState qs = getRandomPartyMemberState(killer, 4, 3, npc);
					if ((qs != null) && giveItemRandomly(qs.getPlayer(), npc, LIZ_PERFUME.getId(), 1, LIZ_PERFUME.getCount(), 0.3, true) && hasItem(qs.getPlayer(), LIZ_GEM))
					{
						qs.setCond(5);
					}
				}
				break;
			}
			case GIANT_ARANE:
			{
				final QuestState qs = getRandomPartyMemberState(killer, 4, 3, npc);
				if ((qs != null) && giveItemRandomly(qs.getPlayer(), npc, LIZ_GEM.getId(), 1, LIZ_GEM.getCount(), 0.3, true) && hasItem(qs.getPlayer(), LIZ_PERFUME))
				{
					qs.setCond(5);
				}
				break;
			}
		}
	}
}