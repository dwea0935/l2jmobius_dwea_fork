# Project Master Context, Architectural Blueprint & Handover Dossier
**Project:** L2J Mobius CT 2.4 (Gracia Epilogue) Private Server Overhaul  
**Repository Fork:** `dwea0935/l2jmobius_dwea_fork.git`  
**Generated At:** 2026-10-15  
**Author / Pair:** Denys Hordiienko & Antigravity (Google DeepMind)

---

## 1. Executive Summary & Vision

### 1.1 The Grand Server Genesis
The objective of this project is to build an elite, high-retention Lineage 2 server based on **Gracia Epilogue (Chronicle 2.4)**.

Private servers running Gracia Epilogue are historically rare and plagued with defects. Most private server operators either run Interlude (C6) with truncated content or run heavily customized High Five / GoD packs with stripped-down features. Our vision bridges the gap:
* **The Progression Curve:** A dynamic progression model designed to bypass the tedious early-game slog (level 1–75 accelerated), transitioning into a deliberate, engaging curve at level 80+. Solo players reach endgame viability in approximately one week of active play; organized group play accelerates progression through synergy.
* **Stage-Gated Expansion Unlocking:** The server content unlocks progressively in organic phases reminiscent of retail chronicle rollouts:
  1. **Phase 1 (Baseline Content):** Core open world, standard olympiad, baseline craft/economy, and entry to Isle of Prayer.
  2. **Phase 2 (Crystal Caverns & Baylor):** Dark Cloud Mansion, Emerald Square, Steam Corridor, Chromatic Highlands, culminating in the Crystal Caverns Baylor raid.
  3. **Phase 3 (Hellbound & Naia):** Unlocking Hellbound Island across trust levels, Tully's Workshop (8-floor mechanical ascent), Darion, and the Tower of Naia.
  4. **Phase 4 (The Beleth Encounter):** The Spire of Naia, the Beleth illusion battle (1 true Beleth among 32 clones), and distribution of the legendary Ring of Beleth.
  5. **Phase 5 (Gracia Continent):** Keucereus Alliance Base, Aerial Combat, Seed of Infinity, Seed of Destruction, and Tiat.

### 1.2 The Technical Reality of Stock Emulators
Thorough reverse engineering and code audits of the L2J Mobius Epilogue engine revealed that while core low-level mechanics (combat calculations, geodata line-of-sight, skill engine, inventory transactions) are functionally sound, **instanced endgame PvE was severely underdeveloped and unstable**:
* Players who crashed or disconnected inside dungeons were permanently expelled or soft-locked into empty coordinate voids in "Instance 0".
* Duo and small parties were instantly disbanded upon a single client crash.
* Global static timers (`QuestTimer`) crossed wires between concurrent party instances, causing doors to prematurely lock or bosses to despawn.
* NPC interactions across shared zones (e.g. Tully's Workshop and Tower of Naia) were implemented using fragile coordinate hacks due to legacy misunderstandings of script event dispatching.

Rather than patching individual quest scripts on quicksand, we established the **Core Instance Infrastructure Pipeline** directly into the engine, guaranteeing that every dungeon built on top is rock-solid.

---

## 2. Comprehensive Forensics & Deep-Dive Catalog

### 2.1 The Isle of Prayer & Crystal Caverns Deep Dive
* **Dark Cloud Mansion:** The prerequisite 2-player instance used to obtain the Contaminated Crystal. Features color-coded elemental monoliths and timed room sweeps.
* **The Three Branch Dungeons:**
  1. **Emerald Square:** Labyrinthine corridors filled with crystalline traps and secret room passages leading to Raid Boss Darnel.
  2. **Steam Corridor:** Timed endurance trial requiring parties to manage Flame Golems. Golems must be fed specific items/drops to control room heat and unlock steam doors, culminating in Raid Boss Kechi.
  3. **Chromatic Highlands:** Elemental-infused cavern requiring Water Dragon Scales and Crystals to survive, leading to Raid Boss Tears (utilizing water mirrors and invulnerability shields).
* **Baylor's Lair:**
  * Requires all three crystals (Emerald, Blue, Clear) obtained from Darnel, Kechi, and Tears.
  * Spawns active alarm devices (`Crystal Prison Alarms`) that must be suppressed before they alert reinforcements.
  * Features the animated golem raid encounter and Baylor's invulnerability shield mechanics (broken only via Water Dragon Claws).

### 2.2 Tully's Workshop & Tower of Naia Forensics
* **Tully's Workshop:** An 8-floor mechanical tower located within Hellbound. Players must navigate portal devices, defeat agent golems, and defeat Darion to gain access to Naia.
* **The Old Dwarven Ghost (NPC 32370) Hack:** In stock Mobius, `TullyWorkshop.java` contained hardcoded coordinates belonging to the Tower of Naia. The Naia developer couldn't register `32370` in `TowerOfNaia.java` because a 20-year-old Javadoc in `Quest.java` claimed an NPC could only have one `onFirstTalk` handler. We proved this was a legacy myth: Mobius's modern `EventDispatcher` already supports chained listener callbacks.
* **Tower of Naia & Beleth:**
  * 12 elemental rooms requiring sequential core transmissions.
  * Epidos raid encounter.
  * The Beleth battle: spawns 32 clone illusions with identical appearance and HP bars. Players must identify the true Beleth by observing subtle spellcasting signatures, combat animations, and debuff reactions.

---

## 3. The 5 Pillars of the Core Instance Infrastructure Pipeline

```
                      CORE INSTANCE PIPELINE ARCHITECTURE
                      
     ┌───────────────────────────────────────────────────────────────┐
     │                     InstanceManager (Core)                    │
     │  - Manages active Instance worlds                             │
     │  - Tracks offline player coordinates & instance associations  │
     │  - Enforces Instance Zone boundaries & evacuation fallbacks   │
     └───────────────────────────────┬───────────────────────────────┘
                                     │
         ┌───────────────────────────┼───────────────────────────┐
         ▼                           ▼                           ▼
┌───────────────────┐       ┌───────────────────┐       ┌───────────────────┐
│   InstanceWorld   │       │      L2Party      │       │  EnterWorld Hook  │
│  - Scoped Timers  │       │  - Slot Reserve   │       │  - Auto-rejoin    │
│  - Task Lifecycle │       │  - Grace Period   │       │  - Instance 0     │
│  - Parameter Set  │       │  - Offline Flags  │       │    Evacuation     │
└────────┬──────────┘       └───────────────────┘       └───────────────────┘
         │
         ▼
┌───────────────────┐
│  InstanceScript   │
│  - ON_FINISH/Kill │
│  - getResumeLoc() │
│  - Event Chaining │
└───────────────────┘
```

### Pillar 1: Instance State Persistence & Offline Evacuation
* **The Flaw:** When an instance is destroyed, `Instance.removePlayers()` iterates over active players using `World.getPlayer(objectId)`. If a player crashed or was offline, `World.getPlayer()` returned `null`. Their saved coordinates in MySQL remained inside the dungeon geometry. When they logged back in later, the instance was gone (`instanceId == 0`), dropping them into the empty "Instance 0" void behind locked doors.
* **The Solution:**
  1. **SQL Evacuation Batch in [`Instance.java`](file:///c:/personal/personal/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/entity/instancezone/Instance.java):** When an instance expires or is destroyed, `removePlayers()` identifies all offline members and dispatches an asynchronous batch SQL query:
     ```sql
     UPDATE characters SET x=?, y=?, z=? WHERE charId=?
     ```
     moving them immediately to `_exitLocation`.
  2. **The Instance 0 Sentinel in [`EnterWorld.java`](file:///c:/personal/personal/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/network/clientpackets/EnterWorld.java):** If any player enters the world in `instanceId == 0` while positioned inside a `ZoneId.NO_RESTART` zone boundary, the server detects the coordinate breach and immediately teleports them safely to Town.

### Pillar 2: Party Reconnection & Slot Reservation State Machine
* **The Flaw:** In stock L2J, `Player.deleteMe()` immediately calls `leaveParty()`. A client disconnect during a boss fight instantly dissolved duo parties, kicked players from party chat/buffs, and prevented re-invitation during combat.
* **The Solution:**
  1. **Grace Period Slot Reservation in [`Party.java`](file:///c:/personal/personal/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/entity/groups/Party.java):** Disconnecting players inside an instance are transitioned to `DisconnectedMember` with a 5-minute timeout task (`GeneralConfig.INSTANCE_DISCONNECT_GRACE_PERIOD`).
  2. **Leader Auto-Handover:** If the party leader disconnects, leadership is automatically transferred to the next active online member so the raid can continue managing party functions and Command Channel authority without pausing.
  3. **Leader `/oust` Authority (Resolving Rage-Quits):** The party is never held hostage by an offline player. If a member rage-quits or goes AFK to eat, the leader can `/oust <Name>` at any second. `Party.removePartyMember(String, PartyMessageType)` resolves offline reservations, cancels the timer, evicts the character from the instance allowed list, and frees the 9th party slot immediately.
  4. **Seamless Reconnect in [`EnterWorld.java`](file:///c:/personal/personal/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/network/clientpackets/EnterWorld.java):** When the player logs back in, `Party.checkPartyReconnect(player)` re-inserts them into `_members`, cancels the timeout, and dispatches `PartySmallWindowAll` and `PartySmallWindowAdd` packets.
  5. **Client Buffer Safety in [`PartySmallWindowAll.java`](file:///c:/personal/personal/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/network/serverpackets/PartySmallWindowAll.java):** Writes `_party.getMembers().size() - 1` into the packet header so the serialized count matches online records exactly, preventing client buffer underflow crashes.

### Pillar 3: Native Instance-Scoped Timers
* **The Flaw:** Legacy scripts relied on global `startQuestTimer(name, delay, npc, player)`. Timers from different instances shared the same name string, creating race conditions, crosstalk, and orphaned thread pool leaks when instances were destroyed prematurely.
* **The Solution in [`InstanceWorld.java`](file:///c:/personal/personal/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/entity/instancezone/InstanceWorld.java):**
  * Added `_instanceTimers = new ConcurrentHashMap<String, ScheduledFuture<?>>()`.
  * Dedicated API: `scheduleTimer()`, `scheduleAtFixedRate()`, `cancelTimer()`, `hasTimer()`.
  * `InstanceWorld.destroy()` automatically executes `cancelAllTimers()`, ensuring zero orphaned background threads.

### Pillar 4: Milestone Re-entry & Dynamic Checkpoints
* **The Flaw:** Returning players were always teleported to the static `enterLocs` at the dungeon mouth. If the party had progressed past one-way doors or fire grates, the reconnecting player was trapped behind closed gates.
* **The Solution in [`InstanceScript.java`](file:///c:/personal/personal/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/mechanics/script/InstanceScript.java):**
  * Added `getResumeLocation(Player player, InstanceWorld world)` hook. Dungeon scripts override this to return active room coordinates based on dungeon progress.
  * Added `setReenterPenalty(InstanceWorld world)` allowing scripts to apply lockout timers immediately upon final boss death rather than waiting for instance shutdown.

### Pillar 5: Chained Event Dispatching
* **The Architecture:** In [`EventDispatcher.java`](file:///c:/personal/personal/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/mechanics/events/EventDispatcher.java), events are dispatched across a list of listeners. When a script's `onFirstTalk` returns `null`, [`Quest.showResult()`](file:///c:/personal/personal/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/mechanics/script/Quest.java) ignores it, allowing downstream listeners to process the interaction. This allows multiple scripts to listen to shared NPCs cleanly without coordinate checks.

---

## 4. Complete Code Modification Audit

The following 9 files have been modified and verified:

1. **[`General.ini`](file:///c:/personal/personal/L2J_Mobius_CT_2.4_Epilogue/dist/game/config/General.ini):**
   * Added `InstanceDisconnectGracePeriod = 300` (seconds).
2. **[`GeneralConfig.java`](file:///c:/personal/personal/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/config/GeneralConfig.java):**
   * Loaded `INSTANCE_DISCONNECT_GRACE_PERIOD = config.getInt("InstanceDisconnectGracePeriod", 300) * 1000;`.
3. **[`InstanceWorld.java`](file:///c:/personal/personal/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/entity/instancezone/InstanceWorld.java):**
   * Implemented isolated timer scheduling API: `scheduleTimer`, `scheduleAtFixedRate`, `cancelTimer`, `cancelAllTimers`, `hasTimer`.
   * Bound `cancelAllTimers()` to `destroy()`.
4. **[`Instance.java`](file:///c:/personal/personal/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/entity/instancezone/Instance.java):**
   * Re-engineered `removePlayers()` with asynchronous SQL batch to update coordinates of offline players to `_exitLocation`.
5. **[`PartySmallWindowAll.java`](file:///c:/personal/personal/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/network/serverpackets/PartySmallWindowAll.java):**
   * Fixed header count from `_party.getMemberCount() - 1` to `_party.getMembers().size() - 1` to prevent client crashes.
6. **[`Party.java`](file:///c:/personal/personal/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/entity/groups/Party.java):**
   * Added `DisconnectedMember` holder and `DISCONNECTED_PLAYERS` thread-safe registry.
   * Implemented `onPlayerDisconnect`, `onDisconnectTimeout`, `checkPartyReconnect`, and `reconnectPartyMember`.
   * Updated `removePartyMember(String, PartyMessageType)` to support kicking offline members.
   * Overrode `getMemberCount()` to return `_members.size() + _disconnectedMembers.size()`.
   * Updated `getCheckedNextLooter()` to safely bound against `_members.size()`.
7. **[`Player.java`](file:///c:/personal/personal/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/entity/actor/Player.java):**
   * In `deleteMe()`: routed instanced players to `_party.onPlayerDisconnect(this)` when `RESTORE_PLAYER_INSTANCE` is active.
8. **[`EnterWorld.java`](file:///c:/personal/personal/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/network/clientpackets/EnterWorld.java):**
   * Restores instanced players and invokes `Party.checkPartyReconnect(player)`.
   * Validates instance permissions and teleports ejected players to Town.
   * Enforced Instance 0 `ZoneId.NO_RESTART` safety net.
9. **[`InstanceScript.java`](file:///c:/personal/personal/L2J_Mobius_CT_2.4_Epilogue/java/org/l2jmobius/gameserver/mechanics/script/InstanceScript.java):**
   * Added `getResumeLocation(Player, InstanceWorld)` hook in `enterInstance()`.
   * Added `setReenterPenalty(InstanceWorld)` for early lockout triggers.

---

## 5. Next Steps: Crystal Caverns Restructuring Plan

With the infrastructure pipeline completed, the next phase is restructuring the 4 dungeon scripts under `dist/game/data/scripts/instances/CrystalCaverns`:

```
Crystal Caverns Restructuring Roadmap
├── 1. Emerald Square (Darnel)
│   ├── Clean instance parameterization
│   ├── Dynamic trap trigger zones
│   ├── Secret room key & door progression
│   └── Darnel AI state machine
├── 2. Steam Corridor (Kechi)
│   ├── Golem feeding mechanic rewrite (timed heat management)
│   ├── Sequential steam door unlocking
│   └── Kechi guardian AI & defeat event
├── 3. Chromatic Highlands (Tears)
│   ├── Water Dragon Scale usage mechanics
│   ├── Tears water mirror illusion handling
│   └── Water Dragon Claw invulnerability purge
└── 4. Baylor's Lair
    ├── Prerequisite 3-crystal validation
    ├── Crystal Prison Alarm NPC placement & alert AI
    ├── Animated Golem encounter
    └── Baylor boss battle & Crystal Caverns completion lockout
```

---

## 6. How to Resume This Session on Another Computer

When you pull this branch on your home machine, you can resume work with full context by providing this file to Antigravity:

### Step 1: Git Fetch & Switch
```bash
git fetch origin
git checkout feature/instance-infrastructure-pipeline
```

### Step 2: Prompting Antigravity / Agent
Provide the following prompt to the assistant on your other machine:
> *"I have pulled the latest branch `feature/instance-infrastructure-pipeline` which includes `PROJECT_MASTER_CONTEXT_AND_ROADMAP.md`. Please read this file in full. The Core Instance Infrastructure Pipeline is already implemented and verified in the Java engine layer. We are now ready to begin Phase 2: Restructuring the Crystal Caverns dungeon scripts (starting with Emerald Square and Steam Corridor golem feeding). Please review the roadmap and propose the implementation plan for Emerald Square."*
