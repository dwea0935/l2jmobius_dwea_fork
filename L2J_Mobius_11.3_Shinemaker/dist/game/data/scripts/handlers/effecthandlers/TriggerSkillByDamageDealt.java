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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.handler.TargetHandler;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureDamageDealt;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.item.type.WeaponType;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.skill.targets.TargetType;

/**
 * Trigger skill by damage dealt effect implementation.
 * @author Zealar, Mobius
 */
public class TriggerSkillByDamageDealt extends AbstractEffect
{
	private final int _minAttackerLevel;
	private final int _maxAttackerLevel;
	private final int _minDamage;
	private final int _chance;
	private final SkillHolder _skill;
	private final TargetType _targetType;
	private final InstanceType _attackerType;
	private int _allowWeapons;
	private final boolean _isCritical;
	private final boolean _renewDuration;
	private final boolean _allowNormalAttack;
	private final boolean _allowSkillAttack;
	private final boolean _onlyMagicSkill;
	private final boolean _onlyPhysicalSkill;
	private final boolean _allowReflect;
	private final int _skillLevelScaleTo;
	private final List<SkillHolder> _triggerSkills;
	
	public TriggerSkillByDamageDealt(StatSet params)
	{
		_minAttackerLevel = params.getInt("minAttackerLevel", 1);
		_maxAttackerLevel = params.getInt("maxAttackerLevel", Integer.MAX_VALUE);
		_minDamage = params.getInt("minDamage", 1);
		_chance = params.getInt("chance", 100);
		_skill = new SkillHolder(params.getInt("skillId", 0), params.getInt("skillLevel", 1));
		_targetType = params.getEnum("targetType", TargetType.class, TargetType.SELF);
		_attackerType = params.getEnum("attackerType", InstanceType.class, InstanceType.Creature);
		_isCritical = params.getBoolean("isCritical", false);
		_renewDuration = params.getBoolean("renewDuration", false);
		_allowNormalAttack = params.getBoolean("allowNormalAttack", true);
		_allowSkillAttack = params.getBoolean("allowSkillAttack", false);
		_onlyMagicSkill = params.getBoolean("onlyMagicSkill", false);
		_onlyPhysicalSkill = params.getBoolean("onlyPhysicalSkill", false);
		_allowReflect = params.getBoolean("allowReflect", false);
		_skillLevelScaleTo = params.getInt("skillLevelScaleTo", 0);
		
		if (params.getString("allowWeapons", "ALL").equalsIgnoreCase("ALL"))
		{
			_allowWeapons = 0;
		}
		else
		{
			for (String s : params.getString("allowWeapons").split(","))
			{
				_allowWeapons |= WeaponType.valueOf(s).mask();
			}
		}
		
		// Specific skills by level.
		final String triggerSkills = params.getString("triggerSkills", "");
		if (triggerSkills.isEmpty())
		{
			_triggerSkills = null;
		}
		else
		{
			final String[] split = triggerSkills.split(";");
			_triggerSkills = new ArrayList<>(split.length);
			for (String skill : split)
			{
				final String[] splitSkill = skill.split(",");
				_triggerSkills.add(new SkillHolder(Integer.parseInt(splitSkill[0]), Integer.parseInt(splitSkill[1])));
			}
		}
	}
	
	private void onAttackEvent(OnCreatureDamageDealt event)
	{
		if (event.isDamageOverTime() || (_chance == 0) || ((_triggerSkills == null) && ((_skill.getSkillId() == 0) || (_skill.getSkillLevel() == 0))) || (!_allowNormalAttack && !_allowSkillAttack))
		{
			return;
		}
		
		// Check if there is dependancy on critical.
		if (_isCritical != event.isCritical())
		{
			return;
		}
		
		// When no normal attacks are allowed.
		if (!_allowNormalAttack && (event.getSkill() == null))
		{
			return;
		}
		
		// When no skill attacks are allowed.
		if (!_allowSkillAttack && (event.getSkill() != null))
		{
			return;
		}
		
		// When only physical skills are allowed (allowSkillAttack should be set to true).
		if (_onlyPhysicalSkill && event.getSkill().isMagic())
		{
			return;
		}
		
		// When only magic skills are allowed (allowSkillAttack should be set to true).
		if (_onlyMagicSkill && !event.getSkill().isMagic())
		{
			return;
		}
		
		if (!_allowReflect && event.isReflect())
		{
			return;
		}
		
		if (event.getAttacker() == event.getTarget())
		{
			return;
		}
		
		if ((event.getAttacker().getLevel() < _minAttackerLevel) || (event.getAttacker().getLevel() > _maxAttackerLevel))
		{
			return;
		}
		
		if (event.getDamage() < _minDamage)
		{
			return;
		}
		
		if ((_chance < 100) && (Rnd.get(100) > _chance))
		{
			return;
		}
		
		if (!event.getAttacker().getInstanceType().isType(_attackerType))
		{
			return;
		}
		
		if ((_allowWeapons > 0) && ((event.getAttacker().getActiveWeaponItem() == null) || ((event.getAttacker().getActiveWeaponItem().getItemType().mask() & _allowWeapons) == 0)))
		{
			return;
		}
		
		WorldObject target = null;
		try
		{
			target = TargetHandler.getInstance().getHandler(_targetType).getTarget(event.getAttacker(), event.getTarget(), _triggerSkills == null ? _skill.getSkill() : _triggerSkills.get(0).getSkill(), false, false, false);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception in ITargetTypeHandler.getTarget(): " + e.getMessage(), e);
		}
		if ((target == null) || !target.isCreature())
		{
			return;
		}
		
		Skill triggerSkill = null;
		if (_triggerSkills == null)
		{
			final BuffInfo buffInfo = target.asCreature().getEffectList().getBuffInfoBySkillId(_skill.getSkillId());
			if ((_skillLevelScaleTo <= 0) || (buffInfo == null))
			{
				triggerSkill = _skill.getSkill();
			}
			else
			{
				triggerSkill = SkillData.getInstance().getSkill(_skill.getSkillId(), Math.min(_skillLevelScaleTo, buffInfo.getSkill().getLevel() + 1));
				
				if (event.getAttacker().isSkillDisabled(buffInfo.getSkill()))
				{
					return;
				}
			}
			
			if ((buffInfo == null) || (buffInfo.getSkill().getLevel() < triggerSkill.getLevel()) || _renewDuration)
			{
				SkillCaster.triggerCast(event.getAttacker(), target.asCreature(), triggerSkill);
			}
		}
		else // Multiple trigger skills.
		{
			final Iterator<SkillHolder> iterator = _triggerSkills.iterator();
			while (iterator.hasNext())
			{
				final Skill nextSkill = iterator.next().getSkill();
				if (target.asCreature().isAffectedBySkill(nextSkill.getId()))
				{
					if (iterator.hasNext())
					{
						target.asCreature().stopSkillEffects(SkillFinishType.SILENT, nextSkill.getId());
						triggerSkill = iterator.next().getSkill();
						break;
					}
					
					// Already at last skill.
					if (!_renewDuration)
					{
						return;
					}
					triggerSkill = nextSkill;
				}
			}
			if (triggerSkill == null)
			{
				triggerSkill = _triggerSkills.get(0).getSkill();
			}
			
			SkillCaster.triggerCast(event.getAttacker(), target.asCreature(), triggerSkill);
		}
	}
	
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.removeListenerIf(EventType.ON_CREATURE_DAMAGE_DEALT, listener -> listener.getOwner() == this);
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.addListener(new ConsumerEventListener(effected, EventType.ON_CREATURE_DAMAGE_DEALT, (OnCreatureDamageDealt event) -> onAttackEvent(event), this));
	}
}
