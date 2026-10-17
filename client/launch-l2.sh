#!/usr/bin/env bash
# ==============================================================================
# Lineage 2 Gracia Epilogue (CT2.4) — Native-Feeling Linux Launcher (Wine/Proton)
# ==============================================================================
# Features:
#   1. Strips Lineage2Ver111 / Lineage2Ver413 encryption from all .ini & .int
#      files into plain, human-editable text (originals saved in system/backup_enc/)
#   2. Supports CLI connection flags:
#        ./launch-l2.sh -server 127.0.0.1 -login dwea -pass 123 -slot 0
#   3. Mandatory DLL overrides (dsetup=n,b for GameGuard bypass + custom RSA key)
#   4. Disables nProtect KeyCrypt (L2UseKeyCrypt=false) while keeping npkcrypt.dll
#      loadable for NWindow.dll static imports
#   5. Installs Windows TrueType fonts (tahoma.ttf, times.ttf, arial.ttf) into
#      drive_c/windows/Fonts & system/ so interface.xdat / UFontManager renders text
#   6. Installs 32-bit DXVK (d3d9.dll -> Vulkan) + silences the obsolete 2004 AGP
#      warning popup in system/d3ddrv.dll
#   7. Binary-patches system/windrv.dll for dynamic window resizing, OS maximize
#      button, XWayland async geometry compatibility, and (0,0) Borderless Windowed
#      mode (UseWindowFrame=False)
#   8. Modernized controller (JOYPAD) support with XInput/SDL2 axis & D-Pad mapping
#   9. Interactive PyQt6 Visual Pre-Launch Configuration GUI with live Docker server
#      status indicators, 1-click GM promotion, and editable fields for all CLI &
#      l2.ini / Option.ini parameters (bypass with --no-gui / --cli)
# ==============================================================================
set -euo pipefail

# Resolve real script directory even if invoked via symlink
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
    DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"
    SOURCE="$(readlink "$SOURCE")"
    [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
CLIENT_DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"
SYSTEM_DIR="$CLIENT_DIR/system"
PREFIX_DIR="$CLIENT_DIR/.wine_l2_prefix"
BACKUP_DIR="$SYSTEM_DIR/backup_encrypted"

# Default CLI options
RUNTIME="auto"         # auto | wine | proton | umu
SERVER_ADDR=""         # e.g. 127.0.0.1
WINDOW_MODE="keep"     # keep | borderless | windowed | fullscreen
RESOLUTION=""          # e.g. 1920x1080 or 2560x1440
GAMEPAD_MODE="keep"    # keep | enable | disable
AUTOLOGIN_MODE=""      # enable | disable | ""
AUTOLOGIN_USER=""
AUTOLOGIN_PASS=""
AUTOLOGIN_SLOT=""      # "" or -1 = stop at Character Select if -login/-pass given without -slot; 0..7 = enter world
ENABLE_HUD="0"
CHECK_ONLY="0"
USE_GUI="1"            # 1 = show PyQt6 visual launcher before starting l2.exe, 0 = launch immediately
VIRTUAL_DESKTOP=""     # e.g. 1920x1080
USE_DXVK="1"           # 1 = use DXVK (Vulkan), 0 = use wined3d (OpenGL)
EXTRA_L2_ARGS=()

usage() {
    cat <<EOF
Usage: $(basename "$0") [OPTIONS]

Connection & Account Flags:
  -server, --server <IP>         Set ServerAddr in system/l2.ini (e.g. 127.0.0.1)
  -login,  --login <USER>        Set auto-login account username (enables AutoLogOn)
  -pass,   --pass <PASS>         Set auto-login account password
  -slot,   --slot <0-6|-1>       Character slot index for auto-login (-1 or omit = stop at Character Select)
  --autologin <USER> <PASS> [SLOT]
                                 Shorthand to set username, password, and optional slot
  --no-autologin                 Disable auto-login in system/l2.ini

Display, Controller & Runtime Flags:
  --gui                          Show interactive visual configuration window before launch (default)
  --no-gui, --cli                Skip visual configuration window and launch l2.exe immediately
  --res <WIDTHxHEIGHT>           Set resolution in Option.ini & l2.ini (e.g. 1920x1080)
  --borderless                   Borderless Windowed mode at (0,0) (UseWindowFrame=False)
  --windowed                     Framed Resizable Windowed mode (titlebar, drag-resize & maximize)
  --fullscreen                   Exclusive Fullscreen mode (StartupFullScreen=True)
  --gamepad                      Enable controller support (UseJoystick=True)
  --no-gamepad                   Disable controller support (UseJoystick=False)
  --desktop <WIDTHxHEIGHT>       Run inside a Wine virtual desktop window of given size
  --runtime <auto|wine|proton|umu>
                                 Choose runner (default: auto)
  --no-dxvk                      Use Wine OpenGL (wined3d) instead of DXVK (Vulkan)
  --hud                          Enable DXVK/Gallium FPS & frametime overlay
  --check                        Run pre-flight diagnostics & verify patches without launching
  -h, --help                     Show this help message
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --check)
            CHECK_ONLY="1"
            shift
            ;;
        --gui|-gui)
            USE_GUI="1"
            shift
            ;;
        --no-gui|-no-gui|--cli|-cli)
            USE_GUI="0"
            shift
            ;;
        --runtime|-runtime)
            RUNTIME="$2"
            shift 2
            ;;
        --server|-server)
            SERVER_ADDR="$2"
            shift 2
            ;;
        --login|-login)
            AUTOLOGIN_MODE="enable"
            AUTOLOGIN_USER="$2"
            shift 2
            ;;
        --pass|-pass)
            AUTOLOGIN_MODE="enable"
            AUTOLOGIN_PASS="$2"
            shift 2
            ;;
        --slot|-slot)
            AUTOLOGIN_SLOT="$2"
            shift 2
            ;;
        --autologin|-autologin)
            AUTOLOGIN_MODE="enable"
            AUTOLOGIN_USER="$2"
            AUTOLOGIN_PASS="$3"
            if [[ $# -ge 4 && "$4" =~ ^(-1|[0-7])$ ]]; then
                AUTOLOGIN_SLOT="$4"
                shift 4
            else
                AUTOLOGIN_SLOT="-1"
                shift 3
            fi
            ;;
        --no-autologin|-no-autologin)
            AUTOLOGIN_MODE="disable"
            shift
            ;;
        --res|-res)
            RESOLUTION="$2"
            shift 2
            ;;
        --borderless|-borderless)
            WINDOW_MODE="borderless"
            shift
            ;;
        --windowed|-windowed)
            WINDOW_MODE="windowed"
            shift
            ;;
        --fullscreen|-fullscreen)
            WINDOW_MODE="fullscreen"
            shift
            ;;
        --gamepad|-gamepad)
            GAMEPAD_MODE="enable"
            shift
            ;;
        --no-gamepad|-no-gamepad)
            GAMEPAD_MODE="disable"
            shift
            ;;
        --desktop|-desktop)
            VIRTUAL_DESKTOP="$2"
            shift 2
            ;;
        --no-dxvk)
            USE_DXVK="0"
            shift
            ;;
        --hud|-hud)
            ENABLE_HUD="1"
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            EXTRA_L2_ARGS+=("$1")
            shift
            ;;
    esac
done

echo "======================================================================"
echo " Lineage 2 Gracia Epilogue (CT2.4) — Linux Client Launcher"
echo " Client Root : $CLIENT_DIR"
echo "======================================================================"

# ------------------------------------------------------------------------------
# Step 1: Ensure case-insensitive symlinks exist for directories & .gly fonts
# ------------------------------------------------------------------------------
ensure_symlink() {
    local base_dir="$1"
    local link_name="$2"
    local target_name="$3"
    if [[ -e "$base_dir/$target_name" && ! -e "$base_dir/$link_name" ]]; then
        ln -s "$target_name" "$base_dir/$link_name"
        echo "[+] Created case-compatibility symlink: $link_name -> $target_name"
    fi
}
ensure_symlink "$CLIENT_DIR" "System"          "system"
ensure_symlink "$CLIENT_DIR" "Maps"            "MAPS"
ensure_symlink "$CLIENT_DIR" "Music"           "music"
ensure_symlink "$SYSTEM_DIR" "SmallFont-e.gly" "smallfont-e.gly"
ensure_symlink "$SYSTEM_DIR" "LargeFont-e.gly" "largefont-e.gly"
ensure_symlink "$SYSTEM_DIR" "SmallFont.gly"   "smallfont.gly"
ensure_symlink "$SYSTEM_DIR" "LargeFont.gly"   "largefont.gly"

# ------------------------------------------------------------------------------
# Step 2: Populate TrueType Fonts (tahoma.ttf, times.ttf, arial.ttf)
# ------------------------------------------------------------------------------
WIN_FONTS_DIR="$PREFIX_DIR/drive_c/windows/Fonts"
mkdir -p "$WIN_FONTS_DIR"

PROTON_DIR="/usr/share/steam/compatibilitytools.d/proton-cachyos-slr"
for font_src in \
    "/usr/share/wine/fonts/tahoma.ttf" \
    "/usr/share/wine/fonts/tahomabd.ttf" \
    "/usr/share/wine/fonts/marlett.ttf" \
    "/usr/share/wine/fonts/symbol.ttf" \
    "$PROTON_DIR/files/share/fonts/arial.ttf" \
    "$PROTON_DIR/files/share/fonts/arialbd.ttf" \
    "$PROTON_DIR/files/share/fonts/times.ttf" \
    "$PROTON_DIR/files/share/fonts/cour.ttf" \
    "$PROTON_DIR/files/share/fonts/courbd.ttf" \
    "$PROTON_DIR/files/share/fonts/micross.ttf" \
    "$PROTON_DIR/files/share/fonts/malgun.ttf"; do
    if [[ -f "$font_src" ]]; then
        fname="$(basename "$font_src")"
        [[ ! -f "$WIN_FONTS_DIR/$fname" ]] && cp -f "$font_src" "$WIN_FONTS_DIR/$fname"
        [[ ! -f "$SYSTEM_DIR/$fname" ]]    && cp -f "$font_src" "$SYSTEM_DIR/$fname"
    fi
done

ensure_symlink "$WIN_FONTS_DIR" "tahoma.TTF" "tahoma.ttf"
ensure_symlink "$WIN_FONTS_DIR" "Tahoma.ttf" "tahoma.ttf"
ensure_symlink "$WIN_FONTS_DIR" "ARIAL.TTF"  "arial.ttf"
ensure_symlink "$SYSTEM_DIR"    "tahoma.TTF" "tahoma.ttf"
ensure_symlink "$SYSTEM_DIR"    "Tahoma.ttf" "tahoma.ttf"
ensure_symlink "$SYSTEM_DIR"    "ARIAL.TTF"  "arial.ttf"
echo "[+] Verified TrueType UI fonts (tahoma.ttf, times.ttf, arial.ttf)"

# ------------------------------------------------------------------------------
# Step 3: Binary-patch system/d3ddrv.dll (AGP warning) & system/windrv.dll
#         (Dynamic Window Resizing, Maximize, XWayland Geometry & Borderless 0,0)
# ------------------------------------------------------------------------------
mkdir -p "$BACKUP_DIR"
if [[ ! -f "$BACKUP_DIR/d3ddrv.dll.orig" && -f "$SYSTEM_DIR/d3ddrv.dll" ]]; then
    cp "$SYSTEM_DIR/d3ddrv.dll" "$BACKUP_DIR/d3ddrv.dll.orig"
fi
if [[ ! -f "$BACKUP_DIR/windrv.dll.orig" && -f "$SYSTEM_DIR/windrv.dll" ]]; then
    cp "$SYSTEM_DIR/windrv.dll" "$BACKUP_DIR/windrv.dll.orig"
fi
if [[ ! -f "$BACKUP_DIR/dsetup.dll.orig" && -f "$SYSTEM_DIR/dsetup.dll" ]]; then
    cp "$SYSTEM_DIR/dsetup.dll" "$BACKUP_DIR/dsetup.dll.orig"
fi

python3 - "$SYSTEM_DIR/d3ddrv.dll" "$SYSTEM_DIR/windrv.dll" "$SYSTEM_DIR/dsetup.dll" <<'PYEOF'
import sys, os, struct

d3d_path, win_path, dsetup_path = sys.argv[1], sys.argv[2], sys.argv[3]

# 1. d3ddrv.dll: Silence obsolete 2004 AGP check popup (raw 0xC24A4 [2009] or 0xC2494 [2010]: 74 22 -> 90 90)
with open(d3d_path, "rb") as f:
    d3d = bytearray(f.read())
for off in (0xC24A4, 0xC2494):
    if d3d[off-8:off] == bytes.fromhex("9c41000000100000"):
        if d3d[off:off+2] == b"\x74\x22":
            d3d[off:off+2] = b"\x90\x90"
            with open(d3d_path, "wb") as f:
                f.write(d3d)
            print("[+] Patched system/d3ddrv.dll: silenced obsolete 2004 AGP check popup")
        elif d3d[off:off+2] == b"\x90\x90":
            print("[+] Verified system/d3ddrv.dll: 2004 AGP check popup already silenced")
        break

# 2. windrv.dll: Dynamic Window Resizing, Maximize, XWayland Sync & Borderless (0,0)
with open(win_path, "rb") as f:
    win = bytearray(f.read())

# Auto-detect 2009-09-28 clean build (+0x70) vs 2010-07-16 build (+0x00)
w_delta = 0x70 if win[0x1101F-6:0x1101F] == bytes.fromhex("453c00000000") else 0x00

windrv_patches = [
    # Patch 1a: Skip broken SIZE_MAXIMIZED cursor-trap & bMaximized=1
    (0x10FAF + w_delta, bytes.fromhex("83f802753d"), bytes.fromhex("eb40909090")),
    # Patch 1b: Force Fullscreen=0 on windowed WM_SIZE & mark [esi+0x234]=4
    (0x11141 + w_delta, bytes.fromhex("83bec8000000007508837d3c00750233c0"),
                        bytes.fromhex("33c0c78634020000040000009090909090")),
    # Patch 2: Skip redundant MoveWindow in ResizeViewport when called from WM_SIZE
    (0x0D43A + w_delta, bytes.fromhex("8b4e3cf781b8050000000200000f8591000000"),
                        bytes.fromhex("33c087863402000083f80490900f8491000000")),
    # Patch 3: Prevent async XWayland GetClientRect from overwriting SizeX/SizeY
    (0x0D4B9 + w_delta, bytes.fromhex("8b4540"), bytes.fromhex("eb1c90")),
    # Patch 4: Spawn WS_POPUP borderless & framed windows at PosX=0, PosY=0
    (0x0E865 + w_delta, bytes.fromhex("c745180000008039451c7507c7451c00000080"),
                        bytes.fromhex("c745180000000039451c7507c7451c00000000")),
]

changed = False
all_verified = True
for raw_off, orig_b, new_b in windrv_patches:
    cur = bytes(win[raw_off : raw_off + len(orig_b)])
    if cur == orig_b:
        win[raw_off : raw_off + len(orig_b)] = new_b
        changed = True
    elif cur != new_b:
        all_verified = False
        print(f"[!] Warning: unexpected bytes in windrv.dll at 0x{raw_off:x}: {cur.hex()}")

if changed:
    with open(win_path, "wb") as f:
        f.write(win)
    print("[+] Patched system/windrv.dll: enabled dynamic window resize, maximize & borderless (0,0)")
elif all_verified:
    print("[+] Verified system/windrv.dll: dynamic window resize, maximize & borderless patches active")


# 3. dsetup.dll: Rebase ImageBase from 0x00400000 to 0x12000000 (avoiding Wine locale.nls collision)
#                and fix .idata (RW) + .reloc (R) section permissions so UNetworkHandler login hook works
if os.path.exists(dsetup_path):
    with open(dsetup_path, "rb") as f:
        ds = bytearray(f.read())
    pe = struct.unpack_from("<I", ds, 0x3c)[0]
    img_base = struct.unpack_from("<I", ds, pe + 0x34)[0]
    opt_size = struct.unpack_from("<H", ds, pe + 20)[0]
    sec_off = pe + 24 + opt_size
    ds_changed = False
    if img_base == 0x00400000:
        new_base = 0x12000000
        delta = new_base - img_base
        struct.pack_into("<I", ds, pe + 0x34, new_base)
        for rva in [0x100e, 0x102b, 0x1034, 0x1049, 0x1057, 0x106f, 0x107b, 0x10a1]:
            roff = 0x400 + (rva - 0x1000)
            val = struct.unpack_from("<I", ds, roff)[0]
            struct.pack_into("<I", ds, roff, val + delta)
        ds_changed = True
    if struct.unpack_from("<I", ds, sec_off + 2*40 + 36)[0] != 0xc0000040:
        struct.pack_into("<I", ds, sec_off + 2*40 + 36, 0xc0000040)
        ds_changed = True
    if struct.unpack_from("<I", ds, sec_off + 4*40 + 36)[0] != 0x42000040:
        struct.pack_into("<I", ds, sec_off + 4*40 + 36, 0x42000040)
        ds_changed = True
    if ds_changed:
        with open(dsetup_path, "wb") as f:
            f.write(ds)
        print("[+] Patched system/dsetup.dll: rebased to 0x12000000 & fixed section permissions for Wine wow64")
    else:
        print("[+] Verified system/dsetup.dll: Wine wow64 relocation & section permissions active")
PYEOF

# ------------------------------------------------------------------------------
# Step 4: Install 32-bit DXVK (D3D9 -> Vulkan) into syswow64 & system32
#         and set Wine Windows Compatibility Version to Windows 7 SP1 (6.1.7601)
# ------------------------------------------------------------------------------
DXVK_32_DIR="$PROTON_DIR/files/lib/wine/dxvk/i386-windows"
SYS32_DIR="$PREFIX_DIR/drive_c/windows/system32"
SYSWOW64_DIR="$PREFIX_DIR/drive_c/windows/syswow64"
mkdir -p "$SYS32_DIR" "$SYSWOW64_DIR"

if [[ "$USE_DXVK" == "1" && -d "$DXVK_32_DIR" ]]; then
    cp -f "$DXVK_32_DIR/d3d9.dll" "$SYS32_DIR/d3d9.dll"
    cp -f "$DXVK_32_DIR/dxgi.dll" "$SYS32_DIR/dxgi.dll"
    cp -f "$DXVK_32_DIR/d3d9.dll" "$SYSWOW64_DIR/d3d9.dll"
    cp -f "$DXVK_32_DIR/dxgi.dll" "$SYSWOW64_DIR/dxgi.dll"
    echo "[+] Installed 32-bit DXVK (d3d9.dll + dxgi.dll -> Vulkan) into syswow64 & system32"
fi

# Ensure Wine reports Windows 7 SP1 (6.1 Build 7601) instead of Vista/Win8/Win10 (6.2/6.3 Build 9200/19045)
if [[ -f "$PREFIX_DIR/system.reg" ]] && ! grep -q '"CurrentBuildNumber"="7601"' "$PREFIX_DIR/system.reg" 2>/dev/null; then
    WINEPREFIX="$PREFIX_DIR" winecfg -v win7 >/dev/null 2>&1 || true
    echo "[+] Set Wine compatibility profile to Windows 7 SP1 (6.1 Build 7601)"
else
    echo "[+] Verified Wine compatibility profile: Windows 7 SP1 (6.1 Build 7601)"
fi

# ------------------------------------------------------------------------------
# Step 5: Strip Lineage2Ver111 / Lineage2Ver413 encryption from .ini & .int files
#         and apply CLI parameters (-server, -login, -pass, -slot, --res, display)
# ------------------------------------------------------------------------------
python3 - "$SYSTEM_DIR" "$BACKUP_DIR" "$SERVER_ADDR" "$WINDOW_MODE" "$RESOLUTION" \
          "$GAMEPAD_MODE" "$AUTOLOGIN_MODE" "$AUTOLOGIN_USER" "$AUTOLOGIN_PASS" "$AUTOLOGIN_SLOT" <<'PYEOF'
import glob, os, sys, re, shutil, zlib

sys_dir, backup_dir, server_addr, win_mode, res, gp_mode, al_mode, al_user, al_pass, al_slot = sys.argv[1:11]

MOD_413 = int(
    "75b4d6de5c016544068a1acf125869f43d2e09fc55b8b1e289556daf9b875763"
    "5593446288b3653da1ce91c87bb1a5c18f16323495c55d7d72c0890a83f69bfd"
    "1fd9434eb1c02f3e4679edfa43309319070129c267c85604d87bb65bae205de3"
    "707af1d2108881abb567c3b3d069ae67c3a4c6a3aa93d26413d4c66094ae2039", 16
)
EXP_413 = 0x1D

def decrypt_bytes(data):
    if len(data) >= 28:
        try:
            hdr = data[:28].decode("utf-16le")
        except Exception:
            hdr = ""
        if hdr == "Lineage2Ver111":
            body = bytearray(b ^ 0xAC for b in data[28:])
            # Strip trailing 20-byte 413 footer if present on converted .ini/.int files
            if len(body) >= 20 and body[-4:] == b"\xac\xac\xac\xac":
                body = body[:-20]
            return hdr, bytes(body).rstrip(b"\x00")
        elif hdr == "Lineage2Ver413":
            payload = data[28:]
            out = bytearray()
            for i in range(len(payload) // 128):
                blk = payload[i*128:(i+1)*128]
                dec = pow(int.from_bytes(blk, "big"), EXP_413, MOD_413).to_bytes(128, "big")
                sz = dec[3]
                if sz <= 124:
                    pad = (124 - sz) % 4
                    out.extend(dec[128 - sz - pad : 128 - pad])
            return hdr, zlib.decompress(bytes(out[4:])).rstrip(b"\x00")
    return None, data

# Strip encryption from all .ini and .int files (except Lineage2us.ini binary blob)
stripped_count = 0
for fname in sorted(os.listdir(sys_dir)):
    if fname.lower().endswith((".ini", ".int")) and fname != "Lineage2us.ini":
        fpath = os.path.join(sys_dir, fname)
        if not os.path.isfile(fpath):
            continue
        with open(fpath, "rb") as f:
            raw = f.read()
        hdr, plain = decrypt_bytes(raw)
        if hdr is not None:
            bpath = os.path.join(backup_dir, fname)
            if not os.path.exists(bpath):
                shutil.copy2(fpath, bpath)
            with open(fpath, "wb") as f:
                f.write(plain)
            stripped_count += 1

if stripped_count:
    print(f"[+] Stripped {stripped_count} encrypted .ini/.int files to plain text (backups in system/backup_encrypted/)")
else:
    print("[+] Verified all .ini & .int files in system/ are plain unencrypted text")

# Now update plain-text system/l2.ini and system/Option.ini
l2_ini_path = os.path.join(sys_dir, "l2.ini")
opt_ini_path = os.path.join(sys_dir, "Option.ini")

with open(l2_ini_path, "rb") as f:
    text = f.read().decode("latin1", errors="replace")

opt_text = ""
if os.path.exists(opt_ini_path):
    with open(opt_ini_path, "rb") as f:
        opt_text = f.read().decode("latin1", errors="replace")

def set_ini_key(content, section, key, value):
    sec_pattern = re.compile(rf"(\[{re.escape(section)}\][^\[]*)", re.IGNORECASE | re.DOTALL)
    m = sec_pattern.search(content)
    if not m:
        return content + f"\r\n[{section}]\r\n{key}={value}\r\n"
    sec_block = m.group(1)
    key_pattern = re.compile(rf"^({re.escape(key)}\s*=).*$", re.IGNORECASE | re.MULTILINE)
    if key_pattern.search(sec_block):
        new_sec = key_pattern.sub(rf"\g<1>{value}\r", sec_block)
    else:
        new_sec = sec_block.rstrip("\r\n") + f"\r\n{key}={value}\r\n\r\n"
    return content[:m.start(1)] + new_sec + content[m.end(1):]

def get_val(content, key):
    m = re.search(rf"^{re.escape(key)}\s*=\s*([^\r\n]+)", content, re.IGNORECASE | re.MULTILINE)
    return m.group(1).strip() if m else "?"

text = set_ini_key(text, "URL", "L2UseKeyCrypt", "false")
text = set_ini_key(text, "URL", "L2MaxPawnResourceLoad", "5.0")
text = set_ini_key(text, "Engine.GameEngine", "CacheSizeMegs", "512")
text = set_ini_key(text, "Editor.EditorEngine", "CacheSizeMegs", "512")
text = set_ini_key(text, "WinDrv.WindowsClient", "MinDesiredFrameRate", "60.000000")
text = set_ini_key(text, "D3DDrv.D3DRenderDevice", "ReduceMouseLag", "False")
text = set_ini_key(text, "D3DDrv.D3DRenderDevice", "UseTripleBuffering", "True")
text = set_ini_key(text, "D3DDrv.D3DRenderDevice", "LevelOfAnisotropy", "16")
if opt_text:
    opt_text = set_ini_key(opt_text, "Video", "GPUAnimation", "True")
text = re.sub(r"IsL2AutoLogOn\s*=\s*Ture", "IsL2AutoLogOn=False", text, flags=re.IGNORECASE)

if server_addr:
    text = set_ini_key(text, "URL", "ServerAddr", server_addr)

if al_mode == "enable":
    text = set_ini_key(text, "AutoLogOn", "IsL2AutoLogOn", "True")
    if al_user:
        text = set_ini_key(text, "AutoLogOn", "L2ID", al_user)
    if al_pass:
        text = set_ini_key(text, "AutoLogOn", "L2Passwd", al_pass)
    text = set_ini_key(text, "AutoLogOn", "L2Slot", al_slot if al_slot != "" else "-1")
elif al_mode == "disable":
    text = set_ini_key(text, "AutoLogOn", "IsL2AutoLogOn", "False")

if win_mode == "borderless":
    text = set_ini_key(text, "WinDrv.WindowsClient", "StartupFullscreen", "False")
    text = set_ini_key(text, "WinDrv.WindowsClient", "UseWindowFrame", "False")
    if opt_text:
        opt_text = set_ini_key(opt_text, "Video", "StartupFullScreen", "False")
elif win_mode == "windowed":
    text = set_ini_key(text, "WinDrv.WindowsClient", "StartupFullscreen", "False")
    text = set_ini_key(text, "WinDrv.WindowsClient", "UseWindowFrame", "True")
    if opt_text:
        opt_text = set_ini_key(opt_text, "Video", "StartupFullScreen", "False")
elif win_mode == "fullscreen":
    text = set_ini_key(text, "WinDrv.WindowsClient", "StartupFullscreen", "True")
    if opt_text:
        opt_text = set_ini_key(opt_text, "Video", "StartupFullScreen", "True")

if gp_mode == "enable":
    text = set_ini_key(text, "WinDrv.WindowsClient", "UseJoystick", "True")
    if opt_text:
        opt_text = set_ini_key(opt_text, "Game", "UseJoystick", "True")
elif gp_mode == "disable":
    text = set_ini_key(text, "WinDrv.WindowsClient", "UseJoystick", "False")
    if opt_text:
        opt_text = set_ini_key(opt_text, "Game", "UseJoystick", "False")

# Auto-detect native monitor resolution if Option.ini is still at legacy < 1920 resolution and --res wasn't passed
if not res and opt_text:
    try:
        cur_w = int(get_val(opt_text, "GamePlayViewportX"))
    except ValueError:
        cur_w = 0
    if cur_w < 1920:
        for mode_file in sorted(glob.glob("/sys/class/drm/*/modes")):
            try:
                first_mode = open(mode_file).readline().strip()
                if "x" in first_mode:
                    res = first_mode
                    break
            except Exception:
                pass

width, height = None, None
if res and "x" in res.lower():
    width, height = res.lower().split("x", 1)
    text = set_ini_key(text, "WinDrv.WindowsClient", "WindowedViewportX", width)
    text = set_ini_key(text, "WinDrv.WindowsClient", "WindowedViewportY", height)
    text = set_ini_key(text, "WinDrv.WindowsClient", "FullscreenViewportX", width)
    text = set_ini_key(text, "WinDrv.WindowsClient", "FullscreenViewportY", height)
    if opt_text:
        opt_text = set_ini_key(opt_text, "Video", "GamePlayViewportX", width)
        opt_text = set_ini_key(opt_text, "Video", "GamePlayViewportY", height)

with open(l2_ini_path, "wb") as f:
    f.write(text.encode("latin1"))

if opt_text:
    with open(opt_ini_path, "wb") as f:
        f.write(opt_text.encode("latin1"))

s_fs = get_val(opt_text or text, "StartupFullScreen").lower() == "true"
u_wf = get_val(text, "UseWindowFrame").lower() == "true"
if s_fs:
    disp_desc = "Exclusive Fullscreen"
elif not u_wf:
    disp_desc = "Borderless Windowed (WS_POPUP @ 0,0)"
else:
    disp_desc = "Framed Resizable Windowed (Dynamic WM_SIZE + Maximize)"

vp_x = get_val(opt_text or text, "GamePlayViewportX")
vp_y = get_val(opt_text or text, "GamePlayViewportY")

print(f"[+] Active plain-text system/l2.ini & system/Option.ini configuration:")
print(f"    ServerAddr        = {get_val(text, 'ServerAddr')}")
print(f"    Resolution        = {vp_x}x{vp_y}")
print(f"    DisplayMode       = {disp_desc}")
print(f"    UseJoystick       = {get_val(opt_text or text, 'UseJoystick')}")
print(f"    L2UseKeyCrypt     = {get_val(text, 'L2UseKeyCrypt')}")
print(f"    IsL2AutoLogOn     = {get_val(text, 'IsL2AutoLogOn')} (User: {get_val(text, 'L2ID')}, Slot: {get_val(text, 'L2Slot')})")
PYEOF

# ------------------------------------------------------------------------------
# Step 5.5: Interactive PyQt6 Visual Pre-Launch Configuration Dashboard
# ------------------------------------------------------------------------------
if [[ "$CHECK_ONLY" == "0" && "$USE_GUI" == "1" && -n "${DISPLAY:-}${WAYLAND_DISPLAY:-}" ]]; then
    GUI_ENV_FILE="$(mktemp)"
    trap 'rm -f "$GUI_ENV_FILE"' EXIT

    echo "[*] Opening Visual Pre-Launch Configuration Dashboard (pass --no-gui to skip)..."
    python3 - "$SYSTEM_DIR" "$GUI_ENV_FILE" "$RUNTIME" "$USE_DXVK" "$ENABLE_HUD" "$VIRTUAL_DESKTOP" "${EXTRA_L2_ARGS[*]:-}" <<'PYEOF'
import os, re, shlex, socket, struct, sys

sys_dir, env_out_path, init_runtime, init_dxvk, init_hud, init_vdesktop, init_extra_args = sys.argv[1:8]
l2_ini_path = os.path.join(sys_dir, "l2.ini")
opt_ini_path = os.path.join(sys_dir, "Option.ini")

with open(l2_ini_path, "rb") as f:
    l2_text = f.read().decode("latin1", errors="replace")

opt_text = ""
if os.path.exists(opt_ini_path):
    with open(opt_ini_path, "rb") as f:
        opt_text = f.read().decode("latin1", errors="replace")

def get_val(content, key, default=""):
    m = re.search(rf"^{re.escape(key)}\s*=\s*([^\r\n]+)", content, re.IGNORECASE | re.MULTILINE)
    return m.group(1).strip() if m else default

def set_ini_key(content, section, key, value):
    sec_pattern = re.compile(rf"(\[{re.escape(section)}\][^\[]*)", re.IGNORECASE | re.DOTALL)
    m = sec_pattern.search(content)
    if not m:
        return content + f"\r\n[{section}]\r\n{key}={value}\r\n"
    sec_block = m.group(1)
    key_pattern = re.compile(rf"^({re.escape(key)}\s*=).*$", re.IGNORECASE | re.MULTILINE)
    if key_pattern.search(sec_block):
        new_sec = key_pattern.sub(rf"\g<1>{value}\r", sec_block)
    else:
        new_sec = sec_block.rstrip("\r\n") + f"\r\n{key}={value}\r\n\r\n"
    return content[:m.start(1)] + new_sec + content[m.end(1):]

def check_port(host, port):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(0.25)
    ok = (s.connect_ex((host, int(port))) == 0)
    s.close()
    return ok

def mysql_exec(sql, db="l2jmobiusepilogue"):
    try:
        s = socket.create_connection(("127.0.0.1", 3306), timeout=1.0)
        hdr = s.recv(4)
        s.recv(int.from_bytes(hdr[:3], "little"))
        flags = 0x00008208
        body = struct.pack("<IIb23s", flags, 0x01000000, 33, b"\x00" * 23)
        body += b"root\x00\x00" + db.encode() + b"\x00"
        s.sendall(len(body).to_bytes(3, "little") + b"\x01" + body)
        rh = s.recv(4)
        rb = s.recv(int.from_bytes(rh[:3], "little"))
        if rb[0] != 0x00:
            s.close()
            return False, "MariaDB auth failed"
        qpkt = b"\x03" + sql.encode("utf-8")
        s.sendall(len(qpkt).to_bytes(3, "little") + b"\x00" + qpkt)
        qh = s.recv(4)
        qb = s.recv(int.from_bytes(qh[:3], "little"))
        s.close()
        if qb[0] == 0x00:
            msg = qb[5:].decode("latin1", errors="ignore")
            return True, msg or "Query executed"
        return False, qb[3:].decode("latin1", errors="ignore")
    except Exception as e:
        return False, str(e)

try:
    from PyQt6.QtCore import Qt
    from PyQt6.QtWidgets import (
        QApplication, QCheckBox, QComboBox, QDialog, QFormLayout, QGridLayout,
        QGroupBox, QHBoxLayout, QLabel, QLineEdit, QPushButton, QSpinBox,
        QVBoxLayout, QWidget
    )
except Exception as e:
    print(f"[!] PyQt6 unavailable ({e}); continuing with CLI launch.")
    with open(env_out_path, "w") as out:
        out.write('GUI_ACTION="launch"\n')
    sys.exit(0)

app = QApplication(sys.argv[:1])
app.setStyle("Fusion")

dlg = QDialog()
dlg.setWindowTitle("Lineage II Gracia Epilogue (CT2.4) — Pre-Launch Command Center")
dlg.setMinimumWidth(780)
dlg.setStyleSheet("""
    QDialog { background-color: #0f141d; color: #e2e8f0; font-family: 'Sans Serif'; font-size: 13px; }
    QGroupBox {
        background-color: #171f2e;
        border: 1px solid #2d3748;
        border-radius: 6px;
        margin-top: 12px;
        padding: 12px 10px 10px 10px;
        font-weight: bold;
        color: #ecc94b;
    }
    QGroupBox::title { subcontrol-origin: margin; left: 10px; padding: 0 6px; }
    QLabel { color: #cbd5e0; font-weight: normal; }
    QLineEdit, QComboBox, QSpinBox {
        background-color: #0b0f17;
        border: 1px solid #384459;
        border-radius: 4px;
        padding: 5px 8px;
        color: #f7fafc;
        selection-background-color: #3182ce;
    }
    QLineEdit:focus, QComboBox:focus, QSpinBox:focus { border: 1px solid #63b3ed; }
    QCheckBox { color: #e2e8f0; spacing: 8px; font-weight: normal; }
    QPushButton {
        background-color: #243044;
        border: 1px solid #3b4d6b;
        border-radius: 5px;
        padding: 6px 14px;
        color: #f7fafc;
        font-weight: bold;
    }
    QPushButton:hover { background-color: #2f3e58; border-color: #63b3ed; }
    QPushButton#LaunchBtn {
        background-color: #2b6cb0;
        border: 1px solid #63b3ed;
        padding: 8px 22px;
        font-size: 14px;
        color: #ffffff;
    }
    QPushButton#LaunchBtn:hover { background-color: #3182ce; }
    QPushButton#GmBtn {
        background-color: #553c9a;
        border: 1px solid #9f7aea;
        color: #faf5ff;
    }
    QPushButton#GmBtn:hover { background-color: #6b46c1; }
""")

root = QVBoxLayout(dlg)
root.setSpacing(8)
root.setContentsMargins(14, 12, 14, 14)

# Header Banner
hdr_lbl = QLabel("⚔️  Lineage II Gracia Epilogue (CT2.4) — Client & CLI Configuration")
hdr_lbl.setStyleSheet("font-size: 16px; font-weight: bold; color: #f6e05e; padding-bottom: 2px;")
root.addWidget(hdr_lbl)

# 1. Live Server & Database Status Bar
srv_box = QGroupBox("Local Docker Server Stack & GM Access Status")
srv_layout = QHBoxLayout(srv_box)
lbl_login_st = QLabel()
lbl_game_st = QLabel()
lbl_db_st = QLabel()
btn_refresh = QPushButton("🔄 Refresh")
btn_gm = QPushButton("👑 Promote Characters to Master GM (100)")
btn_gm.setObjectName("GmBtn")
btn_gm.setToolTip("Executes UPDATE characters SET accesslevel=100 in local MariaDB (127.0.0.1:3306) so Alt+G & //admin work immediately.")

srv_layout.addWidget(lbl_login_st)
srv_layout.addWidget(lbl_game_st)
srv_layout.addWidget(lbl_db_st)
srv_layout.addStretch()
srv_layout.addWidget(btn_refresh)
srv_layout.addWidget(btn_gm)
root.addWidget(srv_box)

# 2. Server Connection & Auto-Login Group
conn_box = QGroupBox("Connection & Instant Auto-Login (system/l2.ini [URL] & [AutoLogOn])")
conn_grid = QGridLayout(conn_box)

ed_server = QLineEdit(get_val(l2_text, "ServerAddr", "127.0.0.1"))
ed_port = QLineEdit(get_val(l2_text, "Port", "7777"))
chk_autologin = QCheckBox("Enable Auto-Login (--autologin / IsL2AutoLogOn=True — boots straight into character)")
chk_autologin.setChecked(get_val(l2_text, "IsL2AutoLogOn", "False").lower() == "true")

ed_user = QLineEdit(get_val(l2_text, "L2ID", "zodiac"))
ed_pass = QLineEdit(get_val(l2_text, "L2Passwd", "zodiac"))
ed_pass.setEchoMode(QLineEdit.EchoMode.Password)
chk_show_pass = QCheckBox("Show")
chk_show_pass.toggled.connect(lambda c: ed_pass.setEchoMode(QLineEdit.EchoMode.Normal if c else QLineEdit.EchoMode.Password))

sp_slot = QSpinBox()
sp_slot.setRange(-1, 7)
sp_slot.setSpecialValueText("None (-1: Stop at Character Select)")
try:
    raw_slot = get_val(l2_text, "L2Slot", "-1").strip()
    sp_slot.setValue(int(raw_slot) if raw_slot != "" else -1)
except ValueError:
    sp_slot.setValue(-1)

conn_grid.addWidget(QLabel("Server IP (-server):"), 0, 0)
conn_grid.addWidget(ed_server, 0, 1)
conn_grid.addWidget(QLabel("Game Port (Port):"), 0, 2)
conn_grid.addWidget(ed_port, 0, 3)

conn_grid.addWidget(chk_autologin, 1, 0, 1, 4)

conn_grid.addWidget(QLabel("Account ID (-login):"), 2, 0)
conn_grid.addWidget(ed_user, 2, 1)
conn_grid.addWidget(QLabel("Password (-pass):"), 2, 2)
pass_row = QHBoxLayout()
pass_row.addWidget(ed_pass)
pass_row.addWidget(chk_show_pass)
conn_grid.addLayout(pass_row, 2, 3)

conn_grid.addWidget(QLabel("Character Slot (-slot 0-6):"), 3, 0)
conn_grid.addWidget(sp_slot, 3, 1)
lbl_ac_hint = QLabel("ℹ️ AutoCreateAccounts=True on local Docker LoginServer (any new login/pass auto-registers)")
lbl_ac_hint.setStyleSheet("color: #90cdf4; font-size: 11px;")
conn_grid.addWidget(lbl_ac_hint, 3, 2, 1, 2)
root.addWidget(conn_box)

# 3. Display, Window Mode & Resolution Group
disp_box = QGroupBox("Display, Windowing & Resolution (Option.ini & l2.ini + windrv.dll Patches)")
disp_grid = QGridLayout(disp_box)

cb_winmode = QComboBox()
cb_winmode.addItem("Borderless Windowed @ (0,0)  [--borderless / UseWindowFrame=False]", "borderless")
cb_winmode.addItem("Framed Resizable Windowed (Titlebar + Drag-Resize + Maximize)  [--windowed]", "windowed")
cb_winmode.addItem("Exclusive Fullscreen  [--fullscreen / StartupFullScreen=True]", "fullscreen")

s_fs = get_val(opt_text or l2_text, "StartupFullScreen", "False").lower() == "true"
u_wf = get_val(l2_text, "UseWindowFrame", "False").lower() == "true"
if s_fs:
    cb_winmode.setCurrentIndex(2)
elif u_wf:
    cb_winmode.setCurrentIndex(1)
else:
    cb_winmode.setCurrentIndex(0)

cur_w_str = get_val(opt_text or l2_text, "GamePlayViewportX", get_val(l2_text, "WindowedViewportX", "1920"))
cur_h_str = get_val(opt_text or l2_text, "GamePlayViewportY", get_val(l2_text, "WindowedViewportY", "1080"))
try:
    cur_w, cur_h = int(cur_w_str), int(cur_h_str)
except ValueError:
    cur_w, cur_h = 1920, 1080

cb_res_preset = QComboBox()
presets = ["1920x1080", "2560x1440", "3840x2160", "1600x900", "1280x720", "Custom"]
for p in presets:
    cb_res_preset.addItem(p, p)

sp_width = QSpinBox()
sp_width.setRange(640, 7680)
sp_width.setValue(cur_w)
sp_height = QSpinBox()
sp_height.setRange(480, 4320)
sp_height.setValue(cur_h)

cur_res_str = f"{cur_w}x{cur_h}"
if cur_res_str in presets:
    cb_res_preset.setCurrentText(cur_res_str)
else:
    cb_res_preset.setCurrentText("Custom")

def on_preset_changed(txt):
    if "x" in txt:
        w, h = txt.split("x", 1)
        sp_width.blockSignals(True)
        sp_height.blockSignals(True)
        sp_width.setValue(int(w))
        sp_height.setValue(int(h))
        sp_width.blockSignals(False)
        sp_height.blockSignals(False)
        update_cli_preview()

def on_spin_res_changed():
    s = f"{sp_width.value()}x{sp_height.value()}"
    cb_res_preset.blockSignals(True)
    cb_res_preset.setCurrentText(s if s in presets else "Custom")
    cb_res_preset.blockSignals(False)
    update_cli_preview()

cb_res_preset.currentTextChanged.connect(on_preset_changed)
sp_width.valueChanged.connect(on_spin_res_changed)
sp_height.valueChanged.connect(on_spin_res_changed)

ed_vdesktop = QLineEdit(init_vdesktop)
ed_vdesktop.setPlaceholderText("Off (or e.g. 1920x1080 for Wine virtual desktop window)")

disp_grid.addWidget(QLabel("Display Mode:"), 0, 0)
disp_grid.addWidget(cb_winmode, 0, 1, 1, 3)

disp_grid.addWidget(QLabel("Resolution Preset (--res):"), 1, 0)
disp_grid.addWidget(cb_res_preset, 1, 1)
res_xy_row = QHBoxLayout()
res_xy_row.addWidget(QLabel("Width:"))
res_xy_row.addWidget(sp_width)
res_xy_row.addWidget(QLabel("Height:"))
res_xy_row.addWidget(sp_height)
disp_grid.addLayout(res_xy_row, 1, 2, 1, 2)

disp_grid.addWidget(QLabel("Virtual Desktop (--desktop):"), 2, 0)
disp_grid.addWidget(ed_vdesktop, 2, 1, 1, 3)
root.addWidget(disp_box)

# 4. Runtime, DXVK, Controller & Engine Options Group
rt_box = QGroupBox("Runtime, Vulkan (DXVK), Controller & Engine Switches")
rt_grid = QGridLayout(rt_box)

cb_runtime = QComboBox()
for r_opt in ["auto", "wine", "proton", "umu"]:
    cb_runtime.addItem(r_opt, r_opt)
cb_runtime.setCurrentText(init_runtime if init_runtime in ["auto", "wine", "proton", "umu"] else "auto")

chk_dxvk = QCheckBox("Enable 32-Bit DXVK (Direct3D 9 -> Vulkan)")
chk_dxvk.setChecked(init_dxvk == "1")

chk_hud = QCheckBox("Enable DXVK / Gallium FPS & Frametime HUD (--hud)")
chk_hud.setChecked(init_hud == "1")

chk_gamepad = QCheckBox("Enable Modernized Controller Support (--gamepad / UseJoystick=True)")
chk_gamepad.setChecked(get_val(opt_text or l2_text, "UseJoystick", "True").lower() == "true")

chk_console = QCheckBox("Enable Unreal Console & Commands (IsL2Console & IsUseCommand)")
chk_console.setChecked(get_val(l2_text, "IsL2Console", "True").lower() == "true")

chk_multibox = QCheckBox("Allow Multiple Client Windows / Multi-Box (EnableSecondWindow)")
chk_multibox.setChecked(get_val(l2_text, "EnableSecondWindow", "True").lower() == "true")

chk_nokeycrypt = QCheckBox("Bypass nProtect KeyCrypt (L2UseKeyCrypt=false — Required for Wine)")
chk_nokeycrypt.setChecked(get_val(l2_text, "L2UseKeyCrypt", "false").lower() == "false")

ed_extra_args = QLineEdit(init_extra_args)
ed_extra_args.setPlaceholderText("Optional raw l2.exe arguments (e.g. -L2ProtocolVersion)")

rt_grid.addWidget(QLabel("Runner (--runtime):"), 0, 0)
rt_grid.addWidget(cb_runtime, 0, 1)
rt_grid.addWidget(chk_dxvk, 0, 2, 1, 2)

rt_grid.addWidget(chk_gamepad, 1, 0, 1, 2)
rt_grid.addWidget(chk_hud, 1, 2, 1, 2)

rt_grid.addWidget(chk_console, 2, 0, 1, 2)
rt_grid.addWidget(chk_multibox, 2, 2, 1, 2)

rt_grid.addWidget(chk_nokeycrypt, 3, 0, 1, 4)

rt_grid.addWidget(QLabel("Extra l2.exe Args:"), 4, 0)
rt_grid.addWidget(ed_extra_args, 4, 1, 1, 3)
root.addWidget(rt_box)

# 5. Equivalent CLI Command Preview
cli_box = QGroupBox("Equivalent Headless CLI Command Preview")
cli_layout = QHBoxLayout(cli_box)
ed_cli_preview = QLineEdit()
ed_cli_preview.setReadOnly(True)
ed_cli_preview.setStyleSheet("color: #68d391; font-family: monospace; font-size: 12px;")
btn_copy_cli = QPushButton("📋 Copy CLI")
cli_layout.addWidget(ed_cli_preview)
cli_layout.addWidget(btn_copy_cli)
root.addWidget(cli_box)

def build_cli_string():
    parts = ["./launch-l2.sh", "--no-gui"]
    parts += ["-server", shlex.quote(ed_server.text().strip() or "127.0.0.1")]
    wmode = cb_winmode.currentData()
    parts.append(f"--{wmode}")
    parts += ["--res", f"{sp_width.value()}x{sp_height.value()}"]
    if chk_autologin.isChecked():
        parts += [
            "-login", shlex.quote(ed_user.text().strip()),
            "-pass", shlex.quote(ed_pass.text()),
        ]
        if sp_slot.value() >= 0:
            parts += ["-slot", str(sp_slot.value())]
    else:
        parts.append("--no-autologin")
    parts.append("--gamepad" if chk_gamepad.isChecked() else "--no-gamepad")
    if cb_runtime.currentData() != "auto":
        parts += ["--runtime", cb_runtime.currentData()]
    if not chk_dxvk.isChecked():
        parts.append("--no-dxvk")
    if chk_hud.isChecked():
        parts.append("--hud")
    if ed_vdesktop.text().strip():
        parts += ["--desktop", shlex.quote(ed_vdesktop.text().strip())]
    if ed_extra_args.text().strip():
        parts.append(ed_extra_args.text().strip())
    return " ".join(parts)

def update_cli_preview(*_):
    ed_cli_preview.setText(build_cli_string())

for w in [ed_server, ed_port, ed_user, ed_pass, ed_vdesktop, ed_extra_args]:
    w.textChanged.connect(update_cli_preview)
for c in [chk_autologin, chk_dxvk, chk_hud, chk_gamepad, chk_console, chk_multibox, chk_nokeycrypt]:
    c.toggled.connect(update_cli_preview)
cb_winmode.currentIndexChanged.connect(update_cli_preview)
cb_runtime.currentIndexChanged.connect(update_cli_preview)
sp_slot.valueChanged.connect(update_cli_preview)

def copy_cli():
    app.clipboard().setText(ed_cli_preview.text())
    btn_copy_cli.setText("✓ Copied!")
btn_copy_cli.clicked.connect(copy_cli)

def refresh_status():
    host = ed_server.text().strip() or "127.0.0.1"
    gport = ed_port.text().strip() or "7777"
    l_ok = check_port(host, 2106)
    g_ok = check_port(host, gport if gport.isdigit() else 7777)
    d_ok = check_port("127.0.0.1", 3306)
    lbl_login_st.setText(f"Login (2106): {'🟢 ONLINE' if l_ok else '🔴 OFFLINE'}")
    lbl_game_st.setText(f"Game ({gport}): {'🟢 ONLINE' if g_ok else '🔴 OFFLINE'}")
    lbl_db_st.setText(f"MariaDB (3306): {'🟢 ONLINE' if d_ok else '🔴 OFFLINE'}")

btn_refresh.clicked.connect(refresh_status)

def promote_gm():
    ok, msg = mysql_exec("UPDATE characters SET accesslevel=100 WHERE accesslevel >= 0")
    btn_gm.setText(f"✓ GM=100 Applied ({msg.strip()})" if ok else f"⚠ {msg}")

btn_gm.clicked.connect(promote_gm)

# Bottom Action Buttons
btn_row = QHBoxLayout()
btn_save_only = QPushButton("💾 Save Config Only")
btn_cancel = QPushButton("✖ Cancel")
btn_launch = QPushButton("🚀 Save & Launch Lineage II")
btn_launch.setObjectName("LaunchBtn")
btn_launch.setDefault(True)

btn_row.addWidget(btn_save_only)
btn_row.addStretch()
btn_row.addWidget(btn_cancel)
btn_row.addWidget(btn_launch)
root.addLayout(btn_row)

def persist_changes(action_name):
    global l2_text, opt_text
    srv = ed_server.text().strip() or "127.0.0.1"
    prt = ed_port.text().strip() or "7777"
    w_str = str(sp_width.value())
    h_str = str(sp_height.value())
    wmode = cb_winmode.currentData()

    l2_text = set_ini_key(l2_text, "URL", "ServerAddr", srv)
    l2_text = set_ini_key(l2_text, "URL", "Port", prt)
    l2_text = set_ini_key(l2_text, "URL", "L2UseKeyCrypt", "false" if chk_nokeycrypt.isChecked() else "true")
    l2_text = set_ini_key(l2_text, "L2WaterEffect", "IsL2Console", "True" if chk_console.isChecked() else "False")
    l2_text = set_ini_key(l2_text, "URL", "IsUseCommand", "True" if chk_console.isChecked() else "False")
    l2_text = set_ini_key(l2_text, "URL", "EnableSecondWindow", "true" if chk_multibox.isChecked() else "false")

    l2_text = set_ini_key(l2_text, "AutoLogOn", "IsL2AutoLogOn", "True" if chk_autologin.isChecked() else "False")
    l2_text = set_ini_key(l2_text, "AutoLogOn", "L2ID", ed_user.text().strip())
    l2_text = set_ini_key(l2_text, "AutoLogOn", "L2Passwd", ed_pass.text())
    l2_text = set_ini_key(l2_text, "AutoLogOn", "L2Slot", str(sp_slot.value()))

    if wmode == "borderless":
        l2_text = set_ini_key(l2_text, "WinDrv.WindowsClient", "StartupFullscreen", "False")
        l2_text = set_ini_key(l2_text, "WinDrv.WindowsClient", "UseWindowFrame", "False")
        if opt_text:
            opt_text = set_ini_key(opt_text, "Video", "StartupFullScreen", "False")
    elif wmode == "windowed":
        l2_text = set_ini_key(l2_text, "WinDrv.WindowsClient", "StartupFullscreen", "False")
        l2_text = set_ini_key(l2_text, "WinDrv.WindowsClient", "UseWindowFrame", "True")
        if opt_text:
            opt_text = set_ini_key(opt_text, "Video", "StartupFullScreen", "False")
    elif wmode == "fullscreen":
        l2_text = set_ini_key(l2_text, "WinDrv.WindowsClient", "StartupFullscreen", "True")
        if opt_text:
            opt_text = set_ini_key(opt_text, "Video", "StartupFullScreen", "True")

    l2_text = set_ini_key(l2_text, "WinDrv.WindowsClient", "WindowedViewportX", w_str)
    l2_text = set_ini_key(l2_text, "WinDrv.WindowsClient", "WindowedViewportY", h_str)
    l2_text = set_ini_key(l2_text, "WinDrv.WindowsClient", "FullscreenViewportX", w_str)
    l2_text = set_ini_key(l2_text, "WinDrv.WindowsClient", "FullscreenViewportY", h_str)
    l2_text = set_ini_key(l2_text, "WinDrv.WindowsClient", "UseJoystick", "True" if chk_gamepad.isChecked() else "False")

    if opt_text:
        opt_text = set_ini_key(opt_text, "Video", "GamePlayViewportX", w_str)
        opt_text = set_ini_key(opt_text, "Video", "GamePlayViewportY", h_str)
        opt_text = set_ini_key(opt_text, "Game", "UseJoystick", "True" if chk_gamepad.isChecked() else "False")

    with open(l2_ini_path, "wb") as f:
        f.write(l2_text.encode("latin1"))
    if opt_text:
        with open(opt_ini_path, "wb") as f:
            f.write(opt_text.encode("latin1"))

    with open(env_out_path, "w") as out:
        out.write(f'GUI_ACTION={shlex.quote(action_name)}\n')
        out.write(f'RUNTIME={shlex.quote(cb_runtime.currentData())}\n')
        out.write(f'USE_DXVK={"1" if chk_dxvk.isChecked() else "0"}\n')
        out.write(f'ENABLE_HUD={"1" if chk_hud.isChecked() else "0"}\n')
        out.write(f'VIRTUAL_DESKTOP={shlex.quote(ed_vdesktop.text().strip())}\n')
        out.write(f'EXTRA_L2_ARGS_STR={shlex.quote(ed_extra_args.text().strip())}\n')
    dlg.accept()

def on_cancel():
    with open(env_out_path, "w") as out:
        out.write('GUI_ACTION="cancel"\n')
    dlg.reject()

btn_launch.clicked.connect(lambda: persist_changes("launch"))
btn_save_only.clicked.connect(lambda: persist_changes("save_only"))
btn_cancel.clicked.connect(on_cancel)

refresh_status()
update_cli_preview()
dlg.exec()
PYEOF

    GUI_ACTION="cancel"
    EXTRA_L2_ARGS_STR=""
    if [[ -f "$GUI_ENV_FILE" ]]; then
        # shellcheck disable=SC1090
        source "$GUI_ENV_FILE"
    fi
    if [[ "$GUI_ACTION" == "cancel" ]]; then
        echo "[*] Launch cancelled from Visual Pre-Launch Dashboard."
        exit 0
    elif [[ "$GUI_ACTION" == "save_only" ]]; then
        echo "[✓] Configuration saved to system/l2.ini & system/Option.ini (without launching)."
        exit 0
    fi
    if [[ -n "${EXTRA_L2_ARGS_STR:-}" ]]; then
        read -r -a EXTRA_L2_ARGS <<< "$EXTRA_L2_ARGS_STR"
    else
        EXTRA_L2_ARGS=()
    fi
fi

# ------------------------------------------------------------------------------
# Step 6: Select Runner & Configure Environment
# ------------------------------------------------------------------------------
PROTON_CACHY="$PROTON_DIR/proton"

if [[ "$RUNTIME" == "auto" ]]; then
    if command -v wine >/dev/null 2>&1; then
        RUNTIME="wine"
    elif [[ -x "$PROTON_CACHY" ]]; then
        RUNTIME="proton"
    elif command -v umu-run >/dev/null 2>&1; then
        RUNTIME="umu"
    else
        echo "[!] Error: Neither wine, proton, nor umu-run was found in PATH."
        exit 1
    fi
fi

DLL_OVERRIDES="dsetup=n,b;vcomp=n,b;msxml4=n,b;openal32=n,b;wrap_oal=n,b;mscoree=d;mshtml=d"
if [[ "$USE_DXVK" == "1" ]]; then
    DLL_OVERRIDES="d3d9=n,b;dxgi=n,b;$DLL_OVERRIDES"
fi

export WINEDLLOVERRIDES="$DLL_OVERRIDES"
export __GLX_VENDOR_LIBRARY_NAME="nvidia"
export __GL_SHADER_DISK_CACHE=1
export __GL_SHADER_DISK_CACHE_SKIP_CLEANUP=1
export DXVK_STATE_CACHE=1
export DXVK_STATE_CACHE_PATH="$SYSTEM_DIR"
export WINEESYNC=1
export WINEFSYNC=1
export WINE_LARGE_ADDRESS_AWARE=1
export STAGING_SHARED_MEMORY=1
export WINEDEBUG="-all"

if [[ "$ENABLE_HUD" == "1" ]]; then
    export DXVK_HUD="fps,frametimes,gpuload,api"
    export GALLIUM_HUD="fps"
else
    export DXVK_LOG_LEVEL="none"
fi

echo "[+] Selected Runtime  : $RUNTIME (DXVK Vulkan: $USE_DXVK)"
echo "[+] DLL Overrides     : $WINEDLLOVERRIDES"

python3 - <<'PYEOF'
import socket
for name, port in [("MariaDB (Database)", 3306), ("LoginServer (Auth)", 2106), ("GameServer (Sieghardt)", 7777)]:
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(0.25)
    ok = (s.connect_ex(("127.0.0.1", port)) == 0)
    s.close()
    status = "ONLINE (listening)" if ok else "OFFLINE (not listening on 127.0.0.1)"
    print(f"[*] Local {name:22s} [127.0.0.1:{port}] : {status}")
PYEOF

if [[ "$CHECK_ONLY" == "1" ]]; then
    echo "[✓] Pre-flight check & plain-text config conversion complete (--check)."
    exit 0
fi

# ------------------------------------------------------------------------------
# Step 7: Launch system/l2.exe from inside system/ working directory
# ------------------------------------------------------------------------------
cd "$SYSTEM_DIR"
echo "[+] Launching system/l2.exe ${EXTRA_L2_ARGS[*]:-}..."

case "$RUNTIME" in
    wine)
        export WINEPREFIX="$PREFIX_DIR"
        if [[ -n "$VIRTUAL_DESKTOP" ]]; then
            exec wine explorer "/desktop=Lineage2,$VIRTUAL_DESKTOP" "$SYSTEM_DIR/l2.exe" "${EXTRA_L2_ARGS[@]}"
        else
            exec wine "$SYSTEM_DIR/l2.exe" "${EXTRA_L2_ARGS[@]}"
        fi
        ;;
    proton)
        export STEAM_COMPAT_DATA_PATH="$PREFIX_DIR/proton"
        export STEAM_COMPAT_CLIENT_INSTALL_PATH="${HOME}/.local/share/Steam"
        mkdir -p "$STEAM_COMPAT_DATA_PATH" "$STEAM_COMPAT_CLIENT_INSTALL_PATH"
        exec "$PROTON_CACHY" run "$SYSTEM_DIR/l2.exe" "${EXTRA_L2_ARGS[@]}"
        ;;
    umu)
        export WINEPREFIX="$PREFIX_DIR/umu"
        export PROTONPATH="$PROTON_DIR"
        export GAMEID="umu-default"
        mkdir -p "$WINEPREFIX"
        exec umu-run "$SYSTEM_DIR/l2.exe" "${EXTRA_L2_ARGS[@]}"
        ;;
    *)
        echo "[!] Unsupported runtime '$RUNTIME' (use wine, proton, or umu)"
        exit 1
        ;;
esac
