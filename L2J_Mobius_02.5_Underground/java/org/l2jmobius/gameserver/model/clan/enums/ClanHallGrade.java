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
package org.l2jmobius.gameserver.model.clan.enums;

/**
 * @author St3eT
 */
public enum ClanHallGrade
{
	GRADE_S(50),
	GRADE_A(40),
	GRADE_B(30),
	GRADE_C(20),
	GRADE_D(10),
	GRADE_NONE(0);
	
	private final int _gradeValue;
	
	private ClanHallGrade(int gradeValue)
	{
		_gradeValue = gradeValue;
	}
	
	/**
	 * @return the grade value.
	 */
	public int getGradeValue()
	{
		return _gradeValue;
	}
}