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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.data.xml.VariationData;
import org.l2jmobius.gameserver.model.VariationInstance;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.options.Variation;
import org.l2jmobius.gameserver.model.options.VariationFee;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExVariationResult;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;

/**
 * @author Mobius
 */
public class RequestRefine extends AbstractRefinePacket
{
	private int _targetItemObjId;
	private int _mineralItemObjId;
	private long _feeCount;
	
	@Override
	protected void readImpl()
	{
		_targetItemObjId = readInt();
		_mineralItemObjId = readInt();
		readInt(); // _feeItemObjId
		_feeCount = readLong();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Item targetItem = player.getInventory().getItemByObjectId(_targetItemObjId);
		if (targetItem == null)
		{
			return;
		}
		
		final Item mineralItem = player.getInventory().getItemByObjectId(_mineralItemObjId);
		if (mineralItem == null)
		{
			return;
		}
		
		final VariationFee fee = VariationData.getInstance().getFee(targetItem.getId(), mineralItem.getId());
		if (fee == null)
		{
			return;
		}
		
		final Item feeItem = player.getInventory().getItemByItemId(fee.getItemId());
		if ((feeItem == null) && (fee.getItemId() != 0))
		{
			return;
		}
		
		if (!isValid(player, targetItem, mineralItem, feeItem, fee))
		{
			player.sendPacket(new ExVariationResult(0, 0, false));
			player.sendPacket(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
			return;
		}
		
		// TODO: Update XMLs.
		// if (_feeCount != fee.getItemCount())
		if (_feeCount <= 0)
		{
			player.sendPacket(new ExVariationResult(0, 0, false));
			player.sendPacket(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
			return;
		}
		
		final long adenaFee = fee.getAdenaFee();
		if ((adenaFee > 0) && (player.getAdena() < adenaFee))
		{
			player.sendPacket(new ExVariationResult(0, 0, false));
			player.sendPacket(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
			return;
		}
		
		final Variation variation = VariationData.getInstance().getVariation(mineralItem.getId());
		if (variation == null)
		{
			player.sendPacket(new ExVariationResult(0, 0, false));
			return;
		}
		
		VariationInstance augment = VariationData.getInstance().generateRandomVariation(variation, targetItem);
		if (augment == null)
		{
			player.sendPacket(new ExVariationResult(0, 0, false));
			return;
		}
		
		// Support for single slot augments.
		final int option1 = augment.getOption1Id();
		final int option2 = augment.getOption2Id();
		if ((option1 == -1) || (option2 == -1))
		{
			final VariationInstance oldAugment = targetItem.getAugmentation();
			if (oldAugment != null)
			{
				if (option1 == -1)
				{
					augment = new VariationInstance(augment.getMineralId(), oldAugment.getOption1Id(), option2);
				}
				else
				{
					augment = new VariationInstance(augment.getMineralId(), option1, oldAugment.getOption2Id());
				}
			}
		}
		
		// Unequip item.
		final InventoryUpdate iu = new InventoryUpdate();
		if (targetItem.isEquipped())
		{
			for (Item itm : player.getInventory().unEquipItemInSlotAndRecord(targetItem.getLocationSlot()))
			{
				iu.addModifiedItem(itm);
			}
			player.broadcastUserInfo();
		}
		
		// Consume the life stone.
		if (!player.destroyItem(ItemProcessType.FEE, mineralItem, 1, null, false))
		{
			return;
		}
		
		// Consume the gemstones.
		if ((feeItem != null) && !player.destroyItem(ItemProcessType.FEE, feeItem, _feeCount, null, false))
		{
			return;
		}
		
		// Consume Adena.
		if ((adenaFee > 0) && !player.reduceAdena(ItemProcessType.FEE, adenaFee, player, false))
		{
			return;
		}
		
		// Remove the augmentation if any (286).
		if (targetItem.isAugmented())
		{
			targetItem.removeAugmentation();
		}
		
		targetItem.setAugmentation(augment, true);
		player.sendPacket(new ExVariationResult(augment.getOption1Id(), augment.getOption2Id(), true));
		iu.addModifiedItem(targetItem);
		player.sendInventoryUpdate(iu);
	}
}
