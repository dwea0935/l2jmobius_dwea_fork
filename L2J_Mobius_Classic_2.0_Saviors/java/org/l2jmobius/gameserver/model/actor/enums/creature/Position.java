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
package org.l2jmobius.gameserver.model.actor.enums.creature;

import org.l2jmobius.gameserver.model.interfaces.ILocational;

/**
 * @author Sdw
 */
public enum Position
{
	FRONT,
	SIDE,
	BACK;
	
	/**
	 * Position calculation based on the retail-like formulas:<br>
	 * <ul>
	 * <li>heading: (unsigned short) abs(heading - (unsigned short)(int)floor(atan2(toY - fromY, toX - fromX) * 65535.0 / 6.283185307179586))</li>
	 * <li>side: if (heading >= 0x2000 && heading <= 0x6000 || (unsigned int)(heading - 0xA000) <= 0x4000)</li>
	 * <li>front: else if ((unsigned int)(heading - 0x2000) <= 0xC000)</li>
	 * <li>back: otherwise.</li>
	 * </ul>
	 * @param from
	 * @param to
	 * @return
	 */
	public static Position getPosition(ILocational from, ILocational to)
	{
		final int heading = Math.abs(to.getHeading() - from.calculateHeadingTo(to));
		if (((heading >= 0x2000) && (heading <= 0x6000)) || (Integer.toUnsignedLong(heading - 0xA000) <= 0x4000))
		{
			return SIDE;
		}
		else if (Integer.toUnsignedLong(heading - 0x2000) <= 0xC000)
		{
			return FRONT;
		}
		else
		{
			return BACK;
		}
	}
}
