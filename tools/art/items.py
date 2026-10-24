"""Item icons (64x64) for monster drops and ruin loot, rendered with lighting like the GUI art.
  python3 items.py <repo-root>
"""
import math, os, sys
import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from scipy import ndimage
from lib import *
from elements import metal_layer

N = 256
OUT = os.path.join(sys.argv[1], "src/main/resources/assets/miggi_exploration/textures/item")


def canvas():
    return Image.new("RGBA", (N, N), (0, 0, 0, 0))


def coords():
    y, x = np.mgrid[0:N, 0:N].astype(np.float32)
    return x, y


def glow(img, rgb, radius, strength=0.8):
    a = np.asarray(img.split()[3], np.float32) / 255
    g = blur(a, radius) * strength
    out = np.zeros((N, N, 4), np.float32)
    out[..., :3] = color(rgb)
    out[..., 3] = np.clip(g, 0, 1)
    base = Image.fromarray((out * 255).astype(np.uint8), "RGBA")
    base.alpha_composite(img)
    return base


def finish(big, size=64):
    a = np.asarray(big.split()[3], np.float32) / 255
    rim = np.clip(ndimage.maximum_filter(a, size=11) - a, 0, 1)
    out = np.zeros((N, N, 4), np.float32)
    out[..., :3] = color((30, 18, 22))
    out[..., 3] = rim * 0.6
    under = Image.fromarray((out * 255).astype(np.uint8), "RGBA")
    under.alpha_composite(big)
    return under.resize((size, size), Image.LANCZOS)


def arcane_shard():
    img = canvas()
    pts = [(N * .52, N * .06), (N * .74, N * .34), (N * .64, N * .92), (N * .40, N * .92), (N * .28, N * .38)]
    c = (N * .50, N * .52)
    facets = [(170, 140, 250), (110, 90, 220), (210, 190, 255), (80, 60, 180), (140, 120, 240)]
    for i in range(len(pts)):
        m = mask_from((N, N), lambda d: d.polygon([c, pts[i], pts[(i + 1) % len(pts)]], fill=255))
        x, y = coords()
        t = np.clip((y - N * .06) / (N * .86), 0, 1)
        al = ramp(t, [(0, (240, 230, 255)), (0.4, facets[i]), (1, (50, 30, 120))])
        img.alpha_composite(Image.fromarray((np.dstack([al, m]) * 255).astype(np.uint8), "RGBA"))
    d = ImageDraw.Draw(img)
    d.line([pts[0], c], fill=(255, 255, 255, 200), width=4)
    d.line([(N * .44, N * .26), (N * .40, N * .70)], fill=(255, 255, 255, 160), width=5)
    return glow(img, (190, 150, 255), 12, 0.8)


def goblin_talisman():
    img = canvas()
    x, y = coords()
    c, R = N / 2, N * .40
    # cord loop
    cm = mask_from((N, N), lambda d: d.arc([c - R * .45, N * .02, c + R * .45, N * .36], 180, 360, fill=255, width=10))
    comp(img, shade(dome(cm, .5), ramp(dome(cm, .5), [(0, (70, 40, 20)), (1, (140, 96, 50))]), cm, strength=10), None)
    disk = mask_from((N, N), lambda d: d.ellipse([c - R, c - R + 16, c + R, c + R + 16], fill=255))
    r = np.sqrt((x - c) ** 2 + (y - c - 16) ** 2) / R
    h = np.where(r > .78, 1 - (r - .78) * 3, .6) + fbm(N, N, 5, 3, 7) * .06
    comp(img, metal_layer(disk, h, seed=3, palette="brass", strength=22), dict(offset=(2, 5), radius=4, opacity=.55))
    # bone teeth around the rim
    for k in range(8):
        a = k * math.pi / 4
        tx, ty = c + math.cos(a) * R * .9, c + 16 + math.sin(a) * R * .9
        tm = mask_from((N, N), lambda d: d.ellipse([tx - 9, ty - 9, tx + 9, ty + 9], fill=255))
        comp(img, shade(dome(tm, .5), np.dstack([np.full((N, N), .88), np.full((N, N), .84), np.full((N, N), .72)]), tm, strength=12, spec=.4), None)
    # green eye gem
    em = mask_from((N, N), lambda d: d.ellipse([c - R * .45, c + 16 - R * .3, c + R * .45, c + 16 + R * .3], fill=255))
    al = ramp(dome(em, .6), [(0, (20, 70, 20)), (0.6, (80, 180, 60)), (1, (190, 250, 140))])
    comp(img, shade(dome(em, .6), al, em, strength=20, spec=1, shininess=40), None)
    pm = mask_from((N, N), lambda d: d.ellipse([c - 7, c + 16 - R * .26, c + 7, c + 16 + R * .26], fill=255))
    img.alpha_composite(shade(np.zeros((N, N)), np.dstack([np.full((N, N), .05)] * 3), pm, ambient=1, diffuse=0))
    return img


def curved(img, path, widths, cols, ridges=True, seed=1):
    """Draws a tapering curved shape along a path with lighting (horns, fangs)."""
    m = Image.new("L", (N, N), 0)
    d = ImageDraw.Draw(m)
    tmap = np.zeros((N, N), np.float32)
    tm = Image.new("L", (N, N), 0)
    td = ImageDraw.Draw(tm)
    for i in range(len(path) - 1):
        t = i / (len(path) - 1)
        w = widths[0] * (1 - t) + widths[1] * t
        (x0, y0), (x1, y1) = path[i], path[i + 1]
        d.line([(x0, y0), (x1, y1)], fill=255, width=int(w))
        d.ellipse([x1 - w / 2, y1 - w / 2, x1 + w / 2, y1 + w / 2], fill=255)
        td.line([(x0, y0), (x1, y1)], fill=int(t * 255), width=int(w) + 2)
    mask = np.asarray(m, np.float32) / 255
    t = blur(np.asarray(tm, np.float32) / 255, 2)
    al = ramp(t, cols)
    if ridges:
        x, y = coords()
        rings = (np.sin(t * 60) * .5 + .5)
        al = al * (0.88 + 0.12 * rings[..., None])
    layer = shade(dome(mask, .55), al, mask, strength=26, spec=.5, shininess=30, ambient=.55)
    comp(img, layer, dict(offset=(2, 5), radius=4, opacity=.5))


def arc_path(cx, cy, r, a0, a1, n=40):
    return [(cx + math.cos(a0 + (a1 - a0) * i / n) * r, cy + math.sin(a0 + (a1 - a0) * i / n) * r) for i in range(n + 1)]


def minotaur_horn():
    img = canvas()
    path = arc_path(N * .62, N * .62, N * .40, math.pi * 1.0, math.pi * 1.62)
    curved(img, path, (58, 6), [(0, (120, 96, 72)), (0.15, (226, 210, 176)), (0.7, (200, 180, 140)), (1, (60, 48, 38))])
    # bronze band near the base
    bx, by = path[5]
    bm = mask_from((N, N), lambda d: d.line([(bx - 4, by - 30), (bx + 6, by + 30)], fill=255, width=12))
    comp(img, metal_layer(bm, dome(bm, .5), seed=5, palette="brass", strength=14), None)
    return img


def orc_fang():
    img = canvas()
    # leather cord
    cm = mask_from((N, N), lambda d: d.arc([N * .18, N * .02, N * .82, N * .5], 200, 340, fill=255, width=8))
    comp(img, shade(dome(cm, .5), ramp(dome(cm, .5), [(0, (60, 34, 18)), (1, (130, 84, 44))]), cm, strength=10), None)
    path = arc_path(N * .30, N * .40, N * .46, -0.35, 0.95)
    curved(img, path, (52, 4), [(0, (150, 110, 80)), (0.18, (236, 226, 196)), (0.8, (226, 214, 180)), (1, (180, 160, 120))])
    return img


def ancient_tablet():
    img = canvas()
    x, y = coords()
    sm = mask_from((N, N), lambda d: d.polygon([(N * .18, N * .12), (N * .78, N * .08), (N * .84, N * .52), (N * .76, N * .9),
                                                (N * .24, N * .92), (N * .14, N * .56)], fill=255))
    n = fbm(N, N, 10, 4, 3)
    h = bevel(sm, 12) * .8 + n * .25
    al = ramp(n, [(0, (70, 74, 84)), (0.5, (112, 116, 128)), (1, (150, 154, 164))])
    comp(img, shade(h, al, sm, strength=24, spec=.12, ambient=.5), dict(offset=(3, 6), radius=5, opacity=.55))
    # glowing rune lines
    gm = Image.new("L", (N, N), 0)
    d = ImageDraw.Draw(gm)
    for i, yy in enumerate((.25, .42, .59, .76)):
        xs = N * (.28 + (i % 2) * .04)
        d.line([(xs, N * yy), (N * .68, N * yy)], fill=255, width=6)
        for k in range(4):
            x0 = xs + k * N * .1
            d.line([(x0, N * yy - 12), (x0 + 8, N * yy + 10)], fill=255, width=4)
    g = np.asarray(gm, np.float32) / 255 * (np.asarray(sm.astype(np.float32)) if isinstance(sm, np.ndarray) else 1)
    gl = np.zeros((N, N, 4), np.float32)
    gl[..., :3] = color((110, 240, 230))
    gl[..., 3] = np.clip(blur(g, 3) * 0.8 + g, 0, 1)
    img.alpha_composite(Image.fromarray((gl * 255).astype(np.uint8), "RGBA"))
    # crack
    cm = mask_from((N, N), lambda d: d.line([(N * .62, N * .08), (N * .55, N * .3), (N * .63, N * .48), (N * .57, N * .62)], fill=255, width=4))
    arr = np.asarray(img, np.float32)
    arr[..., :3] *= (1 - cm * .7)[..., None]
    return Image.fromarray(arr.astype(np.uint8), "RGBA")


ITEMS = {"arcane_shard": arcane_shard, "goblin_talisman": goblin_talisman, "minotaur_horn": minotaur_horn,
         "orc_fang": orc_fang, "ancient_tablet": ancient_tablet}

if __name__ == "__main__":
    for name, fn in ITEMS.items():
        finish(fn()).save(os.path.join(OUT, name + ".png"))
        print(name)
