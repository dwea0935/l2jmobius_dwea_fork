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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.ClanMember;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author -Wooden-
 */
public class PledgeReceiveMemberInfo extends ServerPacket
{
	private final ClanMember _member;
	private final Player _player;
	
	public PledgeReceiveMemberInfo(ClanMember member, Player player)
	{
		_member = member;
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.PLEDGE_RECEIVE_MEMBER_INFO.writeId(this, buffer);
		buffer.writeInt(_member.getClan().getId());
		buffer.writeString(_member.getClan().getName());
		buffer.writeString(_member.getClan().getLeaderName());
		buffer.writeInt(_member.getClan().getCrestId()); // crest id .. is used again
		buffer.writeInt(_member.getClan().getLevel());
		buffer.writeInt(_member.getClan().getCastleId());
		buffer.writeInt(_member.getClan().getHideoutId());
		buffer.writeInt(0);
		buffer.writeInt(_player.getLevel()); // ??
		buffer.writeInt(_member.getClan().getDissolvingExpiryTime() > System.currentTimeMillis() ? 3 : 0);
		buffer.writeInt(0);
		buffer.writeInt(_member.getClan().getAllyId());
		buffer.writeString(_member.getClan().getAllyName());
		buffer.writeInt(_member.getClan().getAllyCrestId());
		buffer.writeInt(_member.getClan().isAtWar()); // new c3
		buffer.writeInt(_member.getClan().getMembers().length - 1);
		for (ClanMember m : _member.getClan().getMembers())
		{
			// TODO is this c4?
			if (m.getObjectId() == _player.getObjectId())
			{
				continue;
			}
			buffer.writeString(m.getName());
			buffer.writeInt(m.getLevel());
			buffer.writeInt(m.getClassId());
			buffer.writeInt(0);
			buffer.writeInt(1);
			buffer.writeInt(m.isOnline() ? m.getObjectId() : 0); // 1=online 0=offline
		}
	}
}
