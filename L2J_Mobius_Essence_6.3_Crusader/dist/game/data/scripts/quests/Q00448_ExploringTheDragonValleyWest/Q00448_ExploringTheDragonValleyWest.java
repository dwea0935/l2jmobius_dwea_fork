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
package quests.Q00448_ExploringTheDragonValleyWest;

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
public class Q00448_ExploringTheDragonValleyWest extends Quest
{
	// NPC
	private static final int GENIE_LAMP = 34369;
	// Monsters
	private static final int DRAGON_BEARER_CAPTAIN = 22096;
	private static final int DRAGON_BEARER_WARRIOR = 20759;
	private static final int DRAGON_BEARER_WARRIOR1 = 22097;
	private static final int DRAGON_BEARER_ARCHER = 22098;
	private static final int HEADLESS_KNIGHT = 20146;
	private static final int CAVE_SERVANT_CAPTAIN = 20239;
	private static final int GARGOYLE_HUNTER = 22074;
	private static final int GARGOYLE_HUNTER1 = 22093;
	private static final int GARGOYLE_HUNTER2 = 18450;
	private static final int GARGOYLE_HUNTER3 = 20241;
	private static final int GARGOYLE_HUNTER4 = 22074;
	private static final int MALUK_SUCCUBUS = 20244;
	private static final int CAVE_SERVANT_WARRIOR = 20238;
	private static final int CAVE_SERVANT_WARRIOR1 = 22082;
	private static final int CAVE_SERVANT_WARRIOR2 = 22071;
	private static final int CAVE_SERVANT_WARRIOR3 = 20274;
	private static final int CAVE_KEEPER = 20246;
	private static final int DUSTWIND_GARGOYLE = 20281;
	private static final int DUSTWIND_GARGOYLE1 = 22075;
	private static final int DUSTWIND_GARGOYLE2 = 22088;
	private static final int CAVE_SERVANT_ARCHER = 20238;
	private static final int CAVE_MAIDEN = 20134;
	private static final int CONVICT = 20235;
	private static final int WYRM = 22089;
	private static final int WYRM1 = 20243;
	private static final int WYRM2 = 20282;
	private static final int WYRM3 = 22076;
	private static final int CAVE_BANSHEE = 20412;
	private static final int DRAKE = 20285;
	private static final int DRAKE2 = 20137;
	private static final int BLOOD_QUEEN = 20142;
	private static final int BLOOD_QUEEN2 = 22066;
	private static final int ROYAL_CAVE = 20240;
	private static final int ISHKA = 21960;
	private static final int ISHKA2 = 25407;
	private static final int DRAGON = 22364;
	// Item
	private static final ItemHolder SPIRIT_ORE = new ItemHolder(3031, 50);
	// Misc
	private static final int MIN_LEVEL = 78;
	private static final int MAX_LEVEL = 90;
	private static final String KILL_COUNT_VAR = "KillCount";
	
	public Q00448_ExploringTheDragonValleyWest()
	{
		super(447);
		addStartNpc(GENIE_LAMP);
		addTalkId(GENIE_LAMP);
		addKillId(DRAGON_BEARER_CAPTAIN, DRAGON_BEARER_WARRIOR, DRAGON_BEARER_WARRIOR1, DRAGON_BEARER_ARCHER);
		addKillId(ISHKA, ISHKA2, DRAGON, ROYAL_CAVE, BLOOD_QUEEN2, BLOOD_QUEEN, DRAKE2, DRAKE, CAVE_BANSHEE);
		addKillId(WYRM3, WYRM2, WYRM1, WYRM, CONVICT, CAVE_MAIDEN, CAVE_SERVANT_ARCHER, DUSTWIND_GARGOYLE2, DUSTWIND_GARGOYLE1, DUSTWIND_GARGOYLE);
		addKillId(HEADLESS_KNIGHT, CAVE_SERVANT_CAPTAIN, GARGOYLE_HUNTER, GARGOYLE_HUNTER1, GARGOYLE_HUNTER2);
		addKillId(GARGOYLE_HUNTER3, GARGOYLE_HUNTER4, MALUK_SUCCUBUS, CAVE_SERVANT_WARRIOR, CAVE_SERVANT_WARRIOR1, CAVE_SERVANT_WARRIOR2, CAVE_SERVANT_WARRIOR3, CAVE_KEEPER);
		addCondMinLevel(MIN_LEVEL, "no_lvl.html");
		addCondMaxLevel(MAX_LEVEL, "no_lvl.html");
		setQuestNameNpcStringId(NpcStringId.LV_78_90_EXPLORING_THE_DRAGON_VALLEY_WEST);
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
					// addExpAndSp(player, 100000000, 2700000);
					giveItems(player, SPIRIT_ORE);
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
				if ((killCount < 500))
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
			if (killCount < 500)
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
			holder.add(new NpcLogListHolder(NpcStringId.DEFEAT_MONSTERS_42.getId(), true, qs.getInt(KILL_COUNT_VAR)));
			return holder;
		}
		return super.getNpcLogList(player);
	}
}
