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
package org.l2jmobius.gameserver.model.skill.holders;

import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * @author UnAfraid
 */
public class SkillUseHolder extends SkillHolder
{
	private final Item _item;
	private final boolean _ctrlPressed;
	private final boolean _shiftPressed;
	
	public SkillUseHolder(Skill skill, Item item, boolean ctrlPressed, boolean shiftPressed)
	{
		super(skill);
		_item = item;
		_ctrlPressed = ctrlPressed;
		_shiftPressed = shiftPressed;
	}
	
	public Item getItem()
	{
		return _item;
	}
	
	public boolean isCtrlPressed()
	{
		return _ctrlPressed;
	}
	
	public boolean isShiftPressed()
	{
		return _shiftPressed;
	}
}
