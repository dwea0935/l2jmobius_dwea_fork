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
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class GMViewWarehouseWithdrawList extends AbstractItemPacket
{
	private final Collection<Item> _items;
	private final String _playerName;
	private final long _money;
	
	public GMViewWarehouseWithdrawList(Player player)
	{
		_items = player.getWarehouse().getItems();
		_playerName = player.getName();
		_money = player.getWarehouse().getAdena();
	}
	
	public GMViewWarehouseWithdrawList(Clan clan)
	{
		_playerName = clan.getLeaderName();
		_items = clan.getWarehouse().getItems();
		_money = clan.getWarehouse().getAdena();
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.GM_VIEW_WAREHOUSE_WITHDRAW_LIST.writeId(this, buffer);
		buffer.writeString(_playerName);
		buffer.writeLong(_money);
		buffer.writeShort(_items.size());
		for (Item item : _items)
		{
			buffer.writeShort(item.getTemplate().getType1());
			buffer.writeInt(item.getObjectId());
			buffer.writeInt(item.getId());
			buffer.writeLong(item.getCount());
			buffer.writeShort(item.getTemplate().getType2());
			buffer.writeShort(item.getCustomType1());
			if (item.getTemplate().getType1() <= 3)
			{
				buffer.writeInt(item.getTemplate().getBodyPart().getMask());
				buffer.writeShort(item.getEnchantLevel());
				buffer.writeShort(0);
				buffer.writeShort(item.getCustomType2());
				buffer.writeInt(item.getObjectId());
				if (item.isAugmented())
				{
					buffer.writeInt(0x0000FFFF & item.getAugmentation().getAugmentationId());
					buffer.writeInt(item.getAugmentation().getAugmentationId() >> 16);
				}
				else
				{
					buffer.writeLong(0);
				}
				
				buffer.writeShort(item.getAttackElementType());
				buffer.writeShort(item.getAttackElementPower());
				for (byte i = 0; i < 6; i++)
				{
					buffer.writeShort(item.getElementDefAttr(i));
				}
			}
			
			buffer.writeInt(item.getMana());
			buffer.writeInt(item.isTimeLimitedItem() ? (int) (item.getRemainingTime() / 1000) : -9999);
			if (isProtocol152(client))
			{
				for (int op : item.getEnchantOptions())
				{
					buffer.writeShort(op);
				}
			}
		}
	}
}
