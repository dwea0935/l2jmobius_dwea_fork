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

import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExRotation;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * @author JIV
 */
public class AnswerCoupleAction extends ClientPacket
{
	private int _objectId;
	private int _actionId;
	private int _answer;
	
	@Override
	protected void readImpl()
	{
		_actionId = readInt();
		_answer = readInt();
		_objectId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		final Player target = World.getInstance().getPlayer(_objectId);
		if ((player == null) || (target == null))
		{
			return;
		}
		
		if ((target.getMultiSocialTarget() != player.getObjectId()) || (target.getMultiSociaAction() != _actionId))
		{
			return;
		}
		
		if (_answer == 0) // cancel
		{
			target.sendPacket(SystemMessageId.THE_COUPLE_ACTION_REQUEST_HAS_BEEN_DENIED);
		}
		else if (_answer == 1) // approve
		{
			final int distance = (int) player.calculateDistance2D(target);
			if ((distance > 125) || (distance < 15) || (player.getObjectId() == target.getObjectId()))
			{
				player.sendPacket(SystemMessageId.THE_REQUEST_CANNOT_BE_COMPLETED_BECAUSE_THE_TARGET_DOES_NOT_MEET_LOCATION_REQUIREMENTS);
				target.sendPacket(SystemMessageId.THE_REQUEST_CANNOT_BE_COMPLETED_BECAUSE_THE_TARGET_DOES_NOT_MEET_LOCATION_REQUIREMENTS);
				return;
			}
			int heading = LocationUtil.calculateHeadingFrom(player, target);
			player.broadcastPacket(new ExRotation(player.getObjectId(), heading));
			player.setHeading(heading);
			heading = LocationUtil.calculateHeadingFrom(target, player);
			target.setHeading(heading);
			target.broadcastPacket(new ExRotation(target.getObjectId(), heading));
			player.broadcastPacket(new SocialAction(player.getObjectId(), _actionId));
			target.broadcastPacket(new SocialAction(_objectId, _actionId));
		}
		else if (_answer == -1) // refused
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_SET_TO_REFUSE_COUPLE_ACTIONS_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(player);
			target.sendPacket(sm);
		}
		target.setMultiSocialAction(0, 0);
	}
}
