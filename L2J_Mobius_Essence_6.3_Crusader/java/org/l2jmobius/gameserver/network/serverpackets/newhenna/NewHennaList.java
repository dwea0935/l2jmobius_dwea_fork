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
package org.l2jmobius.gameserver.network.serverpackets.newhenna;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.henna.Henna;
import org.l2jmobius.gameserver.model.item.henna.HennaPoten;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Index, Serenitty
 */
public class NewHennaList extends ServerPacket
{
	private final HennaPoten[] _hennaId;
	private final int _dailyStep;
	private final int _dailyCount;
	private final int _availableSlots;
	
	public NewHennaList(Player player)
	{
		_dailyStep = player.getDyePotentialDailyStep();
		_dailyCount = player.getDyePotentialDailyCount();
		_hennaId = player.getHennaPotenList();
		_availableSlots = player.getAvailableHennaSlots();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_NEW_HENNA_LIST.writeId(this, buffer);
		buffer.writeShort(_dailyStep);
		buffer.writeShort(_dailyCount);
		buffer.writeInt(_hennaId.length);
		for (int i = 1; i <= _hennaId.length; i++)
		{
			final HennaPoten hennaPoten = _hennaId[i - 1];
			final Henna henna = _hennaId[i - 1].getHenna();
			buffer.writeInt(henna != null ? henna.getDyeId() : 0);
			buffer.writeInt(hennaPoten.getPotenId());
			buffer.writeByte(i != _availableSlots);
			buffer.writeShort(hennaPoten.getEnchantLevel());
			buffer.writeInt(hennaPoten.getEnchantExp());
			buffer.writeShort(hennaPoten.getActiveStep());
			buffer.writeShort(_dailyStep);
			buffer.writeShort(_dailyCount);
		}
	}
}
