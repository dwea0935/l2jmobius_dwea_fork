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
package ai.others.NpcBuffers;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;

/**
 * @author UnAfraid
 */
class NpcBuffersData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(NpcBuffersData.class.getName());
	
	private final Map<Integer, NpcBufferData> _npcBuffers = new HashMap<>();
	
	protected NpcBuffersData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackFile("data/scripts/ai/others/NpcBuffers/NpcBuffersData.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _npcBuffers.size() + " buffers data.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		StatSet set;
		Node attr;
		NamedNodeMap attrs;
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("npc".equalsIgnoreCase(d.getNodeName()))
					{
						attrs = d.getAttributes();
						final int npcId = parseInteger(attrs, "id");
						final NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
						if (template == null)
						{
							LOGGER.warning(getClass().getSimpleName() + ": Could not find NPC with id " + npcId);
							continue;
						}
						
						final NpcBufferData npc = new NpcBufferData(npcId);
						for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
						{
							switch (c.getNodeName())
							{
								case "skill":
								{
									attrs = c.getAttributes();
									set = new StatSet();
									for (int i = 0; i < attrs.getLength(); i++)
									{
										attr = attrs.item(i);
										set.set(attr.getNodeName(), attr.getNodeValue());
									}
									
									final NpcBufferSkillData data = new NpcBufferSkillData(set);
									if (data.getSkill() == null)
									{
										LOGGER.warning(getClass().getSimpleName() + ": Could not find skill with id " + set.getInt("id"));
										continue;
									}
									
									npc.addSkill(data);
									break;
								}
							}
						}
						_npcBuffers.put(npcId, npc);
					}
				}
			}
		}
	}
	
	NpcBufferData getNpcBuffer(int npcId)
	{
		return _npcBuffers.get(npcId);
	}
	
	public Collection<NpcBufferData> getNpcBuffers()
	{
		return _npcBuffers.values();
	}
	
	public Set<Integer> getNpcBufferIds()
	{
		return _npcBuffers.keySet();
	}
}
