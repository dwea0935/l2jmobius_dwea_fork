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
package org.l2jmobius.gameserver.model.events.holders.actor.player;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.IBaseEvent;

/**
 * @author UnAfraid
 */
public class OnPlayerReputationChanged implements IBaseEvent
{
	private final Player _player;
	private final int _oldReputation;
	private final int _newReputation;
	
	public OnPlayerReputationChanged(Player player, int oldReputation, int newReputation)
	{
		_player = player;
		_oldReputation = oldReputation;
		_newReputation = newReputation;
	}
	
	public Player getPlayer()
	{
		return _player;
	}
	
	public int getOldReputation()
	{
		return _oldReputation;
	}
	
	public int getNewReputation()
	{
		return _newReputation;
	}
	
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_REPUTATION_CHANGED;
	}
}
