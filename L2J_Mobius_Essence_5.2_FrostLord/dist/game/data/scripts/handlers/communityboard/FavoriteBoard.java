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
package handlers.communityboard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * Favorite board.
 * @author Zoey76
 */
public class FavoriteBoard implements IParseBoardHandler
{
	// SQL Queries
	private static final String SELECT_FAVORITES = "SELECT * FROM `bbs_favorites` WHERE `playerId`=? ORDER BY `favAddDate` DESC";
	private static final String DELETE_FAVORITE = "DELETE FROM `bbs_favorites` WHERE `playerId`=? AND `favId`=?";
	private static final String ADD_FAVORITE = "REPLACE INTO `bbs_favorites`(`playerId`, `favTitle`, `favBypass`) VALUES(?, ?, ?)";
	
	private static final String[] COMMANDS =
	{
		"_bbsgetfav",
		"bbs_add_fav",
		"_bbsdelfav_"
	};
	
	@Override
	public String[] getCommunityBoardCommands()
	{
		return COMMANDS;
	}
	
	@Override
	public boolean parseCommunityBoardCommand(String command, Player player)
	{
		// None of this commands can be added to favorites.
		if (command.startsWith("_bbsgetfav"))
		{
			// Load Favorite links
			final String list = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/favorite_list.html");
			final StringBuilder sb = new StringBuilder();
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(SELECT_FAVORITES))
			{
				ps.setInt(1, player.getObjectId());
				try (ResultSet rs = ps.executeQuery())
				{
					while (rs.next())
					{
						String link = list.replace("%fav_bypass%", rs.getString("favBypass"));
						link = link.replace("%fav_title%", rs.getString("favTitle"));
						final SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						link = link.replace("%fav_add_date%", date.format(rs.getTimestamp("favAddDate")));
						link = link.replace("%fav_id%", String.valueOf(rs.getInt("favId")));
						sb.append(link);
					}
				}
				String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/favorite.html");
				html = html.replace("%fav_list%", sb.toString());
				CommunityBoardHandler.separateAndSend(html, player);
			}
			catch (Exception e)
			{
				LOG.warning(FavoriteBoard.class.getSimpleName() + ": Couldn't load favorite links for " + player);
			}
		}
		else if (command.startsWith("bbs_add_fav"))
		{
			final String bypass = CommunityBoardHandler.getInstance().removeBypass(player);
			if (bypass != null)
			{
				final String[] parts = bypass.split("&", 2);
				if (parts.length != 2)
				{
					LOG.warning(FavoriteBoard.class.getSimpleName() + ": Couldn't add favorite link, " + bypass + " it's not a valid bypass!");
					return false;
				}
				
				try (Connection con = DatabaseFactory.getConnection();
					PreparedStatement ps = con.prepareStatement(ADD_FAVORITE))
				{
					ps.setInt(1, player.getObjectId());
					ps.setString(2, parts[0].trim());
					ps.setString(3, parts[1].trim());
					ps.execute();
					// Callback
					parseCommunityBoardCommand("_bbsgetfav", player);
				}
				catch (Exception e)
				{
					LOG.warning(FavoriteBoard.class.getSimpleName() + ": Couldn't add favorite link " + bypass + " for " + player);
				}
			}
		}
		else if (command.startsWith("_bbsdelfav_"))
		{
			final String favId = command.replace("_bbsdelfav_", "");
			if (!StringUtil.isNumeric(favId))
			{
				LOG.warning(FavoriteBoard.class.getSimpleName() + ": Couldn't delete favorite link, " + favId + " it's not a valid ID!");
				return false;
			}
			
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(DELETE_FAVORITE))
			{
				ps.setInt(1, player.getObjectId());
				ps.setInt(2, Integer.parseInt(favId));
				ps.execute();
				// Callback
				parseCommunityBoardCommand("_bbsgetfav", player);
			}
			catch (Exception e)
			{
				LOG.warning(FavoriteBoard.class.getSimpleName() + ": Couldn't delete favorite link ID " + favId + " for " + player);
			}
		}
		return true;
	}
}
