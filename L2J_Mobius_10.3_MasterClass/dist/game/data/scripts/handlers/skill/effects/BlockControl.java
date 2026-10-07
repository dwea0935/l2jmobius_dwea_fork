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
package handlers.skill.effects;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * An effect that blocks the player (NPC?) control.<br>
 * It prevents moving, casting, social actions, etc.
 * @author Nik
 */
public class BlockControl extends AbstractEffect
{
	public BlockControl(StatSet params)
	{
	}
	
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.BLOCK_CONTROL.getMask();
	}
	
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		// Raids are immune, checked here so that the effect flag is not applied to them either.
		return (effected != null) && !effected.isRaid();
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.BLOCK_CONTROL;
	}
}
