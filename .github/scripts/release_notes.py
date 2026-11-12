#!/usr/bin/env python3
"""Собирает тело GitHub Release из Conventional Commits между двумя тегами.

Использование: release_notes.py <tag> [previous-tag]

Если previous-tag не задан, берётся ближайший тег-предок. Когда тегов нет,
в заметки попадает вся история. Результат печатается в stdout — workflow
перенаправляет его в release_notes.md и передаёт в `gh release create --notes-file`.

Тело релиза пишется по-английски: релизы публичные, как и всё на GitHub.
"""

from __future__ import annotations

import os
import re
import subprocess
import sys

# Порядок важен: разделы идут сверху вниз в этом же порядке.
GROUPS = [
    ("feat", "Features"),
    ("perf", "Performance"),
    ("fix", "Fixes"),
    ("refactor", "Refactoring"),
    ("docs", "Documentation"),
    ("test", "Tests"),
    ("build", "Build"),
    ("ci", "CI"),
    ("style", "Style"),
    ("revert", "Reverts"),
    ("chore", "Chores"),
]
GROUP_TITLES = dict(GROUPS)
FALLBACK = "Other"

# feat(ui)!: text  ->  type=feat, scope=ui, breaking=True, text=text
COMMIT_RE = re.compile(r"^(\w+)(?:\(([^)]*)\))?(!)?:\s*(.+)$")


def git(*args: str) -> str:
    return subprocess.run(
        ["git", *args], check=True, capture_output=True, text=True
    ).stdout.strip()


def previous_tag(current: str) -> str | None:
    try:
        return git("describe", "--tags", "--abbrev=0", f"{current}^")
    except subprocess.CalledProcessError:
        return None


def commits_in_range(rev_range: str) -> list[str]:
    try:
        out = git("log", "--no-merges", "--pretty=format:%s", rev_range)
    except subprocess.CalledProcessError as exc:
        print(f"git log failed for {rev_range}: {exc.stderr}", file=sys.stderr)
        raise
    return [line.strip() for line in out.splitlines() if line.strip()]


def build_sections(messages: list[str]) -> tuple[dict[str, list[str]], list[str]]:
    sections: dict[str, list[str]] = {}
    breaking: list[str] = []

    for raw in messages:
        match = COMMIT_RE.match(raw)
        if match:
            kind, scope, bang, text = match.groups()
            title = GROUP_TITLES.get(kind.lower(), FALLBACK)
            if bang:
                breaking.append(text)
            text = f"**{scope}**: {text}" if scope else text
        else:
            title = FALLBACK
            text = raw
        sections.setdefault(title, []).append(text[0].upper() + text[1:])

    return sections, breaking


def render(tag: str, prev: str | None, messages: list[str]) -> str:
    sections, breaking = build_sections(messages)

    if not messages:
        summary = ["## What changed", "", "- Maintenance release, no user-facing changes."]
    else:
        summary = ["## What changed", ""]

    if breaking:
        summary += ["### Breaking changes", ""]
        summary += [f"- {item}" for item in breaking]
        summary.append("")

    order = [title for _, title in GROUPS] + [FALLBACK]
    for title in order:
        items = sections.get(title)
        if not items:
            continue
        summary += [f"### {title}", ""]
        summary += [f"- {item}" for item in items]
        summary.append("")

    repo = os.environ.get("GITHUB_REPOSITORY", "")
    server = os.environ.get("GITHUB_SERVER_URL", "https://github.com")
    if repo:
        compare = (
            f"{server}/{repo}/compare/{prev}...{tag}"
            if prev
            else f"{server}/{repo}/commits/{tag}"
        )
        summary += [f"**Full Changelog**: {compare}"]

    return "\n".join(summary).rstrip() + "\n"


def main() -> int:
    if len(sys.argv) < 2:
        print("usage: release_notes.py <tag> [previous-tag]", file=sys.stderr)
        return 2

    tag = sys.argv[1]
    prev = sys.argv[2] if len(sys.argv) > 2 else previous_tag(tag)
    messages = commits_in_range(f"{prev}..{tag}" if prev else tag)

    sys.stdout.write(render(tag, prev, messages))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
