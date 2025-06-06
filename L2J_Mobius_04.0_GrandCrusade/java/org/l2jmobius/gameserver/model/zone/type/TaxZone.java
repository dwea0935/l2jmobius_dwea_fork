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
package org.l2jmobius.gameserver.model.zone.type;

import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.ZoneType;

/**
 * Tax zone type.
 * @author malyelfik
 */
public class TaxZone extends ZoneType
{
	private int _domainId;
	private Castle _castle;
	
	public TaxZone(int id)
	{
		super(id);
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equalsIgnoreCase("domainId"))
		{
			_domainId = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	@Override
	protected void onEnter(Creature creature)
	{
		creature.setInsideZone(ZoneId.TAX, true);
		if (creature.isNpc())
		{
			creature.asNpc().setTaxZone(this);
		}
	}
	
	@Override
	protected void onExit(Creature creature)
	{
		creature.setInsideZone(ZoneId.TAX, false);
		if (creature.isNpc())
		{
			creature.asNpc().setTaxZone(null);
		}
	}
	
	/**
	 * Gets castle associated with tax zone.
	 * @return instance of {@link Castle} if found otherwise {@code null}
	 */
	public Castle getCastle()
	{
		// Lazy loading is used because zone is loaded before residence
		if (_castle == null)
		{
			_castle = CastleManager.getInstance().getCastleById(_domainId);
		}
		return _castle;
	}
}
