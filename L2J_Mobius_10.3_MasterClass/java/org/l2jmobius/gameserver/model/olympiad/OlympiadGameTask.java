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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.olympiad.ExOlympiadMatchInfo;

/**
 * @author DS
 */
public class OlympiadGameTask implements Runnable
{
	protected static final Logger LOGGER = Logger.getLogger(OlympiadGameTask.class.getName());
	
	private static final int[] TELEPORT_TO_ARENA_TIMES =
	{
		120,
		60,
		30,
		15,
		10,
		5,
		4,
		3,
		2,
		1,
		0
	};
	
	private static final int[] BATTLE_START_TIME_FIRST =
	{
		60,
		55,
		50,
		40,
		30,
		20,
		10,
		0
	};
	
	private static final int[] BATTLE_START_TIME_SECOND =
	{
		10,
		5,
		4,
		3,
		2,
		1,
		0
	};
	
	private static final int[] BATTLE_END_TIME_SECOND =
	{
		120,
		60,
		30,
		10,
		5
	};
	
	private static final int[] TELEPORT_TO_TOWN_TIMES =
	{
		40,
		30,
		20,
		10,
		5,
		4,
		3,
		2,
		1,
		0
	};
	
	private final OlympiadStadium _stadium;
	private AbstractOlympiadGame _game;
	private OlympiadGameState _state = OlympiadGameState.IDLE;
	private boolean _needAnnounce = false;
	private int _countDown = 0;
	
	public OlympiadGameTask(OlympiadStadium stadium)
	{
		_stadium = stadium;
		_stadium.registerTask(this);
	}
	
	public boolean isRunning()
	{
		return _state != OlympiadGameState.IDLE;
	}
	
	public boolean isGameStarted()
	{
		return (_state.ordinal() >= OlympiadGameState.GAME_STARTED.ordinal()) && (_state.ordinal() <= OlympiadGameState.CLEANUP.ordinal());
	}
	
	public boolean isBattleStarted()
	{
		return (_state == OlympiadGameState.BATTLE_IN_PROGRESS) || (_state == OlympiadGameState.ROUND_2) || (_state == OlympiadGameState.ROUND_3);
	}
	
	public boolean isBattleFinished()
	{
		return _state == OlympiadGameState.TELEPORT_TO_TOWN;
	}
	
	public boolean needAnnounce()
	{
		if (_needAnnounce)
		{
			_needAnnounce = false;
			return true;
		}
		return false;
	}
	
	public OlympiadStadium getStadium()
	{
		return _stadium;
	}
	
	public AbstractOlympiadGame getGame()
	{
		return _game;
	}
	
	public void attachGame(AbstractOlympiadGame game)
	{
		if ((game != null) && (_state != OlympiadGameState.IDLE))
		{
			LOGGER.warning("Attempt to overwrite non-finished game in state " + _state);
			return;
		}
		
		_game = game;
		_state = OlympiadGameState.BEGIN;
		_needAnnounce = false;
		ThreadPool.execute(this);
	}
	
	@Override
	public void run()
	{
		try
		{
			final String player1 = _game.getPlayerNames()[0];
			final String player2 = _game.getPlayerNames()[1];
			int delay = 1; // schedule next call after 1s
			switch (_state)
			{
				// Game created
				case BEGIN:
				{
					_state = OlympiadGameState.TELEPORT_TO_ARENA;
					_countDown = Config.OLYMPIAD_WAIT_TIME;
					break;
				}
				// Teleport to arena countdown
				case TELEPORT_TO_ARENA:
				{
					if (_countDown > 0)
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_WILL_BE_TAKEN_TO_THE_OLYMPIC_STADIUM_IN_S1_SEC);
						sm.addInt(_countDown);
						_game.broadcastPacket(sm);
					}
					
					if (_countDown == 1)
					{
						_game.untransformPlayers();
					}
					
					delay = getDelay(TELEPORT_TO_ARENA_TIMES);
					if (_countDown <= 0)
					{
						_state = OlympiadGameState.GAME_STARTED;
					}
					break;
				}
				// Game start, port players to arena
				case GAME_STARTED:
				{
					_stadium.makeZonePvPForCharsInside(false);
					if (!startGame())
					{
						_state = OlympiadGameState.GAME_STOPPED;
						break;
					}
					
					_state = OlympiadGameState.BATTLE_COUNTDOWN_FIRST;
					_countDown = BATTLE_START_TIME_FIRST[0];
					_stadium.updateZoneInfoForObservers(); // TODO lion temp hack for remove old info from client about prevoius match
					delay = 5;
					break;
				}
				// Battle start countdown, first part (60-10)
				case BATTLE_COUNTDOWN_FIRST:
				{
					if (_countDown > 0)
					{
						if (_countDown == 10)
						{
							_game.healPlayers();
						}
						else
						{
							final SystemMessage sm = new SystemMessage(SystemMessageId.THE_MATCH_BEGINS_IN_S1_SEC);
							sm.addInt(_countDown);
							_stadium.broadcastPacket(sm);
						}
					}
					
					delay = getDelay(BATTLE_START_TIME_FIRST);
					if (_countDown <= 0)
					{
						_game.makePlayersInvul();
						_game.resetDamage();
						_game.resetDamageFinal();
						_stadium.openDoors();
						
						_state = OlympiadGameState.BATTLE_COUNTDOWN_SECOND;
						_countDown = BATTLE_START_TIME_SECOND[0];
						delay = getDelay(BATTLE_START_TIME_SECOND);
					}
					break;
				}
				// Battle start countdown, second part (10-0)
				case BATTLE_COUNTDOWN_SECOND:
				{
					if (_countDown > 0)
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.THE_MATCH_BEGINS_IN_S1_SEC);
						sm.addInt(_countDown);
						_stadium.broadcastPacket(sm);
					}
					
					delay = getDelay(BATTLE_START_TIME_SECOND);
					if (_countDown <= 0)
					{
						_state = OlympiadGameState.BATTLE_STARTED;
						_game.removePlayersInvul();
					}
					break;
				}
				// Beginning of the battle
				case BATTLE_STARTED:
				{
					_countDown = (int) Config.OLYMPIAD_BATTLE / 1000;
					
					_game.broadcastPacket(new ExOlympiadMatchInfo(player1, player2, 0, 0, 1, 100));
					
					final SystemMessage round1 = new SystemMessage(SystemMessageId.HIDDEN_MSG_OLYMPIAD_ROUND_1);
					_stadium.broadcastPacket(round1);
					final SystemMessage start = new SystemMessage(SystemMessageId.HIDDEN_MSG_START_OLYMPIAD);
					_stadium.broadcastPacket(start);
					_game.broadcastOlympiadInfo(_stadium);
					_state = OlympiadGameState.ROUND_1; // set state first, used in zone update
					if (!startBattle())
					{
						_state = OlympiadGameState.GAME_STOPPED;
					}
					break;
				}
				case ROUND_1:
				{
					_state = OlympiadGameState.BATTLE_IN_PROGRESS; // set state first, used in zone update
					_countDown = (int) Config.OLYMPIAD_BATTLE / 1000;
					_stadium.makeZonePvPForCharsInside(true);
					if (!startBattle())
					{
						_state = OlympiadGameState.GAME_STOPPED;
					}
					break;
				}
				// Checks during battle
				case BATTLE_IN_PROGRESS:
				{
					_countDown -= 1;
					final int remaining = (int) Config.OLYMPIAD_BATTLE / 1000;
					for (int announceTime : BATTLE_END_TIME_SECOND)
					{
						if (announceTime == remaining)
						{
							final SystemMessage sm = new SystemMessage(SystemMessageId.THE_GAME_ENDS_IN_S1_SEC);
							sm.addInt(announceTime);
							_stadium.broadcastPacket(sm);
							break;
						}
					}
					if (roundCheck() || (_countDown <= 0))
					{
						round1();
						_game.makePlayersInvul();
						_state = OlympiadGameState.WAIT_TIME_1;
						_stadium.makeZonePvPForCharsInside(false);
						_countDown = 20;
					}
					else if (checkBattle())
					{
						_state = OlympiadGameState.GAME_STOPPED;
					}
					break;
				}
				case WAIT_TIME_1:
				{
					_countDown -= 1;
					if (_countDown == 14)
					{
						_game.buffPlayers();
						_game.portPlayersToSpots(_stadium.getZone().getSpawns(), _stadium.getInstance());
						_game.broadcastOlympiadInfo(_stadium);
					}
					if (_countDown <= 0)
					{
						_game.removePlayersInvul();
						_state = OlympiadGameState.ROUND_2;
						_countDown = (int) Config.OLYMPIAD_BATTLE / 1000;
						final SystemMessage round2 = new SystemMessage(SystemMessageId.HIDDEN_MSG_OLYMPIAD_ROUND_2);
						_stadium.broadcastPacket(round2);
						final SystemMessage start = new SystemMessage(SystemMessageId.HIDDEN_MSG_START_OLYMPIAD);
						_stadium.broadcastPacket(start);
						_stadium.makeZonePvPForCharsInside(true);
						_game.broadcastOlympiadInfo(_stadium);
					}
					break;
				}
				case ROUND_2:
				{
					_countDown -= 1;
					final int remaining = (int) Config.OLYMPIAD_BATTLE / 1000;
					for (int announceTime : BATTLE_END_TIME_SECOND)
					{
						if (announceTime == remaining)
						{
							final SystemMessage sm = new SystemMessage(SystemMessageId.THE_GAME_ENDS_IN_S1_SEC);
							sm.addInt(announceTime);
							_stadium.broadcastPacket(sm);
							break;
						}
					}
					if (roundCheck() || (_countDown <= 0))
					{
						round2();
						if (_game.isMatchEnd())
						{
							_state = OlympiadGameState.GAME_STOPPED;
							break;
						}
						_state = OlympiadGameState.WAIT_TIME_2;
						_game.makePlayersInvul();
						_stadium.makeZonePvPForCharsInside(false);
						_countDown = 20;
					}
					else if (checkBattle())
					{
						_state = OlympiadGameState.GAME_STOPPED;
					}
					break;
				}
				case WAIT_TIME_2:
				{
					_countDown -= 1;
					if (_countDown == 14)
					{
						_game.buffPlayers();
						_game.roundTwoCleanUp();
						_game.broadcastOlympiadInfo(_stadium);
						_game.portPlayersToSpots(_stadium.getZone().getSpawns(), _stadium.getInstance());
					}
					if (_countDown <= 0)
					{
						_state = OlympiadGameState.ROUND_3;
						_game.removePlayersInvul();
						_countDown = (int) Config.OLYMPIAD_BATTLE / 1000;
						final SystemMessage round2 = new SystemMessage(SystemMessageId.HIDDEN_MSG_OLYMPIAD_ROUND_3);
						_stadium.broadcastPacket(round2);
						final SystemMessage start = new SystemMessage(SystemMessageId.HIDDEN_MSG_START_OLYMPIAD);
						_stadium.broadcastPacket(start);
						_stadium.makeZonePvPForCharsInside(true);
						_game.broadcastOlympiadInfo(_stadium);
					}
					break;
				}
				case ROUND_3:
				{
					_countDown -= 1;
					final int remaining = (int) Config.OLYMPIAD_BATTLE / 1000;
					for (int announceTime : BATTLE_END_TIME_SECOND)
					{
						if (announceTime == remaining)
						{
							final SystemMessage sm = new SystemMessage(SystemMessageId.THE_GAME_ENDS_IN_S1_SEC);
							sm.addInt(announceTime);
							_stadium.broadcastPacket(sm);
							break;
						}
					}
					if (roundCheck() || (_countDown <= 0) || checkBattle())
					{
						round3();
						_game.makePlayersInvul();
						_stadium.makeZonePvPForCharsInside(false);
					}
					break;
				}
				// End of the battle
				case GAME_STOPPED:
				{
					_state = OlympiadGameState.TELEPORT_TO_TOWN;
					_countDown = TELEPORT_TO_TOWN_TIMES[0];
					stopGame();
					delay = getDelay(TELEPORT_TO_TOWN_TIMES);
					break;
				}
				// Teleport to town countdown
				case TELEPORT_TO_TOWN:
				{
					if (_countDown > 0)
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_WILL_BE_MOVED_BACK_TO_TOWN_IN_S1_SECOND_S);
						sm.addInt(_countDown);
						_game.broadcastPacket(sm);
					}
					
					delay = getDelay(TELEPORT_TO_TOWN_TIMES);
					if (_countDown <= 0)
					{
						_state = OlympiadGameState.CLEANUP;
					}
					break;
				}
				// Removals
				case CLEANUP:
				{
					cleanupGame();
					_state = OlympiadGameState.IDLE;
					_game = null;
					return;
				}
			}
			ThreadPool.schedule(this, delay * 1000);
		}
		catch (Exception e)
		{
			switch (_state)
			{
				case GAME_STOPPED:
				case TELEPORT_TO_TOWN:
				case CLEANUP:
				case IDLE:
				{
					LOGGER.warning("Unable to return players back in town, exception: " + e.getMessage());
					_state = OlympiadGameState.IDLE;
					_game = null;
					return;
				}
			}
			
			LOGGER.log(Level.WARNING, "Exception in " + _state + ", trying to port players back: " + e.getMessage(), e);
			_state = OlympiadGameState.GAME_STOPPED;
			ThreadPool.schedule(this, 1000);
		}
	}
	
	private int getDelay(int[] times)
	{
		int time;
		for (int i = 0; i < (times.length - 1); i++)
		{
			time = times[i];
			if (time >= _countDown)
			{
				continue;
			}
			
			final int delay = _countDown - time;
			_countDown = time;
			return delay;
		}
		// should not happens
		_countDown = -1;
		return 1;
	}
	
	/**
	 * Second stage: check for defaulted, port players to arena, announce game.
	 * @return true if no participants defaulted.
	 */
	private boolean startGame()
	{
		try
		{
			// Checking for opponents and teleporting to arena
			if (_game.checkDefaulted())
			{
				return false;
			}
			
			_stadium.closeDoors();
			if (_game.needBuffers())
			{
				_stadium.spawnBuffers();
			}
			
			if (!_game.portPlayersToArena(_stadium.getZone().getSpawns(), _stadium.getInstance()))
			{
				return false;
			}
			
			_game.removals();
			_needAnnounce = true;
			OlympiadGameManager.getInstance().startBattle(); // inform manager
			return true;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		return false;
	}
	
	/**
	 * Fourth stage: last checks, remove buffers, start competition itself.
	 * @return true if all participants online and ready on the stadium.
	 */
	private boolean startBattle()
	{
		try
		{
			if (_game.needBuffers())
			{
				_stadium.deleteBuffers();
			}
			
			if (_game.checkBattleStatus() && _game.makeCompetitionStart())
			{
				// game successfully started
				_game.broadcastOlympiadInfo(_stadium);
				// _stadium.broadcastPacket(new SystemMessage(SystemMessageId.THE_MATCH_HAS_BEGUN_FIGHT));
				_stadium.updateZoneStatusForCharactersInside();
				return true;
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		return false;
	}
	
	private boolean roundCheck()
	{
		try
		{
			return _game.roundWinner();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		return true;
	}
	
	private void round1()
	{
		try
		{
			_game.validateRound1Winner(_stadium);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	private void round2()
	{
		try
		{
			_game.validateRound2Winner(_stadium);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	private void round3()
	{
		try
		{
			_game.validateRound3Winner(_stadium);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		try
		{
			_game.makePlayersInvul();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		try
		{
			_stadium.updateZoneStatusForCharactersInside();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		try
		{
			_state = OlympiadGameState.GAME_STOPPED;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	/**
	 * Fifth stage: battle is running, returns true if winner found.
	 * @return
	 */
	private boolean checkBattle()
	{
		try
		{
			return _game.haveWinner();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		return true;
	}
	
	/**
	 * Sixth stage: winner's validations
	 */
	private void stopGame()
	{
		try
		{
			_game.validateWinner(_stadium);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		try
		{
			_game.cleanEffects();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		try
		{
			_game.makePlayersInvul();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		try
		{
			_stadium.updateZoneStatusForCharactersInside();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	/**
	 * Seventh stage: game cleanup (port players back, closing doors, etc)
	 */
	private void cleanupGame()
	{
		try
		{
			_game.removePlayersInvul();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		try
		{
			_game.playersStatusBack();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		try
		{
			_game.portPlayersBack();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		try
		{
			_game.clearPlayers();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		try
		{
			_stadium.closeDoors();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
}