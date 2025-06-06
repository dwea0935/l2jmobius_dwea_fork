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
 * For the Sake of the Territory - Innadril (722)
 * @author Gigiikun
 */
public class Q00722_ForTheSakeOfTheTerritoryInnadril extends TerritoryWarSuperClass
{
	public Q00722_ForTheSakeOfTheTerritoryInnadril()
	{
		super(722, "For the Sake of the Territory - Innadril");
		CATAPULT_ID = 36504;
		TERRITORY_ID = 86;
		LEADER_IDS = new int[]
		{
			36538,
			36540,
			36543,
			36596
		};
		GUARD_IDS = new int[]
		{
			36539,
			36541,
			36542
		};
		npcString = new String[]
		{
			"The catapult of Innadril has been destroyed!"
		};
		registerKillIds();
	}
}
