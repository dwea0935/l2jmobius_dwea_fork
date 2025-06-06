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
package handlers.skillconditionhandlers;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.skill.ISkillCondition;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * @author UnAfraid
 */
public class TargetMyPartySkillCondition implements ISkillCondition
{
	private final boolean _includeMe;
	
	public TargetMyPartySkillCondition(StatSet params)
	{
		_includeMe = params.getBoolean("includeMe");
	}
	
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		if ((target == null) || !target.isPlayable())
		{
			return false;
		}
		
		final Party party = caster.getParty();
		if (target.isPlayer())
		{
			final Party targetParty = target.asPlayer().getParty();
			return ((party == null) ? (_includeMe && (caster == target)) : (_includeMe ? party == targetParty : (party == targetParty) && (caster != target)));
		}
		else if (target.isSummon())
		{
			final Summon summon = target.asSummon();
			final Player summonOwner = summon.getOwner();
			if (summonOwner != null)
			{
				final Party targetParty = summonOwner.getParty();
				return ((party == null) ? (_includeMe && (caster == summonOwner)) : (_includeMe ? party == targetParty : (party == targetParty) && (caster != summonOwner)));
			}
		}
		return false;
	}
}
