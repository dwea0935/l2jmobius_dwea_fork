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
package ai.bosses.TowerOfInsolence;

import org.l2jmobius.commons.time.SchedulingPattern;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;

import ai.AbstractNpcAI;

/**
 * @author gugaf
 */
public class RamdalGroup extends AbstractNpcAI
{
	// NPCs
	private static final int RAMDAL = 25444;
	private static final int HALLATE = 25220;
	private static final int KIEGE = 25968;
	private static final int MARDIL = 25447;
	private static final int RANKU = 25970;
	private static final int KORIM = 25092;
	private static final int QUEENDARKNESS = 25973;
	private static final int KERNON = 25054;
	private static final int RAFAEL = 25976;
	private static final int GOLKONDA = 25126;
	private static final int MALISHA = 25979;
	private static final int SHURIEL = 25143;
	private static final int GALAXIA = 25450;
	// Locations
	private static final Location RAMDAL_LOC = new Location(112565, 16055, -4376);
	private static final Location HALLATE_LOC = new Location(114635, 18179, -2896);
	private static final Location KIEGE_LOC = new Location(113205, 17440, -1408);
	private static final Location MARDIL_LOC = new Location(114660, 13914, 64);
	private static final Location RANKU_LOC = new Location(114503, 19846, 936);
	private static final Location KORIM_LOC = new Location(112970, 14285, 1944);
	private static final Location QUEENDARKNESS_LOC = new Location(110912, 15914, 2952);
	private static final Location KERNON_LOC = new Location(114646, 13517, 3968);
	private static final Location RAFAEL_LOC = new Location(112366, 16992, 4976);
	private static final Location GOLKONDA_LOC = new Location(112422, 15014, 5984);
	private static final Location MALISHA_LOC = new Location(114641, 16071, 7000);
	private static final Location SHURIEL_LOC = new Location(114243, 14482, 7992);
	private static final Location GALAXIA_LOC = new Location(113430, 14836, 9560);
	// Misc
	private static final String RAMDAL_RESPAWN_PATTERN = "30 16 * * * | 30 21 * * *";
	private SchedulingPattern _respawnPattern = null;
	
	public RamdalGroup()
	{
		addKillId(RAMDAL, HALLATE, KIEGE, MARDIL, RANKU, KORIM, QUEENDARKNESS, KERNON, RAFAEL, GOLKONDA, MALISHA, SHURIEL, GALAXIA);
		
		_respawnPattern = new SchedulingPattern(RAMDAL_RESPAWN_PATTERN);
		
		final long nextRespawnTime = getNextRespawnTime();
		if (nextRespawnTime > 0)
		{
			startQuestTimer("respawn_randal", nextRespawnTime, null, null);
		}
		else
		{
			addSpawn(RAMDAL, RAMDAL_LOC, false, getDespawnTime());
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("respawn_randal"))
		{
			addSpawn(RAMDAL, RAMDAL_LOC, false, getDespawnTime());
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		switch (npc.getId())
		{
			case RAMDAL:
			{
				addSpawn(HALLATE, HALLATE_LOC, false, getDespawnTime());
				final long nextRespawnTime = getNextRespawnTime();
				if (nextRespawnTime > 0)
				{
					startQuestTimer("respawn_randal", nextRespawnTime, null, null);
				}
				else
				{
					addSpawn(RAMDAL, RAMDAL_LOC, false, getDespawnTime());
				}
				break;
			}
			case HALLATE:
			{
				addSpawn(KIEGE, KIEGE_LOC, false, getDespawnTime());
				break;
			}
			case KIEGE:
			{
				addSpawn(MARDIL, MARDIL_LOC, false, getDespawnTime());
				break;
			}
			case MARDIL:
			{
				addSpawn(RANKU, RANKU_LOC, false, getDespawnTime());
				break;
			}
			case RANKU:
			{
				addSpawn(KORIM, KORIM_LOC, false, getDespawnTime());
				break;
			}
			case KORIM:
			{
				addSpawn(QUEENDARKNESS, QUEENDARKNESS_LOC, false, getDespawnTime());
				break;
			}
			case QUEENDARKNESS:
			{
				addSpawn(KERNON, KERNON_LOC, false, getDespawnTime());
				break;
			}
			case KERNON:
			{
				addSpawn(RAFAEL, RAFAEL_LOC, false, getDespawnTime());
				break;
			}
			case RAFAEL:
			{
				addSpawn(GOLKONDA, GOLKONDA_LOC, false, getDespawnTime());
				break;
			}
			case GOLKONDA:
			{
				addSpawn(MALISHA, MALISHA_LOC, false, getDespawnTime());
				break;
			}
			case MALISHA:
			{
				addSpawn(SHURIEL, SHURIEL_LOC, false, getDespawnTime());
				break;
			}
			case SHURIEL:
			{
				addSpawn(GALAXIA, GALAXIA_LOC, false, getDespawnTime());
				break;
			}
			case GALAXIA:
			{
				// Do nothing.
				break;
			}
		}
	}
	
	private long getDespawnTime()
	{
		final long currentTime = System.currentTimeMillis();
		return _respawnPattern.next(currentTime) - currentTime - 60000 /* 1 minute less */;
	}
	
	private long getNextRespawnTime()
	{
		final long currentTime = System.currentTimeMillis();
		return _respawnPattern.next(currentTime) - currentTime;
	}
	
	public static void main(String[] args)
	{
		new RamdalGroup();
	}
}
