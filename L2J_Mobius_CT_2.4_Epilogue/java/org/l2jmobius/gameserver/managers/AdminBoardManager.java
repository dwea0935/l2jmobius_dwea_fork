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
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.l2jmobius.gameserver.GameServer;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.enums.AdminScope;
import org.l2jmobius.gameserver.data.holders.AccessLevel;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.network.enums.HtmlActionScope;
import org.l2jmobius.gameserver.network.serverpackets.GMHennaInfo;
import org.l2jmobius.gameserver.network.serverpackets.GMViewCharacterInfo;
import org.l2jmobius.gameserver.network.serverpackets.GMViewItemList;
import org.l2jmobius.gameserver.network.serverpackets.GMViewPledgeInfo;
import org.l2jmobius.gameserver.network.serverpackets.GMViewSkillInfo;
import org.l2jmobius.gameserver.network.serverpackets.GMViewWarehouseWithdrawList;
import org.l2jmobius.gameserver.network.serverpackets.GmViewQuestInfo;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.scripting.ScriptEngine;
import org.l2jmobius.gameserver.util.HtmlUtil;

/**
 * Native C++ GMWnd (Alt+G) Wide-Screen Admin Control Center v3 (940px / 920px GMWnd.HtmlViewer)
 * with per-GM breadcrumb navigation stack, Java 27 Role-Based Access Control (RBAC),
 * and automatic NpcHtmlMessage adaptation/scrubbing.
 */
public final class AdminBoardManager
{
	private static final String BTN_BACK = "L2UI_CT1.Button_DF_Down";
	private static final String BTN_FORE = "L2UI_CT1.Button_DF";
	private static final int MAX_NAV_STACK_DEPTH = 14;
	
	/** Matches <button ... action="bypass (-h )?admin_..." ...> tags for dynamic RBAC scrubbing. */
	private static final Pattern ADMIN_BUTTON_PATTERN = Pattern.compile(
		"<button\\b[^>]*?\\baction\\s*=\\s*\"bypass\\s+(?:-h\\s+)?(admin_[a-zA-Z0-9_]+)[^\"]*\"[^>]*>",
		Pattern.CASE_INSENSITIVE);
	
	/** Matches <a ... action="bypass (-h )?admin_..." ...>...</a> links for dynamic RBAC scrubbing. */
	private static final Pattern ADMIN_LINK_PATTERN = Pattern.compile(
		"<a\\b[^>]*?\\baction\\s*=\\s*\"bypass\\s+(?:-h\\s+)?(admin_[a-zA-Z0-9_]+)[^\"]*\"[^>]*>.*?</a>",
		Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	
	/** Matches legacy 4-button top navigation bars in data/html/admin/*.htm so they don't duplicate the console header. */
	private static final Pattern LEGACY_TOP_NAV_PATTERN = Pattern.compile(
		"<table[^>]*>\\s*<tr>\\s*<td[^>]*>\\s*<button[^>]*value=\"Main\"[^>]*>.*?</table>(?:\\s*<br1?>)?",
		Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	
	/** Matches <title>...</title> blocks inside legacy HTML files. */
	private static final Pattern TITLE_TAG_PATTERN = Pattern.compile(
		"<title>(.*?)</title>",
		Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	
	private static final Pattern DUPLICATE_QBOX_EDIT_PATTERN = Pattern.compile(
		"<edit\\b[^>]*\\bvar\\s*=\\s*\"?qbox\"?[^>]*>",
		Pattern.CASE_INSENSITIVE);
	
	/** Single entry in the per-GM navigation history stack for 1-level [◄ Back] and clickable breadcrumbs. */
	private record NavEntry(String tab, String commandOrFile, String rawHtml, String label)
	{
	}
	
	/** Per-GM Native C++ GMWnd.HtmlViewer Admin Console session state. */
	private static final class AdminBoardSession
	{
		volatile String activeTab = "dashboard";
		volatile String lastAdaptedBody = null;
		volatile boolean consoleOpen = false;
		volatile boolean sendingNativeGmHtml = false;
		volatile int commandDepth = 0;
		volatile int renderedCount = 0;
		volatile String pendingFileLabel = null;
		volatile String pendingCommandOrFile = null;
		final Deque<NavEntry> navStack = new ConcurrentLinkedDeque<>();
	}
	
	private final Map<Integer, AdminBoardSession> _sessions = new ConcurrentHashMap<>();
	
	private AdminBoardManager()
	{
	}
	
	/**
	 * Strictly verifies that the player is an authenticated staff member with RBAC permissions.
	 * Regular players (accessLevel <= 0) NEVER see or access any part of the Admin Console.
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
	 * Marks the end of an admin command execution. Whenever a command is triggered via bypass while the
	 * Admin Console is open and did not already render a new page, we refresh the current view so live
	 * header toggles and target status update immediately.
	 * @param player the GM executing the command
	 * @param fullCommand the executed command
	 * @param fromBypass true if clicked from HTML bypass
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
		if (fromBypass && session.consoleOpen && (session.renderedCount == 0))
		{
			refreshCurrentView(player);
		}
	}
	
	/**
	 * Re-sends the active Admin Console view (either the current adapted sub-page or the active native tab)
	 * so that the live header and Target Inspector update in GMWnd.HtmlViewer without altering navigation history.
	 * @param player the staff member
	 */
	public void refreshCurrentView(Player player)
	{
		if (!canUseAdminBoard(player))
		{
			return;
		}
		
		final AdminBoardSession session = getSession(player);
		if (!session.consoleOpen)
		{
			return;
		}
		
		final String adapted = session.lastAdaptedBody;
		if ((adapted != null) && !adapted.isEmpty())
		{
			session.renderedCount++;
			sendWrappedConsoleHtml(player, adapted, session.activeTab);
		}
		else
		{
			openTab(player, session.activeTab);
		}
	}
	
	/**
	 * Handles native Admin Console navigation commands (//admin, //admin1..7, //gmw [subcmd]).
	 * All navigation renders strictly inside Native C++ GMWnd.HtmlViewer.
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
		
		final String trimmed = command.trim();
		final int spaceIdx = trimmed.indexOf(' ');
		final String base = (spaceIdx > 0 ? trimmed.substring(0, spaceIdx) : trimmed).toLowerCase(Locale.ROOT);
		final String args = spaceIdx > 0 ? trimmed.substring(spaceIdx + 1).trim() : "";
		
		return switch (base)
		{
			case "admin_gmw" ->
			{
				handleGmwSubCommand(player, args);
				yield true;
			}
			case "admin_admin", "admin_admin1" ->
			{
				openTab(player, "dashboard");
				yield true;
			}
			case "admin_admin6" ->
			{
				openTab(player, "support");
				yield true;
			}
			case "admin_admin2" ->
			{
				openTab(player, "world");
				yield true;
			}
			case "admin_admin3", "admin_admin5", "admin_admin7" ->
			{
				openTab(player, "events");
				yield true;
			}
			case "admin_admin4" ->
			{
				openTab(player, "server");
				yield true;
			}
			default -> false;
		};
	}
	
	private void handleGmwSubCommand(Player player, String args)
	{
		if (args.isEmpty())
		{
			openTab(player, "dashboard");
			return;
		}
		
		final String[] tokens = args.split("\\s+");
		final String sub = tokens[0].toLowerCase(Locale.ROOT);
		switch (sub)
		{
			case "home", "dashboard" -> openTab(player, "dashboard");
			case "back" -> navigateBack(player);
			case "crumb" ->
			{
				int idx = 0;
				if (tokens.length > 1)
				{
					try
					{
						idx = Integer.parseInt(tokens[1]);
					}
					catch (NumberFormatException ignored)
					{
						idx = 0;
					}
				}
				navigateToCrumb(player, idx);
			}
			case "refresh" ->
			{
				final AdminBoardSession session = getSession(player);
				session.consoleOpen = true;
				refreshCurrentView(player);
			}
			case "player", "players", "support" ->
			{
				if (tokens.length > 1)
				{
					final Player targetPlayer = World.getPlayer(tokens[1]);
					if (targetPlayer != null)
					{
						player.setTarget(targetPlayer);
					}
				}
				openTab(player, "support");
			}
			case "world", "spawn", "spawns" -> openTab(player, "world");
			case "instances", "instance", "dungeons", "bosses" -> openTab(player, "instances");
			case "events", "event" -> openTab(player, "events");
			case "economy", "items", "char" -> openTab(player, "economy");
			case "server", "reload" -> openTab(player, "server");
			case "legacy" -> openTab(player, "legacy");
			case "legacy_view" ->
			{
				final String viewType = tokens.length > 1 ? tokens[1].toLowerCase(Locale.ROOT) : "status";
				final String targetName = tokens.length > 2 ? tokens[2] : null;
				handleLegacyViewCommand(player, viewType, targetName);
			}
			default -> openTab(player, "dashboard");
		}
	}
	
	/**
	 * Pops 1 level from the per-GM navigation history stack and renders the parent view.
	 */
	private void navigateBack(Player player)
	{
		if (!canUseAdminBoard(player))
		{
			return;
		}
		
		final AdminBoardSession session = getSession(player);
		if (session.navStack.size() > 1)
		{
			session.navStack.pollLast();
			final NavEntry parent = session.navStack.peekLast();
			if (parent != null)
			{
				renderNavEntry(player, parent);
				return;
			}
		}
		
		openTab(player, session.activeTab);
	}
	
	/**
	 * Jumps directly to an ancestor crumb at {@code targetIndex} in the per-GM navigation stack.
	 */
	private void navigateToCrumb(Player player, int targetIndex)
	{
		if (!canUseAdminBoard(player))
		{
			return;
		}
		
		final AdminBoardSession session = getSession(player);
		if ((targetIndex <= 0) || session.navStack.isEmpty())
		{
			openTab(player, session.activeTab);
			return;
		}
		
		while ((session.navStack.size() > (targetIndex + 1)) && (session.navStack.size() > 1))
		{
			session.navStack.pollLast();
		}
		
		final NavEntry target = session.navStack.peekLast();
		if (target != null)
		{
			renderNavEntry(player, target);
		}
		else
		{
			openTab(player, session.activeTab);
		}
	}
	
	private void renderNavEntry(Player player, NavEntry entry)
	{
		final AdminBoardSession session = getSession(player);
		if ((entry == null) || entry.commandOrFile().startsWith("TAB:"))
		{
			openTab(player, entry != null ? entry.tab() : session.activeTab);
			return;
		}
		
		session.activeTab = entry.tab();
		session.consoleOpen = true;
		session.renderedCount++;
		
		String raw = entry.rawHtml();
		if (entry.commandOrFile().startsWith("FILE:"))
		{
			final String relFile = entry.commandOrFile().substring(5);
			final String cached = HtmCache.getInstance().getHtm(player, "data/html/admin/" + relFile);
			if (cached != null)
			{
				raw = cached;
			}
		}
		
		if ((raw == null) || raw.isEmpty())
		{
			openTab(player, session.activeTab);
			return;
		}
		
		final String adaptedBody = scrubAndAdaptHtml(player, raw);
		session.lastAdaptedBody = adaptedBody;
		sendWrappedConsoleHtml(player, adaptedBody, session.activeTab);
	}
	
	private static String getTabDisplayName(String tab)
	{
		if (tab == null)
		{
			return "Dashboard";
		}
		return switch (tab.toLowerCase(Locale.ROOT))
		{
			case "support" -> "Players & Support";
			case "world" -> "World & Spawns";
			case "instances" -> "Instances & Bosses";
			case "events" -> "Events & GM";
			case "economy" -> "Items & Char";
			case "server" -> "Server & Reload";
			case "legacy" -> "Legacy Tools";
			default -> "Dashboard";
		};
	}
	
	/**
	 * Opens one of the 6 native C++ client inspection windows (GMDetailStatusWnd, GMInventoryWnd,
	 * GMMagicSkillWnd, GMQuestWnd, GMWarehouseWnd, GMClanWnd) by sending the corresponding GMView* packet.
	 */
	private void handleLegacyViewCommand(Player gm, String viewType, String targetName)
	{
		if (!gm.getAccessLevel().hasScope(AdminScope.PLAYER_SUPPORT))
		{
			gm.sendMessage("You do not have the required RBAC role permission (" + AdminScope.PLAYER_SUPPORT.getDisplayName() + ") for this inspector!");
			return;
		}
		
		Player targetPlayer = null;
		Clan targetClan = null;
		if ((targetName != null) && !targetName.isEmpty())
		{
			targetPlayer = World.getPlayer(targetName);
			targetClan = ClanTable.getInstance().getClanByName(targetName);
		}
		else
		{
			final WorldObject obj = gm.getTarget();
			if ((obj != null) && obj.isPlayer())
			{
				targetPlayer = obj.asPlayer();
			}
			else
			{
				targetPlayer = gm;
			}
		}
		
		if ((targetPlayer == null) && ((targetClan == null) || !"warehouse".equals(viewType)))
		{
			gm.sendMessage("Target player not found online.");
			return;
		}
		
		switch (viewType)
		{
			case "status" ->
			{
				gm.sendPacket(new GMViewCharacterInfo(targetPlayer));
				gm.sendPacket(new GMHennaInfo(targetPlayer));
			}
			case "items", "inventory" ->
			{
				gm.sendPacket(new GMViewItemList(targetPlayer));
				gm.sendPacket(new GMHennaInfo(targetPlayer));
			}
			case "skills" -> gm.sendPacket(new GMViewSkillInfo(targetPlayer));
			case "quests" -> gm.sendPacket(new GmViewQuestInfo(targetPlayer));
			case "warehouse" ->
			{
				if (targetPlayer != null)
				{
					gm.sendPacket(new GMViewWarehouseWithdrawList(targetPlayer));
				}
				else if (targetClan != null)
				{
					gm.sendPacket(new GMViewWarehouseWithdrawList(targetClan));
				}
			}
			case "clan" ->
			{
				if ((targetPlayer != null) && (targetPlayer.getClan() != null))
				{
					gm.sendPacket(new GMViewPledgeInfo(targetPlayer.getClan(), targetPlayer));
				}
				else
				{
					gm.sendMessage("Target player does not belong to a clan.");
				}
			}
		}
		
		refreshCurrentView(gm);
	}
	
	/**
	 * Opens one of the native wide-screen RBAC tabs in GMWnd.HtmlViewer and resets the breadcrumb stack to that tab root.
	 * @param player the staff member
	 * @param tab the tab identifier
	 */
	public void openTab(Player player, String tab)
	{
		if (!canUseAdminBoard(player))
		{
			return;
		}
		
		final AccessLevel access = player.getAccessLevel();
		final AdminBoardSession session = getSession(player);
		session.activeTab = tab;
		session.lastAdaptedBody = null;
		session.consoleOpen = true;
		session.renderedCount++;
		
		final String workspaceHtml = switch (tab)
		{
			case "support" -> (access.hasScope(AdminScope.PLAYER_SUPPORT) || access.hasScope(AdminScope.CHAT_MODERATION) || access.hasScope(AdminScope.PUNISHMENT))
				? buildSupportWorkspace(player)
				: buildDashboardWorkspace(player);
			case "world" -> (access.hasScope(AdminScope.TELEPORT) || access.hasScope(AdminScope.WORLD_SPAWN) || access.hasScope(AdminScope.CLAN_MANAGEMENT))
				? buildWorldWorkspace(player)
				: buildDashboardWorkspace(player);
			case "instances" -> (access.hasScope(AdminScope.WORLD_SPAWN) || access.hasScope(AdminScope.TELEPORT) || access.hasScope(AdminScope.PLAYER_SUPPORT))
				? buildInstancesWorkspace(player)
				: buildDashboardWorkspace(player);
			case "events" -> (access.hasScope(AdminScope.EVENT_GM) || access.hasScope(AdminScope.ANNOUNCEMENTS) || access.hasScope(AdminScope.PANEL_ACCESS))
				? buildEventsWorkspace(player)
				: buildDashboardWorkspace(player);
			case "economy" -> (access.hasScope(AdminScope.ECONOMY_ITEMS) || access.hasScope(AdminScope.CHAR_EDIT))
				? buildEconomyWorkspace(player)
				: buildDashboardWorkspace(player);
			case "server" -> (access.hasScope(AdminScope.SERVER_ADMIN) || access.hasScope(AdminScope.MASTER_CONTROL))
				? buildServerWorkspace(player)
				: buildDashboardWorkspace(player);
			case "legacy" -> buildLegacyWorkspace(player);
			default ->
			{
				session.activeTab = "dashboard";
				yield buildDashboardWorkspace(player);
			}
		};
		
		session.navStack.clear();
		session.navStack.addLast(new NavEntry(session.activeTab, "TAB:" + session.activeTab, null, getTabDisplayName(session.activeTab)));
		
		sendWrappedConsoleHtml(player, workspaceHtml, session.activeTab);
	}
	
	/**
	 * Renders a legacy data/html/admin/ file inside the wide-screen RBAC Console shell and pushes it onto the breadcrumb stack.
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
		
		final String normalizedFile = (fileName != null) ? fileName.trim().replace('\\', '/') : "";
		final String html = HtmCache.getInstance().getHtm(player, "data/html/admin/" + normalizedFile);
		if (html == null)
		{
			sendAdminHtml(player, "<html><body><center><br><font color=\"FF6666\">Admin HTML page not found: data/html/admin/" + normalizedFile + "</font></center></body></html>");
			return;
		}
		
		final AdminBoardSession session = getSession(player);
		final int slashIdx = normalizedFile.lastIndexOf('/');
		session.pendingFileLabel = (slashIdx >= 0) ? normalizedFile.substring(slashIdx + 1) : normalizedFile;
		session.pendingCommandOrFile = "FILE:" + normalizedFile;
		try
		{
			sendAdminHtml(player, html);
		}
		finally
		{
			session.pendingFileLabel = null;
			session.pendingCommandOrFile = null;
		}
	}
	
	/**
	 * Intercepts an NpcHtmlMessage destined for a GM if it is sent during an active admin command
	 * execution (commandDepth > 0) and is not bound to a world NPC (npcObjId == 0), routing it to
	 * the RBAC-filtered Native C++ GMWnd.HtmlViewer.
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
		
		if (!player.getAccessLevel().allowAltG() || ((player.getClient() != null) && (player.getClient().getProtocolVersion() >= 152)))
		{
			return false;
		}
		
		final AdminBoardSession session = getSession(player);
		if (session.sendingNativeGmHtml || (session.commandDepth <= 0))
		{
			return false;
		}
		
		final String html = packet.getHtml();
		if ((html == null) || html.isEmpty())
		{
			return false;
		}
		
		sendAdminHtml(player, html);
		return true;
	}
	
	/**
	 * Scrubs unauthorized RBAC buttons/links from raw admin HTML, records the view in the per-GM
	 * navigation stack, and sends it inside the wide-screen 940px Native GMWnd.HtmlViewer shell.
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
		final String label = (session.pendingFileLabel != null) && !session.pendingFileLabel.isEmpty()
			? session.pendingFileLabel
			: extractHtmlLabel(rawHtml);
		final String cmdOrFile = (session.pendingCommandOrFile != null) && !session.pendingCommandOrFile.isEmpty()
			? session.pendingCommandOrFile
			: ("HTML:" + label);
		
		pushNavEntry(session, new NavEntry(session.activeTab, cmdOrFile, rawHtml, label));
		
		final String adaptedBody = scrubAndAdaptHtml(player, rawHtml);
		session.lastAdaptedBody = adaptedBody;
		session.consoleOpen = true;
		session.renderedCount++;
		
		sendWrappedConsoleHtml(player, adaptedBody, session.activeTab);
	}
	
	private void pushNavEntry(AdminBoardSession session, NavEntry entry)
	{
		if (session.navStack.isEmpty())
		{
			session.navStack.addLast(new NavEntry(session.activeTab, "TAB:" + session.activeTab, null, getTabDisplayName(session.activeTab)));
		}
		
		// If this exact file/view is already in the stack, unwind to it and update in-place to avoid circular loops.
		int existingIndex = -1;
		int idx = 0;
		for (NavEntry item : session.navStack)
		{
			if (item.commandOrFile().equalsIgnoreCase(entry.commandOrFile()) || ((idx > 0) && item.label().equalsIgnoreCase(entry.label())))
			{
				existingIndex = idx;
				break;
			}
			idx++;
		}
		
		if (existingIndex >= 0)
		{
			while (session.navStack.size() > existingIndex)
			{
				session.navStack.pollLast();
			}
		}
		else if (session.navStack.size() >= MAX_NAV_STACK_DEPTH)
		{
			// Preserve root tab entry at index 0, drop oldest intermediate entry.
			final NavEntry root = session.navStack.pollFirst();
			session.navStack.pollFirst();
			if (root != null)
			{
				session.navStack.addFirst(root);
			}
		}
		
		session.navStack.addLast(entry);
	}
	
	private static String extractHtmlLabel(String rawHtml)
	{
		if (rawHtml == null)
		{
			return "Sub-View";
		}
		final Matcher m = TITLE_TAG_PATTERN.matcher(rawHtml);
		if (m.find())
		{
			final String title = m.group(1).replaceAll("<[^>]+>", "").trim();
			if (!title.isEmpty())
			{
				return title.length() > 26 ? title.substring(0, 26) : title;
			}
		}
		return "Sub-View";
	}
	
	/**
	 * Wraps workspace inner HTML with the 940px flat top Command Deck, Breadcrumb Navigation Bar,
	 * and Live Target Inspector, populates the NPC_ITEM_HTML action cache exclusively, and transmits
	 * directly to the Native C++ GMWnd.HtmlViewer (itemId=1 NpcHtmlMessage).
	 */
	private void sendWrappedConsoleHtml(Player player, String workspaceBody, String activeTab)
	{
		if (!player.getAccessLevel().allowAltG() || ((player.getClient() != null) && (player.getClient().getProtocolVersion() >= 152)))
		{
			player.sendMessage("Admin console requires the server's client package; use // commands.");
			return;
		}
		
		final AdminBoardSession session = getSession(player);
		final StringBuilder sb = new StringBuilder(6144);
		sb.append("<html><body><center>");
		appendTopHeaderBar(sb, player, session, activeTab);
		appendTargetInspectorAndQuickBox(sb, player);
		sb.append("<img src=\"L2UI.SquareGray\" width=948 height=1><br1>");
		sb.append(workspaceBody);
		sb.append("<br></center></body></html>");
		
		final String finalHtml = sb.toString();
		player.clearHtmlActions(HtmlActionScope.NPC_ITEM_HTML);
		HtmlUtil.buildHtmlActionCache(player, HtmlActionScope.NPC_ITEM_HTML, 0, finalHtml);
		
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
	
	/**
	 * Builds Top Bar 1 & 2 (948px):
	 * - Row 1: Role Identity + Self GM State Toggles + 1-Click [Reload Changed] + [Home] + [Refresh].
	 * - Row 2: [ ^ Back 1 Level ] + Live Clickable Breadcrumb Trail + Dirty Script Status.
	 * Strictly flat 2009 Epilogue <table> tags (zero nested tables).
	 */
	private void appendTopHeaderBar(StringBuilder sb, Player gm, AdminBoardSession session, String activeTab)
	{
		final AccessLevel access = gm.getAccessLevel();
		final String invisLabel = gm.isInvisible() ? "Invis:ON" : "Invis:OFF";
		final String invulLabel = gm.isInvul() ? "Invul:ON" : "Invul:OFF";
		final String silenceLabel = gm.isSilenceMode() ? "MutePM:ON" : "MutePM:OFF";
		final int modifiedScripts = access.hasScope(AdminScope.SERVER_ADMIN) ? ScriptEngine.getInstance().getModifiedScriptCount() : 0;
		final String reloadChangedLabel = modifiedScripts > 0 ? ("Reload Changed (" + modifiedScripts + ")") : "Reload Changed";
		
		// Row 1 (948px): Role Identity + Self GM State Toggles + 1-Click Delta Reload + Home / Refresh controls
		sb.append("<table width=948 border=0 cellpadding=1 cellspacing=0 bgcolor=\"1B1B1B\"><tr>");
		sb.append("<td width=234 align=left><font color=\"LEVEL\">[RBAC: ").append(escapeHtml(access.getName())).append(" Lv.").append(access.getLevel()).append("]</font></td>");
		sb.append("<td width=82 align=center>").append(AdminData.getInstance().hasAccess("admin_invis", access) ? button(invisLabel, "admin_invis", 78, 20) : "").append("</td>");
		sb.append("<td width=82 align=center>").append(AdminData.getInstance().hasAccess("admin_invul", access) ? button(invulLabel, "admin_invul", 78, 20) : "").append("</td>");
		sb.append("<td width=86 align=center>").append(AdminData.getInstance().hasAccess("admin_silence", access) ? button(silenceLabel, "admin_silence", 82, 20) : "").append("</td>");
		if (AdminData.getInstance().hasAccess("admin_gmspeed", access))
		{
			sb.append("<td width=60 align=center>").append(button("Spd 1x", "admin_gmspeed 0", 56, 20)).append("</td>");
			sb.append("<td width=60 align=center>").append(button("Spd 2x", "admin_gmspeed 2", 56, 20)).append("</td>");
			sb.append("<td width=60 align=center>").append(button("Spd 4x", "admin_gmspeed 4", 56, 20)).append("</td>");
		}
		else
		{
			sb.append("<td width=60></td><td width=60></td><td width=60></td>");
		}
		sb.append("<td width=132 align=center>").append(access.hasScope(AdminScope.SERVER_ADMIN) ? button(reloadChangedLabel, "admin_reload changed", 128, 20) : "").append("</td>");
		sb.append("<td width=76 align=center>").append(button("Home", "admin_gmw home", 72, 20)).append("</td>");
		sb.append("<td width=76 align=right>").append(button("Refresh", "admin_gmw refresh", 72, 20)).append("</td>");
		sb.append("</tr></table>");
		
		// Row 2 (948px): [ ^ Back 1 Level ] + Clickable Breadcrumb Trail + Script Dirty Indicator
		final List<NavEntry> crumbs = new ArrayList<>(session.navStack);
		final boolean canGoBack = crumbs.size() > 1;
		sb.append("<table width=948 border=0 cellpadding=1 cellspacing=0 bgcolor=\"252530\"><tr>");
		sb.append("<td width=124 align=left>").append(button(canGoBack ? "^ Back 1 Level" : "^ Tab Root", "admin_gmw back", 118, 20)).append("</td>");
		sb.append("<td width=664 align=left><font color=\"999999\">Path:</font> ");
		if (crumbs.isEmpty())
		{
			sb.append("<font color=\"LEVEL\">").append(escapeHtml(getTabDisplayName(activeTab))).append("</font>");
		}
		else
		{
			for (int i = 0; i < crumbs.size(); i++)
			{
				if (i > 0)
				{
					sb.append(" <font color=\"777777\">&gt;</font> ");
				}
				final NavEntry c = crumbs.get(i);
				if (i < (crumbs.size() - 1))
				{
					sb.append("<a action=\"bypass admin_gmw crumb ").append(i).append("\">").append(escapeHtml(c.label())).append("</a>");
				}
				else
				{
					sb.append("<font color=\"LEVEL\">").append(escapeHtml(c.label())).append("</font>");
				}
			}
		}
		sb.append("</td>");
		sb.append("<td width=160 align=right>");
		if (access.hasScope(AdminScope.SERVER_ADMIN))
		{
			if (modifiedScripts > 0)
			{
				sb.append("<font color=\"FFCC00\">Dirty Scripts: ").append(modifiedScripts).append("</font>");
			}
			else
			{
				sb.append("<font color=\"00FF00\">Scripts: Synced</font>");
			}
		}
		else
		{
			sb.append("<font color=\"888888\">Tab: ").append(escapeHtml(getTabDisplayName(activeTab))).append("</font>");
		}
		sb.append("</td></tr></table>");
	}
	
	/**
	 * Builds Top Bar 3 & 4 (948px): Live Target Inspector + Unified QuickBox with RBAC-filtered actions.
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
		
		sb.append("<table width=948 border=0 cellpadding=1 cellspacing=0 bgcolor=\"141E28\"><tr>");
		sb.append("<td width=478 align=left><font color=\"00CCFF\">Target:</font> <font color=\"FFFFFF\">")
			.append(escapeHtml(target.getName()))
			.append("</font> <font color=\"AAAAAA\">(").append(targetType).append(")</font> ")
			.append("<font color=\"00FF00\">HP ").append(curHp).append("/").append(maxHp).append("</font> ")
			.append("<font color=\"66AAFF\">MP ").append(curMp).append("/").append(maxMp).append("</font> ")
			.append("<font color=\"888888\">[").append(target.getX()).append(",").append(target.getY()).append(",").append(target.getZ()).append("]</font></td>");
		
		if (access.hasScope(AdminScope.PLAYER_SUPPORT))
		{
			sb.append("<td width=68 align=center>").append(button("Inspect", "admin_current_player", 64, 20)).append("</td>");
			sb.append("<td width=58 align=center>").append(button("Heal", "admin_heal", 54, 20)).append("</td>");
			sb.append("<td width=56 align=center>").append(button("Res", "admin_res", 52, 20)).append("</td>");
			sb.append("<td width=58 align=center>").append(AdminData.getInstance().hasAccess("admin_kill", access) ? button("Kill", "admin_kill", 54, 20) : "").append("</td>");
			if (access.hasScope(AdminScope.TELEPORT))
			{
				sb.append("<td width=68 align=center>").append(button("Goto", "admin_teleportto " + target.getName(), 64, 20)).append("</td>");
				sb.append("<td width=72 align=center>").append(button("Recall", "admin_recall " + target.getName(), 68, 20)).append("</td>");
			}
			else
			{
				sb.append("<td width=68></td><td width=72></td>");
			}
			if (access.hasScope(AdminScope.WORLD_SPAWN) && target.isNpc())
			{
				sb.append("<td width=90 align=right>").append(button("Delete NPC", "admin_delete", 86, 20)).append("</td>");
			}
			else if (access.hasScope(AdminScope.PUNISHMENT) && target.isPlayer())
			{
				sb.append("<td width=90 align=right>").append(button("Punish", "admin_punishment info " + target.getName() + " CHARACTER", 86, 20)).append("</td>");
			}
			else
			{
				sb.append("<td width=90></td>");
			}
		}
		else
		{
			sb.append("<td width=470 align=right><font color=\"AAAAAA\">Instance: ").append(target.getInstanceId()).append("</font></td>");
		}
		sb.append("</tr></table>");
		
		// Unified QuickBox Row (948px, RBAC filtered, flat 1-level table)
		sb.append("<table width=948 border=0 cellpadding=1 cellspacing=0 bgcolor=\"202020\"><tr>");
		sb.append("<td width=72 align=left><font color=\"LEVEL\">QuickBox:</font></td>");
		sb.append("<td width=206 align=left><edit var=\"qbox\" width=198 height=14></td>");
		sb.append("<td width=92 align=center>").append(access.hasScope(AdminScope.PLAYER_SUPPORT) ? button("Find Char", "admin_find_character $qbox", 88, 20) : "").append("</td>");
		sb.append("<td width=84 align=center>").append(access.hasScope(AdminScope.TELEPORT) ? button("MoveTo", "admin_move_to $qbox", 80, 20) : "").append("</td>");
		sb.append("<td width=84 align=center>").append(access.hasScope(AdminScope.WORLD_SPAWN) ? button("Spawn", "admin_spawn_monster $qbox", 80, 20) : "").append("</td>");
		sb.append("<td width=74 align=center>").append(access.hasScope(AdminScope.WORLD_SPAWN) ? button("Scan", "admin_scan $qbox", 70, 20) : "").append("</td>");
		sb.append("<td width=98 align=center>").append(access.hasScope(AdminScope.ECONOMY_ITEMS) ? button("Create Item", "admin_create_item $qbox", 94, 20) : "").append("</td>");
		sb.append("<td width=78 align=center>").append(access.hasScope(AdminScope.ECONOMY_ITEMS) ? button("Search", "admin_search $qbox", 74, 20) : "").append("</td>");
		sb.append("<td width=80 align=center>").append(access.hasScope(AdminScope.ANNOUNCEMENTS) ? button("Announce", "admin_announce $qbox", 76, 20) : "").append("</td>");
		sb.append("<td width=80 align=right>").append(access.hasScope(AdminScope.SERVER_ADMIN) ? button("Reload Scr", "admin_reload script $qbox", 76, 20) : "").append("</td>");
		sb.append("</tr></table>");
	}
	
	/**
	 * Appends a single flat 948px workspace row with either 4, 5, or 6 action columns.
	 * Each row is its own 1-row <table> so NWindow.dll column widths align with mathematical precision:
	 * - 4 columns: 148 (label) + 4 * 200 = 948px (buttons 192x22)
	 * - 5 columns: 148 (label) + 5 * 160 = 948px (buttons 152x22)
	 * - 6 columns: 144 (label) + 6 * 134 = 948px (buttons 128x22)
	 */
	private static void appendWorkspaceRow(StringBuilder sb, String categoryLabel, String color, String[]... labelAndCmdPairs)
	{
		final int count = labelAndCmdPairs.length;
		final int cols = (count <= 4) ? 4 : (count == 5 ? 5 : 6);
		final int labelWidth = (cols == 4) ? 148 : (cols == 5 ? 148 : 144);
		final int cellWidth = (cols == 4) ? 200 : (cols == 5 ? 160 : 134);
		final int btnWidth = (cols == 4) ? 192 : (cols == 5 ? 152 : 128);
		
		sb.append("<table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"1B1B1B\"><tr>");
		sb.append("<td width=").append(labelWidth).append("><font color=\"").append(color).append("\">").append(categoryLabel).append("</font></td>");
		for (int i = 0; i < cols; i++)
		{
			sb.append("<td width=").append(cellWidth).append(" align=center>");
			if (i < count)
			{
				final String[] pair = labelAndCmdPairs[i];
				if ((pair != null) && (pair.length >= 2) && (pair[0] != null) && !pair[0].isEmpty() && (pair[1] != null) && !pair[1].isEmpty())
				{
					sb.append(button(pair[0], pair[1], btnWidth, 22));
				}
			}
			sb.append("</td>");
		}
		sb.append("</tr></table>");
	}
	
	/**
	 * Tab 1: Main RBAC Overview, Telemetry & Pinned Quick Launch Dashboard (948px).
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
		final int trackedScripts = ScriptEngine.getInstance().getTrackedScriptCount();
		final int dirtyScripts = access.hasScope(AdminScope.SERVER_ADMIN) ? ScriptEngine.getInstance().getModifiedScriptCount() : 0;
		final int activeInstances = Math.max(0, InstanceManager.getInstance().getInstances().size() - 1);
		
		final StringBuilder sb = new StringBuilder(3072);
		sb.append("<font color=\"LEVEL\">Admin Control Center v3 - Live Server Telemetry &amp; Role Overview</font><br1>");
		sb.append("<table width=948 border=0 cellpadding=3 cellspacing=1 bgcolor=\"1E1E26\"><tr>");
		sb.append("<td width=180><font color=\"AAAAAA\">Players:</font> <font color=\"00FF00\">").append(onlineCount).append(" / ").append(ServerConfig.MAXIMUM_ONLINE_USERS).append("</font></td>");
		sb.append("<td width=140><font color=\"AAAAAA\">Online GMs:</font> <font color=\"00CCFF\">").append(gmCount).append("</font></td>");
		sb.append("<td width=190><font color=\"AAAAAA\">JVM Heap:</font> <font color=\"LEVEL\">").append(usedMb).append(" / ").append(maxMb).append(" MB</font></td>");
		sb.append("<td width=220><font color=\"AAAAAA\">Uptime / Scripts:</font> <font color=\"FFFFFF\">").append(uptimeMin).append("m / ").append(trackedScripts > 0 ? trackedScripts : loadedScripts).append("</font></td>");
		sb.append("<td width=218><font color=\"AAAAAA\">Modified Scripts:</font> ").append(dirtyScripts > 0 ? ("<font color=\"FFCC00\">" + dirtyScripts + " Pending Reload</font>") : "<font color=\"00FF00\">0 (All Synced)</font>").append("</td>");
		sb.append("</tr></table><br1>");
		
		// Role Capabilities Summary
		sb.append("<table width=948 border=0 cellpadding=3 cellspacing=0 bgcolor=\"25252E\"><tr>");
		sb.append("<td width=148><font color=\"LEVEL\">Granted Scopes:</font></td><td width=620><font color=\"00FF00\">");
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
		sb.append("</font></td><td width=180 align=right><font color=\"00CCFF\">Active Instances: ").append(activeInstances).append("</font></td></tr></table><br>");
		
		sb.append("<font color=\"CDB67F\">Pinned Dev &amp; Admin Quick Actions (Filtered by Role)</font><br1>");
		
		if (access.hasScope(AdminScope.SERVER_ADMIN))
		{
			appendWorkspaceRow(sb, "Hot-Reload & Dev", "00FF00",
				new String[]{"Reload Changed Scripts", "admin_reload changed"},
				new String[]{"Reload Script ($qbox)", "admin_reload script $qbox"},
				new String[]{"Script Inspector", "admin_show_quests"},
				new String[]{"Reload HTML Cache", "admin_reload htm"},
				new String[]{"Server Diagnostics", "admin_serverinfo"});
		}
		
		if (access.hasScope(AdminScope.PLAYER_SUPPORT) || access.hasScope(AdminScope.CHAT_MODERATION))
		{
			appendWorkspaceRow(sb, "Players & Support", "00CCFF",
				new String[]{"Online Players List", "admin_show_characters 0"},
				new String[]{"Inspect Target", "admin_current_player"},
				new String[]{"View Petitions", "admin_view_petitions"},
				new String[]{"Target Lockouts", "admin_instancezone"},
				access.hasScope(AdminScope.PUNISHMENT) ? new String[]{"Punishment Center", "admin_punishment"} : new String[]{"", ""});
		}
		
		if (access.hasScope(AdminScope.TELEPORT) || access.hasScope(AdminScope.WORLD_SPAWN))
		{
			appendWorkspaceRow(sb, "World & Dungeons", "LEVEL",
				access.hasScope(AdminScope.TELEPORT) ? new String[]{"World Teleport Dir", "admin_show_moves"} : new String[]{"", ""},
				access.hasScope(AdminScope.WORLD_SPAWN) ? new String[]{"Active Instances", "admin_listinstances"} : new String[]{"", ""},
				access.hasScope(AdminScope.WORLD_SPAWN) ? new String[]{"GrandBoss Manager", "admin_grandboss"} : new String[]{"", ""},
				access.hasScope(AdminScope.WORLD_SPAWN) ? new String[]{"Gracia Seeds Status", "admin_gracia_seeds"} : new String[]{"", ""},
				access.hasScope(AdminScope.WORLD_SPAWN) ? new String[]{"Hellbound Status", "admin_hellbound"} : new String[]{"", ""});
		}
		
		if (access.hasScope(AdminScope.ECONOMY_ITEMS) || access.hasScope(AdminScope.CHAR_EDIT))
		{
			appendWorkspaceRow(sb, "Items & Char Edit", "FF9900",
				access.hasScope(AdminScope.ECONOMY_ITEMS) ? new String[]{"GM Shop", "admin_gmshop"} : new String[]{"", ""},
				access.hasScope(AdminScope.ECONOMY_ITEMS) ? new String[]{"Item Creator", "admin_itemcreate"} : new String[]{"", ""},
				access.hasScope(AdminScope.ECONOMY_ITEMS) ? new String[]{"Enchant Gear", "admin_enchant"} : new String[]{"", ""},
				access.hasScope(AdminScope.CHAR_EDIT) ? new String[]{"Edit Target Char", "admin_edit_character"} : new String[]{"", ""},
				access.hasScope(AdminScope.CHAR_EDIT) ? new String[]{"Manage Target Skills", "admin_skill_list"} : new String[]{"", ""});
		}
		
		return sb.toString();
	}
	
	/**
	 * Tab 2: Player Support, Native C++ Client Inspector Popups, Petitions & Punishments Workspace (948px).
	 */
	private String buildSupportWorkspace(Player gm)
	{
		final AccessLevel access = gm.getAccessLevel();
		final StringBuilder sb = new StringBuilder(3072);
		sb.append("<font color=\"LEVEL\">Players, Native C++ Inspector Popups, Petitions &amp; Punishments</font><br1>");
		
		if (access.hasScope(AdminScope.PLAYER_SUPPORT))
		{
			appendWorkspaceRow(sb, "Character Lookup", "00FF00",
				new String[]{"Online Players List", "admin_show_characters 0"},
				new String[]{"Inspect Target", "admin_current_player"},
				new String[]{"Find Char ($qbox)", "admin_find_character $qbox"},
				new String[]{"Find Dualbox", "admin_find_dualbox 2"},
				new String[]{"Find by IP ($qbox)", "admin_find_ip $qbox"});
			
			appendWorkspaceRow(sb, "Target Assistance", "00FF00",
				new String[]{"Clear Skill Reuse", "admin_removereuse"},
				new String[]{"Active Buffs", "admin_getbuffs"},
				new String[]{"Character Quests", "admin_charquestmenu"},
				new String[]{"Disconnect Target", "admin_character_disconnect"},
				new String[]{"Repair Char ($qbox)", "admin_repair $qbox"});
			
			appendWorkspaceRow(sb, "Native C++ Popups", "00CCFF",
				new String[]{"Client Status Wnd", "admin_gmw legacy_view status"},
				new String[]{"Client Inventory Wnd", "admin_gmw legacy_view items"},
				new String[]{"Client Skills Wnd", "admin_gmw legacy_view skills"},
				new String[]{"Client Quests Wnd", "admin_gmw legacy_view quests"},
				new String[]{"Client Warehouse Wnd", "admin_gmw legacy_view warehouse"},
				new String[]{"Client Clan Wnd", "admin_gmw legacy_view clan"});
		}
		
		if (access.hasScope(AdminScope.CHAT_MODERATION))
		{
			appendWorkspaceRow(sb, "Petitions & Chat", "LEVEL",
				new String[]{"View Petitions", "admin_view_petitions"},
				new String[]{"Force Petition", "admin_force_peti Thank you"},
				new String[]{"Reset Petitions", "admin_reset_petitions"},
				new String[]{"GM Chat Panel", "admin_gmchat_menu"},
				new String[]{"Chat Ban ($qbox)", "admin_banchat $qbox"});
		}
		
		if (access.hasScope(AdminScope.PUNISHMENT))
		{
			appendWorkspaceRow(sb, "Punishments", "FF6666",
				new String[]{"Punishment Center", "admin_punishment"},
				new String[]{"Jail ($qbox)", "admin_jail $qbox"},
				new String[]{"Unjail ($qbox)", "admin_unjail $qbox"},
				new String[]{"Kick ($qbox)", "admin_kick $qbox"});
			
			appendWorkspaceRow(sb, "Account / Char Ban", "FF6666",
				new String[]{"Ban Char ($qbox)", "admin_ban_char $qbox"},
				new String[]{"Unban Char ($qbox)", "admin_unban_char $qbox"},
				new String[]{"Ban Acc ($qbox)", "admin_ban_acc $qbox"},
				new String[]{"Unban Acc ($qbox)", "admin_unban_acc $qbox"});
		}
		
		return sb.toString();
	}
	
	/**
	 * Tab 3: World Navigation, Spawns, Sieges, Zones/Geodata & Clans Workspace (948px).
	 */
	private String buildWorldWorkspace(Player gm)
	{
		final AccessLevel access = gm.getAccessLevel();
		final StringBuilder sb = new StringBuilder(3072);
		sb.append("<font color=\"LEVEL\">World Navigation, Spawns, Sieges, Geodata &amp; Clans</font><br1>");
		
		if (access.hasScope(AdminScope.TELEPORT))
		{
			appendWorkspaceRow(sb, "Teleportation", "00CCFF",
				new String[]{"World Teleport Dir", "admin_show_moves"},
				new String[]{"XYZ Coordinate Pad", "admin_tele"},
				new String[]{"GM Bookmarks", "admin_bk"},
				new String[]{"Recall Party", "admin_recall_party"},
				new String[]{"Recall Clan", "admin_recall_clan"});
		}
		
		if (access.hasScope(AdminScope.WORLD_SPAWN))
		{
			appendWorkspaceRow(sb, "NPC & Spawns", "00FF00",
				new String[]{"Spawn by Level", "admin_spawn_index"},
				new String[]{"Spawn by Name A-Z", "admin_npc_index"},
				new String[]{"Scan Nearby (500)", "admin_scan 500"},
				new String[]{"Delete Target NPC", "admin_delete"},
				new String[]{"Reload Spawns", "admin_reload spawn"});
			
			appendWorkspaceRow(sb, "Sieges & Territory", "FF9900",
				new String[]{"Castle Sieges", "admin_siege"},
				new String[]{"Fortress Sieges", "admin_fortsiege"},
				new String[]{"ClanHall Sieges", "admin_chsiege_siegablehall"},
				new String[]{"Territory War", "admin_territory_war"});
			
			appendWorkspaceRow(sb, "Zones & Geodata", "LEVEL",
				new String[]{"Zone Check", "admin_zone_check"},
				new String[]{"Zone Builder", "admin_zone_build"},
				new String[]{"Geodata Inspector", "admin_html geo_menu.htm"},
				new String[]{"Manor Status", "admin_manor"},
				new String[]{"Find Mammon", "admin_mammon_find"});
		}
		
		if (access.hasScope(AdminScope.CLAN_MANAGEMENT))
		{
			appendWorkspaceRow(sb, "Clan Management", "00CCFF",
				new String[]{"Target Clan Info", "admin_clan_info"},
				new String[]{"Pending Leaders", "admin_clan_show_pending"},
				new String[]{"Pledge Panel", "admin_html pledge.htm"},
				new String[]{"Cursed Weapons", "admin_cw_info_menu"});
		}
		
		return sb.toString();
	}
	
	/**
	 * Tab 4: Dedicated Instances, Dungeons, GrandBosses & Endgame Progression Workspace (948px).
	 */
	private String buildInstancesWorkspace(Player gm)
	{
		final AccessLevel access = gm.getAccessLevel();
		final StringBuilder sb = new StringBuilder(4096);
		sb.append("<font color=\"LEVEL\">Instances, Endgame Progression, GrandBosses &amp; Dungeon Teleports</font><br1>");
		
		if (access.hasScope(AdminScope.WORLD_SPAWN) || access.hasScope(AdminScope.PLAYER_SUPPORT))
		{
			appendWorkspaceRow(sb, "Instance Control", "00FF00",
				new String[]{"Active Instances", "admin_listinstances"},
				new String[]{"Target Lockouts", "admin_instancezone"},
				new String[]{"Create ($qbox)", "admin_createinstance $qbox"},
				new String[]{"Destroy ($qbox)", "admin_destroyinstance $qbox"},
				new String[]{"Reload Instances XML", "admin_reload instance"});
			
			appendWorkspaceRow(sb, "Instance Utilities", "00CCFF",
				new String[]{"Set Instance ($qbox)", "admin_setinstance $qbox"},
				new String[]{"Return to World (0)", "admin_setinstance 0"},
				new String[]{"Ghost Mode ON", "admin_ghoston"},
				new String[]{"Ghost Mode OFF", "admin_ghostoff"},
				new String[]{"Template IDs", "admin_instancezones"});
			
			appendWorkspaceRow(sb, "Bosses & Endgame", "LEVEL",
				new String[]{"GrandBoss Manager", "admin_grandboss"},
				new String[]{"Gracia Seeds Status", "admin_gracia_seeds"},
				new String[]{"Hellbound Status", "admin_hellbound"},
				new String[]{"Olympiad Status", "admin_checkolympiad"});
		}
		
		if (access.hasScope(AdminScope.TELEPORT))
		{
			appendWorkspaceRow(sb, "Endgame Dungeons", "FF9900",
				new String[]{"Crystal Caverns (10)", "admin_move_to 149361 172327 -945"},
				new String[]{"Baylor Raid (10)", "admin_move_to 153569 142075 -12736"},
				new String[]{"Hellbound / Tully", "admin_move_to -12700 273832 -15300"},
				new String[]{"Tower of Naia", "admin_move_to -47271 246098 -9120"},
				new String[]{"Seed of Infinity", "admin_move_to -213678 210670 4408"},
				new String[]{"Seed of Destruction", "admin_move_to -246035 217337 -12208"});
			
			appendWorkspaceRow(sb, "Solo & Party Inst", "00CCFF",
				new String[]{"Kamaloka (Giran)", "admin_move_to 81960 148606 -3468"},
				new String[]{"Pailaka Lv36 (Gludio)", "admin_move_to -14228 123694 -3117"},
				new String[]{"Pailaka Lv61 (Giran)", "admin_move_to 83395 147958 -3405"},
				new String[]{"Pailaka Lv73 (Goddard)", "admin_move_to 147450 -55521 -2732"},
				new String[]{"Nornil's Garden", "admin_move_to -111191 80322 -12936"});
		}
		
		// Inline Active Runtime Instances Summary Table (948px)
		if (access.hasScope(AdminScope.WORLD_SPAWN))
		{
			sb.append("<br1><table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"15151E\">");
			sb.append("<tr bgcolor=\"222230\"><td width=110><font color=\"LEVEL\">Instance ID</font></td><td width=110><font color=\"LEVEL\">Template ID</font></td><td width=388><font color=\"LEVEL\">Instance Name</font></td><td width=140><font color=\"LEVEL\">Players / NPCs</font></td><td width=100 align=center><font color=\"LEVEL\">Enter</font></td><td width=100 align=center><font color=\"LEVEL\">Destroy</font></td></tr>");
			int shown = 0;
			for (var inst : InstanceManager.getInstance().getInstances().values())
			{
				if ((inst == null) || (inst.getId() == 0))
				{
					continue;
				}
				final var world = InstanceManager.getInstance().getWorld(inst.getId());
				final int tplId = (world != null) ? world.getTemplateId() : inst.getTemplateId();
				final String instName = (tplId > 0) ? InstanceManager.getInstance().getInstanceIdName(tplId) : "Dynamic Instance";
				sb.append("<tr bgcolor=\"101016\"><td><font color=\"00FF00\">").append(inst.getId()).append("</font></td><td>").append(tplId).append("</td><td>").append(escapeHtml(instName != null ? instName : "Instance")).append("</td><td>").append(inst.getPlayers().size()).append(" / ").append(inst.getNpcs().size()).append("</td><td align=center>").append(button("Enter", "admin_setinstance " + inst.getId(), 90, 20)).append("</td><td align=center>").append(button("Destroy", "admin_destroyinstance " + inst.getId(), 90, 20)).append("</td></tr>");
				if (++shown >= 6)
				{
					break;
				}
			}
			if (shown == 0)
			{
				sb.append("<tr bgcolor=\"101016\"><td width=948 align=center><font color=\"888888\">No active dynamic instances currently running (Your Instance ID: ").append(gm.getInstanceId()).append(").</font></td><td></td><td></td><td></td><td></td><td></td></tr>");
			}
			sb.append("</table>");
		}
		
		return sb.toString();
	}
	
	/**
	 * Tab 5: Events, Broadcasts, Doors/Fences & GM Utilities Workspace (948px).
	 */
	private String buildEventsWorkspace(Player gm)
	{
		final AccessLevel access = gm.getAccessLevel();
		final StringBuilder sb = new StringBuilder(3072);
		sb.append("<font color=\"LEVEL\">Events, Announcements, World FX &amp; GM Self Controls</font><br1>");
		
		if (access.hasScope(AdminScope.ANNOUNCEMENTS))
		{
			appendWorkspaceRow(sb, "Announcements", "00CCFF",
				new String[]{"Manage Announces", "admin_announces list"},
				new String[]{"Announce ($qbox)", "admin_announce $qbox"},
				new String[]{"Critical ($qbox)", "admin_announce_crit $qbox"},
				new String[]{"Screen ($qbox)", "admin_announce_screen $qbox"});
		}
		
		if (access.hasScope(AdminScope.EVENT_GM))
		{
			appendWorkspaceRow(sb, "Events & Arena", "00FF00",
				new String[]{"Event Manager", "admin_event_menu"},
				new String[]{"FakePlayer AI Control", "admin_fakeplayer menu"},
				new String[]{"Fence Builder", "admin_listfence"},
				new String[]{"Open Target Door", "admin_open"},
				new String[]{"Close Target Door", "admin_close"});
			
			appendWorkspaceRow(sb, "Buffs & Effects", "LEVEL",
				new String[]{"Target Buffs", "admin_getbuffs"},
				new String[]{"Stop All Buffs", "admin_stopallbuffs"},
				new String[]{"Area Cancel (500)", "admin_areacancel 500"},
				new String[]{"Effects & Socials", "admin_html effects_menu.htm"});
			
			appendWorkspaceRow(sb, "Mounts & Transform", "FF9900",
				new String[]{"Ride Wyvern", "admin_ride_wyvern"},
				new String[]{"Ride Strider", "admin_ride_strider"},
				new String[]{"Ride Jet Bike", "admin_ride_bike"},
				new String[]{"Untransform", "admin_untransform"});
		}
		
		appendWorkspaceRow(sb, "GM Self Status", "AAAAAA",
			new String[]{"GM List ON", "admin_gmliston"},
			new String[]{"GM List OFF", "admin_gmlistoff"},
			new String[]{"Toggle Diet", "admin_diet"},
			new String[]{"Toggle TradeOff", "admin_tradeoff"});
		
		return sb.toString();
	}
	
	/**
	 * Tab 6: Economy, Item Creation & Character Editor Workspace (948px).
	 */
	private String buildEconomyWorkspace(Player gm)
	{
		final AccessLevel access = gm.getAccessLevel();
		final StringBuilder sb = new StringBuilder(3072);
		sb.append("<font color=\"LEVEL\">Items, Economy &amp; Character Stat / Skill Editor</font><br1>");
		
		if (access.hasScope(AdminScope.ECONOMY_ITEMS))
		{
			appendWorkspaceRow(sb, "Item Creation", "00FF00",
				new String[]{"Item Creator", "admin_itemcreate"},
				new String[]{"GM Shop", "admin_gmshop"},
				new String[]{"Search Item/NPC", "admin_search $qbox"},
				new String[]{"Destroy Target Inv", "admin_destroy_items"});
			
			appendWorkspaceRow(sb, "Gear Enhancement", "LEVEL",
				new String[]{"Enchant Panel", "admin_enchant"},
				new String[]{"Element Panel", "admin_html element.htm"},
				new String[]{"Augment Panel", "admin_html augment.htm"},
				new String[]{"Premium Menu", "admin_premium_menu"});
			
			appendWorkspaceRow(sb, "Points & Rewards", "00CCFF",
				new String[]{"PC Bang Points", "admin_pccafepoints"},
				new String[]{"Game Points", "admin_gamepoints"},
				new String[]{"Give Adena (10M)", "admin_create_item 57 10000000"},
				new String[]{"Give AA (10M)", "admin_create_item 5575 10000000"});
		}
		
		if (access.hasScope(AdminScope.CHAR_EDIT))
		{
			appendWorkspaceRow(sb, "Character Editor", "FF9900",
				new String[]{"Edit Target Char", "admin_edit_character"},
				new String[]{"Manage Skills", "admin_skill_list"},
				new String[]{"Give All Skills", "admin_give_all_skills"},
				new String[]{"Add EXP / SP", "admin_exp_sp_to_character"},
				new String[]{"Set Level ($qbox)", "admin_set_level $qbox"});
			
			appendWorkspaceRow(sb, "Class & Status", "FF9900",
				new String[]{"Class Selection", "admin_html charclasses.htm"},
				new String[]{"Toggle Hero", "admin_sethero"},
				new String[]{"Toggle Noble", "admin_setnoble"},
				new String[]{"Target Quests", "admin_charquestmenu"});
		}
		
		return sb.toString();
	}
	
	/**
	 * Tab 7: Server Administration & Zero-Downtime Transactional Hot-Reload Console (948px).
	 */
	private String buildServerWorkspace(Player gm)
	{
		final AccessLevel access = gm.getAccessLevel();
		final StringBuilder sb = new StringBuilder(3072);
		sb.append("<font color=\"LEVEL\">Server Administration &amp; Zero-Downtime Delta Hot-Reload Engine</font><br1>");
		
		if (access.hasScope(AdminScope.SERVER_ADMIN))
		{
			final int dirtyCount = ScriptEngine.getInstance().getModifiedScriptCount();
			sb.append("<table width=948 border=0 cellpadding=3 cellspacing=0 bgcolor=\"18281E\"><tr>");
			sb.append("<td width=588 align=left><font color=\"00FF00\">[Delta Hot-Reload Engine]</font> <font color=\"CCCCCC\">Tracked Scripts: ")
				.append(ScriptEngine.getInstance().getTrackedScriptCount())
				.append(" | Modified Since Last Reload: </font>")
				.append(dirtyCount > 0 ? ("<font color=\"FFCC00\">" + dirtyCount + " dirty script(s)</font>") : "<font color=\"00FF00\">0 (Clean)</font>")
				.append("</td>");
			sb.append("<td width=360 align=right>").append(button("[ Reload Changed Scripts (Delta) ]", "admin_reload changed", 340, 24)).append("</td>");
			sb.append("</tr></table><br1>");
			
			appendWorkspaceRow(sb, "Hot-Reload Engine", "00FF00",
				new String[]{"Reload Changed (Delta)", "admin_reload changed"},
				new String[]{"Reload Script ($qbox)", "admin_reload script $qbox"},
				new String[]{"Reload All Handlers", "admin_reload handler"},
				new String[]{"Reload Effect Hdlrs", "admin_reload effect"},
				new String[]{"Script Inspector", "admin_show_quests"});
			
			appendWorkspaceRow(sb, "XML Data Reloaders", "00CCFF",
				new String[]{"Reload Skills XML", "admin_reload skill"},
				new String[]{"Reload Items XML", "admin_reload item"},
				new String[]{"Reload NPCs XML", "admin_reload npc"},
				new String[]{"Reload Multisell", "admin_reload multisell"},
				new String[]{"Reload Teleports", "admin_reload teleport"});
			
			appendWorkspaceRow(sb, "Config & RBAC", "LEVEL",
				new String[]{"Reload HTML Cache", "admin_reload htm"},
				new String[]{"Reload Configs (.ini)", "admin_reload config"},
				new String[]{"Reload Access/RBAC", "admin_reload access"},
				new String[]{"Reload Instances XML", "admin_reload instance"},
				new String[]{"Full Reload Menu", "admin_reload_menu"});
			
			appendWorkspaceRow(sb, "Server Lifecycle", "FF9900",
				new String[]{"JVM & Server Info", "admin_serverinfo"},
				new String[]{"Shutdown / Restart", "admin_server_shutdown"},
				new String[]{"LoginServer Menu", "admin_server_login"},
				new String[]{"Save Olympiad", "admin_saveolymp"},
				new String[]{"FakePlayer AI Control", "admin_fakeplayer menu"});
		}
		
		if (access.hasScope(AdminScope.MASTER_CONTROL))
		{
			appendWorkspaceRow(sb, "Master Security", "FF6666",
				new String[]{"Set Access ($qbox)", "admin_changelvl $qbox"},
				new String[]{"Packet Forge", "admin_forge"},
				new String[]{"System Msg ($qbox)", "admin_msg $qbox"},
				new String[]{"Debug Target", "admin_debug"});
		}
		
		return sb.toString();
	}
	
	/**
	 * Legacy NCSoft Graphical GM Inspectors & Classic L2J HTML Sub-Menus fallback view (920px).
	 */
	private String buildLegacyWorkspace(Player gm)
	{
		final AccessLevel access = gm.getAccessLevel();
		final StringBuilder sb = new StringBuilder(2048);
		sb.append("<font color=\"LEVEL\">Legacy NCSoft GM Inspection Windows &amp; Classic HTML Tools</font><br1>");
		
		if (access.hasScope(AdminScope.PLAYER_SUPPORT))
		{
			appendWorkspaceRow(sb, "Native C++ Popups", "00CCFF",
				new String[]{"Client Status Wnd", "admin_gmw legacy_view status"},
				new String[]{"Client Inventory Wnd", "admin_gmw legacy_view items"},
				new String[]{"Client Skills Wnd", "admin_gmw legacy_view skills"},
				new String[]{"Client Quests Wnd", "admin_gmw legacy_view quests"},
				new String[]{"Client Warehouse Wnd", "admin_gmw legacy_view warehouse"},
				new String[]{"Client Clan Wnd", "admin_gmw legacy_view clan"});
		}
		
		appendWorkspaceRow(sb, "Classic HTML Menus", "AAAAAA",
			access.hasScope(AdminScope.PLAYER_SUPPORT) ? new String[]{"Char Manage HTM", "admin_html charmanage.htm"} : new String[]{"", ""},
			access.hasScope(AdminScope.TELEPORT) ? new String[]{"Teleports HTM", "admin_html teleports.htm"} : new String[]{"", ""},
			access.hasScope(AdminScope.ECONOMY_ITEMS) ? new String[]{"GM Shops HTM", "admin_html gmshops.htm"} : new String[]{"", ""},
			access.hasScope(AdminScope.CHAR_EDIT) ? new String[]{"Skills HTM", "admin_html skills.htm"} : new String[]{"", ""},
			access.hasScope(AdminScope.WORLD_SPAWN) ? new String[]{"Spawns HTM", "admin_html spawns.htm"} : new String[]{"", ""});
		
		return sb.toString();
	}
	
	/**
	 * Dynamically scrubs unauthorized RBAC buttons/links from raw admin HTML and adapts it for
	 * safe rendering inside the 2009 Gracia Epilogue Native C++ GMWnd.HtmlViewer (920px).
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
	 * and adapts it for safe rendering inside the 2009 Gracia Epilogue Native C++ GMWnd.HtmlViewer (920px).
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
		
		// 5. Normalize "bypass -h " -> "bypass " so NCHtmlViewer (nwindow.dll VA 0x100ef525 -> 0x100eda50) never closes GMWnd on button click.
		body = body.replaceAll("(?i)action\\s*=\\s*\"bypass\\s+-h\\s+", "action=\"bypass ");
		
		// 6. Convert legacy "link admin/..." actions into "bypass admin_html ..." so GMWnd.HtmlViewer handles them seamlessly.
		body = body.replaceAll("(?i)action\\s*=\\s*\"link\\s+admin/([^\"]+)\"", "action=\"bypass admin_html $1\"");
		
		// 7. Strip duplicate <edit var="qbox"> inputs in adapted sub-pages so NWindow.dll never collides with Top Bar QuickBox.
		body = DUPLICATE_QBOX_EDIT_PATTERN.matcher(body).replaceAll("<font color=\"LEVEL\">[Use Top QuickBox]</font>");
		
		// 8. Promote cramped 260/270/280/300px legacy tables to full 920px wide-screen layout.
		body = body.replaceAll("(?i)<table\\s+width\\s*=\\s*\"?(?:260|270|280|290|300)\"?\\b", "<table width=920");
		
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
