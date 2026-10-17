"""Dump a MagicaVoxel .vox written by GridSpace: size, voxel count, colour histogram, optional cell query.
Usage: python3 -I docs/scripts/voxdump.py file.vox [x,y_up,z ...]   (cells in app coordinates: x, y up, z)
"""
import struct, sys, collections

def main():
    path = sys.argv[1]
    data = open(path, 'rb').read()
    assert data[:4] == b'VOX ', 'not a vox file'
    pos = 8
    size = None; voxels = []; palette = None
    while pos < len(data):
        cid = data[pos:pos + 4]; n, m = struct.unpack('<ii', data[pos + 4:pos + 12]); body = data[pos + 12:pos + 12 + n]
        if cid == b'SIZE': size = struct.unpack('<iii', body)
        elif cid == b'XYZI':
            cnt = struct.unpack('<i', body[:4])[0]
            voxels = [tuple(body[4 + 4 * i:8 + 4 * i]) for i in range(cnt)]
        elif cid == b'RGBA': palette = [tuple(body[4 * i:4 * i + 4]) for i in range(256)]
        pos += 12 + n
    cells = {}
    for x, zv, yup, i in voxels:  # vox is z-up: (x, z, y_up)
        r, g, b, a = palette[i - 1]
        cells[(x, yup, zv)] = '#%02x%02x%02x' % (r, g, b)
    print('size', size, 'voxels', len(voxels))
    for c, k in collections.Counter(cells.values()).most_common(): print(' ', c, k)
    for q in sys.argv[2:]:
        x, y, z = map(int, q.split(','))
        print(' cell', (x, y, z), cells.get((x, y, z), '-'))

main()
