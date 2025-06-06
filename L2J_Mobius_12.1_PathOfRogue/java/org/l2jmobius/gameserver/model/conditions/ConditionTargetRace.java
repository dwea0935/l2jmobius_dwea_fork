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
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * The Class ConditionTargetRace.
 * @author mkizub
 */
public class ConditionTargetRace extends Condition
{
	private final Race _race;
	
	/**
	 * Instantiates a new condition target race.
	 * @param race containing the allowed race.
	 */
	public ConditionTargetRace(Race race)
	{
		_race = race;
	}
	
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		return _race == effected.getRace();
	}
}
