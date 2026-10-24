import math, sys
import numpy as np
from PIL import Image, ImageDraw
from lib import *
from elements import metal_layer

OUT = sys.argv[1]
U = 4
W, H = 320 * U, 80 * U


def torn_mask(x0, x1, y0, y1, seed):
    r = rng(seed)
    h, w = H, W
    xs = np.arange(w)
    jag_top = (fbm(1, w, scale=36, octaves=3, seed=seed, persistence=0.4)[0] - 0.5) * 6 * U
    jag_bot = (fbm(1, w, scale=36, octaves=3, seed=seed + 1, persistence=0.4)[0] - 0.5) * 6 * U
    ys = np.arange(h)[:, None]
    m = ((ys >= y0 + jag_top[None, :]) & (ys <= y1 + jag_bot[None, :]) & (xs[None, :] >= x0) & (xs[None, :] <= x1))
    return m.astype(np.float32)


def parchment(mask, seed):
    h, w = mask.shape
    fib = fbm(h, w, scale=2, octaves=2, seed=seed)
    mid = fbm(h, w, scale=40, octaves=4, seed=seed + 1)
    blot = blur(np.clip((fbm(h, w, scale=90, octaves=3, seed=seed + 2) - 0.55) * 3, 0, 1), 5)
    d = dist_inside(mask)
    burn = np.clip(1 - d / (10 * U), 0, 1) ** 1.8
    al = ramp(mid * 0.55 + fib * 0.2 + 0.2, [(0, (214, 170, 108)), (0.5, (236, 202, 146)), (1, (248, 226, 180))])
    al = al * (1 - 0.08 * blot[..., None])
    al = al * (1 - 0.5 * burn[..., None]) + burn[..., None] * color((110, 60, 18)) * 0.22
    # waviness of the unrolled sheet
    wave = np.sin(np.arange(w) / (26 * U) * 2 * math.pi)[None, :] * 0.5 + fbm(h, w, scale=60, octaves=2, seed=seed + 3) * 0.6
    height = wave * 0.35 + fib * 0.05
    return shade(height, np.clip(al, 0, 1), mask, strength=10, spec=0.05, ambient=0.66, diffuse=0.4)


def roll(cx, y0, y1, seed):
    """rolled paper end: a vertical cylinder."""
    m = mask_from((W, H), lambda d: d.rounded_rectangle([cx - 11 * U, y0, cx + 11 * U, y1], 10 * U, fill=255))
    xs = (np.arange(W)[None, :] - cx) / (11 * U)
    cyl = np.sqrt(np.clip(1 - xs ** 2, 0, 1)).repeat(H, 0)
    fib = fbm(H, W, scale=2, octaves=2, seed=seed)
    spiral = (np.sin(xs * 9) * 0.5 + 0.5) * 0.15
    al = ramp(cyl * 0.7 + fib * 0.2 + 0.1, [(0, (150, 100, 50)), (0.5, (222, 182, 124)), (1, (246, 220, 172))])
    al = al * (1 - spiral[..., None])
    return shade(cyl * 1.2, np.clip(al, 0, 1), m, strength=14, spec=0.08, ambient=0.6, diffuse=0.5)


def rod(cx, seed):
    def shape(d):
        d.rounded_rectangle([cx - 4.5 * U, 5 * U, cx + 4.5 * U, H - 5 * U], 2 * U, fill=255)
        for ty, s in ((7 * U, -1), (H - 7 * U, 1)):
            d.ellipse([cx - 8 * U, ty - 3.5 * U, cx + 8 * U, ty + 3.5 * U], fill=255)
            d.ellipse([cx - 6 * U, ty + s * 3 * U - 3 * U, cx + 6 * U, ty + s * 3 * U + 3 * U], fill=255)
            d.polygon([(cx - 4 * U, ty + s * 4 * U), (cx, ty + s * 14 * U), (cx + 4 * U, ty + s * 4 * U)], fill=255)
    m = mask_from((W, H), shape)
    xs = (np.arange(W)[None, :] - cx) / (8 * U)
    cyl = np.sqrt(np.clip(1 - xs ** 2, 0, 1)).repeat(H, 0)
    rings = np.zeros((H, W), np.float32)
    for ty in (7 * U, H - 7 * U, 12 * U, H - 12 * U):
        rings += np.exp(-((np.arange(H)[:, None] - ty) / (1.2 * U)) ** 2)
    hgt = cyl * 1.0 + rings * 0.3 + bevel(m, 1.5 * U) * 0.3
    return metal_layer(m, hgt, seed=seed, palette="brass", strength=14)


def main():
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    x0, x1 = 34 * U, W - 34 * U
    y0, y1 = 12 * U, H - 12 * U
    sheet = torn_mask(x0, x1, y0, y1, seed=3)
    comp(img, parchment(sheet, 4), dict(offset=(0, 3 * U), radius=4 * U, opacity=0.55))
    for i, cx in enumerate((x0, x1)):
        comp(img, roll(cx, y0 - 5 * U, y1 + 5 * U, 7 + i), dict(offset=(0, 2 * U), radius=3 * U, opacity=0.5))
    for i, cx in enumerate((x0, x1)):
        comp(img, rod(cx, 20 + i), dict(offset=(U, 2 * U), radius=2 * U, opacity=0.55))
    final = img.resize((640, 160), Image.LANCZOS)
    canvas = Image.new("RGBA", (1024, 256), (0, 0, 0, 0))
    canvas.paste(final, (0, 0))
    canvas.save(f"{OUT}/discovery_scroll.png")


if __name__ == "__main__":
    main()
