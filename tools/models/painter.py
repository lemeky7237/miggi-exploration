"""Face painters for box-UV entity textures. Every painter returns an RGBA float array (h, w, 4).

Face names follow Minecraft's box UV layout:
  top / bottom (w x d), right = -x side (d x h), front = -z (w x h), left = +x side (d x h), back = +z (w x h)
"""
import math
import zlib
import numpy as np
from scipy import ndimage

S = 4  # texture pixels per model unit


def stable_hash(*keys):
    return zlib.crc32(repr(keys).encode()) & 0x7fffffff


def noise(h, w, scale, seed, octaves=3):
    r = np.random.default_rng(seed)
    out = np.zeros((h, w), np.float32)
    amp, tot, s = 1.0, 0.0, float(max(1.0, scale))
    for _ in range(octaves):
        gh, gw = int(h / s) + 3, int(w / s) + 3
        g = r.random((gh, gw)).astype(np.float32)
        z = ndimage.zoom(g, (s, s), order=1)[:h, :w]
        if z.shape != (h, w):
            z = np.pad(z, ((0, h - z.shape[0]), (0, w - z.shape[1])), mode="edge")
        out += amp * z
        tot += amp
        amp *= 0.5
        s = max(1.0, s / 2)
    out /= tot
    lo, hi = out.min(), out.max()
    return (out - lo) / max(1e-6, hi - lo)


def streaks(h, w, seed, along="y", length=6):
    """Hair/fur/grain strands: noise stretched along an axis."""
    r = np.random.default_rng(seed)
    length = max(1, int(length))
    if along == "y":
        base = r.random((max(2, int(h / length) + 2), w)).astype(np.float32)
        z = ndimage.zoom(base, (length, 1), order=1)[:h, :w]
    else:
        base = r.random((h, max(2, int(w / length) + 2))).astype(np.float32)
        z = ndimage.zoom(base, (1, length), order=1)[:h, :w]
    if z.shape != (h, w):
        z = np.pad(z, ((0, h - z.shape[0]), (0, w - z.shape[1])), mode="edge")
    return z


def col(c):
    return np.array(c, np.float32) / 255.0


def mix(a, b, t):
    t = np.clip(t, 0, 1)[..., None]
    return a * (1 - t) + b * t


def edge_ao(h, w, strength=0.22):
    y = np.arange(h)[:, None].repeat(w, 1)
    x = np.arange(w)[None, :].repeat(h, 0)
    d = np.minimum(np.minimum(x, w - 1 - x), np.minimum(y, h - 1 - y)).astype(np.float32)
    return 1 - strength * np.clip(1 - d / (S * 0.75), 0, 1)


def vertical_light(h, w, face):
    if face == "top":
        return np.full((h, w), 1.08, np.float32)
    if face == "bottom":
        return np.full((h, w), 0.76, np.float32)
    return np.linspace(1.06, 0.84, h, dtype=np.float32)[:, None].repeat(w, 1)


class Material:
    def __init__(self, base, dark=None, light=None, kind="skin", grain=6, seed=1, ao=0.22, along="y", spots=None):
        self.base = col(base)
        self.dark = col(dark) if dark else self.base * 0.6
        self.light = col(light) if light else np.clip(self.base * 1.25, 0, 1)
        self.kind, self.grain, self.seed, self.ao, self.along, self.spots = kind, grain, seed, ao, along, spots

    def paint(self, w, h, face, key):
        seed = stable_hash(self.seed, key, face)
        along = ("x" if w > h else "y") if self.along == "auto" else self.along
        k = self.kind
        if k == "skin":
            n = noise(h, w, self.grain, seed)
            fine = noise(h, w, 1.5, seed + 1, 2)
            rgb = mix(self.dark, self.light, n * 0.7 + 0.15)
            rgb = rgb * (0.94 + 0.12 * fine[..., None])
            if self.spots:
                sp = noise(h, w, 6, seed + 7, 2) > 0.8
                rgb = np.where(sp[..., None], rgb * 0.84, rgb)
        elif k == "fur":
            st = streaks(h, w, seed, along=along, length=self.grain * S // 2)
            n = noise(h, w, 6, seed + 3, 2)
            rgb = mix(self.dark, self.light, st * 0.75 + n * 0.25)
            lines = streaks(h, w, seed + 9, along=along, length=self.grain * S) > 0.82
            rgb = np.where(lines[..., None], rgb * 0.72, rgb)
        elif k == "cloth":
            y, x = np.mgrid[0:h, 0:w]
            weave = ((x // 2 + y // 2) % 2).astype(np.float32)
            n = noise(h, w, 5, seed, 2)
            rgb = mix(self.dark, self.light, n * 0.5 + 0.25 + weave * 0.06)
            rgb = rgb * (0.95 + 0.08 * noise(h, w, 1.2, seed + 2, 1)[..., None])
        elif k == "leather":
            n = noise(h, w, 4, seed, 3)
            crack = noise(h, w, 1.0, seed + 1, 1) > 0.9
            rgb = mix(self.dark, self.light, n * 0.6 + 0.2)
            rgb = np.where(crack[..., None], rgb * 0.75, rgb)
            if face not in ("top", "bottom") and h > 3 * S:
                for yy in (S // 2, h - S // 2 - 1):
                    for x0 in range(0, w, S * 2):
                        rgb[yy:yy + 1, x0:x0 + S] = col((206, 176, 126))
        elif k == "wood":
            st = streaks(h, w, seed, along=along, length=max(4, self.grain) * S)
            rgb = mix(self.dark, self.light, st)
            knots = noise(h, w, 2 * S, seed + 5, 1) > 0.9
            rgb = np.where(knots[..., None], rgb * 0.7, rgb)
        elif k == "metal":
            st = streaks(h, w, seed, along=along, length=8 * S)
            if along == "x":
                g = np.linspace(0, 1, h, dtype=np.float32)[:, None].repeat(w, 1)
            else:
                g = np.linspace(0, 1, w, dtype=np.float32)[None, :].repeat(h, 0)
            rgb = mix(self.dark, self.light, 0.35 + 0.35 * st + 0.2 * np.sin(g * math.pi))
            scratch = streaks(h, w, seed + 4, along="y" if along == "x" else "x", length=3 * S) > 0.93
            rgb = np.where(scratch[..., None], np.clip(rgb * 1.35, 0, 1), rgb)
            rust = noise(h, w, 3 * S, seed + 6, 2)
            rgb = mix(rgb, np.broadcast_to(col((120, 70, 40)), rgb.shape), np.clip((rust - 0.75) * 2.5, 0, 0.5))
        elif k == "horn":
            n = noise(h, w, 3, seed, 2)
            if along == "x":
                t = np.linspace(0, 1, w, dtype=np.float32)[None, :].repeat(h, 0)
                rings = np.sin(np.arange(w)[None, :].repeat(h, 0) * 1.2) * 0.5 + 0.5
            else:
                t = np.linspace(0, 1, h, dtype=np.float32)[:, None].repeat(w, 1)
                rings = np.sin(np.arange(h)[:, None].repeat(w, 1) * 1.2) * 0.5 + 0.5
            rgb = mix(self.light, self.dark, t ** 1.6)
            rgb = rgb * (0.9 + 0.08 * rings[..., None] + 0.06 * n[..., None])
        else:  # solid
            n = noise(h, w, 2 * S, seed, 2)
            rgb = mix(self.dark, self.light, 0.4 + 0.3 * n)
        rgb = rgb * vertical_light(h, w, face)[..., None] * edge_ao(h, w, self.ao)[..., None]
        return np.dstack([np.clip(rgb, 0, 1), np.ones((h, w), np.float32)])


# ---------------------------------------------------------------- decor helpers (pixel coordinates)
def disc(img, cx, cy, r, c, alpha=1.0):
    h, w = img.shape[:2]
    y, x = np.mgrid[0:h, 0:w]
    d = np.sqrt((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2)
    a = np.clip(r - d + 0.5, 0, 1) * alpha
    img[..., :3] = img[..., :3] * (1 - a[..., None]) + col(c) * a[..., None]


def ellipse(img, cx, cy, rx, ry, c, alpha=1.0):
    h, w = img.shape[:2]
    y, x = np.mgrid[0:h, 0:w]
    d = np.sqrt(((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2)
    a = np.clip((1 - d) * min(rx, ry) + 0.5, 0, 1) * alpha
    img[..., :3] = img[..., :3] * (1 - a[..., None]) + col(c) * a[..., None]


def rect(img, x0, y0, x1, y1, c, alpha=1.0):
    h, w = img.shape[:2]
    x0, x1 = int(max(0, x0)), int(min(w, x1))
    y0, y1 = int(max(0, y0)), int(min(h, y1))
    if x1 <= x0 or y1 <= y0:
        return
    img[y0:y1, x0:x1, :3] = img[y0:y1, x0:x1, :3] * (1 - alpha) + col(c) * alpha


def darken(img, x0, y0, x1, y1, f):
    h, w = img.shape[:2]
    x0, x1 = int(max(0, x0)), int(min(w, x1))
    y0, y1 = int(max(0, y0)), int(min(h, y1))
    if x1 > x0 and y1 > y0:
        img[y0:y1, x0:x1, :3] *= f


def eye(img, cx, cy, r, iris, pupil=(10, 8, 6), slit=False, glow=False):
    if glow:
        for k in range(4, 0, -1):
            disc(img, cx, cy, r * (1 + k * 0.35), iris, alpha=0.12)
    disc(img, cx, cy, r, (20, 14, 10))
    disc(img, cx, cy, r * 0.82, iris)
    if slit:
        ellipse(img, cx, cy, r * 0.22, r * 0.75, pupil)
    else:
        disc(img, cx, cy, r * 0.4, pupil)
    disc(img, cx - r * 0.3, cy - r * 0.35, r * 0.18, (255, 255, 240), alpha=0.9)
