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
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.ItemContainer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.holders.MailMessage;

/**
 * ExReplySentPost packet implementation.
 * @author Migi, DS
 */
public class ExReplySentPost extends ServerPacket
{
	private final MailMessage _msg;
	private Collection<Item> _items = null;
	
	public ExReplySentPost(MailMessage msg)
	{
		_msg = msg;
		if (msg.hasAttachments())
		{
			final ItemContainer attachments = msg.getAttachments();
			if ((attachments != null) && (attachments.getSize() > 0))
			{
				_items = attachments.getItems();
			}
			else
			{
				PacketLogger.warning("Message " + msg.getId() + " has attachments but itemcontainer is empty.");
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_REPLY_SENT_POST.writeId(this, buffer);
		buffer.writeInt(_msg.getId());
		buffer.writeInt(_msg.isLocked());
		buffer.writeString(_msg.getReceiverName());
		buffer.writeString(_msg.getSubject());
		buffer.writeString(_msg.getContent());
		if ((_items != null) && !_items.isEmpty())
		{
			buffer.writeInt(_items.size());
			for (Item item : _items)
			{
				buffer.writeShort(item.getTemplate().getType2());
				buffer.writeInt(0); // unknown
				buffer.writeInt(item.getId());
				buffer.writeLong(item.getCount());
				buffer.writeInt(item.getEnchantLevel());
				buffer.writeShort(item.getCustomType2());
				buffer.writeShort(0); // unknown
				buffer.writeInt(0); // unknown
				buffer.writeInt(item.isAugmented() ? item.getAugmentation().getAugmentationId() : 0x00);
				buffer.writeInt(0); // unknown
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
			
			buffer.writeLong(_msg.getReqAdena());
			buffer.writeInt(_msg.getSendBySystem());
		}
		else
		{
			buffer.writeInt(0);
			buffer.writeLong(_msg.getReqAdena());
		}
	}
}
