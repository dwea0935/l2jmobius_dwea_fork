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
package org.l2jmobius.gameserver.model.options;

import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Pere
 */
public class Variation
{
	private static final Logger LOGGER = Logger.getLogger(Variation.class.getSimpleName());
	
	private final int _mineralId;
	private final Map<VariationWeaponType, OptionDataGroup[]> _effects = new EnumMap<>(VariationWeaponType.class);
	
	public Variation(int mineralId)
	{
		_mineralId = mineralId;
	}
	
	public int getMineralId()
	{
		return _mineralId;
	}
	
	public void setEffectGroup(VariationWeaponType type, int order, OptionDataGroup group)
	{
		final OptionDataGroup[] effects = _effects.computeIfAbsent(type, _ -> new OptionDataGroup[2]);
		effects[order] = group;
	}
	
	public Options getRandomEffect(VariationWeaponType type, int order)
	{
		final OptionDataGroup[] effects = _effects.get(type);
		if ((effects == null) || (effects[order] == null))
		{
			LOGGER.warning("Null effect: " + type + ", " + order);
			return null;
		}
		return effects[order].getRandomEffect();
	}
}