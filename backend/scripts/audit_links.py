import sys
import socket
import concurrent.futures
import urllib.request
import urllib.error
import urllib.parse
from pathlib import Path

socket.setdefaulttimeout(4.0)

backend_dir = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(backend_dir))

from scripts.seed_data import SPECIFIC_LESSON_RESOURCES, COURSE_DEFAULT_RESOURCES

all_resources = {}

for (c_slug, m_idx, l_idx), (url, provider) in SPECIFIC_LESSON_RESOURCES.items():
    if url not in all_resources:
        all_resources[url] = {"provider": provider, "items": []}
    all_resources[url]["items"].append(f"{c_slug} (M{m_idx+1} L{l_idx+1})")

for c_slug, (url, provider) in COURSE_DEFAULT_RESOURCES.items():
    if url not in all_resources:
        all_resources[url] = {"provider": provider, "items": []}
    all_resources[url]["items"].append(f"default for {c_slug}")

headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
}

def check_link(item):
    url, data = item
    parsed = urllib.parse.urlsplit(url)
    if parsed.scheme.lower() not in ("http", "https"):
        return {
            "url": url,
            "provider": data["provider"],
            "items": data["items"],
            "validation": f"INVALID_SCHEME: {parsed.scheme}",
            "reachability": "REJECTED",
            "status": None
        }

    status_code = None
    reachability = "UNKNOWN"
    try:
        req = urllib.request.Request(url, headers=headers, method="GET")
        with urllib.request.urlopen(req, timeout=4.0) as resp:
            status_code = resp.status
            reachability = "REACHABLE"
    except urllib.error.HTTPError as e:
        status_code = e.code
        if e.code == 403:
            reachability = "HOST_BLOCKED (403 Bot-protection/WAF)"
        elif e.code == 404:
            reachability = "NOT_FOUND (404)"
        else:
            reachability = f"HTTP_{e.code}"
    except urllib.error.URLError as e:
        reachability = f"URL_ERROR: {e.reason}"
    except Exception as e:
        reachability = f"ERROR: {type(e).__name__}"

    return {
        "url": url,
        "provider": data["provider"],
        "items": data["items"],
        "validation": "VALID_HTTP_OR_HTTPS",
        "reachability": reachability,
        "status": status_code
    }

print(f"Auditing {len(all_resources)} unique URLs in parallel with strict socket timeout...", flush=True)

with concurrent.futures.ThreadPoolExecutor(max_workers=25) as executor:
    results = list(executor.map(check_link, all_resources.items()))

reachable = [r for r in results if r["reachability"] == "REACHABLE"]
blocked = [r for r in results if "HOST_BLOCKED" in r["reachability"]]
not_found = [r for r in results if "404" in r["reachability"]]
other = [r for r in results if r not in reachable and r not in blocked and r not in not_found]

print("\n--- LINK AUDIT SUMMARY ---", flush=True)
print(f"Total Unique URLs: {len(results)}", flush=True)
print(f"Reachable (200/3xx): {len(reachable)}", flush=True)
print(f"Protected / WAF (403): {len(blocked)}", flush=True)
print(f"Not Found (404): {len(not_found)}", flush=True)
print(f"Other / Errors: {len(other)}", flush=True)

if not_found:
    print("\n[404 NOT FOUND LINKS]:", flush=True)
    for r in not_found:
        print(f"  {r['url']} -> {r['provider']} ({r['items']})", flush=True)

if other:
    print("\n[OTHER STATUS / ERRORS]:", flush=True)
    for r in other:
        print(f"  [{r['reachability']}] {r['url']} (status: {r['status']}) -> {r['provider']}", flush=True)

if blocked:
    print(f"\n[WAF / BOT PROTECTED: {len(blocked)} URLs]:", flush=True)
    for r in blocked[:5]:
        print(f"  {r['url']} -> {r['provider']}", flush=True)
    if len(blocked) > 5:
        print(f"  ... and {len(blocked) - 5} more", flush=True)

print("\nLink audit complete.", flush=True)
