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
package ai.areas.AztacanTemple;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.BuyListData;
import org.l2jmobius.gameserver.data.xml.SpawnData;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.buylist.ProductList;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.siege.TaxType;
import org.l2jmobius.gameserver.model.spawns.NpcSpawnTemplate;
import org.l2jmobius.gameserver.model.spawns.SpawnGroup;
import org.l2jmobius.gameserver.model.spawns.SpawnTemplate;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.BuyList;
import org.l2jmobius.gameserver.network.serverpackets.ExBuySellList;
import org.l2jmobius.gameserver.network.serverpackets.ExSendUIEvent;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

import ai.AbstractNpcAI;

/**
 * @author Serenitty
 */
public class AztacanArea extends AbstractNpcAI
{
	// NPCs
	private static final int AZTACAN_BOSS = 29228; // Aztacan Boss
	private static final int TRAZENTA = 18970; // Trazenta Boss
	private static final int INIS = 18968; // Inis Boss
	private static final int KAZTAR = 18971; // Kaztar Boss
	private static final int ARCHEON = 18969; // Archeon Boss
	private static final int DEFENSIVE_TOTEM = 18966;
	private static final int SIBEN = 34414; // Teleporter
	// Teleports
	private static final Location MIDWAY_GATE = new Location(16794, 218468, -14808);
	private static final Location EASTERN_GATE = new Location(20077, 214846, -14888);
	private static final Location WESTERN_GATE = new Location(13858, 215575, -14897);
	// Zone
	private static final ZoneType AZTACAN_ZONE = ZoneManager.getInstance().getZoneById(2922800);
	// Misc
	private static final AtomicReference<SpawnTemplate> SPAWNED_MONSTERS = new AtomicReference<>();
	private static final long COMBAT_DURATION = 3600000; // 1 hour
	private static final int[][] RAID_TIMES =
	{
		{
			6,
			0
		},
		{
			12,
			00
		},
		{
			18,
			00
		},
		{
			00,
			00
		}
	};
	
	private static ArrayList<Npc> _totems = new ArrayList<>();
	private int _totemDefeatCount = 0;
	private Npc _bossInstance;
	
	private static long _eventStartTime = 0;
	private static long _eventEndTime = 0;
	
	private static ScheduledFuture<?> _despawnTask;
	
	private static boolean _inProgress = false;
	
	public AztacanArea()
	{
		addExitZoneId(AZTACAN_ZONE.getId());
		addEnterZoneId(AZTACAN_ZONE.getId());
		addKillId(TRAZENTA, INIS, KAZTAR, ARCHEON);
		addKillId(AZTACAN_BOSS, DEFENSIVE_TOTEM);
		addAttackId(AZTACAN_BOSS);
		addSpawnId(AZTACAN_BOSS);
		addFirstTalkId(SIBEN);
		
		schedulePreparation();
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "The_Midway_Gate":
			{
				teleportCheck(player, 1);
				break;
			}
			case "The_Eastern_Gate":
			{
				teleportCheck(player, 2);
				break;
			}
			case "The_Western_Gate":
			{
				teleportCheck(player, 3);
				break;
			}
			case "Mineral_shop":
			{
				player.setInventoryBlockingStatus(true);
				final ProductList buyList = BuyListData.getInstance().getBuyList(3441401);
				player.sendPacket(new BuyList(buyList, player, npc.getCastleTaxRate(TaxType.BUY)));
				player.sendPacket(new ExBuySellList(player, false));
				break;
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	private void spawnMonsters()
	{
		SPAWNED_MONSTERS.set(SpawnData.getInstance().getSpawnByName("Before_Mobs"));
		startBossSpawn();
	}
	
	private void startBossSpawn()
	{
		_inProgress = true;
		SPAWNED_MONSTERS.get().getGroups().forEach(SpawnGroup::despawnAll);
		
		_bossInstance = addSpawn(AZTACAN_BOSS, 16793, 215571, -15150, 50874, false, 0, true);
		_bossInstance.setInvul(true);
		_bossInstance.setImmobilized(true);
		_bossInstance.setTargetable(false);
		startSpawnRelicGuards();
		
		_eventStartTime = System.currentTimeMillis();
		_eventEndTime = _eventStartTime + COMBAT_DURATION;
		
		_despawnTask = ThreadPool.schedule(this::despawnBoss, COMBAT_DURATION);
	}
	
	private void despawnBoss()
	{
		_inProgress = false;
		despawnTotems();
		slowShowUI();
		deSpawnRelicGuard();
		_eventEndTime = 0;
		deleteBoss();
		ThreadPool.schedule(this::restoreArea, 20000);
	}
	
	private void startSpawnRelicGuards()
	{
		final SpawnTemplate defensiveTotems = SpawnData.getInstance().getSpawnByName("boss_guardianmobs");
		if (defensiveTotems != null)
		{
			for (SpawnGroup spawnGroup : defensiveTotems.getGroups())
			{
				spawnGroup.spawnAll();
				for (NpcSpawnTemplate nst : spawnGroup.getSpawns())
				{
					for (Npc npc : nst.getSpawnedNpcs())
					{
						final Spawn spawn = npc.getSpawn();
						if (spawn != null)
						{
							spawn.stopRespawn();
						}
					}
				}
			}
		}
	}
	
	private void deSpawnRelicGuard()
	{
		final SpawnTemplate defensiveTotems = SpawnData.getInstance().getSpawnByName("boss_guardianmobs");
		if (defensiveTotems != null)
		{
			for (SpawnGroup spawnGroup : defensiveTotems.getGroups())
			{
				spawnGroup.despawnAll();
			}
		}
	}
	
	private void restoreArea()
	{
		SPAWNED_MONSTERS.get().getGroups().forEach(SpawnGroup::spawnAll);
	}
	
	@Override
	public void onEnterZone(Creature creature, ZoneType zone)
	{
		if (creature.isPlayer() && (_inProgress))
		{
			slowShowUI();
		}
	}
	
	@Override
	public void onExitZone(Creature creature, ZoneType zone)
	{
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			player.sendPacket(new ExSendUIEvent(player, true, false, 0, 0, NpcStringId.TIME_TILL_THE_RITUAL_END));
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		broadcastScreenMessageToPlayers(NpcStringId.AZTACAN_BEGINS_HIS_RITUAL, 0);
		spawnAndConfigureTotem(DEFENSIVE_TOTEM, 15906, 216533, -15153, 56337, NpcStringId.TRAZENTA);
		spawnAndConfigureTotem(DEFENSIVE_TOTEM, 17736, 216477, -15150, 41088, NpcStringId.INIS);
		spawnAndConfigureTotem(DEFENSIVE_TOTEM, 17717, 214547, -15149, 23561, NpcStringId.KAZTAR);
		spawnAndConfigureTotem(DEFENSIVE_TOTEM, 15835, 214668, -15153, 8696, NpcStringId.ARCHEON);
		ThreadPool.schedule(this::slowShowUI, 6000);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		switch (npc.getId())
		{
			case TRAZENTA:
			case INIS:
			case KAZTAR:
			case ARCHEON:
			{
				handleTotemKill(npc);
				final NpcStringId stringId = getNpcStringIdByNpcId(npc.getId());
				if (stringId != null)
				{
					updateTotemByStringId(stringId);
				}
				break;
			}
			case DEFENSIVE_TOTEM:
			{
				_totemDefeatCount += 1;
				if (_totemDefeatCount == 4)
				{
					_totemDefeatCount = 0;
					_bossInstance.setTargetable(true);
					_bossInstance.setInvul(false);
					_bossInstance.setImmobilized(false);
					broadcastScreenMessageToPlayers(NpcStringId.WHO_DARES_TO_DISTURB_MY_RITUAL, 2);
				}
				break;
			}
			case AZTACAN_BOSS:
			{
				despawnBoss();
				break;
			}
		}
	}
	
	private void broadcastScreenMessageToPlayers(NpcStringId message, int type)
	{
		for (Player player : AZTACAN_ZONE.getPlayersInside())
		{
			if (player != null)
			{
				player.sendPacket(new ExShowScreenMessage(message, ExShowScreenMessage.TOP_CENTER, 6000, true));
				if (type == 2)
				{
					player.sendPacket(new ExShowScreenMessage(message, ExShowScreenMessage.TOP_CENTER, 8000, false));
				}
			}
		}
	}
	
	private void spawnAndConfigureTotem(int npcId, int x, int y, int z, int h, NpcStringId titleStringId)
	{
		final Npc totem = addSpawn(npcId, x, y, z, h, false, 0, true);
		totem.setDisplayEffect(1);
		totem.setInvul(true);
		totem.setImmobilized(true);
		totem.setTargetable(false);
		totem.setRandomWalking(false);
		totem.setTitleString(titleStringId);
		totem.setTitle(totem.getName());
		totem.broadcastInfo();
		_totems.add(totem);
	}
	
	private void despawnTotems()
	{
		for (Npc totem : _totems)
		{
			if ((totem != null) && !totem.isDead())
			{
				totem.deleteMe();
			}
		}
		_totems.clear();
	}
	
	private void handleTotemKill(Npc totem)
	{
		if (totem != null)
		{
			NpcStringId message = null;
			switch (totem.getId())
			{
				case TRAZENTA:
				{
					message = NpcStringId.TRAZENTA_S_SPIRIT_TRAZENTA_S_GUARDIAN_TOTEM_CAN_BE_DESTROYED;
					break;
				}
				case INIS:
				{
					message = NpcStringId.INIS_SPIRIT_INIS_GUARDIAN_TOTEM_CAN_BE_DESTROYED;
					break;
				}
				case KAZTAR:
				{
					message = NpcStringId.KAZTAR_S_SPIRIT_KAZTAR_S_GUARDIAN_TOTEM_CAN_BE_DESTROYED;
					break;
				}
				case ARCHEON:
				{
					message = NpcStringId.ARCHEON_S_SPIRIT_ARCHEON_S_GUARDIAN_TOTEM_CAN_BE_DESTROYED;
					break;
				}
			}
			
			if (message != null)
			{
				broadcastScreenMessageToPlayers(message, 0);
			}
			totem.getSpawn().stopRespawn();
		}
	}
	
	private NpcStringId getNpcStringIdByNpcId(int npcId)
	{
		switch (npcId)
		{
			case TRAZENTA:
			{
				return NpcStringId.TRAZENTA;
			}
			case INIS:
			{
				return NpcStringId.INIS;
			}
			case KAZTAR:
			{
				return NpcStringId.KAZTAR;
			}
			case ARCHEON:
			{
				return NpcStringId.ARCHEON;
			}
			default:
			{
				return null;
			}
		}
	}
	
	private void deleteBoss()
	{
		if (_bossInstance != null)
		{
			_bossInstance.deleteMe();
		}
		if (_despawnTask != null)
		{
			_despawnTask.cancel(true);
		}
	}
	
	private void updateTotemByStringId(NpcStringId stringId)
	{
		for (Npc totem : _totems)
		{
			if ((totem != null) && (totem.getTitleString() == stringId))
			{
				totem.setTargetable(true);
				totem.setInvul(false);
				totem.setImmobilized(false);
				break;
			}
		}
	}
	
	private void slowShowUI()
	{
		if (_inProgress)
		{
			final long currentTime = System.currentTimeMillis();
			final long remainingTime = _eventEndTime - currentTime;
			final int combatDurationInSeconds = (int) (remainingTime / 1000);
			for (Player player : AZTACAN_ZONE.getPlayersInside())
			{
				if (player != null)
				{
					AZTACAN_ZONE.broadcastPacket(new ExSendUIEvent(player, false, false, combatDurationInSeconds, 0, NpcStringId.TIME_TILL_THE_RITUAL_END));
				}
			}
		}
		else
		{
			for (Player player : AZTACAN_ZONE.getPlayersInside())
			{
				if (player != null)
				{
					AZTACAN_ZONE.broadcastPacket(new ExSendUIEvent(player, true, false, 0, 0, NpcStringId.TIME_TILL_THE_RITUAL_END));
					broadcastScreenMessageToPlayers(NpcStringId.AZTACAN_S_RITUAL_IS_OVER, 0);
				}
			}
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (npc.getId() == SIBEN)
		{
			return getHtm(player, "34414.htm");
		}
		return null;
	}
	
	public void teleportCheck(Player player, int locationId)
	{
		int requiredMoney = 0;
		Location location = null;
		switch (locationId)
		{
			case 1:
			{
				requiredMoney = 100000;
				location = MIDWAY_GATE;
				break;
			}
			case 2:
			{
				requiredMoney = 100000;
				location = EASTERN_GATE;
				break;
			}
			case 3:
			{
				requiredMoney = 100000;
				location = WESTERN_GATE;
				break;
			}
			case 4:
			{
				// TODO: Multisell
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
		
		player.teleToLocation(location);
	}
	
	private void schedulePreparation()
	{
		for (int i = 0; i < RAID_TIMES.length; i++)
		{
			final int hour = RAID_TIMES[i][0];
			final int minute = RAID_TIMES[i][1];
			final long currentTimeMillis = System.currentTimeMillis();
			
			final Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.HOUR_OF_DAY, hour);
			calendar.set(Calendar.MINUTE, minute);
			calendar.set(Calendar.SECOND, 0);
			
			if (calendar.getTimeInMillis() <= currentTimeMillis)
			{
				calendar.add(Calendar.DAY_OF_YEAR, 1);
			}
			
			final long timeUntilNextExecution = calendar.getTimeInMillis() - currentTimeMillis;
			ThreadPool.scheduleAtFixedRate(this::spawnMonsters, timeUntilNextExecution, TimeUnit.DAYS.toMillis(1));
		}
	}
	
	public static void main(String[] args)
	{
		new AztacanArea();
	}
}
