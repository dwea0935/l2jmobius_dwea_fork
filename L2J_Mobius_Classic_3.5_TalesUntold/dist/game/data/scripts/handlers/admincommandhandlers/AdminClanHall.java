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
package handlers.admincommandhandlers;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.l2jmobius.gameserver.data.xml.ClanHallData;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.html.PageBuilder;
import org.l2jmobius.gameserver.model.html.PageResult;
import org.l2jmobius.gameserver.model.html.formatters.BypassParserFormatter;
import org.l2jmobius.gameserver.model.html.pagehandlers.NextPrevPageHandler;
import org.l2jmobius.gameserver.model.html.styles.ButtonsStyle;
import org.l2jmobius.gameserver.model.residences.ClanHall;
import org.l2jmobius.gameserver.model.residences.ResidenceFunction;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Clan Hall admin commands.
 * @author St3eT, Mobius
 */
public class AdminClanHall implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_clanhall",
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();
		if (actualCommand.equalsIgnoreCase("admin_clanhall"))
		{
			processBypass(activeChar, command);
		}
		return true;
	}
	
	private void doAction(Player player, int clanHallId, String action, String actionVal)
	{
		final ClanHall clanHall = ClanHallData.getInstance().getClanHallById(clanHallId);
		if (clanHall != null)
		{
			switch (action)
			{
				case "openCloseDoors":
				{
					if (actionVal != null)
					{
						clanHall.openCloseDoors(Boolean.parseBoolean(actionVal));
					}
					break;
				}
				case "teleport":
				{
					if (actionVal != null)
					{
						final Location loc;
						switch (actionVal)
						{
							case "inside":
							{
								loc = clanHall.getOwnerLocation();
								break;
							}
							case "outside":
							{
								loc = clanHall.getBanishLocation();
								break;
							}
							default:
							{
								loc = player.getLocation();
							}
						}
						player.teleToLocation(loc);
					}
					break;
				}
				case "give":
				{
					if ((player.getTarget() != null) && player.getTarget().isPlayer())
					{
						final Clan targetClan = player.getTarget().asPlayer().getClan();
						if ((targetClan == null) || (targetClan.getHideoutId() != 0))
						{
							player.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
						}
						
						clanHall.setOwner(targetClan);
					}
					else
					{
						player.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
					}
					break;
				}
				case "take":
				{
					final Clan clan = clanHall.getOwner();
					if (clan != null)
					{
						clanHall.setOwner(null);
					}
					else
					{
						player.sendMessage("You cannot take Clan Hall which don't have any owner.");
					}
					break;
				}
				case "cancelFunc":
				{
					final ResidenceFunction function = clanHall.getFunction(Integer.parseInt(actionVal));
					if (function != null)
					{
						clanHall.removeFunction(function);
						sendClanHallDetails(player, clanHallId);
					}
					break;
				}
			}
		}
		else
		{
			player.sendMessage("Clan Hall with id " + clanHallId + " does not exist!");
		}
		useAdminCommand("admin_clanhall id=" + clanHallId, player);
	}
	
	private void sendClanHallList(Player player, int page)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/html/admin/clanhall_list.htm");
		final List<ClanHall> clanHallList = ClanHallData.getInstance().getClanHalls().stream().sorted(Comparator.comparingLong(ClanHall::getResidenceId)).collect(Collectors.toList());
		
		//@formatter:off
		final PageResult result = PageBuilder.newBuilder(clanHallList, 4, "bypass -h admin_clanhall")
			.currentPage(page)
			.pageHandler(NextPrevPageHandler.INSTANCE)
			.formatter(BypassParserFormatter.INSTANCE)
			.style(ButtonsStyle.INSTANCE)
			.bodyHandler((pages, clanHall, sb) ->
		{
			sb.append("<table border=0 cellpadding=0 cellspacing=0 bgcolor=\"363636\">");
			sb.append("<tr><td align=center fixwidth=\"250\"><font color=\"LEVEL\">&%" + clanHall.getResidenceId() + "; (" + clanHall.getResidenceId() + ")</font></td></tr>");
			sb.append("</table>");

			sb.append("<table border=0 cellpadding=0 cellspacing=0 bgcolor=\"363636\">");
			sb.append("<tr>");		
			sb.append("<td align=center fixwidth=\"83\">Status:</td>");		
			sb.append("<td align=center fixwidth=\"83\"></td>");		
			sb.append("<td align=center fixwidth=\"83\">" + (clanHall.getOwner() == null ? "<font color=\"00FF00\">Free</font>" : "<font color=\"FF9900\">Owned</font>") + "</td>");		
			sb.append("</tr>");
			
			sb.append("<tr>");
			sb.append("<td align=center fixwidth=\"83\">Location:</td>");
			sb.append("<td align=center fixwidth=\"83\"></td>");
			sb.append("<td align=center fixwidth=\"83\">&^" + clanHall.getResidenceId() + ";</td>");
			sb.append("</tr>");
			
			sb.append("<tr>");
			sb.append("<td align=center fixwidth=\"83\">Detailed Info:</td>");
			sb.append("<td align=center fixwidth=\"83\"></td>");
			sb.append("<td align=center fixwidth=\"83\"><button value=\"Show me!\" action=\"bypass -h admin_clanhall id=" + clanHall.getResidenceId() + "\" width=\"85\" height=\"20\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			sb.append("</tr>");
			
			
			sb.append("</table>");
			sb.append("<br>");
		}).build();
		//@formatter:on
		
		html.replace("%pages%", result.getPages() > 0 ? "<center><table width=\"100%\" cellspacing=0><tr>" + result.getPagerTemplate() + "</tr></table></center>" : "");
		html.replace("%data%", result.getBodyTemplate().toString());
		player.sendPacket(html);
	}
	
	private void sendClanHallDetails(Player player, int clanHallId)
	{
		final ClanHall clanHall = ClanHallData.getInstance().getClanHallById(clanHallId);
		if (clanHall != null)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
			final StringBuilder sb = new StringBuilder();
			html.setFile(player, "data/html/admin/clanhall_detail.htm");
			html.replace("%clanHallId%", clanHall.getResidenceId());
			html.replace("%clanHallOwner%", (clanHall.getOwner() == null ? "<font color=\"00FF00\">Free</font>" : "<font color=\"FF9900\">" + clanHall.getOwner().getName() + "</font>"));
			final String grade = clanHall.getGrade().toString().replace("GRADE_", "") + " Grade";
			html.replace("%clanHallGrade%", grade);
			html.replace("%clanHallSize%", clanHall.getGrade().getGradeValue());
			if (!clanHall.getFunctions().isEmpty())
			{
				sb.append("<table border=0 cellpadding=0 cellspacing=0 bgcolor=\"363636\">");
				sb.append("<tr>");
				sb.append("<td align=center fixwidth=\"40\"><font color=\"LEVEL\">ID</font></td>");
				sb.append("<td align=center fixwidth=\"200\"><font color=\"LEVEL\">Type</font></td>");
				sb.append("<td align=center fixwidth=\"40\"><font color=\"LEVEL\">Lvl</font></td>");
				sb.append("<td align=center fixwidth=\"200\"><font color=\"LEVEL\">End date</font></td>");
				sb.append("<td align=center fixwidth=\"100\"><font color=\"LEVEL\">Action</font></td>");
				sb.append("</tr>");
				sb.append("</table>");
				sb.append("<table border=0 cellpadding=0 cellspacing=0 bgcolor=\"363636\">");
				clanHall.getFunctions().forEach(function ->
				{
					sb.append("<tr>");
					sb.append("<td align=center fixwidth=\"40\">" + function.getId() + "</td>");
					sb.append("<td align=center fixwidth=\"200\">" + function.getType().toString() + "</td>");
					sb.append("<td align=center fixwidth=\"40\">" + function.getLevel() + "</td>");
					sb.append("<td align=center fixwidth=\"200\">" + new SimpleDateFormat("dd/MM HH:mm").format(new Date(function.getExpiration())) + "</td>");
					sb.append("<td align=center fixwidth=\"100\"><button value=\"Remove\" action=\"bypass -h admin_clanhall id=" + clanHallId + " action=cancelFunc actionVal=" + function.getId() + "\" width=50 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
					sb.append("</tr>");
				});
				sb.append("</table>");
			}
			else
			{
				sb.append("This Clan Hall doesn't have any Function yet.");
			}
			html.replace("%functionList%", sb.toString());
			player.sendPacket(html);
		}
		else
		{
			player.sendMessage("Clan Hall with id " + clanHallId + " does not exist!");
			useAdminCommand("admin_clanhall", player);
		}
	}
	
	private void processBypass(Player player, String command)
	{
		final int page = parseInt(command, "page", 0);
		final int clanHallId = parseInt(command, "id", 0);
		final String action = parseString(command, "action", null);
		final String actionVal = parseString(command, "actionVal", null);
		
		if ((clanHallId > 0) && (action != null))
		{
			doAction(player, clanHallId, action, actionVal);
		}
		else if (clanHallId > 0)
		{
			sendClanHallDetails(player, clanHallId);
		}
		else
		{
			sendClanHallList(player, page);
		}
	}
	
	private int parseInt(String command, String paramName, int defaultValue)
	{
		final Pattern pattern = Pattern.compile(paramName + "=([^\\s]+)");
		final Matcher matcher = pattern.matcher(command);
		if (matcher.find())
		{
			try
			{
				return Integer.parseInt(matcher.group(1).trim());
			}
			catch (NumberFormatException e)
			{
				// Ignore and return default.
			}
		}
		return defaultValue;
	}
	
	private String parseString(String command, String paramName, String defaultValue)
	{
		final Pattern pattern = Pattern.compile(paramName + "=([^\\s]+)");
		final Matcher matcher = pattern.matcher(command);
		if (matcher.find())
		{
			return matcher.group(1).trim();
		}
		return defaultValue;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
