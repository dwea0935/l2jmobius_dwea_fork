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
import org.l2jmobius.gameserver.network.serverpackets.ExPledgeDraftListSearch;
import org.l2jmobius.gameserver.util.MathUtil;

/**
 * @author Sdw
 */
public class RequestPledgeDraftListSearch extends ClientPacket
{
	private int _levelMin;
	private int _levelMax;
	private int _classId;
	private String _query;
	private int _sortBy;
	private boolean _descending;
	
	@Override
	protected void readImpl()
	{
		_levelMin = MathUtil.clamp(readInt(), 0, 107);
		_levelMax = MathUtil.clamp(readInt(), 0, 107);
		_classId = readInt();
		_query = readString();
		_sortBy = readInt();
		_descending = readInt() == 2;
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (_query.isEmpty())
		{
			player.sendPacket(new ExPledgeDraftListSearch(ClanEntryManager.getInstance().getSortedWaitingList(_levelMin, _levelMax, _classId, _sortBy, _descending)));
		}
		else
		{
			player.sendPacket(new ExPledgeDraftListSearch(ClanEntryManager.getInstance().queryWaitingListByName(_query.toLowerCase())));
		}
	}
}
