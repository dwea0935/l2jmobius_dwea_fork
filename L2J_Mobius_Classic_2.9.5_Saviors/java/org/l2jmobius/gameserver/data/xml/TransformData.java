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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.transform.AdditionalItemHolder;
import org.l2jmobius.gameserver.model.actor.transform.AdditionalSkillHolder;
import org.l2jmobius.gameserver.model.actor.transform.Transform;
import org.l2jmobius.gameserver.model.actor.transform.TransformLevelData;
import org.l2jmobius.gameserver.model.actor.transform.TransformTemplate;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

/**
 * @author UnAfraid
 */
public class TransformData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(TransformData.class.getName());
	
	private final Map<Integer, Transform> _transformData = new ConcurrentHashMap<>();
	
	protected TransformData()
	{
		load();
	}
	
	@Override
	public synchronized void load()
	{
		_transformData.clear();
		parseDatapackDirectory("data/stats/transformations", false);
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _transformData.size() + " transform templates.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("transform".equalsIgnoreCase(d.getNodeName()))
					{
						NamedNodeMap attrs = d.getAttributes();
						final StatSet set = new StatSet();
						for (int i = 0; i < attrs.getLength(); i++)
						{
							final Node att = attrs.item(i);
							set.set(att.getNodeName(), att.getNodeValue());
						}
						final Transform transform = new Transform(set);
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							final boolean isMale = "Male".equalsIgnoreCase(cd.getNodeName());
							if ("Male".equalsIgnoreCase(cd.getNodeName()) || "Female".equalsIgnoreCase(cd.getNodeName()))
							{
								TransformTemplate templateData = null;
								for (Node z = cd.getFirstChild(); z != null; z = z.getNextSibling())
								{
									switch (z.getNodeName())
									{
										case "common":
										{
											for (Node s = z.getFirstChild(); s != null; s = s.getNextSibling())
											{
												switch (s.getNodeName())
												{
													case "base":
													case "stats":
													case "defense":
													case "magicDefense":
													case "collision":
													case "moving":
													{
														attrs = s.getAttributes();
														for (int i = 0; i < attrs.getLength(); i++)
														{
															final Node att = attrs.item(i);
															set.set(att.getNodeName(), att.getNodeValue());
														}
														break;
													}
												}
											}
											templateData = new TransformTemplate(set);
											transform.setTemplate(isMale, templateData);
											break;
										}
										case "skills":
										{
											if (templateData == null)
											{
												templateData = new TransformTemplate(set);
												transform.setTemplate(isMale, templateData);
											}
											for (Node s = z.getFirstChild(); s != null; s = s.getNextSibling())
											{
												if ("skill".equals(s.getNodeName()))
												{
													attrs = s.getAttributes();
													final int skillId = parseInteger(attrs, "id");
													final int skillLevel = parseInteger(attrs, "level");
													templateData.addSkill(new SkillHolder(skillId, skillLevel));
												}
											}
											break;
										}
										case "actions":
										{
											if (templateData == null)
											{
												templateData = new TransformTemplate(set);
												transform.setTemplate(isMale, templateData);
											}
											set.set("actions", z.getTextContent());
											final int[] actions = set.getIntArray("actions", " ");
											templateData.setBasicActionList(actions);
											break;
										}
										case "additionalSkills":
										{
											if (templateData == null)
											{
												templateData = new TransformTemplate(set);
												transform.setTemplate(isMale, templateData);
											}
											for (Node s = z.getFirstChild(); s != null; s = s.getNextSibling())
											{
												if ("skill".equals(s.getNodeName()))
												{
													attrs = s.getAttributes();
													final int skillId = parseInteger(attrs, "id");
													final int skillLevel = parseInteger(attrs, "level");
													final int minLevel = parseInteger(attrs, "minLevel");
													templateData.addAdditionalSkill(new AdditionalSkillHolder(skillId, skillLevel, minLevel));
												}
											}
											break;
										}
										case "items":
										{
											if (templateData == null)
											{
												templateData = new TransformTemplate(set);
												transform.setTemplate(isMale, templateData);
											}
											for (Node s = z.getFirstChild(); s != null; s = s.getNextSibling())
											{
												if ("item".equals(s.getNodeName()))
												{
													attrs = s.getAttributes();
													final int itemId = parseInteger(attrs, "id");
													final boolean allowed = parseBoolean(attrs, "allowed");
													templateData.addAdditionalItem(new AdditionalItemHolder(itemId, allowed));
												}
											}
											break;
										}
										case "levels":
										{
											if (templateData == null)
											{
												templateData = new TransformTemplate(set);
												transform.setTemplate(isMale, templateData);
											}
											
											final StatSet levelsSet = new StatSet();
											for (Node s = z.getFirstChild(); s != null; s = s.getNextSibling())
											{
												if ("level".equals(s.getNodeName()))
												{
													attrs = s.getAttributes();
													for (int i = 0; i < attrs.getLength(); i++)
													{
														final Node att = attrs.item(i);
														levelsSet.set(att.getNodeName(), att.getNodeValue());
													}
												}
											}
											templateData.addLevelData(new TransformLevelData(levelsSet));
											break;
										}
									}
								}
							}
						}
						_transformData.put(transform.getId(), transform);
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves the {@link Transform} instance associated with the specified ID.
	 * @param id the unique identifier of the transform.
	 * @return the {@link Transform} instance associated with the ID, or {@code null} if not found.
	 */
	public Transform getTransform(int id)
	{
		return _transformData.get(id);
	}
	
	public static TransformData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final TransformData INSTANCE = new TransformData();
	}
}
