/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package handlers.effecthandlers;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.conditions.ConditionUsingItemType;
import org.l2jmobius.gameserver.model.conditions.ConditionUsingSlotType;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.type.WeaponType;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.StatModifierType;
import org.l2jmobius.gameserver.model.stats.Stat;

/**
 * @author Sdw, Mobius
 */
public class TwoHandedSwordBonus extends AbstractEffect
{
	private static final Condition _weaponTypeCondition = new ConditionUsingItemType(WeaponType.SWORD.mask());
	private static final Condition _slotCondition = new ConditionUsingSlotType(ItemTemplate.SLOT_LR_HAND);
	
	private final double _pAtkAmount;
	private final StatModifierType _pAtkMode;
	
	private final double _mAtkAmount;
	private final StatModifierType _mAtkMode;
	
	private final double _pAtkSpeedAmount;
	private final StatModifierType _pAtkSpeedMode;
	
	private final double _mAtkSpeedAmount;
	private final StatModifierType _mAtkSpeedMode;
	
	private final double _pAccuracyAmount;
	private final StatModifierType _pAccuracyMode;
	
	private final double _mAccuracyAmount;
	private final StatModifierType _mAccuracyMode;
	
	private final double _pCritRateAmount;
	private final StatModifierType _pCritRateMode;
	
	private final double _mCritRateAmount;
	private final StatModifierType _mCritRateMode;
	
	private final double _pCritDamageAmount;
	private final StatModifierType _pCritDamageMode;
	
	private final double _mCritDamageAmount;
	private final StatModifierType _mCritDamageMode;
	
	private final double _speedAmount;
	private final StatModifierType _speedMode;
	
	private final double _physicalAttackRange;
	private final StatModifierType _physicalAttackRangeMode;
	
	private final double _skillBonusRange;
	private final StatModifierType _skillBonusRangeMode;
	
	public TwoHandedSwordBonus(StatSet params)
	{
		_pAtkAmount = params.getDouble("pAtkAmount", 0);
		_pAtkMode = params.getEnum("pAtkMode", StatModifierType.class, StatModifierType.DIFF);
		
		_mAtkAmount = params.getDouble("mAtkAmount", 0);
		_mAtkMode = params.getEnum("mAtkMode", StatModifierType.class, StatModifierType.DIFF);
		
		_pAtkSpeedAmount = params.getDouble("pAtkSpeedAmount", 0);
		_pAtkSpeedMode = params.getEnum("pAtkSpeedMode", StatModifierType.class, StatModifierType.DIFF);
		
		_mAtkSpeedAmount = params.getDouble("mAtkSpeedAmount", 0);
		_mAtkSpeedMode = params.getEnum("mAtkSpeedMode", StatModifierType.class, StatModifierType.DIFF);
		
		_pAccuracyAmount = params.getDouble("pAccuracyAmount", 0);
		_pAccuracyMode = params.getEnum("pAccuracyMode", StatModifierType.class, StatModifierType.DIFF);
		
		_mAccuracyAmount = params.getDouble("mAccuracyAmount", 0);
		_mAccuracyMode = params.getEnum("mAccuracyMode", StatModifierType.class, StatModifierType.DIFF);
		
		_pCritRateAmount = params.getDouble("pCritRateAmount", 0);
		_pCritRateMode = params.getEnum("pCritRateMode", StatModifierType.class, StatModifierType.DIFF);
		
		_mCritRateAmount = params.getDouble("mCritRateAmount", 0);
		_mCritRateMode = params.getEnum("mCritRateMode", StatModifierType.class, StatModifierType.DIFF);
		
		_pCritDamageAmount = params.getDouble("pCritDamageAmount", 0);
		_pCritDamageMode = params.getEnum("pCritDamageMode", StatModifierType.class, StatModifierType.DIFF);
		
		_mCritDamageAmount = params.getDouble("mCritDamageAmount", 0);
		_mCritDamageMode = params.getEnum("mCritDamageMode", StatModifierType.class, StatModifierType.DIFF);
		
		_speedAmount = params.getDouble("speedAmount", 0);
		_speedMode = params.getEnum("speedMode", StatModifierType.class, StatModifierType.DIFF);
		
		_physicalAttackRange = params.getDouble("PhysicalAttackRange", 0);
		_physicalAttackRangeMode = params.getEnum("PhysicalAttackRangeMode", StatModifierType.class, StatModifierType.DIFF);
		
		_skillBonusRange = params.getDouble("SkillBonusRange", 0);
		_skillBonusRangeMode = params.getEnum("SkillBonusRangeMode", StatModifierType.class, StatModifierType.DIFF);
	}
	
	@Override
	public void pump(Creature effected, Skill skill)
	{
		if (_weaponTypeCondition.test(effected, effected, skill) && _slotCondition.test(effected, effected, skill))
		{
			if (_pAtkAmount != 0)
			{
				if (_pAtkMode == StatModifierType.DIFF)
				{
					effected.getStat().mergeAdd(Stat.PHYSICAL_ATTACK, _pAtkAmount);
				}
				else // PER
				{
					effected.getStat().mergeMul(Stat.PHYSICAL_ATTACK, (_pAtkAmount / 100) + 1);
				}
			}
			
			if (_mAtkAmount != 0)
			{
				if (_mAtkMode == StatModifierType.DIFF)
				{
					effected.getStat().mergeAdd(Stat.MAGIC_ATTACK, _mAtkAmount);
				}
				else // PER
				{
					effected.getStat().mergeMul(Stat.MAGIC_ATTACK, (_mAtkAmount / 100) + 1);
				}
			}
			
			if (_pAtkSpeedAmount != 0)
			{
				if (_pAtkSpeedMode == StatModifierType.DIFF)
				{
					effected.getStat().mergeAdd(Stat.PHYSICAL_ATTACK_SPEED, _pAtkSpeedAmount);
				}
				else // PER
				{
					effected.getStat().mergeMul(Stat.PHYSICAL_ATTACK_SPEED, (_pAtkSpeedAmount / 100) + 1);
				}
			}
			
			if (_mAtkSpeedAmount != 0)
			{
				if (_mAtkSpeedMode == StatModifierType.DIFF)
				{
					effected.getStat().mergeAdd(Stat.MAGIC_ATTACK_SPEED, _mAtkSpeedAmount);
				}
				else // PER
				{
					effected.getStat().mergeMul(Stat.MAGIC_ATTACK_SPEED, (_mAtkSpeedAmount / 100) + 1);
				}
			}
			
			if (_pAccuracyAmount != 0)
			{
				if (_pAccuracyMode == StatModifierType.DIFF)
				{
					effected.getStat().mergeAdd(Stat.ACCURACY_COMBAT, _pAccuracyAmount);
				}
				else // PER
				{
					effected.getStat().mergeMul(Stat.ACCURACY_COMBAT, (_pAccuracyAmount / 100) + 1);
				}
			}
			
			if (_mAccuracyAmount != 0)
			{
				if (_mAccuracyMode == StatModifierType.DIFF)
				{
					effected.getStat().mergeAdd(Stat.ACCURACY_MAGIC, _mAccuracyAmount);
				}
				else // PER
				{
					effected.getStat().mergeMul(Stat.ACCURACY_MAGIC, (_mAccuracyAmount / 100) + 1);
				}
			}
			
			if (_pCritRateAmount != 0)
			{
				if (_pCritRateMode == StatModifierType.DIFF)
				{
					effected.getStat().mergeAdd(Stat.CRITICAL_RATE, _pCritRateAmount);
				}
				else // PER
				{
					effected.getStat().mergeMul(Stat.CRITICAL_RATE, (_pCritRateAmount / 100) + 1);
				}
			}
			
			if (_mCritRateAmount != 0)
			{
				if (_mCritRateMode == StatModifierType.DIFF)
				{
					effected.getStat().mergeAdd(Stat.MAGIC_CRITICAL_RATE, _mCritRateAmount);
				}
				else // PER
				{
					effected.getStat().mergeMul(Stat.MAGIC_CRITICAL_RATE, (_mCritRateAmount / 100) + 1);
				}
			}
			
			if (_pCritDamageAmount != 0)
			{
				if (_pCritDamageMode == StatModifierType.DIFF)
				{
					effected.getStat().mergeAdd(Stat.CRITICAL_DAMAGE_ADD, _pCritDamageAmount);
				}
				else // PER
				{
					effected.getStat().mergeMul(Stat.CRITICAL_DAMAGE, (_pCritDamageAmount / 100) + 1);
				}
			}
			
			if (_mCritDamageAmount != 0)
			{
				if (_mCritDamageMode == StatModifierType.DIFF)
				{
					effected.getStat().mergeAdd(Stat.MAGIC_CRITICAL_DAMAGE_ADD, _mCritDamageAmount);
				}
				else // PER
				{
					effected.getStat().mergeMul(Stat.MAGIC_CRITICAL_DAMAGE, (_mCritDamageAmount / 100) + 1);
				}
			}
			
			if (_speedAmount != 0)
			{
				if (_speedMode == StatModifierType.DIFF)
				{
					effected.getStat().mergeAdd(Stat.MOVE_SPEED, _speedAmount);
				}
				else // PER
				{
					effected.getStat().mergeMul(Stat.MOVE_SPEED, (_speedAmount / 100) + 1);
				}
			}
			
			if (_physicalAttackRange != 0)
			{
				if (_physicalAttackRangeMode == StatModifierType.DIFF)
				{
					effected.getStat().mergeAdd(Stat.PHYSICAL_ATTACK_RANGE, _physicalAttackRange);
				}
				else // PER
				{
					effected.getStat().mergeMul(Stat.PHYSICAL_ATTACK_RANGE, (_physicalAttackRange / 100) + 1);
				}
			}
			
			if (_skillBonusRange != 0)
			{
				if (_skillBonusRangeMode == StatModifierType.DIFF)
				{
					effected.getStat().mergeAdd(Stat.MAGIC_ATTACK_RANGE, _skillBonusRange);
				}
				else // PER
				{
					effected.getStat().mergeMul(Stat.MAGIC_ATTACK_RANGE, (_skillBonusRange / 100) + 1);
				}
			}
		}
	}
}
