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

import java.util.Collection;
import java.util.Set;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.sql.CostumeTable;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author JoeAlisson
 */
public class ExCostumeEvolution extends ServerPacket
{
	private boolean _success;
	private Collection<CostumeTable> _targetCostumes;
	private CostumeTable _resultCostume;
	
	private ExCostumeEvolution()
	{
	}
	
	public static ExCostumeEvolution failed()
	{
		return new ExCostumeEvolution();
	}
	
	public static ExCostumeEvolution success(Set<CostumeTable> costume, CostumeTable resultCostume)
	{
		final ExCostumeEvolution packet = new ExCostumeEvolution();
		packet._success = true;
		packet._targetCostumes = costume;
		packet._resultCostume = resultCostume;
		return packet;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_COSTUME_EVOLUTION.writeId(this, buffer);
		
		buffer.writeByte(_success);
		if (_targetCostumes != null)
		{
			buffer.writeInt(_targetCostumes.size());
			for (CostumeTable targetCostume : _targetCostumes)
			{
				buffer.writeInt(targetCostume.getId());
				buffer.writeLong(targetCostume.getAmount());
			}
		}
		else
		{
			buffer.writeInt(0); // Handle the case where targetCostumes is null
		}
		if (_resultCostume != null)
		{
			buffer.writeInt(1);
			buffer.writeInt(_resultCostume.getId());
			buffer.writeLong(_resultCostume.getAmount());
		}
		else
		{
			buffer.writeInt(0);
		}
	}
}
