# Multi-Agent Reconciliation & File Ownership Notice

**Last Synced Timestamp:** 2026-10-16T05:48:30+02:00  
**Active Workspace:** `/home/dwea/Projects/L2`

---

## 1. Purpose of This File
Two autonomous agents are collaborating in `/home/dwea/Projects/L2`:
- **Agent 2 (Client & Linux Launcher Specialist):**
  - Authored [`launch-l2.sh`](file:///home/dwea/Projects/L2/Client%20Lineage%202%20Gracia%20Epilogue/launch-l2.sh) (with `--res`, `--borderless`, `--windowed`, `--fullscreen`, `--gamepad`, `--autologin`, `--check`, `--hud`).
  - Decrypted all `.ini` and `.int` files to plain text (backups in `system/backup_encrypted/`).
  - Binary-patched `system/d3ddrv.dll` (`0xC2494` AGP popup bypass, backup at `system/backup_encrypted/d3ddrv.dll.orig`).
  - Binary-patched `system/windrv.dll` (5 in-place patches at `0x10FAF`, `0x11141`, `0x0D43A`, `0x0D4B9`, `0x0E865` for dynamic `WM_SIZE` window resizing, OS maximize button, XWayland async geometry compatibility, and `(0,0)` Borderless Windowed mode; backup at `system/backup_encrypted/windrv.dll.orig`).
  - Upgraded `system/user.ini` and `system/Option.ini` (`1920x1080`, `UseJoystick=True` XInput/SDL2 controller support, Infinite Zoom).
  - Populated TrueType UI fonts (`tahoma.ttf`, `times.ttf`, `arial.ttf`) and 32-bit DXVK (`d3d9.dll` + `dxgi.dll`).
- **Agent 1 (Server Architecture & Native `GMWnd` UI Integration):**
  - Built the 5-Pillar Server Architecture in [`l2jmobius_dwea_fork`](file:///home/dwea/Projects/L2/l2jmobius_dwea_fork).
  - Integrated the **Unified Native C++ `GMWnd` (`Alt+G`) Master Console** into `system/interface.xdat` and `system/interface.u` (and server branch `feature/native-gmwnd-console` merged into `master`).
  - Publishing the complete Client + Server documentation and uploading the client repository to GitHub.

---

## 2. File Ownership & Non-Conflict Guarantees

### Preserved Untouched from Agent 2:
- [`Client Lineage 2 Gracia Epilogue/launch-l2.sh`](file:///home/dwea/Projects/L2/Client%20Lineage%202%20Gracia%20Epilogue/launch-l2.sh) (and symlink `/home/dwea/Projects/L2/launch-l2.sh`)
- `Client Lineage 2 Gracia Epilogue/.wine_l2_prefix/`
- `Client Lineage 2 Gracia Epilogue/system/*.ini` and `*.int` (plain-text decrypted configurations)
- `Client Lineage 2 Gracia Epilogue/system/d3ddrv.dll` (AGP check patched)
- `Client Lineage 2 Gracia Epilogue/system/windrv.dll` (dynamic resize, maximize, XWayland & borderless `(0,0)` patched)
- `Client Lineage 2 Gracia Epilogue/system/*.ttf` and `*.gly` symlinks
- `Client Lineage 2 Gracia Epilogue/system/backup_encrypted/` (all original encrypted backups)

### Modified by Agent 1 (with Full Backups in `system/backup_encrypted/` and `.bak`):
1. **`Client Lineage 2 Gracia Epilogue/system/interface.xdat`**
   - Original backed up at:
     - `system/backup_encrypted/interface.xdat.orig`
     - `system/interface.xdat.bak`
   - Changes:
     - Expanded `GMWnd` (`Alt+G` native C++ GM window) from `350x355` to `992x544` px.
     - Re-arranged the 19 native C++ controls (`EditBox`, 6 Graphical Player Inspector buttons, 3 `.dat` Search Tree buttons, and Quick Action buttons) into a clean 2-column `105x26` left control dock (`x=10..226`), keeping full `105px` button widths so no English labels clip.
     - Repurposed `ChangeServerButton` label to `sysstring[415]` (`Administrator`).
     - Added `GMWnd.texBack` (`GroupBox_DF`, `752x500`) and `GMWnd.HtmlViewer` (`HtmlCtrl` / `NCHtmlViewer`, `744x492` at `x=236, y=38`) on the right side of `GMWnd`.
2. **`Client Lineage 2 Gracia Epilogue/system/interface.u`**
   - Original backed up at:
     - `system/backup_encrypted/interface.u.orig`
     - `system/interface.u.bak`
   - Changes (100% zero-disk-length-delta in-place UnrealScript bytecode patches):
     - `GMFindTreeWnd.ShowSkillList` (`Export #13529`): Enabled `bSummon = true` (`0x28 -> 0x27`).
     - `GMFindTreeWnd.FindAllSkill` (`Export #13526`): Offset skill IDs by `+ 2000000` (`string(cID.ClassID + 2000000)`) so `//summon` distinguishes Items (`< 1000000`), NPCs (`1000000..1999999`), and Skills (`>= 2000000`).
     - `GMWnd.HandleTargetUpdate` (`Export #12137`): Prevents untargeting (`m_nowTargetID < 1`) from wiping text typed in `GMWnd.EditBox`.
     - `GMWnd.OnClickChangeServerButton` (`Export #12081`): Executes `ExecuteCommand("//admin")`.
     - `GMWnd.HandleShowGMWnd` (`Export #12139`): Executes `ExecuteCommand("//gmw")` when opening `GMWnd` (`Alt+G`) so the right-hand `GMWnd.HtmlViewer` immediately loads the live RBAC Admin Console.
     - `ItemDescWnd.OnLoad` (`Export #12029`) & `ItemDescWnd.OnEvent` (`Export #12028`): Binds `m_hItemDescWnd = GetWindowHandle("GMWnd")` and `m_hHtmlViewer = GetHtmlHandle("GMWnd.HtmlViewer")` gated by `if (!IsBuilderPC()) return;` so server-sent `NpcHtmlMessage(0, 1, html)` renders directly inside `GMWnd.HtmlViewer` with zero `PageLock` and zero popup conflict.

> **Note to Agent 2:** All Agent 2 updates (`windrv.dll`, `d3ddrv.dll`, `Option.ini`, `l2.ini`, `user.ini`, `launch-l2.sh`) have been detected, preserved, and documented in our Master Handoff Guide. Please continue to keep `system/interface.xdat` and `system/interface.u` intact.
