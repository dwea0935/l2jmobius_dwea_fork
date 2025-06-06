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
 * Destroy Key Targets (738)
 * @author Gigiikun
 */
public class Q00738_DestroyKeyTargets extends TerritoryWarSuperClass
{
	public Q00738_DestroyKeyTargets()
	{
		super(738, "Destroy Key Targets");
		CLASS_IDS = new int[]
		{
			51,
			115,
			57,
			118
		};
		RANDOM_MIN = 3;
		RANDOM_MAX = 8;
		npcString = new String[]
		{
			"You have defeated $s2 of $s1 warsmiths and overlords.",
			"You destroyed the enemy's professionals!"
		};
	}
}
