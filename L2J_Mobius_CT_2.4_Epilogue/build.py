#!/usr/bin/env python3
"""
L2J Mobius Gracia Epilogue Build Tool
Matches the build lifecycle of build.xml:
  1. Compiles Java source files using JDK (source/target release 24).
  2. Generates LoginServer.jar, GameServer.jar, and DatabaseInstaller.jar with manifests.
  3. Prepares deployable server distribution and zip archive in ../build.
"""

import fnmatch
import glob
import os
import shutil
import subprocess
import sys
import time
import zipfile

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_DIR = SCRIPT_DIR
BUILD_DIR = os.path.abspath(os.path.join(PROJECT_DIR, "..", "build"))
BIN_DIR = os.path.join(BUILD_DIR, "bin")
DIST_DIR = os.path.join(BUILD_DIR, "dist")
SRC_DIR = os.path.join(PROJECT_DIR, "java")
DATAPACK_DIR = os.path.join(PROJECT_DIR, "dist")
LIBS_DIR = os.path.join(DATAPACK_DIR, "libs")


def get_classpath_jars():
    jars = []
    for f in sorted(glob.glob(os.path.join(LIBS_DIR, "*.jar"))):
        if not f.endswith("-sources.jar"):
            jars.append(f)
    return jars


def get_manifest_classpath(jars):
    # Returns relative paths for jar manifest: ../libs/<filename>.jar
    return " ".join(f"../libs/{os.path.basename(j)}" for j in jars)


def clean():
    print(f"[*] Cleaning build directory: {BUILD_DIR}")
    if os.path.exists(BUILD_DIR):
        shutil.rmtree(BUILD_DIR)
    os.makedirs(BIN_DIR, exist_ok=True)
    os.makedirs(os.path.join(DIST_DIR, "libs"), exist_ok=True)
    os.makedirs(os.path.join(DIST_DIR, "db_installer"), exist_ok=True)


def compile_sources(jars):
    print("[*] Collecting Java source files...")
    sources = []
    for root, _, files in os.walk(SRC_DIR):
        for f in files:
            if f.endswith(".java"):
                sources.append(os.path.join(root, f))
    print(f"[*] Found {len(sources)} source files to compile.")

    sources_file = os.path.join(BUILD_DIR, "sources.txt")
    with open(sources_file, "w", encoding="utf-8") as sf:
        for s in sources:
            sf.write(f'"{s}"\n')

    cp_str = ":".join(jars)
    cmd = [
        "javac",
        "--release", "27",
        "-cp", cp_str,
        "-d", BIN_DIR,
        "-encoding", "UTF-8",
        f"@{sources_file}",
    ]

    print("[*] Compiling with javac (Java 27)...")
    start = time.time()
    res = subprocess.run(cmd, capture_output=True, text=True)
    elapsed = time.time() - start

    if res.returncode != 0:
        print(f"[!] Compilation failed in {elapsed:.2f}s with code {res.returncode}:")
        print(res.stderr)
        sys.exit(1)

    print(f"[+] Compilation succeeded in {elapsed:.2f}s!")


def create_jar(dest_jar, main_class, manifest_cp, include_patterns, exclude_patterns=None):
    print(f"[*] Packaging {os.path.basename(dest_jar)}...")
    manifest_lines = [
        "Manifest-Version: 1.0",
        "Created-By: L2J Mobius Build Tool",
        f"Main-Class: {main_class}",
        f"Class-Path: {manifest_cp}",
        "Implementation-URL: http://www.l2jmobius.org/",
        "\n",
    ]
    manifest_bytes = "\r\n".join(manifest_lines).encode("utf-8")

    with zipfile.ZipFile(dest_jar, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as zf:
        zf.writestr("META-INF/MANIFEST.MF", manifest_bytes)

        for root, _, files in os.walk(BIN_DIR):
            for file in files:
                abs_path = os.path.join(root, file)
                rel_path = os.path.relpath(abs_path, BIN_DIR).replace("\\", "/")

                # check exclude
                if exclude_patterns and any(fnmatch.fnmatch(rel_path, p) for p in exclude_patterns):
                    continue

                # check include
                if include_patterns == ["*"] or any(fnmatch.fnmatch(rel_path, p) for p in include_patterns):
                    zf.write(abs_path, rel_path)


def package_jars(manifest_cp):
    # LoginServer.jar
    login_jar = os.path.join(DIST_DIR, "libs", "LoginServer.jar")
    create_jar(
        login_jar,
        main_class="org.l2jmobius.loginserver.LoginServer",
        manifest_cp=manifest_cp,
        include_patterns=["*"],
        exclude_patterns=["*databaseinstaller*", "*gameserver*"],
    )

    # GameServer.jar
    game_jar = os.path.join(DIST_DIR, "libs", "GameServer.jar")
    create_jar(
        game_jar,
        main_class="org.l2jmobius.gameserver.GameServer",
        manifest_cp=manifest_cp,
        include_patterns=["*"],
        exclude_patterns=[
            "*databaseinstaller*",
            "*loginserver*",
            "*accountmanager*",
            "*registerserver*",
        ],
    )

    # DatabaseInstaller.jar
    db_jar = os.path.join(DIST_DIR, "db_installer", "DatabaseInstaller.jar")
    create_jar(
        db_jar,
        main_class="org.l2jmobius.tools.DatabaseInstaller",
        manifest_cp=manifest_cp,
        include_patterns=[
            "*databaseinstaller*",
            "*commons/ui*",
            "*commons/util*",
            "*SQLFilter*",
        ],
        exclude_patterns=None,
    )


def assemble_distribution(jars):
    print("[*] Assembling distribution files into build/dist...")
    # Copy libs into dist/libs
    dest_libs = os.path.join(DIST_DIR, "libs")
    for j in jars:
        shutil.copy2(j, dest_libs)

    # Copy datapack folders
    for item in os.listdir(DATAPACK_DIR):
        if item == "libs":
            continue
        src_path = os.path.join(DATAPACK_DIR, item)
        dst_path = os.path.join(DIST_DIR, item)
        if os.path.isdir(src_path):
            shutil.copytree(src_path, dst_path, dirs_exist_ok=True)
        else:
            shutil.copy2(src_path, dst_path)

    # Copy readme.txt
    readme = os.path.join(PROJECT_DIR, "readme.txt")
    if os.path.exists(readme):
        shutil.copy2(readme, DIST_DIR)

    # Create master zip package
    zip_path = os.path.join(BUILD_DIR, "L2J_Mobius_CT_2.4_Epilogue.zip")
    print(f"[*] Creating release archive: {zip_path}...")
    start = time.time()
    with zipfile.ZipFile(zip_path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as zf:
        for root, _, files in os.walk(DIST_DIR):
            for file in files:
                abs_path = os.path.join(root, file)
                rel_path = os.path.relpath(abs_path, DIST_DIR)
                zf.write(abs_path, rel_path)
    elapsed = time.time() - start
    zip_size_mb = os.path.getsize(zip_path) / (1024 * 1024)
    print(f"[+] Release archive created ({zip_size_mb:.2f} MB) in {elapsed:.2f}s!")


def main():
    start_total = time.time()
    print("=" * 60)
    print(" L2J Mobius CT 2.4 (Gracia Epilogue) Build Pipeline")
    print("=" * 60)

    jars = get_classpath_jars()
    manifest_cp = get_manifest_classpath(jars)

    clean()
    compile_sources(jars)
    package_jars(manifest_cp)
    assemble_distribution(jars)

    elapsed_total = time.time() - start_total
    print("=" * 60)
    print(f"[SUCCESS] Server built and packaged in {elapsed_total:.2f}s!")
    print(f"Artifacts located at: {BUILD_DIR}")
    print("=" * 60)


if __name__ == "__main__":
    main()
