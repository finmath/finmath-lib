#!/usr/bin/env python3

from pathlib import Path
import re

TAB_WIDTH = 4
EXCLUDED_DIRS = {".git", "target", "build", ".idea", ".settings", ".mvn"}

PACKAGE_RE = re.compile(r"^\s*package\s+([a-zA-Z_]\w*(?:\.[a-zA-Z_]\w*)*)\s*;")
LEADING_WS_RE = re.compile(r"^[ \t]+")


def visual_width(prefix):
    width = 0
    for char in prefix:
        if char == "\t":
            width += TAB_WIDTH - (width % TAB_WIDTH)
        else:
            width += 1
    return width


def split_line_ending(line):
    if line.endswith("\r\n"):
        return line[:-2], "\n"
    if line.endswith("\n"):
        return line[:-1], "\n"
    if line.endswith("\r"):
        return line[:-1], "\n"
    return line, ""


def find_package(lines):
    for line in lines:
        match = PACKAGE_RE.match(line)
        if match:
            return match.group(1)
    return None


def remove_redundant_same_package_imports(lines):
    package_name = find_package(lines)
    if package_name is None:
        return lines, 0

    pattern = re.compile(
        r"^\s*import\s+"
        + re.escape(package_name)
        + r"\.(?:[A-Za-z_$][A-Za-z0-9_$]*|\*)\s*;\s*(?://.*)?$"
    )

    new_lines = []
    removed = 0

    for line in lines:
        body, ending = split_line_ending(line)
        if pattern.match(body):
            removed += 1
            continue
        new_lines.append(line)

    return new_lines, removed


def fix_indentation(lines):
    new_lines = []
    changed = 0

    for line in lines:
        body, ending = split_line_ending(line)
        original_body = body

        body = body.rstrip(" \t")

        if body == "":
            if original_body != body:
                changed += 1
            new_lines.append(ending)
            continue

        match = LEADING_WS_RE.match(body)

        if not match:
            new_body = body
        else:
            prefix = match.group(0)
            rest = body[len(prefix):]
            width = visual_width(prefix)

            if rest.startswith("*"):
                tabs = max((width - 1) // TAB_WIDTH, 0)
                new_body = "\t" * tabs + " " + rest
            else:
                tabs = (width + TAB_WIDTH - 1) // TAB_WIDTH
                new_body = "\t" * tabs + rest

        if new_body != original_body:
            changed += 1

        new_lines.append(new_body + ending)

    return new_lines, changed


def should_process(path):
    return path.suffix == ".java" and not any(part in EXCLUDED_DIRS for part in path.parts)


def main():
    root = Path(".").resolve()

    files_changed = 0
    imports_removed_total = 0
    indent_lines_changed_total = 0

    for path in sorted(root.rglob("*.java")):
        rel = path.relative_to(root)

        if not should_process(rel):
            continue

        old_text = path.read_text(encoding="utf-8")
        lines = old_text.splitlines(keepends=True)

        lines, imports_removed = remove_redundant_same_package_imports(lines)
        lines, indent_lines_changed = fix_indentation(lines)

        new_text = "".join(lines)

        if new_text != old_text:
            path.write_text(new_text, encoding="utf-8", newline="")
            files_changed += 1
            imports_removed_total += imports_removed
            indent_lines_changed_total += indent_lines_changed
            print(f"fixed {rel}")

    print()
    print(f"files changed: {files_changed}")
    print(f"same-package imports removed: {imports_removed_total}")
    print(f"indent/trailing-whitespace lines changed: {indent_lines_changed_total}")


if __name__ == "__main__":
    main()
