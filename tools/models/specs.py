"""Model specs for the four monsters.

Units: 16 per block. Model space: y points DOWN, feet stand on y = 24, the face looks toward -z.
Part(name, parent, pivot, rot, boxes): pivot is the joint (shoulder, hip, neck) the part rotates around;
box offsets are relative to the pivot. Children inherit their parent's rotation.
Box(x, y, z, w, h, d, material, decor=None)
decor(img, face) draws onto a face image (pixels = units * S); face is top/bottom/right/front/left/back.
Face image axes: front/top/bottom: x grows toward +x. right(-x side): x grows toward the FRONT.
left(+x side): x grows toward the BACK. back: mirrored. y grows downward (toward the front on top faces).
"""
import math
from painter import Material, S, disc, ellipse, rect, darken, eye, col


class Box:
    def __init__(self, x, y, z, w, h, d, mat, decor=None):
        self.x, self.y, self.z, self.w, self.h, self.d = x, y, z, w, h, d
        self.mat, self.decor = mat, decor
        self.u = self.v = 0


class Part:
    def __init__(self, name, parent, pivot, rot=(0, 0, 0), boxes=()):
        self.name, self.parent, self.pivot, self.rot, self.boxes = name, parent, pivot, rot, list(boxes)


class Model:
    def __init__(self, name, method, tex_width, parts, preview_poses=None):
        self.name, self.method, self.tex_width, self.parts = name, method, tex_width, parts
        self.preview_poses = preview_poses or {}


def mirror_parts(parts, rename):
    """Copy right-side parts to the left side (x -> -x)."""
    out = []
    for p in parts:
        name = rename(p.name)
        parent = rename(p.parent) if p.parent else None
        px, py, pz = p.pivot
        rx, ry, rz = p.rot
        boxes = [Box(-(b.x + b.w), b.y, b.z, b.w, b.h, b.d, b.mat, getattr(b, "decor_mirror", b.decor)) for b in p.boxes]
        out.append(Part(name, parent, (-px, py, pz), (rx, -ry, -rz), boxes))
    return out


def R(name):
    return name.replace("right", "left")


# ================================================================== materials
GOBLIN_SKIN = Material((112, 152, 64), dark=(64, 98, 40), light=(150, 186, 84), kind="skin", grain=5, seed=11, spots=True)
GOBLIN_SKIN_DARK = Material((80, 116, 48), dark=(50, 80, 32), light=(104, 140, 60), kind="skin", grain=4, seed=12)
GOBLIN_EAR_IN = Material((168, 132, 92), dark=(130, 96, 70), light=(196, 156, 116), kind="skin", grain=3, seed=13)
HAIR_BLACK = Material((34, 30, 28), dark=(14, 12, 12), light=(70, 62, 56), kind="fur", grain=3, seed=14)
LEATHER = Material((112, 74, 42), dark=(70, 44, 24), light=(150, 104, 62), kind="leather", seed=15)
LEATHER_DARK = Material((70, 46, 30), dark=(40, 26, 16), light=(98, 66, 42), kind="leather", seed=16)
CLOTH_RAG = Material((118, 102, 74), dark=(78, 66, 46), light=(150, 134, 100), kind="cloth", seed=17)
CLOTH_RED = Material((126, 40, 34), dark=(80, 22, 20), light=(160, 64, 52), kind="cloth", seed=18)
PANTS = Material((74, 62, 50), dark=(46, 38, 30), light=(98, 84, 66), kind="cloth", seed=19)
WOOD = Material((120, 84, 50), dark=(76, 50, 28), light=(156, 114, 70), kind="wood", along="auto", seed=20)
WOOD_DARK = Material((82, 56, 34), dark=(48, 32, 18), light=(110, 78, 48), kind="wood", along="auto", seed=21)
IRON = Material((150, 150, 156), dark=(84, 84, 92), light=(214, 216, 222), kind="metal", along="auto", seed=22)
IRON_DARK = Material((84, 84, 90), dark=(44, 44, 50), light=(124, 124, 132), kind="metal", along="auto", seed=23)
STEEL_BLADE = Material((176, 180, 186), dark=(96, 100, 108), light=(232, 236, 240), kind="metal", along="auto", seed=24)
GOLD = Material((214, 162, 54), dark=(140, 92, 24), light=(250, 214, 120), kind="metal", along="auto", seed=25)
IVORY = Material((226, 214, 184), dark=(150, 130, 100), light=(246, 240, 222), kind="horn", along="auto", seed=26)
BONE = Material((218, 208, 182), dark=(160, 146, 118), light=(240, 234, 214), kind="skin", grain=3, seed=27)

ORC_SKIN = Material((96, 122, 74), dark=(56, 76, 44), light=(130, 156, 96), kind="skin", grain=6, seed=31)
ORC_SKIN_DARK = Material((70, 92, 54), dark=(40, 56, 30), light=(96, 120, 72), kind="skin", grain=4, seed=32)
FUR_GREY = Material((96, 88, 80), dark=(52, 46, 42), light=(140, 130, 118), kind="fur", grain=3, seed=33)

MINO_FUR = Material((108, 70, 44), dark=(60, 36, 22), light=(146, 100, 64), kind="fur", grain=4, seed=41)
MINO_FUR_DARK = Material((60, 40, 28), dark=(30, 20, 14), light=(90, 62, 42), kind="fur", grain=3, seed=42)
MINO_SKIN = Material((134, 92, 66), dark=(90, 58, 40), light=(170, 124, 92), kind="skin", grain=6, seed=43)
MINO_MUZZLE = Material((96, 70, 62), dark=(60, 42, 38), light=(126, 96, 86), kind="skin", grain=3, seed=44)
HORN = Material((222, 206, 170), dark=(66, 54, 44), light=(244, 234, 208), kind="horn", along="auto", seed=45)
HOOF = Material((54, 46, 42), dark=(28, 24, 22), light=(84, 74, 66), kind="solid", seed=46)

BOAR_FUR = Material((104, 72, 46), dark=(60, 40, 26), light=(140, 102, 66), kind="fur", grain=4, seed=51)
BOAR_FUR_DARK = Material((58, 40, 28), dark=(30, 22, 16), light=(86, 62, 44), kind="fur", grain=3, seed=52)
BOAR_SNOUT = Material((152, 104, 92), dark=(104, 68, 60), light=(184, 136, 120), kind="skin", grain=3, seed=53)


# ================================================================== decor helpers
def u(v):
    return v * S


def face_goblin(img, face):
    if face != "front":
        return
    h, w = img.shape[:2]          # 7 x 5.5 units
    darken(img, 0, u(1.2), w, u(2.0), 0.82)                     # heavy brow shadow
    eye(img, u(2.0), u(2.55), u(0.85), (246, 206, 40), slit=True)
    eye(img, u(5.0), u(2.55), u(0.85), (246, 206, 40), slit=True)
    rect(img, u(1.2), u(4.2), u(5.8), u(4.75), (44, 20, 18))     # wide grin
    for x in (1.6, 2.4, 3.3, 4.2, 5.0):                          # jagged teeth
        rect(img, u(x), u(4.2), u(x + 0.45), u(4.55), (232, 222, 190))
    darken(img, u(1.0), u(4.75), u(6.0), u(5.0), 0.8)
    for x, y in ((0.7, 1.0), (6.0, 3.5)):                        # warts
        disc(img, u(x), u(y), u(0.22), (90, 120, 50))


def face_orc(img, face):
    if face != "front":
        return
    h, w = img.shape[:2]          # 8.5 x 6.5
    darken(img, 0, u(1.6), w, u(2.6), 0.75)
    eye(img, u(2.6), u(3.0), u(0.6), (214, 40, 24), glow=False)
    eye(img, u(5.9), u(3.0), u(0.6), (214, 40, 24), glow=False)
    # scar across the left eye
    for k in range(int(u(3.2))):
        x, y = u(4.9) + k * 0.55, u(1.0) + k
        rect(img, x, y, x + 2, y + 2, (150, 86, 80), 0.9)
    # war paint stripes
    rect(img, u(1.6), u(3.9), u(3.4), u(4.2), (40, 30, 30), 0.55)
    rect(img, u(5.1), u(3.9), u(6.9), u(4.2), (40, 30, 30), 0.55)


def jaw_orc(img, face):
    if face != "front":
        return
    for x in (1.2, 2.3, 3.4, 4.5, 5.6):
        rect(img, u(x), 0, u(x + 0.6), u(0.6), (226, 216, 186))
    darken(img, 0, u(0.6), img.shape[1], u(0.9), 0.7)


def face_minotaur(img, face):
    if face != "front":
        return
    h, w = img.shape[:2]          # 11 x 7.5
    darken(img, 0, u(2.4), w, u(3.3), 0.7)
    eye(img, u(3.0), u(3.9), u(0.8), (232, 56, 24), glow=True)
    eye(img, u(8.0), u(3.9), u(0.8), (232, 56, 24), glow=True)
    # white blaze down the forehead
    ellipse(img, u(5.5), u(2.0), u(1.1), u(2.0), (196, 182, 160), 0.7)


def muzzle_minotaur(img, face):
    if face != "front":
        return
    ellipse(img, u(2.3), u(1.9), u(0.75), u(0.55), (26, 14, 12))
    ellipse(img, u(4.9), u(1.9), u(0.75), u(0.55), (26, 14, 12))
    darken(img, 0, u(3.6), img.shape[1], img.shape[0], 0.75)
    rect(img, u(1.0), u(4.0), u(6.2), u(4.3), (30, 18, 16))


def snout_boar(img, face):
    if face != "front":
        return
    ellipse(img, u(1.6), u(1.9), u(0.55), u(0.8), (40, 20, 20))
    ellipse(img, u(3.2), u(1.9), u(0.55), u(0.8), (40, 20, 20))


def boar_head_eyes(img, face):
    if face == "right":             # front is toward image right
        eye(img, img.shape[1] - u(2.0), u(2.6), u(0.55), (60, 30, 20))
    elif face == "left":
        eye(img, u(2.0), u(2.6), u(0.55), (60, 30, 20))


def vest_goblin(img, face):
    if face == "front":
        w = img.shape[1]
        rect(img, w / 2 - u(0.25), 0, w / 2 + u(0.25), img.shape[0], (40, 24, 14))   # open front
        for y in (0.8, 1.8, 2.8, 3.8):                                                 # lacing
            rect(img, w / 2 - u(0.9), u(y), w / 2 + u(0.9), u(y) + 2, (200, 170, 120))


def belt_buckle(img, face):
    if face == "front":
        w = img.shape[1]
        rect(img, w / 2 - u(0.6), 1, w / 2 + u(0.6), img.shape[0] - 1, (214, 170, 70))
        rect(img, w / 2 - u(0.35), u(0.3), w / 2 + u(0.35), img.shape[0] - u(0.3), (90, 60, 30))


def belt_skull(img, face):
    if face == "front":
        w = img.shape[1]
        disc(img, w / 2, u(1.3), u(1.1), (226, 214, 186))
        disc(img, w / 2 - u(0.4), u(1.2), u(0.25), (30, 20, 16))
        disc(img, w / 2 + u(0.4), u(1.2), u(0.25), (30, 20, 16))


def chest_muscle(img, face):
    if face == "front":
        h, w = img.shape[:2]
        darken(img, w / 2 - 2, u(1.0), w / 2 + 2, u(6.0), 0.75)       # sternum line
        darken(img, u(1.0), u(5.2), w - u(1.0), u(5.6), 0.8)          # under the pecs
        for y in (7.0, 9.0, 11.0):                                      # abs
            darken(img, w / 2 - u(2.5), u(y), w / 2 + u(2.5), u(y) + 2, 0.82)
        for k in range(int(u(4))):                                     # claw scar
            x, y = u(2.2) + k * 0.8, u(2.2) + k
            for dx in (0, u(0.7), u(1.4)):
                rect(img, x + dx, y, x + dx + 2, y + 2, (160, 96, 86), 0.8)


def rivets(img, face):
    if face in ("front", "left", "right", "back"):
        h, w = img.shape[:2]
        for x in range(int(u(0.6)), w - 2, int(u(1.4))):
            disc(img, x, u(0.5), 1.6, (176, 176, 182), 0.8)
            disc(img, x, h - u(0.5), 1.6, (176, 176, 182), 0.8)


def blade_edge(img, face):
    """Bright sharpened edge along the far side of an axe blade."""
    if face in ("right", "left"):
        h, w = img.shape[:2]
        rect(img, 0, h - u(0.8), w, h, (238, 242, 246), 0.85)
    if face == "bottom":
        img[..., :3] = img[..., :3] * 0.3 + col((240, 244, 248)) * 0.7


def hoof_split(img, face):
    if face == "front":
        w = img.shape[1]
        rect(img, w / 2 - 1, 0, w / 2 + 1, img.shape[0], (14, 10, 10))


def toes(img, face):
    if face == "front":
        w = img.shape[1]
        for x in (0.25, 1.0, 1.75):
            rect(img, u(x) + 1, 0, u(x + 0.6), img.shape[0], (200, 190, 160), 0.6)


def studs(img, face):
    h, w = img.shape[:2]
    for x in range(int(u(0.8)), w - 2, int(u(1.6))):
        for y in range(int(u(0.8)), h - 2, int(u(1.6))):
            disc(img, x, y, 1.6, (150, 150, 156), 0.85)


# ================================================================== GOBLIN (1.35 blocks)
def goblin():
    sk, skd = GOBLIN_SKIN, GOBLIN_SKIN_DARK
    right = [
        Part("right_ear", "head", (-3.5, -3.25, -0.5), (0, 0.3, 0.35), [
            Box(-2.5, -1.25, -0.5, 2.5, 2.5, 1, GOBLIN_EAR_IN),
            Box(-4.75, -0.75, -0.25, 2.25, 1.5, 0.75, sk),
            Box(-6.0, -0.5, -0.25, 1.25, 0.75, 0.5, sk),
            Box(-2.25, 1.0, -0.25, 0.5, 0.75, 0.5, GOLD)]),
        Part("right_arm", None, (-4.25, 9, 0), (0, 0, 0.06), [
            Box(-2.0, -0.75, -1.25, 2.25, 5, 2.25, sk),
            Box(-2.0, 4.0, -1.25, 2.5, 4.5, 2.5, sk),
            Box(-2.25, 4.75, -1.5, 3.0, 2.0, 3.0, LEATHER),
            Box(-2.0, 8.25, -1.25, 2.5, 1.25, 2.5, skd)]),
        Part("right_leg", None, (-1.75, 15, 0), (0, 0, 0), [
            Box(-1.5, 0, -1.5, 3.0, 4.5, 3.0, PANTS),
            Box(-1.25, 4.25, -1.25, 2.5, 3.75, 2.5, sk),
            Box(-1.5, 7.75, -2.75, 3.0, 1.25, 4.0, skd, decor=toes)]),
    ]
    parts = [
        Part("body", None, (0, 15, 0), (0.08, 0, 0), [
            Box(-3.25, -7, -1.75, 6.5, 7, 3.5, sk),
            Box(-3.5, -7.25, -2.0, 7.0, 4.5, 4.0, LEATHER, decor=vest_goblin),
            Box(-3.75, -2.5, -2.25, 7.5, 1.25, 4.5, LEATHER_DARK, decor=belt_buckle),
            Box(-2.25, -1.25, -2.5, 4.5, 4.5, 0.5, CLOTH_RAG),
            Box(-2.25, -1.25, 2.0, 4.5, 4.5, 0.5, CLOTH_RAG),
            Box(2.75, -1.75, -1.75, 1.5, 2.0, 1.5, LEATHER)]),
        Part("head", None, (0, 8, -0.5), (0, 0, 0), [
            Box(-3.5, -5.5, -3.5, 7, 5.5, 6.5, sk, decor=face_goblin),
            Box(-0.75, -6.5, -2.5, 1.5, 1.25, 5.0, HAIR_BLACK)]),
        Part("nose", "head", (0, -2.75, -3.5), (0.15, 0, 0), [
            Box(-0.75, -0.5, -2.0, 1.5, 1.75, 2.0, sk),
            Box(-0.5, 1.0, -2.25, 1.0, 0.5, 0.75, skd)]),
        Part("right_tusk", "head", (-1.5, -0.75, -3.5), (-0.2, 0, 0), [Box(-0.25, -1.0, -0.5, 0.5, 1.0, 0.5, IVORY)]),
        Part("club", "right_arm", (-0.75, 8.75, 0), (1.25, 0, 0), [
            Box(-0.5, -0.5, -6.5, 1.0, 1.0, 8.0, WOOD),
            Box(-1.25, -1.25, -10.75, 2.5, 2.5, 4.5, WOOD_DARK, decor=studs),
            Box(-0.25, -2.0, -9.5, 0.5, 0.75, 0.5, IRON),
            Box(1.25, -0.25, -8.5, 0.75, 0.5, 0.5, IRON),
            Box(-2.0, -0.25, -7.5, 0.75, 0.5, 0.5, IRON)]),
    ] + right
    left = mirror_parts([p for p in right if p.name in ("right_ear", "right_arm", "right_leg")], R)
    left += mirror_parts([p for p in parts if p.name == "right_tusk"], R)
    parts += left
    return Model("goblin", "goblin", 128, order(parts),
                 preview_poses={"swing": {"right_arm": (-2.2, 0, 0), "left_arm": (-1.3, 0, 0)}})


# ================================================================== ORC (2.35 blocks)
def orc():
    sk, skd = ORC_SKIN, ORC_SKIN_DARK
    right = [
        Part("right_arm", None, (-8.75, -6, 0), (0, 0, 0.05), [
            Box(-3.25, -1.5, -2.5, 5.0, 8.0, 5.0, sk),
            Box(-3.0, 6.0, -2.5, 4.75, 6.5, 5.0, sk),
            Box(-3.5, 7.0, -2.75, 5.5, 3.5, 5.5, IRON_DARK, decor=rivets),
            Box(-3.0, 12.0, -2.5, 4.75, 2.0, 5.0, skd),
            Box(-4.5, -3.25, -3.25, 6.5, 4.25, 6.5, IRON, decor=rivets),
            Box(-2.0, -5.5, -0.5, 1.25, 2.25, 1.25, IRON)]),
        Part("right_leg", None, (-3.5, 9, 0), (0, 0, 0), [
            Box(-2.5, 0, -2.5, 5.0, 8.0, 5.0, PANTS),
            Box(-2.25, 7.5, -2.25, 4.5, 3.5, 4.5, sk),
            Box(-2.0, 6.25, -3.0, 4.0, 2.5, 0.75, IRON),
            Box(-2.75, 10.5, -3.25, 5.5, 4.5, 6.25, LEATHER_DARK)]),
        Part("right_tusk", "head", (-2.5, -1.5, -5.0), (-0.25, 0, -0.1), [Box(-0.5, -2.75, -0.5, 1.0, 2.75, 1.0, IVORY)]),
        Part("right_ear", "head", (-4.25, -3.75, -1.0), (0, 0.35, 0.2), [Box(-2.5, -1.0, -0.5, 2.5, 1.5, 0.75, sk)]),
    ]
    parts = [
        Part("body", None, (0, 9, 0), (0.1, 0, 0), [
            Box(-6.5, -17, -3.75, 13.0, 12.5, 7.5, sk, decor=chest_muscle),
            Box(-6.0, -5.0, -3.5, 12.0, 5.0, 7.0, sk),
            Box(-7.0, -4.5, -4.0, 14.0, 3.0, 8.0, LEATHER, decor=belt_skull),
            Box(-3.0, -1.5, -4.25, 6.0, 7.5, 0.5, CLOTH_RED),
            Box(-3.0, -1.5, 3.75, 6.0, 7.5, 0.5, CLOTH_RED),
            Box(-7.0, -18.0, -4.25, 14.0, 2.5, 8.5, FUR_GREY)]),
        Part("strap", "body", (0, -11, 0), (0, 0, 0.7), [Box(-1.0, -10, -4.0, 2.0, 20.0, 8.0, LEATHER_DARK)]),
        Part("head", None, (0, -8, -1.5), (0, 0, 0), [
            Box(-4.25, -6.5, -4.5, 8.5, 6.5, 7.5, sk, decor=face_orc),
            Box(-4.5, -5.25, -5.0, 9.0, 1.25, 1.25, skd),
            Box(-1.25, -3.5, -5.25, 2.5, 1.75, 1.0, skd),
            Box(-1.0, -7.75, 0.5, 2.0, 1.5, 2.0, HAIR_BLACK),
            Box(-0.5, -7.0, 2.5, 1.0, 1.0, 3.0, HAIR_BLACK)]),
        Part("jaw", "head", (0, -1.5, 0), (0, 0, 0), [Box(-3.75, -0.25, -5.25, 7.5, 2.75, 5.0, sk, decor=jaw_orc)]),
        Part("axe", "right_arm", (-0.5, 13.0, 0), (1.2, 0, 0), [
            Box(-0.75, -0.75, -15.0, 1.5, 1.5, 19.5, WOOD),
            Box(-1.0, -1.0, -1.5, 2.0, 2.0, 3.0, LEATHER_DARK),
            Box(-1.0, -1.5, -14.75, 2.0, 3.0, 2.25, IRON_DARK),
            Box(-0.25, 1.25, -16.25, 0.5, 6.5, 6.25, STEEL_BLADE, decor=blade_edge),
            Box(-0.25, 7.5, -16.75, 0.5, 1.5, 3.0, STEEL_BLADE),
            Box(-0.5, -3.5, -14.25, 1.0, 2.25, 1.25, IRON),
            Box(-1.0, -1.0, 4.5, 2.0, 2.0, 1.25, IRON_DARK)]),
    ] + right
    parts += mirror_parts([p for p in right], R)
    return Model("orc", "orc", 128, order(parts),
                 preview_poses={"chop_up": {"right_arm": (-2.6, 0, 0)}, "slash": {"right_arm": (-1.4, 0.9, 0), "body": (0, -0.4, 0)}})


# ================================================================== WILD BOAR (1.5 blocks)
def wild_boar():
    legs = []
    for name, z in (("front", -6.0), ("back", 7.5)):
        legs.append(Part(f"{name}_right_leg", None, (-3.5, 14.5, z), (0, 0, 0), [
            Box(-2.25, 0, -2.25, 4.5, 4.5, 4.5, BOAR_FUR),
            Box(-1.75, 4.25, -1.75, 3.5, 3.75, 3.5, BOAR_FUR_DARK),
            Box(-1.75, 8.0, -2.0, 3.5, 1.5, 3.5, HOOF, decor=hoof_split)]))
    parts = [
        Part("body", None, (0, 10, 2), (0, 0, 0), [
            Box(-6.0, -5.5, -11.0, 12.0, 11.0, 21.0, BOAR_FUR),
            Box(-5.5, -5.0, 9.5, 11.0, 9.0, 2.5, BOAR_FUR),
            Box(-3.5, -8.5, -10.0, 7.0, 3.5, 13.0, BOAR_FUR_DARK)]),
        Part("tail", "body", (0, -4, 12), (0.7, 0, 0), [
            Box(-0.5, 0, -0.5, 1.0, 4.0, 1.0, BOAR_FUR),
            Box(-0.75, 3.5, -0.75, 1.5, 1.75, 1.5, BOAR_FUR_DARK)]),
        Part("head", None, (0, 8, -9), (0.2, 0, 0), [
            Box(-4.5, -4.0, -7.5, 9.0, 8.5, 8.0, BOAR_FUR, decor=boar_head_eyes),
            Box(-4.75, -4.25, -7.75, 9.5, 1.5, 2.0, BOAR_FUR_DARK)]),
        Part("snout", "head", (0, 1.5, -7.5), (0, 0, 0), [
            Box(-2.75, -1.5, -4.5, 5.5, 4.5, 4.5, BOAR_SNOUT),
            Box(-2.5, -1.25, -5.0, 5.0, 3.75, 0.5, BOAR_SNOUT, decor=snout_boar),
            Box(-2.5, 3.0, -4.0, 5.0, 1.25, 4.0, BOAR_SNOUT)]),
        Part("right_tusk", "snout", (-2.75, 2.5, -3.5), (-0.35, 0, 0.25), [Box(-0.5, -2.5, -0.5, 1.0, 2.75, 1.0, IVORY)]),
        Part("right_tusk_tip", "right_tusk", (0, -2.5, 0), (-0.6, 0, 0), [Box(-0.375, -2.0, -0.375, 0.75, 2.0, 0.75, IVORY)]),
        Part("right_ear", "head", (-3.5, -3.75, -2.5), (0.3, 0, 0.5), [Box(-1.25, -2.75, -0.5, 2.5, 3.0, 0.75, BOAR_FUR_DARK)]),
    ] + legs
    side = [p for p in parts if "right" in p.name]
    parts += mirror_parts(side, R)
    return Model("wild_boar", "wildBoar", 128, order(parts),
                 preview_poses={"ram": {"head": (0.6, 0, 0)}})


# ================================================================== MINOTAUR (3.2 blocks)
def minotaur():
    sk = MINO_SKIN
    right = [
        Part("right_arm", None, (-12.25, -20, 0), (0, 0, 0.08), [
            Box(-4.0, -1.5, -3.5, 7.0, 11.0, 7.0, sk),
            Box(-3.75, 9.0, -3.25, 6.5, 9.0, 6.5, sk),
            Box(-4.25, 11.5, -3.75, 7.5, 4.0, 7.5, IRON_DARK, decor=rivets),
            Box(-3.5, 17.5, -3.25, 6.25, 3.0, 6.5, MINO_MUZZLE),
            Box(-4.75, -3.25, -4.5, 8.5, 5.0, 9.0, MINO_FUR_DARK)]),
        Part("right_leg", None, (-5.0, 2, 0), (0, 0, 0), [
            Box(-3.75, 0, -3.75, 7.5, 9.0, 7.5, MINO_FUR),
            Box(-3.25, 8.5, -3.0, 6.5, 5.0, 6.0, MINO_FUR),
            Box(-2.5, 13.25, -2.25, 5.0, 7.0, 4.5, MINO_FUR_DARK),
            Box(-3.25, 18.5, -3.25, 6.5, 1.75, 6.5, MINO_FUR_DARK),
            Box(-3.0, 20.0, -3.25, 6.0, 2.0, 6.0, HOOF, decor=hoof_split)]),
        Part("right_horn", "head", (-5.25, -5.25, -1.5), (0, 0, 0), [Box(-4.5, -1.5, -1.5, 4.5, 3.0, 3.0, HORN)]),
        Part("right_horn_mid", "right_horn", (-4.25, 0, 0), (0, -0.25, 0.75), [Box(-4.0, -1.25, -1.25, 4.0, 2.5, 2.5, HORN)]),
        Part("right_horn_tip", "right_horn_mid", (-3.75, 0, 0), (0, 0, 0.75), [Box(-3.5, -0.75, -0.75, 3.5, 1.5, 1.5, HORN)]),
        Part("right_ear", "head", (-5.25, -3.25, 0.5), (0, -0.2, -0.5), [Box(-3.0, -0.75, -1.25, 3.0, 1.5, 2.5, MINO_MUZZLE)]),
    ]
    parts = [
        Part("body", None, (0, 2, 0), (0.06, 0, 0), [
            Box(-8.0, -10.0, -4.5, 16.0, 10.0, 9.0, sk),
            Box(-10.0, -23.5, -5.5, 20.0, 13.5, 11.0, sk, decor=chest_muscle),
            Box(-9.0, -25.0, -1.5, 18.0, 4.5, 8.0, MINO_FUR_DARK),
            Box(-8.5, -2.0, -5.0, 17.0, 3.0, 10.0, LEATHER, decor=belt_buckle),
            Box(-4.0, 0.5, -5.25, 8.0, 11.0, 0.5, CLOTH_RED),
            Box(-4.0, 0.5, 4.75, 8.0, 11.0, 0.5, CLOTH_RED),
            Box(-5.0, -22.0, -5.75, 10.0, 1.25, 0.5, BONE)]),
        Part("tail", "body", (0, -1, 4.5), (0.35, 0, 0), [
            Box(-0.5, 0, -0.5, 1.0, 12.0, 1.0, sk),
            Box(-1.0, 11.0, -1.0, 2.0, 3.0, 2.0, MINO_FUR_DARK)]),
        Part("head", None, (0, -21.5, -3), (0, 0, 0), [
            Box(-5.5, -7.0, -5.5, 11.0, 7.5, 9.5, MINO_FUR, decor=face_minotaur),
            Box(-4.0, -8.25, -5.75, 8.0, 2.0, 4.0, MINO_FUR_DARK)]),
        Part("muzzle", "head", (0, -2.25, -5.5), (0, 0, 0), [
            Box(-3.5, -2.0, -4.5, 7.0, 5.0, 4.5, MINO_MUZZLE, decor=muzzle_minotaur),
            Box(-3.25, 2.75, -4.25, 6.5, 1.5, 4.0, MINO_MUZZLE),
            Box(-1.25, 1.5, -5.0, 2.5, 1.5, 0.5, GOLD)]),
        Part("axe", "right_arm", (-0.5, 19.0, 0), (1.15, 0, 0), [
            Box(-1.0, -1.0, -26.0, 2.0, 2.0, 33.0, WOOD_DARK),
            Box(-1.25, -1.25, -1.5, 2.5, 2.5, 3.0, LEATHER_DARK),
            Box(-1.25, -1.25, -17.5, 2.5, 2.5, 1.5, GOLD),
            Box(-0.5, 1.0, -26.5, 1.0, 10.0, 9.0, STEEL_BLADE, decor=blade_edge),
            Box(-0.5, -11.0, -26.5, 1.0, 10.0, 9.0, STEEL_BLADE, decor=blade_edge),
            Box(-1.0, -2.25, -25.5, 2.0, 4.5, 7.0, IRON_DARK, decor=rivets),
            Box(-0.75, -0.75, -29.5, 1.5, 1.5, 3.5, IRON),
            Box(-1.5, -1.5, 7.0, 3.0, 3.0, 1.75, GOLD)]),
    ] + right
    parts += mirror_parts(right, R)
    return Model("minotaur", "minotaur", 256, order(parts),
                 preview_poses={"smash_up": {"right_arm": (-2.8, 0, 0), "left_arm": (-2.4, 0, 0)},
                                "charge": {"head": (0.6, 0, 0), "body": (0.3, 0, 0)}})


def order(parts):
    """Parents before children."""
    done, out = set(), []
    pending = list(parts)
    while pending:
        rest = []
        for p in pending:
            if p.parent is None or p.parent in done:
                out.append(p)
                done.add(p.name)
            else:
                rest.append(p)
        if len(rest) == len(pending):
            raise ValueError("missing parent: " + ", ".join(p.name + "<-" + str(p.parent) for p in rest))
        pending = rest
    return out


MODELS = [goblin(), orc(), wild_boar(), minotaur()]
