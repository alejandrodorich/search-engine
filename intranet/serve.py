"""
Simple HTTP server for the Lima-Labs mock intranet.
Serves all HTML pages from the /pages directory on localhost:8080.

Usage:
    python serve.py

Then point your crawler's Seed-URL to:
    http://localhost:8080/pages/index.html
"""

import http.server
import socketserver
import os

PORT = 8080
DIRECTORY = os.path.dirname(os.path.abspath(__file__))


class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIRECTORY, **kwargs)

    def log_message(self, format, *args):
        # Print each request so you can follow what the crawler is doing
        print(f"[SERVER] {self.address_string()} - {format % args}")

if __name__ == "__main__":
    with socketserver.TCPServer(("", PORT), Handler) as httpd:
        print(f"Lima-Labs mock intranet running at http://localhost:{PORT}")
        print(f"Seed URL: http://localhost:{PORT}/pages/index.html")
        print("Press Ctrl+C to stop.\n")
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nServer stopped.")
