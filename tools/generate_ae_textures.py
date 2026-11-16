#!/usr/bin/env python3
"""Generate original 32x32 AE-inspired Void Fabricator textures using only stdlib."""

from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1]
BLOCK_OUT = ROOT / "src/main/resources/assets/voidfabricator/textures/blocks/void_fabricator.png"
ITEM_OUT = ROOT / "src/main/resources/assets/voidfabricator/textures/items/machine_recipe_pool_pattern.png"
PREVIEW_OUT = ROOT / "docs/ae-texture-preview.png"

C = {
    "clear": (0, 0, 0, 0),
    "void": (6, 12, 17, 255),
    "edge": (10, 20, 27, 255),
    "dark": (18, 29, 38, 255),
    "panel": (29, 43, 54, 255),
    "panel_hi": (40, 57, 68, 255),
    "steel": (76, 96, 107, 255),
    "steel_hi": (111, 133, 142, 255),
    "trace": (34, 72, 82, 255),
    "cyan_dark": (18, 99, 112, 255),
    "cyan": (40, 207, 220, 255),
    "cyan_hi": (151, 248, 246, 255),
    "violet_dark": (88, 54, 125, 255),
    "violet": (177, 112, 218, 255),
    "violet_hi": (229, 185, 250, 255),
    "card_dark": (48, 62, 70, 255),
    "card": (190, 207, 213, 255),
    "card_hi": (237, 247, 247, 255),
    "white": (232, 255, 255, 255),
}


class Canvas:
    def __init__(self, width, height, color=None):
        self.width = width
        self.height = height
        color = C[color] if isinstance(color, str) else (color or C["clear"])
        self.pixels = [[color for _ in range(width)] for _ in range(height)]

    def set(self, x, y, color):
        if 0 <= x < self.width and 0 <= y < self.height:
            self.pixels[y][x] = C[color] if isinstance(color, str) else color

    def rect(self, x, y, width, height, color):
        for py in range(y, y + height):
            for px in range(x, x + width):
                self.set(px, py, color)

    def frame(self, x, y, width, height, color):
        self.rect(x, y, width, 1, color)
        self.rect(x, y + height - 1, width, 1, color)
        self.rect(x, y, 1, height, color)
        self.rect(x + width - 1, y, 1, height, color)

    def hline(self, x, y, length, color):
        self.rect(x, y, length, 1, color)

    def vline(self, x, y, length, color):
        self.rect(x, y, 1, length, color)

    def diamond(self, cx, cy, radius, color):
        for y in range(cy - radius, cy + radius + 1):
            span = radius - abs(y - cy)
            self.hline(cx - span, y, span * 2 + 1, color)

    def paste(self, other, x, y):
        for py in range(other.height):
            for px in range(other.width):
                color = other.pixels[py][px]
                if color[3]:
                    self.set(x + px, y + py, color)

    def scaled(self, factor):
        result = Canvas(self.width * factor, self.height * factor)
        for y in range(self.height):
            for x in range(self.width):
                result.rect(x * factor, y * factor, factor, factor, self.pixels[y][x])
        return result


def make_block():
    c = Canvas(32, 32, "panel")
    c.frame(0, 0, 32, 32, "void")
    c.frame(1, 1, 30, 30, "steel")
    c.hline(2, 2, 28, "steel_hi")
    c.vline(2, 2, 28, "steel_hi")
    c.hline(2, 29, 28, "edge")
    c.vline(29, 2, 28, "edge")
    c.frame(3, 3, 26, 26, "dark")

    # Recessed side panels and stepped AE-style circuit routes.
    c.rect(4, 4, 24, 3, "panel_hi")
    c.rect(4, 25, 24, 3, "dark")
    for x in (6, 10, 21, 25):
        c.vline(x, 5, 3, "trace")
    for x in (7, 12, 19, 24):
        c.vline(x, 24, 3, "trace")
    c.hline(4, 11, 6, "trace")
    c.hline(22, 20, 6, "trace")
    c.vline(6, 11, 5, "trace")
    c.vline(25, 16, 5, "trace")
    c.hline(4, 20, 4, "cyan_dark")
    c.hline(24, 11, 4, "cyan_dark")
    c.set(4, 20, "cyan")
    c.set(27, 11, "cyan")

    # Central fabrication chamber.
    c.rect(8, 8, 16, 16, "void")
    c.frame(8, 8, 16, 16, "cyan_dark")
    c.frame(9, 9, 14, 14, "dark")
    c.rect(10, 10, 12, 12, "edge")
    for x, y in ((8, 8), (23, 8), (8, 23), (23, 23)):
        c.rect(x, y, 2, 2, "cyan_hi")
    c.hline(4, 15, 5, "cyan_dark")
    c.hline(23, 16, 5, "cyan_dark")
    c.vline(15, 4, 5, "cyan_dark")
    c.vline(16, 23, 5, "cyan_dark")
    c.diamond(16, 16, 6, "cyan_dark")
    c.diamond(16, 16, 5, "violet_dark")
    c.diamond(16, 16, 4, "violet")
    c.diamond(16, 16, 2, "cyan")
    c.set(16, 16, "white")
    c.set(15, 15, "cyan_hi")
    c.set(17, 17, "violet_hi")

    # Corner fasteners and restrained status lights.
    for x, y in ((3, 3), (28, 3), (3, 28), (28, 28)):
        c.set(x, y, "steel_hi")
    c.rect(5, 5, 2, 1, "cyan")
    c.rect(25, 26, 2, 1, "violet")
    return c


def make_pattern():
    c = Canvas(32, 32)
    # Cut-corner titanium card silhouette.
    c.rect(4, 2, 23, 28, "void")
    c.rect(2, 4, 28, 23, "void")
    c.rect(3, 3, 26, 26, "void")
    c.rect(4, 3, 23, 26, "card_dark")
    c.rect(3, 5, 26, 21, "card_dark")
    c.rect(5, 4, 21, 23, "card")
    c.rect(4, 6, 23, 18, "card")
    c.hline(5, 4, 20, "card_hi")
    c.vline(4, 6, 17, "card_hi")
    c.hline(5, 26, 20, "steel")
    c.vline(26, 6, 18, "steel")

    # Cyan encoding strip and violet processor package.
    c.rect(6, 6, 19, 4, "cyan_dark")
    c.hline(7, 6, 17, "cyan")
    for x in (8, 12, 16, 20):
        c.vline(x, 7, 2, "cyan_hi")
    c.rect(20, 12, 5, 6, "violet_dark")
    c.frame(20, 12, 5, 6, "violet")
    c.set(22, 14, "violet_hi")
    c.set(23, 15, "white")

    # Encoded 3x3 recipe matrix flowing into a processing arrow.
    for y in range(3):
        for x in range(3):
            color = "cyan" if (x + y) % 2 == 0 else "steel"
            c.rect(7 + x * 4, 12 + y * 4, 2, 2, color)
    c.hline(17, 21, 5, "cyan_dark")
    c.hline(18, 22, 4, "cyan")
    c.set(22, 21, "cyan")
    c.set(22, 23, "cyan")
    c.set(23, 22, "cyan_hi")

    # Contact pads make the item read as a modern AE encoded medium.
    for x in (7, 11, 15, 19, 23):
        c.rect(x, 27, 2, 2, "cyan_dark")
        c.set(x, 27, "cyan")
    c.set(5, 5, "white")
    return c


def png_chunk(kind, data):
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)


def save_png(path, canvas):
    path.parent.mkdir(parents=True, exist_ok=True)
    raw = bytearray()
    for row in canvas.pixels:
        raw.append(0)
        for pixel in row:
            raw.extend(pixel)
    header = struct.pack(">2I5B", canvas.width, canvas.height, 8, 6, 0, 0, 0)
    data = b"\x89PNG\r\n\x1a\n" + png_chunk(b"IHDR", header)
    data += png_chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + png_chunk(b"IEND", b"")
    path.write_bytes(data)


def make_preview(block, pattern):
    p = Canvas(640, 360, (10, 17, 24, 255))
    for y in range(360):
        shade = 17 + y * 10 // 359
        p.hline(0, y, 640, (10, shade, shade + 8, 255))
    for x in range(0, 640, 32):
        p.vline(x, 0, 360, (18, 34, 43, 255))
    for y in range(0, 360, 32):
        p.hline(0, y, 640, (18, 34, 43, 255))
    p.frame(28, 28, 280, 304, C["cyan_dark"])
    p.frame(332, 28, 280, 304, C["violet_dark"])
    p.rect(40, 40, 256, 256, C["void"])
    block_large = block.scaled(8)
    p.paste(block_large, 40, 40)
    # Checkerboard exposes the pattern's real transparent background.
    for y in range(32):
        for x in range(32):
            color = (29, 42, 51, 255) if (x // 2 + y // 2) % 2 == 0 else (20, 30, 38, 255)
            p.rect(344 + x * 8, 40 + y * 8, 8, 8, color)
    p.paste(pattern.scaled(8), 344, 40)
    p.hline(48, 312, 240, C["cyan"])
    p.hline(352, 312, 240, C["violet"])
    return p


def main():
    block = make_block()
    pattern = make_pattern()
    save_png(BLOCK_OUT, block)
    save_png(ITEM_OUT, pattern)
    save_png(PREVIEW_OUT, make_preview(block, pattern))
    print(f"BLOCK={BLOCK_OUT} 32x32 RGBA")
    print(f"ITEM={ITEM_OUT} 32x32 RGBA transparent")
    print(f"PREVIEW={PREVIEW_OUT} 640x360 RGBA")


if __name__ == "__main__":
    main()
