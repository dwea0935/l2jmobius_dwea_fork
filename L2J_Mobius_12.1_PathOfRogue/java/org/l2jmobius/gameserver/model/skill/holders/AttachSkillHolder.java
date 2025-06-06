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
package org.l2jmobius.gameserver.model.skill.holders;

import org.l2jmobius.gameserver.model.StatSet;

/**
 * @author Nik
 */
public class AttachSkillHolder extends SkillHolder
{
	private final int _requiredSkillId;
	private final int _requiredSkillLevel;
	
	public AttachSkillHolder(int skillId, int skillLevel, int requiredSkillId, int requiredSkillLevel)
	{
		super(skillId, skillLevel);
		_requiredSkillId = requiredSkillId;
		_requiredSkillLevel = requiredSkillLevel;
	}
	
	public int getRequiredSkillId()
	{
		return _requiredSkillId;
	}
	
	public int getRequiredSkillLevel()
	{
		return _requiredSkillLevel;
	}
	
	public static AttachSkillHolder fromStatSet(StatSet set)
	{
		return new AttachSkillHolder(set.getInt("skillId"), set.getInt("skillLevel", 1), set.getInt("requiredSkillId"), set.getInt("requiredSkillLevel", 1));
	}
}
