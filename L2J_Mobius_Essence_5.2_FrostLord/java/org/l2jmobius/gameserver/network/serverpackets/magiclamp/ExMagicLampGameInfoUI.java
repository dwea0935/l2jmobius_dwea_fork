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
package org.l2jmobius.gameserver.network.serverpackets.magiclamp;

import java.util.List;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.holders.GreaterMagicLampHolder;
import org.l2jmobius.gameserver.data.xml.MagicLampData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author L2CCCP
 */
public class ExMagicLampGameInfoUI extends ServerPacket
{
	private final Player _player;
	private final byte _mode;
	private final int _count;
	
	public ExMagicLampGameInfoUI(Player player, byte mode, int count)
	{
		_player = player;
		_mode = mode;
		_count = count;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_MAGICLAMP_GAME_INFO.writeId(this, buffer);
		buffer.writeInt(_player.getMaxLampCount()); // MagicLampGameMaxCCount
		buffer.writeInt(_count); // MagicLampGameCCount
		buffer.writeInt(_mode == 0 ? Config.MAGIC_LAMP_CONSUME_COUNT : Config.MAGIC_LAMP_GREATER_CONSUME_COUNT); // MagicLampCountPerGame
		buffer.writeInt(_player.getLampCount()); // MagicLampCount
		buffer.writeByte(_mode); // GameMode
		final List<GreaterMagicLampHolder> greater = MagicLampData.getInstance().getGreaterLamps();
		buffer.writeInt(greater.size()); // costItemList
		for (GreaterMagicLampHolder lamp : greater)
		{
			buffer.writeInt(lamp.getItemId()); // ItemClassID
			buffer.writeLong(lamp.getCount()); // ItemAmountPerGame
			buffer.writeLong(_player.getInventory().getInventoryItemCount(lamp.getItemId(), -1)); // ItemAmount
		}
	}
}
