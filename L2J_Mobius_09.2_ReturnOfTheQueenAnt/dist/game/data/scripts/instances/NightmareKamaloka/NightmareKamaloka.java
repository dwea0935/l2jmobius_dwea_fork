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
package instances.NightmareKamaloka;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

import instances.AbstractInstance;

/**
 * Nightmare Kamaloka instance zone.
 * @author St3eT, Mobius
 */
public class NightmareKamaloka extends AbstractInstance
{
	// NPCs
	private static final int BENUSTA = 34542;
	private static final int DARK_RIDER = 26102;
	private static final int SIONE_ULAF = 26465;
	private static final int INVISIBLE_NPC = 18919;
	// Items
	private static final ItemHolder BENUSTAS_REWARD_BOX = new ItemHolder(81151, 1);
	private static final ItemHolder BENUSTAS_SHINING_REWARD_BOX = new ItemHolder(81452, 1);
	private static final ItemHolder BENUSTAS_REWARD_BOX_110 = new ItemHolder(81741, 1);
	// Misc
	private static final Map<Integer, Integer> BOSS_MAP = new HashMap<>();
	static
	{
		BOSS_MAP.put(26093, 18170002); // Mino
		BOSS_MAP.put(26094, 18170004); // Sola
		BOSS_MAP.put(26096, 18170006); // Ariarc
		BOSS_MAP.put(26099, 18170008); // Sirra
		BOSS_MAP.put(DARK_RIDER, -1); // Dark Rider
	}
	private static final Map<Integer, Integer> BOSS_MAP_110 = new HashMap<>();
	static
	{
		BOSS_MAP_110.put(26461, 18170002); // Noegg
		BOSS_MAP_110.put(26462, 18170004); // Kyshis
		BOSS_MAP_110.put(26463, 18170006); // Ssizz Chronizel
		BOSS_MAP_110.put(26464, 18170008); // Kanan Chronizel
		BOSS_MAP_110.put(SIONE_ULAF, -1); // Sir Sione Ulaf
	}
	private static final int[] TEMPLATE_IDS =
	{
		258, // lv. 105
		313, // lv. 110
	};
	
	public NightmareKamaloka()
	{
		super(TEMPLATE_IDS);
		addStartNpc(BENUSTA);
		addTalkId(BENUSTA);
		addSpawnId(INVISIBLE_NPC);
		addKillId(BOSS_MAP.keySet());
		addKillId(BOSS_MAP_110.keySet());
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.contains("enterInstance"))
		{
			final int templateId = event.contains("110") ? TEMPLATE_IDS[1] : TEMPLATE_IDS[0];
			if (player.isInParty())
			{
				final Party party = player.getParty();
				if (!party.isLeader(player))
				{
					player.sendPacket(SystemMessageId.ONLY_A_PARTY_LEADER_CAN_MAKE_THE_REQUEST_TO_ENTER);
					return null;
				}
				
				if (player.isInCommandChannel())
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_ENTER_BECAUSE_YOU_DO_NOT_MEET_THE_REQUIREMENTS);
					return null;
				}
				
				final long currentTime = System.currentTimeMillis();
				final List<Player> members = party.getMembers();
				for (Player member : members)
				{
					if (!member.isInsideRadius3D(npc, 1000))
					{
						player.sendMessage("Player " + member.getName() + " must come closer.");
						return null;
					}
					
					for (int id : TEMPLATE_IDS)
					{
						if (currentTime < InstanceManager.getInstance().getInstanceTime(member, id))
						{
							final SystemMessage msg = new SystemMessage(SystemMessageId.SINCE_C1_ENTERED_ANOTHER_INSTANCE_ZONE_THEREFORE_YOU_CANNOT_ENTER_THIS_DUNGEON);
							msg.addString(member.getName());
							party.broadcastToPartyMembers(member, msg);
							return null;
						}
					}
				}
				
				for (Player member : members)
				{
					enterInstance(member, npc, templateId);
				}
			}
			else if (player.isGM())
			{
				enterInstance(player, npc, templateId);
			}
			else
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_CURRENTLY_IN_A_PARTY_SO_YOU_CANNOT_ENTER);
			}
		}
		else if ("SPAWN_BOSSES".equals(event))
		{
			final Instance instance = npc.getInstanceWorld();
			if (isInInstance(instance))
			{
				instance.spawnGroup("BOSSES");
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isInInstance(instance) && (npc.getId() == INVISIBLE_NPC))
		{
			startQuestTimer("SPAWN_BOSSES", 10000, npc, null);
		}
	}
	
	@Override
	public void onInstanceCreated(Instance instance, Player player)
	{
		instance.getParameters().set("INITIAL_PARTY_MEMBERS", player.getParty() != null ? player.getParty().getMemberCount() : 1);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world))
		{
			final int nextDoorId = world.getTemplateId() == TEMPLATE_IDS[0] ? BOSS_MAP.getOrDefault(npc.getId(), -1) : BOSS_MAP_110.getOrDefault(npc.getId(), -1);
			if (nextDoorId == -1)
			{
				for (Player member : world.getPlayers())
				{
					giveItems(member, world.getTemplateId() == TEMPLATE_IDS[0] ? BENUSTAS_REWARD_BOX : BENUSTAS_REWARD_BOX_110);
				}
				final Party party = world.getFirstPlayer().getParty();
				final Player randomPlayer = party != null ? party.getRandomPlayer() : null;
				if ((randomPlayer != null) && (getRandom(100) < 80) && (world.getPlayersCount() == world.getParameters().getInt("INITIAL_PARTY_MEMBERS", 0)))
				{
					giveItems(randomPlayer, BENUSTAS_SHINING_REWARD_BOX);
				}
				world.finishInstance();
			}
			else
			{
				world.openCloseDoor(nextDoorId, true);
			}
		}
	}
	
	public static void main(String[] args)
	{
		new NightmareKamaloka();
	}
}