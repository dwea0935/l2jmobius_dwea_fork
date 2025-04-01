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
package handlers.itemhandlers;

import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class Elixir extends ItemSkills
{
	@Override
	public boolean useItem(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.YOUR_PET_CANNOT_CARRY_THIS_ITEM);
			return false;
		}
		
		final Player player = playable.asPlayer();
		final int elixirsAvailable = player.getVariables().getInt(PlayerVariables.ELIXIRS_AVAILABLE, 0);
		if ((player.getLevel() < 76) || //
			((player.getLevel() < 88) && (elixirsAvailable >= 5)) || //
			((player.getLevel() < 91) && (elixirsAvailable >= 10)) || //
			((player.getLevel() < 92) && (elixirsAvailable >= 11)) || //
			((player.getLevel() < 93) && (elixirsAvailable >= 12)) || //
			((player.getLevel() < 94) && (elixirsAvailable >= 13)) || //
			((player.getLevel() < 95) && (elixirsAvailable >= 14)) || //
			((player.getLevel() < 100) && (elixirsAvailable >= 15)))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_ANY_MORE_ELIXIRS);
			return false;
		}
		
		if (super.useItem(player, item, forceUse))
		{
			player.getVariables().set(PlayerVariables.ELIXIRS_AVAILABLE, elixirsAvailable + 1);
			player.sendPacket(new SystemMessage(SystemMessageId.YOUR_CHARACTER_S_STATS_BONUS_POINTS_INCREASED_BY_S1_WITH_THE_ELIXIR).addInt(elixirsAvailable + 1));
			player.broadcastUserInfo();
			return true;
		}
		return false;
	}
}
