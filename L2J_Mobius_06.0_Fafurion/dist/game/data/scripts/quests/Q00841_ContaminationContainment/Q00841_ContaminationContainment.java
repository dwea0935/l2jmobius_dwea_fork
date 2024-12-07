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
package quests.Q00841_ContaminationContainment;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.enums.Faction;
import org.l2jmobius.gameserver.enums.QuestSound;
import org.l2jmobius.gameserver.enums.QuestType;
import org.l2jmobius.gameserver.instancemanager.QuestManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.Id;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.impl.creature.npc.OnNpcMenuSelect;
import org.l2jmobius.gameserver.model.events.impl.creature.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.holders.NpcLogListHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;

import quests.Q10851_ElvenBotany.Q10851_ElvenBotany;
import quests.not_done.Q00838_RequestFromTheMotherTreeGuardians;

/**
 * Contamination Containment (841)
 * @author Kazumi
 */
public final class Q00841_ContaminationContainment extends Quest
{
	// NPCs
	private static final int IRENE = 34233;
	// Monster
	private static final int[] MONSTERS =
	{
		23786, // Nymph Rose - Contaminated
		23787, // Nymph Lily - Contaminated
		23788, // Nymph Tulip - Contaminated
		23789, // Nymph Cosmos - Contaminated
	};
	// Items
	private static final int PURIFIED_WATER = 47170;
	private static final int SUPPLY_BOX_BASIC = 47178;
	private static final int SUPPLY_BOX_INTERMEDIATE = 47179;
	private static final int SUPPLY_BOX_ADVANCED = 47180;
	private static final int FACTION_AMITY_TOKEN = 48030;
	// Misc
	private static final int MIN_LEVEL = 102;
	private static final int COUNT_BASIC = 100;
	private static final int COUNT_INTERMEDIATE = 200;
	private static final int COUNT_ADVANCED = 300;
	private static final int REWARD_BASIC = 100;
	private static final int REWARD_INTERMEDIATE = 200;
	private static final int REWARD_ADVANCED = 300;
	
	public Q00841_ContaminationContainment()
	{
		super(841);
		addStartNpc(IRENE);
		addTalkId(IRENE);
		addKillId(MONSTERS);
		registerQuestItems(PURIFIED_WATER);
		addFactionLevel(Faction.MOTHER_TREE_GUARDIANS, 2, "guardian_leader_q0841_02a.htm");
		addCondMinLevel(MIN_LEVEL, "guardian_leader_q0841_02.htm");
		addCondCompletedQuest(Q10851_ElvenBotany.class.getSimpleName(), "guardian_leader_q0841_02.htm");
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
			case "guardian_leader_q0841_07.htm":
			case "guardian_leader_q0841_07a.htm":
			{
				htmltext = event;
				break;
			}
			case "quest_accept":
			{
				qs.startQuest();
				if ((player.getFactionLevel(Faction.MOTHER_TREE_GUARDIANS) >= 2) && (player.getFactionLevel(Faction.MOTHER_TREE_GUARDIANS) < 4))
				{
					htmltext = "guardian_leader_q0841_05.htm";
				}
				else if ((player.getFactionLevel(Faction.MOTHER_TREE_GUARDIANS) >= 4) && (player.getFactionLevel(Faction.MOTHER_TREE_GUARDIANS) < 5))
				{
					htmltext = "guardian_leader_q0841_05a.htm";
				}
				else if (player.getFactionLevel(Faction.MOTHER_TREE_GUARDIANS) >= 5)
				{
					htmltext = "guardian_leader_q0841_05b.htm";
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
			case State.COMPLETED:
			{
				if (!qs.isNowAvailable())
				{
					htmltext = getAlreadyCompletedMsg(player);
					break;
				}
				qs.setState(State.CREATED);
				// fallthrough
			}
			case State.CREATED:
			{
				htmltext = "guardian_leader_q0841_01.htm";
				break;
			}
			case State.STARTED:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						if ((player.getFactionLevel(Faction.MOTHER_TREE_GUARDIANS) >= 2) && (player.getFactionLevel(Faction.MOTHER_TREE_GUARDIANS) < 4))
						{
							htmltext = "guardian_leader_q0841_08.htm";
						}
						else if ((player.getFactionLevel(Faction.MOTHER_TREE_GUARDIANS) >= 4) && (player.getFactionLevel(Faction.MOTHER_TREE_GUARDIANS) < 5))
						{
							htmltext = "guardian_leader_q0841_08a.htm";
						}
						else if (player.getFactionLevel(Faction.MOTHER_TREE_GUARDIANS) >= 5)
						{
							htmltext = "guardian_leader_q0841_08b.htm";
						}
						break;
					}
					case 2:
					{
						htmltext = "guardian_leader_q0841_11.htm";
						break;
					}
					case 3:
					{
						htmltext = "guardian_leader_q0841_11a.htm";
						break;
					}
					case 4:
					{
						htmltext = "guardian_leader_q0841_11b.htm";
						break;
					}
					case 5:
					{
						htmltext = "guardian_leader_q0841_12.htm";
						break;
					}
					case 6:
					{
						htmltext = "guardian_leader_q0841_12a.htm";
						break;
					}
					case 7:
					{
						htmltext = "guardian_leader_q0841_12b.htm";
						break;
					}
				}
				break;
			}
		}
		return htmltext;
	}
	
	@RegisterEvent(EventType.ON_NPC_MENU_SELECT)
	@RegisterType(ListenerRegisterType.NPC)
	@Id(IRENE)
	public final void onNpcMenuSelect(OnNpcMenuSelect event)
	{
		final Player player = event.getTalker();
		final QuestState qs = getQuestState(player, false);
		final Npc npc = event.getNpc();
		final int ask = event.getAsk();
		final int reply = event.getReply();
		
		if (ask == 841)
		{
			switch (reply)
			{
				case 1:
				{
					showHtmlFile(player, "guardian_leader_q0841_03.htm", npc);
					break;
				}
				case 2:
				{
					showHtmlFile(player, "guardian_leader_q0841_04.htm", npc);
					break;
				}
				case 10:
				{
					if ((player.getFactionLevel(Faction.MOTHER_TREE_GUARDIANS) >= 2) && (player.getFactionLevel(Faction.MOTHER_TREE_GUARDIANS) < 4))
					{
						showHtmlFile(player, "guardian_leader_q0841_06.htm", npc);
					}
					else if ((player.getFactionLevel(Faction.MOTHER_TREE_GUARDIANS) >= 4) && (player.getFactionLevel(Faction.MOTHER_TREE_GUARDIANS) < 5))
					{
						showHtmlFile(player, "guardian_leader_q0841_06a.htm", npc);
					}
					else if (player.getFactionLevel(Faction.MOTHER_TREE_GUARDIANS) >= 5)
					{
						showHtmlFile(player, "guardian_leader_q0841_06b.htm", npc);
					}
					break;
				}
				case 11:
				{
					showHtmlFile(player, "guardian_leader_q0841_09.htm", npc);
					break;
				}
				case 12:
				{
					showHtmlFile(player, "guardian_leader_q0841_09a.htm", npc);
					break;
				}
				case 13:
				{
					showHtmlFile(player, "guardian_leader_q0841_09b.htm", npc);
					break;
				}
				case 21:
				{
					qs.setCond(2);
					giveItems(player, PURIFIED_WATER, 1);
					showHtmlFile(player, "guardian_leader_q0841_10.htm", npc);
					break;
				}
				case 22:
				{
					qs.setCond(3);
					giveItems(player, PURIFIED_WATER, 1);
					showHtmlFile(player, "guardian_leader_q0841_10a.htm", npc);
					break;
				}
				case 23:
				{
					qs.setCond(4);
					giveItems(player, PURIFIED_WATER, 1);
					showHtmlFile(player, "guardian_leader_q0841_10b.htm", npc);
					break;
				}
				case 3101:
				{
					if (qs.getCond() == 5)
					{
						qs.exitQuest(QuestType.DAILY, true);
						
						final Quest qs838 = QuestManager.getInstance().getQuest(Q00838_RequestFromTheMotherTreeGuardians.class.getSimpleName());
						if (qs838 != null)
						{
							qs838.notifyEvent("NOTIFY_QUEST_DONE", npc, player);
						}
						
						if (Rnd.get(100) <= 60)
						{
							giveItems(player, SUPPLY_BOX_BASIC, 1);
						}
						else
						{
							if (Rnd.get(100) <= 70)
							{
								giveItems(player, SUPPLY_BOX_INTERMEDIATE, 1);
							}
							else
							{
								giveItems(player, SUPPLY_BOX_ADVANCED, 1);
							}
						}
						addExpAndSp(player, 5536944000L, 13288590);
						addFactionPoints(player, Faction.MOTHER_TREE_GUARDIANS, REWARD_BASIC);
						showHtmlFile(player, "guardian_leader_q0841_13.htm", npc);
					}
					break;
				}
				case 3102:
				{
					if (qs.getCond() == 5)
					{
						if (hasQuestItems(player, FACTION_AMITY_TOKEN))
						{
							qs.exitQuest(QuestType.DAILY, true);
							
							final Quest qs838 = QuestManager.getInstance().getQuest(Q00838_RequestFromTheMotherTreeGuardians.class.getSimpleName());
							if (qs838 != null)
							{
								qs838.notifyEvent("NOTIFY_QUEST_DONE", npc, player);
							}
							
							takeItems(player, FACTION_AMITY_TOKEN, 1);
							if (Rnd.get(100) <= 60)
							{
								giveItems(player, SUPPLY_BOX_BASIC, 1);
							}
							else
							{
								if (Rnd.get(100) <= 70)
								{
									giveItems(player, SUPPLY_BOX_INTERMEDIATE, 1);
								}
								else
								{
									giveItems(player, SUPPLY_BOX_ADVANCED, 1);
								}
							}
							addExpAndSp(player, 5536944000L * 2, 13288590 * 2);
							addFactionPoints(player, Faction.MOTHER_TREE_GUARDIANS, REWARD_BASIC * 2);
							showHtmlFile(player, "guardian_leader_q0841_13.htm", npc);
							break;
						}
						showHtmlFile(player, "guardian_leader_q0841_14.htm", npc);
					}
					break;
				}
				case 3201:
				{
					if (qs.getCond() == 6)
					{
						qs.exitQuest(QuestType.DAILY, true);
						
						final Quest qs838 = QuestManager.getInstance().getQuest(Q00838_RequestFromTheMotherTreeGuardians.class.getSimpleName());
						if (qs838 != null)
						{
							qs838.notifyEvent("NOTIFY_QUEST_DONE", npc, player);
						}
						
						if (Rnd.get(100) <= 60)
						{
							giveItems(player, SUPPLY_BOX_INTERMEDIATE, 1);
						}
						else
						{
							if (Rnd.get(100) <= 70)
							{
								giveItems(player, SUPPLY_BOX_BASIC, 1);
							}
							else
							{
								giveItems(player, SUPPLY_BOX_ADVANCED, 1);
							}
						}
						addExpAndSp(player, 11073888000L, 26577180);
						addFactionPoints(player, Faction.MOTHER_TREE_GUARDIANS, REWARD_INTERMEDIATE);
						showHtmlFile(player, "guardian_leader_q0841_13a.htm", npc);
					}
					break;
				}
				case 3202:
				{
					if (qs.getCond() == 6)
					{
						if (hasQuestItems(player, FACTION_AMITY_TOKEN))
						{
							qs.exitQuest(QuestType.DAILY, true);
							
							final Quest qs838 = QuestManager.getInstance().getQuest(Q00838_RequestFromTheMotherTreeGuardians.class.getSimpleName());
							if (qs838 != null)
							{
								qs838.notifyEvent("NOTIFY_QUEST_DONE", npc, player);
							}
							
							takeItems(player, FACTION_AMITY_TOKEN, 1);
							if (Rnd.get(100) <= 60)
							{
								giveItems(player, SUPPLY_BOX_INTERMEDIATE, 1);
							}
							else
							{
								if (Rnd.get(100) <= 70)
								{
									giveItems(player, SUPPLY_BOX_BASIC, 1);
								}
								else
								{
									giveItems(player, SUPPLY_BOX_ADVANCED, 1);
								}
							}
							addExpAndSp(player, 11073888000L * 2, 26577180 * 2);
							addFactionPoints(player, Faction.MOTHER_TREE_GUARDIANS, REWARD_INTERMEDIATE * 2);
							showHtmlFile(player, "guardian_leader_q0841_13a.htm", npc);
							break;
						}
						showHtmlFile(player, "guardian_leader_q0841_14a.htm", npc);
					}
					break;
				}
				case 3301:
				{
					if (qs.getCond() == 7)
					{
						qs.exitQuest(QuestType.DAILY, true);
						
						final Quest qs838 = QuestManager.getInstance().getQuest(Q00838_RequestFromTheMotherTreeGuardians.class.getSimpleName());
						if (qs838 != null)
						{
							qs838.notifyEvent("NOTIFY_QUEST_DONE", npc, player);
						}
						
						if (Rnd.get(100) <= 60)
						{
							giveItems(player, SUPPLY_BOX_ADVANCED, 1);
						}
						else
						{
							if (Rnd.get(100) <= 70)
							{
								giveItems(player, SUPPLY_BOX_INTERMEDIATE, 1);
							}
							else
							{
								giveItems(player, SUPPLY_BOX_BASIC, 1);
							}
						}
						addExpAndSp(player, 16610832000L, 39865770);
						addFactionPoints(player, Faction.MOTHER_TREE_GUARDIANS, REWARD_ADVANCED);
						showHtmlFile(player, "guardian_leader_q0841_13b.htm", npc);
					}
					break;
				}
				case 3302:
				{
					if (qs.getCond() == 7)
					{
						if (hasQuestItems(player, FACTION_AMITY_TOKEN))
						{
							qs.exitQuest(QuestType.DAILY, true);
							
							final Quest qs838 = QuestManager.getInstance().getQuest(Q00838_RequestFromTheMotherTreeGuardians.class.getSimpleName());
							if (qs838 != null)
							{
								qs838.notifyEvent("NOTIFY_QUEST_DONE", npc, player);
							}
							
							takeItems(player, FACTION_AMITY_TOKEN, 1);
							if (Rnd.get(100) <= 60)
							{
								giveItems(player, SUPPLY_BOX_ADVANCED, 1);
							}
							else
							{
								if (Rnd.get(100) <= 70)
								{
									giveItems(player, SUPPLY_BOX_INTERMEDIATE, 1);
								}
								else
								{
									giveItems(player, SUPPLY_BOX_BASIC, 1);
								}
							}
							addExpAndSp(player, 16610832000L * 2, 39865770 * 2);
							addFactionPoints(player, Faction.MOTHER_TREE_GUARDIANS, REWARD_ADVANCED * 2);
							showHtmlFile(player, "guardian_leader_q0841_13b.htm", npc);
							break;
						}
						showHtmlFile(player, "guardian_leader_q0841_14b.htm", npc);
					}
					break;
				}
			}
		}
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isStarted() && qs.isCond(1))
		{
			int killedNymphs = qs.getInt("killed_Nymphs");
			switch (qs.getCond())
			{
				case 2:
				{
					if (killedNymphs >= COUNT_BASIC)
					{
						qs.setCond(5, true);
						break;
					}
					qs.set("killed_Nymphs", ++killedNymphs);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					sendNpcLogList(player);
					break;
				}
				case 3:
				{
					if (killedNymphs >= COUNT_INTERMEDIATE)
					{
						qs.setCond(6, true);
						break;
					}
					qs.set("killed_Nymphs", ++killedNymphs);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					sendNpcLogList(player);
					break;
				}
				case 4:
				{
					if (killedNymphs >= COUNT_ADVANCED)
					{
						qs.setCond(7, true);
						break;
					}
					qs.set("killed_Nymphs", ++killedNymphs);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					sendNpcLogList(player);
					break;
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	@Override
	public Set<NpcLogListHolder> getNpcLogList(Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(1))
		{
			final Set<NpcLogListHolder> holder = new HashSet<>();
			holder.add(new NpcLogListHolder(NpcStringId.DEFEAT_THE_ENRAGED_NYMPH, qs.getInt("killed_Nymphs"))); // Defeat the Enraged Nymph
			return holder;
		}
		return super.getNpcLogList(player);
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public final void onLogin(OnPlayerLogin evt)
	{
		final Player player = evt.getPlayer();
		sendNpcLogList(player);
	}
}
