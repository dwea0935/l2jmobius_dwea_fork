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
package org.l2jmobius.gameserver.network.serverpackets.huntingzones;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.holders.TimedHuntingZoneHolder;
import org.l2jmobius.gameserver.data.xml.TimedHuntingZoneData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Mobius
 */
public class TimedHuntingZoneList extends ServerPacket
{
	private final Player _player;
	private final boolean _isInTimedHuntingZone;
	
	public TimedHuntingZoneList(Player player)
	{
		_player = player;
		_isInTimedHuntingZone = player.isInsideZone(ZoneId.TIMED_HUNTING);
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_TIME_RESTRICT_FIELD_LIST.writeId(this, buffer);
		final long currentTime = System.currentTimeMillis();
		buffer.writeInt(TimedHuntingZoneData.getInstance().getSize()); // zone count
		for (TimedHuntingZoneHolder holder : TimedHuntingZoneData.getInstance().getAllHuntingZones())
		{
			buffer.writeInt(holder.getEntryFee() != 0); // required item count
			buffer.writeInt(holder.getEntryItemId());
			buffer.writeLong(holder.getEntryFee());
			buffer.writeInt(!holder.isWeekly()); // reset cycle
			buffer.writeInt(holder.getZoneId());
			buffer.writeInt(holder.getMinLevel());
			buffer.writeInt(holder.getMaxLevel());
			buffer.writeInt(holder.getInitialTime() / 1000); // remain time base
			int remainingTime = _player.getTimedHuntingZoneRemainingTime(holder.getZoneId());
			if ((remainingTime == 0) && ((_player.getTimedHuntingZoneInitialEntry(holder.getZoneId()) + holder.getResetDelay()) < currentTime))
			{
				remainingTime = holder.getInitialTime();
			}
			buffer.writeInt(remainingTime / 1000); // remain time
			buffer.writeInt(holder.getMaximumAddedTime() / 1000);
			buffer.writeInt(_player.getVariables().getInt(PlayerVariables.HUNTING_ZONE_REMAIN_REFILL + holder.getZoneId(), holder.getRemainRefillTime()));
			buffer.writeInt(holder.getRefillTimeMax());
			buffer.writeByte(!_isInTimedHuntingZone); // field activated
		}
	}
}