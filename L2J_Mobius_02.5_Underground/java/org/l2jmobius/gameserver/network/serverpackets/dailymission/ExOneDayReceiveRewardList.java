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
package org.l2jmobius.gameserver.network.serverpackets.dailymission;

import java.time.LocalDate;
import java.util.Collection;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.xml.DailyMissionData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.DailyMissionDataHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Sdw
 */
public class ExOneDayReceiveRewardList extends ServerPacket
{
	final Player _player;
	private final Collection<DailyMissionDataHolder> _rewards;
	
	public ExOneDayReceiveRewardList(Player player)
	{
		_player = player;
		_rewards = DailyMissionData.getInstance().getDailyMissionData(player);
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		if (!DailyMissionData.getInstance().isAvailable())
		{
			return;
		}
		
		ServerPackets.EX_ONE_DAY_RECEIVE_REWARD_LIST.writeId(this, buffer);
		buffer.writeInt(_player.getPlayerClass().getId());
		buffer.writeInt(LocalDate.now().getDayOfWeek().ordinal()); // Day of week
		buffer.writeInt(_rewards.size());
		for (DailyMissionDataHolder reward : _rewards)
		{
			buffer.writeShort(reward.getId());
			buffer.writeByte(reward.getStatus(_player));
			buffer.writeByte(reward.getRequiredCompletions() > 0);
			buffer.writeInt(reward.getProgress(_player));
			buffer.writeInt(reward.getRequiredCompletions());
		}
	}
}
