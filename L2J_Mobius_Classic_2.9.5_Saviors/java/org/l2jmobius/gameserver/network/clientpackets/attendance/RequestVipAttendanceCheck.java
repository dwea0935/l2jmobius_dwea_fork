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
package org.l2jmobius.gameserver.network.clientpackets.attendance;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.data.xml.AttendanceRewardData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.AttendanceInfoHolder;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.attendance.ExConfirmVipAttendanceCheck;

/**
 * @author Mobius
 */
public class RequestVipAttendanceCheck extends ClientPacket
{
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!Config.ENABLE_ATTENDANCE_REWARDS)
		{
			player.sendPacket(SystemMessageId.DUE_TO_A_SYSTEM_ERROR_THE_ATTENDANCE_REWARD_CANNOT_BE_RECEIVED_PLEASE_TRY_AGAIN_LATER_BY_GOING_TO_MENU_ATTENDANCE_CHECK);
			return;
		}
		
		if (Config.PREMIUM_ONLY_ATTENDANCE_REWARDS && !player.hasPremiumStatus())
		{
			player.sendPacket(SystemMessageId.YOUR_VIP_LEVEL_IS_TOO_LOW_TO_RECEIVE_THE_REWARD);
			return;
		}
		else if (Config.VIP_ONLY_ATTENDANCE_REWARDS && (player.getVipTier() <= 0))
		{
			player.sendPacket(SystemMessageId.YOUR_VIP_LEVEL_IS_TOO_LOW_TO_RECEIVE_THE_REWARD);
			return;
		}
		
		// Check login delay.
		if (player.getUptime() < (Config.ATTENDANCE_REWARD_DELAY * 60 * 1000))
		{
			final SystemMessage msg = new SystemMessage(SystemMessageId.YOU_CAN_REDEEM_YOUR_REWARD_S1_MINUTES_AFTER_LOGGING_IN_S2_MINUTES_LEFT);
			msg.addInt(Config.ATTENDANCE_REWARD_DELAY);
			msg.addInt(Config.ATTENDANCE_REWARD_DELAY - Math.toIntExact(player.getUptime() / 1000 / 60));
			player.sendPacket(msg);
			return;
		}
		
		final AttendanceInfoHolder attendanceInfo = player.getAttendanceInfo();
		final boolean isRewardAvailable = attendanceInfo.isRewardAvailable();
		final int rewardIndex = attendanceInfo.getRewardIndex();
		final ItemHolder reward = AttendanceRewardData.getInstance().getRewards().get(rewardIndex);
		final ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(reward.getId());
		
		// Weight check.
		final long weight = itemTemplate.getWeight() * reward.getCount();
		final long slots = itemTemplate.isStackable() ? 1 : reward.getCount();
		if (!player.getInventory().validateWeight(weight) || !player.getInventory().validateCapacity(slots))
		{
			player.sendPacket(SystemMessageId.THE_ATTENDANCE_REWARD_CANNOT_BE_RECEIVED_BECAUSE_THE_INVENTORY_WEIGHT_QUANTITY_LIMIT_HAS_BEEN_EXCEEDED);
			return;
		}
		
		// Reward.
		if (isRewardAvailable)
		{
			// Save date and index.
			player.setAttendanceInfo(rewardIndex + 1);
			// Add items to player.
			player.addItem(ItemProcessType.REWARD, reward, player, true);
			// Send message.
			final SystemMessage msg = new SystemMessage(SystemMessageId.YOU_VE_RECEIVED_YOUR_VIP_ATTENDANCE_REWARD_FOR_DAY_S1);
			msg.addInt(rewardIndex + 1);
			player.sendPacket(msg);
			// Send confirm packet.
			player.sendPacket(new ExConfirmVipAttendanceCheck(isRewardAvailable, rewardIndex + 1));
		}
	}
}
