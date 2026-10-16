#!/usr/bin/env python3
"""
patch_nwindow_unicode.py — Applies two binary modernizations to the 2009-09-28
(Protocol 148) Lineage II Gracia Epilogue NWindow.dll (5,425,684 bytes, 0-byte size delta):

Part A — International Unicode Keyboard Input (U+00A0..U+04FF, excluding U+0300..U+036F):
1. NCEditBox::OnChar (VA 0x100d44ee..0x100d450c, RVA 0x000d44ee, file offset 0x000D38EE, 30 bytes):
   - Removes the `GLanguage == 8` (Russian) requirement that silently dropped all
     non-ASCII characters when `Language=0` in `l2.ini`.
   - Widens allowed input range from basic Russian `U+0410..U+044F` (`А..я`) to
     `U+00A0..U+04FF` (Latin-1 Supplement, Latin Extended-A/B for Polish/Czech/Slovak/
     Croatian/Slovenian/etc., IPA, Greek `U+0370..U+03FF`, and full Cyrillic `U+0400..U+04FF`
     including `Ё/ё`, Ukrainian `Є/І/Ї/Ґ`, Belarusian `Ў`, Serbian/Macedonian `Ђ/Ѓ/Ј/Љ/Њ/Ћ/Ќ/Џ`).
   - Excludes Combining Diacritical Marks `U+0300..U+036F` to prevent "Zalgo" stacking spam.
2. NCMultiEditBox::OnChar (VA 0x100a3468..0x100a347e, RVA 0x000a3468, file offset 0x000A2868, 22 bytes):
   - Identical filter update for multi-line edit boxes (Mail body, Community Board memos,
     Petitions).
3. NWndUtil::GetNativeStrLen (VA 0x101d9326..0x101d9331, RVA 0x001d9326, file offset 0x001D8726, 11 bytes):
   - Widens single-width character counting from `U+0410..U+044F` to `U+0080..U+04FF`
     so European, Greek, and Cyrillic characters count as 1 character (instead of 2 CJK
     columns) toward EditBox/Chat/Mail MaxLength limits.
4. IsObsceneWord (VA 0x10215fbb..0x10215fc6, RVA 0x00215fbb, file offset 0x002153BB, 11 bytes):
   - Removes `GLanguage == 8` gate on Cyrillic uppercase-to-lowercase normalization.

Part B — [AutoLogOn] 4-Stage One-Shot State Machine (`l2.ini` `[AutoLogOn]`):
Restores support for `[AutoLogOn]` (`IsL2AutoLogOn=true`, `L2ID=...`, `L2Passwd=...`,
`L2Slot=...`) in `l2.ini` using 100% Position-Independent Code (PIC, ASLR/relocation-safe):
- PE `.text` `Misc.VirtualSize` (file offset 0x00000218): extended from `0x002dae1c` to
  `0x002db000` (matching `SizeOfRawData`) to map `.text` slack `0x102dbe20..0x102dc000`.
- `.text` Code Cave (VA 0x102dbe20..0x102dbfec, file offset 0x002DB220..0x002DB3EC, 460 bytes):
  * Stage 1 (`NCAuthWnd::ShowWindow` @ VA 0x1014336a): On first login screen show only
    (`STATE_Done == 0`), queries `GConfig` (`*0x102dc4b4`) for `[AutoLogOn]` `IsL2AutoLogOn`,
    `L2Slot`, `L2ID`, and `L2Passwd` from `l2.ini`. If enabled and non-empty, populates the
    ID edit box, sets `STATE_Active = 1`, and calls `NConsoleWnd::RequestAuthLogin` (`0x1021a160`).
  * Stage 2 (`NConsoleWnd::AcceptLogin` @ VA 0x1024e924): When `STATE_Active == 1`, takes the
    native EULA-skip branch (`0x1024e9fa`) directly to `ServerListState` + `RequestServerList`.
  * Stage 3 (`NCServerSelectWnd::EndServerInfo` @ VA 0x101274d6): When `STATE_Active == 1`,
    advances to `STATE_Active = 2`, ensures a server row is selected, and calls
    `NCServerSelectWnd::OnLoginBtnClick` (`0x1012a7c0`).
  * Stage 4a (`NConsoleWnd::ReceiveCharacterInfo` @ VA 0x1024e4bf) &
    Stage 4b (`NConsoleWnd::AddCharacterInfo` @ VA 0x1024e406): Waits until all `numChars`
    character packets have been added to `NCLobbyWnd` (`[esi+0x1e4] >= STATE_TargetChars`),
    resets `STATE_Active = 0` (so subsequent manual Restart/Logout never loops), clamps
    `L2Slot` to `[0, numChars - 1]`, and calls `NCLobbyWnd::SelectCharacter` (`0x10146280`)
    followed by `NCLobbyWnd::GameStart` (`0x101455f0`).
- `.data` Strings & State Block (VA 0x104ba960..0x104ba9e0, file offset 0x004B9160..0x004B91E0):
  UTF-16LE config keys (`AutoLogOn`, `IsL2AutoLogOn`, `L2ID`, `L2Passwd`, `L2Slot`, `l2.ini`)
  and 4 state dwords (`STATE_Done`, `STATE_Active`, `STATE_Slot`, `STATE_TargetChars`).

Patched PE `.reloc` entries (neutralized to IMAGE_REL_BASED_ABSOLUTE = 0x0000 so PE
rebasing does not add BaseDelta to replaced instructions):
- File offset 0x004C80C2 (RVA 0x000a346a in NCMultiEditBox::OnChar): 0x346a -> 0x0000
- File offset 0x004CAC1C (RVA 0x000d44f0 in NCEditBox::OnChar):      0x34f0 -> 0x0000
- File offset 0x004E2024 (RVA 0x00215fbd in IsObsceneWord):          0x3fbd -> 0x0000
"""

import hashlib
import os
import shutil
import struct

ORIG_PATH = "Client Lineage 2 Gracia Epilogue/system/backup_encrypted/nwindow.dll.orig"
OUT_CLIENT = "Client Lineage 2 Gracia Epilogue/system/nwindow.dll"
OUT_REPO = "l2jmobius_dwea_fork/client/system/nwindow.dll"

EXPECTED_SIZE = 5425684
EXPECTED_ORIG_SHA256 = "dcd951c811002318f3164f1221f1b8c9d7466c17f3105c0b7ece49cb0e3bbb45"

# Build the 128-byte .data block at VA 0x104ba960..0x104ba9e0 (file offset 0x004B9160..0x004B91E0)
AUTOLOGIN_DATA_BLOCK = bytearray(0x80)
for ofs, text in [
    (0x00, "AutoLogOn"),
    (0x14, "IsL2AutoLogOn"),
    (0x30, "L2ID"),
    (0x3A, "L2Passwd"),
    (0x4C, "L2Slot"),
    (0x5A, "l2.ini"),
]:
    encoded = (text + "\0").encode("utf-16le")
    AUTOLOGIN_DATA_BLOCK[ofs : ofs + len(encoded)] = encoded

# 477-byte PIC code cave at VA 0x102dbe20..0x102dbffd (file offset 0x002DB220..0x002DB3FD)
AUTOLOGIN_CAVE_BYTES = bytes.fromhex(
    # GetBaseEbx (VA 0x102dbe20): returns runtime VA of BASE (0x104ba9c0) in ebx
    "e800000000"            # call 0x102dbe25
    "5b"                    # pop ebx
    "8d9b9beb1d00"          # lea ebx, [ebx + 0x1deb9b]
    "c3"                    # ret
    # CallGConfig (VA 0x102dbe2d): al = vtbl_ofs, ah = signed OFS_key, ecx = &out_var
    "8d53fa"                # lea edx, [ebx - 0x06] (L"l2.ini")
    "52"                    # push edx
    "51"                    # push ecx
    "0fbed4"                # movsx edx, ah
    "8d1413"                # lea edx, [ebx + edx]
    "52"                    # push edx
    "8d53a0"                # lea edx, [ebx - 0x60] (L"AutoLogOn")
    "52"                    # push edx
    "0fb6c0"                # movzx eax, al
    "89f9"                  # mov ecx, edi
    "8b17"                  # mov edx, [edi]
    "ff1402"                # call dword ptr [edx + eax]
    "c3"                    # ret
    # Stage1_Auth (VA 0x102dbe48): hooked at 0x1014336a in NCAuthWnd::ShowWindow
    "e82373e6ff"            # call 0x10143170 (NCAuthWnd::InitKeyCrypt)
    "53"                    # push ebx
    "e8cdffffff"            # call GetBaseEbx (0x102dbe20)
    "807b1000"              # cmp byte ptr [ebx + 0x10], 0 (STATE_Done)
    "7406"                  # je 0x102dbe5f
    "c6431400"              # mov byte ptr [ebx + 0x14], 0 (STATE_Active = 0)
    "5b"                    # pop ebx
    "c3"                    # ret
    "c6431001"              # mov byte ptr [ebx + 0x10], 1 (STATE_Done = 1)
    "55"                    # push ebp
    "89e5"                  # mov ebp, esp
    "83ec20"                # sub esp, 0x20
    "57"                    # push edi
    "31c0"                  # xor eax, eax
    "8945fc"                # mov [ebp - 0x04], eax (bAuto = 0)
    "48"                    # dec eax (eax = -1)
    "8945f8"                # mov [ebp - 0x08], eax (nSlot = -1 default: stop at lobby if empty/omitted)
    "8d4dec"                # lea ecx, [ebp - 0x14] (fstrID)
    "ff93bc1ae2ff"          # call dword ptr [ebx - 0x1de544] (FString::FString)
    "8d4de0"                # lea ecx, [ebp - 0x20] (fstrPwd)
    "ff93bc1ae2ff"          # call dword ptr [ebx - 0x1de544] (FString::FString)
    "8b83f41ae2ff"          # mov eax, [ebx - 0x1de50c] (GConfig)
    "8b38"                  # mov edi, [eax] (*GConfig)
    "8d4dfc"                # lea ecx, [ebp - 0x04] (&bAuto)
    "66b800b4"              # mov ax, 0xb400 (ah = -0x4c = L"IsL2AutoLogOn", al = 0x00 = GetBool)
    "e894ffffff"            # call CallGConfig (0x102dbe2d)
    "837dfc00"              # cmp dword ptr [ebp - 0x04], 0
    "0f8492000000"          # je 0x102dbf35 (.cleanup)
    "8d4dec"                # lea ecx, [ebp - 0x14] (check if L2Slot string is non-empty)
    "66b80cec"              # mov ax, 0xec0c (ah = -0x14 = L"L2Slot", al = 0x0c = GetString)
    "e87effffff"            # call CallGConfig (0x102dbe2d)
    "8d4dec"                # lea ecx, [ebp - 0x14]
    "ff93b81ae2ff"          # call dword ptr [ebx - 0x1de548] (FString::operator*)
    "66833800"              # cmp word ptr [eax], 0 (is L2Slot= empty?)
    "740c"                  # je 0x102dbeca (keep nSlot = -1)
    "8d4df8"                # lea ecx, [ebp - 0x08] (&nSlot)
    "66b804ec"              # mov ax, 0xec04 (ah = -0x14 = L"L2Slot", al = 0x04 = GetInt)
    "e863ffffff"            # call CallGConfig (0x102dbe2d)
    "8b45f8"                # mov eax, [ebp - 0x08]
    "894318"                # mov [ebx + 0x18], eax (STATE_Slot = nSlot)
    "8d4dec"                # lea ecx, [ebp - 0x14] (&fstrID)
    "66b80cd0"              # mov ax, 0xd00c (ah = -0x30 = L"L2ID", al = 0x0c = GetString)
    "e851ffffff"            # call CallGConfig (0x102dbe2d)
    "8d4de0"                # lea ecx, [ebp - 0x20] (&fstrPwd)
    "66b80cda"              # mov ax, 0xda0c (ah = -0x26 = L"L2Passwd", al = 0x0c = GetString)
    "e845ffffff"            # call CallGConfig (0x102dbe2d)
    "8b8ee4010000"          # mov ecx, [esi + 0x1e4] (m_pIDEditBox)
    "85c9"                  # test ecx, ecx
    "7410"                  # je 0x102dbf02
    "81c100020000"          # add ecx, 0x200
    "8d45ec"                # lea eax, [ebp - 0x14]
    "50"                    # push eax
    "ff93d81ae2ff"          # call dword ptr [ebx - 0x1de528] (FString::operator=)
    "8d4de0"                # lea ecx, [ebp - 0x20]
    "ff93b81ae2ff"          # call dword ptr [ebx - 0x1de548] (FString::operator*)
    "89c7"                  # mov edi, eax (pwd_wstr)
    "66833f00"              # cmp word ptr [edi], 0
    "7422"                  # je 0x102dbf35 (.cleanup)
    "8d4dec"                # lea ecx, [ebp - 0x14]
    "ff93b81ae2ff"          # call dword ptr [ebx - 0x1de548] (FString::operator*)
    "66833800"              # cmp word ptr [eax], 0
    "7413"                  # je 0x102dbf35 (.cleanup)
    "c6431401"              # mov byte ptr [ebx + 0x14], 1 (STATE_Active = 1)
    "6a00"                  # push 0
    "57"                    # push edi (pwd_wstr)
    "50"                    # push eax (id_wstr)
    "ffb3e83b0000"          # push dword ptr [ebx + 0x3be8] (GConsoleWnd @ 0x104be5a8)
    "e82be2f3ff"            # call 0x1021a160 (NConsoleWnd::RequestAuthLogin)
    "8d4de0"                # lea ecx, [ebp - 0x20]
    "ff93c81ae2ff"          # call dword ptr [ebx - 0x1de538] (FString::~FString)
    "8d4dec"                # lea ecx, [ebp - 0x14]
    "ff93c81ae2ff"          # call dword ptr [ebx - 0x1de538] (FString::~FString)
    "5f"                    # pop edi
    "89ec"                  # mov esp, ebp
    "5d"                    # pop ebp
    "5b"                    # pop ebx
    "c3"                    # ret
    # Stage2_SkipEula (VA 0x102dbf4d): hooked at 0x1024e924 in NConsoleWnd::AcceptLogin
    "53"                    # push ebx
    "e8cdfeffff"            # call GetBaseEbx (0x102dbe20)
    "807b1401"              # cmp byte ptr [ebx + 0x14], 1 (STATE_Active == 1?)
    "5b"                    # pop ebx
    "7503"                  # jne 0x102dbf5d
    "39db"                  # cmp ebx, ebx (ZF=1 -> jle 0x1024e9fa taken!)
    "c3"                    # ret
    "83c0ff"                # add eax, -1
    "39d8"                  # cmp eax, ebx
    "c3"                    # ret
    # Stage3_ServerSelect (VA 0x102dbf63): hooked at 0x101274d6 in NCServerSelectWnd::EndServerInfo
    "8b8238020000"          # mov eax, [edx + 0x238]
    "ffd0"                  # call eax (NCServerSelectWnd::ShowWindow)
    "53"                    # push ebx
    "e8affeffff"            # call GetBaseEbx (0x102dbe20)
    "807b1401"              # cmp byte ptr [ebx + 0x14], 1 (STATE_Active == 1?)
    "7521"                  # jne 0x102dbf98
    "c6431402"              # mov byte ptr [ebx + 0x14], 2 (STATE_Active = 2)
    "8b8ee4010000"          # mov ecx, [esi + 0x1e4] (m_pServerList)
    "83b95002000000"        # cmp dword ptr [ecx + 0x250], 0
    "7d07"                  # jge 0x102dbf91
    "83a15002000000"        # and dword ptr [ecx + 0x250], 0 (select row 0)
    "89f1"                  # mov ecx, esi
    "e828e8e4ff"            # call 0x1012a7c0 (NCServerSelectWnd::OnLoginBtnClick)
    "5b"                    # pop ebx
    "c3"                    # ret
    # Stage4a_RecvCharInfo (VA 0x102dbf9a): hooked at 0x1024e4bf in NConsoleWnd::ReceiveCharacterInfo
    "e8a1a1e6ff"            # call 0x10146140 (NCLobbyWnd::ClearCharacterList)
    "53"                    # push ebx
    "e87bfeffff"            # call GetBaseEbx (0x102dbe20)
    "807b1402"              # cmp byte ptr [ebx + 0x14], 2 (STATE_Active == 2?)
    "7514"                  # jne 0x102dbfbf
    "8b4508"                # mov eax, [ebp + 8] (numChars)
    "85c0"                  # test eax, eax
    "7e09"                  # jle 0x102dbfbb
    "89431c"                # mov [ebx + 0x1c], eax (STATE_TargetChars = numChars)
    "c6431403"              # mov byte ptr [ebx + 0x14], 3 (STATE_Active = 3)
    "5b"                    # pop ebx
    "c3"                    # ret
    "c6431400"              # mov byte ptr [ebx + 0x14], 0 (STATE_Active = 0)
    "5b"                    # pop ebx
    "c3"                    # ret
    # Stage4b_AddCharInfo (VA 0x102dbfc1): hooked at 0x1024e406 in NConsoleWnd::AddCharacterInfo
    "e8faa9e6ff"            # call 0x101469c0 (NCLobbyWnd::AddCharacter)
    "53"                    # push ebx
    "e854feffff"            # call GetBaseEbx (0x102dbe20)
    "807b1403"              # cmp byte ptr [ebx + 0x14], 3 (STATE_Active == 3?)
    "7529"                  # jne 0x102dbffb
    "8b86e4010000"          # mov eax, [esi + 0x1e4] (addedChars)
    "3b431c"                # cmp eax, [ebx + 0x1c] (STATE_TargetChars)
    "7c1e"                  # jl 0x102dbffb
    "c6431400"              # mov byte ptr [ebx + 0x14], 0 (consume one-shot auto-login!)
    "8b4b18"                # mov ecx, [ebx + 0x18] (STATE_Slot)
    "85c9"                  # test ecx, ecx
    "7c13"                  # jl 0x102dbffb (if L2Slot < 0 or empty, stop at Character Select!)
    "39c1"                  # cmp ecx, eax
    "7d0f"                  # jge 0x102dbffb (if L2Slot >= numChars, stop at Character Select!)
    "51"                    # push ecx (save slot)
    "56"                    # push esi (NCLobbyWnd*)
    "e88da2e6ff"            # call 0x10146280 (NCLobbyWnd::SelectCharacter)
    "59"                    # pop ecx (restore slot)
    "89f2"                  # mov edx, esi (NCLobbyWnd*)
    "e8f595e6ff"            # call 0x101455f0 (NCLobbyWnd::GameStart)
    "5b"                    # pop ebx
    "c3"                    # ret
)

HEADER_PATCHES = [
    {
        "name": "PE .text Misc.VirtualSize (0x002dae1c -> 0x002db000)",
        "offset": 0x00000218,
        "orig": bytes.fromhex("1cae2d00"),
        "repl": bytes.fromhex("00b02d00"),
    },
]

CODE_PATCHES = [
    {
        "name": "NCEditBox::OnChar (VA 0x100d44ee..0x100d450c)",
        "offset": 0x000D38EE,
        "rva": 0x000D44EE,
        "orig": bytes.fromhex(
            "8b0d84c32d10"      # mov ecx, DWORD PTR ds:0x102dc384
            "833908"            # cmp DWORD PTR [ecx], 0x8
            "0f8531030000"      # jne 0x100d482e
            "8d93f0fbffff"      # lea edx, [ebx - 0x410]
            "83fa3f"            # cmp edx, 0x3f
            "0f8722030000"      # ja  0x100d482e
        ),
        "repl": bytes.fromhex(
            "0fb7c3"            # movzx eax, bx
            "80ec03"            # sub ah, 0x03          (ax = bx - 0x0300 mod 65536)
            "83f86f"            # cmp eax, 0x6f         (in U+0300..U+036F combining diacritics?)
            "0f8631030000"      # jbe 0x100d482e        (reject combining diacritics / Zalgo)
            "66056002"          # add ax, 0x0260        (ax = bx - 0x00A0 mod 65536)
            "663d5f04"          # cmp ax, 0x045f        (in U+00A0..U+04FF?)
            "0f8723030000"      # ja  0x100d482e        (reject if outside U+00A0..U+04FF)
            "90"                # nop
        ),
    },
    {
        "name": "NCMultiEditBox::OnChar (VA 0x100a3468..0x100a347e)",
        "offset": 0x000A2868,
        "rva": 0x000A3468,
        "orig": bytes.fromhex(
            "8b0d84c32d10"      # mov ecx, DWORD PTR ds:0x102dc384
            "833908"            # cmp DWORD PTR [ecx], 0x8
            "7527"              # jne 0x100a349a
            "8d96f0fbffff"      # lea edx, [esi - 0x410]
            "83fa3f"            # cmp edx, 0x3f
            "771c"              # ja  0x100a349a
        ),
        "repl": bytes.fromhex(
            "0fb7c6"            # movzx eax, si
            "80ec03"            # sub ah, 0x03          (ax = si - 0x0300 mod 65536)
            "83f86f"            # cmp eax, 0x6f         (in U+0300..U+036F combining diacritics?)
            "7627"              # jbe 0x100a349a        (reject combining diacritics / Zalgo)
            "66056002"          # add ax, 0x0260        (ax = si - 0x00A0 mod 65536)
            "663d5f04"          # cmp ax, 0x045f        (in U+00A0..U+04FF?)
            "771d"              # ja  0x100a349a        (reject if outside U+00A0..U+04FF)
            "90"                # nop
        ),
    },
    {
        "name": "NWndUtil::GetNativeStrLen (VA 0x101d9326..0x101d9331)",
        "offset": 0x001D8726,
        "rva": 0x001D9326,
        "orig": bytes.fromhex(
            "81c1f0fbffff"      # add ecx, 0xfffffbf0 (-0x0410)
            "83f93f"            # cmp ecx, 0x3f
            "7708"              # ja  0x101d9339
        ),
        "repl": bytes.fromhex(
            "83c180"            # add ecx, 0xffffff80 (-0x0080)
            "81f97f040000"      # cmp ecx, 0x047f     (U+0080..U+04FF -> 1 char width)
            "7708"              # ja  0x101d9339
        ),
    },
    {
        "name": "IsObsceneWord Cyrillic case-fold gate (VA 0x10215fbb..0x10215fc6)",
        "offset": 0x002153BB,
        "rva": 0x00215FBB,
        "orig": bytes.fromhex(
            "8b1584c32d10"      # mov edx, DWORD PTR ds:0x102dc384
            "833a08"            # cmp DWORD PTR [edx], 0x8
            "7526"              # jne 0x10215fec
        ),
        "repl": bytes.fromhex("90" * 11),
    },
    {
        "name": "AutoLogOn Stage 1 Hook in NCAuthWnd::ShowWindow (VA 0x1014336a)",
        "offset": 0x0014276A,
        "rva": 0x0014336A,
        "orig": bytes.fromhex("e801feffff"),
        "repl": bytes.fromhex("e8d98a1900"),
    },
    {
        "name": "AutoLogOn Stage 2 Hook in NConsoleWnd::AcceptLogin (VA 0x1024e924)",
        "offset": 0x0024DD24,
        "rva": 0x0024E924,
        "orig": bytes.fromhex("83c0ff3bc3"),
        "repl": bytes.fromhex("e824d60800"),
    },
    {
        "name": "AutoLogOn Stage 3 Hook in NCServerSelectWnd::EndServerInfo (VA 0x101274d6)",
        "offset": 0x001268D6,
        "rva": 0x001274D6,
        "orig": bytes.fromhex("8b8238020000ffd0"),
        "repl": bytes.fromhex("e8884a1b00909090"),
    },
    {
        "name": "AutoLogOn Stage 4a Hook in NConsoleWnd::ReceiveCharacterInfo (VA 0x1024e4bf)",
        "offset": 0x0024D8BF,
        "rva": 0x0024E4BF,
        "orig": bytes.fromhex("e87c7cefff"),
        "repl": bytes.fromhex("e8d6da0800"),
    },
    {
        "name": "AutoLogOn Stage 4b Hook in NConsoleWnd::AddCharacterInfo (VA 0x1024e406)",
        "offset": 0x0024D806,
        "rva": 0x0024E406,
        "orig": bytes.fromhex("e8b585efff"),
        "repl": bytes.fromhex("e8b6db0800"),
    },
    {
        "name": "AutoLogOn PIC Code Cave in .text slack (VA 0x102dbe20..0x102dbffd)",
        "offset": 0x002DB220,
        "rva": 0x002DBE20,
        "orig": b"\x00" * len(AUTOLOGIN_CAVE_BYTES),
        "repl": AUTOLOGIN_CAVE_BYTES,
    },
    {
        "name": "AutoLogOn Strings & State Block in .data slack (VA 0x104ba960..0x104ba9e0)",
        "offset": 0x004B9160,
        "rva": 0x004BA960,
        "orig": b"\x00" * len(AUTOLOGIN_DATA_BLOCK),
        "repl": bytes(AUTOLOGIN_DATA_BLOCK),
    },
]

RELOC_PATCHES = [
    {
        "name": ".reloc entry for NCMultiEditBox::OnChar+2 (RVA 0x000a346a)",
        "offset": 0x004C80C2,
        "orig": bytes.fromhex("6a34"),  # IMAGE_REL_BASED_HIGHLOW | 0x046a
        "repl": bytes.fromhex("0000"),  # IMAGE_REL_BASED_ABSOLUTE (no-op)
    },
    {
        "name": ".reloc entry for NCEditBox::OnChar+2 (RVA 0x000d44f0)",
        "offset": 0x004CAC1C,
        "orig": bytes.fromhex("f034"),  # IMAGE_REL_BASED_HIGHLOW | 0x04f0
        "repl": bytes.fromhex("0000"),  # IMAGE_REL_BASED_ABSOLUTE (no-op)
    },
    {
        "name": ".reloc entry for IsObsceneWord+2 (RVA 0x00215fbd)",
        "offset": 0x004E2024,
        "orig": bytes.fromhex("bd3f"),  # IMAGE_REL_BASED_HIGHLOW | 0x0fbd
        "repl": bytes.fromhex("0000"),  # IMAGE_REL_BASED_ABSOLUTE (no-op)
    },
]


def verify_no_active_relocs_in_patched_ranges(buf: bytes) -> int:
    """Walk the entire PE .reloc table and verify zero active relocations overlap patched code ranges."""
    reloc_file_start = 0x004BEE00
    reloc_size = 0x0006DB96
    reloc_file_end = reloc_file_start + reloc_size

    patched_ranges = [(p["rva"], p["rva"] + len(p["repl"]), p["name"]) for p in CODE_PATCHES]
    pos = reloc_file_start
    total_active = 0

    while pos < reloc_file_end:
        page_rva, block_size = struct.unpack_from("<II", buf, pos)
        if page_rva == 0 and block_size == 0:
            break
        assert block_size >= 8 and block_size % 2 == 0, f"Invalid .reloc block at 0x{pos:06X}"
        num_entries = (block_size - 8) // 2
        for i in range(num_entries):
            entry_off = pos + 8 + i * 2
            entry = struct.unpack_from("<H", buf, entry_off)[0]
            rel_type = entry >> 12
            rel_offset = entry & 0x0FFF
            if rel_type == 0:
                continue  # IMAGE_REL_BASED_ABSOLUTE (padding / neutralized no-op)
            total_active += 1
            target_rva = page_rva + rel_offset
            # A HIGHLOW (4-byte) relocation at target_rva modifies [target_rva, target_rva + 4)
            for rva_start, rva_end, name in patched_ranges:
                if target_rva + 4 > rva_start and target_rva < rva_end:
                    raise AssertionError(
                        f"Active .reloc entry at file offset 0x{entry_off:06X} "
                        f"(type={rel_type}, target RVA=0x{target_rva:06X}) overlaps patched range "
                        f"{name} [0x{rva_start:06X}..0x{rva_end:06X})"
                    )
        pos += block_size

    return total_active


def main():
    with open(ORIG_PATH, "rb") as f:
        raw = f.read()

    assert len(raw) == EXPECTED_SIZE, f"Unexpected size: {len(raw)} != {EXPECTED_SIZE}"
    orig_sha = hashlib.sha256(raw).hexdigest()
    assert orig_sha == EXPECTED_ORIG_SHA256, f"Unexpected orig SHA256: {orig_sha}"

    buf = bytearray(raw)
    for p in HEADER_PATCHES + CODE_PATCHES + RELOC_PATCHES:
        off = p["offset"]
        orig = p["orig"]
        repl = p["repl"]
        assert len(orig) == len(repl), f"Length mismatch in {p['name']}: {len(orig)} != {len(repl)}"
        actual = bytes(buf[off : off + len(orig)])
        assert actual == orig, (
            f"Original bytes mismatch at {p['name']} (0x{off:06X}):\n"
            f"  expected: {orig.hex()}\n"
            f"  actual:   {actual.hex()}"
        )
        buf[off : off + len(repl)] = repl
        print(f"[OK] Patched {p['name']} @ file offset 0x{off:06X} ({len(repl)} bytes)")

    active_relocs = verify_no_active_relocs_in_patched_ranges(buf)
    print(f"[OK] Verified PE .reloc table ({active_relocs} active entries): 0 overlaps with patched ranges")

    assert len(buf) == EXPECTED_SIZE
    patched_sha = hashlib.sha256(buf).hexdigest()

    with open(OUT_CLIENT, "wb") as f:
        f.write(buf)
    if os.path.exists(os.path.dirname(OUT_REPO)):
        shutil.copy2(OUT_CLIENT, OUT_REPO)

    print(f"Done! {OUT_CLIENT} ({len(buf)} bytes, 0-byte delta) sha256={patched_sha}")


if __name__ == "__main__":
    main()

