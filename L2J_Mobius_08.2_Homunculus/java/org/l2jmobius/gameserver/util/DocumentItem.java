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
package org.l2jmobius.gameserver.util;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.ExtractableProduct;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemSkillType;
import org.l2jmobius.gameserver.model.item.holders.ItemSkillHolder;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.model.stats.functions.FuncTemplate;

/**
 * @author mkizub, JIV
 */
public class DocumentItem extends DocumentBase implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(DocumentItem.class.getName());
	
	private DocumentItemDataHolder _currentItem = null;
	private final List<ItemTemplate> _itemsInFile = new ArrayList<>();
	
	private class DocumentItemDataHolder
	{
		public DocumentItemDataHolder()
		{
		}
		
		int id;
		String type;
		StatSet set;
		int currentLevel;
		ItemTemplate item;
	}
	
	public DocumentItem(File file)
	{
		super(file);
	}
	
	@Override
	protected StatSet getStatSet()
	{
		return _currentItem.set;
	}
	
	@Override
	protected String getTableValue(String name)
	{
		return _tables.get(name)[_currentItem.currentLevel];
	}
	
	@Override
	protected String getTableValue(String name, int idx)
	{
		return _tables.get(name)[idx - 1];
	}
	
	@Override
	protected void parseDocument(Document document)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("item".equalsIgnoreCase(d.getNodeName()))
					{
						try
						{
							_currentItem = new DocumentItemDataHolder();
							parseItem(d);
							_itemsInFile.add(_currentItem.item);
							resetTable();
						}
						catch (Exception e)
						{
							LOGGER.log(Level.WARNING, "Cannot create item " + _currentItem.id, e);
						}
					}
				}
			}
		}
	}
	
	private void parseItem(Node node) throws InvocationTargetException
	{
		Node n = node;
		final int itemId = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
		final String className = n.getAttributes().getNamedItem("type").getNodeValue();
		final String itemName = n.getAttributes().getNamedItem("name").getNodeValue();
		final String additionalName = n.getAttributes().getNamedItem("additionalName") != null ? n.getAttributes().getNamedItem("additionalName").getNodeValue() : null;
		_currentItem.id = itemId;
		_currentItem.type = className;
		_currentItem.set = new StatSet();
		_currentItem.set.set("item_id", itemId);
		_currentItem.set.set("name", itemName);
		_currentItem.set.set("additionalName", additionalName);
		
		final Node first = n.getFirstChild();
		for (n = first; n != null; n = n.getNextSibling())
		{
			if ("table".equalsIgnoreCase(n.getNodeName()))
			{
				if (_currentItem.item != null)
				{
					throw new IllegalStateException("Item created but table node found! Item " + itemId);
				}
				parseTable(n);
			}
			else if ("set".equalsIgnoreCase(n.getNodeName()))
			{
				if (_currentItem.item != null)
				{
					throw new IllegalStateException("Item created but set node found! Item " + itemId);
				}
				parseBeanSet(n, _currentItem.set, 1);
			}
			else if ("stats".equalsIgnoreCase(n.getNodeName()))
			{
				makeItem();
				for (Node b = n.getFirstChild(); b != null; b = b.getNextSibling())
				{
					if ("stat".equalsIgnoreCase(b.getNodeName()))
					{
						final Stat type = Stat.valueOfXml(b.getAttributes().getNamedItem("type").getNodeValue());
						final double value = Double.parseDouble(b.getTextContent());
						_currentItem.item.addFunctionTemplate(new FuncTemplate(null, null, "add", 0x00, type, value));
					}
				}
			}
			else if ("skills".equalsIgnoreCase(n.getNodeName()))
			{
				makeItem();
				for (Node b = n.getFirstChild(); b != null; b = b.getNextSibling())
				{
					if ("skill".equalsIgnoreCase(b.getNodeName()))
					{
						final int id = parseInteger(b.getAttributes(), "id");
						final int level = parseInteger(b.getAttributes(), "level");
						final int subLevel = parseInteger(b.getAttributes(), "subLevel", 0);
						final ItemSkillType type = parseEnum(b.getAttributes(), ItemSkillType.class, "type", ItemSkillType.NORMAL);
						final int chance = parseInteger(b.getAttributes(), "type_chance", 100);
						final int value = parseInteger(b.getAttributes(), "type_value", 0);
						if (type == ItemSkillType.ON_ENCHANT)
						{
							final int enchantLimit = _currentItem.item.getEnchantLimit();
							if ((enchantLimit > 0) && (value > enchantLimit))
							{
								LOGGER.warning(getClass().getSimpleName() + ": Item " + itemId + " has ON_ENCHANT value greater than it's enchant limit.");
							}
						}
						_currentItem.item.addSkill(new ItemSkillHolder(id, level, subLevel, type, chance, value));
					}
				}
			}
			else if ("capsuled_items".equalsIgnoreCase(n.getNodeName()))
			{
				makeItem();
				for (Node b = n.getFirstChild(); b != null; b = b.getNextSibling())
				{
					if ("item".equals(b.getNodeName()))
					{
						final int id = parseInteger(b.getAttributes(), "id");
						final long min = parseLong(b.getAttributes(), "min");
						final long max = parseLong(b.getAttributes(), "max");
						final double chance = parseDouble(b.getAttributes(), "chance");
						final int minEnchant = parseInteger(b.getAttributes(), "minEnchant", 0);
						final int maxEnchant = parseInteger(b.getAttributes(), "maxEnchant", 0);
						_currentItem.item.addCapsuledItem(new ExtractableProduct(id, min, max, chance, minEnchant, maxEnchant));
					}
				}
			}
			else if ("cond".equalsIgnoreCase(n.getNodeName()))
			{
				makeItem();
				final Condition condition = parseCondition(n.getFirstChild(), _currentItem.item);
				final Node msg = n.getAttributes().getNamedItem("msg");
				final Node msgId = n.getAttributes().getNamedItem("msgId");
				if ((condition != null) && (msg != null))
				{
					condition.setMessage(msg.getNodeValue());
				}
				else if ((condition != null) && (msgId != null))
				{
					condition.setMessageId(Integer.decode(getValue(msgId.getNodeValue(), null)));
					final Node addName = n.getAttributes().getNamedItem("addName");
					if ((addName != null) && (Integer.decode(getValue(msgId.getNodeValue(), null)) > 0))
					{
						condition.addName();
					}
				}
				_currentItem.item.attachCondition(condition);
			}
		}
		// bah! in this point item doesn't have to be still created
		makeItem();
	}
	
	private void makeItem() throws InvocationTargetException
	{
		// If item exists just reload the data.
		if (_currentItem.item != null)
		{
			_currentItem.item.set(_currentItem.set);
			return;
		}
		
		try
		{
			final Constructor<?> itemClass = Class.forName("org.l2jmobius.gameserver.model.item." + _currentItem.type).getConstructor(StatSet.class);
			_currentItem.item = (ItemTemplate) itemClass.newInstance(_currentItem.set);
		}
		catch (Exception e)
		{
			throw new InvocationTargetException(e);
		}
	}
	
	public List<ItemTemplate> getItemList()
	{
		return _itemsInFile;
	}
	
	@Override
	public void load()
	{
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
	}
}
