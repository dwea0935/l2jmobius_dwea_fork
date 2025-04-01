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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * @author GolbergSoft
 */
public class CostumesCollectionSkillsData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(CostumesCollectionSkillsData.class.getName());
	private final Map<Integer, Set<Skill>> _stackedBonus;
	
	@Override
	public void load()
	{
	}
	
	public void parseCollectionStackBonusDocument()
	{
		parseDatapackFile("data/CostumesCollectionSkills.xml");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode ->
		{
			for (Node node = listNode.getFirstChild(); node != null; node = node.getNextSibling())
			{
				if ("stack-bonus".equals(node.getNodeName()))
				{
					parseCollectionStackBonus(node);
				}
			}
		});
	}
	
	private void parseCollectionStackBonus(Node node)
	{
		Integer count = parseInteger(node.getAttributes(), "count");
		if (count == null)
		{
			LOGGER.log(Level.SEVERE, "Failed to parse count attribute for stack-bonus node.");
			return;
		}
		
		Set<Skill> bonus = new HashSet<>();
		NodeList skillNodes = node.getChildNodes();
		for (int i = 0; i < skillNodes.getLength(); i++)
		{
			Node skillNode = skillNodes.item(i);
			if ("skill".equalsIgnoreCase(skillNode.getNodeName()))
			{
				NamedNodeMap attr = skillNode.getAttributes();
				Integer skillId = parseInteger(attr, "id");
				Integer skillLevel = parseInteger(attr, "level", 1); // Default level to 1 if not specified
				if (skillId != null)
				{
					Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
					if (skill != null)
					{
						bonus.add(skill);
					}
					else
					{
						LOGGER.warning("Skill ID " + skillId + " with level " + skillLevel + " not found.");
					}
				}
				else
				{
					LOGGER.warning("Skill ID not found for skill node.");
				}
			}
		}
		_stackedBonus.put(count, bonus);
	}
	
	public CostumesCollectionSkillsData(Map<Integer, Set<Skill>> stackedBonus)
	{
		_stackedBonus = stackedBonus;
	}
}
