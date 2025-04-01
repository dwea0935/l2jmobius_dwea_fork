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
package quests.Q00933_TombRaiders;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.actor.Creature; // Imports belov needed for onEnterZone
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.quest.NpcLogListHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.QuestType;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.model.zone.type.ScriptZone;
import org.l2jmobius.gameserver.network.NpcStringId;

/**
 * Tomb Raiders (933)
 * @URL https://l2wiki.com/Tomb_Raiders
 * @author CostyKiller, Neith
 */
public class Q00933_TombRaiders extends Quest
{
	// NPCs
	private static final int SEARCH_TEAM_TELEPORTER = 34552;
	private static final int LEOPARD = 32594;
	// Monsters
	private static final int TOMB_GUARDIAN = 24580;
	private static final int TOMB_RAIDER = 24581;
	private static final int TOMB_WATCHER = 24584;
	private static final int TOMB_SOULTAKER = 24583;
	private static final int TOMB_PATROL = 24582;
	// Item
	private static final int BENUSTA_REWARD_BOX = 81151;
	// Misc
	private static final String KILL_COUNT_VAR = "KillCount";
	// Zone
	private static final ScriptZone QUEST_ZONE = ZoneManager.getInstance().getZoneById(93300, ScriptZone.class); // Session Zone - Imperial Tomb
	
	public Q00933_TombRaiders()
	{
		super(933);
		addEnterZoneId(QUEST_ZONE.getId());
		addStartNpc(SEARCH_TEAM_TELEPORTER);
		addTalkId(SEARCH_TEAM_TELEPORTER, LEOPARD);
		addKillId(TOMB_GUARDIAN, TOMB_RAIDER, TOMB_WATCHER, TOMB_SOULTAKER, TOMB_PATROL);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		switch (event)
		{
			case "34552-02.htm":
			{
				qs.startQuest();
				htmltext = event;
				
				break;
			}
			case "32594-04.htm":
			{
				if (qs.isCond(2))
				{
					addExpAndSp(player, 1035012697, 931511);
					giveItems(player, BENUSTA_REWARD_BOX, 1);
					qs.exitQuest(QuestType.DAILY, true);
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
		final QuestState qs = getQuestState(player, true);
		switch (qs.getState())
		{
			case State.CREATED:
			{
				if (npc.getId() == SEARCH_TEAM_TELEPORTER)
				{
					htmltext = "34552-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				if (npc.getId() == LEOPARD)
				{
					if (qs.isCond(2))
					{
						htmltext = "32594-03.htm";
					}
					else
					{
						htmltext = "32594-06.htm";
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				if (npc.getId() == SEARCH_TEAM_TELEPORTER)
				{
					if (qs.isNowAvailable())
					{
						qs.setState(State.CREATED);
						htmltext = "34552-01.htm";
					}
					else
					{
						htmltext = getAlreadyCompletedMsg(player, QuestType.DAILY);
					}
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onEnterZone(Creature creature, ZoneType zone)
	{
		if (creature.isPlayer())
		{
			final QuestState qs = getQuestState(creature.asPlayer(), true);
			if (qs != null)
			{
				if (qs.isCreated())
				{
					qs.startQuest();
				}
				else if (qs.isNowAvailable() && qs.isCond(0))
				{
					qs.setState(State.CREATED);
					qs.startQuest();
				}
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Party party = killer.getParty();
		if (party != null)
		{
			for (Player member : party.getMembers())
			{
				final QuestState qs = getQuestState(member, false);
				if (qs != null)
				{
					processKill(npc, member);
				}
			}
		}
		else
		{
			processKill(npc, killer);
		}
	}
	
	private void processKill(Npc npc, Player killer)
	{
		final QuestState qs = getRandomPartyMemberState(killer, 1, 3, npc);
		if (qs != null)
		{
			final Player player = qs.getPlayer();
			int killCount = qs.getInt(KILL_COUNT_VAR);
			if (killCount < 150)
			{
				qs.set(KILL_COUNT_VAR, killCount + 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
			if (killCount >= 150)
			{
				qs.setCond(2, true);
			}
			sendNpcLogList(player);
		}
	}
	
	@Override
	public Set<NpcLogListHolder> getNpcLogList(Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(1))
		{
			final Set<NpcLogListHolder> holder = new HashSet<>();
			holder.add(new NpcLogListHolder(NpcStringId.DEFEAT_MONSTERS_IN_THE_IMPERIAL_TOMB.getId(), true, qs.getInt(KILL_COUNT_VAR)));
			return holder;
		}
		return super.getNpcLogList(player);
	}
}
