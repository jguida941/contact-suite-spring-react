#!/usr/bin/env python3
"""
Lightweight helper to preview the QA dashboard artifact locally.

Usage:
    python scripts/serve_quality_dashboard.py
    python scripts/serve_quality_dashboard.py --path /path/to/quality-reports/target/site --port 8080
"""

from __future__ import annotations

import argparse
import contextlib
import http.server
import os
import socket
import socketserver
import sys
import threading
import webbrowser
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SITE = ROOT / "target" / "site"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Serve the QA dashboard artifact locally.")
    parser.add_argument(
        "--path",
        type=Path,
        default=DEFAULT_SITE,
        help="Path to the target/site directory (default: %(default)s)",
    )
    parser.add_argument(
        "--port",
        type=int,
        default=0,
        help="Port to bind (default: auto-pick an open port).",
    )
    return parser.parse_args()


def pick_port(desired: int) -> int:
    if desired:
        return desired
    with contextlib.closing(socket.socket(socket.AF_INET, socket.SOCK_STREAM)) as sock:
        sock.bind(("", 0))
        return sock.getsockname()[1]


def serve_dashboard(site_dir: Path, port: int) -> None:
    if not site_dir.exists():
        raise FileNotFoundError(f"site directory {site_dir} does not exist")

    handler = http.server.SimpleHTTPRequestHandler
    os.chdir(site_dir)
    with socketserver.ThreadingTCPServer(("", port), handler) as httpd:
        actual_port = httpd.server_address[1]
        url = f"http://localhost:{actual_port}/qa-dashboard/index.html"
        print(f"Serving {site_dir} at {url}")
        print("Press Ctrl+C to stop.")
        threading.Thread(target=lambda: webbrowser.open(url), daemon=True).start()
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nShutting down server...")


def main() -> int:
    args = parse_args()
    port = pick_port(args.port)
    try:
        serve_dashboard(args.path.resolve(), port)
        return 0
    except Exception as exc:  # noqa: BLE001
        print(f"Error: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
