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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanRewardBonus;
import org.l2jmobius.gameserver.model.clan.enums.ClanRewardType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

/**
 * @author UnAfraid
 */
public class ClanRewardData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ClanRewardData.class.getName());
	private final Map<ClanRewardType, List<ClanRewardBonus>> _clanRewards = new ConcurrentHashMap<>();
	
	protected ClanRewardData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackFile("config/ClanReward.xml");
		for (ClanRewardType type : ClanRewardType.values())
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + (_clanRewards.containsKey(type) ? _clanRewards.get(type).size() : 0) + " rewards for " + type.toString().replace("_", " ").toLowerCase() + ".");
		}
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document.getFirstChild(), IXmlReader::isNode, listNode ->
		{
			switch (listNode.getNodeName())
			{
				case "membersOnline":
				{
					parseMembersOnline(listNode);
					break;
				}
				case "huntingBonus":
				{
					parseHuntingBonus(listNode);
					break;
				}
				case "raidArena":
				{
					parseRaidArenaBonus(listNode);
					break;
				}
			}
		});
	}
	
	private void parseMembersOnline(Node node)
	{
		forEach(node, IXmlReader::isNode, memberNode ->
		{
			if ("players".equalsIgnoreCase(memberNode.getNodeName()))
			{
				final NamedNodeMap attrs = memberNode.getAttributes();
				final int requiredAmount = parseInteger(attrs, "size");
				final int level = parseInteger(attrs, "level");
				final ClanRewardBonus bonus = new ClanRewardBonus(ClanRewardType.MEMBERS_ONLINE, level, requiredAmount);
				forEach(memberNode, IXmlReader::isNode, skillNode ->
				{
					if ("skill".equalsIgnoreCase(skillNode.getNodeName()))
					{
						final NamedNodeMap skillAttr = skillNode.getAttributes();
						final int skillId = parseInteger(skillAttr, "id");
						final int skillLevel = parseInteger(skillAttr, "level");
						bonus.setSkillReward(new SkillHolder(skillId, skillLevel));
					}
				});
				_clanRewards.computeIfAbsent(bonus.getType(), _ -> new ArrayList<>()).add(bonus);
			}
		});
	}
	
	private void parseHuntingBonus(Node node)
	{
		forEach(node, IXmlReader::isNode, memberNode ->
		{
			if ("hunting".equalsIgnoreCase(memberNode.getNodeName()))
			{
				final NamedNodeMap attrs = memberNode.getAttributes();
				final int requiredAmount = parseInteger(attrs, "points");
				final int level = parseInteger(attrs, "level");
				final ClanRewardBonus bonus = new ClanRewardBonus(ClanRewardType.HUNTING_MONSTERS, level, requiredAmount);
				forEach(memberNode, IXmlReader::isNode, itemsNode ->
				{
					if ("item".equalsIgnoreCase(itemsNode.getNodeName()))
					{
						final NamedNodeMap itemsAttr = itemsNode.getAttributes();
						final int id = parseInteger(itemsAttr, "id");
						final int count = parseInteger(itemsAttr, "count");
						bonus.setItemReward(new ItemHolder(id, count));
					}
				});
				_clanRewards.computeIfAbsent(bonus.getType(), _ -> new ArrayList<>()).add(bonus);
			}
		});
	}
	
	private void parseRaidArenaBonus(Node node)
	{
		forEach(node, IXmlReader::isNode, memberNode ->
		{
			if ("raid".equalsIgnoreCase(memberNode.getNodeName()))
			{
				final NamedNodeMap attrs = memberNode.getAttributes();
				final int requiredStage = parseInteger(attrs, "stage");
				final int level = parseInteger(attrs, "level");
				final ClanRewardBonus bonus = new ClanRewardBonus(ClanRewardType.RAID_ARENA, level, requiredStage);
				forEach(memberNode, IXmlReader::isNode, skillNode ->
				{
					if ("skill".equalsIgnoreCase(skillNode.getNodeName()))
					{
						final NamedNodeMap skillAttr = skillNode.getAttributes();
						final int skillId = parseInteger(skillAttr, "id");
						final int skillLevel = parseInteger(skillAttr, "level");
						bonus.setSkillReward(new SkillHolder(skillId, skillLevel));
					}
				});
				_clanRewards.computeIfAbsent(bonus.getType(), _ -> new ArrayList<>()).add(bonus);
			}
		});
	}
	
	public List<ClanRewardBonus> getClanRewardBonuses(ClanRewardType type)
	{
		return _clanRewards.get(type);
	}
	
	public ClanRewardBonus getHighestReward(ClanRewardType type)
	{
		ClanRewardBonus selectedBonus = null;
		for (ClanRewardBonus currentBonus : _clanRewards.get(type))
		{
			if ((selectedBonus == null) || (selectedBonus.getLevel() < currentBonus.getLevel()))
			{
				selectedBonus = currentBonus;
			}
		}
		return selectedBonus;
	}
	
	public void checkArenaProgress(Clan clan)
	{
		final List<ClanRewardBonus> rewards = _clanRewards.get(ClanRewardType.RAID_ARENA);
		if ((rewards == null) || rewards.isEmpty())
		{
			LOGGER.warning("No RAID_ARENA rewards defined.");
			return;
		}
		
		clan.removeSkill(55887); // Clan Exuberance.
		
		final int progress = clan.getVariables().getInt("MAX_RAID_LEVEL", 0);
		
		final ListIterator<ClanRewardBonus> it = rewards.listIterator();
		ClanRewardBonus selectedReward = null;
		while (it.hasNext())
		{
			ClanRewardBonus reward = it.next();
			if (reward.getLevel() <= progress)
			{
				selectedReward = reward;
			}
			else
			{
				break;
			}
		}
		
		if (selectedReward != null)
		{
			addClanSkillWithLevel(clan, 55887, progress);
			LOGGER.info("Arena Level: " + progress + " for clan " + clan.getName());
		}
	}
	
	public void addClanSkillWithLevel(Clan clan, int skillId, int progress)
	{
		int skillLevel = 1; // Default skill level.
		if (progress >= 25)
		{
			skillLevel = 5;
		}
		else if (progress >= 20)
		{
			skillLevel = 4;
		}
		else if (progress >= 15)
		{
			skillLevel = 3;
		}
		else if (progress >= 10)
		{
			skillLevel = 2;
		}
		
		final Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
		if (skill != null)
		{
			clan.addNewSkill(skill);
			LOGGER.info("Added skill " + skill.getName() + " with level " + skillLevel + " to clan " + clan.getName());
		}
		else
		{
			LOGGER.warning("Skill with ID " + skillId + " and level " + skillLevel + " not found.");
		}
	}
	
	public Collection<List<ClanRewardBonus>> getClanRewardBonuses()
	{
		return _clanRewards.values();
	}
	
	/**
	 * Gets the single instance of ClanRewardData.
	 * @return single instance of ClanRewardData
	 */
	public static ClanRewardData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ClanRewardData INSTANCE = new ClanRewardData();
	}
}
