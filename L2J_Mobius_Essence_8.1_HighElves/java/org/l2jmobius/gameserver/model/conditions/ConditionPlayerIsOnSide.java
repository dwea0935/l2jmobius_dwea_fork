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
package org.l2jmobius.gameserver.model.conditions;

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.siege.CastleSide;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * The Class ConditionPlayerIsOnSide.
 * @author St3eT
 */
public class ConditionPlayerIsOnSide extends Condition
{
	private final CastleSide _side;
	
	/**
	 * Instantiates a new condition player race.
	 * @param side the allowed Castle side.
	 */
	public ConditionPlayerIsOnSide(CastleSide side)
	{
		_side = side;
	}
	
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		if ((effector == null) || !effector.isPlayer())
		{
			return false;
		}
		
		return effector.asPlayer().getPlayerSide() == _side;
	}
}
