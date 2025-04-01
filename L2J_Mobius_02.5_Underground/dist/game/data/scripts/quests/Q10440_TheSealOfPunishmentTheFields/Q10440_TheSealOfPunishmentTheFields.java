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
package quests.Q10440_TheSealOfPunishmentTheFields;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.quest.NpcLogListHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.network.NpcStringId;

/**
 * The Seal of Punishment: The Fields (10440)
 * @author Stayway
 */
public class Q10440_TheSealOfPunishmentTheFields extends Quest
{
	// NPCs
	private static final int HELVETICA = 32641;
	private static final int ATHENIA = 32643;
	// Monsters
	private static final int MUCROKIAN_FANATIC = 22650;
	private static final int MUCROKIAN_ASCETIC = 22651;
	private static final int MUCROKIAN_SAVIOR = 22652;
	private static final int MUCROKIAN_PROPHET = 22653;
	private static final int CONTAMINATED_MUCROKIAN = 22654;
	private static final int AWAKENED_MUCROKIAN = 22655;
	// Misc
	private static final String KILL_COUNT_VAR = "KillCounts";
	private static final int MIN_LEVEL = 81;
	private static final int MAX_LEVEL = 84;
	
	public Q10440_TheSealOfPunishmentTheFields()
	{
		super(10440);
		addStartNpc(HELVETICA, ATHENIA);
		addTalkId(HELVETICA, ATHENIA);
		addKillId(MUCROKIAN_FANATIC, MUCROKIAN_ASCETIC, MUCROKIAN_SAVIOR, MUCROKIAN_PROPHET, CONTAMINATED_MUCROKIAN, AWAKENED_MUCROKIAN);
		addCondMaxLevel(MAX_LEVEL, "noLevel.html");
		addCondMinLevel(MIN_LEVEL, "noLevel.html");
		addCondNotRace(Race.ERTHEIA, "noErtheia.html");
		addCondInCategory(CategoryType.MAGE_GROUP, "noLevel.html");
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return getNoQuestMsg(player);
		}
		String htmltext = event;
		switch (event)
		{
			case "32641-02.htm":
			case "32641-03.htm":
			case "32643-02.htm":
			case "32643-03.htm":
			{
				htmltext = event;
				break;
			}
			case "32641-04.htm":
			{
				qs.startQuest();
				qs.setMemoState(1);
				qs.set(Integer.toString(MUCROKIAN_FANATIC), 0);
				htmltext = event;
				break;
			}
			case "32643-04.htm":
			{
				qs.startQuest();
				qs.setMemoState(2);
				qs.set(Integer.toString(MUCROKIAN_FANATIC), 0);
				htmltext = event;
				break;
			}
			case "reward_9546":
			case "reward_9547":
			case "reward_9548":
			case "reward_9549":
			case "reward_9550":
			case "reward_9551":
			{
				final int stoneId = Integer.parseInt(event.replaceAll("reward_", ""));
				giveItems(player, stoneId, 15);
				giveStoryQuestReward(player, 60);
				final int count = qs.getInt(KILL_COUNT_VAR);
				if ((count >= 50) && (count < 100))
				{
					addExpAndSp(player, 28240800, 6777);
				}
				else if ((count >= 100) && (count < 200))
				{
					addExpAndSp(player, 56481600, 13554);
				}
				else if ((count >= 200) && (count < 300))
				{
					addExpAndSp(player, 84722400, 20331);
				}
				else if ((count >= 300) && (count < 400))
				{
					addExpAndSp(player, 112963200, 27108);
				}
				else if ((count >= 400) && (count < 500))
				{
					addExpAndSp(player, 141204000, 33835);
				}
				else if ((count >= 500) && (count < 600))
				{
					addExpAndSp(player, 169444800, 40662);
				}
				else if ((count >= 600) && (count < 700))
				{
					addExpAndSp(player, 197685600, 47439);
				}
				else if ((count >= 700) && (count < 800))
				{
					addExpAndSp(player, 225926400, 54216);
				}
				else if ((count >= 800) && (count < 900))
				{
					addExpAndSp(player, 254167200, 60993);
				}
				else if (count >= 900)
				{
					addExpAndSp(player, 282408000, 67770);
				}
				if ((qs.isCond(2)) && (qs.isMemoState(1)))
				{
					htmltext = "32641-07.html";
				}
				else if ((qs.isCond(3)) && (qs.isMemoState(2)))
				{
					htmltext = "32643-07.html";
				}
				qs.exitQuest(false, true);
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
		switch (npc.getId())
		{
			case HELVETICA:
			{
				if (qs.isCreated())
				{
					htmltext = "32641-01.htm";
				}
				else if (qs.isCond(1))
				{
					htmltext = "32641-05.html";
				}
				else if (qs.isCond(2))
				{
					htmltext = "32641-06.html";
				}
				else if (qs.isCompleted())
				{
					htmltext = getAlreadyCompletedMsg(player);
				}
				break;
			}
			case ATHENIA:
			{
				if (qs.isCreated())
				{
					htmltext = "32643-01.htm";
				}
				else if (qs.isCond(1))
				{
					htmltext = "32643-05.html";
				}
				else if (qs.isCond(3))
				{
					htmltext = "32643-06.html";
				}
				else if (qs.isCompleted())
				{
					htmltext = getAlreadyCompletedMsg(player);
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && (qs.getCond() > 0))
		{
			final int count = qs.getInt(KILL_COUNT_VAR) + 1;
			qs.set(KILL_COUNT_VAR, count);
			if ((count >= 50) && (qs.isMemoState(1)))
			{
				qs.setCond(2, true);
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
			else if ((count >= 50) && (qs.isMemoState(2)))
			{
				qs.setCond(3, true);
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
			else
			{
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
		}
	}
	
	@Override
	public Set<NpcLogListHolder> getNpcLogList(Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && (qs.getCond() > 0))
		{
			final int killCounts = qs.getInt(KILL_COUNT_VAR);
			if (killCounts > 0)
			{
				final Set<NpcLogListHolder> holder = new HashSet<>();
				holder.add(new NpcLogListHolder(NpcStringId.ELIMINATING_THE_MUCROKIANS, killCounts));
				return holder;
			}
		}
		return super.getNpcLogList(player);
	}
}