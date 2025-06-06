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
package quests.Q00257_TheGuardIsBusy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.managers.QuestManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

import ai.others.NewbieGuide.NewbieGuide;

/**
 * The Guard is Busy (257)
 * @author xban1x
 */
public class Q00257_TheGuardIsBusy extends Quest
{
	public static class MobDrop extends ItemHolder
	{
		private final int _chance;
		private final int _random;
		
		public MobDrop(int random, int chance, int id, long count)
		{
			super(id, count);
			_random = random;
			_chance = chance;
		}
		
		public boolean getDrop()
		{
			return (getRandom(_random) < _chance);
		}
	}
	
	// NPC
	private static final int GILBERT = 30039;
	// Monsters
	private static final Map<Integer, List<MobDrop>> MONSTERS = new HashMap<>();
	// Items
	private static final int GLUDIO_LORDS_MARK = 1084;
	private static final int ORC_AMULET = 752;
	private static final int ORC_NECKLACE = 1085;
	private static final int WEREWOLF_FANG = 1086;
	static
	{
		MONSTERS.put(20006, Arrays.asList(new MobDrop(10, 2, ORC_AMULET, 2), new MobDrop(10, 10, ORC_AMULET, 1))); // Orc Archer
		MONSTERS.put(20093, Arrays.asList(new MobDrop(100, 85, ORC_NECKLACE, 1))); // Orc Fighter
		MONSTERS.put(20096, Arrays.asList(new MobDrop(100, 95, ORC_NECKLACE, 1))); // Orc Fighter Sub Leader
		MONSTERS.put(20098, Arrays.asList(new MobDrop(100, 100, ORC_NECKLACE, 1))); // Orc Fighter Leader
		MONSTERS.put(20130, Arrays.asList(new MobDrop(10, 7, ORC_AMULET, 1))); // Orc
		MONSTERS.put(20131, Arrays.asList(new MobDrop(10, 9, ORC_AMULET, 1))); // Orc Grunt
		MONSTERS.put(20132, Arrays.asList(new MobDrop(10, 7, WEREWOLF_FANG, 1))); // Werewolf
		MONSTERS.put(20342, Arrays.asList(new MobDrop(0, 1, WEREWOLF_FANG, 1))); // Werewolf Chieftain
		MONSTERS.put(20343, Arrays.asList(new MobDrop(100, 85, WEREWOLF_FANG, 1))); // Werewolf Hunter
	}
	private static final ItemHolder SPIRITSHOTS_NO_GRADE_FOR_ROOKIES = new ItemHolder(5790, 3000);
	private static final ItemHolder SOULSHOTS_NO_GRADE_FOR_ROOKIES = new ItemHolder(5789, 6000);
	// Misc
	private static final int MIN_LEVEL = 6;
	private static final int GUIDE_MISSION = 41;
	
	public Q00257_TheGuardIsBusy()
	{
		super(257, "The Guard is Busy");
		addStartNpc(GILBERT);
		addTalkId(GILBERT);
		addKillId(MONSTERS.keySet());
		registerQuestItems(ORC_AMULET, GLUDIO_LORDS_MARK, ORC_NECKLACE, WEREWOLF_FANG);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		String htmltext = null;
		if (qs == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "30039-03.htm":
			{
				qs.startQuest();
				giveItems(player, GLUDIO_LORDS_MARK, 1);
				htmltext = event;
				break;
			}
			case "30039-05.html":
			{
				qs.exitQuest(true, true);
				htmltext = event;
				break;
			}
			case "30039-06.html":
			{
				htmltext = event;
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if (qs == null)
		{
			return;
		}
		
		for (MobDrop drop : MONSTERS.get(npc.getId()))
		{
			if (drop.getDrop())
			{
				giveItems(killer, drop);
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				break;
			}
		}
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
				htmltext = (player.getLevel() >= MIN_LEVEL) ? "30039-02.htm" : "30039-01.html";
				break;
			}
			case State.STARTED:
			{
				if (hasAtLeastOneQuestItem(player, ORC_AMULET, ORC_NECKLACE, WEREWOLF_FANG))
				{
					final long amulets = getQuestItemsCount(player, ORC_AMULET);
					final long common = getQuestItemsCount(player, ORC_NECKLACE, WEREWOLF_FANG);
					giveAdena(player, ((amulets * 10) + (common * 20) + (((amulets + common) >= 10) ? 1000 : 0)), true);
					takeItems(player, -1, ORC_AMULET, ORC_NECKLACE, WEREWOLF_FANG);
					
					if ((player.getLevel() < 25) && (getOneTimeQuestFlag(player, 57) == 0))
					{
						if (player.isMageClass())
						{
							giveItems(player, SPIRITSHOTS_NO_GRADE_FOR_ROOKIES);
							playSound(player, "tutorial_voice_027");
						}
						else
						{
							giveItems(player, SOULSHOTS_NO_GRADE_FOR_ROOKIES);
							playSound(player, "tutorial_voice_026");
						}
						
						setOneTimeQuestFlag(player, 57, 1);
					}
					
					// Newbie Guide.
					final Quest newbieGuide = QuestManager.getInstance().getQuest(NewbieGuide.class.getSimpleName());
					if (newbieGuide != null)
					{
						final QuestState newbieGuideQs = newbieGuide.getQuestState(player, true);
						if (!haveNRMemo(newbieGuideQs, GUIDE_MISSION))
						{
							setNRMemo(newbieGuideQs, GUIDE_MISSION);
							setNRMemoState(newbieGuideQs, GUIDE_MISSION, 1000);
							showOnScreenMsg(player, "Acquisition of Soulshot for beginners complete. \\n Go find the Newbie Guide.", 2, 5000);
						}
						else if (((getNRMemoState(newbieGuideQs, GUIDE_MISSION) % 10000) / 1000) != 1)
						{
							setNRMemo(newbieGuideQs, GUIDE_MISSION);
							setNRMemoState(newbieGuideQs, GUIDE_MISSION, getNRMemoState(newbieGuideQs, GUIDE_MISSION) + 1000);
							showOnScreenMsg(player, "Acquisition of Soulshot for beginners complete. \\n Go find the Newbie Guide.", 2, 5000);
						}
					}
					
					htmltext = "30039-07.html";
				}
				else
				{
					htmltext = "30039-04.html";
				}
				break;
			}
		}
		return htmltext;
	}
}
