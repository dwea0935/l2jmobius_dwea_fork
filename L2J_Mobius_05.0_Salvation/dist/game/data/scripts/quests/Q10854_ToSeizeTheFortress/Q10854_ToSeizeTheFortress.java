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
package quests.Q10854_ToSeizeTheFortress;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.enums.Faction;
import org.l2jmobius.gameserver.enums.QuestSound;
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

/**
 * To Seize the Fortress (10854)
 * @author Kazumi
 */
public final class Q10854_ToSeizeTheFortress extends Quest
{
	// NPCs
	private static final int VAN_DYKE = 34235;
	private static final int BARTON = 34059;
	private static final int HAYUK = 34060;
	private static final int ELISE = 34061;
	private static final int ELIYAH = 34062;
	// Mobs
	private static final int GEORK = 23586;
	private static final int QUARTERMASTER = 23588;
	private static final int BURNSTEIN = 26136;
	// Items
	private static final int REPORT_BARTON = 47193;
	private static final int REPORT_HAYUK = 47194;
	private static final int REPORT_ELISE = 47195;
	private static final int REPORT_ELIYAH = 47196;
	private static final int RUNE_STONE = 39738;
	private static final int SPELLBOOK_WAR_HORSE = 47149;
	// Misc
	private static final int MIN_LEVEL = 101;
	private static boolean _bartonReport = false;
	private static boolean _hayukReport = false;
	private static boolean _eliseReport = false;
	private static boolean _eliyahReport = false;
	
	public Q10854_ToSeizeTheFortress()
	{
		super(10854);
		addStartNpc(VAN_DYKE);
		addTalkId(VAN_DYKE, BARTON, HAYUK, ELISE, ELIYAH);
		addKillId(GEORK, QUARTERMASTER);
		addCondMinLevel(MIN_LEVEL, "royal_maestre_q10854_02.htm");
		addFactionLevel(Faction.KINGDOM_ROYAL_GUARDS, 10, "royal_maestre_q10854_03.htm");
		registerQuestItems(REPORT_BARTON, REPORT_HAYUK, REPORT_ELISE, REPORT_ELIYAH);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		String htmltext = event;
		if (qs == null)
		{
			return null;
		}
		
		if (event.equals("quest_accept"))
		{
			htmltext = "royal_maestre_q10854_06.htm";
			qs.startQuest();
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
				if (npc.getId() == VAN_DYKE)
				{
					if (player.getFactionLevel(Faction.KINGDOM_ROYAL_GUARDS) >= 10)
					{
						if (player.getLevel() >= MIN_LEVEL)
						{
							htmltext = "royal_maestre_q10854_01.htm";
							break;
						}
						htmltext = "royal_maestre_q10854_02.htm";
						break;
					}
					htmltext = "royal_maestre_q10854_03.htm";
					break;
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case VAN_DYKE:
					{
						switch (qs.getCond())
						{
							case 1:
							{
								if (hasQuestItems(player, REPORT_BARTON) && hasQuestItems(player, REPORT_HAYUK) && hasQuestItems(player, REPORT_ELISE) && hasQuestItems(player, REPORT_ELIYAH))
								{
									qs.setCond(2);
									htmltext = "royal_maestre_q10854_08.htm";
									break;
								}
								htmltext = "royal_maestre_q10854_07.htm";
								break;
							}
							case 2:
							{
								htmltext = "royal_maestre_q10854_08.htm";
								break;
							}
							case 3:
							{
								qs.setCond(4);
								htmltext = "royal_maestre_q10854_09.htm";
								break;
							}
							case 4:
							{
								htmltext = "royal_maestre_q10854_12.htm";
								break;
							}
							case 5:
							{
								htmltext = "royal_maestre_q10854_10.htm";
								break;
							}
						}
						break;
					}
					case BARTON:
					{
						if (qs.isCond(1))
						{
							if (hasQuestItems(player, REPORT_BARTON))
							{
								htmltext = "member_barton_q10854_04.htm";
								break;
							}
							if (!_bartonReport)
							{
								giveItems(player, REPORT_BARTON, 1);
								htmltext = "member_barton_q10854_03.htm";
								break;
							}
							htmltext = "member_barton_q10854_01.htm";
						}
						break;
					}
					case HAYUK:
					{
						if (qs.isCond(1))
						{
							if (hasQuestItems(player, REPORT_HAYUK))
							{
								htmltext = "member_hayuk_q10854_04.htm";
								break;
							}
							if (!_hayukReport)
							{
								giveItems(player, REPORT_HAYUK, 1);
								htmltext = "member_hayuk_q10854_03.htm";
								break;
							}
							htmltext = "member_hayuk_q10854_01.htm";
						}
						break;
					}
					case ELISE:
					{
						if (qs.isCond(1))
						{
							if (hasQuestItems(player, REPORT_ELISE))
							{
								htmltext = "member_alice_q10854_04.htm";
								break;
							}
							if (!_eliseReport)
							{
								giveItems(player, REPORT_ELISE, 1);
								htmltext = "member_alice_q10854_03.htm";
								break;
							}
							htmltext = "member_alice_q10854_01.htm";
						}
						break;
					}
					case ELIYAH:
					{
						if (qs.isCond(1))
						{
							if (hasQuestItems(player, REPORT_ELIYAH))
							{
								htmltext = "member_elliyah_q10854_04.htm";
								break;
							}
							if (!_eliyahReport)
							{
								giveItems(player, REPORT_ELIYAH, 1);
								htmltext = "member_elliyah_q10854_03.htm";
								break;
							}
							htmltext = "member_elliyah_q10854_01.htm";
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
	
	@RegisterEvent(EventType.ON_NPC_MENU_SELECT)
	@RegisterType(ListenerRegisterType.NPC)
	@Id(VAN_DYKE)
	@Id(BARTON)
	@Id(HAYUK)
	@Id(ELISE)
	@Id(ELIYAH)
	public final void onNpcMenuSelect(OnNpcMenuSelect event)
	{
		final Player player = event.getTalker();
		final QuestState qs = getQuestState(player, false);
		final Npc npc = event.getNpc();
		final int ask = event.getAsk();
		final int reply = event.getReply();
		
		if (ask == 10854)
		{
			switch (reply)
			{
				case 1:
				{
					switch (npc.getId())
					{
						case VAN_DYKE:
						{
							showHtmlFile(player, "royal_maestre_q10854_04.htm", npc);
							break;
						}
						case BARTON:
						{
							giveItems(player, REPORT_BARTON, 1);
							_bartonReport = true;
							showHtmlFile(player, "member_barton_q10854_02.htm", npc);
							break;
						}
						case HAYUK:
						{
							giveItems(player, REPORT_HAYUK, 1);
							_hayukReport = true;
							showHtmlFile(player, "member_hayuk_q10854_02.htm", npc);
							break;
						}
						case ELISE:
						{
							giveItems(player, REPORT_ELISE, 1);
							_eliseReport = true;
							showHtmlFile(player, "member_alice_q10854_02.htm", npc);
							break;
						}
						case ELIYAH:
						{
							giveItems(player, REPORT_ELIYAH, 1);
							_eliyahReport = true;
							showHtmlFile(player, "member_elliyah_q10854_02.htm", npc);
							break;
						}
					}
					break;
				}
				case 2:
				{
					showHtmlFile(player, "royal_maestre_q10854_05.htm", npc);
					break;
				}
				case 10:
				{
					qs.exitQuest(false, true);
					addExpAndSp(player, 203344446600L, 488025000);
					giveItems(player, RUNE_STONE, 1);
					giveItems(player, SPELLBOOK_WAR_HORSE, 1);
					showHtmlFile(player, "royal_maestre_q10854_11.htm", npc);
					break;
				}
			}
		}
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isStarted())
		{
			if (qs.isCond(2))
			{
				int killedGeork = qs.getInt("killed_" + GEORK);
				int killedHummel = qs.getInt("killed_" + QUARTERMASTER);
				switch (npc.getId())
				{
					case GEORK:
					{
						if (killedGeork < 1)
						{
							qs.set("killed_" + GEORK, 1);
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
						sendNpcLogList(player);
						break;
					}
					case QUARTERMASTER:
					{
						if (killedHummel < 1)
						{
							qs.set("killed_" + GEORK, 1);
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
						sendNpcLogList(player);
						break;
					}
				}
				
				if ((killedGeork == 1) && (killedHummel == 1))
				{
					qs.setCond(3, true);
				}
			}
			else if (qs.isCond(4))
			{
				if (npc.getId() == BURNSTEIN)
				{
					qs.setCond(5, true);
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	@Override
	public Set<NpcLogListHolder> getNpcLogList(Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(2))
		{
			final Set<NpcLogListHolder> holder = new HashSet<>();
			holder.add(new NpcLogListHolder(GEORK, false, qs.getInt("killed_" + GEORK)));
			holder.add(new NpcLogListHolder(QUARTERMASTER, false, qs.getInt("killed_" + QUARTERMASTER)));
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
