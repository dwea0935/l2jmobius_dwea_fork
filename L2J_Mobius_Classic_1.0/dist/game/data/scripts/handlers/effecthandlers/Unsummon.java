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

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.Formulas;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Unsummon effect implementation.
 * @author Adry_85
 */
public class Unsummon extends AbstractEffect
{
	private final int _chance;
	
	public Unsummon(StatSet params)
	{
		_chance = params.getInt("chance", -1);
	}
	
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		if (_chance < 0)
		{
			return true;
		}
		
		final int magicLevel = skill.getMagicLevel();
		if ((magicLevel <= 0) || ((effected.getLevel() - 9) <= magicLevel))
		{
			final double chance = _chance * Formulas.calcAttributeBonus(effector, effected, skill) * Formulas.calcGeneralTraitBonus(effector, effected, skill.getTraitType(), false);
			if ((chance >= 100) || (chance > (Rnd.nextDouble() * 100)))
			{
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return effected.isSummon();
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isServitor())
		{
			final Summon servitor = effected.asSummon();
			final Player summonOwner = servitor.getOwner();
			
			servitor.abortAttack();
			servitor.abortCast();
			servitor.stopAllEffects();
			
			servitor.unSummon(summonOwner);
			summonOwner.sendPacket(SystemMessageId.YOUR_SERVITOR_HAS_VANISHED_YOU_LL_NEED_TO_SUMMON_A_NEW_ONE);
		}
	}
}
