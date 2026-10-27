#!/usr/bin/env python3
"""Retired legacy 25-candidate checker; this is intentionally not a CI gate.

Its historical 'previousMemory' records refer to the removed two-file schema.
The historical evidence remains in Git; use verify_repository.py for live assets.
"""
import sys

def main():
    if '--help' in sys.argv or '-h' in sys.argv:
        print('Retired legacy B02 25-card candidate checker. Use scripts/verify_repository.py for current v3-only assets.')
        return 0
    print('RETIRED_LEGACY_CHECKER: The old 25-card two-file baseline is not valid after v3 migration. Do not use it as a release gate; run scripts/verify_repository.py instead.', file=sys.stderr)
    return 2
if __name__ == '__main__':
    raise SystemExit(main())
