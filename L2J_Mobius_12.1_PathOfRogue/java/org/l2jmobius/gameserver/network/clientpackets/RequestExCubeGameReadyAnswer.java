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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.managers.HandysBlockCheckerManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.PacketLogger;

/**
 * Format: chddd d: Arena d: Answer
 * @author mrTJO
 */
public class RequestExCubeGameReadyAnswer extends ClientPacket
{
	private int _arena;
	private int _answer;
	
	@Override
	protected void readImpl()
	{
		// client sends -1,0,1,2 for arena parameter
		_arena = readInt() + 1;
		// client sends 1 if clicked confirm on not clicked, 0 if clicked cancel
		_answer = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		switch (_answer)
		{
			case 0:
			{
				// Cancel - Answer No
				break;
			}
			case 1:
			{
				// OK or Time Over
				HandysBlockCheckerManager.getInstance().increaseArenaVotes(_arena);
				break;
			}
			default:
			{
				PacketLogger.warning("Unknown Cube Game Answer ID: " + _answer);
				break;
			}
		}
	}
}
