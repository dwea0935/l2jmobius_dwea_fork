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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.model.PremiumItem;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExGetPremiumItemList;

/**
 * @author Gnacik
 */
public class RequestWithDrawPremiumItem extends ClientPacket
{
	private int _itemNum;
	private int _charId;
	private long _itemCount;
	
	@Override
	protected void readImpl()
	{
		_itemNum = readInt();
		_charId = readInt();
		_itemCount = readLong();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		else if (_itemCount < 1)
		{
			return;
		}
		else if (player.getObjectId() != _charId)
		{
			PunishmentManager.handleIllegalPlayerAction(player, "[RequestWithDrawPremiumItem] Incorrect owner, Player: " + player.getName(), Config.DEFAULT_PUNISH);
			return;
		}
		else if (player.getPremiumItemList().isEmpty())
		{
			PunishmentManager.handleIllegalPlayerAction(player, "[RequestWithDrawPremiumItem] Player: " + player.getName() + " try to get item with empty list!", Config.DEFAULT_PUNISH);
			return;
		}
		else if ((player.getWeightPenalty() >= 3) || !player.isInventoryUnder90(false))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_RECEIVE_THE_DIMENSIONAL_ITEM_BECAUSE_YOU_HAVE_EXCEED_YOUR_INVENTORY_WEIGHT_QUANTITY_LIMIT);
			return;
		}
		else if (player.isProcessingTransaction())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_RECEIVE_A_DIMENSIONAL_ITEM_DURING_AN_EXCHANGE);
			return;
		}
		
		final PremiumItem item = player.getPremiumItemList().get(_itemNum);
		if (item == null)
		{
			return;
		}
		else if (item.getCount() < _itemCount)
		{
			return;
		}
		
		final long itemsLeft = (item.getCount() - _itemCount);
		player.addItem(ItemProcessType.TRANSFER, item.getItemId(), _itemCount, player.getTarget(), true);
		if (itemsLeft > 0)
		{
			item.updateCount(itemsLeft);
			player.updatePremiumItem(_itemNum, itemsLeft);
		}
		else
		{
			player.getPremiumItemList().remove(_itemNum);
			player.deletePremiumItem(_itemNum);
		}
		
		if (player.getPremiumItemList().isEmpty())
		{
			player.sendPacket(SystemMessageId.THERE_ARE_NO_MORE_DIMENSIONAL_ITEMS_TO_BE_FOUND);
		}
		else
		{
			player.sendPacket(new ExGetPremiumItemList(player));
		}
	}
}
