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
package quests.Q10311_BestMedicine;

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
public class Q10311_BestMedicine extends Quest
{
	// NPC
	private static final int GRILL = 34319;
	// Monsters
	private static final int TAILED_WARRIOR = 22426;
	private static final int TAILED_HUNTER = 22427;
	private static final int TAILED_BERSERKER = 22428;
	private static final int TAILED_WIZARD = 22429;
	// Items
	private static final ItemHolder SAYHA_COOKIE = new ItemHolder(93274, 20);
	private static final ItemHolder SAYHA_STORM = new ItemHolder(91712, 12);
	private static final ItemHolder MAGIC_LAMP_CHARGING_POTION = new ItemHolder(91757, 2);
	// Misc
	private static final int MIN_LEVEL = 85;
	private static final String KILL_COUNT_VAR = "KillCount";
	
	public Q10311_BestMedicine()
	{
		super(10311);
		addStartNpc(GRILL);
		addTalkId(GRILL);
		addKillId(TAILED_WARRIOR, TAILED_HUNTER, TAILED_BERSERKER, TAILED_WIZARD);
		setQuestNameNpcStringId(NpcStringId.LV_85_BEST_MEDICINE);
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
			case "StartBestMedicine":
			{
				qs.startQuest();
				htmltext = event;
				showOnScreenMsg(player, NpcStringId.DEFEAT_THE_MONSTERS_IN_THE_HOT_SPRINGS, ExShowScreenMessage.TOP_CENTER, 10000);
				break;
			}
			case "34319-02.htm":
			{
				htmltext = event;
				break;
			}
			case "reward":
			{
				if (qs.isStarted())
				{
					addExpAndSp(player, 500000000, 13500000);
					giveItems(player, SAYHA_COOKIE);
					giveItems(player, SAYHA_STORM);
					giveItems(player, MAGIC_LAMP_CHARGING_POTION);
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
		if (qs.isCreated() && (player.getLevel() < MIN_LEVEL))
		{
			htmltext = "noreq.htm";
			return htmltext;
		}
		if (qs.isCreated())
		{
			htmltext = "34319-01.htm";
		}
		else if (qs.isStarted())
		{
			if (qs.isCond(1))
			{
				htmltext = "34319-03.html";
			}
			else if (qs.isCond(2))
			{
				htmltext = "34319-02.htm";
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
			if (killCount < 2000)
			{
				qs.set(KILL_COUNT_VAR, killCount);
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				sendNpcLogList(killer);
			}
			else
			{
				qs.setCond(2, true);
				qs.unset(KILL_COUNT_VAR);
				showOnScreenMsg(killer, NpcStringId.TALK_TO_ASSISTANT_GRILL, ExShowScreenMessage.TOP_CENTER, 10000);
			}
		}
	}
	
	@Override
	public Set<NpcLogListHolder> getNpcLogList(Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs != null)
		{
			if (qs.isCond(1))
			{
				final Set<NpcLogListHolder> holder = new HashSet<>();
				holder.add(new NpcLogListHolder(NpcStringId.DEFEAT_THE_MONSTERS_IN_THE_HOT_SPRINGS.getId(), true, qs.getInt(KILL_COUNT_VAR)));
				return holder;
			}
			else if (qs.isCond(2))
			{
				final Set<NpcLogListHolder> holder = new HashSet<>();
				holder.add(new NpcLogListHolder(NpcStringId.LV_85_BEST_MEDICINE_COMPLETED.getId(), true, qs.getInt(KILL_COUNT_VAR)));
				return holder;
			}
		}
		return super.getNpcLogList(player);
	}
}
