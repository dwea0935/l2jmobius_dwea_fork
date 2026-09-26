/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ai.others;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * Party XP bonus of the party hunting zones.
 * @author GuruGel
 */
public class MonsterBonusXpParty extends Script
{
	// NPCs
	private static final int[] MONSTERS =
	{
		// Immortal Plateau
		23882, // Nerva Orc Magus
		23883, // Nerva Orc Scout
		23884, // Nerva Orc Reservist
		23885, // Nerva Orc Pikeman
		23886, // Kasha Orc Special Support Warrior
		23887, // Kasha Orc Support Berserker
		
		// Kasha Orcs' Logging Site
		23888, // Nerva Orc Priest
		23889, // Nerva Orc Bloodhound
		23890, // Nerva Orc Fanatic
		23891, // Nerva Orc Reservist
		23892, // Nerva Orc Footman
		23893, // Nerva Orc Woodcutter
		23894, // Nerva Orc Commander
		23895, // Kasha Orc Training Mentor
		
		// Kasha Orc Fortress
		23896, // Kasha Orc Shaman
		23897, // Kasha Orc Destroyer
		23898, // Kasha Orc Combatant
		23899, // Kasha Orc Berserker
		23900, // Kasha Orc Spearman
		23901, // Kasha Orc Elite Shaman
		23902, // Kasha Orc Elite Destroyer
		23903, // Kasha Orc Elite Combatant
		23904, // Kasha Orc Elite Berserker
		23905, // Kasha Orc Elite Spearman
	};
	
	// Misc
	private static final int BONUS_RADIUS = 1500;
	private static final int MIN_LEVEL = 118;
	private static final int MAX_LEVEL = 131;
	private static final double MAX_BONUS_PERCENTAGE = 0.25;
	
	private MonsterBonusXpParty()
	{
		addKillId(MONSTERS);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Party party = killer.getParty();
		if (party == null)
		{
			return;
		}
		
		for (Player member : party.getMembers())
		{
			final int level = member.getLevel();
			if ((level >= MIN_LEVEL) && (level <= MAX_LEVEL) && killer.isInsideRadius3D(member, BONUS_RADIUS))
			{
				// Add the bonus experience to the member after 1 second.
				final long bonusXp = (long) (npc.getExpReward(level) * getBonusPercentage(level));
				ThreadPool.schedule(() -> member.addExpAndSp(bonusXp, 0), 1000);
			}
		}
	}
	
	/**
	 * @param level the level of the party member, between {@link #MIN_LEVEL} and {@link #MAX_LEVEL}
	 * @return the share of the monster XP given as bonus, it grows with the level up to the maximum at level 128
	 */
	private static double getBonusPercentage(int level)
	{
		switch (level)
		{
			case 118:
			{
				return MAX_BONUS_PERCENTAGE * 0.1;
			}
			case 119:
			{
				return MAX_BONUS_PERCENTAGE * 0.2;
			}
			case 120:
			{
				return MAX_BONUS_PERCENTAGE * 0.3;
			}
			case 121:
			{
				return MAX_BONUS_PERCENTAGE * 0.4;
			}
			case 122:
			{
				return MAX_BONUS_PERCENTAGE * 0.5;
			}
			case 123:
			{
				return MAX_BONUS_PERCENTAGE * 0.6;
			}
			case 124:
			case 125:
			{
				return MAX_BONUS_PERCENTAGE * 0.8;
			}
			case 126:
			{
				return MAX_BONUS_PERCENTAGE * 0.9;
			}
			case 127:
			{
				return MAX_BONUS_PERCENTAGE * 0.95;
			}
			default:
			{
				return MAX_BONUS_PERCENTAGE;
			}
		}
	}
	
	public static void main(String[] args)
	{
		new MonsterBonusXpParty();
	}
}
