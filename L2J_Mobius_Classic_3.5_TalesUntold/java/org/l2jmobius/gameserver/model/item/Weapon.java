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
package org.l2jmobius.gameserver.model.item;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcSkillSee;
import org.l2jmobius.gameserver.model.item.enums.ItemSkillType;
import org.l2jmobius.gameserver.model.item.type.WeaponType;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.Formulas;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This class is dedicated to the management of weapons.
 */
public class Weapon extends ItemTemplate
{
	private WeaponType _type;
	private boolean _isMagicWeapon;
	private int _soulShotCount;
	private int _spiritShotCount;
	private int _mpConsume;
	private int _baseAttackRange;
	private int _baseAttackRadius;
	private int _baseAttackAngle;
	private int _changeWeaponId;
	
	private int _reducedSoulshot;
	private int _reducedSoulshotChance;
	
	private int _reducedMpConsume;
	private int _reducedMpConsumeChance;
	
	private boolean _isForceEquip;
	private boolean _isAttackWeapon;
	private boolean _useWeaponSkillsOnly;
	
	/**
	 * Constructor for Weapon.
	 * @param set the StatSet designating the set of couples (key,value) characterizing the weapon.
	 */
	public Weapon(StatSet set)
	{
		super(set);
	}
	
	@Override
	public void set(StatSet set)
	{
		super.set(set);
		_type = WeaponType.valueOf(set.getString("weapon_type", "none").toUpperCase());
		_type1 = ItemTemplate.TYPE1_WEAPON_RING_EARRING_NECKLACE;
		_type2 = ItemTemplate.TYPE2_WEAPON;
		_isMagicWeapon = set.getBoolean("is_magic_weapon", false);
		_soulShotCount = set.getInt("soulshots", 0);
		_spiritShotCount = set.getInt("spiritshots", 0);
		_mpConsume = set.getInt("mp_consume", 0);
		_baseAttackRange = set.getInt("attack_range", 40);
		final String[] damageRange = set.getString("damage_range", "").split(";"); // 0?;0?;fan sector;base attack angle
		if ((damageRange.length > 1) && StringUtil.isNumeric(damageRange[2]) && StringUtil.isNumeric(damageRange[3]))
		{
			_baseAttackRadius = Integer.parseInt(damageRange[2]);
			_baseAttackAngle = Integer.parseInt(damageRange[3]);
		}
		else
		{
			_baseAttackRadius = 40;
			_baseAttackAngle = 0;
		}
		
		final String[] reducedSoulshots = set.getString("reduced_soulshot", "").split(",");
		_reducedSoulshotChance = (reducedSoulshots.length == 2) ? Integer.parseInt(reducedSoulshots[0]) : 0;
		_reducedSoulshot = (reducedSoulshots.length == 2) ? Integer.parseInt(reducedSoulshots[1]) : 0;
		
		final String[] reducedMpConsume = set.getString("reduced_mp_consume", "").split(",");
		_reducedMpConsumeChance = (reducedMpConsume.length == 2) ? Integer.parseInt(reducedMpConsume[0]) : 0;
		_reducedMpConsume = (reducedMpConsume.length == 2) ? Integer.parseInt(reducedMpConsume[1]) : 0;
		_changeWeaponId = set.getInt("change_weaponId", 0);
		_isForceEquip = set.getBoolean("isForceEquip", false);
		_isAttackWeapon = set.getBoolean("isAttackWeapon", true);
		_useWeaponSkillsOnly = set.getBoolean("useWeaponSkillsOnly", false);
		
		// Check if ranged weapon reuse delay is missing.
		if ((_reuseDelay == 0) && _type.isRanged())
		{
			_reuseDelay = 1500;
		}
	}
	
	/**
	 * @return the type of Weapon
	 */
	@Override
	public WeaponType getItemType()
	{
		return _type;
	}
	
	/**
	 * @return the ID of the Etc item after applying the mask.
	 */
	@Override
	public int getItemMask()
	{
		return _type.mask();
	}
	
	/**
	 * @return {@code true} if the item is a weapon, {@code false} otherwise.
	 */
	@Override
	public boolean isWeapon()
	{
		return true;
	}
	
	/**
	 * @return {@code true} if the weapon is magic, {@code false} otherwise.
	 */
	@Override
	public boolean isMagicWeapon()
	{
		return _isMagicWeapon;
	}
	
	/**
	 * @return the quantity of SoulShot used.
	 */
	public int getSoulShotCount()
	{
		return _soulShotCount;
	}
	
	/**
	 * @return the quantity of SpiritShot used.
	 */
	public int getSpiritShotCount()
	{
		return _spiritShotCount;
	}
	
	/**
	 * @return the reduced quantity of SoultShot used.
	 */
	public int getReducedSoulShot()
	{
		return _reducedSoulshot;
	}
	
	/**
	 * @return the chance to use Reduced SoultShot.
	 */
	public int getReducedSoulShotChance()
	{
		return _reducedSoulshotChance;
	}
	
	/**
	 * @return the MP consumption with the weapon.
	 */
	public int getMpConsume()
	{
		return _mpConsume;
	}
	
	public int getBaseAttackRange()
	{
		return _baseAttackRange;
	}
	
	public int getBaseAttackRadius()
	{
		return _baseAttackRadius;
	}
	
	public int getBaseAttackAngle()
	{
		return _baseAttackAngle;
	}
	
	/**
	 * @return the reduced MP consumption with the weapon.
	 */
	public int getReducedMpConsume()
	{
		return _reducedMpConsume;
	}
	
	/**
	 * @return the chance to use getReducedMpConsume()
	 */
	public int getReducedMpConsumeChance()
	{
		return _reducedMpConsumeChance;
	}
	
	/**
	 * @return the Id in which weapon this weapon can be changed.
	 */
	public int getChangeWeaponId()
	{
		return _changeWeaponId;
	}
	
	/**
	 * @return {@code true} if the weapon is force equip, {@code false} otherwise.
	 */
	public boolean isForceEquip()
	{
		return _isForceEquip;
	}
	
	/**
	 * @return {@code true} if the weapon is attack weapon, {@code false} otherwise.
	 */
	public boolean isAttackWeapon()
	{
		return _isAttackWeapon;
	}
	
	/**
	 * @return {@code true} if the weapon is skills only, {@code false} otherwise.
	 */
	public boolean useWeaponSkillsOnly()
	{
		return _useWeaponSkillsOnly;
	}
	
	/**
	 * @param caster the Creature pointing out the caster
	 * @param target the Creature pointing out the target
	 * @param trigger
	 * @param type
	 */
	public void applyConditionalSkills(Creature caster, Creature target, Skill trigger, ItemSkillType type)
	{
		forEachSkill(type, holder ->
		{
			final Skill skill = holder.getSkill();
			if (Rnd.get(100) >= holder.getChance())
			{
				return;
			}
			
			if (type == ItemSkillType.ON_MAGIC_SKILL)
			{
				// Trigger only if both are good or bad magic.
				if (trigger.isBad() != skill.isBad())
				{
					return;
				}
				
				// No Trigger if not Magic Skill or is toggle
				if (trigger.isMagic() != skill.isMagic())
				{
					return;
				}
				
				// No Trigger if skill is toggle
				if (trigger.isToggle())
				{
					return;
				}
				
				if (skill.isBad() && (Formulas.calcShldUse(caster, target) == Formulas.SHIELD_DEFENSE_PERFECT_BLOCK))
				{
					return;
				}
			}
			
			// Skill condition not met
			if (!skill.checkCondition(caster, target, true))
			{
				return;
			}
			
			skill.activateSkill(caster, target);
			
			// TODO: Verify if this applies ONLY to ON_MAGIC_SKILL!
			if (type == ItemSkillType.ON_MAGIC_SKILL)
			{
				// notify quests of a skill use
				if (caster.isPlayer())
				{
					World.getInstance().forEachVisibleObjectInRange(caster, Npc.class, 1000, npc ->
					{
						if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_SKILL_SEE, npc))
						{
							EventDispatcher.getInstance().notifyEventAsync(new OnNpcSkillSee(npc, caster.asPlayer(), skill, false, target), npc);
						}
					});
				}
				if (caster.isPlayer())
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_ACTIVATED);
					sm.addSkillName(skill);
					caster.sendPacket(sm);
				}
			}
		});
	}
}
