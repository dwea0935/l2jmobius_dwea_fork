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
package quests.Q00114_ResurrectionOfAnOldManager;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;

import quests.Q00121_PavelTheGiant.Q00121_PavelTheGiant;

/**
 * Resurrection of an Old Manager (114)<br>
 * Original Jython script by Kerberos
 * @author malyelfik
 */
public class Q00114_ResurrectionOfAnOldManager extends Quest
{
	// NPCs
	private static final int NEWYEAR = 31961;
	private static final int YUMI = 32041;
	private static final int STONES = 32046;
	private static final int WENDY = 32047;
	private static final int BOX = 32050;
	// Items
	private static final int STARSTONE = 8287;
	private static final int LETTER = 8288;
	private static final int STARSTONE2 = 8289;
	private static final int DETCTOR = 8090;
	private static final int DETCTOR2 = 8091;
	// Monster
	private static final int GUARDIAN = 27318;
	
	private static Attackable golem = null;
	
	public Q00114_ResurrectionOfAnOldManager()
	{
		super(114, "Resurrection of an Old Manager");
		addStartNpc(YUMI);
		addTalkId(YUMI, WENDY, BOX, STONES, NEWYEAR);
		addKillId(GUARDIAN);
		addCreatureSeeId(STONES);
		registerQuestItems(STARSTONE, STARSTONE2, DETCTOR, DETCTOR2, LETTER);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = event;
		switch (event)
		{
			case "32041-04.htm":
			{
				qs.startQuest();
				break;
			}
			case "32041-08.html":
			{
				qs.set("talk", "1");
				break;
			}
			case "32041-09.html":
			{
				qs.setCond(2, true);
				qs.unset("talk");
				break;
			}
			case "32041-12.html":
			{
				switch (qs.getCond())
				{
					case 3:
					{
						htmltext = "32041-12.html";
						break;
					}
					case 4:
					{
						htmltext = "32041-13.html";
						break;
					}
					case 5:
					{
						htmltext = "32041-14.html";
						break;
					}
				}
				break;
			}
			case "32041-15.html":
			{
				qs.set("talk", "1");
				break;
			}
			case "32041-23.html":
			{
				qs.set("talk", "2");
				break;
			}
			case "32041-26.html":
			{
				qs.setCond(6, true);
				qs.unset("talk");
				break;
			}
			case "32041-31.html":
			{
				giveItems(player, DETCTOR, 1);
				qs.setCond(17, true);
				break;
			}
			case "32041-34.html":
			{
				qs.set("talk", "1");
				takeItems(player, DETCTOR2, 1);
				break;
			}
			case "32041-38.html":
			{
				if (qs.getInt("choice") == 2)
				{
					htmltext = "32041-37.html";
				}
				break;
			}
			case "32041-39.html":
			{
				qs.unset("talk");
				qs.setCond(20, true);
				break;
			}
			case "32041-40.html":
			{
				qs.setCond(21, true);
				qs.unset("talk");
				giveItems(player, LETTER, 1);
				break;
			}
			case "32046-03.html":
			{
				qs.setCond(19, true);
				break;
			}
			case "32046-07.html":
			{
				addExpAndSp(player, 1846611, 144270);
				qs.exitQuest(false, true);
				break;
			}
			case "32047-02.html":
			{
				if (qs.getInt("talk") == 0)
				{
					qs.set("talk", "1");
				}
				break;
			}
			case "32047-03.html":
			{
				if (qs.getInt("talk1") == 0)
				{
					qs.set("talk1", "1");
				}
				break;
			}
			case "32047-05.html":
			{
				if ((qs.getInt("talk") == 0) || (qs.getInt("talk1") == 0))
				{
					htmltext = "32047-04.html";
				}
				break;
			}
			case "32047-06.html":
			{
				qs.set("choice", "1");
				qs.setCond(3, true);
				qs.unset("talk1");
				qs.unset("talk");
				break;
			}
			case "32047-07.html":
			{
				qs.set("choice", "2");
				qs.setCond(4, true);
				qs.unset("talk1");
				qs.unset("talk");
				break;
			}
			case "32047-09.html":
			{
				qs.set("choice", "3");
				qs.setCond(5, true);
				qs.unset("talk1");
				qs.unset("talk");
				break;
			}
			case "32047-14ab.html":
			{
				qs.set("choice", "3");
				qs.setCond(7, true);
				break;
			}
			case "32047-14b.html":
			{
				qs.setCond(10, true);
				break;
			}
			case "32047-15b.html":
			{
				if ((golem == null) || golem.isDead())
				{
					golem = addSpawn(GUARDIAN, 96977, -110625, -3280, 0, false, 0).asAttackable();
					golem.broadcastPacket(new NpcSay(golem.getObjectId(), ChatType.NPC_GENERAL, golem.getId(), "You, " + player.getName() + ", you attacked Wendy. Prepare to die!"));
					golem.setRunning();
					golem.addDamageHate(player, 0, 999);
					golem.getAI().setIntention(Intention.ATTACK, player);
					qs.set("spawned", "1");
					startQuestTimer("golem_despawn", 300000, null, player);
				}
				else if (qs.getInt("spawned") == 1)
				{
					htmltext = "32047-17b.html";
				}
				else
				{
					htmltext = "32047-16b.html";
				}
				break;
			}
			case "32047-20a.html":
			{
				qs.setCond(8, true);
				break;
			}
			case "32047-20b.html":
			{
				qs.setCond(12, true);
				break;
			}
			case "32047-20c.html":
			{
				qs.setCond(13, true);
				break;
			}
			case "32047-21a.html":
			{
				qs.setCond(9, true);
				break;
			}
			case "32047-23a.html":
			{
				qs.setCond(23, true);
				break;
			}
			case "32047-23c.html":
			{
				takeItems(player, STARSTONE, 1);
				qs.setCond(15, true);
				break;
			}
			case "32047-29c.html":
			{
				if (player.getAdena() >= 3000)
				{
					giveItems(player, STARSTONE2, 1);
					takeItems(player, Inventory.ADENA_ID, 3000);
					qs.unset("talk");
					qs.setCond(26, true);
				}
				else
				{
					htmltext = "32047-29ca.html";
				}
				break;
			}
			case "32047-30c.html":
			{
				qs.set("talk", "1");
				break;
			}
			case "32050-01r.html":
			{
				qs.set("talk", "1");
				break;
			}
			case "32050-03.html":
			{
				giveItems(player, STARSTONE, 1);
				qs.setCond(14, true);
				qs.unset("talk");
				break;
			}
			case "32050-05.html":
			{
				qs.setCond(24, true);
				giveItems(player, STARSTONE2, 1);
				break;
			}
			case "31961-02.html":
			{
				takeItems(player, LETTER, 1);
				giveItems(player, STARSTONE2, 1);
				qs.setCond(22, true);
				break;
			}
			case "golem_despawn":
			{
				qs.unset("spawned");
				golem.broadcastPacket(new NpcSay(golem.getObjectId(), ChatType.NPC_GENERAL, golem.getId(), player.getName() + ", your enemy was driven out. I will now withdraw and await your next command."));
				golem.deleteMe();
				golem = null;
				htmltext = null;
				break;
			}
			case "32041-05.html":
			case "32041-06.html":
			case "32041-07.html":
			case "32041-17.html":
			case "32041-18.html":
			case "32041-19.html":
			case "32041-20.html":
			case "32041-21.html":
			case "32041-22.html":
			case "32041-25.html":
			case "32041-29.html":
			case "32041-30.html":
			case "32041-35.html":
			case "32041-36.html":
			case "32046-05.html":
			case "32046-06.html":
			case "32047-06a.html":
			case "32047-12a.html":
			case "32047-12b.html":
			case "32047-12c.html":
			case "32047-13a.html":
			case "32047-14a.html":
			case "32047-13b.html":
			case "32047-13c.html":
			case "32047-14c.html":
			case "32047-15c.html":
			case "32047-17c.html":
			case "32047-13ab.html":
			case "32047-15a.html":
			case "32047-16a.html":
			case "32047-16c.html":
			case "32047-18a.html":
			case "32047-19a.html":
			case "32047-18ab.html":
			case "32047-19ab.html":
			case "32047-18c.html":
			case "32047-17a.html":
			case "32047-19c.html":
			case "32047-21b.html":
			case "32047-27c.html":
			case "32047-28c.html":
			{
				break;
			}
			default:
			{
				htmltext = null;
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(10) && (qs.getInt("spawned") == 1))
		{
			npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), "This enemy is far too powerful for me to fight. I must withdraw."));
			qs.setCond(11, true);
			qs.unset("spawned");
			cancelQuestTimers("golem_despawn");
		}
	}
	
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			final QuestState qs = getQuestState(player, false);
			if ((qs != null) && qs.isCond(17))
			{
				takeItems(creature.asPlayer(), DETCTOR, 1);
				giveItems(creature.asPlayer(), DETCTOR2, 1);
				qs.setCond(18, true);
				showOnScreenMsg(player, "The radio signal detector is responding. A suspicious pile of stones catches your eye.", 2, 4500);
			}
		}
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		final int talk = qs.getInt("talk");
		
		switch (npc.getId())
		{
			case YUMI:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						final QuestState prev = player.getQuestState(Q00121_PavelTheGiant.class.getSimpleName());
						if ((prev != null) && prev.isCompleted())
						{
							htmltext = (player.getLevel() >= 70) ? "32041-02.htm" : "32041-03.htm";
						}
						else
						{
							htmltext = "32041-01.htm";
						}
						break;
					}
					case State.STARTED:
					{
						switch (qs.getCond())
						{
							case 1:
							{
								htmltext = (talk == 1) ? "32041-08.html" : "32041-04.htm";
								break;
							}
							case 2:
							{
								htmltext = "32041-10.html";
								break;
							}
							case 3:
							case 4:
							case 5:
							{
								switch (talk)
								{
									case 0:
									{
										htmltext = "32041-11.html";
										break;
									}
									case 1:
									{
										htmltext = "32041-16.html";
										break;
									}
									case 2:
									{
										htmltext = "32041-24.html";
										break;
									}
								}
								break;
							}
							case 6:
							case 7:
							case 8:
							case 10:
							case 11:
							case 13:
							case 14:
							case 15:
							{
								htmltext = "32041-27.html";
								break;
							}
							case 9:
							case 12:
							case 16:
							{
								htmltext = "32041-28.html";
								break;
							}
							case 17:
							case 18:
							{
								htmltext = "32041-32.html";
								break;
							}
							case 19:
							{
								htmltext = (talk == 1) ? "32041-34z.html" : "32041-33.html";
								break;
							}
							case 20:
							{
								htmltext = "32041-39z.html";
								break;
							}
							case 21:
							{
								htmltext = "32041-40z.html";
								break;
							}
							case 22:
							case 25:
							case 26:
							{
								qs.setCond(27, true);
								htmltext = "32041-41.html";
								break;
							}
							case 27:
							{
								htmltext = "32041-42.html";
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
				break;
			}
			case WENDY:
			{
				if (qs.isStarted())
				{
					switch (qs.getCond())
					{
						case 2:
						{
							htmltext = ((talk == 1) && (qs.getInt("talk1") == 1)) ? "32047-05.html" : "32047-01.html";
							break;
						}
						case 3:
						{
							htmltext = "32047-06b.html";
							break;
						}
						case 4:
						{
							htmltext = "32047-08.html";
							break;
						}
						case 5:
						{
							htmltext = "32047-10.html";
							break;
						}
						case 6:
						{
							switch (qs.getInt("choice"))
							{
								case 1:
								{
									htmltext = "32047-11a.html";
									break;
								}
								case 2:
								{
									htmltext = "32047-11b.html";
									break;
								}
								case 3:
								{
									htmltext = "32047-11c.html";
									break;
								}
							}
							break;
						}
						case 7:
						{
							htmltext = "32047-11c.html";
							break;
						}
						case 8:
						{
							htmltext = "32047-17a.html";
							break;
						}
						case 9:
						case 12:
						case 16:
						{
							htmltext = "32047-25c.html";
							break;
						}
						case 10:
						{
							htmltext = "32047-18b.html";
							break;
						}
						case 11:
						{
							htmltext = "32047-19b.html";
							break;
						}
						case 13:
						{
							htmltext = "32047-21c.html";
							break;
						}
						case 14:
						{
							htmltext = "32047-22c.html";
							break;
						}
						case 15:
						{
							qs.setCond(16, true);
							htmltext = "32047-24c.html";
							break;
						}
						case 20:
						{
							if (qs.getInt("choice") == 1)
							{
								htmltext = "32047-22a.html";
							}
							else
							{
								htmltext = (talk == 1) ? "32047-31c.html" : "32047-26c.html";
							}
							break;
						}
						case 23:
						{
							htmltext = "32047-23z.html";
							break;
						}
						case 24:
						{
							qs.setCond(25, true);
							htmltext = "32047-24a.html";
							break;
						}
						case 25:
						{
							htmltext = "32047-24a.html";
							break;
						}
						case 26:
						{
							htmltext = "32047-32c.html";
							break;
						}
					}
				}
				break;
			}
			case NEWYEAR:
			{
				if (qs.isStarted())
				{
					switch (qs.getCond())
					{
						case 21:
						{
							htmltext = "31961-01.html";
							break;
						}
						case 22:
						{
							htmltext = "31961-03.html";
							break;
						}
					}
				}
				break;
			}
			case BOX:
			{
				if (qs.isStarted())
				{
					switch (qs.getCond())
					{
						case 13:
						{
							htmltext = (talk == 1) ? "32050-02.html" : "32050-01.html";
							break;
						}
						case 14:
						{
							htmltext = "32050-04.html";
							break;
						}
						case 23:
						{
							htmltext = "32050-04b.html";
							break;
						}
						case 24:
						{
							htmltext = "32050-05z.html";
							break;
						}
					}
				}
				break;
			}
			case STONES:
			{
				if (qs.isStarted())
				{
					switch (qs.getCond())
					{
						case 18:
						{
							htmltext = "32046-02.html";
							break;
						}
						case 19:
						{
							htmltext = "32046-03.html";
							break;
						}
						case 27:
						{
							htmltext = "32046-04.html";
							break;
						}
					}
				}
				break;
			}
		}
		return htmltext;
	}
}