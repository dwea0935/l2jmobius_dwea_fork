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
package quests.Q10297_MemoryOfTheGloriousPast;

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

import quests.Q10295_RespectForGraves.Q10295_RespectForGraves;

/**
 * @author QuangNguyen
 */
public class Q10297_MemoryOfTheGloriousPast extends Quest
{
	// NPCs
	private static final int ORVEN = 30857;
	// Monsters
	private static final int VANOR_SILENOS = 20681;
	private static final int VANOR_SILENOS_SOLDIER = 20682;
	private static final int VANOR_SILENOS_SCOUT = 20683;
	private static final int VANOR_SILENOS_WARRIOR = 20684;
	private static final int VANOR_SILENOS_SHAMAN = 20685;
	private static final int VANOR_SILENOS_CHIEFTAIN = 20686;
	private static final int VANOR_MERCENARY_OF_GLORY = 24014;
	private static final int GUARD_OF_HONOR = 22102;
	// Items
	private static final ItemHolder SOE_PLAINS_OF_GLORY = new ItemHolder(95593, 1);
	private static final ItemHolder SOE_HIGH_PRIEST_OVEN = new ItemHolder(91768, 1);
	private static final ItemHolder MAGIC_LAMP_CHARGING_POTION = new ItemHolder(91757, 3);
	private static final ItemHolder SOULSHOT_TICKET = new ItemHolder(90907, 10);
	private static final ItemHolder SAYHA_GUST = new ItemHolder(91776, 9);
	private static final ItemHolder SPIRIT_ORE = new ItemHolder(3031, 450);
	// Misc
	private static final int MIN_LEVEL = 56;
	private static final int MAX_LEVEL = 64;
	private static final String KILL_COUNT_VAR = "KillCount";
	
	public Q10297_MemoryOfTheGloriousPast()
	{
		super(10297);
		addStartNpc(ORVEN);
		addTalkId(ORVEN);
		addKillId(VANOR_SILENOS, VANOR_SILENOS_SOLDIER, VANOR_SILENOS_SCOUT, VANOR_SILENOS_WARRIOR, VANOR_SILENOS_SHAMAN, VANOR_SILENOS_CHIEFTAIN, VANOR_MERCENARY_OF_GLORY, GUARD_OF_HONOR);
		addCondMinLevel(MIN_LEVEL, "no_lvl.html");
		addCondMaxLevel(MAX_LEVEL, "no_lvl.html");
		addCondCompletedQuest(Q10295_RespectForGraves.class.getSimpleName(), "no_lvl.html");
		registerQuestItems(SOE_PLAINS_OF_GLORY.getId(), SOE_HIGH_PRIEST_OVEN.getId());
		setQuestNameNpcStringId(NpcStringId.LV_56_64_MEMORY_OF_THE_GLORIOUS_PAST);
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
			{
				htmltext = event;
				break;
			}
			case "30857-03.htm":
			{
				qs.startQuest();
				giveItems(player, SOE_PLAINS_OF_GLORY);
				htmltext = event;
				break;
			}
			case "reward":
			{
				if (qs.isCond(2))
				{
					addExpAndSp(player, 35000000, 945000);
					giveItems(player, MAGIC_LAMP_CHARGING_POTION);
					giveItems(player, SOULSHOT_TICKET);
					giveItems(player, SAYHA_GUST);
					giveItems(player, SPIRIT_ORE);
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
				final int killCount = qs.getInt(KILL_COUNT_VAR) + 1;
				if ((killCount < 500) && (player.getLevel() < 64))
				{
					htmltext = "no_kill.html";
				}
				else
				{
					htmltext = "30857-01.htm";
				}
			}
			else if (qs.isCond(2))
			{
				htmltext = "30857-04.html";
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
				giveItems(killer, SOE_HIGH_PRIEST_OVEN);
				qs.unset(KILL_COUNT_VAR);
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
			holder.add(new NpcLogListHolder(NpcStringId.KILL_MONSTERS_ON_THE_PLAINS_OF_GLORY.getId(), true, qs.getInt(KILL_COUNT_VAR)));
			holder.add(new NpcLogListHolder(NpcStringId.REACH_LV_64, player.getLevel() > 63 ? 1 : 0));
			return holder;
		}
		return super.getNpcLogList(player);
	}
}
