#!/usr/bin/env python3
"""
Lineage 2 Gracia Epilogue (2009-09-28, Protocol 148) — Engine.dll Network Thread Exit Crash & Packet Buffer Fix
=================================================================================================================
Fixes the manual game exit page fault at VA 0x2039f3bc (`mov ecx, [esi+0x38]` where `ESI = 0x7fbd0000` is the
freed `FSocket` / `UNetworkHandler` sub-object) in background worker thread `RecvThreadProc` (`VA 0x2039f2b0`):

1. Root Cause #1 — `CloseSocket` (`VA 0x203c50a0`, file offset `0x3c44af..0x3c44e3`, 52 bytes):
   - Original code closed `[edi+0x38]` (`closesocket`), deleted `CRITICAL_SECTION` `[edi+0x9090]`, and only
     cleared `ds:0x206daf80` (`GNetworkThreadRunning = 0`) at the very end WITHOUT waiting for the background
     thread `[edi+0x895c]` (`hThread`) to terminate before `FSocket::Destroy` (`VA 0x203c5620`) freed `edi`.
   - Patched to clear `ds:0x206daf80 = 0` and `[edi+0x38] = 0` FIRST, call `closesocket(old_sock)`, wait for
     the background receive thread via `WaitForSingleObject([edi+0x895c], 100)`, and only then call
     `DeleteCriticalSection(&[edi+0x9090])`.

2. Root Cause #2 — `RecvThreadProc` Wait Loop (`VA 0x2039f2c3..0x2039f300`, file offset `0x39e6c3..0x39e700`, 61 bytes):
   - Original code only checked `ds:0x206daf80 == 0` when `WaitForSingleObject([esi+0x8958], 100)` returned
     `WAIT_TIMEOUT` (`0x102`). When `WaitForSingleObject` returned `WAIT_OBJECT_0` (`0`) or `WAIT_FAILED`
     (`0xFFFFFFFF`, e.g., when `[esi+0x8958]` was invalidated or corrupted by an oversized >32,768-byte packet),
     `jne 0x2039f309` bypassed `ds:0x206daf80` and jumped straight into `recv()`. Furthermore, on thread exit
     (`0x2039f2e2`), it read and wrote `[esi+0x8958]` on the already-freed `esi` pointer.
   - Patched to check `ds:0x206daf80 == 0` BEFORE reading `[esi+0x8958]` and IMMEDIATELY after
     `WaitForSingleObject`, loop back on any non-zero return (`WAIT_TIMEOUT` or `WAIT_FAILED`), and exit via
     `0x2039f2fd` (`pop edi; pop ebp; pop ebx; pop esi; add esp, 4; jmp _endthread`) without touching freed `esi`.

3. Root Cause #3 — Thread-Unsafe `GConfig->GetString` in `RecvThreadProc` (`VA 0x2039f37c..0x2039f3a1`, file offset `0x39e77c..0x39e7a1`, 37 bytes):
   - Original code called `GConfig->GetString(L"SECURE", L"SecureModule", 0, L"TRUE")` + `appToBool` on every
     single `recv()` iteration from the background socket thread without synchronization, racing against main
     thread `GConfig` and `UNetworkHandler` teardown on game exit before executing `mov ecx, [esi+0x38]` at
     `VA 0x2039f3bc`.
   - Patched to check `ds:0x206daf80 == 0` and `[esi+0x38] == 0` (`je 0x2039f2fd`) and set `eax = 1` directly.
"""

from pathlib import Path

ENGINE_PATH = Path("/home/dwea/Projects/L2/Client Lineage 2 Gracia Epilogue/system/Engine.dll")

# 1. RecvThreadProc wait loop (VA 0x2039f2c3..0x2039f300, file offset 0x39e6c3..0x39e700, 61 bytes)
ORIG_WAIT_LOOP = bytes.fromhex(
    "8b8658890000"          # mov eax, [esi+0x8958]
    "6a64"                  # push 0x64
    "50"                    # push eax
    "ff15386e4420"          # call dword ptr ds:[0x20446e38] (WaitForSingleObject)
    "3d02010000"            # cmp eax, 0x102
    "7530"                  # jne 0x2039f309
    "833d80af6d2000"        # cmp dword ptr ds:[0x206daf80], 0
    "75e1"                  # jne 0x2039f2c3
    "8b8658890000"          # mov eax, [esi+0x8958]
    "85c0"                  # test eax, eax
    "5f5d5b"                # pop edi; pop ebp; pop ebx
    "7411"                  # je 0x2039f300
    "50"                    # push eax
    "ff15306e4420"          # call dword ptr ds:[0x20446e30] (CloseHandle)
    "c7865889000000000000"  # mov dword ptr [esi+0x8958], 0
)

PATCH_WAIT_LOOP = bytes.fromhex(
    "833d80af6d2000"        # 2039f2c3: cmp dword ptr ds:[0x206daf80], 0
    "7431"                  # 2039f2ca: je 0x2039f2fd (exit_clean)
    "8b8658890000"          # 2039f2cc: mov eax, [esi+0x8958]
    "85c0"                  # 2039f2d2: test eax, eax
    "7427"                  # 2039f2d4: je 0x2039f2fd (exit_clean)
    "6a64"                  # 2039f2d6: push 0x64
    "50"                    # 2039f2d8: push eax
    "ff15386e4420"          # 2039f2d9: call dword ptr ds:[0x20446e38] (WaitForSingleObject)
    "833d80af6d2000"        # 2039f2df: cmp dword ptr ds:[0x206daf80], 0
    "7415"                  # 2039f2e6: je 0x2039f2fd (exit_clean)
    "85c0"                  # 2039f2e8: test eax, eax (WAIT_OBJECT_0 == 0?)
    "75d7"                  # 2039f2ea: jne 0x2039f2c3 (loop on WAIT_TIMEOUT or WAIT_FAILED)
    "eb1b"                  # 2039f2ec: jmp 0x2039f309
    + ("90" * 15)           # 2039f2ee..2039f2fc: 15x nop
    + "5f5d5b"              # 2039f2fd: pop edi; pop ebp; pop ebx -> 2039f300: pop esi; add esp, 4; jmp _endthread
)

# 2. RecvThreadProc pre-recv check (VA 0x2039f37c..0x2039f3a1, file offset 0x39e77c..0x39e7a1, 37 bytes)
ORIG_PRE_RECV = bytes.fromhex(
    "8b0a"                  # mov ecx, [edx]
    "8b01"                  # mov eax, [ecx]
    "8b5014"                # mov edx, [eax+0x14]
    "6874524920"            # push 0x20495274 (L"TRUE")
    "55"                    # push ebp
    "6880524920"            # push 0x20495280 (L"SecureModule")
    "689c524920"            # push 0x2049529c (L"SECURE")
    "ffd2"                  # call edx (GConfig->GetString)
    "50"                    # push eax
    "ff15f4624420"          # call dword ptr ds:[0x204462f4] (appToBool)
    "83c408"                # add esp, 8
    "85c0"                  # test eax, eax
)

PATCH_PRE_RECV = bytes.fromhex(
    "833d80af6d2000"        # 2039f37c: cmp dword ptr ds:[0x206daf80], 0
    "0f8474ffffff"          # 2039f383: je 0x2039f2fd (exit_clean)
    "8b4e38"                # 2039f389: mov ecx, [esi+0x38]
    "85c9"                  # 2039f38c: test ecx, ecx
    "0f8469ffffff"          # 2039f38e: je 0x2039f2fd (exit_clean if socket == 0)
    "b801000000"            # 2039f394: mov eax, 1 (SecureModule = TRUE)
    "909090909090"          # 2039f399..2039f39e: 6x nop
    "85c0"                  # 2039f39f: test eax, eax
)

# 3. CloseSocket synchronization (VA 0x203c50af..0x203c50e3, file offset 0x3c44af..0x3c44e3, 52 bytes)
ORIG_CLOSE_SOCK = bytes.fromhex(
    "8b4738"                # mov eax, [edi+0x38]
    "33f6"                  # xor esi, esi
    "3bc6"                  # cmp eax, esi
    "7414"                  # je 0x203c50cc
    "50"                    # push eax
    "ff15b86f4420"          # call dword ptr ds:[0x20446fb8] (closesocket)
    "8d8790900000"          # lea eax, [edi+0x9090]
    "50"                    # push eax
    "ff15486e4420"          # call dword ptr ds:[0x20446e48] (DeleteCriticalSection)
    "39b75c890000"          # cmp [edi+0x895c], esi
    "897738"                # mov [edi+0x38], esi
    "740c"                  # je 0x203c50e3
    "89b75c890000"          # mov [edi+0x895c], esi
    "893580af6d20"          # mov dword ptr ds:[0x206daf80], esi
)

PATCH_CLOSE_SOCK = bytes.fromhex(
    "33f6"                  # 203c50af: xor esi, esi
    "893580af6d20"          # 203c50b1: mov dword ptr ds:[0x206daf80], esi (GNetworkThreadRunning = 0 FIRST!)
    "8b4738"                # 203c50b7: mov eax, [edi+0x38]
    "3bc6"                  # 203c50ba: cmp eax, esi
    "7425"                  # 203c50bc: je 0x203c50e3
    "897738"                # 203c50be: mov [edi+0x38], esi (zero socket before closesocket)
    "50"                    # 203c50c1: push eax
    "ff15b86f4420"          # 203c50c2: call dword ptr ds:[0x20446fb8] (closesocket)
    "6a64"                  # 203c50c8: push 0x64 (100ms timeout)
    "ffb75c890000"          # 203c50ca: push dword ptr [edi+0x895c] (hThread)
    "ff15386e4420"          # 203c50d0: call dword ptr ds:[0x20446e38] (WaitForSingleObject)
    "8d8790900000"          # 203c50d6: lea eax, [edi+0x9090]
    "50"                    # 203c50dc: push eax
    "ff15486e4420"          # 203c50dd: call dword ptr ds:[0x20446e48] (DeleteCriticalSection)
)


def main():
    assert len(ORIG_WAIT_LOOP) == len(PATCH_WAIT_LOOP) == 61
    assert len(ORIG_PRE_RECV) == len(PATCH_PRE_RECV) == 37
    assert len(ORIG_CLOSE_SOCK) == len(PATCH_CLOSE_SOCK) == 52

    data = bytearray(ENGINE_PATH.read_bytes())

    for name, off, orig, patch in [
        ("RecvThreadProc wait loop (VA 0x2039f2c3)", 0x39E6C3, ORIG_WAIT_LOOP, PATCH_WAIT_LOOP),
        ("RecvThreadProc pre-recv (VA 0x2039f37c)", 0x39E77C, ORIG_PRE_RECV, PATCH_PRE_RECV),
        ("CloseSocket sync (VA 0x203c50af)", 0x3C44AF, ORIG_CLOSE_SOCK, PATCH_CLOSE_SOCK),
    ]:
        cur = bytes(data[off : off + len(orig)])
        if cur == orig:
            data[off : off + len(patch)] = patch
            print(f"[+] Applied {name} at file offset 0x{off:x} ({len(patch)} bytes)")
        elif cur == patch:
            print(f"[=] Already patched: {name} at file offset 0x{off:x}")
        else:
            raise RuntimeError(f"Unexpected bytes at {name} (0x{off:x}): {cur.hex()}")

    ENGINE_PATH.write_bytes(data)
    print(f"[+] Successfully updated {ENGINE_PATH} ({len(data)} bytes)")


if __name__ == "__main__":
    main()
