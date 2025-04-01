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
package quests.Q10892_RevengeOneStepAtATime;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Faction;
import org.l2jmobius.gameserver.model.quest.NpcLogListHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.util.ArrayUtil;

/**
 * Revenge, One Step at a Time (10892)
 * @URL https://l2wiki.com/Revenge,_One_Step_at_a_Time
 * @author Dmitri
 */
public class Q10892_RevengeOneStepAtATime extends Quest
{
	// NPCs
	private static final int LEONA = 34425; // Blackbird Clan Lord: Leona Blackbird
	private static final int[] MONSTERS =
	{
		24144, // Death Rogue
		24145, // Death Shooter
		24146, // Death Warrior
		24147, // Death Sorcerer
		24148, // Death Pondus
		24149, // Devil Nightmare
		24150, // Devil Warrior
		24151, // Devil Guardian
		24152, // Devil Sinist
		24153, // Devil Varos
		24154, // Demonic Wizard
		24155, // Demonic Warrior
		24156, // Demonic Archer
		24157, // Demonic Keras
		24158, // Demonic Weiss
		24159, // Atelia Yuyurina
		24160 // Atelia Popobena
	};
	private static final int[] GUARDIAN =
	{
		24161, // Harke
		24162, // Ergalion
		24163 // Spira
	};
	// Reward
	private static final int RUNE_STONE = 39738;
	private static final int ELCYUM_CRYSTAL = 36514;
	// Misc
	private static final int MIN_LEVEL = 103;
	
	public Q10892_RevengeOneStepAtATime()
	{
		super(10892);
		addStartNpc(LEONA);
		addTalkId(LEONA);
		addKillId(MONSTERS);
		addKillId(GUARDIAN);
		addCondMinLevel(MIN_LEVEL, "34425-00.htm");
		addFactionLevel(Faction.BLACKBIRD_CLAN, 10, "34425-00.htm");
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "34425-02.htm":
			case "34425-04.htm":
			{
				htmltext = event;
				break;
			}
			case "34425-05.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "34425-08.html":
			{
				giveItems(player, ELCYUM_CRYSTAL, 3);
				giveItems(player, RUNE_STONE, 1);
				addExpAndSp(player, 543832495200L, 543832200);
				qs.exitQuest(false, true);
				htmltext = event;
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
				if (npc.getId() == LEONA)
				{
					htmltext = "34425-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case LEONA:
					{
						if (qs.isCond(1))
						{
							htmltext = "34425-06.html";
						}
						else
						{
							htmltext = "34425-07.html";
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
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && qs.isCond(1))
		{
			int killedMonsters = qs.getInt("killed_" + MONSTERS[0]);
			int killedGuardian = qs.getInt("killed_" + GUARDIAN[0]);
			if (ArrayUtil.contains(MONSTERS, npc.getId()))
			{
				if (killedMonsters < 1000)
				{
					killedMonsters++;
					qs.set("killed_" + MONSTERS[0], killedMonsters);
					playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
			}
			else if (ArrayUtil.contains(GUARDIAN, npc.getId()))
			{
				if (killedGuardian < 1)
				{
					killedGuardian++;
					qs.set("killed_" + GUARDIAN[0], killedGuardian);
					playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
			}
			
			if ((killedMonsters == 1000) && (killedGuardian == 1))
			{
				qs.setCond(2, true);
			}
		}
	}
	
	@Override
	public Set<NpcLogListHolder> getNpcLogList(Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isStarted() && qs.isCond(1))
		{
			final Set<NpcLogListHolder> npcLogList = new HashSet<>(2);
			npcLogList.add(new NpcLogListHolder(MONSTERS[0], false, qs.getInt("killed_" + MONSTERS[0])));
			npcLogList.add(new NpcLogListHolder(GUARDIAN[0], false, qs.getInt("killed_" + GUARDIAN[0])));
			return npcLogList;
		}
		return super.getNpcLogList(player);
	}
}
