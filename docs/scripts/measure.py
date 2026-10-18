"""Pixel measurement of named regions on 2160-px emulator captures, driven by a JSON spec.

Planar windows (entry, Sheet) scale dynamically; the Volume and everything attached to it keeps
its physical size, so the same dp renders fewer px there (0.70-0.80 px/Volume-dp on side cards,
1.6 on the front plane). This script turns "is the target big enough" into numbers the gate can
compare: px, px/design-dp, px/Volume-dp (= px / (dp * scale)) and PASS/FAIL against thresholds.

Usage: python -P measure.py <root> <prefix> <spec.json> [--langs zh,en] [--json out.json]   (also --langs=zh,en / --json=out.json)
       (Pillow lives in the user site, so -P rather than -I; reads frames <root>/<lang>/<prefix>-<frame>.png,
        or <root>/<prefix>-<frame>.png when --langs is empty; writes nothing unless --json)

Spec (see measure-example.json):
  {"preds": {"mint": "g > 190 and g - r > 20 and g - b > 10", ...},        # optional extra colour predicates (r, g, b ints)
   "checks": [
     {"name": "...", "frame": "entry", "kind": "color", "pred": "mint", "region": [x0,y0,x1,y1],
      "dp": [440, 80], "scale": 1.0, "min_px": [0, 92]},                   # bbox of pixels matching pred
     {"name": "...", "frame": "rail", "kind": "grid", "pred": "sat", "region": [...], "dp": 88, "scale": 1.5, "min_px": 92},  # swatch grid cell size
     {"name": "...", "frame": "ghost", "ref": "workbench", "kind": "diff", "region": [...], "dp": [360, 96], "scale": 1.0,
      "left_of": "rail", "margin_px": 0},                                   # card bbox by difference against a card-free frame
     {"name": "...", "frame": "workbench", "kind": "runs", "pred": "dark", "region": [...], "min_len": 90, "max_len": 140,
      "min_count": 5, "dp": 72, "scale": 1.0, "min_px": 92},               # median width of round buttons (horizontal dark runs)
     {"name": "...", "frame": "edit", "kind": "black_share", "region": [...], "max": 0.0}   # near-black share (glass lag band)
   ]}
Every check is skipped (reported as "skip") when its frame is missing, so partial sets work.
Source: GridSpace docs/scripts/measure_r4.py (helpers kept verbatim: bbox, diffbbox, runs, vruns,
swatch_grid, card_edges); the JSON driver replaces the hard-coded R4..R8 blocks.
"""
import json
import sys
from statistics import median

from PIL import Image, ImageChops

PREDS = {
    'mint': lambda r, g, b: g > 190 and g - r > 20 and g - b > 10,
    'sat': lambda r, g, b: max(r, g, b) - min(r, g, b) > 60,
    'coral': lambda r, g, b: r > 200 and 150 < g < 215 and 120 < b < 190 and r - b > 40,
    'dark': lambda r, g, b: r < 62 and g < 62 and b < 62,
    'disc': lambda r, g, b: 140 <= r <= 170 and 132 <= g <= 160 and 125 <= b <= 152 and r - b < 30,
    'capsule': lambda r, g, b: abs(r - g) < 8 and abs(g - b) < 8 and (95 <= r <= 112 or 168 <= r <= 200),
    'nearblack': lambda r, g, b: max(r, g, b) < 12,
}


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


def swatch_grid(im, pred, region):
    """Swatch cell size and grid bbox from saturated runs (rows with >= 3 swatches)."""
    x0, y0, x1, y1 = region
    rows = []
    for y in range(y0, y1, 4):
        r = runs(im, y, x0, x1, pred, 40)
        if len(r) >= 3:
            rows.append((y, r))
    if not rows:
        return None
    widths = [w for _, r in rows for _, w in r]
    left = min(s for _, r in rows for s, _ in r)
    right = max(s + w for _, r in rows for s, w in r)
    cx = left + max(widths) // 2
    heights = [h for _, h in vruns(im, cx, y0, y1, pred, 40)]
    return dict(w=max(widths), w_med=median(widths), h=max(heights) if heights else None, left=left, right=right,
                top=rows[0][0], bottom=rows[-1][0], cols=max(len(r) for _, r in rows))


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


def fmt(px, dp, scale):
    if px is None or dp is None:
        return '-'
    return f'{px / dp:.2f} px/design-dp ({px / (dp * scale):.2f} px/Volume-dp)'


def main():
    # options take a value as `--langs zh,en` or `--langs=zh,en`; `--langs ''` / `--langs=` = no language folders
    args, opts, argv, i = [], {}, sys.argv[1:], 0
    while i < len(argv):
        a = argv[i]
        if a.startswith('--'):
            if '=' in a:
                k, v = a.split('=', 1)
            elif a in ('--langs', '--json') and i + 1 < len(argv):
                k, v = a, argv[i + 1]
                i += 1
            else:
                k, v = a, True
            opts[k] = v
        else:
            args.append(a)
        i += 1
    if len(args) < 3:
        sys.exit('usage: measure.py <root> <prefix> <spec.json> [--langs zh,en] [--json out.json]')
    root, prefix, spec_path = args[0], args[1], args[2]
    lang_opt = opts.get('--langs', 'zh,en')
    langs = str(lang_opt).split(',') if isinstance(lang_opt, str) and lang_opt != '' else ['']
    spec = json.load(open(spec_path))
    preds = dict(PREDS)
    for name, expr in spec.get('preds', {}).items():
        preds[name] = eval(f'lambda r, g, b: {expr}', {'__builtins__': {}}, {'abs': abs, 'max': max, 'min': min})
    results = []
    overall = True
    for lang in langs:
        folder = f'{root}/{lang}' if lang else root
        cache = {}

        def img(name):
            if name not in cache:
                try:
                    cache[name] = Image.open(f'{folder}/{prefix}-{name}.png').convert('RGB')
                except FileNotFoundError:
                    cache[name] = None
            return cache[name]

        print(f'== {lang or root}')
        named = {}
        for c in spec['checks']:
            im = img(c['frame'])
            row = dict(lang=lang, name=c['name'], frame=c['frame'], status='skip')
            if im is None:
                print(f'  {c["name"]}: skip (frame {c["frame"]} missing)')
                results.append(row)
                continue
            kind = c['kind']
            scale = c.get('scale', 1.0)
            region = tuple(c['region'])
            ok = True
            if kind == 'color':
                bb = bbox(im, region, preds[c['pred']])
                if bb:
                    w, h = bb[2] - bb[0], bb[3] - bb[1]
                    dpw, dph = c.get('dp', [None, None])
                    mins = c.get('min_px', [0, 0])
                    ok = w >= mins[0] and h >= mins[1]
                    row.update(px=[w, h], bbox=bb)
                    print(f'  {c["name"]}: {dpw} x {dph} dp -> {w} x {h} px  {fmt(w, dpw, scale)} / {fmt(h, dph, scale)} (bbox {bb}) {"OK" if ok else "FAIL"}')
                else:
                    ok = False
                    print(f'  {c["name"]}: no {c["pred"]} pixels in {region} FAIL')
            elif kind == 'grid':
                grid = swatch_grid(im, preds[c['pred']], region)
                if grid:
                    ok = grid['w'] >= c.get('min_px', 0) and (grid['h'] or 0) >= c.get('min_px', 0)
                    named[c['name']] = (grid['left'], grid['top'], grid['right'], grid['bottom'])
                    row.update(px=[grid['w'], grid['h']], bbox=named[c['name']])
                    print(f'  {c["name"]}: cell {c.get("dp")} dp -> {grid["w"]} x {grid["h"]} px (median w {grid["w_med"]:.0f}), {grid["cols"]} cols, x {grid["left"]}..{grid["right"]}  {fmt(grid["w"], c.get("dp"), scale)} {"OK" if ok else "FAIL"}')
                else:
                    ok = False
                    print(f'  {c["name"]}: no grid found FAIL')
            elif kind == 'diff':
                ref = img(c['ref'])
                cb = card_edges(im, ref, region) if ref is not None else None
                if cb:
                    w, h = cb[2] - cb[0], cb[3] - cb[1]
                    dpw, dph = c.get('dp', [None, None])
                    row.update(px=[w, h], bbox=cb)
                    msg = f'  {c["name"]}: {dpw} x {dph} dp -> {w} x {h} px  {fmt(w, dpw, scale)} / {fmt(h, dph, scale)} (bbox {cb})'
                    other = named.get(c.get('left_of', ''))
                    if other:
                        ok = cb[2] + c.get('margin_px', 0) <= other[0]
                        msg += f'; right edge {cb[2]} vs {c["left_of"]} left edge {other[0]}: {"no overlap" if ok else "OVERLAP"}'
                    mins = c.get('min_px')
                    if mins:
                        ok = ok and w >= mins[0] and h >= mins[1]
                    print(msg + (' OK' if ok else ' FAIL'))
                else:
                    ok = False
                    print(f'  {c["name"]}: no difference against {c.get("ref")} FAIL')
            elif kind == 'runs':
                x0, y0, x1, y1 = region
                rows = []
                for y in range(y0, y1, 2):
                    r = [(s, l) for s, l in runs(im, y, x0, x1, preds[c['pred']], c.get('min_len', 60)) if l <= c.get('max_len', 10 ** 6)]
                    if len(r) >= c.get('min_count', 1):
                        rows.append((y, r))
                if rows:
                    widths = sorted(l for _, r in rows for _, l in r)
                    ys = [y for y, _ in rows]
                    med = widths[len(widths) // 2]
                    ok = med >= c.get('min_px', 0)
                    row.update(px=[med, max(ys) - min(ys) + 2], bbox=(x0, min(ys), x1, max(ys)))
                    print(f'  {c["name"]}: {c.get("dp")} dp -> median {med} px wide (min {widths[0]}, max {widths[-1]}), rows y {min(ys)}..{max(ys)}  {fmt(med, c.get("dp"), scale)} {"OK" if ok else "FAIL"}')
                else:
                    ok = False
                    print(f'  {c["name"]}: no runs found FAIL')
            elif kind == 'black_share':
                crop = im.crop(region)
                p = crop.load()
                n = sum(1 for y in range(crop.height) for x in range(crop.width) if preds['nearblack'](*p[x, y]))
                share = n / (crop.width * crop.height)
                ok = share <= c.get('max', 0.0) + 1e-9
                row.update(share=share)
                print(f'  {c["name"]}: near-black share {share:.3f} in {region} (max {c.get("max", 0.0)}) {"OK" if ok else "FAIL"}')
            row['status'] = 'pass' if ok else 'fail'
            overall = overall and ok
            results.append(row)
    if isinstance(opts.get('--json'), str):
        with open(opts['--json'], 'w') as out:
            json.dump(dict(overall='pass' if overall else 'fail', results=results), out, indent=1)
    print('OVERALL', 'PASS' if overall else 'FAIL')
    sys.exit(0 if overall else 1)


main()
