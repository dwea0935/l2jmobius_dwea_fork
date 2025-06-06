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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.SkillLearn;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.actor.enums.player.SocialClass;
import org.l2jmobius.gameserver.model.actor.enums.player.SubclassType;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.holders.ItemSkillHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.CommonSkill;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.AcquireSkillType;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

/**
 * This class loads and manage the characters and pledges skills trees.<br>
 * Here can be found the following skill trees:<br>
 * <ul>
 * <li>Class skill trees: player skill trees for each class.</li>
 * <li>Transfer skill trees: player skill trees for each healer class.</li>
 * <li>Collect skill tree: player skill tree for Gracia related skills.</li>
 * <li>Fishing skill tree: player skill tree for fishing related skills.</li>
 * <li>Transform skill tree: player skill tree for transformation related skills.</li>
 * <li>Sub-Class skill tree: player skill tree for sub-class related skills.</li>
 * <li>Noble skill tree: player skill tree for noblesse related skills.</li>
 * <li>Hero skill tree: player skill tree for heroes related skills.</li>
 * <li>GM skill tree: player skill tree for Game Master related skills.</li>
 * <li>Common skill tree: custom skill tree for players, skills in this skill tree will be available for all players.</li>
 * <li>Pledge skill tree: clan skill tree for main clan.</li>
 * <li>Sub-Pledge skill tree: clan skill tree for sub-clans.</li>
 * </ul>
 * For easy customization of player class skill trees, the parent Id of each class is taken from the XML data, this means you can use a different class parent Id than in the normal game play, for example all 3rd class dagger users will have Treasure Hunter skills as 1st and 2nd class skills.<br>
 * For XML schema please refer to skillTrees.xsd in datapack in xsd folder and for parameters documentation refer to documentation.txt in skillTrees folder.<br>
 * @author Zoey76, Mobius
 */
public class SkillTreeData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(SkillTreeData.class.getName());
	
	// ClassId, Map of Skill Hash Code, SkillLearn
	private static final Map<PlayerClass, Map<Long, SkillLearn>> _classSkillTrees = new ConcurrentHashMap<>();
	private static final Map<PlayerClass, Map<Long, SkillLearn>> _completeClassSkillTree = new HashMap<>();
	private static final Map<PlayerClass, Map<Long, SkillLearn>> _transferSkillTrees = new ConcurrentHashMap<>();
	private static final Map<Race, Map<Long, SkillLearn>> _raceSkillTree = new ConcurrentHashMap<>();
	private static final Map<SubclassType, Map<Long, SkillLearn>> _revelationSkillTree = new ConcurrentHashMap<>();
	private static final Map<PlayerClass, Set<Integer>> _awakeningSaveSkillTree = new ConcurrentHashMap<>();
	// Skill Hash Code, SkillLearn
	private static final Map<Long, SkillLearn> _collectSkillTree = new ConcurrentHashMap<>();
	private static final Map<Long, SkillLearn> _fishingSkillTree = new ConcurrentHashMap<>();
	private static final Map<Long, SkillLearn> _pledgeSkillTree = new ConcurrentHashMap<>();
	private static final Map<Long, SkillLearn> _subClassSkillTree = new ConcurrentHashMap<>();
	private static final Map<Long, SkillLearn> _subPledgeSkillTree = new ConcurrentHashMap<>();
	private static final Map<Long, SkillLearn> _transformSkillTree = new ConcurrentHashMap<>();
	private static final Map<Long, SkillLearn> _commonSkillTree = new ConcurrentHashMap<>();
	private static final Map<Long, SkillLearn> _abilitySkillTree = new ConcurrentHashMap<>();
	private static final Map<Long, SkillLearn> _alchemySkillTree = new ConcurrentHashMap<>();
	private static final Map<Long, SkillLearn> _dualClassSkillTree = new ConcurrentHashMap<>();
	// Other skill trees
	private static final Map<Long, SkillLearn> _nobleSkillTree = new ConcurrentHashMap<>();
	private static final Map<Long, SkillLearn> _heroSkillTree = new ConcurrentHashMap<>();
	private static final Map<Long, SkillLearn> _gameMasterSkillTree = new ConcurrentHashMap<>();
	private static final Map<Long, SkillLearn> _gameMasterAuraSkillTree = new ConcurrentHashMap<>();
	// Remove skill tree
	private static final Map<PlayerClass, Set<Integer>> _removeSkillCache = new ConcurrentHashMap<>();
	
	// Checker, sorted arrays of hash codes
	private Map<Integer, long[]> _skillsByClassIdHashCodes; // Occupation skills
	private Map<Integer, long[]> _skillsByRaceHashCodes; // Race-specific Transformations
	private long[] _allSkillsHashCodes; // Fishing, Collection, Transformations, Common Skills.
	
	/** Parent class Ids are read from XML and stored in this map, to allow easy customization. */
	private static final Map<PlayerClass, PlayerClass> _parentClassMap = new ConcurrentHashMap<>();
	
	private boolean _loading = true;
	
	/**
	 * Instantiates a new skill trees data.
	 */
	protected SkillTreeData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_loading = true;
		_parentClassMap.clear();
		_classSkillTrees.clear();
		_collectSkillTree.clear();
		_fishingSkillTree.clear();
		_pledgeSkillTree.clear();
		_subClassSkillTree.clear();
		_subPledgeSkillTree.clear();
		_transferSkillTrees.clear();
		_transformSkillTree.clear();
		_nobleSkillTree.clear();
		_abilitySkillTree.clear();
		_alchemySkillTree.clear();
		_heroSkillTree.clear();
		_gameMasterSkillTree.clear();
		_gameMasterAuraSkillTree.clear();
		_raceSkillTree.clear();
		_revelationSkillTree.clear();
		_dualClassSkillTree.clear();
		_removeSkillCache.clear();
		_awakeningSaveSkillTree.clear();
		
		// Load files.
		parseDatapackDirectory("data/skillTrees/", true);
		
		// Cache the complete class skill trees.
		_completeClassSkillTree.clear();
		for (Entry<PlayerClass, Map<Long, SkillLearn>> entry : _classSkillTrees.entrySet())
		{
			final Map<Long, SkillLearn> skillTree = new HashMap<>();
			
			// Add all skills that belong to all classes.
			skillTree.putAll(_commonSkillTree);
			final PlayerClass entryClassId = entry.getKey();
			PlayerClass currentClassId = entryClassId;
			while ((currentClassId != null) && (_classSkillTrees.get(currentClassId) != null))
			{
				skillTree.putAll(_classSkillTrees.get(currentClassId));
				currentClassId = _parentClassMap.get(currentClassId);
			}
			
			_completeClassSkillTree.put(entryClassId, skillTree);
		}
		
		// Generate check arrays.
		generateCheckArrays();
		
		// Logs a report with skill trees info.
		report();
		
		_loading = false;
	}
	
	/**
	 * Parse a skill tree file and store it into the correct skill tree.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		NamedNodeMap attrs;
		Node attr;
		String type = null;
		Race race = null;
		SubclassType subType = null;
		int cId = -1;
		int parentClassId = -1;
		PlayerClass classId = null;
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("skillTree".equalsIgnoreCase(d.getNodeName()))
					{
						final Map<Long, SkillLearn> classSkillTree = new HashMap<>();
						final Map<Long, SkillLearn> transferSkillTree = new HashMap<>();
						final Map<Long, SkillLearn> raceSkillTree = new HashMap<>();
						final Map<Long, SkillLearn> revelationSkillTree = new HashMap<>();
						type = d.getAttributes().getNamedItem("type").getNodeValue();
						attr = d.getAttributes().getNamedItem("classId");
						if (attr != null)
						{
							cId = Integer.parseInt(attr.getNodeValue());
							classId = PlayerClass.getPlayerClass(cId);
						}
						else
						{
							cId = -1;
						}
						
						attr = d.getAttributes().getNamedItem("race");
						if (attr != null)
						{
							race = parseEnum(attr, Race.class);
						}
						
						attr = d.getAttributes().getNamedItem("subType");
						if (attr != null)
						{
							subType = parseEnum(attr, SubclassType.class);
						}
						
						attr = d.getAttributes().getNamedItem("parentClassId");
						if (attr != null)
						{
							parentClassId = Integer.parseInt(attr.getNodeValue());
							if ((cId > -1) && (cId != parentClassId) && (parentClassId > -1) && !_parentClassMap.containsKey(classId))
							{
								_parentClassMap.put(classId, PlayerClass.getPlayerClass(parentClassId));
							}
						}
						
						for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
						{
							if ("skill".equalsIgnoreCase(c.getNodeName()))
							{
								final StatSet learnSkillSet = new StatSet();
								attrs = c.getAttributes();
								for (int i = 0; i < attrs.getLength(); i++)
								{
									attr = attrs.item(i);
									learnSkillSet.set(attr.getNodeName(), attr.getNodeValue());
								}
								
								final SkillLearn skillLearn = new SkillLearn(learnSkillSet);
								
								// test if skill exists
								SkillData.getInstance().getSkill(skillLearn.getSkillId(), skillLearn.getSkillLevel());
								for (Node b = c.getFirstChild(); b != null; b = b.getNextSibling())
								{
									attrs = b.getAttributes();
									switch (b.getNodeName())
									{
										case "item":
										{
											final List<ItemHolder> itemList = new ArrayList<>(1);
											final int count = parseInteger(attrs, "count");
											for (String id : parseString(attrs, "id").split(","))
											{
												itemList.add(new ItemHolder(Integer.parseInt(id), count));
											}
											skillLearn.addRequiredItem(itemList);
											break;
										}
										case "preRequisiteSkill":
										{
											skillLearn.addPreReqSkill(new SkillHolder(parseInteger(attrs, "id"), parseInteger(attrs, "lvl")));
											break;
										}
										case "race":
										{
											skillLearn.addRace(Race.valueOf(b.getTextContent()));
											break;
										}
										case "residenceId":
										{
											skillLearn.addResidenceId(Integer.parseInt(b.getTextContent()));
											break;
										}
										case "socialClass":
										{
											skillLearn.setSocialClass(Enum.valueOf(SocialClass.class, b.getTextContent()));
											break;
										}
										case "removeSkill":
										{
											final int removeSkillId = parseInteger(attrs, "id");
											skillLearn.addRemoveSkills(removeSkillId);
											if (!parseBoolean(attrs, "onlyReplaceByLearn", false).booleanValue())
											{
												_removeSkillCache.computeIfAbsent(classId, _ -> new HashSet<>()).add(removeSkillId);
											}
											break;
										}
									}
								}
								
								final long skillHashCode = SkillData.getSkillHashCode(skillLearn.getSkillId(), skillLearn.getSkillLevel());
								switch (type)
								{
									case "classSkillTree":
									{
										if (cId != -1)
										{
											classSkillTree.put(skillHashCode, skillLearn);
										}
										else
										{
											_commonSkillTree.put(skillHashCode, skillLearn);
										}
										break;
									}
									case "transferSkillTree":
									{
										transferSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "collectSkillTree":
									{
										_collectSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "raceSkillTree":
									{
										raceSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "revelationSkillTree":
									{
										revelationSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "fishingSkillTree":
									{
										_fishingSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "pledgeSkillTree":
									{
										_pledgeSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "subClassSkillTree":
									{
										_subClassSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "subPledgeSkillTree":
									{
										_subPledgeSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "transformSkillTree":
									{
										_transformSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "nobleSkillTree":
									{
										_nobleSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "abilitySkillTree":
									{
										_abilitySkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "alchemySkillTree":
									{
										_alchemySkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "heroSkillTree":
									{
										_heroSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "gameMasterSkillTree":
									{
										_gameMasterSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "gameMasterAuraSkillTree":
									{
										_gameMasterAuraSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "dualClassSkillTree":
									{
										_dualClassSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "awakeningSaveSkillTree":
									{
										_awakeningSaveSkillTree.computeIfAbsent(classId, _ -> new HashSet<>()).add(skillLearn.getSkillId());
										break;
									}
									default:
									{
										LOGGER.warning(getClass().getSimpleName() + ": Unknown Skill Tree type: " + type + "!");
									}
								}
							}
						}
						
						if (type.equals("transferSkillTree"))
						{
							_transferSkillTrees.put(classId, transferSkillTree);
						}
						else if (type.equals("classSkillTree") && (cId > -1))
						{
							final Map<Long, SkillLearn> classSkillTrees = _classSkillTrees.get(classId);
							if (classSkillTrees == null)
							{
								_classSkillTrees.put(classId, classSkillTree);
							}
							else
							{
								classSkillTrees.putAll(classSkillTree);
							}
						}
						else if (type.equals("raceSkillTree") && (race != null))
						{
							final Map<Long, SkillLearn> raceSkillTrees = _raceSkillTree.get(race);
							if (raceSkillTrees == null)
							{
								_raceSkillTree.put(race, raceSkillTree);
							}
							else
							{
								raceSkillTrees.putAll(raceSkillTree);
							}
						}
						else if (type.equals("revelationSkillTree") && (subType != null))
						{
							final Map<Long, SkillLearn> revelationSkillTrees = _revelationSkillTree.get(subType);
							if (revelationSkillTrees == null)
							{
								_revelationSkillTree.put(subType, revelationSkillTree);
							}
							else
							{
								revelationSkillTrees.putAll(revelationSkillTree);
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Method to get the complete skill tree for a given class id.<br>
	 * Include all skills common to all classes.<br>
	 * Includes all parent skill trees.
	 * @param classId the class skill tree Id
	 * @return the complete Class Skill Tree including skill trees from parent class for a given {@code classId}
	 */
	public Map<Long, SkillLearn> getCompleteClassSkillTree(PlayerClass classId)
	{
		return _completeClassSkillTree.getOrDefault(classId, Collections.emptyMap());
	}
	
	/**
	 * Gets the transfer skill tree.<br>
	 * If new classes are implemented over 3rd class, we use a recursive call.
	 * @param classId the transfer skill tree Id
	 * @return the complete Transfer Skill Tree for a given {@code classId}
	 */
	public Map<Long, SkillLearn> getTransferSkillTree(PlayerClass classId)
	{
		return _transferSkillTrees.get(classId);
	}
	
	/**
	 * Gets the race skill tree.
	 * @param race the race skill tree Id
	 * @return the complete race Skill Tree for a given {@code Race}
	 */
	public Collection<SkillLearn> getRaceSkillTree(Race race)
	{
		return _raceSkillTree.containsKey(race) ? _raceSkillTree.get(race).values() : Collections.emptyList();
	}
	
	/**
	 * Gets the common skill tree.
	 * @return the complete Common Skill Tree
	 */
	public Map<Long, SkillLearn> getCommonSkillTree()
	{
		return _commonSkillTree;
	}
	
	/**
	 * Gets the collect skill tree.
	 * @return the complete Collect Skill Tree
	 */
	public Map<Long, SkillLearn> getCollectSkillTree()
	{
		return _collectSkillTree;
	}
	
	/**
	 * Gets the fishing skill tree.
	 * @return the complete Fishing Skill Tree
	 */
	public Map<Long, SkillLearn> getFishingSkillTree()
	{
		return _fishingSkillTree;
	}
	
	/**
	 * Gets the pledge skill tree.
	 * @return the complete Clan Skill Tree
	 */
	public Map<Long, SkillLearn> getPledgeSkillTree()
	{
		return _pledgeSkillTree;
	}
	
	/**
	 * Gets the sub class skill tree.
	 * @return the complete Sub-Class Skill Tree
	 */
	public Map<Long, SkillLearn> getSubClassSkillTree()
	{
		return _subClassSkillTree;
	}
	
	/**
	 * Gets the sub pledge skill tree.
	 * @return the complete Sub-Pledge Skill Tree
	 */
	public Map<Long, SkillLearn> getSubPledgeSkillTree()
	{
		return _subPledgeSkillTree;
	}
	
	/**
	 * Gets the transform skill tree.
	 * @return the complete Transform Skill Tree
	 */
	public Map<Long, SkillLearn> getTransformSkillTree()
	{
		return _transformSkillTree;
	}
	
	/**
	 * Gets the ability skill tree.
	 * @return the complete Ability Skill Tree
	 */
	public Map<Long, SkillLearn> getAbilitySkillTree()
	{
		return _abilitySkillTree;
	}
	
	/**
	 * Gets the ability skill tree.
	 * @return the complete Ability Skill Tree
	 */
	public Map<Long, SkillLearn> getAlchemySkillTree()
	{
		return _alchemySkillTree;
	}
	
	/**
	 * Gets the noble skill tree.
	 * @return the complete Noble Skill Tree
	 */
	public List<Skill> getNobleSkillTree()
	{
		final List<Skill> result = new LinkedList<>();
		for (SkillLearn skill : _nobleSkillTree.values())
		{
			result.add(SkillData.getInstance().getSkill(skill.getSkillId(), skill.getSkillLevel()));
		}
		return result;
	}
	
	/**
	 * Gets the noble skill tree.
	 * @return the complete Noble Skill Tree
	 */
	public List<Skill> getNobleSkillAutoGetTree()
	{
		final List<Skill> result = new LinkedList<>();
		for (SkillLearn skill : _nobleSkillTree.values())
		{
			if (skill.isAutoGet())
			{
				result.add(SkillData.getInstance().getSkill(skill.getSkillId(), skill.getSkillLevel()));
			}
		}
		return result;
	}
	
	/**
	 * Gets the hero skill tree.
	 * @return the complete Hero Skill Tree
	 */
	public List<Skill> getHeroSkillTree()
	{
		final List<Skill> result = new LinkedList<>();
		for (SkillLearn skill : _heroSkillTree.values())
		{
			result.add(SkillData.getInstance().getSkill(skill.getSkillId(), skill.getSkillLevel()));
		}
		return result;
	}
	
	/**
	 * Gets the Game Master skill tree.
	 * @return the complete Game Master Skill Tree
	 */
	public List<Skill> getGMSkillTree()
	{
		final List<Skill> result = new LinkedList<>();
		for (SkillLearn skill : _gameMasterSkillTree.values())
		{
			result.add(SkillData.getInstance().getSkill(skill.getSkillId(), skill.getSkillLevel()));
		}
		return result;
	}
	
	/**
	 * Gets the Game Master Aura skill tree.
	 * @return the complete Game Master Aura Skill Tree
	 */
	public List<Skill> getGMAuraSkillTree()
	{
		final List<Skill> result = new LinkedList<>();
		for (SkillLearn skill : _gameMasterAuraSkillTree.values())
		{
			result.add(SkillData.getInstance().getSkill(skill.getSkillId(), skill.getSkillLevel()));
		}
		return result;
	}
	
	/**
	 * @param player
	 * @param classId
	 * @return {@code true} if player is able to learn new skills on his current level, {@code false} otherwise.
	 */
	public boolean hasAvailableSkills(Player player, PlayerClass classId)
	{
		final Map<Long, SkillLearn> skills = getCompleteClassSkillTree(classId);
		for (SkillLearn skill : skills.values())
		{
			if ((skill.getSkillId() == CommonSkill.DIVINE_INSPIRATION.getId()) || skill.isAutoGet() || skill.isLearnedByFS() || (skill.getGetLevel() > player.getLevel()))
			{
				continue;
			}
			final Skill oldSkill = player.getKnownSkill(skill.getSkillId());
			if ((oldSkill != null) && (oldSkill.getLevel() == (skill.getSkillLevel() - 1)))
			{
				return true;
			}
			else if ((oldSkill == null) && (skill.getSkillLevel() == 1))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Gets the available skills.
	 * @param player the learning skill player
	 * @param classId the learning skill class Id
	 * @param includeByFs if {@code true} skills from Forgotten Scroll will be included
	 * @param includeAutoGet if {@code true} Auto-Get skills will be included
	 * @return all available skills for a given {@code player}, {@code classId}, {@code includeByFs} and {@code includeAutoGet}
	 */
	public Collection<SkillLearn> getAvailableSkills(Player player, PlayerClass classId, boolean includeByFs, boolean includeAutoGet)
	{
		return getAvailableSkills(player, classId, includeByFs, includeAutoGet, true, player.getSkills());
	}
	
	/**
	 * Gets the available skills.
	 * @param player the learning skill player
	 * @param classId the learning skill class Id
	 * @param includeByFs if {@code true} skills from Forgotten Scroll will be included
	 * @param includeAutoGet if {@code true} Auto-Get skills will be included
	 * @param includeRequiredItems if {@code true} skills that have required items will be added
	 * @param existingSkills the complete Map of currently known skills.
	 * @return all available skills for a given {@code player}, {@code classId}, {@code includeByFs} and {@code includeAutoGet}
	 */
	private Collection<SkillLearn> getAvailableSkills(Player player, PlayerClass classId, boolean includeByFs, boolean includeAutoGet, boolean includeRequiredItems, Map<Integer, Skill> existingSkills)
	{
		final Set<SkillLearn> result = new HashSet<>();
		final Map<Long, SkillLearn> skills = getCompleteClassSkillTree(classId);
		if (skills.isEmpty())
		{
			// The Skill Tree for this class is undefined.
			LOGGER.warning(getClass().getSimpleName() + ": Skilltree for class " + classId + " is not defined!");
			return result;
		}
		
		for (Entry<Long, SkillLearn> entry : skills.entrySet())
		{
			final SkillLearn skill = entry.getValue();
			if (((skill.getSkillId() == CommonSkill.DIVINE_INSPIRATION.getId()) && (!Config.AUTO_LEARN_DIVINE_INSPIRATION && includeAutoGet) && !player.isGM()) || (!includeAutoGet && skill.isAutoGet()) || (!includeByFs && skill.isLearnedByFS()) || isRemoveSkill(classId, skill.getSkillId()))
			{
				continue;
			}
			
			// Forgotten Scroll requirements checked above.
			if (!includeRequiredItems && !skill.getRequiredItems().isEmpty() && !skill.isLearnedByFS())
			{
				continue;
			}
			
			if (player.getLevel() >= skill.getGetLevel())
			{
				if (skill.getSkillLevel() > SkillData.getInstance().getMaxLevel(skill.getSkillId()))
				{
					LOGGER.severe(getClass().getSimpleName() + ": SkillTreesData found learnable skill " + skill.getSkillId() + " with level higher than max skill level!");
					continue;
				}
				
				final Skill oldSkill = existingSkills.get(player.getReplacementSkill(skill.getSkillId()));
				if (oldSkill != null)
				{
					if (oldSkill.getLevel() == (skill.getSkillLevel() - 1))
					{
						result.add(skill);
					}
				}
				else if (skill.getSkillLevel() == 1)
				{
					result.add(skill);
				}
			}
		}
		
		// Manage skill unlearn for player skills.
		for (Skill knownSkill : player.getSkillList())
		{
			SkillLearn skillLearn = getClassSkill(player.getOriginalSkill(knownSkill.getId()), knownSkill.getLevel(), classId);
			if (skillLearn == null)
			{
				continue;
			}
			
			Set<Integer> removeSkills = skillLearn.getRemoveSkills();
			if (removeSkills.isEmpty())
			{
				if (knownSkill.getLevel() > 1)
				{
					// Check first skill level for removed skills.
					skillLearn = getClassSkill(knownSkill.getId(), 1, classId);
					if (skillLearn == null)
					{
						continue;
					}
					
					removeSkills = skillLearn.getRemoveSkills();
					if (removeSkills.isEmpty())
					{
						continue;
					}
				}
				else
				{
					continue;
				}
			}
			
			for (int removeId : removeSkills)
			{
				SEARCH: for (SkillLearn knownLearn : result)
				{
					if (knownLearn.getSkillId() == removeId)
					{
						result.remove(knownLearn);
						break SEARCH;
					}
				}
			}
		}
		
		// Manage skill unlearn for player replaced skills.
		for (int skillId : player.getReplacedSkills())
		{
			final SkillLearn skillLearn = getClassSkill(skillId, 1, classId);
			if (skillLearn != null)
			{
				final Set<Integer> removeSkills = skillLearn.getRemoveSkills();
				if (removeSkills != null)
				{
					for (int removeId : removeSkills)
					{
						SEARCH: for (SkillLearn knownLearn : result)
						{
							if (knownLearn.getSkillId() == removeId)
							{
								result.remove(knownLearn);
								break SEARCH;
							}
						}
					}
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Used by auto learn configuration.
	 * @param player
	 * @param classId
	 * @param includeByFs if {@code true} forgotten scroll skills present in the skill tree will be added
	 * @param includeAutoGet if {@code true} auto-get skills present in the skill tree will be added
	 * @param includeRequiredItems if {@code true} skills that have required items will be added
	 * @return a list of auto learnable skills for the player.
	 */
	public Collection<Skill> getAllAvailableSkills(Player player, PlayerClass classId, boolean includeByFs, boolean includeAutoGet, boolean includeRequiredItems)
	{
		final Map<Integer, Skill> result = new HashMap<>();
		for (Skill skill : player.getSkills().values())
		{
			// Adding only skills that can be learned by the player.
			if (isSkillAllowed(player, skill))
			{
				result.put(skill.getId(), skill);
			}
		}
		
		final Set<Integer> removed = new HashSet<>();
		Collection<SkillLearn> learnable;
		for (int i = 0; i < 10; i++)
		{
			learnable = getAvailableSkills(player, classId, includeByFs, includeAutoGet, includeRequiredItems, result);
			if (learnable.isEmpty())
			{
				break;
			}
			
			// All remaining skills have been removed.
			boolean allRemoved = true;
			SEARCH: for (SkillLearn skillLearn : learnable)
			{
				if (!removed.contains(skillLearn.getSkillId()))
				{
					allRemoved = false;
					break SEARCH;
				}
			}
			if (allRemoved)
			{
				break;
			}
			
			for (SkillLearn skillLearn : learnable)
			{
				// Cleanup skills that has to be removed.
				for (int skillId : skillLearn.getRemoveSkills())
				{
					// Mark skill as removed, so it doesn't gets added.
					removed.add(skillId);
					
					// Remove skill from player's skill list or existing skill list.
					final Skill playerSkillToRemove = player.getKnownSkill(skillId);
					final Skill holderSkillToRemove = result.get(skillId);
					
					// If player has the skill remove it.
					if (playerSkillToRemove != null)
					{
						player.removeSkill(playerSkillToRemove);
					}
					
					// If result already contains the skill remove it.
					if (holderSkillToRemove != null)
					{
						result.remove(skillId);
					}
				}
				
				if (!removed.contains(skillLearn.getSkillId()))
				{
					final Skill skill = SkillData.getInstance().getSkill(skillLearn.getSkillId(), skillLearn.getSkillLevel());
					result.put(skill.getId(), skill);
				}
			}
		}
		
		return result.values();
	}
	
	/**
	 * Gets the available auto get skills.
	 * @param player the player requesting the Auto-Get skills
	 * @return all the available Auto-Get skills for a given {@code player}
	 */
	public List<SkillLearn> getAvailableAutoGetSkills(Player player)
	{
		final List<SkillLearn> result = new LinkedList<>();
		final Map<Long, SkillLearn> skills = getCompleteClassSkillTree(player.getPlayerClass());
		if (skills.isEmpty())
		{
			// The Skill Tree for this class is undefined, so we return an empty list.
			LOGGER.warning(getClass().getSimpleName() + ": Skill Tree for this class Id(" + player.getPlayerClass() + ") is not defined!");
			return result;
		}
		
		final Race race = player.getRace();
		for (SkillLearn skill : skills.values())
		{
			if (!skill.isAutoGet())
			{
				continue;
			}
			
			if ((player.getLevel() < skill.getGetLevel()))
			{
				continue;
			}
			
			if (!skill.getRaces().isEmpty() && !skill.getRaces().contains(race))
			{
				continue;
			}
			
			final Skill oldSkill = player.getKnownSkill(player.getReplacementSkill(skill.getSkillId()));
			if (oldSkill != null)
			{
				if (oldSkill.getLevel() < skill.getSkillLevel())
				{
					result.add(skill);
				}
			}
			else
			{
				result.add(skill);
			}
		}
		
		// Manage skill unlearn for player skills.
		for (Skill knownSkill : player.getSkillList())
		{
			SkillLearn skillLearn = getClassSkill(player.getOriginalSkill(knownSkill.getId()), knownSkill.getLevel(), player.getPlayerClass());
			if (skillLearn == null)
			{
				continue;
			}
			
			Set<Integer> removeSkills = skillLearn.getRemoveSkills();
			if (removeSkills.isEmpty())
			{
				if (knownSkill.getLevel() > 1)
				{
					// Check first skill level for removed skills.
					skillLearn = getClassSkill(knownSkill.getId(), 1, player.getPlayerClass());
					if (skillLearn == null)
					{
						continue;
					}
					
					removeSkills = skillLearn.getRemoveSkills();
					if (removeSkills.isEmpty())
					{
						continue;
					}
				}
				else
				{
					continue;
				}
			}
			
			for (int removeId : removeSkills)
			{
				SEARCH: for (SkillLearn knownLearn : result)
				{
					if (knownLearn.getSkillId() == removeId)
					{
						result.remove(knownLearn);
						break SEARCH;
					}
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Dwarvens will get additional dwarven only fishing skills.
	 * @param player the player
	 * @return all the available Fishing skills for a given {@code player}
	 */
	public List<SkillLearn> getAvailableFishingSkills(Player player)
	{
		final List<SkillLearn> result = new LinkedList<>();
		final Race playerRace = player.getRace();
		for (SkillLearn skill : _fishingSkillTree.values())
		{
			// If skill is Race specific and the player's race isn't allowed, skip it.
			if (!skill.getRaces().isEmpty() && !skill.getRaces().contains(playerRace))
			{
				continue;
			}
			
			if (skill.isLearnedByNpc() && (player.getLevel() >= skill.getGetLevel()))
			{
				final Skill oldSkill = player.getSkills().get(skill.getSkillId());
				if (oldSkill != null)
				{
					if (oldSkill.getLevel() == (skill.getSkillLevel() - 1))
					{
						result.add(skill);
					}
				}
				else if (skill.getSkillLevel() == 1)
				{
					result.add(skill);
				}
			}
		}
		return result;
	}
	
	/**
	 * Gets the available revelation skills
	 * @param player the player requesting the revelation skills
	 * @param type the player current subclass type
	 * @return all the available revelation skills for a given {@code player}
	 */
	public List<SkillLearn> getAvailableRevelationSkills(Player player, SubclassType type)
	{
		final List<SkillLearn> result = new LinkedList<>();
		final Map<Long, SkillLearn> revelationSkills = _revelationSkillTree.get(type);
		for (SkillLearn skill : revelationSkills.values())
		{
			final Skill oldSkill = player.getSkills().get(skill.getSkillId());
			if (oldSkill == null)
			{
				result.add(skill);
			}
		}
		return result;
	}
	
	/**
	 * Gets the available alchemy skills, restricted to Ertheia
	 * @param player the player requesting the alchemy skills
	 * @return all the available alchemy skills for a given {@code player}
	 */
	public List<SkillLearn> getAvailableAlchemySkills(Player player)
	{
		final List<SkillLearn> result = new LinkedList<>();
		for (SkillLearn skill : _alchemySkillTree.values())
		{
			if (skill.isLearnedByNpc() && (player.getLevel() >= skill.getGetLevel()))
			{
				final Skill oldSkill = player.getSkills().get(skill.getSkillId());
				if (oldSkill != null)
				{
					if (oldSkill.getLevel() == (skill.getSkillLevel() - 1))
					{
						result.add(skill);
					}
				}
				else if (skill.getSkillLevel() == 1)
				{
					result.add(skill);
				}
			}
		}
		return result;
	}
	
	/**
	 * Used in Gracia continent.
	 * @param player the collecting skill learning player
	 * @return all the available Collecting skills for a given {@code player}
	 */
	public List<SkillLearn> getAvailableCollectSkills(Player player)
	{
		final List<SkillLearn> result = new LinkedList<>();
		for (SkillLearn skill : _collectSkillTree.values())
		{
			final Skill oldSkill = player.getSkills().get(skill.getSkillId());
			if (oldSkill != null)
			{
				if (oldSkill.getLevel() == (skill.getSkillLevel() - 1))
				{
					result.add(skill);
				}
			}
			else if (skill.getSkillLevel() == 1)
			{
				result.add(skill);
			}
		}
		return result;
	}
	
	/**
	 * Gets the available transfer skills.
	 * @param player the transfer skill learning player
	 * @return all the available Transfer skills for a given {@code player}
	 */
	public List<SkillLearn> getAvailableTransferSkills(Player player)
	{
		final List<SkillLearn> result = new LinkedList<>();
		final PlayerClass classId = player.getPlayerClass();
		if (!_transferSkillTrees.containsKey(classId))
		{
			return result;
		}
		
		for (SkillLearn skill : _transferSkillTrees.get(classId).values())
		{
			// If player doesn't know this transfer skill:
			if (player.getKnownSkill(skill.getSkillId()) == null)
			{
				result.add(skill);
			}
		}
		return result;
	}
	
	/**
	 * Some transformations are not available for some races.
	 * @param player the transformation skill learning player
	 * @return all the available Transformation skills for a given {@code player}
	 */
	public List<SkillLearn> getAvailableTransformSkills(Player player)
	{
		final List<SkillLearn> result = new LinkedList<>();
		final Race race = player.getRace();
		for (SkillLearn skill : _transformSkillTree.values())
		{
			if ((player.getLevel() >= skill.getGetLevel()) && (skill.getRaces().isEmpty() || skill.getRaces().contains(race)))
			{
				final Skill oldSkill = player.getSkills().get(skill.getSkillId());
				if (oldSkill != null)
				{
					if (oldSkill.getLevel() == (skill.getSkillLevel() - 1))
					{
						result.add(skill);
					}
				}
				else if (skill.getSkillLevel() == 1)
				{
					result.add(skill);
				}
			}
		}
		return result;
	}
	
	/**
	 * Gets the available pledge skills.
	 * @param clan the pledge skill learning clan
	 * @return all the available Clan skills for a given {@code clan}
	 */
	public List<SkillLearn> getAvailablePledgeSkills(Clan clan)
	{
		final List<SkillLearn> result = new LinkedList<>();
		for (SkillLearn skill : _pledgeSkillTree.values())
		{
			if (!skill.isResidencialSkill() && (clan.getLevel() >= skill.getGetLevel()))
			{
				final Skill oldSkill = clan.getSkills().get(skill.getSkillId());
				if (oldSkill != null)
				{
					if ((oldSkill.getLevel() + 1) == skill.getSkillLevel())
					{
						result.add(skill);
					}
				}
				else if (skill.getSkillLevel() == 1)
				{
					result.add(skill);
				}
			}
		}
		return result;
	}
	
	/**
	 * Gets the available pledge skills.
	 * @param clan the pledge skill learning clan
	 * @param includeSquad if squad skill will be added too
	 * @return all the available pledge skills for a given {@code clan}
	 */
	public Map<Integer, SkillLearn> getMaxPledgeSkills(Clan clan, boolean includeSquad)
	{
		final Map<Integer, SkillLearn> result = new HashMap<>();
		for (SkillLearn skill : _pledgeSkillTree.values())
		{
			if (!skill.isResidencialSkill() && (clan.getLevel() >= skill.getGetLevel()))
			{
				final Skill oldSkill = clan.getSkills().get(skill.getSkillId());
				if ((oldSkill == null) || (oldSkill.getLevel() < skill.getSkillLevel()))
				{
					result.put(skill.getSkillId(), skill);
				}
			}
		}
		
		if (includeSquad)
		{
			for (SkillLearn skill : _subPledgeSkillTree.values())
			{
				if ((clan.getLevel() >= skill.getGetLevel()))
				{
					final Skill oldSkill = clan.getSkills().get(skill.getSkillId());
					if ((oldSkill == null) || (oldSkill.getLevel() < skill.getSkillLevel()))
					{
						result.put(skill.getSkillId(), skill);
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * Gets the available sub pledge skills.
	 * @param clan the sub-pledge skill learning clan
	 * @return all the available Sub-Pledge skills for a given {@code clan}
	 */
	public List<SkillLearn> getAvailableSubPledgeSkills(Clan clan)
	{
		final List<SkillLearn> result = new LinkedList<>();
		for (SkillLearn skill : _subPledgeSkillTree.values())
		{
			if ((clan.getLevel() >= skill.getGetLevel()) && clan.isLearnableSubSkill(skill.getSkillId(), skill.getSkillLevel()))
			{
				result.add(skill);
			}
		}
		return result;
	}
	
	/**
	 * Gets the available sub class skills.
	 * @param player the sub-class skill learning player
	 * @return all the available Sub-Class skills for a given {@code player}
	 */
	public List<SkillLearn> getAvailableSubClassSkills(Player player)
	{
		final List<SkillLearn> result = new LinkedList<>();
		for (SkillLearn skill : _subClassSkillTree.values())
		{
			final Skill oldSkill = player.getSkills().get(skill.getSkillId());
			if (((oldSkill == null) && (skill.getSkillLevel() == 1)) || ((oldSkill != null) && (oldSkill.getLevel() == (skill.getSkillLevel() - 1))))
			{
				result.add(skill);
			}
		}
		return result;
	}
	
	/**
	 * Gets the available dual class skills.
	 * @param player the dual-class skill learning player
	 * @return all the available Dual-Class skills for a given {@code player} sorted by skill ID
	 */
	public List<SkillLearn> getAvailableDualClassSkills(Player player)
	{
		final List<SkillLearn> result = new LinkedList<>();
		for (SkillLearn skill : _dualClassSkillTree.values())
		{
			final Skill oldSkill = player.getSkills().get(skill.getSkillId());
			if (((oldSkill == null) && (skill.getSkillLevel() == 1)) || ((oldSkill != null) && (oldSkill.getLevel() == (skill.getSkillLevel() - 1))))
			{
				result.add(skill);
			}
		}
		result.sort(Comparator.comparing(SkillLearn::getSkillId));
		return result;
	}
	
	/**
	 * Gets the available residential skills.
	 * @param residenceId the id of the Castle, Fort, Territory
	 * @return all the available Residential skills for a given {@code residenceId}
	 */
	public List<SkillLearn> getAvailableResidentialSkills(int residenceId)
	{
		final List<SkillLearn> result = new LinkedList<>();
		for (SkillLearn skill : _pledgeSkillTree.values())
		{
			if (skill.isResidencialSkill() && skill.getResidenceIds().contains(residenceId))
			{
				result.add(skill);
			}
		}
		return result;
	}
	
	/**
	 * Just a wrapper for all skill trees.
	 * @param skillType the skill type
	 * @param id the skill Id
	 * @param lvl the skill level
	 * @param player the player learning the skill
	 * @return the skill learn for the specified parameters
	 */
	public SkillLearn getSkillLearn(AcquireSkillType skillType, int id, int lvl, Player player)
	{
		SkillLearn sl = null;
		switch (skillType)
		{
			case CLASS:
			{
				sl = getClassSkill(id, lvl, player.getPlayerClass());
				break;
			}
			case TRANSFORM:
			{
				sl = getTransformSkill(id, lvl);
				break;
			}
			case FISHING:
			{
				sl = getFishingSkill(id, lvl);
				break;
			}
			case PLEDGE:
			{
				sl = getPledgeSkill(id, lvl);
				break;
			}
			case SUBPLEDGE:
			{
				sl = getSubPledgeSkill(id, lvl);
				break;
			}
			case TRANSFER:
			{
				sl = getTransferSkill(id, lvl, player.getPlayerClass());
				break;
			}
			case SUBCLASS:
			{
				sl = getSubClassSkill(id, lvl);
				break;
			}
			case COLLECT:
			{
				sl = getCollectSkill(id, lvl);
				break;
			}
			case REVELATION:
			{
				sl = getRevelationSkill(SubclassType.BASECLASS, id, lvl);
				break;
			}
			case REVELATION_DUALCLASS:
			{
				sl = getRevelationSkill(SubclassType.DUALCLASS, id, lvl);
				break;
			}
			case ALCHEMY:
			{
				sl = getAlchemySkill(id, lvl);
				break;
			}
			case DUALCLASS:
			{
				sl = getDualClassSkill(id, lvl);
				break;
			}
		}
		return sl;
	}
	
	/**
	 * Gets the transform skill.
	 * @param id the transformation skill Id
	 * @param lvl the transformation skill level
	 * @return the transform skill from the Transform Skill Tree for a given {@code id} and {@code lvl}
	 */
	private SkillLearn getTransformSkill(int id, int lvl)
	{
		return _transformSkillTree.get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Gets the ability skill.
	 * @param id the ability skill Id
	 * @param lvl the ability skill level
	 * @return the ability skill from the Ability Skill Tree for a given {@code id} and {@code lvl}
	 */
	public SkillLearn getAbilitySkill(int id, int lvl)
	{
		return _abilitySkillTree.get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Gets the alchemy skill.
	 * @param id the alchemy skill Id
	 * @param lvl the alchemy skill level
	 * @return the alchemy skill from the Alchemy Skill Tree for a given {@code id} and {@code lvl}
	 */
	private SkillLearn getAlchemySkill(int id, int lvl)
	{
		return _alchemySkillTree.get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Gets the class skill.
	 * @param id the class skill Id
	 * @param lvl the class skill level.
	 * @param classId the class skill tree Id
	 * @return the class skill from the Class Skill Trees for a given {@code classId}, {@code id} and {@code lvl}
	 */
	public SkillLearn getClassSkill(int id, int lvl, PlayerClass classId)
	{
		return getCompleteClassSkillTree(classId).get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Gets the fishing skill.
	 * @param id the fishing skill Id
	 * @param lvl the fishing skill level
	 * @return Fishing skill from the Fishing Skill Tree for a given {@code id} and {@code lvl}
	 */
	private SkillLearn getFishingSkill(int id, int lvl)
	{
		return _fishingSkillTree.get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Gets the pledge skill.
	 * @param id the pledge skill Id
	 * @param lvl the pledge skill level
	 * @return the pledge skill from the Clan Skill Tree for a given {@code id} and {@code lvl}
	 */
	public SkillLearn getPledgeSkill(int id, int lvl)
	{
		return _pledgeSkillTree.get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Gets the sub pledge skill.
	 * @param id the sub-pledge skill Id
	 * @param lvl the sub-pledge skill level
	 * @return the sub-pledge skill from the Sub-Pledge Skill Tree for a given {@code id} and {@code lvl}
	 */
	public SkillLearn getSubPledgeSkill(int id, int lvl)
	{
		return _subPledgeSkillTree.get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Gets the transfer skill.
	 * @param id the transfer skill Id
	 * @param lvl the transfer skill level.
	 * @param classId the transfer skill tree Id
	 * @return the transfer skill from the Transfer Skill Trees for a given {@code classId}, {@code id} and {@code lvl}
	 */
	private SkillLearn getTransferSkill(int id, int lvl, PlayerClass classId)
	{
		if (_transferSkillTrees.get(classId) != null)
		{
			return _transferSkillTrees.get(classId).get(SkillData.getSkillHashCode(id, lvl));
		}
		return null;
	}
	
	/**
	 * Gets the race skill.
	 * @param id the race skill Id
	 * @param lvl the race skill level.
	 * @param race the race skill tree Id
	 * @return the transfer skill from the Race Skill Trees for a given {@code race}, {@code id} and {@code lvl}
	 */
	private SkillLearn getRaceSkill(int id, int lvl, Race race)
	{
		for (SkillLearn skill : getRaceSkillTree(race))
		{
			if ((skill.getSkillId() == id) && (skill.getSkillLevel() == lvl))
			{
				return skill;
			}
		}
		return null;
	}
	
	/**
	 * Gets the sub class skill.
	 * @param id the sub-class skill Id
	 * @param lvl the sub-class skill level
	 * @return the sub-class skill from the Sub-Class Skill Tree for a given {@code id} and {@code lvl}
	 */
	private SkillLearn getSubClassSkill(int id, int lvl)
	{
		return _subClassSkillTree.get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Gets the dual class skill.
	 * @param id the dual-class skill Id
	 * @param lvl the dual-class skill level
	 * @return the dual-class skill from the Dual-Class Skill Tree for a given {@code id} and {@code lvl}
	 */
	public SkillLearn getDualClassSkill(int id, int lvl)
	{
		return _dualClassSkillTree.get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Gets the common skill.
	 * @param id the common skill Id.
	 * @param lvl the common skill level
	 * @return the common skill from the Common Skill Tree for a given {@code id} and {@code lvl}
	 */
	public SkillLearn getCommonSkill(int id, int lvl)
	{
		return _commonSkillTree.get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Gets the collect skill.
	 * @param id the collect skill Id
	 * @param lvl the collect skill level
	 * @return the collect skill from the Collect Skill Tree for a given {@code id} and {@code lvl}
	 */
	public SkillLearn getCollectSkill(int id, int lvl)
	{
		return _collectSkillTree.get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Gets the revelation skill.
	 * @param type the subclass type
	 * @param id the revelation skill Id
	 * @param lvl the revelation skill level
	 * @return the revelation skill from the Revelation Skill Tree for a given {@code id} and {@code lvl}
	 */
	public SkillLearn getRevelationSkill(SubclassType type, int id, int lvl)
	{
		return _revelationSkillTree.get(type).get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Gets the minimum level for new skill.
	 * @param player the player that requires the minimum level
	 * @param skillTree the skill tree to search the minimum get level
	 * @return the minimum level for a new skill for a given {@code player} and {@code skillTree}
	 */
	public int getMinLevelForNewSkill(Player player, Map<Long, SkillLearn> skillTree)
	{
		int minLevel = 0;
		if (skillTree.isEmpty())
		{
			LOGGER.warning(getClass().getSimpleName() + ": SkillTree is not defined for getMinLevelForNewSkill!");
		}
		else
		{
			for (SkillLearn s : skillTree.values())
			{
				if ((player.getLevel() < s.getGetLevel()) && ((minLevel == 0) || (minLevel > s.getGetLevel())))
				{
					minLevel = s.getGetLevel();
				}
			}
		}
		return minLevel;
	}
	
	public Collection<SkillLearn> getNextAvailableSkills(Player player, PlayerClass classId, boolean includeByFs, boolean includeAutoGet)
	{
		final Map<Long, SkillLearn> completeClassSkillTree = getCompleteClassSkillTree(classId);
		final Set<SkillLearn> result = new HashSet<>();
		if (completeClassSkillTree.isEmpty())
		{
			return result;
		}
		
		final int minLevelForNewSkill = getMinLevelForNewSkill(player, completeClassSkillTree);
		if (minLevelForNewSkill > 0)
		{
			for (SkillLearn skill : completeClassSkillTree.values())
			{
				if (skill.getGetLevel() > Config.PLAYER_MAXIMUM_LEVEL)
				{
					continue;
				}
				
				if ((!includeAutoGet && skill.isAutoGet()) || (!includeByFs && skill.isLearnedByFS()))
				{
					continue;
				}
				
				if (minLevelForNewSkill <= skill.getGetLevel())
				{
					final Skill oldSkill = player.getKnownSkill(player.getReplacementSkill(skill.getSkillId()));
					if (oldSkill != null)
					{
						if (oldSkill.getLevel() == (skill.getSkillLevel() - 1))
						{
							result.add(skill);
						}
					}
					else if (skill.getSkillLevel() == 1)
					{
						result.add(skill);
					}
				}
			}
		}
		
		// Manage skill unlearn for player skills.
		for (Skill knownSkill : player.getSkillList())
		{
			SkillLearn skillLearn = getClassSkill(player.getOriginalSkill(knownSkill.getId()), knownSkill.getLevel(), classId);
			if (skillLearn == null)
			{
				continue;
			}
			
			Set<Integer> removeSkills = skillLearn.getRemoveSkills();
			if (removeSkills.isEmpty())
			{
				if (knownSkill.getLevel() > 1)
				{
					// Check first skill level for removed skills.
					skillLearn = getClassSkill(knownSkill.getId(), 1, classId);
					if (skillLearn == null)
					{
						continue;
					}
					
					removeSkills = skillLearn.getRemoveSkills();
					if (removeSkills.isEmpty())
					{
						continue;
					}
				}
				else
				{
					continue;
				}
			}
			
			for (int removeId : removeSkills)
			{
				SEARCH: for (SkillLearn knownLearn : result)
				{
					if (knownLearn.getSkillId() == removeId)
					{
						result.remove(knownLearn);
						break SEARCH;
					}
				}
			}
		}
		
		return result;
	}
	
	public void cleanSkillUponChangeClass(Player player)
	{
		PlayerClass currentClass = player.getPlayerClass();
		for (Skill skill : player.getAllSkills())
		{
			final int maxLevel = SkillData.getInstance().getMaxLevel(skill.getId());
			final long hashCode = SkillData.getSkillHashCode(skill.getId(), maxLevel);
			if (!isCurrentClassSkillNoParent(currentClass, hashCode) && !isRemoveSkill(currentClass, skill.getId()) && !isAwakenSaveSkill(currentClass, skill.getId()) && !isAlchemySkill(skill.getId(), skill.getLevel()))
			{
				// Do not remove equipped item skills.
				boolean isItemSkill = false;
				SEARCH: for (Item item : player.getInventory().getItems())
				{
					final List<ItemSkillHolder> itemSkills = item.getTemplate().getAllSkills();
					if (itemSkills != null)
					{
						for (ItemSkillHolder itemSkillHolder : itemSkills)
						{
							if (itemSkillHolder.getSkillId() == skill.getId())
							{
								isItemSkill = true;
								break SEARCH;
							}
						}
					}
				}
				if (!isItemSkill)
				{
					player.removeSkill(skill, true, true);
				}
			}
		}
		
		// Check previous classes as well, in case classes where skipped.
		while (currentClass.getParent() != null)
		{
			final Set<Integer> removedList = _removeSkillCache.get(currentClass);
			if (removedList != null)
			{
				for (Integer skillId : removedList)
				{
					final int currentLevel = player.getSkillLevel(skillId);
					if (currentLevel > 0)
					{
						player.removeSkill(SkillData.getInstance().getSkill(skillId, currentLevel));
					}
				}
			}
			currentClass = currentClass.getParent();
		}
	}
	
	public boolean isAlchemySkill(int skillId, int skillLevel)
	{
		return _alchemySkillTree.containsKey(SkillData.getSkillHashCode(skillId, skillLevel));
	}
	
	/**
	 * Checks if is hero skill.
	 * @param skillId the Id of the skill to check
	 * @param skillLevel the level of the skill to check, if it's -1 only Id will be checked
	 * @return {@code true} if the skill is present in the Hero Skill Tree, {@code false} otherwise
	 */
	public boolean isHeroSkill(int skillId, int skillLevel)
	{
		return _heroSkillTree.containsKey(SkillData.getSkillHashCode(skillId, skillLevel));
	}
	
	/**
	 * Checks if is GM skill.
	 * @param skillId the Id of the skill to check
	 * @param skillLevel the level of the skill to check, if it's -1 only Id will be checked
	 * @return {@code true} if the skill is present in the Game Master Skill Trees, {@code false} otherwise
	 */
	public boolean isGMSkill(int skillId, int skillLevel)
	{
		final long hashCode = SkillData.getSkillHashCode(skillId, skillLevel);
		return _gameMasterSkillTree.containsKey(hashCode) || _gameMasterAuraSkillTree.containsKey(hashCode);
	}
	
	/**
	 * Checks if a skill is a Clan skill.
	 * @param skillId the Id of the skill to check
	 * @param skillLevel the level of the skill to check
	 * @return {@code true} if the skill is present in the Clan or Subpledge Skill Trees, {@code false} otherwise
	 */
	public boolean isClanSkill(int skillId, int skillLevel)
	{
		final long hashCode = SkillData.getSkillHashCode(skillId, skillLevel);
		return _pledgeSkillTree.containsKey(hashCode) || _subPledgeSkillTree.containsKey(hashCode);
	}
	
	public boolean isRemoveSkill(PlayerClass classId, int skillId)
	{
		return _removeSkillCache.getOrDefault(classId, Collections.emptySet()).contains(skillId);
	}
	
	public boolean isCurrentClassSkillNoParent(PlayerClass classId, Long hashCode)
	{
		return _classSkillTrees.getOrDefault(classId, Collections.emptyMap()).containsKey(hashCode);
	}
	
	public boolean isAwakenSaveSkill(PlayerClass classId, int skillId)
	{
		return _awakeningSaveSkillTree.getOrDefault(classId, Collections.emptySet()).contains(skillId);
	}
	
	/**
	 * Adds the skills.
	 * @param gmchar the player to add the Game Master skills
	 * @param auraSkills if {@code true} it will add "GM Aura" skills, else will add the "GM regular" skills
	 */
	public void addSkills(Player gmchar, boolean auraSkills)
	{
		final Collection<SkillLearn> skills = auraSkills ? _gameMasterAuraSkillTree.values() : _gameMasterSkillTree.values();
		final SkillData st = SkillData.getInstance();
		for (SkillLearn sl : skills)
		{
			gmchar.addSkill(st.getSkill(sl.getSkillId(), sl.getSkillLevel()), false); // Don't Save GM skills to database
		}
	}
	
	/**
	 * Create and store hash values for skills for easy and fast checks.
	 */
	private void generateCheckArrays()
	{
		int index;
		long[] skillHashes;
		
		// Class-specific skills.
		Map<Long, SkillLearn> skillLearnMap;
		final Set<PlayerClass> classIdSet = _classSkillTrees.keySet();
		_skillsByClassIdHashCodes = new HashMap<>(classIdSet.size());
		for (PlayerClass classId : classIdSet)
		{
			index = 0;
			skillLearnMap = new HashMap<>(getCompleteClassSkillTree(classId));
			skillHashes = new long[skillLearnMap.size()];
			for (long skillHash : skillLearnMap.keySet())
			{
				skillHashes[index++] = skillHash;
			}
			skillLearnMap.clear();
			Arrays.sort(skillHashes);
			_skillsByClassIdHashCodes.put(classId.getId(), skillHashes);
		}
		
		// Race-specific skills from Fishing and Transformation skill trees.
		final List<Long> skillHashList = new LinkedList<>();
		_skillsByRaceHashCodes = new HashMap<>(Race.values().length);
		for (Race race : Race.values())
		{
			for (SkillLearn skillLearn : _fishingSkillTree.values())
			{
				if (skillLearn.getRaces().contains(race))
				{
					skillHashList.add(SkillData.getSkillHashCode(skillLearn.getSkillId(), skillLearn.getSkillLevel()));
				}
			}
			
			for (SkillLearn skillLearn : _transformSkillTree.values())
			{
				if (skillLearn.getRaces().contains(race))
				{
					skillHashList.add(SkillData.getSkillHashCode(skillLearn.getSkillId(), skillLearn.getSkillLevel()));
				}
			}
			
			index = 0;
			skillHashes = new long[skillHashList.size()];
			for (long skillHash : skillHashList)
			{
				skillHashes[index++] = skillHash;
			}
			Arrays.sort(skillHashes);
			_skillsByRaceHashCodes.put(race.ordinal(), skillHashes);
			skillHashList.clear();
		}
		
		// Skills available for all classes and races.
		for (SkillLearn skillLearn : _commonSkillTree.values())
		{
			if (skillLearn.getRaces().isEmpty())
			{
				skillHashList.add(SkillData.getSkillHashCode(skillLearn.getSkillId(), skillLearn.getSkillLevel()));
			}
		}
		
		for (SkillLearn skillLearn : _fishingSkillTree.values())
		{
			if (skillLearn.getRaces().isEmpty())
			{
				skillHashList.add(SkillData.getSkillHashCode(skillLearn.getSkillId(), skillLearn.getSkillLevel()));
			}
		}
		
		for (SkillLearn skillLearn : _transformSkillTree.values())
		{
			if (skillLearn.getRaces().isEmpty())
			{
				skillHashList.add(SkillData.getSkillHashCode(skillLearn.getSkillId(), skillLearn.getSkillLevel()));
			}
		}
		
		for (SkillLearn skillLearn : _collectSkillTree.values())
		{
			skillHashList.add(SkillData.getSkillHashCode(skillLearn.getSkillId(), skillLearn.getSkillLevel()));
		}
		
		for (SkillLearn skillLearn : _abilitySkillTree.values())
		{
			skillHashList.add(SkillData.getSkillHashCode(skillLearn.getSkillId(), skillLearn.getSkillLevel()));
		}
		
		for (SkillLearn skillLearn : _alchemySkillTree.values())
		{
			skillHashList.add(SkillData.getSkillHashCode(skillLearn.getSkillId(), skillLearn.getSkillLevel()));
		}
		
		_allSkillsHashCodes = new long[skillHashList.size()];
		int hashIndex = 0;
		for (long skillHash : skillHashList)
		{
			_allSkillsHashCodes[hashIndex++] = skillHash;
		}
		Arrays.sort(_allSkillsHashCodes);
	}
	
	/**
	 * Verify if the give skill is valid for the given player.<br>
	 * GM's skills are excluded for GM players
	 * @param player the player to verify the skill
	 * @param skill the skill to be verified
	 * @return {@code true} if the skill is allowed to the given player
	 */
	public boolean isSkillAllowed(Player player, Skill skill)
	{
		if (skill.isExcludedFromCheck())
		{
			return true;
		}
		
		if (player.isGM() && skill.isGMSkill())
		{
			return true;
		}
		
		// Prevent accidental skill remove during reload
		if (_loading)
		{
			return true;
		}
		
		final int maxLevel = SkillData.getInstance().getMaxLevel(skill.getId());
		final long hashCode = SkillData.getSkillHashCode(skill.getId(), Math.min(skill.getLevel(), maxLevel));
		if (Arrays.binarySearch(_skillsByClassIdHashCodes.get(player.getPlayerClass().getId()), hashCode) >= 0)
		{
			return true;
		}
		
		if (Arrays.binarySearch(_skillsByRaceHashCodes.get(player.getRace().ordinal()), hashCode) >= 0)
		{
			return true;
		}
		
		if (Arrays.binarySearch(_allSkillsHashCodes, hashCode) >= 0)
		{
			return true;
		}
		
		// Exclude Transfer Skills from this check.
		if (getTransferSkill(skill.getId(), Math.min(skill.getLevel(), maxLevel), player.getPlayerClass()) != null)
		{
			return true;
		}
		
		// Exclude Race skills from this check.
		if (getRaceSkill(skill.getId(), Math.min(skill.getLevel(), maxLevel), player.getRace()) != null)
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * Logs current Skill Trees skills count.
	 */
	private void report()
	{
		int classSkillTreeCount = 0;
		for (Map<Long, SkillLearn> classSkillTree : _classSkillTrees.values())
		{
			classSkillTreeCount += classSkillTree.size();
		}
		
		int transferSkillTreeCount = 0;
		for (Map<Long, SkillLearn> trasferSkillTree : _transferSkillTrees.values())
		{
			transferSkillTreeCount += trasferSkillTree.size();
		}
		
		int raceSkillTreeCount = 0;
		for (Map<Long, SkillLearn> raceSkillTree : _raceSkillTree.values())
		{
			raceSkillTreeCount += raceSkillTree.size();
		}
		
		int revelationSkillTreeCount = 0;
		for (Map<Long, SkillLearn> revelationSkillTree : _revelationSkillTree.values())
		{
			revelationSkillTreeCount += revelationSkillTree.size();
		}
		
		int dwarvenOnlyFishingSkillCount = 0;
		for (SkillLearn fishSkill : _fishingSkillTree.values())
		{
			if (fishSkill.getRaces().contains(Race.DWARF))
			{
				dwarvenOnlyFishingSkillCount++;
			}
		}
		
		int resSkillCount = 0;
		for (SkillLearn pledgeSkill : _pledgeSkillTree.values())
		{
			if (pledgeSkill.isResidencialSkill())
			{
				resSkillCount++;
			}
		}
		
		final String className = getClass().getSimpleName();
		LOGGER.info(className + ": Loaded " + classSkillTreeCount + " Class skills for " + _classSkillTrees.size() + " class skill trees.");
		LOGGER.info(className + ": Loaded " + _subClassSkillTree.size() + " sub-class skills.");
		LOGGER.info(className + ": Loaded " + _dualClassSkillTree.size() + " dual-class skills.");
		LOGGER.info(className + ": Loaded " + transferSkillTreeCount + " transfer skills for " + _transferSkillTrees.size() + " transfer skill trees.");
		LOGGER.info(className + ": Loaded " + raceSkillTreeCount + " race skills for " + _raceSkillTree.size() + " race skill trees.");
		LOGGER.info(className + ": Loaded " + _fishingSkillTree.size() + " fishing skills, " + dwarvenOnlyFishingSkillCount + " Dwarven only fishing skills.");
		LOGGER.info(className + ": Loaded " + _collectSkillTree.size() + " collect skills.");
		LOGGER.info(className + ": Loaded " + _pledgeSkillTree.size() + " clan skills, " + (_pledgeSkillTree.size() - resSkillCount) + " for clan and " + resSkillCount + " residential.");
		LOGGER.info(className + ": Loaded " + _subPledgeSkillTree.size() + " sub-pledge skills.");
		LOGGER.info(className + ": Loaded " + _transformSkillTree.size() + " transform skills.");
		LOGGER.info(className + ": Loaded " + _nobleSkillTree.size() + " noble skills.");
		LOGGER.info(className + ": Loaded " + _heroSkillTree.size() + " hero skills.");
		LOGGER.info(className + ": Loaded " + _gameMasterSkillTree.size() + " game master skills.");
		LOGGER.info(className + ": Loaded " + _gameMasterAuraSkillTree.size() + " game master aura skills.");
		LOGGER.info(className + ": Loaded " + _abilitySkillTree.size() + " ability skills.");
		LOGGER.info(className + ": Loaded " + _alchemySkillTree.size() + " alchemy skills.");
		LOGGER.info(className + ": Loaded " + _awakeningSaveSkillTree.size() + " class awaken save skills.");
		LOGGER.info(className + ": Loaded " + revelationSkillTreeCount + " Revelation skills.");
		
		final int commonSkills = _commonSkillTree.size();
		if (commonSkills > 0)
		{
			LOGGER.info(className + ": Loaded " + commonSkills + " common skills.");
		}
	}
	
	/**
	 * Gets the single instance of SkillTreesData.
	 * @return the only instance of this class
	 */
	public static SkillTreeData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	/**
	 * Singleton holder for the SkillTreesData class.
	 */
	private static class SingletonHolder
	{
		protected static final SkillTreeData INSTANCE = new SkillTreeData();
	}
}
