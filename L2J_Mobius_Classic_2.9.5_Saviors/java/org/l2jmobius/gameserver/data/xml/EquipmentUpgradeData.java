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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.EquipmentUpgradeHolder;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.item.enums.UpgradeType;
import org.l2jmobius.gameserver.model.item.holders.ItemEnchantHolder;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;

/**
 * @author Mobius
 */
public class EquipmentUpgradeData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(EquipmentUpgradeData.class.getName());
	private static final Map<UpgradeType, Map<Integer, EquipmentUpgradeHolder>> _upgrades = new HashMap<>();
	
	protected EquipmentUpgradeData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_upgrades.clear();
		_upgrades.put(UpgradeType.RARE, new HashMap<>());
		_upgrades.put(UpgradeType.NORMAL, new HashMap<>());
		_upgrades.put(UpgradeType.SPECIAL, new HashMap<>());
		parseDatapackDirectory("data/stats/upgrade", false);
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _upgrades.get(UpgradeType.RARE).size() + " rare upgrade equipment data.");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _upgrades.get(UpgradeType.NORMAL).size() + " normal upgrade equipment data.");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _upgrades.get(UpgradeType.SPECIAL).size() + " special upgrade equipment data.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "upgrade", upgradeNode ->
		{
			final StatSet set = new StatSet(parseAttributes(upgradeNode));
			final int id = set.getInt("id"); // Upgrade ID
			final UpgradeType type = UpgradeType.valueOf(set.getString("type")); // Type
			
			final String[] item = set.getString("item").split(","); // Required Item
			final int requiredItemId = Integer.parseInt(item[0]);
			ItemEnchantHolder requiredItem = null;
			if (ItemData.getInstance().getTemplate(requiredItemId) == null)
			{
				LOGGER.info(getClass().getSimpleName() + ": Required item with id " + requiredItemId + " does not exist.");
			}
			else
			{
				requiredItem = new ItemEnchantHolder(requiredItemId, 1, Integer.parseInt(item[1]));
			}
			
			final String materials = set.getString("materials"); // Materials
			final List<ItemHolder> materialList = new ArrayList<>();
			
			if (!materials.isEmpty())
			{
				for (String mat : materials.split(";"))
				{
					final String[] matValues = mat.split(",");
					final int matItemId = Integer.parseInt(matValues[0]);
					if (ItemData.getInstance().getTemplate(matItemId) == null)
					{
						LOGGER.info(getClass().getSimpleName() + ": Material item with id " + matItemId + " does not exist.");
					}
					else
					{
						materialList.add(new ItemHolder(matItemId, Long.parseLong(matValues[1])));
					}
				}
			}
			
			final long adena = set.getLong("adena", 0); // Cost of Upgrade
			
			final List<ItemEnchantHolder> resultItems = new ArrayList<>();
			final String[] result = set.getString("result").split(","); // Result
			final int resultId = Integer.parseInt(result[0]);
			
			if (ItemData.getInstance().getTemplate(resultId) == null)
			{
				LOGGER.info(getClass().getSimpleName() + ": Result item with id " + resultId + " does not exist.");
			}
			else
			{
				resultItems.add(new ItemEnchantHolder(resultId, 1, Integer.parseInt(result[1])));
			}
			
			final int chance = set.getInt("chance", 100);
			
			final String onFailed = set.getString("on_fail", "");
			final List<ItemEnchantHolder> onFailedList = new ArrayList<>();
			if (!Objects.equals(onFailed, ""))
			{
				for (String fail : onFailed.split(";"))
				{
					final String[] failValues = fail.split(",");
					final int failItemId = Integer.parseInt(failValues[0]);
					if (ItemData.getInstance().getTemplate(failItemId) == null)
					{
						LOGGER.info(getClass().getSimpleName() + ": Material item with id " + failItemId + " does not exist.");
					}
					else
					{
						onFailedList.add(new ItemEnchantHolder(failItemId, Long.parseLong(failValues[1])));
					}
				}
			}
			
			final String bonus = set.getString("bonus", "");
			final List<ItemEnchantHolder> bonusItems = new ArrayList<>();
			
			if (!Objects.equals(bonus, ""))
			{
				final int bonusId = Integer.parseInt(bonus.split(",")[0]);
				if (ItemData.getInstance().getTemplate(bonusId) == null)
				{
					LOGGER.info(getClass().getSimpleName() + ": Required bonus with id " + bonusId + " does not exist.");
				}
				else
				{
					bonusItems.add(new ItemEnchantHolder(bonusId, Integer.parseInt(bonus.split(",")[1])));
				}
			}
			final int bonusChance = set.getInt("chance_bonus", 0);
			
			if ((requiredItem != null) || !resultItems.isEmpty() || !materialList.isEmpty())
			{
				_upgrades.get(type).put(id, new EquipmentUpgradeHolder(id, requiredItem, adena, resultItems, chance, materialList, onFailedList.isEmpty() ? null : onFailedList, bonusItems.isEmpty() ? null : bonusItems, bonusChance));
			}
		}));
	}
	
	public EquipmentUpgradeHolder getUpgrade(UpgradeType type, int id)
	{
		return _upgrades.get(type).get(id);
	}
	
	public static EquipmentUpgradeData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EquipmentUpgradeData INSTANCE = new EquipmentUpgradeData();
	}
}
