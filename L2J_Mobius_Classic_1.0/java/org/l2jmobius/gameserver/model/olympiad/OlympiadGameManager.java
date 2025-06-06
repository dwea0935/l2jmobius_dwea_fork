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
package org.l2jmobius.gameserver.model.olympiad;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.zone.type.OlympiadStadiumZone;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author GodKratos, DS
 */
public class OlympiadGameManager implements Runnable
{
	private static final Logger LOGGER = Logger.getLogger(OlympiadGameManager.class.getName());
	private static final int STADIUM_COUNT = 80; // TODO dynamic
	
	private volatile boolean _battleStarted = false;
	private final List<OlympiadStadium> _tasks;
	private int _delay = 0;
	
	protected OlympiadGameManager()
	{
		final Collection<OlympiadStadiumZone> zones = ZoneManager.getInstance().getAllZones(OlympiadStadiumZone.class);
		if ((zones == null) || zones.isEmpty())
		{
			throw new Error("No olympiad stadium zones defined !");
		}
		
		final OlympiadStadiumZone[] array = zones.toArray(new OlympiadStadiumZone[zones.size()]);
		_tasks = new ArrayList<>(STADIUM_COUNT);
		
		final int zonesCount = array.length;
		for (int i = 0; i < STADIUM_COUNT; i++)
		{
			final OlympiadStadium stadium = new OlympiadStadium(array[i % zonesCount], i);
			stadium.registerTask(new OlympiadGameTask(stadium));
			_tasks.add(stadium);
		}
		
		LOGGER.info("Olympiad System: Loaded " + _tasks.size() + " stadiums.");
	}
	
	protected final boolean isBattleStarted()
	{
		return _battleStarted;
	}
	
	protected void startBattle()
	{
		_battleStarted = true;
	}
	
	@Override
	public void run()
	{
		if (Olympiad.getInstance().isOlympiadEnd())
		{
			return;
		}
		
		if (Olympiad.getInstance().inCompPeriod())
		{
			AbstractOlympiadGame newGame;
			List<Set<Integer>> readyClassed = OlympiadManager.getInstance().hasEnoughRegisteredClassed();
			boolean readyNonClassed = OlympiadManager.getInstance().hasEnoughRegisteredNonClassed();
			if ((readyClassed != null) || readyNonClassed)
			{
				// reset delay broadcast
				_delay = 0;
				
				// set up the games queue
				for (int i = 0; i < _tasks.size(); i++)
				{
					final OlympiadGameTask task = _tasks.get(i).getTask();
					synchronized (task)
					{
						if (!task.isRunning())
						{
							// Fair arena distribution
							// 0,2,4,6,8.. arenas checked for classed or teams first
							if (readyClassed != null)
							{
								newGame = OlympiadGameClassed.createGame(i, readyClassed);
								if (newGame != null)
								{
									task.attachGame(newGame);
									continue;
								}
								readyClassed = null;
							}
							// 1,3,5,7,9.. arenas used for non-classed
							// also other arenas will be used for non-classed if no classed or teams available
							if (readyNonClassed)
							{
								newGame = OlympiadGameNonClassed.createGame(i, OlympiadManager.getInstance().getRegisteredNonClassBased());
								if (newGame != null)
								{
									task.attachGame(newGame);
									continue;
								}
								readyNonClassed = false;
							}
						}
					}
					
					// stop generating games if no more participants
					if ((readyClassed == null) && !readyNonClassed)
					{
						break;
					}
				}
			}
			// olympiad is delayed
			else
			{
				_delay++;
				if (_delay >= 10) // 5min
				{
					for (Integer id : OlympiadManager.getInstance().getRegisteredNonClassBased())
					{
						if (id == null)
						{
							continue;
						}
						
						final Player noble = World.getInstance().getPlayer(id);
						if (noble != null)
						{
							noble.sendPacket(new SystemMessage(SystemMessageId.THE_GAMES_MAY_BE_DELAYED_DUE_TO_AN_INSUFFICIENT_NUMBER_OF_PLAYERS_WAITING));
						}
					}
					
					for (Set<Integer> list : OlympiadManager.getInstance().getRegisteredClassBased().values())
					{
						for (Integer id : list)
						{
							if (id == null)
							{
								continue;
							}
							
							final Player noble = World.getInstance().getPlayer(id);
							if (noble != null)
							{
								noble.sendPacket(new SystemMessage(SystemMessageId.THE_GAMES_MAY_BE_DELAYED_DUE_TO_AN_INSUFFICIENT_NUMBER_OF_PLAYERS_WAITING));
							}
						}
					}
					
					_delay = 0;
				}
			}
		}
		else
		{
			// not in competition period
			if (isAllTasksFinished())
			{
				OlympiadManager.getInstance().clearRegistered();
				_battleStarted = false;
				// LOGGER.info("Olympiad System: All current games finished.");
			}
		}
	}
	
	public boolean isAllTasksFinished()
	{
		for (OlympiadStadium stadium : _tasks)
		{
			final OlympiadGameTask task = stadium.getTask();
			if (task.isRunning())
			{
				return false;
			}
		}
		return true;
	}
	
	public OlympiadGameTask getOlympiadTask(int id)
	{
		if ((id < 0) || (id >= _tasks.size()))
		{
			return null;
		}
		return _tasks.get(id).getTask();
	}
	
	public int getNumberOfStadiums()
	{
		return _tasks.size();
	}
	
	public void notifyCompetitorDamage(Player attacker, int damage)
	{
		if (attacker == null)
		{
			return;
		}
		
		final int id = attacker.getOlympiadGameId();
		if ((id < 0) || (id >= _tasks.size()))
		{
			return;
		}
		
		final AbstractOlympiadGame game = _tasks.get(id).getTask().getGame();
		if (game != null)
		{
			game.addDamage(attacker, damage);
		}
	}
	
	public List<OlympiadStadium> getTasks()
	{
		return _tasks;
	}
	
	public static OlympiadGameManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final OlympiadGameManager INSTANCE = new OlympiadGameManager();
	}
}
