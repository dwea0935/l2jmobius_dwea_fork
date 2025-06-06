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
package handlers.skillconditionhandlers;

import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.enums.creature.Position;
import org.l2jmobius.gameserver.model.skill.ISkillCondition;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * @author Sdw
 */
public class OpBlinkSkillCondition implements ISkillCondition
{
	private final int _angle;
	private final int _range;
	
	public OpBlinkSkillCondition(StatSet params)
	{
		final Position position = params.getEnum("direction", Position.class);
		switch (position)
		{
			case BACK:
			{
				_angle = 0;
				break;
			}
			case FRONT:
			{
				_angle = 180;
				break;
			}
			default:
			{
				_angle = -1;
				break;
			}
		}
		
		_range = params.getInt("range") + 15; // TODO: Check if 15 can be replaced with effected collision radius.
	}
	
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		final double angle = LocationUtil.convertHeadingToDegree(caster.getHeading());
		final double radian = Math.toRadians(angle);
		final double course = Math.toRadians(_angle);
		final int x1 = (int) (Math.cos(Math.PI + radian + course) * _range);
		final int y1 = (int) (Math.sin(Math.PI + radian + course) * _range);
		final int x = caster.getX() + x1;
		final int y = caster.getY() + y1;
		final int z = caster.getZ();
		return GeoEngine.getInstance().canMoveToTarget(caster.getX(), caster.getY(), caster.getZ(), x, y, z, caster.getInstanceWorld());
	}
}
