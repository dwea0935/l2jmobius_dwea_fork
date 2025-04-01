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
package quests.Q10963_ExploringTheAntNest;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.NpcLogListHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * Exploring The Ant Nest (10963)
 * @author RobikBobik
 * @Note: Based on NA server September 2019
 */
public class Q10963_ExploringTheAntNest extends Quest
{
	// NPC
	private static final int RAYMOND = 30289;
	// Monsters
	private static final int ANT_LARVA = 20075;
	private static final int ANT = 20079;
	private static final int ANT_CAPTAIN = 20080;
	private static final int ANT_OVERSEER = 20081;
	private static final int ANT_RECRUIT = 20082;
	private static final int ANT_PATROL = 20084;
	private static final int ANT_GUARD = 20086;
	private static final int ANT_SOLDIER = 20087;
	private static final int ANT_WARRIOR_CAPTAIN = 20088;
	private static final int ANT_NOBLE = 20089;
	private static final int ANT_NOBLE_CAPTAIN = 20090;
	// Misc
	private static final String KILL_COUNT_VAR = "KillCount";
	private static final int MIN_LEVEL = 34;
	private static final int MAX_LEVEL = 37;
	
	public Q10963_ExploringTheAntNest()
	{
		super(10963);
		addStartNpc(RAYMOND);
		addTalkId(RAYMOND);
		addKillId(ANT_LARVA, ANT, ANT_CAPTAIN, ANT_OVERSEER, ANT_RECRUIT, ANT_PATROL, ANT_GUARD, ANT_SOLDIER, ANT_WARRIOR_CAPTAIN, ANT_NOBLE, ANT_NOBLE_CAPTAIN);
		setQuestNameNpcStringId(NpcStringId.LV_34_37_EXPLORING_THE_ANT_NEST);
		addCondMinLevel(MIN_LEVEL, "no_lvl.html");
		addCondMaxLevel(MAX_LEVEL, "no_lvl.html");
	}
	
	@Override
	public boolean checkPartyMember(Player member, Npc npc)
	{
		final QuestState qs = getQuestState(member, false);
		return ((qs != null) && qs.isStarted());
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
			case "30289-01.htm":
			{
				htmltext = event;
				break;
			}
			case "30289-02.htm":
			{
				htmltext = event;
				break;
			}
			case "30289-03.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "30289-05.html":
			{
				if (qs.isStarted())
				{
					addExpAndSp(player, 3000000, 75000);
					qs.exitQuest(false, true);
					htmltext = event;
					break;
				}
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
			final int killCount = qs.getInt(KILL_COUNT_VAR) + 1;
			if (killCount < 500)
			{
				qs.set(KILL_COUNT_VAR, killCount);
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				sendNpcLogList(killer);
			}
			else
			{
				qs.setCond(2, true);
				qs.unset(KILL_COUNT_VAR);
				killer.sendPacket(new ExShowScreenMessage(NpcStringId.MONSTERS_OF_THE_ANT_NEST_ARE_KILLED_NUSE_THE_TELEPORT_TO_GET_TO_HIGH_PRIEST_RAYMOND_IN_GLUDIO, 2, 5000));
			}
		}
	}
	
	@Override
	public Set<NpcLogListHolder> getNpcLogList(Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(1))
		{
			final Set<NpcLogListHolder> holder = new HashSet<>();
			holder.add(new NpcLogListHolder(NpcStringId.DEFEAT_THE_MONSTERS_IN_THE_ANT_NEST.getId(), true, qs.getInt(KILL_COUNT_VAR)));
			return holder;
		}
		return super.getNpcLogList(player);
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		if (qs.isCreated())
		{
			htmltext = "30289.htm";
		}
		else if (qs.isStarted())
		{
			switch (npc.getId())
			{
				case RAYMOND:
				{
					if (qs.isCond(2))
					{
						htmltext = "30289-04.html";
					}
					break;
				}
			}
		}
		else if (qs.isCompleted())
		{
			if (npc.getId() == RAYMOND)
			{
				htmltext = getAlreadyCompletedMsg(player);
			}
		}
		return htmltext;
	}
}