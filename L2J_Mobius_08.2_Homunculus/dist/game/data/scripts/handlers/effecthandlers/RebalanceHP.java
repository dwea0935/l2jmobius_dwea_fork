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

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Rebalance HP effect implementation.
 * @author Adry_85, earendil
 */
public class RebalanceHP extends AbstractEffect
{
	public RebalanceHP(StatSet params)
	{
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.REBALANCE_HP;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effector.isPlayer())
		{
			return;
		}
		
		double fullHP = 0;
		double currentHPs = 0;
		final Party party = effector.getParty();
		if (party != null)
		{
			for (Player member : party.getMembers())
			{
				if (!member.isDead() && LocationUtil.checkIfInRange(skill.getAffectRange(), effector, member, true))
				{
					fullHP += member.getMaxHp();
					currentHPs += member.getCurrentHp();
				}
				
				final Summon summon = member.getPet();
				if ((summon != null) && (!summon.isDead() && LocationUtil.checkIfInRange(skill.getAffectRange(), effector, summon, true)))
				{
					fullHP += summon.getMaxHp();
					currentHPs += summon.getCurrentHp();
				}
				
				for (Summon servitors : member.getServitors().values())
				{
					if (!servitors.isDead() && LocationUtil.checkIfInRange(skill.getAffectRange(), effector, servitors, true))
					{
						fullHP += servitors.getMaxHp();
						currentHPs += servitors.getCurrentHp();
					}
				}
			}
			
			final double percentHP = currentHPs / fullHP;
			for (Player member : party.getMembers())
			{
				if (!member.isDead() && LocationUtil.checkIfInRange(skill.getAffectRange(), effector, member, true))
				{
					double newHP = member.getMaxHp() * percentHP;
					if (newHP > member.getCurrentHp()) // The target gets healed
					{
						// The heal will be blocked if the current hp passes the limit
						if (member.getCurrentHp() > member.getMaxRecoverableHp())
						{
							newHP = member.getCurrentHp();
						}
						else if (newHP > member.getMaxRecoverableHp())
						{
							newHP = member.getMaxRecoverableHp();
						}
					}
					
					member.setCurrentHp(newHP);
				}
				
				final Summon summon = member.getPet();
				if ((summon != null) && (!summon.isDead() && LocationUtil.checkIfInRange(skill.getAffectRange(), effector, summon, true)))
				{
					double newHP = summon.getMaxHp() * percentHP;
					if (newHP > summon.getCurrentHp()) // The target gets healed
					{
						// The heal will be blocked if the current hp passes the limit
						if (summon.getCurrentHp() > summon.getMaxRecoverableHp())
						{
							newHP = summon.getCurrentHp();
						}
						else if (newHP > summon.getMaxRecoverableHp())
						{
							newHP = summon.getMaxRecoverableHp();
						}
					}
					summon.setCurrentHp(newHP);
				}
				
				for (Summon servitors : member.getServitors().values())
				{
					if (!servitors.isDead() && LocationUtil.checkIfInRange(skill.getAffectRange(), effector, servitors, true))
					{
						double newHP = servitors.getMaxHp() * percentHP;
						if (newHP > servitors.getCurrentHp()) // The target gets healed
						{
							// The heal will be blocked if the current hp passes the limit
							if (servitors.getCurrentHp() > servitors.getMaxRecoverableHp())
							{
								newHP = servitors.getCurrentHp();
							}
							else if (newHP > servitors.getMaxRecoverableHp())
							{
								newHP = servitors.getMaxRecoverableHp();
							}
						}
						servitors.setCurrentHp(newHP);
					}
				}
			}
		}
	}
}
