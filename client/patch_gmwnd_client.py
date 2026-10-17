#!/usr/bin/env python3
"""
Lineage 2 Gracia Epilogue (CT2.4) — Native C++ GMWnd (Alt+G) v3 Top-Tab Master Console Patcher & Verifier

Patches:
  1. system/interface.xdat:
     - Expands GMWnd from 350x355 to 992x544 px.
     - Arranges the top of GMWnd (y=34, h=24) into a Community-Board-style horizontal Native C++
       Category Tab Bar on the left/center (7 category tabs: Dashboard, Players, World, Instances,
       Events & GM, Items & Char, Server & Reload) + 3 Native Client .dat Search Tree buttons on the
       right (Item .dat, NPC .dat, Skill .dat).
     - Parks the remaining 9 unused legacy widgets (EditBox, TextBox, DetailStatusButton,
       InventoryButton, MagicSkillButton, QuestButton, InfoButton, StoreButton, ClanButton) off-canvas
       at (-2000.0, -2000.0, 1x1) so all 19 original child widget records remain valid while freeing
       the entire left side of the window.
     - Expands GMWnd.HtmlViewer (HtmlCtrl / NCHtmlViewer) to full window width: 968x463 @ (12.0, 67.0)
       inside GroupBox_DF texBack (976x471 @ 8.0, 63.0) + top bar GroupBox_DF texDock1 (976x30 @ 8.0, 31.0),
       setting GMWnd child count to 22.

  2. system/interface.u (100% zero-disk-length-delta in-place UnrealScript bytecode patches):
     - GMFindTreeWnd.ShowSkillList (#10184): Enables bSummon = true (0x28 -> 0x27).
     - GMFindTreeWnd.FindAllSkill  (#10231): Offsets skill IDs by +2,000,000 so //summon
       distinguishes Items (<1,000,000), NPCs (1,000,000..1,999,999), and Skills (>=2,000,000).
     - GMWnd.HandleShowGMWnd       (#12109): Executes ExecuteCommand("//gmw") when opening
       GMWnd (Alt+G) so GMWnd.HtmlViewer immediately loads the live RBAC Admin Console.
     - GMWnd.HandleTargetUpdate    (#12112): Returns on untarget (m_nowTargetID < 1) without
       wiping text typed into GMWnd.EditBox.
     - GMWnd.OnClickButton         (#12120): Routes MoveButton ("Instances") to OnClickInfoButton (#12163)
       so it has 45 bytes of code space for ExecuteCommand("//gmw instances").
     - 7 Top Category Tab Handlers:
       * GMWnd.OnClickChangeServerButton  (#12192): ExecuteCommand("//gmw home")
       * GMWnd.OnClickPetitionButton      (#12173): ExecuteCommand("//gmw support")
       * GMWnd.OnClickTeleButton          (#12126): ExecuteCommand("//gmw world")
       * GMWnd.OnClickInfoButton          (#12163): ExecuteCommand("//gmw instances") (invoked by MoveButton)
       * GMWnd.OnClickMoveButton          (#12128): ExecuteCommand("//gmw instance")
       * GMWnd.OnClickForcePetitionButton (#12191): ExecuteCommand("//gmw events")
       * GMWnd.OnClickRecallButton        (#12129): ExecuteCommand("//gmw economy")
       * GMWnd.OnClickSendHomeButton      (#12174): ExecuteCommand("//gmw server")
     - ItemDescWnd.OnLoad (#12259) & ItemDescWnd.OnEvent (#12269): Binds m_hItemDescWnd =
       GetWindowHandle("GMWnd") and m_hHtmlViewer = GetHtmlHandle("GMWnd.HtmlViewer") gated by
       if (!IsBuilderPC()) return; so server NpcHtmlMessage(0, 1, html) renders directly inside
       GMWnd.HtmlViewer with zero PageLock and zero popup conflict.
"""

import os
import struct

CLIENT_SYS_DIR = "/home/dwea/Projects/L2/Client Lineage 2 Gracia Epilogue/system"
BACKUP_DIR = os.path.join(CLIENT_SYS_DIR, "backup_encrypted")


def read_index(buf: bytes, pos: int):
    b0 = buf[pos]
    pos += 1
    neg = b0 & 0x80
    val = b0 & 0x3F
    if b0 & 0x40:
        b1 = buf[pos]
        pos += 1
        val |= (b1 & 0x7F) << 6
        if b1 & 0x80:
            b2 = buf[pos]
            pos += 1
            val |= (b2 & 0x7F) << 13
            if b2 & 0x80:
                b3 = buf[pos]
                pos += 1
                val |= (b3 & 0x7F) << 20
                if b3 & 0x80:
                    b4 = buf[pos]
                    pos += 1
                    val |= b4 << 27
    return (-val if neg else val), pos


def sim_expr(code: bytes, dpos: int, mpos: int):
    op = code[dpos]
    dpos += 1
    mpos += 1
    if op in (0x00, 0x01, 0x02, 0x20, 0x21, 0x29):
        _, dpos = read_index(code, dpos)
        mpos += 4
    elif op == 0x04:
        dpos, mpos = sim_expr(code, dpos, mpos)
    elif op == 0x05:
        dpos += 1
        mpos += 1
        dpos, mpos = sim_expr(code, dpos, mpos)
    elif op == 0x06:
        dpos += 2
        mpos += 2
    elif op == 0x07:
        dpos += 2
        mpos += 2
        dpos, mpos = sim_expr(code, dpos, mpos)
    elif op == 0x0A:
        w = struct.unpack_from("<H", code, dpos)[0]
        dpos += 2
        mpos += 2
        if w != 0xFFFF:
            dpos, mpos = sim_expr(code, dpos, mpos)
    elif op in (0x0B, 0x16, 0x25, 0x26, 0x27, 0x28, 0x2A):
        pass
    elif op in (0x0F, 0x14, 0x10):
        dpos, mpos = sim_expr(code, dpos, mpos)
        dpos, mpos = sim_expr(code, dpos, mpos)
    elif op in (0x12, 0x19):
        dpos, mpos = sim_expr(code, dpos, mpos)
        dpos += 3
        mpos += 3
        dpos, mpos = sim_expr(code, dpos, mpos)
    elif op == 0x18:
        dpos += 2
        mpos += 2
        dpos, mpos = sim_expr(code, dpos, mpos)
    elif op in (0x1B, 0x1C):
        _, dpos = read_index(code, dpos)
        mpos += 4
        while True:
            sub_op = code[dpos]
            dpos, mpos = sim_expr(code, dpos, mpos)
            if sub_op == 0x16:
                break
    elif op == 0x1D:
        dpos += 4
        mpos += 4
    elif op == 0x1F:
        end = code.find(b"\x00", dpos) + 1
        ln = end - dpos
        dpos += ln
        mpos += ln
    elif op in (0x24, 0x2C):
        dpos += 1
        mpos += 1
    elif op in (0x2D, 0x37):
        dpos, mpos = sim_expr(code, dpos, mpos)
    elif op in (0x2E, 0x36):
        _, dpos = read_index(code, dpos)
        mpos += 4
        dpos, mpos = sim_expr(code, dpos, mpos)
    elif op in (0x38, 0x39):
        dpos += 1
        mpos += 1
        dpos, mpos = sim_expr(code, dpos, mpos)
    elif op == 0x41:
        dpos, mpos = sim_expr(code, dpos, mpos)
        dpos, mpos = sim_expr(code, dpos, mpos)
        dpos, mpos = sim_expr(code, dpos, mpos)
    elif op >= 0x70:
        while True:
            sub_op = code[dpos]
            dpos, mpos = sim_expr(code, dpos, mpos)
            if sub_op == 0x16:
                break
    else:
        raise ValueError(f"Unknown opcode {hex(op)} at dpos={dpos}")
    return dpos, mpos


def patch_interface_xdat():
    orig_path = os.path.join(BACKUP_DIR, "interface.xdat.orig")
    out_path = os.path.join(CLIENT_SYS_DIR, "interface.xdat")
    with open(orig_path, "rb") as f:
        xdat = bytearray(f.read())

    # Guard: Must be clean 2009-09-28 Interface.xdat (1,305,499 bytes)
    assert len(xdat) == 1305499, f"Expected clean 2009-09-28 interface.xdat.orig (1305499 B), got {len(xdat)} B"

    # 1. Verify GMWnd header @ 389560 and GuideWnd @ 395235
    gm_off = 389560
    guide_off = 395235
    assert xdat[gm_off : gm_off + 7] == b"\x06GMWnd\x00", "GMWnd signature mismatch"
    assert xdat[guide_off : guide_off + 10] == b"\x09GuideWnd\x00", "GuideWnd boundary mismatch"

    # 2. Expand GMWnd dimensions at offset gm_off + 65 (389625) from (350, 355) to (992, 544)
    w_orig, h_orig = struct.unpack_from("<ii", xdat, gm_off + 65)
    assert (w_orig, h_orig) == (350, 355), f"Unexpected GMWnd size: {(w_orig, h_orig)}"
    struct.pack_into("<ii", xdat, gm_off + 65, 992, 544)

    # 3. Locate GMWnd child count right before first child (\x08EditBox\x00)
    cc_off = xdat.find(b"\x13\x00\x00\x00\x08EditBox\x00", gm_off, gm_off + 1000)
    assert cc_off != -1, "Could not locate GMWnd child count before EditBox"
    # 19 original widgets + texDock1 (top bar GroupBox_DF) + texBack (main HTML GroupBox_DF) + HtmlViewer = 22
    struct.pack_into("<i", xdat, cc_off, 22)

    # 4. Collect all 19 widget parent-name offsets inside [gm_off..guide_off]
    widget_offsets = []
    pos = gm_off + 7
    while True:
        p = xdat.find(b"\x06GMWnd\x00", pos, guide_off)
        if p == -1:
            break
        if xdat[p + 7 : p + 29] == b"\x0aundefined\x00\x0aundefined\x00":
            widget_offsets.append(p)
        pos = p + 7
    assert len(widget_offsets) == 19, f"Expected 19 widgets, found {len(widget_offsets)}"

    btn_order_names = [
        "TeleButton",
        "MoveButton",
        "RecallButton",
        "DetailStatusButton",
        "InventoryButton",
        "MagicSkillButton",
        "QuestButton",
        "InfoButton",
        "StoreButton",
        "ClanButton",
        "PetitionButton",
        "SendHomeButton",
        "NPCListButton",
        "ItemListButton",
        "SkillListButton",
        "ForcePetitionButton",
        "ChangeServerButton",
    ]

    # GMWnd v3 Top-Tab Console Layout (BoardWnd Community-Board visual polish):
    #   - 7 Native C++ Category Tabs on the left/center (y=31.0, h=23, contiguous x=7..695)
    #     styled with L2UI_CT1.tab.Tab_DF_Tab_Unselected / L2UI_CT1.tab.Tab_DF_Tab_Selected
    #   - 3 Native Client .dat Search Tree Buttons on the right (y=28.0, h=24, x=716..980)
    #     styled with L2UI_CT1.Button.Button_DF / Button_DF_Click / Button_DF_Over
    #   - 7 Unused Legacy GM Inspector Buttons parked off-canvas at (-2000.0, -2000.0, 1x1)
    tab_btn_names = {
        "ChangeServerButton",
        "PetitionButton",
        "TeleButton",
        "MoveButton",
        "ForcePetitionButton",
        "RecallButton",
        "SendHomeButton",
    }
    btn_spec = {
        # 7 Native C++ Category Tabs (y=31.0, h=23, contiguous x=7.0..695.0)
        "ChangeServerButton":  (  7.0, 31.0,  96, 23, "Dashboard"),
        "PetitionButton":      (103.0, 31.0,  96, 23, "Players"),
        "TeleButton":          (199.0, 31.0,  96, 23, "World"),
        "MoveButton":          (295.0, 31.0,  98, 23, "Instances"),
        "ForcePetitionButton": (393.0, 31.0,  98, 23, "Events & GM"),
        "RecallButton":        (491.0, 31.0,  98, 23, "Items & Char"),
        "SendHomeButton":      (589.0, 31.0, 106, 23, "Server & Reload"),
        # 3 Native Client .dat Search Tree Popups (y=28.0, h=24, right-aligned above Tab_DF_bg_line)
        "ItemListButton":      (716.0, 28.0,  86, 24, "Item .dat"),
        "NPCListButton":       (804.0, 28.0,  86, 24, "NPC .dat"),
        "SkillListButton":     (892.0, 28.0,  88, 24, "Skill .dat"),
        # 7 Unused Legacy Inspector Buttons parked off-canvas
        "DetailStatusButton":  (-2000.0, -2000.0, 1, 1, "View Status"),
        "InventoryButton":     (-2000.0, -2000.0, 1, 1, "View Items"),
        "MagicSkillButton":    (-2000.0, -2000.0, 1, 1, "View Skills"),
        "QuestButton":         (-2000.0, -2000.0, 1, 1, "View Quests"),
        "InfoButton":          (-2000.0, -2000.0, 1, 1, "Target Debug"),
        "StoreButton":         (-2000.0, -2000.0, 1, 1, "Warehouse"),
        "ClanButton":          (-2000.0, -2000.0, 1, 1, "View Clan"),
    }

    # Update coordinates and sizes while offsets are still at their original positions
    for idx, p in enumerate(widget_offsets):
        ap = p + 7 + 22  # right after \x06GMWnd\x00\x0aundefined\x00\x0aundefined\x00
        anch_len = xdat[ap + 36]
        if anch_len > 0:
            anch_wnd = xdat[ap + 37 : ap + 37 + anch_len - 1].decode("latin1")
            fp = ap + 37 + anch_len
        else:
            anch_wnd = ""
            fp = ap + 37

        if idx == 0:  # EditBox parked off-canvas
            struct.pack_into("<ii", xdat, ap + 16, 1, 1)
            struct.pack_into("<ff", xdat, fp, -2000.0, -2000.0)
        elif idx == 18:  # TextBox parked off-canvas
            struct.pack_into("<ii", xdat, ap + 16, 1, 1)
            struct.pack_into("<ff", xdat, fp, -2000.0, -2000.0)
        else:  # Button 1..17
            bname = btn_order_names[idx - 1]
            tx, ty, tw, th, _ = btn_spec[bname]
            struct.pack_into("<ii", xdat, ap + 16, tw, th)
            struct.pack_into("<iii", xdat, ap + 24, 1, 1, 1)
            if anch_wnd == "":
                rx, ry = tx, ty
            else:
                px, py, _, _, _ = btn_spec[anch_wnd]
                rx, ry = tx - px, ty - py
            struct.pack_into("<ff", xdat, fp, rx, ry)

    # Slice the 19 individual widget records from xdat[cc_off+4 : guide_off]
    children_start = cc_off + 4
    children_end = guide_off
    starts = [children_start]
    pos = children_start + 1
    while len(starts) < 19:
        p1 = xdat.find(b"\x07Button\x00", pos, children_end)
        p2 = xdat.find(b"\x08TextBox\x00", pos, children_end)
        cands = [c for c in (p1, p2) if c != -1]
        nxt = min(cands)
        starts.append(nxt)
        pos = nxt + 1

    widget_recs = []
    for i in range(19):
        s = starts[i]
        e = starts[i + 1] if i + 1 < 19 else children_end
        widget_recs.append(bytearray(xdat[s:e]))

    # Upgrade the 7 Category Tabs with genuine CT1 Tab textures (Tab_DF_Tab_Unselected / Tab_DF_Tab_Selected)
    # and the remaining buttons with Button_DF / Button_DF_Click / Button_DF_Over textures
    old_btn_tex = (
        b" L2UI_CT1.Button.Button_DF_Small\x00"
        b" L2UI_CT1.Button.Button_DF_Small\x00"
        b"\nundefined\x00"
        b"\nundefined\x00"
    )
    for idx in range(1, 18):
        bname = btn_order_names[idx - 1]
        _, _, _, _, label = btn_spec[bname]
        rec = widget_recs[idx]
        tpos = rec.find(old_btn_tex)
        assert tpos != -1, f"Could not find Button_DF_Small texture block in {bname}"
        tail_pos = tpos + len(old_btn_tex)
        assert rec[tail_pos + 4 : tail_pos + 19] == b"\nundefined\x00\x01\x00\x00\x00", (
            f"Unexpected Button tail in {bname}"
        )
        lbl_bytes = label.encode("latin1") + b"\x00"
        if bname in tab_btn_names:
            tex_tuple = (
                b"\x23L2UI_CT1.tab.Tab_DF_Tab_Unselected\x00"
                b"\x21L2UI_CT1.tab.Tab_DF_Tab_Selected\x00"
                b"\x21L2UI_CT1.tab.Tab_DF_Tab_Selected\x00"
                b"\nundefined\x00"
            )
        else:
            tex_tuple = (
                b"\x1aL2UI_CT1.Button.Button_DF\x00"
                b" L2UI_CT1.Button.Button_DF_Click\x00"
                b"\x1fL2UI_CT1.Button.Button_DF_Over\x00"
                b"\nundefined\x00"
            )
        new_btn_block = (
            tex_tuple
            + struct.pack("<i", -9999)
            + bytes([len(lbl_bytes)])
            + lbl_bytes
            + struct.pack("<i", -1)  # NoHighlight = -1 (enables hover + pushed click feedback!)
        )
        widget_recs[idx] = rec[:tpos] + new_btn_block + rec[tail_pos + 19 :]

    # 5. Extract BoardWnd's Tab_DF_bg_line (TabTex3) & Tab_DF_bg (backgroundtex) + ItemDescWnd's HtmlViewer
    board_off = xdat.find(b"\x09BoardWnd\x00")
    tex3_off = xdat.find(b"\x08Texture\x00\x08TabTex3\x00", board_off, board_off + 3000)
    bg_off = xdat.find(b"\x08Texture\x00\x0ebackgroundtex\x00", tex3_off, board_off + 3000)
    br_off = xdat.find(b"\x0dBR_BuyingWnd\x00", bg_off, board_off + 3000)
    assert board_off != -1 and tex3_off != -1 and bg_off != -1 and br_off != -1

    itemdesc_off = xdat.find(b"\x0cItemDescWnd\x00")
    tex_start = xdat.find(b"\x08Texture\x00\x08texBack\x00", itemdesc_off, itemdesc_off + 1000)
    html_start = xdat.find(b"\x09HtmlCtrl\x00\x0bHtmlViewer\x00", tex_start, tex_start + 500)
    npc_start = xdat.find(b"\x0dNPCDialogWnd\x00", html_start, html_start + 500)
    assert tex_start != -1 and html_start != -1 and npc_start != -1

    # Top Tab Bar bottom line: L2UI_CT1.tab.Tab_DF_bg_line from x=695.0 to x=988.0 (w=293, y=31.0, h=23)
    tex_dock1 = (
        bytearray(xdat[tex3_off:bg_off])
        .replace(b"\x08TabTex3\x00", b"\x09texDock1\x00", 1)
        .replace(b"\x09BoardWnd\x00", b"\x06GMWnd\x00", 1)
    )
    t1_p = tex_dock1.find(b"\x06GMWnd\x00") + 7 + 22
    struct.pack_into("<ii", tex_dock1, t1_p + 16, 293, 23)
    struct.pack_into("<ff", tex_dock1, t1_p + 37, 695.0, 31.0)

    # Tabbed Content Frame: L2UI_CT1.tab.Tab_DF_bg at (0.0, 31.0), size=(992, 512)
    tex_back = (
        bytearray(xdat[bg_off:br_off])
        .replace(b"\x0ebackgroundtex\x00", b"\x08texBack\x00", 1)
        .replace(b"\x09BoardWnd\x00", b"\x06GMWnd\x00", 1)
    )
    t2_p = tex_back.find(b"\x06GMWnd\x00") + 7 + 22
    struct.pack_into("<ii", tex_back, t2_p + 16, 992, 512)
    struct.pack_into("<ff", tex_back, t2_p + 37, 0.0, 31.0)

    # Set HtmlViewer size=(968, 472), pos=(12.0, 58.0) inside the Tab_DF_bg content frame
    html_rec = bytearray(xdat[html_start:npc_start]).replace(b"\x0cItemDescWnd\x00", b"\x06GMWnd\x00", 1)
    h_p = html_rec.find(b"\x06GMWnd\x00") + 7 + 22
    struct.pack_into("<ii", html_rec, h_p + 16, 968, 472)
    struct.pack_into("<ff", html_rec, h_p + 37, 12.0, 58.0)

    # Assemble all 22 children of GMWnd (Tab_DF_bg and Tab_DF_bg_line + 19 original widgets + HtmlViewer)
    all_children = tex_dock1 + tex_back + b"".join(widget_recs) + html_rec
    patched_xdat = xdat[:children_start] + all_children + xdat[guide_off:]
    with open(out_path, "wb") as f:
        f.write(patched_xdat)

    print(
        f"[+] Patched system/interface.xdat ({len(xdat)} -> {len(patched_xdat)} bytes): "
        "Clean 2009-09-28 base + GMWnd v3 (992x544) with 7 CT1 Tab_DF Category Tabs + Tab_DF_bg_line + Tab_DF_bg + 3 .dat Search Buttons + Full-Width HtmlViewer (968x472)"
    )


def patch_func_in_place(data: bytearray, off: int, sz: int, new_code: bytes, new_mem_size: int):
    buf = data[off : off + sz]
    pos = 1
    for _ in range(5):
        _, pos = read_index(buf, pos)
    pos += 1
    line, textpos, old_script_sz = struct.unpack_from("<iii", buf, pos)
    code_start = pos + 12
    old_code_len = sz - code_start - 7
    assert len(new_code) == old_code_len, (
        f"Disk code length mismatch at off={off}: got {len(new_code)}, expected {old_code_len}"
    )

    # Verify with SerializeExpr simulator!
    dpos, mpos = 0, 0
    while dpos < len(new_code):
        dpos, mpos = sim_expr(new_code, dpos, mpos)
    assert dpos == len(new_code) and mpos == new_mem_size, (
        f"SerializeExpr verification failed at off={off}: dpos={dpos}/{len(new_code)}, mpos={mpos}/{new_mem_size}"
    )

    struct.pack_into("<i", data, off + pos + 8, new_mem_size)
    data[off + code_start : off + code_start + old_code_len] = new_code


def make_exec_cmd_stub(cmd_str: str, target_disk_len: int) -> tuple[bytes, int]:
    """
    Builds `ExecuteCommand(cmd_str); return;` padded with `0x0B` (`EX_Nothing`) to `target_disk_len`.
    Returns `(new_code_bytes, new_mem_size)`.
    """
    cmd_bytes = cmd_str.encode("latin1") + b"\x00"
    stmt = b"\x1c\xfa\x02\x1f" + cmd_bytes + b"\x16\x04\x0b"
    pad_len = target_disk_len - len(stmt)
    assert pad_len >= 0, f"Command {cmd_str!r} ({len(stmt)} B) exceeds target_disk_len={target_disk_len}"
    code = stmt + (b"\x0b" * pad_len)
    # ExecuteCommand compact index \xfa\x02 is 2 bytes on disk and 4 bytes in memory (+2 delta)
    return code, target_disk_len + 2


def patch_interface_u():
    orig_path = os.path.join(BACKUP_DIR, "interface.u.orig")
    out_path = os.path.join(CLIENT_SYS_DIR, "interface.u")
    with open(orig_path, "rb") as f:
        raw = f.read()

    # Guard: Must be clean 2009-09-28 Interface.u (4,061,011 bytes)
    assert len(raw) == 4061011, f"Expected clean 2009-09-28 interface.u.orig (4061011 B), got {len(raw)} B"

    hdr = raw[:28]
    data = bytearray(b ^ 0xAC for b in raw[28:])

    # 1. GMFindTreeWnd.ShowSkillList (#10184, off=467201, sz=88): bSummon = false (0x28) -> true (0x27)
    patch_func_in_place(
        data,
        467201,
        88,
        bytes.fromhex("070e007a0069671f0016040b19015e0c0600001c95161b52180069671619015e0c0600001ca01619015e0c0600001ce90116142d015e2c27040b"),
        80,
    )

    # 2. GMFindTreeWnd.FindAllSkill (#10231, off=474923, sz=287):
    #    record.LVDataList[1].szData = string(cID.ClassID + 2000000); + 7x EX_Nothing (0x0B)
    patch_func_in_place(
        data,
        474923,
        287,
        bytes.fromhex(
            "070e007a0078671f0016040b4101591425370159140f00668d01250f37369b005b1c2c021b4203160f005168ee0078671f20001f0028160f00542c1220d4020600081b550716075f011b450100542c160f007f481220d4020c00001b500300542c26160f004868ee007f481f20001f0028160747019b7e004868005168161dffffffff16142d36f9051025369b005b1c270f36a21025369b005b1c007f480f36e7031025369b005b1c0157140f36a21026369b005b1c39539236c10300542c1d80841e00160b0b0b0b0b0b0b1220df012000001b4c017001621d1f2e4c69737446696e64576e640016005b1c160f00542c1220d4020600081b5f0716065e00040b"
        ),
        353,
    )

    # 3. GMWnd.HandleShowGMWnd (#12109, off=780057, sz=78):
    #    Replace m_hGMwnd.SetFocus() with ExecuteCommand("//gmw")
    patch_func_in_place(
        data,
        780057,
        78,
        bytes.fromhex("0724001901f6010600041cb7161901f6010600001c95160640001901f6010600001ca0161cfa021f2f2f676d770016040b"),
        66,
    )

    # 4. GMWnd.HandleTargetUpdate (#12112, off=780213, sz=144):
    #    Place 04 0b (return;) right after m_TargetID = 0 so untargeting does not clear EditBox
    patch_func_in_place(
        data,
        780213,
        144,
        bytes.fromhex("0f0062281220d8050600041b7a0c160726009a006228014b3816040b074b009600622826160f014b3825040b190165040800001ccf021f00161b5803005d4516078d0082970062282516180f00f22d36d607005d45281616190165041000001ccf0236e403005d45160f014b38006228040b"),
        154,
    )

    # 5. GMWnd.OnClickButton (#12120, off=782198, sz=476, code_len=445):
    #    Route MoveButton ("Instances") to OnClickInfoButton (1b 4b 17 16 -> 1b 44 17 16)
    #    so it has 45 disk bytes available for ExecuteCommand("//gmw instances").
    orig_onclick_btn = bytes(data[782198 + 24 : 782198 + 476 - 7])
    assert len(orig_onclick_btn) == 445
    patched_onclick_btn = orig_onclick_btn.replace(
        b"\x1fMoveButton\x00\x1b\x4b\x17\x16",
        b"\x1fMoveButton\x00\x1b\x44\x17\x16",
        1,
    )
    assert patched_onclick_btn != orig_onclick_btn
    patch_func_in_place(data, 782198, 476, patched_onclick_btn, 480)

    # 6. Top Category Tab 1 — GMWnd.OnClickChangeServerButton (#12192, off=792065, sz=94, code_len=64):
    #    ExecuteCommand("//gmw home"); return;
    code, msz = make_exec_cmd_stub("//gmw home", 64)
    patch_func_in_place(data, 792065, 94, code, msz)

    # 7. Top Category Tab 2 — GMWnd.OnClickPetitionButton (#12173, off=791198, sz=83, code_len=53):
    #    ExecuteCommand("//gmw support"); return;
    code, msz = make_exec_cmd_stub("//gmw support", 53)
    patch_func_in_place(data, 791198, 83, code, msz)

    # 8. Top Category Tab 3 — GMWnd.OnClickTeleButton (#12126, off=783104, sz=80, code_len=50):
    #    ExecuteCommand("//gmw world"); return;
    code, msz = make_exec_cmd_stub("//gmw world", 50)
    patch_func_in_place(data, 783104, 80, code, msz)

    # 9. Top Category Tab 4 — GMWnd.OnClickInfoButton (#12163, off=790206, sz=75, code_len=45, routed from MoveButton):
    #    ExecuteCommand("//gmw instances"); return;
    code, msz = make_exec_cmd_stub("//gmw instances", 45)
    patch_func_in_place(data, 790206, 75, code, msz)

    # 9b. GMWnd.OnClickMoveButton (#12128, off=783308, sz=51, code_len=22):
    #     ExecuteCommand("//gmw instance"); return; (exact 22-byte fallback)
    code, msz = make_exec_cmd_stub("//gmw instance", 22)
    patch_func_in_place(data, 783308, 51, code, msz)

    # 10. Top Category Tab 5 — GMWnd.OnClickForcePetitionButton (#12191, off=791975, sz=90, code_len=60):
    #     ExecuteCommand("//gmw events"); return;
    code, msz = make_exec_cmd_stub("//gmw events", 60)
    patch_func_in_place(data, 791975, 90, code, msz)

    # 11. Top Category Tab 6 — GMWnd.OnClickRecallButton (#12129, off=783359, sz=50, code_len=21):
    #     ExecuteCommand("//gmw economy"); return;
    code, msz = make_exec_cmd_stub("//gmw economy", 21)
    patch_func_in_place(data, 783359, 50, code, msz)

    # 12. Top Category Tab 7 — GMWnd.OnClickSendHomeButton (#12174, off=791281, sz=50, code_len=21):
    #     ExecuteCommand("//gmw server"); return;
    code, msz = make_exec_cmd_stub("//gmw server", 21)
    patch_func_in_place(data, 791281, 50, code, msz)

    # 13. ItemDescWnd.OnLoad (#12259, off=800138, sz=125):
    #     OnRegisterEvent(); m_hItemDescWnd = GetWindowHandle("GMWnd"); m_hHtmlViewer = GetHtmlHandle("GMWnd.HtmlViewer"); return;
    patch_func_in_place(
        data,
        800138,
        125,
        bytes.fromhex("1b05160f0153281caa1f474d576e6400160f0178461cd2051f474d576e642e48746d6c5669657765720016040b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b"),
        108,
    )

    # 14. ItemDescWnd.OnEvent (#12269, off=801345, sz=110):
    #     if (!IsBuilderPC()) return;
    #     switch(Event_ID) {
    #       case EV_ItemDescWndShow: m_hItemDescWnd.ShowWindow(); m_hItemDescWnd.SetFocus(); break;
    #       case EV_ItemDescWndLoadHtmlFromString: HandleLoadHtmlFromString(param); break;
    #     }
    patch_func_in_place(
        data,
        801345,
        110,
        bytes.fromhex("070d00811b77031616040b0504004d86010a3d001de40c0000190153280600001ca016190153280600001ce901160656000a53001dee0c00001b7f04007772160656000affff040b0b0b0b0b0b0b0b"),
        95,
    )

    out_raw = hdr + bytes(b ^ 0xAC for b in data)
    assert len(out_raw) == len(raw) == 4061011, "interface.u file size changed!"
    with open(out_path, "wb") as f:
        f.write(out_raw)

    print(
        f"[+] Patched system/interface.u ({len(out_raw)} bytes, 0-byte size delta on clean 2009-09-28 base): "
        "All 15 UnrealScript functions verified via SerializeExpr!"
    )


if __name__ == "__main__":
    patch_interface_xdat()
    patch_interface_u()
