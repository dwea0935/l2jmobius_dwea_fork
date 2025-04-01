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
package org.l2jmobius.gameserver.managers;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;

/**
 * This class manage the Balthus Knight Path.
 * @author Kazumi
 */
public final class BalthusKnightManager
{
	/**
	 * Remove Dragon Weapons on enter world
	 * @param player
	 */
	public static final void removeDragonWeapons(Player player)
	{
		PlayerInventory inv = player.getInventory();
		
		if (inv.getItemByItemId(48194) != null)
		{
			inv.destroyItemByItemId(ItemProcessType.DESTROY, 48194, 1, player, player);
		}
		else if (inv.getItemByItemId(48195) != null)
		{
			inv.destroyItemByItemId(ItemProcessType.DESTROY, 48195, 1, player, player);
		}
		else if (inv.getItemByItemId(48196) != null)
		{
			inv.destroyItemByItemId(ItemProcessType.DESTROY, 48196, 1, player, player);
		}
		else if (inv.getItemByItemId(48197) != null)
		{
			inv.destroyItemByItemId(ItemProcessType.DESTROY, 48197, 1, player, player);
		}
		else if (inv.getItemByItemId(48198) != null)
		{
			inv.destroyItemByItemId(ItemProcessType.DESTROY, 48198, 1, player, player);
		}
		else if (inv.getItemByItemId(48199) != null)
		{
			inv.destroyItemByItemId(ItemProcessType.DESTROY, 48199, 1, player, player);
		}
		else if (inv.getItemByItemId(48200) != null)
		{
			inv.destroyItemByItemId(ItemProcessType.DESTROY, 48200, 1, player, player);
		}
		else if (inv.getItemByItemId(48201) != null)
		{
			inv.destroyItemByItemId(ItemProcessType.DESTROY, 48201, 1, player, player);
		}
	}
	
	/**
	 * Check spawn location on enter world
	 * @param player
	 */
	public final static void checkSpawnLocation(Player player)
	{
		switch (player.getVariables().getInt(PlayerVariables.BALTHUS_PHASE, 1))
		{
			case 1:
			{
				// Teleport to Nest
				player.teleToLocation(190441, 168020, -11232);
				break;
			}
			case 2:
			{
				// Teleport to Barracks
				player.teleToLocation(178306, 177018, -12104);
				break;
			}
			default:
			{
				// Reset Phase
				player.getVariables().set(PlayerVariables.BALTHUS_PHASE, 2);
				// Teleport to Barracks
				player.teleToLocation(178306, 177018, -12104);
				break;
			}
		}
	}
	
	/**
	 * Gets the single instance of {@code BalthusKnightManager}.
	 * @return single instance of {@code BalthusKnightManager}
	 */
	public static BalthusKnightManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final BalthusKnightManager INSTANCE = new BalthusKnightManager();
	}
}
