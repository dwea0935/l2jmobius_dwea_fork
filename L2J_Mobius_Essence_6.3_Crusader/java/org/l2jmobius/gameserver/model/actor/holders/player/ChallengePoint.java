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
package org.l2jmobius.gameserver.model.actor.holders.player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.xml.EnchantChallengePointData;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * @author Serenitty
 */
public class ChallengePoint
{
	private static final Logger LOGGER = Logger.getLogger(ChallengePoint.class.getName());
	
	// Character enchant challenge points.
	private static final String INSERT_CHALLENGE_POINTS = "REPLACE INTO enchant_challenge_points (`charId`, `groupId`, `points`) VALUES (?, ?, ?)";
	private static final String RESTORE_CHALLENGE_POINTS = "SELECT * FROM enchant_challenge_points WHERE charId=?";
	private static final String INSERT_CHALLENGE_POINTS_RECHARGES = "REPLACE INTO enchant_challenge_points_recharges (`charId`, `groupId`, `optionIndex`, `count`) VALUES (?, ?, ?, ?)";
	private static final String RESTORE_CHALLENGE_POINTS_RECHARGES = "SELECT * FROM enchant_challenge_points_recharges WHERE charId=?";
	
	private final Player _owner;
	private int _nowGroup;
	private int _nowPoint;
	private final Map<Integer, Integer> _challengePoints = new HashMap<>();
	private final Map<Integer, Map<Integer, Integer>> _challengePointsRecharges = new HashMap<>();
	private final int[] _challengePointsPendingRecharge =
	{
		-1,
		-1,
	};
	
	public ChallengePoint(Player owner)
	{
		_owner = owner;
		_nowGroup = 0;
		_nowPoint = 0;
	}
	
	public void storeChallengePoints()
	{
		// LOGGER.info("Storing Challenge Points for " + _owner);
		
		if (_challengePoints.isEmpty())
		{
			return;
		}
		
		try (Connection conn = DatabaseFactory.getConnection())
		{
			try (PreparedStatement ps1 = conn.prepareStatement(INSERT_CHALLENGE_POINTS))
			{
				for (Entry<Integer, Integer> entry : _challengePoints.entrySet())
				{
					ps1.setInt(1, _owner.getObjectId());
					ps1.setInt(2, entry.getKey());
					ps1.setInt(3, entry.getValue());
					ps1.addBatch();
				}
				ps1.executeBatch();
			}
			try (PreparedStatement ps2 = conn.prepareStatement(INSERT_CHALLENGE_POINTS_RECHARGES))
			{
				for (Entry<Integer, Map<Integer, Integer>> entry : _challengePointsRecharges.entrySet())
				{
					for (Entry<Integer, Integer> entry2 : entry.getValue().entrySet())
					{
						ps2.setInt(1, _owner.getObjectId());
						ps2.setInt(2, entry.getKey());
						ps2.setInt(3, entry2.getKey());
						ps2.setInt(4, entry2.getValue());
						ps2.addBatch();
					}
				}
				ps2.executeBatch();
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("Could not store Challenge Points for " + _owner + " " + e);
		}
	}
	
	public void restoreChallengePoints()
	{
		_challengePoints.clear();
		try (Connection con = DatabaseFactory.getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement(RESTORE_CHALLENGE_POINTS))
			{
				ps.setInt(1, _owner.getObjectId());
				try (ResultSet rs = ps.executeQuery())
				{
					while (rs.next())
					{
						final int groupId = rs.getInt("groupId");
						final int points = rs.getInt("points");
						_challengePoints.put(groupId, points);
					}
				}
			}
			
			_challengePointsRecharges.clear();
			try (PreparedStatement ps = con.prepareStatement(RESTORE_CHALLENGE_POINTS_RECHARGES))
			{
				ps.setInt(1, _owner.getObjectId());
				try (ResultSet rs = ps.executeQuery())
				{
					while (rs.next())
					{
						final int groupId = rs.getInt("groupId");
						final int optionIndex = rs.getInt("optionIndex");
						final int count = rs.getInt("count");
						Map<Integer, Integer> options = _challengePointsRecharges.get(groupId);
						if (options == null)
						{
							options = new HashMap<>();
							_challengePointsRecharges.put(groupId, options);
						}
						options.put(optionIndex, count);
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("Could not restore Challenge Points for " + _owner + " " + e);
		}
		
		// LOGGER.info("Restored Challenge Points recharges for " + _owner);
		// LOGGER.info("Restored Challenge Points for " + _owner);
	}
	
	public int getNowPoint()
	{
		final int nowPoint = _nowPoint;
		_nowPoint = 0;
		return nowPoint;
	}
	
	public int getNowGroup()
	{
		final int nowGroup = _nowGroup;
		_nowGroup = 0;
		return nowGroup;
	}
	
	public void setNowGroup(int val)
	{
		_nowGroup = val;
	}
	
	public void setNowPoint(int val)
	{
		_nowPoint = val;
	}
	
	public Map<Integer, Integer> getChallengePoints()
	{
		return _challengePoints;
	}
	
	public int getChallengePointsRecharges(int groupId, int optionIndex)
	{
		final Map<Integer, Integer> options = _challengePointsRecharges.get(groupId);
		if (options != null)
		{
			return options.getOrDefault(optionIndex, 0);
		}
		return 0;
	}
	
	public void addChallengePointsRecharge(int groupId, int optionIndex, int amount)
	{
		Map<Integer, Integer> options = _challengePointsRecharges.get(groupId);
		if (options == null)
		{
			options = new HashMap<>();
			_challengePointsRecharges.put(groupId, options);
		}
		options.compute(optionIndex, (_, v) -> v == null ? amount : v + amount);
	}
	
	public void setChallengePointsPendingRecharge(int groupId, int optionIndex)
	{
		_challengePointsPendingRecharge[0] = groupId;
		_challengePointsPendingRecharge[1] = optionIndex;
	}
	
	public int[] getChallengePointsPendingRecharge()
	{
		return _challengePointsPendingRecharge;
	}
	
	public ChallengePointInfoHolder[] initializeChallengePoints()
	{
		final Map<Integer, Integer> challengePoints = getChallengePoints();
		final ChallengePointInfoHolder[] info = new ChallengePointInfoHolder[challengePoints.size()];
		int i = 0;
		for (Entry<Integer, Integer> entry : challengePoints.entrySet())
		{
			final int groupId = entry.getKey();
			info[i] = new ChallengePointInfoHolder(groupId, entry.getValue(), //
				getChallengePointsRecharges(groupId, 0), //
				getChallengePointsRecharges(groupId, 1), //
				getChallengePointsRecharges(groupId, 2), //
				getChallengePointsRecharges(groupId, 3), //
				getChallengePointsRecharges(groupId, 4), //
				getChallengePointsRecharges(groupId, 5));
			i++;
		}
		return info;
	}
	
	public boolean canAddPoints(int categoryId, int points)
	{
		final int totalPoints = _challengePoints.getOrDefault(categoryId, 0) + points;
		final int maxPoints = EnchantChallengePointData.getInstance().getMaxPoints();
		return maxPoints > totalPoints;
	}
}
