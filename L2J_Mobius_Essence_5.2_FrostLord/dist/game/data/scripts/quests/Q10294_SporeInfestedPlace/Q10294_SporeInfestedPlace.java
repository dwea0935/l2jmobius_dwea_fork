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
package quests.Q10294_SporeInfestedPlace;

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

/**
 * @author QuangNguyen, Mobius
 */
public class Q10294_SporeInfestedPlace extends Quest
{
	// NPC
	private static final int MAXIMILLIAN = 30120;
	private static final int ORVEN = 30857;
	// Monsters
	private static final int GIANT_FUNGUS = 20555;
	private static final int GIANT_MONSTER_EYE = 20556;
	private static final int DIRE_WYRM = 20557;
	private static final int ROTTING_TREE = 20558;
	private static final int ROTTING_GOLEM = 20559;
	private static final int SPIDER_TRISALIM = 20560;
	private static final int TRISALIM_TARANTULA = 20561;
	private static final int SPORE_ZOMBIE = 20562;
	private static final int EARTH_GUARDIAN_WYRM = 20176;
	// Items
	private static final ItemHolder SOE_SEA_OF_SPORES = new ItemHolder(95590, 1);
	private static final ItemHolder SOE_HIGH_PRIEST_OVEN = new ItemHolder(91768, 1);
	private static final ItemHolder SPIRIT_ORE = new ItemHolder(3031, 500);
	private static final ItemHolder SOULSHOT_TICKET = new ItemHolder(90907, 50);
	private static final ItemHolder HP_POTS = new ItemHolder(91912, 100);
	private static final ItemHolder SAYHA_GUST = new ItemHolder(91776, 9);
	// Misc
	private static final String KILL_COUNT_VAR = "KillCount";
	private static final int MIN_LEVEL = 40;
	private static final int MAX_LEVEL = 44;
	
	public Q10294_SporeInfestedPlace()
	{
		super(10294);
		addStartNpc(MAXIMILLIAN);
		addTalkId(MAXIMILLIAN, ORVEN);
		addKillId(GIANT_FUNGUS, GIANT_MONSTER_EYE, DIRE_WYRM, ROTTING_TREE, ROTTING_GOLEM, SPIDER_TRISALIM, TRISALIM_TARANTULA, SPORE_ZOMBIE, EARTH_GUARDIAN_WYRM);
		addCondMinLevel(MIN_LEVEL, "no_lvl.html");
		addCondMaxLevel(MAX_LEVEL, "no_lvl.html");
		registerQuestItems(SOE_SEA_OF_SPORES.getId(), SOE_HIGH_PRIEST_OVEN.getId());
		setQuestNameNpcStringId(NpcStringId.LV_40_44_SPORE_INFESTED_PLACE);
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
			case "30120.htm":
			case "30120-01.htm":
			case "30120-02.htm":
			case "30857.html":
			case "30857-01.html":
			case "30857-03.html":
			case "30857-05.html":
			{
				htmltext = event;
				break;
			}
			case "30120-03.htm":
			{
				qs.startQuest();
				final ListenersContainer container = player;
				container.addListener(new ConsumerEventListener(player, EventType.ON_PLAYER_LEVEL_CHANGED, (OnPlayerLevelChanged levelChange) -> onLevelUp(levelChange, qs), this));
				htmltext = event;
				break;
			}
			case "TELEPORT_TO_ORVEN":
			{
				player.teleToLocation(147452, 22638, -1984);
				break;
			}
			case "30857-02.html":
			{
				qs.setCond(2);
				giveItems(player, SOE_SEA_OF_SPORES);
				htmltext = event;
				break;
			}
			case "reward":
			{
				if (qs.isCond(3))
				{
					addExpAndSp(player, 10000000, 270000);
					giveItems(player, SPIRIT_ORE);
					giveItems(player, SOULSHOT_TICKET);
					giveItems(player, HP_POTS);
					giveItems(player, SAYHA_GUST);
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
			htmltext = "30120.htm";
		}
		else if (qs.isStarted())
		{
			switch (npc.getId())
			{
				case MAXIMILLIAN:
				{
					htmltext = "30120-01.html";
					break;
				}
				case ORVEN:
				{
					if (qs.isCond(1))
					{
						htmltext = "30857.html";
					}
					else if (qs.isCond(3) && (player.getLevel() > 43))
					{
						htmltext = "30857-04.html";
					}
					else
					{
						htmltext = "no_enough.html";
					}
					break;
				}
			}
		}
		else if (qs.isCompleted())
		{
			if (npc.getId() == ORVEN)
			{
				htmltext = getAlreadyCompletedMsg(player);
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
			if (killCount <= 200)
			{
				qs.set(KILL_COUNT_VAR, killCount);
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				sendNpcLogList(killer);
			}
			else if (allConditionsMet(killer, qs))
			{
				prepareToFinishQuest(killer, qs);
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
			holder.add(new NpcLogListHolder(NpcStringId.KILL_MONSTERS_IN_THE_SEA_OF_SPORES.getId(), true, qs.getInt(KILL_COUNT_VAR)));
			holder.add(new NpcLogListHolder(NpcStringId.REACH_LV_44, player.getLevel() > 43 ? 1 : 0));
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
		return (qs != null) && qs.isCond(2) && (player.getLevel() > 43) && (qs.getInt(KILL_COUNT_VAR) >= 200);
	}
	
	private void prepareToFinishQuest(Player killer, QuestState qs)
	{
		qs.setCond(3, true);
		giveItems(killer, SOE_HIGH_PRIEST_OVEN);
		qs.unset(KILL_COUNT_VAR);
		killer.removeListenerIf(EventType.ON_PLAYER_LEVEL_CHANGED, listener -> listener.getOwner() == killer);
	}
}
