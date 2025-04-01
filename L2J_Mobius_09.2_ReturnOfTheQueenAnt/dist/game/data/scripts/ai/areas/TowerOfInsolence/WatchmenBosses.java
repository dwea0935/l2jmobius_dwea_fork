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
package ai.areas.TowerOfInsolence;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;

import ai.AbstractNpcAI;

/**
 * @author Mobius
 */
public class WatchmenBosses extends AbstractNpcAI
{
	// NPCs
	private static final int WATCHMAN_OF_THE_FORGOTTEN = 24555;
	private static final int WATCHMAN_OF_THE_RESURRECTED = 24561;
	private static final int WATCHMAN_OF_THE_CURSED = 24567;
	// Locations
	private static final Location[] WATCHMAN_OF_THE_FORGOTTEN_LOCATIONS =
	{
		new Location(-86568, 19304, -15429),
		new Location(-82024, 20440, -15426),
		new Location(-79624, 18488, -15429),
		new Location(-77560, 16184, -15426),
		new Location(-77656, 12776, -15429),
		new Location(-82024, 11848, -15426),
		new Location(-80152, 14328, -15429),
		new Location(-83864, 18136, -15429),
		new Location(-83976, 14296, -15429),
	};
	private static final Location[] WATCHMAN_OF_THE_RESURRECTED_LOCATIONS =
	{
		new Location(-83944, 17960, -12933),
		new Location(-80120, 14184, -12933),
		new Location(-80168, 17992, -12933),
		new Location(-83816, 14296, -12933),
		new Location(-86504, 16168, -12929),
		new Location(-85912, 12088, -12933),
		new Location(-82024, 11720, -12929),
		new Location(-80088, 10936, -12933),
		new Location(-77560, 16152, -12929),
	};
	private static final Location[] WATCHMAN_OF_THE_CURSED_LOCATIONS =
	{
		new Location(-77673, 16163, -10306),
		new Location(-79384, 18344, -10309),
		new Location(-82024, 20584, -10306),
		new Location(-84344, 18552, -10309),
		new Location(-86360, 16168, -10306),
		new Location(-86104, 12888, -10309),
		new Location(-83736, 10792, -10309),
		new Location(-83448, 14792, -10306),
		new Location(-80088, 17336, -10309),
		new Location(-84216, 17208, -10309),
		new Location(-80904, 13976, -10309),
	};
	
	private WatchmenBosses()
	{
		addKillId(WATCHMAN_OF_THE_FORGOTTEN, WATCHMAN_OF_THE_RESURRECTED, WATCHMAN_OF_THE_CURSED);
		
		startQuestTimer("SPAWN_WATCHMAN_OF_THE_FORGOTTEN", 1000, null, null);
		startQuestTimer("SPAWN_WATCHMAN_OF_THE_RESURRECTED", 1000, null, null);
		startQuestTimer("SPAWN_WATCHMAN_OF_THE_CURSED", 1000, null, null);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "SPAWN_WATCHMAN_OF_THE_FORGOTTEN":
			{
				addSpawn(WATCHMAN_OF_THE_FORGOTTEN, getRandomEntry(WATCHMAN_OF_THE_FORGOTTEN_LOCATIONS));
				break;
			}
			case "SPAWN_WATCHMAN_OF_THE_RESURRECTED":
			{
				addSpawn(WATCHMAN_OF_THE_RESURRECTED, getRandomEntry(WATCHMAN_OF_THE_RESURRECTED_LOCATIONS));
				break;
			}
			case "SPAWN_WATCHMAN_OF_THE_CURSED":
			{
				addSpawn(WATCHMAN_OF_THE_CURSED, getRandomEntry(WATCHMAN_OF_THE_CURSED_LOCATIONS));
				break;
			}
		}
		return null;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		switch (npc.getId())
		{
			case WATCHMAN_OF_THE_FORGOTTEN:
			{
				startQuestTimer("SPAWN_WATCHMAN_OF_THE_FORGOTTEN", 28800000, null, null);
				break;
			}
			case WATCHMAN_OF_THE_RESURRECTED:
			{
				startQuestTimer("SPAWN_WATCHMAN_OF_THE_RESURRECTED", 28800000, null, null);
				break;
			}
			case WATCHMAN_OF_THE_CURSED:
			{
				startQuestTimer("SPAWN_WATCHMAN_OF_THE_CURSED", 28800000, null, null);
				break;
			}
		}
	}
	
	public static void main(String[] args)
	{
		new WatchmenBosses();
	}
}
