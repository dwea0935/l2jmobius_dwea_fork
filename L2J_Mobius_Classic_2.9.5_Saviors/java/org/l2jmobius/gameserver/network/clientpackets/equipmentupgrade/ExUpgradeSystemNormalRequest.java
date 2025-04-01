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
package org.l2jmobius.gameserver.network.clientpackets.equipmentupgrade;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.holders.EquipmentUpgradeHolder;
import org.l2jmobius.gameserver.data.xml.EquipmentUpgradeData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.enums.UpgradeType;
import org.l2jmobius.gameserver.model.item.holders.ItemEnchantHolder;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.equipmentupgrade.ExUpgradeSystemNormalResult;
import org.l2jmobius.gameserver.network.serverpackets.equipmentupgrade.ExUpgradeSystemResult;

public class ExUpgradeSystemNormalRequest extends ClientPacket
{
	private int _objectId;
	private UpgradeType _type;
	private int _upgradeId;
	
	@Override
	protected void readImpl()
	{
		_objectId = readInt();
		_type = UpgradeType.ofId(readInt());
		_upgradeId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if ((player == null) || (_type == null))
		{
			return;
		}
		
		final Item existingItem = player.getInventory().getItemByObjectId(_objectId);
		if (existingItem == null)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.FAILED_BECAUSE_THE_TARGET_ITEM_DOES_NOT_EXIST));
			player.sendPacket(new ExUpgradeSystemResult(0, 0));
			return;
		}
		
		final EquipmentUpgradeHolder upgradeHolder = EquipmentUpgradeData.getInstance().getUpgrade(_type, _upgradeId);
		if (upgradeHolder == null)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.FAILED_THE_OPERATION));
			player.sendPacket(new ExUpgradeSystemResult(0, 0));
			return;
		}
		
		for (ItemHolder material : upgradeHolder.getMaterials())
		{
			if (player.getInventory().getInventoryItemCount(material.getId(), -1) < material.getCount())
			{
				player.sendPacket(new SystemMessage(SystemMessageId.FAILED_BECAUSE_THERE_ARE_NOT_ENOUGH_INGREDIENTS));
				player.sendPacket(new ExUpgradeSystemResult(0, 0));
				return;
			}
		}
		
		final long adena = upgradeHolder.getAdena();
		if ((adena > 0) && (player.getAdena() < adena))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA));
			player.sendPacket(new ExUpgradeSystemResult(0, 0));
			return;
		}
		
		if ((existingItem.getTemplate().getId() != upgradeHolder.getRequiredItem().getId()) || (existingItem.getEnchantLevel() != upgradeHolder.getRequiredItem().getEnchantLevel()) || existingItem.isAugmented() || (existingItem.getAttributes() != null))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.FAILED_THE_OPERATION));
			player.sendPacket(new ExUpgradeSystemResult(0, 0));
			return;
		}
		
		player.destroyItem(ItemProcessType.FEE, _objectId, 1, player, true);
		for (ItemHolder material : upgradeHolder.getMaterials())
		{
			player.destroyItemByItemId(ItemProcessType.FEE, material.getId(), material.getCount(), player, true);
		}
		if (adena > 0)
		{
			player.reduceAdena(ItemProcessType.FEE, adena, player, true);
		}
		
		final double random = (Rnd.nextDouble() * 100);
		final boolean success = random <= upgradeHolder.getChance();
		final Map<Integer, Item> items = new HashMap<>();
		
		if (success)
		{
			for (ItemEnchantHolder item : upgradeHolder.getResult())
			{
				final Item addItem = player.addItem(ItemProcessType.REWARD, item.getId(), item.getCount(), player, false);
				items.put(addItem.getObjectId(), addItem);
				if (item.getEnchantLevel() > 0)
				{
					addItem.setEnchantLevel(item.getEnchantLevel());
				}
				player.sendPacket(new SystemMessage(SystemMessageId.C1_YOU_OBTAINED_S2_THROUGH_EQUIPMENT_UPGRADE).addPcName(player).addItemName(addItem));
			}
			if (upgradeHolder.getBonus() != null)
			{
				final double randomBonus = (Rnd.nextDouble() * 100);
				final boolean successBonus = randomBonus <= upgradeHolder.getBonusChance();
				if (successBonus)
				{
					for (ItemEnchantHolder item : upgradeHolder.getBonus())
					{
						final Item addItem = player.addItem(ItemProcessType.REWARD, item.getId(), item.getCount(), player, false);
						items.put(addItem.getObjectId(), addItem);
						if (item.getEnchantLevel() > 0)
						{
							addItem.setEnchantLevel(item.getEnchantLevel());
						}
						player.sendPacket(new SystemMessage(SystemMessageId.C1_YOU_OBTAINED_S2_THROUGH_EQUIPMENT_UPGRADE).addPcName(player).addItemName(addItem));
					}
				}
			}
		}
		else
		{
			if (upgradeHolder.getOnFail() != null)
			{
				for (ItemEnchantHolder item : upgradeHolder.getOnFail())
				{
					final Item addItem = player.addItem(ItemProcessType.COMPENSATE, item.getId(), item.getCount(), player, false);
					items.put(addItem.getObjectId(), addItem);
					if (item.getEnchantLevel() > 0)
					{
						addItem.setEnchantLevel(item.getEnchantLevel());
					}
					player.sendPacket(new SystemMessage(SystemMessageId.C1_YOU_OBTAINED_S2_THROUGH_EQUIPMENT_UPGRADE).addPcName(player).addItemName(addItem));
				}
			}
		}
		items.forEach((_, item) ->
		{
			item.updateDatabase(true);
		});
		
		player.sendPacket(new InventoryUpdate());
		player.sendPacket(new ExUpgradeSystemNormalResult(upgradeHolder, success, items));
	}
}
