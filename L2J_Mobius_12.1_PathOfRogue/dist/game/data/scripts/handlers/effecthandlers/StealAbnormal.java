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

import java.util.List;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.EffectScope;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.DispelSlotType;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.stats.Formulas;

/**
 * Steal Abnormal effect implementation.
 * @author Adry_85, Zoey76
 */
public class StealAbnormal extends AbstractEffect
{
	private final DispelSlotType _slot;
	private final int _rate;
	private final int _max;
	
	public StealAbnormal(StatSet params)
	{
		_slot = params.getEnum("slot", DispelSlotType.class, DispelSlotType.BUFF);
		_rate = params.getInt("rate", 0);
		_max = params.getInt("max", 0);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.STEAL_ABNORMAL;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isPlayer() && (effector != effected))
		{
			final List<BuffInfo> toSteal = Formulas.calcCancelStealEffects(effector, effected, skill, _slot, _rate, _max);
			if (toSteal.isEmpty())
			{
				return;
			}
			
			for (BuffInfo infoToSteal : toSteal)
			{
				// Invert effected and effector.
				final BuffInfo stolen = new BuffInfo(effected, effector, infoToSteal.getSkill(), false, null, null);
				stolen.setAbnormalTime(infoToSteal.getTime()); // Copy the remaining time.
				// To include all the effects, it's required to go through the template rather the buff info.
				infoToSteal.getSkill().applyEffectScope(EffectScope.GENERAL, stolen, true, true);
				effected.getEffectList().remove(infoToSteal, SkillFinishType.REMOVED, true, true);
				effector.getEffectList().add(stolen);
			}
		}
	}
}