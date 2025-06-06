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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.item.combination.CombinationItem;
import org.l2jmobius.gameserver.model.item.combination.CombinationItemReward;
import org.l2jmobius.gameserver.model.item.combination.CombinationItemType;

/**
 * @author UnAfraid
 */
public class CombinationItemsData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(CombinationItemsData.class.getName());
	
	private final List<CombinationItem> _items = new ArrayList<>();
	
	protected CombinationItemsData()
	{
		load();
	}
	
	@Override
	public synchronized void load()
	{
		_items.clear();
		parseDatapackFile("data/CombinationItems.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _items.size() + " combinations.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "item", itemNode ->
		{
			// Create a CombinationItem using attributes from itemNode.
			final CombinationItem item = new CombinationItem(new StatSet(parseAttributes(itemNode)));
			
			forEach(itemNode, "reward", rewardNode ->
			{
				// Parse reward attributes.
				final int id = parseInteger(rewardNode.getAttributes(), "id");
				final int count = parseInteger(rewardNode.getAttributes(), "count", 1);
				final int enchant = parseInteger(rewardNode.getAttributes(), "enchant", 0);
				final CombinationItemType type = parseEnum(rewardNode.getAttributes(), CombinationItemType.class, "type");
				
				// Add reward to the item.
				item.addReward(new CombinationItemReward(id, count, type, enchant));
				
				// Log if the item template does not exist.
				if ((id > 0) && (ItemData.getInstance().getTemplate(id) == null))
				{
					LOGGER.info(getClass().getSimpleName() + ": Could not find item with id " + id);
				}
			});
			
			// Add the item to the _items collection.
			_items.add(item);
		}));
	}
	
	public int getLoadedElementsCount()
	{
		return _items.size();
	}
	
	public List<CombinationItem> getItems()
	{
		return _items;
	}
	
	public CombinationItem getItemsBySlots(int firstSlot, int enchantOne, int secondSlot, int enchantTwo)
	{
		for (CombinationItem item : _items)
		{
			if ((item.getItemOne() == firstSlot) && (item.getItemTwo() == secondSlot) && (item.getEnchantOne() == enchantOne) && (item.getEnchantTwo() == enchantTwo))
			{
				return item;
			}
		}
		return null;
	}
	
	public List<CombinationItem> getItemsByFirstSlot(int id, int enchantOne)
	{
		final List<CombinationItem> result = new ArrayList<>();
		for (CombinationItem item : _items)
		{
			if ((item.getItemOne() == id) && (item.getEnchantOne() == enchantOne))
			{
				result.add(item);
			}
		}
		return result;
	}
	
	public static final CombinationItemsData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CombinationItemsData INSTANCE = new CombinationItemsData();
	}
}