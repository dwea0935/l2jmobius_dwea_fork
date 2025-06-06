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
package org.l2jmobius.gameserver.managers;

import java.io.File;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.Config;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.InstanceReenterTimeHolder;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.data.xml.SpawnData;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.npc.MinionHolder;
import org.l2jmobius.gameserver.model.actor.templates.DoorTemplate;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.instancezone.InstanceReenterType;
import org.l2jmobius.gameserver.model.instancezone.InstanceRemoveBuffType;
import org.l2jmobius.gameserver.model.instancezone.InstanceTeleportType;
import org.l2jmobius.gameserver.model.instancezone.InstanceTemplate;
import org.l2jmobius.gameserver.model.instancezone.conditions.Condition;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.spawns.SpawnTemplate;

/**
 * Instance manager.
 * @author evill33t, GodKratos, malyelfik
 */
public class InstanceManager implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(InstanceManager.class.getName());
	// Database query
	private static final String DELETE_INSTANCE_TIME = "DELETE FROM character_instance_time WHERE charId=? AND instanceId=?";
	
	// Client instance names
	private final Map<Integer, String> _instanceNames = new HashMap<>();
	// Instance templates holder
	private final Map<Integer, InstanceTemplate> _instanceTemplates = new ConcurrentHashMap<>();
	// Created instance worlds
	private int _currentInstanceId = 0;
	private final Map<Integer, Instance> _instanceWorlds = new ConcurrentHashMap<>();
	// Player reenter times
	private final Map<Integer, Map<Integer, Long>> _playerTimes = new ConcurrentHashMap<>();
	
	protected InstanceManager()
	{
		load();
	}
	
	// --------------------------------------------------------------------
	// Instance data loader
	// --------------------------------------------------------------------
	@Override
	public void load()
	{
		// Load instance names
		_instanceNames.clear();
		parseDatapackFile("data/InstanceNames.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _instanceNames.size() + " instance names.");
		// Load instance templates
		_instanceTemplates.clear();
		parseDatapackDirectory("data/instances", true);
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _instanceTemplates.size() + " instance templates.");
		// Load player's reenter data
		_playerTimes.clear();
		restoreInstanceTimes();
		LOGGER.info(getClass().getSimpleName() + ": Loaded instance reenter times for " + _playerTimes.size() + " players.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, IXmlReader::isNode, listNode ->
		{
			switch (listNode.getNodeName())
			{
				case "list":
				{
					parseInstanceName(listNode);
					break;
				}
				case "instance":
				{
					parseInstanceTemplate(listNode, file);
					break;
				}
			}
		});
	}
	
	/**
	 * Read instance names from XML file.
	 * @param n starting XML tag
	 */
	private void parseInstanceName(Node n)
	{
		forEach(n, "instance", instanceNode ->
		{
			final NamedNodeMap attrs = instanceNode.getAttributes();
			_instanceNames.put(parseInteger(attrs, "id"), parseString(attrs, "name"));
		});
	}
	
	/**
	 * Parse instance template from XML file.
	 * @param instanceNode start XML tag
	 * @param file currently parsed file
	 */
	private void parseInstanceTemplate(Node instanceNode, File file)
	{
		// Parse "instance" node
		final int id = parseInteger(instanceNode.getAttributes(), "id");
		if (_instanceTemplates.containsKey(id))
		{
			LOGGER.warning(getClass().getSimpleName() + ": Instance template with ID " + id + " already exists");
			return;
		}
		
		final InstanceTemplate template = new InstanceTemplate(new StatSet(parseAttributes(instanceNode)));
		
		// Update name if wasn't provided
		if (template.getName() == null)
		{
			template.setName(_instanceNames.get(id));
		}
		
		// Parse "instance" node children
		forEach(instanceNode, IXmlReader::isNode, innerNode ->
		{
			switch (innerNode.getNodeName())
			{
				case "time":
				{
					final NamedNodeMap attrs = innerNode.getAttributes();
					template.setDuration(parseInteger(attrs, "duration", -1));
					template.setEmptyDestroyTime(parseInteger(attrs, "empty", -1));
					template.setEjectTime(parseInteger(attrs, "eject", -1));
					break;
				}
				case "misc":
				{
					final NamedNodeMap attrs = innerNode.getAttributes();
					template.allowPlayerSummon(parseBoolean(attrs, "allowPlayerSummon", false));
					template.setPvP(parseBoolean(attrs, "isPvP", false));
					break;
				}
				case "rates":
				{
					final NamedNodeMap attrs = innerNode.getAttributes();
					template.setExpRate(parseFloat(attrs, "exp", Config.RATE_INSTANCE_XP));
					template.setSPRate(parseFloat(attrs, "sp", Config.RATE_INSTANCE_SP));
					template.setExpPartyRate(parseFloat(attrs, "partyExp", Config.RATE_INSTANCE_PARTY_XP));
					template.setSPPartyRate(parseFloat(attrs, "partySp", Config.RATE_INSTANCE_PARTY_SP));
					break;
				}
				case "locations":
				{
					forEach(innerNode, IXmlReader::isNode, locationsNode ->
					{
						switch (locationsNode.getNodeName())
						{
							case "enter":
							{
								final InstanceTeleportType type = parseEnum(locationsNode.getAttributes(), InstanceTeleportType.class, "type");
								final List<Location> locations = new ArrayList<>();
								forEach(locationsNode, "location", locationNode -> locations.add(parseLocation(locationNode)));
								template.setEnterLocation(type, locations);
								break;
							}
							case "exit":
							{
								final InstanceTeleportType type = parseEnum(locationsNode.getAttributes(), InstanceTeleportType.class, "type");
								if (type.equals(InstanceTeleportType.ORIGIN))
								{
									template.setExitLocation(type, null);
								}
								else
								{
									final List<Location> locations = new ArrayList<>();
									forEach(locationsNode, "location", locationNode -> locations.add(parseLocation(locationNode)));
									if (locations.isEmpty())
									{
										LOGGER.warning(getClass().getSimpleName() + ": Missing exit location data for instance " + template.getName() + " (" + template.getId() + ")!");
									}
									else
									{
										template.setExitLocation(type, locations);
									}
								}
								break;
							}
						}
					});
					break;
				}
				case "spawnlist":
				{
					final List<SpawnTemplate> spawns = new ArrayList<>();
					SpawnData.getInstance().parseSpawn(innerNode, file, spawns);
					template.addSpawns(spawns);
					break;
				}
				case "doorlist":
				{
					for (Node doorNode = innerNode.getFirstChild(); doorNode != null; doorNode = doorNode.getNextSibling())
					{
						if (doorNode.getNodeName().equals("door"))
						{
							final StatSet parsedSet = DoorData.getInstance().parseDoor(doorNode);
							final StatSet mergedSet = new StatSet();
							final int doorId = parsedSet.getInt("id");
							final StatSet templateSet = DoorData.getInstance().getDoorTemplate(doorId);
							if (templateSet != null)
							{
								mergedSet.merge(templateSet);
							}
							else
							{
								LOGGER.warning(getClass().getSimpleName() + ": Cannot find template for door: " + doorId + ", instance: " + template.getName() + " (" + template.getId() + ")");
							}
							mergedSet.merge(parsedSet);
							
							try
							{
								template.addDoor(doorId, new DoorTemplate(mergedSet));
							}
							catch (Exception e)
							{
								LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Cannot initialize template for door: " + doorId + ", instance: " + template.getName() + " (" + template.getId() + ")", e);
							}
						}
					}
					break;
				}
				case "removeBuffs":
				{
					final InstanceRemoveBuffType removeBuffType = parseEnum(innerNode.getAttributes(), InstanceRemoveBuffType.class, "type");
					final List<Integer> exceptionBuffList = new ArrayList<>();
					for (Node e = innerNode.getFirstChild(); e != null; e = e.getNextSibling())
					{
						if (e.getNodeName().equals("skill"))
						{
							exceptionBuffList.add(parseInteger(e.getAttributes(), "id"));
						}
					}
					template.setRemoveBuff(removeBuffType, exceptionBuffList);
					break;
				}
				case "reenter":
				{
					final InstanceReenterType type = parseEnum(innerNode.getAttributes(), InstanceReenterType.class, "apply", InstanceReenterType.NONE);
					final List<InstanceReenterTimeHolder> data = new ArrayList<>();
					for (Node e = innerNode.getFirstChild(); e != null; e = e.getNextSibling())
					{
						if (e.getNodeName().equals("reset"))
						{
							final NamedNodeMap attrs = e.getAttributes();
							final int time = parseInteger(attrs, "time", -1);
							if (time > 0)
							{
								data.add(new InstanceReenterTimeHolder(time));
							}
							else
							{
								final DayOfWeek day = parseEnum(attrs, DayOfWeek.class, "day");
								final int hour = parseInteger(attrs, "hour", -1);
								final int minute = parseInteger(attrs, "minute", -1);
								data.add(new InstanceReenterTimeHolder(day, hour, minute));
							}
						}
					}
					template.setReenterData(type, data);
					break;
				}
				case "parameters":
				{
					final Map<String, Object> parameters = new HashMap<>();
					for (Node parameterNode = innerNode.getFirstChild(); parameterNode != null; parameterNode = parameterNode.getNextSibling())
					{
						NamedNodeMap attributes = parameterNode.getAttributes();
						switch (parameterNode.getNodeName().toLowerCase())
						{
							case "param":
							{
								parameters.put(parseString(attributes, "name"), parseString(attributes, "value"));
								break;
							}
							case "skill":
							{
								parameters.put(parseString(attributes, "name"), new SkillHolder(parseInteger(attributes, "id"), parseInteger(attributes, "level")));
								break;
							}
							case "location":
							{
								parameters.put(parseString(attributes, "name"), new Location(parseInteger(attributes, "x"), parseInteger(attributes, "y"), parseInteger(attributes, "z"), parseInteger(attributes, "heading", 0)));
								break;
							}
							case "minions":
							{
								final List<MinionHolder> minions = new ArrayList<>(1);
								for (Node minionNode = parameterNode.getFirstChild(); minionNode != null; minionNode = minionNode.getNextSibling())
								{
									if (minionNode.getNodeName().equalsIgnoreCase("npc"))
									{
										attributes = minionNode.getAttributes();
										minions.add(new MinionHolder(parseInteger(attributes, "id"), parseInteger(attributes, "count"), parseInteger(attributes, "max", 0), parseInteger(attributes, "respawnTime"), parseInteger(attributes, "weightPoint", 0)));
									}
								}
								
								if (!minions.isEmpty())
								{
									parameters.put(parseString(parameterNode.getAttributes(), "name"), minions);
								}
								break;
							}
						}
					}
					template.setParameters(parameters);
					break;
				}
				case "conditions":
				{
					final List<Condition> conditions = new ArrayList<>();
					for (Node conditionNode = innerNode.getFirstChild(); conditionNode != null; conditionNode = conditionNode.getNextSibling())
					{
						if (conditionNode.getNodeName().equals("condition"))
						{
							final NamedNodeMap attrs = conditionNode.getAttributes();
							final String type = parseString(attrs, "type");
							final boolean onlyLeader = parseBoolean(attrs, "onlyLeader", false);
							final boolean showMessageAndHtml = parseBoolean(attrs, "showMessageAndHtml", false);
							// Load parameters
							StatSet params = null;
							for (Node f = conditionNode.getFirstChild(); f != null; f = f.getNextSibling())
							{
								if (f.getNodeName().equals("param"))
								{
									if (params == null)
									{
										params = new StatSet();
									}
									
									params.set(parseString(f.getAttributes(), "name"), parseString(f.getAttributes(), "value"));
								}
							}
							
							// If none parameters found then set empty StatSet
							if (params == null)
							{
								params = StatSet.EMPTY_STATSET;
							}
							
							// Now when everything is loaded register condition to template
							try
							{
								final Class<?> clazz = Class.forName("org.l2jmobius.gameserver.model.instancezone.conditions.Condition" + type);
								final Constructor<?> constructor = clazz.getConstructor(InstanceTemplate.class, StatSet.class, boolean.class, boolean.class);
								conditions.add((Condition) constructor.newInstance(template, params, onlyLeader, showMessageAndHtml));
							}
							catch (Exception ex)
							{
								LOGGER.warning(getClass().getSimpleName() + ": Unknown condition type " + type + " for instance " + template.getName() + " (" + id + ")!");
							}
						}
					}
					template.setConditions(conditions);
					break;
				}
			}
		});
		
		// Save template
		_instanceTemplates.put(id, template);
	}
	
	/**
	 * Parses a Location object from the given node.
	 * @param node the XML node to parse
	 * @return a Location object with x, y, z, and heading values, or default values if attributes are missing
	 */
	private Location parseLocation(Node node)
	{
		final NamedNodeMap attributes = node.getAttributes();
		final int x = parseInteger(attributes, "x");
		final int y = parseInteger(attributes, "y");
		final int z = parseInteger(attributes, "z");
		final int heading = parseInteger(attributes, "heading", 0);
		return new Location(x, y, z, heading);
	}
	
	/**
	 * Create new instance with default template.
	 * @return newly created default instance.
	 */
	public Instance createInstance()
	{
		return new Instance(getNewInstanceId(), new InstanceTemplate(StatSet.EMPTY_STATSET), null);
	}
	
	/**
	 * Create new instance from given template.
	 * @param template template used for instance creation
	 * @param player player who create instance.
	 * @return newly created instance if success, otherwise {@code null}
	 */
	public Instance createInstance(InstanceTemplate template, Player player)
	{
		return (template != null) ? new Instance(getNewInstanceId(), template, player) : null;
	}
	
	/**
	 * Create new instance with template defined in datapack.
	 * @param id template id of instance
	 * @param player player who create instance
	 * @return newly created instance if template was found, otherwise {@code null}
	 */
	public Instance createInstance(int id, Player player)
	{
		if (!_instanceTemplates.containsKey(id))
		{
			LOGGER.warning(getClass().getSimpleName() + ": Missing template for instance with id " + id + "!");
			return null;
		}
		return new Instance(getNewInstanceId(), _instanceTemplates.get(id), player);
	}
	
	/**
	 * Get instance world with given ID.
	 * @param instanceId ID of instance
	 * @return instance itself if found, otherwise {@code null}
	 */
	public Instance getInstance(int instanceId)
	{
		return _instanceWorlds.get(instanceId);
	}
	
	/**
	 * Get all active instances.
	 * @return Collection of all instances
	 */
	public Collection<Instance> getInstances()
	{
		return _instanceWorlds.values();
	}
	
	/**
	 * Get instance world for player.
	 * @param player player who wants to get instance world
	 * @param isInside when {@code true} find world where player is currently located, otherwise find world where player can enter
	 * @return instance if found, otherwise {@code null}
	 */
	public Instance getPlayerInstance(Player player, boolean isInside)
	{
		for (Instance instance : _instanceWorlds.values())
		{
			if (isInside)
			{
				if (instance.containsPlayer(player))
				{
					return instance;
				}
			}
			else
			{
				if (instance.isAllowed(player))
				{
					return instance;
				}
			}
		}
		return null;
	}
	
	/**
	 * Get ID for newly created instance.
	 * @return instance id
	 */
	private synchronized int getNewInstanceId()
	{
		do
		{
			if (_currentInstanceId == Integer.MAX_VALUE)
			{
				_currentInstanceId = 0;
			}
			_currentInstanceId++;
		}
		while (_instanceWorlds.containsKey(_currentInstanceId));
		return _currentInstanceId;
	}
	
	/**
	 * Register instance world.
	 * @param instance instance which should be registered
	 */
	public void register(Instance instance)
	{
		final int instanceId = instance.getId();
		if (!_instanceWorlds.containsKey(instanceId))
		{
			_instanceWorlds.put(instanceId, instance);
		}
	}
	
	/**
	 * Unregister instance world.<br>
	 * <b><font color=red>To remove instance world properly use {@link Instance#destroy()}.</font></b>
	 * @param instanceId ID of instance to unregister
	 */
	public void unregister(int instanceId)
	{
		if (_instanceWorlds.containsKey(instanceId))
		{
			_instanceWorlds.remove(instanceId);
		}
	}
	
	/**
	 * Get instance name from file "InstanceNames.xml"
	 * @param templateId template ID of instance
	 * @return name of instance if found, otherwise {@code null}
	 */
	public String getInstanceName(int templateId)
	{
		return _instanceNames.get(templateId);
	}
	
	/**
	 * Restore instance reenter data for all players.
	 */
	private void restoreInstanceTimes()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement ps = con.createStatement();
			ResultSet rs = ps.executeQuery("SELECT * FROM character_instance_time ORDER BY charId"))
		{
			while (rs.next())
			{
				// Check if instance penalty passed
				final long time = rs.getLong("time");
				if (time > System.currentTimeMillis())
				{
					// Load params
					final int charId = rs.getInt("charId");
					final int instanceId = rs.getInt("instanceId");
					// Set penalty
					setReenterPenalty(charId, instanceId, time);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Cannot restore players instance reenter data: ", e);
		}
	}
	
	/**
	 * Get all instance re-enter times for specified player.<br>
	 * This method also removes the penalties that have already expired.
	 * @param player instance of player who wants to get re-enter data
	 * @return map in form templateId, penaltyEndTime
	 */
	public Map<Integer, Long> getAllInstanceTimes(Player player)
	{
		// When player don't have any instance penalty
		final Map<Integer, Long> instanceTimes = _playerTimes.get(player.getObjectId());
		if ((instanceTimes == null) || instanceTimes.isEmpty())
		{
			return Collections.emptyMap();
		}
		
		// Find passed penalty
		final List<Integer> invalidPenalty = new ArrayList<>(instanceTimes.size());
		for (Entry<Integer, Long> entry : instanceTimes.entrySet())
		{
			if (entry.getValue() <= System.currentTimeMillis())
			{
				invalidPenalty.add(entry.getKey());
			}
		}
		
		// Remove them
		if (!invalidPenalty.isEmpty())
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(DELETE_INSTANCE_TIME))
			{
				for (Integer id : invalidPenalty)
				{
					ps.setInt(1, player.getObjectId());
					ps.setInt(2, id);
					ps.addBatch();
				}
				ps.executeBatch();
				invalidPenalty.forEach(instanceTimes::remove);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Cannot delete instance character reenter data: ", e);
			}
		}
		return instanceTimes;
	}
	
	/**
	 * Set re-enter penalty for specified player.<br>
	 * <font color=red><b>This method store penalty into memory only. Use {@link Instance#setReenterTime} to set instance penalty properly.</b></font>
	 * @param objectId object ID of player
	 * @param id instance template id
	 * @param time penalty time
	 */
	public void setReenterPenalty(int objectId, int id, long time)
	{
		_playerTimes.computeIfAbsent(objectId, _ -> new ConcurrentHashMap<>()).put(id, time);
	}
	
	/**
	 * Get re-enter time to instance (by template ID) for player.<br>
	 * This method also removes penalty if expired.
	 * @param player player who wants to get re-enter time
	 * @param id template ID of instance
	 * @return penalty end time if penalty is found, otherwise -1
	 */
	public long getInstanceTime(Player player, int id)
	{
		// Check if exists reenter data for player
		final Map<Integer, Long> playerData = _playerTimes.get(player.getObjectId());
		if ((playerData == null) || !playerData.containsKey(id))
		{
			return -1;
		}
		
		// If reenter time is higher then current, delete it
		final long time = playerData.get(id);
		if (time <= System.currentTimeMillis())
		{
			deleteInstanceTime(player, id);
			return -1;
		}
		return time;
	}
	
	/**
	 * Remove re-enter penalty for specified instance from player.
	 * @param player player who wants to delete penalty
	 * @param id template id of instance world
	 */
	public void deleteInstanceTime(Player player, int id)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_INSTANCE_TIME))
		{
			ps.setInt(1, player.getObjectId());
			ps.setInt(2, id);
			ps.execute();
			if (_playerTimes.get(player.getObjectId()) != null)
			{
				_playerTimes.get(player.getObjectId()).remove(id);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not delete character instance reenter data: ", e);
		}
	}
	
	/**
	 * Get instance template by template ID.
	 * @param id template id of instance
	 * @return instance template if found, otherwise {@code null}
	 */
	public InstanceTemplate getInstanceTemplate(int id)
	{
		return _instanceTemplates.get(id);
	}
	
	/**
	 * Get all instances template.
	 * @return Collection of all instance templates
	 */
	public Collection<InstanceTemplate> getInstanceTemplates()
	{
		return _instanceTemplates.values();
	}
	
	/**
	 * Get count of created instance worlds with same template ID.
	 * @param templateId template id of instance
	 * @return count of created instances
	 */
	public long getWorldCount(int templateId)
	{
		long count = 0;
		for (Instance i : _instanceWorlds.values())
		{
			if (i.getTemplateId() == templateId)
			{
				count++;
			}
		}
		return count;
	}
	
	/**
	 * Gets the single instance of {@code InstanceManager}.
	 * @return single instance of {@code InstanceManager}
	 */
	public static InstanceManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final InstanceManager INSTANCE = new InstanceManager();
	}
}
