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
package handlers.chat.commands.admin;

import java.util.StringTokenizer;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.sql.ClanHallTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.GMViewPledgeInfo;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Unified Clan & Pledge administration handler.
 * @author UnAfraid, Zoey76
 */
public class AdminClan implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_clan_info",
		"admin_clan_changeleader",
		"admin_clan_show_pending",
		"admin_clan_force_pending",
		"admin_pledge"
	};
	
	private static final int MAX_CLAN_LEVEL = 11;
	private static final int REP_POINTS_REWARD_LEVEL = 5;
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command);
		final String cmd = st.nextToken();
		switch (cmd)
		{
			case "admin_clan_info":
			{
				final Player player = getPlayer(activeChar, st);
				if (player == null)
				{
					break;
				}
				
				final Clan clan = player.getClan();
				if (clan == null)
				{
					activeChar.sendPacket(SystemMessageId.THE_TARGET_MUST_BE_A_CLAN_MEMBER);
					return false;
				}
				
				final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
				html.setHtml(HtmCache.getInstance().getHtm(activeChar, "data/html/admin/claninfo.htm"));
				html.replace("%clan_name%", clan.getName());
				html.replace("%clan_leader%", clan.getLeaderName());
				html.replace("%clan_level%", String.valueOf(clan.getLevel()));
				html.replace("%clan_has_castle%", clan.getCastleId() > 0 ? CastleManager.getInstance().getCastleById(clan.getCastleId()).getName() : "No");
				html.replace("%clan_has_clanhall%", clan.getHideoutId() > 0 ? ClanHallTable.getInstance().getClanHallById(clan.getHideoutId()).getName() : "No");
				html.replace("%clan_has_fortress%", clan.getFortId() > 0 ? FortManager.getInstance().getFortById(clan.getFortId()).getName() : "No");
				html.replace("%clan_points%", String.valueOf(clan.getReputationScore()));
				html.replace("%clan_players_count%", String.valueOf(clan.getMembersCount()));
				html.replace("%clan_ally%", clan.getAllyId() > 0 ? clan.getAllyName() : "Not in ally");
				html.replace("%current_player_objectId%", String.valueOf(player.getObjectId()));
				html.replace("%current_player_name%", player.getName());
				activeChar.sendPacket(html);
				break;
			}
			case "admin_clan_changeleader":
			{
				final Player player = getPlayer(activeChar, st);
				if (player == null)
				{
					break;
				}
				
				final Clan clan = player.getClan();
				if (clan == null)
				{
					activeChar.sendPacket(SystemMessageId.THE_TARGET_MUST_BE_A_CLAN_MEMBER);
					return false;
				}
				
				final ClanMember member = clan.getClanMember(player.getObjectId());
				if (member != null)
				{
					if (player.isAcademyMember())
					{
						player.sendPacket(SystemMessageId.THAT_PRIVILEGE_CANNOT_BE_GRANTED_TO_A_CLAN_ACADEMY_MEMBER);
					}
					else
					{
						clan.setNewLeader(member);
					}
				}
				break;
			}
			case "admin_clan_show_pending":
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
				html.setHtml(HtmCache.getInstance().getHtm(activeChar, "data/html/admin/clanchanges.htm"));
				final StringBuilder sb = new StringBuilder();
				for (Clan clan : ClanTable.getInstance().getClans())
				{
					if (clan.getNewLeaderId() != 0)
					{
						sb.append("<tr>");
						sb.append("<td>" + clan.getName() + "</td>");
						sb.append("<td>" + clan.getNewLeaderName() + "</td>");
						sb.append("<td><a action=\"bypass -h admin_clan_force_pending " + clan.getId() + "\">Force</a></td>");
						sb.append("</tr>");
					}
				}
				
				html.replace("%data%", sb.toString());
				activeChar.sendPacket(html);
				break;
			}
			case "admin_clan_force_pending":
			{
				if (st.hasMoreElements())
				{
					final String token = st.nextToken();
					if (!StringUtil.isNumeric(token))
					{
						break;
					}
					
					final int clanId = Integer.parseInt(token);
					final Clan clan = ClanTable.getInstance().getClan(clanId);
					if (clan == null)
					{
						break;
					}
					
					final ClanMember member = clan.getClanMember(clan.getNewLeaderId());
					if (member == null)
					{
						break;
					}
					
					clan.setNewLeader(member);
					activeChar.sendSysMessage("Task have been forcely executed.");
					break;
				}
				break;
			}
			case "admin_pledge":
			{
				return handlePledgeCommand(activeChar, st);
			}
		}
		
		return true;
	}
	
	private boolean handlePledgeCommand(Player activeChar, StringTokenizer st)
	{
		final WorldObject target = activeChar.getTarget();
		final Player player = (target instanceof Player) ? target.asPlayer() : activeChar;
		final String name = player.getName();
		final String action = st.hasMoreTokens() ? st.nextToken() : null;
		if (action == null)
		{
			activeChar.sendSysMessage("Not allowed Action on Clan");
			AdminHtml.showAdminHtml(activeChar, "pledge.htm");
			return false;
		}
		
		if (!action.equals("create") && !player.isClanLeader())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_IS_NOT_A_CLAN_LEADER).addString(name));
			AdminHtml.showAdminHtml(activeChar, "pledge.htm");
			return false;
		}
		
		final String parameter = st.hasMoreTokens() ? st.nextToken() : null;
		switch (action)
		{
			case "create":
			{
				if ((parameter == null) || parameter.isEmpty())
				{
					activeChar.sendSysMessage("Please, enter clan name.");
					AdminHtml.showAdminHtml(activeChar, "pledge.htm");
					return false;
				}
				
				final long cet = player.getClanCreateExpiryTime();
				player.setClanCreateExpiryTime(0);
				final Clan clan = ClanTable.getInstance().createClan(player, parameter);
				if (clan != null)
				{
					activeChar.sendSysMessage("Clan " + parameter + " created. Leader: " + player.getName());
					return true;
				}
				
				player.setClanCreateExpiryTime(cet);
				activeChar.sendSysMessage("There was a problem while creating the clan.");
				AdminHtml.showAdminHtml(activeChar, "pledge.htm");
				return false;
			}
			case "dismiss":
			{
				ClanTable.getInstance().destroyClan(player.getClanId());
				final Clan clan = player.getClan();
				if (clan == null)
				{
					activeChar.sendSysMessage("Clan disbanded.");
					return true;
				}
				
				activeChar.sendSysMessage("There was a problem while destroying the clan.");
				AdminHtml.showAdminHtml(activeChar, "pledge.htm");
				return false;
			}
			case "info":
			{
				final Clan clan = (parameter != null) ? ClanTable.getInstance().getClanByName(parameter) : player.getClan();
				activeChar.sendPacket(new GMViewPledgeInfo(clan, player));
				return true;
			}
			case "setlevel":
			{
				if (parameter == null)
				{
					activeChar.sendSysMessage("Usage: //pledge <setlevel|rep> <number>");
					AdminHtml.showAdminHtml(activeChar, "pledge.htm");
					return false;
				}
				
				final Clan clan = player.getClan();
				try
				{
					final int level = Integer.parseInt(parameter);
					if ((level >= 0) && (level <= MAX_CLAN_LEVEL))
					{
						clan.changeLevel(level);
						activeChar.sendSysMessage("You set level " + level + " for clan " + clan.getName());
						return true;
					}
				}
				catch (NumberFormatException ignored)
				{
				}
				
				activeChar.sendSysMessage("Level incorrect. Usage: //pledge <setlevel|rep> <number>");
				AdminHtml.showAdminHtml(activeChar, "pledge.htm");
				return false;
			}
			case "rep":
			{
				if (parameter == null)
				{
					activeChar.sendSysMessage("Usage: //pledge <setlevel|rep> <number>");
					AdminHtml.showAdminHtml(activeChar, "pledge.htm");
					return false;
				}
				
				final Clan clan = player.getClan();
				if (clan.getLevel() < REP_POINTS_REWARD_LEVEL)
				{
					activeChar.sendSysMessage("Only clans of level 5 or above may receive reputation points.");
					AdminHtml.showAdminHtml(activeChar, "pledge.htm");
					return false;
				}
				
				try
				{
					final int points = Integer.parseInt(parameter);
					clan.addReputationScore(points);
					activeChar.sendSysMessage("You " + (points > 0 ? "add " : "remove ") + Math.abs(points) + " points " + (points > 0 ? "to " : "from ") + clan.getName() + "'s reputation. Their current score is " + clan.getReputationScore());
					AdminHtml.showAdminHtml(activeChar, "pledge.htm");
					return true;
				}
				catch (Exception e)
				{
					activeChar.sendSysMessage("Usage: //pledge <rep> <number>");
					return false;
				}
			}
			default:
			{
				activeChar.sendSysMessage("Clan action not allowed.");
				AdminHtml.showAdminHtml(activeChar, "pledge.htm");
				return false;
			}
		}
	}
	
	private Player getPlayer(Player activeChar, StringTokenizer st)
	{
		String val;
		Player player = null;
		if (st.hasMoreTokens())
		{
			val = st.nextToken();
			if (StringUtil.isNumeric(val))
			{
				player = World.getPlayer(Integer.parseInt(val));
				if (player == null)
				{
					activeChar.sendPacket(SystemMessageId.THAT_PLAYER_IS_NOT_ONLINE);
					return null;
				}
			}
			else
			{
				player = World.getPlayer(val);
				if (player == null)
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_NAME_PLEASE_TRY_AGAIN);
					return null;
				}
			}
		}
		else
		{
			final WorldObject targetObj = activeChar.getTarget();
			if ((targetObj == null) || !targetObj.isPlayer())
			{
				activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
				return null;
			}
			
			player = targetObj.asPlayer();
		}
		
		return player;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
