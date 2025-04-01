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

import java.util.List;

import org.l2jmobius.gameserver.model.item.holders.ItemEnchantHolder;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;

/**
 * @author Mobius
 */
public class EquipmentUpgradeHolder
{
	private final int _id;
	private final ItemEnchantHolder _requiredItem;
	private final long _adena;
	private final List<ItemEnchantHolder> _results;
	private final int _chance;
	private final List<ItemHolder> _materials;
	private final List<ItemEnchantHolder> _onFail;
	private final List<ItemEnchantHolder> _bonus;
	private final int _bonusChance;
	
	public EquipmentUpgradeHolder(int id, ItemEnchantHolder requiredItem, long adena, List<ItemEnchantHolder> results, int chance, List<ItemHolder> materials, List<ItemEnchantHolder> onFail, List<ItemEnchantHolder> bonus, int bonusChance)
	{
		_id = id;
		_requiredItem = requiredItem;
		_materials = materials;
		_adena = adena;
		_results = results;
		_chance = chance;
		_onFail = onFail;
		_bonus = bonus;
		_bonusChance = bonusChance;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public List<ItemHolder> getMaterials()
	{
		return _materials;
	}
	
	public long getAdena()
	{
		return _adena;
	}
	
	public List<ItemEnchantHolder> getResult()
	{
		return _results;
	}
	
	public int getChance()
	{
		return _chance;
	}
	
	public List<ItemEnchantHolder> getBonus()
	{
		return _bonus;
	}
	
	public List<ItemEnchantHolder> getOnFail()
	{
		return _onFail;
	}
	
	public int getBonusChance()
	{
		return _bonusChance;
	}
	
	public ItemEnchantHolder getRequiredItem()
	{
		return _requiredItem;
	}
}
