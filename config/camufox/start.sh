#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

if [[ ! -x "$DIR/.venv/bin/python" ]]; then
  echo "Нет .venv — создаю и ставлю зависимости..."
  python3 -m venv .venv
  .venv/bin/pip install -U pip
  .venv/bin/pip install -r requirements.txt
fi

# браузер может отсутствовать даже при готовом venv
if ! .venv/bin/python -c 'from camoufox.pkgman import installed_verstr; installed_verstr()' 2>/dev/null; then
  echo "Camoufox не установлен — скачиваю (camoufox fetch)..."
  .venv/bin/python -m camoufox fetch
fi

export CAMOUFOX_PORT_START="${CAMOUFOX_PORT_START:-9222}"
export CAMOUFOX_HTTP_BIND="${CAMOUFOX_HTTP_BIND:-127.0.0.1}"
export CAMOUFOX_WS_HOST="${CAMOUFOX_WS_HOST:-127.0.0.1}"

exec .venv/bin/python camoufox_launcher.py
