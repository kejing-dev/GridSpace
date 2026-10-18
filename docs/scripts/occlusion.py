"""Occlusion / clipping check: share of "background that should not show" pixels inside a card's
bounding box, with an optional control region just outside the card for comparison.

GridSpace K4.73: the brush row at z 0.97 must sit in front of the tilted base plate; the check
counts plate grid-line / grey-blue plate pixels inside the row's bbox (expected 0.0000) against the
band above the row (0.0003-0.0005, proving the predicate sees the plate). The same check detects a
card clipped by the frame (bbox touching 0 / 2160) and a black glass band (pred nearblack).

Usage: python -P occlusion.py <frame.png> x0 y0 x1 y1 [--pred "r<120 and 90<b<140 and b>r"] [--max 0.0]
                               [--control x0 y0 x1 y1] [--pred-name nearblack]
Exit 0 = PASS. Source: QA_REPORT_R8 §1.1 (K4.73 method), QA_REPORT_R4 §1 "黑卡扫描".
"""
import sys

from PIL import Image

PREDS = {
    'nearblack': lambda r, g, b: max(r, g, b) < 12,
    'nearwhite': lambda r, g, b: min(r, g, b) > 248,
    # base plate of GridSpace: BaseInk #2D383B @70% over glass + grid line #688681 @40% read as cool grey-blue
    'plate': lambda r, g, b: 60 <= r <= 130 and 80 <= g <= 145 and 85 <= b <= 150 and b >= r,
}


def share(im, region, pred, step=1):
    c = im.crop(region)
    p = c.load()
    n = tot = 0
    for y in range(0, c.height, step):
        for x in range(0, c.width, step):
            tot += 1
            if pred(*p[x, y]):
                n += 1
    return n / max(tot, 1)


def main():
    a = sys.argv[1:]
    frame = a[0]
    region = tuple(int(v) for v in a[1:5])
    rest = a[5:]
    pred = PREDS['nearblack']
    mx = 0.0
    control = None
    i = 0
    while i < len(rest):
        if rest[i] == '--pred':
            pred = eval(f'lambda r, g, b: {rest[i + 1]}', {'__builtins__': {}}, {'abs': abs, 'max': max, 'min': min}); i += 2
        elif rest[i] == '--pred-name':
            pred = PREDS[rest[i + 1]]; i += 2
        elif rest[i] == '--max':
            mx = float(rest[i + 1]); i += 2
        elif rest[i] == '--control':
            control = tuple(int(v) for v in rest[i + 1:i + 5]); i += 5
        else:
            i += 1
    im = Image.open(frame).convert('RGB')
    w, h = im.size
    s = share(im, region, pred)
    clipped = region[0] <= 0 or region[1] <= 0 or region[2] >= w or region[3] >= h
    print(f'{frame}: share {s:.4f} in {region} (max {mx})' + (f'; control {share(im, control, pred):.4f}' if control else '') + ('; bbox touches the frame edge' if clipped else ''))
    ok = s <= mx + 1e-9 and not clipped
    print('PASS' if ok else 'FAIL')
    sys.exit(0 if ok else 1)


main()
