#!/usr/bin/env python3
"""
Audits all lesson resources for the 7 canonical EduNova courses:
1. c-cpp  (cpp-programming)
2. c-dsa  (data-structures-algorithms)
3. c-ds   (data-science-python)
4. c-py   (python-fundamentals)
5. c-ml   (machine-learning-fundamentals)
6. c-fs   (full-stack-development)
7. c-sde  (software-engineering-fundamentals)
"""

import sys
import urllib.request
import urllib.error
import urllib.parse
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

# Add backend directory to path
backend_dir = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(backend_dir))

from scripts.seed_data import SPECIFIC_LESSON_RESOURCES, COURSES

CANONICAL_TRACKS = [
    ("c-cpp", "cpp-programming", "C++ Programming"),
    ("c-dsa", "data-structures-algorithms", "Data Structures & Algorithms"),
    ("c-ds", "data-science-python", "Data Science with Python"),
    ("c-py", "python-fundamentals", "Python Fundamentals"),
    ("c-ml", "machine-learning-fundamentals", "Machine Learning Fundamentals"),
    ("c-fs", "full-stack-development", "Full Stack Development"),
    ("c-sde", "software-engineering-fundamentals", "Software Engineering Fundamentals"),
]

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/124.0.0.0 Safari/537.36"
    ),
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
}


def check_url(course_alias, course_slug, course_name, mod_idx, les_idx, url, provider):
    parsed = urllib.parse.urlsplit(url)
    is_valid_scheme = parsed.scheme in ("http", "https")
    has_netloc = bool(parsed.netloc)

    if not is_valid_scheme or not has_netloc:
        return {
            "alias": course_alias,
            "slug": course_slug,
            "course": course_name,
            "module": mod_idx,
            "lesson": les_idx,
            "url": url,
            "provider": provider,
            "valid_syntax": False,
            "status": "MALFORMED",
            "code": 0,
            "final_url": url,
            "reachable": False,
            "notes": "Invalid scheme or missing host",
        }

    # Test HTTP request
    req = urllib.request.Request(url, headers=HEADERS)
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            status_code = response.getcode()
            final_url = response.geturl()
            return {
                "alias": course_alias,
                "slug": course_slug,
                "course": course_name,
                "module": mod_idx,
                "lesson": les_idx,
                "url": url,
                "provider": provider,
                "valid_syntax": True,
                "status": "OK",
                "code": status_code,
                "final_url": final_url,
                "reachable": True,
                "notes": "Directly accessible",
            }
    except urllib.error.HTTPError as e:
        status_code = e.code
        # 403 can be Cloudflare / WAF bot block
        if status_code in (403, 429):
            return {
                "alias": course_alias,
                "slug": course_slug,
                "course": course_name,
                "module": mod_idx,
                "lesson": les_idx,
                "url": url,
                "provider": provider,
                "valid_syntax": True,
                "status": f"HTTP {status_code} (Bot/WAF check)",
                "code": status_code,
                "final_url": url,
                "reachable": False,
                "notes": "Protected by Cloudflare/WAF bot block (requires interactive browser)",
            }
        elif status_code == 404:
            return {
                "alias": course_alias,
                "slug": course_slug,
                "course": course_name,
                "module": mod_idx,
                "lesson": les_idx,
                "url": url,
                "provider": provider,
                "valid_syntax": True,
                "status": f"HTTP {status_code} Not Found",
                "code": status_code,
                "final_url": url,
                "reachable": False,
                "notes": "Broken link (404)",
            }
        else:
            return {
                "alias": course_alias,
                "slug": course_slug,
                "course": course_name,
                "module": mod_idx,
                "lesson": les_idx,
                "url": url,
                "provider": provider,
                "valid_syntax": True,
                "status": f"HTTP {status_code}",
                "code": status_code,
                "final_url": url,
                "reachable": False,
                "notes": f"Server returned HTTP {status_code}",
            }
    except urllib.error.URLError as e:
        return {
            "alias": course_alias,
            "slug": course_slug,
            "course": course_name,
            "module": mod_idx,
            "lesson": les_idx,
            "url": url,
            "provider": provider,
            "valid_syntax": True,
            "status": "CONNECTION_ERROR",
            "code": 0,
            "final_url": url,
            "reachable": False,
            "notes": str(e.reason)[:80],
        }
    except Exception as e:
        return {
            "alias": course_alias,
            "slug": course_slug,
            "course": course_name,
            "module": mod_idx,
            "lesson": les_idx,
            "url": url,
            "provider": provider,
            "valid_syntax": True,
            "status": "TIMEOUT_OR_ERROR",
            "code": 0,
            "final_url": url,
            "reachable": False,
            "notes": str(e)[:80],
        }


def main():
    items_to_check = []
    for alias, slug, name in CANONICAL_TRACKS:
        for m in range(3):
            for l in range(5):
                entry = SPECIFIC_LESSON_RESOURCES.get((slug, m, l))
                if entry:
                    url, provider = entry
                    items_to_check.append((alias, slug, name, m, l, url, provider))

    print(f"Total Canonical Track Lessons to check: {len(items_to_check)}")

    results = []
    with ThreadPoolExecutor(max_workers=10) as executor:
        futures = [executor.submit(check_url, *item) for item in items_to_check]
        for f in as_completed(futures):
            res = f.result()
            results.append(res)
            # Print brief status
            status_symbol = "OK" if res["reachable"] else res["status"]
            print(f"[{res['alias']}] M{res['module']}L{res['lesson']} -> {res['provider']}: {status_symbol} ({res['url']})")

    # Sort results
    results.sort(key=lambda r: (r["alias"], r["module"], r["lesson"]))

    print("\n" + "="*80)
    print("AUDIT SUMMARY BY COURSE")
    print("="*80)

    course_stats = {}
    for r in results:
        alias = r["alias"]
        if alias not in course_stats:
            course_stats[alias] = {
                "course": r["course"],
                "total": 0,
                "valid": 0,
                "reachable": 0,
                "bot_blocked": 0,
                "failed": 0,
            }
        course_stats[alias]["total"] += 1
        if r["valid_syntax"]:
            course_stats[alias]["valid"] += 1
        if r["reachable"]:
            course_stats[alias]["reachable"] += 1
        elif "Bot/WAF" in r["status"]:
            course_stats[alias]["bot_blocked"] += 1
        else:
            course_stats[alias]["failed"] += 1

    print(f"{'Course Alias':<10} | {'Course Title':<35} | {'Checked':<7} | {'Valid':<5} | {'Reachable':<9} | {'Bot-Blocked':<11} | {'Failed':<6}")
    print("-" * 95)
    for alias, st in course_stats.items():
        print(f"{alias:<10} | {st['course']:<35} | {st['total']:<7} | {st['valid']:<5} | {st['reachable']:<9} | {st['bot_blocked']:<11} | {st['failed']:<6}")

    print("\n" + "="*80)
    print("VERIFICATION OF 3 RECENT LEARNCPP CORRECTIONS")
    print("="*80)
    learncpp_targets = [
        "https://www.learncpp.com/cpp-tutorial/introduction-to-compound-data-types/",
        "https://www.learncpp.com/cpp-tutorial/introduction-to-standard-library-algorithms/",
        "https://www.learncpp.com/cpp-tutorial/introduction-to-functions/",
    ]
    for target in learncpp_targets:
        match = next((r for r in results if r["url"] == target), None)
        if match:
            print(f"CONFIRMED IN SEED: {target}")
            print(f"  Status: {match['status']} (HTTP {match['code']}) | Final: {match['final_url']}")
        else:
            print(f"MISSING IN SEED: {target}")


if __name__ == "__main__":
    main()
