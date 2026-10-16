# Java 27 Role-Based Access Control (RBAC) & 2009 Client-Safe Admin Community Board Architecture

**Branch:** `feature/admin-community-board-rbac` (Merged into `master`)  
**Target Runtime:** OpenJDK 27 (`--release 27`)  
**Target Game Client:** Lineage II — The Chaotic Throne: Gracia Epilogue (Protocol 146 / 2009 `NWindow.dll` HTML 3.2 Engine)

---

## 1. Executive Summary & Architectural Goals

For over two decades, L2J servers relied on a cramped 270px NPC dialog window (`NpcHtmlMessage`), flat integer `accessLevel` checks (`30` vs `100`), and 80+ fragmented admin command handlers dating back to 2004. Worse still:
1. **Broken Stock Staff Roles:** In stock `AccessLevels.xml`, levels `10` through `60` (`Chat Moderator`, `Test GM`, `General GM`, `Support GM`, `Event GM`, `Head GM`) had `isGM="false"`, causing `AdminCommandHandler` (`if (!player.isGM()) return;`) to block every staff tier below `Admin (70)`. Conversely, stock `AdminCommands.xml` assigned `accessLevel="30"` to `server_shutdown`, `server_restart`, and `server_gm_only` while assigning `accessLevel="100"` to basic support commands like `heal` and `res`.
2. **2009 Gracia Epilogue Client `ShowBoard` Crash Bugs:**
   * `HtmlUtil.sendCBHtml` split Community Board packets at `16,250` characters instead of Gracia Epilogue's authentic `4,090`-character per-packet limit (`101`, `102`, `103` = `12,270` characters max), overflowing `NWindow.dll`'s 4096-wchar buffer on medium-sized boards.
   * `new ShowBoard(null, "102")` concatenated the literal string `"null"` (`"102\u0008null"`) into the client's HTML parser buffer.
   * Wrapping legacy admin `.htm` files inside a nested left-sidebar/right-content `<table>` pushed `NWindow.dll` past its strict 2-level `<table>` nesting limit.

This architecture replaces the legacy `//admin` popup and flat integer checks with a **Wide-Screen (`ShowBoard` / `ALT+B`) Role-Based Access Control (RBAC) Admin Control Center** engineered specifically for the 2009 Gracia Epilogue client.

---

## 2. Complete Isolation from Regular Players (`accessLevel <= 0`)

A strict security invariant is enforced across every entry point:
* **Regular Players (`User`, `level="0"`, `isGM="false"`, `scopes="NONE"`) and Banned Accounts (`level="-1"`):**
  * Pressing `ALT+B` (`_bbshome`) opens only the standard player Community Board (`HomeBoard.java`). No "Admin" tab, button, or trace of `//admin` is ever rendered in the player Community Board.
  * Typing `//admin` or sending any `admin_*` packet bypass is immediately rejected by `RequestBypassToServer`, `AdminCommandHandler`, `HomeBoard`, and `AdminBoardManager.canUseAdminBoard(player)`.
* **Staff Roles (`level 10..100`, `isGM="true"`):**
  * Typing `//admin` (or `//board`) opens the wide-screen RBAC Admin Control Center inside the Community Board (`ShowBoard`).
  * Staff can switch between the **Admin Control Center** and the **Regular Player Community Board** at any time via the `[Player CB]` button in the top command deck (and return via the GM-only `[Admin Control Center]` banner rendered at the top of `HomeBoard` exclusively when `AdminBoardManager.getInstance().canUseAdminBoard(player)` is `true`).

---

## 3. Java 27 Role-Based Access Control (`AdminScope` Engine)

### 3.1 Composable Capability Scopes (`AdminScope.java`)
Instead of checking whether an integer `accessLevel` is `30` or `100`, [`AdminScope.java`](L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/data/enums/AdminScope.java) defines **13 granular capability scopes** and classifies all 466 admin commands using Java 27 `switch` pattern matching (`case String s when ... ->`):

| `AdminScope` | Capability Domain | Representative Commands |
| :--- | :--- | :--- |
| `PANEL_ACCESS` | Panel Navigation & Self GM State | `admin`, `board*`, `html`, `help`, `invis`, `invul`, `silence`, `gmspeed`, `superhaste` |
| `CHAT_MODERATION` | Chat Bans, Petitions & Snooping | `banchat`, `unbanchat`, `view_petitions`, `accept_petition`, `gmchat`, `snoop` |
| `ANNOUNCEMENTS` | Server Broadcasts | `announce`, `announce_crit`, `announce_screen`, `autoannounce` |
| `TELEPORT` | World Navigation & Recall | `tele`, `teleportto`, `move_to`, `recall`, `recall_party_menu`, `sendhome`, `gonorth` |
| `PLAYER_SUPPORT` | Player Inspection & Stuck Recovery | `character_info`, `find_character`, `find_dualbox`, `heal`, `res`, `repair`, `kick`, `instancezone` |
| `PUNISHMENT` | Account/Char Bans & Jail | `jail`, `unjail`, `ban_char`, `unban_char`, `ban_acc`, `punishment`, `char_manage` |
| `EVENT_GM` | Events, Buffs, Doors, Fences & FX | `event_menu`, `buff`, `transform`, `open`/`close`, `addfence`, `para`, `atmosphere`, `vitality` |
| `WORLD_SPAWN` | Spawns, Bosses, Sieges & Instances | `spawn_monster`, `delete`, `grandboss`, `siege`, `fortsiege`, `territory_war`, `gracia_seeds`, `zone_check` |
| `CLAN_MANAGEMENT` | Clan & Pledge Operations | `clan_info`, `clan_changeleader`, `pledge`, `remove_clan_penalty` |
| `CHAR_EDIT` | Character Stat, Skill, Class & Quest Editor | `edit_character`, `set_level`, `add_exp_sp`, `skill_list`, `setclass`, `sethero`, `setnoble`, `setcharquest` |
| `ECONOMY_ITEMS` | Item Creation, GM Shop, Enchant & Points | `itemcreate`, `create_item`, `create_coin`, `gmshop`, `enchant`, `setew`, `augment`, `premium_menu` |
| `SERVER_ADMIN` | Hot-Reload Engine, Geodata & Lifecycle | `reload`, `quest_reload`, `script_load`, `server_shutdown`, `server_login`, `serverinfo`, `geo_pos`, `checkolympiad` |
| `MASTER_CONTROL` | Security & Packet Forging | `changelvl`, `setaccess`, `forge`, `forge_values`, `forge_send` |

### 3.2 Staff Role Hierarchy (`AccessLevels.xml`)
[`AccessLevels.xml`](L2J_Mobius_CT_2.4_Epilogue/dist/game/config/AccessLevels.xml) and [`AccessLevels.xsd`](L2J_Mobius_CT_2.4_Epilogue/dist/game/data/xsd/AccessLevels.xsd) define the explicit `scopes` attribute for all 10 access levels:

| Level | Role Name | `isGM` | Assigned RBAC `AdminScope` Capabilities |
| :---: | :--- | :---: | :--- |
| `-1` | **Banned** | `false` | `NONE` |
| `0` | **User (Regular Player)** | `false` | `NONE` (100% isolated from `//admin` and Admin Community Board) |
| `10` | **Chat Moderator** | `true` | `PANEL_ACCESS`, `CHAT_MODERATION` |
| `20` | **Test GM** | `true` | `PANEL_ACCESS`, `CHAT_MODERATION`, `TELEPORT`, `PLAYER_SUPPORT` |
| `30` | **General GM** | `true` | `PANEL_ACCESS`, `CHAT_MODERATION`, `TELEPORT`, `PLAYER_SUPPORT` |
| `40` | **Support GM** | `true` | `PANEL_ACCESS`, `CHAT_MODERATION`, `ANNOUNCEMENTS`, `TELEPORT`, `PLAYER_SUPPORT`, `PUNISHMENT` |
| `50` | **Event GM** | `true` | `PANEL_ACCESS`, `CHAT_MODERATION`, `ANNOUNCEMENTS`, `TELEPORT`, `PLAYER_SUPPORT`, `EVENT_GM`, `WORLD_SPAWN` |
| `60` | **Head GM** | `true` | `PANEL_ACCESS`, `CHAT_MODERATION`, `ANNOUNCEMENTS`, `TELEPORT`, `PLAYER_SUPPORT`, `PUNISHMENT`, `EVENT_GM`, `WORLD_SPAWN`, `CLAN_MANAGEMENT`, `CHAR_EDIT` |
| `70` | **Admin** | `true` | All scopes except `MASTER_CONTROL` (`ECONOMY_ITEMS` + `SERVER_ADMIN` unlocked) |
| `100` | **Master** | `true` | `ALL` (Includes `MASTER_CONTROL`: `changelvl`, `setaccess`, `forge`) |

---

## 4. 2009 Gracia Epilogue Client HTML 3.2 Dialect Safety & Automatic Interceptor

### 4.1 Core Packet & Buffer Fixes
1. **[`ShowBoard.java`](L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/network/serverpackets/ShowBoard.java):** Fixed `ShowBoard(String htmlCode, String id)` so `null` `htmlCode` is converted to `""` instead of appending `"null"` to packet `102`/`103`.
2. **[`HtmlUtil.java`](L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/util/HtmlUtil.java):** Restored `sendCBHtml` chunking to the authentic 2009 Gracia Epilogue `4,090`-character per-packet limit (`101`, `102`, `103` = `12,270` characters max) and populated both `HtmlActionScope.COMM_BOARD_HTML` and `HtmlActionScope.NPC_HTML` action caches so `admin_*` and `_bbs*` bypasses validate seamlessly.

### 4.2 Wide-Screen Community Board Shell & Dynamic RBAC HTML Scrubber ([`AdminBoardManager.java`](L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/managers/AdminBoardManager.java))
* **Flat Stacked Layout (Max 2-Level `<table>` Nesting):**
  * **Top Command Deck (`740px`):** Displays staff member name, role badge, active RBAC scopes, 1-click self toggles (`Invis`, `Invul`, `MutePM`, `Spd 1x`, `Spd 2x`, `Spd 4x`), `[Player CB]`, and `[Close X]`.
  * **RBAC-Filtered Navigation Bar:** Renders only the workspace tabs (`[Dashboard]`, `[Support & Players]`, `[World & Spawns]`, `[Events & GM]`, `[Items & Chars]`, `[Server & Reload]`) that the GM's role has permission to access.
  * **Live Target Inspector & QuickBox Bar:** Displays live stats of `player.getTarget()` (`Name`, `ObjectId`, `Level`, `HP/MP`, `X/Y/Z`, `InstanceId`, `Ping`) alongside a shared `$qbox` `<edit>` input and 1-click target actions (`Inspect`, `Goto`, `Recall`, `Heal`, `Res`, `Kill`, `Delete`).
  * **6 Native Wide-Screen Workspaces:** Including a dedicated **Zero-Downtime Transactional Script Hot-Reload Console** on the `Server & Reload` tab (`Reload Script ($qbox)`, `Script Inspector`, `Reload Access/RBAC`, `Reload HTML Cache`, `Reload Skills/Items/NPCs/Multisell`).
* **Automatic `NpcHtmlMessage` Interceptor & RBAC Scrubber (`scrubAndAdaptHtml`):**
  * Whenever an admin command executes while the Admin Community Board is active, any `NpcHtmlMessage` sent to the GM (such as `//character_info`, `//edit_character`, `//show_skills`, `//siege`, `//grandboss`, `//view_petitions`, `//show_quests`) is automatically intercepted in `Player.sendPacket`:
    1. Outer `<html>`, `<body>`, `<head>`, `<title>` tags and duplicate legacy 4-button top nav bars are stripped so `NWindow.dll` never encounters nested document tags.
    2. Every `<button ... action="bypass ... admin_...">` and `<a action="bypass ... admin_...">` element is checked against `AdminData.getInstance().hasAccess(cmd, gmAccess)` — **unauthorized buttons and links are stripped from the HTML before it is sent to the client**.
    3. `action="bypass -h "` is normalized to `action="bypass "` so clicking buttons inside `ShowBoard` never closes the Community Board window.
    4. Cramped `width=260`/`270` tables are expanded to `width=640` wide-screen layout.
  * For state-changing commands that do not send an `NpcHtmlMessage` (e.g., `//heal`, `//invul`, `//invis`, `//gmspeed`), `AdminBoardManager.onCommandEnd` automatically refreshes the active Community Board tab so the Top Command Deck and Live Target Inspector update immediately.

---

## 5. Legacy Purge & Handler Consolidation Summary

11 legacy admin command handlers from `dist/game/data/scripts/handlers/chat/commands/admin/` were purged or consolidated, reducing the handler count from **80 files down to 69 clean handlers**:

### 5.1 Purged Obsolete / Broken 2004-Era Handlers (5 Deleted)
1. **`AdminCamera.java`** (`//cam`, `//camex`, `//cam3`): Hardcoded debug packet studio replaced by `AbstractScripting.specialCamera`.
2. **`AdminFightCalculator.java`** (`//fight_calculator`, `//fcs`): C1-era mock combat loops that bypassed Gracia Epilogue elementals, mastery, and blow formulas.
3. **`AdminPathNode.java`** (`//path_find`): Unfinished `"TODO: Find path"` stub replaced by our Panama FFM `AdminGeodata` tools.
4. **`AdminTargetSay.java`** (`//targetsay`): Spoofed public chat packets from targeted players.
5. **`AdminTest.java`** (`//skill_test`, `//st`): Misleading handler that broadcasted fake `MagicSkillUse` animations without casting skills.
* Also removed dead `//set`, `//set_menu`, `//set_mod` stubs from `AdminAdmin.java`.

### 5.2 Consolidated Redundant Handlers (6 Merged)
1. **`AdminDisconnect.java` $\rightarrow$ [`AdminKick.java`](L2J_Mobius_CT_2.4_Epilogue/dist/game/data/scripts/handlers/chat/commands/admin/AdminKick.java)** (`admin_character_disconnect` merged with `admin_kick` / `admin_kick_non_gm`).
2. **`AdminSuperHaste.java` $\rightarrow$ [`AdminGmSpeed.java`](L2J_Mobius_CT_2.4_Epilogue/dist/game/data/scripts/handlers/chat/commands/admin/AdminGmSpeed.java)** (`admin_superhaste` / `admin_speed` merged with `admin_gmspeed`).
3. **`AdminInstanceZone.java` $\rightarrow$ [`AdminInstance.java`](L2J_Mobius_CT_2.4_Epilogue/dist/game/data/scripts/handlers/chat/commands/admin/AdminInstance.java)** (`admin_instancezone` / `admin_instancezone_clear` merged into the main instance manager).
4. **`AdminPledge.java` $\rightarrow$ [`AdminClan.java`](L2J_Mobius_CT_2.4_Epilogue/dist/game/data/scripts/handlers/chat/commands/admin/AdminClan.java)** (`admin_pledge` merged into the unified clan handler).
5. **`AdminZoneBuild.java` $\rightarrow$ [`AdminZone.java`](L2J_Mobius_CT_2.4_Epilogue/dist/game/data/scripts/handlers/chat/commands/admin/AdminZone.java)** (`admin_zone_build*` merged into the zone handler).
6. **`AdminShowQuests.java` $\rightarrow$ [`AdminQuest.java`](L2J_Mobius_CT_2.4_Epilogue/dist/game/data/scripts/handlers/chat/commands/admin/AdminQuest.java)** (`admin_charquestmenu` / `admin_setcharquest` merged into `AdminQuest.java`, which is also wired to `ScriptEngine.getInstance().reloadScriptByNameOrPath(...)`).
