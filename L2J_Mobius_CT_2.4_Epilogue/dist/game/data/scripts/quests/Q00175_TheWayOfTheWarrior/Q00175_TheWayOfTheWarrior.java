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
package quests.Q00175_TheWayOfTheWarrior;

import org.l2jmobius.gameserver.managers.QuestManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

import ai.others.NewbieGuide.NewbieGuide;

/**
 * The Way of the Warrior (175)
 * @author ivantotov
 */
public class Q00175_TheWayOfTheWarrior extends Quest
{
	// NPCs
	private static final int PERWAN = 32133;
	private static final int KEKROPUS = 32138;
	// Items
	private static final ItemHolder WOLF_TAIL = new ItemHolder(9807, 5);
	private static final ItemHolder MUERTOS_CLAW = new ItemHolder(9808, 10);
	// Buff
	private static final SkillHolder UNSEALED_ALTAR = new SkillHolder(4549, 1);
	// Rewards
	private static final int WARRIORS_SWORD = 9720;
	private static final ItemHolder SOULSHOTS_NO_GRADE_FOR_ROOKIES = new ItemHolder(5789, 7000);
	private static final ItemHolder[] REWARDS =
	{
		new ItemHolder(1060, 100), // Lesser Healing Potion
		new ItemHolder(4412, 10), // Echo Crystal - Theme of Battle
		new ItemHolder(4413, 10), // Echo Crystal - Theme of Love
		new ItemHolder(4414, 10), // Echo Crystal - Theme of Solitude
		new ItemHolder(4415, 10), // Echo Crystal - Theme of Feast
		new ItemHolder(4416, 10), // Echo Crystal - Theme of Celebration
	};
	// Monsters
	private static final int MOUNTAIN_WEREWOLF = 22235;
	private static final int[] MONSTERS =
	{
		22236, // Muertos Archer
		22239, // Muertos Guard
		22240, // Muertos Scout
		22242, // Muertos Warrior
		22243, // Muertos Captain
		22245, // Muertos Lieutenant
		22246, // Muertos Commander
	};
	// Misc
	private static final int MIN_LEVEL = 10;
	private static final int GUIDE_MISSION = 41;
	
	public Q00175_TheWayOfTheWarrior()
	{
		super(175, "The Way of the Warrior");
		addStartNpc(KEKROPUS);
		addTalkId(KEKROPUS, PERWAN);
		addKillId(MOUNTAIN_WEREWOLF);
		addKillId(MONSTERS);
		registerQuestItems(WOLF_TAIL.getId(), MUERTOS_CLAW.getId());
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
			case "32138-02.htm":
			{
				htmltext = event;
				break;
			}
			case "32138-05.htm":
			{
				if (qs.isCreated())
				{
					qs.startQuest();
					qs.setMemoState(1);
					htmltext = event;
				}
				break;
			}
			case "32138-10.html":
			{
				qs.setMemoState(6);
				qs.setCond(7, true);
				htmltext = event;
				break;
			}
			case "32138-13.html":
			{
				if (hasItem(player, MUERTOS_CLAW))
				{
					takeItem(player, MUERTOS_CLAW);
					giveItems(player, WARRIORS_SWORD, 1);
					
					if (player.getLevel() < 25)
					{
						playSound(player, "tutorial_voice_026");
						giveItems(player, SOULSHOTS_NO_GRADE_FOR_ROOKIES);
					}
					
					// Newbie Guide.
					final Quest newbieGuide = QuestManager.getInstance().getQuest(NewbieGuide.class.getSimpleName());
					if (newbieGuide != null)
					{
						final QuestState newbieGuideQs = newbieGuide.getQuestState(player, true);
						if (!haveNRMemo(newbieGuideQs, GUIDE_MISSION))
						{
							setNRMemo(newbieGuideQs, GUIDE_MISSION);
							setNRMemoState(newbieGuideQs, GUIDE_MISSION, 100000);
							showOnScreenMsg(player, "Acquisition of race-specific weapon complete. \\n Go find the Newbie Guide.", 2, 5000);
						}
						else if (((getNRMemoState(newbieGuideQs, GUIDE_MISSION) % 1000000) / 100000) != 1)
						{
							setNRMemo(newbieGuideQs, GUIDE_MISSION);
							setNRMemoState(newbieGuideQs, GUIDE_MISSION, getNRMemoState(newbieGuideQs, GUIDE_MISSION) + 100000);
							showOnScreenMsg(player, "Acquisition of race-specific weapon complete. \\n Go find the Newbie Guide.", 2, 5000);
						}
					}
					
					addExpAndSp(player, 20739, 1777);
					giveAdena(player, 8799, true);
					for (ItemHolder reward : REWARDS)
					{
						giveItems(player, reward);
					}
					qs.exitQuest(false, true);
					player.sendPacket(new SocialAction(player.getObjectId(), 3));
					htmltext = event;
				}
				break;
			}
			case "32133-06.html":
			{
				qs.setMemoState(5);
				qs.setCond(6, true);
				npc.setTarget(player);
				npc.doCast(UNSEALED_ALTAR.getSkill());
				htmltext = event;
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		if (npc.getId() == MOUNTAIN_WEREWOLF)
		{
			final QuestState qs = getRandomPartyMemberState(player, 2, 3, npc);
			if ((qs != null) && giveItemRandomly(qs.getPlayer(), npc, WOLF_TAIL.getId(), 1, WOLF_TAIL.getCount(), 0.5, true))
			{
				qs.setCond(3, true);
			}
		}
		else
		{
			final QuestState qs = getRandomPartyMemberState(player, 7, 3, npc);
			if ((qs != null) && giveItemRandomly(qs.getPlayer(), npc, MUERTOS_CLAW.getId(), 1, MUERTOS_CLAW.getCount(), 1, true))
			{
				qs.setCond(8, true);
			}
		}
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (npc.getId())
		{
			case KEKROPUS:
			{
				if (qs.isCreated())
				{
					if (player.getRace() != Race.KAMAEL)
					{
						htmltext = "32138-04.htm";
					}
					else if (player.getLevel() >= MIN_LEVEL)
					{
						htmltext = "32138-01.htm";
					}
					else
					{
						htmltext = "32138-03.htm";
					}
					break;
				}
				else if (qs.isStarted())
				{
					switch (qs.getCond())
					{
						case 1:
						case 2:
						case 3:
						{
							htmltext = "32138-06.html";
							break;
						}
						case 4:
						{
							qs.setMemoState(4);
							qs.setCond(5, true);
							htmltext = "32138-07.html";
							break;
						}
						case 5:
						{
							htmltext = "32138-08.html";
							break;
						}
						case 6:
						{
							htmltext = "32138-09.html";
							break;
						}
						case 7:
						{
							htmltext = "32138-11.html";
							break;
						}
						case 8:
						{
							if (hasItem(player, MUERTOS_CLAW))
							{
								htmltext = "32138-12.html";
							}
							break;
						}
					}
					break;
				}
				else if (qs.isCompleted())
				{
					htmltext = getAlreadyCompletedMsg(player);
					break;
				}
				break;
			}
			case PERWAN:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						qs.setMemoState(2);
						qs.setCond(2, true);
						htmltext = "32133-01.html";
						break;
					}
					case 2:
					{
						htmltext = "32133-02.html";
						break;
					}
					case 3:
					{
						if (hasItem(player, WOLF_TAIL))
						{
							takeItem(player, WOLF_TAIL);
							qs.setMemoState(3);
							qs.setCond(4, true);
							htmltext = "32133-03.html";
						}
						break;
					}
					case 4:
					{
						htmltext = "32133-04.html";
						break;
					}
					case 5:
					{
						htmltext = "32133-05.html";
						break;
					}
					case 6:
					{
						htmltext = "32133-07.html";
						break;
					}
				}
				break;
			}
		}
		return htmltext;
	}
}