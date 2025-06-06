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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.data.holders.MultisellEntryHolder;
import org.l2jmobius.gameserver.data.holders.PreparedMultisellListHolder;
import org.l2jmobius.gameserver.data.xml.EnsoulData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.model.ItemInfo;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.ensoul.EnsoulOption;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enchant.attribute.AttributeHolder;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.enums.SpecialItemType;
import org.l2jmobius.gameserver.model.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExMultiSellResult;
import org.l2jmobius.gameserver.network.serverpackets.ExPCCafePointInfo;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.ArrayUtil;

/**
 * @author Mobius
 */
public class MultiSellChoose extends ClientPacket
{
	private int _listId;
	private int _entryId;
	private long _amount;
	private int _enchantLevel;
	private int _augmentOption1;
	private int _augmentOption2;
	private short _attackAttribute;
	private short _attributePower;
	private short _fireDefence;
	private short _waterDefence;
	private short _windDefence;
	private short _earthDefence;
	private short _holyDefence;
	private short _darkDefence;
	private EnsoulOption[] _soulCrystalOptions;
	private EnsoulOption[] _soulCrystalSpecialOptions;
	
	@Override
	protected void readImpl()
	{
		_listId = readInt();
		_entryId = readInt();
		_amount = readLong();
		_enchantLevel = readShort();
		_augmentOption1 = readInt();
		_augmentOption2 = readInt();
		_attackAttribute = readShort();
		_attributePower = readShort();
		_fireDefence = readShort();
		_waterDefence = readShort();
		_windDefence = readShort();
		_earthDefence = readShort();
		_holyDefence = readShort();
		_darkDefence = readShort();
		_soulCrystalOptions = new EnsoulOption[readByte()]; // Ensoul size
		for (int i = 0; i < _soulCrystalOptions.length; i++)
		{
			final int ensoulId = readInt(); // Ensoul option id
			_soulCrystalOptions[i] = EnsoulData.getInstance().getOption(ensoulId);
		}
		_soulCrystalSpecialOptions = new EnsoulOption[readByte()]; // Special ensoul size
		for (int i = 0; i < _soulCrystalSpecialOptions.length; i++)
		{
			final int ensoulId = readInt(); // Special ensoul option id.
			_soulCrystalSpecialOptions[i] = EnsoulData.getInstance().getOption(ensoulId);
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
		
		if (!getClient().getFloodProtectors().canUseMultiSell())
		{
			player.setMultiSell(null);
			return;
		}
		
		if ((_amount < 1) || (_amount > Config.MULTISELL_AMOUNT_LIMIT)) // 999 999 is client max.
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED);
			return;
		}
		
		final PreparedMultisellListHolder list = player.getMultiSell();
		if ((list == null) || (list.getId() != _listId))
		{
			player.setMultiSell(null);
			return;
		}
		
		final Npc npc = player.getLastFolkNPC();
		if (!list.isNpcAllowed(-1))
		{
			if ((npc == null) //
				|| !list.isNpcAllowed(npc.getId()) //
				|| !list.checkNpcObjectId(npc.getObjectId()) //
				|| (player.getInstanceId() != npc.getInstanceId()) //
				|| !player.isInsideRadius3D(npc, Npc.INTERACTION_DISTANCE))
			{
				if (player.isGM())
				{
					player.sendMessage("Multisell " + _listId + " is restricted. Under current conditions cannot be used. Only GMs are allowed to use it.");
				}
				else
				{
					player.setMultiSell(null);
					return;
				}
			}
		}
		
		if (((_soulCrystalOptions != null) && ArrayUtil.contains(_soulCrystalOptions, null)) || ((_soulCrystalSpecialOptions != null) && ArrayUtil.contains(_soulCrystalSpecialOptions, null)))
		{
			PacketLogger.warning("Character: " + player.getName() + " requested multisell entry with invalid soul crystal options. Multisell: " + _listId + " entry: " + _entryId);
			player.setMultiSell(null);
			return;
		}
		
		final List<MultisellEntryHolder> entries = list.getEntries();
		if (entries == null)
		{
			PacketLogger.warning("Character: " + player.getName() + " requested null multisell entry. Multisell: " + _listId + " entry: " + _entryId);
			return;
		}
		if (entries.isEmpty())
		{
			PacketLogger.warning("Character: " + player.getName() + " requested empty multisell entry. Multisell: " + _listId + " entry: " + _entryId);
			return;
		}
		if ((_entryId - 1) >= entries.size())
		{
			PacketLogger.warning("Character: " + player.getName() + " requested out of bounds multisell entry. Multisell: " + _listId + " entry: " + _entryId);
			return;
		}
		
		final MultisellEntryHolder entry = entries.get(_entryId - 1); // Entry Id begins from 1. We currently use entry IDs as index pointer.
		if (entry == null)
		{
			PacketLogger.warning("Character: " + player.getName() + " requested inexistant prepared multisell entry. Multisell: " + _listId + " entry: " + _entryId);
			player.setMultiSell(null);
			return;
		}
		
		if (!entry.isStackable() && (_amount > 1))
		{
			PacketLogger.warning("Character: " + player.getName() + " is trying to set amount > 1 on non-stackable multisell. Id: " + _listId + " entry: " + _entryId);
			player.setMultiSell(null);
			return;
		}
		
		ItemInfo itemEnchantment = list.getItemEnchantment(_entryId - 1); // Entry Id begins from 1. We currently use entry IDs as index pointer.
		
		// Validate the requested item with its full stats.
		//@formatter:off
		if ((itemEnchantment != null) && ((_amount > 1)
			|| (itemEnchantment.getEnchantLevel() != _enchantLevel)
			|| (itemEnchantment.getAttackElementType() != _attackAttribute) 
			|| (itemEnchantment.getAttackElementPower() != _attributePower)
			|| (itemEnchantment.getAttributeDefence(AttributeType.FIRE) != _fireDefence)
			|| (itemEnchantment.getAttributeDefence(AttributeType.WATER) != _waterDefence)
			|| (itemEnchantment.getAttributeDefence(AttributeType.WIND) != _windDefence)
			|| (itemEnchantment.getAttributeDefence(AttributeType.EARTH) != _earthDefence)
			|| (itemEnchantment.getAttributeDefence(AttributeType.HOLY) != _holyDefence)
			|| (itemEnchantment.getAttributeDefence(AttributeType.DARK) != _darkDefence)
			|| ((itemEnchantment.getAugmentation() == null) && ((_augmentOption1 != 0) || (_augmentOption2 != 0)))
			|| ((itemEnchantment.getAugmentation() != null) && ((itemEnchantment.getAugmentation().getOption1Id() != _augmentOption1) || (itemEnchantment.getAugmentation().getOption2Id() != _augmentOption2)))
			|| ((_soulCrystalOptions != null) && !itemEnchantment.soulCrystalOptionsMatch(_soulCrystalOptions))
			|| ((_soulCrystalOptions == null) && !itemEnchantment.getSoulCrystalOptions().isEmpty())
			|| ((_soulCrystalSpecialOptions != null) && !itemEnchantment.soulCrystalSpecialOptionsMatch(_soulCrystalSpecialOptions))
			|| ((_soulCrystalSpecialOptions == null) && !itemEnchantment.getSoulCrystalSpecialOptions().isEmpty())
			))
		//@formatter:on
		{
			PacketLogger.warning("Character: " + player.getName() + " is trying to upgrade equippable item, but the stats doesn't match. Id: " + _listId + " entry: " + _entryId);
			player.setMultiSell(null);
			return;
		}
		
		final Clan clan = player.getClan();
		final PlayerInventory inventory = player.getInventory();
		
		try
		{
			int slots = 0;
			int weight = 0;
			for (ItemChanceHolder product : entry.getProducts())
			{
				if (product.getId() < 0)
				{
					// Check if clan exists for clan reputation products.
					if ((clan == null) && (SpecialItemType.CLAN_REPUTATION.getClientId() == product.getId()))
					{
						player.sendPacket(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER_AND_CANNOT_PERFORM_THIS_ACTION);
						return;
					}
					continue;
				}
				
				final ItemTemplate template = ItemData.getInstance().getTemplate(product.getId());
				if (template == null)
				{
					player.setMultiSell(null);
					return;
				}
				
				final long totalCount = Math.multiplyExact(list.getProductCount(product), _amount);
				if ((totalCount < 1) || (totalCount > Integer.MAX_VALUE))
				{
					player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED);
					return;
				}
				
				if (!template.isStackable() || (player.getInventory().getItemByItemId(product.getId()) == null))
				{
					slots++;
				}
				
				weight += totalCount * template.getWeight();
				if (!inventory.validateWeight(weight))
				{
					player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
					return;
				}
				
				if ((slots > 0) && !inventory.validateCapacity(slots))
				{
					player.sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
					return;
				}
				
				// If this is a chance multisell, reset slots and weight because only one item should be seleted. We just need to check if conditions for every item is met.
				if (list.isChanceMultisell())
				{
					slots = 0;
					weight = 0;
				}
			}
			
			// Check for enchanted item if it is present in the inventory.
			if ((itemEnchantment != null) && (inventory.getItemByObjectId(itemEnchantment.getObjectId()) == null))
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_NEED_A_N_S1);
				sm.addItemName(itemEnchantment.getItem().getId());
				player.sendPacket(sm);
				return;
			}
			
			// Check for enchanted level and ingredient count requirements.
			final List<ItemChanceHolder> summedIngredients = new ArrayList<>();
			for (ItemChanceHolder ingredient : entry.getIngredients())
			{
				boolean added = false;
				for (ItemChanceHolder summedIngredient : summedIngredients)
				{
					if ((summedIngredient.getId() == ingredient.getId()) && (summedIngredient.getEnchantmentLevel() == ingredient.getEnchantmentLevel()))
					{
						summedIngredients.add(new ItemChanceHolder(ingredient.getId(), ingredient.getChance(), ingredient.getCount() + summedIngredient.getCount(), ingredient.getEnchantmentLevel(), ingredient.isMaintainIngredient()));
						summedIngredients.remove(summedIngredient);
						added = true;
					}
				}
				if (!added)
				{
					summedIngredients.add(ingredient);
				}
			}
			for (ItemChanceHolder ingredient : summedIngredients)
			{
				if (ingredient.getEnchantmentLevel() > 0)
				{
					int found = 0;
					for (Item item : inventory.getAllItemsByItemId(ingredient.getId(), ingredient.getEnchantmentLevel()))
					{
						if (item.getEnchantLevel() >= ingredient.getEnchantmentLevel())
						{
							found++;
						}
					}
					
					if (found < ingredient.getCount())
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_NEED_A_N_S1);
						sm.addString("+" + ingredient.getEnchantmentLevel() + " " + ItemData.getInstance().getTemplate(ingredient.getId()).getName());
						player.sendPacket(sm);
						return;
					}
				}
				else if (!checkIngredients(player, list, inventory, clan, ingredient.getId(), Math.multiplyExact(ingredient.getCount(), _amount)))
				{
					return;
				}
			}
			
			final InventoryUpdate iu = new InventoryUpdate();
			boolean itemEnchantmentProcessed = (itemEnchantment == null);
			
			// Take all ingredients
			for (ItemChanceHolder ingredient : entry.getIngredients())
			{
				if (ingredient.isMaintainIngredient())
				{
					continue;
				}
				
				final long totalCount = Math.multiplyExact(list.getIngredientCount(ingredient), _amount);
				final SpecialItemType specialItem = SpecialItemType.getByClientId(ingredient.getId());
				if (specialItem != null)
				{
					// Take special item.
					switch (specialItem)
					{
						case CLAN_REPUTATION:
						{
							if (clan != null)
							{
								clan.takeReputationScore((int) totalCount);
								final SystemMessage smsg = new SystemMessage(SystemMessageId.S1_POINT_S_HAVE_BEEN_DEDUCTED_FROM_THE_CLAN_S_REPUTATION);
								smsg.addLong(totalCount);
								player.sendPacket(smsg);
							}
							break;
						}
						case FAME:
						{
							player.setFame(player.getFame() - (int) totalCount);
							player.updateUserInfo();
							// player.sendPacket(new ExBrExtraUserInfo(player));
							break;
						}
						case RAIDBOSS_POINTS:
						{
							player.setRaidbossPoints(player.getRaidbossPoints() - (int) totalCount);
							player.updateUserInfo();
							player.sendPacket(new SystemMessage(SystemMessageId.YOU_CONSUMED_S1_RAID_POINTS).addLong(totalCount));
							break;
						}
						case PC_CAFE_POINTS:
						{
							player.setPcCafePoints((int) (player.getPcCafePoints() - totalCount));
							player.sendPacket(new ExPCCafePointInfo(player.getPcCafePoints(), (int) -totalCount, 1));
							break;
						}
						default:
						{
							PacketLogger.warning("Character: " + player.getName() + " has suffered possible item loss by using multisell " + _listId + " which has non-implemented special ingredient with id: " + ingredient.getId() + ".");
							return;
						}
					}
				}
				else if (ingredient.getEnchantmentLevel() > 0)
				{
					// Take the enchanted item.
					final Item destroyedItem = inventory.destroyItem(ItemProcessType.FEE, inventory.getAllItemsByItemId(ingredient.getId(), ingredient.getEnchantmentLevel()).iterator().next(), totalCount, player, npc);
					if (destroyedItem != null)
					{
						itemEnchantmentProcessed = true;
						iu.addItem(destroyedItem);
						if (itemEnchantmentProcessed && destroyedItem.isEquipable()) // Will only consider first equipable ingredient.
						{
							itemEnchantment = new ItemInfo(destroyedItem);
						}
					}
					else
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_NEED_A_N_S1);
						sm.addItemName(ingredient.getId());
						player.sendPacket(sm);
						return;
					}
				}
				else if (!itemEnchantmentProcessed && (itemEnchantment != null) && (itemEnchantment.getItem().getId() == ingredient.getId()))
				{
					// Take the enchanted item.
					final Item destroyedItem = inventory.destroyItem(ItemProcessType.FEE, itemEnchantment.getObjectId(), totalCount, player, npc);
					if (destroyedItem != null)
					{
						itemEnchantmentProcessed = true;
						iu.addItem(destroyedItem);
						if (itemEnchantmentProcessed && destroyedItem.isEquipable()) // Will only consider first equipable ingredient.
						{
							itemEnchantment = new ItemInfo(destroyedItem);
						}
					}
					else
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_NEED_A_N_S1);
						sm.addItemName(ingredient.getId());
						player.sendPacket(sm);
						return;
					}
				}
				else
				{
					// Take a regular item.
					final Item destroyedItem = inventory.destroyItemByItemId(ItemProcessType.FEE, ingredient.getId(), totalCount, player, npc);
					if (destroyedItem != null)
					{
						iu.addItem(destroyedItem);
						if (itemEnchantmentProcessed && destroyedItem.isEquipable()) // Will only consider first equipable ingredient.
						{
							itemEnchantment = new ItemInfo(destroyedItem);
						}
					}
					else
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_NEED_S2_S1_S);
						sm.addItemName(ingredient.getId());
						sm.addLong(totalCount);
						player.sendPacket(sm);
						return;
					}
				}
			}
			
			// Generate the appropriate items
			List<ItemChanceHolder> products = entry.getProducts();
			if (list.isChanceMultisell())
			{
				final ItemChanceHolder randomProduct = ItemChanceHolder.getRandomHolder(entry.getProducts());
				products = randomProduct != null ? Collections.singletonList(randomProduct) : Collections.emptyList();
			}
			
			for (ItemChanceHolder product : products)
			{
				final long totalCount = Math.multiplyExact(list.getProductCount(product), _amount);
				final SpecialItemType specialItem = SpecialItemType.getByClientId(product.getId());
				if (specialItem != null)
				{
					// Give special item.
					switch (specialItem)
					{
						case CLAN_REPUTATION:
						{
							if (clan != null)
							{
								clan.addReputationScore((int) totalCount);
							}
							break;
						}
						case FAME:
						{
							player.setFame((int) (player.getFame() + totalCount));
							player.updateUserInfo();
							// player.sendPacket(new ExBrExtraUserInfo(player));
							break;
						}
						case RAIDBOSS_POINTS:
						{
							player.increaseRaidbossPoints((int) totalCount);
							player.updateUserInfo();
							break;
						}
						default:
						{
							PacketLogger.warning("Character: " + player.getName() + " has suffered possible item loss by using multisell " + _listId + " which has non-implemented special product with id: " + product.getId() + ".");
							return;
						}
					}
				}
				else
				{
					// Give item.
					final Item addedItem = inventory.addItem(ItemProcessType.BUY, product.getId(), totalCount, player, npc, false);
					
					// Check if the newly given item should be enchanted.
					if (itemEnchantmentProcessed && list.isMaintainEnchantment() && (itemEnchantment != null) && addedItem.isEquipable() && addedItem.getTemplate().getClass().equals(itemEnchantment.getItem().getClass()))
					{
						addedItem.setEnchantLevel(itemEnchantment.getEnchantLevel());
						addedItem.setAugmentation(itemEnchantment.getAugmentation(), false);
						if (addedItem.isWeapon())
						{
							if (itemEnchantment.getAttackElementPower() > 0)
							{
								addedItem.setAttribute(new AttributeHolder(AttributeType.findByClientId(itemEnchantment.getAttackElementType()), itemEnchantment.getAttackElementPower()), false);
							}
						}
						else
						{
							if (itemEnchantment.getAttributeDefence(AttributeType.FIRE) > 0)
							{
								addedItem.setAttribute(new AttributeHolder(AttributeType.FIRE, itemEnchantment.getAttributeDefence(AttributeType.FIRE)), false);
							}
							if (itemEnchantment.getAttributeDefence(AttributeType.WATER) > 0)
							{
								addedItem.setAttribute(new AttributeHolder(AttributeType.WATER, itemEnchantment.getAttributeDefence(AttributeType.WATER)), false);
							}
							if (itemEnchantment.getAttributeDefence(AttributeType.WIND) > 0)
							{
								addedItem.setAttribute(new AttributeHolder(AttributeType.WIND, itemEnchantment.getAttributeDefence(AttributeType.WIND)), false);
							}
							if (itemEnchantment.getAttributeDefence(AttributeType.EARTH) > 0)
							{
								addedItem.setAttribute(new AttributeHolder(AttributeType.EARTH, itemEnchantment.getAttributeDefence(AttributeType.EARTH)), false);
							}
							if (itemEnchantment.getAttributeDefence(AttributeType.HOLY) > 0)
							{
								addedItem.setAttribute(new AttributeHolder(AttributeType.HOLY, itemEnchantment.getAttributeDefence(AttributeType.HOLY)), false);
							}
							if (itemEnchantment.getAttributeDefence(AttributeType.DARK) > 0)
							{
								addedItem.setAttribute(new AttributeHolder(AttributeType.DARK, itemEnchantment.getAttributeDefence(AttributeType.DARK)), false);
							}
						}
						if (_soulCrystalOptions != null)
						{
							int pos = -1;
							for (EnsoulOption ensoul : _soulCrystalOptions)
							{
								pos++;
								addedItem.addSpecialAbility(ensoul, pos, 1, false);
							}
						}
						if (_soulCrystalSpecialOptions != null)
						{
							for (EnsoulOption ensoul : _soulCrystalSpecialOptions)
							{
								addedItem.addSpecialAbility(ensoul, 0, 2, false);
							}
						}
						
						addedItem.updateDatabase(true);
						
						// Mark that we have already upgraded the item.
						itemEnchantmentProcessed = false;
					}
					
					if (product.getEnchantmentLevel() > 0)
					{
						addedItem.setEnchantLevel(product.getEnchantmentLevel());
						addedItem.updateDatabase(true);
					}
					
					if (addedItem.getCount() > 1)
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
						sm.addItemName(addedItem.getId());
						sm.addLong(totalCount);
						player.sendPacket(sm);
					}
					else if (addedItem.getEnchantLevel() > 0)
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.ACQUIRED_S1_S2);
						sm.addLong(addedItem.getEnchantLevel());
						sm.addItemName(addedItem.getId());
						player.sendPacket(sm);
					}
					else
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1);
						sm.addItemName(addedItem);
						player.sendPacket(sm);
					}
					
					// Inventory update.
					iu.addItem(addedItem);
					player.sendPacket(new ExMultiSellResult(1, 0, (int) (addedItem.getCount())));
				}
			}
			
			// Update inventory and weight.
			player.sendInventoryUpdate(iu);
			
			// Finally, give the tax to the castle.
			if ((npc != null) && list.isApplyTaxes())
			{
				long taxPaid = 0;
				for (ItemChanceHolder ingredient : entry.getIngredients())
				{
					if (ingredient.getId() == Inventory.ADENA_ID)
					{
						taxPaid += Math.round(ingredient.getCount() * list.getIngredientMultiplier() * list.getTaxRate()) * _amount;
					}
				}
				if (taxPaid > 0)
				{
					npc.handleTaxPayment(taxPaid);
				}
			}
		}
		catch (ArithmeticException ae)
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED);
			return;
		}
		
		// Re-send multisell after successful exchange of inventory-only shown items.
		if (list.isInventoryOnly() || list.isMaintainEnchantment())
		{
			MultisellData.getInstance().separateAndSend(list.getId(), player, npc, list.isInventoryOnly(), list.getProductMultiplier(), list.getIngredientMultiplier(), 0);
		}
	}
	
	/**
	 * @param player
	 * @param list
	 * @param inventory
	 * @param clan
	 * @param ingredientId
	 * @param totalCount
	 * @return {@code false} if ingredient amount is not enough, {@code true} otherwise.
	 */
	private boolean checkIngredients(Player player, PreparedMultisellListHolder list, PlayerInventory inventory, Clan clan, int ingredientId, long totalCount)
	{
		final SpecialItemType specialItem = SpecialItemType.getByClientId(ingredientId);
		if (specialItem != null)
		{
			// Check special item.
			switch (specialItem)
			{
				case CLAN_REPUTATION:
				{
					if (clan == null)
					{
						player.sendPacket(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER_AND_CANNOT_PERFORM_THIS_ACTION);
						return false;
					}
					else if (!player.isClanLeader())
					{
						player.sendPacket(SystemMessageId.ONLY_THE_CLAN_LEADER_IS_ENABLED);
						return false;
					}
					else if (clan.getReputationScore() < totalCount)
					{
						player.sendPacket(SystemMessageId.THE_CLAN_REPUTATION_IS_TOO_LOW);
						return false;
					}
					return true;
				}
				case FAME:
				{
					if (player.getFame() < totalCount)
					{
						player.sendPacket(SystemMessageId.YOU_DON_T_HAVE_ENOUGH_FAME_TO_DO_THAT);
						return false;
					}
					return true;
				}
				case RAIDBOSS_POINTS:
				{
					if (player.getRaidbossPoints() < totalCount)
					{
						player.sendPacket(SystemMessageId.NOT_ENOUGH_RAID_POINTS);
						return false;
					}
					return true;
				}
				case PC_CAFE_POINTS:
				{
					if (player.getPcCafePoints() < totalCount)
					{
						player.sendPacket(SystemMessageId.YOU_ARE_SHORT_OF_PA_POINTS);
						return false;
					}
					return true;
				}
				default:
				{
					PacketLogger.warning("Multisell: " + _listId + " is using a non-implemented special ingredient with id: " + ingredientId + ".");
					return false;
				}
			}
		}
		// Check if the necessary items are there. If list maintains enchantment, allow all enchanted items, otherwise only unenchanted. TODO: Check how retail does it.
		else if (inventory.getInventoryItemCount(ingredientId, list.isMaintainEnchantment() ? -1 : 0, false) < totalCount)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_NEED_S2_S1_S);
			sm.addItemName(ingredientId);
			sm.addLong(totalCount);
			player.sendPacket(sm);
			return false;
		}
		return true;
	}
}