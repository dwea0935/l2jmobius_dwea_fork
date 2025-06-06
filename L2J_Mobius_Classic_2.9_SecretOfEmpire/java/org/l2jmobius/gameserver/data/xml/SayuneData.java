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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.SayuneEntry;

/**
 * @author UnAfraid
 */
public class SayuneData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(SayuneData.class.getName());
	
	private final Map<Integer, SayuneEntry> _maps = new HashMap<>();
	
	protected SayuneData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackFile("data/SayuneData.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _maps.size() + " maps.");
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
					if ("map".equalsIgnoreCase(d.getNodeName()))
					{
						final int id = parseInteger(d.getAttributes(), "id");
						final SayuneEntry map = new SayuneEntry(id);
						parseEntries(map, d);
						_maps.put(map.getId(), map);
					}
				}
			}
		}
	}
	
	private void parseEntries(SayuneEntry lastEntry, Node n)
	{
		NamedNodeMap attrs;
		for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
		{
			if ("selector".equals(d.getNodeName()) || "choice".equals(d.getNodeName()) || "loc".equals(d.getNodeName()))
			{
				attrs = d.getAttributes();
				final int id = parseInteger(attrs, "id");
				final int x = parseInteger(attrs, "x");
				final int y = parseInteger(attrs, "y");
				final int z = parseInteger(attrs, "z");
				parseEntries(lastEntry.addInnerEntry(new SayuneEntry("selector".equals(d.getNodeName()), id, x, y, z)), d);
			}
		}
	}
	
	/**
	 * Retrieves a specific Sayune map entry by its unique ID.
	 * @param id the unique identifier of the Sayune map to retrieve
	 * @return the {@code SayuneEntry} associated with the specified ID, or {@code null} if no map entry exists for this ID
	 */
	public SayuneEntry getMap(int id)
	{
		return _maps.get(id);
	}
	
	/**
	 * Retrieves a collection of all available Sayune map entries.
	 * @return a collection of {@code SayuneEntry} objects representing all maps stored in this data structure
	 */
	public Collection<SayuneEntry> getMaps()
	{
		return _maps.values();
	}
	
	public static SayuneData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SayuneData INSTANCE = new SayuneData();
	}
}
