import time
import httpx
from pathlib import Path

URL = "https://huggingface.co/buckets/PatilKrish/Qwen_Models2/resolve/Qwen2.5-1.5B-Instruct-Q4_K_M.gguf?download=true"
DEST = Path(__file__).resolve().parent.parent / "models" / "Qwen2.5-1.5B-Instruct-Q4_K_M.gguf"

def download():
    DEST.parent.mkdir(parents=True, exist_ok=True)
    print(f"Downloading Qwen2.5-1.5B-Instruct-Q4_K_M.gguf to {DEST}...")
    t0 = time.time()
    
    with httpx.stream("GET", URL, follow_redirects=True, timeout=600.0) as resp:
        if resp.status_code != 200:
            print(f"Failed: HTTP {resp.status_code}")
            return False
        total = int(resp.headers.get("content-length", 0))
        downloaded = 0
        last_log = t0
        with open(DEST, "wb") as f:
            for chunk in resp.iter_bytes(chunk_size=1024 * 1024):
                f.write(chunk)
                downloaded += len(chunk)
                now = time.time()
                if now - last_log >= 10.0:
                    pct = (downloaded / total * 100) if total else 0
                    mb = downloaded / (1024 * 1024)
                    speed = mb / (now - t0)
                    print(f"Downloaded {mb:.1f} MB / {total/(1024*1024):.1f} MB ({pct:.1f}%) at {speed:.2f} MB/s")
                    last_log = now
                    
    duration = time.time() - t0
    print(f"Download complete! Saved {DEST.stat().st_size} bytes in {duration:.1f}s.")
    return True

if __name__ == "__main__":
    download()
