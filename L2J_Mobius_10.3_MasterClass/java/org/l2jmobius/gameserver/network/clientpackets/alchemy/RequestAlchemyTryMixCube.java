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
package org.l2jmobius.gameserver.network.clientpackets.alchemy;

import java.util.LinkedList;
import java.util.List;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerCondOverride;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.skill.CommonSkill;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.enums.TryMixCubeResultType;
import org.l2jmobius.gameserver.network.enums.TryMixCubeType;
import org.l2jmobius.gameserver.network.holders.AlchemyResult;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.ServerClose;
import org.l2jmobius.gameserver.network.serverpackets.alchemy.ExTryMixCube;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;

/**
 * @author Sdw
 */
public class RequestAlchemyTryMixCube extends ClientPacket
{
	// TODO: Figure out how much stones are given
	private static final int TEMPEST_STONE_AMOUNT = 1;
	
	private List<ItemHolder> _items = new LinkedList<>();
	
	@Override
	protected void readImpl()
	{
		final int itemsCount = readInt();
		if ((itemsCount < 1) || (itemsCount > 4))
		{
			return;
		}
		
		for (int i = 0; i < itemsCount; i++)
		{
			final int id = readInt();
			final long count = readLong();
			if ((count < 1) || (count > 10000))
			{
				_items = null;
				return;
			}
			
			_items.add(new ItemHolder(id, count));
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (_items == null)
		{
			PacketLogger.warning("Kicked " + player + " for using packet injection tool with " + getClass().getSimpleName());
			Disconnection.of(player).defaultSequence(ServerClose.STATIC_PACKET);
			return;
		}
		
		if (player.getRace() != Race.ERTHEIA)
		{
			return;
		}
		
		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(player))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_ALCHEMY_WHILE_IN_BATTLE);
			return;
		}
		else if (player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_ALCHEMY_WHILE_TRADING_OR_USING_A_PRIVATE_STORE_OR_SHOP);
			return;
		}
		else if (player.isDead())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_ALCHEMY_WHILE_DEAD);
			return;
		}
		else if (player.isMovementDisabled())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_ALCHEMY_WHILE_IMMOBILE);
			return;
		}
		
		if ((player.getKnownSkill(CommonSkill.ALCHEMY_CUBE.getId()) == null) && !player.canOverrideCond(PlayerCondOverride.SKILL_CONDITIONS))
		{
			player.sendPacket(new ExTryMixCube(TryMixCubeType.FAIL_SKILL_WRONG));
			return;
		}
		
		int position = 0;
		long itemsPrice = 0;
		
		// First loop for safety check + price calculation
		for (ItemHolder item : _items)
		{
			final Item itemInstance = player.getInventory().getItemByObjectId(item.getId());
			if (itemInstance == null)
			{
				return;
			}
			
			if ((itemInstance.getCount() < 1) || (itemInstance.getCount() < item.getCount()))
			{
				player.sendPacket(new ExTryMixCube(TryMixCubeType.FAIL_ITEM_WRONG));
				return;
			}
			
			final long price = itemInstance.getReferencePrice();
			if (itemInstance.getReferencePrice() == 0)
			{
				player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_USED_IN_COMPOUNDING);
				player.sendPacket(new ExTryMixCube(TryMixCubeType.FAIL_ITEM_WRONG));
				return;
			}
			
			if ((itemInstance.getEnchantLevel() > 0) || itemInstance.isAugmented())
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_COMBINE_ITEMS_THAT_HAVE_BEEN_ENCHANTED_OR_AUGMENTED);
				player.sendPacket(new ExTryMixCube(TryMixCubeType.FAIL_ITEM_WRONG));
				return;
			}
			
			itemsPrice += price * item.getCount();
			position++;
			
			if ((position == 4) && (itemInstance.getId() != Inventory.ELCYUM_CRYSTAL_ID))
			{
				player.sendPacket(new ExTryMixCube(TryMixCubeType.FAIL_ITEM_WRONG));
				return;
			}
		}
		
		if (itemsPrice < 0)
		{
			player.sendPacket(new ExTryMixCube(TryMixCubeType.FAIL_ITEM_WRONG));
			return;
		}
		
		// Calculate the amount of air stones the player should received based on the total price of items he mixed.
		int airStonesCount = (int) Math.floor((itemsPrice / 5000) * (_items.size() < 3 ? 0.3f : 0.5f));
		
		// Process only if there is at least one air stone to give
		if (airStonesCount > 0)
		{
			final InventoryUpdate iu = new InventoryUpdate();
			long elcyumCrystals = 0;
			
			// Second loop for items deletion if we're still in the game
			for (ItemHolder item : _items)
			{
				final Item itemInstance = player.getInventory().getItemByObjectId(item.getId());
				if (itemInstance == null)
				{
					return;
				}
				
				if (itemInstance.getCount() < item.getCount())
				{
					return;
				}
				
				if (itemInstance.getId() == Inventory.ELCYUM_CRYSTAL_ID)
				{
					elcyumCrystals = item.getCount();
				}
				
				player.getInventory().destroyItem(ItemProcessType.FEE, itemInstance, item.getCount(), player, null);
				iu.addItem(itemInstance);
			}
			
			final ExTryMixCube mixCubeResult = new ExTryMixCube(TryMixCubeType.SUCCESS_NORMAL);
			
			// Whenever there is Elcyum Crystal applied there's a chance to receive Tempest Stone
			// TODO: Figure out the chance
			if ((elcyumCrystals > 0) && (Rnd.get(100) < 50))
			{
				// Broadcast animation on success
				player.broadcastPacket(new MagicSkillUse(player, CommonSkill.ALCHEMY_CUBE_RANDOM_SUCCESS.getId(), TEMPEST_STONE_AMOUNT, 500, 1500));
				
				// Give Tempest Stone to the player
				final Item tempestStonesInstance = player.addItem(ItemProcessType.REWARD, Inventory.TEMPEST_STONE_ID, TEMPEST_STONE_AMOUNT, null, true);
				iu.addItem(tempestStonesInstance);
				
				// Add the alchemy result entry to the packet
				mixCubeResult.addItem(new AlchemyResult(Inventory.TEMPEST_STONE_ID, TEMPEST_STONE_AMOUNT, TryMixCubeResultType.EXTRA));
			}
			
			// Calculate the elcyum crystals bonus
			final boolean bonusSuccess = ((100. * Rnd.nextDouble()) < (elcyumCrystals / 1000));
			if (bonusSuccess)
			{
				airStonesCount *= Math.min(elcyumCrystals, 2);
			}
			
			final Item airStonesInstance = player.addItem(ItemProcessType.REWARD, Inventory.AIR_STONE_ID, airStonesCount, null, true);
			iu.addItem(airStonesInstance);
			
			// Add the Air Stones
			mixCubeResult.addItem(new AlchemyResult(Inventory.AIR_STONE_ID, airStonesCount, bonusSuccess ? TryMixCubeResultType.BONUS : TryMixCubeResultType.NORMAL));
			
			// send packets
			player.sendPacket(mixCubeResult);
			player.sendInventoryUpdate(iu);
		}
		else
		{
			player.sendPacket(new ExTryMixCube(TryMixCubeType.FAIL_ITEM_WRONG));
		}
	}
}
