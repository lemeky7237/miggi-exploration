import sys
import numpy as np
from PIL import Image
from lib import *
OUT = sys.argv[1]
S = 8  # 16x16 GUI arrow, drawn at 128 then saved at 32 per frame
img = Image.new("RGBA", (32 * S, 16 * S), (0, 0, 0, 0))
for i, (dark, light) in enumerate((((96, 58, 30), (160, 104, 56)), ((150, 92, 24), (250, 196, 96)))):
    ox = i * 16 * S
    m = mask_from(img.size, lambda d: d.polygon([(ox + 3 * S, 2.5 * S), (ox + 13.5 * S, 8 * S), (ox + 3 * S, 13.5 * S), (ox + 6 * S, 8 * S)], fill=255))
    h = bevel(m, 1.2 * S)
    layer = shade(h, ramp(h, [(0, dark), (1, light)]), m, strength=10, spec=0.4, ambient=0.6)
    img.alpha_composite(drop_shadow(layer, (0, S // 2), S // 2, 0.4))
    img.alpha_composite(layer)
img.resize((64, 32), Image.LANCZOS).save(f"{OUT}/arrows.png")
