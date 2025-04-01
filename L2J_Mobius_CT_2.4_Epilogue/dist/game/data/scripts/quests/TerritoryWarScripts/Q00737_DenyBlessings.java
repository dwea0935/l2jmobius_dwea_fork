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
package quests.TerritoryWarScripts;

/**
 * Deny Blessings (737)
 * @author Gigiikun
 */
public class Q00737_DenyBlessings extends TerritoryWarSuperClass
{
	public Q00737_DenyBlessings()
	{
		super(737, "Deny Blessings");
		CLASS_IDS = new int[]
		{
			43,
			112,
			30,
			105,
			16,
			97,
			17,
			98,
			52,
			116
		};
		RANDOM_MIN = 3;
		RANDOM_MAX = 8;
		npcString = new String[]
		{
			"You have defeated $s2 of $s1 healers and buffers.",
			"You weakened the enemy's attack!"
		};
	}
}
