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

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.skill.ISkillCondition;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * TODO: Verify me, also should Quest items be counted?
 * @author UnAfraid
 */
public class OpEncumberedSkillCondition implements ISkillCondition
{
	private final int _slotsPercent;
	private final int _weightPercent;
	
	public OpEncumberedSkillCondition(StatSet params)
	{
		_slotsPercent = params.getInt("slotsPercent");
		_weightPercent = params.getInt("weightPercent");
	}
	
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		if (!caster.isPlayer())
		{
			return false;
		}
		
		final Player player = caster.asPlayer();
		final int currentSlotsPercent = calcPercent(player.getInventoryLimit(), player.getInventory().getNonQuestSize());
		final int currentWeightPercent = calcPercent(player.getMaxLoad(), player.getCurrentLoad());
		return (currentSlotsPercent >= _slotsPercent) && (currentWeightPercent >= _weightPercent);
	}
	
	private int calcPercent(int max, int current)
	{
		return 100 - ((current * 100) / max);
	}
}
