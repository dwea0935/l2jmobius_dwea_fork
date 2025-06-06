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
package instances.MonsterArena;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanMember;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerClanLeft;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.ExSendUIEvent;

import instances.AbstractInstance;

/**
 * @author Mobius
 * @URL https://l2wiki.com/classic/Clan_-_Clan_Arena
 */
public class MonsterArena extends AbstractInstance
{
	// NPCs
	private static final int LEO = 30202;
	private static final int MACHINE = 30203;
	private static final int SUPPLIES = 30204;
	private static final int[] BOSSES =
	{
		25794, // Kutis
		25795, // Garan
		25796, // Batur
		25797, // Venir
		25798, // Oel
		25799, // Taranka
		25800, // Kasha
		25801, // Dorak
		25802, // Turan
		25803, // Varkan
		25804, // Ketran
		25805, // Death Lord Likan
		25806, // Anbarad
		25807, // Baranos
		25808, // Takuran
		25809, // Nast
		25810, // Keltar
		25811, // Satur
		25812, // Kosnak
		25813, // Garaki
		25834, // Shadai
		25835, // Tyrobait
		25836, // Tier
		25837, // Cherkia
		25838, // Spicula
	};
	// Rewards
	private static final int BATTLE_BOX_1 = 70917;
	private static final int BATTLE_BOX_2 = 70918;
	private static final int BATTLE_BOX_3 = 70919;
	private static final int BATTLE_BOX_4 = 70920;
	private static final int TICKET_L = 90945;
	private static final int TICKET_M = 90946;
	private static final int TICKET_H = 90947;
	// Skill
	private static final int CLAN_EXUBERANCE = 1867;
	// Misc
	private static final Collection<Player> REWARDED_PLAYERS = ConcurrentHashMap.newKeySet();
	private static final int TEMPLATE_ID = 192;
	
	public MonsterArena()
	{
		super(TEMPLATE_ID);
		addStartNpc(LEO, MACHINE, SUPPLIES);
		addFirstTalkId(LEO, MACHINE, SUPPLIES);
		addTalkId(LEO, MACHINE, SUPPLIES);
		addKillId(BOSSES);
		addInstanceLeaveId(TEMPLATE_ID);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "30202-01.htm":
			case "30202-02.htm":
			case "30202-03.htm":
			case "30203-01.htm":
			{
				return event;
			}
			case "enter_monster_arena":
			{
				// If you died, you may return to the arena.
				final Clan clan = player.getClan();
				if ((clan != null) && (player.getCommandChannel() != null))
				{
					for (Player member : player.getCommandChannel().getMembers())
					{
						final Instance world = member.getInstanceWorld();
						if ((world != null) && (world.getTemplateId() == TEMPLATE_ID) && (world.getPlayersCount() < 40) && (player.getClanId() == member.getClanId()))
						{
							player.teleToLocation(world.getNpc(MACHINE), true, world);
							if ((world.getStatus() > 0) && (world.getStatus() < 5)) // Show remaining countdown.
							{
								player.sendPacket(new ExSendUIEvent(player, false, false, (int) (world.getRemainingTime() / 1000), 0, NpcStringId.REMAINING_TIME));
							}
							return null;
						}
					}
				}
				
				// Clan checks.
				if ((clan == null) || (clan.getLeaderId() != player.getObjectId()) || (player.getCommandChannel() == null))
				{
					return "30202-03.htm";
				}
				if (clan.getLevel() < 3)
				{
					player.sendMessage("Your clan must be at least level 3.");
					return null;
				}
				for (Player member : player.getCommandChannel().getMembers())
				{
					if ((member.getClan() == null) || (member.getClanId() != player.getClanId()))
					{
						player.sendMessage("Your command channel must be consisted only by clan members.");
						return null;
					}
				}
				
				enterInstance(player, npc, TEMPLATE_ID);
				
				final Instance world = player.getInstanceWorld();
				if (world != null)
				{
					final Npc machine = world.getNpc(MACHINE);
					machine.setScriptValue(player.getClanId());
					
					// Initialize progress if it does not exist.
					if (GlobalVariablesManager.getInstance().getInt(GlobalVariablesManager.MONSTER_ARENA_VARIABLE + machine.getScriptValue(), -1) == -1)
					{
						GlobalVariablesManager.getInstance().set(GlobalVariablesManager.MONSTER_ARENA_VARIABLE + machine.getScriptValue(), 1);
					}
					
					// On max progress, set last four bosses.
					final int progress = GlobalVariablesManager.getInstance().getInt(GlobalVariablesManager.MONSTER_ARENA_VARIABLE + machine.getScriptValue());
					if (progress > 17)
					{
						GlobalVariablesManager.getInstance().set(GlobalVariablesManager.MONSTER_ARENA_VARIABLE + machine.getScriptValue(), 17);
					}
					
					startQuestTimer("machine_talk", 10000, machine, null);
					startQuestTimer("start_countdown", 60000, machine, null);
					startQuestTimer("next_spawn", 60000, machine, null);
				}
				break;
			}
			case "machine_talk":
			{
				final Instance world = npc.getInstanceWorld();
				if (world != null)
				{
					npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.WELCOME_TO_THE_ARENA_TEST_YOUR_CLAN_S_STRENGTH);
				}
				break;
			}
			case "start_countdown":
			{
				final Instance world = npc.getInstanceWorld();
				if (world != null)
				{
					world.setStatus(1);
					for (Player plr : world.getPlayers())
					{
						plr.sendPacket(new ExSendUIEvent(plr, false, false, 1200, 0, NpcStringId.REMAINING_TIME));
					}
				}
				break;
			}
			case "next_spawn":
			{
				final Instance world = npc.getInstanceWorld();
				if (world != null)
				{
					world.spawnGroup("boss_" + GlobalVariablesManager.getInstance().getInt(GlobalVariablesManager.MONSTER_ARENA_VARIABLE + npc.getScriptValue()));
				}
				break;
			}
			case "supply_reward":
			{
				final Instance world = npc.getInstanceWorld();
				if ((world != null) && (npc.getId() == SUPPLIES) && (player.getLevel() > 39) && !REWARDED_PLAYERS.contains(player) && npc.isScriptValue(0))
				{
					npc.setScriptValue(1);
					npc.doDie(npc);
					REWARDED_PLAYERS.add(player);
					ThreadPool.schedule(() ->
					{
						REWARDED_PLAYERS.remove(player);
					}, 60000);
					
					// Mandatory reward.
					final Npc machine = world.getNpc(MACHINE);
					final int progress = GlobalVariablesManager.getInstance().getInt(GlobalVariablesManager.MONSTER_ARENA_VARIABLE + machine.getScriptValue());
					if (progress > 16)
					{
						giveItems(player, BATTLE_BOX_4, 1);
					}
					else if (progress > 11)
					{
						giveItems(player, BATTLE_BOX_3, 1);
					}
					else if (progress > 6)
					{
						giveItems(player, BATTLE_BOX_2, 1);
					}
					else
					{
						giveItems(player, BATTLE_BOX_1, 1);
					}
					
					// Rare reward.
					if (getRandom(100) < 1) // 1% chance.
					{
						giveItems(player, TICKET_L, 1);
					}
					else if (getRandom(100) < 1) // 1% chance.
					{
						giveItems(player, TICKET_M, 1);
					}
					else if (getRandom(100) < 1) // 1% chance.
					{
						giveItems(player, TICKET_H, 1);
					}
				}
				break;
			}
			case "remove_supplies":
			{
				final Instance world = npc.getInstanceWorld();
				if (world != null)
				{
					for (Npc aliveNpc : world.getAliveNpcs())
					{
						if ((aliveNpc != null) && (aliveNpc.getId() == SUPPLIES))
						{
							aliveNpc.deleteMe();
						}
					}
				}
				break;
			}
		}
		return null;
	}
	
	@Override
	public void onInstanceLeave(Player player, Instance instance)
	{
		player.sendPacket(new ExSendUIEvent(player, false, false, 0, 0, NpcStringId.REMAINING_TIME));
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (world != null)
		{
			// Change world status.
			world.incStatus();
			
			// Make machine talk.
			final Npc machine = world.getNpc(MACHINE);
			machine.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.HA_NOT_BAD);
			
			// Save progress to global variables.
			GlobalVariablesManager.getInstance().increaseInt(GlobalVariablesManager.MONSTER_ARENA_VARIABLE + machine.getScriptValue(), 1);
			
			final Clan clan = player.getClan();
			if (clan != null)
			{
				clan.getVariables().set("MAX_RAID_LEVEL", clan.getVariables().getInt("MAX_RAID_LEVEL", 0) + 1);
			}
			
			// Spawn reward chests.
			world.spawnGroup("supplies");
			startQuestTimer("remove_supplies", 60000, machine, null);
			
			// Next boss spawn.
			if (world.getStatus() < 5)
			{
				startQuestTimer("next_spawn", 60000, machine, null);
			}
			else // Finish.
			{
				for (Player plr : world.getPlayers())
				{
					plr.sendPacket(new ExSendUIEvent(plr, false, false, 0, 0, NpcStringId.REMAINING_TIME));
				}
				world.finishInstance();
			}
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + "-01.htm";
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
		
		final Clan clan = player.getClan();
		if (clan == null)
		{
			// Should never happen.
			final Skill knownSkill = player.getKnownSkill(CLAN_EXUBERANCE);
			if (knownSkill != null)
			{
				player.removeSkill(knownSkill, true);
			}
			return;
		}
		
		final int stage = GlobalVariablesManager.getInstance().getInt(GlobalVariablesManager.MONSTER_ARENA_VARIABLE + clan.getId(), 0);
		if (stage > 4)
		{
			player.addSkill(SkillData.getInstance().getSkill(CLAN_EXUBERANCE, stage / 5), false);
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_CLAN_LEFT)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerClanLeft(OnPlayerClanLeft event)
	{
		final ClanMember member = event.getClanMember();
		if ((member == null) || !member.isOnline())
		{
			return;
		}
		
		member.getPlayer().removeSkill(CLAN_EXUBERANCE, true);
	}
	
	public static void main(String[] args)
	{
		new MonsterArena();
	}
}
