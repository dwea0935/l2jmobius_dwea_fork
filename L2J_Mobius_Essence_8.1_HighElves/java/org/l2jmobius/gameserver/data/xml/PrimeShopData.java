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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.primeshop.PrimeShopGroup;
import org.l2jmobius.gameserver.model.primeshop.PrimeShopItem;
import org.l2jmobius.gameserver.network.serverpackets.primeshop.ExBRProductInfo;

/**
 * @author Gnacik, UnAfraid
 */
public class PrimeShopData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(PrimeShopData.class.getName());
	
	private final Map<Integer, PrimeShopGroup> _primeItems = new LinkedHashMap<>();
	
	protected PrimeShopData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_primeItems.clear();
		parseDatapackFile("data/PrimeShop.xml");
		
		if (!_primeItems.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _primeItems.size() + " items.");
		}
		else
		{
			LOGGER.info(getClass().getSimpleName() + ": System is disabled.");
		}
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				final NamedNodeMap at = n.getAttributes();
				final Node attribute = at.getNamedItem("enabled");
				if ((attribute != null) && Boolean.parseBoolean(attribute.getNodeValue()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("item".equalsIgnoreCase(d.getNodeName()))
						{
							NamedNodeMap attrs = d.getAttributes();
							Node att;
							final StatSet set = new StatSet();
							for (int i = 0; i < attrs.getLength(); i++)
							{
								att = attrs.item(i);
								set.set(att.getNodeName(), att.getNodeValue());
							}
							
							final List<PrimeShopItem> items = new ArrayList<>();
							for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
							{
								if ("item".equalsIgnoreCase(b.getNodeName()))
								{
									attrs = b.getAttributes();
									
									final int itemId = parseInteger(attrs, "itemId");
									final int count = parseInteger(attrs, "count");
									final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
									if (item == null)
									{
										LOGGER.severe(getClass().getSimpleName() + ": Item template null for itemId: " + itemId + " brId: " + set.getInt("id"));
										return;
									}
									
									items.add(new PrimeShopItem(itemId, count, item.getWeight(), item.isTradeable() ? 1 : 0));
								}
							}
							
							_primeItems.put(set.getInt("id"), new PrimeShopGroup(set, items));
						}
					}
				}
			}
		}
	}
	
	/**
	 * Displays product information for a specified item to a player.
	 * @param player the {@link Player} to whom the product information will be shown.
	 * @param productId the unique identifier of the item in the Prime Shop.
	 */
	public void showProductInfo(Player player, int productId)
	{
		final PrimeShopGroup item = _primeItems.get(productId);
		if ((player == null) || (item == null))
		{
			return;
		}
		
		player.sendPacket(new ExBRProductInfo(item, player));
	}
	
	/**
	 * Retrieves a {@link PrimeShopGroup} item from the Prime Shop based on its unique ID.
	 * @param productId the unique identifier of the item in the Prime Shop.
	 * @return the {@link PrimeShopGroup} item associated with the specified ID, or {@code null} if not found.
	 */
	public PrimeShopGroup getItem(int productId)
	{
		return _primeItems.get(productId);
	}
	
	/**
	 * Retrieves all items available in the Prime Shop.
	 * @return a {@link Map} containing all Prime Shop items, where the key is the item ID and the value is the {@link PrimeShopGroup} item.
	 */
	public Map<Integer, PrimeShopGroup> getPrimeItems()
	{
		return _primeItems;
	}
	
	public static PrimeShopData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PrimeShopData INSTANCE = new PrimeShopData();
	}
}
