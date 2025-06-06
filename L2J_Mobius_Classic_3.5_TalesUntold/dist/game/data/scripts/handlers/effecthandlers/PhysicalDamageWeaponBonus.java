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

import java.util.EnumMap;
import java.util.Map;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.item.enums.ShotType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.item.type.WeaponType;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.Formulas;
import org.l2jmobius.gameserver.model.stats.Stat;

/**
 * Physical damage effect implementation.<br>
 * <b>Note</b>: Initial formula taken from PhysicalDamage.
 * @author Adry_85, Nik
 */
public class PhysicalDamageWeaponBonus extends AbstractEffect
{
	private final double _power;
	private final double _criticalChance;
	private final boolean _ignoreShieldDefence;
	private final boolean _overHit;
	private final double _pDefMod;
	
	private final Map<WeaponType, Double> _weaponBonus = new EnumMap<>(WeaponType.class);
	
	public PhysicalDamageWeaponBonus(StatSet params)
	{
		_power = params.getDouble("power", 0);
		_criticalChance = params.getDouble("criticalChance", 10);
		_ignoreShieldDefence = params.getBoolean("ignoreShieldDefence", false);
		_overHit = params.getBoolean("overHit", false);
		_pDefMod = params.getDouble("pDefMod", 1.0);
		
		for (WeaponType weapon : WeaponType.values())
		{
			final double bonus = params.getDouble(weapon.name(), 1);
			if (bonus != 1)
			{
				_weaponBonus.put(weapon, bonus);
			}
		}
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return !Formulas.calcSkillEvasion(effector, effected, skill);
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
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effector.isAlikeDead())
		{
			return;
		}
		
		if (effected.isPlayer() && effected.asPlayer().isFakeDeath() && Config.FAKE_DEATH_DAMAGE_STAND)
		{
			effected.stopFakeDeath(true);
		}
		
		if (_overHit && effected.isAttackable())
		{
			effected.asAttackable().overhitEnabled(true);
		}
		
		if (_overHit && effected.isAttackable())
		{
			effected.asAttackable().overhitEnabled(true);
		}
		
		final double attack = effector.getPAtk();
		double defence = effected.getPDef() * _pDefMod;
		
		if (!_ignoreShieldDefence)
		{
			switch (Formulas.calcShldUse(effector, effected))
			{
				case Formulas.SHIELD_DEFENSE_SUCCEED:
				{
					defence += effected.getShldDef();
					break;
				}
				case Formulas.SHIELD_DEFENSE_PERFECT_BLOCK:
				{
					defence = -1;
					break;
				}
			}
		}
		
		double damage = 1;
		final boolean critical = Formulas.calcCrit(_criticalChance, effector, effected, skill);
		
		if (defence != -1)
		{
			// Trait, elements
			final double weaponTraitMod = Formulas.calcWeaponTraitBonus(effector, effected);
			final double generalTraitMod = Formulas.calcGeneralTraitBonus(effector, effected, skill.getTraitType(), true);
			final double weaknessMod = Formulas.calcWeaknessBonus(effector, effected, skill.getTraitType());
			final double attributeMod = Formulas.calcAttributeBonus(effector, effected, skill);
			final double pvpPveMod = Formulas.calculatePvpPveBonus(effector, effected, skill, true);
			final double randomMod = effector.getRandomDamageMultiplier();
			
			// Skill specific mods.
			final double weaponMod = effector.getAttackType().isRanged() ? 70 : 77;
			final double weaponBonus = _weaponBonus.containsKey(effector.getAttackType()) ? _weaponBonus.get(effector.getAttackType()).doubleValue() : 1d;
			final double power = _power + effector.getStat().getValue(Stat.SKILL_POWER_ADD, 0);
			final double rangedBonus = effector.getAttackType().isRanged() ? attack + power : 0;
			final double critMod = critical ? Formulas.calcCritDamage(effector, effected, skill) : 1;
			double ssmod = 1;
			if (skill.useSoulShot())
			{
				if (effector.isChargedShot(ShotType.SOULSHOTS))
				{
					ssmod = Math.max(1, 2 + (effector.getStat().getValue(Stat.SHOTS_BONUS) / 100)); // 2.04 for dual weapon?
				}
				else if (effector.isChargedShot(ShotType.BLESSED_SOULSHOTS))
				{
					ssmod = Math.max(1, 4 + (effector.getStat().getValue(Stat.SHOTS_BONUS) / 100));
				}
			}
			
			// ...................____________Melee Damage_____________......................................___________________Ranged Damage____________________
			// ATTACK CALCULATION 77 * ((pAtk * lvlMod) + power) / pdef            RANGED ATTACK CALCULATION 70 * ((pAtk * lvlMod) + power + patk + power) / pdef
			// ```````````````````^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^``````````````````````````````````````^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
			final double baseMod = (weaponMod * ((attack * effector.getLevelMod()) + power + rangedBonus)) / defence;
			// NasSeKa rev. 10200: generalTraitMod == 0 ? 1 : generalTraitMod (no invulnerable traits).
			damage = baseMod * ssmod * critMod * weaponBonus * weaponTraitMod * (generalTraitMod == 0 ? 1 : generalTraitMod) * weaknessMod * attributeMod * pvpPveMod * randomMod;
			damage *= effector.getStat().getValue(Stat.PHYSICAL_SKILL_POWER, 1);
		}
		
		effector.doAttack(damage, effected, skill, false, false, critical, false);
	}
}
