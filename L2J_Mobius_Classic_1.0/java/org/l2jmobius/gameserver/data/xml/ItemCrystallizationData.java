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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.enums.CrystallizationType;
import org.l2jmobius.gameserver.data.holders.CrystallizationDataHolder;
import org.l2jmobius.gameserver.model.item.Armor;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.Weapon;
import org.l2jmobius.gameserver.model.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.item.type.CrystalType;

/**
 * @author UnAfraid
 */
public class ItemCrystallizationData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ItemCrystallizationData.class.getName());
	
	private final Map<CrystalType, Map<CrystallizationType, List<ItemChanceHolder>>> _crystallizationTemplates = new EnumMap<>(CrystalType.class);
	private final Map<Integer, CrystallizationDataHolder> _items = new HashMap<>();
	
	protected ItemCrystallizationData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_crystallizationTemplates.clear();
		for (CrystalType crystalType : CrystalType.values())
		{
			_crystallizationTemplates.put(crystalType, new EnumMap<>(CrystallizationType.class));
		}
		_items.clear();
		parseDatapackFile("data/CrystallizableItems.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _crystallizationTemplates.size() + " crystallization templates.");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _items.size() + " pre-defined crystallizable items.");
		
		// Generate remaining data.
		generateCrystallizationData();
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node o = n.getFirstChild(); o != null; o = o.getNextSibling())
				{
					if ("templates".equalsIgnoreCase(o.getNodeName()))
					{
						for (Node d = o.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if ("crystallizable_template".equalsIgnoreCase(d.getNodeName()))
							{
								final CrystalType crystalType = parseEnum(d.getAttributes(), CrystalType.class, "crystalType");
								final CrystallizationType crystallizationType = parseEnum(d.getAttributes(), CrystallizationType.class, "crystallizationType");
								final List<ItemChanceHolder> crystallizeRewards = new ArrayList<>();
								for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
								{
									if ("item".equalsIgnoreCase(c.getNodeName()))
									{
										final NamedNodeMap attrs = c.getAttributes();
										final int itemId = parseInteger(attrs, "id");
										final long itemCount = parseLong(attrs, "count");
										final double itemChance = parseDouble(attrs, "chance");
										crystallizeRewards.add(new ItemChanceHolder(itemId, itemChance, itemCount));
									}
								}
								
								_crystallizationTemplates.get(crystalType).put(crystallizationType, crystallizeRewards);
							}
						}
					}
					else if ("items".equalsIgnoreCase(o.getNodeName()))
					{
						for (Node d = o.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if ("crystallizable_item".equalsIgnoreCase(d.getNodeName()))
							{
								final int id = parseInteger(d.getAttributes(), "id");
								final List<ItemChanceHolder> crystallizeRewards = new ArrayList<>();
								for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
								{
									if ("item".equalsIgnoreCase(c.getNodeName()))
									{
										final NamedNodeMap attrs = c.getAttributes();
										final int itemId = parseInteger(attrs, "id");
										final long itemCount = parseLong(attrs, "count");
										final double itemChance = parseDouble(attrs, "chance");
										crystallizeRewards.add(new ItemChanceHolder(itemId, itemChance, itemCount));
									}
								}
								_items.put(id, new CrystallizationDataHolder(id, crystallizeRewards));
							}
						}
					}
				}
			}
		}
	}
	
	public int getLoadedCrystallizationTemplateCount()
	{
		return _crystallizationTemplates.size();
	}
	
	private List<ItemChanceHolder> calculateCrystallizeRewards(ItemTemplate item, List<ItemChanceHolder> crystallizeRewards)
	{
		if (crystallizeRewards == null)
		{
			return null;
		}
		
		final List<ItemChanceHolder> rewards = new ArrayList<>();
		for (ItemChanceHolder reward : crystallizeRewards)
		{
			double chance = reward.getChance() * item.getCrystalCount();
			long count = reward.getCount();
			if (chance > 100.)
			{
				final double countMul = Math.ceil(chance / 100.);
				chance /= countMul;
				count *= countMul;
			}
			
			rewards.add(new ItemChanceHolder(reward.getId(), chance, count));
		}
		
		return rewards;
	}
	
	private void generateCrystallizationData()
	{
		final int previousCount = _items.size();
		for (ItemTemplate item : ItemData.getInstance().getAllItems())
		{
			// Check if the data has not been generated.
			if (((item instanceof Weapon) || (item instanceof Armor)) && item.isCrystallizable() && !_items.containsKey(item.getId()))
			{
				final List<ItemChanceHolder> holder = _crystallizationTemplates.get(item.getCrystalType()).get((item instanceof Weapon) ? CrystallizationType.WEAPON : CrystallizationType.ARMOR);
				if (holder != null)
				{
					_items.put(item.getId(), new CrystallizationDataHolder(item.getId(), calculateCrystallizeRewards(item, holder)));
				}
			}
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Generated " + (_items.size() - previousCount) + " crystallizable items from templates.");
	}
	
	public List<ItemChanceHolder> getCrystallizationTemplate(CrystalType crystalType, CrystallizationType crystallizationType)
	{
		return _crystallizationTemplates.get(crystalType).get(crystallizationType);
	}
	
	/**
	 * @param itemId
	 * @return {@code CrystallizationData} for unenchanted items (enchanted items just have different crystal count, but same rewards),<br>
	 *         or {@code null} if there is no such data registered.
	 */
	public CrystallizationDataHolder getCrystallizationData(int itemId)
	{
		return _items.get(itemId);
	}
	
	/**
	 * @param item to calculate its worth in crystals.
	 * @return List of {@code ItemChanceHolder} for the rewards with altered crystal count.
	 */
	public List<ItemChanceHolder> getCrystallizationRewards(Item item)
	{
		final List<ItemChanceHolder> result = new ArrayList<>();
		final int crystalItemId = item.getTemplate().getCrystalItemId();
		final CrystallizationDataHolder data = getCrystallizationData(item.getId());
		if (data != null)
		{
			// If there are no crystals on the template, add such.
			boolean found = false;
			final List<ItemChanceHolder> items = data.getItems();
			for (ItemChanceHolder holder : items)
			{
				if (holder.getId() == crystalItemId)
				{
					found = true;
					break;
				}
			}
			if (!found)
			{
				result.add(new ItemChanceHolder(crystalItemId, 100, item.getCrystalCount()));
			}
			
			result.addAll(items);
		}
		else
		{
			// Add basic crystal reward.
			result.add(new ItemChanceHolder(crystalItemId, 100, item.getCrystalCount()));
		}
		
		return result;
	}
	
	/**
	 * Gets the single instance of ItemCrystalizationData.
	 * @return single instance of ItemCrystalizationData
	 */
	public static ItemCrystallizationData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ItemCrystallizationData INSTANCE = new ItemCrystallizationData();
	}
}
