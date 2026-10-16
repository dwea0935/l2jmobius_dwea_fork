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
   - [`launch-l2.sh`](file:///home/dwea/Projects/L2/Client%20Lineage%202%20Gracia%20Epilogue/launch-l2.sh): Full Linux Wine/Proton + 32-bit DXVK (`d3d9.dll` + `dxgi.dll`) launcher with `-server`, `-login`, `-pass`, `-slot`, `--borderless`, `--windowed`, `--fullscreen`, `--res`, `--gamepad`, and `--check`.
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
- [ ] Optional: Provide `.l2j` geodata files in `game/data/geodata/` when testing off-heap FFM pathfinding locally.
- [ ] Ready for interactive client login (`./launch-l2.sh --windowed --res 1920x1080 -server 127.0.0.1`).
