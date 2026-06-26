#!/usr/bin/env python3
# ==============================================================================================
# Reads pom.xml, extracts Maven artifact coordinates from <dependencyManagement> and <plugin>
# declarations, then queries the Maven Central Search API for available stable updates.
#
# Artifact URLs (used for the summary output and to avoid name clashes on mvnrepository.com)
# are stored as Maven properties in pom.xml using the naming convention:
#
#   <mvnrepo.ARTIFACT_ID>https://mvnrepository.com/artifact/GROUP/ARTIFACT</mvnrepo.ARTIFACT_ID>
#
# If a <dependency> or <plugin> with an explicit version is found in pom.xml but has no
# corresponding mvnrepo.* property it is reported as a WARNING and skipped from the
# version check. This ensures newly added artifacts are detected automatically.
#
# For each tracked artifact it reports:
#   - latest PATCH update : newest stable version sharing the current major.minor
#   - latest MINOR update : newest stable version sharing the current major (higher minor)
#   - latest MAJOR hint   : newest stable version with a higher major number (if any)
#
# Pre-release versions (alpha, beta, milestone, RC, SNAPSHOT, ...) are ignored.
#
# Using the Maven Central Search API instead of scraping mvnrepository.com
# avoids "I am not a robot" bot-detection dialogs reliably.
#
# SYNTAX:
#   python ./start-check-dependency-updates.py [--json] [path/to/pom.xml]
#
# If no path is given the pom.xml located next to this script is used.
#
# Return codes (higher = more pressure to update):
#   0  all tracked artifacts are up to date
#   1  at least one artifact has a newer MAJOR version   (no minor/patch updates anywhere)
#   2  at least one artifact has a newer MINOR version   (no patch updates anywhere)
#   3  at least one artifact has a newer PATCH version   (always takes priority)
#
# The return code is also included in the JSON output when --json is used.
#
# Requires: Python 3.6+  (no third-party packages)
# ==============================================================================================

import argparse
import io
import json as json_mod
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET

# Force UTF-8 output so Unicode characters work on all platforms/consoles.
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

MAVEN_SEARCH_API  = "https://search.maven.org/solrsearch/select"
POM_NS            = "http://maven.apache.org/POM/4.0.0"
MVNREPO_PROP_PFX  = "mvnrepo."       # property prefix for artifact URLs in pom.xml
REQUEST_DELAY_SEC = 0.4              # polite delay between API calls
MAX_VERSIONS      = 200              # max versions to fetch per artifact

RC_UP_TO_DATE  = 0   # no updates of any kind
RC_MAJOR_AVAIL = 1   # only major updates exist (no minor/patch anywhere)   — lowest pressure
RC_MINOR_AVAIL = 2   # at least one minor update exists (no patch anywhere)
RC_PATCH_AVAIL = 3   # at least one patch update exists                     — highest pressure


# ── POM parsing ───────────────────────────────────────────────────────────────

def parse_pom(filepath):
    """
    Parse pom.xml and return (deps, missing_urls).

    deps         — list of dicts: kind ("library"|"plugin"), url, group_id, artifact_id, current_version
    missing_urls — list of dicts: kind, group_id, artifact_id, current_version
                   for each versioned artifact that lacks a mvnrepo.* URL property
    """
    try:
        tree = ET.parse(filepath)
    except ET.ParseError as e:
        print(f"ERROR: Cannot parse {filepath}: {e}", file=sys.stderr)
        sys.exit(1)

    root = tree.getroot()
    ns = {"m": POM_NS}

    # ── collect <properties> ──────────────────────────────────────────────────
    props = {}
    props_elem = root.find("m:properties", ns)
    if props_elem is not None:
        for child in props_elem:
            local = child.tag.split("}")[1] if "}" in child.tag else child.tag
            props[local] = (child.text or "").strip()

    def resolve(ver_text):
        """Expand a ${property} reference; return None if the property is missing."""
        if not ver_text:
            return None
        ver_text = ver_text.strip()
        m = re.fullmatch(r"\$\{(.+)\}", ver_text)
        if m:
            return props.get(m.group(1))   # None when property is not defined
        return ver_text

    seen    = set()   # (group_id, artifact_id) — deduplicate across sections
    deps    = []
    missing = []

    def register(group_id, artifact_id, version_raw, kind):
        key = (group_id, artifact_id)
        if key in seen:
            return
        seen.add(key)

        version = resolve(version_raw)
        if not version:
            return   # unresolvable version reference — skip silently

        url_key = MVNREPO_PROP_PFX + artifact_id
        url     = props.get(url_key)

        if url:
            deps.append({
                "kind":            kind,
                "url":             url,
                "group_id":        group_id,
                "artifact_id":     artifact_id,
                "current_version": version,
            })
        else:
            missing.append({
                "kind":            kind,
                "group_id":        group_id,
                "artifact_id":     artifact_id,
                "current_version": version,
            })

    # ── JAR dependencies from <dependencyManagement> ──────────────────────────
    for dep in root.findall("m:dependencyManagement/m:dependencies/m:dependency", ns):
        gid = (dep.findtext("m:groupId",    "", ns) or "").strip()
        aid = (dep.findtext("m:artifactId", "", ns) or "").strip()
        ver =  dep.findtext("m:version",   None, ns)
        if ver:
            register(gid, aid, ver, "library")

    # ── plugins (main <build> and all <profile> builds) ───────────────────────
    for plugin in root.findall(".//m:plugin", ns):
        gid = (plugin.findtext("m:groupId",    "", ns) or "").strip()
        aid = (plugin.findtext("m:artifactId", "", ns) or "").strip()
        ver =  plugin.findtext("m:version",   None, ns)
        if ver:
            register(gid, aid, ver, "plugin")

    return deps, missing


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
        data = json_mod.loads(resp.read())

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


def version_parts(v):
    """Return (major, minor, patch) as integers, defaulting missing segments to 0."""
    nums = [int(x) for x in re.findall(r'\d+', v)]
    return (
        nums[0] if len(nums) > 0 else 0,
        nums[1] if len(nums) > 1 else 0,
        nums[2] if len(nums) > 2 else 0,
    )


def is_newer(a, b):
    """Return True if version a is strictly greater than version b."""
    try:
        return _version_key(a) > _version_key(b)
    except Exception:
        return a != b


def find_updates(all_versions, current):
    """
    Given all published versions and the current version, return:
      latest_patch — newest stable version with the same major.minor as current
                     (None when current is already the latest patch in its minor line)
      latest_minor — newest stable version with the same major but a higher minor
                     (None when no such version exists)
      latest_major — newest stable version with a strictly higher major
                     (None when no such version exists)
    """
    stable = sorted(
        [v for v in all_versions if not is_prerelease(v)],
        key=_version_key,
        reverse=True,
    )

    cur_maj, cur_min, _ = version_parts(current)

    same_maj_min        = [v for v in stable if version_parts(v)[:2] == (cur_maj, cur_min)]
    same_maj_higher_min = [v for v in stable
                           if version_parts(v)[0] == cur_maj and version_parts(v)[1] > cur_min]
    higher_maj          = [v for v in stable if version_parts(v)[0] > cur_maj]

    latest_patch = same_maj_min[0]        if same_maj_min        else None
    latest_minor = same_maj_higher_min[0] if same_maj_higher_min else None
    latest_major = higher_maj[0]          if higher_maj          else None

    # Only report a patch update when it is actually newer than current.
    if latest_patch and not is_newer(latest_patch, current):
        latest_patch = None

    return latest_patch, latest_minor, latest_major


# ── Result helpers ────────────────────────────────────────────────────────────

def _status(latest_patch, latest_minor, latest_major):
    """Derive status string — mirrors the return code priority: patch > minor > major."""
    if latest_patch:
        return "patch_available"
    if latest_minor:
        return "minor_available"
    if latest_major:
        return "major_available"
    return "up_to_date"


def _analysis(status, latest_patch, latest_minor, latest_major, error_msg=None):
    """Human-readable one-liner for the analysis field."""
    if status == "missing_url":
        return error_msg or "missing mvnrepo.* URL property in pom.xml"
    if status == "not_found":
        return "not found on Maven Central"
    if status == "error":
        return f"error: {error_msg}"
    if status == "up_to_date":
        return "up to date"
    if status == "patch_available":
        notes = []
        if latest_minor:
            notes.append(f"minor: {latest_minor}")
        if latest_major:
            notes.append(f"major: {latest_major}")
        extra = f"  ({', '.join(notes)})" if notes else ""
        return f"patch update available: {latest_patch}{extra}"
    if status == "minor_available":
        notes = []
        if latest_patch:
            notes.append(f"patch: {latest_patch}")
        if latest_major:
            notes.append(f"major available: {latest_major}")
        extra = f"  ({', '.join(notes)})" if notes else ""
        return f"minor update available: {latest_minor}{extra}"
    if status == "major_available":
        # current major line is fully up to date; only a new major exists
        return f"up to date in current major line  (major available: {latest_major})"
    return ""


def _compute_return_code(all_results):
    """
    Derive the overall return code from the collected results.
    Only version-checked entries (status in the four update/ok states) contribute.
    """
    version_states = {"up_to_date", "patch_available", "minor_available", "major_available"}
    has_major = any(r["latest_major"] for r in all_results if r["status"] in version_states)
    has_minor = any(r["latest_minor"] for r in all_results if r["status"] in version_states)
    has_patch = any(r["latest_patch"] for r in all_results if r["status"] in version_states)
    # Patch carries the most pressure — check from highest to lowest.
    if has_patch:
        return RC_PATCH_AVAIL
    if has_minor:
        return RC_MINOR_AVAIL
    if has_major:
        return RC_MAJOR_AVAIL
    return RC_UP_TO_DATE


# ── Output ────────────────────────────────────────────────────────────────────

def _print_text(pom_file, all_results, return_code):
    """Print the human-readable summary to stdout."""
    version_states = {"up_to_date", "patch_available", "minor_available", "major_available"}

    patch_updates = [r for r in all_results if r["status"] == "patch_available"]
    minor_updates = [r for r in all_results if r["status"] == "minor_available"]
    major_avail   = [r for r in all_results if r["status"] == "major_available"]
    not_found     = [r for r in all_results if r["status"] == "not_found"]
    errors        = [r for r in all_results if r["status"] == "error"]
    missing_urls  = [r for r in all_results if r["status"] == "missing_url"]

    SEP = "=" * 72
    print(f"\n{SEP}")
    print("SUMMARY")
    print(SEP)

    if patch_updates:
        print(f"\n  Patch updates available ({len(patch_updates)}):\n")
        for r in patch_updates:
            notes = []
            if r["latest_minor"]:
                notes.append(f"minor: {r['latest_minor']}")
            if r["latest_major"]:
                notes.append(f"major: {r['latest_major']}")
            extra = f"    hint: {', '.join(notes)}" if notes else ""
            print(f"    {r['artifact_id']:<38} {r['current_version']:>10}  ->  {r['latest_patch']}")
            if extra:
                print(f"    {extra}")
            print(f"    {r['url']}")
    else:
        print("\n  No patch updates available.")

    if minor_updates:
        print(f"\n  Minor updates available ({len(minor_updates)}):\n")
        for r in minor_updates:
            notes = []
            if r["latest_patch"]:
                notes.append(f"patch: {r['latest_patch']}")
            if r["latest_major"]:
                notes.append(f"major available: {r['latest_major']}")
            extra = f"    hint: {', '.join(notes)}" if notes else ""
            print(f"    {r['artifact_id']:<38} {r['current_version']:>10}  ->  {r['latest_minor']}")
            if extra:
                print(f"    {extra}")
            print(f"    {r['url']}")
    else:
        print("\n  No minor updates available.")

    if major_avail:
        print(f"\n  Major version hints ({len(major_avail)}):\n")
        for r in major_avail:
            print(f"    {r['artifact_id']:<38} {r['current_version']:>10}  ->  {r['latest_major']}  (major)")
            print(f"    {r['url']}")

    if not_found:
        print(f"\n  Not found on Maven Central ({len(not_found)}):")
        for r in not_found:
            print(f"    {r['artifact_id']:<38}  {r['group_id']}:{r['artifact_id']}")

    if errors:
        print(f"\n  Errors ({len(errors)}):")
        for r in errors:
            print(f"    {r['artifact_id']:<38}  {r['analysis']}")

    if missing_urls:
        print(f"\n  Missing mvnrepo.* URL properties ({len(missing_urls)}) — skipped:")
        for r in missing_urls:
            label = "library" if r["kind"] == "library" else "plugin"
            print(f"    [{label}]  {r['group_id']}:{r['artifact_id']}  {r['current_version']}")
            print(f"    Add to pom.xml <properties>:")
            print(f"      <mvnrepo.{r['artifact_id']}>https://mvnrepository.com/artifact/"
                  f"{r['group_id']}/{r['artifact_id']}</mvnrepo.{r['artifact_id']}>")

    print()


def _print_json(pom_file, all_results, return_code):
    """Print JSON result to stdout, grouped by kind and sorted by artifact_id within each group."""

    def make_entry(r):
        return {
            "artifact_id":     r["artifact_id"],
            "group_id":        r["group_id"],
            "url":             r.get("url"),
            "current_version": r["current_version"],
            "latest_patch":    r.get("latest_patch"),
            "latest_minor":    r.get("latest_minor"),
            "latest_major":    r.get("latest_major"),
            "status":          r["status"],
            "analysis":        r["analysis"],
        }

    libraries = sorted(
        [make_entry(r) for r in all_results if r["kind"] == "library"],
        key=lambda e: e["artifact_id"],
    )
    plugins = sorted(
        [make_entry(r) for r in all_results if r["kind"] == "plugin"],
        key=lambda e: e["artifact_id"],
    )

    out = {
        "return_code": return_code,
        "pom_file":    pom_file,
        "libraries":   libraries,
        "plugins":     plugins,
    }
    print(json_mod.dumps(out, indent=2, ensure_ascii=False))


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    default_pom = os.path.join(os.path.dirname(os.path.abspath(__file__)), "pom.xml")

    parser = argparse.ArgumentParser(
        description="Check Maven dependency and plugin versions against Maven Central."
    )
    parser.add_argument(
        "pom_file", nargs="?", default=default_pom,
        metavar="path/to/pom.xml",
        help="Path to pom.xml (default: pom.xml next to this script)",
    )
    parser.add_argument(
        "--json", action="store_true", dest="json_output",
        help="Emit a single JSON object to stdout instead of human-readable text",
    )
    args = parser.parse_args()

    pom_file    = args.pom_file
    json_output = args.json_output

    if not json_output:
        print(f"Reading {pom_file}\n")

    deps, missing_urls = parse_pom(pom_file)

    if missing_urls and not json_output:
        print(
            f"WARNING: {len(missing_urls)} artifact(s) have no mvnrepo.* URL property in\n"
            f"         pom.xml and will be skipped from the version check.\n"
            f"         Add a property named\n"
            f"           <mvnrepo.ARTIFACT_ID>https://mvnrepository.com/artifact/GROUP/ARTIFACT"
            f"</mvnrepo.ARTIFACT_ID>\n"
            f"         to the <properties> section for each artifact listed in the summary.\n"
        )

    if not deps and not missing_urls:
        if not json_output:
            print("No dependencies found — check the pom.xml path and structure.")
        sys.exit(1)

    if deps and not json_output:
        print(f"Checking {len(deps)} artifacts against Maven Central ...\n")

    # ── Seed all_results with missing-URL entries (no network call needed) ────
    all_results = []
    for d in missing_urls:
        aid = d["artifact_id"]
        gid = d["group_id"]
        all_results.append({
            "kind":            d["kind"],
            "artifact_id":     aid,
            "group_id":        gid,
            "url":             None,
            "current_version": d["current_version"],
            "latest_patch":    None,
            "latest_minor":    None,
            "latest_major":    None,
            "status":          "missing_url",
            "analysis": (
                f"Add <mvnrepo.{aid}>https://mvnrepository.com/artifact/{gid}/{aid}"
                f"</mvnrepo.{aid}> to pom.xml <properties>"
            ),
        })

    # ── Query Maven Central for each tracked artifact ─────────────────────────
    for dep in deps:
        aid     = dep["artifact_id"]
        current = dep["current_version"]
        gid     = dep["group_id"]

        if not json_output:
            print(f"  {aid:<38} {current:<12}", end="", flush=True)

        try:
            versions = fetch_all_versions(gid, aid)
        except urllib.error.URLError as e:
            msg = str(e.reason)
            if not json_output:
                print(f"  network error: {msg}")
            all_results.append({
                **dep,
                "latest_patch": None, "latest_minor": None, "latest_major": None,
                "status": "error",
                "analysis": f"error: {msg}",
            })
            time.sleep(REQUEST_DELAY_SEC)
            continue
        except Exception as e:
            msg = str(e)
            if not json_output:
                print(f"  error: {msg}")
            all_results.append({
                **dep,
                "latest_patch": None, "latest_minor": None, "latest_major": None,
                "status": "error",
                "analysis": f"error: {msg}",
            })
            time.sleep(REQUEST_DELAY_SEC)
            continue

        if not versions:
            if not json_output:
                print("  not found on Maven Central")
            all_results.append({
                **dep,
                "latest_patch": None, "latest_minor": None, "latest_major": None,
                "status":   "not_found",
                "analysis": "not found on Maven Central",
            })
            time.sleep(REQUEST_DELAY_SEC)
            continue

        latest_patch, latest_minor, latest_major = find_updates(versions, current)
        st  = _status(latest_patch, latest_minor, latest_major)
        ana = _analysis(st, latest_patch, latest_minor, latest_major)

        if not json_output:
            if latest_minor:
                notes = []
                if latest_patch:
                    notes.append(f"patch: {latest_patch}")
                if latest_major:
                    notes.append(f"major: {latest_major}")
                note_str = f"  ({', '.join(notes)})" if notes else ""
                print(f"  ->  {latest_minor:<14} *** MINOR ***{note_str}")
            elif latest_patch:
                notes = []
                if latest_minor:
                    notes.append(f"minor: {latest_minor}")
                if latest_major:
                    notes.append(f"major: {latest_major}")
                note_str = f"  ({', '.join(notes)})" if notes else ""
                print(f"  ->  {latest_patch:<14} *** PATCH ***{note_str}")
            elif latest_major:
                print(f"  up to date  (major available: {latest_major})")
            else:
                print("  up to date")

        all_results.append({
            **dep,
            "latest_patch": latest_patch,
            "latest_minor": latest_minor,
            "latest_major": latest_major,
            "status":       st,
            "analysis":     ana,
        })

        time.sleep(REQUEST_DELAY_SEC)

    return_code = _compute_return_code(all_results)

    if json_output:
        _print_json(pom_file, all_results, return_code)
    else:
        _print_text(pom_file, all_results, return_code)

    return return_code


if __name__ == "__main__":
    sys.exit(main())
