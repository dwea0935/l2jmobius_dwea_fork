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
package org.l2jmobius.gameserver.model.residences;

import java.time.Duration;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;

/**
 * @author UnAfraid
 */
public class ResidenceFunctionTemplate
{
	private final int _id;
	private final int _level;
	private final ResidenceFunctionType _type;
	private final ItemHolder _cost;
	private final Duration _duration;
	private final double _value;
	
	public ResidenceFunctionTemplate(StatSet set)
	{
		_id = set.getInt("id");
		_level = set.getInt("level");
		_type = set.getEnum("type", ResidenceFunctionType.class, ResidenceFunctionType.NONE);
		_cost = new ItemHolder(set.getInt("costId"), set.getLong("costCount"));
		_duration = set.getDuration("duration");
		_value = set.getDouble("value", 0);
	}
	
	/**
	 * @return the function id
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * @return the function level
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * @return the function type
	 */
	public ResidenceFunctionType getType()
	{
		return _type;
	}
	
	/**
	 * @return the cost of the function
	 */
	public ItemHolder getCost()
	{
		return _cost;
	}
	
	/**
	 * @return the duration of the function
	 */
	public Duration getDuration()
	{
		return _duration;
	}
	
	/**
	 * @return the duration of the function as days
	 */
	public long getDurationAsDays()
	{
		return _duration.toDays();
	}
	
	/**
	 * @return value of the function
	 */
	public double getValue()
	{
		return _value;
	}
}
