import math
import numpy as np
from PIL import Image, ImageDraw
from lib import *


def local_canvas(r):
    return int(r * 2 + 8)


def leaf(length, width, ang, seed=0, hue=0.0, light=1.0):
    """Returns (img, (ox, oy)) where (ox,oy) is the stem point inside img."""
    c = local_canvas(length)
    cx = cy = c // 2
    pts = poly_rot(cx, cy, leaf_pts(length, width), ang)
    m = mask_from((c, c), lambda d: d.polygon(pts, fill=255))
    h = dome(m, 0.55) * 1.0
    # veins
    vm = Image.new("L", (c, c), 0)
    vd = ImageDraw.Draw(vm)
    tip = (cx + math.cos(ang) * length * 0.95, cy + math.sin(ang) * length * 0.95)
    vd.line([(cx, cy), tip], fill=255, width=max(2, int(width * 0.06)))
    for i in range(1, 7):
        t = i / 7.5
        bx, by = cx + math.cos(ang) * length * t, cy + math.sin(ang) * length * t
        for s in (1, -1):
            a2 = ang + s * 0.75
            L = width * 0.45 * math.sin(t * math.pi) + width * 0.08
            vd.line([(bx, by), (bx + math.cos(a2) * L, by + math.sin(a2) * L)], fill=180, width=max(1, int(width * 0.025)))
    v = blur(np.asarray(vm, np.float32) / 255.0, 1.0)
    h = h - v * 0.25
    n = fbm(c, c, scale=max(4, width / 3), octaves=4, seed=seed)
    d = dist_inside(m)
    edge = np.clip(1 - d / (width * 0.18), 0, 1)
    base = ramp(n * 0.6 + 0.2 + hue, [(0, (38, 92, 30)), (0.45, (74, 140, 46)), (0.8, (118, 176, 62)), (1, (160, 200, 80))])
    base = base * (1 - 0.35 * edge[..., None]) * light
    base = base + v[..., None] * color((90, 110, 40)) * 0.6
    img = shade(h, np.clip(base, 0, 1), m, strength=width * 0.35, spec=0.18, shininess=30, ambient=0.5)
    return img, (cx, cy)


def flower(r, petal_in=(255, 252, 248), petal_out=(232, 226, 236), center=(244, 190, 60), n=5, rot=0.0, seed=0, blush=None):
    c = local_canvas(r * 1.1)
    cx = cy = c / 2
    img = Image.new("RGBA", (c, c), (0, 0, 0, 0))
    for i in range(n):
        a = rot + i * 2 * math.pi / n
        pts = []
        for k in range(40):
            t = k / 39 * 2 * math.pi
            x = (math.cos(t) + 1) * 0.5 * r * 1.0
            y = math.sin(t) * r * 0.36 * (0.6 + 0.4 * (x / r))
            # notch at tip
            if x > r * 0.92:
                y *= 0.9
            pts.append((x, y))
        p = poly_rot(cx, cy, pts, a)
        m = mask_from((c, c), lambda d: d.polygon(p, fill=255))
        h = dome(m, 0.6)
        dx = np.arange(c)[None, :] - cx
        dy = np.arange(c)[:, None] - cy
        rad = np.clip(np.sqrt(dx * dx + dy * dy) / r, 0, 1)
        al = ramp(rad, [(0, petal_out), (0.35, petal_in), (1, petal_in)])
        if blush is not None:
            al = al * (1 - 0.0) + (color(blush) - al) * np.clip(0.55 - rad, 0, 1)[..., None] * 1.2
        streak = fbm(c, c, scale=3, octaves=2, seed=seed + i)
        al = al * (0.94 + 0.06 * streak[..., None])
        layer = shade(h, np.clip(al, 0, 1), m, strength=r * 0.25, spec=0.12, shininess=18, ambient=0.6, diffuse=0.5)
        img.alpha_composite(drop_shadow(layer, (0, 1), 1.5, 0.35), (0, 0))
        img.alpha_composite(layer)
    cr = r * 0.28
    m = mask_from((c, c), lambda d: d.ellipse([cx - cr, cy - cr, cx + cr, cy + cr], fill=255))
    bumps = fbm(c, c, scale=max(2, cr / 3), octaves=2, seed=seed + 99)
    h = dome(m, 0.7) + bumps * 0.3 * m
    al = ramp(bumps, [(0, (180, 110, 20)), (1, center)])
    img.alpha_composite(shade(h, al, m, strength=cr * 0.6, spec=0.2, ambient=0.5))
    return img, (int(cx), int(cy))


def stem(size, pts, width, seed=0):
    m = mask_from(size, lambda d: d.line(pts, fill=255, width=int(width), joint="curve"))
    h = dome(m, 0.5)
    n = fbm(size[1], size[0], scale=6, octaves=3, seed=seed)
    al = ramp(n, [(0, (60, 82, 30)), (1, (104, 128, 52))])
    return shade(h, al, m, strength=width * 0.5, spec=0.1, ambient=0.5)


def paste_at(base, el, anchor, x, y, shadow=(0, 3), sr=3, so=0.45):
    img, (ax, ay) = el
    tmp = Image.new("RGBA", base.size, (0, 0, 0, 0))
    tmp.paste(img, (int(x - ax), int(y - ay)), img)
    if shadow:
        base.alpha_composite(drop_shadow(tmp, shadow, sr, so))
    base.alpha_composite(tmp)


def vine(base, pts, scale, seed=0, flowers=(), leaf_every=1, pink=False):
    import random
    rnd = random.Random(seed)
    base.alpha_composite(drop_shadow(stem(base.size, pts, 5 * scale, seed), (0, 3), 3, 0.4))
    base.alpha_composite(stem(base.size, pts, 5 * scale, seed))
    for i, (x, y) in enumerate(pts):
        if i % leaf_every:
            continue
        nx, ny = pts[min(i + 1, len(pts) - 1)]
        px, py = pts[max(i - 1, 0)]
        a = math.atan2(ny - py, nx - px)
        side = 1 if i % 2 == 0 else -1
        L = 30 * scale * rnd.uniform(0.8, 1.2)
        el = leaf(L, L * 0.55, a + side * rnd.uniform(0.6, 1.1), seed=seed * 31 + i, hue=rnd.uniform(-0.1, 0.1))
        paste_at(base, el, None, x, y)
    for (x, y, r) in flowers:
        if pink:
            el = flower(r * scale, petal_in=(255, 196, 214), petal_out=(226, 92, 140), center=(255, 214, 110), seed=int(x), blush=(214, 60, 120))
        else:
            el = flower(r * scale, seed=int(x + y))
        paste_at(base, el, None, x, y)


def gold_mat(h, w, seed):
    n = fbm(h, w, scale=8, octaves=3, seed=seed)
    return n


def metal_layer(mask, height, seed=0, palette="gold", strength=10):
    hgt, wid = mask.shape
    n = fbm(hgt, wid, scale=12, octaves=3, seed=seed)
    scratches = fbm(hgt, wid, scale=2, octaves=1, seed=seed + 5)
    if palette == "gold":
        stops = [(0, (96, 54, 14)), (0.35, (176, 112, 30)), (0.7, (232, 172, 64)), (1, (255, 226, 140))]
    else:  # brass
        stops = [(0, (86, 52, 22)), (0.4, (160, 104, 46)), (0.75, (214, 160, 88)), (1, (250, 214, 150))]
    nn = normals(height, strength)
    ndl = np.clip((nn * LIGHT).sum(2), 0, 1)
    t = 0.15 + 0.75 * ndl + 0.1 * (n - 0.5) + 0.05 * (scratches - 0.5)
    al = ramp(t, stops)
    return shade(height, al, mask, strength=strength, spec=0.9, shininess=40, ambient=0.75, diffuse=0.35, metal=True)
