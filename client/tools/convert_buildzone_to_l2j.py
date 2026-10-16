#!/usr/bin/env python3
"""
Lineage 2 Gracia Epilogue (CT2.4) — FWorldBuilder (///buildzone) & L2OFF Geodata Converter
==========================================================================================

Converts between:
  1. `<X>_<Y>_path.txt` (ASCII geodata dump produced by `Engine.dll` `FWorldBuilder::buildWorldStructure`
     at VA `0x2018dd39..0x2018dea9` and loaded by `///loadpath` at VA `0x2018bb20..0x2018be00`)
  2. `<X>_<Y>_conv.dat` (Binary L2OFF geodata format loaded by `Engine.dll` at VA `0x203902f0..0x20390650`)
  3. `<X>_<Y>.l2j`      (Binary L2J geodata format loaded by `Region.java` in `L2J_Mobius_CT_2.4_Epilogue`)

Forensic Reference (`Engine.dll` Base `0x20000000`):
  - `FWorldBuilder` 4-direction table at VA `0x20467b08`:
      dir[0] (`[node+0x10]`): dx = +1, dy =  0  -> East  (`Cell.NSWE_EAST  = 1 << 0 = 0x01`)
      dir[1] (`[node+0x14]`): dx = -1, dy =  0  -> West  (`Cell.NSWE_WEST  = 1 << 1 = 0x02`)
      dir[2] (`[node+0x18]`): dx =  0, dy = +1  -> South (`Cell.NSWE_SOUTH = 1 << 2 = 0x04`)
      dir[3] (`[node+0x1c]`): dx =  0, dy = -1  -> North (`Cell.NSWE_NORTH = 1 << 3 = 0x08`)
    In `<X>_<Y>_path.txt`, each layer is formatted as `(<height>:<d0><d1><d2><d3>)` where `d0..d3` are
    `'0'` or `'1'` corresponding to `dir[0..3]` (East, West, South, North).
  - 16-bit Cell/Layer Word Encoding (`short layerData`, little-endian):
      encoded_word = (((height & ~7) << 1) & 0xFFF0) | (nswe & 0x0F)
      decoded_z    = (short(encoded_word & 0xFFF0)) >> 1
      decoded_nswe = encoded_word & 0x0F
"""

import argparse
import os
import re
import struct
import sys
from pathlib import Path
from typing import List, Optional, Tuple

REGION_BLOCKS_X = 256
REGION_BLOCKS_Y = 256
REGION_BLOCKS = REGION_BLOCKS_X * REGION_BLOCKS_Y  # 65,536
BLOCK_CELLS_X = 8
BLOCK_CELLS_Y = 8
BLOCK_CELLS = BLOCK_CELLS_X * BLOCK_CELLS_Y  # 64
REGION_CELLS_X = REGION_BLOCKS_X * BLOCK_CELLS_X  # 2,048
REGION_CELLS_Y = REGION_BLOCKS_Y * BLOCK_CELLS_Y  # 2,048
REGION_CELLS = REGION_CELLS_X * REGION_CELLS_Y  # 4,194,304

TYPE_FLAT = 0
TYPE_COMPLEX = 1
TYPE_MULTILAYER = 2

NSWE_EAST = 1 << 0   # 0x01 (+X, dir[0])
NSWE_WEST = 1 << 1   # 0x02 (-X, dir[1])
NSWE_SOUTH = 1 << 2  # 0x04 (+Y, dir[2])
NSWE_NORTH = 1 << 3  # 0x08 (-Y, dir[3])
NSWE_ALL = NSWE_EAST | NSWE_WEST | NSWE_SOUTH | NSWE_NORTH  # 0x0F
NSWE_NONE = 0x00

MAX_LAYERS = 125
MIN_HEIGHT = -16384
MAX_HEIGHT = 16376

LINE_RE = re.compile(r"^\[(\d+),(\d+)\](\d+)(.*)$")
LAYER_RE = re.compile(r"\((-?\d+):([01])([01])([01])([01])\)")
REGION_NAME_RE = re.compile(r"(\d{2})_(\d{2})")


def clamp_and_align_height(h: int) -> int:
    """Clamps world Z height to [-16384, 16376] and aligns to 8-unit geodata resolution."""
    if h < MIN_HEIGHT:
        h = MIN_HEIGHT
    elif h > MAX_HEIGHT:
        h = MAX_HEIGHT
    # Match Engine.dll signed 8-unit quantization (sar 3, shl 3)
    if h >= 0:
        return (h // 8) * 8
    return -((-h) // 8) * 8


def encode_layer_word(height: int, nswe: int) -> int:
    """Packs (height, nswe) into an unsigned 16-bit L2J/L2OFF layer word."""
    h = clamp_and_align_height(height)
    return (((h << 1) & 0xFFF0) | (nswe & 0x0F)) & 0xFFFF


def decode_layer_word(word: int) -> Tuple[int, int]:
    """Unpacks an unsigned 16-bit L2J/L2OFF layer word into (height, nswe) matching Region.java."""
    masked = word & 0xFFF0
    signed_masked = masked - 0x10000 if (masked & 0x8000) else masked
    height = signed_masked >> 1
    nswe = word & 0x0F
    return height, nswe


def parse_region_coords(filename: str) -> Tuple[int, int]:
    """Extracts (MapX, MapY) from a filename like '17_25_path.txt', '17_25_conv.dat', or '17_25.l2j'."""
    m = REGION_NAME_RE.search(os.path.basename(filename))
    if m:
        return int(m.group(1)), int(m.group(2))
    return 20, 18


def convert_path_txt_to_l2j(
    in_path: Path,
    out_path: Path,
    default_height: int = MIN_HEIGHT,
    empty_nswe: int = NSWE_NONE,
) -> dict:
    """
    Parses an Engine.dll `///buildzone` `<X>_<Y>_path.txt` file and writes a standard `.l2j` file.
    """
    # Store layers per cell index (x * 2048 + y) as a list of (height, nswe) tuples.
    # To keep memory compact across 4,194,304 cells, store packed uint16 words per cell.
    cells: List[Optional[Tuple[int, ...]]] = [None] * REGION_CELLS
    parsed_cells = 0
    empty_cells = 0
    total_layers = 0

    with in_path.open("r", encoding="ascii", errors="replace") as f:
        for raw_line in f:
            line = raw_line.strip()
            if not line or not line.startswith("["):
                continue
            m = LINE_RE.match(line)
            if not m:
                continue
            x = int(m.group(1))
            y = int(m.group(2))
            if not (0 <= x < REGION_CELLS_X and 0 <= y < REGION_CELLS_Y):
                continue
            num_layers = int(m.group(3))
            rest = m.group(4)

            if num_layers == 0 or not rest:
                cells[(x << 11) | y] = ()
                parsed_cells += 1
                empty_cells += 1
                continue

            layers_raw = []
            for lm in LAYER_RE.finditer(rest):
                h = int(lm.group(1))
                d0 = int(lm.group(2))  # East  (bit 0)
                d1 = int(lm.group(3))  # West  (bit 1)
                d2 = int(lm.group(4))  # South (bit 2)
                d3 = int(lm.group(5))  # North (bit 3)
                nswe = (d0 << 0) | (d1 << 1) | (d2 << 2) | (d3 << 3)
                layers_raw.append((clamp_and_align_height(h), nswe))

            if not layers_raw:
                cells[(x << 11) | y] = ()
                parsed_cells += 1
                empty_cells += 1
                continue

            # Sort descending by height (highest Z first, matching FWorldBuilder::InsertNode)
            # and deduplicate identical heights
            layers_raw.sort(key=lambda item: item[0], reverse=True)
            deduped: List[int] = []
            last_h = None
            for h, nswe in layers_raw:
                if h != last_h:
                    deduped.append(encode_layer_word(h, nswe))
                    last_h = h
                    if len(deduped) >= MAX_LAYERS:
                        break

            cells[(x << 11) | y] = tuple(deduped)
            parsed_cells += 1
            total_layers += len(deduped)

    fallback_word = encode_layer_word(default_height, empty_nswe)

    flat_blocks = 0
    complex_blocks = 0
    multilayer_blocks = 0

    out_path.parent.mkdir(parents=True, exist_ok=True)
    with out_path.open("wb") as out_f:
        for bx in range(REGION_BLOCKS_X):
            base_x = bx << 3
            for by in range(REGION_BLOCKS_Y):
                base_y = by << 3

                # Gather 64 cells in block
                block_cells: List[Tuple[int, ...]] = []
                nearest_valid_word = None
                for cx in range(BLOCK_CELLS_X):
                    gx = base_x | cx
                    row_off = gx << 11
                    for cy in range(BLOCK_CELLS_Y):
                        gy = base_y | cy
                        c_layers = cells[row_off | gy]
                        if c_layers:
                            if nearest_valid_word is None:
                                h0, _ = decode_layer_word(c_layers[0])
                                nearest_valid_word = encode_layer_word(h0, empty_nswe)
                            block_cells.append(c_layers)
                        else:
                            block_cells.append(())

                fill_word = nearest_valid_word if nearest_valid_word is not None else fallback_word
                block_cells = [c if c else (fill_word,) for c in block_cells]

                max_cell_layers = max(len(c) for c in block_cells)
                if max_cell_layers == 1:
                    first_word = block_cells[0][0]
                    first_h, first_nswe = decode_layer_word(first_word)
                    if first_nswe == NSWE_ALL and all(c[0] == first_word for c in block_cells):
                        out_f.write(struct.pack("<Bh", TYPE_FLAT, first_h))
                        flat_blocks += 1
                    else:
                        words = [c[0] for c in block_cells]
                        out_f.write(struct.pack("<B64H", TYPE_COMPLEX, *words))
                        complex_blocks += 1
                else:
                    buf = bytearray([TYPE_MULTILAYER])
                    for c in block_cells:
                        buf.append(len(c))
                        buf.extend(struct.pack(f"<{len(c)}H", *c))
                    out_f.write(buf)
                    multilayer_blocks += 1

    return {
        "parsed_cells": parsed_cells,
        "empty_cells": empty_cells,
        "total_layers": total_layers,
        "flat_blocks": flat_blocks,
        "complex_blocks": complex_blocks,
        "multilayer_blocks": multilayer_blocks,
        "output_bytes": out_path.stat().st_size,
    }


def convert_conv_dat_to_l2j(in_path: Path, out_path: Path) -> dict:
    """
    Converts an L2OFF `<X>_<Y>_conv.dat` binary geodata file (as read by Engine.dll VA 0x203902f0)
    into a standard L2J `<X>_<Y>.l2j` file.
    """
    data = in_path.read_bytes()
    if len(data) < 18:
        raise ValueError(f"File too small to be a valid _conv.dat (got {len(data)} bytes)")

    map_x, map_y, cell_size, sub_cell_size, total_cells, total_nodes, total_blocks = struct.unpack_from(
        "<BBHHIII", data, 0
    )
    pos = 18

    flat_blocks = 0
    complex_blocks = 0
    multilayer_blocks = 0

    out_path.parent.mkdir(parents=True, exist_ok=True)
    with out_path.open("wb") as out_f:
        for _ in range(REGION_BLOCKS):
            sub_type = struct.unpack_from("<H", data, pos)[0]
            pos += 2
            if sub_type == 0x0000:
                # Solid Sector (TYPE_FLAT): 2x int16 (max_height, min_height); Engine.dll uses first int16 [esi+0xca]
                h_max, _h_min = struct.unpack_from("<hh", data, pos)
                pos += 4
                out_f.write(struct.pack("<Bh", TYPE_FLAT, h_max))
                flat_blocks += 1
            elif sub_type == 0x0040:
                # Single Layer (TYPE_COMPLEX): 64x uint16
                raw_cells = data[pos : pos + 128]
                pos += 128
                out_f.write(b"\x01" + raw_cells)
                complex_blocks += 1
            else:
                # Multi Layer (TYPE_MULTILAYER): 64 cells, each starting with uint16 n_layers + n_layers * uint16
                buf = bytearray([TYPE_MULTILAYER])
                for _ in range(BLOCK_CELLS):
                    n_layers = struct.unpack_from("<H", data, pos)[0]
                    pos += 2
                    if not (1 <= n_layers <= MAX_LAYERS):
                        raise ValueError(f"Invalid layer count {n_layers} in _conv.dat at offset {pos - 2}")
                    buf.append(n_layers)
                    layer_bytes = data[pos : pos + (n_layers << 1)]
                    pos += n_layers << 1
                    buf.extend(layer_bytes)
                out_f.write(buf)
                multilayer_blocks += 1

    return {
        "map_x": map_x,
        "map_y": map_y,
        "cell_size": cell_size,
        "sub_cell_size": sub_cell_size,
        "header_total_cells": total_cells,
        "header_total_nodes": total_nodes,
        "header_total_blocks": total_blocks,
        "flat_blocks": flat_blocks,
        "complex_blocks": complex_blocks,
        "multilayer_blocks": multilayer_blocks,
        "consumed_bytes": pos,
        "input_bytes": len(data),
        "output_bytes": out_path.stat().st_size,
    }


def convert_l2j_to_path_txt(in_path: Path, out_path: Path) -> dict:
    """
    Converts a standard L2J `<X>_<Y>.l2j` file into `<X>_<Y>_path.txt` format compatible with
    Engine.dll `///loadpath FILE=<X>_<Y>_path.txt` (`FWorldBuilder::LoadPath`, VA 0x2018bb20).
    """
    data = in_path.read_bytes()
    map_x, map_y = parse_region_coords(str(in_path))
    min_x = (map_x - 20) << 15
    min_y = (map_y - 18) << 15
    max_x = min_x + 0x7FFF
    max_y = min_y + 0x7FFF

    # First pass: parse all 65,536 blocks and count total layers
    pos = 0
    block_descriptors: List[Tuple[int, int]] = []  # (block_type, offset_or_height)
    total_nodes = 0

    for _ in range(REGION_BLOCKS):
        btype = data[pos]
        pos += 1
        if btype == TYPE_FLAT:
            h = struct.unpack_from("<h", data, pos)[0]
            pos += 2
            block_descriptors.append((TYPE_FLAT, h))
            total_nodes += BLOCK_CELLS
        elif btype == TYPE_COMPLEX:
            block_descriptors.append((TYPE_COMPLEX, pos))
            pos += BLOCK_CELLS * 2
            total_nodes += BLOCK_CELLS
        elif btype == TYPE_MULTILAYER:
            block_descriptors.append((TYPE_MULTILAYER, pos))
            for _ in range(BLOCK_CELLS):
                nl = data[pos]
                if not (1 <= nl <= MAX_LAYERS):
                    raise ValueError(f"Invalid layer count {nl} at offset {pos}")
                pos += 1 + (nl << 1)
                total_nodes += nl
        else:
            raise ValueError(f"Invalid block type {btype} at offset {pos - 1}")

    if pos != len(data):
        raise ValueError(f"Trailing bytes in .l2j file: consumed {pos} of {len(data)} bytes")

    # Pre-index multilayer cell offsets per multilayer block if needed, or index by (bx, by)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    with out_path.open("w", encoding="ascii", newline="\r\n") as out_f:
        out_f.write(f"total : {total_nodes}\n")
        out_f.write(f"visited : {REGION_CELLS}\n")
        out_f.write(f"({min_x}, {min_y}) ~ ({max_x}, {max_y})\n")
        out_f.write("cellsize : 16\n")
        out_f.write("heightunit : 8\n")
        out_f.write("\n")

        for gx in range(REGION_CELLS_X):
            bx = gx >> 3
            cx = gx & 7
            lines = []
            for gy in range(REGION_CELLS_Y):
                by = gy >> 3
                cy = gy & 7
                btype, payload = block_descriptors[(bx << 8) | by]
                cell_idx = (cx << 3) | cy

                if btype == TYPE_FLAT:
                    h = clamp_and_align_height(payload)
                    lines.append(f"[{gx},{gy}]1({h}:1111)\n")
                elif btype == TYPE_COMPLEX:
                    word = struct.unpack_from("<H", data, payload + (cell_idx << 1))[0]
                    h, nswe = decode_layer_word(word)
                    d0 = (nswe >> 0) & 1
                    d1 = (nswe >> 1) & 1
                    d2 = (nswe >> 2) & 1
                    d3 = (nswe >> 3) & 1
                    lines.append(f"[{gx},{gy}]1({h}:{d0}{d1}{d2}{d3})\n")
                else:
                    p = payload
                    for _ in range(cell_idx):
                        nl_skip = data[p]
                        p += 1 + (nl_skip << 1)
                    nl = data[p]
                    p += 1
                    parts = [f"[{gx},{gy}]{nl}"]
                    for _ in range(nl):
                        word = struct.unpack_from("<H", data, p)[0]
                        p += 2
                        h, nswe = decode_layer_word(word)
                        d0 = (nswe >> 0) & 1
                        d1 = (nswe >> 1) & 1
                        d2 = (nswe >> 2) & 1
                        d3 = (nswe >> 3) & 1
                        parts.append(f"({h}:{d0}{d1}{d2}{d3})")
                    parts.append("\n")
                    lines.append("".join(parts))
            out_f.writelines(lines)

        out_f.write(f"\noutput : {REGION_CELLS}\n")

    return {
        "map_x": map_x,
        "map_y": map_y,
        "total_nodes": total_nodes,
        "total_cells": REGION_CELLS,
        "output_bytes": out_path.stat().st_size,
    }


def verify_l2j_file(l2j_path: Path) -> dict:
    """
    Validates a `.l2j` geodata file against all `Region.java` loader invariants.
    """
    data = l2j_path.read_bytes()
    pos = 0
    flat_blocks = 0
    complex_blocks = 0
    multilayer_blocks = 0
    total_layers = 0
    min_z = 32767
    max_z = -32768

    for block_idx in range(REGION_BLOCKS):
        if pos >= len(data):
            raise ValueError(f"Unexpected EOF at block {block_idx} (offset {pos})")
        btype = data[pos]
        pos += 1
        if btype == TYPE_FLAT:
            h = struct.unpack_from("<h", data, pos)[0]
            pos += 2
            flat_blocks += 1
            total_layers += BLOCK_CELLS
            if h < min_z:
                min_z = h
            if h > max_z:
                max_z = h
        elif btype == TYPE_COMPLEX:
            words = struct.unpack_from("<64H", data, pos)
            pos += 128
            complex_blocks += 1
            total_layers += BLOCK_CELLS
            for w in words:
                h, _ = decode_layer_word(w)
                if h < min_z:
                    min_z = h
                if h > max_z:
                    max_z = h
        elif btype == TYPE_MULTILAYER:
            multilayer_blocks += 1
            for cell_idx in range(BLOCK_CELLS):
                nl = data[pos]
                pos += 1
                if nl <= 0 or nl > MAX_LAYERS:
                    raise ValueError(
                        f"Region.java invariant violation at block {block_idx} cell {cell_idx}: nLayers={nl}"
                    )
                words = struct.unpack_from(f"<{nl}H", data, pos)
                pos += nl << 1
                total_layers += nl
                for w in words:
                    h, _ = decode_layer_word(w)
                    if h < min_z:
                        min_z = h
                    if h > max_z:
                        max_z = h
        else:
            raise ValueError(f"Invalid block type {btype} at block {block_idx} (offset {pos - 1})")

    if pos != len(data):
        raise ValueError(f"File size mismatch: Region.java consumed {pos} bytes, file has {len(data)} bytes")

    return {
        "valid": True,
        "file_bytes": len(data),
        "flat_blocks": flat_blocks,
        "complex_blocks": complex_blocks,
        "multilayer_blocks": multilayer_blocks,
        "total_layers": total_layers,
        "min_z": min_z,
        "max_z": max_z,
    }


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Convert between Engine.dll ///buildzone (_path.txt), L2OFF (_conv.dat), and L2J (.l2j) geodata."
    )
    parser.add_argument("input", type=Path, help="Input file (<X>_<Y>_path.txt, <X>_<Y>_conv.dat, or <X>_<Y>.l2j)")
    parser.add_argument("output", type=Path, nargs="?", help="Output file path (optional when using --verify)")
    parser.add_argument(
        "--to-path-txt",
        action="store_true",
        help="Convert a server .l2j file to Engine.dll _path.txt format (for ///loadpath & ///c_rmode geodata)",
    )
    parser.add_argument(
        "--verify",
        action="store_true",
        help="Verify a .l2j file against Region.java off-heap MemorySegment loader invariants",
    )
    parser.add_argument(
        "--default-height",
        type=int,
        default=MIN_HEIGHT,
        help="Fallback Z height for empty unvisited cells in _path.txt (default: -16384)",
    )
    parser.add_argument(
        "--empty-nswe",
        type=int,
        default=NSWE_NONE,
        help="Fallback NSWE bitmask for empty unvisited cells in _path.txt (default: 0x00)",
    )

    args = parser.parse_args()
    in_path: Path = args.input

    if not in_path.is_file():
        print(f"[-] Input file not found: {in_path}", file=sys.stderr)
        return 1

    if args.verify and args.output is None and in_path.suffix.lower() == ".l2j":
        stats = verify_l2j_file(in_path)
        print(f"[+] Verified {in_path.name} ({stats['file_bytes']:,} bytes):")
        print(
            f"    Blocks: {stats['flat_blocks']:,} flat, "
            f"{stats['complex_blocks']:,} complex, "
            f"{stats['multilayer_blocks']:,} multilayer (total = 65,536)"
        )
        print(f"    Total layers: {stats['total_layers']:,} | Z range: [{stats['min_z']}, {stats['max_z']}]")
        return 0

    if args.to_path_txt or in_path.suffix.lower() == ".l2j":
        out_path = args.output
        if out_path is None:
            stem = in_path.stem
            out_path = in_path.with_name(f"{stem}_path.txt")
        stats = convert_l2j_to_path_txt(in_path, out_path)
        print(
            f"[+] Converted {in_path.name} -> {out_path.name} "
            f"({stats['total_cells']:,} cells, {stats['total_nodes']:,} nodes, {stats['output_bytes']:,} bytes)"
        )
        return 0

    out_path = args.output
    if out_path is None:
        map_x, map_y = parse_region_coords(in_path.name)
        out_path = in_path.with_name(f"{map_x:02d}_{map_y:02d}.l2j")

    if in_path.name.lower().endswith(".dat"):
        stats = convert_conv_dat_to_l2j(in_path, out_path)
        print(
            f"[+] Converted L2OFF {in_path.name} -> {out_path.name} ({stats['output_bytes']:,} bytes): "
            f"flat={stats['flat_blocks']:,}, complex={stats['complex_blocks']:,}, "
            f"multilayer={stats['multilayer_blocks']:,}"
        )
    else:
        stats = convert_path_txt_to_l2j(
            in_path,
            out_path,
            default_height=args.default_height,
            empty_nswe=args.empty_nswe,
        )
        print(
            f"[+] Converted ///buildzone {in_path.name} -> {out_path.name} ({stats['output_bytes']:,} bytes): "
            f"parsed_cells={stats['parsed_cells']:,}, empty_cells={stats['empty_cells']:,}, "
            f"flat={stats['flat_blocks']:,}, complex={stats['complex_blocks']:,}, "
            f"multilayer={stats['multilayer_blocks']:,}"
        )

    if args.verify:
        vstats = verify_l2j_file(out_path)
        print(
            f"[+] Verified output {out_path.name}: 65,536 blocks valid, "
            f"total_layers={vstats['total_layers']:,}, Z=[{vstats['min_z']}, {vstats['max_z']}]"
        )

    return 0


if __name__ == "__main__":
    sys.exit(main())
