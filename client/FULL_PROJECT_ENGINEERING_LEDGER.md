# Lineage 2 Gracia Epilogue (`CT2.4`) — Full Project Context & Engineering Ledger

**Repository:** `dwea0935/l2jmobius_dwea_fork.git` (`master` + 7 dedicated feature/client branches)  
**Server Workspace:** [`/home/dwea/Projects/L2/l2jmobius_dwea_fork`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork)  
**Client Workspace:** [`/home/dwea/Projects/L2/Client Lineage 2 Gracia Epilogue`](file:///home/dwea/Projects/L2/Client%20Lineage%202%20Gracia%20Epilogue) (and version-controlled [`client/system/`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/client/system))  
**Target Runtime:** OpenJDK 27 (`javac --release 27`, Generational ZGC, Project Panama FFM, Project Loom Virtual Threads) + Unreal Engine 2.5 (`PE32` + 32-bit DXVK Vulkan)

---

## 1. Index of Persistent Project Documentation (Committed to `master`)

To keep project context durable across machines and future AI sessions, the repository maintains **8 specialized markdown documents** at the root of [`l2jmobius_dwea_fork`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork):

| Document | Scope & Purpose |
| :--- | :--- |
| 1. [`FULL_PROJECT_ENGINEERING_LEDGER.md`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/FULL_PROJECT_ENGINEERING_LEDGER.md) | **This Master Index:** Chronological ledger of every milestone, branch, commit, binary patch, and server subsystem built since project inception. |
| 2. [`MASTER_HANDOFF_AND_CLIENT_SERVER_GUIDE.md`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/MASTER_HANDOFF_AND_CLIENT_SERVER_GUIDE.md) | **Unified Client + Server Handoff Guide:** Consolidates work from both autonomous agents, including `GMWnd` (`Alt+G`) C++ console architecture, `interface.xdat`/`interface.u` patches, `.dat` catalog, and work-PC agent prompt. |
| 3. [`CLIENT_MASTER_ARCHITECTURE_AND_GUIDE.md`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/CLIENT_MASTER_ARCHITECTURE_AND_GUIDE.md) | **Client Reverse-Engineering & Linux Guide:** Deep dive into `Lineage2Ver111`/`413` encryption, `d3ddrv.dll` & `windrv.dll` x86 patches, `launch-l2.sh`, DXVK Vulkan, TrueType fonts, and `JOYPAD` controller bindings. |
| 4. [`PROJECT_MASTER_CONTEXT_AND_ROADMAP.md`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/PROJECT_MASTER_CONTEXT_AND_ROADMAP.md) | **Server Vision, 5-Pillar Instance Pipeline & Roadmap:** Server progression philosophy, Crystal Caverns / Hellbound / Tully / Naia forensics, the 5-Pillar Instance Pipeline, and next queued tasks (Tasks B & C). |
| 5. [`SKILL_QUEUE_AND_LAG_TOLERANCE.md`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/SKILL_QUEUE_AND_LAG_TOLERANCE.md) | **Combat Netcode & Input Buffering:** Retail skill queue (`_queuedSkill`), cooldown-boundary tolerance (`_pendingReuseSkill`), `Ctrl` force-cast fix, cached `NetPing` RTT, and `.queue` / `.latency` player commands. |
| 6. [`GEOENGINE_AND_MOVEMENT_ARCHITECTURE.md`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/GEOENGINE_AND_MOVEMENT_ARCHITECTURE.md) | **Java 27 Panama FFM GeoEngine & Movement:** Off-heap `MemorySegment` geodata (~15M heap objects eliminated), $O(1)$ multilayer cell offsets, fused `getNearestZAndNswe`, CAS `NodeBuffer` pool, and lock-free `MovementTaskManager`. |
| 7. [`HOT_RELOAD_ENGINE_ARCHITECTURE.md`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/HOT_RELOAD_ENGINE_ARCHITECTURE.md) | **Zero-Downtime Script Hot-Reload Engine:** Child-first disposable `ScriptClassLoader`, Java 27 `ClassFile` bytecode validation, compile-before-unload transaction safety, live NPC adoption (`_handoverSpawns`), and `< 2ms` skill effect hot-swap. |
| 8. [`ADMIN_COMMUNITY_BOARD_RBAC_ARCHITECTURE.md`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/ADMIN_COMMUNITY_BOARD_RBAC_ARCHITECTURE.md) | **Java 27 RBAC & Admin Console:** 13 composable `AdminScope` capabilities across 8 GM tiers (`10..100`), 2009 client HTML 3.2 crash fixes, automatic `NpcHtmlMessage` RBAC scrubber, and purge/consolidation of 11 legacy handlers (`80 -> 69`). |
| 9. [`LESSONS_LEARNED_AND_FORENSICS.md`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/LESSONS_LEARNED_AND_FORENSICS.md) | **Reverse-Engineering & Forensics Dossier (Sections 1–10):** Deep-dive post-mortems on `[AutoLogOn]`, CLI `WinMain` stripping, `Option.ini` `FirstRun < 2110` trap, `00010084 1289` crash, Protocol `148` vs `152`, PE `.reloc` rebasing trap, DualSense controller quirks, `///buildzone` geodata builder, player skill/channelizer backports, 3rd-prof passives/triggers, and custom CB/config audits. |
| 10. [`AGENT_SYNC_AND_RECONCILIATION.md`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/AGENT_SYNC_AND_RECONCILIATION.md) | **Multi-Agent Reconciliation & Ownership Ledger:** Tracks the 4-Agent engineering topology (Server, Client, DevOps, Auditor), cross-repo synchronization status, and automated audit routines (`tools/auditor/l2_auditor.py`). |
| 11. [`ADMIN_CB_COUPLING_AUDIT.md`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/ADMIN_CB_COUPLING_AUDIT.md) | **Admin Console ↔ Community Board Decoupling Spec:** Audit and verification checklist for separating the player Community Board (`Alt+B`) from the native C++ `GMWnd` (`Alt+G`) Admin Console. |

---

## 2. End-to-End Architecture & Branch Topology

```mermaid
flowchart TD
    subgraph Client["Gracia Epilogue Client (Unreal Engine 2.5 + 32-bit DXVK Vulkan)"]
        L2SH["launch-l2.sh\n(PyQt6 Command Center, Plain-text .ini/.int, Fonts, DXVK)"]
        DLL["Engine & UI DLL Patches (2009-09-28 Clean Protocol 148)\n- l2.exe (Mutex bypass + Retail RSA-1024 97df3984 key)\n- Engine.dll (GameGuard bypass + ///buildzone unlocked)\n- nwindow.dll (Unicode U+00A0..U+04FF + 4-Stage PIC AutoLogOn)\n- d3ddrv.dll (AGP bypass) & windrv.dll (Dynamic WM_SIZE, Borderless 0,0)"]
        GMWND["Native C++ GMWnd v2 (Alt+G, 992x544)\n- interface.xdat (Button_DF click feedback, 3-section dock + 744x492 HtmlViewer)\n- interface.u (0-byte-delta bytecode patches)"]
    end

    subgraph Server["L2J Mobius CT 2.4 Server (OpenJDK 27 + Virtual Threads + Panama FFM)"]
        P0["Milestone 1: Upstream Rebase & Java 27 Runtime\n(Custom CB Buffer, Cleanse, Virtual Threads, build.py)"]
        P1["Milestone 2: 5-Pillar Instance Infrastructure\n(feature/instance-infrastructure-pipeline)"]
        P2["Milestone 3: Skill Queue & Lag Tolerance\n(feature/netcode-skill-queue-lag-tolerance)"]
        P3["Milestone 4: Panama FFM GeoEngine & Movement\n(feature/java27-ffm-geoengine-and-movement)"]
        P4["Milestone 5: Zero-Downtime Hot-Reload Engine\n(feature/zero-downtime-hot-reload-engine)"]
        P5["Milestone 6: Composable RBAC & Admin Console\n(feature/admin-community-board-rbac)"]
        P6["Milestones 7-11: Native GMWnd v2 & Dual-Protocol (148/152)\n(Complete CB Decoupling + Tri-Modal //summon)"]
        P7["Milestones 12-15: Unicode/Naming, AutoLogOn, 196-Region Geodata,\nCustom CB 70 Buffs, 2h Buffs & Full CT2.4 Player Skills Audit"]
    end

    L2SH --> DLL --> GMWND
    GMWND <-->|"RequestGMCommand (0x7E) / GMView*\nNpcHtmlMessage(0, 1, html) -> GMWnd.HtmlViewer\n//summon (Item <1M, NPC 1M..2M, Skill >=2M)"| P6
    P0 --> P1 --> P2 --> P3 --> P4 --> P5 --> P6 --> P7
```

---

## 3. Chronological Engineering Ledger (All 15 Milestones)

### Milestone 1: Upstream Rebase, Chronicle Pruning & Java 27 Upgrade
* **Commits:** `074d3d6a33c`, `b4269b6fc70`, `bf4067a3553`
* **Key Accomplishments:**
  1. **Rebased Custom Epilogue Features onto Upstream Mobius (`074d3d6a33c`):** Preserved custom Community Board Buffer UI (`data/html/CommunityBoard/Custom/buffer/*.html`), `.cleanse`, and Premium Buff integration in [`HomeBoard.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/dist/game/data/scripts/handlers/bypass/communityboard/HomeBoard.java) and [`PremiumManager.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/managers/PremiumManager.java).
  2. **Single-Chronicle Focus (`b4269b6fc70`):** Removed non-Epilogue chronicle folders from the repository so the workspace exclusively targets `L2J_Mobius_CT_2.4_Epilogue`.
  3. **Java 27 & Project Loom Virtual Threads (`bf4067a3553`):**
     * Upgraded [`build.xml`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/build.xml) and added [`build.py`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/build.py) targeting `javac --release 27`.
     * Added `ThreadPool.executeVirtual(...)` (`Executors.newVirtualThreadPerTaskExecutor()`) in [`ThreadPool.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/commons/threads/ThreadPool.java) for non-blocking asynchronous database and I/O tasks.

---

### Milestone 2: The 5-Pillar Core Instance Infrastructure Pipeline
* **Branch:** `origin/feature/instance-infrastructure-pipeline` (Merged to `master`, commits `b3488df54f5`, `09dcbd9a178`)
* **Problem Solved:** Stock L2J Mobius suffered from catastrophic dungeon state loss whenever a player crashed or disconnected inside an instance (duo parties immediately disbanded, disconnected players logged back into empty "Instance 0" behind locked doors, and static `QuestTimer` names collided across concurrent party instances).
* **5 Architectural Pillars Implemented:**
  1. **Pillar 1 — Offline SQL Batch Evacuation & Instance 0 Sentinel:**
     * [`Instance.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/entity/instancezone/Instance.java): When an instance expires or is destroyed, `removePlayers()` collects all offline player IDs and executes an async Virtual Thread SQL batch (`UPDATE characters SET x=?, y=?, z=? WHERE charId=?`) moving them to `_exitLocation`.
     * [`EnterWorld.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/network/clientpackets/EnterWorld.java): If a player logs in with `instanceId == 0` inside a `ZoneId.NO_RESTART` dungeon boundary, they are automatically evacuated to the nearest town.
  2. **Pillar 2 — Party Slot Reservation & Disconnect Grace Period:**
     * [`Party.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/entity/groups/Party.java): Added a `DisconnectedMember` state machine with a configurable 5-minute grace window (`GeneralConfig.INSTANCE_DISCONNECT_GRACE_PERIOD = 300` in [`General.ini`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/dist/game/config/General.ini)). A duo party no longer implodes when one player disconnects; reconnecting within 5 minutes seamlessly restores party membership, leadership, and Party UI windows (`PartySmallWindowAll`) **after** `player.spawnMe()` in `EnterWorld.java`.
  3. **Pillar 3 — Re-Entry Coordinate Tracking & `getResumeLocation` Hook:**
     * [`InstanceWorld.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/entity/instancezone/InstanceWorld.java) & [`InstanceScript.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/mechanics/script/InstanceScript.java): Records last valid inside-dungeon coordinates on disconnect and exposes `getResumeLocation(Player, InstanceWorld)` for multi-wing dungeons (`CrystalCaverns`, `DarkCloudMansion`).
  4. **Pillar 4 — Instance-Scoped Timer Engine:**
     * [`InstanceWorld.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/entity/instancezone/InstanceWorld.java): Added `scheduleTimer`, `scheduleRepeatingTimer`, and `cancelTimer` scoped strictly to the `InstanceWorld` lifecycle (`ConcurrentHashMap<String, ScheduledFuture<?>>`), automatically cancelling all tasks on `Instance.destroy()`.
  5. **Pillar 5 — Multi-Script `EventDispatcher` Chaining Verification:**
     * [`EventDispatcher.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/mechanics/events/EventDispatcher.java): Verified and documented that multiple scripts (`TullyWorkshop` and `TowerOfNaia`) can safely register `onFirstTalk` listeners on shared NPC IDs (`32370`).

---

### Milestone 3: Combat Netcode — Skill Queue & Dynamic Action Lag Tolerance
* **Branch:** `origin/feature/netcode-skill-queue-lag-tolerance` (Merged to `master`, commit `6c824e6faf8`, plus `ctrlPressed` fix in `db33430559f`)
* **Documentation:** [`SKILL_QUEUE_AND_LAG_TOLERANCE.md`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/SKILL_QUEUE_AND_LAG_TOLERANCE.md)
* **Key Accomplishments:**
  1. **Recast & Combo Buffering (`_queuedSkill` in [`Player.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/entity/actor/Player.java)):** Eliminated the recast lockout in `Player.useMagic()` so mages and fighters can buffer consecutive casts (including same-skill recasts and `Ctrl`-forced casts via `SkillUseHolder.isCtrlPressed()`) with 0ms dead air between animations.
  2. **Pre-Cooldown Expiration Scheduling (`_pendingReuseSkill` in [`Player.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/entity/actor/Player.java)):** If a player presses a skill within `queueWindow` (`50..500ms`) of its cooldown expiring, the server schedules execution on the exact millisecond the reuse timer finishes instead of dropping the input.
  3. **Mid-Movement Skill Queuing:** Allows buffering a spell while moving so it fires immediately on arrival.
  4. **Zero-Syscall RTT Tracking & `.queue` / `.latency` Voiced Commands:**
     * Added `NetPing` / `RequestNetPing` RTT measurement cached in `GameClient` (`player.getPing()`) and wired [`SkillQueue.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/dist/game/data/scripts/handlers/chat/commands/voiced/SkillQueue.java) (`.queue auto`, `.queue <ms>`, `.queue off`).
     * Tuned [`Network.ini`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/dist/game/config/Network.ini) (`UseNagle = False` / `TCP_NODELAY`, `128 KB` socket buffers).

---

### Milestone 4: Java 27 Project Panama FFM Off-Heap GeoEngine & Lock-Free Movement
* **Branch:** `origin/feature/java27-ffm-geoengine-and-movement` (Merged to `master`, commit `6b08afaa1d2`, plus dynamic mutation fix in `db33430559f`)
* **Documentation:** [`GEOENGINE_AND_MOVEMENT_ARCHITECTURE.md`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/GEOENGINE_AND_MOVEMENT_ARCHITECTURE.md)
* **Key Accomplishments:**
  1. **Zero-Copy Off-Heap `MemorySegment` Geodata ([`Region.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/geoengine/geodata/regions/Region.java), [`GeoEngine.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/geoengine/GeoEngine.java)):**
     * Replaced `IBlock[65536]` (~15,000,000 heap objects across 130 regions, ~1.5 GB heap) with a Project Panama `MemorySegment` (`Arena.ofAuto()`) and a packed `int[65536] _blockDescriptors` table (`DESC_FLAT`, `DESC_COMPLEX`, `DESC_MULTILAYER`, `DESC_MUTATED` Copy-on-Write for Doors/Fences).
     * Parallelized startup `.l2j` region loading across Java 27 Virtual Threads.
  2. **$O(1)$ Multilayer Cell Offset Indexing & Fused `getNearestZAndNswe` ([`MultilayerBlock.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/geoengine/geodata/blocks/MultilayerBlock.java), [`ComplexBlock.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/geoengine/geodata/blocks/ComplexBlock.java), [`NodeBuffer.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/geoengine/pathfinding/NodeBuffer.java)):**
     * Precomputed `_mlCellOffsets` / `_cellOffsets` tables (eliminating the $O(64)$ linear byte-scanning loop) and fused `getNearestZAndNswe(geoX, geoY, worldZ)` (cutting A* pathfinding `NodeBuffer.touch()` geodata lookups by **80%**, from 5 calls per node to 1).
  3. **Virtual-Thread-Safe Striped CAS `NodeBuffer` Pool ([`PathFinding.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/geoengine/pathfinding/PathFinding.java)):**
     * Replaced `ThreadLocal<NodeBuffer[]>` with a bounded striped `AtomicReferenceArray<NodeBuffer>[]` pool leased via lock-free `AtomicBoolean.compareAndSet`.
  4. **Lock-Free Fibonacci-Sharded `MovementTaskManager` ([`MovementTaskManager.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/taskmanagers/MovementTaskManager.java)):**
     * Removed global `synchronized` monitor contention and $O(P)$ `.contains()` scans, routing moving Players (`4` shards @ 50ms) and Creatures (`8` shards @ 100ms) in $O(1)$ lock-free time via Fibonacci hashing on `objectId`.

---

### Milestone 5: Zero-Downtime Transactional Script Hot-Reload Engine
* **Branch:** `origin/feature/zero-downtime-hot-reload-engine` (Merged to `master`, commit `40131d79438`)
* **Documentation:** [`HOT_RELOAD_ENGINE_ARCHITECTURE.md`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/HOT_RELOAD_ENGINE_ARCHITECTURE.md)
* **Key Accomplishments:**
  1. **Disposable Child-First `ScriptClassLoader` & Java 27 `ClassFile` Validation ([`ScriptClassLoader.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/scripting/engine/ScriptClassLoader.java), [`ScriptExecutor.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/scripting/engine/ScriptExecutor.java)):**
     * Fixed the upstream static `SCRIPT_CLASS_LOADER` bug where `findLoadedClass` always returned stale boot-time classes.
     * Validates compiled bytecode with `java.lang.classfile.ClassFile.of().parse(bytes)` and groups `$` inner/lambda nestmates (`JEP 181`) in the same disposable loader while reusing unchanged base classes (`AbstractNpcAI`) in $O(1)$ time.
  2. **Compile-Before-Unload Transaction & Live Init-Spawn NPC Adoption ([`ScriptManager.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/managers/ScriptManager.java), [`Quest.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/mechanics/script/Quest.java)):**
     * Syntax errors abort compilation *before* `prepareHotReload` unloads the live script.
     * Alive constructor-spawned open-world NPCs are staged in `_handoverSpawns` and adopted by `Quest.addSpawn` (`tryAdoptInitSpawn`), preserving HP, aggro, and combat state without duplicate spawns.
  3. **In-Memory Skill `AbstractEffect` Hot-Swap (`< 2ms`, Zero XML I/O):**
     * Reloading any `AbstractEffect` script (`//reload script Blink`) automatically calls `SkillData.getInstance().refreshEffectHandler(name)`, swapping live `AbstractEffect` instances across all loaded `Skill`s in `< 2ms`.
  4. **Surgical Single-File Reloading (`//reload script <name|path>`):**
     * Upgraded all 8 `*Handler` registries to `ConcurrentHashMap` and added standalone handler/effect auto-registration (~29ms single-file hot-swap).

---

### Milestone 6: Java 27 Composable RBAC (`AdminScope`), 2009 Client HTML Safety & Cross-Pillar Audit
* **Branch:** `origin/feature/admin-community-board-rbac` (Merged to `master`, commits `8a6534aefce`, `db33430559f`)
* **Documentation:** [`ADMIN_COMMUNITY_BOARD_RBAC_ARCHITECTURE.md`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/ADMIN_COMMUNITY_BOARD_RBAC_ARCHITECTURE.md)
* **Key Accomplishments:**
  1. **13 Composable `AdminScope` Capabilities ([`AdminScope.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/data/enums/AdminScope.java), [`AccessLevels.xml`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/dist/game/config/AccessLevels.xml)):**
     * Replaced flat integer checks with composable RBAC scopes across 8 functional staff roles (`10 Chat Moderator` through `100 Master`), while `User (0)` and `Banned (-1)` have `isGM="false"` and `scopes="NONE"`.
     * Synced `UserInfo.java` (`builderLevel = _player.isGM() && _player.getAccessLevel().allowAltG()`), `RequestGMCommand.java` (`PLAYER_SUPPORT` scope check), and `EnterWorld.java` (`EVENT_GM` scope check for GM special skills).
  2. **2009 Gracia Epilogue Client `ShowBoard` Crash Fixes ([`ShowBoard.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/network/serverpackets/ShowBoard.java), [`HtmlUtil.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/util/HtmlUtil.java)):**
     * Fixed `null` `"102\u0008null"` string corruption in `ShowBoard` and restored `4,090`-char per-packet chunking (`12,270` chars max across `101`/`102`/`103`) matching `NWindow.dll`'s buffer limits.
  3. **Dynamic `NpcHtmlMessage` Interceptor & RBAC HTML Scrubber ([`AdminBoardManager.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/managers/AdminBoardManager.java)):**
     * Built 6 wide-screen 740px workspaces (`Dashboard`, `Support & Players`, `World & Spawns`, `Events & GM`, `Items & Chars`, `Server & Reload`) with max 2-level table nesting, automatic legacy `NpcHtmlMessage` capture, unauthorized button/link scrubbing, and `qbox` `<edit>` deduplication.
  4. **Legacy Admin Handler Purge & Consolidation (`80 -> 69` Handlers):**
     * Deleted 5 broken/obsolete 2004 handlers (`AdminCamera`, `AdminFightCalculator`, `AdminPathNode`, `AdminTargetSay`, `AdminTest`) and merged 6 redundant handlers (`AdminDisconnect` $\rightarrow$ `AdminKick`, `AdminSuperHaste` $\rightarrow$ `AdminGmSpeed`, `AdminInstanceZone` $\rightarrow$ `AdminInstance`, `AdminPledge` $\rightarrow$ `AdminClan`, `AdminZoneBuild` $\rightarrow$ `AdminZone`, `AdminShowQuests` $\rightarrow$ `AdminQuest`).

---

### Milestone 7: Client Reverse-Engineering, Linux/DXVK Launcher & Unified Native C++ `GMWnd` (`Alt+G`) Master Console
* **Branches:** `origin/feature/native-gmwnd-console` & `origin/client/gracia-epilogue-system` (Merged to `master`, commits `9a029c459a8`..`9ddf57e6c12`)
* **Documentation:** [`MASTER_HANDOFF_AND_CLIENT_SERVER_GUIDE.md`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/MASTER_HANDOFF_AND_CLIENT_SERVER_GUIDE.md) & [`CLIENT_MASTER_ARCHITECTURE_AND_GUIDE.md`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/CLIENT_MASTER_ARCHITECTURE_AND_GUIDE.md)
* **Key Accomplishments (Multi-Agent Collaboration):**
  1. **100% Decrypted Plain-Text `.ini` & `.int` Configs + `launch-l2.sh` (Agent 2):**
     * Stripped `Lineage2Ver111`/`413` encryption from all 30 `.ini`/`.int` files in [`client/system/`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/client/system) (backups in [`client/system/backup_encrypted/`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/client/system/backup_encrypted)).
     * Created [`launch-l2.sh`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/client/launch-l2.sh) with 32-bit DXVK (`d3d9.dll` $\rightarrow$ Vulkan), TrueType UI fonts (`tahoma.ttf`, `times.ttf`, `arial.ttf`), `dsetup=n,b` override, CLI flags (`-server`, `-login`, `-pass`, `-slot`, `--res`, `--borderless`, `--windowed`, `--fullscreen`, `--gamepad`, `--check`), and modernized XInput/SDL2 `JOYPAD` bindings in `user.ini`.
  2. **Binary Engine DLL Patches (`d3ddrv.dll` & `windrv.dll` — Agent 2):**
     * [`d3ddrv.dll`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/client/system/d3ddrv.dll) (`0xC2494`/`0xC24A4`: `74 22` $\rightarrow$ `90 90`): Silenced obsolete 2004 AGP warning dialog.
     * [`windrv.dll`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/client/system/windrv.dll) (5 x86 patches): Enabled real-time drag window resizing (`WM_SIZE` $\rightarrow$ `SetRes`), OS Maximize button without cursor-trapping or forced exclusive fullscreen, XWayland async geometry safety, and `(0,0)` Borderless Windowed mode (`UseWindowFrame=False`).
  3. **Unified Native C++ `GMWnd` (`Alt+G`) Master Console ([`interface.xdat`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/client/system/interface.xdat), [`interface.u`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/client/system/interface.u), [`patch_gmwnd_client.py`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/client/patch_gmwnd_client.py), [`AdminBoardManager.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/managers/AdminBoardManager.java), [`AdminSummon.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/dist/game/data/scripts/handlers/chat/commands/admin/AdminSummon.java) — Agent 1):**
     * Expanded `GMWnd` (`Alt+G`) to `992 x 544` px with a 2-column native C++ left control dock (`x=10..225`) and an embedded right-hand `GMWnd.HtmlViewer` (`744 x 492` px).
     * Patched 7 UnrealScript functions in `interface.u` with 0-byte size delta so `NpcHtmlMessage(0, 1, html)` (`EV_ItemDescWndLoadHtmlFromString`) renders directly into `GMWnd.HtmlViewer` with zero `PageLock`, `Alt+G` auto-triggers `//gmw`, `Administrator` triggers `//admin`, untargeting preserves `GMWnd.EditBox`, and `GMFindTreeWnd` -> `Skill Search` enables `Summon` with `+ 2,000,000` skill ID offset handled by `AdminSummon.java` and `AdminSkill.java`.

---

### Milestone 8: Local Dockerized Test Environment & `build.py` Packaging Fixes (Agent 3)
* **Files:** [`docker-compose.yml`](file:///home/dwea/Projects/L2/docker-compose.yml), [`docker/run-server.sh`](file:///home/dwea/Projects/L2/docker/run-server.sh), [`docker/db-init/01-l2jmobius-schema.sh`](file:///home/dwea/Projects/L2/docker/db-init/01-l2jmobius-schema.sh), [`build.py`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/build.py), [`AGENT_SYNC_AND_RECONCILIATION.md`](file:///home/dwea/Projects/L2/AGENT_SYNC_AND_RECONCILIATION.md)
* **Key Accomplishments & Findings:**
  1. **3-Container Host-Networked Docker Stack (`l2-db`, `l2-login`, `l2-game`):**
     * `l2-db` (`mariadb:11.8` on `127.0.0.1:3306`) automatically initializes `l2jmobiusepilogue` with all **116 SQL tables** (`db_installer/sql/login/*.sql` + `sql/game/*.sql`) on first volume boot via `01-l2jmobius-schema.sh`.
     * `l2-login` (`0.0.0.0:2106`, GS link `127.0.0.1:9014`) and `l2-game` (`0.0.0.0:7777`, registered as **Server 2: Sieghardt**) run on `eclipse-temurin:27-jdk` with `-Djava.awt.headless=true` and `--enable-native-access=ALL-UNNAMED`.
  2. **Root-Cause Fix for `LoginServer.jar` Missing `gameserverpackets` (`InitLS`):**
     * Agent 3 discovered that [`build.py`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/build.py#L125-L127) used `exclude_patterns=["*databaseinstaller*", "*gameserver*"]` when building `LoginServer.jar`. Because Python's `fnmatch` `*` matches `/`, `*gameserver*` inadvertently stripped `org/l2jmobius/loginserver/network/gameserverpackets/**` (18 classes including `InitLS`), crashing the GameServer↔LoginServer handshake.
     * Fixed `build.py` to anchor the exclusion to `"org/l2jmobius/gameserver/*"`, repacked `LoginServer.jar` (`153 -> 171` entries), and restored the full `META-INF/MANIFEST.MF` (`Main-Class` & `Class-Path`) in `GameServer.jar`.

---

### Milestone 9: Client Login Crash (`00010084 1289`) Forensic Resolution & Clean `2009-09-28` (`L2 Epilogue Clean`) Unification
* **Commits:** `7636477796b`..`d35432adcb8`
* **Files:** [`client/system/l2.exe`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/client/system/l2.exe), [`client/system/Engine.dll`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/client/system/Engine.dll), [`client/system/Core.dll`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/client/system/Core.dll), [`client/system/windrv.dll`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/client/system/windrv.dll), [`client/system/d3ddrv.dll`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/client/system/d3ddrv.dll), [`client/system/dsetup.dll`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/client/system/dsetup.dll), [`client/system/interface.xdat`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/client/system/interface.xdat), [`client/system/interface.u`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/client/system/interface.u), [`client/launch-l2.sh`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/client/launch-l2.sh)
* **Root Causes & Resolution:**
  1. **Hybrid Binary / Vtable Mismatch (`2010-07-16` vs `2009-09-28`):** Replaced the mixed `2010-07-16` (`Protocol 152`) / `2009-09-28` (`Protocol 148`) binaries with the **100% unpacked, version-matched `2009-09-28` (`Protocol 148`) build** from `/home/dwea/Projects/L2/L2 Epilogue Clean/system/` (preserving backups in `system/backup_hybrid_152/` and removing `unichat.dll`).
  2. **`l2.exe` (`434,176` B) & `dsetup.dll` (`8,704` B) Patches:** Aliased `?GL2RunningFirstHandle@@3PAXA` $\rightarrow$ `?GIsServer@@3HA`, NOPed the mutex handle store at `0x25CEE` (`89 01` $\rightarrow$ `90 90`), updated the `Lineage2Ver413` RSA-1024 modulus (`0x2F500`) and exponent (`0x2F604`) to NCSoft's official retail key (`97df3984...`, `e = 0x35`), and rebased `dsetup.dll` to `0x12000000`.
  3. **`Engine.dll`, `d3ddrv.dll` & `windrv.dll` (`2009-09-28`) Patches:** Bypassed GameGuard in `Engine.dll` (`0x39B750`: `33 c0 fe c0 c3`), silenced the AGP warning in `d3ddrv.dll` (`0xC24A4`: `90 90`), and applied all 5 dynamic window-resize/borderless patches to `windrv.dll` (`+0x70` offsets: `0x1101F`, `0x111B1`, `0x0D4AA`, `0x0D529`, `0x0E8D5`).

---

### Milestone 10: `2009-09-28` `Engine.dll` 439-Handler Packet Alignment & Option C Dual-Protocol (`148` + `152`) Hardening
* **Commits:** `33205b9c184`, `e0eb703f9e6`, `7604d41d265`, `14933860a51`, `9419c4d7b5a`
* **Key Accomplishments:**
  1. **Protocol-Conditional Item Serialization (`70` B vs `76` B):** Gated the trailing 3× `short` `EnchantOptions` (`6` bytes) on `isProtocol152(client)` across [`AbstractItemPacket.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/network/serverpackets/AbstractItemPacket.java) and all 21 item/shop/warehouse/trade/pet/store/mail packet classes, eliminating the `NConsoleWindow.cpp:4212` `iWorkType` assertion crash on Protocol `148` clients while simultaneously supporting Protocol `152` clients (`AllowedProtocolRevisions = 148;152`).
  2. **Swapped Extended Opcode Branching (`0xFE:0xBD` vs `0xFE:0xBE`) & Broadcast-Cache Opt-Out (`isProtocolSensitive()`):** Updated `ExBrExtraUserInfo.java` and `ExBrBroadcastEventState.java` to branch on `isProtocol152(client)`, added `isProtocolSensitive()` and `_broadcastRequested` tripwire in [`ServerPacket.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/network/serverpackets/ServerPacket.java), ordered `sendSkillList()` before `ShortcutInit` in `EnterWorld.java` and `Player.java`, and added Protocol-148 `SystemMessageId.S1_2` (`$s1`) text fallback for IDs `3093..3207` in [`SystemMessage.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/network/serverpackets/SystemMessage.java).

---

### Milestone 11: Admin Console ↔ Community Board Full Decoupling & Native C++ `GMWnd` (`Alt+G`) Admin Panel v2
* **Commits:** `9973fd039af`, `a9bc33c79ff`, `3dc24796dcd`
* **Documentation:** [`ADMIN_CB_COUPLING_AUDIT.md`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/ADMIN_CB_COUPLING_AUDIT.md), [`AGENT_SYNC_AND_RECONCILIATION.md`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/AGENT_SYNC_AND_RECONCILIATION.md) (§4)
* **Key Accomplishments:**
  1. **100% Decoupling of Player Community Board (`Alt+B`):** Removed all GM banners, `tryInterceptAdminBoardHtml` hooks, content-sniffing in `Player.sendPacket`, and `ShowBoard` rendering paths from [`HomeBoard.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/dist/game/data/scripts/handlers/bypass/communityboard/HomeBoard.java), [`HtmlUtil.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/util/HtmlUtil.java), [`AdminBoardManager.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/managers/AdminBoardManager.java), and [`RequestBypassToServer.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/network/clientpackets/RequestBypassToServer.java). Restored strict `HtmlActionScope.NPC_ITEM_HTML` (`itemId=1`) action validation for GM console bypasses.
  2. **Native C++ `GMWnd` Admin Panel v2 (`patch_gmwnd_client.py`):**
     * Rebased [`patch_gmwnd_client.py`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/client/patch_gmwnd_client.py) onto clean `2009-09-28` `interface.u` (`4,061,011` B, 0-byte delta) and `interface.xdat` (`1,305,499` B).
     * Fixed NCSoft's broken button visual feedback across all 17 `GMWnd` buttons (`NormalTex=L2UI_CT1.Button.Button_DF`, `PushedTex=Button_DF_Click`, `HighlightTex=Button_DF_Over`, `NoHighlight=-1`).
     * Reorganized the left C++ dock into 3 framed sections (**1. Primary Console**: `Admin Home`, `Player Panel`, `Legacy Tools`, `Target Debug`, `Teleport To`, `Instant Move`; **2. `.dat` Search**: `Search Item`, `Search NPC`, `Search Skill`; **3. Legacy GM Inspector**: `View Status/Items/Skills/Quests/Warehouse/Clan`, `Recall Chr`, `Send Home`) and added `//gmw player [target]`, `//gmw legacy`, and `//gmw legacy_view <type> [target]` server endpoints.

---

### Milestone 12: International Unicode Chat/Mail/Title Input (`NWindow.dll`), PE `.reloc` Neutralization & English Naming Policy
* **Commits:** `360e662efb3`, `200253715b8`, `9175038c30f`, `2696ea9b508`
* **Files:** [`client/patch_nwindow_unicode.py`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/client/patch_nwindow_unicode.py), [`client/system/nwindow.dll`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/client/system/nwindow.dll), [`Server.ini`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/dist/game/config/Server.ini)
* **Key Accomplishments:**
  1. **Unlocked Wide Unicode Input (`U+00A0..U+04FF` excl. Combining Marks `U+0300..U+036F`) with `Language=0`:** Patched `NCEditBox::OnChar` (`0x000D38EE`), `NCMultiEditBox::OnChar` (`0x000A2868`), `NWndUtil::GetNativeStrLen` (`0x001D8726`), and `IsObsceneWord` (`0x002153BB`) in `NWindow.dll` so Latin-Extended, Greek, and full Cyrillic (`Ё/ё`, `Є/І/Ї/Ґ`, `Ђ/Ѓ/Ј/Љ/Њ/Ћ/Ќ/Џ`) work natively in chat, whispers, mail, titles, and boards while keeping English UI (`Language=0`).
  2. **PE `.reloc` Rebasing Trap Neutralization (`2696ea9b508`):** Zeroed the 3 `IMAGE_REL_BASED_HIGHLOW` entries (`0x004C80C2`, `0x004CAC1C`, `0x004E2024`) that previously pointed at the `GLanguage` global operand inside the patched instructions, preventing Wine/Windows DLL rebasing from corrupting the instructions at runtime.
  3. **Strict English-Only (`[A-Za-z0-9]`) Naming Policy (`360e662efb3`):** Enforced ASCII alphanumeric validation for character, pet, clan, sub-pledge, and alliance names on the server while preserving full UTF-16LE support for chat, titles, and mail.

---

### Milestone 13: 4-Stage One-Shot `[AutoLogOn]` PIC State Machine (`NWindow.dll`) & DualSense PS5 Controller Mapping
* **Commits:** `4b3ed650e09`, `293ec8acd1b`, `e678b53289b`, `1f152ce8abe`, `3ff1221b202`
* **Files:** [`client/patch_nwindow_unicode.py`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/client/patch_nwindow_unicode.py), [`client/system/nwindow.dll`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/client/system/nwindow.dll), [`client/system/user.ini`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/client/system/user.ini)
* **Key Accomplishments:**
  1. **100% Position-Independent (`PIC`) 4-Stage `[AutoLogOn]` State Machine in `NWindow.dll`:** Because retail Gracia Epilogue stripped the `[AutoLogOn]` parser from all binaries, implemented a 477-byte PIC code cave (`VA 0x102dbe20..0x102dbffd`) and `.data` state block (`VA 0x104ba960..0x104ba9e0`) hooking `NCAuthWnd::ShowWindow` (Stage 1), `NConsoleWnd::AcceptLogin` (Stage 2 EULA skip), `NCServerSelectWnd::EndServerInfo` (Stage 3 server select), and `NConsoleWnd::ReceiveCharacterInfo` / `AddCharacterInfo` (Stage 4 character lobby stop when `L2Slot` is empty/`-1`, or auto-enter world when `0 <= L2Slot < numChars`).
  2. **Sony DualSense PS5 Right-Stick & Trigger Axis Fix (`1f152ce8abe`):** Under Linux `hid-playstation` + Wine DirectInput, `JoyU`/`JoyR` (`Rx`/`Ry`) map to the L2/R2 analog triggers (resting at `-1.0`), whereas `JoyZ` (`Z`) and `JoyV` (`Rz`) are the Right Stick axes. Bound `JoyZ` (`CameraYaw`) and `JoyV` (`CameraPitch`) with `DeadZone=0.07` and unbound `;JoyU=` / `;JoyR=`.

---

### Milestone 14: GameServer Config Purge, Virtual-Thread Loader, Custom CB Buffer, FakePlayers, 196-Region Geodata & 2-Hour Buffs
* **Commits:** `4c29c75ef25`, `8aa3b62f4d2`, `afd3d5d84b6`
* **Key Accomplishments:**
  1. **Config Cleanup & Virtual-Thread XML Loader (`4c29c75ef25`):** Purged 13 dead config keys across `GeoEngine.ini`, `Server.ini`, `General.ini`, `NPC.ini`, `Feature.ini`, `ConquerableHallSiege.ini`, and `Pvp.ini`; replaced the busy-wait spin-loops and non-atomic counter race in `IXmlReader.java`, `ItemData.java`, and `SkillData.java` with `Executors.newVirtualThreadPerTaskExecutor()`.
  2. **Custom Community Board Buffer Completion & FakePlayers Fix (`4c29c75ef25`):** Added all 70 skills from `Custom/buffer/*.html` to `CommunityAvailableBuffs` in `CommunityBoard.ini`, fixed `HomeBoard.java` to validate buffs before deducting currency, fixed the `buffer/main-damces` typo in `main-dances.html`, wired `main-presets.html`, fixed the impossible boolean guard `(npcId < 80000) || (npcId > 89999)` in `SpawnData.java:164`, and enabled `FakePlayers`.
  3. **196-Region Off-Heap FFM Geodata & 2-Hour (`7200s`) Buff Durations (`8aa3b62f4d2`, `afd3d5d84b6`):** Provisioned all 196 `.l2j` geodata region files (`1.2 GB`, gitignored via `.gitignore`) in `dist/game/data/geodata/` and `build/dist/game/data/geodata/` (196 regions loaded by Panama FFM `GeoEngine`), and configured `EnableModifySkillDuration = True` with 2-hour (`7200s`) durations for all 110 buffer/song/dance/prophecy/summon support skills in `Player.ini`.

---

### Milestone 15: Native Client Geodata Builder (`///buildzone`) Disassembly & Converter + Full `CT2.4` Player Skills & 3rd-Prof Passives/Triggers Audit + 4-Agent Auditor Governance
* **Commits:** `121c6d69dd6`, `ff520e82973`, `1b35c07a259`, `e9a8b96d6a2`, `1a4b56efd31`, `a0b1b54be95`
* **Documentation:** [`LESSONS_LEARNED_AND_FORENSICS.md`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/LESSONS_LEARNED_AND_FORENSICS.md) (§7, §8, §9, §10), [`AGENTS.md`](file:///home/dwea/Projects/L2/AGENTS.md)
* **Key Accomplishments:**
  1. **`Engine.dll` `FWorldBuilder::buildWorldStructure` (`///buildzone`) Disassembly & Bidirectional Converter (`121c6d69dd6`):** Documented all `///` builder commands (`///buildzone`, `///loadpath`, `///c_rmode geodata`), proved why `NSWE` bits map to `East=1, West=2, South=4, North=8` (`VA 0x20467b08`), and built [`client/tools/convert_buildzone_to_l2j.py`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/client/tools/convert_buildzone_to_l2j.py) supporting `<X>_<Y>_path.txt` $\leftrightarrow$ `<X>_<Y>.l2j` and `<X>_<Y>_conv.dat` $\rightarrow$ `<X>_<Y>.l2j`.
  2. **Player Skills, Momentum Charges (8 Max), Channeling Engine & `+Duel` Enchant Door Scope (`ff520e82973`):** Fixed `FocusMaxEnergy.java` and `FocusEnergy.java` to resolve max 8 charges from Epilogue active skills `8` (*Sonic Focus*) and `50` (*Focused Force*) (`918`, `919`, `345`, `346`, `5127`, `54`); guarded `SkillChannelizer.java` `effectRange` check for `GROUND` channeling skills (`Volcano` `1419`, `Cyclone` `1420`, `Raging Waves` `1421`, `Gehenna` `1423`) and restored 7-arrow channeling on `924` (*Seven Arrow*) and `1472`/`1545` (*Soul Sucking*); removed High Five `CallSkill` `23299` from `772` (*Arrow Rain*); uncommented `DamOverTime` on `1467` (*Meteor*) and `1468` (*Star Fall*); and fixed `+Duel` enchant PvE scope against `Door` and `SiegeFlag` targets in `Skill.java`.
  3. **3rd-Profession Passives, Toggles, Tank Lv83 Seeds/Ultimates & Triggered Sub-Skills Audit (`e9a8b96d6a2`, `1a4b56efd31`):**
     * Moved Ghost Sentinel's 8 Lv81–82 Forgotten Scroll skills (`755`–`759`, `773`, `924`, `946`) from `2ndClass/PhantomRanger.xml` to `3rdClass/GhostSentinel.xml`.
     * Fixed `TriggerSkillByDamageDealt.java:91` (`if (_isCritical && !event.isCritical()) return;`) so critical hits proc `758` (*Fighter's Will*), `759` (*Archer's Will*), `1498` (*Mutual Response*), Subclass Master Abilities, and PvP Weapons.
     * Added `<targetType>SELF</targetType>` to Tank Lv83 Seed sub-skills `5561`–`5564` and `Creature.makeTriggerCast` fallback so all 4 Tank Seeds (`761`, `784`, `786`, `788`) grow to Stage 4 and unlock all 4 Tank Lv83 Ultimates (`762` *Insane Crusher*, `785` *Flame Icon*, `787` *Touch of Eva*, `789` *Spirit of Shilen*).
     * Fixed `5426` (*Counter Chance*) `mAtkSpd` multiplier (`0.85` $\rightarrow$ `1.15`), `CreatureStat.getMCriticalHit` integer truncation for `945` (*Magician's Will* `+5%` M.Crit), `Formulas.calcEffectAbnormalTime` `Skill Mastery` (`330`/`331`) `-1 -> -2` passive/toggle duration bug, `335` (*Fortitude*) `MpConsumePerLevel`, `767`/`5565` (*Expose Weak Point*), and `1498` (*Mutual Response*) `<allowSkillAttack>true</allowSkillAttack>`.
  4. **4-Agent Auditor Governance & Automated `l2_auditor.py` Suite (`a0b1b54be95`):** Established Agent 4 (`38e33746-e411-44db-8ed9-525f8cf93b55`) as dedicated Project Auditor, built [`tools/auditor/l2_auditor.py`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/tools/auditor/l2_auditor.py) (`--check`, `--sync`, `--json`) and [`AGENTS.md`](file:///home/dwea/Projects/L2/AGENTS.md), and reconciled all shared documentation, `GameServer.jar` manifest, `General.ini` `GMAudit = True`, and spawn XMLs across `dist/` and `build/dist/`.

---

### Milestone 16: GM Console v3 (`948px` + CT1 Top Tabs + Breadcrumb Stack), Transactional Delta Script Hot-Reload (`//reload changed`), Pre-Public Tiers 1–3 Forensic Bug Fixes, Tactical Utility-AI + Virtual-Thread LLM FakePlayer Engine & Java 27 / UE2.5 Optimizations
* **Commits:** `abcc9bec4b6`, `decaf386a5e`, `3917083adbc` & `44288d5e295`, `1b0a4ac10b0`, `29652ca9e5e`, `6baa530a970`
* **Key Accomplishments:**
  1. **Phase 1 — `GMWnd` (`Alt+G`) v3 Community-Board-Style Top-Tab Console & Transactional Delta Script Hot-Reload (`abcc9bec4b6`):**
     * **Client (`patch_gmwnd_client.py`, `interface.xdat` `1,306,456` B, `interface.u` `4,061,011` B 0-byte delta):** Removed the 228px left button dock, styled 7 contiguous native C++ category tabs (`y=31.0, h=23, x=7..695`: `Dashboard`, `Players`, `World`, `Instances`, `Events & GM`, `Items & Char`, `Server & Reload`) with genuine CT1 `L2UI_CT1.tab.Tab_DF_Tab_Unselected`/`Selected` textures + `BoardWnd` `Tab_DF_bg_line` & `Tab_DF_bg` frame, kept 3 top-right `.dat` search tree buttons (`Item .dat`, `NPC .dat`, `Skill .dat`), and expanded `GMWnd.HtmlViewer` to **`968×472` px**.
     * **Server (`ScriptEngine.java`, `AdminReload.java`, `AdminBoardManager.java`, `AdminInstance.java`):** Added `_scriptLastModified` tracking across all 1,185 `.java` scripts and transactional `reloadModifiedScripts()` (`//reload changed` / `//reload delta`); widened `AdminBoardManager` tables to `948px`, added per-GM `Deque<NavEntry>` breadcrumb navigation with **`[ ^ Back 1 Level ]`** (`//gmw back`), added the `Instances` tab (`//gmw instances`) with active runtime instance table and 1-click dungeon/raid teleports, and added `admin_ghoston`/`admin_ghostoff`.
  2. **Phase 2 — All 3 Tiers of Pre-Public Forensic Bug Fixes (`decaf386a5e`):**
     * **Tier 1 (`CrystalCaverns.java`):** Fixed Emerald Square `world.cleanedRooms` soft-lock (`roomsStatus[i] == 1 -> 2`), replaced all 18 global `cancelQuestTimers(...)` calls with instance-scoped `cancelQuestTimer(...)` / `cancelSteamTimers(CCWorld, ...)`, implemented `"baylor_despawn"` timer handler and `"BaylorMinions"` case-insensitive matching, spawned Darnel's chamber traps (`18379..18389`), and fixed disconnect re-entry stranding by routing reconnecting players to their active branch/stage coordinate (`activeBranch` + `world.getStatus()`).
     * **Tier 2 (`HeartInfinityAttack.java`, `TowerOfNaia.java`, `TullyWorkshop.java`, `25700-25799.xml`, `Kamaloka.java`, `PailakaInjuredDragon.java`):** Moved `tumorRespawnTime`, `conquestBegun`, `conquestEnded`, and `houndBlocked` into `HIAWorld`, fixed `_deadTumor` assignment in `TumorRevival`/`RegenerationCoffinSpawn`, and implemented `handleEkimusStats(HIAWorld)` with `FuncSet` stat modifiers; removed duplicate `DWARVEN_GHOST` (`32370`) registration from `TowerOfNaia.java`; scheduled `TullyWorkshop.java` `3%` heal after `healSkill.getHitTime()`; created `25700-25799.xml` (`25706..25712`) and wired Level 73/83 `Kamaloka.java` (`index 18`); and added `<= 900` distance + `GeoEngine.canSeeTarget` LOS check to `PailakaInjuredDragon.java`.
     * **Tier 3 (`CreatureTemplate.java`, `CreatureStat.java`, `Creature.java`, `NpcData.java`, `TerritoryWarSuperClass.java`, `CastleChamberlain.java`, `FortressOfResistance.java`):** Restored parsed XML `_baseAccuracy`, `_baseEvasion`, and `_baseReuseDelay` across all 9,883 NPCs in `CreatureTemplate`/`CreatureStat`/`Creature`; implemented `TerritoryWarSuperClass.getTerritoryIdForThisNPCId(int)` (`36499..36599` $\rightarrow$ `81..89`); wired `castle.getTaxPercent()` in `CastleChamberlain.java`; and fixed per-talk `NpcHtmlMessage(npc.getObjectId())` + `_damageToNurka.clear()` in `FortressOfResistance.java`.
  3. **Phase 3 — Tactical Utility-AI (`250ms`) + Java 27 Virtual-Thread LLM FakePlayer Engine (`3917083adbc` & `44288d5e295`, `1b0a4ac10b0`, `29652ca9e5e`):**
     * Built `FakePlayerAiEngine.java` (`250ms` periodic Utility-AI loop classifying FakePlayers into `ARCHER`, `MAGE`, `DAGGER`, `HEALER`, `TANK`, `WARRIOR` with 6 real-time micro-tactics: 6-slot radial Squad Follow & Assist, Healer Triage `1218`, Tank Hate-Intercept `28`, Archer/Mage `GeoEngine`-validated Standoff Kiting, Dagger Backstab Flanking `263`, and `< 35%` HP Second Wind Potion `2037`).
     * Upgraded `FakePlayerChatManager.java` (`CopyOnWriteArrayList`, shared Java 27 `HttpClient` on `Executors.newVirtualThreadPerTaskExecutor()`, instant whisper tactical directives `"assist me"`, `"kite"`, `"aggressive"`, `"guard"`, `"heal me"`, `"status"`, and non-blocking OpenAI-compatible LLM completion bridge with `[MODE:...]` tag parsing).
     * Added 6 Level 85 tactical squad templates (`81001` `Gladus` Duelist, `81002` `Lyra` Sagittarius, `81003` `Seraphina` Cardinal, `81004` `Aegis` Phoenix Knight, `81005` `Vex` Adventurer, `81006` `Zephyr` Archmage) in `fpc_combat.xml` and built the `948px` `AdminFakePlayers.java` (`//fakeplayer`) Control Center.
  4. **Client UE2.5/DXVK Tuning & Local Docker Java 27 (`Temurin-27+35`) Lilliput + ZGC Optimization:**
     * **Client (`l2.ini`, `Option.ini`, `launch-l2.sh`):** Set `CacheSizeMegs=512` (16× increase), `L2MaxPawnResourceLoad=5.0` (5× increase), `GPUAnimation=True`, `ReduceMouseLag=False`, `UseTripleBuffering=True`, `LevelOfAnisotropy=16`, and `DXVK_STATE_CACHE=1`.
     * **Docker (`docker/run-server.sh`, `docker-compose.yml`):** Enabled `-XX:+UseCompactObjectHeaders` (JEP 519), `-XX:+AlwaysPreTouch`, `-XX:+UseStringDeduplication`, `-XX:+ExplicitGCInvokesConcurrent`, `-XX:SoftMaxHeapSize=3g`, `-XX:ZUncommitDelay=300`, rotating `log/gc.log`, passive `/proc/net/tcp{,6}` `0A` (`TCP_LISTEN`) healthchecks, and MariaDB 11.8 tuning (`--innodb-buffer-pool-size=512M`, `--max-connections=250`), reducing `l2-game` startup heap by **`-152 MB` (`-7.5%`)** (`1,866 MB`, `9s` boot).

---

## 4. Queued Next Tasks (Ready for Interactive In-Game Testing)

1. **In-Game Verification of `GMWnd` v3 (`Alt+G`), `//reload changed`, Tier 1–3 Dungeons & `//fakeplayer` Squad AI:**
   * Test the 7 native CT1 top category tabs, `[ ^ Back 1 Level ]` breadcrumb navigation, `//reload changed`, Crystal Caverns + Baylor Raid, and `//fakeplayer summon SQUAD` (`81001..81006`).
2. **Task B — The 5-Phase Expansion Gatekeeper & Dynamic 1–75 / 76–85 Progression Engine (`feature/expansion-phase-and-progression-engine`):**
   * Central `ExpansionPhaseManager` singleton (`Phase 1` Baseline/IoP $\rightarrow$ `Phase 2` Crystal Caverns/Baylor $\rightarrow$ `Phase 3` Hellbound/Tully/Naia $\rightarrow$ `Phase 4` Beleth $\rightarrow$ `Phase 5` Gracia Continent/Seeds), two-tier XP/SP/Drop curve (`1–75` accelerated, `76–85` endgame), `.phase` player command, and `//setphase <1-5>` integrated into the `GMWnd` / RBAC Admin Console.
