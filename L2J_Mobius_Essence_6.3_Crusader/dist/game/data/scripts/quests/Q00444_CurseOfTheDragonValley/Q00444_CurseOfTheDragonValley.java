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
package quests.Q00444_CurseOfTheDragonValley;

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
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * @author Serenitty
 */
public class Q00444_CurseOfTheDragonValley extends Quest
{
	// NPC
	private static final int GENIE_LAMP = 34369;
	// Monsters
	private static final int CONVICT = 20235;
	private static final int WYRM = 22089;
	private static final int WYRM1 = 20243;
	private static final int WYRM2 = 20282;
	private static final int WYRM3 = 22076;
	private static final int CAVE_BANSHEE = 20412;
	private static final int CAVE_SERVANT_WARRIOR = 20238;
	private static final int CAVE_SERVANT_WARRIOR1 = 22082;
	private static final int CAVE_SERVANT_WARRIOR2 = 22071;
	private static final int CAVE_SERVANT_WARRIOR3 = 20274;
	private static final int DRAGON = 22364;
	// Item
	private static final ItemHolder BOOST_ATK_SCROLL = new ItemHolder(94269, 4);
	// Misc
	private static final int MIN_LEVEL = 78;
	private static final int MAX_LEVEL = 90;
	private static final String KILL_COUNT_VAR = "KillCount";
	
	public Q00444_CurseOfTheDragonValley()
	{
		super(444);
		addStartNpc(GENIE_LAMP);
		addTalkId(GENIE_LAMP);
		addKillId(CONVICT, CAVE_BANSHEE, DRAGON);
		addKillId(WYRM, WYRM1, WYRM2, WYRM3, CAVE_SERVANT_WARRIOR);
		addKillId(CAVE_SERVANT_WARRIOR1, CAVE_SERVANT_WARRIOR2, CAVE_SERVANT_WARRIOR3);
		addCondMinLevel(MIN_LEVEL, "no_lvl.html");
		addCondMaxLevel(MAX_LEVEL, "no_lvl.html");
		setQuestNameNpcStringId(NpcStringId.LV_78_90_CURSE_OF_THE_DRAGON_VALLEY);
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
			case "34369.htm":
			case "34369-01.html":
			case "34369-02.htm":
			{
				htmltext = event;
				break;
			}
			case "StartMission":
			{
				qs.startQuest();
				qs.setCond(1, true);
				htmltext = "34369-02.htm"; // no kill htm
				break;
			}
			case "reward":
			{
				if (qs.isCond(2))
				{
					addExpAndSp(player, 100000000, 2700000);
					giveItems(player, BOOST_ATK_SCROLL);
					htmltext = "34369-05.html";
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
			htmltext = "34369.htm";
		}
		else if (qs.isStarted())
		{
			if (qs.isCond(1))
			{
				final int killCount = qs.getInt(KILL_COUNT_VAR) + 1;
				if ((killCount < 200))
				{
					htmltext = "34369-03.html"; // no kill html
				}
				else
				{
					htmltext = "34369.htm";
				}
			}
			else if (qs.isCond(2))
			{
				htmltext = "34369-04.html"; // reward
			}
		}
		else if (qs.isCompleted())
		{
			if (npc.getId() == GENIE_LAMP)
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
			if (killCount < 200)
			{
				qs.set(KILL_COUNT_VAR, killCount);
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				sendNpcLogList(killer);
			}
			else
			{
				showOnScreenMsg(killer, NpcStringId.SUMMON_GENIE_AND_TALK_TO_HIM, ExShowScreenMessage.TOP_CENTER, 10000);
				qs.setCond(2, true);
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
			holder.add(new NpcLogListHolder(NpcStringId.DEFEAT_MONSTERS_38.getId(), true, qs.getInt(KILL_COUNT_VAR)));
			return holder;
		}
		return super.getNpcLogList(player);
	}
}
