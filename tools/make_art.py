"""Procedural art for 미끼탐험모드: journal GUI, discovery scroll, item icons.
Clean shapes + smooth gradients (no noisy texture). Drawn 4x then downsampled."""
import math, random, os, sys
from PIL import Image, ImageDraw, ImageFilter, ImageChops

OUT = sys.argv[1] if len(sys.argv) > 1 else "out"
os.makedirs(OUT, exist_ok=True)
S = 4  # supersample


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


def vgrad(w, h, top, bot):
    im = Image.new("RGBA", (w, h))
    d = ImageDraw.Draw(im)
    for y in range(h):
        d.line([(0, y), (w, y)], fill=lerp(top, bot, y / max(1, h - 1)))
    return im


def radial(w, h, inner, outer, cx=0.5, cy=0.5, power=1.6):
    im = Image.new("RGBA", (w, h))
    px = im.load()
    for y in range(h):
        for x in range(w):
            dx = (x / w - cx) * 2
            dy = (y / h - cy) * 2
            t = min(1, (dx * dx + dy * dy) ** 0.5 / 1.41) ** power
            px[x, y] = lerp(inner, outer, t)
    return im


def mask_paste(base, fill_img, mask):
    base.paste(fill_img, (0, 0), mask)


def shape_mask(size, fn):
    m = Image.new("L", size, 0)
    fn(ImageDraw.Draw(m))
    return m


def down(im, w, h):
    return im.resize((w, h), Image.LANCZOS)


def shadow(mask, off, blur, alpha=110):
    sh = Image.new("RGBA", mask.size, (40, 22, 10, 0))
    a = mask.point(lambda v: v * alpha // 255)
    sh.putalpha(a)
    sh = sh.filter(ImageFilter.GaussianBlur(blur))
    out = Image.new("RGBA", mask.size, (0, 0, 0, 0))
    out.paste(sh, off, sh)
    return out


# ---------------------------------------------------------------- leaves / flowers
def leaf(d, cx, cy, length, width, angle, col=(92, 160, 58), dark=(52, 112, 38), vein=(150, 205, 100)):
    ca, sa = math.cos(angle), math.sin(angle)
    pts = []
    for i in range(41):
        t = i / 40
        x = t * length
        y = math.sin(t * math.pi) ** 0.9 * width / 2
        pts.append((x, y))
    poly = pts + [(x, -y) for (x, y) in reversed(pts)]
    rot = [(cx + x * ca - y * sa, cy + x * sa + y * ca) for (x, y) in poly]
    d.polygon(rot, fill=dark)
    inner = [(cx + (x * 0.96) * ca - (y * 0.82) * sa, cy + (x * 0.96) * sa + (y * 0.82) * ca) for (x, y) in poly]
    d.polygon(inner, fill=col)
    # lighter half
    half = [(cx + (x * 0.94) * ca - (y * 0.7) * sa, cy + (x * 0.94) * sa + (y * 0.7) * ca) for (x, y) in pts]
    half = half + [(cx + x * ca, cy + x * sa) for (x, _) in reversed(pts)]
    d.polygon(half, fill=lerp(col, (190, 230, 120), 0.35))
    d.line([(cx, cy), (cx + length * 0.92 * ca, cy + length * 0.92 * sa)], fill=vein, width=max(1, int(width * 0.07)))


def flower(d, cx, cy, r, petal=(255, 255, 255), edge=(225, 215, 220), center=(250, 200, 60), n=5, rot=0.0):
    for i in range(n):
        a = rot + i * 2 * math.pi / n
        px, py = cx + math.cos(a) * r * 0.55, cy + math.sin(a) * r * 0.55
        d.ellipse([px - r * 0.52, py - r * 0.52, px + r * 0.52, py + r * 0.52], fill=edge)
        d.ellipse([px - r * 0.44, py - r * 0.44, px + r * 0.44, py + r * 0.44], fill=petal)
    d.ellipse([cx - r * 0.26, cy - r * 0.26, cx + r * 0.26, cy + r * 0.26], fill=center)


def vine(d, pts, leaves_every=2, size=1.0, rnd=None, flowers=(), pink=False):
    rnd = rnd or random.Random(1)
    d.line(pts, fill=(70, 112, 40), width=int(5 * size), joint="curve")
    for i, (x, y) in enumerate(pts):
        if i % leaves_every == 0:
            side = 1 if i % (2 * leaves_every) == 0 else -1
            if i + 1 < len(pts):
                nx, ny = pts[i + 1]
            else:
                nx, ny = pts[i - 1]
                nx, ny = 2 * x - nx, 2 * y - ny
            base = math.atan2(ny - y, nx - x)
            leaf(d, x, y, 44 * size * rnd.uniform(0.85, 1.15), 26 * size, base + side * rnd.uniform(0.7, 1.2))
    for (x, y, r) in flowers:
        if pink:
            flower(d, x, y, r, petal=(255, 182, 205), edge=(236, 120, 160), center=(255, 230, 120))
        else:
            flower(d, x, y, r)


# ---------------------------------------------------------------- journal GUI
def journal():
    W, H = 384, 240  # final texture (texture file is 512x256 canvas, top-left region)
    w, h = W * S, H * S
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    m = 6 * S
    # cover
    cover_mask = shape_mask((w, h), lambda d: d.rounded_rectangle([m, m, w - m, h - m], 16 * S, fill=255))
    im.alpha_composite(shadow(cover_mask, (0, 3 * S), 5 * S, 140))
    leather = radial(w // 8, h // 8, (132, 72, 38, 255), (78, 38, 18, 255))
    leather = leather.resize((w, h), Image.BICUBIC)
    im.paste(leather, (0, 0), cover_mask)
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([m, m, w - m, h - m], 16 * S, outline=(54, 26, 12), width=3 * S)
    d.rounded_rectangle([m + 6 * S, m + 6 * S, w - m - 6 * S, h - m - 6 * S], 12 * S, outline=(170, 104, 52), width=S)

    # pages
    pt, pb = 20 * S, h - 22 * S
    pl, pr, mid = 22 * S, w - 22 * S, w // 2
    for (x0, x1, left) in ((pl, mid - 3 * S, True), (mid + 3 * S, pr, False)):
        pm = shape_mask((w, h), lambda dd: dd.rounded_rectangle([x0, pt, x1, pb], 6 * S, fill=255))
        # page stack edge
        d.rounded_rectangle([x0 - (2 * S if left else 0), pt + 3 * S, x1 + (0 if left else 2 * S), pb + 3 * S], 6 * S, fill=(214, 186, 140))
        paper = radial((x1 - x0) // 8, (pb - pt) // 8, (250, 236, 206, 255), (226, 196, 150, 255),
                       cx=0.62 if left else 0.38, power=2.0).resize((x1 - x0, pb - pt), Image.BICUBIC)
        layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        layer.paste(paper, (x0, pt))
        im.paste(layer, (0, 0), pm)
        # gutter shading
        g = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        gd = ImageDraw.Draw(g)
        for i in range(18 * S):
            a = int(70 * (1 - i / (18 * S)) ** 2)
            x = (x1 - i) if left else (x0 + i)
            gd.line([(x, pt), (x, pb)], fill=(120, 80, 40, a))
        im.paste(Image.alpha_composite(im, g), (0, 0), pm)
    d = ImageDraw.Draw(im)
    # spine
    d.rectangle([mid - 4 * S, pt - 2 * S, mid + 4 * S, pb + 6 * S], fill=(92, 46, 22))
    d.line([(mid, pt - 2 * S), (mid, pb + 6 * S)], fill=(60, 28, 12), width=S)

    # decorative dividers (diamond + line) under headings
    def divider(cx, y, half):
        d.line([(cx - half, y), (cx + half, y)], fill=(160, 112, 70), width=S)
        for sx in (cx - half, cx + half):
            d.polygon([(sx - 3 * S, y), (sx, y - 3 * S), (sx + 3 * S, y), (sx, y + 3 * S)], fill=(150, 98, 58))
        d.polygon([(cx - 4 * S, y), (cx, y - 4 * S), (cx + 4 * S, y), (cx, y + 4 * S)], fill=(176, 120, 64))
    lc, rc = (pl + mid) // 2, (mid + pr) // 2
    divider(lc, 64 * S, 64 * S)
    divider(rc, 64 * S, 64 * S)
    # faint landscape sketches at page bottoms
    sk = (222, 196, 154)
    for i in range(6):
        x = pl + 30 * S + i * 11 * S
        d.polygon([(x, pb - 12 * S), (x + 4 * S, pb - 26 * S - (i % 2) * 6 * S), (x + 8 * S, pb - 12 * S)], fill=sk)
    d.polygon([(pr - 120 * S, pb - 10 * S), (pr - 80 * S, pb - 44 * S), (pr - 55 * S, pb - 26 * S),
               (pr - 40 * S, pb - 34 * S), (pr - 12 * S, pb - 10 * S)], fill=sk)
    d.polygon([(pr - 92 * S, pb - 33 * S), (pr - 80 * S, pb - 44 * S), (pr - 70 * S, pb - 36 * S)], fill=(242, 230, 206))

    # gold corner brackets
    gold, gold_d = (230, 168, 60), (150, 92, 24)
    for (cx, cy, sx, sy) in ((m, m, 1, 1), (w - m, m, -1, 1), (m, h - m, 1, -1), (w - m, h - m, -1, -1)):
        L, T = 26 * S, 7 * S
        poly = [(cx, cy), (cx + sx * L, cy), (cx + sx * L, cy + sy * T), (cx + sx * T, cy + sy * T),
                (cx + sx * T, cy + sy * L), (cx, cy + sy * L)]
        d.polygon(poly, fill=gold_d)
        inner = [(cx + sx * S, cy + sy * S), (cx + sx * (L - S), cy + sy * S), (cx + sx * (L - S), cy + sy * (T - S)),
                 (cx + sx * (T - S), cy + sy * (T - S)), (cx + sx * (T - S), cy + sy * (L - S)), (cx + sx * S, cy + sy * (L - S))]
        d.polygon(inner, fill=gold)
        d.ellipse([cx + sx * 11 * S - 3 * S, cy + sy * 11 * S - 3 * S, cx + sx * 11 * S + 3 * S, cy + sy * 11 * S + 3 * S], fill=gold_d)

    # vines
    rnd = random.Random(7)
    tl = [(m + 4 * S + i * 15 * S, m + 4 * S + int(math.sin(i * 0.9) * 4 * S)) for i in range(8)]
    vine(d, tl, 1, 1.3, rnd, flowers=[(m + 40 * S, m + 14 * S, 9 * S)])
    lv = [(m + 4 * S + int(math.sin(i * 0.8) * 4 * S), m + 10 * S + i * 15 * S) for i in range(10)]
    vine(d, lv, 1, 1.2, rnd, flowers=[(m + 12 * S, m + 100 * S, 7 * S)])
    tr = [(w - m - 4 * S - i * 15 * S, m + 4 * S + int(math.sin(i) * 4 * S)) for i in range(7)]
    vine(d, tr, 1, 1.2, rnd, flowers=[(w - m - 30 * S, m + 12 * S, 6 * S)])
    br = [(w - m - 4 * S - int(math.sin(i * 0.7) * 3 * S), h - m - 4 * S - i * 15 * S) for i in range(8)]
    vine(d, br, 1, 1.4, rnd, flowers=[(w - m - 18 * S, h - m - 22 * S, 12 * S), (w - m - 46 * S, h - m - 10 * S, 8 * S)], pink=True)

    # green ribbon bookmark with compass star
    rx0, rx1 = mid - 13 * S, mid + 13 * S
    d.rectangle([rx0, 0, rx1, 50 * S], fill=(40, 98, 46))
    d.polygon([(rx0, 50 * S), (mid, 40 * S), (rx1, 50 * S), (rx1, 54 * S), (mid, 46 * S), (rx0, 54 * S)], fill=(40, 98, 46))
    d.rectangle([rx0 + 2 * S, 0, rx1 - 2 * S, 48 * S], outline=(224, 170, 70), width=S)
    d.rounded_rectangle([rx0 - 3 * S, 0, rx1 + 3 * S, 7 * S], 3 * S, fill=(52, 120, 56), outline=(28, 70, 32), width=S)
    cx, cy = mid, 26 * S
    d.ellipse([cx - 9 * S, cy - 9 * S, cx + 9 * S, cy + 9 * S], outline=(224, 170, 70), width=S)
    for a, r in ((0, 9), (math.pi / 2, 9), (math.pi, 9), (3 * math.pi / 2, 9), (math.pi / 4, 5), (3 * math.pi / 4, 5), (5 * math.pi / 4, 5), (7 * math.pi / 4, 5)):
        tip = (cx + math.cos(a) * r * S, cy + math.sin(a) * r * S)
        l = (cx + math.cos(a + math.pi / 2) * 2 * S, cy + math.sin(a + math.pi / 2) * 2 * S)
        rr = (cx + math.cos(a - math.pi / 2) * 2 * S, cy + math.sin(a - math.pi / 2) * 2 * S)
        d.polygon([tip, l, rr], fill=(240, 190, 80))
    d.ellipse([cx - 2 * S, cy - 2 * S, cx + 2 * S, cy + 2 * S], fill=(200, 60, 40))

    final = down(im, W, H)
    canvas = Image.new("RGBA", (512, 256), (0, 0, 0, 0))
    canvas.paste(final, (0, 0))
    canvas.save(f"{OUT}/journal.png")
    return final


# ---------------------------------------------------------------- arrow buttons (page turn) 16x16 x2
def arrows():
    sz = 16 * S
    im = Image.new("RGBA", (32 * S, 16 * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    for i, col in enumerate(((120, 76, 40), (190, 120, 50))):
        ox = i * sz
        d.polygon([(ox + 4 * S, 3 * S), (ox + 13 * S, 8 * S), (ox + 4 * S, 13 * S), (ox + 6 * S, 8 * S)], fill=col)
    down(im, 32, 16).save(f"{OUT}/arrows.png")


# ---------------------------------------------------------------- discovery scroll
def scroll():
    W, H = 320, 80
    w, h = W * S, H * S
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    rnd = random.Random(3)
    x0, x1 = 34 * S, w - 34 * S
    y0, y1 = 12 * S, h - 12 * S

    # torn edges (gentle)
    def edge(y, sign):
        pts = []
        x = x0
        while x < x1:
            pts.append((x, y + sign * rnd.randint(0, 3) * S))
            x += rnd.randint(5, 11) * S
        pts.append((x1, y))
        return pts
    top = edge(y0, 1)
    bot = edge(y1, -1)
    poly = top + list(reversed(bot))
    pm = shape_mask((w, h), lambda d: d.polygon(poly, fill=255))
    im.alpha_composite(shadow(pm, (0, 2 * S), 4 * S, 120))
    paper = radial(w // 8, h // 8, (246, 226, 186, 255), (214, 170, 110, 255), power=1.8).resize((w, h), Image.BICUBIC)
    im.paste(paper, (0, 0), pm)
    d = ImageDraw.Draw(im)
    # edge darkening line
    d.line(top, fill=(176, 126, 70), width=S)
    d.line(bot, fill=(176, 126, 70), width=S)
    # curled ends
    for cx in (x0, x1):
        d.rounded_rectangle([cx - 10 * S, y0 - 4 * S, cx + 10 * S, y1 + 4 * S], 9 * S, fill=(222, 182, 122), outline=(160, 112, 60), width=S)
        d.rounded_rectangle([cx - 4 * S, y0 - 2 * S, cx + 4 * S, y1 + 2 * S], 4 * S, fill=(196, 150, 92))
    # brass rods + finials
    for cx in (x0, x1):
        rod = vgrad(8 * S, h - 8 * S, (230, 176, 96, 255), (120, 70, 26, 255))
        rodh = Image.new("RGBA", (8 * S, h - 8 * S))
        # horizontal shading
        rd = ImageDraw.Draw(rodh)
        for i in range(8 * S):
            t = abs(i / (8 * S) - 0.35)
            rd.line([(i, 0), (i, h)], fill=lerp((244, 200, 120, 255), (110, 62, 22, 255), min(1, t * 1.8)))
        im.paste(rodh, (cx - 4 * S, 4 * S))
        for (ty, sign) in ((4 * S, -1), (h - 4 * S, 1)):
            d.ellipse([cx - 8 * S, ty - 4 * S, cx + 8 * S, ty + 4 * S], fill=(186, 128, 54), outline=(110, 62, 22), width=S)
            d.polygon([(cx - 5 * S, ty), (cx, ty + sign * 12 * S), (cx + 5 * S, ty)], fill=(206, 146, 64))
            d.line([(cx, ty), (cx, ty + sign * 11 * S)], fill=(246, 206, 130), width=S)
    final = down(im, W, H)
    canvas = Image.new("RGBA", (512, 128), (0, 0, 0, 0))
    canvas.paste(final, (0, 0))
    canvas.save(f"{OUT}/discovery_scroll.png")
    return final


# ---------------------------------------------------------------- items (32x32)
N = 32 * 8  # draw size


def outline_of(im, col=(40, 24, 30, 255), r=6):
    a = im.split()[3]
    grown = a.filter(ImageFilter.MaxFilter(r * 2 + 1))
    ol = Image.new("RGBA", im.size, col)
    ol.putalpha(grown)
    ol.alpha_composite(im)
    return ol


def gem_facets(d, pts, cols):
    c = (sum(p[0] for p in pts) / len(pts), sum(p[1] for p in pts) / len(pts))
    for i in range(len(pts)):
        d.polygon([c, pts[i], pts[(i + 1) % len(pts)]], fill=cols[i % len(cols)])


def item_coin():
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    c, r = N / 2, N * 0.42
    d.ellipse([c - r, c - r + 6, c + r, c + r + 6], fill=(150, 90, 20))
    d.ellipse([c - r, c - r, c + r, c + r], fill=(232, 168, 44))
    d.ellipse([c - r * 0.84, c - r * 0.84, c + r * 0.84, c + r * 0.84], fill=(196, 128, 26))
    d.ellipse([c - r * 0.78, c - r * 0.78, c + r * 0.78, c + r * 0.78], fill=(246, 196, 76))
    # sun emblem
    for i in range(12):
        a = i * math.pi / 6
        tip = (c + math.cos(a) * r * 0.62, c + math.sin(a) * r * 0.62)
        l = (c + math.cos(a + 0.22) * r * 0.32, c + math.sin(a + 0.22) * r * 0.32)
        rr = (c + math.cos(a - 0.22) * r * 0.32, c + math.sin(a - 0.22) * r * 0.32)
        d.polygon([tip, l, rr], fill=(200, 128, 30))
    d.ellipse([c - r * 0.3, c - r * 0.3, c + r * 0.3, c + r * 0.3], fill=(214, 142, 34))
    d.ellipse([c - r * 0.2, c - r * 0.2, c + r * 0.2, c + r * 0.2], fill=(250, 214, 110))
    # rim dots
    for i in range(20):
        a = i * math.pi / 10
        x, y = c + math.cos(a) * r * 0.91, c + math.sin(a) * r * 0.91
        d.ellipse([x - 4, y - 4, x + 4, y + 4], fill=(255, 226, 140))
    # shine
    d.arc([c - r * 0.7, c - r * 0.7, c + r * 0.7, c + r * 0.7], 200, 250, fill=(255, 245, 200), width=7)
    return im


def item_pearl():
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    c = N / 2
    # bottom shell
    def fan(cy, rx, ry, start, end, col, rib):
        d.pieslice([c - rx, cy - ry, c + rx, cy + ry], start, end, fill=col)
        for i in range(9):
            a = math.radians(start + (end - start) * (i + 0.5) / 9)
            d.line([(c, cy), (c + math.cos(a) * rx * 0.95, cy + math.sin(a) * ry * 0.95)], fill=rib, width=6)
    fan(N * 0.62, N * 0.44, N * 0.30, 0, 180, (64, 120, 196), (40, 86, 160))
    fan(N * 0.56, N * 0.42, N * 0.40, 180, 360, (110, 170, 232), (70, 128, 200))
    d.ellipse([c - N * 0.38, N * 0.5, c + N * 0.38, N * 0.68], fill=(200, 226, 250))
    d.ellipse([c - N * 0.30, N * 0.52, c + N * 0.30, N * 0.64], fill=(236, 214, 236))
    # pearl
    pr = N * 0.13
    py = N * 0.53
    pearl = radial(int(pr * 2), int(pr * 2), (255, 255, 255, 255), (196, 200, 232, 255), cx=0.35, cy=0.35, power=1.3)
    pm = shape_mask(pearl.size, lambda dd: dd.ellipse([0, 0, pr * 2 - 1, pr * 2 - 1], fill=255))
    im.paste(pearl, (int(c - pr), int(py - pr)), pm)
    # bubbles
    for (x, y, r) in ((N * 0.78, N * 0.18, 12), (N * 0.86, N * 0.30, 8), (N * 0.72, N * 0.08, 7)):
        d.ellipse([x - r, y - r, x + r, y + r], fill=(130, 200, 255), outline=(60, 130, 210), width=3)
        d.ellipse([x - r * 0.5, y - r * 0.6, x - r * 0.1, y - r * 0.2], fill=(255, 255, 255))
    return im


def crystal(cols, dark, glow):
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    shards = [
        [(N * .50, N * .06), (N * .64, N * .30), (N * .60, N * .86), (N * .40, N * .86), (N * .36, N * .30)],
        [(N * .24, N * .30), (N * .36, N * .46), (N * .38, N * .88), (N * .20, N * .86), (N * .14, N * .52)],
        [(N * .80, N * .36), (N * .86, N * .56), (N * .80, N * .88), (N * .62, N * .88), (N * .66, N * .52)],
    ]
    for s in (shards[1], shards[2], shards[0]):
        d.polygon(s, fill=dark)
        inset = [(p[0] * 0.94 + sum(q[0] for q in s) / len(s) * 0.06, p[1] * 0.94 + sum(q[1] for q in s) / len(s) * 0.06) for p in s]
        gem_facets(d, inset, cols)
    d.line([(N * .45, N * .2), (N * .42, N * .7)], fill=(255, 255, 255), width=6)
    for (x, y) in ((N * .2, N * .2), (N * .82, N * .2)):
        d.polygon([(x, y - 14), (x + 4, y), (x, y + 14), (x - 4, y)], fill=glow)
        d.polygon([(x - 14, y), (x, y - 4), (x + 14, y), (x, y + 4)], fill=glow)
    return im


def item_pouch():
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    c = N / 2
    # body
    d.rounded_rectangle([N * .22, N * .34, N * .78, N * .86], 60, fill=(206, 86, 122))
    d.rounded_rectangle([N * .25, N * .36, N * .75, N * .82], 54, fill=(244, 150, 178))
    d.ellipse([N * .30, N * .40, N * .52, N * .58], fill=(252, 196, 212))
    # top frill
    d.polygon([(N * .30, N * .36), (N * .22, N * .18), (N * .36, N * .24), (N * .50, N * .14), (N * .64, N * .24), (N * .78, N * .18), (N * .70, N * .36)], fill=(232, 112, 150))
    # cord
    d.rounded_rectangle([N * .26, N * .32, N * .74, N * .40], 12, fill=(150, 40, 70))
    d.line([(N * .70, N * .36), (N * .84, N * .52), (N * .80, N * .66)], fill=(150, 40, 70), width=8)
    d.ellipse([N * .76, N * .62, N * .86, N * .72], fill=(230, 180, 60))
    d.line([(N * .81, N * .72), (N * .81, N * .88)], fill=(230, 80, 120), width=10)
    # cherry blossoms
    for (x, y, r) in ((N * .48, N * .62, 34), (N * .64, N * .54, 22), (N * .36, N * .74, 18)):
        flower(d, x, y, r, petal=(255, 236, 242), edge=(236, 140, 170), center=(240, 120, 150))
    return im


def item_leaf():
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    leaf(d, N * .08, N * .92, N * 1.08, N * .46, -math.pi / 4, col=(110, 186, 62), dark=(48, 110, 36), vein=(206, 236, 140))
    leaf(d, N * .2, N * .82, N * .48, N * .20, -math.pi / 2.6, col=(90, 162, 52), dark=(44, 100, 34), vein=(180, 220, 120))
    d.line([(N * .12, N * .9), (N * .26, N * .74)], fill=(120, 80, 40), width=8)
    # golden vine curl
    d.arc([N * .30, N * .40, N * .58, N * .68], 180, 420, fill=(230, 180, 70), width=6)
    for (x, y, r) in ((N * .70, N * .32, 18), (N * .58, N * .22, 13)):
        flower(d, x, y, r, petal=(255, 250, 220), edge=(230, 200, 120), center=(240, 160, 40))
    return im


def item_mushroom():
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    # glow
    glow = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse([N * .1, N * .1, N * .9, N * .7], fill=(170, 120, 255, 110))
    im.alpha_composite(glow.filter(ImageFilter.GaussianBlur(18)))
    d = ImageDraw.Draw(im)
    # small mushroom
    d.rounded_rectangle([N * .66, N * .60, N * .74, N * .88], 10, fill=(210, 200, 236))
    d.chord([N * .54, N * .46, N * .88, N * .74], 180, 360, fill=(70, 120, 220))
    d.chord([N * .56, N * .48, N * .86, N * .70], 180, 360, fill=(110, 170, 250))
    # big mushroom
    d.rounded_rectangle([N * .34, N * .48, N * .52, N * .90], 18, fill=(196, 182, 232))
    d.rounded_rectangle([N * .38, N * .50, N * .46, N * .88], 10, fill=(230, 222, 250))
    d.chord([N * .10, N * .10, N * .76, N * .66], 180, 360, fill=(92, 40, 160))
    d.chord([N * .13, N * .13, N * .73, N * .62], 180, 360, fill=(150, 80, 220))
    d.chord([N * .18, N * .17, N * .56, N * .50], 190, 300, fill=(196, 140, 250))
    d.ellipse([N * .14, N * .36, N * .72, N * .42], fill=(110, 56, 180))
    for (x, y, r) in ((N * .32, N * .26, 12), (N * .52, N * .22, 9), (N * .60, N * .32, 7), (N * .24, N * .34, 6), (N * .78, N * .56, 6)):
        d.ellipse([x - r, y - r, x + r, y + r], fill=(230, 210, 255))
    for (x, y) in ((N * .12, N * .78), (N * .86, N * .22)):
        d.ellipse([x - 6, y - 6, x + 6, y + 6], fill=(200, 170, 255))
    return im


def item_journal():
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([N * .16, N * .12, N * .84, N * .90], 18, fill=(92, 46, 22))
    d.rounded_rectangle([N * .20, N * .14, N * .80, N * .86], 14, fill=(150, 84, 42))
    d.rectangle([N * .74, N * .18, N * .82, N * .84], fill=(244, 228, 196))
    d.rectangle([N * .16, N * .12, N * .26, N * .90], fill=(110, 56, 26))
    # compass emblem
    c = (N * .50, N * .48)
    d.ellipse([c[0] - 44, c[1] - 44, c[0] + 44, c[1] + 44], fill=(230, 170, 60), outline=(150, 92, 24), width=5)
    d.polygon([(c[0], c[1] - 36), (c[0] + 9, c[1]), (c[0], c[1] + 36), (c[0] - 9, c[1])], fill=(200, 60, 40))
    d.polygon([(c[0], c[1]), (c[0] + 9, c[1]), (c[0], c[1] + 36), (c[0] - 9, c[1])], fill=(250, 240, 220))
    # ribbon
    d.polygon([(N * .60, N * .86), (N * .68, N * .86), (N * .68, N * .98), (N * .64, N * .94), (N * .60, N * .98)], fill=(40, 120, 50))
    # leaf
    leaf(d, N * .24, N * .20, 60, 30, -0.3)
    return im


ITEMS = {
    "ancient_coin": item_coin,
    "pearl_shell": item_pearl,
    "sky_crystal": lambda: crystal([(170, 226, 255), (110, 180, 250), (70, 130, 230), (200, 240, 255)], (40, 70, 160), (220, 245, 255)),
    "abyss_crystal": lambda: crystal([(40, 120, 140), (20, 70, 96), (60, 190, 210), (16, 46, 70)], (8, 24, 40), (90, 230, 240)),
    "blossom_charm": item_pouch,
    "spirit_leaf": item_leaf,
    "glow_mushroom": item_mushroom,
    "exploration_journal": item_journal,
}


def items():
    os.makedirs(f"{OUT}/item", exist_ok=True)
    res = {}
    for name, fn in ITEMS.items():
        big = outline_of(fn(), r=7)
        # pad so outline is not clipped
        small = down(big, 32, 32)
        small.save(f"{OUT}/item/{name}.png")
        res[name] = small
    return res


if __name__ == "__main__":
    j = journal()
    arrows()
    s = scroll()
    it = items()
    print("ok")
