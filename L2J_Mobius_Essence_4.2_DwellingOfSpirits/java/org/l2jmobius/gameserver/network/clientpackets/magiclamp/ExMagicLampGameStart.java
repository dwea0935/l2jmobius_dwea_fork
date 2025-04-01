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
package org.l2jmobius.gameserver.network.clientpackets.magiclamp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.enums.LampMode;
import org.l2jmobius.gameserver.data.enums.LampType;
import org.l2jmobius.gameserver.data.holders.MagicLampDataHolder;
import org.l2jmobius.gameserver.data.holders.MagicLampHolder;
import org.l2jmobius.gameserver.data.xml.MagicLampData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.magiclamp.ExMagicLampExpInfoUI;
import org.l2jmobius.gameserver.network.serverpackets.magiclamp.ExMagicLampGameInfoUI;
import org.l2jmobius.gameserver.network.serverpackets.magiclamp.ExMagicLampGameResult;

/**
 * @author L2CCCP
 */
public class ExMagicLampGameStart extends ClientPacket
{
	private int _count;
	private byte _mode;
	
	@Override
	protected void readImpl()
	{
		_count = readInt(); // MagicLampGameCCount
		_mode = readByte(); // GameMode
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (_count < 1)
		{
			return;
		}
		
		final LampMode lampMode = LampMode.getByMode(_mode);
		final int consume = calcConsume(lampMode, _count);
		if (consume < 1)
		{
			return;
		}
		
		final int have = player.getLampCount();
		if (have >= consume)
		{
			final Map<LampType, MagicLampHolder> rewards = new HashMap<>();
			for (int x = _count; x > 0; x--)
			{
				final List<MagicLampDataHolder> available = MagicLampData.getInstance().getLamps().stream().filter(lamp -> (lamp.getMode() == lampMode) && chance(lamp.getChance())).collect(Collectors.toList());
				final MagicLampDataHolder random = getRandom(available);
				if (random != null)
				{
					rewards.computeIfAbsent(random.getType(), _ -> new MagicLampHolder(random)).inc();
				}
			}
			
			// Consume.
			player.setLampCount(have - consume);
			if (lampMode == LampMode.GREATER)
			{
				player.destroyItemByItemId(ItemProcessType.FEE, 91641, Config.MAGIC_LAMP_GREATER_SAYHA_CONSUME_COUNT * _count, player, true);
			}
			
			// Reward.
			rewards.values().forEach(lamp -> player.addExpAndSp(lamp.getExp(), lamp.getSp()));
			
			// Update.
			final int left = player.getLampCount();
			player.sendPacket(new ExMagicLampGameInfoUI(player, _mode, left > consume ? _count : left));
			player.sendPacket(new ExMagicLampExpInfoUI(player));
			player.sendPacket(new ExMagicLampGameResult(rewards.values()));
		}
	}
	
	private boolean chance(double chance)
	{
		return (chance > 0) && ((chance >= 100) || (Rnd.get(100d) <= chance));
	}
	
	private <E> E getRandom(List<E> list)
	{
		if (list.isEmpty())
		{
			return null;
		}
		if (list.size() == 1)
		{
			return list.get(0);
		}
		return list.get(Rnd.get(list.size()));
	}
	
	private int calcConsume(LampMode mode, int count)
	{
		switch (mode)
		{
			case NORMAL:
			{
				return Config.MAGIC_LAMP_CONSUME_COUNT * count;
			}
			case GREATER:
			{
				return Config.MAGIC_LAMP_GREATER_CONSUME_COUNT * count;
			}
			default:
			{
				return 0;
			}
		}
	}
}
