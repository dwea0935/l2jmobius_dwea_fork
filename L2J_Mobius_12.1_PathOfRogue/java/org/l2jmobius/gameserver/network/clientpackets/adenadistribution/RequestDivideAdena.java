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
package org.l2jmobius.gameserver.network.clientpackets.adenadistribution;

import java.util.List;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.AdenaDistributionRequest;
import org.l2jmobius.gameserver.model.groups.CommandChannel;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.adenadistribution.ExDivideAdenaCancel;
import org.l2jmobius.gameserver.network.serverpackets.adenadistribution.ExDivideAdenaDone;

/**
 * @author Sdw
 */
public class RequestDivideAdena extends ClientPacket
{
	private int _adenaObjId;
	private long _adenaCount;
	
	@Override
	protected void readImpl()
	{
		_adenaObjId = readInt();
		_adenaCount = readLong();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final AdenaDistributionRequest request = player.getRequest(AdenaDistributionRequest.class);
		if (request == null)
		{
			return;
		}
		else if (request.getDistributor() != player)
		{
			cancelDistribution(request);
			return;
		}
		else if (request.getAdenaObjectId() != _adenaObjId)
		{
			cancelDistribution(request);
			return;
		}
		
		final Party party = player.getParty();
		if (party == null)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_DISTRIBUTE_ADENA_IF_YOU_ARE_NOT_A_MEMBER_OF_AN_ALLIANCE_OR_A_COMMAND_CHANNEL);
			cancelDistribution(request);
			return;
		}
		
		final CommandChannel commandChannel = party.getCommandChannel();
		if ((commandChannel != null) && !commandChannel.isLeader(player))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_PROCEED_AS_YOU_ARE_NOT_AN_ALLIANCE_LEADER_OR_PARTY_LEADER);
			cancelDistribution(request);
			return;
		}
		else if (!party.isLeader(player))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_PROCEED_AS_YOU_ARE_NOT_A_PARTY_LEADER);
			cancelDistribution(request);
			return;
		}
		
		final List<Player> targets = commandChannel != null ? commandChannel.getMembers() : party.getMembers();
		if (player.getAdena() < targets.size())
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_ADENA_2);
			cancelDistribution(request);
			return;
		}
		
		if (player.getAdena() < request.getAdenaCount())
		{
			player.sendPacket(SystemMessageId.THE_ADENA_IN_POSSESSION_HAS_BEEN_DECREASED_ADENA_DISTRIBUTION_HAS_BEEN_CANCELLED);
			cancelDistribution(request);
			return;
		}
		else if (targets.size() < request.getPlayers().size())
		{
			player.sendPacket(SystemMessageId.THE_DISTRIBUTION_PARTICIPANTS_HAVE_CHANGED_ADENA_DISTRIBUTION_HAS_BEEN_CANCELLED);
			cancelDistribution(request);
			return;
		}
		else if (player.getAdena() < _adenaCount)
		{
			player.sendPacket(SystemMessageId.DISTRIBUTION_CANNOT_PROCEED_AS_THERE_IS_INSUFFICIENT_ADENA_FOR_DISTRIBUTION);
			cancelDistribution(request);
			return;
		}
		
		final long memberAdenaGet = (long) Math.floor(_adenaCount / targets.size());
		if (player.reduceAdena(ItemProcessType.TRANSFER, memberAdenaGet * targets.size(), player, false))
		{
			for (Player target : targets)
			{
				if ((target == null))
				{
					// TODO : handle that case here + regive adena OR filter with Objects::nonNull on memberCount ?
					// those sys msg exists and bother me ADENA_WAS_NOT_DISTRIBUTED_TO_S1 / YOU_DID_NOT_RECEIVE_ADENA_DISTRIBUTION
					continue;
				}
				target.addAdena(ItemProcessType.TRANSFER, memberAdenaGet, player, false);
				target.sendPacket(new ExDivideAdenaDone(party.isLeader(target), (commandChannel != null) && commandChannel.isLeader(target), _adenaCount, memberAdenaGet, targets.size(), player.getName()));
				target.removeRequest(AdenaDistributionRequest.class);
			}
		}
		else
		{
			cancelDistribution(request);
		}
	}
	
	private void cancelDistribution(AdenaDistributionRequest request)
	{
		for (Player player : request.getPlayers())
		{
			if (player != null)
			{
				player.sendPacket(SystemMessageId.ADENA_DISTRIBUTION_HAS_BEEN_CANCELLED);
				player.sendPacket(ExDivideAdenaCancel.STATIC_PACKET);
				player.removeRequest(AdenaDistributionRequest.class);
			}
		}
	}
}