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
package ai.areas.DragonValley;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

import ai.AbstractNpcAI;

/**
 * Dragon Valley Area Mobs AI.
 * @info When you kill certain monsters within the zone at a certain time, special monsters will appear. The type of the monster depends on the time of day. Daytime: Behemoth Dragon Nighttime: Soul Hunter
 * @author CostyKiller
 */
public final class DragonValleyDayNightMonsters extends AbstractNpcAI
{
	// Trigger Mobs
	private static final int DRAGON_PELTAST = 24617; // Dragon Peltast
	private static final int DRAGON_OFFICER = 24618; // Dragon Officer
	// Special Mobs
	private static final int BEHEMOTH_DRAGON = 24619; // Behemoth Dragon
	private static final int SOUL_HUNTER = 24620; // Soul Hunter
	
	// Misc
	private static final int MOB_SPAWN_CHANCE = 1; // 1% chance to spawn
	
	private DragonValleyDayNightMonsters()
	{
		super();
		addKillId(DRAGON_PELTAST, DRAGON_OFFICER);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (getRandom(100) < MOB_SPAWN_CHANCE)
		{
			addSpawn(GameTimeTaskManager.getInstance().isNight() ? SOUL_HUNTER : BEHEMOTH_DRAGON, npc, true, 0, true);
		}
	}
	
	public static void main(String[] args)
	{
		new DragonValleyDayNightMonsters();
	}
}