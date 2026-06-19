#!/usr/bin/env python3
# ==============================================================================================
# Reads dependencies.html, extracts Maven artifact coordinates from the
# mvnrepository.com URLs, and queries the Maven Central Search API for
# available stable updates.
#
# For each artifact it reports:
#   - latest MINOR update  : newest stable version sharing the current major number
#   - latest MAJOR hint    : newest stable version with a higher major number (if any)
#
# Pre-release versions (alpha, beta, milestone, RC, SNAPSHOT, ...) are ignored.
#
# Using the Maven Central Search API instead of scraping mvnrepository.com
# avoids "I am not a robot" bot-detection dialogs reliably.
#
# SYNTAX:
# python ./start-check-dependency-updates.py
#
# Requires: Python 3.6+  (no third-party packages)
# ==============================================================================================

import io
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

# Force UTF-8 output so Unicode characters work on all platforms/consoles.
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

MAVEN_SEARCH_API  = "https://search.maven.org/solrsearch/select"
DEPENDENCIES_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "dependencies.html")
REQUEST_DELAY_SEC = 0.4    # polite delay between API calls
MAX_VERSIONS      = 200    # max versions to fetch per artifact


# ── Parsing ───────────────────────────────────────────────────────────────────

def parse_dependencies(filepath):
    """Return list of dicts: name, url, group_id, artifact_id, current_version."""
    with open(filepath, encoding="utf-8") as f:
        content = f.read()

    # Each entry looks like:
    #   <li><a href="https://mvnrepository.com/artifact/GROUP/ARTIFACT">NAME</a> VERSION </li>
    pattern = re.compile(
        r'<li>\s*<a\s+href="(https://mvnrepository\.com/artifact/([^/"]+)/([^/"]+))">'
        r'([^<]+)</a>\s*([\d][^\s<]*)\s*</li>'
    )
    deps = []
    for m in pattern.finditer(content):
        url, group_id, artifact_id, name, version = m.groups()
        deps.append({
            "name":            name.strip(),
            "url":             url.strip(),
            "group_id":        group_id,
            "artifact_id":     artifact_id,
            "current_version": version.strip(),
        })
    return deps


# ── Maven Central lookup ──────────────────────────────────────────────────────

def fetch_all_versions(group_id, artifact_id):
    """
    Return a list of all version strings published for this artifact on Maven Central.
    Uses core=gav to get per-version documents.
    """
    params = urllib.parse.urlencode({
        "q":    f'g:"{group_id}" AND a:"{artifact_id}"',
        "core": "gav",
        "rows": MAX_VERSIONS,
        "wt":   "json",
    })
    req = urllib.request.Request(
        f"{MAVEN_SEARCH_API}?{params}",
        headers={"User-Agent": "start-check-dependencies/1.0 (maven-version-checker)"},
    )
    with urllib.request.urlopen(req, timeout=15) as resp:
        data = json.loads(resp.read())

    docs = data.get("response", {}).get("docs", [])
    return [d["v"] for d in docs if "v" in d]


# ── Version helpers ───────────────────────────────────────────────────────────

def is_prerelease(version):
    """
    Return True if the version string contains a pre-release marker.
    A marker must start at the beginning of a version segment (after . or - or at pos 0).
    Trailing digits after the keyword are allowed (e.g. alpha1, beta3, rc2).
    """
    v = version.lower()
    # keywords that, once a segment starts with them, mark a pre-release
    for kw in ("alpha", "beta", "snapshot", "preview", "cr", "ea"):
        if re.search(r'(^|[.\-])' + kw, v):
            return True
    # milestone: M1, M2, M10 …
    if re.search(r'(^|[.\-])m\d+', v):
        return True
    # release candidate: rc, rc1, rc2 …
    if re.search(r'(^|[.\-])rc\d*($|[.\-\d])', v):
        return True
    return False


def _version_key(v):
    """Convert version string to a sortable tuple. Numeric segments sort before text."""
    parts = []
    for seg in re.split(r"[.\-]", v):
        try:
            parts.append((0, int(seg)))
        except ValueError:
            parts.append((1, seg.lower()))
    return parts


def major(version):
    m = re.match(r"^(\d+)", version)
    return int(m.group(1)) if m else 0


def is_newer(a, b):
    """Return True if version a is strictly greater than version b."""
    try:
        return _version_key(a) > _version_key(b)
    except Exception:
        return a != b


def find_updates(all_versions, current):
    """
    Given all published versions and the current version, return:
      latest_minor  — newest stable version with the same major as current
                      (None when current is already the latest in its major line)
      latest_major  — newest stable version with a strictly higher major
                      (None when no such version exists)
    """
    stable = sorted(
        [v for v in all_versions if not is_prerelease(v)],
        key=_version_key,
        reverse=True,
    )

    current_major = major(current)

    same_major   = [v for v in stable if major(v) == current_major]
    higher_major = [v for v in stable if major(v)  > current_major]

    latest_minor = same_major[0]   if same_major   else None
    latest_major = higher_major[0] if higher_major else None

    # Only report a minor update when it is actually newer than current.
    if latest_minor and not is_newer(latest_minor, current):
        latest_minor = None

    return latest_minor, latest_major


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    print(f"Reading {DEPENDENCIES_FILE}\n")
    deps = parse_dependencies(DEPENDENCIES_FILE)

    if not deps:
        print("No dependencies found — check the file path and HTML structure.")
        sys.exit(1)

    print(f"Checking {len(deps)} artifacts against Maven Central ...\n")

    minor_updates = []   # (dep, latest_minor, latest_major|None)
    major_hints   = []   # (dep, latest_major)           — up-to-date on minor, major exists
    up_to_date    = []
    not_found     = []
    errors        = []

    for dep in deps:
        name    = dep["name"]
        current = dep["current_version"]
        g, a    = dep["group_id"], dep["artifact_id"]

        print(f"  {name:<38} {current:<12}", end="", flush=True)

        try:
            versions = fetch_all_versions(g, a)
        except urllib.error.URLError as e:
            print(f"  network error: {e.reason}")
            errors.append({**dep, "error": str(e.reason)})
            time.sleep(REQUEST_DELAY_SEC)
            continue
        except Exception as e:
            print(f"  error: {e}")
            errors.append({**dep, "error": str(e)})
            time.sleep(REQUEST_DELAY_SEC)
            continue

        if not versions:
            print("  not found on Maven Central")
            not_found.append(dep)
            time.sleep(REQUEST_DELAY_SEC)
            continue

        latest_minor, latest_major = find_updates(versions, current)

        if latest_minor:
            major_note = f"  (major: {latest_major})" if latest_major else ""
            print(f"  ->  {latest_minor:<14} *** UPDATE ***{major_note}")
            minor_updates.append((dep, latest_minor, latest_major))
        elif latest_major:
            print(f"  up to date  (major available: {latest_major})")
            major_hints.append((dep, latest_major))
        else:
            print("  up to date")
            up_to_date.append(dep)

        time.sleep(REQUEST_DELAY_SEC)

    # ── Summary ──────────────────────────────────────────────────────────────
    SEP = "=" * 72
    print(f"\n{SEP}")
    print("SUMMARY")
    print(SEP)

    if minor_updates:
        print(f"\n  Minor updates available ({len(minor_updates)}):\n")
        for dep, lminor, lmajor in minor_updates:
            major_note = f"    hint: major {lmajor} also available" if lmajor else ""
            print(f"    {dep['name']:<38} {dep['current_version']:>10}  ->  {lminor}")
            if major_note:
                print(f"    {major_note}")
            print(f"    {dep['url']}")
    else:
        print("\n  No minor updates available.")

    if major_hints:
        print(f"\n  Major version hints ({len(major_hints)}):\n")
        for dep, lmajor in major_hints:
            print(f"    {dep['name']:<38} {dep['current_version']:>10}  ->  {lmajor}  (major)")
            print(f"    {dep['url']}")

    if not_found:
        print(f"\n  Not found on Maven Central ({len(not_found)}):")
        for d in not_found:
            print(f"    {d['name']:<38}  {d['group_id']}:{d['artifact_id']}")

    if errors:
        print(f"\n  Errors ({len(errors)}):")
        for d in errors:
            print(f"    {d['name']:<38}  {d['error']}")

    print()
    return 1 if minor_updates else 0


if __name__ == "__main__":
    sys.exit(main())
