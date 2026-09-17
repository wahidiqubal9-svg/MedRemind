#!/usr/bin/env python3
"""Tiny dev server for the Medicine Reminder prototype.

Faster than `python -m http.server` on the phone<->PRoot loopback bridge:
  - HTTP/1.1 keep-alive (one connection instead of one per request)
  - gzip when the browser asks for it (~52KB -> ~11KB)
  - no-store so a refresh always shows the latest edit
  - /favicon.ico returns 204 instead of a 404 + broken pipe
"""
import gzip
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import unquote, urlparse

ROOT = os.path.dirname(os.path.abspath(__file__))
PORT = 8080


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"
    server_version = "MedPrototype/1.0"

    def _write(self, data, ctype, compress):
        self.send_response(200)
        self.send_header("Content-Type", ctype)
        self.send_header("Cache-Control", "no-store, must-revalidate")
        if compress:
            self.send_header("Content-Encoding", "gzip")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        try:
            self.wfile.write(data)
        except (BrokenPipeError, ConnectionResetError):
            pass

    def do_GET(self):
        path = unquote(urlparse(self.path).path)
        if path in ("", "/"):
            path = "/index.html"

        if path == "/favicon.ico":
            self.send_response(204)
            self.send_header("Content-Length", "0")
            self.end_headers()
            return

        fp = os.path.normpath(os.path.join(ROOT, path.lstrip("/")))
        if not fp.startswith(ROOT) or not os.path.isfile(fp):
            self.send_error(404)
            return

        with open(fp, "rb") as fh:
            data = fh.read()

        ctype = "text/html; charset=utf-8" if fp.endswith((".html", ".htm")) else "application/octet-stream"
        wants_gzip = "gzip" in self.headers.get("Accept-Encoding", "").lower()
        compress = wants_gzip and len(data) > 1024
        if compress:
            data = gzip.compress(data, 6)
        self._write(data, ctype, compress)

    def do_HEAD(self):
        self.do_GET()


if __name__ == "__main__":
    ThreadingHTTPServer.daemon_threads = True
    ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
