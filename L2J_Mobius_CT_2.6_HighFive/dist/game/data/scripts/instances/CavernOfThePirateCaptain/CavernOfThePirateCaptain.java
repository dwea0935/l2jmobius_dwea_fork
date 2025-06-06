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
package instances.CavernOfThePirateCaptain;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerCondOverride;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.instancezone.InstanceWorld;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

import instances.AbstractInstance;

/**
 * Cavern Of The Pirate Captain (Day Dream) instance Zone.
 * @author St3eT
 */
public class CavernOfThePirateCaptain extends AbstractInstance
{
	// NPCs
	private static final int PATHFINDER = 32713; // Pathfinder Worker
	private static final int ZAKEN_60 = 29176; // Zaken
	private static final int ZAKEN_60_NIGHT = 29022; // Zaken Night
	private static final int ZAKEN_83 = 29181; // Zaken
	private static final int CANDLE = 32705; // Zaken's Candle
	private static final int DOLL_BLADER_60 = 29023; // Doll Blader
	private static final int DOLL_BLADER_83 = 29182; // Doll Blader
	private static final int VALE_MASTER_60 = 29024; // Veil Master
	private static final int VALE_MASTER_83 = 29183; // Veil Master
	private static final int PIRATES_ZOMBIE_60 = 29027; // Pirate Zombie
	private static final int PIRATES_ZOMBIE_83 = 29185; // Pirate Zombie
	private static final int PIRATES_ZOMBIE_CAPTAIN_60 = 29026; // Pirate Zombie Captain
	private static final int PIRATES_ZOMBIE_CAPTAIN_83 = 29184; // Pirate Zombie Captain
	// Items
	private static final int VORPAL_RING = 15763; // Sealed Vorpal Ring
	private static final int VORPAL_EARRING = 15764; // Sealed Vorpal Earring
	private static final int FIRE = 15280; // Transparent 1HS (for NPC)
	private static final int RED = 15281; // Transparent 1HS (for NPC)
	private static final int BLUE = 15302; // Transparent Bow (for NPC)
	// Locations
	private static final Location[] ENTER_LOC =
	{
		new Location(52684, 219989, -3496),
		new Location(52669, 219120, -3224),
		new Location(52672, 219439, -3312),
	};
	private static final Location[] CANDLE_LOC =
	{
		// Floor 1
		new Location(53313, 220133, -3498),
		new Location(53313, 218079, -3498),
		new Location(54240, 221045, -3498),
		new Location(54325, 219095, -3498),
		new Location(54240, 217155, -3498),
		new Location(55257, 220028, -3498),
		new Location(55257, 218172, -3498),
		new Location(56280, 221045, -3498),
		new Location(56195, 219095, -3498),
		new Location(56280, 217155, -3498),
		new Location(57215, 220133, -3498),
		new Location(57215, 218079, -3498),
		// Floor 2
		new Location(53313, 220133, -3226),
		new Location(53313, 218079, -3226),
		new Location(54240, 221045, -3226),
		new Location(54325, 219095, -3226),
		new Location(54240, 217155, -3226),
		new Location(55257, 220028, -3226),
		new Location(55257, 218172, -3226),
		new Location(56280, 221045, -3226),
		new Location(56195, 219095, -3226),
		new Location(56280, 217155, -3226),
		new Location(57215, 220133, -3226),
		new Location(57215, 218079, -3226),
		// Floor 3
		new Location(53313, 220133, -2954),
		new Location(53313, 218079, -2954),
		new Location(54240, 221045, -2954),
		new Location(54325, 219095, -2954),
		new Location(54240, 217155, -2954),
		new Location(55257, 220028, -2954),
		new Location(55257, 218172, -2954),
		new Location(56280, 221045, -2954),
		new Location(56195, 219095, -2954),
		new Location(56280, 217155, -2954),
		new Location(57215, 220133, -2954),
		new Location(57215, 218079, -2954),
	};
	// Misc
	private static final int MIN_LV_60 = 55;
	private static final int MIN_LV_83 = 78;
	private static final int PLAYERS_60_MIN = 9;
	private static final int PLAYERS_60_MAX = 27;
	private static final int PLAYERS_60_NIGHT_MIN = 72;
	private static final int PLAYERS_60_NIGHT_MAX = 450;
	private static final int PLAYERS_83_MIN = 9;
	private static final int PLAYERS_83_MAX = 27;
	private static final int TEMPLATE_ID_60 = 133;
	private static final int TEMPLATE_ID_60_NIGHT = 114;
	private static final int TEMPLATE_ID_83 = 135;
	//@formatter:off
	private static final int[][] ROOM_DATA =
	{
		// Floor 1
		{54240, 220133, -3498, 1, 3, 4, 6},
		{54240, 218073, -3498, 2, 5, 4, 7},
		{55265, 219095, -3498, 4, 9, 6, 7},
		{56289, 220133, -3498, 8, 11, 6, 9},
		{56289, 218073, -3498, 10, 12, 7, 9},
		// Floor 2
		{54240, 220133, -3226, 13, 15, 16, 18},
		{54240, 218073, -3226, 14, 17, 16, 19},
		{55265, 219095, -3226, 21, 16, 19, 18},
		{56289, 220133, -3226, 20, 23, 21, 18},
		{56289, 218073, -3226, 22, 24, 19, 21},
		// Floor 3
		{54240, 220133, -2954, 25, 27, 28, 30},
		{54240, 218073, -2954, 26, 29, 28, 31},
		{55265, 219095, -2954, 33, 28, 31, 30},
		{56289, 220133, -2954, 32, 35, 30, 33},
		{56289, 218073, -2954, 34, 36, 31, 33}
	};
	//@formatter:on
	
	private CavernOfThePirateCaptain()
	{
		addStartNpc(PATHFINDER);
		addTalkId(PATHFINDER);
		addKillId(ZAKEN_60, ZAKEN_60_NIGHT, ZAKEN_83);
		addFirstTalkId(CANDLE);
	}
	
	@Override
	public void onEnterInstance(Player player, InstanceWorld world, boolean firstEntrance)
	{
		if (firstEntrance)
		{
			world.setParameter("is83", world.getTemplateId() == TEMPLATE_ID_83);
			world.setParameter("isNight", world.getTemplateId() == TEMPLATE_ID_60_NIGHT);
			world.setParameter("storeTime", System.currentTimeMillis());
			
			final List<Player> playersInside = new ArrayList<>();
			final Party party = player.getParty();
			if (party == null)
			{
				playersInside.add(player);
				managePlayerEnter(player, world);
			}
			else if (party.isInCommandChannel())
			{
				for (Player member : party.getCommandChannel().getMembers())
				{
					playersInside.add(member);
					managePlayerEnter(member, world);
				}
			}
			else
			{
				for (Player member : party.getMembers())
				{
					playersInside.add(member);
					managePlayerEnter(member, world);
				}
			}
			world.setParameter("playersInside", playersInside);
			manageNpcSpawn(world);
		}
		else
		{
			teleportPlayer(player, getRandomEntry(ENTER_LOC), world.getInstanceId(), false);
		}
	}
	
	private void managePlayerEnter(Player player, InstanceWorld world)
	{
		world.addAllowed(player);
		teleportPlayer(player, getRandomEntry(ENTER_LOC), world.getInstanceId(), false);
	}
	
	private boolean checkConditions(Player player, int templateId)
	{
		if (player.canOverrideCond(PlayerCondOverride.INSTANCE_CONDITIONS))
		{
			return true;
		}
		
		if (!player.isInParty())
		{
			broadcastSystemMessage(player, null, SystemMessageId.YOU_ARE_NOT_CURRENTLY_IN_A_PARTY_SO_YOU_CANNOT_ENTER, false);
			return false;
		}
		
		final boolean is83 = templateId == TEMPLATE_ID_83;
		final boolean isNight = templateId == TEMPLATE_ID_60_NIGHT;
		final Party party = player.getParty();
		final boolean isInCC = party.isInCommandChannel();
		final List<Player> members = (isInCC) ? party.getCommandChannel().getMembers() : party.getMembers();
		final boolean isPartyLeader = (isInCC) ? party.getCommandChannel().isLeader(player) : party.isLeader(player);
		if (!isPartyLeader)
		{
			broadcastSystemMessage(player, null, SystemMessageId.ONLY_A_PARTY_LEADER_CAN_MAKE_THE_REQUEST_TO_ENTER, false);
			return false;
		}
		
		if ((members.size() < (is83 ? PLAYERS_83_MIN : isNight ? PLAYERS_60_NIGHT_MIN : PLAYERS_60_MIN)) || (members.size() > (is83 ? PLAYERS_83_MAX : isNight ? PLAYERS_60_NIGHT_MAX : PLAYERS_60_MAX)))
		{
			broadcastSystemMessage(player, null, SystemMessageId.YOU_CANNOT_ENTER_DUE_TO_THE_PARTY_HAVING_EXCEEDED_THE_LIMIT, false);
			return false;
		}
		
		for (Player groupMembers : members)
		{
			if (groupMembers.getLevel() < (is83 ? MIN_LV_83 : MIN_LV_60))
			{
				broadcastSystemMessage(player, groupMembers, SystemMessageId.C1_S_LEVEL_DOES_NOT_CORRESPOND_TO_THE_REQUIREMENTS_FOR_ENTRY, true);
				return false;
			}
			
			if (!player.isInsideRadius3D(groupMembers, 1000))
			{
				broadcastSystemMessage(player, groupMembers, SystemMessageId.C1_IS_IN_A_LOCATION_WHICH_CANNOT_BE_ENTERED_THEREFORE_IT_CANNOT_BE_PROCESSED, true);
				return false;
			}
			
			final Long reentertime = InstanceManager.getInstance().getInstanceTime(groupMembers.getObjectId(), (is83 ? TEMPLATE_ID_83 : isNight ? TEMPLATE_ID_60_NIGHT : TEMPLATE_ID_60));
			if (System.currentTimeMillis() < reentertime)
			{
				broadcastSystemMessage(player, groupMembers, SystemMessageId.C1_MAY_NOT_RE_ENTER_YET, true);
				return false;
			}
		}
		return true;
	}
	
	private void broadcastSystemMessage(Player player, Player member, SystemMessageId msgId, boolean toGroup)
	{
		final SystemMessage sm = new SystemMessage(msgId);
		if (toGroup)
		{
			sm.addPcName(member);
			
			final Party party = player.getParty();
			if (party.isInCommandChannel())
			{
				party.getCommandChannel().broadcastPacket(sm);
			}
			else
			{
				party.broadcastPacket(sm);
			}
		}
		else
		{
			player.broadcastPacket(sm);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("enter60"))
		{
			if (checkConditions(player, TEMPLATE_ID_60))
			{
				enterInstance(player, TEMPLATE_ID_60);
			}
		}
		else if (event.equals("enter60night"))
		{
			if (checkConditions(player, TEMPLATE_ID_60_NIGHT))
			{
				enterInstance(player, TEMPLATE_ID_60_NIGHT);
			}
		}
		else if (event.equals("enter83"))
		{
			if (checkConditions(player, TEMPLATE_ID_83))
			{
				enterInstance(player, TEMPLATE_ID_83);
			}
		}
		else
		{
			final InstanceWorld world = InstanceManager.getInstance().getWorld(npc);
			if (world != null)
			{
				switch (event)
				{
					case "BURN_BLUE":
					{
						if (npc.getRightHandItem() == 0)
						{
							npc.setRHandId(FIRE);
							startQuestTimer("BURN_BLUE2", 3000, npc, player);
							if (world.getParameters().getInt("blueFounded", 0) == 4)
							{
								startQuestTimer("SHOW_ZAKEN", 5000, npc, player);
							}
						}
						break;
					}
					case "BURN_BLUE2":
					{
						if (npc.getRightHandItem() == FIRE)
						{
							npc.setRHandId(BLUE);
						}
						break;
					}
					case "BURN_RED":
					{
						if (npc.getRightHandItem() == 0)
						{
							npc.setRHandId(FIRE);
							startQuestTimer("BURN_RED2", 3000, npc, player);
						}
						break;
					}
					case "BURN_RED2":
					{
						if (npc.getRightHandItem() == FIRE)
						{
							final int room = getRoomByCandle(npc);
							npc.setRHandId(RED);
							manageScreenMsg(world, NpcStringId.THE_CANDLES_CAN_LEAD_YOU_TO_ZAKEN_DESTROY_HIM);
							spawnNpc(world.getParameters().getBoolean("is83", false) ? DOLL_BLADER_83 : DOLL_BLADER_60, room, player, world);
							spawnNpc(world.getParameters().getBoolean("is83", false) ? VALE_MASTER_83 : VALE_MASTER_60, room, player, world);
							spawnNpc(world.getParameters().getBoolean("is83", false) ? PIRATES_ZOMBIE_83 : PIRATES_ZOMBIE_60, room, player, world);
							spawnNpc(world.getParameters().getBoolean("is83", false) ? PIRATES_ZOMBIE_CAPTAIN_83 : PIRATES_ZOMBIE_CAPTAIN_60, room, player, world);
						}
						break;
					}
					case "SHOW_ZAKEN":
					{
						if (world.getParameters().getBoolean("is83", false))
						{
							manageScreenMsg(world, NpcStringId.WHO_DARES_AWAKEN_THE_MIGHTY_ZAKEN);
						}
						world.getParameters().getObject("zaken", Npc.class).setInvisible(false);
						world.getParameters().getObject("zaken", Npc.class).setParalyzed(false);
						spawnNpc(world.getParameters().getBoolean("is83", false) ? DOLL_BLADER_83 : DOLL_BLADER_60, world.getParameters().getInt("zakenRoom", 0), player, world);
						spawnNpc(world.getParameters().getBoolean("is83", false) ? PIRATES_ZOMBIE_83 : PIRATES_ZOMBIE_60, world.getParameters().getInt("zakenRoom", 0), player, world);
						spawnNpc(world.getParameters().getBoolean("is83", false) ? PIRATES_ZOMBIE_CAPTAIN_83 : PIRATES_ZOMBIE_CAPTAIN_60, world.getParameters().getInt("zakenRoom", 0), player, world);
						break;
					}
				}
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final InstanceWorld world = InstanceManager.getInstance().getWorld(npc);
		if (world != null)
		{
			if (npc.getId() == ZAKEN_83)
			{
				for (Player playersInside : world.getParameters().getList("playersInside", Player.class))
				{
					if ((playersInside != null) && ((playersInside.getInstanceId() == world.getInstanceId()) && playersInside.isInsideRadius3D(npc, 1500)))
					{
						final long time = System.currentTimeMillis() - world.getParameters().getLong("storeTime", 0);
						if (time <= 300000) // 5 minutes
						{
							if (getRandomBoolean())
							{
								giveItems(playersInside, VORPAL_RING, 1);
							}
						}
						else if (time <= 600000) // 10 minutes
						{
							if (getRandom(100) < 30)
							{
								giveItems(playersInside, VORPAL_EARRING, 1);
							}
						}
						else if (time <= 900000) // 15 minutes
						{
							if (getRandom(100) < 25)
							{
								giveItems(playersInside, VORPAL_RING, 1);
							}
						}
					}
				}
			}
			finishInstance(world);
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final InstanceWorld world = InstanceManager.getInstance().getWorld(npc);
		if (world != null)
		{
			final boolean isBlue = npc.getVariables().getInt("isBlue", 0) == 1;
			if (npc.isScriptValue(0))
			{
				if (isBlue)
				{
					int blueFounded = world.getParameters().getInt("blueFounded", 0);
					blueFounded++;
					world.setParameter("blueFounded", blueFounded);
					startQuestTimer("BURN_BLUE", 500, npc, player);
				}
				else
				{
					startQuestTimer("BURN_RED", 500, npc, player);
				}
				npc.setScriptValue(1);
			}
		}
		return null;
	}
	
	private int getRoomByCandle(Npc npc)
	{
		final int candleId = npc.getVariables().getInt("candleId", 0);
		for (int i = 0; i < 15; i++)
		{
			if ((ROOM_DATA[i][3] == candleId) || (ROOM_DATA[i][4] == candleId))
			{
				return i + 1;
			}
		}
		
		if ((candleId == 6) || (candleId == 7))
		{
			return 3;
		}
		else if ((candleId == 18) || (candleId == 19))
		{
			return 8;
		}
		else if ((candleId == 30) || (candleId == 31))
		{
			return 13;
		}
		return 0;
	}
	
	private void manageScreenMsg(InstanceWorld world, NpcStringId stringId)
	{
		for (Player players : world.getParameters().getList("playersInside", Player.class))
		{
			if ((players != null) && (players.getInstanceId() == world.getInstanceId()))
			{
				showOnScreenMsg(players, stringId, 2, 6000);
			}
		}
	}
	
	private Attackable spawnNpc(int npcId, int roomId, Player player, InstanceWorld world)
	{
		if ((player != null) && (npcId != ZAKEN_60) && (npcId != ZAKEN_83))
		{
			final Attackable mob = addSpawn(npcId, ROOM_DATA[roomId - 1][0] + getRandom(350), ROOM_DATA[roomId - 1][1] + getRandom(350), ROOM_DATA[roomId - 1][2], 0, false, 0, false, world.getInstanceId()).asAttackable();
			addAttackDesire(mob, player);
			return mob;
		}
		return addSpawn(npcId, ROOM_DATA[roomId - 1][0], ROOM_DATA[roomId - 1][1], ROOM_DATA[roomId - 1][2], 0, false, 0, false, world.getInstanceId()).asAttackable();
	}
	
	private void manageNpcSpawn(InstanceWorld world)
	{
		final List<Npc> candles = new ArrayList<>();
		world.setParameter("zakenRoom", getRandom(1, 15));
		for (int i = 0; i < 36; i++)
		{
			final Npc candle = addSpawn(CANDLE, CANDLE_LOC[i], false, 0, false, world.getInstanceId());
			candle.getVariables().set("candleId", i + 1);
			candles.add(candle);
		}
		
		for (int i = 3; i < 7; i++)
		{
			candles.get(ROOM_DATA[world.getParameters().getInt("zakenRoom", 0) - 1][i] - 1).getVariables().set("isBlue", 1);
		}
		final Npc zaken = spawnNpc(world.getParameters().getBoolean("is83", false) ? ZAKEN_83 : world.getParameters().getBoolean("isNight", false) ? ZAKEN_60_NIGHT : ZAKEN_60, world.getParameters().getInt("zakenRoom", 0), null, world);
		zaken.setInvisible(true);
		zaken.setParalyzed(true);
		world.setParameter("zaken", zaken);
	}
	
	public static void main(String[] args)
	{
		new CavernOfThePirateCaptain();
	}
}
