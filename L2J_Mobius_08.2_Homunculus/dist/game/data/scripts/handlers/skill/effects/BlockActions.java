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

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Block Actions effect implementation.
 * @author mkizub
 */
public class BlockActions extends AbstractEffect
{
	private final Set<Integer> _allowedSkills = new HashSet<>();
	
	public BlockActions(StatSet params)
	{
		for (String skill : params.getString("allowedSkills", "").split(";"))
		{
			if (!skill.isEmpty())
			{
				_allowedSkills.add(Integer.parseInt(skill));
			}
		}
	}
	
	@Override
	public long getEffectFlags()
	{
		return _allowedSkills.isEmpty() ? EffectFlag.BLOCK_ACTIONS.getMask() : EffectFlag.CONDITIONAL_BLOCK_ACTIONS.getMask();
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.BLOCK_ACTIONS;
	}
	
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		// Raids are immune, checked here so that the effect flag is not applied to them either.
		return (effected != null) && !effected.isRaid();
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		for (Integer skillId : _allowedSkills)
		{
			effected.addBlockActionsAllowedSkill(skillId);
		}
		
		effected.startParalyze();
		
		// Cancel running skill casters.
		effected.abortAllSkillCasters();
	}
	
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		for (Integer skillId : _allowedSkills)
		{
			effected.removeBlockActionsAllowedSkill(skillId);
		}
		
		if (effected.isPlayable())
		{
			if (effected.isSummon())
			{
				if ((effector != null) && !effector.isDead())
				{
					if (effector.isPlayable() && (effected.asPlayer().getPvpFlag() == 0))
					{
						effected.disableCoreAI(false);
					}
					else
					{
						effected.asSummon().doAutoAttack(effector);
					}
				}
				else
				{
					effected.disableCoreAI(false);
				}
			}
			else
			{
				effected.getAI().setIntentionActive();
			}
		}
		else
		{
			effected.getAI().notifyActionThink();
		}
	}
}
