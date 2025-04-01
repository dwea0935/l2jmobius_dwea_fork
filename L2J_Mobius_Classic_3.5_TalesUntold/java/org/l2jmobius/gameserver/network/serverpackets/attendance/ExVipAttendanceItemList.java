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
package org.l2jmobius.gameserver.network.serverpackets.attendance;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.xml.AttendanceRewardData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.AttendanceInfoHolder;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Mobius
 */
public class ExVipAttendanceItemList extends ServerPacket
{
	boolean _available;
	int _index;
	
	public ExVipAttendanceItemList(Player player)
	{
		final AttendanceInfoHolder attendanceInfo = player.getAttendanceInfo();
		_available = attendanceInfo.isRewardAvailable();
		_index = attendanceInfo.getRewardIndex();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_VIP_ATTENDANCE_ITEM_LIST.writeId(this, buffer);
		buffer.writeByte(_available ? _index + 1 : _index); // index to receive?
		buffer.writeByte(_index); // last received index?
		buffer.writeInt(0);
		buffer.writeInt(0);
		buffer.writeByte(1);
		buffer.writeByte(_available); // player can receive reward today?
		buffer.writeByte(250);
		buffer.writeByte(AttendanceRewardData.getInstance().getRewardsCount()); // reward size
		int rewardCounter = 0;
		for (ItemHolder reward : AttendanceRewardData.getInstance().getRewards())
		{
			rewardCounter++;
			buffer.writeInt(reward.getId());
			buffer.writeLong(reward.getCount());
			buffer.writeByte(1); // is unknown?
			buffer.writeByte((rewardCounter % 7) == 0); // is last in row?
		}
		buffer.writeByte(0);
		buffer.writeInt(0);
	}
}
