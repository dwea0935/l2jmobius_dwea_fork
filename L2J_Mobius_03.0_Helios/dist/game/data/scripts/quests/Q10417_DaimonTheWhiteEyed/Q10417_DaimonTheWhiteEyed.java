/* 
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package quests.Q10417_DaimonTheWhiteEyed;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.quest.NpcLogListHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.util.ArrayUtil;

import quests.Q10416_InSearchOfTheEyeOfArgos.Q10416_InSearchOfTheEyeOfArgos;

/**
 * Daimon the White-eyed (10417)
 * @author St3eT, Iris
 */
public class Q10417_DaimonTheWhiteEyed extends Quest
{
	// NPCs
	private static final int EYE_OF_ARGOS = 31683;
	private static final int JANITT = 33851;
	private static final int DAIMON_THE_WHITEEYED = 27499;
	// Items
	private static final int EAA = 730; // Scroll: Enchant Armor (A-grade)
	// Misc
	private static final int MIN_LEVEL = 70;
	private static final int MAX_LEVEL = 75;
	
	private static final int[] MONSTER_TO_KILL =
	{
		21294, // Canyon Antelope
		21295, // Canyon Antelope Slave
		21296, // Canyon Bandersnatch
		21297, // Canyon Bandersnatch Slave
		21304, // Valley Grendel Slave
		23312, // Valley Grendel
		21299, // Valley Buffalo Slave
		23311, // Valley Buffalo
	};
	
	public Q10417_DaimonTheWhiteEyed()
	{
		super(10417);
		addStartNpc(EYE_OF_ARGOS);
		addTalkId(EYE_OF_ARGOS, JANITT);
		addKillId(DAIMON_THE_WHITEEYED);
		addKillId(MONSTER_TO_KILL);
		addCondNotRace(Race.ERTHEIA, "31683-09.html");
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "31683-08.htm");
		addCondCompletedQuest(Q10416_InSearchOfTheEyeOfArgos.class.getSimpleName(), "31683-08.htm");
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
			case "31683-02.htm":
			case "31683-03.htm":
			{
				htmltext = event;
				break;
			}
			case "31683-04.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "31683-07.html":
			{
				if (qs.isCond(3))
				{
					qs.setCond(4, true);
					htmltext = event;
				}
				break;
			}
			case "31683-03.html":
			{
				if (qs.isCond(4))
				{
					qs.exitQuest(false, true);
					giveItems(player, EAA, 5);
					giveStoryQuestReward(player, 26);
					if (player.getLevel() > MIN_LEVEL)
					{
						addExpAndSp(player, 306_167_814, 3265);
					}
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
		String htmltext = getNoQuestMsg(player);
		if (qs.getState() == State.CREATED)
		{
			if (npc.getId() == EYE_OF_ARGOS)
			{
				htmltext = "31683-01.htm";
			}
		}
		else if (qs.getState() == State.STARTED)
		{
			switch (qs.getCond())
			{
				case 1:
				{
					htmltext = npc.getId() == EYE_OF_ARGOS ? "31683-05.html" : "33851-01.html";
					break;
				}
				case 2:
				{
					htmltext = npc.getId() == EYE_OF_ARGOS ? "31683-05.html" : "33851-01.html";
					break;
				}
				case 3:
				{
					htmltext = npc.getId() == EYE_OF_ARGOS ? "31683-06.html" : "33851-01.html";
					break;
				}
				case 4:
				{
					htmltext = npc.getId() == EYE_OF_ARGOS ? "31683-07.html" : "33851-02.html";
					break;
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final int npcHolder = npc.getId();
		if (killer.isInParty())
		{
			final Party party = killer.getParty();
			final List<Player> partyMember = party.getMembers();
			for (Player singleMember : partyMember)
			{
				final QuestState qsPartyMember = getQuestState(singleMember, false);
				final double distance = npc.calculateDistance3D(singleMember);
				if ((qsPartyMember != null) && qsPartyMember.isCond(1) && (distance <= 1000))
				{
					int mobCount = qsPartyMember.getInt("KillCount_MOBS");
					if (mobCount < 100)
					{
						if (ArrayUtil.contains(MONSTER_TO_KILL, npcHolder))
						{
							mobCount++;
							qsPartyMember.set("KillCount_MOBS", mobCount);
							playSound(singleMember, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
						sendNpcLogList(singleMember);
					}
					
					if (mobCount == 100)
					{
						qsPartyMember.setCond(2, true);
					}
				}
				
				if ((qsPartyMember != null) && qsPartyMember.isCond(2))
				{
					if (npcHolder == DAIMON_THE_WHITEEYED)
					{
						playSound(singleMember, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						qsPartyMember.setCond(3, true);
					}
					sendNpcLogList(singleMember);
				}
			}
		}
		else
		{
			final QuestState qs = getQuestState(killer, false);
			if ((qs != null) && qs.isCond(1))
			{
				int mobCount = qs.getInt("KillCount_MOBS");
				if (mobCount < 100)
				{
					if (ArrayUtil.contains(MONSTER_TO_KILL, npcHolder))
					{
						mobCount++;
						qs.set("KillCount_MOBS", mobCount);
						playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				
				if (mobCount == 100)
				{
					qs.setCond(2, true);
				}
			}
			
			if ((qs != null) && qs.isCond(2))
			{
				if (npcHolder == DAIMON_THE_WHITEEYED)
				{
					playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					qs.setCond(3, true);
				}
			}
		}
	}
	
	@Override
	public Set<NpcLogListHolder> getNpcLogList(Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(1))
		{
			final int killCount = qs.getInt("KillCount_MOBS");
			if ((killCount > 0) && (killCount != 200))
			{
				final Set<NpcLogListHolder> holder = new HashSet<>();
				holder.add(new NpcLogListHolder(NpcStringId.DEFEAT_THE_BEASTS_OF_THE_VALLEY, killCount));
				return holder;
			}
			if (qs.isCond(3))
			{
				final Set<NpcLogListHolder> holder = new HashSet<>();
				holder.add(new NpcLogListHolder(DAIMON_THE_WHITEEYED, false, killCount));
				return holder;
			}
		}
		return super.getNpcLogList(player);
	}
}