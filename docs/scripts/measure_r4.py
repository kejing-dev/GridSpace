"""QA R4 / R5 / R7: measure how many screen pixels one dp gets on the 2160-px emulator captures.
Planar windows (entry, Sheet) scale dynamically; the Volume and everything attached to it
(Augments, SpatialPopup) keep their physical size, so the same dp renders smaller there. R5 wraps
every side card in EditorScaled (x1.5 side cards, x1.3 brush row); R7 renders the front-plane cards
(target label 360 x 96 at z 0.92, colour card 560 x 624 at z 0.90) unscaled.

Usage: python -P docs/scripts/measure_r4.py [root=docs/screenshots/r7] [prefix=r7]
       (Pillow lives in the user site, so -P rather than -I; writes nothing)
Every block is skipped when its frames are missing, so the script works on the full R5 set and on
the partial R6 / R7 sets. Regions are for the default emulator viewpoint of the captures; the rail,
brush row and buttons are located by colour search inside a generous region, cards by image
difference against a frame without the card.
"""
import sys
from statistics import median
from PIL import Image, ImageChops

ROOT = sys.argv[1] if len(sys.argv) > 1 else 'docs/screenshots/r8'
PREFIX = sys.argv[2] if len(sys.argv) > 2 else 'r8'
R5 = PREFIX != 'r4'
SIDE = 1.5 if R5 else 1.0
BRUSH = 1.3 if R5 else 1.0
LABEL = 1.0 if PREFIX >= 'r7' else (1.4 if R5 else 1.0)
LABEL_W, LABEL_H = (360, 96) if PREFIX >= 'r7' else (440, 120)
CARD_SCALE = 1.0 if PREFIX >= 'r7' else SIDE
SWATCH_DP = 88 if R5 else 80
SLOT_DP = 88 if R5 else 80
STEP_DP = 84 if R5 else 72
BRUSH_ITEM_DP = 72 if R5 else 64


def img(lang, name):
    try:
        return Image.open(f'{ROOT}/{lang}/{PREFIX}-{name}.png').convert('RGB')
    except FileNotFoundError:
        return None


def bbox(im, region, pred):
    x0, y0, _, _ = region
    c = im.crop(region)
    px = c.load()
    xs, ys = [], []
    for y in range(c.height):
        for x in range(c.width):
            if pred(*px[x, y]):
                xs.append(x)
                ys.append(y)
    return None if not xs else (x0 + min(xs), y0 + min(ys), x0 + max(xs) + 1, y0 + max(ys) + 1)


def diffbbox(a, b, region, thr=28):
    d = ImageChops.difference(a.crop(region), b.crop(region)).convert('L').point(lambda v: 255 if v > thr else 0)
    bb = d.getbbox()
    return None if bb is None else (region[0] + bb[0], region[1] + bb[1], region[0] + bb[2], region[1] + bb[3])


def runs(im, y, x0, x1, pred, min_len):
    """Horizontal runs of pixels matching pred on row y, as (start, length)."""
    px = im.load()
    out, start = [], None
    for x in range(x0, x1):
        hit = pred(*px[x, y])
        if hit and start is None:
            start = x
        if not hit and start is not None:
            if x - start >= min_len:
                out.append((start, x - start))
            start = None
    if start is not None and x1 - start >= min_len:
        out.append((start, x1 - start))
    return out


def vruns(im, x, y0, y1, pred, min_len):
    px = im.load()
    out, start = [], None
    for y in range(y0, y1):
        hit = pred(*px[x, y])
        if hit and start is None:
            start = y
        if not hit and start is not None:
            if y - start >= min_len:
                out.append((start, y - start))
            start = None
    return out


mint = lambda r, g, b: g > 190 and g - r > 20 and g - b > 10
sat = lambda r, g, b: max(r, g, b) - min(r, g, b) > 60
coral = lambda r, g, b: r > 200 and 150 < g < 215 and 120 < b < 190 and r - b > 40
# Stepper IconButton container on the selection panel: warm mid grey (~155, 145, 138) on the lighter glass (~198).
disc = lambda r, g, b: 140 <= r <= 170 and 132 <= g <= 160 and 125 <= b <= 152 and r - b < 30
# The selected segment item of the brush row is a uniform light (~177) or dark (~102) capsule on the glass (~146).
capsule = lambda r, g, b: abs(r - g) < 8 and abs(g - b) < 8 and (95 <= r <= 112 or 168 <= r <= 200)


def swatch_grid(im, region=(1680, 500, 2160, 1800)):
    """Swatch cell size and grid bbox from saturated runs (rows with >= 3 swatches)."""
    x0, y0, x1, y1 = region
    rows = []
    for y in range(y0, y1, 4):
        r = runs(im, y, x0, x1, sat, 40)
        if len(r) >= 3:
            rows.append((y, r))
    if not rows:
        return None
    widths = [w for _, r in rows for _, w in r]
    left = min(s for _, r in rows for s, _ in r)
    right = max(s + w for _, r in rows for s, w in r)
    cx = left + max(widths) // 2
    heights = [h for _, h in vruns(im, cx, y0, y1, sat, 40)]
    return dict(w=max(widths), w_med=median(widths), h=max(heights) if heights else None, left=left, right=right, top=rows[0][0], bottom=rows[-1][0], cols=max(len(r) for _, r in rows))


def fmt(px, dp, scale):
    if px is None or dp is None:
        return '-'
    return f'{px / dp:.2f} px/design-dp ({px / (dp * scale):.2f} px/Volume-dp)'


def card_edges(a, b, region):
    """Bbox of the card in `a` against `b` (no card) from per-row diff extents (median over rows)."""
    d = ImageChops.difference(a.crop(region), b.crop(region)).convert('L').point(lambda v: 255 if v > 28 else 0)
    dp = d.load()
    lefts, rights, ys = [], [], []
    for y in range(0, d.height, 2):
        xs = [x for x in range(d.width) if dp[x, y]]
        if len(xs) > 40:
            lefts.append(region[0] + min(xs))
            rights.append(region[0] + max(xs))
            ys.append(region[1] + y)
    if not ys:
        return None
    lefts.sort()
    rights.sort()
    return (lefts[len(lefts) // 2], ys[0], rights[len(rights) // 2], ys[-1])


for lang in ('zh', 'en'):
    print(f'== {lang}')
    e, w, g, dm, ed, rl, bar = (img(lang, n) for n in ('entry', 'workbench', 'ghost', 'dragmove', 'edit', 'rail', 'bar'))
    size = img(lang, 'size')
    occ, box, edc, eye = (img(lang, n) for n in ('occupied', 'box', 'edit-close', 'eyedrop'))

    if e is not None:
        bb = bbox(e, (300, 900, 1050, 1100), mint)
        if bb:
            print(f'  entry: new-project button 440 x 80 -> {bb[2]-bb[0]} x {bb[3]-bb[1]} px  {fmt(bb[2]-bb[0], 440, 1)} / {fmt(bb[3]-bb[1], 80, 1)}')

    rail_src = rl if rl is not None else (edc if edc is not None else occ)
    grid = swatch_grid(rail_src) if rail_src is not None else None
    rail_box = None
    if grid:
        pad = round(24 * grid['w'] / SWATCH_DP)
        rail_box = (grid['left'] - pad, 0, grid['right'] + pad, 2158)
        print(f'  rail: swatch {SWATCH_DP} dp -> {grid["w"]} x {grid["h"]} px (median w {grid["w_med"]:.0f}), {grid["cols"]} cols, grid x {grid["left"]}..{grid["right"]}  {fmt(grid["w"], SWATCH_DP, SIDE)}; swatch >= 92 px: {"OK" if grid["w"] >= 92 and (grid["h"] or 0) >= 92 else "FAIL"}')
        slot_src = w if w is not None else occ
        if slot_src is not None:
            sb = bbox(slot_src, (grid['left'] - 40, max(0, grid['top'] - 520), grid['right'] + 40, grid['top'] - 120), coral)
            if sb:
                sw = sb[2] - sb[0]
                print(f'  rail: slot disc 72 dp (hit {SLOT_DP}) -> {sw} x {sb[3]-sb[1]} px  {fmt(sw, 72, SIDE)}; hit area ~ {sw * SLOT_DP / 72:.0f} px -> {"OK" if sw * SLOT_DP / 72 >= 92 else "FAIL"}')

    # Target label cards (R6/R7: above the target on the front plane). Reference frame = a frame
    # of the same session without that card: workbench (R5/R6) or edit-close (R7, label-free).
    ref = w if w is not None else edc
    if ref is not None:
        for name, a, region in (('ghost', g, (0, 500, 1600, 1250)), ('occupied', occ, (0, 500, 1600, 1250)), ('bar', bar, (0, 500, 1600, 1300)), ('size', size, (0, 500, 1600, 1140)), ('box', box, (900, 700, 2160, 1500))):
            if a is None:
                continue
            # The box frame shares the Soft page with `occupied` (edit-close is on Vivid), so use it as the reference.
            cb = card_edges(a, occ if (name == 'box' and occ is not None) else ref, region)
            if cb:
                cw, ch = cb[2] - cb[0], cb[3] - cb[1]
                hit = rail_box is not None and cb[2] >= rail_box[0]
                print(f'  label card ({name}) {LABEL_W} x {LABEL_H} -> {cw} x {ch} px  {fmt(cw, LABEL_W, LABEL)} / {fmt(ch, LABEL_H, LABEL)}  (bbox {cb}); overlaps rail glass (x >= {rail_box[0] if rail_box else "?"}): {"YES" if hit else "no"}')

    if dm is not None and w is not None:
        pb = diffbbox(dm, w, (0, 1000, 540, 1850))
        if pb:
            pw, ph = pb[2] - pb[0], pb[3] - pb[1]
            print(f'  selection panel 400 x {584 if R5 else 548} (diff dragmove vs workbench, may include the moved cubes) -> {pw} x {ph} px  {fmt(pw, 400, SIDE)}  (bbox {pb})')
            discs = []
            for y in range(pb[1] + ph // 2, pb[3], 3):
                for s, l in runs(dm, y, pb[0], pb[0] + 400, disc, 40):
                    if l <= 115:
                        discs.append((l, y, s))
            if discs:
                dmax, dy, dx = max(discs)
                print(f'  selection panel: stepper button {STEP_DP} dp -> {dmax} px wide  {fmt(dmax, STEP_DP, SIDE)}; >= 86 px: {"OK" if dmax >= 86 else "FAIL"}')

    # Colour-edit card: R5 popup (diff edit vs rail) or R6/R7 Augment (diff edit vs edit-close).
    if ed is not None:
        cref = edc if edc is not None else rl
        if cref is not None:
            region = (0, 0, 2160, 2158) if PREFIX >= 'r6' else (1200, 300, 2160, 1900)
            cb = card_edges(ed, cref, region) if PREFIX >= 'r6' else diffbbox(ed, cref, region)
            if cb:
                print(f'  colour card 560 x 624 -> {cb[2]-cb[0]} x {cb[3]-cb[1]} px  {fmt(cb[2]-cb[0], 560, CARD_SCALE)} / {fmt(cb[3]-cb[1], 624, CARD_SCALE)}  (bbox {cb}); right edge inside 2160: {"yes" if cb[2] < 2150 else "NO"}')
                # Done: the only mint run wider than 400 px (the rail's mint swatches are ~96 px).
                done_rows = [(y, s, l) for y in range(cb[1] + (cb[3] - cb[1]) * 2 // 3, min(cb[3] + 10, 2158), 2) for s, l in runs(ed, y, cb[0], min(cb[2] + 10, 2160), mint, 400)]
                db = (min(s for _, s, _ in done_rows), done_rows[0][0], max(s + l for _, s, l in done_rows), done_rows[-1][0] + 2) if done_rows else None
                if db:
                    print(f'  colour card: Done button 80 high -> {db[2]-db[0]} x {db[3]-db[1]} px  {fmt(db[3]-db[1], 80, CARD_SCALE)}')
                    strip = ed.crop((db[0], db[3], db[2], min(db[3] + 120, 2158)))
                    spx = strip.load()
                    n = sum(1 for y in range(strip.height) for x in range(strip.width) if max(spx[x, y]) < 12)
                    print(f'  colour card: near-black share under Done = {n / (strip.width * strip.height):.3f} (expect 0.000)')
    if eye is not None:
        # The bar dims the rail, so no card-free reference exists; measure its Cancel button (72 dp,
        # dark container): the longest bounded dark run on the bar's rows, then a vertical run at its centre.
        dark_btn = lambda r, g, b: r < 62 and g < 62 and b < 62
        best = None
        for y in range(880, 1040, 4):
            for s, l in runs(eye, y, 1500, 2160, dark_btn, 100):
                if l <= 320 and (best is None or l > best[0]):
                    best = (l, y, s)
        if best:
            l, y, s = best
            # The light button text breaks a single run, so take the dark span at the centre column.
            epx = eye.load()
            dark_ys = [yy for yy in range(820, 1100) if dark_btn(*epx[s + l // 2, yy])]
            h = (max(dark_ys) - min(dark_ys) + 1) if dark_ys else None
            print(f'  eyedropper bar: Cancel button 72 high -> {l} x {h} px  {fmt(h, 72, CARD_SCALE)}  (x {s}..{s + l}); right edge inside 2160: {"yes" if s + l < 2150 else "NO"}')

    # Brush row. R8 (prefix >= r8): three steppers, unscaled on the front plane; the [-] / [+]
    # IconButtons are dark discs (72 dp) in the bottom band. Earlier sets: the selected segment capsule.
    if PREFIX >= 'r8':
        src = w if w is not None else img(lang, 'size')
        if src is not None:
            dark_btn = lambda r, g, b: r < 62 and g < 62 and b < 62
            rows = []
            for y in range(1880, 2080, 2):
                r = [(s, l) for s, l in runs(src, y, 250, 1960, dark_btn, 90) if l <= 140]
                if len(r) >= 5:
                    rows.append((y, r))
            if rows:
                widths = sorted(l for _, r in rows for _, l in r)
                ys = [y for y, _ in rows]
                print(f'  brush row: stepper button 72 dp -> {widths[len(widths) // 2]} px wide (min {widths[0]}, max {widths[-1]}), {max(ys) - min(ys) + 2} px high, rows y {min(ys)}..{max(ys)} (frame 2158)  {fmt(widths[len(widths) // 2], 72, 1.0)}; >= 92 px: {"OK" if widths[len(widths) // 2] >= 92 else "FAIL"}')
    else:
        for name, im in (('workbench', w), ('bar', bar)):
            if im is None:
                continue
            best = None
            for y in range(1870, 2050, 4):
                for s, l in runs(im, y, 300, 1000, capsule, 100):
                    if l <= 320 and (best is None or l > best[0]):
                        best = (l, y, s)
            if best:
                l, y, s = best
                h = max((hh for _, hh in vruns(im, s + l // 6, 1800, 2100, capsule, 40)), default=None)
                print(f'  brush row ({name}): selected item 120 x {BRUSH_ITEM_DP} -> {l} x {h} px  {fmt(l, 120, BRUSH)} / {fmt(h, BRUSH_ITEM_DP, BRUSH)}; height >= 86 px: {"OK" if (h or 0) >= 86 else "FAIL"}')

    for name, region in (('projects', (600, 450, 1700, 800)), ('export', (600, 1350, 1700, 1750))):
        sh = img(lang, name)
        if sh is None:
            continue
        bb = bbox(sh, region, mint)
        if bb:
            print(f'  {name} sheet: primary button (80 high) -> {bb[2]-bb[0]} x {bb[3]-bb[1]} px  {fmt(bb[3]-bb[1], 80, 1)}; width = {(bb[2]-bb[0]) / ((bb[3]-bb[1]) / 80):.0f} dp at that scale')
