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
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.costumes.Costume;
import org.l2jmobius.gameserver.model.costumes.CostumeGrade;
import org.l2jmobius.gameserver.model.costumes.Costumes;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * @author GolbergSoft
 */
public class CostumesData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(CostumesData.class.getName());
	
	private final Map<Integer, Costume> _costumes;
	private final EnumMap<CostumeGrade, Set<Costume>> _costumesGrade;
	
	@Override
	public void load()
	{
	}
	
	public void parseCostumeDocument()
	{
		parseDatapackFile("data/Costumes.xml");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode ->
		{
			for (Node node = listNode.getFirstChild(); node != null; node = node.getNextSibling())
			{
				if ("costume".equals(node.getNodeName()))
				{
					parseCostume(node);
				}
			}
		});
	}
	
	private void parseCostume(Node node)
	{
		final NamedNodeMap attrs = node.getAttributes();
		final int id = parseInteger(attrs, "id");
		final int skillId = parseInteger(attrs, "skill");
		final int evolutionFee = parseInteger(attrs, "evolution-fee");
		
		final Node extractNode = Costumes.findChildNodeByName(node, "extract");
		final int extractItem = parseInteger(extractNode.getAttributes(), "item");
		final Set<ItemHolder> extractCost = Costumes.parseExtractCost(extractNode);
		final Skill skill = SkillData.getInstance().getSkill(skillId, 1);
		
		if (skill != null)
		{
			final Costume costume = new Costume(id, skill, evolutionFee, extractItem, extractCost);
			_costumes.put(id, costume);
			final CostumeGrade grade = parseEnum(attrs, CostumeGrade.class, "grade", CostumeGrade.STANDARD);
			_costumesGrade.computeIfAbsent(grade, _ -> new HashSet<>()).add(costume);
		}
		else
		{
			LOGGER.warning("Skill with ID " + skillId + " not found, costume not added.");
		}
	}
	
	public CostumesData(Map<Integer, Costume> costumes, EnumMap<CostumeGrade, Set<Costume>> costumesGrade)
	{
		_costumes = costumes;
		_costumesGrade = costumesGrade;
	}
}
