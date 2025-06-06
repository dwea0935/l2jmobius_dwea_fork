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
package quests.Q00462_StuffedAncientHeroes;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.NpcLogListHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.QuestType;
import org.l2jmobius.gameserver.model.quest.State;

import quests.Q10317_OrbisWitch.Q10317_OrbisWitch;

/**
 * Stuffed Ancient Heroes (462)
 * @URL https://l2wiki.com/Stuffed_Ancient_Heroes
 * @author Gigi
 */
public class Q00462_StuffedAncientHeroes extends Quest
{
	// NPCs
	private static final int LYDIA = 32892;
	private static final int ANCIENT_HEROES = 33347;
	// Boss
	private static final int[] BOSES =
	{
		25760, // Turanclass
		25761, // Georgios
		25762, // Angelos
		25763, // Theofanis
		25764, // Steregos
		25766, // Talicrome
		25767, // Meikaliya
		25768, // Evangelos
		25769, // Sotiris
		25770 // Lazaros
	};
	// Item
	private static final int CERTIFICATE_OF_HERO = 30386;
	// Misc
	private static final int MIN_LEVEL = 95;
	private static final String KILL_COUNT_VAR = "KillCount";
	
	public Q00462_StuffedAncientHeroes()
	{
		super(462);
		addStartNpc(LYDIA);
		addTalkId(LYDIA);
		addKillId(BOSES);
		addCondMinLevel(MIN_LEVEL, "32892-00.htm");
		addCondCompletedQuest(Q10317_OrbisWitch.class.getSimpleName(), "32892-00.htm");
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return getNoQuestMsg(player);
		}
		switch (event)
		{
			case "32892-02.htm":
			case "32892-03.htm":
			case "32892-04.htm":
			case "32892-09.html":
			{
				htmltext = event;
				break;
			}
			case "32892-05.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "32892-08.html":
			{
				final int killCount = qs.getInt(KILL_COUNT_VAR);
				if (qs.isCond(2))
				{
					giveItems(player, CERTIFICATE_OF_HERO, 2);
					qs.exitQuest(QuestType.DAILY, true);
				}
				else if (qs.isCond(3))
				{
					giveItems(player, CERTIFICATE_OF_HERO, killCount);
					qs.exitQuest(QuestType.DAILY, true);
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
		if (npc.getId() == LYDIA)
		{
			switch (qs.getState())
			{
				case State.COMPLETED:
				{
					if (!qs.isNowAvailable())
					{
						htmltext = "32892-00a.html";
						break;
					}
					qs.setState(State.CREATED);
					// fallthrough
				}
				case State.CREATED:
				{
					htmltext = "32892-01.htm";
					qs.isStarted();
					break;
				}
				case State.STARTED:
				{
					if (qs.isCond(1))
					{
						htmltext = "32892-06.html";
					}
					else if (qs.getCond() > 1)
					{
						htmltext = "32892-07.html";
					}
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
		if ((qs != null) && (qs.getCond() > 0))
		{
			final int killCount = qs.getInt(KILL_COUNT_VAR) + 1;
			qs.set(KILL_COUNT_VAR, killCount);
			if (killCount == 1)
			{
				qs.setCond(2, true);
			}
			else if ((killCount > 1) && (killCount <= 10))
			{
				qs.setCond(1);
				qs.setCond(3);
			}
			else
			{
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
		}
	}
	
	@Override
	public Set<NpcLogListHolder> getNpcLogList(Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && (qs.getCond() > 0))
		{
			final int killCount = qs.getInt(KILL_COUNT_VAR);
			if (killCount > 0)
			{
				final Set<NpcLogListHolder> holder = new HashSet<>(1);
				holder.add(new NpcLogListHolder(ANCIENT_HEROES, false, killCount));
				return holder;
			}
		}
		return super.getNpcLogList(player);
	}
}