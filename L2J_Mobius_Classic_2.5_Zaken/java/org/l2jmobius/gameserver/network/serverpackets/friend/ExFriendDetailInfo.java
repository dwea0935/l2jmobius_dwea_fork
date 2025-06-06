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
package org.l2jmobius.gameserver.network.serverpackets.friend;

import java.util.Calendar;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Sdw
 */
public class ExFriendDetailInfo extends ServerPacket
{
	private final int _objectId;
	private final Player _friend;
	private final String _name;
	private final int _lastAccess;
	private final boolean _isOnline;
	private final int _friendObjectId;
	private final int _level;
	private final int _classId;
	private final int _clanId;
	private final int _clanCrestId;
	private final String _clanName;
	private final int _allyId;
	private final int _allyCrestId;
	private final String _allyName;
	private final Calendar _createDate;
	private final int _lastAccessDelay;
	private final String _friendMemo;
	
	public ExFriendDetailInfo(Player player, String name)
	{
		_objectId = player.getObjectId();
		_name = name;
		_friend = World.getInstance().getPlayer(_name);
		_lastAccess = (_friend == null) || _friend.isBlocked(player) ? 0 : _friend.isOnline() ? (int) System.currentTimeMillis() : (int) (System.currentTimeMillis() - _friend.getLastAccess()) / 1000;
		
		final CharInfoTable charInfoTable = CharInfoTable.getInstance();
		if (_friend == null)
		{
			final int charId = charInfoTable.getIdByName(_name);
			_isOnline = false;
			_friendObjectId = charId;
			_level = charInfoTable.getLevelById(charId);
			_classId = charInfoTable.getClassIdById(charId);
			
			final Clan clan = ClanTable.getInstance().getClan(charInfoTable.getClanIdById(charId));
			if (clan != null)
			{
				_clanId = clan.getId();
				_clanCrestId = clan.getCrestId();
				_clanName = clan.getName();
				_allyId = clan.getAllyId();
				_allyCrestId = clan.getAllyCrestId();
				_allyName = clan.getAllyName();
			}
			else
			{
				_clanId = 0;
				_clanCrestId = 0;
				_clanName = "";
				_allyId = 0;
				_allyCrestId = 0;
				_allyName = "";
			}
			
			_createDate = charInfoTable.getCharacterCreationDate(charId);
			_lastAccessDelay = charInfoTable.getLastAccessDelay(charId);
			_friendMemo = charInfoTable.getFriendMemo(_objectId, charId);
		}
		else
		{
			_isOnline = _friend.isOnlineInt() == 1;
			_friendObjectId = _friend.getObjectId();
			_level = _friend.getLevel();
			_classId = _friend.getPlayerClass().getId();
			
			_clanId = _friend.getClanId();
			_clanCrestId = _friend.getClanCrestId();
			
			final Clan clan = _friend.getClan();
			_clanName = clan != null ? clan.getName() : "";
			_allyId = _friend.getAllyId();
			_allyCrestId = _friend.getAllyCrestId();
			_allyName = clan != null ? clan.getAllyName() : "";
			
			_createDate = _friend.getCreateDate();
			_lastAccessDelay = _lastAccess;
			_friendMemo = charInfoTable.getFriendMemo(_objectId, _friend.getObjectId());
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_FRIEND_DETAIL_INFO.writeId(this, buffer);
		buffer.writeInt(_objectId);
		buffer.writeString(_name);
		buffer.writeInt(_isOnline);
		buffer.writeInt(_friendObjectId);
		buffer.writeShort(_level);
		buffer.writeShort(_classId);
		buffer.writeInt(_clanId);
		buffer.writeInt(_clanCrestId);
		buffer.writeString(_clanName);
		buffer.writeInt(_allyId);
		buffer.writeInt(_allyCrestId);
		buffer.writeString(_allyName);
		buffer.writeByte(_createDate.get(Calendar.MONTH) + 1);
		buffer.writeByte(_createDate.get(Calendar.DAY_OF_MONTH));
		buffer.writeInt(_lastAccessDelay);
		buffer.writeString(_friendMemo);
	}
}
