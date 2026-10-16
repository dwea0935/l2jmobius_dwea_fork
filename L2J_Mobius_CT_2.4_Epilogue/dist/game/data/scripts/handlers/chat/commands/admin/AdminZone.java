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
package handlers.chat.commands.admin;

import java.util.StringTokenizer;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerAction;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.entity.zone.type.NpcSpawnTerritory;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.ZoneBuildManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.network.serverpackets.ConfirmDlg;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Unified Zone Inspector & Zone Builder handler.
 */
public class AdminZone implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_zone_check",
		"admin_zone_visual",
		"admin_zone_visual_clear",
		"admin_zone_build",
		"admin_zone_build_save",
		"admin_zone_build_delete",
		"admin_zone_build_toggle",
		"admin_zone_build_clear"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (activeChar == null)
		{
			return false;
		}
		
		if (command.equals("admin_zone_build"))
		{
			sendZoneBuilderHtml(activeChar);
			return true;
		}
		else if (command.startsWith("admin_zone_build_toggle"))
		{
			if (activeChar.getTeleMode() == 3)
			{
				activeChar.setTeleMode(0);
				activeChar.sendMessage("Draw zone disabled. You can move.");
			}
			else
			{
				activeChar.setTeleMode(3);
				activeChar.sendMessage("Draw zone active. Click on point to record.");
			}
			
			sendZoneBuilderHtml(activeChar);
			return true;
		}
		else if (command.startsWith("admin_zone_build_clear"))
		{
			ZoneBuildManager.getInstance().clearZone(activeChar);
			sendZoneBuilderHtml(activeChar);
			return true;
		}
		else if (command.startsWith("admin_zone_build_delete"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			if (st.hasMoreTokens())
			{
				final String nextT = st.nextToken();
				if (StringUtil.isNumeric(nextT))
				{
					final int entry = Integer.parseInt(nextT);
					ZoneBuildManager.getInstance().deleteEntry(activeChar, entry);
					sendZoneBuilderHtml(activeChar);
					return true;
				}
			}
			return false;
		}
		else if (command.equals("admin_zone_build_save"))
		{
			activeChar.addAction(PlayerAction.ADMIN_SAVE_ZONE);
			activeChar.sendPacket(new ConfirmDlg("Do you wish to save the zone?"));
			return true;
		}
		
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();
		if (actualCommand.equalsIgnoreCase("admin_zone_check"))
		{
			showHtml(activeChar);
			activeChar.sendSysMessage("MapRegion: x:" + MapRegionData.getInstance().getMapRegionX(activeChar.getX()) + " y:" + MapRegionData.getInstance().getMapRegionY(activeChar.getY()) + " (" + MapRegionData.getInstance().getMapRegionLocId(activeChar) + ")");
			getGeoRegionXY(activeChar);
			activeChar.sendSysMessage("Closest Town: " + MapRegionData.getInstance().getClosestTownName(activeChar));
			
			if (!activeChar.isInInstance())
			{
				Location loc;
				loc = MapRegionData.getInstance().getTeleToLocation(activeChar, TeleportWhereType.CASTLE);
				activeChar.sendSysMessage("TeleToLocation (Castle): x:" + loc.getX() + " y:" + loc.getY() + " z:" + loc.getZ());
				loc = MapRegionData.getInstance().getTeleToLocation(activeChar, TeleportWhereType.CLANHALL);
				activeChar.sendSysMessage("TeleToLocation (ClanHall): x:" + loc.getX() + " y:" + loc.getY() + " z:" + loc.getZ());
				loc = MapRegionData.getInstance().getTeleToLocation(activeChar, TeleportWhereType.SIEGEFLAG);
				activeChar.sendSysMessage("TeleToLocation (SiegeFlag): x:" + loc.getX() + " y:" + loc.getY() + " z:" + loc.getZ());
				loc = MapRegionData.getInstance().getTeleToLocation(activeChar, TeleportWhereType.TOWN);
				activeChar.sendSysMessage("TeleToLocation (Town): x:" + loc.getX() + " y:" + loc.getY() + " z:" + loc.getZ());
			}
		}
		else if (actualCommand.equalsIgnoreCase("admin_zone_visual"))
		{
			final String next = st.nextToken();
			if (next.equalsIgnoreCase("all"))
			{
				for (ZoneType zone : ZoneManager.getInstance().getZones(activeChar))
				{
					zone.visualizeZone(activeChar.getZ());
				}
				
				for (NpcSpawnTerritory territory : ZoneManager.getInstance().getSpawnTerritories(activeChar))
				{
					territory.visualizeZone(activeChar.getZ());
				}
				
				showHtml(activeChar);
			}
			else
			{
				final int zoneId = Integer.parseInt(next);
				ZoneManager.getInstance().getZoneById(zoneId).visualizeZone(activeChar.getZ());
			}
		}
		else if (actualCommand.equalsIgnoreCase("admin_zone_visual_clear"))
		{
			ZoneManager.getInstance().clearDebugItems();
			showHtml(activeChar);
		}
		
		return true;
	}
	
	private void sendZoneBuilderHtml(Player activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(activeChar, "data/html/admin/zone_builder.htm");
		html.replace("%paths%", ZoneBuildManager.getInstance().getPathsForHtml(activeChar));
		html.replace("%movement%", activeChar.getTeleMode() == 3 ? "Disable Draw" : "Enable Draw");
		activeChar.sendPacket(html);
	}
	
	private void showHtml(Player activeChar)
	{
		final String htmContent = HtmCache.getInstance().getHtm(activeChar, "data/html/admin/zone.htm");
		final NpcHtmlMessage adminReply = new NpcHtmlMessage();
		adminReply.setHtml(htmContent);
		adminReply.replace("%PEACE%", (activeChar.isInsideZone(ZoneId.PEACE) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%PVP%", (activeChar.isInsideZone(ZoneId.PVP) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%SIEGE%", (activeChar.isInsideZone(ZoneId.SIEGE) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%TOWN%", (activeChar.isInsideZone(ZoneId.TOWN) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%CASTLE%", (activeChar.isInsideZone(ZoneId.CASTLE) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%FORT%", (activeChar.isInsideZone(ZoneId.FORT) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%HQ%", (activeChar.isInsideZone(ZoneId.HQ) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%CLANHALL%", (activeChar.isInsideZone(ZoneId.CLAN_HALL) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%LAND%", (activeChar.isInsideZone(ZoneId.LANDING) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%NOLAND%", (activeChar.isInsideZone(ZoneId.NO_LANDING) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%NOSUMMON%", (activeChar.isInsideZone(ZoneId.NO_SUMMON_FRIEND) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%WATER%", (activeChar.isInsideZone(ZoneId.WATER) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%SWAMP%", (activeChar.isInsideZone(ZoneId.SWAMP) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%DANGER%", (activeChar.isInsideZone(ZoneId.DANGER_AREA) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%NOSTORE%", (activeChar.isInsideZone(ZoneId.NO_STORE) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		adminReply.replace("%SCRIPT%", (activeChar.isInsideZone(ZoneId.SCRIPT) ? "<font color=\"LEVEL\">YES</font>" : "NO"));
		
		final StringBuilder zones = new StringBuilder(100);
		for (ZoneType zone : ZoneManager.getInstance().getZones(activeChar))
		{
			if (zone.getName() != null)
			{
				zones.append(zone.getName());
				if (zone.getId() < 300000)
				{
					zones.append(" (");
					zones.append(zone.getId());
					zones.append(')');
				}
				zones.append("<br1>");
			}
			else
			{
				zones.append(zone.getId());
			}
			zones.append(' ');
		}
		
		for (NpcSpawnTerritory territory : ZoneManager.getInstance().getSpawnTerritories(activeChar))
		{
			zones.append(territory.getName());
			zones.append("<br1>");
		}
		
		adminReply.replace("%ZLIST%", zones.toString());
		activeChar.sendPacket(adminReply);
	}
	
	private void getGeoRegionXY(Player activeChar)
	{
		final int worldX = activeChar.getX();
		final int worldY = activeChar.getY();
		final int geoX = (((worldX - -327680) >> 4) >> 11) + 10;
		final int geoY = (((worldY - -262144) >> 4) >> 11) + 10;
		activeChar.sendSysMessage("GeoRegion: " + geoX + "_" + geoY);
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
