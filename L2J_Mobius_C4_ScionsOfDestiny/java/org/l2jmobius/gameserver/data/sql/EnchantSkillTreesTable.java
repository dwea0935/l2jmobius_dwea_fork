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
package org.l2jmobius.gameserver.data.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.model.EnchantSkillLearn;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * @author Mobius
 * @implNote Adapted from old C6 project
 */
public class EnchantSkillTreesTable
{
	private static final Logger LOGGER = Logger.getLogger(EnchantSkillTreesTable.class.getName());
	
	private final List<EnchantSkillLearn> _enchantSkillTrees = new ArrayList<>();
	
	protected EnchantSkillTreesTable()
	{
		load();
	}
	
	private void load()
	{
		_enchantSkillTrees.clear();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM enchant_skill_trees ORDER BY skill_id, level");
			ResultSet skilltree = statement.executeQuery())
		{
			int prevSkillId = -1;
			while (skilltree.next())
			{
				final int id = skilltree.getInt("skill_id");
				final int lvl = skilltree.getInt("level");
				final String name = skilltree.getString("name");
				final int baseLevel = skilltree.getInt("base_lvl");
				final int minskillLevel = skilltree.getInt("min_skill_lvl");
				final int sp = skilltree.getInt("sp");
				final int exp = skilltree.getInt("exp");
				final byte rate76 = skilltree.getByte("success_rate76");
				final byte rate77 = skilltree.getByte("success_rate77");
				final byte rate78 = skilltree.getByte("success_rate78");
				final byte rate79 = skilltree.getByte("success_rate79");
				final byte rate80 = skilltree.getByte("success_rate80");
				if (prevSkillId != id)
				{
					prevSkillId = id;
				}
				_enchantSkillTrees.add(new EnchantSkillLearn(id, lvl, minskillLevel, baseLevel, name, sp, exp, rate76, rate77, rate78, rate79, rate80));
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("EnchantSkillTreesTable: Error while creating enchant skill table " + e);
		}
		
		LOGGER.info("EnchantSkillTreesTable: Loaded " + _enchantSkillTrees.size() + " enchant skills.");
	}
	
	public List<EnchantSkillLearn> getAvailableEnchantSkills(Player player)
	{
		if (player.getLevel() < 76)
		{
			return Collections.emptyList();
		}
		
		final List<EnchantSkillLearn> result = new ArrayList<>();
		final List<EnchantSkillLearn> skills = new ArrayList<>();
		skills.addAll(_enchantSkillTrees);
		
		final Collection<Skill> knownSkills = player.getAllSkills();
		for (EnchantSkillLearn skillLearn : skills)
		{
			SEARCH: for (Skill skill : knownSkills)
			{
				if (skill.getId() == skillLearn.getId())
				{
					if (skill.getLevel() == skillLearn.getMinSkillLevel())
					{
						// this is the next level of a skill that we know
						result.add(skillLearn);
					}
					break SEARCH;
				}
			}
		}
		return result;
	}
	
	public int getSkillSpCost(Player player, Skill skill)
	{
		int skillCost = 100000000;
		for (EnchantSkillLearn enchantSkillLearn : getAvailableEnchantSkills(player))
		{
			if (enchantSkillLearn.getId() != skill.getId())
			{
				continue;
			}
			
			if (enchantSkillLearn.getLevel() != skill.getLevel())
			{
				continue;
			}
			
			if (76 > player.getLevel())
			{
				continue;
			}
			
			skillCost = enchantSkillLearn.getSpCost();
		}
		return skillCost;
	}
	
	public int getSkillExpCost(Player player, Skill skill)
	{
		int skillCost = 100000000;
		for (EnchantSkillLearn enchantSkillLearn : getAvailableEnchantSkills(player))
		{
			if (enchantSkillLearn.getId() != skill.getId())
			{
				continue;
			}
			
			if (enchantSkillLearn.getLevel() != skill.getLevel())
			{
				continue;
			}
			
			if (76 > player.getLevel())
			{
				continue;
			}
			
			skillCost = enchantSkillLearn.getExp();
		}
		return skillCost;
	}
	
	public byte getSkillRate(Player player, Skill skill)
	{
		for (EnchantSkillLearn enchantSkillLearn : getAvailableEnchantSkills(player))
		{
			if (enchantSkillLearn.getId() != skill.getId())
			{
				continue;
			}
			
			if (enchantSkillLearn.getLevel() != skill.getLevel())
			{
				continue;
			}
			
			return enchantSkillLearn.getRate(player);
		}
		return 0;
	}
	
	public static EnchantSkillTreesTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EnchantSkillTreesTable INSTANCE = new EnchantSkillTreesTable();
	}
}
