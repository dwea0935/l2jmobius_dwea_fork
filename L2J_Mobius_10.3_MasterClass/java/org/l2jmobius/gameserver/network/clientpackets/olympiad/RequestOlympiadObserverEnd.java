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
package org.l2jmobius.gameserver.network.clientpackets.olympiad;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.olympiad.OlympiadMode;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.olympiad.ExOlympiadMode;

/**
 * format ch c: (id) 0xD0 h: (subid) 0x12
 * @author -Wooden-
 */
public class RequestOlympiadObserverEnd extends ClientPacket
{
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (player.inObserverMode())
		{
			player.leaveOlympiadObserverMode();
		}
		else
		{
			player.sendPacket(new ExOlympiadMode(OlympiadMode.NONE));
		}
	}
}