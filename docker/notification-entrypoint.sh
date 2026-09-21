#!/bin/sh
set -e

export PYTHONPATH="/opt/camoufox-python${PYTHONPATH:+:$PYTHONPATH}"

python3 /opt/camoufox/camoufox_launcher.py &
launcher_pid=$!

i=0
while [ "$i" -lt 60 ]; do
  if nc -z 127.0.0.1 8888 2>/dev/null; then
    break
  fi
  if ! kill -0 "$launcher_pid" 2>/dev/null; then
    echo "Camoufox launcher process exited before bind on :8888" >&2
    exit 1
  fi
  i=$((i + 1))
  sleep 1
done

if ! nc -z 127.0.0.1 8888 2>/dev/null; then
  echo "Camoufox launcher timeout waiting for :8888" >&2
  exit 1
fi

exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher
