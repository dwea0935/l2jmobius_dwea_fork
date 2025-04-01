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
package quests.Q10905_HuntingTime;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.quest.NpcLogListHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.util.ArrayUtil;

import quests.Q10904_JourneyToTheConquestWorld.Q10904_JourneyToTheConquestWorld;

/**
 * @author CostyKiller
 */
public class Q10905_HuntingTime extends Quest
{
	// NPC
	private static final int ENTROPY = 34599;
	// Monsters
	private static final int[] MONSTERS_OUTER_CASTLE =
	{
		// Daril's Water Source (Lv. 112)
		27701, // Daril
		27702, // Atron
		27703, // Seo
		27704, // Craigo
		27705, // Kiku
		27706, // Floato
		// Daril Phran's Water Source (Lv. 116)
		27707, // Daril Phran
		27708, // Atron Phran
		27709, // Seo Phran
		27710, // Craigo Phran
		27711, // Kiku Phran
		27712, // Floato Phran
	};
	private static final int[] MONSTERS_ASA =
	{
		// Asa Area 1 (Lv. 116)
		27713, // Daril Asa Ar
		27714, // Seo Asa Ar
		27715, // Floato Asa Ar
		27716, // Asa Ar Hunter
		27717, // Asa Ar Sorceress
		27718, // Saida Asa Ar
		19830, // Luminous Soul
		// Asa Area 2 (Lv. 120)
		27719, // Atron Asa Mide
		27720, // Craigo Asa Mide
		27721, // Kerberos Asa Mide
		27722, // Asa Mide Hunter
		27723, // Asa Mide Sorceress
		27724, // Saida Asa Mide
		27725, // Asa Mide Blader
		// Asa Area 3 (Lv. 124)
		27726, // Atron Asa Telro
		27727, // Craigo Asa Telro
		27728, // Beor Asa Telro
		27729, // Asa Telro Hunter
		27730, // Asa Telro Sorceress
		27731, // Saida Asa Telro
		27732, // Asa Telro Blader
		27733, // Asa Telro Guard
	};
	private static final int[] MONSTERS_ANIMA =
	{
		// Anima Area 1 (Lv. 116)
		27755, // Daril Anima Ar
		27756, // Seo Anima Ar
		27757, // Floato Anima Ar
		27758, // Anima Ar Hunter
		27759, // Anima Ar Sorceress
		27760, // Saida Anima Ar
		// Anima Area 2 (Lv. 120)
		27761, // Atron Anima Mide
		27762, // Craigo Anima Mide
		27763, // Kerberos Anima Mide
		27764, // Anima Mide Hunter
		27765, // Anima Mide Sorceress
		27766, // Saida Anima Mide
		27767, // Anima Mide Blader
		// Anima Area 3 (Lv. 124)
		27768, // Atron Anima Telro
		27769, // Craigo Anima Telro
		27770, // Beor Anima Telro
		27771, // Anima Telro Hunter
		27772, // Anima Telro Sorceress
		27773, // Saida Anima Telro
		27774, // Anima Telro Blader
		27775, // Anima Telro Guard
	};
	private static final int[] MONSTERS_NOX =
	{
		// Nox Area 1 (Lv. 116)
		27734, // Daril Nox Ar
		27735, // Seo Nox Ar
		27736, // Floato Nox Ar
		27737, // Nox Ar Hunter
		27738, // Nox Ar Sorceress
		27739, // Saida Nox Ar
		// Nox Area 2 (Lv. 120)
		27740, // Atron Nox Mide
		27741, // Craigo Nox Mide
		27742, // Kerberos Nox Mide
		27743, // Nox Mide Hunter
		27744, // Nox Mide Sorceress
		27745, // Saida Nox Mide
		27746, // Nox Mide Blader
		// Nox Area 3 (Lv. 124)
		27747, // Atron Nox Telro
		27748, // Craigo Nox Telro
		27749, // Beor Nox Telro
		27750, // Nox Telro Hunter
		27751, // Nox Telro Sorceress
		27752, // Saida Nox Telro
		27753, // Nox Telro Blader
		27754, // Nox Telro Guard
	};
	// Misc
	private static final int MIN_LEVEL = 110;
	private static final String KILL_COUNT_VAR_OUTER_CASTLE = "KillCountOuterCastle";
	private static final String KILL_COUNT_VAR_ASA = "KillCountAsa";
	private static final String KILL_COUNT_VAR_ANIMA = "KillCountAnima";
	private static final String KILL_COUNT_VAR_NOX = "KillCountNox";
	// Monsters Kill Target
	private static final int MAX_KILLED_MOBS_OUTER_CASTLE = 600;
	private static final int MAX_KILLED_MOBS_ASA = 600;
	private static final int MAX_KILLED_MOBS_ANIMA = 600;
	private static final int MAX_KILLED_MOBS_NOX = 600;
	
	public Q10905_HuntingTime()
	{
		super(10905);
		addStartNpc(ENTROPY);
		addTalkId(ENTROPY);
		addKillId(MONSTERS_OUTER_CASTLE);
		addKillId(MONSTERS_ASA);
		addKillId(MONSTERS_ANIMA);
		addKillId(MONSTERS_NOX);
		addCondCompletedQuest(Q10904_JourneyToTheConquestWorld.class.getSimpleName(), "34599-00.html");
		addCondMinLevel(MIN_LEVEL, "34599-00.html");
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
			case "34599-02.html":
			{
				if ((player.getLevel() >= MIN_LEVEL))
				{
					qs.startQuest();
					qs.setMemoState(1);
					htmltext = event;
				}
				break;
			}
			case "34599-05.html":
			{
				if (qs.isCond(2))
				{
					addExpAndSp(player, 26617381116L, 23955643);
					qs.exitQuest(false, true);
					htmltext = event;
				}
				else
				{
					htmltext = "34599-04.html";
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
		switch (qs.getState())
		{
			case State.CREATED:
			{
				if (npc.getId() == ENTROPY)
				{
					htmltext = "34599-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case ENTROPY:
					{
						if ((qs.isCond(1) & qs.isMemoState(1)) || qs.isCond(2))
						{
							htmltext = "34599-03.htm";
						}
						break;
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
		final Party party = killer.getParty();
		if (party != null)
		{
			for (Player member : party.getMembers())
			{
				final QuestState qs = getQuestState(member, false);
				if (qs != null)
				{
					processKill(npc, member);
				}
			}
		}
		else
		{
			processKill(npc, killer);
		}
	}
	
	private void processKill(Npc npc, Player killer)
	{
		final QuestState qs = getRandomPartyMemberState(killer, 1, 3, npc);
		if (qs != null)
		{
			final Player player = qs.getPlayer();
			final int killCountOuterCastle = qs.getInt(KILL_COUNT_VAR_OUTER_CASTLE);
			final int killCountAsa = qs.getInt(KILL_COUNT_VAR_ASA);
			final int killCountAnima = qs.getInt(KILL_COUNT_VAR_ANIMA);
			final int killCountNox = qs.getInt(KILL_COUNT_VAR_NOX);
			
			if (ArrayUtil.contains(MONSTERS_OUTER_CASTLE, npc.getId()))
			{
				if (killCountOuterCastle < MAX_KILLED_MOBS_OUTER_CASTLE)
				{
					qs.set(KILL_COUNT_VAR_OUTER_CASTLE, killCountOuterCastle + 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					checkKillProgress(killer);
				}
			}
			if (ArrayUtil.contains(MONSTERS_ASA, npc.getId()))
			{
				if (killCountAsa < MAX_KILLED_MOBS_ASA)
				{
					qs.set(KILL_COUNT_VAR_ASA, killCountAsa + 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					checkKillProgress(killer);
				}
			}
			if (ArrayUtil.contains(MONSTERS_ANIMA, npc.getId()))
			{
				if (killCountAnima < MAX_KILLED_MOBS_ANIMA)
				{
					qs.set(KILL_COUNT_VAR_ANIMA, killCountAnima + 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					checkKillProgress(killer);
				}
			}
			if (ArrayUtil.contains(MONSTERS_NOX, npc.getId()))
			{
				if (killCountNox < MAX_KILLED_MOBS_NOX)
				{
					qs.set(KILL_COUNT_VAR_NOX, killCountNox + 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					checkKillProgress(killer);
				}
			}
			
			sendNpcLogList(player);
		}
	}
	
	private void checkKillProgress(Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs.getInt(KILL_COUNT_VAR_OUTER_CASTLE) >= MAX_KILLED_MOBS_OUTER_CASTLE) && (qs.getInt(KILL_COUNT_VAR_ASA) >= MAX_KILLED_MOBS_ASA) && (qs.getInt(KILL_COUNT_VAR_ANIMA) >= MAX_KILLED_MOBS_ANIMA) && (qs.getInt(KILL_COUNT_VAR_NOX) >= MAX_KILLED_MOBS_NOX))
		{
			qs.setCond(2, true);
			playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
		}
	}
	
	@Override
	public Set<NpcLogListHolder> getNpcLogList(Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(1))
		{
			final Set<NpcLogListHolder> holder = new HashSet<>();
			holder.add(new NpcLogListHolder(NpcStringId.MONSTERS_IN_THE_OUTER_CASTLE.getId(), true, qs.getInt(KILL_COUNT_VAR_OUTER_CASTLE)));
			holder.add(new NpcLogListHolder(NpcStringId.MONSTERS_IN_THE_ASA_ZONE.getId(), true, qs.getInt(KILL_COUNT_VAR_ASA)));
			holder.add(new NpcLogListHolder(NpcStringId.MONSTERS_IN_THE_ANIMA_ZONE.getId(), true, qs.getInt(KILL_COUNT_VAR_ANIMA)));
			holder.add(new NpcLogListHolder(NpcStringId.MONSTERS_IN_THE_NOX_ZONE.getId(), true, qs.getInt(KILL_COUNT_VAR_NOX)));
			return holder;
		}
		return super.getNpcLogList(player);
	}
}
