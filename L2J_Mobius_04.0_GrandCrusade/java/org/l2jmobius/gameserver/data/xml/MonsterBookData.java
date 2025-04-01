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
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.MonsterBookCardHolder;
import org.l2jmobius.gameserver.data.holders.MonsterBookRewardHolder;
import org.l2jmobius.gameserver.model.quest.Faction;

/**
 * @author Mobius
 */
public class MonsterBookData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(MonsterBookData.class.getName());
	private final List<MonsterBookCardHolder> _monsterBook = new ArrayList<>();
	
	protected MonsterBookData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_monsterBook.clear();
		parseDatapackFile("data/MonsterBook.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _monsterBook.size() + " monster data.");
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
					if ("card".equalsIgnoreCase(d.getNodeName()))
					{
						final NamedNodeMap attrs = d.getAttributes();
						final int itemId = parseInteger(attrs, "id");
						final int monster = parseInteger(attrs, "monster");
						final String faction = parseString(attrs, "faction");
						final MonsterBookCardHolder card = new MonsterBookCardHolder(itemId, monster, Faction.valueOf(faction));
						if (NpcData.getInstance().getTemplate(monster) == null)
						{
							LOGGER.severe(getClass().getSimpleName() + ": Could not find NPC template with id " + monster + ".");
						}
						
						for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
						{
							if ("rewards".equalsIgnoreCase(b.getNodeName()))
							{
								final NamedNodeMap rewardAttrs = b.getAttributes();
								final int kills = parseInteger(rewardAttrs, "kills");
								final Long exp = parseLong(rewardAttrs, "exp");
								final int sp = parseInteger(rewardAttrs, "sp");
								final int points = parseInteger(rewardAttrs, "points");
								card.addReward(new MonsterBookRewardHolder(kills, exp, sp, points));
							}
						}
						
						_monsterBook.add(card);
					}
				}
			}
		}
	}
	
	public List<MonsterBookCardHolder> getMonsterBookCards()
	{
		return _monsterBook;
	}
	
	public MonsterBookCardHolder getMonsterBookCardByMonsterId(int monsterId)
	{
		for (MonsterBookCardHolder card : _monsterBook)
		{
			if (card.getMonsterId() == monsterId)
			{
				return card;
			}
		}
		return null;
	}
	
	public MonsterBookCardHolder getMonsterBookCardById(int cardId)
	{
		for (MonsterBookCardHolder card : _monsterBook)
		{
			if (card.getId() == cardId)
			{
				return card;
			}
		}
		return null;
	}
	
	public static MonsterBookData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final MonsterBookData INSTANCE = new MonsterBookData();
	}
}
