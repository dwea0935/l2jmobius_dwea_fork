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

    # 1. Verify GMWnd header @ 399160 and GuideWnd @ 404835
    assert xdat[399160:399167] == b"\x06GMWnd\x00", "GMWnd signature mismatch"
    assert xdat[404835:404845] == b"\x09GuideWnd\x00", "GuideWnd boundary mismatch"

    # 2. Expand GMWnd dimensions at offset 399225 from (350, 355) to (992, 544)
    w_orig, h_orig = struct.unpack_from("<ii", xdat, 399225)
    assert (w_orig, h_orig) == (350, 355), f"Unexpected GMWnd size: {(w_orig, h_orig)}"
    struct.pack_into("<ii", xdat, 399225, 992, 544)

    # 3. Increment GMWnd child count right before first child (\x08EditBox\x00) from 19 to 21
    cc_off = xdat.find(b"\x13\x00\x00\x00\x08EditBox\x00", 399160, 400000)
    assert cc_off != -1, "Could not locate GMWnd child count before EditBox"
    struct.pack_into("<i", xdat, cc_off, 21)

    # 4. Reposition the 19 existing native C++ controls in GMWnd into a 2-column left dock (x=10..225)
    #    Collect all 19 widget parent-name offsets inside [399160..404835]
    widget_offsets = []
    pos = 399167
    while True:
        p = xdat.find(b"\x06GMWnd\x00", pos, 404835)
        if p == -1:
            break
        if xdat[p + 7 : p + 29] == b"\x0aundefined\x00\x0aundefined\x00":
            widget_offsets.append(p)
        pos = p + 7
    assert len(widget_offsets) == 19, f"Expected 19 widgets, found {len(widget_offsets)}"

    # Desired absolute (x, y, w, h) inside GMWnd for the 19 widgets:
    #   Widget 0  (EditBox):  x=48, y=40, w=177, h=20
    #   Widget 18 (TextBox):  x=12, y=43, w=24,  h=14
    #   Widgets 1..17 (17 Buttons in 2 columns of w=105, h=26, Col1 x=10, Col2 x=120, y=68 + row*30):
    btn_Order_names = [
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
    abs_coords = {}
    for i, bname in enumerate(btn_Order_names):
        col = i % 2
        row = i // 2
        bx = 10.0 if col == 0 else 120.0
        by = 68.0 + row * 30.0
        abs_coords[bname] = (bx, by, 105, 26)

    for idx, p in enumerate(widget_offsets):
        ap = p + 7 + 22  # right after \x06GMWnd\x00\x0aundefined\x00\x0aundefined\x00
        anch_len = xdat[ap + 36]
        if anch_len > 0:
            anch_wnd = xdat[ap + 37 : ap + 37 + anch_len - 1].decode("latin1")
            fp = ap + 37 + anch_len
        else:
            anch_wnd = ""
            fp = ap + 37

        if idx == 0:  # EditBox
            struct.pack_into("<ii", xdat, ap + 16, 177, 20)
            struct.pack_into("<ff", xdat, fp, 48.0, 40.0)
        elif idx == 18:  # TextBox ("Name")
            struct.pack_into("<ff", xdat, fp, 12.0, 43.0)
        else:  # Button 1..17
            bname = btn_Order_names[idx - 1]
            tx, ty, tw, th = abs_coords[bname]
            struct.pack_into("<ii", xdat, ap + 16, tw, th)
            # Set RelativePoint=1 (TopLeft), AnchorPoint=1 (TopLeft)
            struct.pack_into("<iii", xdat, ap + 24, 1, 1, 1)
            if anch_wnd == "":
                rx, ry = tx, ty
            else:
                px, py, _, _ = abs_coords[anch_wnd]
                rx, ry = tx - px, ty - py
            struct.pack_into("<ff", xdat, fp, rx, ry)

            # For ChangeServerButton (idx == 17), change label sysstring ID from 1275 to 415 ('Administrator')
            if idx == 17:
                next_p = widget_offsets[18]
                # Search for 1275 (fb 04 00 00) inside ChangeServerButton record
                lbl_pos = xdat.find(struct.pack("<i", 1275), p, next_p)
                assert lbl_pos != -1, "Could not find 1275 sysstring ID in ChangeServerButton"
                struct.pack_into("<i", xdat, lbl_pos, 415)

    # 5. Extract ItemDescWnd's 2 children (texBack and HtmlViewer) from orig_xdat [444075..444471]
    tex_start = xdat.find(b"\x08Texture\x00\x08texBack\x00", 443626, 444471)
    html_start = xdat.find(b"\x09HtmlCtrl\x00\x0bHtmlViewer\x00", 443626, 444471)
    npc_start = xdat.find(b"\x0dNPCDialogWnd\x00", 443626, 444500)
    assert tex_start != -1 and html_start != -1 and npc_start != -1

    tex_rec = bytearray(xdat[tex_start:html_start])
    html_rec = bytearray(xdat[html_start:npc_start])

    # Replace parent window name \x0cItemDescWnd\x00 with \x06GMWnd\x00
    tex_rec = tex_rec.replace(b"\x0cItemDescWnd\x00", b"\x06GMWnd\x00", 1)
    html_rec = html_rec.replace(b"\x0cItemDescWnd\x00", b"\x06GMWnd\x00", 1)

    # Set texBack size=(752, 500), pos=(232.0, 34.0)
    t_p = tex_rec.find(b"\x06GMWnd\x00") + 7 + 22
    struct.pack_into("<ii", tex_rec, t_p + 16, 752, 500)
    struct.pack_into("<ff", tex_rec, t_p + 37, 232.0, 34.0)

    # Set HtmlViewer size=(744, 492), pos=(236.0, 38.0)
    h_p = html_rec.find(b"\x06GMWnd\x00") + 7 + 22
    struct.pack_into("<ii", html_rec, h_p + 16, 744, 492)
    struct.pack_into("<ff", html_rec, h_p + 37, 236.0, 38.0)

    # Insert tex_rec + html_rec right before GuideWnd @ 404835
    patched_xdat = xdat[:404835] + tex_rec + html_rec + xdat[404835:]
    with open(out_path, "wb") as f:
        f.write(patched_xdat)

    print(
        f"[+] Patched system/interface.xdat ({len(xdat)} -> {len(patched_xdat)} bytes): "
        "GMWnd expanded to 992x544 with 2-col C++ dock + GMWnd.HtmlViewer (744x492)"
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

    hdr = raw[:28]
    data = bytearray(b ^ 0xAC for b in raw[28:])

    # 1. GMFindTreeWnd.ShowSkillList (#13529, off=2542899, sz=88): bSummon = false (0x28) -> true (0x27)
    off, sz = 2542899, 88
    code_13529 = bytearray(data[off + 23 : off + sz - 7])
    assert code_13529[-3:] == b"\x28\x04\x0b"
    code_13529[-3] = 0x27
    patch_func_in_place(data, off, sz, bytes(code_13529), 80)

    # 2. GMFindTreeWnd.FindAllSkill (#13526, off=2542436, sz=287):
    #    Replace record.LVDataList[1].szData = string(cID.ClassID); record.LVDataList[1].TextColor = Gold;
    #    with    record.LVDataList[1].szData = string(cID.ClassID + 2000000); + 7x EX_Nothing (0x0B)
    off, sz = 2542436, 287
    code_13526 = bytearray(data[off + 23 : off + sz - 7])
    old_s12 = bytes.fromhex("0f36a21026369b00531b395336ff02005f280f36d3031026369b00531b016213")
    new_s12 = (
        bytes.fromhex("0f36a21026369b00531b39539236ff02005f281d80841e0016")
        + (b"\x0b" * 7)
    )
    assert len(old_s12) == len(new_s12) == 32
    assert old_s12 in code_13526
    code_13526 = code_13526.replace(old_s12, new_s12, 1)
    # Adjust the 2 forward jump targets (0x0150 -> 0x0147, 0x0168 -> 0x015f) and MemScriptSize (362 -> 353)
    code_13526 = code_13526.replace(b"\x07\x68\x01", b"\x07\x5f\x01", 1)
    code_13526 = code_13526.replace(b"\x07\x50\x01", b"\x07\x47\x01", 1)
    patch_func_in_place(data, off, sz, bytes(code_13526), 353)

    # 3. GMWnd.HandleTargetUpdate (#12137, off=762030, sz=144):
    #    Place 04 0b (return;) right after m_TargetID = 0 (0f 01 6b34 25) so untargeting does not clear EditBox
    off, sz = 762030, 144
    code_12137 = bytearray(data[off + 23 : off + sz - 7])
    old_seq = bytes.fromhex("0f016b3425190166040800001ccf021f0016040b")
    new_seq = bytes.fromhex("0f016b3425040b190166040800001ccf021f0016")
    assert old_seq in code_12137
    code_12137 = code_12137.replace(old_seq, new_seq, 1)
    patch_func_in_place(data, off, sz, bytes(code_12137), 154)

    # 4. GMWnd.OnClickChangeServerButton (#12081, off=752175, sz=94):
    #    ExecuteCommand("//admin"); return; + 49x EX_Nothing (0x0B)
    off, sz = 752175, 94
    stmt_admin = bytes.fromhex("1cfb021f2f2f61646d696e0016040b")  # 15 disk bytes, 17 mem bytes
    code_12081 = stmt_admin + (b"\x0b" * (64 - len(stmt_admin)))
    patch_func_in_place(data, off, sz, code_12081, 17 + (64 - len(stmt_admin)))

    # 5. GMWnd.HandleShowGMWnd (#12139, off=762244, sz=78):
    #    Replace m_hGMwnd.SetFocus() (11 disk bytes) with ExecuteCommand("//gmw") (11 disk bytes)
    off, sz = 762244, 78
    code_12139 = bytearray(data[off + 22 : off + sz - 7])
    old_focus = bytes.fromhex("0642001901f9010600001ca01619017e470600001ceb0116040b")
    new_gmw = bytes.fromhex("0640001901f9010600001ca0161cfb021f2f2f676d770016040b")
    assert len(old_focus) == len(new_gmw)
    assert old_focus in code_12139
    code_12139 = code_12139.replace(old_focus, new_gmw, 1)
    patch_func_in_place(data, off, sz, bytes(code_12139), 66)

    # 6. ItemDescWnd.OnLoad (#12029, off=743910, sz=125):
    #    OnRegisterEvent(); m_hItemDescWnd = GetWindowHandle("GMWnd"); m_hHtmlViewer = GetHtmlHandle("GMWnd.HtmlViewer"); return;
    off, sz = 743910, 125
    stmt_onload = (
        bytes.fromhex("1b0516")  # OnRegisterEvent(); (3 disk, 6 mem)
        + bytes.fromhex("0f016c2b1ca81f474d576e640016")  # m_hItemDescWnd = GetWindowHandle("GMWnd"); (14 disk, 19 mem)
        + bytes.fromhex("0f016c491cc9051f474d576e642e48746d6c5669657765720016")  # m_hHtmlViewer = GetHtmlHandle("GMWnd.HtmlViewer"); (26 disk, 30 mem)
        + bytes.fromhex("040b")  # return; (2 disk, 2 mem)
    )
    pad_len = 96 - len(stmt_onload)
    code_12029 = stmt_onload + (b"\x0b" * pad_len)
    patch_func_in_place(data, off, sz, code_12029, 57 + pad_len)

    # 7. ItemDescWnd.OnEvent (#12028, off=743800, sz=110):
    #    if (!IsBuilderPC()) return;
    #    switch(Event_ID) {
    #      case EV_ItemDescWndShow: m_hItemDescWnd.ShowWindow(); m_hItemDescWnd.SetFocus(); break;
    #      case EV_ItemDescWndLoadHtmlFromString: HandleLoadHtmlFromString(param); break;
    #    }
    off, sz = 743800, 110
    stmt_onevent = (
        bytes.fromhex("070c00811b58041616040b")  # if (!IsBuilderPC()) return; (11 disk, 13 mem? Wait: let's check!)
    )
    # Wait: let's compute exact jump offsets for ItemDescWnd.OnEvent using sim_expr!
    # Prefix: 07 <u16> 81 1b 5804 16 16 04 0b
    _, prefix_mem = sim_expr(bytes.fromhex("070000811b58041616"), 0, 0)
    prefix_mem += 2  # + 04 0b (return;)
    switch_hdr = bytes.fromhex("0504006f8c01")
    _, sw_mem = sim_expr(switch_hdr, 0, prefix_mem)
    case1_body = bytes.fromhex("19016c2b0600001ca01619016c2b0600001ceb0116")
    d, c1_body_mem = 0, 0
    while d < len(case1_body):
        d, c1_body_mem = sim_expr(case1_body, d, c1_body_mem)
    case1_next_off = sw_mem + 8 + c1_body_mem + 3

    case2_body = bytes.fromhex("1b7e04007b6516")
    _, c2_body_mem = sim_expr(case2_body, 0, 0)
    case2_next_off = case1_next_off + 8 + c2_body_mem + 3
    end_switch_off = case2_next_off + 3

    built_onevent = (
        b"\x07" + struct.pack("<H", prefix_mem) + bytes.fromhex("811b58041616040b")
        + switch_hdr
        + b"\x0a" + struct.pack("<H", case1_next_off) + bytes.fromhex("1de40c0000") + case1_body + b"\x06" + struct.pack("<H", end_switch_off)
        + b"\x0a" + struct.pack("<H", case2_next_off) + bytes.fromhex("1dee0c0000") + case2_body + b"\x06" + struct.pack("<H", end_switch_off)
        + bytes.fromhex("0affff040b")
    )
    pad_12028 = 79 - len(built_onevent)
    code_12028 = built_onevent + (b"\x0b" * pad_12028)
    patch_func_in_place(data, off, sz, code_12028, end_switch_off + 2 + pad_12028)

    out_raw = hdr + bytes(b ^ 0xAC for b in data)
    assert len(out_raw) == len(raw), "interface.u file size changed!"
    with open(out_path, "wb") as f:
        f.write(out_raw)

    print(
        f"[+] Patched system/interface.u ({len(out_raw)} bytes, 0-byte size delta): "
        "All 7 UnrealScript functions verified via SerializeExpr!"
    )


if __name__ == "__main__":
    patch_interface_xdat()
    patch_interface_u()
