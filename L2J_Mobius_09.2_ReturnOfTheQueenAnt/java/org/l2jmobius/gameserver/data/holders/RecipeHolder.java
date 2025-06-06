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
package org.l2jmobius.gameserver.data.holders;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.StatusUpdateType;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * A holder representing a craftable recipe based on the former RecipeList.<br>
 * It contains all the recipe data and methods required for crafting this recipe.
 * @author Nik
 */
public class RecipeHolder
{
	/** List of materials required to craft this recipe. */
	private final List<ItemHolder> _materials;
	
	/** Group of products where a single product will randomly be selected upon crafting. */
	private final List<ItemChanceHolder> _productGroup;
	
	private final List<ItemHolder> _npcFee;
	
	/** Stats and amount required to perform the craft. */
	private final Map<StatusUpdateType, Double> _statUse;
	
	private final int _id;
	private final int _level;
	private final int _itemId;
	private final String _name;
	private final double _successRate;
	private final boolean _isCommonRecipe;
	private final double _maxOfferingBonus;
	private final long _maxOffering;
	
	public RecipeHolder(StatSet set, List<ItemHolder> ingredients, List<ItemChanceHolder> productGroup, List<ItemHolder> npcFee, Map<StatusUpdateType, Double> statUse)
	{
		_id = set.getInt("id");
		_level = set.getInt("level");
		_itemId = set.getInt("itemId");
		_name = set.getString("name");
		_successRate = set.getDouble("successRate");
		_isCommonRecipe = set.getBoolean("isCommonRecipe");
		_maxOfferingBonus = set.getDouble("maxOfferingBonus", Math.max(0, 100 - _successRate));
		_maxOffering = set.getLong("maxOffering", 0);
		_materials = Collections.unmodifiableList(ingredients);
		_productGroup = Collections.unmodifiableList(productGroup);
		_npcFee = Collections.unmodifiableList(npcFee);
		_statUse = Collections.unmodifiableMap(statUse);
	}
	
	/**
	 * @return the recipe id, NOT the recipe's item id
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * @return the crafting level needed to use this RecipeList.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * @return the recipe's item id.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * @return the name of the RecipeList.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * @return the crafting success rate when using the RecipeList.
	 */
	public double getSuccessRate()
	{
		return _successRate;
	}
	
	/**
	 * @return {@code true} if this a Dwarven recipe or {@code false} if it is a Common recipe
	 */
	public boolean isDwarvenRecipe()
	{
		return !_isCommonRecipe;
	}
	
	/**
	 * @return list of materials required to complete the recipe.
	 */
	public List<ItemHolder> getMaterials()
	{
		return _materials;
	}
	
	/**
	 * @return the whole group of products from which one random item will result in being crafted.
	 */
	public List<ItemChanceHolder> getProductGroup()
	{
		return _productGroup;
	}
	
	/**
	 * @return list of items that NPCs take for crafting this recipe.
	 */
	public List<ItemHolder> getNpcFee()
	{
		return _npcFee;
	}
	
	/**
	 * @return the table containing all RecipeStatHolder of the statUse parameter of the RecipeList.
	 */
	public Map<StatusUpdateType, Double> getStatUse()
	{
		return _statUse;
	}
	
	/**
	 * @return Maximum bonus success rate when maximum offering is reached. Default is the rate needed to reach 100% success rate.
	 */
	public double getMaxOfferingBonus()
	{
		return _maxOfferingBonus;
	}
	
	/**
	 * @return Maximum amount of items' adena worth offering. {@code 0} if this recipe does not allow offering.
	 */
	public long getMaxOffering()
	{
		return _maxOffering;
	}
	
	/**
	 * Picks a random number then attempts to get a product from the group based on it.
	 * @return {@code ItemChanceHolder} that is the randomly picked product from the group,<br>
	 *         or {@code null} if the whole chance sum of the products in the group didn't manage to outnumber the random.
	 */
	public ItemChanceHolder getRandomProduct()
	{
		double random = Rnd.get(100);
		for (ItemChanceHolder product : _productGroup)
		{
			if (product.getChance() > random)
			{
				return product;
			}
			
			random -= product.getChance();
		}
		
		return null;
	}
	
	public boolean checkNecessaryStats(Player player, Player manufacturer, boolean sendMessage)
	{
		for (Entry<StatusUpdateType, Double> entry : _statUse.entrySet())
		{
			final StatusUpdateType stat = entry.getKey();
			final double requiredAmount = entry.getValue();
			
			// Less than or equals to because some stats bad interraction - like HP could kill the player if it is taken all.
			if (stat.getValue(manufacturer) <= requiredAmount)
			{
				if (sendMessage)
				{
					switch (stat)
					{
						case CUR_HP:
						{
							player.sendPacket(SystemMessageId.NOT_ENOUGH_HP);
							break;
						}
						case CUR_MP:
						{
							player.sendPacket(SystemMessageId.NOT_ENOUGH_MP);
							break;
						}
						default:
						{
							player.sendMessage("You need " + requiredAmount + " " + stat.toString().toLowerCase() + " to perform this craft.");
							break;
						}
					}
				}
				return false;
			}
		}
		return true;
	}
	
	/**
	 * @param player the player's inventory to check.
	 * @param sendMessage send system messages for item requirements if there is missing ingredient.
	 * @return {@code true} if all necessary ingredients are met, {@code false} if there are missing ingredients.
	 */
	public boolean checkNecessaryIngredients(Player player, boolean sendMessage)
	{
		for (ItemHolder ingredient : _materials)
		{
			final long count = player.getInventory().getInventoryItemCount(ingredient.getId(), -1);
			if (count < ingredient.getCount())
			{
				if (sendMessage)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_NEED_S2_MORE_S1_S);
					sm.addItemName(ingredient.getId());
					sm.addLong(ingredient.getCount() - count);
					player.sendPacket(sm);
				}
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * @param player the player requesting the craft.
	 * @param manufacturer the player doing the craft (either the same player or manufacture shop).
	 * @param success {@code true} to give the product item to the player, {@code false} otherwise.
	 * @param craftingCritical {@code true} to give double of the product (if success), {@code false} otherwise.
	 * @param sendMessage send system messages of the process.
	 * @return {@code ItemHolder} of the randomly created product (even if it is failing craft), {@code null} if the item creation was not performed due to failed checks.
	 */
	public ItemHolder doCraft(Player player, Player manufacturer, boolean success, boolean craftingCritical, boolean sendMessage)
	{
		if (!checkNecessaryStats(player, manufacturer, sendMessage))
		{
			return null;
		}
		
		if (!checkNecessaryIngredients(player, sendMessage))
		{
			return null;
		}
		
		// Take necessary stats.
		for (Entry<StatusUpdateType, Double> entry : _statUse.entrySet())
		{
			final StatusUpdateType stat = entry.getKey();
			final double requiredAmount = entry.getValue();
			
			switch (stat)
			{
				case CUR_HP:
				{
					manufacturer.reduceCurrentHp(requiredAmount, manufacturer, null);
					break;
				}
				case CUR_MP:
				{
					manufacturer.reduceCurrentMp(requiredAmount);
					break;
				}
				case CUR_CP:
				{
					manufacturer.getStatus().reduceCp((int) requiredAmount);
					break;
				}
				case EXP:
				{
					manufacturer.getStat().removeExp((long) requiredAmount);
					break;
				}
				case REPUTATION:
				{
					manufacturer.setReputation((int) (manufacturer.getReputation() - requiredAmount));
					break;
				}
			}
		}
		
		// Take necessary ingredients. If there was problem destroying item, return null to indicate that process didn't go well.
		for (ItemHolder material : _materials)
		{
			if (!player.destroyItemByItemId(ItemProcessType.CRAFT, material.getId(), material.getCount(), manufacturer, sendMessage))
			{
				return null;
			}
		}
		
		// Check if success. Luck triggers no matter the success rate - even with 100% craft. Luck chance is taken from your stat and not manufacturer's stat.
		final ItemHolder result = getRandomProduct();
		if (success)
		{
			player.addItem(ItemProcessType.CRAFT, result, manufacturer, true);
			
			// Award another item if it is crafting critical. Double blessed items is very, very rare, but still possible.
			if (craftingCritical)
			{
				player.addItem(ItemProcessType.CRAFT, result, manufacturer, true);
			}
		}
		
		return result;
	}
}
