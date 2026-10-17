# Multi-Agent Engineering Topology, File Ownership & Auditor Governance Rules

**Active Workspace:** `/home/dwea/Projects/L2`  
**Project:** Lineage 2 Gracia Epilogue (`CT2.4`, Protocol `148` & `152`) Server + Client Modernization

---

## 1. Active 4-Agent Roster (`send_message` Conversation IDs)

When communicating across agents with the `send_message` tool, **always use these exact active Conversation IDs**:

| Agent | Role | `send_message` Recipient ID | Primary Scope & Responsibilities |
| :---: | :--- | :--- | :--- |
| **Agent 1** | **Server Core & Datapack Agent** | `6117006f-8ccb-4196-bebe-1ba7b09aea0f` | `/home/dwea/Projects/L2/l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue/` Java 27 core, dual-protocol (`148`/`152`) packets, Panama FFM `GeoEngine`, zero-downtime hot-reload engine, RBAC `AdminBoardManager`, datapack XML/scripts, and `build/dist/libs/GameServer.jar` compilation. *(Do NOT message `4a4db620-c2a4-4949-be36-98f66c3f3191` — that is an archived Oct 14 Ansible session.)* |
| **Agent 2** | **Client & Reverse-Engineering Agent** | `9379ca1e-d28c-4127-8680-c8e529c91ed7` | `/home/dwea/Projects/L2/Client Lineage 2 Gracia Epilogue/` & `/home/dwea/Projects/L2/l2jmobius_dwea_fork/client/`: x86 PE32 binary patches (`l2.exe`, `Engine.dll`, `NWindow.dll`, `d3ddrv.dll`, `windrv.dll`, `dsetup.dll`), `interface.u`/`interface.xdat` (`patch_gmwnd_client.py`), `patch_nwindow_unicode.py`, `launch-l2.sh`, `user.ini`, and `tools/convert_buildzone_to_l2j.py`. |
| **Agent 3** | **DevOps, Docker & Infrastructure Agent** | `f996ed99-9472-40bb-9157-05630ba9775b` | Local Docker test stack (`docker-compose.yml`, `docker/run-server.sh`, `docker/db-init/01-l2jmobius-schema.sh`, `l2-db`, `l2-login`, `l2-game`), container lifecycle (`echo "docker ..." \| newgrp docker`), and runtime log monitoring. *(Note: `ansible/` and `terraform/` are strictly off-limits / read-only unless the user explicitly instructs an agent to modify them.)* |
| **Agent 4** | **Project Manager, Delivery Lead, Auditor & Context Governance Agent** | `38e33746-e411-44db-8ed9-525f8cf93b55` | Hybrid Project Manager + Delivery Lead + Auditor: architectural breakdown & scoped task delegation (`send_message`) to Agents 1–3, cross-cutting code/datapack/client forensic bug audits, delivery verification via `tools/auditor/l2_auditor.py`, and maintaining all master documentation, lessons-learned dossiers, and inter-agent context across all 4 roots. |

---

## 2. Mandatory Synchronization & Handoff Protocol

1. **Notify the Auditor (`38e33746-e411-44db-8ed9-525f8cf93b55`) on Major Milestones or Forensics:**
   - Whenever Agent 1, Agent 2, or Agent 3 completes a code/binary change, uncovers a forensic root cause, or commits to Git, send a brief summary via `send_message` to **Agent 4 (`38e33746-e411-44db-8ed9-525f8cf93b55`)** so the Auditor can run `l2_auditor.py --sync` and update `FULL_PROJECT_ENGINEERING_LEDGER.md`, `LESSONS_LEARNED_AND_FORENSICS.md`, `AGENT_SYNC_AND_RECONCILIATION.md`, and `MASTER_HANDOFF_AND_CLIENT_SERVER_GUIDE.md`.
2. **Server Source vs. Runtime `build/dist/` Invariant (Agent 1 & Agent 3):**
   - Any edit to `L2J_Mobius_CT_2.4_Epilogue/dist/game/` MUST be mirrored to `build/dist/game/` (and vice versa for intentional runtime config changes like `General.ini`).
   - Any edit to `L2J_Mobius_CT_2.4_Epilogue/java/` MUST be compiled (`--release 27`) into `build/bin` and repacked into `build/dist/libs/GameServer.jar` (preserving `META-INF/MANIFEST.MF`).
3. **Client Standalone vs. Server-Tracked Mirror Invariant (Agent 2):**
   - Any binary patch or script change in `Client Lineage 2 Gracia Epilogue/` (`system/*.dll`, `system/l2.exe`, `system/interface.u`, `system/interface.xdat`, `launch-l2.sh`, `patch_*.py`, `tools/*`) MUST be mirrored to `l2jmobius_dwea_fork/client/`.
4. **Strict Read-Only Protection on `ansible/` & `terraform/`:**
   - `/home/dwea/Projects/L2/ansible/` and `/home/dwea/Projects/L2/terraform/` belong to a separate live homelab deployment track (`peak`, `10.9.0.0/24`) and **MUST NOT be modified** by any agent unless explicitly instructed by the user. All local testing and verification run exclusively via `docker-compose.yml` + `docker/`.
5. **Automated Audit & Sync Command:**
   - Run `python3 /home/dwea/Projects/L2/l2jmobius_dwea_fork/tools/auditor/l2_auditor.py --check` (or `--sync`) to verify zero drift across all documentation roots, `dist/` vs `build/dist/`, client mirrors, binary patch signatures, and Git commit coverage.
