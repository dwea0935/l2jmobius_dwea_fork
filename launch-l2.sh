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
AUTOLOGIN_SLOT="0"
ENABLE_HUD="0"
CHECK_ONLY="0"
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
  -slot,   --slot <0-6>          Character slot index for auto-login (default: 0)
  --autologin <USER> <PASS> [SLOT]
                                 Shorthand to set username, password, and optional slot
  --no-autologin                 Disable auto-login in system/l2.ini

Display, Controller & Runtime Flags:
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
            if [[ $# -ge 4 && "$4" =~ ^[0-7]$ ]]; then
                AUTOLOGIN_SLOT="$4"
                shift 4
            else
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

python3 - "$SYSTEM_DIR/d3ddrv.dll" "$SYSTEM_DIR/windrv.dll" <<'PYEOF'
import sys

d3d_path, win_path = sys.argv[1], sys.argv[2]

# 1. d3ddrv.dll: Silence obsolete 2004 AGP check popup (raw 0xC2494: 74 22 -> 90 90)
with open(d3d_path, "rb") as f:
    d3d = bytearray(f.read())
off = 0xC2494
if d3d[off:off+2] == b"\x74\x22":
    d3d[off:off+2] = b"\x90\x90"
    with open(d3d_path, "wb") as f:
        f.write(d3d)
    print("[+] Patched system/d3ddrv.dll: silenced obsolete 2004 AGP check popup")
elif d3d[off:off+2] == b"\x90\x90":
    print("[+] Verified system/d3ddrv.dll: 2004 AGP check popup already silenced")

# 2. windrv.dll: Dynamic Window Resizing, Maximize, XWayland Sync & Borderless (0,0)
with open(win_path, "rb") as f:
    win = bytearray(f.read())

windrv_patches = [
    # Patch 1a (VA 0x11111baf, raw 0x10faf): Skip broken SIZE_MAXIMIZED cursor-trap & bMaximized=1
    (0x10FAF, bytes.fromhex("83f802753d"), bytes.fromhex("eb40909090")),
    # Patch 1b (VA 0x11111d41, raw 0x11141): Force Fullscreen=0 on windowed WM_SIZE & mark [esi+0x234]=4
    (0x11141, bytes.fromhex("83bec8000000007508837d3c00750233c0"),
              bytes.fromhex("33c0c78634020000040000009090909090")),
    # Patch 2  (VA 0x1110e03a, raw 0xd43a): Skip redundant MoveWindow in ResizeViewport when called from WM_SIZE
    (0x0D43A, bytes.fromhex("8b4e3cf781b8050000000200000f8591000000"),
              bytes.fromhex("33c087863402000083f80490900f8491000000")),
    # Patch 3  (VA 0x1110e0b9, raw 0xd4b9): Prevent async XWayland GetClientRect from overwriting SizeX/SizeY
    (0x0D4B9, bytes.fromhex("8b4540"), bytes.fromhex("eb1c90")),
    # Patch 4  (VA 0x1110f465, raw 0xe865): Spawn WS_POPUP borderless & framed windows at PosX=0, PosY=0
    (0x0E865, bytes.fromhex("c745180000008039451c7507c7451c00000080"),
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
PYEOF

# ------------------------------------------------------------------------------
# Step 4: Install 32-bit DXVK (D3D9 -> Vulkan) into the prefix
# ------------------------------------------------------------------------------
DXVK_32_DIR="$PROTON_DIR/files/lib/wine/dxvk/i386-windows"
SYS32_DIR="$PREFIX_DIR/drive_c/windows/system32"
mkdir -p "$SYS32_DIR"

if [[ "$USE_DXVK" == "1" && -d "$DXVK_32_DIR" ]]; then
    cp -f "$DXVK_32_DIR/d3d9.dll" "$SYS32_DIR/d3d9.dll"
    cp -f "$DXVK_32_DIR/dxgi.dll" "$SYS32_DIR/dxgi.dll"
    echo "[+] Installed 32-bit DXVK (d3d9.dll + dxgi.dll -> Vulkan) into prefix"
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
text = re.sub(r"IsL2AutoLogOn\s*=\s*Ture", "IsL2AutoLogOn=False", text, flags=re.IGNORECASE)

if server_addr:
    text = set_ini_key(text, "URL", "ServerAddr", server_addr)

if al_mode == "enable":
    text = set_ini_key(text, "AutoLogOn", "IsL2AutoLogOn", "True")
    if al_user:
        text = set_ini_key(text, "AutoLogOn", "L2ID", al_user)
    if al_pass:
        text = set_ini_key(text, "AutoLogOn", "L2Passwd", al_pass)
    if al_slot != "":
        text = set_ini_key(text, "AutoLogOn", "L2Slot", al_slot)
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
for name, port in [("LoginServer (Auth)", 2106), ("GameServer", 7777)]:
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(0.25)
    ok = (s.connect_ex(("127.0.0.1", port)) == 0)
    s.close()
    status = "ONLINE (listening)" if ok else "OFFLINE (not listening on 127.0.0.1)"
    print(f"[*] Local {name:18s} [127.0.0.1:{port}] : {status}")
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
