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
package org.l2jmobius.gameserver.managers;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.l2jmobius.gameserver.GameServer;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.enums.AdminScope;
import org.l2jmobius.gameserver.data.holders.AccessLevel;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.enums.HtmlActionScope;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.ShowBoard;
import org.l2jmobius.gameserver.util.HtmlUtil;

/**
 * 2009 Gracia Epilogue Client-Safe Wide-Screen Community Board (ShowBoard) Admin Control Center
 * with Java 27 Role-Based Access Control (RBAC) and automatic NpcHtmlMessage adaptation/scrubbing.
 */
public final class AdminBoardManager
{
	private static final String BTN_BACK = "L2UI_CT1.Button_DF_Down";
	private static final String BTN_FORE = "L2UI_CT1.Button_DF";
	
	/** Matches <button ... action="bypass (-h )?admin_..." ...> tags for dynamic RBAC scrubbing. */
	private static final Pattern ADMIN_BUTTON_PATTERN = Pattern.compile(
		"<button\\b[^>]*?\\baction\\s*=\\s*\"bypass\\s+(?:-h\\s+)?(admin_[a-zA-Z0-9_]+)[^\"]*\"[^>]*>",
		Pattern.CASE_INSENSITIVE);
	
	/** Matches <a ... action="bypass (-h )?admin_..." ...>...</a> links for dynamic RBAC scrubbing. */
	private static final Pattern ADMIN_LINK_PATTERN = Pattern.compile(
		"<a\\b[^>]*?\\baction\\s*=\\s*\"bypass\\s+(?:-h\\s+)?(admin_[a-zA-Z0-9_]+)[^\"]*\"[^>]*>.*?</a>",
		Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	
	/** Matches legacy 4-button top navigation bars in data/html/admin/*.htm so they don't duplicate the CB header. */
	private static final Pattern LEGACY_TOP_NAV_PATTERN = Pattern.compile(
		"<table[^>]*>\\s*<tr>\\s*<td[^>]*>\\s*<button[^>]*value=\"Main\"[^>]*>.*?</table>(?:\\s*<br1?>)?",
		Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	
	/** Matches <title>...</title> blocks inside legacy HTML files. */
	private static final Pattern TITLE_TAG_PATTERN = Pattern.compile(
		"<title>.*?</title>",
		Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	
	private static final Pattern DUPLICATE_QBOX_EDIT_PATTERN = Pattern.compile(
		"<edit\\b[^>]*\\bvar\\s*=\\s*\"?qbox\"?[^>]*>",
		Pattern.CASE_INSENSITIVE);
	
	/** Per-GM Admin Console session state (supports both Native C++ GMWnd.HtmlViewer and BoardWnd). */
	private static final class AdminBoardSession
	{
		volatile String activeTab = "dashboard";
		volatile String lastAdaptedBody = null;
		volatile boolean boardOpen = false;
		volatile boolean useCommunityBoard = false;
		volatile boolean sendingNativeGmHtml = false;
		volatile boolean sendingAdminCbHtml = false;
		volatile int commandDepth = 0;
		volatile int renderedCount = 0;
	}
	
	private final Map<Integer, AdminBoardSession> _sessions = new ConcurrentHashMap<>();
	
	private AdminBoardManager()
	{
	}
	
	/**
	 * Strictly verifies that the player is an authenticated staff member with RBAC permissions.
	 * Regular players (accessLevel <= 0) NEVER see or access any part of the Admin Board.
	 * @param player the player to check
	 * @return true if authorized staff member
	 */
	public boolean canUseAdminBoard(Player player)
	{
		if ((player == null) || !player.isGM())
		{
			return false;
		}
		
		final AccessLevel access = player.getAccessLevel();
		return (access != null) && access.hasAdminAccess();
	}
	
	private AdminBoardSession getSession(Player player)
	{
		return _sessions.computeIfAbsent(player.getObjectId(), k -> new AdminBoardSession());
	}
	
	public void clearSession(Player player)
	{
		if (player != null)
		{
			_sessions.remove(player.getObjectId());
		}
	}
	
	/**
	 * Marks the start of an admin command execution for automatic NpcHtmlMessage interception and reactive refresh.
	 * @param player the GM executing the command
	 */
	public void beginAdminCommand(Player player)
	{
		if (!canUseAdminBoard(player))
		{
			return;
		}
		
		final AdminBoardSession session = getSession(player);
		session.commandDepth++;
		session.renderedCount = 0;
	}
	
	/**
	 * Marks the end of an admin command execution. In the 2009 Gracia Epilogue client (BoardWnd.uc / NWindow.dll),
	 * clicking any bypass button inside BoardWnd.HtmlViewer sets PageLock = true until SetHtmlBuffData is called
	 * by a new ShowBoard packet. Therefore, whenever a command is triggered via bypass while the Admin Board is open
	 * and did not already render a new ShowBoard page, we always refresh the current ShowBoard view.
	 * @param player the GM executing the command
	 * @param fullCommand the executed command
	 * @param fromBypass true if clicked from HTML/CommunityBoard bypass
	 */
	public void endAdminCommand(Player player, String fullCommand, boolean fromBypass)
	{
		if (player != null)
		{
			final AdminBoardSession s = _sessions.get(player.getObjectId());
			if ((s != null) && (s.commandDepth > 0))
			{
				s.commandDepth--;
			}
		}
		
		if (!canUseAdminBoard(player))
		{
			return;
		}
		
		final AdminBoardSession session = getSession(player);
		if (fromBypass && session.boardOpen && (session.renderedCount == 0) && shouldRefreshBoardAfter(fullCommand))
		{
			refreshCurrentView(player);
		}
	}
	
	/**
	 * Re-sends the active Admin Console view (either the current adapted sub-page or the active native tab)
	 * so that the live header updates (and BoardWnd.HtmlViewer.SetHtmlBuffData unlocks PageLock when in CB mode).
	 * @param player the staff member
	 */
	public void refreshCurrentView(Player player)
	{
		if (!canUseAdminBoard(player))
		{
			return;
		}
		
		final AdminBoardSession session = getSession(player);
		if (!session.boardOpen)
		{
			return;
		}
		
		session.useCommunityBoard = !player.getAccessLevel().allowAltG();
		final String adapted = session.lastAdaptedBody;
		if ((adapted != null) && !adapted.isEmpty())
		{
			session.renderedCount++;
			sendWrappedBoardHtml(player, adapted, session.activeTab);
		}
		else
		{
			openTab(player, session.activeTab);
		}
	}
	
	private boolean shouldRefreshBoardAfter(String fullCommand)
	{
		if (fullCommand == null)
		{
			return true;
		}
		
		final String cmd = (fullCommand.contains(" ") ? fullCommand.substring(0, fullCommand.indexOf(' ')) : fullCommand).toLowerCase(Locale.ROOT);
		// Do not re-open the board if the command explicitly closes it.
		return !cmd.equals("admin_board_close");
	}
	
	/**
	 * Handles native Admin Console navigation commands (//admin, //admin1..7, //gmw, admin_board_*).
	 * For GMs with allowAltG() == true, all navigation stays strictly inside Native C++ GMWnd.HtmlViewer
	 * and auto-closes any open Community Board window so the user is never stuck with 2 overlapping UIs.
	 * @param player the staff member
	 * @param command the full admin command
	 * @return true if handled natively by AdminBoardManager
	 */
	public boolean handleNavigationCommand(Player player, String command)
	{
		if (!canUseAdminBoard(player) || (command == null))
		{
			return false;
		}
		
		final AdminBoardSession session = getSession(player);
		final boolean canUseNativeGmWnd = player.getAccessLevel().allowAltG();
		session.useCommunityBoard = !canUseNativeGmWnd;
		final String base = (command.contains(" ") ? command.substring(0, command.indexOf(' ')) : command).toLowerCase(Locale.ROOT);
		return switch (base)
		{
			case "admin_gmw", "admin_admin", "admin_admin1", "admin_board" ->
			{
				if (canUseNativeGmWnd)
				{
					// Automatically hide BoardWnd (Community) if it was open so two UIs never overlap.
					player.sendPacket(new ShowBoard());
				}
				openTab(player, "dashboard");
				yield true;
			}
			case "admin_board_home" ->
			{
				openTab(player, "dashboard");
				yield true;
			}
			case "admin_board_refresh" ->
			{
				session.boardOpen = true;
				refreshCurrentView(player);
				yield true;
			}
			case "admin_admin6", "admin_board_support" ->
			{
				openTab(player, "support");
				yield true;
			}
			case "admin_admin2", "admin_board_world" ->
			{
				openTab(player, "world");
				yield true;
			}
			case "admin_admin3", "admin_admin5", "admin_admin7", "admin_board_events" ->
			{
				openTab(player, "events");
				yield true;
			}
			case "admin_board_economy" ->
			{
				openTab(player, "economy");
				yield true;
			}
			case "admin_admin4", "admin_board_server" ->
			{
				openTab(player, "server");
				yield true;
			}
			case "admin_board_close" ->
			{
				session.boardOpen = false;
				session.lastAdaptedBody = null;
				player.sendPacket(new ShowBoard());
				yield true;
			}
			default -> false;
		};
	}
	
	/**
	 * Opens one of the 6 native wide-screen RBAC tabs in the active viewport (Native C++ GMWnd.HtmlViewer or BoardWnd).
	 * @param player the staff member
	 * @param tab the tab identifier
	 */
	public void openTab(Player player, String tab)
	{
		if (!canUseAdminBoard(player))
		{
			return;
		}
		
		final AdminBoardSession session = getSession(player);
		session.useCommunityBoard = !player.getAccessLevel().allowAltG();
		session.activeTab = tab;
		session.lastAdaptedBody = null;
		session.boardOpen = true;
		session.renderedCount++;
		
		final String workspaceHtml = switch (tab)
		{
			case "support" -> buildSupportWorkspace(player);
			case "world" -> buildWorldWorkspace(player);
			case "events" -> buildEventsWorkspace(player);
			case "economy" -> buildEconomyWorkspace(player);
			case "server" -> buildServerWorkspace(player);
			default ->
			{
				session.activeTab = "dashboard";
				yield buildDashboardWorkspace(player);
			}
		};
		
		sendWrappedBoardHtml(player, workspaceHtml, session.activeTab);
	}
	
	/**
	 * Renders a legacy data/html/admin/ file inside the wide-screen RBAC Console shell.
	 * @param player the staff member
	 * @param fileName relative path under data/html/admin/
	 */
	public void showAdminHtmlFile(Player player, String fileName)
	{
		if (!canUseAdminBoard(player))
		{
			return;
		}
		
		// Map legacy root menu files directly to native wide-screen RBAC tabs.
		if (fileName != null)
		{
			switch (fileName.toLowerCase(Locale.ROOT))
			{
				case "main_menu.htm" ->
				{
					openTab(player, "dashboard");
					return;
				}
				case "char_menu.htm" ->
				{
					openTab(player, "support");
					return;
				}
				case "game_menu.htm" ->
				{
					openTab(player, "world");
					return;
				}
				case "gm_menu.htm", "effects_menu.htm", "mods_menu.htm" ->
				{
					openTab(player, "events");
					return;
				}
				case "server_menu.htm" ->
				{
					openTab(player, "server");
					return;
				}
			}
		}
		
		final String html = HtmCache.getInstance().getHtm(player, "data/html/admin/" + fileName);
		if (html == null)
		{
			sendAdminHtml(player, "<html><body><center><br><font color=\"FF6666\">Admin HTML page not found: data/html/admin/" + fileName + "</font></center></body></html>");
			return;
		}
		
		sendAdminHtml(player, html);
	}
	
	/**
	 * Intercepts an NpcHtmlMessage destined for a GM if it is an unbound admin panel (npcObjId == 0)
	 * and automatically promotes it to the RBAC-filtered Native C++ GMWnd.HtmlViewer (or BoardWnd).
	 * @param player the GM receiving the packet
	 * @param packet the NpcHtmlMessage packet
	 * @return true if intercepted and rendered to the Admin Console
	 */
	public boolean tryInterceptAdminHtml(Player player, NpcHtmlMessage packet)
	{
		if (!canUseAdminBoard(player) || (packet == null) || (packet.getNpcObjId() != 0))
		{
			return false;
		}
		
		final AdminBoardSession session = getSession(player);
		if (session.sendingNativeGmHtml)
		{
			return false;
		}
		
		final String html = packet.getHtml();
		if ((html == null) || html.isEmpty())
		{
			return false;
		}
		
		final boolean isAdminContent = (session.commandDepth > 0)
			|| html.contains("bypass admin_")
			|| html.contains("bypass -h admin_")
			|| html.contains("link admin/");
		if (!isAdminContent)
		{
			return false;
		}
		
		session.useCommunityBoard = !player.getAccessLevel().allowAltG();
		sendAdminHtml(player, html);
		return true;
	}
	
	/**
	 * Intercepts direct HtmlUtil.sendCBHtml calls made by legacy admin handlers (e.g. AdminAnnouncements,
	 * AdminGeodata) during an active admin command so they render inside Native C++ GMWnd.HtmlViewer
	 * instead of popping open a second Community Board window.
	 * @param player the GM receiving the CB HTML
	 * @param html the raw HTML content
	 * @return true if intercepted and routed to GMWnd.HtmlViewer
	 */
	public boolean tryInterceptAdminBoardHtml(Player player, String html)
	{
		if (!canUseAdminBoard(player) || (html == null) || html.isEmpty() || !player.getAccessLevel().allowAltG())
		{
			return false;
		}
		
		final AdminBoardSession session = getSession(player);
		if (session.sendingAdminCbHtml || (session.commandDepth <= 0))
		{
			return false;
		}
		
		session.useCommunityBoard = false;
		sendAdminHtml(player, html);
		return true;
	}
	
	/**
	 * Scrubs unauthorized RBAC buttons/links from raw admin HTML, adapts it for the 2009 Epilogue
	 * client, and sends it inside the wide-screen Admin Console shell.
	 * @param player the staff member
	 * @param rawHtml raw HTML content
	 */
	public void sendAdminHtml(Player player, String rawHtml)
	{
		if (!canUseAdminBoard(player) || (rawHtml == null))
		{
			return;
		}
		
		final AdminBoardSession session = getSession(player);
		session.useCommunityBoard = !player.getAccessLevel().allowAltG();
		final String adaptedBody = scrubAndAdaptHtml(player, rawHtml);
		session.lastAdaptedBody = adaptedBody;
		session.boardOpen = true;
		session.renderedCount++;
		
		sendWrappedBoardHtml(player, adaptedBody, session.activeTab);
	}
	
	/**
	 * Wraps workspace inner HTML with the 2009 Gracia Epilogue client-safe flat top Command Deck
	 * and Live Target Inspector, populates action caches, and transmits either directly to the
	 * Native C++ GMWnd.HtmlViewer (itemId=1 NpcHtmlMessage, zero PageLock) or via ShowBoard.
	 */
	private void sendWrappedBoardHtml(Player player, String workspaceBody, String activeTab)
	{
		final AdminBoardSession session = getSession(player);
		final boolean useNativeGmWnd = player.getAccessLevel().allowAltG() && ((player.getClient() == null) || (player.getClient().getProtocolVersion() < 152));
		session.useCommunityBoard = !useNativeGmWnd;
		
		final StringBuilder sb = new StringBuilder(4096);
		sb.append("<html><body><center>");
		appendTopHeaderBar(sb, player, activeTab);
		appendTargetInspectorAndQuickBox(sb, player);
		sb.append("<img src=\"L2UI.SquareGray\" width=740 height=1><br1>");
		sb.append(workspaceBody);
		sb.append("<br></center></body></html>");
		
		final String finalHtml = sb.toString();
		player.clearHtmlActions(HtmlActionScope.NPC_HTML);
		player.clearHtmlActions(HtmlActionScope.NPC_ITEM_HTML);
		HtmlUtil.buildHtmlActionCache(player, HtmlActionScope.NPC_HTML, 0, finalHtml);
		HtmlUtil.buildHtmlActionCache(player, HtmlActionScope.NPC_ITEM_HTML, 0, finalHtml);
		
		if (useNativeGmWnd)
		{
			HtmlUtil.buildHtmlActionCache(player, HtmlActionScope.COMM_BOARD_HTML, 0, finalHtml);
			final NpcHtmlMessage gmPacket = new NpcHtmlMessage(0, 1, finalHtml);
			gmPacket.disableValidation();
			session.sendingNativeGmHtml = true;
			try
			{
				player.sendPacket(gmPacket);
			}
			finally
			{
				session.sendingNativeGmHtml = false;
			}
		}
		else
		{
			session.sendingAdminCbHtml = true;
			try
			{
				HtmlUtil.sendCBHtml(player, finalHtml, 0);
			}
			finally
			{
				session.sendingAdminCbHtml = false;
			}
		}
	}
	
	/**
	 * Builds Top Bar 1 & 2: Role Badge, GM Self-Toggles, and RBAC-Filtered Category Tabs.
	 * Strictly flat 2009 Epilogue <table> tags (zero nested tables).
	 */
	private void appendTopHeaderBar(StringBuilder sb, Player gm, String activeTab)
	{
		final AccessLevel access = gm.getAccessLevel();
		final AdminBoardSession session = getSession(gm);
		final String invisLabel = gm.isInvisible() ? "Invis:ON" : "Invis:OFF";
		final String invulLabel = gm.isInvul() ? "Invul:ON" : "Invul:OFF";
		final String silenceLabel = gm.isSilenceMode() ? "MutePM:ON" : "MutePM:OFF";
		
		// Row 1: Role Identity + Self GM State Toggles (strictly RBAC filtered) + Home / Refresh controls
		sb.append("<table width=740 border=0 cellpadding=1 cellspacing=0 bgcolor=\"1B1B1B\"><tr>");
		sb.append("<td width=210 align=left><font color=\"LEVEL\">[RBAC: ").append(access.getName()).append(" Lv.").append(access.getLevel()).append("]</font></td>");
		sb.append("<td width=72 align=center>").append(AdminData.getInstance().hasAccess("admin_invis", access) ? button(invisLabel, "admin_invis", 70, 20) : "").append("</td>");
		sb.append("<td width=72 align=center>").append(AdminData.getInstance().hasAccess("admin_invul", access) ? button(invulLabel, "admin_invul", 70, 20) : "").append("</td>");
		sb.append("<td width=76 align=center>").append(AdminData.getInstance().hasAccess("admin_silence", access) ? button(silenceLabel, "admin_silence", 74, 20) : "").append("</td>");
		if (AdminData.getInstance().hasAccess("admin_gmspeed", access))
		{
			sb.append("<td width=52 align=center>").append(button("Spd 1x", "admin_gmspeed 0", 50, 20)).append("</td>");
			sb.append("<td width=52 align=center>").append(button("Spd 2x", "admin_gmspeed 2", 50, 20)).append("</td>");
			sb.append("<td width=52 align=center>").append(button("Spd 4x", "admin_gmspeed 4", 50, 20)).append("</td>");
		}
		else
		{
			sb.append("<td width=52></td><td width=52></td><td width=52></td>");
		}
		if (session.useCommunityBoard)
		{
			sb.append("<td width=78 align=center><button value=\"Player CB\" action=\"bypass _bbshome\" width=74 height=20 back=\"").append(BTN_BACK).append("\" fore=\"").append(BTN_FORE).append("\"></td>");
			sb.append("<td width=76 align=right>").append(button("Close [X]", "admin_board_close", 72, 20)).append("</td>");
		}
		else
		{
			sb.append("<td width=78 align=center>").append(button("Home", "admin_board_home", 74, 20)).append("</td>");
			sb.append("<td width=76 align=right>").append(button("Refresh", "admin_board_refresh", 72, 20)).append("</td>");
		}
		sb.append("</tr></table>");
		
		// Row 2: Dynamic RBAC Category Navigation Bar (only renders tabs permitted for this Role!)
		sb.append("<table width=740 border=0 cellpadding=1 cellspacing=0 bgcolor=\"2B2B2B\"><tr>");
		appendNavTab(sb, "Dashboard", "admin_board_home", "dashboard".equals(activeTab));
		if (access.hasScope(AdminScope.PLAYER_SUPPORT) || access.hasScope(AdminScope.CHAT_MODERATION) || access.hasScope(AdminScope.PUNISHMENT))
		{
			appendNavTab(sb, "Support & Players", "admin_board_support", "support".equals(activeTab));
		}
		if (access.hasScope(AdminScope.TELEPORT) || access.hasScope(AdminScope.WORLD_SPAWN) || access.hasScope(AdminScope.CLAN_MANAGEMENT))
		{
			appendNavTab(sb, "World & Spawns", "admin_board_world", "world".equals(activeTab));
		}
		if (access.hasScope(AdminScope.EVENT_GM) || access.hasScope(AdminScope.ANNOUNCEMENTS) || access.hasScope(AdminScope.PANEL_ACCESS))
		{
			appendNavTab(sb, "Events & GM", "admin_board_events", "events".equals(activeTab));
		}
		if (access.hasScope(AdminScope.ECONOMY_ITEMS) || access.hasScope(AdminScope.CHAR_EDIT))
		{
			appendNavTab(sb, "Items & Chars", "admin_board_economy", "economy".equals(activeTab));
		}
		if (access.hasScope(AdminScope.SERVER_ADMIN) || access.hasScope(AdminScope.MASTER_CONTROL))
		{
			appendNavTab(sb, "Server & Reload", "admin_board_server", "server".equals(activeTab));
		}
		sb.append("</tr></table>");
	}
	
	private void appendNavTab(StringBuilder sb, String label, String bypassCmd, boolean active)
	{
		final String text = active ? ("[" + label + "]") : label;
		sb.append("<td align=center>").append(button(text, bypassCmd, 118, 22)).append("</td>");
	}
	
	/**
	 * Builds Top Bar 3: Live Target Inspector + Unified QuickBox with RBAC-filtered actions.
	 * Uses strictly 1-level flat <table> rows for 2009 NWindow.dll compatibility.
	 */
	private void appendTargetInspectorAndQuickBox(StringBuilder sb, Player gm)
	{
		final AccessLevel access = gm.getAccessLevel();
		final WorldObject targetObj = gm.getTarget();
		final Creature target = (targetObj != null) && targetObj.isCreature() ? targetObj.asCreature() : gm;
		
		final String targetType;
		if (target.isPlayer())
		{
			targetType = "Player Lv." + target.getLevel();
		}
		else if (target.isNpc())
		{
			final Npc npc = target.asNpc();
			targetType = "NPC #" + npc.getId() + " Lv." + npc.getLevel();
		}
		else
		{
			targetType = target.getClass().getSimpleName() + " Lv." + target.getLevel();
		}
		
		final int curHp = (int) target.getCurrentHp();
		final int maxHp = target.getMaxHp();
		final int curMp = (int) target.getCurrentMp();
		final int maxMp = target.getMaxMp();
		
		sb.append("<table width=740 border=0 cellpadding=1 cellspacing=0 bgcolor=\"141E28\"><tr>");
		sb.append("<td width=350 align=left><font color=\"00CCFF\">Target:</font> <font color=\"FFFFFF\">")
			.append(escapeHtml(target.getName()))
			.append("</font> <font color=\"AAAAAA\">(").append(targetType).append(")</font> ")
			.append("<font color=\"00FF00\">HP ").append(curHp).append("/").append(maxHp).append("</font> ")
			.append("<font color=\"66AAFF\">MP ").append(curMp).append("/").append(maxMp).append("</font></td>");
		
		if (access.hasScope(AdminScope.PLAYER_SUPPORT))
		{
			sb.append("<td width=56 align=center>").append(button("Inspect", "admin_current_player", 54, 19)).append("</td>");
			sb.append("<td width=46 align=center>").append(button("Heal", "admin_heal", 44, 19)).append("</td>");
			sb.append("<td width=44 align=center>").append(button("Res", "admin_res", 42, 19)).append("</td>");
			sb.append("<td width=46 align=center>").append(AdminData.getInstance().hasAccess("admin_kill", access) ? button("Kill", "admin_kill", 44, 19) : "").append("</td>");
			if (access.hasScope(AdminScope.TELEPORT))
			{
				sb.append("<td width=48 align=center>").append(button("Goto", "admin_teleportto " + target.getName(), 46, 19)).append("</td>");
				sb.append("<td width=52 align=center>").append(button("Recall", "admin_recall " + target.getName(), 50, 19)).append("</td>");
			}
			else
			{
				sb.append("<td width=48></td><td width=52></td>");
			}
			if (access.hasScope(AdminScope.WORLD_SPAWN) && target.isNpc())
			{
				sb.append("<td width=56 align=center>").append(button("DelNPC", "admin_delete", 54, 19)).append("</td>");
			}
			else if (access.hasScope(AdminScope.PUNISHMENT) && target.isPlayer())
			{
				sb.append("<td width=56 align=center>").append(button("Punish", "admin_punishment info " + target.getName() + " CHARACTER", 54, 19)).append("</td>");
			}
			else
			{
				sb.append("<td width=56></td>");
			}
		}
		else
		{
			sb.append("<td width=390 align=right><font color=\"AAAAAA\">Loc: ").append(target.getX()).append(", ").append(target.getY()).append(", ").append(target.getZ()).append("</font></td>");
		}
		sb.append("</tr></table>");
		
		// Unified QuickBox Row (RBAC filtered, flat 1-level table)
		sb.append("<table width=740 border=0 cellpadding=1 cellspacing=0 bgcolor=\"202020\"><tr>");
		sb.append("<td width=65 align=left><font color=\"LEVEL\">QuickBox:</font></td>");
		sb.append("<td width=155 align=left><edit var=\"qbox\" width=148 height=14></td>");
		if (access.hasScope(AdminScope.PLAYER_SUPPORT))
		{
			sb.append("<td width=74 align=center>").append(button("Find Char", "admin_find_character $qbox", 72, 20)).append("</td>");
		}
		if (access.hasScope(AdminScope.TELEPORT))
		{
			sb.append("<td width=64 align=center>").append(button("MoveTo", "admin_move_to $qbox", 62, 20)).append("</td>");
		}
		if (access.hasScope(AdminScope.WORLD_SPAWN))
		{
			sb.append("<td width=60 align=center>").append(button("Spawn", "admin_spawn_monster $qbox", 58, 20)).append("</td>");
			sb.append("<td width=54 align=center>").append(button("Scan", "admin_scan $qbox", 52, 20)).append("</td>");
		}
		if (access.hasScope(AdminScope.ECONOMY_ITEMS))
		{
			sb.append("<td width=80 align=center>").append(button("Create Item", "admin_create_item $qbox", 78, 20)).append("</td>");
			sb.append("<td width=60 align=center>").append(button("Search", "admin_search $qbox", 58, 20)).append("</td>");
		}
		if (access.hasScope(AdminScope.ANNOUNCEMENTS))
		{
			sb.append("<td width=70 align=center>").append(button("Announce", "admin_announce $qbox", 68, 20)).append("</td>");
		}
		if (access.hasScope(AdminScope.SERVER_ADMIN))
		{
			sb.append("<td width=86 align=center>").append(button("Reload Script", "admin_reload script $qbox", 84, 20)).append("</td>");
		}
		sb.append("<td width=10></td></tr></table>");
	}
	
	/**
	 * Tab 1: Main RBAC Overview & Quick Launch Dashboard.
	 */
	private String buildDashboardWorkspace(Player gm)
	{
		final AccessLevel access = gm.getAccessLevel();
		final MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
		final long usedMb = heap.getUsed() / (1024 * 1024);
		final long maxMb = heap.getMax() / (1024 * 1024);
		final long uptimeMin = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - GameServer.getStartTime());
		final int onlineCount = World.getPlayers().size();
		final int gmCount = AdminData.getInstance().getAllGms(true).size();
		final int loadedScripts = ScriptManager.getInstance().getQuests().size();
		
		final StringBuilder sb = new StringBuilder(2048);
		sb.append("<font color=\"LEVEL\">Admin Control Center - Live Server & Role Overview</font><br1>");
		sb.append("<table width=720 border=0 cellpadding=2 cellspacing=1 bgcolor=\"1E1E1E\">");
		sb.append("<tr>");
		sb.append("<td width=180><font color=\"AAAAAA\">Online Players:</font> <font color=\"00FF00\">").append(onlineCount).append(" / ").append(ServerConfig.MAXIMUM_ONLINE_USERS).append("</font></td>");
		sb.append("<td width=180><font color=\"AAAAAA\">Online GMs:</font> <font color=\"00CCFF\">").append(gmCount).append("</font></td>");
		sb.append("<td width=180><font color=\"AAAAAA\">JVM Heap:</font> <font color=\"LEVEL\">").append(usedMb).append(" / ").append(maxMb).append(" MB</font></td>");
		sb.append("<td width=180><font color=\"AAAAAA\">Uptime / Scripts:</font> <font color=\"FFFFFF\">").append(uptimeMin).append("m / ").append(loadedScripts).append("</font></td>");
		sb.append("</tr></table><br1>");
		
		// Role Capabilities Summary
		sb.append("<table width=720 border=0 cellpadding=2 cellspacing=0 bgcolor=\"252525\"><tr>");
		sb.append("<td width=140><font color=\"LEVEL\">Granted Scopes:</font></td><td width=580><font color=\"00FF00\">");
		final Set<AdminScope> scopes = access.getScopes();
		int idx = 0;
		for (AdminScope scope : scopes)
		{
			if (idx++ > 0)
			{
				sb.append(", ");
			}
			sb.append(scope.getDisplayName());
		}
		sb.append("</font></td></tr></table><br>");
		
		// Dynamic RBAC Action Matrix (only displays cards the GM's Role is authorized to use)
		sb.append("<font color=\"CDB67F\">Authorized Quick Operations (Filtered by Role)</font><br1>");
		sb.append("<table width=720 border=0 cellpadding=2 cellspacing=2 bgcolor=\"181818\">");
		
		if (access.hasScope(AdminScope.PLAYER_SUPPORT) || access.hasScope(AdminScope.CHAT_MODERATION))
		{
			sb.append("<tr><td width=140><font color=\"00CCFF\">Support & Chat</font></td>");
			sb.append("<td width=145>").append(button("Online Players", "admin_show_characters 0", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Inspect Target", "admin_current_player", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("View Petitions", "admin_view_petitions", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(access.hasScope(AdminScope.PUNISHMENT) ? button("Punishment Panel", "admin_punishment", 135, 22) : "").append("</td></tr>");
		}
		
		if (access.hasScope(AdminScope.TELEPORT) || access.hasScope(AdminScope.WORLD_SPAWN))
		{
			sb.append("<tr><td width=140><font color=\"00FF00\">World & Navigation</font></td>");
			sb.append("<td width=145>").append(access.hasScope(AdminScope.TELEPORT) ? button("Teleport Menu", "admin_show_moves", 135, 22) : "").append("</td>");
			sb.append("<td width=145>").append(access.hasScope(AdminScope.WORLD_SPAWN) ? button("Spawn Menu", "admin_spawn_index", 135, 22) : "").append("</td>");
			sb.append("<td width=145>").append(access.hasScope(AdminScope.WORLD_SPAWN) ? button("GrandBoss Info", "admin_grandboss", 135, 22) : "").append("</td>");
			sb.append("<td width=145>").append(access.hasScope(AdminScope.WORLD_SPAWN) ? button("Castles & Sieges", "admin_siege", 135, 22) : "").append("</td></tr>");
		}
		
		if (access.hasScope(AdminScope.EVENT_GM) || access.hasScope(AdminScope.ANNOUNCEMENTS))
		{
			sb.append("<tr><td width=140><font color=\"LEVEL\">Events & Broadcast</font></td>");
			sb.append("<td width=145>").append(access.hasScope(AdminScope.ANNOUNCEMENTS) ? button("Announcements", "admin_announces list", 135, 22) : "").append("</td>");
			sb.append("<td width=145>").append(access.hasScope(AdminScope.EVENT_GM) ? button("Events Menu", "admin_event_menu", 135, 22) : "").append("</td>");
			sb.append("<td width=145>").append(access.hasScope(AdminScope.EVENT_GM) ? button("Target Buffs", "admin_getbuffs", 135, 22) : "").append("</td>");
			sb.append("<td width=145>").append(access.hasScope(AdminScope.EVENT_GM) ? button("Fence Builder", "admin_listfence", 135, 22) : "").append("</td></tr>");
		}
		
		if (access.hasScope(AdminScope.ECONOMY_ITEMS) || access.hasScope(AdminScope.CHAR_EDIT))
		{
			sb.append("<tr><td width=140><font color=\"FF9900\">Items & Editor</font></td>");
			sb.append("<td width=145>").append(access.hasScope(AdminScope.ECONOMY_ITEMS) ? button("Create Item", "admin_itemcreate", 135, 22) : "").append("</td>");
			sb.append("<td width=145>").append(access.hasScope(AdminScope.ECONOMY_ITEMS) ? button("GM Shop", "admin_gmshop", 135, 22) : "").append("</td>");
			sb.append("<td width=145>").append(access.hasScope(AdminScope.ECONOMY_ITEMS) ? button("Enchant Armor/Wpn", "admin_enchant", 135, 22) : "").append("</td>");
			sb.append("<td width=145>").append(access.hasScope(AdminScope.CHAR_EDIT) ? button("Edit Target Char", "admin_edit_character", 135, 22) : "").append("</td></tr>");
		}
		
		if (access.hasScope(AdminScope.SERVER_ADMIN))
		{
			sb.append("<tr><td width=140><font color=\"FF6666\">Server & Engine</font></td>");
			sb.append("<td width=145>").append(button("Server Diagnostics", "admin_serverinfo", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Hot-Reload Center", "admin_board_server", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Script Inspector", "admin_show_quests", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Server Shutdown", "admin_server_shutdown", 135, 22)).append("</td></tr>");
		}
		
		sb.append("</table>");
		return sb.toString();
	}
	
	/**
	 * Tab 2: Player Support, Petitions & Punishments Workspace.
	 */
	private String buildSupportWorkspace(Player gm)
	{
		final AccessLevel access = gm.getAccessLevel();
		final StringBuilder sb = new StringBuilder(2048);
		sb.append("<font color=\"LEVEL\">Player Support, Moderation & Punishments</font><br1>");
		sb.append("<table width=720 border=0 cellpadding=2 cellspacing=2 bgcolor=\"1B1B1B\">");
		
		if (access.hasScope(AdminScope.CHAT_MODERATION))
		{
			sb.append("<tr><td width=140><font color=\"00CCFF\">Petitions & Chat</font></td>");
			sb.append("<td width=145>").append(button("View Petitions", "admin_view_petitions", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Force Petition", "admin_force_peti Thank you", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Reset Petitions", "admin_reset_petitions", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("GM Chat Panel", "admin_gmchat_menu", 135, 22)).append("</td></tr>");
		}
		
		if (access.hasScope(AdminScope.PLAYER_SUPPORT))
		{
			sb.append("<tr><td width=140><font color=\"00FF00\">Character Lookup</font></td>");
			sb.append("<td width=145>").append(button("Online List", "admin_show_characters 0", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Inspect Target", "admin_current_player", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Find Dualbox", "admin_find_dualbox 2", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Find by IP ($qbox)", "admin_find_ip $qbox", 135, 22)).append("</td></tr>");
			
			sb.append("<tr><td width=140><font color=\"00FF00\">Target Assistance</font></td>");
			sb.append("<td width=145>").append(button("Instance Lockouts", "admin_instancezone", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Active Buffs", "admin_getbuffs", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Clear Skill Reuse", "admin_removereuse", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Character Quests", "admin_charquestmenu", 135, 22)).append("</td></tr>");
			
			sb.append("<tr><td width=140><font color=\"00FF00\">Recovery Actions</font></td>");
			sb.append("<td width=145>").append(button("Heal Target", "admin_heal", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Resurrect Target", "admin_res", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Disconnect Target", "admin_character_disconnect", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Repair Char ($qbox)", "admin_repair $qbox", 135, 22)).append("</td></tr>");
		}
		
		if (access.hasScope(AdminScope.PUNISHMENT))
		{
			sb.append("<tr><td width=140><font color=\"FF6666\">Punishments</font></td>");
			sb.append("<td width=145>").append(button("Punishment Center", "admin_punishment", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Jail ($qbox)", "admin_jail $qbox", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Unjail ($qbox)", "admin_unjail $qbox", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Kick ($qbox)", "admin_kick $qbox", 135, 22)).append("</td></tr>");
			
			sb.append("<tr><td width=140><font color=\"FF6666\">Account / Char Ban</font></td>");
			sb.append("<td width=145>").append(button("Ban Char ($qbox)", "admin_ban_char $qbox", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Unban Char ($qbox)", "admin_unban_char $qbox", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Ban Acc ($qbox)", "admin_ban_acc $qbox", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Unban Acc ($qbox)", "admin_unban_acc $qbox", 135, 22)).append("</td></tr>");
		}
		
		sb.append("</table>");
		return sb.toString();
	}
	
	/**
	 * Tab 3: World Navigation, Spawns, Bosses, Sieges & Clans Workspace.
	 */
	private String buildWorldWorkspace(Player gm)
	{
		final AccessLevel access = gm.getAccessLevel();
		final StringBuilder sb = new StringBuilder(2048);
		sb.append("<font color=\"LEVEL\">World Navigation, Spawns, Bosses & Sieges</font><br1>");
		sb.append("<table width=720 border=0 cellpadding=2 cellspacing=2 bgcolor=\"1B1B1B\">");
		
		if (access.hasScope(AdminScope.TELEPORT))
		{
			sb.append("<tr><td width=140><font color=\"00CCFF\">Teleportation</font></td>");
			sb.append("<td width=145>").append(button("Teleport Menu", "admin_show_moves", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Teleport Panel", "admin_tele", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Recall Party", "admin_recall_party", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Recall Clan", "admin_recall_clan", 135, 22)).append("</td></tr>");
		}
		
		if (access.hasScope(AdminScope.WORLD_SPAWN))
		{
			sb.append("<tr><td width=140><font color=\"00FF00\">NPC & Spawns</font></td>");
			sb.append("<td width=145>").append(button("Spawn By Level", "admin_spawn_index", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("NPC Index", "admin_npc_index", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Scan Nearby NPCs", "admin_scan", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Delete Target NPC", "admin_delete", 135, 22)).append("</td></tr>");
			
			sb.append("<tr><td width=140><font color=\"LEVEL\">Bosses & World</font></td>");
			sb.append("<td width=145>").append(button("GrandBoss Manager", "admin_grandboss", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Gracia Seeds", "admin_gracia_seeds", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Cursed Weapons", "admin_cw_info_menu", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Find Mammon", "admin_mammon_find", 135, 22)).append("</td></tr>");
			
			sb.append("<tr><td width=140><font color=\"FF9900\">Sieges & Territory</font></td>");
			sb.append("<td width=145>").append(button("Castle Sieges", "admin_siege", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Fortress Sieges", "admin_fortsiege", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("ClanHall Sieges", "admin_chsiege_siegablehall", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Territory War", "admin_territory_war", 135, 22)).append("</td></tr>");
			
			sb.append("<tr><td width=140><font color=\"AAAAAA\">Zones & Manor</font></td>");
			sb.append("<td width=145>").append(button("Zone Check", "admin_zone_check", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Zone Builder", "admin_zone_build", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Active Instances", "admin_listinstances", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Manor Status", "admin_manor", 135, 22)).append("</td></tr>");
		}
		
		if (access.hasScope(AdminScope.CLAN_MANAGEMENT))
		{
			sb.append("<tr><td width=140><font color=\"00CCFF\">Clan Management</font></td>");
			sb.append("<td width=145>").append(button("Target Clan Info", "admin_clan_info", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Pending Leaders", "admin_clan_show_pending", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Pledge Panel", "admin_html pledge.htm", 135, 22)).append("</td>");
			sb.append("<td width=145></td></tr>");
		}
		
		sb.append("</table>");
		return sb.toString();
	}
	
	/**
	 * Tab 4: Events, Broadcasts, Doors/Fences & GM Utilities Workspace.
	 */
	private String buildEventsWorkspace(Player gm)
	{
		final AccessLevel access = gm.getAccessLevel();
		final StringBuilder sb = new StringBuilder(2048);
		sb.append("<font color=\"LEVEL\">Events, Announcements, World FX & GM Controls</font><br1>");
		sb.append("<table width=720 border=0 cellpadding=2 cellspacing=2 bgcolor=\"1B1B1B\">");
		
		if (access.hasScope(AdminScope.ANNOUNCEMENTS))
		{
			sb.append("<tr><td width=140><font color=\"00CCFF\">Announcements</font></td>");
			sb.append("<td width=145>").append(button("Manage Announces", "admin_announces list", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Announce ($qbox)", "admin_announce $qbox", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Critical ($qbox)", "admin_announce_crit $qbox", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Screen ($qbox)", "admin_announce_screen $qbox", 135, 22)).append("</td></tr>");
		}
		
		if (access.hasScope(AdminScope.EVENT_GM))
		{
			sb.append("<tr><td width=140><font color=\"00FF00\">Events & Arena</font></td>");
			sb.append("<td width=145>").append(button("Event Manager", "admin_event_menu", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Fence Builder", "admin_listfence", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Open Target Door", "admin_open", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Close Target Door", "admin_close", 135, 22)).append("</td></tr>");
			
			sb.append("<tr><td width=140><font color=\"LEVEL\">Buffs & Effects</font></td>");
			sb.append("<td width=145>").append(button("Target Buffs", "admin_getbuffs", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Stop All Buffs", "admin_stopallbuffs", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Area Cancel", "admin_areacancel 500", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Effects & Socials", "admin_html effects_menu.htm", 135, 22)).append("</td></tr>");
			
			sb.append("<tr><td width=140><font color=\"FF9900\">Mounts & Transform</font></td>");
			sb.append("<td width=145>").append(button("Ride Wyvern", "admin_ride_wyvern", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Ride Strider", "admin_ride_strider", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Ride Jet Bike", "admin_ride_bike", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Untransform", "admin_untransform", 135, 22)).append("</td></tr>");
		}
		
		sb.append("<tr><td width=140><font color=\"AAAAAA\">GM Self Status</font></td>");
		sb.append("<td width=145>").append(button("GM List ON", "admin_gmliston", 135, 22)).append("</td>");
		sb.append("<td width=145>").append(button("GM List OFF", "admin_gmlistoff", 135, 22)).append("</td>");
		sb.append("<td width=145>").append(button("Toggle Diet", "admin_diet", 135, 22)).append("</td>");
		sb.append("<td width=145>").append(button("Toggle TradeOff", "admin_tradeoff", 135, 22)).append("</td></tr>");
		
		sb.append("</table>");
		return sb.toString();
	}
	
	/**
	 * Tab 5: Economy, Item Creation & Character Editor Workspace.
	 */
	private String buildEconomyWorkspace(Player gm)
	{
		final AccessLevel access = gm.getAccessLevel();
		final StringBuilder sb = new StringBuilder(2048);
		sb.append("<font color=\"LEVEL\">Items, Economy & Character Editor</font><br1>");
		sb.append("<table width=720 border=0 cellpadding=2 cellspacing=2 bgcolor=\"1B1B1B\">");
		
		if (access.hasScope(AdminScope.ECONOMY_ITEMS))
		{
			sb.append("<tr><td width=140><font color=\"00FF00\">Item Creation</font></td>");
			sb.append("<td width=145>").append(button("Item Creator", "admin_itemcreate", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("GM Shop", "admin_gmshop", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Search Item/NPC", "admin_search $qbox", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Destroy Target Inv", "admin_destroy_items", 135, 22)).append("</td></tr>");
			
			sb.append("<tr><td width=140><font color=\"LEVEL\">Gear Enhancement</font></td>");
			sb.append("<td width=145>").append(button("Enchant Panel", "admin_enchant", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Element Panel", "admin_html element.htm", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Augment Panel", "admin_html augment.htm", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Premium Menu", "admin_premium_menu", 135, 22)).append("</td></tr>");
			
			sb.append("<tr><td width=140><font color=\"00CCFF\">Points & Rewards</font></td>");
			sb.append("<td width=145>").append(button("PC Bang Points", "admin_pccafepoints", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Game Points", "admin_gamepoints", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Give Adena (10M)", "admin_create_item 57 10000000", 135, 22)).append("</td>");
			sb.append("<td width=145></td></tr>");
		}
		
		if (access.hasScope(AdminScope.CHAR_EDIT))
		{
			sb.append("<tr><td width=140><font color=\"FF9900\">Character Editor</font></td>");
			sb.append("<td width=145>").append(button("Edit Target Char", "admin_edit_character", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Manage Skills", "admin_skill_list", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Add EXP / SP", "admin_exp_sp_to_character", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Set Level ($qbox)", "admin_set_level $qbox", 135, 22)).append("</td></tr>");
			
			sb.append("<tr><td width=140><font color=\"FF9900\">Status & Class</font></td>");
			sb.append("<td width=145>").append(button("Give All Skills", "admin_give_all_skills", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Toggle Hero", "admin_sethero", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Class Selection", "admin_html charclasses.htm", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Target Quests", "admin_charquestmenu", 135, 22)).append("</td></tr>");
		}
		
		sb.append("</table>");
		return sb.toString();
	}
	
	/**
	 * Tab 6: Server Administration & Zero-Downtime Transactional Hot-Reload Console.
	 */
	private String buildServerWorkspace(Player gm)
	{
		final AccessLevel access = gm.getAccessLevel();
		final StringBuilder sb = new StringBuilder(2048);
		sb.append("<font color=\"LEVEL\">Server Administration & Zero-Downtime Hot-Reload Engine</font><br1>");
		sb.append("<table width=720 border=0 cellpadding=2 cellspacing=2 bgcolor=\"1B1B1B\">");
		
		if (access.hasScope(AdminScope.SERVER_ADMIN))
		{
			sb.append("<tr><td width=140><font color=\"00FF00\">Script Hot-Reload</font></td>");
			sb.append("<td width=145>").append(button("Reload Script ($qbox)", "admin_reload script $qbox", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Script Inspector", "admin_show_quests", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Reload HTML Cache", "admin_reload htm", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Reload Configs", "admin_reload config", 135, 22)).append("</td></tr>");
			
			sb.append("<tr><td width=140><font color=\"00CCFF\">Data Reloaders</font></td>");
			sb.append("<td width=145>").append(button("Reload Skills XML", "admin_reload skill", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Reload Items XML", "admin_reload item", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Reload NPCs XML", "admin_reload npc", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Reload Multisell", "admin_reload multisell", 135, 22)).append("</td></tr>");
			
			sb.append("<tr><td width=140><font color=\"LEVEL\">RBAC & World Data</font></td>");
			sb.append("<td width=145>").append(button("Reload Access/RBAC", "admin_reload access", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Reload Teleports", "admin_reload teleport", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Reload Buylists", "admin_reload buylist", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Reload Enchants", "admin_reload enchant", 135, 22)).append("</td></tr>");
			
			sb.append("<tr><td width=140><font color=\"FF9900\">Server Lifecycle</font></td>");
			sb.append("<td width=145>").append(button("JVM & Server Info", "admin_serverinfo", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Shutdown / Restart", "admin_server_shutdown", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("LoginServer Menu", "admin_server_login", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Geodata Menu", "admin_html geo_menu.htm", 135, 22)).append("</td></tr>");
			
			sb.append("<tr><td width=140><font color=\"AAAAAA\">Olympiad & Debug</font></td>");
			sb.append("<td width=145>").append(button("Check Olympiad", "admin_checkolympiad", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Save Olympiad", "admin_saveolymp", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Debug Target", "admin_debug", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Fake Players", "admin_fakeplayers", 135, 22)).append("</td></tr>");
		}
		
		if (access.hasScope(AdminScope.MASTER_CONTROL))
		{
			sb.append("<tr><td width=140><font color=\"FF6666\">Master Security</font></td>");
			sb.append("<td width=145>").append(button("Set Access ($qbox)", "admin_changelvl $qbox", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("Packet Forge", "admin_forge", 135, 22)).append("</td>");
			sb.append("<td width=145>").append(button("System Msg ($qbox)", "admin_msg $qbox", 135, 22)).append("</td>");
			sb.append("<td width=145></td></tr>");
		}
		
		sb.append("</table>");
		return sb.toString();
	}
	
	/**
	 * Dynamically scrubs unauthorized RBAC buttons/links from raw admin HTML and adapts it for
	 * safe rendering inside the 2009 Gracia Epilogue Community Board (ShowBoard).
	 * @param gm the staff member viewing the page
	 * @param rawHtml the raw HTML from NpcHtmlMessage or data/html/admin/
	 * @return sanitized, RBAC-filtered inner HTML fragment
	 */
	public String scrubAndAdaptHtml(Player gm, String rawHtml)
	{
		return scrubAndAdaptHtml(gm != null ? gm.getAccessLevel() : null, rawHtml);
	}
	
	/**
	 * Dynamically scrubs unauthorized RBAC buttons/links from raw admin HTML for a given AccessLevel
	 * and adapts it for safe rendering inside the 2009 Gracia Epilogue Community Board (ShowBoard).
	 * @param access the staff member's AccessLevel
	 * @param rawHtml the raw HTML from NpcHtmlMessage or data/html/admin/
	 * @return sanitized, RBAC-filtered inner HTML fragment
	 */
	public String scrubAndAdaptHtml(AccessLevel access, String rawHtml)
	{
		if ((rawHtml == null) || rawHtml.isEmpty())
		{
			return "";
		}
		
		String body = rawHtml;
		
		// 1. Strip <title>...</title> and outer <html> / <body> wrappers so NWindow.dll never sees nested <html> tags.
		body = TITLE_TAG_PATTERN.matcher(body).replaceAll("");
		body = body.replaceAll("(?i)</?html[^>]*>", "");
		body = body.replaceAll("(?i)</?body[^>]*>", "");
		body = body.replaceAll("(?i)</?head[^>]*>", "");
		
		// 2. Strip legacy 4-button top navigation bar (Main | Char | Game | GM) to avoid duplicate headers.
		body = LEGACY_TOP_NAV_PATTERN.matcher(body).replaceFirst("");
		
		// 3. Dynamically scrub <button ... action="bypass ... admin_..."> where the GM's Role lacks permission.
		final Matcher btnMatcher = ADMIN_BUTTON_PATTERN.matcher(body);
		final StringBuilder btnBuffer = new StringBuilder(body.length());
		while (btnMatcher.find())
		{
			final String cmd = btnMatcher.group(1);
			if (AdminData.getInstance().hasAccess(cmd, access))
			{
				btnMatcher.appendReplacement(btnBuffer, Matcher.quoteReplacement(btnMatcher.group(0)));
			}
			else
			{
				btnMatcher.appendReplacement(btnBuffer, "");
			}
		}
		btnMatcher.appendTail(btnBuffer);
		body = btnBuffer.toString();
		
		// 4. Dynamically scrub <a action="bypass ... admin_...">...</a> where the GM's Role lacks permission.
		final Matcher linkMatcher = ADMIN_LINK_PATTERN.matcher(body);
		final StringBuilder linkBuffer = new StringBuilder(body.length());
		while (linkMatcher.find())
		{
			final String cmd = linkMatcher.group(1);
			if (AdminData.getInstance().hasAccess(cmd, access))
			{
				linkMatcher.appendReplacement(linkBuffer, Matcher.quoteReplacement(linkMatcher.group(0)));
			}
			else
			{
				linkMatcher.appendReplacement(linkBuffer, "");
			}
		}
		linkMatcher.appendTail(linkBuffer);
		body = linkBuffer.toString();
		
		// 5. Normalize "bypass -h " -> "bypass " so clicking buttons in ShowBoard never closes the Community Board window!
		body = body.replaceAll("(?i)action\\s*=\\s*\"bypass\\s+-h\\s+", "action=\"bypass ");
		
		// 6. Convert legacy "link admin/..." actions into "bypass admin_html ..." so ShowBoard handles them seamlessly.
		body = body.replaceAll("(?i)action\\s*=\\s*\"link\\s+admin/([^\"]+)\"", "action=\"bypass admin_html $1\"");
		
		// 7. Strip duplicate <edit var="qbox"> inputs in adapted sub-pages so NWindow.dll never collides with Top Bar QuickBox.
		body = DUPLICATE_QBOX_EDIT_PATTERN.matcher(body).replaceAll("<font color=\"LEVEL\">[Use Top QuickBox]</font>");
		
		// 8. Promote cramped 260/270px legacy tables to comfortable wide-screen widths.
		body = body.replaceAll("(?i)<table\\s+width\\s*=\\s*\"?(?:260|270|280)\"?\\b", "<table width=640");
		
		return body.trim();
	}
	
	private static String button(String label, String adminCommand, int width, int height)
	{
		return "<button value=\"" + label + "\" action=\"bypass " + adminCommand + "\" width=" + width + " height=" + height + " back=\"" + BTN_BACK + "\" fore=\"" + BTN_FORE + "\">";
	}
	
	private static String escapeHtml(String text)
	{
		if (text == null)
		{
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
	
	public static AdminBoardManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AdminBoardManager INSTANCE = new AdminBoardManager();
	}
}
