#!/usr/bin/env bash
set -eo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKENDS=("xlinks-router-admin" "xlinks-router-api" "xlinks-router-client")
FORCE_COMMON_INSTALL=0
TARGET=""

lower() {
  printf '%s' "$1" | tr '[:upper:]' '[:lower:]'
}

ensure_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[ERROR] $1 not found. Please install it and add it to PATH first." >&2
    exit 1
  fi
}

resolve_backend() {
  case "$(lower "$1")" in
    admin|xlinks-router-admin) printf '%s\n' "xlinks-router-admin" ;;
    api|xlinks-router-api) printf '%s\n' "xlinks-router-api" ;;
    client|xlinks-router-client) printf '%s\n' "xlinks-router-client" ;;
    *) printf '%s\n' "$1" ;;
  esac
}

prepare_common_install() {
  if [[ "$FORCE_COMMON_INSTALL" != "1" ]]; then
    return 0
  fi

  if [[ ! -f "$ROOT/xlinks-router-common/pom.xml" ]]; then
    echo "[ERROR] xlinks-router-common/pom.xml not found" >&2
    exit 1
  fi

  echo "[PREPARE] Running mvn clean install for xlinks-router-common"
  (
    cd "$ROOT/xlinks-router-common"
    mvn clean install
  )
}

run_one() {
  local module
  module="$(resolve_backend "$1")"

  if [[ ! -f "$ROOT/$module/pom.xml" ]]; then
    echo "[ERROR] Backend module not found: $module" >&2
    echo "Available short names: admin api client" >&2
    echo "Available full names : ${BACKENDS[*]}" >&2
    return 1
  fi

  prepare_common_install

  echo "[START] $module"
  (
    cd "$ROOT/$module"
    exec mvn clean package spring-boot:run
  )
}

run_all() {
  local module
  local pids=()
  local pid
  local status=0

  prepare_common_install

  for module in "${BACKENDS[@]}"; do
    if [[ -f "$ROOT/$module/pom.xml" ]]; then
      echo "[START] $module"
      (
        cd "$ROOT/$module"
        exec mvn clean package spring-boot:run
      ) &
      pids+=("$!")
    else
      echo "[SKIP] $module/pom.xml not found"
    fi
  done

  if [[ "${#pids[@]}" -eq 0 ]]; then
    echo "[INFO] No backend module started."
    return 0
  fi

  for pid in "${pids[@]}"; do
    if ! wait "$pid"; then
      status=1
    fi
  done

  return "$status"
}

while [[ "$#" -gt 0 ]]; do
  case "$(lower "$1")" in
    --common-install|-ci|--ci)
      FORCE_COMMON_INSTALL=1
      ;;
    *)
      if [[ -z "$TARGET" ]]; then
        TARGET="$1"
      fi
      ;;
  esac
  shift
done

ensure_command "mvn"

if [[ "$(lower "${TARGET:-}")" == "all" ]]; then
  run_all
  exit $?
fi

if [[ -n "$TARGET" ]]; then
  run_one "$TARGET"
  exit $?
fi

echo
echo "===== Backend Modules ====="
echo "1. admin  (xlinks-router-admin)"
echo "2. api    (xlinks-router-api)"
echo "3. client (xlinks-router-client)"
echo "A. Run all (parallel)"
echo
read -r -p "Input index / short name / full name / A: " choice

case "$(lower "${choice:-}")" in
  a) run_all ;;
  1) run_one "admin" ;;
  2) run_one "api" ;;
  3) run_one "client" ;;
  "")
    echo "[INFO] Empty input. Exit."
    ;;
  *)
    run_one "$choice"
    ;;
esac
