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
package org.l2jmobius.gameserver.taskmanagers;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.TraceUtil;
import org.l2jmobius.gameserver.entity.actor.Creature;

/**
 * Movement task manager class.
 * @author Mobius
 */
public class MovementTaskManager
{
	protected static final Logger LOGGER = Logger.getLogger(MovementTaskManager.class.getName());
	
	// Power-of-two shard counts for deterministic lock-free routing via Fibonacci hashing.
	private static final int PLAYER_SHARD_COUNT = 4;
	private static final int PLAYER_SHARD_MASK = PLAYER_SHARD_COUNT - 1;
	private static final int CREATURE_SHARD_COUNT = 8;
	private static final int CREATURE_SHARD_MASK = CREATURE_SHARD_COUNT - 1;
	
	private static final int POOL_SIZE_CREATURE = 512;
	private static final int POOL_SIZE_PLAYER = 256;
	private static final int TASK_DELAY_CREATURE = 100;
	private static final int TASK_DELAY_PLAYER = 50;
	
	@SuppressWarnings("unchecked")
	private static final Set<Creature>[] PLAYER_SHARDS = new Set[PLAYER_SHARD_COUNT];
	@SuppressWarnings("unchecked")
	private static final Set<Creature>[] CREATURE_SHARDS = new Set[CREATURE_SHARD_COUNT];
	
	protected MovementTaskManager()
	{
		for (int i = 0; i < PLAYER_SHARD_COUNT; i++)
		{
			final Set<Creature> shard = ConcurrentHashMap.newKeySet(POOL_SIZE_PLAYER);
			PLAYER_SHARDS[i] = shard;
			final int initialDelay = TASK_DELAY_PLAYER + ((i * TASK_DELAY_PLAYER) / PLAYER_SHARD_COUNT);
			ThreadPool.schedulePriorityTaskAtFixedRate(new Movement(shard), initialDelay, TASK_DELAY_PLAYER);
		}
		
		for (int i = 0; i < CREATURE_SHARD_COUNT; i++)
		{
			final Set<Creature> shard = ConcurrentHashMap.newKeySet(POOL_SIZE_CREATURE);
			CREATURE_SHARDS[i] = shard;
			final int initialDelay = TASK_DELAY_CREATURE + ((i * TASK_DELAY_CREATURE) / CREATURE_SHARD_COUNT);
			ThreadPool.scheduleAtFixedRate(new Movement(shard), initialDelay, TASK_DELAY_CREATURE);
		}
	}
	
	private static class Movement implements Runnable
	{
		private final Set<Creature> _creatures;
		
		public Movement(Set<Creature> creatures)
		{
			_creatures = creatures;
		}
		
		@Override
		public void run()
		{
			if (_creatures.isEmpty())
			{
				return;
			}
			
			Creature creature;
			final Iterator<Creature> iterator = _creatures.iterator();
			while (iterator.hasNext())
			{
				creature = iterator.next();
				boolean removed = false;
				try
				{
					if (creature.updatePosition())
					{
						iterator.remove();
						removed = true;
						creature.getAI().notifyActionArrived();
					}
				}
				catch (Exception e)
				{
					if (!removed)
					{
						iterator.remove();
					}
					LOGGER.warning("MovementTaskManager: Problem updating position of " + creature);
					LOGGER.warning(TraceUtil.getStackTrace(e));
				}
			}
		}
	}
	
	/**
	 * Registers a {@link Creature} for periodic position updates using lock-free $O(1)$ deterministic Fibonacci hashing on {@code objectId}.
	 * @param creature The Creature to add to moving objects of MovementTaskManager
	 */
	public void registerMovingObject(Creature creature)
	{
		final int hash = creature.getObjectId() * 0x9E3779B9;
		if (creature.isPlayer())
		{
			PLAYER_SHARDS[(hash >>> 30) & PLAYER_SHARD_MASK].add(creature);
		}
		else
		{
			CREATURE_SHARDS[(hash >>> 29) & CREATURE_SHARD_MASK].add(creature);
		}
	}
	
	public static MovementTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final MovementTaskManager INSTANCE = new MovementTaskManager();
	}
}
