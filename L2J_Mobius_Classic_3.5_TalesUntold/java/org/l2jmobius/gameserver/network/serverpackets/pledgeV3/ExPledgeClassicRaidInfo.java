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
package org.l2jmobius.gameserver.network.serverpackets.pledgeV3;

import java.util.Comparator;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.xml.ClanRewardData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanRewardBonus;
import org.l2jmobius.gameserver.model.clan.enums.ClanRewardType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author GolbergSoft
 */
public class ExPledgeClassicRaidInfo extends ServerPacket
{
	private final Player _player;
	
	public ExPledgeClassicRaidInfo(Player player)
	{
		_player = player;
	}
	
	@Override
	protected void writeImpl(GameClient client, WritableBuffer buffer)
	{
		final Clan clan = _player.getClan();
		if (clan == null)
		{
			return;
		}
		
		ServerPackets.EX_PLEDGE_CLASSIC_RAID_INFO.writeId(this, buffer);
		buffer.writeInt(clan.getMaxRaidLevel());
		buffer.writeInt(5);
		
		ClanRewardData.getInstance().getClanRewardBonuses(ClanRewardType.RAID_ARENA).stream().sorted(Comparator.comparingInt(ClanRewardBonus::getLevel)).forEach(bonus ->
		{
			if (bonus.getSkillReward() != null)
			{
				buffer.writeInt(bonus.getSkillReward().getSkillId());
				buffer.writeInt(bonus.getSkillReward().getSkillLevel());
			}
		});
	}
}
