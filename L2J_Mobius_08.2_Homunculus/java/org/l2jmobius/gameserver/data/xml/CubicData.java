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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.templates.CubicTemplate;
import org.l2jmobius.gameserver.model.cubic.CubicSkill;
import org.l2jmobius.gameserver.model.cubic.ICubicConditionHolder;
import org.l2jmobius.gameserver.model.cubic.conditions.HealthCondition;
import org.l2jmobius.gameserver.model.cubic.conditions.HpCondition;
import org.l2jmobius.gameserver.model.cubic.conditions.HpCondition.HpConditionType;
import org.l2jmobius.gameserver.model.cubic.conditions.RangeCondition;

/**
 * @author UnAfraid
 */
public class CubicData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(CubicData.class.getName());
	
	private final Map<Integer, Map<Integer, CubicTemplate>> _cubics = new ConcurrentHashMap<>();
	
	protected CubicData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_cubics.clear();
		parseDatapackDirectory("data/stats/cubics", true);
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _cubics.size() + " cubics.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "cubic", cubicNode -> parseTemplate(cubicNode, new CubicTemplate(new StatSet(parseAttributes(cubicNode))))));
	}
	
	private void parseTemplate(Node cubicNode, CubicTemplate template)
	{
		forEach(cubicNode, IXmlReader::isNode, innerNode ->
		{
			switch (innerNode.getNodeName())
			{
				case "conditions":
				{
					parseConditions(innerNode, template, template);
					break;
				}
				case "skills":
				{
					parseSkills(innerNode, template);
					break;
				}
			}
		});
		_cubics.computeIfAbsent(template.getId(), _ -> new HashMap<>()).put(template.getLevel(), template);
	}
	
	private void parseConditions(Node cubicNode, CubicTemplate template, ICubicConditionHolder holder)
	{
		forEach(cubicNode, IXmlReader::isNode, conditionNode ->
		{
			switch (conditionNode.getNodeName())
			{
				case "hp":
				{
					final HpConditionType type = parseEnum(conditionNode.getAttributes(), HpConditionType.class, "type");
					final int hpPer = parseInteger(conditionNode.getAttributes(), "percent");
					holder.addCondition(new HpCondition(type, hpPer));
					break;
				}
				case "range":
				{
					final int range = parseInteger(conditionNode.getAttributes(), "value");
					holder.addCondition(new RangeCondition(range));
					break;
				}
				case "healthPercent":
				{
					final int min = parseInteger(conditionNode.getAttributes(), "min");
					final int max = parseInteger(conditionNode.getAttributes(), "max");
					holder.addCondition(new HealthCondition(min, max));
					break;
				}
				default:
				{
					LOGGER.warning("Attempting to use not implemented condition: " + conditionNode.getNodeName() + " for cubic id: " + template.getId() + " level: " + template.getLevel());
					break;
				}
			}
		});
	}
	
	private void parseSkills(Node cubicNode, CubicTemplate template)
	{
		forEach(cubicNode, "skill", skillNode ->
		{
			final CubicSkill skill = new CubicSkill(new StatSet(parseAttributes(skillNode)));
			forEach(cubicNode, "conditions", _ -> parseConditions(cubicNode, template, skill));
			template.getCubicSkills().add(skill);
		});
	}
	
	/**
	 * Retrieves the cubic template for a specified cubic ID and level.
	 * @param id the ID of the cubic
	 * @param level the level of the cubic
	 * @return the {@link CubicTemplate} associated with the given ID and level, or {@code null} if no template is found
	 */
	public CubicTemplate getCubicTemplate(int id, int level)
	{
		return _cubics.getOrDefault(id, Collections.emptyMap()).get(level);
	}
	
	public static CubicData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CubicData INSTANCE = new CubicData();
	}
}
