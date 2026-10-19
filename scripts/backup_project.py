#!/usr/bin/env python3
"""Independent, verifiable ZIP source snapshot. Does not invoke Gradle.

By default, skips .git history, local build caches, local backups, and signing
secrets. Use --include-git to retain Git history for full repository recovery.
Keystores must be stored separately in a secure location.
"""
from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import os
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SKIP_DIRS = {'.gradle', '.idea', '__pycache__', '.pytest_cache', 'build', 'local_backups'}
SKIP_FILES = {'local.properties', '.DS_Store'}
SKIP_SUFFIXES = {'.jks', '.keystore', '.pending', '.pyc', '.pyo'}
MANIFEST_NAME = 'SNAPSHOT_MANIFEST.json'
ARCHIVE_ROOT = 'SocialWork-Flashcards'
MANIFEST_PATH = f'{ARCHIVE_ROOT}/{MANIFEST_NAME}'


def source_files(include_git: bool):
    for base, dirs, files in os.walk(ROOT):
        dirs[:] = sorted(d for d in dirs if d not in SKIP_DIRS and (include_git or d != '.git'))
        for name in sorted(files):
            if name in SKIP_FILES or Path(name).suffix.lower() in SKIP_SUFFIXES:
                continue
            path = Path(base) / name
            if path.is_symlink():
                raise ValueError(f'Symlink requires manual inspection: {path}')
            relative = path.relative_to(ROOT).as_posix()
            # A previous source snapshot may leave this generated manifest in
            # the project root. Do not archive that stale copy; create() writes
            # a fresh manifest for the current snapshot after hashing all files.
            if relative == MANIFEST_NAME:
                continue
            yield path, relative


def verify(archive: Path) -> tuple[int, int]:
    with zipfile.ZipFile(archive) as z:
        if z.testzip() is not None:
            raise ValueError('ZIP CRC verification failed')
        names = z.namelist()
        if len(names) != len(set(names)):
            raise ValueError('Duplicate filenames in ZIP')
        if MANIFEST_PATH not in names:
            raise ValueError('Snapshot manifest missing')
        info = json.loads(z.read(MANIFEST_PATH).decode('utf-8'))
        entries = info['files']
        if set(names) != {e['path'] for e in entries} | {MANIFEST_PATH}:
            raise ValueError('Manifest/archive file list mismatch')
        total = 0
        for entry in entries:
            name = entry['path']
            parts = Path(name).parts
            if name.startswith('/') or '..' in parts or '\\' in name or not name.startswith(ARCHIVE_ROOT + '/'):
                raise ValueError(f'Unsafe path: {name}')
            data = z.read(name)
            if len(data) != entry['size'] or hashlib.sha256(data).hexdigest() != entry['sha256']:
                raise ValueError(f'Content hash mismatch: {name}')
            total += len(data)
        return len(entries), total


def create(output: Path, include_git: bool):
    output = output.resolve()
    if output.is_relative_to(ROOT.resolve()):
        raise ValueError('Backup ZIP must be outside the project directory')
    if output.exists():
        raise FileExistsError(f'Refusing to overwrite: {output}')
    output.parent.mkdir(parents=True, exist_ok=True)
    tmp = output.with_name(output.name + '.incomplete')
    if tmp.exists():
        raise FileExistsError(f'Remove abandoned incomplete backup manually: {tmp}')
    files = []
    try:
        with zipfile.ZipFile(tmp, 'x', compression=zipfile.ZIP_DEFLATED, compresslevel=6, allowZip64=True) as z:
            for path, name in source_files(include_git):
                data = path.read_bytes()
                archive_name = f'{ARCHIVE_ROOT}/{name}'
                z.writestr(archive_name, data)
                files.append({'path': archive_name, 'size': len(data), 'sha256': hashlib.sha256(data).hexdigest()})
            manifest = {
                'format': 'socialwork-source-snapshot-v1',
                'created_utc': dt.datetime.now(dt.timezone.utc).isoformat(),
                'git_history_included': include_git,
                'signing_secrets_included': False,
                'files': files,
            }
            z.writestr(MANIFEST_PATH, json.dumps(manifest, ensure_ascii=False, indent=2))
        count, size = verify(tmp)
        os.replace(tmp, output)
        print(f'ZIP BACKUP OK: {output} | {count} files | {size} source bytes | SHA-256 checked')
    except BaseException:
        tmp.unlink(missing_ok=True)
        raise


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    group = parser.add_mutually_exclusive_group()
    group.add_argument('--output', type=Path, help='Create a new ZIP at this destination (outside the project)')
    group.add_argument('--verify', type=Path, help='Validate an existing snapshot without changing it')
    parser.add_argument('--include-git', action='store_true', help='Include .git metadata and history in new ZIP')
    args = parser.parse_args()
    if args.verify:
        count, size = verify(args.verify)
        print(f'ZIP VERIFY OK: {count} files | {size} source bytes | SHA-256 checked')
    else:
        default = ROOT.parent / 'SocialWork-Flashcards-snapshots' / ('SocialWork-Flashcards_' + dt.datetime.now().strftime('%Y%m%d_%H%M%S_%f') + '.zip')
        create(args.output or default, args.include_git)


if __name__ == '__main__':
    try:
        main()
    except (ValueError, OSError, zipfile.BadZipFile, KeyError, json.JSONDecodeError) as exc:
        print(f'BACKUP ERROR: {exc}', file=sys.stderr)
        sys.exit(2)
