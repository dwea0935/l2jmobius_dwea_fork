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
package org.l2jmobius.gameserver.network.clientpackets.blessing;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.BlessingItemRequest;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.enums.ItemSkillType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.CommonSkill;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.EnchantResult;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.blessing.ExBlessOptionEnchant;
import org.l2jmobius.gameserver.network.serverpackets.blessing.ExBlessOptionPutItem;

/**
 * @author Horus
 */
public class RequestBlessOptionEnchant extends ClientPacket
{
	private int _itemObjId;
	
	@Override
	protected void readImpl()
	{
		_itemObjId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Item targetInstance = player.getInventory().getItemByObjectId(_itemObjId);
		if (targetInstance == null)
		{
			player.sendPacket(new ExBlessOptionEnchant(EnchantResult.ERROR));
			return;
		}
		
		final BlessingItemRequest request = player.getRequest(BlessingItemRequest.class);
		if ((request == null) || request.isProcessing())
		{
			player.sendPacket(new ExBlessOptionEnchant(EnchantResult.ERROR));
			return;
		}
		request.setProcessing(true);
		request.setTimestamp(System.currentTimeMillis());
		
		if (!player.isOnline() || getClient().isDetached())
		{
			return;
		}
		
		if (player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_ENCHANT_WHILE_OPERATING_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP);
			player.sendPacket(new ExBlessOptionEnchant(EnchantResult.ERROR));
			return;
		}
		
		final Item item = player.getInventory().getItemByObjectId(_itemObjId);
		if (item == null)
		{
			player.sendPacket(new ExBlessOptionEnchant(EnchantResult.ERROR));
			return;
		}
		
		// first validation check - also over enchant check
		if (item.isBlessed())
		{
			player.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITIONS);
			player.sendPacket(new ExBlessOptionPutItem(0));
			return;
		}
		
		final Item targetScroll = player.getInventory().getItemByItemId(request.getBlessScrollId());
		if (targetScroll == null)
		{
			player.sendPacket(new ExBlessOptionEnchant(EnchantResult.ERROR));
			return;
		}
		
		// attempting to destroy scroll
		if (player.getInventory().destroyItem(ItemProcessType.FEE, targetScroll.getObjectId(), 1, player, item) == null)
		{
			player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			PunishmentManager.handleIllegalPlayerAction(player, player + " tried to bless with a scroll he doesn't have", Config.DEFAULT_PUNISH);
			player.sendPacket(new ExBlessOptionEnchant(EnchantResult.ERROR));
			return;
		}
		
		if (Rnd.get(100) < Config.BLESSING_CHANCE) // Success
		{
			final ItemTemplate it = item.getTemplate();
			// Increase enchant level only if scroll's base template has chance, some armors can success over +20 but they shouldn't have increased.
			item.setBlessed(true);
			item.updateDatabase();
			player.sendPacket(new ExBlessOptionEnchant(1));
			// Announce the success.
			if ((item.getEnchantLevel() >= (item.isArmor() ? Config.MIN_ARMOR_ENCHANT_ANNOUNCE : Config.MIN_WEAPON_ENCHANT_ANNOUNCE)) //
				&& (item.getEnchantLevel() <= (item.isArmor() ? Config.MAX_ARMOR_ENCHANT_ANNOUNCE : Config.MAX_WEAPON_ENCHANT_ANNOUNCE)))
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_SUCCESSFULLY_ENCHANTED_A_S2_S3);
				sm.addString(player.getName());
				sm.addInt(item.getEnchantLevel());
				sm.addItemName(item);
				player.broadcastPacket(sm);
				// Broadcast.toAllOnlinePlayers(new ExItemAnnounce(player, item, ExItemAnnounce.ENCHANT));
				
				final Skill skill = CommonSkill.FIREWORK.getSkill();
				if (skill != null)
				{
					player.broadcastPacket(new MagicSkillUse(player, player, skill.getId(), skill.getLevel(), skill.getHitTime(), skill.getReuseDelay()));
				}
			}
			if (item.isEquipped())
			{
				if (item.isArmor())
				{
					it.forEachSkill(ItemSkillType.ON_BLESSING, holder ->
					{
						player.addSkill(holder.getSkill(), false);
						player.sendSkillList();
					});
				}
				player.broadcastUserInfo();
			}
		}
		else // Failure.
		{
			player.sendPacket(new ExBlessOptionEnchant(0));
		}
		
		request.setProcessing(false);
		player.sendItemList();
		player.broadcastUserInfo();
	}
}
