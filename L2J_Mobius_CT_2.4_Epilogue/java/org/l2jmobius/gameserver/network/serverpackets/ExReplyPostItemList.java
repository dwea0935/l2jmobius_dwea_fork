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

import java.util.Collection;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Migi, DS
 */
public class ExReplyPostItemList extends ServerPacket
{
	private final Player _player;
	private final Collection<Item> _itemList;
	
	public ExReplyPostItemList(Player player)
	{
		_player = player;
		_itemList = _player.getInventory().getAvailableItems(true, false, false);
	}
	
	@Override
	protected boolean isProtocolSensitive()
	{
		return true;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_REPLY_POST_ITEM_LIST.writeId(this, buffer);
		buffer.writeInt(_itemList.size());
		for (Item item : _itemList)
		{
			buffer.writeInt(item.getObjectId());
			buffer.writeInt(item.getId());
			buffer.writeLong(item.getCount());
			buffer.writeShort(item.getTemplate().getType2());
			buffer.writeShort(item.getCustomType1());
			buffer.writeInt(item.getTemplate().getBodyPart().getMask());
			buffer.writeShort(item.getEnchantLevel());
			buffer.writeShort(item.getCustomType2());
			buffer.writeShort(item.getAttackElementType());
			buffer.writeShort(item.getAttackElementPower());
			for (byte i = 0; i < 6; i++)
			{
				buffer.writeShort(item.getElementDefAttr(i));
			}
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
