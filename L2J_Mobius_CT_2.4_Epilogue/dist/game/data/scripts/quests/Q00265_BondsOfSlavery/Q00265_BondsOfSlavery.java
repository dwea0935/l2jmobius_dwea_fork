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
package quests.Q00265_BondsOfSlavery;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.managers.QuestManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

import ai.others.NewbieGuide.NewbieGuide;

/**
 * Bonds of Slavery (265)
 * @author xban1x
 */
public class Q00265_BondsOfSlavery extends Quest
{
	// NPC
	private static final int KRISTIN = 30357;
	// Monsters
	private static final Map<Integer, Integer> MONSTERS = new HashMap<>();
	static
	{
		MONSTERS.put(20004, 5); // Imp
		MONSTERS.put(20005, 6); // Imp Elder
	}
	// Items
	private static final int IMP_SHACKLES = 1368;
	private static final ItemHolder SPIRITSHOTS_NO_GRADE_FOR_ROOKIES = new ItemHolder(5790, 3000);
	private static final ItemHolder SOULSHOTS_NO_GRADE_FOR_ROOKIES = new ItemHolder(5789, 6000);
	// Misc
	private static final int MIN_LEVEL = 6;
	private static final int GUIDE_MISSION = 41;
	
	public Q00265_BondsOfSlavery()
	{
		super(265, "Chains of Slavery");
		addStartNpc(KRISTIN);
		addTalkId(KRISTIN);
		addKillId(MONSTERS.keySet());
		registerQuestItems(IMP_SHACKLES);
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
			case "30357-04.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "30357-07.html":
			{
				qs.exitQuest(true, true);
				htmltext = event;
				break;
			}
			case "30357-08.html":
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
		if ((qs != null) && (getRandom(10) < MONSTERS.get(npc.getId())))
		{
			giveItems(killer, IMP_SHACKLES, 1);
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
				htmltext = (player.getRace() == Race.DARK_ELF) ? (player.getLevel() >= MIN_LEVEL) ? "30357-03.htm" : "30357-02.html" : "30357-01.html";
				break;
			}
			case State.STARTED:
			{
				if (hasQuestItems(player, IMP_SHACKLES))
				{
					final long shackles = getQuestItemsCount(player, IMP_SHACKLES);
					giveAdena(player, (shackles * 12) + (shackles >= 10 ? 500 : 0), true);
					takeItems(player, IMP_SHACKLES, -1);
					
					if ((player.getLevel() < 25) && (getOneTimeQuestFlag(player, 57) == 0))
					{
						if (player.isMageClass())
						{
							giveItems(player, SPIRITSHOTS_NO_GRADE_FOR_ROOKIES);
							playSound(player, "tutorial_voice_027");
						}
						else
						{
							giveItems(player, SOULSHOTS_NO_GRADE_FOR_ROOKIES);
							playSound(player, "tutorial_voice_026");
						}
						
						setOneTimeQuestFlag(player, 57, 1);
					}
					
					// Newbie Guide.
					final Quest newbieGuide = QuestManager.getInstance().getQuest(NewbieGuide.class.getSimpleName());
					if (newbieGuide != null)
					{
						final QuestState newbieGuideQs = newbieGuide.getQuestState(player, true);
						if (!haveNRMemo(newbieGuideQs, GUIDE_MISSION))
						{
							setNRMemo(newbieGuideQs, GUIDE_MISSION);
							setNRMemoState(newbieGuideQs, GUIDE_MISSION, 1000);
							showOnScreenMsg(player, "Acquisition of Soulshot for beginners complete. \\n Go find the Newbie Guide.", 2, 5000);
						}
						else if (((getNRMemoState(newbieGuideQs, GUIDE_MISSION) % 10000) / 1000) != 1)
						{
							setNRMemo(newbieGuideQs, GUIDE_MISSION);
							setNRMemoState(newbieGuideQs, GUIDE_MISSION, getNRMemoState(newbieGuideQs, GUIDE_MISSION) + 1000);
							showOnScreenMsg(player, "Acquisition of Soulshot for beginners complete. \\n Go find the Newbie Guide.", 2, 5000);
						}
					}
					
					htmltext = "30357-06.html";
				}
				else
				{
					htmltext = "30357-05.html";
				}
				break;
			}
		}
		return htmltext;
	}
}
