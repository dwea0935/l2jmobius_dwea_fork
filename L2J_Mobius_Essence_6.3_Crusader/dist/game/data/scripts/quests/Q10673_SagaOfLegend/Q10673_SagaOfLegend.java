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
package quests.Q10673_SagaOfLegend;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.xml.CategoryData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerProfessionChange;
import org.l2jmobius.gameserver.model.quest.NpcLogListHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.classchange.ExClassChangeSetAlarm;

/**
 * Saga of Legend (10673)
 * @URL https://l2wiki.com/classic/Saga_of_Legend
 * @TODO: Retail htmls.
 * @author Dmitri, Mobius, Manax
 */
public class Q10673_SagaOfLegend extends Quest
{
	// NPCs
	private static final int ORVEN = 30857;
	// Monsters
	private static final int[] MOBS =
	{
		// Cruma Tower 2nd Floor
		22206, // Premo
		22207, // Validus
		22209, // Perum
		22210, // Torfe
		22211, // Death Lord
		20220, // Dicor
		// Sillent Valley
		20965, // Chimera Piece 72
		20966, // Mutated Creation 74
		20967, // Creature of the Past 75
		20968, // Forgotten Face 75
		20969, // Giant's Shadow 75
		20970, // Soldier of Ancient Times 73
		20971, // Warrior of Ancient Times 74
		20973, // Forgotten Ancient People 75
		20972, // Shaman of Ancient Times 75
		// Plains Of Lizardmen
		22151, // Tanta Lizardmen
		22152, // Tanta Lizardmen Warrior
		22153, // Tanta Lizardmen Berserker
		22154, // Tanta Lizardmen Archer
		22155, // Tanta Lizardmen Summoner
	};
	// Rewards
	private static final int MAGICAL_TABLET = 90045;
	private static final int SPELLBOOK_HUMAN = 90038; // Spellbook: Mount Golden Lion
	private static final int SPELLBOOK_ELF = 90039; // Spellbook: Mount Pegasus
	private static final int SPELLBOOK_DELF = 90040; // Spellbook: Mount Saber Tooth Cougar
	private static final int SPELLBOOK_ORC = 90042; // Spellbook: Mount Black Bear
	private static final int SPELLBOOK_DWARF = 90041; // Spellbook: Mount Kukuru
	private static final int SPELLBOOK_KAMAEL = 91946; // Spellbook: Mount Griffin
	private static final int SPELLBOOK_DEATH_KNIGHT = 93383; // Spellbook: Mount Nightmare Steed
	private static final int SPELLBOOK_SYLPH = 95367; // Spellbook: Mount Elemental Lyn Draco
	// Misc
	private static final int MIN_LEVEL = 76;
	private static final String KILL_COUNT_VAR = "KillCount";
	
	public Q10673_SagaOfLegend()
	{
		super(10673);
		addStartNpc(ORVEN);
		addTalkId(ORVEN);
		addKillId(MOBS);
		addCondMinLevel(MIN_LEVEL, "30857-00.htm");
		addCondInCategory(CategoryType.THIRD_CLASS_GROUP, "30857-00.htm");
		setQuestNameNpcStringId(NpcStringId.LV_76_SAGA_OF_LEGEND);
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
			case "30857-02.htm":
			case "30857-03.htm":
			case "30857-04.htm":
			case "30857-06.html":
			{
				htmltext = event;
				break;
			}
			case "30857-05.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "30857-07.html":
			{
				qs.setCond(2, true);
				htmltext = event;
				break;
			}
			case "30857-07a.html":
			{
				qs.setCond(3, true);
				htmltext = event;
				break;
			}
			case "30857-07b.html":
			{
				qs.setCond(4, true);
				htmltext = event;
				break;
			}
			case "30857-10.html":
			{
				if (qs.isCond(5))
				{
					giveItems(player, MAGICAL_TABLET, 10);
					qs.exitQuest(false, true);
					if (CategoryData.getInstance().isInCategory(CategoryType.THIRD_CLASS_GROUP, player.getPlayerClass().getId()))
					{
						player.sendPacket(ExClassChangeSetAlarm.STATIC_PACKET);
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
		switch (qs.getState())
		{
			case State.CREATED:
			{
				htmltext = "30857-01.htm";
				break;
			}
			case State.STARTED:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						htmltext = "30857-05.htm";
						break;
					}
					case 2:
					case 3:
					case 4:
					{
						htmltext = "30857-08.html";
						break;
					}
					case 5:
					{
						htmltext = "30857-09.html";
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
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && ((qs.getCond() > 1) && (qs.getCond() < 5)))
		{
			final int killCount = qs.getInt(KILL_COUNT_VAR) + 1;
			if (killCount < 700)
			{
				qs.set(KILL_COUNT_VAR, killCount);
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				sendNpcLogList(killer);
			}
			else
			{
				qs.setCond(5, true);
				qs.unset(KILL_COUNT_VAR);
			}
		}
	}
	
	@Override
	public Set<NpcLogListHolder> getNpcLogList(Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && (qs.getCond() > 1))
		{
			final Set<NpcLogListHolder> holder = new HashSet<>();
			holder.add(new NpcLogListHolder(NpcStringId.ORVEN_S_REQUEST_2.getId(), true, qs.getInt(KILL_COUNT_VAR)));
			return holder;
		}
		return super.getNpcLogList(player);
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogin(OnPlayerLogin event)
	{
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!CategoryData.getInstance().isInCategory(CategoryType.THIRD_CLASS_GROUP, player.getPlayerClass().getId()))
		{
			return;
		}
		
		final QuestState qs = getQuestState(player, false);
		if (Config.DISABLE_TUTORIAL || ((qs != null) && qs.isCompleted()))
		{
			player.sendPacket(ExClassChangeSetAlarm.STATIC_PACKET);
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_PROFESSION_CHANGE)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onProfessionChange(OnPlayerProfessionChange event)
	{
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!CategoryData.getInstance().isInCategory(CategoryType.FOURTH_CLASS_GROUP, player.getPlayerClass().getId()))
		{
			return;
		}
		
		// Avoid reward more than once.
		if (player.getVariables().getBoolean("3rdClassMountRewarded", false))
		{
			return;
		}
		
		// Death Knights.
		if (player.isDeathKnight())
		{
			player.getVariables().set("3rdClassMountRewarded", true);
			giveItems(player, SPELLBOOK_DEATH_KNIGHT, 1);
			return;
		}
		
		// Vanguard.
		if (player.isVanguard())
		{
			return;
		}
		
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCompleted())
		{
			player.getVariables().set("3rdClassMountRewarded", true);
			
			switch (player.getRace())
			{
				case ELF:
				{
					giveItems(player, SPELLBOOK_ELF, 1);
					break;
				}
				case DARK_ELF:
				{
					giveItems(player, SPELLBOOK_DELF, 1);
					break;
				}
				case ORC:
				{
					giveItems(player, SPELLBOOK_ORC, 1);
					break;
				}
				case DWARF:
				{
					giveItems(player, SPELLBOOK_DWARF, 1);
					break;
				}
				case KAMAEL:
				{
					giveItems(player, SPELLBOOK_KAMAEL, 1);
					break;
				}
				case HUMAN:
				{
					giveItems(player, SPELLBOOK_HUMAN, 1);
					break;
				}
				case SYLPH:
				{
					giveItems(player, SPELLBOOK_SYLPH, 1);
					break;
				}
			}
		}
	}
}