#!/usr/bin/env python3
"""Validate Void Fabricator textures, tooltip localization, and packaged resources."""

from pathlib import Path
import re
import struct
import sys
import zipfile
import zlib

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/voidfabricator"
BLOCK_TEXTURE = ASSETS / "textures/blocks/void_fabricator.png"
ITEM_TEXTURE = ASSETS / "textures/items/machine_recipe_pool_pattern.png"
PREVIEW = ROOT / "docs/ae-texture-preview.png"
JAR = ROOT / "build/libs/VoidFabricator-0.2.0-alpha.jar"


def read_png(path, expected_size):
    data = path.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError(f"{path}: invalid PNG signature")
    width, height, depth, color_type, compression, filtering, interlace = struct.unpack(">2I5B", data[16:29])
    if (width, height) != expected_size or depth != 8 or color_type != 6 or interlace != 0:
        raise ValueError(f"{path}: expected {expected_size}, 8-bit non-interlaced RGBA")
    offset = 8
    compressed = bytearray()
    while offset < len(data):
        length = struct.unpack(">I", data[offset:offset + 4])[0]
        kind = data[offset + 4:offset + 8]
        payload = data[offset + 8:offset + 8 + length]
        crc = struct.unpack(">I", data[offset + 8 + length:offset + 12 + length])[0]
        if zlib.crc32(kind + payload) & 0xFFFFFFFF != crc:
            raise ValueError(f"{path}: CRC mismatch in {kind!r}")
        if kind == b"IDAT":
            compressed.extend(payload)
        offset += length + 12
    raw = zlib.decompress(bytes(compressed))
    stride = width * 4
    if len(raw) != height * (stride + 1):
        raise ValueError(f"{path}: invalid RGBA data length")
    alpha = bytearray()
    for y in range(height):
        row = raw[y * (stride + 1):(y + 1) * (stride + 1)]
        if row[0] != 0:
            raise ValueError(f"{path}: generator must use PNG filter 0")
        alpha.extend(row[4::4])
    print(f"PNG_OK={path.name} {width}x{height} RGBA alpha={min(alpha)}..{max(alpha)}")
    return alpha


def language_keys(path):
    return {
        line.split("=", 1)[0]
        for line in path.read_text(encoding="utf-8").splitlines()
        if line and not line.startswith("#")
    }


def required_tooltip_keys():
    java = ROOT / "src/main/java/com/wztwzt/voidfabricator/item"
    required = set()
    add_call = re.compile(r'add\(\s*tooltip,\s*"([^"]+)"', re.S)
    direct = re.compile(r'translateToLocal\("([^"]+)"\)')
    for prefix, name in (("block", "ItemBlockVoidFabricator.java"),
                         ("pattern", "ItemMachineRecipePoolPattern.java")):
        source = (java / name).read_text(encoding="utf-8")
        required.update("voidfabricator.tooltip." + prefix + "." + suffix for suffix in add_call.findall(source))
        required.update(direct.findall(source))
    return required


def main():
    failures = []
    block_alpha = read_png(BLOCK_TEXTURE, (32, 32))
    item_alpha = read_png(ITEM_TEXTURE, (32, 32))
    read_png(PREVIEW, (640, 360))
    if min(block_alpha) != 255:
        failures.append("Block texture must be fully opaque")
    if min(item_alpha) != 0 or max(item_alpha) != 255:
        failures.append("Pattern texture needs both transparent and opaque pixels")

    zh = language_keys(ASSETS / "lang/zh_CN.lang")
    en = language_keys(ASSETS / "lang/en_US.lang")
    required = required_tooltip_keys()
    for name, keys in (("zh_CN", zh), ("en_US", en)):
        missing = sorted(required - keys)
        print(f"LANG_{name}_TOOLTIP_KEYS={len(required & keys)}/{len(required)} MISSING={missing}")
        failures.extend(f"{name} missing {key}" for key in missing)
    if zh != en:
        failures.append(f"Language key mismatch: {sorted(zh ^ en)}")

    if JAR.exists():
        with zipfile.ZipFile(JAR) as jar:
            entries = set(jar.namelist())
        packaged = {
            "com/wztwzt/voidfabricator/item/ItemBlockVoidFabricator.class",
            "assets/voidfabricator/textures/blocks/void_fabricator.png",
            "assets/voidfabricator/textures/items/machine_recipe_pool_pattern.png",
            "assets/voidfabricator/lang/zh_CN.lang",
            "assets/voidfabricator/lang/en_US.lang",
        }
        missing = sorted(packaged - entries)
        print(f"JAR_RESOURCE_CHECK={'OK' if not missing else 'MISSING=' + str(missing)}")
        failures.extend(f"Jar missing {entry}" for entry in missing)
        if not missing:
            with zipfile.ZipFile(JAR) as jar:
                if jar.read("assets/voidfabricator/textures/blocks/void_fabricator.png") != BLOCK_TEXTURE.read_bytes():
                    failures.append("Packaged block texture differs from source")
                if jar.read("assets/voidfabricator/textures/items/machine_recipe_pool_pattern.png") != ITEM_TEXTURE.read_bytes():
                    failures.append("Packaged item texture differs from source")

    if failures:
        for failure in failures:
            print("FAIL=" + failure)
        return 1
    print("ASSET_VALIDATION=PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
