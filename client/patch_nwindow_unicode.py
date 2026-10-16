#!/usr/bin/env python3
"""
patch_nwindow_unicode.py — Unlocks international Unicode keyboard input (U+00A0..U+04FF)
in the 2009-09-28 (Protocol 148) Lineage II Gracia Epilogue NWindow.dll while keeping
the English UI (Language=0 / *-e.dat).

Patched code sites in NWindow.dll (5,425,684 bytes, 0-byte size delta):
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

Patched PE `.reloc` entries (neutralized to IMAGE_REL_BASED_ABSOLUTE = 0x0000 so PE
rebasing does not add BaseDelta to the replaced instructions):
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
    for p in CODE_PATCHES + RELOC_PATCHES:
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
