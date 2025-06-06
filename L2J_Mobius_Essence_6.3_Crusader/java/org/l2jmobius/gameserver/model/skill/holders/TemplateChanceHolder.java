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

/**
 * An object for holding template id and chance
 * @author Nik
 */
public class TemplateChanceHolder
{
	private final int _templateId;
	private final int _minChance;
	private final int _maxChance;
	
	public TemplateChanceHolder(int templateId, int minChance, int maxChance)
	{
		_templateId = templateId;
		_minChance = minChance;
		_maxChance = maxChance;
	}
	
	public int getTemplateId()
	{
		return _templateId;
	}
	
	public boolean calcChance(int chance)
	{
		return (_maxChance > chance) && (chance >= _minChance);
	}
	
	@Override
	public String toString()
	{
		return "[TemplateId: " + _templateId + " minChance: " + _minChance + " maxChance: " + _minChance + "]";
	}
}