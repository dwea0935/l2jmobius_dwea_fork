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

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.krateisCube.KrateiArena;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * Teleport effect implementation.
 * @author Adry_85
 */
public class Teleport extends AbstractEffect
{
	private final Location _loc;
	
	public Teleport(StatSet params)
	{
		_loc = new Location(params.getInt("x", 0), params.getInt("y", 0), params.getInt("z", 0));
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.TELEPORT;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isPlayer())
		{
			final Player player = effected.asPlayer();
			final KrateiArena arena = player.getKrateiArena();
			if (arena != null)
			{
				arena.removePlayer(player);
			}
			else if (player.getUCState() != Player.UC_STATE_NONE)
			{
				return;
			}
		}
		
		effected.teleToLocation(_loc, true, null);
	}
}
