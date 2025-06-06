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
package org.l2jmobius.gameserver.network.serverpackets.pledgeV2;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.holders.ClanMasteryHolder;
import org.l2jmobius.gameserver.data.xml.ClanMasteryData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.AbstractItemPacket;

/**
 * @author Mobius
 */
public class ExPledgeMasteryInfo extends AbstractItemPacket
{
	private final Player _player;
	
	public ExPledgeMasteryInfo(Player player)
	{
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		final Clan clan = _player.getClan();
		if (clan == null)
		{
			return;
		}
		
		ServerPackets.EX_PLEDGE_MASTERY_INFO.writeId(this, buffer);
		buffer.writeInt(clan.getUsedDevelopmentPoints()); // Consumed development points
		buffer.writeInt(clan.getTotalDevelopmentPoints()); // Total development points
		buffer.writeInt(16); // Mastery count
		for (ClanMasteryHolder mastery : ClanMasteryData.getInstance().getMasteries())
		{
			if (mastery.getId() < 17)
			{
				final int id = mastery.getId();
				buffer.writeInt(id); // Mastery
				buffer.writeInt(0); // ?
				boolean available = true;
				if (clan.getLevel() < mastery.getClanLevel())
				{
					available = false;
				}
				else
				{
					final int previous = mastery.getPreviousMastery();
					final int previousAlt = mastery.getPreviousMasteryAlt();
					if (previousAlt > 0)
					{
						available = clan.hasMastery(previous) || clan.hasMastery(previousAlt);
					}
					else if (previous > 0)
					{
						available = clan.hasMastery(previous);
					}
				}
				buffer.writeByte(clan.hasMastery(id) ? 2 : available ? 1 : 0); // Availability.
			}
		}
	}
}
