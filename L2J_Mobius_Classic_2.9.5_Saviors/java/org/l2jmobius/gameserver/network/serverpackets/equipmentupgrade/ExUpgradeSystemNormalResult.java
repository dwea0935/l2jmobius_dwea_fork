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
package org.l2jmobius.gameserver.network.serverpackets.equipmentupgrade;

import java.util.Map;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.holders.EquipmentUpgradeHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

public class ExUpgradeSystemNormalResult extends ServerPacket
{
	private final boolean _success;
	private final EquipmentUpgradeHolder _upgrade;
	private final Map<Integer, Item> _items;
	
	public ExUpgradeSystemNormalResult(EquipmentUpgradeHolder upgrade, boolean success, Map<Integer, Item> items)
	{
		_upgrade = upgrade;
		_success = success;
		_items = items;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_UPGRADE_SYSTEM_NORMAL_RESULT.writeId(this, buffer);
		buffer.writeShort(1);
		buffer.writeInt(_upgrade.getId());
		buffer.writeByte(_success);
		
		buffer.writeInt(_items.size());
		_items.forEach((objectId, item) ->
		{
			buffer.writeInt(objectId);
			buffer.writeInt(item.getId());
			buffer.writeInt(item.getEnchantLevel());
			buffer.writeInt((int) item.getCount());
		});
		
		buffer.writeShort(0);
		buffer.writeInt(0);
	}
}
