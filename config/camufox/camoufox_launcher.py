import json
import os
import socket
import subprocess
import sys
import time
from http.server import BaseHTTPRequestHandler, HTTPServer
from threading import Lock

instances = {}          # key -> {'process', 'endpoint', 'port', 'site', 'country'}
lock = Lock()

HTTP_BIND = os.environ.get('CAMOUFOX_HTTP_BIND', '127.0.0.1')
WS_HOST = os.environ.get('CAMOUFOX_WS_HOST', '127.0.0.1')
PORT_START = int(os.environ.get('CAMOUFOX_PORT_START', '8080'))
SERVER_BIND = os.environ.get(
    'CAMOUFOX_SERVER_BIND',
    '127.0.0.1' if WS_HOST in ('127.0.0.1', 'localhost') else '0.0.0.0',
)
PROBE_HOST = '127.0.0.1'

NEXT_PORT = PORT_START


class Handler(BaseHTTPRequestHandler):

    def do_GET(self):
        if self.path == '/health':
            return self._json({'status': 'ok'})
        self.send_error(404)

    def do_POST(self):
        if self.path == '/launch':
            return self._launch()
        if self.path == '/release':
            return self._release()
        self.send_error(404)

    def _launch(self):
        global NEXT_PORT
        length = int(self.headers.get('Content-Length', 0))
        data = json.loads(self.rfile.read(length))
        proxy = data.get('proxy')
        site = data.get('site', 'unknown')
        country = data.get('country') or '?'
        headless = bool(data.get('headless', True))
        proxy_part = json.dumps(proxy, sort_keys=True) if proxy else 'none'
        key = f'{headless}|{proxy_part}'

        with lock:
            inst = instances.get(key)
            if inst and inst['process'].poll() is None:
                print(f'[launcher] [{site}] reuse port {inst["port"]} country={inst.get("country", "?")}', flush=True)
                return self._json({'endpoint': inst['endpoint'], 'key': key})

            port = NEXT_PORT
            NEXT_PORT += 1

            script = (
                "import sys, json\n"
                "from camoufox.server import launch_server\n"
                "p = json.loads(sys.argv[1]) if sys.argv[1] != 'null' else None\n"
                f"launch_server(headless={headless}, port={port}, ws_path='camoufox', "
                f"host='{SERVER_BIND}', proxy=p, locale='ru-RU')\n"
            )
            proc = subprocess.Popen(
                [sys.executable, "-c", script, json.dumps(proxy)],
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
            )
            endpoint = f'ws://{WS_HOST}:{port}/camoufox'
            proxy_str = proxy.get('server') if proxy else 'no-proxy'

            print(f'[launcher] [{site}] start port {port} headless={headless} proxy={proxy_str} country={country}', flush=True)

            deadline = time.time() + 60
            ready = False
            while time.time() < deadline:
                if proc.poll() is not None:
                    print(f'[launcher] [{site}] FAILED port {port} (process died)', flush=True)
                    return self._json({'error': 'Camoufox failed to start'}, status=500)
                try:
                    with socket.create_connection((PROBE_HOST, port), timeout=1):
                        ready = True
                        break
                except (ConnectionRefusedError, OSError):
                    time.sleep(0.5)

            if not ready:
                print(f'[launcher] [{site}] TIMEOUT port {port}', flush=True)
                proc.terminate()
                return self._json({'error': 'Camoufox timeout'}, status=500)

            print(f'[launcher] [{site}] ready port {port} country={country}', flush=True)
            instances[key] = {
                'process': proc,
                'endpoint': endpoint,
                'port': port,
                'site': site,
                'country': country,
            }
            self._json({'endpoint': endpoint, 'key': key})

    def _release(self):
        length = int(self.headers.get('Content-Length', 0))
        data = json.loads(self.rfile.read(length))
        key = data.get('key')

        with lock:
            inst = instances.pop(key, None)
            if inst is None:
                return self._json({'status': 'not_found'})

            proc = inst['process']
            site = inst.get('site', 'unknown')
            country = inst.get('country', '?')
            if proc.poll() is None:
                print(f'[launcher] [{site}] kill port {inst["port"]} country={country}', flush=True)
                proc.terminate()
                try:
                    proc.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    proc.kill()

            self._json({'status': 'released'})

    def _json(self, obj, status=200):
        body = json.dumps(obj).encode()
        self.send_response(status)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, fmt, *args):
        print(f'[launcher] {fmt % args}', flush=True)


if __name__ == '__main__':
    print(f'Camoufox launcher on http://{HTTP_BIND}:8888 ws_host={WS_HOST}', flush=True)
    HTTPServer((HTTP_BIND, 8888), Handler).serve_forever()
