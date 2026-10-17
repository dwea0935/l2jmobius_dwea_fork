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
package org.l2jmobius.gameserver.data.enums;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Java 27 Role-Based Access Control (RBAC) capability scopes for the Admin Control Center.
 * Replaces flat integer access checks with composable permission sets and dynamic UI filtering.
 */
public enum AdminScope
{
	PANEL_ACCESS("Panel & Self GM", "Basic access to open the Native GMWnd Admin Console and toggle self GM states."),
	CHAT_MODERATION("Chat & Petitions", "Handle player petitions, chat bans, and GM chat."),
	ANNOUNCEMENTS("Announcements", "Broadcast and manage server announcements."),
	TELEPORT("Teleport & Navigation", "Teleport self, players, or NPCs across the world."),
	PLAYER_SUPPORT("Player Support", "Inspect characters, heal/resurrect, unstick, kick, and clear instance lockouts."),
	PUNISHMENT("Punishments & Bans", "Jail, unjail, ban characters or accounts, and manage punishments."),
	EVENT_GM("Events & World FX", "Manage in-game events, buffs, transforms, doors, fences, and effects."),
	WORLD_SPAWN("Spawns, Bosses & Sieges", "Spawn NPCs, inspect raid/grand bosses, and control sieges or instances."),
	CLAN_MANAGEMENT("Clan & Pledge", "Inspect and manage clans, leaders, and reputation."),
	CHAR_EDIT("Character Editor", "Modify player stats, levels, EXP/SP, skills, class, hero/noble, and quests."),
	ECONOMY_ITEMS("Items & Economy", "Create items, open GM shop, enchant/augment gear, and grant points/premium."),
	SERVER_ADMIN("Server & Hot-Reload", "Hot-reload scripts/configs, view JVM diagnostics, geodata, and server lifecycle."),
	MASTER_CONTROL("Master & Security", "Change GM access levels, send raw packets (pforge), and unrestricted control.");
	
	private final String _displayName;
	private final String _description;
	
	AdminScope(String displayName, String description)
	{
		_displayName = displayName;
		_description = description;
	}
	
	public String getDisplayName()
	{
		return _displayName;
	}
	
	public String getDescription()
	{
		return _description;
	}
	
	/**
	 * Parses a comma-separated list of scope names (or "ALL" / "NONE"), falling back to role defaults if blank.
	 * @param scopesCsv comma-separated scope names from AccessLevels.xml
	 * @param level numeric access level
	 * @return unmodifiable EnumSet of granted AdminScopes
	 */
	public static Set<AdminScope> parseScopes(String scopesCsv, int level)
	{
		if (level <= 0)
		{
			return Collections.emptySet();
		}
		
		if ((scopesCsv == null) || scopesCsv.isBlank())
		{
			return defaultScopesForLevel(level);
		}
		
		final String trimmed = scopesCsv.trim();
		if ("ALL".equalsIgnoreCase(trimmed) || "*".equals(trimmed))
		{
			return Collections.unmodifiableSet(EnumSet.allOf(AdminScope.class));
		}
		
		if ("NONE".equalsIgnoreCase(trimmed))
		{
			return Collections.emptySet();
		}
		
		final EnumSet<AdminScope> result = EnumSet.of(PANEL_ACCESS);
		for (String token : trimmed.split(","))
		{
			final String name = token.trim().toUpperCase(Locale.ROOT);
			if (!name.isEmpty())
			{
				try
				{
					result.add(AdminScope.valueOf(name));
				}
				catch (IllegalArgumentException ignored)
				{
					// Ignore unknown custom token.
				}
			}
		}
		
		return Collections.unmodifiableSet(result);
	}
	
	/**
	 * Returns default RBAC capability scopes for a standard L2J access level tier.
	 * @param level the numeric access level
	 * @return unmodifiable set of AdminScopes
	 */
	public static Set<AdminScope> defaultScopesForLevel(int level)
	{
		if (level <= 0)
		{
			return Collections.emptySet();
		}
		
		final EnumSet<AdminScope> scopes = switch (level)
		{
			case 10 -> EnumSet.of(
				PANEL_ACCESS,
				CHAT_MODERATION);
			case 20, 30 -> EnumSet.of(
				PANEL_ACCESS,
				CHAT_MODERATION,
				TELEPORT,
				PLAYER_SUPPORT);
			case 40 -> EnumSet.of(
				PANEL_ACCESS,
				CHAT_MODERATION,
				ANNOUNCEMENTS,
				TELEPORT,
				PLAYER_SUPPORT,
				PUNISHMENT);
			case 50 -> EnumSet.of(
				PANEL_ACCESS,
				CHAT_MODERATION,
				ANNOUNCEMENTS,
				TELEPORT,
				PLAYER_SUPPORT,
				EVENT_GM,
				WORLD_SPAWN);
			case 60 -> EnumSet.of(
				PANEL_ACCESS,
				CHAT_MODERATION,
				ANNOUNCEMENTS,
				TELEPORT,
				PLAYER_SUPPORT,
				PUNISHMENT,
				EVENT_GM,
				WORLD_SPAWN,
				CLAN_MANAGEMENT,
				CHAR_EDIT);
			case 70 -> EnumSet.of(
				PANEL_ACCESS,
				CHAT_MODERATION,
				ANNOUNCEMENTS,
				TELEPORT,
				PLAYER_SUPPORT,
				PUNISHMENT,
				EVENT_GM,
				WORLD_SPAWN,
				CLAN_MANAGEMENT,
				CHAR_EDIT,
				ECONOMY_ITEMS,
				SERVER_ADMIN);
			default -> level >= 100
				? EnumSet.allOf(AdminScope.class)
				: EnumSet.of(PANEL_ACCESS, CHAT_MODERATION, TELEPORT, PLAYER_SUPPORT);
		};
		return Collections.unmodifiableSet(scopes);
	}
	
	/**
	 * Classifies any admin command into its governing RBAC {@link AdminScope} using Java 27 switch pattern matching.
	 * @param rawCommand full or base admin command (with or without "admin_" prefix)
	 * @return the governing AdminScope
	 */
	public static AdminScope classifyCommand(String rawCommand)
	{
		if ((rawCommand == null) || rawCommand.isEmpty())
		{
			return MASTER_CONTROL;
		}
		
		final int spaceIdx = rawCommand.indexOf(' ');
		final String base = (spaceIdx > 0 ? rawCommand.substring(0, spaceIdx) : rawCommand).toLowerCase(Locale.ROOT);
		final String cmd = base.startsWith("admin_") ? base.substring(6) : base;
		
		return switch (cmd)
		{
			// 1. Panel Navigation & Non-Combat PM/List Toggles
			case String s when s.equals("admin") || s.startsWith("admin1") || s.startsWith("admin2")
				|| s.startsWith("admin3") || s.startsWith("admin4") || s.startsWith("admin5")
				|| s.startsWith("admin6") || s.startsWith("admin7")
				|| s.equals("html") || s.equals("help") || s.equals("commands") || s.equals("instancezones")
				|| s.equals("gm") || s.equals("gmw") || s.startsWith("gmlist")
				|| s.equals("silence") || s.equals("tradeoff") -> PANEL_ACCESS;
			
			// 2. Chat Moderation & Petitions
			case String s when s.contains("petition") || s.equals("force_peti")
				|| s.equals("banchat") || s.equals("unbanchat")
				|| s.equals("chatban") || s.equals("chatunban")
				|| s.equals("ban_chat") || s.equals("unban_chat") || s.startsWith("chat_ban")
				|| s.startsWith("gmchat") || s.startsWith("snoop") -> CHAT_MODERATION;
			
			// 3. Announcements
			case String s when s.startsWith("announce") || s.startsWith("autoannounce")
				|| s.startsWith("critannounce") -> ANNOUNCEMENTS;
			
			// 4. Teleport, Field Movement, Invisibility & GM Speed
			case String s when s.startsWith("tele") || s.startsWith("move_to")
				|| s.equals("show_moves") || s.equals("show_moves_other")
				|| s.startsWith("recall") || s.startsWith("go") || s.equals("walk")
				|| s.startsWith("instant_move") || s.equals("sendhome")
				|| s.equals("invis") || s.equals("invis_menu") || s.equals("vis")
				|| s.equals("invisible") || s.equals("visible") || s.equals("hide")
				|| s.equals("ghoston") || s.equals("ghostoff")
				|| s.equals("gmspeed") || s.startsWith("superhaste") || s.startsWith("speed") -> TELEPORT;
			
			// 5. Player Support & Inspection
			case String s when s.equals("current_player") || s.equals("character_info")
				|| s.equals("character_list") || s.equals("show_characters")
				|| s.startsWith("find_") || s.equals("strict_find_dualbox") || s.equals("tracert")
				|| s.equals("partyinfo") || s.equals("summon_info") || s.equals("unsummon") || s.equals("show_pet_inv")
				|| s.equals("heal") || s.equals("res") || s.equals("res_monster")
				|| s.equals("kick") || s.equals("kick_menu") || s.equals("character_disconnect")
				|| s.equals("repair") || s.equals("restore")
				|| s.startsWith("instancezone") || s.startsWith("getbuffs")
				|| s.equals("stopbuff") || s.equals("stopallbuffs")
				|| s.startsWith("viewblocked") || s.startsWith("viewskill")
				|| s.equals("removereuse") || s.equals("switch_gm_buffs")
				|| s.equals("charquestmenu") || s.equals("quest_info")
				|| s.startsWith("debug") || s.equals("target") || s.equals("online") -> PLAYER_SUPPORT;
			
			// 6. Punishments & Bans
			case String s when s.startsWith("punishment") || s.startsWith("jail")
				|| s.equals("unjail") || s.startsWith("ban_") || s.startsWith("unban_")
				|| s.equals("kick_non_gm") || s.equals("char_manage") -> PUNISHMENT;
			
			// 7. Events, GM Combat States (Invul/Kill/Diet), Doors, Fences & World FX
			case String s when s.startsWith("event") || s.equals("invul") || s.equals("diet")
				|| s.startsWith("kill") || s.equals("buff") || s.equals("areacancel")
				|| s.startsWith("transform") || s.equals("untransform")
				|| s.startsWith("ride") || s.startsWith("unride")
				|| s.equals("open") || s.equals("close") || s.equals("openall") || s.equals("closeall") || s.equals("showdoors")
				|| s.contains("fence") || s.startsWith("effect") || s.startsWith("social")
				|| s.startsWith("ave_") || s.startsWith("abnormal") || s.startsWith("atmosphere") || s.startsWith("play_sound")
				|| s.startsWith("para") || s.startsWith("unpara") || s.startsWith("bighead")
				|| s.startsWith("shrinkhead") || s.contains("vitality") || s.equals("fullfood")
				|| s.equals("clearteams") || s.startsWith("setteam")
				|| s.startsWith("earthquake") || s.startsWith("gmsay") || s.equals("fakechat") -> EVENT_GM;
			
			// 8. World Spawns, Bosses, Sieges, Zones & Instances (summon delegates items < 1M to create_item [ECONOMY_ITEMS])
			case String s when s.startsWith("spawn") || s.equals("summon") || s.equals("unspawnall") || s.equals("respawnall")
				|| s.startsWith("delete") || s.startsWith("list_spawns") || s.startsWith("list_positions")
				|| s.startsWith("scan") || s.equals("show_spawns") || s.equals("show_npcs") || s.equals("npc_index")
				|| s.equals("topspawncount") || s.equals("top_spawn_count") || s.equals("instance_spawns")
				|| s.startsWith("grandboss") || s.contains("siege") || s.startsWith("territory")
				|| s.startsWith("clanhall") || s.equals("add_attacker") || s.equals("add_defender")
				|| s.equals("add_guard") || s.equals("move_defenders") || s.equals("setcastle") || s.equals("removecastle")
				|| s.equals("add_fortattacker") || s.equals("setfort") || s.equals("removefort")
				|| s.startsWith("mammon") || s.startsWith("cw_") || s.startsWith("cursed")
				|| s.startsWith("gracia") || s.equals("kill_tiat") || s.equals("set_sodstate") || s.equals("set_soistage")
				|| s.startsWith("hellbound") || s.startsWith("manor") || s.startsWith("zone")
				|| s.equals("setinstance") || s.equals("createinstance")
				|| s.equals("destroyinstance") || s.equals("listinstances")
				|| s.startsWith("region") || s.startsWith("show_region") || s.startsWith("showregion") -> WORLD_SPAWN;
			
			// 9. Clan & Pledge Management
			case String s when s.startsWith("clan") || s.startsWith("pledge") || s.equals("remove_clan_penalty") -> CLAN_MANAGEMENT;
			
			// 10. Character Stat / Skill / Class Editor
			case String s when s.equals("edit_character") || s.startsWith("add_exp") || s.startsWith("remove_exp")
				|| s.equals("exp_sp_to_character") || s.startsWith("add_level") || s.startsWith("set_level")
				|| s.contains("skill") || s.equals("setcharquest") || s.equals("rec")
				|| s.equals("nokarma") || s.startsWith("setkarma") || s.startsWith("setfame")
				|| s.equals("changename") || s.startsWith("setname")
				|| s.startsWith("settitle") || s.startsWith("setsex") || s.startsWith("setcolor")
				|| s.startsWith("settcolor") || s.startsWith("setclass") || s.startsWith("setpk")
				|| s.startsWith("setpvp") || s.startsWith("sethero") || s.startsWith("givehero")
				|| s.startsWith("setnoble") || s.equals("summon_setlvl")
				|| s.equals("set") || s.startsWith("set_") || s.equals("setinvul") || s.equals("setinvis") -> CHAR_EDIT;
			
			// 11. Economy, Items, Enchant, Augment & Points
			case String s when s.equals("itemcreate") || s.startsWith("create_") || s.startsWith("give_item")
				|| s.equals("gmshop") || s.equals("buy") || s.startsWith("enchant")
				|| s.startsWith("seteh") || s.startsWith("setec") || s.startsWith("seteg")
				|| s.startsWith("setel") || s.startsWith("seteb") || s.startsWith("setew")
				|| s.startsWith("setes") || s.startsWith("setle") || s.startsWith("setre")
				|| s.startsWith("setlf") || s.startsWith("setrf") || s.startsWith("seten")
				|| s.startsWith("setun") || s.startsWith("setba") || s.startsWith("setbe")
				|| s.startsWith("setlh") || s.startsWith("setlc") || s.startsWith("setll")
				|| s.startsWith("setlg") || s.startsWith("setlb") || s.startsWith("setlw") || s.startsWith("setls")
				|| s.startsWith("augment") || s.startsWith("element") || s.startsWith("destroy")
				|| s.startsWith("pccafe") || s.contains("game_points") || s.equals("gamepoints")
				|| s.equals("reload_itemmall") || s.startsWith("premium") || s.startsWith("search") -> ECONOMY_ITEMS;
			
			// 12. Server Admin, Hot-Reload, Geodata & Diagnostics
			case String s when s.startsWith("reload") || s.equals("quest_reload")
				|| s.startsWith("script_") || s.equals("show_quests") || s.equals("loadhtml")
				|| s.startsWith("server") || s.startsWith("login")
				|| s.equals("setconfig") || s.equals("config_server") || s.equals("set_mod") || s.equals("msg")
				|| s.startsWith("geo")
				|| s.equals("en") || s.equals("dn") || s.equals("es") || s.equals("ds")
				|| s.equals("ee") || s.equals("de") || s.equals("ew") || s.equals("dw") || s.equals("ge")
				|| s.contains("olymp") || s.startsWith("fakeplayer") || s.equals("fpc")
				|| s.equals("exceptions") -> SERVER_ADMIN;
			
			// 13. Master / Security Operations (changelvl, setaccess, forge*)
			default -> MASTER_CONTROL;
		};
	}
}
