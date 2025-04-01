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
package org.l2jmobius.gameserver.network.serverpackets.costume;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.sql.CostumeTable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author JoeAlisson
 */
public class ExSendCostumeListFull extends ServerPacket
{
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_SEND_COSTUME_LIST_FULL.writeId(this, buffer);
		final Player player = client.getPlayer();
		
		// Load costume list from database.
		final List<CostumeTable> costumes = loadCostumes(player.getObjectId());
		buffer.writeInt(costumes.size());
		for (CostumeTable costume : costumes)
		{
			buffer.writeInt(costume.getId());
			buffer.writeLong(costume.getAmount());
			buffer.writeInt(costume.isLocked() ? 1 : 0);
			buffer.writeInt(costume.checkIsNewAndChange() ? 1 : 0);
		}
		
		buffer.writeInt(0); // Shortcut disabled.
	}
	
	private List<CostumeTable> loadCostumes(int playerId)
	{
		final List<CostumeTable> costumeList = new ArrayList<>();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT id, amount, locked FROM character_costumes WHERE player_id = ?"))
		{
			ps.setInt(1, playerId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final CostumeTable costume = new CostumeTable();
					costume.setId(rs.getInt("id"));
					costume.setAmount(rs.getLong("amount"));
					costume.setLocked(rs.getBoolean("locked"));
					costumeList.add(costume);
				}
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		return costumeList;
	}
}
