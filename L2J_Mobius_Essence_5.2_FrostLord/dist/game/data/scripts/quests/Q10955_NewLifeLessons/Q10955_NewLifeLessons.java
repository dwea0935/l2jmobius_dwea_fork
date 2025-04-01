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
package quests.Q10955_NewLifeLessons;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.quest.NpcLogListHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * @author QuangNguyen
 */
public class Q10955_NewLifeLessons extends Quest
{
	// NPCs
	private static final int KERKIN = 34210;
	// Monsters
	private static final int DIFFLOE = 22325;
	private static final int APIS = 22326;
	private static final int ECHINU = 22327;
	// Items
	private static final ItemHolder SOE_KERKIR = new ItemHolder(95586, 1);
	private static final ItemHolder SOE_NOVICE = new ItemHolder(10650, 10);
	private static final ItemHolder RING_NOVICE = new ItemHolder(49041, 2);
	private static final ItemHolder EARRING_NOVICE = new ItemHolder(49040, 2);
	private static final ItemHolder NECKLACE_NOVICE = new ItemHolder(49039, 1);
	// Location
	private static final Location QUIET_PLAIN = new Location(106025, 53773, -4576);
	// Misc
	private static final int MIN_LEVEL = 2;
	private static final int MAX_LEVEL = 15;
	private static final String KILL_COUNT_VAR = "KillCount";
	
	public Q10955_NewLifeLessons()
	{
		super(10955);
		addStartNpc(KERKIN);
		addTalkId(KERKIN);
		addKillId(DIFFLOE, APIS, ECHINU);
		addCondMinLevel(MIN_LEVEL, "no_lvl.html");
		addCondMaxLevel(MAX_LEVEL, "no_lvl.html");
		addCondRace(Race.SYLPH, "no_race.html");
		registerQuestItems(SOE_KERKIR.getId());
		setQuestNameNpcStringId(NpcStringId.LV_2_15_NEW_LIFE_S_LESSONS);
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
			case "34210-00.html":
			case "34210-01.html":
			case "34210-02.html":
			{
				htmltext = event;
				break;
			}
			case "34210-03.html":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "teleport":
			{
				if (qs.isCond(1))
				{
					player.teleToLocation(QUIET_PLAIN);
				}
				break;
			}
			case "34210-05.html":
			{
				if (qs.isCond(2))
				{
					giveItems(player, SOE_NOVICE);
					giveItems(player, RING_NOVICE);
					giveItems(player, EARRING_NOVICE);
					giveItems(player, NECKLACE_NOVICE);
					addExpAndSp(player, 260000, 6000);
					qs.exitQuest(false, true);
				}
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
		
		// Sylphs.
		if (player.getRace() != Race.SYLPH)
		{
			htmltext = "no_race.html";
		}
		
		switch (qs.getState())
		{
			case State.CREATED:
			{
				htmltext = "34210-01.html";
				break;
			}
			case State.STARTED:
			{
				if (npc.getId() == KERKIN)
				{
					if (qs.isCond(1))
					{
						htmltext = "34210-03a.html";
					}
					else if (qs.isCond(2))
					{
						final int killCount = qs.getInt(KILL_COUNT_VAR);
						if (killCount < 20)
						{
							htmltext = "34210-03.html";
						}
						htmltext = "34210-04.html";
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
			final int killCount = qs.getInt(KILL_COUNT_VAR) + 1;
			if (killCount < 20)
			{
				qs.set(KILL_COUNT_VAR, killCount);
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				sendNpcLogList(killer);
			}
			else
			{
				qs.setCond(2, true);
				qs.unset(KILL_COUNT_VAR);
				giveItems(killer, SOE_KERKIR);
				showOnScreenMsg(killer, NpcStringId.THE_TRAINING_IN_OVER_NUSE_A_SCROLL_OF_ESCAPE_IN_YOUR_INVENTORY_TO_GO_BACK_TO_MASTER_KERKIR, ExShowScreenMessage.TOP_CENTER, 10000);
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
			holder.add(new NpcLogListHolder(NpcStringId.KILL_MONSTERS_IN_THE_QUIET_PLAIN.getId(), true, qs.getInt(KILL_COUNT_VAR)));
			return holder;
		}
		return super.getNpcLogList(player);
	}
}
