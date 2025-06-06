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
package quests.Q10295_RespectForGraves;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenersContainer;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLevelChanged;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.quest.NpcLogListHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * @author QuangNguyen
 */
public class Q10295_RespectForGraves extends Quest
{
	// NPC
	private static final int ORVEN = 30857;
	// Monsters
	private static final int TAIK_ORC_WATCHMAN = 20666;
	private static final int GRAVE_GUARD = 20668;
	private static final int TAIK_ORC_SUPPLY_OFFICER = 20669;
	private static final int TAIRIM = 20675;
	private static final int TORTURED_UNDEAD = 20678;
	private static final int SPITEFUL_GHOST_OF_RUINS = 20996;
	private static final int SOLDIER_OF_GRIEF = 20997;
	private static final int CRUEL_PUNISHER = 20998;
	private static final int ROVING_SOUL = 20999;
	private static final int SOUL_OF_RUNIS = 21000;
	// Items
	private static final ItemHolder SOE_CEMETERY = new ItemHolder(95591, 1);
	private static final ItemHolder SOE_HIGH_PRIEST_OVEN = new ItemHolder(91768, 1);
	// Reward
	private static final ItemHolder ASOFE = new ItemHolder(92994, 1);
	private static final ItemHolder SAYHA_GUST = new ItemHolder(91776, 9);
	// Misc
	private static final String KILL_COUNT_VAR = "KillCount";
	private static final int MIN_LEVEL = 45;
	private static final int MAX_LEVEL = 52;
	
	public Q10295_RespectForGraves()
	{
		super(10295);
		addStartNpc(ORVEN);
		addTalkId(ORVEN);
		addKillId(TAIK_ORC_WATCHMAN, GRAVE_GUARD, TAIK_ORC_SUPPLY_OFFICER, TAIRIM, TORTURED_UNDEAD, SPITEFUL_GHOST_OF_RUINS, SOLDIER_OF_GRIEF, CRUEL_PUNISHER, ROVING_SOUL, SOUL_OF_RUNIS);
		addCondMinLevel(MIN_LEVEL, "no_lvl.html");
		addCondMaxLevel(MAX_LEVEL, "no_lvl.html");
		registerQuestItems(SOE_CEMETERY.getId(), SOE_HIGH_PRIEST_OVEN.getId());
		setQuestNameNpcStringId(NpcStringId.LV_45_52_RESPECT_FOR_GRAVES);
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
			case "30857.htm":
			case "30857-01.htm":
			case "30857-02.htm":
			case "30857-06.html":
			case "30857-07.html":
			case "30857-08.html":
			{
				htmltext = event;
				break;
			}
			case "30857-03.htm":
			{
				qs.startQuest();
				final ListenersContainer container = player;
				container.addListener(new ConsumerEventListener(player, EventType.ON_PLAYER_LEVEL_CHANGED, (OnPlayerLevelChanged levelChange) -> onLevelUp(levelChange, qs), this));
				giveItems(player, SOE_CEMETERY);
				htmltext = event;
				break;
			}
			case "reward":
			{
				if (qs.isCond(2))
				{
					addExpAndSp(player, 25000000, 675000);
					giveItems(player, ASOFE);
					giveItems(player, SAYHA_GUST);
					showOnScreenMsg(player, NpcStringId.FROM_NOW_TRY_TO_GET_AS_MUCH_QUESTS_AS_YOU_CAN_I_LL_TELL_YOU_WHAT_TO_DO_NEXT, ExShowScreenMessage.TOP_CENTER, 10000);
					htmltext = "30857-05.html";
					qs.exitQuest(false, true);
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
		if (qs.isCreated())
		{
			htmltext = "30857.htm";
		}
		else if (qs.isStarted())
		{
			if (qs.isCond(1))
			{
				htmltext = "no_enough.html";
			}
			else if (qs.isCond(2))
			{
				htmltext = "30857-04.html";
			}
		}
		else if (qs.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
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
			if (killCount <= 300)
			{
				qs.set(KILL_COUNT_VAR, killCount);
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				sendNpcLogList(killer);
			}
			else
			{
				if (allConditionsMet(killer, qs))
				{
					prepareToFinishQuest(killer, qs);
				}
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
			holder.add(new NpcLogListHolder(NpcStringId.KILL_MONSTERS_IN_THE_CEMETERY.getId(), true, qs.getInt(KILL_COUNT_VAR)));
			holder.add(new NpcLogListHolder(NpcStringId.LEVEL_52_ACCOMPLISHED, player.getLevel() > 51 ? 1 : 0));
			return holder;
		}
		return super.getNpcLogList(player);
	}
	
	@Override
	public void onQuestAborted(Player player)
	{
		player.removeListenerIf(EventType.ON_PLAYER_LEVEL_CHANGED, listener -> listener.getOwner() == player);
		super.onQuestAborted(player);
	}
	
	private void onLevelUp(OnPlayerLevelChanged event, QuestState qs)
	{
		final Player player = event.getPlayer();
		sendNpcLogList(player);
		if (allConditionsMet(player, qs))
		{
			prepareToFinishQuest(player, qs);
		}
	}
	
	private boolean allConditionsMet(Player player, QuestState qs)
	{
		return (qs != null) && qs.isCond(1) && (player.getLevel() > 51) && (qs.getInt(KILL_COUNT_VAR) >= 300);
	}
	
	private void prepareToFinishQuest(Player killer, QuestState qs)
	{
		qs.setCond(2, true);
		giveItems(killer, SOE_HIGH_PRIEST_OVEN);
		qs.unset(KILL_COUNT_VAR);
		sendNpcLogList(killer);
		killer.removeListenerIf(EventType.ON_PLAYER_LEVEL_CHANGED, listener -> listener.getOwner() == killer);
	}
}
