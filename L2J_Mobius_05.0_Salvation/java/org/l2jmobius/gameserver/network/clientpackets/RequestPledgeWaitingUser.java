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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.managers.ClanEntryManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.entry.PledgeApplicantInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExPledgeWaitingList;
import org.l2jmobius.gameserver.network.serverpackets.ExPledgeWaitingUser;

/**
 * @author Sdw
 */
public class RequestPledgeWaitingUser extends ClientPacket
{
	private int _clanId;
	private int _playerId;
	
	@Override
	protected void readImpl()
	{
		_clanId = readInt();
		_playerId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if ((player == null) || (player.getClanId() != _clanId))
		{
			return;
		}
		
		final PledgeApplicantInfo infos = ClanEntryManager.getInstance().getPlayerApplication(_clanId, _playerId);
		if (infos == null)
		{
			player.sendPacket(new ExPledgeWaitingList(_clanId));
		}
		else
		{
			player.sendPacket(new ExPledgeWaitingUser(infos));
		}
	}
}