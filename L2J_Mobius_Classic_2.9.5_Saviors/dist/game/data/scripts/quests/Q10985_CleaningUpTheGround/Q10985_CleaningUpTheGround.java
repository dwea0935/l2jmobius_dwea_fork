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
package quests.Q10985_CleaningUpTheGround;

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
 * Cleaning-Up The Ground (10985)
 * @author RobikBobik
 * @Notee: Based on NA server September 2019
 */
public class Q10985_CleaningUpTheGround extends Quest
{
	// NPCs
	private static final int NEWBIE_GUIDE = 30600;
	private static final int VOLODOS = 30137;
	// Monsters
	private static final int ASHEN_WOLF = 20456;
	private static final int GOBLIN = 20003;
	private static final int IMP = 20004;
	// Items
	private static final ItemHolder SOE_TO_VOLLODOS = new ItemHolder(91648, 1);
	private static final ItemHolder SOE_NOVICE = new ItemHolder(10650, 10);
	private static final ItemHolder RING_NOVICE = new ItemHolder(49041, 2);
	private static final ItemHolder EARRING_NOVICE = new ItemHolder(49040, 2);
	private static final ItemHolder NECKLACE_NOVICE = new ItemHolder(49039, 1);
	// Misc
	private static final int MAX_LEVEL = 20;
	private static final String KILL_COUNT_VAR = "KillCount";
	
	public Q10985_CleaningUpTheGround()
	{
		super(10985);
		addStartNpc(NEWBIE_GUIDE);
		addTalkId(NEWBIE_GUIDE, VOLODOS);
		addKillId(ASHEN_WOLF, GOBLIN, IMP);
		addCondMaxLevel(MAX_LEVEL, "no_lvl.html");
		registerQuestItems(SOE_TO_VOLLODOS.getId());
		setQuestNameNpcStringId(NpcStringId.LV_2_20_CLEAN_THE_YARD);
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
			case "TELEPORT_TO_HUNTING_GROUND":
			{
				player.teleToLocation(8945, 3529, -2504);
				break;
			}
			case "30600-02.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "30137-02.htm":
			{
				if (qs.isStarted())
				{
					addExpAndSp(player, 260000, 6000);
					giveItems(player, SOE_NOVICE);
					giveItems(player, RING_NOVICE);
					giveItems(player, EARRING_NOVICE);
					giveItems(player, NECKLACE_NOVICE);
					qs.exitQuest(false, true);
					htmltext = event;
					break;
				}
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
				killer.sendPacket(new ExShowScreenMessage("You hunted all monsters.#Use the Scroll of Escape in you inventory.", 5000));
				giveItems(killer, SOE_TO_VOLLODOS);
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
			holder.add(new NpcLogListHolder(NpcStringId.DRIVE_OUT_MONSTERS_AROUND_THE_VILLAGE.getId(), true, qs.getInt(KILL_COUNT_VAR)));
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
			htmltext = "30600-01.htm";
		}
		else if (qs.isStarted())
		{
			switch (npc.getId())
			{
				case NEWBIE_GUIDE:
				{
					if (qs.isCond(1))
					{
						htmltext = "30600-02.htm";
					}
					break;
				}
				case VOLODOS:
				{
					if (qs.isCond(2))
					{
						htmltext = "30137.htm";
					}
					break;
				}
			}
		}
		else if (qs.isCompleted())
		{
			if (npc.getId() == NEWBIE_GUIDE)
			{
				htmltext = getAlreadyCompletedMsg(player);
			}
		}
		return htmltext;
	}
}