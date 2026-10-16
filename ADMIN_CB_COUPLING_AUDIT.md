# Admin Console ↔ Community Board Coupling Audit

**Baseline:** `c2f6f65db50` (master before the CB admin panel) → `8a6534aefce` *"Java 27 RBAC Admin Community Board (2009 Epilogue safe)"* → `db33430559f` → `feature/native-gmwnd-console` → current `master` `9419c4d7b5a` (running).

**Trigger:** after the 10:53 deploy, Alt+B correctly opens `BoardWnd` again, but the player Community Board home page still carries the injected GM banner (`[GM Staff Detected: Master Lv.100] RBAC Console (Alt+G Native / //admin)` + `Open //admin Control Center`).

> Screenshot (user, 10:58): Alt+B BoardWnd still shows the GM banner; Alt+G GMWnd shows the native RBAC console.

---

## A. Admin code living inside generic CB / HTML / packet infrastructure (remove or relocate)

| # | Location | Introduced | What it does | Verdict |
| :-- | :-- | :-- | :-- | :-- |
| A1 | [HomeBoard.java:161-165](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/dist/game/data/scripts/handlers/bypass/communityboard/HomeBoard.java#L161-L165) | `8a6534aefce`, edited `7636477796b` | Injects the GM banner + `bypass admin_gmw` button into `_bbshome` / `_bbstop` for every staff member. **This is the screenshot.** | **Remove.** Player CB must not know about the admin console. |
| A2 | [HtmlUtil.java:415](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/util/HtmlUtil.java#L415) | `7636477796b` | Hook in the **generic** `sendCBHtml` path → `AdminBoardManager.tryInterceptAdminBoardHtml` (now `commandDepth > 0` only). Exists solely because 2 admin handlers still call `sendCBHtml` directly: **`AdminAnnouncements`, `AdminGeodata`**. | **Remove hook**; make those 2 handlers call `AdminBoardManager.sendAdminHtml(...)` explicitly. |
| A3 | `Player.sendPacket` ~L4565 → `tryInterceptAdminHtml` ([AdminBoardManager.java:394-425](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/managers/AdminBoardManager.java#L394-L425)) | `8a6534aefce` | Hijacks **every** `NpcHtmlMessage` with `npcObjId == 0` sent to a GM if `commandDepth > 0` **or** the HTML contains `bypass admin_` / `bypass -h admin_` / `link admin/`. Same content-sniffing anti-pattern as the Alt+B bug just fixed. | **Narrow to `commandDepth > 0`** (drop sniffing). Long-term: route admin handlers explicitly, remove the hook from `Player.sendPacket`. |
| A4 | [AdminBoardManager.java:496-503](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/managers/AdminBoardManager.java#L496-L503) (`sendWrappedBoardHtml`, native path) | `8a6534aefce` / native branch | When rendering into `GMWnd`, it also (a) registers all admin actions in the **`COMM_BOARD_HTML`** action cache and (b) **clears `NPC_HTML`**. (a) whitelists admin bypasses in the player-CB scope; (b) invalidates the bypasses of any NPC dialog that is open when the console refreshes. | **Native path: only `NPC_ITEM_HTML`** (the `itemId=1` packet's own scope). No CB scope, no `NPC_HTML` clear. |
| A5 | [RequestBypassToServer.java:~95](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/network/clientpackets/RequestBypassToServer.java) | `8a6534aefce` | Any `admin_*` bypass from a GM **skips HTML-action validation** entirely (added so ShowBoard admin buttons work). RBAC is still enforced in `AdminCommandHandler`, so this is defense-in-depth, not an open hole. | **Optional:** restore validation once A4 builds correct caches; keep if CB fallback (C) survives. |

**Keep (generic, legitimate, not admin-specific):**
- `HtmlUtil.sendCBHtml` packet split `16250 → 4090` chars (authentic 2009 Epilogue limit; benefits the player CB).
- `ShowBoard` null-guard.
- `AbstractHtmlPacket.runImpl`: cache clear moved inside the validated branch (correctness fix).

## B. Community-Board rendering mode inside `AdminBoardManager` (dual-render)

The console still renders to **either** `GMWnd.HtmlViewer` **or** `BoardWnd` (`ShowBoard`):
- Session state: `useCommunityBoard`, `boardOpen`, `sendingAdminCbHtml`, `renderedCount` (9 / 7 / 2 refs).
- CB-only UI: `Player CB` (`bypass _bbshome`) + `Close [X]` buttons in [appendTopHeaderBar:558-562](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/managers/AdminBoardManager.java#L558-L562).
- CB-only commands: `admin_board`, `admin_board_close` (+ `admin_board_*` tab aliases) in `AdminAdmin` and `AdminCommands.xml` L13-21.
- CB-only adaptation: `bypass -h` → `bypass` rewrite *"so clicking buttons in ShowBoard never closes the Community Board"* ([scrubAndAdaptHtml:1145-1146](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/managers/AdminBoardManager.java#L1145-L1146)).
- `handleNavigationCommand` sends `new ShowBoard()` (close BoardWnd) on every `//gmw` / `//admin` for Alt+G staff.

> [!IMPORTANT]
> **Mode B is still the only console path for two groups:**
> 1. **Chat Moderator (level 10)** — `allowAltg="false"` in `AccessLevels.xml` → always CB mode.
> 2. **GMs on a stock Protocol-152 client** — fallback added in `14933860a51` (they lack our patched `GMWnd.HtmlViewer`).
>
> Retiring mode B therefore requires a decision for both groups (see C).

## C. Decision required

| Option | Level-10 Chat Moderator | Protocol-152 GMs | Result |
| :-- | :-- | :-- | :-- |
| **C1. Native-only (full decoupling)** | `allowAltg="true"` (they then need our full `system/` folder) | No console; commands via `//` chat only | Delete mode B entirely, `admin_board*` commands, A5 bypass, CB caches. Cleanest. |
| **C2. Isolated CB fallback** | stays CB | stays CB | Extract mode B into its own small class (e.g. `AdminBoardCbFallback`), used **only** when `!allowAltG() \|\| protocol >= 152`. No hooks in HomeBoard / HtmlUtil / Player; Alt+G staff never touch CB code. |

Items **A1–A4 are needed under both options**.
