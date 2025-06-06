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
package ai.areas.FrostCastleZone;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicReference;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.SpawnData;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerBypass;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.spawns.SpawnGroup;
import org.l2jmobius.gameserver.model.spawns.SpawnTemplate;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

import ai.AbstractNpcAI;

/**
 * @author Serenitty
 */
public class FrostLordCastleZone extends AbstractNpcAI
{
	// NPC
	public static final int REGGIESYS = 25942;
	public static final int SLICING = 25943;
	public static final int TIRON = 29135;
	private static final int CHARGED_CRYSTAL = 34232;
	
	private static final AtomicReference<SpawnTemplate> SPAWN_BATTLE_MOBS = new AtomicReference<>();
	private static final AtomicReference<SpawnTemplate> SPAWN_ENCHANCED_MOBS = new AtomicReference<>();
	
	protected static final int[] REGGIESYS_GLAKIAS =
	{
		29136,
		29137
	};
	protected static final int[] SLICING_GLAKIAS =
	{
		29138,
		29139
	};
	
	// Locations
	private static final Location REGGIESYS_SLICING_SPAWN_LOC = new Location(14859, 142621, -11861, 20525);
	private static final Location TIRON_SPAWN_LOC = new Location(149943, 144657, -12211, 28725);
	private static final Location CHARGED_CRYSTAL_SPAWN_LOC = new Location(149216, 143823, -12200, 48000);
	private static final Location GLAKIAS_SPAWN_LOC = new Location(114713, -114799, -11209, 33289);
	
	// Teleports
	private static final Location NORTH_EASTERN_ENTRANCE = new Location(-56255, 13537, -3336);
	private static final Location SOUTH_EASTERN_ENTRANCE = new Location(-49550, 17189, -3016);
	private static final Location NORTH_WESTERN_ENTRANCE = new Location(-52849, 5272, -240);
	private static final Location SOUTH_WESTERN_ENTRANCE = new Location(-52849, 5272, -240);
	
	private static final Location CROSSROAD = new Location(145598, 144091, -11789);
	private static final Location NOTHERN_SECRET_PASSAGE = new Location(149478, 147145, -12339);
	
	private static final Location GLAKIAS_RESIDENCE = new Location(113479, -114804, -11076);
	
	public static final int UNDERCOVER_AGENT = 34230; // Teleport npc
	public static final int CRYSTAL_ENERGY = 34232; // Teleport npc
	
	// Zones
	private static final ZoneType[] ZONES =
	{
		ZoneManager.getInstance().getZoneByName("frost_castle_zone"),
		ZoneManager.getInstance().getZoneByName("Glakias_castle")
	};
	
	// Timings
	private static final int[] DAYS_OF_WEEK =
	{
		Calendar.TUESDAY,
		Calendar.THURSDAY,
	};
	private static final int[] FIRST_RAID_TIME =
	{
		21,
		30
	};
	private static final int[] SECOND_RAID_TIME =
	{
		22,
		00
	};
	
	private static final long DESPAWN_DELAY = 16 * 60 * 60 * 1000;
	
	private static Npc _teleportchargedCrystal = null;
	
	private static final String SCRIPT_BYPASS = "Quest FrostLordCastleZone ";
	
	private FrostLordCastleZone()
	{
		addKillId(REGGIESYS, SLICING, TIRON);
		addKillId(REGGIESYS_GLAKIAS);
		addKillId(SLICING_GLAKIAS);
		addDespawnId(REGGIESYS, SLICING, TIRON);
		addFirstTalkId(UNDERCOVER_AGENT);
		addFirstTalkId(CRYSTAL_ENERGY);
		addSpawnId(TIRON);
		addSpawnId(CRYSTAL_ENERGY);
		scheduleFirstRaid();
		scheduleSecondRaid();
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "Crossroad":
			{
				teleportCheck(player, 1);
				break;
			}
			case "Northern_Secret_Passage":
			{
				teleportCheck(player, 2);
				break;
			}
			case "The_north_eastern_entrance":
			{
				teleportCheck(player, 3);
				break;
			}
			case "The_south_eastern_entrance":
			{
				teleportCheck(player, 4);
				break;
			}
			case "The_north_western_entrance":
			{
				teleportCheck(player, 5);
				break;
			}
			case "The_south_western_entrance":
			{
				teleportCheck(player, 6);
				break;
			}
			case "Crystal_Energy_Teleport":
			{
				player.teleToLocation(GLAKIAS_RESIDENCE);
				break;
			}
			case "FIRST_RAID_SPAWN":
			{
				int id = Rnd.get(100) < 7 ? SLICING : REGGIESYS;
				addSpawn(id, REGGIESYS_SLICING_SPAWN_LOC, false, DESPAWN_DELAY);
				break;
			}
			case "SECOND_RAID_SPAWN":
			{
				addSpawn(TIRON, TIRON_SPAWN_LOC, false, DESPAWN_DELAY);
				break;
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (npc.getId() == CRYSTAL_ENERGY)
		{
			return getHtm(player, "34232.htm");
		}
		
		if (npc.getId() == UNDERCOVER_AGENT)
		{
			if ((_teleportchargedCrystal != null) && _teleportchargedCrystal.isSpawned())
			{
				return getHtm(player, "34230-full.htm");
			}
			
			return getHtm(player, "34230.htm");
		}
		
		return null;
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		switch (npc.getId())
		{
			case TIRON:
			{
				final ExShowScreenMessage msg = new ExShowScreenMessage("Heathen blasphemers of this holy place and our lord! You will be punished!", ExShowScreenMessage.BOTTOM_CENTER, 17000, 0, false, true);
				for (ZoneType zone : ZONES)
				{
					zone.broadcastPacket(msg);
				}
				break;
			}
			case CRYSTAL_ENERGY:
			{
				SPAWN_ENCHANCED_MOBS.set(SpawnData.getInstance().getSpawnByName("mobs_Enhanced_with_Crystal_Energy"));
				SPAWN_ENCHANCED_MOBS.get().getGroups().forEach(SpawnGroup::spawnAll);
				break;
			}
		}
	}
	
	@Override
	public void onNpcDespawn(Npc npc)
	{
		switch (npc.getId())
		{
			case REGGIESYS:
			case SLICING:
			{
				scheduleFirstRaid();
				break;
			}
			case TIRON:
			{
				scheduleSecondRaid();
				break;
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		switch (npc.getId())
		{
			case REGGIESYS:
			{
				addSpawn(REGGIESYS_GLAKIAS[Rnd.get(0, REGGIESYS_GLAKIAS.length - 1)], GLAKIAS_SPAWN_LOC, false, DESPAWN_DELAY);
				SPAWN_BATTLE_MOBS.set(SpawnData.getInstance().getSpawnByName("glakias_mobs_pretorian"));
				SPAWN_BATTLE_MOBS.get().getGroups().forEach(SpawnGroup::despawnAll);
				SPAWN_BATTLE_MOBS.get().getGroups().forEach(SpawnGroup::spawnAll);
				break;
			}
			case SLICING:
			{
				addSpawn(SLICING_GLAKIAS[Rnd.get(0, SLICING_GLAKIAS.length - 1)], GLAKIAS_SPAWN_LOC, false, DESPAWN_DELAY);
				SPAWN_BATTLE_MOBS.set(SpawnData.getInstance().getSpawnByName("glakias_mobs_pretorian"));
				SPAWN_BATTLE_MOBS.get().getGroups().forEach(SpawnGroup::despawnAll);
				SPAWN_BATTLE_MOBS.get().getGroups().forEach(SpawnGroup::spawnAll);
				break;
			}
			case TIRON:
			{
				_teleportchargedCrystal = addSpawn(CHARGED_CRYSTAL, CHARGED_CRYSTAL_SPAWN_LOC, false, DESPAWN_DELAY);
				break;
			}
		}
		deleteGlakiasSpawns(npc);
	}
	
	@RegisterEvent(EventType.ON_PLAYER_BYPASS)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerBypass(OnPlayerBypass event)
	{
		final Player player = event.getPlayer();
		if (event.getCommand().startsWith(SCRIPT_BYPASS))
		{
			notifyEvent(event.getCommand().replace(SCRIPT_BYPASS, ""), null, player);
		}
	}
	
	private void scheduleFirstRaid()
	{
		long time = Long.MAX_VALUE;
		for (int day : DAYS_OF_WEEK)
		{
			final long nextDateMillis = getNextDateMilis(day, FIRST_RAID_TIME[0], FIRST_RAID_TIME[1]);
			if (nextDateMillis < time)
			{
				time = nextDateMillis;
			}
		}
		startQuestTimer("FIRST_RAID_SPAWN", time - System.currentTimeMillis(), null, null);
	}
	
	private void scheduleSecondRaid()
	{
		long time = Long.MAX_VALUE;
		for (int day : DAYS_OF_WEEK)
		{
			final long nextDateMillis = getNextDateMilis(day, SECOND_RAID_TIME[0], SECOND_RAID_TIME[1]);
			if (nextDateMillis < time)
			{
				time = nextDateMillis;
			}
		}
		startQuestTimer("SECOND_RAID_SPAWN", time - System.currentTimeMillis(), null, null);
	}
	
	private long getNextDateMilis(int dayOfWeek, int hour, int minute)
	{
		final Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, minute);
		calendar.set(Calendar.SECOND, 0);
		for (int i = 0; i < 7; i++)
		{
			if ((calendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek) && (calendar.getTimeInMillis() > System.currentTimeMillis()))
			{
				return calendar.getTimeInMillis();
			}
			calendar.add(Calendar.DAY_OF_WEEK, 1);
		}
		return calendar.getTimeInMillis();
	}
	
	public void teleportCheck(Player player, int locationId)
	{
		int requiredMoney = 0;
		Location teleportLocation = null;
		switch (locationId)
		{
			case 1:
			{
				requiredMoney = 100000;
				teleportLocation = CROSSROAD;
				break;
			}
			case 2:
			{
				requiredMoney = 100000;
				teleportLocation = NOTHERN_SECRET_PASSAGE;
				break;
			}
			case 3:
			{
				requiredMoney = 500000;
				teleportLocation = NORTH_EASTERN_ENTRANCE;
				break;
			}
			case 4:
			{
				requiredMoney = 500000;
				teleportLocation = SOUTH_EASTERN_ENTRANCE;
				break;
			}
			case 5:
			{
				requiredMoney = 500000;
				teleportLocation = NORTH_WESTERN_ENTRANCE;
				break;
			}
			case 6:
			{
				requiredMoney = 500000;
				teleportLocation = SOUTH_WESTERN_ENTRANCE;
				break;
			}
			default:
			{
				return;
			}
		}
		
		if (!player.destroyItemByItemId(ItemProcessType.FEE, Inventory.ADENA_ID, requiredMoney, player, true))
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_MONEY_TO_USE_THE_FUNCTION);
			return;
		}
		
		player.teleToLocation(teleportLocation);
	}
	
	private void deleteGlakiasSpawns(Npc npc)
	{
		int npcId = npc.getId();
		if ((npcId == REGGIESYS_GLAKIAS[0]) || (npcId == REGGIESYS_GLAKIAS[1]) || (npcId == SLICING_GLAKIAS[0]) || (npcId == SLICING_GLAKIAS[1]))
		{
			SPAWN_BATTLE_MOBS.get().getGroups().forEach(SpawnGroup::despawnAll);
		}
	}
	
	public static void main(String[] args)
	{
		new FrostLordCastleZone();
	}
}
