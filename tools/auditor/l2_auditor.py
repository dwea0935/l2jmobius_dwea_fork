#!/usr/bin/env python3
"""
Lineage 2 Gracia Epilogue (CT2.4) — Automated Project Auditor & Context Sync Suite
Owner: Agent 4 (Project Auditor, Documentation & Context Governance Agent: 38e33746-e411-44db-8ed9-525f8cf93b55)

Subroutines:
  1. subroutine_doc_drift        : Audits & syncs shared markdown dossiers across all 4 roots + brain artifacts.
  2. subroutine_dist_sync        : Audits L2J_Mobius_CT_2.4_Epilogue/dist vs build/dist, JAR manifests, and geodata.
  3. subroutine_client_sync      : Audits Client Lineage 2 Gracia Epilogue vs l2jmobius_dwea_fork/client + binary patches.
  4. subroutine_git_ledger_audit : Audits git commit coverage in FULL_PROJECT_ENGINEERING_LEDGER.md & working-tree state.
  5. subroutine_agent_roster     : Reports live status & artifact health across the 4-agent engineering team.
"""

import argparse
import hashlib
import json
import os
import shutil
import subprocess
import sys
import zipfile
from datetime import datetime, timezone

WORKSPACE_ROOT = "/home/dwea/Projects/L2"
SERVER_REPO = os.path.join(WORKSPACE_ROOT, "l2jmobius_dwea_fork")
SERVER_CLIENT_DIR = os.path.join(SERVER_REPO, "client")
STANDALONE_CLIENT_DIR = os.path.join(WORKSPACE_ROOT, "Client Lineage 2 Gracia Epilogue")
SRC_DIST_DIR = os.path.join(SERVER_REPO, "L2J_Mobius_CT_2.4_Epilogue", "dist")
BLD_DIST_DIR = os.path.join(SERVER_REPO, "build", "dist")
JAVA_SRC_DIR = os.path.join(SERVER_REPO, "L2J_Mobius_CT_2.4_Epilogue", "java")
BRAIN_ROOT = "/home/dwea/.gemini/antigravity/brain"

AGENT_ROSTER = [
    {
        "id": "6117006f-8ccb-4196-bebe-1ba7b09aea0f",
        "num": "Agent 1",
        "role": "Server Core & Datapack Agent",
        "scope": "l2jmobius_dwea_fork/L2J_Mobius_CT_2.4_Epilogue (Java 27, Packets, GeoEngine, Hot-Reload, RBAC, Datapack)",
    },
    {
        "id": "9379ca1e-d28c-4127-8680-c8e529c91ed7",
        "num": "Agent 2",
        "role": "Client & Reverse-Engineering Agent",
        "scope": "Client Lineage 2 Gracia Epilogue & l2jmobius_dwea_fork/client (PE32 DLLs, interface.u/.xdat, launch-l2.sh)",
    },
    {
        "id": "f996ed99-9472-40bb-9157-05630ba9775b",
        "num": "Agent 3",
        "role": "DevOps, Docker & Infrastructure Agent",
        "scope": "docker-compose.yml, docker/, terraform/ (Proxmox LXC), ansible/, runtime container ops",
    },
    {
        "id": "38e33746-e411-44db-8ed9-525f8cf93b55",
        "num": "Agent 4",
        "role": "Project Auditor, Documentation & Context Governance Agent",
        "scope": "Cross-repo forensic audits, master documentation/artifact sync, tools/auditor/l2_auditor.py",
    },
]

# Shared markdown files and the locations that must stay 100% identical
DOC_SYNC_MAP = {
    "AGENT_SYNC_AND_RECONCILIATION.md": [
        os.path.join(SERVER_REPO, "AGENT_SYNC_AND_RECONCILIATION.md"),
        os.path.join(WORKSPACE_ROOT, "AGENT_SYNC_AND_RECONCILIATION.md"),
        os.path.join(SERVER_CLIENT_DIR, "AGENT_SYNC_AND_RECONCILIATION.md"),
        os.path.join(STANDALONE_CLIENT_DIR, "AGENT_SYNC_AND_RECONCILIATION.md"),
    ],
    "LESSONS_LEARNED_AND_FORENSICS.md": [
        os.path.join(WORKSPACE_ROOT, "LESSONS_LEARNED_AND_FORENSICS.md"),
        os.path.join(SERVER_REPO, "LESSONS_LEARNED_AND_FORENSICS.md"),
        os.path.join(SERVER_CLIENT_DIR, "LESSONS_LEARNED_AND_FORENSICS.md"),
        os.path.join(STANDALONE_CLIENT_DIR, "LESSONS_LEARNED_AND_FORENSICS.md"),
        os.path.join(BRAIN_ROOT, "f996ed99-9472-40bb-9157-05630ba9775b", "lessons_learned_and_forensics.md"),
        os.path.join(BRAIN_ROOT, "38e33746-e411-44db-8ed9-525f8cf93b55", "lessons_learned_and_forensics.md"),
    ],
    "FULL_PROJECT_ENGINEERING_LEDGER.md": [
        os.path.join(SERVER_REPO, "FULL_PROJECT_ENGINEERING_LEDGER.md"),
        os.path.join(WORKSPACE_ROOT, "FULL_PROJECT_ENGINEERING_LEDGER.md"),
        os.path.join(SERVER_CLIENT_DIR, "FULL_PROJECT_ENGINEERING_LEDGER.md"),
        os.path.join(STANDALONE_CLIENT_DIR, "FULL_PROJECT_ENGINEERING_LEDGER.md"),
        os.path.join(BRAIN_ROOT, "6117006f-8ccb-4196-bebe-1ba7b09aea0f", "full_project_context_and_engineering_ledger.md"),
        os.path.join(BRAIN_ROOT, "38e33746-e411-44db-8ed9-525f8cf93b55", "full_project_context_and_engineering_ledger.md"),
    ],
    "MASTER_HANDOFF_AND_CLIENT_SERVER_GUIDE.md": [
        os.path.join(SERVER_REPO, "MASTER_HANDOFF_AND_CLIENT_SERVER_GUIDE.md"),
        os.path.join(WORKSPACE_ROOT, "MASTER_HANDOFF_AND_CLIENT_SERVER_GUIDE.md"),
        os.path.join(SERVER_CLIENT_DIR, "MASTER_HANDOFF_AND_CLIENT_SERVER_GUIDE.md"),
        os.path.join(STANDALONE_CLIENT_DIR, "MASTER_HANDOFF_AND_CLIENT_SERVER_GUIDE.md"),
    ],
    "CLIENT_MASTER_ARCHITECTURE_AND_GUIDE.md": [
        os.path.join(SERVER_REPO, "CLIENT_MASTER_ARCHITECTURE_AND_GUIDE.md"),
        os.path.join(SERVER_CLIENT_DIR, "CLIENT_MASTER_ARCHITECTURE_AND_GUIDE.md"),
        os.path.join(STANDALONE_CLIENT_DIR, "CLIENT_MASTER_ARCHITECTURE_AND_GUIDE.md"),
    ],
    "AGENTS.md": [
        os.path.join(WORKSPACE_ROOT, "AGENTS.md"),
        os.path.join(SERVER_REPO, "AGENTS.md"),
        os.path.join(SERVER_CLIENT_DIR, "AGENTS.md"),
        os.path.join(STANDALONE_CLIENT_DIR, "AGENTS.md"),
    ],
}


def sha256_file(path: str) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def subroutine_doc_drift(do_sync: bool = False) -> dict:
    report = {"name": "subroutine_doc_drift", "status": "OK", "documents": {}, "synced_files": []}
    for doc_name, paths in DOC_SYNC_MAP.items():
        existing = []
        for p in paths:
            if os.path.exists(p):
                existing.append((p, sha256_file(p)[:12], os.path.getsize(p), os.path.getmtime(p)))
        if not existing:
            report["documents"][doc_name] = {"status": "MISSING"}
            report["status"] = "WARN"
            continue

        # Canonical source is the largest / newest file among existing copies
        canonical_path, canonical_hash, canonical_size, _ = max(existing, key=lambda x: (x[2], x[3]))
        hashes = {x[1] for x in existing}
        missing_targets = [p for p in paths if not os.path.exists(p) and os.path.exists(os.path.dirname(p))]

        if do_sync and (len(hashes) > 1 or missing_targets):
            for p in paths:
                parent = os.path.dirname(p)
                if not os.path.exists(parent):
                    continue
                if p != canonical_path and (not os.path.exists(p) or sha256_file(p)[:12] != canonical_hash):
                    shutil.copy2(canonical_path, p)
                    report["synced_files"].append(p)
            existing = [
                (p, sha256_file(p)[:12], os.path.getsize(p), os.path.getmtime(p))
                for p in paths
                if os.path.exists(p)
            ]
            hashes = {x[1] for x in existing}
            missing_targets = [p for p in paths if not os.path.exists(p) and os.path.exists(os.path.dirname(p))]

        is_synced = len(hashes) == 1 and not missing_targets
        if not is_synced:
            report["status"] = "DRIFT_DETECTED"
        report["documents"][doc_name] = {
            "status": "SYNCED" if is_synced else "DRIFT",
            "canonical": canonical_path,
            "hash": canonical_hash,
            "size": canonical_size,
            "copies": len(existing),
            "expected_copies": len([p for p in paths if os.path.exists(os.path.dirname(p))]),
        }
    return report


def subroutine_dist_sync(do_sync: bool = False) -> dict:
    report = {
        "name": "subroutine_dist_sync",
        "status": "OK",
        "diffs": [],
        "synced_files": [],
        "jars": {},
        "geodata_regions": 0,
    }
    ignore_dirs = {"geodata", "log", "libs", "cachedir", "db_installer"}
    ignore_files = {"hexid.txt", "ipconfig.xml", "readme.txt"}

    src_game = os.path.join(SRC_DIST_DIR, "game")
    bld_game = os.path.join(BLD_DIST_DIR, "game")

    for root, dirs, files in os.walk(src_game):
        dirs[:] = [d for d in dirs if d not in ignore_dirs]
        rel = os.path.relpath(root, src_game)
        bld_root = os.path.join(bld_game, rel) if rel != "." else bld_game
        for f in files:
            if f in ignore_files:
                continue
            p_src = os.path.join(root, f)
            p_bld = os.path.join(bld_root, f)
            rel_file = os.path.join("game", rel, f) if rel != "." else os.path.join("game", f)
            if not os.path.exists(p_bld):
                if do_sync:
                    os.makedirs(os.path.dirname(p_bld), exist_ok=True)
                    shutil.copy2(p_src, p_bld)
                    report["synced_files"].append(rel_file)
                else:
                    report["diffs"].append({"type": "MISSING_IN_BUILD_DIST", "file": rel_file})
            else:
                h_src = sha256_file(p_src)[:12]
                h_bld = sha256_file(p_bld)[:12]
                if h_src != h_bld:
                    report["diffs"].append({
                        "type": "CONTENT_DIFF",
                        "file": rel_file,
                        "src_hash": h_src,
                        "bld_hash": h_bld,
                        "newer": "build/dist" if os.path.getmtime(p_bld) > os.path.getmtime(p_src) else "dist",
                    })

    # Check GameServer.jar & LoginServer.jar
    gs_jar = os.path.join(BLD_DIST_DIR, "libs", "GameServer.jar")
    ls_jar = os.path.join(BLD_DIST_DIR, "libs", "LoginServer.jar")
    if os.path.exists(gs_jar):
        gs_mtime = os.path.getmtime(gs_jar)
        newer_java = []
        for root, _, files in os.walk(JAVA_SRC_DIR):
            for f in files:
                if f.endswith(".java"):
                    p = os.path.join(root, f)
                    if os.path.getmtime(p) > gs_mtime:
                        newer_java.append(os.path.relpath(p, JAVA_SRC_DIR))
        with zipfile.ZipFile(gs_jar, "r") as zf:
            manifest = zf.read("META-INF/MANIFEST.MF").decode("utf-8", errors="ignore")
            has_main = "Main-Class:" in manifest
        report["jars"]["GameServer.jar"] = {
            "has_main_manifest": has_main,
            "uncompiled_java_files": newer_java,
        }
        if not has_main or newer_java:
            report["status"] = "DRIFT_DETECTED"

    if os.path.exists(ls_jar):
        with zipfile.ZipFile(ls_jar, "r") as zf:
            names = zf.namelist()
            gspackets = [n for n in names if "loginserver/network/gameserverpackets/" in n and n.endswith(".class")]
        report["jars"]["LoginServer.jar"] = {
            "total_entries": len(names),
            "gameserverpackets_classes": len(gspackets),
        }
        if len(gspackets) < 18:
            report["status"] = "DRIFT_DETECTED"

    geo_dir = os.path.join(BLD_DIST_DIR, "game", "data", "geodata")
    if os.path.exists(geo_dir):
        report["geodata_regions"] = len([f for f in os.listdir(geo_dir) if f.endswith(".l2j")])

    if report["diffs"]:
        report["status"] = "DRIFT_DETECTED"
    return report


def subroutine_client_sync(do_sync: bool = False) -> dict:
    report = {
        "name": "subroutine_client_sync",
        "status": "OK",
        "binary_checks": {},
        "diffs": [],
        "synced_files": [],
    }
    sys_dir = os.path.join(STANDALONE_CLIENT_DIR, "system")

    # Verify binary patch signatures
    eng_path = os.path.join(sys_dir, "Engine.dll")
    if os.path.exists(eng_path):
        with open(eng_path, "rb") as f:
            f.seek(0x39B750)
            gg_bytes = f.read(5).hex()
            f.seek(0x4A3AE0)
            rev_str = f.read(16).decode("ascii", errors="ignore").rstrip("\x00")
        report["binary_checks"]["Engine.dll"] = {
            "gameguard_bypass_0x39B750": gg_bytes == "33c0fec0c3",
            "protocol_revision_0x4A3AE0": rev_str,
        }

    nw_path = os.path.join(sys_dir, "nwindow.dll")
    if os.path.exists(nw_path):
        nw_hash = sha256_file(nw_path)[:16]
        with open(nw_path, "rb") as f:
            f.seek(0x004C80C2)
            r1 = f.read(2).hex()
            f.seek(0x004CAC1C)
            r2 = f.read(2).hex()
            f.seek(0x004E2024)
            r3 = f.read(2).hex()
        report["binary_checks"]["nwindow.dll"] = {
            "sha256_prefix": nw_hash,
            "reloc_neutralized": (r1 == "0000" and r2 == "0000" and r3 == "0000"),
        }

    u_path = os.path.join(sys_dir, "interface.u")
    xdat_path = os.path.join(sys_dir, "interface.xdat")
    if os.path.exists(u_path) and os.path.exists(xdat_path):
        report["binary_checks"]["interface.u_xdat"] = {
            "interface_u_size": os.path.getsize(u_path),
            "interface_u_zero_delta": os.path.getsize(u_path) == 4061011,
            "interface_xdat_size": os.path.getsize(xdat_path),
        }

    # Compare tracked client files between Standalone Client and Server Repo client/ mirror
    ignore_runtime = {
        "Running.ini",
        "CheckGrp.log",
        "l2.log",
        "L2CompiledShader.bin",
        "Option.ini",
        "WindowsInfo.ini",
        "chatfilter.ini",
        "l2.ini",
        "s_info.ini",
        ".gitignore",
    }
    for sub in ["", "system", "tools"]:
        d1 = os.path.join(STANDALONE_CLIENT_DIR, sub)
        d2 = os.path.join(SERVER_CLIENT_DIR, sub)
        if not os.path.exists(d1) or not os.path.exists(d2):
            continue
        f1 = {f for f in os.listdir(d1) if os.path.isfile(os.path.join(d1, f)) and f not in ignore_runtime}
        f2 = {f for f in os.listdir(d2) if os.path.isfile(os.path.join(d2, f)) and f not in ignore_runtime}
        for f in sorted(f1 | f2):
            p1 = os.path.join(d1, f)
            p2 = os.path.join(d2, f)
            rel_f = os.path.join(sub, f) if sub else f
            if not os.path.exists(p1) or not os.path.exists(p2):
                report["diffs"].append({"type": "MISSING_MIRROR", "file": rel_f})
            elif sha256_file(p1) != sha256_file(p2):
                report["diffs"].append({"type": "HASH_MISMATCH", "file": rel_f})

    if report["diffs"]:
        report["status"] = "DRIFT_DETECTED"
    return report


def subroutine_git_ledger_audit() -> dict:
    report = {"name": "subroutine_git_ledger_audit", "status": "OK", "server_git": {}, "client_git": {}}
    ledger_path = os.path.join(SERVER_REPO, "FULL_PROJECT_ENGINEERING_LEDGER.md")
    sync_path = os.path.join(SERVER_REPO, "AGENT_SYNC_AND_RECONCILIATION.md")
    combined_docs = ""
    for p in (ledger_path, sync_path):
        if os.path.exists(p):
            combined_docs += open(p, "r", encoding="utf-8", errors="ignore").read()

    # Check server repo recent commits
    out = subprocess.check_output(
        ["git", "-C", SERVER_REPO, "log", "-n", "15", "--format=%h|%s"], text=True
    ).strip()
    untracked_commits = []
    for line in out.splitlines():
        short_hash, subj = line.split("|", 1)
        if short_hash[:7] not in combined_docs and not subj.startswith("docs"):
            untracked_commits.append({"hash": short_hash, "subject": subj})

    srv_status = subprocess.check_output(["git", "-C", SERVER_REPO, "status", "-s"], text=True).strip()
    cli_status = subprocess.check_output(["git", "-C", STANDALONE_CLIENT_DIR, "status", "-s"], text=True).strip()

    report["server_git"] = {
        "dirty_files": len(srv_status.splitlines()) if srv_status else 0,
        "undocumented_recent_commits": untracked_commits,
    }
    report["client_git"] = {
        "dirty_files": len(cli_status.splitlines()) if cli_status else 0,
    }
    if untracked_commits:
        report["status"] = "UNDOCUMENTED_COMMITS"
    return report


def subroutine_agent_roster() -> dict:
    report = {"name": "subroutine_agent_roster", "status": "OK", "agents": []}
    for ag in AGENT_ROSTER:
        cid = ag["id"]
        bdir = os.path.join(BRAIN_ROOT, cid)
        tpath = os.path.join(bdir, ".system_generated", "logs", "transcript.jsonl")
        steps = 0
        last_ts = None
        if os.path.exists(tpath):
            with open(tpath, "r", encoding="utf-8", errors="ignore") as f:
                lines = f.readlines()
                steps = len(lines)
                if lines:
                    try:
                        last_ts = json.loads(lines[-1]).get("created_at")
                    except Exception:
                        pass
        artifacts = []
        if os.path.exists(bdir):
            artifacts = sorted([f for f in os.listdir(bdir) if f.endswith(".md")])
        report["agents"].append({
            "agent": ag["num"],
            "id": cid,
            "role": ag["role"],
            "steps": steps,
            "last_active": last_ts,
            "artifacts": artifacts,
        })
    return report


def main():
    parser = argparse.ArgumentParser(description="L2 Gracia Epilogue Automated Auditor & Sync Suite")
    parser.add_argument("--check", action="store_true", help="Run all audit subroutines in read-only mode")
    parser.add_argument("--sync", action="store_true", help="Synchronize shared documentation across all roots & brain dirs")
    parser.add_argument("--json", action="store_true", help="Output full audit report as JSON")
    args = parser.parse_args()

    do_sync = args.sync
    results = {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "auditor_agent_id": "38e33746-e411-44db-8ed9-525f8cf93b55",
        "subroutines": [
            subroutine_doc_drift(do_sync=do_sync),
            subroutine_dist_sync(do_sync=False),
            subroutine_client_sync(do_sync=False),
            subroutine_git_ledger_audit(),
            subroutine_agent_roster(),
        ],
    }

    if args.json:
        print(json.dumps(results, indent=2))
        return

    print("=" * 84)
    print(f" L2 GRACIA EPILOGUE — AUTOMATED AUDITOR REPORT ({results['timestamp']})")
    print(" Auditor Agent: 38e33746-e411-44db-8ed9-525f8cf93b55")
    print("=" * 84)
    for sub in results["subroutines"]:
        print(f"\n[{sub['status']}] {sub['name']}:")
        if sub["name"] == "subroutine_doc_drift":
            for doc, info in sub["documents"].items():
                print(
                    f"  - {doc:44s} : {info['status']:6s} | {info['hash']} ({info['size']:6d} B, {info['copies']}/{info['expected_copies']} copies)"
                )
            if sub["synced_files"]:
                print(f"  * Synced {len(sub['synced_files'])} file(s): {sub['synced_files']}")
        elif sub["name"] == "subroutine_dist_sync":
            print(f"  - Geodata regions in build/dist/game/data/geodata : {sub['geodata_regions']} .l2j files")
            print(f"  - GameServer.jar  : {sub['jars'].get('GameServer.jar')}")
            print(f"  - LoginServer.jar : {sub['jars'].get('LoginServer.jar')}")
            if sub["diffs"]:
                for d in sub["diffs"]:
                    print(f"  ! DIFF: {d}")
            else:
                print("  - Zero config/data diffs between dist/ and build/dist/")
        elif sub["name"] == "subroutine_client_sync":
            for k, v in sub["binary_checks"].items():
                print(f"  - Binary Check [{k}]: {v}")
            if sub["diffs"]:
                for d in sub["diffs"]:
                    print(f"  ! CLIENT MIRROR DIFF: {d}")
            else:
                print("  - Client Lineage 2 Gracia Epilogue <-> l2jmobius_dwea_fork/client: 100% synced")
        elif sub["name"] == "subroutine_git_ledger_audit":
            print(f"  - Server Repo (`l2jmobius_dwea_fork`)     : {sub['server_git']}")
            print(f"  - Client Repo (`Client Lineage 2 ...`)    : {sub['client_git']}")
        elif sub["name"] == "subroutine_agent_roster":
            for a in sub["agents"]:
                print(
                    f"  - {a['agent']} ({a['id']}) | {a['role']:54s} | steps={a['steps']:4d} | artifacts={len(a['artifacts'])}"
                )


if __name__ == "__main__":
    main()
