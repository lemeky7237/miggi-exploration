"""Small material renderer: height map + albedo -> lit RGBA, plus procedural noise."""
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from scipy import ndimage

LIGHT = np.array([-0.45, -0.65, 0.62])
LIGHT = LIGHT / np.linalg.norm(LIGHT)


def rng(seed):
    return np.random.default_rng(seed)


def fbm(h, w, scale=64, octaves=5, seed=0, persistence=0.5):
    """Fractal value noise in [0,1]. scale = size in px of the largest feature."""
    r = rng(seed)
    out = np.zeros((h, w), np.float32)
    amp, total = 1.0, 0.0
    s = float(scale)
    for _ in range(octaves):
        gh, gw = max(2, int(h / s) + 3), max(2, int(w / s) + 3)
        g = r.random((gh, gw)).astype(np.float32)
        z = ndimage.zoom(g, (s, s), order=3)[:h, :w]
        if z.shape != (h, w):
            z = np.pad(z, ((0, h - z.shape[0]), (0, w - z.shape[1])), mode="edge")
        out += amp * z
        total += amp
        amp *= persistence
        s = max(1.0, s / 2)
    out /= total
    out -= out.min()
    out /= max(1e-6, out.max())
    return out


def blur(a, s):
    return ndimage.gaussian_filter(a, s) if s > 0 else a


def mask_from(size, fn):
    """size=(w,h); fn(draw) paints white on black. Returns float mask [0,1] (h,w)."""
    m = Image.new("L", size, 0)
    fn(ImageDraw.Draw(m))
    return np.asarray(m, np.float32) / 255.0


def dist_inside(mask):
    return ndimage.distance_transform_edt(mask > 0.5).astype(np.float32)


def bevel(mask, width):
    """Height rising from 0 at the edge to 1 at `width` px inside (smooth)."""
    d = dist_inside(mask)
    t = np.clip(d / max(1, width), 0, 1)
    return (np.sin(t * math.pi / 2) ** 1.2) * (mask > 0.5)


def dome(mask, power=0.5):
    d = dist_inside(mask)
    if d.max() <= 0:
        return d
    return (d / d.max()) ** power


def normals(height, strength):
    gy, gx = np.gradient(height * strength)
    n = np.dstack([-gx, -gy, np.ones_like(height)])
    n /= np.linalg.norm(n, axis=2, keepdims=True)
    return n


def shade(height, albedo, alpha, strength=6.0, spec=0.25, shininess=24, ambient=0.42, diffuse=0.7,
          ao=0.0, metal=False, spec_color=(1, 1, 1)):
    """albedo (h,w,3) floats 0..1; returns RGBA uint8 image."""
    n = normals(height, strength)
    ndl = np.clip((n * LIGHT).sum(2), 0, 1)
    view = np.array([0, 0, 1.0])
    half = LIGHT + view
    half /= np.linalg.norm(half)
    ndh = np.clip((n * half).sum(2), 0, 1)
    sp = spec * ndh ** shininess
    light = ambient + diffuse * ndl
    if ao > 0:
        occl = np.clip(blur(height, 6) - height, 0, 1)
        light = light * (1 - ao * occl * 4)
    rgb = albedo * light[..., None]
    sc = albedo if metal else np.array(spec_color, np.float32)
    rgb = rgb + sp[..., None] * (sc * (1.6 if metal else 1.0))
    rgb = np.clip(rgb, 0, 1)
    out = np.dstack([rgb, np.clip(alpha, 0, 1)])
    return Image.fromarray((out * 255).astype(np.uint8), "RGBA")


def color(c):
    return np.array(c, np.float32) / 255.0


def ramp(t, stops):
    """t (h,w) in 0..1 -> rgb via list of (pos, (r,g,b))."""
    t = np.clip(t, 0, 1)
    out = np.zeros(t.shape + (3,), np.float32)
    for i in range(len(stops) - 1):
        p0, c0 = stops[i]
        p1, c1 = stops[i + 1]
        m = (t >= p0) & (t <= p1)
        f = ((t - p0) / max(1e-6, p1 - p0))[..., None]
        out[m] = (color(c0) * (1 - f) + color(c1) * f)[m]
    return out


def drop_shadow(img, offset=(0, 4), radius=6, opacity=0.55, tint=(30, 16, 8)):
    a = np.asarray(img.split()[3], np.float32) / 255.0
    a = ndimage.shift(blur(a, radius), (offset[1], offset[0]), order=1)
    sh = np.zeros(a.shape + (4,), np.float32)
    sh[..., :3] = color(tint)
    sh[..., 3] = a * opacity
    return Image.fromarray((sh * 255).astype(np.uint8), "RGBA")


def comp(base, layer, shadow=None):
    if shadow is not None:
        base.alpha_composite(drop_shadow(layer, **shadow))
    base.alpha_composite(layer)
    return base


def poly_rot(cx, cy, pts, ang):
    ca, sa = math.cos(ang), math.sin(ang)
    return [(cx + x * ca - y * sa, cy + x * sa + y * ca) for x, y in pts]


def leaf_pts(length, width, n=48, tip=1.0):
    top = []
    for i in range(n + 1):
        t = i / n
        y = (math.sin(t * math.pi) ** 0.85) * (1 - 0.25 * t) * width / 2
        top.append((t * length, y))
    return top + [(x, -y) for x, y in reversed(top)]
