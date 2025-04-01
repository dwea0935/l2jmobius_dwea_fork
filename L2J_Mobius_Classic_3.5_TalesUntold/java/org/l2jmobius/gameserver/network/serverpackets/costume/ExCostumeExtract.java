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
package org.l2jmobius.gameserver.network.serverpackets.costume;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.sql.CostumeTable;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author JoeAlisson
 */
public class ExCostumeExtract extends ServerPacket
{
	private int _costumeId;
	private boolean _success;
	private long _amount;
	private int _extractedItem;
	private long _totalAmount;
	
	private ExCostumeExtract()
	{
	}
	
	public static ExCostumeExtract failed(int costumeId)
	{
		final ExCostumeExtract packet = new ExCostumeExtract();
		packet._costumeId = costumeId;
		return packet;
	}
	
	public static ExCostumeExtract success(CostumeTable costume, int extractItem, long amount)
	{
		final ExCostumeExtract packet = new ExCostumeExtract();
		packet._costumeId = costume.getId();
		packet._success = true;
		packet._extractedItem = extractItem;
		packet._amount = amount;
		packet._totalAmount = costume.getAmount();
		return packet;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_COSTUME_EXTRACT.writeId(this, buffer);
		buffer.writeByte(_success);
		buffer.writeInt(_costumeId);
		buffer.writeLong(_amount);
		buffer.writeInt(_extractedItem);
		buffer.writeLong(_amount);
		buffer.writeLong(_totalAmount);
	}
}
