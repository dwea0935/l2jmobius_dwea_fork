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
package quests.Q10790_AMercenaryHelper;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExQuestNpcLogList;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * A Mercenary Helper (10790)
 * @author Stayway
 */
public class Q10790_AMercenaryHelper extends Quest
{
	// NPC
	private static final int DOKARA = 33847;
	// Monsters
	private static final int SPLINTER_STAKATO = 21508;
	private static final int SPLINTER_STAKATO_WORKER = 21509;
	private static final int SPLINTER_STAKATO_SOLDIER = 21510;
	private static final int SPLINTER_STAKATO_DRONE = 21511;
	private static final int NEEDLE_STAKATO = 21513;
	private static final int NEEDLE_STAKATO_WORKER = 21514;
	private static final int NEEDLE_STAKATO_SOLDIER = 21515;
	private static final int NEEDLE_STAKATO_DRONE = 21516;
	private static final Map<Integer, Integer> MOBS_REQUIRED = new HashMap<>();
	static
	{
		MOBS_REQUIRED.put(SPLINTER_STAKATO, 50);
	}
	// Item
	private static final ItemHolder GUILD_COIN = new ItemHolder(37045, 3);
	private static final ItemHolder ENCHANT_ARMOR_A = new ItemHolder(26351, 3);
	// Rewards
	private static final int EXP_REWARD = 942690;
	private static final int SP_REWARD = 226;
	// Other
	private static final int MIN_LEVEL = 65;
	private static final int MAX_LEVEL = 70;
	
	public Q10790_AMercenaryHelper()
	{
		super(10790);
		addStartNpc(DOKARA);
		addTalkId(DOKARA);
		addKillId(SPLINTER_STAKATO, SPLINTER_STAKATO_WORKER, SPLINTER_STAKATO_SOLDIER, SPLINTER_STAKATO_DRONE, NEEDLE_STAKATO, NEEDLE_STAKATO_WORKER, NEEDLE_STAKATO_SOLDIER, NEEDLE_STAKATO_DRONE);
		addCondMinLevel(MIN_LEVEL, "no_level.htm");
		addCondRace(Race.ERTHEIA, "no Ertheia.html");
		addCondClassId(PlayerClass.MARAUDER, "no_class.html");
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
			case "33847-02.htm":
			case "33847-03.htm":
			{
				htmltext = event;
				break;
			}
			case "33847-04.htm": // start the quest
			{
				qs.startQuest();
				qs.set(Integer.toString(SPLINTER_STAKATO), 0);
				htmltext = event;
				break;
			}
			case "33847-07.html":
			{
				if (qs.isCond(2))
				{
					giveItems(player, GUILD_COIN);
					giveItems(player, ENCHANT_ARMOR_A);
					addExpAndSp(player, EXP_REWARD, SP_REWARD);
					qs.exitQuest(false, true);
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
		final QuestState qs = getQuestState(player, true);
		String htmltext = null;
		switch (qs.getState())
		{
			case State.CREATED:
			{
				if ((player.getLevel() < MIN_LEVEL) || (player.getLevel() > MAX_LEVEL))
				{
					htmltext = "no_level.html";
				}
				else
				{
					htmltext = "33847-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				if (qs.isCond(1))
				{
					htmltext = "33847-05.html"; // Need find proper html
				}
				else if (qs.isCond(2))
				{
					htmltext = "33847-06.html";
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
		final QuestState qs = getRandomPartyMemberState(killer, -1, 3, npc);
		if ((qs != null) && qs.isStarted() && qs.isCond(1) && LocationUtil.checkIfInRange(1500, npc, qs.getPlayer(), false))
		{
			int kills = 0;
			switch (npc.getId())
			{
				case SPLINTER_STAKATO:
				case SPLINTER_STAKATO_WORKER:
				case SPLINTER_STAKATO_SOLDIER:
				case SPLINTER_STAKATO_DRONE:
				case NEEDLE_STAKATO:
				case NEEDLE_STAKATO_WORKER:
				case NEEDLE_STAKATO_SOLDIER:
				case NEEDLE_STAKATO_DRONE:
				{
					kills = qs.getInt(Integer.toString(SPLINTER_STAKATO));
					kills++;
					qs.set(Integer.toString(SPLINTER_STAKATO), kills);
					break;
				}
			}
			
			final ExQuestNpcLogList log = new ExQuestNpcLogList(getId());
			log.addNpc(SPLINTER_STAKATO, qs.getInt(Integer.toString(SPLINTER_STAKATO)));
			log.addNpcString(NpcStringId.KILL_STAKATOS, qs.getInt(Integer.toString(SPLINTER_STAKATO)));
			killer.sendPacket(log);
			
			if ((qs.getInt(Integer.toString(SPLINTER_STAKATO)) >= MOBS_REQUIRED.get(SPLINTER_STAKATO)) && (qs.getInt(Integer.toString(SPLINTER_STAKATO)) >= MOBS_REQUIRED.get(SPLINTER_STAKATO)))
			{
				qs.setCond(2);
			}
		}
	}
}
