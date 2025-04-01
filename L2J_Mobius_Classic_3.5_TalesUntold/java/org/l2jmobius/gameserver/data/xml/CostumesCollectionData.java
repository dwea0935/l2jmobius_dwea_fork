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
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.costumes.CostumeCollection;
import org.l2jmobius.gameserver.model.costumes.Costumes;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * @author GolbergSoft
 */
public class CostumesCollectionData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(CostumesCollectionData.class.getName());
	
	private final Map<Integer, CostumeCollection> _collections;
	
	@Override
	public void load()
	{
	}
	
	public void parseCollectionDocument()
	{
		parseDatapackFile("data/CostumesCollections.xml");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode ->
		{
			for (Node node = listNode.getFirstChild(); node != null; node = node.getNextSibling())
			{
				if ("collection".equals(node.getNodeName()))
				{
					parseCollection(node);
				}
			}
		});
	}
	
	private void parseCollection(Node node)
	{
		final NamedNodeMap attrs = node.getAttributes();
		final int id = parseInteger(attrs, "id");
		final int skillId = parseInteger(attrs, "skill");
		final Set<Integer> costumes = Costumes.parseIntSet(node.getFirstChild());
		final Skill skill = SkillData.getInstance().getSkill(skillId, 1);
		if (skill != null)
		{
			_collections.put(id, new CostumeCollection(id, skill, costumes));
		}
		else
		{
			LOGGER.warning("Skill ID " + skillId + " not found, collection not added.");
		}
	}
	
	public CostumesCollectionData(Map<Integer, CostumeCollection> collections)
	{
		_collections = collections;
	}
}
