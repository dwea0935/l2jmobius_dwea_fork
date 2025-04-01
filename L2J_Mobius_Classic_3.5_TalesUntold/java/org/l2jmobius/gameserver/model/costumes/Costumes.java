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
package org.l2jmobius.gameserver.model.costumes;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.sql.CostumeTable;
import org.l2jmobius.gameserver.data.xml.CostumesCollectionData;
import org.l2jmobius.gameserver.data.xml.CostumesCollectionSkillsData;
import org.l2jmobius.gameserver.data.xml.CostumesData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.skill.AbnormalType;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.costume.ExSendCostumeListFull;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;

/**
 * @author GolbergSoft, Mobius, Liamxroy
 */
public class Costumes
{
	private static final Logger LOGGER = Logger.getLogger(Costumes.class.getName());
	
	private final Map<Integer, Costume> _costumes = new HashMap<>(159);
	private final EnumMap<CostumeGrade, Set<Costume>> _costumesGrade = new EnumMap<>(CostumeGrade.class);
	private final Map<Integer, CostumeCollection> _collections = new HashMap<>(12);
	private final Map<Integer, Set<Skill>> _stackedBonus = new HashMap<>(12);
	private final CostumesData _costumeParser;
	private final CostumesCollectionData _collectionParser;
	private final CostumesCollectionSkillsData _stackBonusParser;
	
	protected Costumes()
	{
		_costumeParser = new CostumesData(_costumes, _costumesGrade);
		_collectionParser = new CostumesCollectionData(getCollections());
		_stackBonusParser = new CostumesCollectionSkillsData(_stackedBonus);
		Containers.Global().addListener(new ConsumerEventListener(Containers.Players(), EventType.ON_PLAYER_LOGIN, (OnPlayerLogin event) -> onPlayLogin(event), this));
		load();
	}
	
	public void load()
	{
		_costumeParser.parseCostumeDocument();
		_collectionParser.parseCollectionDocument();
		_stackBonusParser.parseCollectionStackBonusDocument();
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _costumes.size() + " costumes.");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _costumesGrade.size() + " costumes grade.");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + getCollections().size() + " collections.");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _stackedBonus.size() + " collections skills.");
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayLogin(OnPlayerLogin event)
	{
		final Player player = event.getPlayer();
		processCollections(player);
		player.sendPacket(new ExSendCostumeListFull());
		checkStackedEffects(player, 0);
		
		final CostumeCollectionData activeCollection = player.getActiveCostumeCollection();
		if (activeCollection != null)
		{
			final Integer activeCollectionId = activeCollection.getId();
			final CostumeCollection collection = getCollections().get(activeCollectionId);
			if (collection != null)
			{
				player.addSkill(collection.skill(), false);
			}
		}
	}
	
	private void checkStackedEffects(Player player, int previousStack)
	{
		final int currentBonus = player.getCostumeCollectionAmount();
		if (currentBonus != previousStack)
		{
			final Set<Skill> previousSkills = _stackedBonus.get(previousStack);
			if (previousSkills != null)
			{
				previousSkills.forEach(skill -> player.removeSkill(skill, false));
			}
			
			final Set<Skill> currentSkills = _stackedBonus.get(currentBonus);
			if (currentSkills != null)
			{
				currentSkills.forEach(skill -> player.addSkill(skill, false));
			}
		}
	}
	
	public void processCollections(Player player)
	{
		final Map<Integer, CostumeTable> playerCostumes = loadPlayerCostumes(player);
		player.setCostumes(playerCostumes);
		
		boolean collectionAdded = false;
		for (CostumeCollection collection : getCollections().values())
		{
			if (hasAllCostumes(player, collection) && ((player.getActiveCostumeCollection() == null) || (player.getActiveCostumeCollection().getId() != collection.id())))
			{
				player.addCostumeCollection(collection.id());
				saveActiveCollection(player, collection);
				collectionAdded = true;
				break;
			}
		}
		
		if (!collectionAdded && (player.getActiveCostumeCollection() != null))
		{
			removeActiveCollection(player);
		}
	}
	
	private void saveActiveCollection(Player player, CostumeCollection collection)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("REPLACE INTO character_costume_collection (player_id, id) VALUES (?, ?)"))
		{
			ps.setInt(1, player.getObjectId());
			ps.setInt(2, collection.id());
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Failed to save active collection for player " + player.getObjectId(), e);
		}
	}
	
	private void removeActiveCollection(Player player)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM character_costume_collection WHERE player_id = ?"))
		{
			ps.setInt(1, player.getObjectId());
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Failed to remove active collection for player " + player.getObjectId(), e);
		}
	}
	
	private Map<Integer, CostumeTable> loadPlayerCostumes(Player player)
	{
		final Map<Integer, CostumeTable> costumes = new HashMap<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT id, amount, locked FROM character_costumes WHERE player_id = ?"))
		{
			ps.setInt(1, player.getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					CostumeTable costume = new CostumeTable();
					costume.setPlayerId(player.getObjectId());
					costume.setId(rs.getInt("id"));
					costume.setAmount(rs.getLong("amount"));
					costume.setLocked(rs.getBoolean("locked"));
					costume.setNew(false);
					costumes.put(costume.getId(), costume);
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Failed to load costumes for player " + player.getObjectId(), e);
		}
		return costumes;
	}
	
	public static Node findChildNodeByName(Node parentNode, String childName)
	{
		final NodeList children = parentNode.getChildNodes();
		for (int i = 0; i < children.getLength(); i++)
		{
			final Node child = children.item(i);
			if (child.getNodeName().equals(childName))
			{
				return child;
			}
		}
		return null;
	}
	
	public static Set<ItemHolder> parseExtractCost(Node extractNode)
	{
		final Set<ItemHolder> extractCost = new HashSet<>(extractNode.getChildNodes().getLength());
		final NodeList childNodes = extractNode.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++)
		{
			final Node costNode = childNodes.item(i);
			if ("cost".equalsIgnoreCase(costNode.getNodeName()))
			{
				final NamedNodeMap costAttrs = costNode.getAttributes();
				extractCost.add(new ItemHolder(parseInteger(costAttrs, "id"), parseLong(costAttrs, "count")));
			}
		}
		return extractCost;
	}
	
	public static Set<Integer> parseIntSet(Node node)
	{
		final Set<Integer> intSet = new HashSet<>();
		final NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++)
		{
			final Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE)
			{
				try
				{
					final int value = Integer.parseInt(child.getTextContent().trim());
					intSet.add(value);
				}
				catch (NumberFormatException e)
				{
					System.out.println("Failed to parse integer from node: " + child.getTextContent());
				}
			}
		}
		return intSet;
	}
	
	private static int parseInteger(NamedNodeMap attrs, String name)
	{
		return Integer.parseInt(attrs.getNamedItem(name).getNodeValue());
	}
	
	private static long parseLong(NamedNodeMap attrs, String name)
	{
		return Long.parseLong(attrs.getNamedItem(name).getNodeValue());
	}
	
	public Costume getCostume(int id)
	{
		return _costumes.get(id);
	}
	
	public Skill getCostumeSkill(int costumeId)
	{
		final Costume costume = getCostume(costumeId);
		if (costume != null)
		{
			return costume.skill();
		}
		return null; // Or however you want to handle the null case.
	}
	
	public void checkCostumeCollection(Player player, int id)
	{
		final int stackBonus = player.getCostumeCollectionAmount();
		getCollections().values().stream().filter(c -> c.costumes().contains(id)).forEach(c ->
		{
			if (hasAllCostumes(player, c))
			{
				player.addCostumeCollection(c.id());
			}
			else
			{
				player.removeCostumeCollection(c.id());
				if (player.getActiveCostumeCollection().getId() == c.id())
				{
					player.removeSkill(c.skill(), false);
				}
			}
		});
		checkStackedEffects(player, stackBonus);
	}
	
	public Costume getRandomCostume(EnumSet<CostumeGrade> grades)
	{
		if (grades.isEmpty())
		{
			return null;
		}
		
		// Collect all costumes from the specified grades into a single set
		final Set<Costume> available = _costumesGrade.entrySet().stream().filter(entry -> grades.contains(entry.getKey())).flatMap(entry -> entry.getValue().stream()).collect(Collectors.toSet());
		if (available.isEmpty())
		{
			return null;
		}
		
		// Choose a random costume from the set.
		final int index = Rnd.get(available.size());
		return available.stream().skip(index).findFirst().orElse(null);
	}
	
	public boolean hasAllCostumes(Player player, CostumeCollection costumeCollection)
	{
		return costumeCollection.costumes().stream().map(id -> player.getCostume((Integer) id)) // Assuming costumes contains Integer IDs and getCostume accepts an Integer.
			.allMatch(costume -> costume != null); // Check if all costumes are not null, indicating the player has all costumes.
	}
	
	public boolean checkCostumeAction(Player player)
	{
		if (player.getPrivateStoreType() != PrivateStoreType.NONE)
		{
			player.sendPacket(SystemMessageId.CANNOT_USE_SEALBOOKS_AND_EVOLVE_OR_EXTRACT_TRANSFORMATIONS_WHILE_USING_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP);
			return false;
		}
		else if (player.isDead())
		{
			player.sendPacket(SystemMessageId.CANNOT_USE_SEALBOOKS_AND_EVOLVE_OR_EXTRACT_TRANSFORMATIONS_WHEN_DEAD);
			return false;
		}
		else if (player.hasAbnormalType(AbnormalType.TURN_STONE))
		{
			player.sendPacket(SystemMessageId.CANNOT_USE_SEALBOOKS_AND_EVOLVE_OR_EXTRACT_TRANSFORMATIONS_WHILE_PETRIFIED);
			return false;
		}
		else if (player.isFishing())
		{
			player.sendPacket(SystemMessageId.CANNOT_USE_SEALBOOKS_AND_EVOLVE_OR_EXTRACT_TRANSFORMATIONS_WHILE_FISHING);
			return false;
		}
		else if (player.isSitting())
		{
			player.sendPacket(SystemMessageId.CANNOT_USE_SEALBOOKS_AND_EVOLVE_OR_EXTRACT_TRANSFORMATIONS_WHILE_SITTING);
			return false;
		}
		else if (player.isMovementDisabled())
		{
			player.sendPacket(SystemMessageId.CANNOT_USE_SEALBOOKS_AND_EVOLVE_OR_EXTRACT_TRANSFORMATIONS_WHILE_FROZEN);
			return false;
		}
		else if (player.isProcessingTransaction())
		{
			player.sendPacket(SystemMessageId.CANNOT_USE_SEALBOOKS_AND_EVOLVE_OR_EXTRACT_TRANSFORMATIONS_DURING_EXCHANGE);
			return false;
		}
		else if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(player))
		{
			player.sendPacket(SystemMessageId.CANNOT_USE_SEALBOOKS_AND_EVOLVE_OR_EXTRACT_TRANSFORMATIONS_DURING_A_BATTLE);
			return false;
		}
		return true;
	}
	
	public Map<Integer, CostumeCollection> getCollections()
	{
		return _collections;
	}
	
	public static Costumes getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static final class SingletonHolder
	{
		private static final Costumes INSTANCE = new Costumes();
	}
}
