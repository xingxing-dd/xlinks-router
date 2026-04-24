#!/usr/bin/env bash
set -eo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONT_ROOT="$ROOT/xlinks-router-web"
FRONTENDS=("Tokenrouter" "xlinks-router-admin" "xlinks-router-client")

lower() {
  printf '%s' "$1" | tr '[:upper:]' '[:lower:]'
}

ensure_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[ERROR] $1 not found. Please install it and add it to PATH first." >&2
    exit 1
  fi
}

resolve_frontend() {
  case "$(lower "$1")" in
    token|tokenrouter) printf '%s\n' "Tokenrouter" ;;
    admin|xlinks-router-admin) printf '%s\n' "xlinks-router-admin" ;;
    client|xlinks-router-client) printf '%s\n' "xlinks-router-client" ;;
    *) printf '%s\n' "$1" ;;
  esac
}

run_one() {
  local module
  module="$(resolve_frontend "$1")"

  if [[ ! -f "$FRONT_ROOT/$module/package.json" ]]; then
    echo "[ERROR] Frontend module not found: $module" >&2
    echo "Available short names: token admin client" >&2
    echo "Available full names : ${FRONTENDS[*]}" >&2
    return 1
  fi

  echo "[START] $module"
  (
    cd "$FRONT_ROOT/$module"
    exec npm run dev
  )
}

run_all() {
  local module
  local pids=()
  local pid
  local status=0

  for module in "${FRONTENDS[@]}"; do
    if [[ -f "$FRONT_ROOT/$module/package.json" ]]; then
      echo "[START] $module"
      (
        cd "$FRONT_ROOT/$module"
        exec npm run dev
      ) &
      pids+=("$!")
    else
      echo "[SKIP] $module/package.json not found"
    fi
  done

  if [[ "${#pids[@]}" -eq 0 ]]; then
    echo "[INFO] No frontend module started."
    return 0
  fi

  for pid in "${pids[@]}"; do
    if ! wait "$pid"; then
      status=1
    fi
  done

  return "$status"
}

ensure_command "npm"

if [[ "$#" -gt 0 ]]; then
  if [[ "$(lower "$1")" == "all" ]]; then
    run_all
  else
    run_one "$1"
  fi
  exit $?
fi

echo
echo "===== Frontend Modules ====="
echo "1. token  (Tokenrouter)"
echo "2. admin  (xlinks-router-admin)"
echo "3. client (xlinks-router-client)"
echo "A. Run all (parallel)"
echo
read -r -p "Input index / short name / full name / A: " choice

case "$(lower "${choice:-}")" in
  a) run_all ;;
  1) run_one "token" ;;
  2) run_one "admin" ;;
  3) run_one "client" ;;
  "")
    echo "[INFO] Empty input. Exit."
    ;;
  *)
    run_one "$choice"
    ;;
esac
