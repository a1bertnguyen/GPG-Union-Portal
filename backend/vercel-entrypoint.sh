#!/bin/sh

set -u

public_port="${PORT:-80}"
app_port="${SERVER_PORT:-8080}"
ready_port=8081
public_relay_pid=""
ready_relay_pid=""
java_pid=""

cleanup() {
  for pid in "$ready_relay_pid" "$public_relay_pid" "$java_pid"; do
    if [ -n "$pid" ]; then
      kill "$pid" 2>/dev/null || true
    fi
  done
}

trap cleanup INT TERM EXIT

# Vercel requires a listener on PORT before its startup deadline. Connections
# wait on the private readiness port until Spring Boot is fully ready.
/usr/bin/socat \
  "TCP-LISTEN:${public_port},reuseaddr,fork" \
  "TCP:127.0.0.1:${ready_port},retry=600,interval=0.1" &
public_relay_pid=$!

/opt/java/openjdk/bin/java -jar /app/app.jar --server.port="${app_port}" &
java_pid=$!

while kill -0 "$java_pid" 2>/dev/null; do
  if /usr/bin/wget -q -O - "http://127.0.0.1:${app_port}/actuator/health" 2>/dev/null \
    | grep -q '"status":"UP"'; then
    break
  fi
  sleep 0.2
done

if ! kill -0 "$java_pid" 2>/dev/null; then
  wait "$java_pid"
  exit $?
fi

/usr/bin/socat \
  "TCP-LISTEN:${ready_port},reuseaddr,fork" \
  "TCP:127.0.0.1:${app_port}" &
ready_relay_pid=$!

wait "$java_pid"
exit $?
