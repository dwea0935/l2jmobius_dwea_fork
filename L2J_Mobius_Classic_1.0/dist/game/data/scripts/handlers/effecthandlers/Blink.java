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
package handlers.effecthandlers;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.FlyType;
import org.l2jmobius.gameserver.network.serverpackets.FlyToLocation;
import org.l2jmobius.gameserver.network.serverpackets.ValidateLocation;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Blink effect implementation.<br>
 * This class handles warp effects, disappear and quickly turn up in a near location.<br>
 * If geodata enabled and an object is between initial and final point, flight is stopped just before colliding with object.<br>
 * Flight course and radius are set as skill properties (flyCourse and flyRadius):
 * <ul>
 * <li>Fly Radius means the distance between starting point and final point, it must be an integer.</li>
 * <li>Fly Course means the movement direction: imagine a compass above player's head, making north player's heading. So if fly course is 180, player will go backwards (good for blink, e.g.).</li>
 * </ul>
 * By the way, if flyCourse = 360 or 0, player will be moved in in front of him.<br>
 * If target is effector, put in XML self="1", this will make _actor = getEffector(). This, combined with target type, allows more complex actions like flying target's backwards or player's backwards.
 * @author DrHouse
 */
public class Blink extends AbstractEffect
{
	private final int _flyCourse;
	private final int _flyRadius;
	
	private final FlyType _flyType;
	private final int _flySpeed;
	private final int _flyDelay;
	private final int _animationSpeed;
	
	public Blink(StatSet params)
	{
		_flyCourse = params.getInt("angle", 0);
		_flyRadius = params.getInt("range", 0);
		_flyType = params.getEnum("flyType", FlyType.class, FlyType.DUMMY);
		_flySpeed = params.getInt("speed", 0);
		_flyDelay = params.getInt("delay", 0);
		_animationSpeed = params.getInt("animationSpeed", 0);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		// While affected by escape blocking effect you cannot use Blink or Scroll of Escape
		return !effected.cannotEscape();
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final double angle = LocationUtil.convertHeadingToDegree(effected.getHeading());
		final double radian = Math.toRadians(angle);
		final double course = Math.toRadians(_flyCourse);
		final int x1 = (int) (Math.cos(Math.PI + radian + course) * _flyRadius);
		final int y1 = (int) (Math.sin(Math.PI + radian + course) * _flyRadius);
		
		final int x = effected.getX() + x1;
		final int y = effected.getY() + y1;
		final int z = effected.getZ();
		
		final Location destination = GeoEngine.getInstance().getValidLocation(effected.getX(), effected.getY(), effected.getZ(), x, y, z, effected.getInstanceWorld());
		
		effected.getAI().setIntention(Intention.IDLE);
		effected.broadcastPacket(new FlyToLocation(effected, destination, _flyType, _flySpeed, _flyDelay, _animationSpeed));
		effected.setXYZ(destination);
		effected.broadcastPacket(new ValidateLocation(effected));
		effected.revalidateZone(true);
	}
}
