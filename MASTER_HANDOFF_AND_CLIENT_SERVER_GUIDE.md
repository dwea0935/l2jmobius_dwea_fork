# Lineage 2 Gracia Epilogue (CT2.4) — Master Client/Server Architecture, Reverse-Engineering & Agent Handoff Guide

**Author / Multi-Agent Engineering Team:** Denys Hordiienko & Antigravity Agents 1 & 2  
**Server Repository:** `dwea0935/l2jmobius_dwea_fork.git` (`master` + feature branches)  
**Client Directory / Repository:** `Client Lineage 2 Gracia Epilogue` (`dwea0935/l2-gracia-epilogue-client.git`)  
**Engine Protocol:** Unreal Engine 2.5 (Package Version 123 / Licensee 37, Network Protocol 146/148) + Java 27 Server Runtime

---

## 1. Executive Summary & Multi-Agent Reconciliation

This document consolidates the complete engineering, reverse-engineering, binary patching, and server architecture work performed across **both autonomous agents** in `/home/dwea/Projects/L2` so any agent on your work machine has 100% of the context immediately:

1. **Client Forensics, Encryption Unlocking & Native Linux Launcher (Agent 2):**
   - Decrypted all `Lineage2Ver111` (XOR `0xAC`) and `Lineage2Ver413` (RSA + zlib) `.ini` and `.int` files in `system/` to human-editable plain text (backups in `system/backup_encrypted/`).
   - Built [`launch-l2.sh`](launch-l2.sh) with full Wine/Proton/UMU + 32-bit DXVK (Vulkan) support, TrueType font provisioning (`tahoma.ttf`, `times.ttf`, `arial.ttf`), CLI flags (`--server`, `--autologin`, `--res`, `--borderless`, `--windowed`, `--fullscreen`, `--gamepad`, `--hud`, `--check`), and controller (`JOYPAD`) bindings.
   - Binary-patched `system/d3ddrv.dll` (silencing the obsolete 2004 AGP popup) and `system/windrv.dll` (5 in-place x86 patches enabling dynamic `WM_SIZE` window resizing, OS maximize button, XWayland async geometry safety, and `(0,0)` Borderless Windowed mode).
2. **Unified Native C++ `GMWnd` (`Alt+G`) Master Console & 6-Pillar Java 27 Server Engine (Agent 1):**
   - Expanded the native C++ `GMWnd` (`Alt+G`) in `system/interface.xdat` from `350 x 355` px to a `992 x 544` px **Unified Master Admin Console** featuring a 2-column native C++ left control dock (`x=10..225`) and an embedded right-hand `GMWnd.HtmlViewer` (`744 x 492` px).
   - Patched 7 UnrealScript functions in `system/interface.u` with **zero file-size delta** (`4,128,791` bytes, verified via `Core.dll` `UStruct::Serialize` / `SerializeExpr` simulation in [`patch_gmwnd_client.py`](patch_gmwnd_client.py)):
     - Routed server-sent `NpcHtmlMessage(0, 1, html)` (`EV_ItemDescWndLoadHtmlFromString`) directly into `GMWnd.HtmlViewer` with zero `BoardWnd` `PageLock` and zero popup conflict.
     - Unlocked `Summon` inside `GMFindTreeWnd` -> `Skill Search` (`bSummon = true`) and offset skill IDs by `+ 2000000` so `//summon` handles Items (`< 1,000,000`), NPCs (`1,000,000..1,999,999`), and Skills (`>= 2,000,000`).
     - Wired `Alt+G` (`GMWnd.HandleShowGMWnd`) to auto-execute `//gmw`, wired `Administrator` (`ChangeServerButton`) to `//admin`, and prevented untargeting from wiping `GMWnd.EditBox`.
   - Upgraded the L2J Mobius CT 2.4 server across 6 feature branches (all merged into `master`):
     1. `feature/instance-infrastructure-pipeline` — 5-Pillar Core Instance Infrastructure Pipeline
     2. `feature/netcode-skill-queue-lag-tolerance` — Retail Skill Queue (`Ctrl`/NextAction) & Dynamic Lag Tolerance
     3. `feature/java27-ffm-geoengine-and-movement` — Project Panama FFM Off-Heap GeoEngine & Lock-Free Movement Engine
     4. `feature/zero-downtime-hot-reload-engine` — Transactional Child-First Script Hot-Reload Engine
     5. `feature/admin-community-board-rbac` — Java 27 Composable RBAC (`AdminScope`) & 2009 Client-Safe Admin Console
     6. `feature/native-gmwnd-console` — Native C++ `GMWnd` (`Alt+G`) Master Console & Tri-Modal `//summon` Integration

---

## 2. Client `system/` Reverse-Engineering & Encryption Architecture

### 2.1 Dual-Binary Startup & GameGuard Neutralization
- **`L2.bin` (`2,229,208` bytes, Aug 20, 2010):** Original retail binary.
- **`l2.exe` (`434,176` bytes, May 28, 2010) + `dsetup.dll` (`8,704` bytes, Oct 31, 2009):** Unpacked private-server loader that neutralizes `GameGuard.des` and overrides the retail `Lineage2Ver413` RSA modulus with the standard modding keypair (`0x75b4d6de...`).
- **Mandatory Wine DLL Override:** `WINEDLLOVERRIDES="dsetup=n,b;..."` ensures Wine loads the local `system/dsetup.dll` before any builtin DLL.

### 2.2 File Encryption Formats (`Lineage2VerXXX`)
When `Core.dll` / `l2.exe` opens any file in `system/`, it inspects the first 28 bytes (UTF-16LE header `"Lineage2VerXXX"`). If no header is present, the file is read as raw unencrypted bytes!

| Header / Format | Algorithm & Key | Files in `system/` |
| :--- | :--- | :--- |
| **Raw (Unencrypted)** | Plain text or raw binary | `interface.xdat`, all `.ini` and `.int` files (decrypted by `launch-l2.sh`), `*.gly`, `predefinedshader.bin` |
| **`Lineage2Ver111`** | 28-byte UTF-16LE header + every payload byte XORed with `0xAC` | All 25 `.u` UnrealScript bytecode packages (`interface.u`, `Engine.u`, `Core.u`, etc.) |
| **`Lineage2Ver413`** | 28-byte UTF-16LE header + 128-byte RSA blocks (`e = 0x1d`, `mod = 0x75b4d6de...`) wrapping `[uint32 uncompressed_size]` + `zlib` stream | All 45 `.dat` client data tables (`itemname-e.dat`, `skillgrp.dat`, `npcgrp.dat`, etc.) |

#### Python Decryption Snippet for Any `Lineage2Ver111` or `Lineage2Ver413` File:
```python
import zlib

MOD_413 = int(
    "75b4d6de5c016544068a1acf125869f43d2e09fc55b8b1e289556daf9b875763"
    "5593446288b3653da1ce91c87bb1a5c18f16323495c55d7d72c0890a83f69bfd"
    "1fd9434eb1c02f3e4679edfa43309319070129c267c85604d87bb65bae205de3"
    "707af1d2108881abb567c3b3d069ae67c3a4c6a3aa93d26413d4c66094ae2039", 16
)
EXP_413 = 0x1D

def decrypt_l2_file(data: bytes) -> bytes:
    hdr = data[:28].decode("utf-16le", errors="ignore")
    if hdr == "Lineage2Ver111":
        return bytes(b ^ 0xAC for b in data[28:])
    if hdr == "Lineage2Ver413":
        payload = data[28:]
        out = bytearray()
        for i in range(len(payload) // 128):
            blk = payload[i*128:(i+1)*128]
            dec = pow(int.from_bytes(blk, "big"), EXP_413, MOD_413).to_bytes(128, "big")
            sz = dec[3]
            if sz <= 124:
                pad = (124 - sz) % 4
                out.extend(dec[128 - sz - pad : 128 - pad])
        return zlib.decompress(bytes(out[4:]))
    return data
```

### 2.3 Complete Catalog of the 45 Client `.dat` Data Tables
All 45 `.dat` files in `system/` begin with a `uint32` row count after decompression and map directly to server-side IDs in `L2J_Mobius_CT_2.4_Epilogue/dist/game/data/`:

| Category | `.dat` File | Rows | Decompressed Size | Purpose & Server Counterpart |
| :--- | :--- | :---: | :---: | :--- |
| **Items & Gear** | `itemname-e.dat` | 16,801 | 3.64 MB | Item names, sub-names, descriptions, set bonuses (`data/stats/items/*.xml`) |
| | `weapongrp.dat` | 3,553 | 4.05 MB | Weapon meshes, textures, icons, grade, soulshot counts, glow offsets |
| | `armorgrp.dat` | 3,046 | 7.97 MB | Per-race armor meshes, textures, icons, P.Def/M.Def/MP display |
| | `etcitemgrp.dat` | 10,202 | 4.01 MB | Consumables, scrolls, quest items, materials, icons |
| | `britemgrp.dat` / `productname-e.dat` | 16,801 / 1,074 | 1.01 MB / 155 KB | PrimeShop / Item Mall metadata |
| | `optiondata_client-e.dat` | 24,949 | 1.64 MB | Augmentation / Life Stone tooltip strings (`data/stats/augmentation/`) |
| | `variationeffectgrp-e.dat` | 352 | 25 KB | Augmentation visual particle effects |
| | `hennagrp-e.dat` | 180 | 16 KB | Dye/Henna IDs, icons, stat modifiers (`data/stats/henna.xml`) |
| | `recipe-c.dat` | 935 | 100 KB | Dwarven & Common crafting ingredient lists (`data/recipes.xml`) |
| **Skills & FX** | `skillgrp.dat` | 60,101 | 7.21 MB | Skill ID + Level (incl. enchant routes `101..330`), MP cost, range, cast time, icon (`data/stats/skills/*.xml`) |
| | `skillname-e.dat` | 60,101 | 11.43 MB | Skill names, descriptions, and enchant route descriptions |
| | `skillsoundgrp.dat` | 4,086 | 2.72 MB | Cast/shot/explosion sound effects and vocal grunts |
| **NPCs & Raids** | `npcgrp.dat` | 10,829 | 5.21 MB | NPC/Monster mesh, textures, collision radius/height, speed, skills (`data/stats/npcs/*.xml`) |
| | `npcname-e.dat` | 10,823 | 692 KB | NPC names, titles, and title colors (`GMFindTreeWnd` NPC search source) |
| | `raiddata-e.dat` | 240 | 51 KB | Raid Boss map pins, levels, descriptions |
| | `transformdata.dat` | 126 | 42 KB | Transformation IDs, meshes, combat animations |
| **Quests & Zones** | `questname-e.dat` | 2,135 | 1.67 MB | Quest journal titles, step descriptions, map pins, required items |
| | `zonename-e.dat` | 360 | 74 KB | Screen-center zone entry banners and town map bounds |
| | `huntingzone-e.dat` | 189 | 21 KB | World Map "Hunting Zones" level brackets and coordinates |
| | `instantzonedata-e.dat` | 53 | 4.8 KB | Instance dungeon names (`Kamaloka`, `Dark Cloud Mansion`, `Crystal Caverns`, `Tower of Naia`, etc.) |
| | `teleportlist-e.dat` | 60 | 3.7 KB | World Map teleport points |
| **UI & Strings** | `sysstring-e.dat` | 2,213 | 124 KB | Client UI button labels and static strings (e.g., `415 = Administrator`, `417 = Summon`) |
| | `systemmsg-e.dat` | 2,994 | 540 KB | System chat messages (`SystemMessageId.java`), RGBA colors, screen-popup flags |
| | `commandname-e.dat` | 116 | 4.2 KB | `/command` user slash commands (`/instancezone`, `/olympiadstat`, etc.) |

---

## 3. Linux Launcher (`launch-l2.sh`) & Engine DLL Binary Patches

### 3.1 Quick Start CLI Commands (`launch-l2.sh`)
```bash
# Pre-flight check (verifies fonts, DLL patches, plain-text configs, and local server ports 2106/7777):
./launch-l2.sh --check

# Launch in 1920x1080 Framed Resizable Windowed mode (with titlebar, drag-resize & maximize button):
./launch-l2.sh --windowed --res 1920x1080 -server 127.0.0.1

# Launch in Borderless Windowed mode at (0,0) with instant auto-login into character slot 0:
./launch-l2.sh --borderless --res 1920x1080 --autologin dwea 123 0
```

### 3.2 Native Engine DLL Patches Applied Automatically by `launch-l2.sh`
1. **`system/d3ddrv.dll` (Backup: `system/backup_encrypted/d3ddrv.dll.orig`):**
   - **Raw Offset `0xC2494` (`74 22 -> 90 90`):** Silences the obsolete 2004 AGP memory warning popup on modern PCIe/DXVK GPUs.
2. **`system/windrv.dll` (Backup: `system/backup_encrypted/windrv.dll.orig`):**
   - **Patch 1a (`0x10FAF`):** Skips broken `SIZE_MAXIMIZED` cursor-trap logic in `UWindowsViewport::ViewportWndProc`.
   - **Patch 1b (`0x11141`):** Forces `Fullscreen=0` on windowed `WM_SIZE` events and marks `[esi+0x234]=4` so dragging the window border dynamically resizes the 3D D3D9 viewport instead of switching to exclusive fullscreen.
   - **Patch 2 (`0x0D43A`):** Skips redundant `MoveWindow` inside `ResizeViewport` when triggered by `WM_SIZE`.
   - **Patch 3 (`0x0D4B9`):** Prevents asynchronous XWayland `GetClientRect` calls from overwriting the requested `SizeX`/`SizeY` during startup.
   - **Patch 4 (`0x0E865`):** Spawns borderless (`WS_POPUP`) and framed windows at `PosX=0, PosY=0` instead of `CW_USEDEFAULT` (`0x80000000`).

---

## 4. Unified Native C++ `GMWnd` (`Alt+G`) Master Console Architecture

### 4.1 Why Native C++ `GMWnd` + `GMWnd.HtmlViewer` Is the Ultimate Admin Architecture
Reverse engineering `NWindow.dll` (`10.2 MB`) and `interface.u` (`4.1 MB`) revealed that `GMWnd` (`Alt+G`) has two complementary layers:
1. **Native C++ Binary Packet Inspectors:** Clicking `View Status`, `View Items`, `View Skills`, `View Quests`, `Recall`, or `View Warehouse` in `GMWnd` sends `RequestGMCommand` (`0x7E`), and the server responds with `GMView*` binary packets (`0x95..0x9B`) that open real native C++ drag-and-drop inventory grids, skill trees, quest logs, and radial stat gauges (`GMDetailStatusWnd`, `GMInventoryWnd`, `GMSkillWnd`, `GMQuestWnd`, `GMHennaInfoWnd`, `GMWarehouseWnd`).
2. **Client-Side `.dat` Search Trees (`GMFindTreeWnd`):** Clicking `Item Search`, `NPC Search`, or `Skill Search` scans all 16,801 items, 10,823 NPCs, and 60,101 skills locally in C++ memory with zero network lag.
3. **Embedded Right-Hand `GMWnd.HtmlViewer` (`744 x 492` px):** By expanding `GMWnd` to `992 x 544` px and embedding an `NCHtmlViewer` (`GMWnd.HtmlViewer`) on the right side (`x=236, y=38`), our entire Java 27 **RBAC Admin Control Center** renders directly inside `Alt+G` alongside the native C++ controls!

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│ [GM] Game Master Window (GMWnd — 992 x 544 px, opened via Alt+G or //gmw)                   [X] │
├───────────────────────────┬─────────────────────────────────────────────────────────────────────┤
│ [ Target / Input Box    ] │ [GM: Name | Master (100)]     [Invis][Invul][Spd2x][To Alt+B][Close]│
│                           ├─────────────────────────────────────────────────────────────────────┤
│  -- NATIVE C++ TOOLS --   │ [Dashboard] [Support] [World & Spawns] [Events] [Economy] [Server]  │
│ [Stop    ] [7Signs Sky  ] ├─────────────────────────────────────────────────────────────────────┤
│ [Play    ] [Administrator│ Target: [Name (Lv.85)] HP: 9999/9999  [$qbox        ] [Goto][Heal]  │
│ [Status  ] [View Items  ] ├─────────────────────────────────────────────────────────────────────┤
│ [Skills  ] [View Quests ] │                                                                     │
│ [Recall  ] [Warehouse   ] │              GMWnd.HtmlViewer (744 x 492 px)                        │
│ [ItemFind] [NPC Search  ] │                                                                     │
│ [SkillFnd] [Poll/Peti   ] │   - Live RBAC-filtered workspaces (Dashboard, Support, World,       │
│ [Help    ] [Close       ] │     Events, Economy, Zero-Downtime Script Hot-Reload Console)       │
│ [  Delete NPC/Item      ] │   - Automatically captures & scrubs all legacy //admin sub-menus    │
│                           │   - Zero PageLock, zero popup windows, 100% isolated from BoardWnd  │
│  Target/Input above drives│                                                                     │
│  all 6 C++ Inspector Wnds │                                                                     │
│  & 3 .dat Search Trees    │                                                                     │
└───────────────────────────┴─────────────────────────────────────────────────────────────────────┘
```

### 4.2 `system/interface.xdat` Layout Specification (`patch_gmwnd_client.py`)
- **Backups:** `system/backup_encrypted/interface.xdat.orig` and `system/interface.xdat.bak` (`1,319,629` bytes).
- **Patched `system/interface.xdat` (`1,320,012` bytes):**
  - `GMWnd` frame resized to `992 x 544` px (`backTex = L2UI_CH3.ChattingWnd.chatting_back1`, `movable = 1`).
  - Left Control Dock (`x=10..226`):
    - `GMWnd.EditBox` (`210 x 17` at `x=12, y=38`)
    - 16 standard buttons arranged in 2 columns of `105 x 26` px (`Col 1: x=10`, `Col 2: x=120`, rows `y=66..282`, keeping the original `105px` width so no English button text ever clips).
    - `ChangeServerButton` label updated from `sysstring[1549]` (`Switch Server`) to `sysstring[415]` (`Administrator`).
    - `Command4Button` (`Delete`) spans `215 x 26` at `x=10, y=310`.
    - `TextBox` helper note positioned below `Command4Button` at `x=12, y=344`.
  - Right Console Viewport (`x=232..984`):
    - `GMWnd.texBack` (`Texture` `GroupBox_DF`, `752 x 500` at `x=232, y=34`).
    - `GMWnd.HtmlViewer` (`HtmlCtrl` / `NCHtmlViewer`, `744 x 492` at `x=236, y=38`).

### 4.3 `system/interface.u` Zero-Byte-Delta UnrealScript Patches (`patch_gmwnd_client.py`)
- **Backups:** `system/backup_encrypted/interface.u.orig` and `system/interface.u.bak` (`4,128,791` bytes).
- **Patched `system/interface.u` (`4,128,791` bytes — 0-byte size delta):**
  Every function patch was verified with a full `Core.dll` `UStruct::Serialize` (`0x15065f30`) / `SerializeExpr` (`0x15064b40`) simulator to ensure both disk byte length and in-memory `ScriptSize` (where compact-index object/name references expand to 4 bytes) match `Core.dll` invariants:

| Export # | Function | Disk Code Len | Mem `ScriptSize` | Modification |
| :---: | :--- | :---: | :---: | :--- |
| `#13529` | `GMFindTreeWnd.ShowSkillList` | `852` B | `985` B | Changed `bSummon = false` (`0x28`) to `bSummon = true` (`0x27`) so `GMFindTreeWnd` -> `Skill Search` enables the `Summon` button & quantity input (`Count` = skill level). |
| `#13526` | `GMFindTreeWnd.FindAllSkill` | `462` B | `539` B | Changed `string(cID.ClassID)` to `string(cID.ClassID + 2000000)` (`0x53 0x92 <ClassID> 0x1D 0x00841E00 0x16`) so `//summon` receives `skillId + 2000000`. |
| `#12137` | `GMWnd.HandleTargetUpdate` | `134` B | `162` B | Changed `if (m_nowTargetID > 0)` (`0x97`) to `if (m_nowTargetID < 1) return;` (`0x96 ... 0x04 0x0B`) so clicking the ground / untargeting does **not** wipe text typed in `GMWnd.EditBox`. |
| `#12081` | `GMWnd.OnClickChangeServerButton` | `124` B | `122` B | Replaced dead `"//ce"` debug toggle with `ExecuteCommand("//admin"); return;`. |
| `#12139` | `GMWnd.HandleShowGMWnd` | `102` B | `118` B | Added `ExecuteCommand("//gmw");` after `m_hGMWnd.ShowWindow()` so pressing `Alt+G` immediately populates `GMWnd.HtmlViewer`. |
| `#12029` | `ItemDescWnd.OnLoad` | `119` B | `129` B | Bound `m_hItemDescWnd = GetWindowHandle("GMWnd")` and `m_hHtmlViewer = GetHtmlHandle("GMWnd.HtmlViewer")`. |
| `#12028` | `ItemDescWnd.OnEvent` | `85` B | `101` B | Gated `EV_ItemDescWndLoadHtmlFromString` (`2610`) with `if (!IsBuilderPC()) return;` so server-sent `new NpcHtmlMessage(0, 1, html)` loads directly into `GMWnd.HtmlViewer` with zero `PageLock` and leaves `NPCDialogWnd` (`itemId=0`) and `BoardWnd` (`Alt+B`) 100% independent. |

To re-verify or re-apply the client patches at any time:
```bash
python3 "Client Lineage 2 Gracia Epilogue/patch_gmwnd_client.py"
```

---

## 5. Server Architecture Summary (`l2jmobius_dwea_fork`)

All 6 major architectural branches have been verified, pushed to `origin`, and merged into `master`:

1. **5-Pillar Core Instance Infrastructure Pipeline (`feature/instance-infrastructure-pipeline`, doc: `PROJECT_MASTER_CONTEXT_AND_ROADMAP.md`):**
   - Offline SQL batch evacuation (`Instance.java`) + `EnterWorld` Instance 0 sentinel.
   - Party slot reservation & 5-minute disconnect grace period (`Party.java`).
   - Re-entry coordinate tracking & `getResumeLocation` hook (`InstanceWorld.java`, `Instance.java`).
   - Scoped instance timers (`InstanceWorld.scheduleTimer`) preventing cross-instance `QuestTimer` leaks.
   - Chained `EventDispatcher` listener verification for shared NPCs (NPC `32370` in Tully/Naia).
2. **Retail Skill Queue & Dynamic Lag Tolerance (`feature/netcode-skill-queue-lag-tolerance`, doc: `SKILL_QUEUE_AND_LAG_TOLERANCE.md`):**
   - Fixed the `Ctrl`-force-cast skill drop bug in `Player.useMagic`, enabled mid-movement skill queuing, and added RTT-adaptive cast-bar boundary tolerance (`120ms..250ms`) via `NetPing`.
3. **Java 27 Project Panama FFM Off-Heap GeoEngine & Lock-Free Movement (`feature/java27-ffm-geoengine-and-movement`, doc: `GEOENGINE_AND_MOVEMENT_ARCHITECTURE.md`):**
   - Zero-GC off-heap `Arena.ofShared()` `MemorySegment` geodata storage, $O(1)$ multilayer cell offset indexing, fused `getNearestZAndNswe` (80% fewer lookups per A* node), Virtual-Thread-safe CAS `NodeBuffer` pool, and lock-free Fibonacci-sharded `MovementTaskManager`.
4. **Zero-Downtime Transactional Script Hot-Reload Engine (`feature/zero-downtime-hot-reload-engine`, doc: `HOT_RELOAD_ENGINE_ARCHITECTURE.md`):**
   - Disposable child-first `ScriptClassLoader` with Java 27 `ClassFile` validation, compile-before-unload transaction safety, live init-spawned NPC adoption (`_handoverSpawns`), `< 2ms` in-memory skill `AbstractEffect` hot-swap, and ~29ms single-file reload (`//reload script <name|path>`).
5. **Java 27 Composable RBAC (`AdminScope`) & 2009 Client-Safe Admin Console (`feature/admin-community-board-rbac`, doc: `ADMIN_COMMUNITY_BOARD_RBAC_ARCHITECTURE.md`):**
   - 13 composable `AdminScope` capabilities across 8 staff tiers (`10..100`) with 100% regular player isolation, `ShowBoard` 2009 client crash fixes, dynamic HTML button/link RBAC scrubber, and consolidation of 80 -> 69 admin handlers.
6. **Native C++ `GMWnd` (`Alt+G`) Master Console Integration (`feature/native-gmwnd-console`):**
   - [`AdminBoardManager.java`](L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/managers/AdminBoardManager.java): Automatically delivers the wide-screen RBAC console into `GMWnd.HtmlViewer` via `new NpcHtmlMessage(0, 1, finalHtml)` for `allowAltG()` roles (with 1-click `[To Alt+B]` / `[To Alt+G]` switching in the Top Command Deck).
   - [`AdminAdmin.java`](L2J_Mobius_CT_2.4_Epilogue/dist/game/data/scripts/handlers/chat/commands/admin/AdminAdmin.java), [`AdminScope.java`](L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/data/enums/AdminScope.java), [`AdminCommands.xml`](L2J_Mobius_CT_2.4_Epilogue/dist/game/config/AdminCommands.xml): Registered `admin_gmw` (`//gmw`) under `PANEL_ACCESS`.
   - [`AdminSummon.java`](L2J_Mobius_CT_2.4_Epilogue/dist/game/data/scripts/handlers/chat/commands/admin/AdminSummon.java) & [`AdminSkill.java`](L2J_Mobius_CT_2.4_Epilogue/dist/game/data/scripts/handlers/chat/commands/admin/AdminSkill.java): Tri-modal `//summon` handler supporting Items (`< 1,000,000` -> `admin_create_item`), NPCs (`1,000,000..1,999,999` -> `admin_spawn_once`), and Skills (`>= 2,000,000` -> `admin_add_skill` with automatic fallback to `activeChar` when no player is targeted).

---

## 6. Prompt for Resuming on Your Work Machine

Copy and paste the following prompt to your AI coding agent at work:

> *"I have cloned both our server repository (`l2jmobius_dwea_fork`, branch `master`) and our Gracia Epilogue client repository. Please read `MASTER_HANDOFF_AND_CLIENT_SERVER_GUIDE.md` and `PROJECT_MASTER_CONTEXT_AND_ROADMAP.md` first. They document our complete 6-pillar Java 27 server architecture, our Linux Wine/DXVK client launcher (`launch-l2.sh`), our `d3ddrv.dll` / `windrv.dll` binary patches, and our Unified Native C++ `GMWnd` (`Alt+G`) Master Console (`interface.xdat` + `interface.u` + `AdminBoardManager.java`). Let's review Section 8 of `PROJECT_MASTER_CONTEXT_AND_ROADMAP.md` (Task B: 5-Phase Expansion Gatekeeper & Dynamic 1–75 / 76–85 Progression Engine, and Task C: Endgame Instance & Hellbound Script Modernization) and begin implementation."*
