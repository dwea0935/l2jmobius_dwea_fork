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
package quests.Q00100_SagaOfTheMaestro;

import quests.AbstractSagaQuest;

/**
 * Saga of the Maestro (100)
 * @author Emperorc
 */
public class Q00100_SagaOfTheMaestro extends AbstractSagaQuest
{
	public Q00100_SagaOfTheMaestro()
	{
		super(100, "Saga of the Maestro");
		_npc = new int[]
		{
			31592,
			31273,
			31597,
			31597,
			31596,
			31646,
			31648,
			31653,
			31654,
			31655,
			31656,
			31597
		};
		_items = new int[]
		{
			7080,
			7607,
			7081,
			7515,
			7298,
			7329,
			7360,
			7391,
			7422,
			7453,
			7108,
			0
		};
		_mob = new int[]
		{
			27260,
			27249,
			27308
		};
		_classId = new int[]
		{
			118
		};
		_prevClass = new int[]
		{
			0x39
		};
		_x = new int[]
		{
			164650,
			47429,
			47391
		};
		_y = new int[]
		{
			-74121,
			-56923,
			-56929
		};
		_z = new int[]
		{
			-2871,
			-2383,
			-2370
		};
		_text = new String[]
		{
			"PLAYERNAME! Pursued to here! However, I jumped out of the Banshouren boundaries! You look at the giant as the sign of power!",
			"... Oh ... good! So it was ... let's begin!",
			"I do not have the patience ..! I have been a giant force ...! Cough chatter ah ah ah!",
			"Paying homage to those who disrupt the orderly will be PLAYERNAME's death!",
			"Now, my soul freed from the shackles of the millennium, Halixia, to the back side I come ...",
			"Why do you interfere others' battles?",
			"This is a waste of time.. Say goodbye...!",
			"...That is the enemy",
			"...Goodness! PLAYERNAME you are still looking?",
			"PLAYERNAME ... Not just to whom the victory. Only personnel involved in the fighting are eligible to share in the victory.",
			"Your sword is not an ornament. Don't you think, PLAYERNAME?",
			"Goodness! I no longer sense a battle there now.",
			"let...",
			"Only engaged in the battle to bar their choice. Perhaps you should regret.",
			"The human nation was foolish to try and fight a giant's strength.",
			"Must...Retreat... Too...Strong.",
			"PLAYERNAME. Defeat...by...retaining...and...Mo...Hacker",
			"....! Fight...Defeat...It...Fight...Defeat...It..."
		};
		registerNPCs();
	}
}