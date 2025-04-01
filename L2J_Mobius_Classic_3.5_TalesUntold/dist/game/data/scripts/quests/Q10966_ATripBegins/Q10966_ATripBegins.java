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
package quests.Q10966_ATripBegins;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.quest.NpcLogListHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;

/**
 * A Trip Begins (10966)
 * @author RobikBobik
 * @Notee: Based on NA server September 2019
 */
public class Q10966_ATripBegins extends Quest
{
	// NPCs
	private static final int CAPTAIN_BATHIS = 30332;
	private static final int BELLA = 30256;
	// Items
	private static final ItemHolder SOE_TO_CAPTAIN_BATHIS = new ItemHolder(91651, 1);
	private static final ItemHolder SOE_NOVICE = new ItemHolder(10650, 10); // TODO: Replace with Advanced Scroll of Escape Event(s)
	private static final ItemHolder TALISMAN_OF_ADEN = new ItemHolder(91745, 1); // TODO: Fix item
	private static final ItemHolder SCROLL_OF_ENCHANT_TALISMAN_OF_ADEN = new ItemHolder(91756, 1); // TODO: Fix item
	private static final ItemHolder ADVENTURERS_BRACELET = new ItemHolder(91934, 1);
	// Monsters
	private static final int ARACHNID_PREDATOR = 20926;
	private static final int SKELETON_BOWMAN = 20051;
	private static final int RUIN_SPARTOI = 20054;
	private static final int RAGING_SPARTOI = 20060;
	private static final int TUMRAN_BUGBEAR = 20062;
	private static final int TUMRAN_BUGBEAR_WARRIOR = 20064;
	// Misc
	private static final int MIN_LEVEL = 20;
	private static final int MAX_LEVEL = 25;
	private static final String KILL_COUNT_VAR = "KillCount";
	
	public Q10966_ATripBegins()
	{
		super(10966);
		addStartNpc(CAPTAIN_BATHIS);
		addTalkId(CAPTAIN_BATHIS, BELLA);
		addKillId(ARACHNID_PREDATOR, SKELETON_BOWMAN, RUIN_SPARTOI, RAGING_SPARTOI, RAGING_SPARTOI, TUMRAN_BUGBEAR, TUMRAN_BUGBEAR_WARRIOR);
		addCondMinLevel(MIN_LEVEL, "no_lvl.html");
		addCondMaxLevel(MAX_LEVEL, "no_lvl.html");
		setQuestNameNpcStringId(NpcStringId.LV_20_25_BEGINNING_OF_ADVENTURE);
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
				htmltext = event;
				break;
			}
			case "30332-02.htm":
			{
				htmltext = event;
				break;
			}
			case "30332-03.htm":
			{
				qs.startQuest();
				npc.broadcastPacket(new NpcSay(npc, ChatType.NPC_GENERAL, NpcStringId.TALK_TO_THE_GATEKEEPER));
				htmltext = event;
				break;
			}
			case "30256-01.html":
			{
				qs.setCond(2, true);
				htmltext = event;
				break;
			}
			case "30332-05.html":
			{
				if (qs.isStarted())
				{
					addExpAndSp(player, 500000, 12500);
					giveItems(player, SOE_NOVICE);
					giveItems(player, TALISMAN_OF_ADEN);
					giveItems(player, SCROLL_OF_ENCHANT_TALISMAN_OF_ADEN);
					giveItems(player, ADVENTURERS_BRACELET);
					qs.exitQuest(false, true);
					htmltext = event;
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
		if ((qs != null) && qs.isCond(2))
		{
			final int killCount = qs.getInt(KILL_COUNT_VAR) + 1;
			if (killCount < 300)
			{
				qs.set(KILL_COUNT_VAR, killCount);
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				sendNpcLogList(killer);
			}
			else
			{
				qs.setCond(3, true);
				qs.unset(KILL_COUNT_VAR);
				killer.sendPacket(new ExShowScreenMessage("You have taken your first step as an adventurer.#Return to Bathis and get your reward.", 5000));
				giveItems(killer, SOE_TO_CAPTAIN_BATHIS);
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
			holder.add(new NpcLogListHolder(NpcStringId.CLEAR_RUINS_OF_AGONY.getId(), true, qs.getInt(KILL_COUNT_VAR)));
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
						htmltext = "30332-03.htm";
					}
					else if (qs.isCond(3))
					{
						htmltext = "30332-04.html";
					}
					break;
				}
				case BELLA:
				{
					if (qs.isCond(1))
					{
						htmltext = "30256.html";
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