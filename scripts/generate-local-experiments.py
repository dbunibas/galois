#!/usr/bin/env python3
"""Derive the "local" experiment files from the "gpt" ones.

For every *-gpt-*-experiment.json in the llm-bench datasets, writes a copy named
*-local-*-experiment.json where every "queryExecutor": "open-ai-*" becomes
"queryExecutor": "local-*". Everything else in the file is left untouched.

Usage:
    python3 scripts/generate-local-experiments.py [llm-bench-dir] [--dry-run] [--force]
"""

import argparse
import re
import sys
from pathlib import Path

DEFAULT_BENCH_DIR = Path(__file__).resolve().parent.parent / "core/src/test/resources/llm-bench"
# "spider" is stored as "spider1" in llm-bench: both names are accepted
DATASETS = ["bird", "galois", "qatch", "spider1"]
DATASET_ALIASES = {"spider": "spider1"}

GPT_FILE_GLOB = "*-gpt-*-experiment.json"
EXECUTOR_PATTERN = re.compile(r'("queryExecutor"\s*:\s*")open-ai-')


def to_local_name(path: Path) -> Path:
    """presidents-gpt-nl-experiment.json -> presidents-local-nl-experiment.json"""
    return path.with_name(path.name.replace("-gpt-", "-local-", 1))


def to_local_content(content: str) -> tuple[str, int]:
    return EXECUTOR_PATTERN.subn(r"\1local-", content)


def convert_dataset(dataset_dir: Path, dry_run: bool, force: bool) -> tuple[int, int]:
    written, skipped = 0, 0
    for gpt_file in sorted(dataset_dir.glob(GPT_FILE_GLOB)):
        local_file = to_local_name(gpt_file)
        if local_file.exists() and not force:
            print(f"  SKIP    {local_file.name} (already exists, use --force to overwrite)")
            skipped += 1
            continue
        content, replacements = to_local_content(gpt_file.read_text())
        if replacements == 0:
            print(f"  WARNING {gpt_file.name} has no open-ai-* queryExecutor")
        if not dry_run:
            local_file.write_text(content)
        print(f"  {'WOULD WRITE' if dry_run else 'WRITE  '} {local_file.name} ({replacements} executor(s) renamed)")
        written += 1
    return written, skipped


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("bench_dir", nargs="?", type=Path, default=DEFAULT_BENCH_DIR,
                        help=f"llm-bench directory (default: {DEFAULT_BENCH_DIR})")
    parser.add_argument("--datasets", nargs="+", default=DATASETS, help="datasets to convert")
    parser.add_argument("--dry-run", action="store_true", help="only report what would be written")
    parser.add_argument("--force", action="store_true", help="overwrite existing *-local-*-experiment.json files")
    args = parser.parse_args()

    if not args.bench_dir.is_dir():
        print(f"Not a directory: {args.bench_dir}", file=sys.stderr)
        return 1

    total_written, total_skipped, missing = 0, 0, []
    for dataset in args.datasets:
        dataset_dir = args.bench_dir / DATASET_ALIASES.get(dataset, dataset)
        if not dataset_dir.is_dir():
            missing.append(dataset_dir.name)
            continue
        print(f"{dataset_dir.name}:")
        written, skipped = convert_dataset(dataset_dir, args.dry_run, args.force)
        total_written += written
        total_skipped += skipped

    print(f"\n{total_written} file(s) {'to write' if args.dry_run else 'written'}, {total_skipped} skipped")
    if missing:
        print(f"Missing dataset directories: {', '.join(missing)}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
