#!/usr/bin/env python3
"""
patch_nwindow_unicode.py — Unlocks international Unicode keyboard input (U+00A0..U+04FF)
in the 2009-09-28 (Protocol 148) Lineage II Gracia Epilogue NWindow.dll while keeping
the English UI (Language=0 / *-e.dat).

Patched sites in NWindow.dll (5,425,684 bytes, 0-byte size delta):
1. NCEditBox::OnChar (VA 0x100d44ee..0x100d450c, file offset 0x000D38EE, 30 bytes):
   - Removes the `GLanguage == 8` (Russian) requirement that silently dropped all
     non-ASCII characters when `Language=0` in `l2.ini`.
   - Widens allowed input range from basic Russian `U+0410..U+044F` (`А..я`) to
     `U+00A0..U+04FF` (Latin-1 Supplement, Latin Extended-A/B for Polish/Czech/Slovak/
     Croatian/Slovenian/etc., IPA, Greek `U+0370..U+03FF`, and full Cyrillic `U+0400..U+04FF`
     including `Ё/ё`, Ukrainian `Є/І/Ї/Ґ`, Belarusian `Ў`, Serbian/Macedonian `Ђ/Ѓ/Ј/Љ/Њ/Ћ/Ќ/Џ`).
   - Excludes Combining Diacritical Marks `U+0300..U+036F` to prevent "Zalgo" stacking spam.
2. NCMultiEditBox::OnChar (VA 0x100a3468..0x100a347e, file offset 0x000A2868, 22 bytes):
   - Identical filter update for multi-line edit boxes (Mail body, Community Board memos,
     Petitions).
3. NWndUtil::GetNativeStrLen (VA 0x101d9326..0x101d9331, file offset 0x001D8726, 11 bytes):
   - Widens single-width character counting from `U+0410..U+044F` to `U+0080..U+04FF`
     so European, Greek, and Cyrillic characters count as 1 character (instead of 2 CJK
     columns) toward EditBox/Chat/Mail MaxLength limits.
4. IsObsceneWord (VA 0x10215fbb..0x10215fc6, file offset 0x002153BB, 11 bytes):
   - Removes `GLanguage == 8` gate on Cyrillic uppercase-to-lowercase normalization.
"""

import hashlib
import os
import shutil

ORIG_PATH = "Client Lineage 2 Gracia Epilogue/system/backup_encrypted/nwindow.dll.orig"
OUT_CLIENT = "Client Lineage 2 Gracia Epilogue/system/nwindow.dll"
OUT_REPO = "l2jmobius_dwea_fork/client/system/nwindow.dll"

EXPECTED_SIZE = 5425684
EXPECTED_ORIG_SHA256 = "dcd951c811002318f3164f1221f1b8c9d7466c17f3105c0b7ece49cb0e3bbb45"

PATCHES = [
    {
        "name": "NCEditBox::OnChar (VA 0x100d44ee..0x100d450c)",
        "offset": 0x000D38EE,
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
        "orig": bytes.fromhex(
            "8b1584c32d10"      # mov edx, DWORD PTR ds:0x102dc384
            "833a08"            # cmp DWORD PTR [edx], 0x8
            "7526"              # jne 0x10215fec
        ),
        "repl": bytes.fromhex("90" * 11),
    },
]


def main():
    with open(ORIG_PATH, "rb") as f:
        raw = f.read()

    assert len(raw) == EXPECTED_SIZE, f"Unexpected size: {len(raw)} != {EXPECTED_SIZE}"
    orig_sha = hashlib.sha256(raw).hexdigest()
    assert orig_sha == EXPECTED_ORIG_SHA256, f"Unexpected orig SHA256: {orig_sha}"

    buf = bytearray(raw)
    for p in PATCHES:
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

    assert len(buf) == EXPECTED_SIZE
    patched_sha = hashlib.sha256(buf).hexdigest()

    with open(OUT_CLIENT, "wb") as f:
        f.write(buf)
    if os.path.exists(os.path.dirname(OUT_REPO)):
        shutil.copy2(OUT_CLIENT, OUT_REPO)

    print(f"Done! {OUT_CLIENT} ({len(buf)} bytes, 0-byte delta) sha256={patched_sha}")


if __name__ == "__main__":
    main()
