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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.MerchantZeroSellPriceConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.buylist.BuyListHolder;
import org.l2jmobius.gameserver.mechanics.buylist.Product;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author ShanSoft
 */
public class ExBuySellList extends ServerPacket
{
	private final int _buyListId;
	private final List<Product> _buyList = new ArrayList<>();
	private final long _money;
	private double _taxRate = 0;
	private Collection<Item> _sellList = null;
	private Collection<Item> _refundList = null;
	private final boolean _done;
	
	public ExBuySellList(Player player, BuyListHolder list, boolean done)
	{
		_money = player.getAdena();
		_buyListId = list.getListId();
		for (Product item : list.getProducts())
		{
			if (item.hasLimitedStock() && (item.getCount() <= 0))
			{
				continue;
			}
			
			_buyList.add(item);
		}
		
		_sellList = player.getInventory().getAvailableItems(false, false, false);
		if (player.hasRefund())
		{
			_refundList = player.getRefund().getItems();
		}
		
		_done = done;
	}
	
	public ExBuySellList(Player player, BuyListHolder list, double taxRate, boolean done)
	{
		_money = player.getAdena();
		_taxRate = taxRate;
		_buyListId = list.getListId();
		for (Product item : list.getProducts())
		{
			if (item.hasLimitedStock() && (item.getCount() <= 0))
			{
				continue;
			}
			
			_buyList.add(item);
		}
		
		_sellList = player.getInventory().getAvailableItems(false, false, false);
		if (player.hasRefund())
		{
			_refundList = player.getRefund().getItems();
		}
		
		_done = done;
	}
	
	@Override
	protected boolean isProtocolSensitive()
	{
		return true;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_BUY_SELL_LIST.writeId(this, buffer);
		buffer.writeLong(_money);
		buffer.writeInt(_buyListId);
		buffer.writeShort(_buyList.size());
		for (Product item : _buyList)
		{
			buffer.writeShort(item.getItem().getType1());
			buffer.writeInt(0); // objectId
			buffer.writeInt(item.getItemId());
			buffer.writeLong(item.getCount() < 0 ? 0 : item.getCount());
			buffer.writeShort(item.getItem().getType2());
			buffer.writeShort(0); // ?
			if (item.getItem().getType1() != ItemTemplate.TYPE1_ITEM_QUESTITEM_ADENA)
			{
				buffer.writeInt(item.getItem().getBodyPart().getMask());
				buffer.writeShort(0); // item enchant level
				buffer.writeShort(0); // ?
				buffer.writeShort(0);
			}
			else
			{
				buffer.writeInt(0);
				buffer.writeShort(0);
				buffer.writeShort(0);
				buffer.writeShort(0);
			}
			
			if ((item.getItemId() >= 3960) && (item.getItemId() <= 4026))
			{
				buffer.writeLong((long) (item.getPrice() * RatesConfig.RATE_SIEGE_GUARDS_PRICE));
			}
			else
			{
				buffer.writeLong((long) (item.getPrice() * (1 + (_taxRate / 2))));
			}
			
			// T1
			for (byte i = 0; i < 8; i++)
			{
				buffer.writeShort(0);
			}
			
			if (isProtocol152(client))
			{
				buffer.writeShort(0); // Enchant effect 1
				buffer.writeShort(0); // Enchant effect 2
				buffer.writeShort(0); // Enchant effect 3
			}
		}
		
		if ((_sellList != null) && !_sellList.isEmpty())
		{
			buffer.writeShort(_sellList.size());
			for (Item item : _sellList)
			{
				buffer.writeShort(item.getTemplate().getType1());
				buffer.writeInt(item.getObjectId());
				buffer.writeInt(item.getId());
				buffer.writeLong(item.getCount());
				buffer.writeShort(item.getTemplate().getType2());
				buffer.writeShort(0);
				buffer.writeInt(item.getTemplate().getBodyPart().getMask());
				buffer.writeShort(item.getEnchantLevel());
				buffer.writeShort(0);
				buffer.writeShort(0);
				buffer.writeLong(MerchantZeroSellPriceConfig.MERCHANT_ZERO_SELL_PRICE ? 0 : item.getTemplate().getReferencePrice() / 2);
				
				// T1
				buffer.writeShort(item.getAttackElementType());
				buffer.writeShort(item.getAttackElementPower());
				for (byte i = 0; i < 6; i++)
				{
					buffer.writeShort(item.getElementDefAttr(i));
				}
				
				if (isProtocol152(client))
				{
					buffer.writeShort(0); // Enchant effect 1
					buffer.writeShort(0); // Enchant effect 2
					buffer.writeShort(0); // Enchant effect 3
				}
			}
		}
		else
		{
			buffer.writeShort(0);
		}
		
		if ((_refundList != null) && !_refundList.isEmpty())
		{
			buffer.writeShort(_refundList.size());
			int idx = 0;
			for (Item item : _refundList)
			{
				buffer.writeInt(idx++);
				buffer.writeInt(item.getId());
				buffer.writeLong(item.getCount());
				buffer.writeShort(item.getTemplate().getType2());
				buffer.writeShort(0); // ?
				buffer.writeShort(item.getEnchantLevel());
				buffer.writeShort(0); // ?
				buffer.writeLong(MerchantZeroSellPriceConfig.MERCHANT_ZERO_SELL_PRICE ? 0 : (item.getTemplate().getReferencePrice() / 2) * item.getCount());
				
				// T1
				buffer.writeShort(item.getAttackElementType());
				buffer.writeShort(item.getAttackElementPower());
				for (byte i = 0; i < 6; i++)
				{
					buffer.writeShort(item.getElementDefAttr(i));
				}
				
				if (isProtocol152(client))
				{
					buffer.writeShort(0); // Enchant effect 1
					buffer.writeShort(0); // Enchant effect 2
					buffer.writeShort(0); // Enchant effect 3
				}
			}
		}
		else
		{
			buffer.writeShort(0);
		}
		
		buffer.writeByte(_done);
	}
}
