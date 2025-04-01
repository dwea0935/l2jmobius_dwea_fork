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
package org.l2jmobius.gameserver.network.clientpackets.costume;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.l2jmobius.gameserver.data.sql.CostumeTable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.costumes.Costume;
import org.l2jmobius.gameserver.model.costumes.Costumes;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.itemcontainer.InventoryBlockType;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.costume.ExCostumeExtract;
import org.l2jmobius.gameserver.network.serverpackets.costume.ExSendCostumeList;

/**
 * @author JoeAlisson
 */
public class ExRequestCostumeExtract extends ClientPacket
{
	private int id;
	private long amount;
	
	@Override
	protected void readImpl()
	{
		readShort(); // data size
		id = readInt();
		amount = readLong();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		var playerCostume = player.getCostume(id);
		Costume costume;
		
		var costumeEngine = Costumes.getInstance();
		if (canExtract(player, playerCostume) && consumeItemsCost(player, costume = costumeEngine.getCostume(id)))
		{
			playerCostume.reduceCount(amount);
			player.sendPacket(new ExSendCostumeList(playerCostume));
			player.sendPacket(ExCostumeExtract.success(playerCostume, costume.extractItem(), amount));
			player.addItem(ItemProcessType.REWARD, costume.extractItem(), amount, null, true);
			
			if (playerCostume.getAmount() <= 0)
			{
				player.removeCostume(id);
				player.removeSkill(costume.skill());
				costumeEngine.checkCostumeCollection(player, id);
			}
		}
		else
		{
			player.sendPacket(ExCostumeExtract.failed(id));
		}
	}
	
	private boolean canExtract(Player player, CostumeTable costume)
	{
		if ((costume == null) || (costume.getAmount() < amount))
		{
			player.sendPacket(SystemMessageId.THIS_TRANSFORMATION_CANNOT_BE_EXTRACTED);
			return false;
		}
		else if (player.isInventoryUnder90(false))
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_SPACE_IN_THE_INVENTORY_PLEASE_MAKE_MORE_ROOM_AND_TRY_AGAIN);
			return false;
		}
		return Costumes.getInstance().checkCostumeAction(player);
	}
	
	private boolean consumeItemsCost(Player player, Costume costume)
	{
		final Set<ItemHolder> extractCost = costume.extractCost();
		final PlayerInventory inventory = player.getInventory();
		try
		{
			// final Collection<Integer> blockItems = Util.collectToSet(extractCost.stream().mapToInt(ItemHolder::getId));
			final Collection<Integer> blockItems = extractCost.stream().mapToInt(ItemHolder::getId).boxed().collect(Collectors.toSet());
			inventory.setInventoryBlock(blockItems, InventoryBlockType.BLACKLIST);
			for (ItemHolder cost : extractCost)
			{
				if (inventory.getInventoryItemCount(cost.getId(), -1) < (cost.getCount() * amount))
				{
					player.sendPacket(SystemMessageId.NOT_ENOUGH_MATERIAL_TO_EXTRACT);
					return false;
				}
			}
			for (ItemHolder itemHolder : extractCost)
			{
				player.destroyItemByItemId(null, itemHolder.getId(), itemHolder.getCount(), player, true);
			}
		}
		finally
		{
			inventory.unblock();
		}
		return true;
	}
}
