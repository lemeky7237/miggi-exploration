import math, sys, random
import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from lib import *
from elements import *

OUT = sys.argv[1]
U = 4                      # supersampled px per GUI unit (book is 384x240 units)
W, H = 384 * U, 240 * U    # 1536x960, saved at 768x480


def leather(mask, seed=1):
    h, w = mask.shape
    grain = fbm(h, w, scale=2, octaves=2, seed=seed)
    pebble = np.abs(fbm(h, w, scale=6, octaves=2, seed=seed + 1) - 0.5) * 2
    lowf = fbm(h, w, scale=180, octaves=3, seed=seed + 2)
    d = dist_inside(mask)
    wear = np.clip(1 - d / (14 * U), 0, 1) ** 1.5
    height = grain * 0.12 + pebble * 0.16
    # embossed groove border
    groove = mask_from((w, h), lambda dd: dd.rounded_rectangle([12 * U, 12 * U, w - 12 * U, h - 12 * U], 12 * U, outline=255, width=int(1.6 * U)))
    height = height - blur(groove, 2) * 0.9
    # stitching
    st = Image.new("L", (w, h), 0)
    sd = ImageDraw.Draw(st)
    inset = 9 * U
    x0, y0, x1, y1 = inset, inset, w - inset, h - inset
    step, dash = 7 * U, 4 * U
    for x in range(int(x0 + 10 * U), int(x1 - 10 * U), step):
        sd.line([(x, y0), (x + dash, y0)], fill=255, width=int(1.2 * U))
        sd.line([(x, y1), (x + dash, y1)], fill=255, width=int(1.2 * U))
    for y in range(int(y0 + 10 * U), int(y1 - 10 * U), step):
        sd.line([(x0, y), (x0, y + dash)], fill=255, width=int(1.2 * U))
        sd.line([(x1, y), (x1, y + dash)], fill=255, width=int(1.2 * U))
    stitch = blur(np.asarray(st, np.float32) / 255.0, 1.2)
    height = height + stitch * 0.8
    base = ramp(lowf * 0.7 + grain * 0.3, [(0, (70, 34, 16)), (0.5, (110, 58, 28)), (1, (146, 84, 44))])
    base = base * (1 - 0.12 * pebble[..., None]) + wear[..., None] * color((60, 40, 26)) * 0.9
    base = base * (1 - 0.35 * blur(groove, 2)[..., None])
    base = base + stitch[..., None] * (color((214, 176, 120)) - base) * 0.85
    return shade(height, np.clip(base, 0, 1), mask, strength=10, spec=0.22, shininess=14, ambient=0.5, diffuse=0.65)


def paper(mask, left, seed):
    h, w = mask.shape
    fib = fbm(h, w, scale=2, octaves=2, seed=seed)
    mid = fbm(h, w, scale=40, octaves=4, seed=seed + 1)
    stains = fbm(h, w, scale=120, octaves=3, seed=seed + 2)
    stain = blur(np.clip((stains - 0.6) * 3, 0, 1), 6)
    ring = blur(np.clip(1 - np.abs(stains - 0.66) * 40, 0, 1), 1.5) * 0.25
    d = dist_inside(mask)
    burn = np.clip(1 - d / (16 * U), 0, 1) ** 2.2
    al = ramp(mid * 0.5 + fib * 0.2 + 0.25, [(0, (226, 192, 138)), (0.5, (240, 214, 166)), (1, (250, 232, 192))])
    al = al * (1 - 0.06 * stain[..., None] - 0.10 * ring[..., None]) + stain[..., None] * color((150, 100, 40)) * 0.05
    al = al * (1 - 0.38 * burn[..., None]) + burn[..., None] * color((120, 70, 20)) * 0.18
    # page curvature: darker toward gutter, slight highlight on the outer third
    xs = np.linspace(0, 1, w)[None, :].repeat(h, 0)
    ys, xs_idx = np.nonzero(mask > 0.5)
    x_min, x_max = xs_idx.min(), xs_idx.max()
    t = np.clip((np.arange(w) - x_min) / max(1, x_max - x_min), 0, 1)[None, :].repeat(h, 0)
    g = t if left else 1 - t  # 1 at gutter
    curve = 1 - 0.42 * g ** 5 + 0.05 * np.sin((1 - g) * math.pi) * 0.6
    height = fib * 0.08 + mid * 0.1
    img = shade(height, np.clip(al * curve[..., None], 0, 1), mask, strength=4, spec=0.03, ambient=0.72, diffuse=0.32)
    return img


def ink_layer(size, draw_fn, rgb=(98, 64, 38), opacity=0.85, seed=0):
    m = mask_from(size, draw_fn)
    m = blur(m, 0.6)
    n = fbm(size[1], size[0], scale=3, octaves=2, seed=seed)
    a = m * opacity * (0.8 + 0.2 * n)
    out = np.zeros((size[1], size[0], 4), np.float32)
    out[..., :3] = color(rgb)
    out[..., 3] = a
    return Image.fromarray((np.clip(out, 0, 1) * 255).astype(np.uint8), "RGBA")


def main():
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    m = 6 * U
    cover = mask_from((W, H), lambda d: d.rounded_rectangle([m, m, W - m, H - m], 16 * U, fill=255))
    cov = leather(cover)
    comp(img, cov, dict(offset=(0, 4 * U), radius=6 * U, opacity=0.6))

    pt, pb, pl, pr, mid = 20 * U, 218 * U, 22 * U, 362 * U, 192 * U
    # page stacks (thickness lines) under each page
    for k in range(5, 0, -1):
        off = k * 0.9 * U
        for (x0, x1, left) in ((pl, mid - 2 * U, True), (mid + 2 * U, pr, False)):
            sm = mask_from((W, H), lambda d: d.rounded_rectangle([x0 - (off if left else 0), pt + off, x1 + (0 if left else off), pb + off], 5 * U, fill=255))
            shadeval = 205 - k * 10
            layer = np.zeros((H, W, 4), np.float32)
            layer[..., :3] = color((shadeval + 30, shadeval + 10, shadeval - 30))
            layer[..., 3] = sm
            L = Image.fromarray((layer * 255).astype(np.uint8), "RGBA")
            img.alpha_composite(L)
    for (x0, x1, left, seed) in ((pl, mid - 2 * U, True, 10), (mid + 2 * U, pr, False, 20)):
        pm = mask_from((W, H), lambda d: d.rounded_rectangle([x0, pt, x1, pb], 5 * U, fill=255))
        comp(img, paper(pm, left, seed), dict(offset=(0, 1 * U), radius=2 * U, opacity=0.35))
    # spine crease
    crease = mask_from((W, H), lambda d: d.rectangle([mid - 3 * U, pt, mid + 3 * U, pb], fill=255))
    cr = np.zeros((H, W, 4), np.float32)
    cr[..., :3] = color((60, 34, 16))
    xs = np.abs(np.arange(W) - mid)[None, :] / (6 * U)
    cr[..., 3] = blur(crease, 2 * U) * np.clip(1 - xs, 0, 1) * 0.85
    img.alpha_composite(Image.fromarray((cr * 255).astype(np.uint8), "RGBA"))

    # printed ornaments (ink) -------------------------------------------------
    lc, rc = (pl + mid) // 2, (mid + pr) // 2

    def ornaments(d):
        for cx in (lc, rc):
            y = 64 * U
            half = 62 * U
            d.line([(cx - half, y), (cx - 8 * U, y)], fill=255, width=int(0.7 * U))
            d.line([(cx + 8 * U, y), (cx + half, y)], fill=255, width=int(0.7 * U))
            d.polygon([(cx - 5 * U, y), (cx, y - 4 * U), (cx + 5 * U, y), (cx, y + 4 * U)], fill=255)
            for sx in (cx - half, cx + half):
                d.polygon([(sx - 3 * U, y), (sx, y - 2.5 * U), (sx + 3 * U, y), (sx, y + 2.5 * U)], fill=255)
            for s in (-1, 1):
                d.arc([cx + s * 12 * U - 4 * U, y - 4 * U, cx + s * 12 * U + 4 * U, y + 4 * U], 0, 360, fill=255, width=int(0.5 * U))
        # corner flourishes on pages
        for (x, y, sx, sy) in ((pl + 8 * U, pt + 8 * U, 1, 1), (pr - 8 * U, pt + 8 * U, -1, 1), (pl + 8 * U, pb - 8 * U, 1, -1), (pr - 8 * U, pb - 8 * U, -1, -1)):
            d.line([(x, y), (x + sx * 14 * U, y)], fill=255, width=int(0.5 * U))
            d.line([(x, y), (x, y + sy * 14 * U)], fill=255, width=int(0.5 * U))
            d.ellipse([x - 1.5 * U, y - 1.5 * U, x + 1.5 * U, y + 1.5 * U], fill=255)
    img.alpha_composite(ink_layer((W, H), ornaments, rgb=(126, 84, 46), opacity=0.8, seed=3))

    # engraved landscape sketches (line art, very faint)
    def sketch(d):
        r = random.Random(4)
        # left: pine forest
        for i in range(9):
            x = pl + 26 * U + i * 9 * U + r.uniform(-2, 2) * U
            base = pb - 12 * U
            hgt = r.uniform(14, 24) * U
            d.line([(x, base), (x, base - hgt)], fill=255, width=int(0.5 * U))
            for k in range(5):
                yy = base - hgt * (0.2 + k * 0.17)
                w2 = (1 - k / 5) * 5 * U
                d.line([(x - w2, yy + 2 * U), (x, yy - 2 * U), (x + w2, yy + 2 * U)], fill=255, width=int(0.45 * U))
        d.line([(pl + 18 * U, pb - 12 * U), (pl + 120 * U, pb - 12 * U)], fill=255, width=int(0.4 * U))
        # right: mountains + lone tree
        base = pb - 12 * U
        pts = [(pr - 150 * U, base), (pr - 118 * U, base - 22 * U), (pr - 100 * U, base - 12 * U), (pr - 74 * U, base - 40 * U),
               (pr - 50 * U, base - 18 * U), (pr - 34 * U, base - 26 * U), (pr - 14 * U, base)]
        d.line(pts, fill=255, width=int(0.6 * U))
        for i in range(10):
            x = pr - 74 * U + i * 2.5 * U
            d.line([(x, base - 40 * U + i * 2.6 * U), (x - 6 * U, base - 30 * U + i * 2.2 * U)], fill=255, width=int(0.35 * U))
        tx, ty = pr - 128 * U, base
        d.line([(tx, ty), (tx, ty - 18 * U)], fill=255, width=int(0.9 * U))
        for (ox, oy, rr) in ((0, -24, 7), (-6, -20, 5), (6, -20, 5), (0, -30, 5)):
            d.arc([tx + ox * U - rr * U, ty + oy * U - rr * U, tx + ox * U + rr * U, ty + oy * U + rr * U], 160, 380, fill=255, width=int(0.5 * U))
        # compass rose watermark top-right page
        cx, cy, R = pr - 30 * U, pt + 30 * U, 14 * U
        d.ellipse([cx - R, cy - R, cx + R, cy + R], outline=255, width=int(0.4 * U))
        for a in range(8):
            ang = a * math.pi / 4
            L = R * (1.25 if a % 2 == 0 else 0.7)
            d.line([(cx, cy), (cx + math.cos(ang) * L, cy + math.sin(ang) * L)], fill=255, width=int(0.4 * U))
    img.alpha_composite(ink_layer((W, H), sketch, rgb=(150, 110, 70), opacity=0.2, seed=5))

    # gold corner brackets --------------------------------------------------
    for (cx, cy, sx, sy) in ((m, m, 1, 1), (W - m, m, -1, 1), (m, H - m, 1, -1), (W - m, H - m, -1, -1)):
        L, T = 30 * U, 7 * U

        def bracket(d):
            d.polygon([(cx, cy), (cx + sx * L, cy), (cx + sx * L, cy + sy * T), (cx + sx * T, cy + sy * T),
                       (cx + sx * T, cy + sy * L), (cx, cy + sy * L)], fill=255)
            d.ellipse([cx + sx * L - 4 * U, cy + sy * T / 2 - 4 * U, cx + sx * L + 4 * U, cy + sy * T / 2 + 4 * U], fill=255)
            d.ellipse([cx + sx * T / 2 - 4 * U, cy + sy * L - 4 * U, cx + sx * T / 2 + 4 * U, cy + sy * L + 4 * U], fill=255)
            d.pieslice([cx + sx * 4 * U - 9 * U, cy + sy * 4 * U - 9 * U, cx + sx * 4 * U + 9 * U, cy + sy * 4 * U + 9 * U], 0, 360, fill=255)
        bm = mask_from((W, H), bracket)
        hgt = bevel(bm, 2.5 * U)
        rivets = mask_from((W, H), lambda d: d.ellipse([cx + sx * 4 * U - 3 * U, cy + sy * 4 * U - 3 * U, cx + sx * 4 * U + 3 * U, cy + sy * 4 * U + 3 * U], fill=255))
        hgt = hgt + dome(rivets, 0.5) * 1.2
        engr = mask_from((W, H), lambda d: d.line([(cx + sx * 9 * U, cy + sy * 3.5 * U), (cx + sx * (L - 3 * U), cy + sy * 3.5 * U)], fill=255, width=int(0.6 * U)))
        engr2 = mask_from((W, H), lambda d: d.line([(cx + sx * 3.5 * U, cy + sy * 9 * U), (cx + sx * 3.5 * U, cy + sy * (L - 3 * U))], fill=255, width=int(0.6 * U)))
        hgt = hgt - blur(engr + engr2, 1) * 0.5
        comp(img, metal_layer(bm, hgt, seed=cx + cy, strength=14), dict(offset=(U, 2 * U), radius=2 * U, opacity=0.55))

    # vines + flowers ---------------------------------------------------------
    r = random.Random(7)
    tl = [(m + 2 * U + i * 13 * U, m + 3 * U + math.sin(i * 0.9) * 3 * U) for i in range(7)]
    vine(img, tl, U * 0.62, seed=1, flowers=[(m + 36 * U, m + 10 * U, 16), (m + 4 * U, m + 4 * U, 11)])
    lv = [(m + 3 * U + math.sin(i * 0.8) * 3 * U, m + 8 * U + i * 13 * U) for i in range(9)]
    vine(img, lv, U * 0.55, seed=2, flowers=[(m + 6 * U, m + 92 * U, 12), (m + 4 * U, m + 40 * U, 9)])
    tr = [(W - m - 2 * U - i * 13 * U, m + 3 * U + math.sin(i) * 3 * U) for i in range(6)]
    vine(img, tr, U * 0.55, seed=3, flowers=[(W - m - 28 * U, m + 10 * U, 11)])
    br = [(W - m - 3 * U - math.sin(i * 0.7) * 3 * U, H - m - 3 * U - i * 13 * U) for i in range(8)]
    vine(img, br, U * 0.62, seed=4, flowers=[(W - m - 18 * U, H - m - 20 * U, 19), (W - m - 44 * U, H - m - 8 * U, 13), (W - m - 6 * U, H - m - 46 * U, 10)], pink=True)
    bl = [(m + 3 * U + i * 12 * U, H - m - 3 * U - math.sin(i * 0.8) * 3 * U) for i in range(4)]
    vine(img, bl, U * 0.5, seed=5, flowers=[(m + 30 * U, H - m - 6 * U, 9)])

    # ribbon bookmark -------------------------------------------------------
    rx0, rx1 = mid - 12 * U, mid + 12 * U
    rib = mask_from((W, H), lambda d: d.polygon([(rx0, 0), (rx1, 0), (rx1, 54 * U), (mid, 46 * U), (rx0, 54 * U)], fill=255))
    weave = (np.sin(np.arange(H)[:, None] * 1.4) * 0.5 + 0.5) * 0.5 + (np.sin(np.arange(W)[None, :] * 1.4) * 0.5 + 0.5) * 0.5
    xs = (np.arange(W)[None, :] - mid) / (12 * U)
    fold = np.clip(1 - xs ** 2, 0, 1) ** 0.5
    rh = fold * 0.8 + weave * 0.06
    rn = fbm(H, W, scale=30, octaves=3, seed=9)
    ral = ramp(rn * 0.4 + fold * 0.6, [(0, (18, 58, 26)), (0.6, (38, 104, 48)), (1, (70, 146, 76))])
    emb = mask_from((W, H), lambda d: d.line([(rx0 + 2.5 * U, 0), (rx0 + 2.5 * U, 50 * U), (mid, 43 * U), (rx1 - 2.5 * U, 50 * U), (rx1 - 2.5 * U, 0)], fill=255, width=int(1 * U)))
    ral = ral + emb[..., None] * (color((226, 176, 76)) - ral)
    rh = rh + emb * 0.25
    comp(img, shade(rh, np.clip(ral, 0, 1), rib, strength=8, spec=0.35, shininess=20, ambient=0.5),
         dict(offset=(0, 3 * U), radius=3 * U, opacity=0.5))
    # cap at the top
    cap = mask_from((W, H), lambda d: d.rounded_rectangle([rx0 - 3 * U, 0, rx1 + 3 * U, 6 * U], 2 * U, fill=255))
    comp(img, metal_layer(cap, bevel(cap, 2 * U), seed=11, strength=10), dict(offset=(0, U), radius=U, opacity=0.5))
    # compass star medallion
    cx, cy = mid, 25 * U
    ring = mask_from((W, H), lambda d: d.ellipse([cx - 10 * U, cy - 10 * U, cx + 10 * U, cy + 10 * U], fill=255))
    comp(img, metal_layer(ring, bevel(ring, 2.5 * U) * 0.6, seed=12, strength=12), dict(offset=(0, U), radius=U, opacity=0.5))
    inner = mask_from((W, H), lambda d: d.ellipse([cx - 7.5 * U, cy - 7.5 * U, cx + 7.5 * U, cy + 7.5 * U], fill=255))
    img.alpha_composite(shade(np.zeros((H, W)), np.dstack([np.full((H, W), 0.12), np.full((H, W), 0.30), np.full((H, W), 0.16)]), inner, ambient=1, diffuse=0))

    def star(d):
        for a, L in [(k * math.pi / 4, 9 if k % 2 == 0 else 5) for k in range(8)]:
            tip = (cx + math.cos(a - math.pi / 2) * L * U, cy + math.sin(a - math.pi / 2) * L * U)
            l = (cx + math.cos(a) * 1.8 * U, cy + math.sin(a) * 1.8 * U)
            rr = (cx - math.cos(a) * 1.8 * U, cy - math.sin(a) * 1.8 * U)
            d.polygon([tip, l, rr], fill=255)
    sm = mask_from((W, H), star)
    # ridge height for a faceted star
    sh = dome(sm, 0.8)
    comp(img, metal_layer(sm, sh, seed=13, strength=16), dict(offset=(0, U // 2), radius=U, opacity=0.5))
    gem = mask_from((W, H), lambda d: d.ellipse([cx - 2 * U, cy - 2 * U, cx + 2 * U, cy + 2 * U], fill=255))
    gal = np.dstack([np.full((H, W), 0.8), np.full((H, W), 0.15), np.full((H, W), 0.12)])
    img.alpha_composite(shade(dome(gem, 0.5), gal, gem, strength=10, spec=0.9, shininess=30, ambient=0.6))

    final = img.resize((768, 480), Image.LANCZOS)
    canvas = Image.new("RGBA", (1024, 512), (0, 0, 0, 0))
    canvas.paste(final, (0, 0))
    canvas.save(f"{OUT}/journal.png")


if __name__ == "__main__":
    main()
