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

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.item.enums.ShotType;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.Formulas;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Physical soul damage effect implementation.
 * @author Adry_85
 */
public class PhysicalSoulDamage extends AbstractEffect
{
	public PhysicalSoulDamage(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
	}
	
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return !Formulas.calcPhysicalSkillEvasion(effector, effected, skill);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.PHYSICAL_ATTACK;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
		if (effector.isAlikeDead())
		{
			return;
		}
		
		if (((skill.getFlyRadius() > 0) || (skill.getFlyType() != null)) && effector.isMovementDisabled())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
			sm.addSkillName(skill);
			effector.sendPacket(sm);
			return;
		}
		
		if (effected.isPlayer() && effected.asPlayer().isFakeDeath() && Config.FAKE_DEATH_DAMAGE_STAND)
		{
			effected.stopFakeDeath(true);
		}
		
		int damage = 0;
		final boolean ss = skill.isPhysical() && effector.isChargedShot(ShotType.SOULSHOTS);
		final byte shld = Formulas.calcShldUse(effector, effected, skill);
		// Physical damage critical rate is only affected by STR.
		boolean crit = false;
		if (skill.getBaseCritRate() > 0)
		{
			crit = Formulas.calcCrit(effector, effected, skill);
		}
		
		damage = (int) Formulas.calcPhysDam(effector, effected, skill, shld, false, ss);
		if ((skill.getMaxSoulConsumeCount() > 0) && effector.isPlayer())
		{
			// Souls Formula (each soul increase +4%).
			final int chargedSouls = (effector.asPlayer().getChargedSouls() <= skill.getMaxSoulConsumeCount()) ? effector.asPlayer().getChargedSouls() : skill.getMaxSoulConsumeCount();
			damage *= 1 + (chargedSouls * 0.04);
		}
		if (crit)
		{
			damage *= 2;
		}
		
		if (damage > 0)
		{
			effector.sendDamageMessage(effected, damage, false, crit, false);
			effected.reduceCurrentHp(damage, effector, skill);
			effected.notifyDamageReceived(damage, effector, skill, crit, false);
			
			// Check if damage should be reflected
			Formulas.calcDamageReflected(effector, effected, skill, crit);
		}
		else
		{
			effector.sendPacket(SystemMessageId.YOUR_ATTACK_HAS_FAILED);
		}
		
		if (skill.isSuicideAttack())
		{
			effector.doDie(effector);
		}
	}
}