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
package org.l2jmobius.gameserver.model.costumes;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * @author JoeAlisson, GolbergSoft
 */
public class CostumeCollectionData
{
	private static final Logger LOGGER = Logger.getLogger(CostumeCollectionData.class.getName());
	
	private static final String UPDATE_REUSE_TIME = "UPDATE character_costume_collection SET reuse=? WHERE player_id=? AND id=?";
	private static final String INSERT_COSTUME = "INSERT INTO character_costume_collection (player_id, id, reuse) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE reuse=VALUES(reuse)";
	
	private int _playerId;
	private int _id;
	private int _reuse;
	
	public static final CostumeCollectionData DEFAULT = new CostumeCollectionData();
	
	public static CostumeCollectionData of(Player player, int id, int reuseTime)
	{
		final CostumeCollectionData collection = new CostumeCollectionData();
		collection._playerId = player.getObjectId();
		collection._id = id;
		collection._reuse = reuseTime;
		return collection;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public void updateReuseTime()
	{
		_reuse = (int) Instant.now().plus(Duration.ofSeconds(1)).getEpochSecond();
		updateReuseTimeInDB();
	}
	
	private void updateReuseTimeInDB()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE_REUSE_TIME))
		{
			ps.setInt(1, _reuse);
			ps.setInt(2, _playerId);
			ps.setInt(3, _id);
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.warning("Error updating reuse time for player ID: " + _playerId + ", costume ID: " + _id + ", " + e.getMessage());
		}
	}
	
	public int getReuseTime()
	{
		return (int) Math.max(0, _reuse - Instant.now().getEpochSecond());
	}
	
	public static void createCostumeInDB(Player player, int id, int reuseTime)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(INSERT_COSTUME))
		{
			ps.setInt(1, player.getObjectId());
			ps.setInt(2, id);
			ps.setInt(3, reuseTime);
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.warning("Error creating costume in DB for player ID: " + player.getObjectId() + ", costume ID: " + id + ", " + e.getMessage());
		}
	}
}
