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
package org.l2jmobius.gameserver.network.clientpackets.luckygame;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map.Entry;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.holders.LuckyGameDataHolder;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.LuckyGameData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.enums.LuckyGameItemType;
import org.l2jmobius.gameserver.network.enums.LuckyGameResultType;
import org.l2jmobius.gameserver.network.enums.LuckyGameType;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.luckygame.ExBettingLuckyGameResult;
import org.l2jmobius.gameserver.util.MathUtil;

/**
 * @author Sdw
 */
public class RequestLuckyGamePlay extends ClientPacket
{
	private static final int FORTUNE_READING_TICKET = 23767;
	private static final int LUXURY_FORTUNE_READING_TICKET = 23768;
	private LuckyGameType _type;
	private int _reading;
	
	@Override
	protected void readImpl()
	{
		final int type = MathUtil.clamp(readInt(), 0, LuckyGameType.values().length);
		_type = LuckyGameType.values()[type];
		_reading = MathUtil.clamp(readInt(), 0, 50); // max play is 50
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final int index = _type == LuckyGameType.LUXURY ? 102 : 2; // move to event config
		final LuckyGameDataHolder holder = LuckyGameData.getInstance().getLuckyGameDataByIndex(index);
		if (holder == null)
		{
			return;
		}
		
		final long tickets = _type == LuckyGameType.LUXURY ? player.getInventory().getInventoryItemCount(LUXURY_FORTUNE_READING_TICKET, -1) : player.getInventory().getInventoryItemCount(FORTUNE_READING_TICKET, -1);
		if (tickets < _reading)
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_TICKETS_YOU_CANNOT_CONTINUE_THE_GAME);
			player.sendPacket(_type == LuckyGameType.LUXURY ? ExBettingLuckyGameResult.LUXURY_INVALID_ITEM_COUNT : ExBettingLuckyGameResult.NORMAL_INVALID_ITEM_COUNT);
			return;
		}
		
		int playCount = player.getVariables().getInt(PlayerVariables.FORTUNE_TELLING_VARIABLE, 0);
		boolean blackCat = player.getVariables().getBoolean(PlayerVariables.FORTUNE_TELLING_BLACK_CAT_VARIABLE, false);
		final EnumMap<LuckyGameItemType, List<ItemHolder>> rewards = new EnumMap<>(LuckyGameItemType.class);
		for (int i = 0; i < _reading; i++)
		{
			final double chance = 100 * Rnd.nextDouble();
			double totalChance = 0;
			for (ItemChanceHolder item : holder.getCommonReward())
			{
				totalChance += item.getChance();
				if (totalChance >= chance)
				{
					rewards.computeIfAbsent(LuckyGameItemType.COMMON, _ -> new ArrayList<>()).add(item);
					break;
				}
			}
			playCount++;
			if ((playCount >= holder.getMinModifyRewardGame()) && (playCount <= holder.getMaxModifyRewardGame()) && !blackCat)
			{
				final List<ItemChanceHolder> modifyReward = holder.getModifyReward();
				final double chanceModify = 100 * Rnd.nextDouble();
				totalChance = 0;
				for (ItemChanceHolder item : modifyReward)
				{
					totalChance += item.getChance();
					if (totalChance >= chanceModify)
					{
						rewards.computeIfAbsent(LuckyGameItemType.RARE, _ -> new ArrayList<>()).add(item);
						blackCat = true;
						break;
					}
				}
				
				if (playCount == holder.getMaxModifyRewardGame())
				{
					rewards.computeIfAbsent(LuckyGameItemType.RARE, _ -> new ArrayList<>()).add(modifyReward.get(Rnd.get(modifyReward.size())));
					blackCat = true;
				}
			}
		}
		
		final int totalWeight = rewards.values().stream().mapToInt(list -> list.stream().mapToInt(item -> ItemData.getInstance().getTemplate(item.getId()).getWeight()).sum()).sum();
		
		// Check inventory capacity
		if (!rewards.isEmpty() && (!player.getInventory().validateCapacity(rewards.size()) || !player.getInventory().validateWeight(totalWeight)))
		{
			player.sendPacket(_type == LuckyGameType.LUXURY ? ExBettingLuckyGameResult.LUXURY_INVALID_CAPACITY : ExBettingLuckyGameResult.NORMAL_INVALID_CAPACITY);
			player.sendPacket(SystemMessageId.YOUR_INVENTORY_IS_EITHER_FULL_OR_OVERWEIGHT);
			return;
		}
		
		if (!player.destroyItemByItemId(ItemProcessType.FEE, _type == LuckyGameType.LUXURY ? LUXURY_FORTUNE_READING_TICKET : FORTUNE_READING_TICKET, _reading, player, true))
		{
			player.sendPacket(_type == LuckyGameType.LUXURY ? ExBettingLuckyGameResult.LUXURY_INVALID_ITEM_COUNT : ExBettingLuckyGameResult.NORMAL_INVALID_ITEM_COUNT);
			return;
		}
		
		for (int i = 0; i < _reading; i++)
		{
			final int serverGameNumber = LuckyGameData.getInstance().increaseGame();
			holder.getUniqueReward().stream().filter(reward -> reward.getPoints() == serverGameNumber).forEach(item -> rewards.computeIfAbsent(LuckyGameItemType.UNIQUE, _ -> new ArrayList<>()).add(item));
		}
		
		player.sendPacket(new ExBettingLuckyGameResult(LuckyGameResultType.SUCCESS, _type, rewards, (int) (_type == LuckyGameType.LUXURY ? player.getInventory().getInventoryItemCount(LUXURY_FORTUNE_READING_TICKET, -1) : player.getInventory().getInventoryItemCount(FORTUNE_READING_TICKET, -1))));
		
		for (Entry<LuckyGameItemType, List<ItemHolder>> reward : rewards.entrySet())
		{
			for (ItemHolder r : reward.getValue())
			{
				final Item item = player.addItem(ItemProcessType.REWARD, r.getId(), r.getCount(), player, true);
				if (reward.getKey() == LuckyGameItemType.UNIQUE)
				{
					final SystemMessage sm = new SystemMessage(_type == LuckyGameType.LUXURY ? SystemMessageId.CONGRATULATIONS_C1_HAS_OBTAINED_S2_X_S3_IN_THE_PREMIUM_LUCKY_GAME : SystemMessageId.CONGRATULATIONS_C1_HAS_OBTAINED_S2_X_S3_IN_THE_STANDARD_LUCKY_GAME);
					sm.addPcName(player);
					sm.addLong(r.getCount());
					sm.addItemName(item);
					player.broadcastPacket(sm, 1000);
					break;
				}
			}
		}
		
		player.sendItemList();
		
		player.getVariables().set(PlayerVariables.FORTUNE_TELLING_VARIABLE, playCount >= 50 ? (playCount - 50) : playCount);
		if (blackCat && (playCount < 50))
		{
			player.getVariables().set(PlayerVariables.FORTUNE_TELLING_BLACK_CAT_VARIABLE, true);
		}
	}
}
