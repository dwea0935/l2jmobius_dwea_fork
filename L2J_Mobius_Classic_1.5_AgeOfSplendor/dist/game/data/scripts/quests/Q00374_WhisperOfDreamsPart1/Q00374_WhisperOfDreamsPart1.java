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
package quests.Q00374_WhisperOfDreamsPart1;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;

/**
 * Whisper Of Dreams Part1 (374)
 * @author Stayway
 */
public class Q00374_WhisperOfDreamsPart1 extends Quest
{
	// NPCs
	private static final int VANUTU = 30938;
	private static final int GALMAN = 31044;
	private static final int CAVE_BEAST = 20620;
	private static final int DEATH_WAVE = 20621;
	// Items
	private static final ItemHolder CAVE_BEAST_TOOTH = new ItemHolder(5884, 360);
	private static final ItemHolder DEATH_WAVE_LIGHT = new ItemHolder(5885, 360);
	private static final ItemHolder SEALED_MYSTERIOUS_STONE = new ItemHolder(5886, 1);
	private static final int MYSTERIOUS_STONE = 5887;
	// Rewards
	private static final int SCROLL_PART_EA = 49475;
	private static final int REFINED_SCROLL_PART_EA = 49478;
	private static final int ENCHANT_ARMOR_B = 948;
	private static final int IMPROVED_ENCHANT_ARMOR_B = 29743;
	// Misc
	private static final int MIN_LEVEL = 56;
	private static final int MAX_LEVEL = 66;
	
	public Q00374_WhisperOfDreamsPart1()
	{
		super(374);
		addStartNpc(VANUTU);
		addTalkId(VANUTU, GALMAN);
		addKillId(CAVE_BEAST, DEATH_WAVE);
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "30938-02.html");
		registerQuestItems(SEALED_MYSTERIOUS_STONE.getId());
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		String htmltext = null;
		if (qs != null)
		{
			switch (event)
			{
				case "30938-01.htm":
				{
					qs.startQuest();
					htmltext = event;
					break;
				}
				case "30938-06.html":
				{
					if (qs.isCond(2))
					{
						qs.setCond(3, true);
						htmltext = event;
					}
					break;
				}
				case "reward1":
				{
					if (hasAllItems(player, true, DEATH_WAVE_LIGHT, CAVE_BEAST_TOOTH))
					{
						if (qs.isCond(2))
						{
							giveItems(player, SCROLL_PART_EA, 1);
							takeAllItems(player, DEATH_WAVE_LIGHT, CAVE_BEAST_TOOTH);
							giveAdena(player, 9000, true);
							htmltext = "30938-05.html";
						}
						else if (qs.isCond(4))
						{
							giveItems(player, SCROLL_PART_EA, 1);
							takeAllItems(player, DEATH_WAVE_LIGHT, CAVE_BEAST_TOOTH);
							giveAdena(player, 9000, true);
							htmltext = "30938-08.html";
						}
					}
					break;
				}
				case "reward2":
				{
					if (hasAllItems(player, true, DEATH_WAVE_LIGHT, CAVE_BEAST_TOOTH))
					{
						if (qs.isCond(2))
						{
							giveItems(player, REFINED_SCROLL_PART_EA, 1);
							takeAllItems(player, DEATH_WAVE_LIGHT, CAVE_BEAST_TOOTH);
							giveAdena(player, 9000, true);
							htmltext = "30938-05.html";
						}
						else if (qs.isCond(4))
						{
							giveItems(player, REFINED_SCROLL_PART_EA, 1);
							takeAllItems(player, DEATH_WAVE_LIGHT, CAVE_BEAST_TOOTH);
							giveAdena(player, 9000, true);
							htmltext = "30938-08.html";
						}
					}
					break;
				}
				case "reward3":
				{
					if (hasAllItems(player, true, DEATH_WAVE_LIGHT, CAVE_BEAST_TOOTH))
					{
						if (qs.isCond(2))
						{
							giveItems(player, ENCHANT_ARMOR_B, 1);
							takeAllItems(player, DEATH_WAVE_LIGHT, CAVE_BEAST_TOOTH);
							giveAdena(player, 9000, true);
							htmltext = "30938-05.html";
						}
						else if (qs.isCond(4))
						{
							giveItems(player, ENCHANT_ARMOR_B, 1);
							takeAllItems(player, DEATH_WAVE_LIGHT, CAVE_BEAST_TOOTH);
							giveAdena(player, 9000, true);
							htmltext = "30938-08.html";
						}
					}
					break;
				}
				case "reward4":
				{
					if (hasAllItems(player, true, DEATH_WAVE_LIGHT, CAVE_BEAST_TOOTH))
					{
						if (qs.isCond(2))
						{
							giveItems(player, IMPROVED_ENCHANT_ARMOR_B, 1);
							takeAllItems(player, DEATH_WAVE_LIGHT, CAVE_BEAST_TOOTH);
							giveAdena(player, 9000, true);
							htmltext = "30938-05.html";
						}
						else if (qs.isCond(4))
						{
							giveItems(player, IMPROVED_ENCHANT_ARMOR_B, 1);
							takeAllItems(player, DEATH_WAVE_LIGHT, CAVE_BEAST_TOOTH);
							giveAdena(player, 9000, true);
							htmltext = "30938-08.html";
						}
					}
					break;
				}
				case "31044-01.html":
				{
					if (qs.isCond(4))
					{
						giveItems(player, MYSTERIOUS_STONE, 1);
						takeAllItems(player, SEALED_MYSTERIOUS_STONE);
						qs.exitQuest(true, true);
						htmltext = event;
					}
					break;
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (npc.getId())
		{
			case VANUTU:
			{
				if (qs.isCompleted())
				{
					htmltext = getAlreadyCompletedMsg(player);
				}
				else if (qs.isCreated())
				{
					htmltext = "30938.htm";
				}
				else if (qs.isStarted())
				{
					switch (qs.getCond())
					{
						case 1:
						{
							htmltext = "30938-03.html";
							break;
						}
						case 2:
						{
							htmltext = "30938-04.html";
							break;
						}
						case 3:
						{
							htmltext = "30938-07.html";
							break;
						}
						case 4:
						{
							htmltext = "30938-08.html";
							break;
						}
					}
				}
				break;
			}
			case GALMAN:
			{
				if (qs.isCond(4))
				{
					htmltext = "31044.html";
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getRandomPartyMemberState(killer, -1, 3, npc);
		if (qs != null)
		{
			switch (npc.getId())
			{
				case CAVE_BEAST:
				{
					if (qs.getCond() < 4)
					{
						giveItemRandomly(qs.getPlayer(), npc, CAVE_BEAST_TOOTH.getId(), 1, CAVE_BEAST_TOOTH.getCount(), 0.9, true);
						if (qs.isCond(3))
						{
							giveItemRandomly(qs.getPlayer(), npc, SEALED_MYSTERIOUS_STONE.getId(), 1, SEALED_MYSTERIOUS_STONE.getCount(), 0.2, true);
						}
					}
					break;
				}
				case DEATH_WAVE:
				{
					if (qs.getCond() < 4)
					{
						giveItemRandomly(qs.getPlayer(), npc, DEATH_WAVE_LIGHT.getId(), 1, DEATH_WAVE_LIGHT.getCount(), 0.9, true);
						if (qs.isCond(3))
						{
							giveItemRandomly(qs.getPlayer(), npc, SEALED_MYSTERIOUS_STONE.getId(), 1, SEALED_MYSTERIOUS_STONE.getCount(), 0.2, true);
						}
					}
					break;
				}
			}
			if (qs.isCond(1) && (hasAllItems(qs.getPlayer(), true, DEATH_WAVE_LIGHT, CAVE_BEAST_TOOTH)))
			{
				qs.setCond(2, true);
			}
			if (qs.isCond(3) && (hasAllItems(qs.getPlayer(), true, SEALED_MYSTERIOUS_STONE)))
			{
				qs.setCond(4, true);
			}
		}
	}
}
