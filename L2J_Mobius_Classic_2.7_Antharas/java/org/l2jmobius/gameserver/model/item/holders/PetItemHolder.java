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
package org.l2jmobius.gameserver.model.item.holders;

import org.l2jmobius.gameserver.model.item.instance.Item;

/**
 * @author UnAfraid
 */
public class PetItemHolder
{
	private final Item _item;
	
	public PetItemHolder(Item item)
	{
		_item = item;
	}
	
	public Item getItem()
	{
		return _item;
	}
}
