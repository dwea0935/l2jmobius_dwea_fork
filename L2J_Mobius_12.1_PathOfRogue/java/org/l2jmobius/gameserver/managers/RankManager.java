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
package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.olympiad.Hero;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;

/**
 * @author NviX
 */
public class RankManager
{
	private static final Logger LOGGER = Logger.getLogger(RankManager.class.getName());
	
	public static final Long TIME_LIMIT = 2592000000L; // 30 days in milliseconds
	public static final long CURRENT_TIME = System.currentTimeMillis();
	public static final int PLAYER_LIMIT = 500;
	public static final int CONQUEST_PLAYER_LIMIT = 100;
	
	private static final String SELECT_CHARACTERS = "SELECT charId,char_name,level,race,base_class, clanid FROM characters WHERE (" + CURRENT_TIME + " - cast(lastAccess as signed) < " + TIME_LIMIT + ") AND accesslevel = 0 AND level > 84 ORDER BY exp DESC, onlinetime DESC LIMIT " + PLAYER_LIMIT;
	private static final String SELECT_CHARACTERS_PVP = "SELECT charId,char_name,level,race,base_class, clanid, deaths, kills, pvpkills FROM characters WHERE (" + CURRENT_TIME + " - cast(lastAccess as signed) < " + TIME_LIMIT + ") AND accesslevel = 0 AND level > 84 ORDER BY kills DESC, onlinetime DESC LIMIT " + PLAYER_LIMIT;
	private static final String SELECT_CHARACTERS_BY_RACE = "SELECT charId FROM characters WHERE (" + CURRENT_TIME + " - cast(lastAccess as signed) < " + TIME_LIMIT + ") AND accesslevel = 0 AND level > 84 AND race = ? ORDER BY exp DESC, onlinetime DESC LIMIT " + PLAYER_LIMIT;
	
	private static final String GET_CURRENT_CYCLE_DATA = "SELECT characters.char_name, characters.level, characters.base_class, characters.clanid, olympiad_nobles.charId, olympiad_nobles.olympiad_points, olympiad_nobles.competitions_won, olympiad_nobles.competitions_lost FROM characters, olympiad_nobles WHERE characters.charId = olympiad_nobles.charId ORDER BY olympiad_nobles.olympiad_points DESC LIMIT " + PLAYER_LIMIT;
	private static final String GET_HEROES = "SELECT characters.charId, characters.char_name, characters.race, characters.sex, characters.base_class, characters.level, characters.clanid, olympiad_nobles_eom.competitions_won, olympiad_nobles_eom.competitions_lost, olympiad_nobles_eom.olympiad_points, heroes.legend_count, heroes.count FROM heroes, characters, olympiad_nobles_eom WHERE characters.charId = heroes.charId AND characters.charId = olympiad_nobles_eom.charId AND heroes.played = 1 AND characters.accesslevel = 0 ORDER BY olympiad_nobles_eom.olympiad_points DESC, characters.base_class ASC LIMIT " + RankManager.PLAYER_LIMIT;
	private static final String GET_CHARACTERS_BY_CLASS = "SELECT charId FROM characters WHERE (" + CURRENT_TIME + " - cast(lastAccess as signed) < " + TIME_LIMIT + ") AND accesslevel = 0 AND level > 84 AND characters.base_class = ? ORDER BY exp DESC, onlinetime DESC LIMIT " + PLAYER_LIMIT;
	
	// Conquest Data
	private static final String GET_CURRENT_CONQUEST_CYCLE_DATA = "SELECT characters.charId, characters.char_name, character_variables.charId, character_variables.var, character_variables.val FROM characters, character_variables WHERE characters.charId = character_variables.charId AND val > 0 AND var IN ('" + PlayerVariables.CONQUEST_PERSONAL_POINTS + "') ORDER BY CONVERT(val, UNSIGNED INTEGER) DESC LIMIT " + CONQUEST_PLAYER_LIMIT;
	private static final String GET_PREVIOUS_CONQUEST_CYCLE_DATA = "SELECT * FROM conquest_prev_season_ranklist ORDER BY CONVERT(personal_points, UNSIGNED INTEGER) DESC LIMIT " + CONQUEST_PLAYER_LIMIT;
	
	private final Map<Integer, StatSet> _mainList = new ConcurrentHashMap<>();
	private Map<Integer, StatSet> _snapshotList = new ConcurrentHashMap<>();
	private final Map<Integer, StatSet> _mainOlyList = new ConcurrentHashMap<>();
	private Map<Integer, StatSet> _snapshotOlyList = new ConcurrentHashMap<>();
	private final List<HeroInfo> _mainHeroList = new LinkedList<>();
	private List<HeroInfo> _snapshotHeroList = new LinkedList<>();
	private final Map<Integer, StatSet> _mainPvpList = new ConcurrentHashMap<>();
	private Map<Integer, StatSet> _snapshotPvpList = new ConcurrentHashMap<>();
	
	// Conquest Lists
	private final Map<Integer, StatSet> _currentConquestList = new ConcurrentHashMap<>();
	private Map<Integer, StatSet> _snapshotCurrentConquestList = new ConcurrentHashMap<>();
	private final Map<Integer, StatSet> _previousConquestList = new ConcurrentHashMap<>();
	private Map<Integer, StatSet> _snapshotPreviousConquestList = new ConcurrentHashMap<>();
	private String _conquestTopRankName = "";
	
	public class HeroInfo
	{
		public String charName;
		public String clanName;
		public int serverId;
		public int race;
		public boolean isMale;
		public int baseClass;
		public int level;
		public int legendCount;
		public int competitionsWon;
		public int competitionsLost;
		public int olympiadPoints;
		public int clanLevel;
		public boolean isTopHero;
		
		HeroInfo(String charName, String clanName, int serverId, int race, boolean isMale, int baseClass, int level, int legendCount, int competitionsWon, int competitionsLost, int olympiadPoints, int clanLevel, boolean isTopHero)
		{
			this.charName = charName;
			this.clanName = clanName;
			this.serverId = serverId;
			this.race = race;
			this.isMale = isMale;
			this.baseClass = baseClass;
			this.level = level;
			this.legendCount = legendCount;
			this.competitionsWon = competitionsWon;
			this.competitionsLost = competitionsLost;
			this.olympiadPoints = olympiadPoints;
			this.clanLevel = clanLevel;
			this.isTopHero = isTopHero;
		}
	}
	
	protected RankManager()
	{
		ThreadPool.scheduleAtFixedRate(this::update, 0, 1800000);
	}
	
	private synchronized void update()
	{
		// Load charIds All
		_snapshotList = _mainList;
		_mainList.clear();
		_snapshotOlyList = _mainOlyList;
		_mainOlyList.clear();
		_snapshotHeroList = _mainHeroList;
		_mainHeroList.clear();
		_snapshotPvpList = _mainPvpList;
		_mainPvpList.clear();
		_snapshotCurrentConquestList = _currentConquestList;
		_currentConquestList.clear();
		_snapshotPreviousConquestList = _previousConquestList;
		_previousConquestList.clear();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SELECT_CHARACTERS))
		{
			try (ResultSet result = statement.executeQuery())
			{
				int i = 1;
				while (result.next())
				{
					final StatSet stats = new StatSet();
					final int charId = result.getInt("charId");
					final int classId = result.getInt("base_class");
					stats.set("charId", charId);
					stats.set("name", result.getString("char_name"));
					stats.set("level", result.getInt("level"));
					stats.set("classId", result.getInt("base_class"));
					final int race = result.getInt("race");
					stats.set("race", race);
					loadRaceRank(charId, race, stats);
					loadClassRank(charId, classId, stats);
					
					final Clan clan = ClanTable.getInstance().getClan(result.getInt("clanid"));
					if (clan != null)
					{
						stats.set("clanName", clan.getName());
					}
					else
					{
						stats.set("clanName", "");
					}
					
					_mainList.put(i, stats);
					i++;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not load chars total rank data: " + this + " - " + e.getMessage(), e);
		}
		
		// load olympiad data.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(GET_CURRENT_CYCLE_DATA))
		{
			try (ResultSet result = statement.executeQuery())
			{
				int i = 1;
				while (result.next())
				{
					final StatSet stats = new StatSet();
					final int charId = result.getInt("charId");
					stats.set("charId", charId);
					stats.set("name", result.getString("char_name"));
					
					final Clan clan = ClanTable.getInstance().getClan(result.getInt("clanid"));
					if (clan != null)
					{
						stats.set("clanName", clan.getName());
						stats.set("clanLevel", clan.getLevel());
					}
					else
					{
						stats.set("clanName", "");
						stats.set("clanLevel", 0);
					}
					
					stats.set("level", result.getInt("level"));
					final int classId = result.getInt("base_class");
					stats.set("classId", classId);
					stats.set("competitions_won", result.getInt("competitions_won"));
					stats.set("competitions_lost", result.getInt("competitions_lost"));
					stats.set("olympiad_points", result.getInt("olympiad_points"));
					
					if (Hero.getInstance().getCompleteHeroes().containsKey(charId))
					{
						final StatSet heroStats = Hero.getInstance().getCompleteHeroes().get(charId);
						stats.set("count", heroStats.getInt("count", 0));
						stats.set("legend_count", heroStats.getInt("legend_count", 0));
					}
					else
					{
						stats.set("count", 0);
						stats.set("legend_count", 0);
					}
					
					loadClassRank(charId, classId, stats);
					
					_mainOlyList.put(i, stats);
					i++;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not load olympiad total rank data: " + this + " - " + e.getMessage(), e);
		}
		
		if (!Hero.getInstance().getHeroes().isEmpty())
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement statement = con.prepareStatement(GET_HEROES);
				ResultSet result = statement.executeQuery())
			{
				boolean isFirstHero = true;
				while (result.next())
				{
					final String charName = result.getString("char_name");
					final int clanId = result.getInt("clanid");
					final String clanName = (clanId > 0) ? ClanTable.getInstance().getClan(clanId).getName() : "";
					final int race = result.getInt("race");
					final int baseClass = result.getInt("base_class");
					final boolean isMale = (baseClass == 235 /* Fix for Shinemaker icon. */) || (result.getInt("sex") != 1);
					final int level = result.getInt("level");
					final int legendCount = result.getInt("legend_count");
					final int competitionsWon = result.getInt("competitions_won");
					final int competitionsLost = result.getInt("competitions_lost");
					final int olympiadPoints = result.getInt("olympiad_points");
					final int clanLevel = (clanId > 0) ? ClanTable.getInstance().getClan(clanId).getLevel() : 0;
					final boolean isTopHero = isFirstHero;
					_mainHeroList.add(new HeroInfo(charName, clanName, Config.SERVER_ID, race, isMale, baseClass, level, legendCount, competitionsWon, competitionsLost, olympiadPoints, clanLevel, isTopHero));
					isFirstHero = false;
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Could not load Hero and Legend Info rank data: " + this + " - " + e.getMessage(), e);
			}
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SELECT_CHARACTERS_PVP))
		{
			try (ResultSet result = statement.executeQuery())
			{
				int i = 1;
				while (result.next())
				{
					final StatSet stats = new StatSet();
					final int charId = result.getInt("charId");
					stats.set("charId", charId);
					stats.set("name", result.getString("char_name"));
					stats.set("level", result.getInt("level"));
					stats.set("classId", result.getInt("base_class"));
					final int race = result.getInt("race");
					stats.set("race", race);
					stats.set("kills", result.getInt("kills"));
					stats.set("deaths", result.getInt("deaths"));
					stats.set("points", result.getInt("pvpkills"));
					loadRaceRank(charId, race, stats);
					
					final Clan clan = ClanTable.getInstance().getClan(result.getInt("clanid"));
					if (clan != null)
					{
						stats.set("clanName", clan.getName());
					}
					else
					{
						stats.set("clanName", "");
					}
					
					_mainPvpList.put(i, stats);
					i++;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not load pvp total rank data: " + this + " - " + e.getMessage(), e);
		}
		
		// load conquest data.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(GET_CURRENT_CONQUEST_CYCLE_DATA))
		{
			try (ResultSet result = statement.executeQuery())
			{
				int i = 1;
				while (result.next())
				{
					final StatSet stats = new StatSet();
					final int charId = result.getInt("charId");
					stats.set("charId", charId);
					stats.set("name", result.getString("char_name"));
					stats.set("conquestPersonalPoints", result.getLong("val"));
					if (i == 1)
					{
						loadConquestTopRank(charId);
					}
					
					_currentConquestList.put(i, stats);
					i++;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not load conquest rank data: " + this + " - " + e.getMessage(), e);
		}
		
		// load previous season conquest data.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(GET_PREVIOUS_CONQUEST_CYCLE_DATA))
		{
			try (ResultSet result = statement.executeQuery())
			{
				int i = 1;
				while (result.next())
				{
					final StatSet stats = new StatSet();
					final int charId = result.getInt("charId");
					stats.set("charId", charId);
					stats.set("conquest_name", result.getString("char_name"));
					stats.set("conquestPersonalPoints", result.getLong("personal_points"));
					
					_previousConquestList.put(i, stats);
					i++;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not load conquest previous season ranklist data: " + this + " - " + e.getMessage(), e);
		}
	}
	
	private void loadClassRank(int charId, int classId, StatSet stats)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(GET_CHARACTERS_BY_CLASS))
		{
			statement.setInt(1, classId);
			try (ResultSet result = statement.executeQuery())
			{
				int i = 0;
				while (result.next())
				{
					if (result.getInt("charId") == charId)
					{
						stats.set("classRank", i + 1);
					}
					i++;
				}
				if (i == 0)
				{
					stats.set("classRank", 0);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not load chars classId olympiad rank data: " + this + " - " + e.getMessage(), e);
		}
	}
	
	private void loadRaceRank(int charId, int race, StatSet stats)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SELECT_CHARACTERS_BY_RACE))
		{
			statement.setInt(1, race);
			try (ResultSet result = statement.executeQuery())
			{
				int i = 0;
				while (result.next())
				{
					if (result.getInt("charId") == charId)
					{
						stats.set("raceRank", i + 1);
					}
					i++;
				}
				if (i == 0)
				{
					stats.set("raceRank", 0);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not load chars race rank data: " + this + " - " + e.getMessage(), e);
		}
	}
	
	private void loadConquestTopRank(int charId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM character_variables WHERE var='" + PlayerVariables.CONQUEST_NAME + "' AND charId=?"))
		{
			statement.setString(1, String.valueOf(charId));
			try (ResultSet result = statement.executeQuery())
			{
				while (result.next())
				{
					_conquestTopRankName = result.getString("val");
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not load conquest name data: " + this + " - " + e.getMessage(), e);
		}
	}
	
	public Map<Integer, StatSet> getRankList()
	{
		return _mainList;
	}
	
	public Map<Integer, StatSet> getSnapshotList()
	{
		return _snapshotList;
	}
	
	public Map<Integer, StatSet> getOlyRankList()
	{
		return _mainOlyList;
	}
	
	public Map<Integer, StatSet> getSnapshotOlyList()
	{
		return _snapshotOlyList;
	}
	
	public Collection<HeroInfo> getSnapshotHeroList()
	{
		return _snapshotHeroList;
	}
	
	public Map<Integer, StatSet> getPvpRankList()
	{
		return _mainPvpList;
	}
	
	public Map<Integer, StatSet> getSnapshotPvpRankList()
	{
		return _snapshotPvpList;
	}
	
	public Map<Integer, StatSet> getCurrentConquestRankList()
	{
		return _currentConquestList;
	}
	
	public Map<Integer, StatSet> getSnapshotCurrentConquestRankList()
	{
		return _snapshotCurrentConquestList;
	}
	
	public Map<Integer, StatSet> getPreviousConquestRankList()
	{
		return _previousConquestList;
	}
	
	public Map<Integer, StatSet> getSnapshotPreviousConquestRankList()
	{
		return _snapshotPreviousConquestList;
	}
	
	public int getPlayerConquestGlobalRank(Player player)
	{
		final int objectId = player.getObjectId();
		for (Entry<Integer, StatSet> entry : _currentConquestList.entrySet())
		{
			final StatSet stats = entry.getValue();
			if (stats.getInt("charId", 0) == objectId)
			{
				return entry.getKey();
			}
		}
		
		return 0;
	}
	
	public int getPlayerGlobalRank(Player player)
	{
		if (!player.isInCategory(CategoryType.SIXTH_CLASS_GROUP))
		{
			return 0;
		}
		
		final int objectId = player.getObjectId();
		for (Entry<Integer, StatSet> entry : _mainList.entrySet())
		{
			final StatSet stats = entry.getValue();
			if (stats.getInt("charId", 0) == objectId)
			{
				return entry.getKey();
			}
		}
		
		return 0;
	}
	
	public int getPlayerRaceRank(Player player)
	{
		if (!player.isInCategory(CategoryType.SIXTH_CLASS_GROUP))
		{
			return 0;
		}
		
		final int objectId = player.getObjectId();
		for (StatSet stats : _mainList.values())
		{
			if (stats.getInt("charId", 0) == objectId)
			{
				return stats.getInt("raceRank", 0);
			}
		}
		
		return 0;
	}
	
	public int getPlayerClassRank(Player player)
	{
		if (!player.isInCategory(CategoryType.SIXTH_CLASS_GROUP))
		{
			return 0;
		}
		
		final int objectId = player.getObjectId();
		for (StatSet stats : _mainList.values())
		{
			if (stats.getInt("charId", 0) == objectId)
			{
				return stats.getInt("classRank", 0);
			}
		}
		
		return 0;
	}
	
	public String getConquestTopRankName()
	{
		return _conquestTopRankName;
	}
	
	public static RankManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RankManager INSTANCE = new RankManager();
	}
}