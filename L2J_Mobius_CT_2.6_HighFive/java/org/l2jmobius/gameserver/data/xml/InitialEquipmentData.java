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
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.item.PlayerItemTemplate;

/**
 * This class holds the Initial Equipment information.<br>
 * What items get each newly created character and if this item is equipped upon creation (<b>Requires the item to be equippable</b>).
 * @author Zoey76
 */
public class InitialEquipmentData implements IXmlReader
{
	private final Map<PlayerClass, List<PlayerItemTemplate>> _initialEquipmentList = new EnumMap<>(PlayerClass.class);
	
	private static final String NORMAL = "data/stats/initialEquipment.xml";
	private static final String EVENT = "data/stats/initialEquipmentEvent.xml";
	
	/**
	 * Instantiates a new initial equipment data.
	 */
	protected InitialEquipmentData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_initialEquipmentList.clear();
		parseDatapackFile(Config.INITIAL_EQUIPMENT_EVENT ? EVENT : NORMAL);
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _initialEquipmentList.size() + " initial equipment data.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("equipment".equalsIgnoreCase(d.getNodeName()))
					{
						parseEquipment(d);
					}
				}
			}
		}
	}
	
	/**
	 * Parses the equipment.
	 * @param d parse an initial equipment and add it to {@link #_initialEquipmentList}
	 */
	private void parseEquipment(Node d)
	{
		NamedNodeMap attrs = d.getAttributes();
		final PlayerClass classId = PlayerClass.getPlayerClass(Integer.parseInt(attrs.getNamedItem("classId").getNodeValue()));
		final List<PlayerItemTemplate> equipList = new ArrayList<>();
		for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
		{
			if ("item".equalsIgnoreCase(c.getNodeName()))
			{
				final StatSet set = new StatSet();
				attrs = c.getAttributes();
				for (int i = 0; i < attrs.getLength(); i++)
				{
					final Node attr = attrs.item(i);
					set.set(attr.getNodeName(), attr.getNodeValue());
				}
				equipList.add(new PlayerItemTemplate(set));
			}
		}
		_initialEquipmentList.put(classId, equipList);
	}
	
	/**
	 * Gets the equipment list.
	 * @param cId the class Id for the required initial equipment.
	 * @return the initial equipment for the given class Id.
	 */
	public List<PlayerItemTemplate> getEquipmentList(PlayerClass cId)
	{
		return _initialEquipmentList.get(cId);
	}
	
	/**
	 * Gets the equipment list.
	 * @param cId the class Id for the required initial equipment.
	 * @return the initial equipment for the given class Id.
	 */
	public List<PlayerItemTemplate> getEquipmentList(int cId)
	{
		return _initialEquipmentList.get(PlayerClass.getPlayerClass(cId));
	}
	
	/**
	 * Gets the single instance of InitialEquipmentData.
	 * @return single instance of InitialEquipmentData
	 */
	public static InitialEquipmentData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final InitialEquipmentData INSTANCE = new InitialEquipmentData();
	}
}