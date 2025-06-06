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
package org.l2jmobius.gameserver.model.actor.transform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.item.type.WeaponType;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.stats.Stat;

/**
 * @author UnAfraid
 */
public class TransformTemplate
{
	private final Float _collisionRadius;
	private final Float _collisionHeight;
	private final WeaponType _baseAttackType;
	private List<SkillHolder> _skills;
	private List<AdditionalSkillHolder> _additionalSkills;
	private List<AdditionalItemHolder> _additionalItems;
	private Map<Integer, Integer> _baseDefense;
	private Map<Integer, Double> _baseStats;
	private int[] _actions;
	private final Map<Integer, TransformLevelData> _data = new LinkedHashMap<>(100);
	
	public TransformTemplate(StatSet set)
	{
		_collisionRadius = set.contains("radius") ? set.getFloat("radius") : null;
		_collisionHeight = set.contains("height") ? set.getFloat("height") : null;
		_baseAttackType = set.getEnum("attackType", WeaponType.class, null);
		if (set.contains("range"))
		{
			addStats(Stat.PHYSICAL_ATTACK_RANGE, set.getDouble("range", 0));
		}
		if (set.contains("randomDamage"))
		{
			addStats(Stat.RANDOM_DAMAGE, set.getDouble("randomDamage", 0));
		}
		if (set.contains("walk"))
		{
			addStats(Stat.WALK_SPEED, set.getDouble("walk", 0));
		}
		if (set.contains("run"))
		{
			addStats(Stat.RUN_SPEED, set.getDouble("run", 0));
		}
		if (set.contains("waterWalk"))
		{
			addStats(Stat.SWIM_WALK_SPEED, set.getDouble("waterWalk", 0));
		}
		if (set.contains("waterRun"))
		{
			addStats(Stat.SWIM_RUN_SPEED, set.getDouble("waterRun", 0));
		}
		if (set.contains("flyWalk"))
		{
			addStats(Stat.FLY_WALK_SPEED, set.getDouble("flyWalk", 0));
		}
		if (set.contains("flyRun"))
		{
			addStats(Stat.FLY_RUN_SPEED, set.getDouble("flyRun", 0));
		}
		if (set.contains("pAtk"))
		{
			addStats(Stat.PHYSICAL_ATTACK, set.getDouble("pAtk", 0));
		}
		if (set.contains("mAtk"))
		{
			addStats(Stat.MAGIC_ATTACK, set.getDouble("mAtk", 0));
		}
		if (set.contains("range"))
		{
			addStats(Stat.PHYSICAL_ATTACK_RANGE, set.getInt("range", 0));
		}
		if (set.contains("attackSpeed"))
		{
			addStats(Stat.PHYSICAL_ATTACK_SPEED, set.getInt("attackSpeed", 0));
		}
		if (set.contains("critRate"))
		{
			addStats(Stat.CRITICAL_RATE, set.getInt("critRate", 0));
		}
		if (set.contains("str"))
		{
			addStats(Stat.STAT_STR, set.getInt("str", 0));
		}
		if (set.contains("int"))
		{
			addStats(Stat.STAT_INT, set.getInt("int", 0));
		}
		if (set.contains("con"))
		{
			addStats(Stat.STAT_CON, set.getInt("con", 0));
		}
		if (set.contains("dex"))
		{
			addStats(Stat.STAT_DEX, set.getInt("dex", 0));
		}
		if (set.contains("wit"))
		{
			addStats(Stat.STAT_WIT, set.getInt("wit", 0));
		}
		if (set.contains("men"))
		{
			addStats(Stat.STAT_MEN, set.getInt("men", 0));
		}
		
		if (set.contains("chest"))
		{
			addDefense(Inventory.PAPERDOLL_CHEST, set.getInt("chest", 0));
		}
		if (set.contains("legs"))
		{
			addDefense(Inventory.PAPERDOLL_LEGS, set.getInt("legs", 0));
		}
		if (set.contains("head"))
		{
			addDefense(Inventory.PAPERDOLL_HEAD, set.getInt("head", 0));
		}
		if (set.contains("feet"))
		{
			addDefense(Inventory.PAPERDOLL_FEET, set.getInt("feet", 0));
		}
		if (set.contains("gloves"))
		{
			addDefense(Inventory.PAPERDOLL_GLOVES, set.getInt("gloves", 0));
		}
		if (set.contains("underwear"))
		{
			addDefense(Inventory.PAPERDOLL_UNDER, set.getInt("underwear", 0));
		}
		if (set.contains("cloak"))
		{
			addDefense(Inventory.PAPERDOLL_CLOAK, set.getInt("cloak", 0));
		}
		if (set.contains("rear"))
		{
			addDefense(Inventory.PAPERDOLL_REAR, set.getInt("rear", 0));
		}
		if (set.contains("lear"))
		{
			addDefense(Inventory.PAPERDOLL_LEAR, set.getInt("lear", 0));
		}
		if (set.contains("rfinger"))
		{
			addDefense(Inventory.PAPERDOLL_RFINGER, set.getInt("rfinger", 0));
		}
		if (set.contains("lfinger"))
		{
			addDefense(Inventory.PAPERDOLL_LFINGER, set.getInt("lfinger", 0));
		}
		if (set.contains("neck"))
		{
			addDefense(Inventory.PAPERDOLL_NECK, set.getInt("neck", 0));
		}
	}
	
	private void addDefense(int type, int value)
	{
		if (_baseDefense == null)
		{
			_baseDefense = new HashMap<>();
		}
		_baseDefense.put(type, value);
	}
	
	/**
	 * @param type the slot type for where to search defense.
	 * @param defaultValue value to be used if no value for the type is found.
	 * @return altered value if it is present, or {@code defaultValue} if no such type is assigned to this template.
	 */
	public int getDefense(int type, int defaultValue)
	{
		return (_baseDefense == null) ? defaultValue : _baseDefense.getOrDefault(type, defaultValue);
	}
	
	private void addStats(Stat stat, double value)
	{
		if (_baseStats == null)
		{
			_baseStats = new HashMap<>();
		}
		_baseStats.put(stat.ordinal(), value);
	}
	
	/**
	 * @param stat the Stat value to search for.
	 * @param defaultValue value to be used if no such stat is found.
	 * @return altered stat if it is present, or {@code defaultValue} if no such stat is assigned to this template.
	 */
	public double getStats(Stat stat, double defaultValue)
	{
		return _baseStats == null ? defaultValue : _baseStats.getOrDefault(stat.ordinal(), defaultValue);
	}
	
	/**
	 * @return collision radius if set, {@code null} otherwise.
	 */
	public Float getCollisionRadius()
	{
		return _collisionRadius;
	}
	
	/**
	 * @return collision height if set, {@code null} otherwise.
	 */
	public Float getCollisionHeight()
	{
		return _collisionHeight;
	}
	
	public WeaponType getBaseAttackType()
	{
		return _baseAttackType;
	}
	
	public void addSkill(SkillHolder holder)
	{
		if (_skills == null)
		{
			_skills = new ArrayList<>();
		}
		_skills.add(holder);
	}
	
	public List<SkillHolder> getSkills()
	{
		return _skills != null ? _skills : Collections.emptyList();
	}
	
	public void addAdditionalSkill(AdditionalSkillHolder holder)
	{
		if (_additionalSkills == null)
		{
			_additionalSkills = new ArrayList<>();
		}
		_additionalSkills.add(holder);
	}
	
	public List<AdditionalSkillHolder> getAdditionalSkills()
	{
		return _additionalSkills != null ? _additionalSkills : Collections.emptyList();
	}
	
	public void addAdditionalItem(AdditionalItemHolder holder)
	{
		if (_additionalItems == null)
		{
			_additionalItems = new ArrayList<>();
		}
		_additionalItems.add(holder);
	}
	
	public List<AdditionalItemHolder> getAdditionalItems()
	{
		return _additionalItems != null ? _additionalItems : Collections.emptyList();
	}
	
	public void setBasicActionList(int[] actions)
	{
		_actions = actions;
	}
	
	public int[] getBasicActionList()
	{
		return _actions;
	}
	
	public boolean hasBasicActionList()
	{
		return _actions != null;
	}
	
	public void addLevelData(TransformLevelData data)
	{
		_data.put(data.getLevel(), data);
	}
	
	public TransformLevelData getData(int level)
	{
		return _data.get(level);
	}
}
