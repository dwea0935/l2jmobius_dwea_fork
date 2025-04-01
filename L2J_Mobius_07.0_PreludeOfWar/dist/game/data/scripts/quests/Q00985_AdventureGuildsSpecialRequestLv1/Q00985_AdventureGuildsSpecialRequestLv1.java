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
package quests.Q00985_AdventureGuildsSpecialRequestLv1;

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
import org.l2jmobius.gameserver.network.NpcStringId;

/**
 * Adventure Guilds Special Request Lv1 (985)
 * @author Dmitri
 */
public class Q00985_AdventureGuildsSpecialRequestLv1 extends Quest
{
	// NPCs
	private static final int ADVENTURE_GUILDSMAN = 33946;
	// Monsters
	private static final int[] MONSTERS =
	{
		24458, // Vampire Swamp Warrior
		24457, // Swamp Vampire Rogue
		24460, // Swamp Vampire Shooter
		24459, // Swamp Vampire Wizard
		24454, // Berserker of Fate
		24455, // Prophet of Doom
		24453, // Servant of Fate
		24452, // Soldier of Fate
	};
	// Misc
	private static final int MIN_LEVEL = 85;
	private static final int MAX_LEVEL = 89;
	private static final boolean PARTY_QUEST = true;
	// Reward
	private static final int RUNE_WIND_RESISTANCE_RING = 14599;
	
	public Q00985_AdventureGuildsSpecialRequestLv1()
	{
		super(985);
		addStartNpc(ADVENTURE_GUILDSMAN);
		addTalkId(ADVENTURE_GUILDSMAN);
		addKillId(MONSTERS);
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "33946-00.htm");
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "33946-02.htm":
			case "33946-03.htm":
			case "33946-04.htm":
			case "33946-08.html":
			{
				htmltext = event;
				break;
			}
			case "33946-05.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "33946-09.html":
			{
				giveItems(player, RUNE_WIND_RESISTANCE_RING, 1);
				addExpAndSp(player, 2108117571L, 2529741);
				qs.exitQuest(QuestType.DAILY, true);
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
		
		switch (qs.getState())
		{
			case State.CREATED:
			{
				htmltext = "33946-01.htm";
				break;
			}
			case State.STARTED:
			{
				if (qs.isCond(1))
				{
					htmltext = "33946-06.html";
				}
				else
				{
					htmltext = "33946-07.html";
				}
				break;
			}
			case State.COMPLETED:
			{
				if (qs.isNowAvailable())
				{
					qs.setState(State.CREATED);
					htmltext = "33946-01.htm";
				}
				else
				{
					htmltext = getAlreadyCompletedMsg(player, QuestType.DAILY);
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = PARTY_QUEST ? getRandomPartyMemberState(killer, -1, 3, npc) : getQuestState(killer, false);
		if ((qs != null) && qs.isCond(1))
		{
			final int killedGhosts = qs.getInt("AncientGhosts") + 1;
			qs.set("AncientGhosts", killedGhosts);
			playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			
			if (killedGhosts >= 200)
			{
				qs.setCond(2, true);
			}
			sendNpcLogList(killer);
		}
	}
	
	@Override
	public Set<NpcLogListHolder> getNpcLogList(Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(1))
		{
			final Set<NpcLogListHolder> holder = new HashSet<>();
			holder.add(new NpcLogListHolder(NpcStringId.DEFEAT_MONSTER.getId(), true, qs.getInt("AncientGhosts")));
			return holder;
		}
		return super.getNpcLogList(player);
	}
}
