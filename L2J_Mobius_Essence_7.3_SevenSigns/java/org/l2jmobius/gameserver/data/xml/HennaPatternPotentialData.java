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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.item.henna.DyePotential;
import org.l2jmobius.gameserver.model.item.henna.DyePotentialFee;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * @author Serenitty
 */
public class HennaPatternPotentialData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(HennaPatternPotentialData.class.getName());
	
	private final Map<Integer, Integer> _potenExpTable = new HashMap<>();
	private final Map<Integer, DyePotentialFee> _potenFees = new HashMap<>();
	private final Map<Integer, DyePotential> _potentials = new HashMap<>();
	private final List<ItemHolder> _enchancedReset = new ArrayList<>();
	
	private int MAX_POTEN_LEVEL = 0;
	private int MAX_POTEN_EXP = 0;
	
	protected HennaPatternPotentialData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_potenFees.clear();
		_potenExpTable.clear();
		_potentials.clear();
		_enchancedReset.clear();
		parseDatapackFile("data/stats/hennaPatternPotential.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _potenFees.size() + " dye pattern fee data.");
		
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node m = document.getFirstChild(); m != null; m = m.getNextSibling())
		{
			if ("list".equals(m.getNodeName()))
			{
				for (Node k = m.getFirstChild(); k != null; k = k.getNextSibling())
				{
					switch (k.getNodeName())
					{
						case "enchantFees":
						{
							for (Node n = k.getFirstChild(); n != null; n = n.getNextSibling())
							{
								if ("fee".equals(n.getNodeName()))
								{
									NamedNodeMap attrs = n.getAttributes();
									Node att;
									final StatSet set = new StatSet();
									for (int i = 0; i < attrs.getLength(); i++)
									{
										att = attrs.item(i);
										set.set(att.getNodeName(), att.getNodeValue());
									}
									
									final int step = parseInteger(attrs, "step");
									int itemId = 0;
									long itemCount = 0;
									int dailyCount = 0;
									final Map<Integer, Double> enchantExp = new HashMap<>();
									final List<ItemHolder> items = new ArrayList<>();
									for (Node b = n.getFirstChild(); b != null; b = b.getNextSibling())
									{
										attrs = b.getAttributes();
										switch (b.getNodeName())
										{
											case "requiredItem":
											{
												itemId = parseInteger(attrs, "id");
												itemCount = parseLong(attrs, "count", 1L);
												items.add(new ItemHolder(itemId, itemCount));
												break;
											}
											case "dailyCount":
											{
												dailyCount = Integer.parseInt(b.getTextContent());
												break;
											}
											case "enchantExp":
											{
												enchantExp.put(parseInteger(attrs, "count"), parseDouble(attrs, "chance"));
												break;
											}
										}
									}
									_potenFees.put(step, new DyePotentialFee(step, items, dailyCount, enchantExp));
								}
							}
							break;
						}
						case "resetCount":
						{
							for (Node n = k.getFirstChild(); n != null; n = n.getNextSibling())
							{
								if ("reset".equalsIgnoreCase(n.getNodeName()))
								{
									final StatSet set = new StatSet(parseAttributes(n));
									final int itemId = set.getInt("itemid");
									final int itemCount = set.getInt("count");
									if (ItemData.getInstance().getTemplate(itemId) == null)
									{
										LOGGER.info(getClass().getSimpleName() + ": Item with id " + itemId + " does not exist.");
									}
									else
									{
										_enchancedReset.add(new ItemHolder(itemId, itemCount));
									}
								}
							}
							break;
						}
						case "experiencePoints":
						{
							for (Node n = k.getFirstChild(); n != null; n = n.getNextSibling())
							{
								if ("hiddenPower".equals(n.getNodeName()))
								{
									NamedNodeMap attrs = n.getAttributes();
									Node att;
									final StatSet set = new StatSet();
									for (int i = 0; i < attrs.getLength(); i++)
									{
										att = attrs.item(i);
										set.set(att.getNodeName(), att.getNodeValue());
									}
									
									final int level = parseInteger(attrs, "level");
									final int exp = parseInteger(attrs, "exp");
									_potenExpTable.put(level, exp);
									if (MAX_POTEN_LEVEL < level)
									{
										MAX_POTEN_LEVEL = level;
									}
									if (MAX_POTEN_EXP < exp)
									{
										MAX_POTEN_EXP = exp;
									}
								}
							}
							break;
						}
						case "hiddenPotentials":
						{
							for (Node n = k.getFirstChild(); n != null; n = n.getNextSibling())
							{
								if ("poten".equals(n.getNodeName()))
								{
									NamedNodeMap attrs = n.getAttributes();
									Node att;
									final StatSet set = new StatSet();
									for (int i = 0; i < attrs.getLength(); i++)
									{
										att = attrs.item(i);
										set.set(att.getNodeName(), att.getNodeValue());
									}
									
									final int id = parseInteger(attrs, "id");
									final int slotId = parseInteger(attrs, "slotId");
									final int maxSkillLevel = parseInteger(attrs, "maxSkillLevel");
									final int skillId = parseInteger(attrs, "skillId");
									_potentials.put(id, new DyePotential(id, slotId, skillId, maxSkillLevel));
								}
							}
							break;
						}
					}
				}
			}
		}
	}
	
	public DyePotentialFee getFee(int step)
	{
		return _potenFees.get(step);
	}
	
	public int getMaxPotenEnchantStep()
	{
		return _potenFees.size();
	}
	
	public List<ItemHolder> getEnchantReset()
	{
		return _enchancedReset;
	}
	
	public int getExpForLevel(int level)
	{
		return _potenExpTable.get(level);
	}
	
	public int getMaxPotenLevel()
	{
		return MAX_POTEN_LEVEL;
	}
	
	public int getMaxPotenExp()
	{
		return MAX_POTEN_EXP;
	}
	
	public DyePotential getPotential(int potenId)
	{
		return _potentials.get(potenId);
	}
	
	public Skill getPotentialSkill(int potenId, int slotId, int level)
	{
		final DyePotential potential = _potentials.get(potenId);
		if (potential == null)
		{
			return null;
		}
		if (potential.getSlotId() == slotId)
		{
			return potential.getSkill(level);
		}
		return null;
	}
	
	public Collection<Integer> getSkillIdsBySlotId(int slotId)
	{
		final List<Integer> skillIds = new ArrayList<>();
		for (DyePotential potential : _potentials.values())
		{
			if (potential.getSlotId() == slotId)
			{
				skillIds.add(potential.getSkillId());
			}
		}
		return skillIds;
	}
	
	public static HennaPatternPotentialData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final HennaPatternPotentialData INSTANCE = new HennaPatternPotentialData();
	}
}