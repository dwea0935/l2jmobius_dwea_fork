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
package org.l2jmobius.gameserver.model.quest;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.sql.AnnouncementsTable;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.managers.EventDropManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.npc.EventDropHolder;
import org.l2jmobius.gameserver.model.announce.EventAnnouncement;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.OnServerStart;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.script.DateRange;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * Parent class for long time events.<br>
 * Maintains config reading, spawn of NPCs, adding of event's drop.
 * @author GKR, Mobius
 */
public class LongTimeEvent extends Quest
{
	protected String _eventName;
	protected DateRange _eventPeriod = null;
	protected boolean _initialized = false;
	protected boolean _active = false;
	
	// Messages
	protected String _onEnterMsg = "";
	protected String _endMsg = "";
	protected int _enterAnnounceId = -1;
	
	// NPCs to spawn and their spawn points
	protected final List<NpcSpawn> _spawnList = new ArrayList<>();
	
	// Drop data for event
	protected final List<EventDropHolder> _dropList = new ArrayList<>();
	
	// Items to destroy when event ends
	protected final List<Integer> _destroyItemsOnEnd = new ArrayList<>();
	
	protected class NpcSpawn
	{
		protected final int npcId;
		protected final Location loc;
		protected final int respawnTime;
		
		protected NpcSpawn(int spawnNpcId, Location spawnLoc, int spawnRespawnTime)
		{
			npcId = spawnNpcId;
			loc = spawnLoc;
			respawnTime = spawnRespawnTime;
		}
	}
	
	public LongTimeEvent()
	{
		super(-1, "");
		loadConfig();
		
		if (_eventPeriod != null)
		{
			if (_eventPeriod.isWithinRange(new Date()))
			{
				startEvent();
				LOGGER.info("Event " + _eventName + " active till " + _eventPeriod.getEndDate());
			}
			else if (_eventPeriod.getStartDate().after(new Date()))
			{
				final long delay = _eventPeriod.getStartDate().getTime() - System.currentTimeMillis();
				ThreadPool.schedule(new ScheduleStart(), delay);
				LOGGER.info("Event " + _eventName + " will be started at " + _eventPeriod.getStartDate());
			}
			else
			{
				// Destroy items that must exist only on event period.
				destroyItemsOnEnd();
				LOGGER.info("Event " + _eventName + " has passed... Ignored ");
			}
		}
		
		_initialized = true;
	}
	
	/**
	 * Load event configuration file
	 */
	private void loadConfig()
	{
		final File configFile = new File("data/scripts/events/" + getName() + "/config.xml");
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			final DocumentBuilder builder = factory.newDocumentBuilder();
			final Document document = builder.parse(configFile);
			if (!document.getDocumentElement().getNodeName().equalsIgnoreCase("event"))
			{
				throw new NullPointerException("WARNING!!! " + getScriptName() + " event: bad config file!");
			}
			_eventName = document.getDocumentElement().getAttributes().getNamedItem("name").getNodeValue();
			final String currentYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
			final String period = document.getDocumentElement().getAttributes().getNamedItem("active").getNodeValue();
			if (period.length() == 21)
			{
				// dd MM yyyy-dd MM yyyy
				_eventPeriod = DateRange.parse(period, new SimpleDateFormat("dd MM yyyy", Locale.US));
			}
			else if (period.length() == 11)
			{
				// dd MM-dd MM
				final String start = period.split("-")[0].concat(" ").concat(currentYear);
				final String end = period.split("-")[1].concat(" ").concat(currentYear);
				final String activePeriod = start.concat("-").concat(end);
				_eventPeriod = DateRange.parse(activePeriod, new SimpleDateFormat("dd MM yyyy", Locale.US));
			}
			
			if (_eventPeriod == null)
			{
				throw new NullPointerException("WARNING!!! " + getName() + " event: illegal event period");
			}
			
			final Date today = new Date();
			
			if (_eventPeriod.getStartDate().after(today) || _eventPeriod.isWithinRange(today))
			{
				for (Node n = document.getDocumentElement().getFirstChild(); n != null; n = n.getNextSibling())
				{
					// Loading droplist
					if (n.getNodeName().equalsIgnoreCase("droplist"))
					{
						for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if (d.getNodeName().equalsIgnoreCase("add"))
							{
								try
								{
									final int itemId = Integer.parseInt(d.getAttributes().getNamedItem("item").getNodeValue());
									final int minCount = Integer.parseInt(d.getAttributes().getNamedItem("min").getNodeValue());
									final int maxCount = Integer.parseInt(d.getAttributes().getNamedItem("max").getNodeValue());
									final String chance = d.getAttributes().getNamedItem("chance").getNodeValue();
									final double finalChance = !chance.isEmpty() && chance.endsWith("%") ? Double.parseDouble(chance.substring(0, chance.length() - 1)) : 0;
									final Node minLevelNode = d.getAttributes().getNamedItem("minLevel");
									final int minLevel = minLevelNode == null ? 1 : Integer.parseInt(minLevelNode.getNodeValue());
									final Node maxLevelNode = d.getAttributes().getNamedItem("maxLevel");
									final int maxLevel = maxLevelNode == null ? Integer.MAX_VALUE : Integer.parseInt(maxLevelNode.getNodeValue());
									final Node monsterIdsNode = d.getAttributes().getNamedItem("monsterIds");
									final Set<Integer> monsterIds = new HashSet<>();
									if (monsterIdsNode != null)
									{
										for (String id : monsterIdsNode.getNodeValue().split(","))
										{
											monsterIds.add(Integer.parseInt(id));
										}
									}
									
									if (ItemData.getInstance().getTemplate(itemId) == null)
									{
										LOGGER.warning(getName() + " event: " + itemId + " is wrong item id, item was not added in droplist");
										continue;
									}
									
									if (minCount > maxCount)
									{
										LOGGER.warning(getName() + " event: item " + itemId + " - min greater than max, item was not added in droplist");
										continue;
									}
									
									if ((finalChance < 0) || (finalChance > 100))
									{
										LOGGER.warning(getName() + " event: item " + itemId + " - incorrect drop chance, item was not added in droplist");
										continue;
									}
									
									_dropList.add(new EventDropHolder(itemId, minCount, maxCount, finalChance, minLevel, maxLevel, monsterIds));
								}
								catch (NumberFormatException nfe)
								{
									LOGGER.warning("Wrong number format in config.xml droplist block for " + getName() + " event");
								}
							}
						}
					}
					else if (n.getNodeName().equalsIgnoreCase("spawnlist"))
					{
						// Loading spawnlist
						for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if (d.getNodeName().equalsIgnoreCase("add"))
							{
								try
								{
									final int npcId = Integer.parseInt(d.getAttributes().getNamedItem("npc").getNodeValue());
									final int xPos = Integer.parseInt(d.getAttributes().getNamedItem("x").getNodeValue());
									final int yPos = Integer.parseInt(d.getAttributes().getNamedItem("y").getNodeValue());
									final int zPos = Integer.parseInt(d.getAttributes().getNamedItem("z").getNodeValue());
									final Node headingNode = d.getAttributes().getNamedItem("heading");
									final String headingValue = headingNode == null ? null : headingNode.getNodeValue();
									final int heading = headingValue != null ? Integer.parseInt(headingValue) : 0;
									final Node respawnTimeNode = d.getAttributes().getNamedItem("respawnTime");
									final String respawnTimeValue = respawnTimeNode == null ? null : respawnTimeNode.getNodeValue();
									final int respawnTime = respawnTimeValue != null ? Integer.parseInt(respawnTimeValue) : 0;
									
									if (NpcData.getInstance().getTemplate(npcId) == null)
									{
										LOGGER.warning(getName() + " event: " + npcId + " is wrong NPC id, NPC was not added in spawnlist");
										continue;
									}
									
									_spawnList.add(new NpcSpawn(npcId, new Location(xPos, yPos, zPos, heading), respawnTime * 1000));
								}
								catch (NumberFormatException nfe)
								{
									LOGGER.warning("Wrong number format in config.xml spawnlist block for " + getName() + " event");
								}
							}
						}
					}
					else if (n.getNodeName().equalsIgnoreCase("messages"))
					{
						// Loading Messages
						for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if (d.getNodeName().equalsIgnoreCase("add"))
							{
								final String msgType = d.getAttributes().getNamedItem("type").getNodeValue();
								final String msgText = d.getAttributes().getNamedItem("text").getNodeValue();
								if ((msgType != null) && (msgText != null))
								{
									if (msgType.equalsIgnoreCase("onEnd"))
									{
										_endMsg = msgText;
									}
									else if (msgType.equalsIgnoreCase("onEnter"))
									{
										_onEnterMsg = msgText;
									}
								}
							}
						}
					}
				}
			}
			
			// Load destroy item list at all times.
			for (Node n = document.getDocumentElement().getFirstChild(); n != null; n = n.getNextSibling())
			{
				if (n.getNodeName().equalsIgnoreCase("destroyItemsOnEnd"))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if (d.getNodeName().equalsIgnoreCase("item"))
						{
							try
							{
								final int itemId = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
								if (ItemData.getInstance().getTemplate(itemId) == null)
								{
									LOGGER.warning(getScriptName() + " event: Item " + itemId + " does not exist.");
									continue;
								}
								_destroyItemsOnEnd.add(itemId);
							}
							catch (NumberFormatException nfe)
							{
								LOGGER.warning("Wrong number format in config.xml destroyItemsOnEnd block for " + getScriptName() + " event");
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getName() + " event: error reading " + configFile.getAbsolutePath() + " ! " + e.getMessage(), e);
		}
	}
	
	protected class ScheduleStart implements Runnable
	{
		@Override
		public void run()
		{
			startEvent();
		}
	}
	
	protected void startEvent()
	{
		// Set Active.
		_active = true;
		
		// Add event drops.
		EventDropManager.getInstance().addDrops(this, _dropList);
		
		if (!_spawnList.isEmpty())
		{
			if (_initialized)
			{
				// Add spawns on event start.
				spawnNpcs();
			}
			else // Add spawns on server start.
			{
				Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_SERVER_START, _spawnNpcs, this));
			}
		}
		
		// Event enter announcement.
		if (!_onEnterMsg.isEmpty())
		{
			// Send message on begin.
			Broadcast.toAllOnlinePlayers(_onEnterMsg);
			
			// Add announce for entering players.
			final EventAnnouncement announce = new EventAnnouncement(_eventPeriod, _onEnterMsg);
			AnnouncementsTable.getInstance().addAnnouncement(announce);
			_enterAnnounceId = announce.getId();
		}
		
		// Schedule event end.
		final Long millisToEventEnd = _eventPeriod.getEndDate().getTime() - System.currentTimeMillis();
		ThreadPool.schedule(new ScheduleEnd(), millisToEventEnd);
	}
	
	/**
	 * Event spawns must initialize after server loads scripts.
	 */
	private final Consumer<OnServerStart> _spawnNpcs = _ ->
	{
		spawnNpcs();
		Containers.Global().removeListenerIf(EventType.ON_SERVER_START, listener -> listener.getOwner() == this);
	};
	
	protected void spawnNpcs()
	{
		final Long millisToEventEnd = _eventPeriod.getEndDate().getTime() - System.currentTimeMillis();
		for (NpcSpawn npcSpawn : _spawnList)
		{
			final Npc npc = addSpawn(npcSpawn.npcId, npcSpawn.loc.getX(), npcSpawn.loc.getY(), npcSpawn.loc.getZ(), npcSpawn.loc.getHeading(), false, millisToEventEnd, false);
			if (npcSpawn.respawnTime > 0)
			{
				final Spawn spawn = npc.getSpawn();
				spawn.setRespawnDelay(npcSpawn.respawnTime);
				spawn.startRespawn();
				ThreadPool.schedule(spawn::stopRespawn, millisToEventEnd - npcSpawn.respawnTime);
			}
		}
	}
	
	protected class ScheduleEnd implements Runnable
	{
		@Override
		public void run()
		{
			stopEvent();
		}
	}
	
	protected void stopEvent()
	{
		// Set Active.
		_active = false;
		
		// Stop event drops.
		EventDropManager.getInstance().removeDrops(this);
		
		// Destroy items that must exist only on event period.
		destroyItemsOnEnd();
		
		// Send message on end.
		if (!_endMsg.isEmpty())
		{
			Broadcast.toAllOnlinePlayers(_endMsg);
		}
		
		// Remove announce for entering players.
		if (_enterAnnounceId != -1)
		{
			AnnouncementsTable.getInstance().deleteAnnouncement(_enterAnnounceId);
		}
	}
	
	protected void destroyItemsOnEnd()
	{
		if (!_destroyItemsOnEnd.isEmpty())
		{
			for (int itemId : _destroyItemsOnEnd)
			{
				// Remove item from online players.
				for (Player player : World.getInstance().getPlayers())
				{
					if (player != null)
					{
						player.destroyItemByItemId(ItemProcessType.DESTROY, itemId, -1, player, true);
					}
				}
				// Update database.
				try (Connection con = DatabaseFactory.getConnection();
					PreparedStatement statement = con.prepareStatement("DELETE FROM items WHERE item_id=?"))
				{
					statement.setInt(1, itemId);
					statement.execute();
				}
				catch (SQLException e)
				{
					LOGGER.warning(e.toString());
				}
			}
		}
	}
	
	public DateRange getEventPeriod()
	{
		return _eventPeriod;
	}
	
	/**
	 * @return {@code true} if now is event period
	 */
	public boolean isEventPeriod()
	{
		return _active;
	}
}
