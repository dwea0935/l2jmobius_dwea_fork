# Multi-Agent Reconciliation & File Ownership Notice

**Timestamp:** 2026-10-17T01:17:00+02:00  
**Active Workspace:** `/home/dwea/Projects/L2`  
**Status:** **100% RECONCILED, AUDITED & SYNCED ACROSS ALL 4 AGENTS (`1a4b56efd31`)**

---

## 1. Summary of Multi-Agent Convergence
Both autonomous agents (`Agent 1` and `Agent 2`) have completed all tasks and reconciled into a unified repository state:

1. **Version-Controlled Client (`client/system/` + Launcher & Patcher Scripts):**
   - Tracked in **both**:
     - [`l2jmobius_dwea_fork/client/`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/client) (pushed to `git@github.com:dwea0935/l2jmobius_dwea_fork.git` on `master` at commit `9ddf57e6c12`).
     - [`Client Lineage 2 Gracia Epilogue/.git`](file:///home/dwea/Projects/L2/Client%20Lineage%202%20Gracia%20Epilogue) (standalone Git repository restricted via [`.gitignore`](file:///home/dwea/Projects/L2/Client%20Lineage%202%20Gracia%20Epilogue/.gitignore) strictly to `system/`, `launch-l2.sh`, `patch_gmwnd_client.py`, and the master documentation guides).
2. **Agent 2 Deliverables Included & Verified:**
   - [`launch-l2.sh`](file:///home/dwea/Projects/L2/Client%20Lineage%202%20Gracia%20Epilogue/launch-l2.sh): Full Linux Wine/Proton + 32-bit DXVK (`d3d9.dll` + `dxgi.dll`) launcher with an interactive **PyQt6 Visual Pre-Launch Command Center** (`--gui` default, `--no-gui` / `--cli` to bypass) featuring editable fields for all CLI & `l2.ini`/`Option.ini` settings (`-server`, `Port`, `-login`, `-pass`, `-slot`, `--borderless`, `--windowed`, `--fullscreen`, `--res`, `--desktop`, `--runtime`, `--no-dxvk`, `--hud`, `--gamepad`, `IsL2Console`, `EnableSecondWindow`, `L2UseKeyCrypt`, extra `l2.exe` args), live TCP status pills for Agent 3's Docker stack (`3306`, `2106`, `7777`), a 1-click **Promote Characters to Master GM (`accesslevel=100`)** button via pure-Python MariaDB wire protocol, and a live-updating CLI preview bar.
   - [`system/windrv.dll`](file:///home/dwea/Projects/L2/Client%20Lineage%202%20Gracia%20Epilogue/system/windrv.dll): 5 binary patches (`0x10FAF`, `0x11141`, `0x0D43A`, `0x0D4B9`, `0x0E865`) enabling dynamic `WM_SIZE` window resizing, OS Maximize button support, XWayland async geometry compatibility, and `(0,0)` `WS_POPUP` Borderless Windowed mode.
   - [`system/d3ddrv.dll`](file:///home/dwea/Projects/L2/Client%20Lineage%202%20Gracia%20Epilogue/system/d3ddrv.dll): Binary patch at `0xC2494` (`74 22 -> 90 90`) silencing the obsolete 2004 AGP warning popup.
   - Plain-text decrypted `.ini` and `.int` files (`1920x1080` Borderless Windowed default in [`Option.ini`](file:///home/dwea/Projects/L2/Client%20Lineage%202%20Gracia%20Epilogue/system/Option.ini) and [`l2.ini`](file:///home/dwea/Projects/L2/Client%20Lineage%202%20Gracia%20Epilogue/system/l2.ini), plus modernized XInput/SDL2 controller & D-Pad bindings in [`user.ini`](file:///home/dwea/Projects/L2/Client%20Lineage%202%20Gracia%20Epilogue/system/user.ini)).
   - [`CLIENT_MASTER_ARCHITECTURE_AND_GUIDE.md`](file:///home/dwea/Projects/L2/Client%20Lineage%202%20Gracia%20Epilogue/CLIENT_MASTER_ARCHITECTURE_AND_GUIDE.md): Complete client reverse-engineering, binary patch, encryption, controller, and `.dat` table dossier.
3. **Agent 1 Deliverables Included & Verified:**
   - [`patch_gmwnd_client.py`](file:///home/dwea/Projects/L2/Client%20Lineage%202%20Gracia%20Epilogue/patch_gmwnd_client.py), [`system/interface.xdat`](file:///home/dwea/Projects/L2/Client%20Lineage%202%20Gracia%20Epilogue/system/interface.xdat), and [`system/interface.u`](file:///home/dwea/Projects/L2/Client%20Lineage%202%20Gracia%20Epilogue/system/interface.u): Unified Native C++ `GMWnd` (`Alt+G`, `992x544`) Master Console with embedded `GMWnd.HtmlViewer` (`744x492`) and tri-modal `//summon`.
   - [`MASTER_HANDOFF_AND_CLIENT_SERVER_GUIDE.md`](file:///home/dwea/Projects/L2/Client%20Lineage%202%20Gracia%20Epilogue/MASTER_HANDOFF_AND_CLIENT_SERVER_GUIDE.md) and [`PROJECT_MASTER_CONTEXT_AND_ROADMAP.md`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/PROJECT_MASTER_CONTEXT_AND_ROADMAP.md): Full 6-pillar server + client architecture and work-PC handoff prompt.

---

## 2. Local Dockerized Server Stack & Build Findings (Agent 3)

**Timestamp:** 2026-10-16T06:22+02:00  
**Status:** **RUNNING LOCALLY** — MariaDB + LoginServer + GameServer in Docker, GameServer registered on login as **Server 2: Sieghardt**. ⚠️ The `build.py` fix below is **uncommitted** in `l2jmobius_dwea_fork` (`build/` is gitignored, so the repacked jar exists only on this machine).

### 2.1 Host Prerequisites Installed
- `docker` (29.9.0) + `docker-compose` installed via `pacman`, `docker.service` enabled, user `dwea` added to the `docker` group (re-login required; until then use `echo "<cmd>" | newgrp docker` — note `newgrp` spawns **fish**, so wrap bash syntax in a script).

### 2.2 New Files
| File | Purpose |
| :--- | :--- |
| [`docker-compose.yml`](file:///home/dwea/Projects/L2/docker-compose.yml) | `db` (`mariadb:11.8`), `login` + `game` (`eclipse-temurin:27-jdk` — JDK required: datapack scripts compile at runtime, jars are class file v71). All `network_mode: host`, so dist configs (`localhost` DB, `127.0.0.1:9014` link, `127.0.0.1` client address) work **unmodified**. Servers run as `1000:1000`; DB data in named volume `l2db`. |
| [`docker/run-server.sh`](file:///home/dwea/Projects/L2/docker/run-server.sh) | Entrypoint replacing `*ServerTask.sh`: strips CRLF from `java.cfg`, adds `-Djava.awt.headless=true` (auto-disables Swing GUI) + `--enable-native-access=ALL-UNNAMED`, launches via explicit `-cp` + main class (see 2.3 #2). Restart-on-exit-code-2 handled by `restart: on-failure`. |
| [`docker/db-init/01-l2jmobius-schema.sh`](file:///home/dwea/Projects/L2/docker/db-init/01-l2jmobius-schema.sh) | Replaces GUI `DatabaseInstaller`: imports `db_installer/sql/login/*.sql` then `sql/game/*.sql` (116 tables) on first start of an empty volume. **Must stay non-executable** (sourced by the mariadb entrypoint for `docker_process_sql`). |

| Service | Endpoint |
| :--- | :--- |
| MariaDB | `127.0.0.1:3306` only — `root` / empty password (matches dist `Database.ini`), DB `l2jmobiusepilogue` |
| LoginServer | `0.0.0.0:2106` (clients), `127.0.0.1:9014` (GS link) |
| GameServer | `0.0.0.0:7777` |

### 2.3 Build Defects Found in `build/dist`
1. **`LoginServer.jar` was missing 18 classes → Game↔Login handshake crashed** (`ClassNotFoundException: ...loginserver.network.gameserverpackets.send.InitLS`).
   - **Root cause:** [`build.py`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/build.py#L125-L127) excluded `*gameserver*` from `LoginServer.jar`; `fnmatch` `*` matches `/`, so it also dropped `org/l2jmobius/loginserver/network/gameserverpackets/**`.
   - **Fix (uncommitted):** pattern anchored to `org/l2jmobius/gameserver/*`. Only `LoginServer.jar` was repacked from the existing `build/bin` (153 → 171 entries, 0 gameserver classes). Old jar backed up to `build/LoginServer.jar.bak-missing-gspackets` (outside `libs/` so it never lands on the classpath).
2. **`build/dist/libs/GameServer.jar` (05:46) has no `Main-Class` / `Class-Path` manifest** (only `Created-By: 27 (Arch Linux)`) — it was repacked outside `build.py` (likely plain `jar cf`). `java -jar` fails with `no main manifest attribute`.
   - **Workaround:** `run-server.sh` uses explicit classpath + main class. **Jar itself is NOT fixed.** Correct manifest lives in `build/gs_manifest.mf` (`jar ufm` or a `build.py` run would restore it).
   - ⚠️ `build.py` `clean()` does `rmtree(build/)` — this wipes **all of `build/dist`**, including config edits currently only in `build/dist/game/config` (04:26 / `AdminCommands.xml` 05:46) and runtime state. Sync those back to `L2J_Mobius_CT_2.4_Epilogue/dist/` before a full rebuild.
3. **No geodata:** `game/data/geodata/` contains only `Readme.txt` → `GeoEngine: Loaded 0 off-heap FFM regions`, pathfinding disabled. The FFM GeoEngine path is therefore **not exercised** by this local stack.

### 2.4 Operations Cheat Sheet
```bash
docker compose up -d                 # start (DB auto-initialised on first run)
docker compose logs -f game          # follow GameServer
docker compose restart login game    # after replacing jars / editing configs
docker compose down                  # stop, keep DB   |   docker compose down -v  # wipe DB
docker exec l2-db mariadb -uroot l2jmobiusepilogue -e "UPDATE characters SET accesslevel=100 WHERE char_name='Name'"
./launch-l2.sh --windowed --res 1920x1080 -server 127.0.0.1   # AutoCreateAccounts=True
```

### 2.5 Open Items Status (Resolved & Synced by Agent 1)
- [x] Committed the `build.py` exclude-pattern fix (`org/l2jmobius/gameserver/*`) + `docker/` stack in `l2jmobius_dwea_fork` and pushed to `origin/master`.
- [x] Restored `Main-Class` / `Class-Path` manifest into `build/dist/libs/GameServer.jar` via `jar ufm` (and verified 0 config drift between `build/dist/game/config/` and `L2J_Mobius_CT_2.4_Epilogue/dist/game/config/`).
- [x] Re-synced all copies of `AGENT_SYNC_AND_RECONCILIATION.md` and added Milestone 8 (Agent 3 Docker Stack) to `FULL_PROJECT_ENGINEERING_LEDGER.md`.
- [x] Optional: Provide `.l2j` geodata files in `game/data/geodata/` when testing off-heap FFM pathfinding locally.
- [x] Verified interactive client login, character creation, and entering the game world (`./launch-l2.sh`).

---

## 3. Client Login Crash Resolution & Clean `2009-09-28` (`L2 Epilogue Clean`) Unification

### 3.1 Root Cause of `00010084 1289` (`FD_CONNECT`) Crash
- The original `Client Lineage 2 Gracia Epilogue/system/` mixed **2010-07-16 (Protocol 152)** DLLs (`Core.dll`, packed `Engine.dll`, `nwindow.dll`, `window.dll`, `windrv.dll`, where `UObject` vtable offsets shifted by `+4` bytes) with **2009 / early-2010 (Protocol 148)** `l2.exe`, `dsetup.dll`, `fire.dll`, and `unichat.dll`.
-Replacing the hybrid binaries with the **100% unpacked, version-matched `2009-09-28` build** from `/home/dwea/Projects/L2/L2 Epilogue Clean/system/` (backed up to `system/backup_hybrid_152/`) resolved the vtable and `UNetworkHandler` hook mismatches completely.

### 3.2 Binary & Package Patches Applied to the Unified `2009-09-28` `system/`
1. **`system/l2.exe` (`434,176` bytes):**
   - Aliased import `?GL2RunningFirstHandle@@3PAXA` to `?GIsServer@@3HA` and NOPed the 2-byte mutex handle store at file offset `0x25CEE` (`89 01` → `90 90`).
   - Updated the embedded `Lineage2Ver413` RSA-1024 modulus (`0x2F500`, `VA 0x10930300`) and exponent (`0x2F604`, `VA 0x10930404`) from `75b4d6de...` (`0000001d`) to NCSoft's official retail `97df3984...` (`00000035`) key so all 45 pristine `.dat` files inflate cleanly.
2. **`system/Engine.dll` (`6,885,120` bytes, `2009-09-28`):**
   - Patched GameGuard check at file offset `0x0039B750` (`VA 0x2039C350`: `55 8b ec 6a ff` → `33 c0 fe c0 c3`, returning `1`).
   - Restored native protocol string at file offset `0x004A3AE0` (`VA 0x204A46E0`) to `$Revision: 148 $` (per Option C reconciliation below so the `2009-09-28` client truthfully announces Protocol `148`; verified only the 5 bytes at `0x39B750` differ from pristine `L2 Epilogue Clean/system/Engine.dll`).
3. **`system/d3ddrv.dll`, `system/windrv.dll`, `system/dsetup.dll` & `launch-l2.sh`:**
   - Silenced the AGP popup in `d3ddrv.dll` (`0xC24A4`: `74 22` → `90 90`), applied all 5 dynamic window resize / borderless patches to `windrv.dll` (`+0x70` offsets: `0x1101F`, `0x111B1`, `0x0D4AA`, `0x0D529`, `0x0E8D5`), rebased `dsetup.dll` to `0x12000000`, and installed 32-bit DXVK (`d3d9.dll` + `dxgi.dll`) into both `syswow64` and `system32`.
4. **`system/interface.xdat` & `system/interface.u` (`2009-09-28`):**
   - Applied the 992×544 `GMWnd` (`Alt+G`) Master Console layout and all 7 UnrealScript bytecode patches (`SerializeExpr` verified).

### 3.3 Server–Client Packet Alignment Audit (`2009-09-28` `Engine.dll` vs `L2J_Mobius_CT_2.4_Epilogue`)
- **Symptom (`NConsoleWindow.cpp` Line 4212 Assertion on Multi-Item `InventoryUpdate`):**
  ```text
  Assertion failed: iWorkType == IUT_ADD || iWorkType == IUT_UPDATE || iWorkType == IUT_DELETE [File:.\Src\NConsoleWindow.cpp] [Line: 4212]
  History: NConsoleWnd::UpdateInventoryItem <- TEXT("InventoryUpdatePacket") <- UNetworkHandler::Tick
  ```
- **Root Cause:**
  - Disassembling `InventoryUpdatePacket` (`VA 0x203c0940`) and `ItemListPacket` (`VA 0x203c1060`) in the unpacked `2009-09-28` `Engine.dll` showed that per-item deserialization uses `'hh'` (`iWorkType`, `itemType1`) followed by `'dddQhhhdhhhhdhhhhhhhhd'` (`0x204984e0` — ending right after `int time`, **70 bytes** per item body).
  - `L2J_Mobius_CT_2.4_Epilogue` appended **3 extra `short`s (6 bytes)** at the end of `AbstractItemPacket.writeItem` (`for (int op : item.getEnchantOptions()) buffer.writeShort(op);`), which was a late mini-patch addition (`optiondata_client` IDs for special items like *Olf's T-Shirt*, **not** the normal `+N` item enchant level, which is written earlier via `buffer.writeShort(item.getEnchant())` at line 51).
  - Whenever `InventoryUpdate` contained **2+ items** (e.g., turning in a newbie quest item and receiving reward items within the 100ms batch in `Player.sendInventoryUpdate`), item #1's trailing 6 bytes (`0x0000, 0x0000, 0x0000`) shifted the packet stream so item #2 read `0` as `iWorkType`, triggering the assertion in `NConsoleWnd::UpdateInventoryItem`.
- **Full Audit & 23 Updated Server Classes (Compiled into `build/dist/libs/GameServer.jar` in commit `d35432adcb8`):**
  1. **`AbstractItemPacket.java` & `ItemInfo.java`:** Omitted the trailing 3× `short` `EnchantOptions` loop in `writeItem` (fixing `InventoryUpdate`, `ItemList`, and `GMViewItemList`) and added `default: _change = 2;` (`MODIFIED`) in `ItemInfo(Item)` so `iWorkType` can never be `0`. Normal item `+N` enchanting (`item.getEnchant()`), weapon glows (`_player.getEnchantEffect()`), Life Stone augmentations (`item.getAugmentationBonus()`), elemental attributes, and server-side `EnchantOptions` stat skills remain 100% intact.
  2. **All Other Item Packet Handlers Audited Against `Engine.dll` (`0x20433000` Dispatch Table):**
     - Removed trailing 3× `short` `EnchantOptions` / `Enchant effect` fields from: `BuyList.java`, `ExBuySellList.java` (`_buyList`, `_sellList`, `_refundList`), `WareHouseDepositList.java`, `WareHouseWithdrawalList.java`, `SortedWareHouseWithdrawalList.java`, `TradeStart.java`, `TradeOwnAdd.java`, `TradeOtherAdd.java`, `PetItemList.java`, `PetInventoryUpdate.java`, `PrivateStoreManageListSell.java`, `PrivateStoreListSell.java`, `PrivateStoreManageListBuy.java`, `PrivateStoreListBuy.java`, `ExReplyPostItemList.java`, `ExReplyReceivedPost.java`, `ExReplySentPost.java`, and `ExRpItemLink.java`.
     - Aligned `PackageSendableList.java` (`'h'` + `'ddQhhdhhhdhhhhhhhh'`) and `GMViewWarehouseWithdrawList.java` (`'h'` + `'ddQhh'` + `'dhhhddhhhhhhhhd'` + `'dd'`) with their exact `Engine.dll` handlers (`0x203beec0` and `0x203c12c0`).
  3. **Swapped Extended Opcode Fix (`ServerPackets.java`):**
     - In `Engine.dll`'s opcode table (`0x20687f70`), `0xFE:0xBD` (`0x2068be04`) is `ExBrBroadcastEventState` and `0xFE:0xBE` (`0x2068be28`) is `ExBrExtraUserInfo`. Fixed `ServerPackets.java` where those two were swapped (`// BE?` / `// BD?`).
  4. **`ShortcutInit` / `SkillList` Packet-Ordering Fix (`EnterWorld.java` & `Player.java`):**
     - Disassembly of `NConsoleWnd::AddShortCutItem` (`nwindow.dll` `VA 0x1023b2f0`) and `NCShortcutItemManager::AddShortcutItem` (`VA 0x101f2ac0`) showed that when `ShortCutInitPacket` (`Engine.dll` `VA 0x203af410`) arrives, it first wipes all 144 shortcut slots (`NCShortcutItemManager::Clear` `VA 0x101f1ad0`) and then validates each shortcut against the client's loaded `ItemList` (`0x1020b490`), `SkillList` (`0x101fe020` / `0x101fe0e0` / `0x1020d600`), and `ExBasicActionList` (`0x101f1220`), silently dropping any entry whose prerequisite list hasn't arrived yet.
     - Fixed `EnterWorld.java` to send `ShortcutInit` after `ExBasicActionList` and `player.sendSkillList()`, and fixed `Player.giveAvailableSkills()` (called on level-up via `rewardSkills()`) and subclass change (`Player.java`) to call `sendSkillList()` before `updateShortcuts()` and `ShortcutInit` (matching `RequestAcquireSkill.java`). All **25** updated server classes are compiled into `build/dist/libs/GameServer.jar`.

### 3.4 Saved Forensics for Paused Task (Auto-Login & `Option.ini` / `windrv.dll` Settings)
- **Why `[AutoLogOn]` in `l2.ini` was ignored:** Retail Gracia Epilogue compiled out the internal `[AutoLogOn]` parser (`IsL2AutoLogOn`, `L2ID`, `L2Passwd`, `L2Slot` have zero string references in `l2.exe`, `Engine.dll`, `Core.dll`, or `NWindow.dll`). Login is driven by C++ classes in `NWindow.dll` (`ImageBase = 0x10000000`):
  - `NCAuthWnd::ShowWindow` (`VA 0x10143300`) & `NCAuthWnd::OnLoginBtnClick` (`VA 0x10144b50` → calls `NConsoleWnd::RequestAuthLogin` at `VA 0x1021a160`).
  - `NCEulaWnd::ShowWindow` (`VA 0x10140f10`) & `NCEulaWnd::OnOkBtnClick` (`VA 0x10142350` → calls `NCEulaWnd::RequestServerList` at `VA 0x10140ad0`).
  - `NCServerSelectWnd::EndServerInfo` (`VA 0x10127480`) & `NCServerSelectWnd::OnLoginBtnClick` (`VA 0x1012a7c0` → calls `NConsoleWnd::RequestServerLogin` at `VA 0x10219ab0`).
  - `NCLobbyWnd::ReceiveCharacterSelectedPacket` / `NCLobbyWnd::GameStart` (`VA 0x101455f0`) and `LobbyMenuWnd` in `interface.u` (`EV_LobbyCharacterSelect = 0xbce` → `StartGame(index)`).
- **Why `Option.ini` & Borderless Window settings were partially overridden:**
  - `l2.exe` (`VA 0x10925180`) checks `FirstRun >= 2` in `Option.ini` (`OptionWnd` sets `FirstRun=2` on save; `launch-l2.sh` was writing `FirstRun=False` which parses as `0`, triggering `SetInitialConfiguration` hardware defaults).
  - `windrv.dll`'s `UWindowsViewport::ResizeViewport` at `VA 0x1110e012` and `VA 0x1110e03c` executes `or eax, 0x00c40000` (`WS_CAPTION | WS_THICKFRAME`) unconditionally when windowed without checking `UseWindowFrame` (`[edi+0xc4]`).
  - `launch-l2.sh` line 1009 placed `IsL2Console` under `[L2WaterEffect]` instead of `[URL]`.

---

### 3.5 Option C Reconciliation (Client Agent ↔ Server Agent ↔ Expert Agent): Dual-Protocol (`148` + `152`) Support
Following the 3rd Agent's protocol-differentiation audit, the user selected **Option C**:
> *"Restore `$Revision: 148 $` in `Engine.dll` AND branch on `client.getProtocolVersion() >= 152` in the item packets so both 148 (2009) and 152 (2010) clients are supported simultaneously."*

#### A. Completed by Client Agent (Done & Pushed)
1. **Restored `$Revision: 148 $` in `Engine.dll` (`0x004A3AE0` / `VA 0x204A46E0`):**
   - Both `/home/dwea/Projects/L2/Client Lineage 2 Gracia Epilogue/system/Engine.dll` and `/home/dwea/Projects/L2/l2jmobius_dwea_fork/client/system/Engine.dll` have been restored to the exact ASCII string `b"$Revision: 148 $\x00"` at file offset `0x4A3AE0`.
   - Verified via full 6,885,120-byte comparison against pristine `/home/dwea/Projects/L2/L2 Epilogue Clean/system/Engine.dll`: **only** the 5-byte GameGuard check bypass at `0x0039B750` (`33 c0 fe c0 c3`) differs.
   - Our `2009-09-28` client now truthfully announces `ProtocolVersion = 148` on connect (matching `PosCode : LS1(148)`), allowing `GameClient.getProtocolVersion()` on the server to distinguish `148` (`2009-09-28`) clients from `152` (`2010-07-16`) clients.

#### B. Completed by Server-Focused Agent (Done & Pushed)
1. **`Server.ini` Config (`L2J_Mobius_CT_2.4_Epilogue/dist/game/config/Server.ini` & `build/dist/game/config/Server.ini`):**
   - Updated `AllowedProtocolRevisions = 148;152` (removing the unverified `146` revision so only the two supported layouts `148` and `152` are accepted).
2. **Thread-Safe Protocol Detection in `ServerPacket.java` (`isProtocol152()` / `isProtocol152(GameClient)`):**
   - Added a `ThreadLocal<GameClient> WRITING_CLIENT` scoped inside `ServerPacket.write(GameClient client, WriteBuffer buffer)` (`try { writeImpl(c, buffer); } finally { WRITING_CLIENT.remove(); }`) along with `protected boolean isProtocol152()` and `protected boolean isProtocol152(GameClient client)` (`client != null && client.getProtocolVersion() >= 152`).
3. **Protocol-Conditional `EnchantOptions` Across All 21 Item Packet Classes:**
   - Gated the trailing 3× `short` `EnchantOptions` (`6` bytes) on `isProtocol152()` / `isProtocol152(client)` in `AbstractItemPacket.java` (`ItemList`, `InventoryUpdate`, `GMViewItemList`), `BuyList.java`, `ExBuySellList.java` (`_buyList`, `_sellList`, `_refundList`), `WareHouseDepositList.java`, `WareHouseWithdrawalList.java`, `SortedWareHouseWithdrawalList.java`, `TradeStart.java`, `TradeOwnAdd.java`, `TradeOtherAdd.java`, `PetItemList.java`, `PetInventoryUpdate.java`, `PrivateStoreManageListSell.java`, `PrivateStoreListSell.java`, `PrivateStoreManageListBuy.java`, `PrivateStoreListBuy.java`, `ExReplyPostItemList.java`, `ExReplyReceivedPost.java`, `ExReplySentPost.java`, `ExRpItemLink.java`, `PackageSendableList.java`, and `GMViewWarehouseWithdrawList.java`.
   - Protocol `148` (`2009-09-28`) clients receive the 70-byte item body (`'dddQhhhdhhhhdhhhhhhhhd'`); Protocol `152` (`2010-07-16`) clients receive the 76-byte item body (`+ hhh`).
4. **Protocol-Conditional `ExBrExtraUserInfo` / `ExBrBroadcastEventState` (`0xFE:0xBD` vs `0xFE:0xBE`):**
   - Updated `ExBrExtraUserInfo.java` and `ExBrBroadcastEventState.java` to branch on `isProtocol152(client)` so Protocol `148` uses `0xFE:0xBE` (`'dd'`, no `_lectureMark` byte) for `ExBrExtraUserInfo` and `0xFE:0xBD` for `ExBrBroadcastEventState`, while Protocol `152` uses `0xFE:0xBD` (`'ddc'` with `_lectureMark`) and `0xFE:0xBE`.
5. **Recompiled & Repacked `build/dist/libs/GameServer.jar` (28 updated classes total including `EnterWorld.java` and `Player.java`).**

#### C. Completed Hardening Item — Mixed 148 + 152 Sessions (Broadcast-Cache Opt-Out, Tripwire & `AdminBoardManager` Fallback)
**Status:** ✅ COMPLETED & PACKED into `build/dist/libs/GameServer.jar`.

**Why this matters:** Players commonly drop only a custom `l2.ini` into a retail **2010-07-16 (Protocol 152)** client instead of using our full `system/` folder, producing **mixed 148 + 152 sessions** on the same server.

**Hardening Applied:**
1. **Broadcast-Cache Opt-Out Guarantee (`ServerPacket.isProtocolSensitive()`):**
   - Added `protected boolean isProtocolSensitive() { return false; }` in [`ServerPacket.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/network/serverpackets/ServerPacket.java) and overrode `sendInBroadcast()` so `super.sendInBroadcast()` is skipped whenever `isProtocolSensitive()` is `true` (ensuring per-recipient serialization instead of cached broadcast bytes).
   - Overrode `isProtocolSensitive()` to return `true` in `AbstractItemPacket.java` (covering `InventoryUpdate`, `ItemList`, `GMViewItemList`, `AbstractInventoryUpdate`, `PetInventoryUpdate`, `PackageSendableList`, and `GMViewWarehouseWithdrawList`), `ExBrExtraUserInfo.java`, `ExBrBroadcastEventState.java`, and all 17 direct `ServerPacket` item/shop/warehouse/trade/pet/store/mail subclasses (`BuyList`, `ExBuySellList`, `ExReplyPostItemList`, `ExReplyReceivedPost`, `ExReplySentPost`, `ExRpItemLink`, `PetItemList`, `PrivateStoreListBuy`, `PrivateStoreListSell`, `PrivateStoreManageListBuy`, `PrivateStoreManageListSell`, `SortedWareHouseWithdrawalList`, `TradeOtherAdd`, `TradeOwnAdd`, `TradeStart`, `WareHouseDepositList`, `WareHouseWithdrawalList`).
2. **Broadcast Tripwire (`ServerPacket._broadcastRequested`):**
   - `ServerPacket.sendInBroadcast()` records `_broadcastRequested = true`. If `isProtocol152()` or `isProtocol152(client)` is ever called on a packet instance where `_broadcastRequested && !isProtocolSensitive()`, `PacketLogger.warning(...)` logs a warning with a full stack trace.
3. **GM Console Fallback for Stock Protocol 152 Clients (`AdminBoardManager.java`):**
   - Updated [`AdminBoardManager.sendWrappedBoardHtml`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/managers/AdminBoardManager.java#L489) (`player.getClient().getProtocolVersion() < 152`) so GMs on our patched `2009-09-28` (`Protocol 148`) client use the native 992×544 `GMWnd.HtmlViewer`, while GMs on a stock `2010-07-16` (`Protocol 152`) client without our patched `interface.u`/`interface.xdat` automatically fall back to `BoardWnd` (`ShowBoard`).
4. **`Alt+B` vs `Alt+G` Interception Fix (`AdminBoardManager.tryInterceptAdminBoardHtml`):**
   - Updated `tryInterceptAdminBoardHtml` to intercept `HtmlUtil.sendCBHtml` calls **only** when `session.commandDepth > 0` (i.e., when an `admin_*` command handler such as `AdminAnnouncements` or `AdminGeodata` is actively executing inside `AdminCommandHandler`), and ensured `endAdminCommand` always decrements `session.commandDepth` before checking access level. Pressing `Alt+B` (`_bbshome`) now opens the normal Community Board (`BoardWnd`) even when the GM banner button (`bypass admin_gmw`) is present in the HTML, while `Alt+G` and the banner button open `GMWnd`.
5. **Protocol-148 `SystemMessage` Fallback for IDs `3093..3207` (`SystemMessageId.java` & `SystemMessage.java`):**
   - Because the `2009-09-28` (`Protocol 148`) `systemmsg-e.dat` ends at ID `3092` before jumping to `6001`, `SystemMessageId.init()` now attaches a default `SMLocalisation` from `@ClientString(message = ...)` for IDs `3093..5999` (`3094`, `3108` *"You are no longer protected from aggressive monsters."*, `3206`, `3207`).
   - When `!isProtocol152(client)`, `SystemMessage.writeImpl` formats the message with its parameters via `SystemMessageId.S1_2` (`1987: "$s1"`), eliminating `"Invalid System Message format : 3108"` while keeping native IDs on Protocol `152` clients (and marking those `SystemMessage` instances `isProtocolSensitive() == true`).

---

## 4. Admin Console ↔ Community Board Full Decoupling & Native `GMWnd` Admin Panel v2 (User Decision, 2026-10-16 11:08)

**Raised by:** Docker/Ops Agent audit (`c2f6f65db50` baseline → `8a6534aefce` CB admin panel → `master` `9419c4d7b5a`). Full audit with line references: `ADMIN_CB_COUPLING_AUDIT.md (repo root of l2jmobius_dwea_fork)`.
**Owners:** Server Agent (Phase 1, server side of Phase 2) · Client Agent (client side of Phase 2) · Docker/Ops Agent (deploy + verification).

### 4.1 User Decisions
1. **Completely decouple the Community Board: rip out every admin interface from it.** The player Community Board (`Alt+B`) must contain zero admin code, banners, buttons, action caches or interception hooks. The `BoardWnd` CB rendering mode of the admin console is **retired** (this supersedes the Protocol-152 `BoardWnd` fallback from `14933860a51` and the `allowAltg="false"` CB path for level-10 Chat Moderators).
2. **Restore HTML-action validation** for GM `admin_*` bypasses in `RequestBypassToServer` (defense-in-depth), with correctly scoped action caches.
3. **The native `GMWnd` (`Alt+G`) admin panel becomes the only admin UI**, co-designed by Client + Server Agents:
   - Legacy NCSoft GM tools (`View Status/Items/Skills/Quests/Warehouse`, `Recall`, `Item/NPC/Skill Search`, etc.) are mainly troubleshooting/inspection tools → group them into a small **"Legacy"** category inside the new board instead of treating them as primary admin controls.
   - **UX defect:** the native C++ buttons execute their action but give **no visual feedback** (no highlight / pushed / colour change on click). Fix as part of the panel work (client side; likely `interface.xdat` `ButtonCtrl` normal/pushed/highlight texture fields on the re-laid-out `GMWnd` buttons — to be confirmed by the Client Agent).
   - Evaluate how much of the console should move from `GMWnd.HtmlViewer` (HTML) to true native C++ controls, and propose better features.
4. **Out of scope now (future milestone):** a full custom Community Board (buffer, GM shop, utilities, etc.) replacing the legacy NCSoft board — the decoupling must leave the CB clean for that.

### 4.2 Phase 1 — Server: remove all admin code from CB / generic paths (no client changes) — **COMPLETED**
| # | Item | Action | Status |
| :-- | :-- | :-- | :-- |
| A1 | `HomeBoard.java` L161-165 GM banner + `bypass admin_gmw` button | Delete. | **DONE** |
| A2 | `HtmlUtil.sendCBHtml` L415 hook → `tryInterceptAdminBoardHtml` | Delete hook + method; make `AdminAnnouncements` (4 calls) and `AdminGeodata` (2 calls) call `AdminBoardManager.sendAdminHtml(...)` directly. | **DONE** |
| A3 | `Player.sendPacket` ~L4565 → `tryInterceptAdminHtml` (content sniffing `bypass admin_` / `link admin/`) | Drop sniffing (`commandDepth > 0` only, gated on `allowAltG() && protocolVersion < 152`). | **DONE** |
| A4 | `AdminBoardManager.sendWrappedConsoleHtml` registers admin actions | Native path clears and populates **only** `HtmlActionScope.NPC_ITEM_HTML` (`itemId=1`), leaving `NPC_HTML` and `COMM_BOARD_HTML` untouched. | **DONE** |
| A5 | `RequestBypassToServer` ~L95 `admin_*` validation skip for GMs | Removed; GM console buttons/links now validate through `HtmlActionScope.NPC_ITEM_HTML`. | **DONE** |
| B | CB render mode in `AdminBoardManager`: `useCommunityBoard`, `boardOpen`, `sendingAdminCbHtml`, `Player CB` / `Close [X]` buttons, `new ShowBoard()` on `//gmw`, `sendCBHtml` branch; `admin_board` / `admin_board_close` / `admin_board_*` in `AdminAdmin` + `AdminCommands.xml` + `AdminScope.java` + `server_menu.htm` + `AdminCommandHandler.java` | Deleted all CB rendering code and `admin_board*` aliases (`admin_gmw <home\|refresh\|support\|player\|world\|events\|economy\|server\|legacy\|legacy_view>` is now used everywhere). **Note on `bypass -h` → `bypass` in `scrubAndAdaptHtml`:** Kept for `GMWnd.HtmlViewer` because `NConsoleWnd::BypassToServer` (`nwindow.dll` `VA 0x1021ebf7`) returns `eax = 2` on `-h`, causing `NCHtmlViewer::OnClickButton` (`VA 0x100ef523` → `0x100eda50`) to send close event `0xf0000004` to `GMWnd` itself! | **DONE** |
| C | `AccessLevels.xml` level 10 `allowAltg="false"`; Protocol-152 fallback | Set level 10 `allowAltg="true"` (`dist/game/config/AccessLevels.xml` + `build/dist/game/config/AccessLevels.xml`). Protocol ≥ 152 GMs receive one-line system notice `"Admin console requires the server's client package; use // commands."` instead of a CB fallback. | **DONE** |
| Keep | `sendCBHtml` 4090-char split, `ShowBoard` null-guard, `AbstractHtmlPacket` validated-branch cache clear | Generic 2009-client fixes, not admin-specific. | **KEPT** |

**Phase 1 verification (`grep` checks passed):**
- `grep -rn AdminBoardManager` in `handlers/bypass/communityboard`, `HtmlUtil.java`, and `RequestBypassToServer.java` returns **zero** hits.
- `AdminBoardManager.java` has **zero** references to `ShowBoard` or `admin_board*`.

### 4.3 Phase 2 — Client + Server: Native `GMWnd` Admin Panel v2 (joint design)
- Client Agent: `interface.xdat` / `interface.u` layout & controls (button feedback textures, Legacy category, any new native controls), within the zero-size-delta / `SerializeExpr` constraints documented in §4 of `MASTER_HANDOFF_AND_CLIENT_SERVER_GUIDE.md`.
- Server Agent (**COMPLETED**): Implemented matching server commands / packets / HTML for each native control and `[Legacy]` tab in `AdminBoardManager.java` & `AdminAdmin.java`:
  - `//gmw player [targetName]` (`admin_gmw player [targetName]`): Resolves `targetName` via `World.getInstance().getPlayer(targetName)` (if provided and online, sets `activeChar.setTarget(target)`) and opens the `Support & Players` (`support`) tab.
  - `//gmw legacy` (`admin_gmw legacy`): Opens the new `[Legacy]` (`legacy`) tab in `GMWnd.HtmlViewer`, providing 1-click native C++ inspector popup launchers, recall/sendhome shortcuts, `.dat` search instructions, and curated legacy `data/html/admin/*.htm` entry points.
  - `//gmw legacy_view <status|items|skills|quests|warehouse|clan> [targetName]`: Gated by `AdminScope.PLAYER_SUPPORT`; resolves target (by `targetName`, current `Player` target, or self) and sends `GMViewCharacterInfo`+`GMHennaInfo`, `GMViewItemList`+`GMHennaInfo`, `GMViewSkillInfo`, `GmViewQuestInfo`, `GMViewWarehouseWithdrawList`, or `GMViewPledgeInfo` (which automatically pops open the corresponding native C++ `GM*Wnd` via `EV_GMObserving*`), then re-renders the console so `NPC_ITEM_HTML` stays valid.

### 4.4 Phase 2 Design Spec — Native `GMWnd` (`Alt+G`) Admin Panel v2

#### A. Root Cause & Fix for Native `GMWnd` Button Visual Feedback Defect (Client Agent)
- **Forensic Root Cause in Stock NCSoft [`interface.xdat.orig`](file:///home/dwea/Projects/L2/Client%20Lineage%202%20Gracia%20Epilogue/system/backup_encrypted/interface.xdat.orig):**
  Every one of the 17 `Button` widgets inside `GMWnd` (`399160..404835`) was authored by NCSoft with:
  ```python
  NormalTex    = "L2UI_CT1.Button.Button_DF_Small"
  PushedTex    = "L2UI_CT1.Button.Button_DF_Small"  # IDENTICAL to NormalTex -> no pushed bevel!
  HighlightTex = "undefined"                        # No hover highlight texture!
  DropTex      = "undefined"
  NoHighlight  = 1 (\x01\x00\x00\x00)               # Suppresses hover state & 1px click label shift!
  ```
- **Client Fix in [`patch_gmwnd_client.py`](file:///home/dwea/Projects/L2/Client%20Lineage%202%20Gracia%20Epilogue/patch_gmwnd_client.py) (`interface.xdat`):**
  For all 17 `Button` widgets in `GMWnd`, replace the texture quartet and `NoHighlight` flag with the standard interactive Lineage 2 button definition:
  ```python
  NormalTex    = "L2UI_CT1.Button.Button_DF"
  PushedTex    = "L2UI_CT1.Button.Button_DF_Click"
  HighlightTex = "L2UI_CT1.Button.Button_DF_Over"
  DropTex      = "undefined"
  NoHighlight  = -1 (\xff\xff\xff\xff)
  ```
  Additionally, `NWindow.dll` supports custom literal English strings on any `Button` (`ButtonName = -9999` / `\xf1\xd8\xff\xff` + literal `ButtonNameText`) and `TextBox` (`sysstring = -9999` + literal `Text`), allowing crisp, descriptive English labels without modifying `sysstring-e.dat`.

#### B. Evaluation: Native C++ Controls vs `GMWnd.HtmlViewer` (`interface.u` / `interface.xdat` Constraints)
- **What Must & Should Be Native C++ Controls (`GMWnd` Left Dock `x=10..225`):**
  1. **Persistent Target/Filter `EditBox` (`GMWnd.EditBox`):** Auto-populates on in-game player target (`EV_TargetUpdate`, patched in `GMWnd.HandleTargetUpdate` `#12137` so untargeting never wipes typed input) and remains persistent across HTML page transitions.
  2. **Primary Console Navigation & Quick-Action Bar (Top Section of Left Dock):**
     - By repurposing the 2 obsolete retail petition buttons (`PetitionButton` `#12101`, `53` code bytes; `ForcePetitionButton` `#12089`, `60` code bytes) alongside `ChangeServerButton` (`#12081`, `64` code bytes), `InfoButton` (`#12105`), `TeleButton` (`#12131`), and `MoveButton` (`#12129`), the top of the native C++ dock provides instant, zero-latency hardware buttons for:
       - **`[Admin Home]`** (`ChangeServerButton` → `ExecuteCommand("//admin")`)
       - **`[Player Panel]`** (`PetitionButton` → `ExecuteCommand("//gmw player" @ m_hEditBox.GetString())`)
       - **`[Legacy Tools]`** (`ForcePetitionButton` → `ExecuteCommand("//gmw legacy")`)
       - **`[Target Debug]`** (`InfoButton` → `ExecuteCommand("//debug" @ m_hEditBox.GetString())`)
       - **`[Teleport To]`** (`TeleButton` → `ExecuteCommand("//teleportto" @ m_hEditBox.GetString())`)
       - **`[Instant Move]`** (`MoveButton` → `ExecuteCommand("//instant_move")`)
  3. **Client-Side `.dat` Search Trees (Middle Section of Left Dock):**
     - **`[Find Item]`** (`ItemListButton`), **`[Find NPC]`** (`NPCListButton`), **`[Find Skill]`** (`SkillListButton`) open `GMFindTreeWnd` using the filter string in `GMWnd.EditBox`. Only native UnrealScript can query the client's local `.dat` tables (`itemname-e.dat`, `npcname-e.dat`, `skillname-e.dat`) with real 32×32 icons and double-click `//summon`.
  4. **Compact "Legacy NCSoft GM Tools" Category (Bottom Section of Left Dock + Server `[Legacy]` Tab):**
     - The 6 native graphical player-inspector buttons (`DetailStatusButton`, `InventoryButton`, `MagicSkillButton`, `QuestButton`, `StoreButton`, `ClanButton`) and the 2 modal recall buttons (`RecallButton`, `SendHomeButton`) are visually separated and compacted (`h=22`) into a dedicated **`Legacy GM Inspector`** group at the bottom of the left dock (`y=276..430`), framed by its own `GroupBox_DF` background and `TextBox` section header.
     - **Discovery from `interface.u` `ScriptText`:** All 6 native graphical inspector popups (`GMDetailStatusWnd`, `GMInventoryWnd`, `GMMagicSkillWnd`, `GMQuestWnd`, `GMWarehouseWnd`, `GMClanWnd`) register `EV_GMObserving*` events and automatically call `m_hOwnerWnd.ShowWindow()` whenever the server sends `GMViewCharacterInfo`, `GMViewItemList`, `GMViewSkillInfo`, `GmViewQuestInfo`, `GMViewWarehouseWithdrawList`, or `GMViewPledgeInfo`. Therefore, the server's new **`[Legacy]`** tab in `GMWnd.HtmlViewer` can ALSO pop open those exact native C++ graphical windows on click via a server command (`//gmw legacy_view <type> [target]`)!
- **What Should Remain in `GMWnd.HtmlViewer` (`744×492` Right Workspace):**
  - Dynamic multi-field forms, live server telemetry, paginated player/spawn/skill/item tables, phase progression controls, hot-reload controls, RBAC audit logs, and adapted `data/html/admin/*.htm` pages (`Home | Support & Players | World & Spawns | Events | Economy | Server & Audit | Legacy`). Adding dozens of dynamic `EditBox`/`ComboBox` controls in `interface.u` is impossible without shifting the 14,000-entry Export Table, whereas `GMWnd.HtmlViewer` (`ItemDescWnd` `itemId=1` zero-PageLock route) renders rich tables and forms with zero client size delta.

#### C. Native Control → Command / Packet → RBAC Scope Mapping Table

| Dock Section | Widget ID (`GMWnd.*`) | Button Label (`interface.xdat`) | Pos `(x, y)` & Size `(w, h)` | Client Action (`interface.u`) | Server Handler / Packet | Required `AdminScope` RBAC |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Header / Input** | `TextBox` | `"Target / Search:"` | `(12, 36)` `110×14` | Static label (`sysstring=-9999`) | — | — |
| **Header / Input** | `EditBox` | *(Input Field)* | `(12, 52)` `211×20` | Auto-fills on player target (`HandleTargetUpdate` `#12137`) | — | — |
| **1. Primary Console** | `ChangeServerButton` | `"Admin Home"` | `(12, 78)` `103×24` | `ExecuteCommand("//admin")` (`#12081`) | `AdminAdmin` → `AdminBoardManager` (`Home` tab) | Any GM (`isGM() && allowAltG()`) |
| **1. Primary Console** | `PetitionButton` | `"Player Panel"` | `(120, 78)` `103×24` | `ExecuteCommand("//gmw player" @ EditBox)` (`#12101`) | `AdminAdmin` → `AdminBoardManager` (`Support & Players` tab for target) | `PLAYER_SUPPORT` |
| **1. Primary Console** | `ForcePetitionButton` | `"Legacy Tools"` | `(12, 105)` `103×24` | `ExecuteCommand("//gmw legacy")` (`#12089`) | `AdminAdmin` → `AdminBoardManager` (`Legacy` tab) | Any GM (`isGM() && allowAltG()`) |
| **1. Primary Console** | `InfoButton` | `"Target Debug"` | `(120, 105)` `103×24` | `ExecuteCommand("//debug" @ EditBox)` (`#12105`) | `AdminDebug` (`admin_debug`) | `PLAYER_SUPPORT` |
| **1. Primary Console** | `TeleButton` | `"Teleport To"` | `(12, 132)` `103×24` | `ExecuteCommand("//teleportto" @ EditBox)` (`#12131`) | `AdminTeleport` (`admin_teleportto`) | `TELEPORT` |
| **1. Primary Console** | `MoveButton` | `"Instant Move"` | `(120, 132)` `103×24` | `ExecuteCommand("//instant_move")` (`#12129`) | `AdminTeleport` (`admin_instant_move`) | `TELEPORT` |
| **2. `.dat` Search** | `ItemListButton` | `"Search Item"` | `(12, 182)` `103×23` | `GMFindTreeWnd.ShowItemList(EditBox)` (`#12092`) | Double-click sends `//summon <itemId> <count>` | `ECONOMY_ITEMS` |
| **2. `.dat` Search** | `NPCListButton` | `"Search NPC"` | `(120, 182)` `103×23` | `GMFindTreeWnd.ShowNPCList(EditBox)` (`#12093`) | Double-click sends `//summon <npcId+1000000>` | `WORLD_SPAWN` |
| **2. `.dat` Search** | `SkillListButton` | `"Search Skill"` | `(12, 208)` `211×23` | `GMFindTreeWnd.ShowSkillList(EditBox)` (`#12090`) | Double-click sends `//summon <skillId+2000000> <lvl>` | `CHAR_EDIT` |
| **3. Legacy GM Tools** | `DetailStatusButton` | `"View Status"` | `(12, 258)` `103×22` | `RequestGMCommand(1, EditBox)` (`#12109`) | `RequestGMCommand` (`0x81` cmd `1`) → `GMViewCharacterInfo` | `PLAYER_SUPPORT` |
| **3. Legacy GM Tools** | `InventoryButton` | `"View Items"` | `(120, 258)` `103×22` | `RequestGMCommand(5, EditBox)` (`#12108`) | `RequestGMCommand` (`0x81` cmd `5`) → `GMViewItemList` | `PLAYER_SUPPORT` |
| **3. Legacy GM Tools** | `MagicSkillButton` | `"View Skills"` | `(12, 283)` `103×22` | `RequestGMCommand(3, EditBox)` (`#12107`) | `RequestGMCommand` (`0x81` cmd `3`) → `GMViewSkillInfo` | `PLAYER_SUPPORT` |
| **3. Legacy GM Tools** | `QuestButton` | `"View Quests"` | `(120, 283)` `103×22` | `RequestGMCommand(4, EditBox)` (`#12106`) | `RequestGMCommand` (`0x81` cmd `4`) → `GmViewQuestInfo` | `PLAYER_SUPPORT` |
| **3. Legacy GM Tools** | `StoreButton` | `"Warehouse"` | `(12, 308)` `103×22` | `RequestGMCommand(6, EditBox)` (`#12104`) | `RequestGMCommand` (`0x81` cmd `6`) → `GMViewWarehouseWithdrawList` | `PLAYER_SUPPORT` |
| **3. Legacy GM Tools** | `ClanButton` | `"View Clan"` | `(120, 308)` `103×22` | `RequestGMCommand(2, EditBox)` (`#12103`) | `RequestGMCommand` (`0x81` cmd `2`) → `GMViewPledgeInfo` | `PLAYER_SUPPORT` |
| **3. Legacy GM Tools** | `RecallButton` | `"Recall Chr"` | `(12, 333)` `103×22` | Confirm dialog → `ExecuteCommand("//recall" @ EditBox)` | `AdminTeleport` (`admin_recall`) | `TELEPORT` |
| **3. Legacy GM Tools** | `SendHomeButton` | `"Send Home"` | `(120, 333)` `103×22` | Confirm dialog → `ExecuteCommand("//sendhome" @ EditBox)` | `AdminSendHome` (`admin_sendhome`) | `TELEPORT` |
| **Right Workspace** | `texBack` + `HtmlViewer` | *(RBAC HTML View)* | `(232, 34)` `752×500` | `ItemDescWnd` (`#12028`/`#12029`) `itemId=1` | `NpcHtmlMessage(0, 1, html)` (`NPC_ITEM_HTML` cache) | Per-tab / per-command `AdminScope` |

---

## 5. International / Cyrillic / Greek / Slavic Text Input (`NWindow.dll`) & Server Name Policy

### 5.1 Root Cause of Blocked Non-ASCII Keyboard Input When `Language=0`
- **Wine / `windrv.dll` Unicode Delivery:** `windrv.dll` registers a Unicode window (`RegisterClassExW` / `PeekMessageW` / `DispatchMessageW`) and passes UTF-16 codepoints (`wParam`) in `WM_CHAR` (`0x0102`) directly through `ViewportWndProc` (`0x1110ffc8..0x1111001a`) to `NWindow.dll`.
- **Font Rendering (`tahoma.ttf`):** All UI text and 3D world overhead text in Gracia Epilogue (`2009-09-28`) render via `Engine.dll`'s `UFontManager` + `UCanvas::DrawTextTTFToCanvas` backed by `C:\windows\Fonts\tahoma.ttf` (configured at the end of `interface.xdat`), which already contains full glyph coverage for Latin-Extended (`U+00A0..U+017E`), Greek (`U+0370..U+03FF`), and Cyrillic (`U+0400..U+04FF`).
- **`NWindow.dll` `GLanguage == 8` Input Gates:** Both `NCEditBox::OnChar` (`VA 0x100d44ee`) and `NCMultiEditBox::OnChar` (`VA 0x100a3468`) checked `GLanguage == 8` (`*0x102dc384 == 8`, Russian `l2.ini` mode) before accepting non-ASCII characters, and even when `GLanguage == 8`, restricted input to `U+0410..U+044F` (`А..я`), dropping Russian/Belarusian `Ё/ё`, Ukrainian `Є/І/Ї/Ґ`, Serbian/Macedonian `Ђ/Ѓ/Ј/Љ/Њ/Ћ/Ќ/Џ`, Greek, and West/South Slavic Latin-Extended.

### 5.2 Client Patch (`patch_nwindow_unicode.py` → `system/nwindow.dll`)
- **Base Binary:** Clean `2009-09-28` (`Protocol 148`) `NWindow.dll` (`5,425,684` bytes, original SHA-256 `dcd951c811002318f3164f1221f1b8c9d7466c17f3105c0b7ece49cb0e3bbb45`, backed up to `system/backup_encrypted/nwindow.dll.orig`).
- **Patched Binary:** `5,425,684` bytes (**0-byte size delta**), SHA-256 `5d09c16151795b7369f4190bdd4a0dc65a1e986e15129c6ba2bb16db082dac66`:
  1. **`NCEditBox::OnChar` (`VA 0x100d44ee..0x100d450c`, file offset `0x000D38EE`, 30 bytes):**
     - Replaced `mov ecx,[0x102dc384]; cmp [ecx],8; jne; lea edx,[ebx-0x410]; cmp edx,0x3f; ja` with `movzx eax,bx; sub ah,3; cmp eax,0x6f; jbe 0x100d482e; add ax,0x260; cmp ax,0x45f; ja 0x100d482e; nop` (`0fb7c380ec0383f86f0f863103000066056002663d5f040f872303000090`).
     - Unconditionally allows **`U+00A0..U+04FF`** (Latin-1 Supplement, Latin Extended-A/B, IPA, Greek, and full Cyrillic) while keeping `Language=0` (`*-e.dat`), and rejects Combining Diacritical Marks **`U+0300..U+036F`** to prevent "Zalgo" stacking spam.
  2. **`NCMultiEditBox::OnChar` (`VA 0x100a3468..0x100a347e`, file offset `0x000A2868`, 22 bytes):**
     - Replaced with identical logic on `si` (`0fb7c680ec0383f86f762766056002663d5f04771d90`) for multi-line fields (mail bodies, board memos, petitions).
  3. **`NWndUtil::GetNativeStrLen` (`VA 0x101d9326..0x101d9331`, file offset `0x001D8726`, 11 bytes):**
     - Replaced `add ecx,-0x410; cmp ecx,0x3f; ja` with `add ecx,-0x80; cmp ecx,0x47f; ja` (`83c18081f97f0400007708`) so all characters in `U+0080..U+04FF` count as 1 character (instead of 2 CJK columns) toward `EditBox`/`MultiEditBox` length limits.
  4. **`IsObsceneWord` (`VA 0x10215fbb..0x10215fc6`, file offset `0x002153BB`, 11 bytes):**
     - NOPed out (`90` × 11) the `GLanguage == 8` gate on Cyrillic uppercase-to-lowercase normalization.
  5. **PE `.reloc` Neutralization (3 `IMAGE_REL_BASED_HIGHLOW` entries → `0x0000` `IMAGE_REL_BASED_ABSOLUTE`):**
     - Because `NWindow.dll` has preferred `ImageBase = 0x10000000` (colliding with `Core.dll`), Windows/Wine always rebases it at load time. The original `mov reg, [0x102dc384]` instructions at `RVA 0x000a346a`, `RVA 0x000d44f0`, and `RVA 0x00215fbd` had `IMAGE_REL_BASED_HIGHLOW` entries in `.reloc` (`file offset 0x004C80C2`: `6a 34`, `0x004CAC1C`: `f0 34`, `0x004E2024`: `bd 3f`) which otherwise added `BaseDelta` into the middle of the patched instructions at runtime. Zeroing those 3 entries (`00 00`) prevents relocation corruption (verified across all `210,170` active `.reloc` entries).

### 5.3 Server-Side Naming vs Chat/Mail/Title Policy (`360e662efb3`)
- **Names (Strict English-Only `[A-Za-z0-9]`):** Enforced server-side across character creation (`CnameTemplate`), pet names (`PetNameTemplate`), clan names (`ClanNameTemplate`), sub-pledge names, and alliance names in `game/config/Server.ini` and packet handlers (`360e662efb3`).
- **Chat, Whisper, Mail, Titles, Community Board, Petitions, HTML Inputs:** Full UTF-16 (`U+00A0..U+04FF` and standard ASCII) supported end-to-end (client UTF-16LE packets ↔ Java `String` ↔ MariaDB `utf8mb4`).

---

## 6. `[AutoLogOn]` 4-Stage One-Shot State Machine (`NWindow.dll`) & Modernized `system/user.ini`

### 6.1 Why `[AutoLogOn]` in `l2.ini` Previously Did Nothing
- In the retail `2009-09-28` (`Protocol 148`) client binaries (`L2.bin`, `Engine.dll`, `NWindow.dll`), the config keys `IsL2AutoLogOn`, `L2ID`, `L2Passwd`, and `L2Slot` in `l2.ini` (`[AutoLogOn]`) were completely stripped from the retail C++ build (zero references to those strings exist in any `.exe`, `.dll`, or `.u` file).
- Furthermore, the login flow in Gracia Epilogue is driven by `NWindow.dll`'s C++ `UIStateManager` (`LoginState` → `EulaMsgState` → `ServerListState` → `CharacterSelectState`), not UnrealScript.

### 6.2 4-Stage One-Shot PIC State Machine in `NWindow.dll` (`patch_nwindow_unicode.py`)
- **ASLR / PE `.reloc` Safety:** Because `NWindow.dll` (`ImageBase = 0x10000000`) is always relocated at runtime by Wine/Windows, the entire 477-byte `.text` code cave (`VA 0x102dbe20..0x102dbffd`, file offset `0x002DB220..0x002DB3FD`) is written as **100% Position-Independent Code (PIC)** using PC-relative `call rel32` and a runtime base resolver `GetBaseEbx` (`call +0; pop ebx; lea ebx, [ebx + (0x104ba9c0 - .Lpic0)]`). All 5 hook sites and the cave have **zero active `.reloc` overlaps** (verified across all `210,170` active `.reloc` entries).
- **PE `.text` `Misc.VirtualSize` (`file offset 0x00000218`):** Extended from `0x002dae1c` to `0x002db000` (matching `SizeOfRawData = 0x002db000`) so the PE loader maps the `.text` slack cave `0x102dbe20..0x102dc000` as executable.
- **`.data` Strings & State Block (`VA 0x104ba960..0x104ba9e0`, file offset `0x004B9160..0x004B91E0`, 128 bytes):**
  - Stores UTF-16LE strings `L"AutoLogOn"`, `L"IsL2AutoLogOn"`, `L"L2ID"`, `L"L2Passwd"`, `L"L2Slot"`, `L"l2.ini"`, and 4 state variables: `STATE_Done` (`0x104ba9d0`), `STATE_Active` (`0x104ba9d4`), `STATE_Slot` (`0x104ba9d8`), and `STATE_TargetChars` (`0x104ba9dc`).
- **4-Stage Flow:**
  1. **Stage 1 (`NCAuthWnd::ShowWindow` @ `VA 0x1014336a`, file offset `0x0014276A`):**
     - Calls `NCAuthWnd::InitKeyCrypt` (`0x10143170`).
     - Checks `STATE_Done`: if already `1` (e.g., user logged out to login screen or login failed), sets `STATE_Active = 0` and returns immediately so it never loops.
     - On first launch (`STATE_Done == 0`), sets `STATE_Done = 1`, initializes `nSlot = -1`, and queries `*GConfig` (`*0x102dc4b4`) for `IsL2AutoLogOn` (`vtable[0x00]` `GetBool`), `L2Slot` (first checked via `vtable[0x0c]` `GetString` so an empty `L2Slot=` or omitted key stays `-1`, otherwise parsed via `vtable[0x04]` `GetInt`), `L2ID` (`vtable[0x0c]` `GetString`), and `L2Passwd` (`vtable[0x0c]` `GetString`) in `[AutoLogOn]` of `l2.ini`.
     - If `IsL2AutoLogOn` is true and both `L2ID` and `L2Passwd` are non-empty, copies `L2ID` into `m_pIDEditBox` (`[esi+0x1e4]+0x200`), sets `STATE_Active = 1`, and calls `NConsoleWnd::RequestAuthLogin(GConsoleWnd, id_wstr, pwd_wstr, 0)` (`0x1021a160`).
  2. **Stage 2 (`NConsoleWnd::AcceptLogin` @ `VA 0x1024e924`, file offset `0x0024DD24`):**
     - When `STATE_Active == 1`, sets `ZF = 1` (`cmp ebx, ebx`) so `jle 0x1024e9fa` takes the native EULA-skip branch directly to `ServerListState` + `NConsoleWnd::RequestServerList`.
  3. **Stage 3 (`NCServerSelectWnd::EndServerInfo` @ `VA 0x101274d6`, file offset `0x001268D6`):**
     - After `NCServerSelectWnd::ShowWindow` (`vtable[0x238]`), if `STATE_Active == 1`, advances to `STATE_Active = 2`, ensures `m_pServerList` (`[esi+0x1e4]`) has a selected server row (`[ecx+0x250] = 0` if `< 0`), and calls `NCServerSelectWnd::OnLoginBtnClick` (`0x1012a7c0`).
  4. **Stage 4a (`NConsoleWnd::ReceiveCharacterInfo` @ `VA 0x1024e4bf`) & Stage 4b (`NConsoleWnd::AddCharacterInfo` @ `VA 0x1024e406`):**
     - In `ReceiveCharacterInfo(int numChars)`, if `STATE_Active == 2` and `numChars > 0`, records `STATE_TargetChars = numChars` and advances to `STATE_Active = 3` (if `numChars <= 0`, resets `STATE_Active = 0` at the empty character lobby so the player can create a character).
     - In `AddCharacterInfo`, after `NCLobbyWnd::AddCharacter` (`0x101469c0`), when `STATE_Active == 3` and added characters `[esi+0x1e4] >= STATE_TargetChars`, consumes the one-shot state (`STATE_Active = 0` so subsequent manual `Restart` to character lobby never re-triggers auto-enter). If `L2Slot` (`STATE_Slot`) is `< 0` (e.g. `L2Slot=` empty, omitted, or `-1`) or `>= numChars`, it stops cleanly at the **Character Selection Lobby**; if `0 <= L2Slot < numChars`, it calls `NCLobbyWnd::SelectCharacter` (`0x10146280`) followed by `NCLobbyWnd::GameStart` (`0x101455f0`).

### 6.3 Modernized `system/user.ini` Restoration & DualSense PS5 Right Stick Mapping
- Restored the modernized [`system/user.ini`](file:///home/dwea/Projects/L2/Client%20Lineage%202%20Gracia%20Epilogue/system/user.ini) to both `Client Lineage 2 Gracia Epilogue/system/user.ini` and `l2jmobius_dwea_fork/client/system/user.ini` (translated comments, Hall-Effect/DualSense `DeadZone=0.07`, D-pad bindings `PovUp`/`PovDown`/`PovLeft`/`PovRight`, and `NextTargetMode=0` `3000`-unit target cycling).
- **DualSense PS5 DirectInput Axis Mapping (`hid-playstation` + Wine DirectInput):** On a Sony DualSense PS5 controller, `JoyU` (`Rx`) and `JoyR` (`Ry`) are the **L2 and R2 analog triggers** (which rest at `-1.0` when unpressed), whereas **`JoyZ` (`Z`) is Right Stick Horizontal** and **`JoyV` (`Rz`) is Right Stick Vertical**. Therefore, `JoyZ` (`CameraYaw Factor=38000 DeadZone=0.07 Direction=1`) and `JoyV` (`CameraPitch Factor=38000 DeadZone=0.07 Direction=1`) are bound to the Right Stick camera while `;JoyU=` and `;JoyR=` remain unbound so unpressed triggers never spin or tilt the camera.

---

## 7. Native Client Geodata Builder (`///buildzone` / `FWorldBuilder`) & L2J Geodata Converter

### 7.1 `Engine.dll` Builder Command Dispatch (`VA 0x202b37b4..0x202b45a2`)
All `///` builder console commands in [`Engine.dll`](file:///home/dwea/Projects/L2/Client%20Lineage%202%20Gracia%20Epilogue/system/Engine.dll) (`ImageBase = 0x20000000`) require two gates:
1. `bIsBuilder == 1` (`VA 0x202a898f`, file offset `0x002A7D8F`, already unlocked in `Engine.dll`).
2. `GIsEditor == 0` (`*0x20446464 == 0`, imported from `Core.dll!?GIsEditor@@3HA`), which is always `0` in the normal game client.

| Command | Handler VA | Callee / Global State | Behavior & Parameters |
| :--- | :--- | :--- | :--- |
| `///buildzone [Map=<X>_<Y>.unr] [Width=<INT>] [Post=<INT>]` | `0x202b37b4` | `FWorldBuilder::buildWorldStructure` (`0x2018be10`, ordinal `9236`) | Defaults: `Map=""`, `Width=128` (`0x80`), `Post=0`. Builds world structure geodata cells into `[0x206daf58]` (`32 MB` `FWorldStructure` grid). |
| `///loadpath FILE=<path>` | `0x202b3891` | `FWorldBuilder::LoadPath` (`0x2018bb20`) | Parses an ASCII `<X>_<Y>_path.txt` file into the `[0x206daf58]` `FWorldStructure` grid and sets `[0x206e3204] = 1`. |
| `///c_rmode geodata` | `0x202b3c28` | `[0x206daf48]` (`0` $\leftrightarrow$ `1`) | Toggles real-time 3D viewport rendering of the loaded `[0x206daf58]` geodata cells and NSWE passability arrows. |
| `///c_rmode seamless` | `0x202b3d17` | `[0x206daf44]` (`0` $\leftrightarrow$ `1`) | Toggles seamless map boundary visualization. |
| `///maploc` | `0x202b3e43` | `GLog->Logf` | Prints current region coordinates (`MapX=%d, MapY=%d`). |
| `///showborderline` | `0x202b3f4f` | `[0x206e31e4]` (`0` $\leftrightarrow$ `1`) | Toggles region/block border lines in the 3D viewport. |
| `///showsectors` | `0x202b3f79` | `[0x206e31e8]` (`0` $\leftrightarrow$ `1`) | Toggles sector overlay rendering. |
| `///linecheck` | `0x202b3fa3` | `[0x206e3208]` (`0` $\leftrightarrow$ `1`) | Toggles interactive line-of-sight / collision raycast debugging. |
| `///batch File=<path>` | `0x202b4548` | `UGameEngine::StartCommandMacro` (`0x202bf590`) | Reads and executes a macro script of console commands (`;` comments, `{...}` blocks). |

### 7.2 `FWorldBuilder::buildWorldStructure` (`VA 0x2018be10..0x2018df0d`) Internals
- **Signature:** `int __thiscall FWorldBuilder::buildWorldStructure(FWorldBuilder* this, ULevel* Level, const TCHAR* MapName, UViewport* Viewport, INT Width, INT Post)`
- **`Unrealed.ini` `[WorldBuildOptions]` Configuration (`VA 0x2018c424..0x2018c49e`):**
  Queries `GConfig->GetBool(L"WorldBuildOptions", Key, &val, L"Unrealed.ini")`, defaulting all 3 options to `1` (`TRUE`) if `Unrealed.ini` is absent:
  1. `RunWalkbuild` (`[ebp-0x50]`, default `1`): Enables multi-threaded/pawn walkability flood-fill (`FindReachable`).
  2. `GeoTestDuringBuild` (`[ebp+0x10]`, default `1`): When `1`, runs three slow `2032 × 2032` debug verification passes (`%d_%d_BuildTest_1_TopMostCells.txt`, `%d_%d_BuildTest_2_WithWalk.txt`, `%d_%d_BuildTest_3_AfterPostprocessing.txt`) and compares against `.\GeoData\%d_%d_conv.dat` (`VA 0x203902f0`). **Setting `GeoTestDuringBuild=0` in `system/Unrealed.ini` skips all 3 debug dump passes and `_conv.dat` comparison.**
  3. `RunPostbuild` (`[ebp-0x70]`, default `1`): Runs `FWorldBuilder::ComboPostProcessing` (`VA 0x2018a240`, up to 20 passes pruning inconsistent diagonal/cardinal transitions) when `Post != 0` or `GIsEditor != 0`.
- **Two Execution Paths Controlled by `this->Viewport` (`[ebx+0x04]`):**
  1. **Full-Map Batch Mode (`this->Viewport == NULL`, `[ebx+0x04] == 0`, used by `UnrealEd` via ordinal `9236`):**
     - Parses `MapX` (`[ebp+0x48]`) and `MapY` (`[ebp+0x6c]`) from the filename stem of `MapName` and strips the last 4 characters (`.unr` at `VA 0x2018bf22`: `mov word ptr [ebp+eax*2-0x5d8], 0`).
     - Allocates the `0x2000018`-byte (`32 MB`) `FWorldStructure` grid at `[0x206daf58]` covering `minX = (MapX - 20) << 15`, `minY = (MapY - 18) << 15`, `width = 0x7fff`, `height = 0x7fff`.
     - Spawns a temporary `LineageWarrior.FFighter` collision probe actor (`FWorldBuilder::getScout` at `VA 0x201899c0`), collects all `APathNode` seed actors in `ULevel`, runs OpenMP 2-thread `SetInitialPoints` (`VA 0x2018a0d0` $\rightarrow$ `0x20189ec0`) across all `2048 × 2048` cells and OpenMP 2-thread `FindReachable` (`VA 0x2018b9a0` $\rightarrow$ `0x2018b760`), and writes **`<MapX>_<MapY>_path.txt`** (`"%S_path.txt"` at `VA 0x2018dd39..0x2018dea9`).
  2. **In-Game Client Interactive Mode (`this->Viewport != NULL`, `[ebx+0x04] != 0`, passed by `///buildzone` at `VA 0x202b384c`):**
     - Because `///buildzone` at `VA 0x202b384c` passes `Viewport = [esi+0xe78]` (non-null), `[ebx+0x04]` is non-zero. In this mode, `FWorldBuilder::buildWorldStructure` uses the player's own `APawn`, restricts the bounding box `[ebx+0x14..0x20]` to `[player.X ± Width/2, player.Y ± Width/2]` (default `128 × 128` units = `8 × 8` cells), steps the player pawn visually across the local patch while redrawing the viewport, and **skips writing `<MapName>_path.txt`** at `VA 0x2018dd39` (`cmp dword ptr [ebx+0x4], 0; jne 0x2018dee0`) so the built patch is only held in `[0x206daf58]` for in-game inspection via `///c_rmode geodata`.

### 7.3 `<X>_<Y>_path.txt`, `<X>_<Y>_conv.dat`, and `<X>_<Y>.l2j` Format Specification & Converter (`convert_buildzone_to_l2j.py`)
1. **`<X>_<Y>_path.txt` Direction & Height Mapping (`VA 0x2018de50..0x2018de7e` & `0x20467b08`):**
   - Header: 5 lines (`total : %d`, `visited : %d`, `(%d, %d) ~ (%d, %d)`, `cellsize : 16`, `heightunit : 8`) + blank line.
   - Body: `2048 × 2048 = 4,194,304` lines (`x = 0..2047` outer, `y = 0..2047` inner):
     `[<x>,<y>]<num_layers>(<height_1>:<d0><d1><d2><d3>)(<height_2>:<d0><d1><d2><d3>)...`
   - The 4 directional passability flags `d0, d1, d2, d3` (`[node+0x10..0x1c]`) are indexed by the direction table at `VA 0x20467b08`:
     - `d0` (`dir[0]`): `dx = +1, dy =  0` $\rightarrow$ **East** (`Cell.NSWE_EAST = 1 << 0 = 0x01`)
     - `d1` (`dir[1]`): `dx = -1, dy =  0` $\rightarrow$ **West** (`Cell.NSWE_WEST = 1 << 1 = 0x02`)
     - `d2` (`dir[2]`): `dx =  0, dy = +1` $\rightarrow$ **South** (`Cell.NSWE_SOUTH = 1 << 2 = 0x04`)
     - `d3` (`dir[3]`): `dx =  0, dy = -1` $\rightarrow$ **North** (`Cell.NSWE_NORTH = 1 << 3 = 0x08`)
     - Combined L2J NSWE byte: `nswe = (d0 << 0) | (d1 << 1) | (d2 << 2) | (d3 << 3)`.
   - 16-bit Layer Word Encoding (`VA 0x2018da4a..0x2018da58` & [`Region.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/geoengine/geodata/regions/Region.java#L157-L158)):
     - Encode: `word = (((height & ~7) << 1) & 0xFFF0) | (nswe & 0x0F)` (little-endian `uint16`).
     - Decode: `height = ((short)(word & 0xFFF0)) >> 1`, `nswe = word & 0x0F`.
2. **Converter Tool ([`tools/convert_buildzone_to_l2j.py`](file:///home/dwea/Projects/L2/Client%20Lineage%202%20Gracia%20Epilogue/tools/convert_buildzone_to_l2j.py) & [`client/tools/convert_buildzone_to_l2j.py`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/client/tools/convert_buildzone_to_l2j.py)):**
   - Converts `<X>_<Y>_path.txt` $\rightarrow$ `<X>_<Y>.l2j` (`TYPE_FLAT=0`, `TYPE_COMPLEX=1`, `TYPE_MULTILAYER=2`).
   - Converts L2OFF `<X>_<Y>_conv.dat` (18-byte header + `0x0000`/`0x0040`/`>0x0040` blocks) $\rightarrow$ `<X>_<Y>.l2j`.
   - Converts `<X>_<Y>.l2j` $\rightarrow$ `<X>_<Y>_path.txt` (`--to-path-txt`) so any server `.l2j` region can be loaded into the client with `///loadpath` and rendered in 3D with `///c_rmode geodata`.
   - Validates `.l2j` files against all [`Region.java`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/geoengine/geodata/regions/Region.java) off-heap `MemorySegment` invariants (`--verify`).

---

## 8. Config Cleanup, Custom CB Buffer, FakePlayers, 196-Region Geodata, 2h Buffs & Full `CT2.4` Player Skills Audit (`4c29c75ef25`..`1a4b56efd31`)

1. **GameServer Config Cleanup, Virtual-Thread XML Loader, Custom Community Board Buffer & FakePlayers (`4c29c75ef25`):**
   - Purged 13 dead `.ini` config properties and `*Config.java` fields across `GeoEngine.ini`, `Server.ini`, `General.ini`, `NPC.ini`, `Feature.ini`, `ConquerableHallSiege.ini`, and `Pvp.ini`.
   - Replaced busy-wait spin-loops (`while (!tasks.isEmpty())`) and non-atomic counter increments in `IXmlReader.java`, `ItemData.java`, and `SkillData.java` (`ThreadsForLoading`) with `Executors.newVirtualThreadPerTaskExecutor()` and `AtomicInteger`.
   - Unlocked all 70 skills used in `data/html/CommunityBoard/Custom/buffer/*.html` inside `CommunityBoard.ini` (`CommunityAvailableBuffs`), fixed `HomeBoard.java` to validate buffs before deducting currency, fixed the `buffer/main-damces` typo in `main-dances.html`, wired `main-presets.html`, fixed the impossible boolean guard `(npcId < 80000) || (npcId > 89999)` in `SpawnData.java:164`, and enabled `FakePlayers`.
2. **196-Region Off-Heap Geodata & 2-Hour (`7200s`) Buff Durations (`8aa3b62f4d2`, `afd3d5d84b6`):**
   - Provisioned 196 `.l2j` geodata region files (`1.2 GB`, gitignored via `.gitignore`) in `dist/game/data/geodata/` and `build/dist/game/data/geodata/`.
   - Enabled `EnableModifySkillDuration = True` with 2-hour (`7200s`) durations for all 110 buffer/song/dance/prophecy/summon support skills in `Player.ini`.
3. **Gracia Epilogue (`CT2.4`) Player Skills, Channelizer, Momentum Charges & 3rd-Profession Passives/Toggles/Triggers Audit (`ff520e82973`, `e9a8b96d6a2`, `1a4b56efd31`):**
   - Fixed Duelist / Grand Khavatari max 8 momentum charges (`FocusMaxEnergy.java`, `FocusEnergy.java`, `918`, `919`, `345`, `346`, `5127`, `54`).
   - Fixed `GROUND` channeling skills (`Volcano` `1419`, `Cyclone` `1420`, `Raging Waves` `1421`, `Gehenna` `1423`) and shot consumption per tick in `SkillChannelizer.java`, restored 7-arrow channeling on `924` (*Seven Arrow*) and `1472`/`1545` (*Soul Sucking*), removed High Five `CallSkill` `23299` from `772` (*Arrow Rain*), uncommented `DamOverTime` on `1467` (*Meteor*) and `1468` (*Star Fall*), and fixed `+Duel` enchant PvE scope against `Door`/`SiegeFlag` targets in `Skill.java`.
   - Moved Ghost Sentinel's 8 Lv81–82 Forgotten Scroll skills from `2ndClass/PhantomRanger.xml` to `3rdClass/GhostSentinel.xml` (`e9a8b96d6a2`).
   - Fixed `TriggerSkillByDamageDealt.java:91` (`_isCritical && !event.isCritical()`), `TriggerSkillByDamageReceived.java`, `Creature.makeTriggerCast` + `<targetType>SELF</targetType>` on Tank Lv83 Seed sub-skills `5561`–`5564` (unlocking `762` *Insane Crusher*, `785` *Flame Icon*, `787` *Touch of Eva*, `789` *Spirit of Shilen*), `5426` (*Counter Chance*) `mAtkSpd` (`1.15`), `CreatureStat.getMCriticalHit` precision for `945` (*Magician's Will*), `Formulas.calcEffectAbnormalTime` `Skill Mastery` (`330`/`331`) passive/toggle duration guard, `335` (*Fortitude*) `MpConsumePerLevel`, `767`/`5565` (*Expose Weak Point*), and `1498` (*Mutual Response*) `<allowSkillAttack>true</allowSkillAttack>` (`1a4b56efd31`).

---

## 9. Multi-Agent Roster, Role Separation & Automated Auditor Routines (`2026-10-17`)

To prevent context overload and keep DevOps separate from auditing/documentation governance, the project operates with **4 specialized autonomous agents** communicating via `send_message`:

| Agent # | Role & Title | Conversation ID (`send_message` Recipient) | Primary Ownership & Scope |
| :---: | :--- | :--- | :--- |
| **Agent 1** | **Server Core & Datapack Agent** | `6117006f-8ccb-4196-bebe-1ba7b09aea0f` | `l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/` Java 27 core, packet serialization (`148`/`152`), Panama FFM `GeoEngine`, hot-reload engine, RBAC `AdminBoardManager`, datapack XML/scripts, and `build/dist/libs/GameServer.jar` compilation. *(Note: `4a4db620-c2a4-4949-be36-98f66c3f3191` is an archived Oct 14 Ansible session — always use `6117006f-8ccb-4196-bebe-1ba7b09aea0f` for Server Agent!)* |
| **Agent 2** | **Client & Reverse-Engineering Agent** | `9379ca1e-d28c-4127-8680-c8e529c91ed7` | `Client Lineage 2 Gracia Epilogue/` (mirrored to `l2jmobius_dwea_fork/client/`): x86 PE32 binary patches (`l2.exe`, `Engine.dll`, `NWindow.dll`, `d3ddrv.dll`, `windrv.dll`, `dsetup.dll`), `interface.u`/`interface.xdat` (`patch_gmwnd_client.py`), `patch_nwindow_unicode.py`, `launch-l2.sh`, `user.ini`, and `tools/convert_buildzone_to_l2j.py`. |
| **Agent 3** | **DevOps, Docker & Infrastructure Agent** | `f996ed99-9472-40bb-9157-05630ba9775b` | Local Docker Compose stack (`docker-compose.yml`, `docker/run-server.sh`, `l2-db`, `l2-login`, `l2-game`), Proxmox LXC Terraform (`terraform/`), Ansible (`ansible/`), container restarts, and live runtime log monitoring. |
| **Agent 4** | **Project Auditor, Documentation & Context Governance Agent** | `38e33746-e411-44db-8ed9-525f8cf93b55` | Deep cross-cutting code/datapack/client forensic audits, maintaining all master documentation & lessons-learned dossiers across all 4 roots and agent brain directories, running `tools/auditor/l2_auditor.py` routines, and keeping all 4 agents' context synchronized. |

### Automated Auditor CLI & Subroutines ([`tools/auditor/l2_auditor.py`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork/tools/auditor/l2_auditor.py))
Any agent can run the automated audit and sync suite from `/home/dwea/Projects/L2`:
```bash
# Run all 5 audit subroutines (Doc Drift, Dist vs Build/Dist Sync, Client Mirror Sync + Binary Signatures, Git vs Ledger Coverage, Agent Context Health):
python3 /home/dwea/Projects/L2/l2jmobius_dwea_fork/tools/auditor/l2_auditor.py --check

# Automatically synchronize canonical markdown dossiers across all 4 workspace roots + brain artifacts, and sync dist/ <-> build/dist/:
python3 /home/dwea/Projects/L2/l2jmobius_dwea_fork/tools/auditor/l2_auditor.py --sync
```

---

## 10. Overnight Multi-Agent Execution Wave: `GMWnd` v3 CT1 Tabs + Delta Reload (`abcc9bec4b6`), Tiers 1–3 Bug Fixes (`decaf386a5e`), Tactical AI + LLM FakePlayers (`3917083adbc`) & Java 27 / UE2.5 Optimizations

1. **Phase 1 — `GMWnd` v3 Top-Tab Console & Transactional Delta Script Hot-Reload (`abcc9bec4b6`):**
   - **Agent 2 (`patch_gmwnd_client.py`, `interface.xdat` `1,306,456` B, `interface.u` `4,061,011` B 0-byte delta):** Removed the 228px left button dock, styled 7 contiguous native C++ top category tabs (`y=31.0, h=23, x=7..695`) with genuine CT1 `L2UI_CT1.tab.Tab_DF_Tab_Unselected`/`Selected` textures + `BoardWnd` `Tab_DF_bg_line` & `Tab_DF_bg` frame, kept 3 top-right `.dat` search tree buttons (`Item .dat`, `NPC .dat`, `Skill .dat`), and expanded `GMWnd.HtmlViewer` to **`968×472` px**.
   - **Agent 1 (`ScriptEngine.java`, `AdminReload.java`, `AdminBoardManager.java`, `AdminInstance.java`):** Implemented `_scriptLastModified` tracking across all 1,185 `.java` scripts and transactional `reloadModifiedScripts()` (`//reload changed` / `//reload delta`); widened `AdminBoardManager` tables to `948px`, added per-GM `Deque<NavEntry>` breadcrumb navigation with `[ ^ Back 1 Level ]` (`//gmw back`), added the `Instances` tab (`//gmw instances`), and added `admin_ghoston`/`admin_ghostoff`.
2. **Phase 2 — All 3 Tiers of Pre-Public Forensic Bug Fixes (`decaf386a5e`):**
   - **Tier 1 (`CrystalCaverns.java`):** Fixed Emerald Square `world.cleanedRooms` soft-lock, replaced all 18 global `cancelQuestTimers(...)` calls with instance-scoped `cancelQuestTimer(...)` / `cancelSteamTimers(CCWorld, ...)`, implemented `"baylor_despawn"` and `"BaylorMinions"` case-insensitive matching, spawned Darnel's chamber traps (`18379..18389`), and fixed disconnect re-entry stranding via `activeBranch` + `world.getStatus()`.
   - **Tier 2 (`HeartInfinityAttack.java`, `TowerOfNaia.java`, `TullyWorkshop.java`, `25700-25799.xml`, `Kamaloka.java`, `PailakaInjuredDragon.java`):** Moved `tumorRespawnTime`/`conquestBegun`/`conquestEnded`/`houndBlocked` into `HIAWorld`, fixed `_deadTumor` in `TumorRevival`/`RegenerationCoffinSpawn`, and implemented `handleEkimusStats(HIAWorld)` with `FuncSet`; removed duplicate `DWARVEN_GHOST` (`32370`) registration from `TowerOfNaia.java`; scheduled `TullyWorkshop.java` `3%` heal after `healSkill.getHitTime()`; created `25700-25799.xml` (`25706..25712`) for Level 73/83 `Kamaloka.java` (`index 18`); and added distance + `GeoEngine.canSeeTarget` LOS check to `PailakaInjuredDragon.java`.
   - **Tier 3 (`CreatureTemplate.java`, `CreatureStat.java`, `Creature.java`, `NpcData.java`, `TerritoryWarSuperClass.java`, `CastleChamberlain.java`, `FortressOfResistance.java`):** Restored parsed XML `_baseAccuracy`, `_baseEvasion`, and `_baseReuseDelay` across all 9,883 NPCs; implemented `TerritoryWarSuperClass.getTerritoryIdForThisNPCId(int)`; wired `castle.getTaxPercent()` in `CastleChamberlain.java`; and fixed per-talk `NpcHtmlMessage(npc.getObjectId())` + `_damageToNurka.clear()` in `FortressOfResistance.java`.
3. **Phase 3 — Tactical Utility-AI (`250ms`) + Java 27 Virtual-Thread LLM FakePlayer Engine (`3917083adbc`):**
   - **Agent 1 (`FakePlayerAiEngine.java`, `FakePlayerChatManager.java`, `FakePlayersConfig.java`, `FakePlayers.ini`, `fpc_combat.xml`, `AdminFakePlayers.java`):** Implemented `250ms` Utility-AI loop classifying FakePlayers into `ARCHER`, `MAGE`, `DAGGER`, `HEALER`, `TANK`, `WARRIOR` with 6 real-time micro-tactics (Squad Follow & Assist, Healer Triage `1218`, Tank Hate-Intercept `28`, Archer/Mage `GeoEngine` Standoff Kiting, Dagger Backstab Flanking `263`, and `< 35%` HP Second Wind Potion `2037`), non-blocking Java 27 Virtual-Thread `HttpClient` LLM bridge + natural language whisper directives, 6 Level 85 squad archetypes (`81001..81006`), and the `948px` `//fakeplayer` (`//fpc`) Control Center.
4. **Client UE2.5/DXVK Tuning & Local Docker Java 27 (`Temurin-27+35`) Lilliput + ZGC Optimization:**
   - **Agent 2 (`l2.ini`, `Option.ini`, `launch-l2.sh`):** Set `CacheSizeMegs=512`, `L2MaxPawnResourceLoad=5.0`, `GPUAnimation=True`, `ReduceMouseLag=False`, `UseTripleBuffering=True`, `LevelOfAnisotropy=16`, and `DXVK_STATE_CACHE=1`.
   - **Agent 3 (`docker/run-server.sh`, `docker-compose.yml`):** Enabled `-XX:+UseCompactObjectHeaders` (JEP 519), `-XX:+AlwaysPreTouch`, `-XX:+UseStringDeduplication`, `-XX:+ExplicitGCInvokesConcurrent`, `-XX:SoftMaxHeapSize=3g`, `-XX:ZUncommitDelay=300`, rotating `log/gc.log`, passive `/proc/net/tcp{,6}` `0A` (`TCP_LISTEN`) healthchecks, and MariaDB 11.8 tuning, reducing `l2-game` startup heap by **`-152 MB` (`-7.5%`)** (`1,866 MB`, `9s` boot).

---

## 11. Wave 2 & 3 Autonomous Citizen Lifecycle (`Lv 1 -> 85`), Multi-Stage Profession/Subclass/Noblesse Quests, 4-Archetype Combat Doctrines + UCB1 Toolkit Exploration, `EconomyProgressionAuditor`, Unified `NG–B` Merchant & Mana Potion/Drug Fix (`7d59d9ba0e9`, `b23447f1917`, `babe943ad19`)

1. **Mana Potion (`728`) & Mana Drug (`726`) Restoration Fix (`b23447f1917`):**
   - Updated `ManaHeal.java`, `ManaHealByLevel.java`, and `Heal.java` (`effected.isInvul() && !effected.isGM()`) so `GMStartupInvulnerable = True` never blocks potion/heal effects on GM accounts; configured Mana Potion (`728`, skill `10001`, `A1`) for instant `+400 MP` (`2000ms` reuse) and Mana Drug (`726`, skill `10000`, `A2`) for a 20-second `2.0x + 15` MP Regeneration buff + `ManaHealOverTime` (`15 MP/s`).
2. **Community Board Merchant Unification (`b23447f1917`):**
   - Unified all weapons into **4 Grade multisells** (`50039941..50039944.xml`, deleting `50039001..50039035.xml` and stripping monster/NPC weapons), stripped **Ring of Queen Ant (`6660`)** and unobtainable C1 jewelry from `50039963.xml`, combined all potions/shots/arrows/scrolls into a single **`Consumables`** button (`50039990.xml`), and fixed the C-Grade Armor icon (`icon.armor_t61_ul_i00`) in `merchant/armors.html`.
3. **Zero-Shortcut Autonomous Citizen Lifecycle (`7d59d9ba0e9`, `b23447f1917`):**
   - Implemented strict Dwarven Spoiler (`Fortune Seeker`) & Crafter (`Maestro`) role separation and inter-citizen trade (`AutonomousPlayerManager.commissionDwarvenCraft`), multi-stage Profession Quest chains (`3` Path stages at `Lv 20`, `3` separate Trial/Testimony/Test Occupation Marks at `Lv 40`, `6` Resonance Amulets / Tablets of Vision at `Lv 76`), full Subclass (`Q00234 -> Q00235` + real `Lv 40 -> 75` leveling) and 4-Part Noblesse (`Q00241 -> Q00242 -> Q00246 -> Q00247` + *Barakiel `25325`* Raid) campaigns, 4-Archetype Tactical Combat Doctrines + **UCB1 Contextual Toolkit Exploration Memory (`_contextualSkillQ`)**, and `EconomyProgressionAuditor.java` (`log/citizen_economy_audit.jsonl`).

