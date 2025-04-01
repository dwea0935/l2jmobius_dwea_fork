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
package org.l2jmobius.gameserver.network.serverpackets.vip;

import java.util.Collection;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.xml.PrimeShopData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.primeshop.PrimeShopGroup;
import org.l2jmobius.gameserver.model.primeshop.PrimeShopItem;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

public class ReceiveVipProductList extends ServerPacket
{
	private final Player _player;
	
	public ReceiveVipProductList(Player player)
	{
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		if (!Config.VIP_SYSTEM_ENABLED)
		{
			return;
		}
		
		final Collection<PrimeShopGroup> products = PrimeShopData.getInstance().getPrimeItems().values();
		final PrimeShopGroup gift = PrimeShopData.getInstance().getVipGiftOfTier(_player.getVipTier());
		ServerPackets.RECIVE_VIP_PRODUCT_LIST.writeId(this, buffer);
		buffer.writeLong(_player.getAdena());
		buffer.writeLong(_player.getGoldCoin()); // Gold Coin Amount
		buffer.writeLong(_player.getSilverCoin()); // Silver Coin Amount
		buffer.writeByte(1); // Show Reward tab
		if (gift != null)
		{
			buffer.writeInt(products.size() + 1);
			writeProduct(gift, buffer);
		}
		else
		{
			buffer.writeInt(products.size());
		}
		for (PrimeShopGroup product : products)
		{
			writeProduct(product, buffer);
		}
	}
	
	private void writeProduct(PrimeShopGroup product, WritableBuffer buffer)
	{
		buffer.writeInt(product.getBrId());
		buffer.writeByte(product.getCat());
		buffer.writeByte(product.getPaymentType());
		buffer.writeInt(product.getPrice()); // L2 Coin | Gold Coin seems to use the same field based on payment type
		buffer.writeInt(product.getSilverCoin());
		buffer.writeByte(product.getPanelType()); // NEW - 6; HOT - 5 ... Unk
		buffer.writeByte(product.getVipTier());
		buffer.writeByte(10);
		buffer.writeByte(product.getItems().size());
		for (PrimeShopItem item : product.getItems())
		{
			buffer.writeInt(item.getId());
			buffer.writeInt((int) item.getCount());
		}
	}
}
