# Multi-Agent Reconciliation & File Ownership Notice

**Timestamp:** 2026-10-16T05:57:30+02:00  
**Active Workspace:** `/home/dwea/Projects/L2`  
**Status:** **100% RECONCILED, VERIFIED & PUSHED TO `origin/master` (`9ddf57e6c12`)**

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
