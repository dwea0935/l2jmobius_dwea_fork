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
package org.l2jmobius.gameserver.network.clientpackets.herobook;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.herobook.HeroBookManager;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.herobook.ExHeroBookCharge;
import org.l2jmobius.gameserver.network.serverpackets.herobook.ExHeroBookInfo;

/**
 * @author Index
 */
public class RequestHeroBookCharge extends ClientPacket
{
	private final Map<Integer, Long> _items = new HashMap<>();
	
	@Override
	protected void readImpl()
	{
		final int size = readInt();
		for (int curr = 0; curr < size; curr++)
		{
			final int id = readInt();
			final long count = readLong();
			if ((count < 1) || (count > 10000))
			{
				_items.clear();
				return;
			}
			_items.put(id, count);
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (_items.isEmpty())
		{
			return;
		}
		
		final boolean isSuccess = new HeroBookManager().tryEnchant(player, _items);
		player.sendPacket(new ExHeroBookCharge(isSuccess));
		player.sendPacket(new ExHeroBookInfo(player.getHeroBookProgress()));
	}
}
