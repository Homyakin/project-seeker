#!/usr/bin/env python3
"""
Concatenate all Java sources under battle/v4 into one text file.
Default output: <repo>/target/battle_v4_all.java
Run from repo root: python3 scripts/unite_battle_v4_java.py
"""

from __future__ import annotations

import argparse
from pathlib import Path


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Merge all .java files under battle/v4 into one file."
    )
    repo_root = Path(__file__).resolve().parents[1]
    default_root = repo_root / "src/main/java/ru/homyakin/seeker/game/battle/v4"
    parser.add_argument(
        "--root",
        type=Path,
        default=default_root,
        help="Directory to scan (default: battle/v4 under this repo)",
    )
    parser.add_argument(
        "-o",
        "--output",
        type=Path,
        default=None,
        help="Output file (default: target/battle_v4_all.java under repo root)",
    )
    args = parser.parse_args()

    root: Path = args.root.resolve()
    if not root.is_dir():
        raise SystemExit(f"Not a directory: {root}")

    out: Path = (
        args.output.resolve()
        if args.output is not None
        else (repo_root / "target" / "battle_v4_all.java")
    )

    java_files = sorted(root.rglob("*.java"), key=lambda p: str(p).lower())
    if not java_files:
        raise SystemExit(f"No .java files under {root}")

    lines: list[str] = []
    lines.append(f"// Combined from: {root}")
    lines.append(f"// Files: {len(java_files)}")
    lines.append("")

    for path in java_files:
        rel = path.relative_to(root)
        lines.append("// " + "=" * 72)
        lines.append(f"// {rel.as_posix()}")
        lines.append("// " + "=" * 72)
        text = path.read_text(encoding="utf-8")
        if text and not text.endswith("\n"):
            text += "\n"
        lines.append(text)
        if not lines[-1].endswith("\n"):
            lines[-1] += "\n"

    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text("".join(lines), encoding="utf-8")
    print(f"Wrote {len(java_files)} files -> {out}")


if __name__ == "__main__":
    main()
