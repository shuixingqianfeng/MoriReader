#!/usr/bin/env python3
"""Fail CI when an Android screencap contains no visible app content."""

import struct
import sys
import zlib


def paeth(a: int, b: int, c: int) -> int:
    estimate = a + b - c
    da, db, dc = abs(estimate - a), abs(estimate - b), abs(estimate - c)
    return a if da <= db and da <= dc else b if db <= dc else c


def decode_png(path: str) -> tuple[int, int, int, bytes]:
    data = open(path, "rb").read()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("screencap is not a PNG")

    position = 8
    compressed = bytearray()
    width = height = color_type = 0
    while position < len(data):
        length = struct.unpack(">I", data[position : position + 4])[0]
        kind = data[position + 4 : position + 8]
        payload = data[position + 8 : position + 8 + length]
        position += length + 12
        if kind == b"IHDR":
            width, height, depth, color_type, _, _, interlace = struct.unpack(">IIBBBBB", payload)
            if depth != 8 or color_type not in (2, 6) or interlace != 0:
                raise ValueError("unsupported Android screencap PNG format")
        elif kind == b"IDAT":
            compressed.extend(payload)
        elif kind == b"IEND":
            break

    channels = 4 if color_type == 6 else 3
    stride = width * channels
    raw = zlib.decompress(bytes(compressed))
    decoded = bytearray(height * stride)
    source = 0
    for y in range(height):
        filter_type = raw[source]
        source += 1
        row = bytearray(raw[source : source + stride])
        source += stride
        previous = decoded[(y - 1) * stride : y * stride] if y else bytes(stride)
        for index in range(stride):
            left = row[index - channels] if index >= channels else 0
            above = previous[index]
            upper_left = previous[index - channels] if index >= channels else 0
            if filter_type == 1:
                row[index] = (row[index] + left) & 255
            elif filter_type == 2:
                row[index] = (row[index] + above) & 255
            elif filter_type == 3:
                row[index] = (row[index] + ((left + above) // 2)) & 255
            elif filter_type == 4:
                row[index] = (row[index] + paeth(left, above, upper_left)) & 255
            elif filter_type != 0:
                raise ValueError(f"unsupported PNG filter {filter_type}")
        decoded[y * stride : (y + 1) * stride] = row
    return width, height, channels, bytes(decoded)


def main() -> int:
    width, height, channels, pixels = decode_png(sys.argv[1])
    stride = width * channels
    dark = 0
    bright = 0
    sampled = 0
    for y in range(height // 10, height * 9 // 10, 6):
        for x in range(0, width, 6):
            offset = y * stride + x * channels
            red, green, blue = pixels[offset : offset + 3]
            sampled += 1
            if red < 192 and green < 192 and blue < 192:
                dark += 1
            if red > 64 or green > 64 or blue > 64:
                bright += 1
    print(f"screenshot={width}x{height} sampled={sampled} dark={dark} bright={bright}")
    if dark < 40 or bright < 40:
        print("App shell appears blank", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
