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
package ai.areas.FantasyIsle;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

import ai.AbstractNpcAI;

/**
 * Fantasy Isle Parade
 * @author JOJO, Mobius
 */
public class Parade extends AbstractNpcAI
{
	// @formatter:off
	final int[] ACTORS =
	{
		32379,	0,	32379,
		32379,	0,	32379,
		32379,	0,	32379,
		0,	0,	0,
		32380,	0,	32380,
		32380,	32381,	32380,
		32380,	0,	32380,
		32380,	32381,	32380,
		0,	0,	0,
		32382,	32382,	32382,
		32382,	32383,	32382,
		32383,	32384,	32383,
		32383,	32384,	32383,
		0,	0,	0,
		0,	32385,	0,
		32385,	0,	32385,
		0,	32385,	0,
		0,	0,	0,
		32412,	0,	32411,
		0,	0,	0,
		32421,	0,	32409,
		32423,	0,	32422,
		0,	0,	0,
		32420,	32419,	32417,
		32418,	0,	32416,
		0,	0,	0,
		32414,	0,	32414,
		0,	32413,	0,
		32414,	0,	32414,
		0,	0,	0,
		32393,	0,	32394,
		0,	32430,	0,
		32392,	0,	32391,
		0,	0,	0,
		0,	32404,	0,
		32403,	0,	32401,
		0,	0,	0,
		0,	32408,	0,
		32406,	0,	32407,
		0,	32405,	0,
		0,	0,	0,
		32390,	32389,	32387,
		32388,	0,	32386,
		0,	0,	0,
		0,	32400,	0,
		32397,	32398,	32396,
		0,	0,	0,
		0,	32450,	0,
		32448,	32449,	32447,
		0,	0,	0,
		32380,	0,	32380,
		32380,	32381,	32380,
		32380,	0,	32380,
		32380,	32381,	32380,
		0,	0,	0,
		32379,	0,	32379,
		32379,	0,	32379,
		32379,	0,	32379,
		0,	0,	0,
		0,	32415,	0
	};
	
	//(Northbound 270 degrees) Route 1
	private final int[][] START1 = {{-54780, -56810, -2015, 49152},{-54860, -56810, -2015, 49152},{-54940, -56810, -2015, 49152}};
	private final int[][] GOAL1  = {{-54780, -57965, -2015, 49152},{-54860, -57965, -2015, 49152},{-54940, -57965, -2015, 49152}};
	//(Westbound 180 degrees) Route 2
	private final int[][] START2 = {{-55715, -58900, -2015, 32768},{-55715, -58820, -2015, 32768},{-55715, -58740, -2015, 32768}};
	private final int[][] GOAL2  = {{-60850, -58900, -2015, 32768},{-60850, -58820, -2015, 32768},{-60850, -58740, -2015, 32768}};
	//(Southbound 90 degrees) Route 3
	private final int[][] START3 = {{-61790, -57965, -2015, 16384},{-61710, -57965, -2015, 16384},{-61630, -57965, -2015, 16384}};
	private final int[][] GOAL3  = {{-61790, -53890, -2116, 16384},{-61710, -53890, -2116, 16384},{-61630, -53890, -2116, 16384}};
	//(Eastbound 0 degrees) Route 4
	private final int[][] START4 = {{-60840, -52990, -2108, 0},{-60840, -53070, -2108, 0},{-60840, -53150, -2108, 0}};
	private final int[][] GOAL4  = {{-58620, -52990, -2015, 0},{-58620, -53070, -2015, 0},{-58620, -53150, -2015, 0}};
	//(To 315 degrees northeast) Route 5
	private final int[][] START5 = {{-57233, -53554, -2015, 57344},{-57290, -53610, -2015, 57344},{-57346, -53667, -2015, 57344}};
	private final int[][] GOAL5  = {{-55338, -55435, -2015, 57344},{-55395, -55491, -2015, 57344},{-55451, -55547, -2015, 57344}};
	
	final int[][][] START = {START1, START2, START3, START4, START5};
	final int[][][] GOAL  = {GOAL1, GOAL2, GOAL3, GOAL4, GOAL5};
	// @formatter:on
	
	int npcIndex;
	Collection<Npc> spawns = ConcurrentHashMap.newKeySet();
	ScheduledFuture<?> spawnTask;
	ScheduledFuture<?> deleteTask;
	ScheduledFuture<?> cleanTask;
	
	public Parade()
	{
		// Starts at 8:00 and repeats every 6 hours.
		final long diff = timeLeftMilli(8, 0, 0);
		final long cycle = 3600000;
		ThreadPool.scheduleAtFixedRate(new Start(), diff, cycle);
		
		// Test - Starts 3 minutes after server startup and repeats every 20 minutes.
		// final long diff = timeLeftMilli(8, 0, 0), cycle = 600000;
		// ThreadPoolManager.scheduleAtFixedRate(new Start(), 180000, cycle);
		
		LOGGER.info("Fantasy Isle: Parade starting at " + new SimpleDateFormat("yyyy/MM/dd HH:mm").format(System.currentTimeMillis() + diff) + " and is scheduled each next " + (cycle / 3600000) + " hours.");
	}
	
	void load()
	{
		npcIndex = 0;
	}
	
	void clean()
	{
		for (Npc spawn : spawns)
		{
			if (spawn != null)
			{
				spawn.deleteMe();
			}
		}
		spawns.clear();
	}
	
	private long timeLeftMilli(int hh, int mm, int ss)
	{
		final int now = (GameTimeTaskManager.getInstance().getGameTicks() * 60) / 100;
		int dd = ((hh * 3600) + (mm * 60) + ss) - (now % 86400);
		if (dd < 0)
		{
			dd += 86400;
		}
		return (dd * 1000) / 6;
	}
	
	class Start implements Runnable
	{
		@Override
		public void run()
		{
			load();
			spawnTask = ThreadPool.scheduleAtFixedRate(new Spawn(), 0, 5000);
			deleteTask = ThreadPool.scheduleAtFixedRate(new Delete(), 10000, 1000);
			cleanTask = ThreadPool.schedule(new Clean(), 420000);
		}
	}
	
	class Spawn implements Runnable
	{
		@Override
		public void run()
		{
			for (int i = 0; i < 3; ++i)
			{
				if (npcIndex >= ACTORS.length)
				{
					spawnTask.cancel(false);
					break;
				}
				final int npcId = ACTORS[npcIndex++];
				if (npcId == 0)
				{
					continue;
				}
				for (int route = 0; route < 5; ++route)
				{
					final int[] start = START[route][i];
					final int[] goal = GOAL[route][i];
					final Npc actor = addSpawn(npcId, start[0], start[1], start[2], start[3], false, 0);
					actor.setRunning();
					actor.getAI().setIntention(Intention.MOVE_TO, new Location(goal[0], goal[1], goal[2], goal[3]));
					spawns.add(actor);
				}
			}
		}
	}
	
	class Delete implements Runnable
	{
		@Override
		public void run()
		{
			if (spawns.isEmpty())
			{
				return;
			}
			for (Npc actor : spawns)
			{
				if (actor != null)
				{
					if (actor.calculateDistance2D(actor.getXdestination(), actor.getYdestination(), 0) < 100)
					{
						actor.deleteMe();
						spawns.remove(actor);
					}
					else if (!actor.isMoving())
					{
						actor.getAI().setIntention(Intention.MOVE_TO, new Location(actor.getXdestination(), actor.getYdestination(), actor.getZdestination(), actor.getHeading()));
					}
				}
			}
			if ((spawns.isEmpty()) && (deleteTask != null))
			{
				deleteTask.cancel(false);
			}
		}
	}
	
	class Clean implements Runnable
	{
		@Override
		public void run()
		{
			if (spawnTask != null)
			{
				spawnTask.cancel(true);
			}
			if (deleteTask != null)
			{
				deleteTask.cancel(true);
			}
			if (cleanTask != null)
			{
				cleanTask.cancel(true);
			}
			clean();
		}
	}
	
	public static void main(String[] args)
	{
		new Parade();
	}
}