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
package org.l2jmobius.gameserver.model.item;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;

/**
 * @author Zoey76
 */
public class PlayerItemTemplate extends ItemHolder
{
	private final boolean _equipped;
	
	/**
	 * @param set the set containing the values for this object
	 */
	public PlayerItemTemplate(StatSet set)
	{
		super(set.getInt("id"), set.getInt("count"));
		_equipped = set.getBoolean("equipped", false);
	}
	
	/**
	 * @return {@code true} if the items is equipped upon character creation, {@code false} otherwise
	 */
	public boolean isEquipped()
	{
		return _equipped;
	}
}