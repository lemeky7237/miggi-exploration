"""16x16 block textures for the arcane forest and rune ruins, in a clean vanilla-like style.
  python3 make_blocks.py <repo-root>
"""
import os, sys
import numpy as np
from PIL import Image

N = 16
OUT = os.path.join(sys.argv[1], "src/main/resources/assets/miggi_exploration/textures/block")


def rng(seed):
    return np.random.default_rng(seed)


def c(*v):
    return np.array(v, np.float32)


def save(name, rgb, alpha=None):
    a = np.full((N, N), 255, np.uint8) if alpha is None else alpha.astype(np.uint8)
    img = np.dstack([np.clip(rgb, 0, 255).astype(np.uint8), a])
    Image.fromarray(img, "RGBA").save(os.path.join(OUT, name + ".png"))


def palette_pick(levels, cols):
    """levels: int array of indices into cols."""
    cols = np.array(cols, np.float32)
    return cols[np.clip(levels, 0, len(cols) - 1)]


# ------------------------------------------------------------------ wood
BARK = [(38, 28, 50), (56, 42, 70), (74, 58, 92), (92, 74, 112), (112, 92, 132)]
WOOD = [(96, 74, 120), (118, 92, 144), (140, 112, 164), (160, 132, 182)]


def bark(seed, mossy=False):
    r = rng(seed)
    lv = np.zeros((N, N), int)
    for x in range(N):
        base = r.integers(1, 4)
        col_ = np.full(N, base)
        # vertical ridges with occasional breaks
        for y in range(N):
            if r.random() < 0.18:
                base = int(np.clip(base + r.choice([-1, 1]), 1, 4))
            col_[y] = base
        lv[:, x] = col_
    # dark furrows every 3-4 columns
    for x in range(0, N, 4):
        xs = (x + r.integers(0, 2)) % N
        for y in range(N):
            if r.random() < 0.8:
                lv[y, xs] = 0
    rgb = palette_pick(lv, BARK)
    # glowing veins
    for _ in range(2):
        x, y = r.integers(1, N - 1), r.integers(0, N - 4)
        for k in range(r.integers(2, 4)):
            rgb[(y + k) % N, x] = (96, 214, 214) if k % 2 == 0 else (70, 170, 180)
    if mossy:
        m = r.random((N, N)) < 0.22
        rgb[m] = rgb[m] * 0.4 + c(70, 110, 60) * 0.6
    return rgb


def log_top(seed):
    r = rng(seed)
    y, x = np.mgrid[0:N, 0:N]
    d = np.sqrt((x - 7.5) ** 2 + (y - 7.5) ** 2)
    rings = (d + r.random((N, N)) * 0.6).astype(int) % 3
    rgb = palette_pick(rings + 1, WOOD)
    edge = np.maximum(np.abs(x - 7.5), np.abs(y - 7.5)) > 6.6
    rgb[edge] = palette_pick(r.integers(1, 4, (N, N)), BARK)[edge]
    center = d < 1.2
    rgb[center] = WOOD[0]
    return rgb


def planks(seed):
    r = rng(seed)
    rgb = np.zeros((N, N, 3), np.float32)
    for row in range(4):
        y0 = row * 4
        shade = r.integers(1, 3)
        for y in range(y0, y0 + 4):
            for x in range(N):
                g = shade + (1 if r.random() < 0.15 else 0) - (1 if r.random() < 0.1 else 0)
                rgb[y, x] = WOOD[int(np.clip(g, 0, 3))]
        rgb[y0 + 3, :] = (70, 54, 90)                        # seam under each plank
        cut = (row * 7 + r.integers(0, 4)) % N               # end joint
        rgb[y0:y0 + 3, cut] = (80, 62, 100)
        for _ in range(2):                                   # nail holes / knots
            rgb[y0 + 1, r.integers(0, N)] = (84, 66, 104)
    return rgb


def leaves(seed):
    r = rng(seed)
    cols = [(34, 92, 100), (46, 122, 128), (62, 150, 150), (86, 176, 170), (120, 204, 192)]
    lv = np.clip((r.random((N, N)) * 3.2 + 0.8).astype(int), 0, 4)
    # clumpy shading: blur a coarse noise
    coarse = r.random((5, 5))
    from scipy import ndimage
    big = ndimage.zoom(coarse, 16 / 5, order=1)[:N, :N]
    lv = np.clip((lv + (big - 0.5) * 3).astype(int), 0, 4)
    rgb = palette_pick(lv, cols)
    alpha = np.where(r.random((N, N)) < 0.24, 0, 255)
    # little glowing buds
    for _ in range(3):
        x, y = r.integers(0, N), r.integers(0, N)
        rgb[y, x] = (210, 250, 240)
        alpha[y, x] = 255
    return rgb, alpha


# ------------------------------------------------------------------ stone
STONE = [(62, 66, 78), (84, 88, 100), (104, 108, 120), (122, 126, 138), (140, 144, 156)]
MORTAR = (50, 52, 62)


def bricks(seed, cracked=False, mossy=False):
    r = rng(seed)
    lv = np.clip((r.random((N, N)) * 2.6 + 1.2).astype(int), 0, 4)
    rgb = palette_pick(lv, STONE)
    # brick layout like stone bricks: 2 rows of 8px, offset
    for y in (7, 15):
        rgb[y, :] = MORTAR
    for x in (0, 8):
        rgb[0:7, (x + 7) % N] = MORTAR
    for x in (4, 12):
        rgb[8:15, (x + 7) % N] = MORTAR
    # bevel: light top-left edge of each brick, dark bottom
    for y0 in (0, 8):
        rgb[y0, :] = np.minimum(rgb[y0, :] * 1.15, 255)
        rgb[y0 + 6, :] = rgb[y0 + 6, :] * 0.85
    if cracked:
        x, y = 3, 1
        for _ in range(18):
            rgb[y % N, x % N] = (40, 42, 50)
            if r.random() < 0.5:
                x += r.choice([-1, 1])
            else:
                y += 1
        for p in [(11, 9), (12, 10), (12, 11), (13, 12), (11, 12)]:
            rgb[p[1], p[0]] = (40, 42, 50)
    if mossy:
        m = np.zeros((N, N), bool)
        for _ in range(5):
            cx, cy = r.integers(0, N), r.integers(0, N)
            y, x = np.mgrid[0:N, 0:N]
            m |= ((x - cx) ** 2 + (y - cy) ** 2) < r.integers(4, 12)
        moss = palette_pick(r.integers(0, 3, (N, N)), [(60, 92, 44), (78, 112, 52), (96, 132, 62)])
        rgb[m] = moss[m]
    return rgb


GLYPH = [  # 8x8 rune, placed in the middle
    "...##...",
    "..#..#..",
    "..#..#..",
    "...##...",
    "#..##..#",
    ".#.##.#.",
    "..####..",
    "...##...",
]


def rune(rgb, color, glow):
    for gy, row in enumerate(GLYPH):
        for gx, ch in enumerate(row):
            if ch == "#":
                y, x = 4 + gy, 4 + gx
                rgb[y, x] = color
                if glow:
                    for dy, dx in ((0, 1), (1, 0), (0, -1), (-1, 0)):
                        yy, xx = y + dy, x + dx
                        if GLYPH[min(7, max(0, gy + dy))][min(7, max(0, gx + dx))] != "#":
                            rgb[yy, xx] = rgb[yy, xx] * 0.55 + np.array(color) * 0.45
    return rgb


def pillar(seed):
    r = rng(seed)
    lv = np.clip((r.random((N, N)) * 2.4 + 1.4).astype(int), 0, 4)
    rgb = palette_pick(lv, STONE)
    rgb[:, 0] = rgb[:, 0] * 0.75
    rgb[:, 15] = rgb[:, 15] * 0.75
    for y in (0, 1, 14, 15):                      # carved bands top and bottom
        rgb[y, :] = STONE[3] if y in (0, 15) else MORTAR
    for y in range(4, 12):                        # vertical flutes
        for x in (4, 8, 12):
            rgb[y, x] = STONE[0]
    return rgb


def altar_top(seed):
    rgb = bricks(seed)
    y, x = np.mgrid[0:N, 0:N]
    d = np.sqrt((x - 7.5) ** 2 + (y - 7.5) ** 2)
    ring = (d > 5.2) & (d < 6.4)
    rgb[ring] = (110, 230, 230)
    rgb[(d > 4.4) & (d <= 5.2)] = rgb[(d > 4.4) & (d <= 5.2)] * 0.6
    rgb = rune(rgb, (150, 245, 240), glow=False)
    return rgb


def main():
    save("arcane_log", bark(1))
    save("arcane_log_top", log_top(2))
    save("ancient_bark", bark(3, mossy=True))
    save("arcane_planks", planks(4))
    rgb, a = leaves(5)
    save("arcane_leaves", rgb, a)
    save("rune_stone", bricks(6))
    save("cracked_rune_stone", bricks(7, cracked=True))
    save("mossy_rune_stone", bricks(8, mossy=True))
    save("glowing_rune_stone", rune(bricks(9) * 0.8, (120, 245, 235), glow=True))
    save("rune_pillar", pillar(10))
    save("rune_altar", altar_top(11))


if __name__ == "__main__":
    main()
