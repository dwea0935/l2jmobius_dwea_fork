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
package quests.Q10964_SecretGarden;

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
 * Secret Garden (10964)
 * @author RobikBobik
 * @Note: Based on NA server September 2019
 */
public class Q10964_SecretGarden extends Quest
{
	// NPC
	private static final int CAPTAIN_BATHIS = 30332;
	private static final int RAYMOND = 30289;
	// Monsters
	private static final int HARPY = 20145;
	private static final int MEDUSA = 20158;
	private static final int WYRM = 20176;
	private static final int TURAK_BUGBEAR = 20248;
	private static final int TURAK_BUGBEAR_WARRIOR = 20249;
	// Misc
	private static final String KILL_COUNT_VAR = "KillCount";
	private static final int MAX_LEVEL = 34;
	private static final int MIN_LEVEL = 30;
	
	public Q10964_SecretGarden()
	{
		super(10964);
		addStartNpc(CAPTAIN_BATHIS);
		addTalkId(CAPTAIN_BATHIS, RAYMOND);
		addKillId(HARPY, MEDUSA, WYRM, TURAK_BUGBEAR, TURAK_BUGBEAR_WARRIOR);
		setQuestNameNpcStringId(NpcStringId.LV_30_35_SECRET_GARDEN);
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
			case "30332-01.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "30289-01.html":
			{
				qs.setCond(2, true);
				htmltext = event;
				break;
			}
			case "30289-02.html":
			{
				htmltext = event;
				break;
			}
			case "30289-03.html":
			{
				if (qs.isStarted())
				{
					player.sendPacket(new ExShowScreenMessage(NpcStringId.YOU_CAN_START_ADVENTURER_S_JOURNEY_II_PRESS_THE_YELLOW_QUESTION_MARK_ON_THE_BOTTOM_RIGHT_TO_SEE_DETAILS_OF_THE_MISSION, 2, 5000));
					addExpAndSp(player, 2500000, 75000);
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
		if ((qs != null) && qs.isCond(2))
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
				qs.setCond(3, true);
				qs.unset(KILL_COUNT_VAR);
				killer.sendPacket(new ExShowScreenMessage(NpcStringId.YOU_DEFEATED_ALL_MONSTERS_IN_THE_GORGON_FLOWER_GARDEN_TELEPORT_TO_THE_TOWN_OF_GLUDIO_TO_MEET_HIGH_PRIEST_RAYMOND, 2, 5000));
			}
		}
	}
	
	@Override
	public Set<NpcLogListHolder> getNpcLogList(Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(2))
		{
			final Set<NpcLogListHolder> holder = new HashSet<>();
			holder.add(new NpcLogListHolder(NpcStringId.CLEAR_GORGON_FLOWER_GARDEN.getId(), true, qs.getInt(KILL_COUNT_VAR)));
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
			htmltext = "30332.htm";
		}
		else if (qs.isStarted())
		{
			switch (npc.getId())
			{
				case CAPTAIN_BATHIS:
				{
					if (qs.isCond(1))
					{
						htmltext = "30332-01.htm";
					}
					break;
				}
				case RAYMOND:
				{
					if (qs.isCond(1))
					{
						htmltext = "30289.html";
					}
					else if (qs.isCond(3))
					{
						htmltext = "30289-02.html";
					}
					break;
				}
			}
		}
		else if (qs.isCompleted())
		{
			if (npc.getId() == CAPTAIN_BATHIS)
			{
				htmltext = getAlreadyCompletedMsg(player);
			}
		}
		return htmltext;
	}
}