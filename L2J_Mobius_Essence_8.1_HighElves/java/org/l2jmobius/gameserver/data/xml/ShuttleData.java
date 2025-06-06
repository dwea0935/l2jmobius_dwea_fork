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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.VehiclePathPoint;
import org.l2jmobius.gameserver.model.actor.instance.Shuttle;
import org.l2jmobius.gameserver.model.actor.templates.CreatureTemplate;
import org.l2jmobius.gameserver.model.shuttle.ShuttleDataHolder;
import org.l2jmobius.gameserver.model.shuttle.ShuttleEngine;
import org.l2jmobius.gameserver.model.shuttle.ShuttleStop;

/**
 * @author UnAfraid
 */
public class ShuttleData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ShuttleData.class.getName());
	
	private final Map<Integer, ShuttleDataHolder> _shuttles = new HashMap<>();
	private final Map<Integer, Shuttle> _shuttleInstances = new HashMap<>();
	
	protected ShuttleData()
	{
		load();
	}
	
	@Override
	public synchronized void load()
	{
		if (!_shuttleInstances.isEmpty())
		{
			for (Shuttle shuttle : _shuttleInstances.values())
			{
				shuttle.deleteMe();
			}
			_shuttleInstances.clear();
		}
		parseDatapackFile("data/ShuttleData.xml");
		init();
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _shuttles.size() + " shuttles.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		NamedNodeMap attrs;
		StatSet set;
		Node att;
		ShuttleDataHolder data;
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("shuttle".equalsIgnoreCase(d.getNodeName()))
					{
						attrs = d.getAttributes();
						set = new StatSet();
						for (int i = 0; i < attrs.getLength(); i++)
						{
							att = attrs.item(i);
							set.set(att.getNodeName(), att.getNodeValue());
						}
						data = new ShuttleDataHolder(set);
						for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
						{
							if ("doors".equalsIgnoreCase(b.getNodeName()))
							{
								for (Node a = b.getFirstChild(); a != null; a = a.getNextSibling())
								{
									if ("door".equalsIgnoreCase(a.getNodeName()))
									{
										attrs = a.getAttributes();
										data.addDoor(parseInteger(attrs, "id"));
									}
								}
							}
							else if ("stops".equalsIgnoreCase(b.getNodeName()))
							{
								for (Node a = b.getFirstChild(); a != null; a = a.getNextSibling())
								{
									if ("stop".equalsIgnoreCase(a.getNodeName()))
									{
										attrs = a.getAttributes();
										final ShuttleStop stop = new ShuttleStop(parseInteger(attrs, "id"));
										for (Node z = a.getFirstChild(); z != null; z = z.getNextSibling())
										{
											if ("dimension".equalsIgnoreCase(z.getNodeName()))
											{
												attrs = z.getAttributes();
												stop.addDimension(new Location(parseInteger(attrs, "x"), parseInteger(attrs, "y"), parseInteger(attrs, "z")));
											}
										}
										data.addStop(stop);
									}
								}
							}
							else if ("routes".equalsIgnoreCase(b.getNodeName()))
							{
								for (Node a = b.getFirstChild(); a != null; a = a.getNextSibling())
								{
									if ("route".equalsIgnoreCase(a.getNodeName()))
									{
										attrs = a.getAttributes();
										final List<Location> locs = new ArrayList<>();
										for (Node z = a.getFirstChild(); z != null; z = z.getNextSibling())
										{
											if ("loc".equalsIgnoreCase(z.getNodeName()))
											{
												attrs = z.getAttributes();
												locs.add(new Location(parseInteger(attrs, "x"), parseInteger(attrs, "y"), parseInteger(attrs, "z")));
											}
										}
										
										final VehiclePathPoint[] route = new VehiclePathPoint[locs.size()];
										int i = 0;
										for (Location loc : locs)
										{
											route[i++] = new VehiclePathPoint(loc);
										}
										data.addRoute(route);
									}
								}
							}
						}
						_shuttles.put(data.getId(), data);
					}
				}
			}
		}
	}
	
	/**
	 * Initializes all shuttles based on the data stored in the `_shuttles` map.<br>
	 * For each `ShuttleDataHolder`, this method creates a `Shuttle` instance, sets its location, heading, and speed properties and registers and starts its engine.
	 */
	private void init()
	{
		for (ShuttleDataHolder data : _shuttles.values())
		{
			final Shuttle shuttle = new Shuttle(new CreatureTemplate(new StatSet()));
			shuttle.setData(data);
			shuttle.setHeading(data.getLocation().getHeading());
			shuttle.setLocationInvisible(data.getLocation());
			shuttle.spawnMe();
			shuttle.getStat().setMoveSpeed(300);
			shuttle.getStat().setRotationSpeed(0);
			shuttle.registerEngine(new ShuttleEngine(data, shuttle));
			shuttle.runEngine(1000);
			_shuttleInstances.put(shuttle.getObjectId(), shuttle);
		}
	}
	
	/**
	 * Retrieves a `Shuttle` instance by its unique object ID or shuttle ID.
	 * @param id the ID to search for, which can be either the shuttle's object ID or its specific shuttle ID
	 * @return the `Shuttle` instance that matches the specified ID, or {@code null} if no such shuttle exists
	 */
	public Shuttle getShuttle(int id)
	{
		for (Shuttle shuttle : _shuttleInstances.values())
		{
			if ((shuttle.getObjectId() == id) || (shuttle.getId() == id))
			{
				return shuttle;
			}
		}
		return null;
	}
	
	public static ShuttleData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ShuttleData INSTANCE = new ShuttleData();
	}
}
