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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.data.holders.TeleportListHolder;
import org.l2jmobius.gameserver.data.xml.TeleportListData;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.model.effects.EffectFlag;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * @author NviX, Mobius
 */
public class ExRequestTeleport extends ClientPacket
{
	private int _teleportId;
	
	@Override
	protected void readImpl()
	{
		_teleportId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final TeleportListHolder teleport = TeleportListData.getInstance().getTeleport(_teleportId);
		if (teleport == null)
		{
			PacketLogger.warning("No registered teleport location for id: " + _teleportId);
			return;
		}
		
		// Dead characters cannot use teleports.
		if (player.isDead())
		{
			player.sendMessage("Dead characters cannot use teleports.");
			return;
		}
		
		// Players should not be able to teleport if in a special location.
		if ((player.getMovieHolder() != null) || player.isFishing() || player.isInInstance() || player.isOnEvent() || player.isInOlympiadMode() || player.inObserverMode() || player.isInTraingCamp() || (player.getPrivateStoreType() != PrivateStoreType.NONE))
		{
			player.sendMessage("You cannot teleport right now.");
			return;
		}
		
		// Teleport in combat configuration.
		if (!Config.TELEPORT_WHILE_PLAYER_IN_COMBAT && (player.isInCombat() || player.isCastingNow()))
		{
			player.sendMessage("You cannot teleport in combat.");
			return;
		}
		
		// Karma related configurations.
		if ((!Config.ALT_GAME_KARMA_PLAYER_CAN_TELEPORT || !Config.ALT_GAME_KARMA_PLAYER_CAN_USE_GK) && (player.getReputation() < 0))
		{
			player.sendMessage("You cannot teleport right now.");
			return;
		}
		
		// Cannot escape effect.
		if (player.isAffected(EffectFlag.CANNOT_ESCAPE))
		{
			player.sendMessage("You cannot teleport right now.");
			return;
		}
		
		final Location location = teleport.getLocation();
		if (!Config.TELEPORT_WHILE_SIEGE_IN_PROGRESS)
		{
			final Castle castle = CastleManager.getInstance().getCastle(location.getX(), location.getY(), location.getZ());
			if ((castle != null) && castle.getSiege().isInProgress())
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_TO_A_VILLAGE_THAT_IS_IN_A_SIEGE);
				return;
			}
		}
		
		if (player.getLevel() > Config.MAX_FREE_TELEPORT_LEVEL)
		{
			final int price = teleport.getPrice();
			if (price > 0)
			{
				if (player.getAdena() < price)
				{
					player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
					return;
				}
				player.reduceAdena(ItemProcessType.FEE, price, player, true);
			}
		}
		
		player.abortCast();
		player.stopMove(null);
		player.teleToLocation(location);
	}
}