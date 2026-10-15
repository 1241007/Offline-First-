import re
import urllib.request
import urllib.error
import socket
from pathlib import Path

socket.setdefaulttimeout(3.5)

kt_file = Path(__file__).resolve().parent.parent.parent / "app" / "src" / "main" / "java" / "com" / "offline_First" / "data" / "local" / "LocalCourseRepository.kt"

with open(kt_file, "r", encoding="utf-8") as f:
    text = f.read()

urls = list(set(re.findall(r'resourceUrl\s*=\s*"([^"]+)"', text)))
print(f"Total unique URLs in LocalCourseRepository: {len(urls)}")

headers = {"User-Agent": "Mozilla/5.0"}
broken = []
for u in urls:
    try:
        req = urllib.request.Request(u, headers=headers)
        with urllib.request.urlopen(req, timeout=3.5) as r:
            pass
    except urllib.error.HTTPError as e:
        if e.code == 404:
            broken.append((u, 404))
    except Exception:
        pass

print("Broken 404s in LocalCourseRepository:", broken)
