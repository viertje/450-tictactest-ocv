#!/usr/bin/env python3
"""Local stand-in for the /version endpoint that nginx serves on the server.

Usage: python scripts/dev-version-server.py [version] [port]
       python scripts/dev-version-server.py 1.0.1 8081

The frontend runs on another origin (another port) than this endpoint, so the
browser only lets it read the answer if the response carries a CORS header.
"""
import sys
from http.server import BaseHTTPRequestHandler, HTTPServer

VERSION = sys.argv[1] if len(sys.argv) > 1 else "0.0.0-SNAPSHOT"
PORT = int(sys.argv[2]) if len(sys.argv) > 2 else 8081


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path != "/version":
            self.send_error(404)
            return
        body = (VERSION + "\n").encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "text/plain; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)


if __name__ == "__main__":
    print(f"Serving version {VERSION} on http://localhost:{PORT}/version")
    HTTPServer(("127.0.0.1", PORT), Handler).serve_forever()
