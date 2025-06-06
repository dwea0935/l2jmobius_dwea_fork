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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.VariationInstance;
import org.l2jmobius.gameserver.model.item.EtcItem;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.options.OptionDataCategory;
import org.l2jmobius.gameserver.model.options.OptionDataGroup;
import org.l2jmobius.gameserver.model.options.Options;
import org.l2jmobius.gameserver.model.options.Variation;
import org.l2jmobius.gameserver.model.options.VariationFee;

/**
 * @author Pere, Mobius
 */
public class VariationData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(VariationData.class.getSimpleName());
	
	private final Map<Integer, Variation> _variations = new ConcurrentHashMap<>();
	private final Map<Integer, Map<Integer, VariationFee>> _fees = new ConcurrentHashMap<>();
	
	protected VariationData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_variations.clear();
		_fees.clear();
		parseDatapackFile("data/stats/augmentation/Variations.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _variations.size() + " variations.");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _fees.size() + " fees.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode ->
		{
			forEach(listNode, "variations", variationsNode -> forEach(variationsNode, "variation", variationNode ->
			{
				final int mineralId = parseInteger(variationNode.getAttributes(), "mineralId");
				if (ItemData.getInstance().getTemplate(mineralId) == null)
				{
					LOGGER.warning(getClass().getSimpleName() + ": Mineral with item id " + mineralId + " was not found.");
				}
				final Variation variation = new Variation(mineralId);
				
				forEach(variationNode, "optionGroup", groupNode ->
				{
					final int order = parseInteger(groupNode.getAttributes(), "order");
					final List<OptionDataCategory> sets = new ArrayList<>();
					forEach(groupNode, "optionCategory", categoryNode ->
					{
						final double chance = parseDouble(categoryNode.getAttributes(), "chance");
						final Map<Options, Double> options = new HashMap<>();
						forEach(categoryNode, "option", optionNode ->
						{
							final double optionChance = parseDouble(optionNode.getAttributes(), "chance");
							final int optionId = parseInteger(optionNode.getAttributes(), "id");
							final Options opt = OptionData.getInstance().getOptions(optionId);
							if (opt == null)
							{
								LOGGER.warning(getClass().getSimpleName() + ": Null option for id " + optionId + " mineral " + mineralId);
								return;
							}
							options.put(opt, optionChance);
						});
						forEach(categoryNode, "optionRange", optionNode ->
						{
							final double optionChance = parseDouble(optionNode.getAttributes(), "chance");
							final int fromId = parseInteger(optionNode.getAttributes(), "from");
							final int toId = parseInteger(optionNode.getAttributes(), "to");
							for (int id = fromId; id <= toId; id++)
							{
								final Options op = OptionData.getInstance().getOptions(id);
								if (op == null)
								{
									LOGGER.warning(getClass().getSimpleName() + ": Null option for id " + id + " mineral " + mineralId);
									return;
								}
								options.put(op, optionChance);
							}
						});
						
						// Support for specific item ids.
						final Set<Integer> itemIds = new HashSet<>();
						forEach(categoryNode, "item", optionNode ->
						{
							final int itemId = parseInteger(optionNode.getAttributes(), "id");
							itemIds.add(itemId);
						});
						forEach(categoryNode, "items", optionNode ->
						{
							final int fromId = parseInteger(optionNode.getAttributes(), "from");
							final int toId = parseInteger(optionNode.getAttributes(), "to");
							for (int id = fromId; id <= toId; id++)
							{
								itemIds.add(id);
							}
						});
						
						sets.add(new OptionDataCategory(options, itemIds, chance));
					});
					
					variation.setEffectGroup(order, new OptionDataGroup(sets));
				});
				
				_variations.put(mineralId, variation);
				((EtcItem) ItemData.getInstance().getTemplate(mineralId)).setMineral();
			}));
			
			final Map<Integer, List<Integer>> itemGroups = new HashMap<>();
			forEach(listNode, "itemGroups", variationsNode -> forEach(variationsNode, "itemGroup", variationNode ->
			{
				final int id = parseInteger(variationNode.getAttributes(), "id");
				final List<Integer> items = new ArrayList<>();
				forEach(variationNode, "item", itemNode ->
				{
					final int itemId = parseInteger(itemNode.getAttributes(), "id");
					if (ItemData.getInstance().getTemplate(itemId) == null)
					{
						LOGGER.warning(getClass().getSimpleName() + ": Item with id " + itemId + " was not found.");
					}
					items.add(itemId);
				});
				
				itemGroups.put(id, items);
			}));
			
			forEach(listNode, "fees", variationNode -> forEach(variationNode, "fee", feeNode ->
			{
				final int itemGroupId = parseInteger(feeNode.getAttributes(), "itemGroup");
				final List<Integer> itemGroup = itemGroups.get(itemGroupId);
				final int itemId = parseInteger(feeNode.getAttributes(), "itemId", 0);
				final long itemCount = parseLong(feeNode.getAttributes(), "itemCount", 0L);
				final long adenaFee = parseLong(feeNode.getAttributes(), "adenaFee", 0L);
				final long cancelFee = parseLong(feeNode.getAttributes(), "cancelFee", 0L);
				if ((itemId != 0) && (ItemData.getInstance().getTemplate(itemId) == null))
				{
					LOGGER.warning(getClass().getSimpleName() + ": Item with id " + itemId + " was not found.");
				}
				
				final VariationFee fee = new VariationFee(itemId, itemCount, adenaFee, cancelFee);
				final Map<Integer, VariationFee> feeByMinerals = new HashMap<>();
				forEach(feeNode, "mineral", mineralNode ->
				{
					final int mId = parseInteger(mineralNode.getAttributes(), "id");
					feeByMinerals.put(mId, fee);
				});
				forEach(feeNode, "mineralRange", mineralNode ->
				{
					final int fromId = parseInteger(mineralNode.getAttributes(), "from");
					final int toId = parseInteger(mineralNode.getAttributes(), "to");
					for (int id = fromId; id <= toId; id++)
					{
						feeByMinerals.put(id, fee);
					}
				});
				
				for (int item : itemGroup)
				{
					Map<Integer, VariationFee> fees = _fees.get(item);
					if (fees == null)
					{
						fees = new HashMap<>();
					}
					fees.putAll(feeByMinerals);
					_fees.put(item, fees);
				}
			}));
		});
	}
	
	public int getVariationCount()
	{
		return _variations.size();
	}
	
	public int getFeeCount()
	{
		return _fees.size();
	}
	
	/**
	 * Generate a new random variation instance
	 * @param variation The variation template to generate the variation instance from
	 * @param targetItem The item on which the variation will be applied
	 * @return VariationInstance
	 */
	public VariationInstance generateRandomVariation(Variation variation, Item targetItem)
	{
		return generateRandomVariation(variation, targetItem.getId());
	}
	
	private VariationInstance generateRandomVariation(Variation variation, int targetItemId)
	{
		final Options option1 = variation.getRandomEffect(0, targetItemId);
		final Options option2 = variation.getRandomEffect(1, targetItemId);
		return new VariationInstance(variation.getMineralId(), option1, option2);
	}
	
	public Variation getVariation(int mineralId)
	{
		return _variations.get(mineralId);
	}
	
	public VariationFee getFee(int itemId, int mineralId)
	{
		return _fees.getOrDefault(itemId, Collections.emptyMap()).get(mineralId);
	}
	
	public long getCancelFee(int itemId, int mineralId)
	{
		final Map<Integer, VariationFee> fees = _fees.get(itemId);
		if (fees == null)
		{
			return -1;
		}
		
		VariationFee fee = fees.get(mineralId);
		if (fee == null)
		{
			// FIXME This will happen when the data is pre-rework or when augments were manually given, but still that's a cheap solution
			LOGGER.warning(getClass().getSimpleName() + ": Cancellation fee not found for item [" + itemId + "] and mineral [" + mineralId + "]");
			fee = fees.values().iterator().next();
			if (fee == null)
			{
				return -1;
			}
		}
		
		return fee.getCancelFee();
	}
	
	public boolean hasFeeData(int itemId)
	{
		final Map<Integer, VariationFee> itemFees = _fees.get(itemId);
		return (itemFees != null) && !itemFees.isEmpty();
	}
	
	public static VariationData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final VariationData INSTANCE = new VariationData();
	}
}
