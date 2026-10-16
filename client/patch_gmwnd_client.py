#!/usr/bin/env python3
"""
Lineage 2 Gracia Epilogue (CT2.4) — Native C++ GMWnd (Alt+G) Master Console Binary Patcher & Verifier

Patches:
  1. system/interface.xdat:
     - Expands GMWnd from 350x355 to 992x544 px.
     - Re-arranges the 19 native C++ controls (EditBox, 6 Graphical Player Inspector buttons,
       3 client-side .dat Search Tree buttons, and Quick Action buttons) into a 2-column 105x26
       left control dock (x=10..225), preserving full 105px button widths so no English text clips.
     - Updates ChangeServerButton label to sysstring[415] ('Administrator').
     - Appends GMWnd.texBack (GroupBox_DF, 752x500 @ x=232, y=34) and GMWnd.HtmlViewer
       (HtmlCtrl / NCHtmlViewer, 744x492 @ x=236, y=38), incrementing GMWnd child count 19 -> 21.

  2. system/interface.u (100% zero-disk-length-delta in-place UnrealScript bytecode patches):
     - GMFindTreeWnd.ShowSkillList (#13529): Enables bSummon = true (0x28 -> 0x27).
     - GMFindTreeWnd.FindAllSkill  (#13526): Offsets skill IDs by +2,000,000 so //summon
       distinguishes Items (<1,000,000), NPCs (1,000,000..1,999,999), and Skills (>=2,000,000).
     - GMWnd.HandleTargetUpdate    (#12137): Returns on untarget (m_nowTargetID < 1) without
       wiping text typed into GMWnd.EditBox.
     - GMWnd.OnClickChangeServerButton (#12081): Executes ExecuteCommand("//admin").
     - GMWnd.HandleShowGMWnd       (#12139): Executes ExecuteCommand("//gmw") when opening
       GMWnd (Alt+G) so GMWnd.HtmlViewer immediately loads the live RBAC Admin Console.
     - ItemDescWnd.OnLoad (#12029) & ItemDescWnd.OnEvent (#12028): Binds m_hItemDescWnd =
       GetWindowHandle("GMWnd") and m_hHtmlViewer = GetHtmlHandle("GMWnd.HtmlViewer") gated by
       if (!IsBuilderPC()) return; so server NpcHtmlMessage(0, 1, html) renders directly inside
       GMWnd.HtmlViewer with zero PageLock and zero popup conflict.
"""

import os
import struct
import sys

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
    # 19 original widgets + 3 left-dock GroupBox_DF backgrounds + 2 extra section TextBoxes + texBack + HtmlViewer = 26
    struct.pack_into("<i", xdat, cc_off, 26)

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

    # 3-Section Left Native C++ Dock Layout (x=8..228) per AGENT_SYNC_AND_RECONCILIATION.md §4.4:
    #   Section 1 (y=34..162): Target / Quick Console
    #   Section 2 (y=166..240): Client .dat Search (Summon)
    #   Section 3 (y=244..368): Legacy NCSoft GM Inspector & Recall
    btn_spec = {
        # Section 1: Primary Console & Target Quick Actions (w=102, h=24)
        "ChangeServerButton":  (13.0,  78.0, 102, 24, "Admin Home"),
        "PetitionButton":      (119.0, 78.0, 102, 24, "Player Panel"),
        "ForcePetitionButton": (13.0, 105.0, 102, 24, "Legacy Tools"),
        "InfoButton":          (119.0, 105.0, 102, 24, "Target Debug"),
        "TeleButton":          (13.0, 132.0, 102, 24, "Teleport To"),
        "MoveButton":          (119.0, 132.0, 102, 24, "Instant Move"),
        # Section 2: Client .dat Search Trees (w=102 / 208, h=23)
        "ItemListButton":      (13.0, 187.0, 102, 23, "Search Item"),
        "NPCListButton":       (119.0, 187.0, 102, 23, "Search NPC"),
        "SkillListButton":     (13.0, 212.0, 208, 23, "Search Skill (.dat)"),
        # Section 3: Compact Legacy NCSoft GM Inspector (w=102, h=22)
        "DetailStatusButton":  (13.0, 265.0, 102, 22, "View Status"),
        "InventoryButton":     (119.0, 265.0, 102, 22, "View Items"),
        "MagicSkillButton":    (13.0, 289.0, 102, 22, "View Skills"),
        "QuestButton":         (119.0, 289.0, 102, 22, "View Quests"),
        "StoreButton":         (13.0, 313.0, 102, 22, "Warehouse"),
        "ClanButton":          (119.0, 313.0, 102, 22, "View Clan"),
        "RecallButton":        (13.0, 337.0, 102, 22, "Recall Chr"),
        "SendHomeButton":      (119.0, 337.0, 102, 22, "Send Home"),
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

        if idx == 0:  # EditBox (full-width inside Section 1)
            struct.pack_into("<ii", xdat, ap + 16, 208, 20)
            struct.pack_into("<ff", xdat, fp, 14.0, 54.0)
        elif idx == 18:  # TextBox (Section 1 header)
            struct.pack_into("<ii", xdat, ap + 16, 200, 14)
            struct.pack_into("<ff", xdat, fp, 14.0, 38.0)
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

    # Upgrade all 17 Button records with standard interactive Button_DF / Button_DF_Click / Button_DF_Over
    # textures, NoHighlight = -1, and custom literal English labels (ButtonName = -9999)
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
        # Verify original trailing bytes: <int sys_id> + \nundefined\x00 + \x01\x00\x00\x00 (NoHighlight=1)
        assert rec[tail_pos + 4 : tail_pos + 19] == b"\nundefined\x00\x01\x00\x00\x00", (
            f"Unexpected Button tail in {bname}"
        )
        lbl_bytes = label.encode("latin1") + b"\x00"
        new_btn_block = (
            b"\x1aL2UI_CT1.Button.Button_DF\x00"
            b" L2UI_CT1.Button.Button_DF_Click\x00"
            b"\x1fL2UI_CT1.Button.Button_DF_Over\x00"
            b"\nundefined\x00"
            + struct.pack("<i", -9999)
            + bytes([len(lbl_bytes)])
            + lbl_bytes
            + struct.pack("<i", -1)  # NoHighlight = -1 (enables hover + pushed click feedback!)
        )
        widget_recs[idx] = rec[:tpos] + new_btn_block + rec[tail_pos + 19 :]

    # Helper to build a customized TextBox record cloned from Widget 18
    tb_template = bytes(widget_recs[18])

    def build_textbox(wname: str, text: str, x: float, y: float, w: int, h: int, rgba: bytes) -> bytearray:
        rec = bytearray(tb_template)
        # Replace second \x08TextBox\x00 (widget name) with wname
        wname_b = wname.encode("latin1") + b"\x00"
        rec = bytearray(b"\x08TextBox\x00" + bytes([len(wname_b)]) + wname_b + rec[18:])
        t_p = rec.find(b"\x06GMWnd\x00") + 7 + 22
        struct.pack_into("<ii", rec, t_p + 16, w, h)
        struct.pack_into("<ff", rec, t_p + 37, x, y)
        # Replace trailing \nundefined\x00 (Text) and sysstring ID (50 -> -9999) + RGBA color
        marker = b"\xf1\xd8\xff\xff\xf1\xd8\xff\xff\xf1\xd8\xff\xff\xf1\xd8\xff\xff\nundefined\x00"
        mpos = rec.find(marker)
        assert mpos != -1, "Could not locate TextBox text marker"
        txt_pos = mpos + 16
        txt_b = text.encode("latin1") + b"\x00"
        after_txt = bytearray(rec[txt_pos + 11 :])  # after \nundefined\x00
        # sysstring ID is at offset 23 inside after_txt (12 bytes zeros/ff + 11 bytes \nundefined\x00)
        struct.pack_into("<i", after_txt, 23, -9999)
        after_txt[31:35] = rgba
        return rec[:txt_pos] + bytes([len(txt_b)]) + txt_b + after_txt

    widget_recs[18] = build_textbox(
        "TextBox", "1. Target & Console", 14.0, 38.0, 200, 14, b"\xff\xd7\x66\xff"
    )
    txt_sec_search = build_textbox(
        "txtSecSearch", "2. Client .dat Search", 14.0, 170.0, 200, 14, b"\xff\xd7\x66\xff"
    )
    txt_sec_legacy = build_textbox(
        "txtSecLegacy", "3. Legacy GM Inspector", 14.0, 248.0, 200, 14, b"\xa8\xc4\xe0\xff"
    )

    # 5. Extract ItemDescWnd's 2 children (texBack and HtmlViewer) from clean 2009 orig_xdat [434475..434870]
    itemdesc_off = xdat.find(b"\x0cItemDescWnd\x00")
    tex_start = xdat.find(b"\x08Texture\x00\x08texBack\x00", itemdesc_off, itemdesc_off + 1000)
    html_start = xdat.find(b"\x09HtmlCtrl\x00\x0bHtmlViewer\x00", tex_start, tex_start + 500)
    npc_start = xdat.find(b"\x0dNPCDialogWnd\x00", html_start, html_start + 500)
    assert tex_start != -1 and html_start != -1 and npc_start != -1

    tex_template = bytearray(xdat[tex_start:html_start]).replace(b"\x0cItemDescWnd\x00", b"\x06GMWnd\x00", 1)
    html_rec = bytearray(xdat[html_start:npc_start]).replace(b"\x0cItemDescWnd\x00", b"\x06GMWnd\x00", 1)

    def build_groupbox(wname: str, x: float, y: float, w: int, h: int) -> bytearray:
        rec = bytearray(tex_template)
        wname_b = wname.encode("latin1") + b"\x00"
        rec = bytearray(b"\x08Texture\x00" + bytes([len(wname_b)]) + wname_b + rec[18:])
        t_p = rec.find(b"\x06GMWnd\x00") + 7 + 22
        struct.pack_into("<ii", rec, t_p + 16, w, h)
        struct.pack_into("<ff", rec, t_p + 37, x, y)
        return rec

    tex_dock1 = build_groupbox("texDock1", 8.0, 34.0, 220, 128)
    tex_dock2 = build_groupbox("texDock2", 8.0, 166.0, 220, 74)
    tex_dock3 = build_groupbox("texDock3", 8.0, 244.0, 220, 124)
    tex_back = build_groupbox("texBack", 232.0, 34.0, 752, 500)

    # Set HtmlViewer size=(744, 492), pos=(236.0, 38.0)
    h_p = html_rec.find(b"\x06GMWnd\x00") + 7 + 22
    struct.pack_into("<ii", html_rec, h_p + 16, 744, 492)
    struct.pack_into("<ff", html_rec, h_p + 37, 236.0, 38.0)

    # Assemble all 26 children of GMWnd (backgrounds first so AlwaysOnBack GroupBoxes sit underneath controls)
    all_children = (
        tex_dock1
        + tex_dock2
        + tex_dock3
        + tex_back
        + b"".join(widget_recs)
        + txt_sec_search
        + txt_sec_legacy
        + html_rec
    )
    patched_xdat = xdat[:children_start] + all_children + xdat[guide_off:]
    with open(out_path, "wb") as f:
        f.write(patched_xdat)

    print(
        f"[+] Patched system/interface.xdat ({len(xdat)} -> {len(patched_xdat)} bytes): "
        "Clean 2009-09-28 base + GMWnd v2 (992x544) with 3-section left dock, Button_DF feedback, Legacy group + HtmlViewer"
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
            "070e007a0078671f0016040b4101591425370159140f00668d01250f37369b005b1c2c021b4203160f005168ee0078671f20001f0028160f00542c1220d4020600081b550716075f011b450100542c160f007f481220d4020c00001b500300542c26160f004868ee007f481f20001f0028160747019b7e004868005168161dffffffff16142d36f9051025369b005b1c270f36a21025369b005b1c007f480f36e7031025369b005b1c0157140f36a21026369b005b1c39539236c10300542c1d80841e00160b0b0b0b0b0b0b1220df012000001b4c017001621d1f2e4c6973744696e64576e640016005b1c160f00542c1220d4020600081b5f0716065e00040b"
            if False else
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

    # 5. GMWnd.OnClickChangeServerButton (#12192, off=792065, sz=94):
    #    ExecuteCommand("//admin"); return; + 49x EX_Nothing (0x0B)
    off, sz = 792065, 94
    stmt_admin = bytes.fromhex("1cfa021f2f2f61646d696e0016040b")  # 15 disk bytes, 17 mem bytes
    code_12192 = stmt_admin + (b"\x0b" * (64 - len(stmt_admin)))
    patch_func_in_place(data, off, sz, code_12192, 66)

    # 6. GMWnd.OnClickPetitionButton (#12173, off=791198, sz=83):
    #    Repurpose obsolete Petition button into "Player Panel":
    #    ExecuteCommand("//gmw player" @ m_hEditBox.GetString()); return; + 20x EX_Nothing (0x0B)
    off, sz = 791198, 83
    stmt_peti = (
        bytes.fromhex("1cfa02a8")
        + b"\x1f//gmw player\x00"
        + bytes.fromhex("190165040600001cf901161616040b")
    )
    pad_12173 = 53 - len(stmt_peti)
    code_12173 = stmt_peti + (b"\x0b" * pad_12173)
    patch_func_in_place(data, off, sz, code_12173, 59)

    # 7. GMWnd.OnClickForcePetitionButton (#12191, off=791975, sz=90):
    #    Repurpose obsolete ForcePetition button into "Legacy Tools":
    #    ExecuteCommand("//gmw legacy"); return; + 40x EX_Nothing (0x0B)
    off, sz = 791975, 90
    stmt_fpeti = bytes.fromhex("1cfa02") + b"\x1f//gmw legacy\x00" + bytes.fromhex("16040b")
    pad_12191 = 60 - len(stmt_fpeti)
    code_12191 = stmt_fpeti + (b"\x0b" * pad_12191)
    patch_func_in_place(data, off, sz, code_12191, 62)

    # 8. ItemDescWnd.OnLoad (#12259, off=800138, sz=125):
    #    OnRegisterEvent(); m_hItemDescWnd = GetWindowHandle("GMWnd"); m_hHtmlViewer = GetHtmlHandle("GMWnd.HtmlViewer"); return;
    patch_func_in_place(
        data,
        800138,
        125,
        bytes.fromhex("1b05160f0153281caa1f474d576e6400160f0178461cd2051f474d576e642e48746d6c5669657765720016040b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b"),
        108,
    )

    # 9. ItemDescWnd.OnEvent (#12269, off=801345, sz=110):
    #    if (!IsBuilderPC()) return;
    #    switch(Event_ID) {
    #      case EV_ItemDescWndShow: m_hItemDescWnd.ShowWindow(); m_hItemDescWnd.SetFocus(); break;
    #      case EV_ItemDescWndLoadHtmlFromString: HandleLoadHtmlFromString(param); break;
    #    }
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
        "All 9 UnrealScript functions verified via SerializeExpr!"
    )


if __name__ == "__main__":
    patch_interface_xdat()
    patch_interface_u()

