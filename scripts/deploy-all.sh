#!/usr/bin/env bash
set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SSH_STATE_DIR="$SCRIPT_DIR/.ssh"

SCOPE="all"
DRY_RUN=0
MAVEN_REPO_LOCAL=""
SKIP_BACKEND_BUILD=0
SKIP_BACKEND_DEPLOY=0
SKIP_FRONTEND_BUILD=0
SKIP_FRONTEND_DEPLOY=0
SELECTED_BACKEND_NAMES=()
SELECTED_FRONTEND_NAMES=()

VALID_BACKEND_NAMES=("api" "api-test" "client" "admin")
DEFAULT_BACKEND_NAMES=("api" "client" "admin")
VALID_FRONTEND_NAMES=("client" "admin")
DEFAULT_FRONTEND_NAMES=("client" "admin")

usage() {
  cat <<'EOF'
Usage: ./scripts/deploy-all.sh [options]

Options:
  --scope, -Scope <all|backend|frontend>
  --backend-apps, -BackendApps <name[,name...]>
  --frontend-apps, -FrontendApps <name[,name...]>
  --dry-run, -DryRun
  --maven-repo-local, -MavenRepoLocal <path>
  --skip-backend-build, -SkipBackendBuild
  --skip-backend-deploy, -SkipBackendDeploy
  --skip-frontend-build, -SkipFrontendBuild
  --skip-frontend-deploy, -SkipFrontendDeploy
  --help, -h
EOF
}

lower() {
  printf '%s' "$1" | tr '[:upper:]' '[:lower:]'
}

trim() {
  printf '%s' "$1" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'
}

die() {
  echo "FAILED: $*" >&2
  exit 1
}

write_step() {
  printf '\n==== %s ====\n' "$1"
}

ensure_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    die "Required command not found: $1"
  fi
}

contains_name() {
  local needle="$1"
  shift || true
  local item
  for item in "$@"; do
    if [[ "$item" == "$needle" ]]; then
      return 0
    fi
  done
  return 1
}

append_unique_name() {
  local array_name="$1"
  local raw_value="$2"
  local normalized

  normalized="$(lower "$(trim "$raw_value")")"
  [[ -n "$normalized" ]] || return 0

  if [[ "$array_name" == "backend" ]]; then
    contains_name "$normalized" "${SELECTED_BACKEND_NAMES[@]}" || SELECTED_BACKEND_NAMES+=("$normalized")
  else
    contains_name "$normalized" "${SELECTED_FRONTEND_NAMES[@]}" || SELECTED_FRONTEND_NAMES+=("$normalized")
  fi
}

append_name_list() {
  local array_name="$1"
  local csv="$2"
  local values=()
  local value

  IFS=',' read -r -a values <<<"$csv"
  for value in "${values[@]}"; do
    append_unique_name "$array_name" "$value"
  done
}

validate_names() {
  local item_type="$1"
  shift
  local selected=("$@")
  local known_names=()
  local expected
  local name

  if [[ "$item_type" == "backend" ]]; then
    known_names=("${VALID_BACKEND_NAMES[@]}")
  else
    known_names=("${VALID_FRONTEND_NAMES[@]}")
  fi

  for name in "${selected[@]}"; do
    if ! contains_name "$name" "${known_names[@]}"; then
      expected="$(printf '%s, ' "${known_names[@]}")"
      expected="${expected%, }"
      die "Unknown ${item_type} app: $name. Available: $expected"
    fi
  done
}

quote_command() {
  local parts=()
  local arg
  for arg in "$@"; do
    parts+=("$(printf '%q' "$arg")")
  done
  printf '%s' "${parts[*]}"
}

invoke_local_command() {
  local working_directory="$1"
  shift
  local file_path="$1"
  shift

  echo ">> [$working_directory] $(quote_command "$file_path" "$@")"
  if [[ "$DRY_RUN" == "1" ]]; then
    return 0
  fi

  (
    cd "$working_directory"
    "$file_path" "$@"
  )
}

backend_module_dir() {
  case "$1" in
    api) printf '%s\n' "xlinks-router-api" ;;
    api-test) printf '%s\n' "xlinks-router-api" ;;
    client) printf '%s\n' "xlinks-router-client" ;;
    admin) printf '%s\n' "xlinks-router-admin" ;;
    *) return 1 ;;
  esac
}

backend_deploy_host() {
  case "$1" in
    api) printf '%s\n' "101.35.218.196" ;;
    api-test) printf '%s\n' "119.28.150.166" ;;
    client|admin) printf '%s\n' "106.14.134.62" ;;
    *) return 1 ;;
  esac
}

backend_deploy_user() {
  printf '%s\n' "root"
}

backend_deploy_password() {
  printf '%s\n' "132311aA."
}

backend_jar_target_dir() {
  case "$1" in
    api) printf '%s\n' "/app/x-links-api/docker/target" ;;
    api-test) printf '%s\n' "/app/x-links-api/docker/target" ;;
    client) printf '%s\n' "/app/x-links-client/docker/target" ;;
    admin) printf '%s\n' "/app/x-links-admin/docker/target" ;;
    *) return 1 ;;
  esac
}

backend_compose_root() {
  case "$1" in
    api) printf '%s\n' "/app/x-links-api" ;;
    api-test) printf '%s\n' "/app/x-links-api" ;;
    client) printf '%s\n' "/app/x-links-client" ;;
    admin) printf '%s\n' "/app/x-links-admin" ;;
    *) return 1 ;;
  esac
}

frontend_app_dir() {
  case "$1" in
    client) printf '%s\n' "xlinks-router-web/xlinks-router-client" ;;
    admin) printf '%s\n' "xlinks-router-web/xlinks-router-admin" ;;
    *) return 1 ;;
  esac
}

frontend_deploy_host() {
  case "$1" in
    client) printf '%s\n' "101.35.218.196" ;;
    admin) printf '%s\n' "123.60.29.123" ;;
    *) return 1 ;;
  esac
}

frontend_deploy_user() {
  printf '%s\n' "root"
}

frontend_deploy_password() {
  printf '%s\n' "132311aA."
}

frontend_docker_dir() {
  printf '%s\n' "/app/simple-nginx/docker"
}

frontend_compose_root() {
  printf '%s\n' "/app/simple-nginx"
}

host_key_fingerprint() {
  case "$1" in
    101.35.218.196) printf '%s\n' "SHA256:PfiPY69KeproT34U+fYlVw2A3URhqcCI0sKC6eguGJc" ;;
    119.28.150.166) printf '%s\n' "SHA256:/s5Kmh1ukYkGpz/d7r0EvGG3gxtSnCVcriSOqvZSNhw" ;;
    106.14.134.62) printf '%s\n' "SHA256:Y0bjWgodEzH/lJSp5J+uDcaX9T8Q+Ud2BoIunmpELps" ;;
    123.60.29.123) printf '%s\n' "SHA256:ggDyPPYBU3G0NgqWzwJ4TK0F9gxc4RYlg+R/g357E+I" ;;
    *) printf '\n' ;;
  esac
}

known_hosts_file_for() {
  printf '%s\n' "$SSH_STATE_DIR/$1.known_hosts"
}

ensure_host_key() {
  local remote_host="$1"
  local expected_hash
  local known_hosts_file
  local scan_file
  local actual_hash

  expected_hash="$(host_key_fingerprint "$remote_host")"
  known_hosts_file="$(known_hosts_file_for "$remote_host")"

  mkdir -p "$SSH_STATE_DIR"

  if [[ "$DRY_RUN" == "1" ]]; then
    return 0
  fi

  [[ -n "$expected_hash" ]] || return 0

  scan_file="$(mktemp)"
  if ! ssh-keyscan -t ed25519 "$remote_host" >"$scan_file" 2>/dev/null; then
    rm -f "$scan_file"
    die "Unable to read host key from $remote_host"
  fi

  actual_hash="$(ssh-keygen -lf "$scan_file" -E sha256 | awk 'NR==1 {print $2}')"
  if [[ "$actual_hash" != "$expected_hash" ]]; then
    rm -f "$scan_file"
    die "Host key fingerprint mismatch for $remote_host. Expected $expected_hash, got $actual_hash"
  fi

  mv "$scan_file" "$known_hosts_file"
}

run_expect() {
  local password="$1"
  shift
  local program="$1"
  shift

  echo ">> [$REPO_ROOT] $(quote_command "$program" "$@")"
  if [[ "$DRY_RUN" == "1" ]]; then
    return 0
  fi

  expect -f - -- "$program" "$password" "$@" <<'EOF'
set timeout -1
set program [lindex $argv 0]
set password [lindex $argv 1]
set cmdArgs [lrange $argv 2 end]

eval spawn [linsert $cmdArgs 0 $program]
expect {
  -re "(?i)are you sure you want to continue connecting" {
    send "yes\r"
    exp_continue
  }
  -re "(?i)password:" {
    send "$password\r"
    exp_continue
  }
  eof {
    catch wait result
    set exitStatus [lindex $result 3]
    exit $exitStatus
  }
}
EOF
}

invoke_remote_command() {
  local remote_host="$1"
  local user="$2"
  local password="$3"
  local command_text="$4"
  local known_hosts_file

  ensure_host_key "$remote_host"
  known_hosts_file="$(known_hosts_file_for "$remote_host")"

  run_expect "$password" ssh \
    -o BatchMode=no \
    -o PreferredAuthentications=password,keyboard-interactive \
    -o PubkeyAuthentication=no \
    -o StrictHostKeyChecking=yes \
    -o UserKnownHostsFile="$known_hosts_file" \
    "$user@$remote_host" \
    "$command_text"
}

upload_file() {
  local local_path="$1"
  local remote_host="$2"
  local user="$3"
  local password="$4"
  local remote_dir="$5"
  local known_hosts_file

  ensure_host_key "$remote_host"
  known_hosts_file="$(known_hosts_file_for "$remote_host")"

  run_expect "$password" scp \
    -o BatchMode=no \
    -o PreferredAuthentications=password,keyboard-interactive \
    -o PubkeyAuthentication=no \
    -o StrictHostKeyChecking=yes \
    -o UserKnownHostsFile="$known_hosts_file" \
    "$local_path" \
    "$user@$remote_host:$remote_dir/"
}

upload_directory() {
  local local_path="$1"
  local remote_host="$2"
  local user="$3"
  local password="$4"
  local remote_dir="$5"
  local known_hosts_file

  ensure_host_key "$remote_host"
  known_hosts_file="$(known_hosts_file_for "$remote_host")"

  run_expect "$password" scp \
    -r \
    -o BatchMode=no \
    -o PreferredAuthentications=password,keyboard-interactive \
    -o PubkeyAuthentication=no \
    -o StrictHostKeyChecking=yes \
    -o UserKnownHostsFile="$known_hosts_file" \
    "$local_path" \
    "$user@$remote_host:$remote_dir"
}

get_latest_jar() {
  local module_path="$1"
  local target_dir="$module_path/target"
  local files=()
  local sorted=()
  local file

  if [[ ! -d "$target_dir" ]]; then
    if [[ "$DRY_RUN" == "1" ]]; then
      printf '%s\n' "$target_dir/<latest-runnable-jar>.jar"
      return 0
    fi
    die "Target directory not found: $target_dir"
  fi

  while IFS= read -r -d '' file; do
    case "$(basename "$file")" in
      *sources*|*javadoc*|*original*)
        ;;
      *)
        files+=("$file")
        ;;
    esac
  done < <(find "$target_dir" -maxdepth 1 -type f -name '*.jar' -print0)

  if [[ "${#files[@]}" -eq 0 ]]; then
    if [[ "$DRY_RUN" == "1" ]]; then
      printf '%s\n' "$target_dir/<latest-runnable-jar>.jar"
      return 0
    fi
    die "No runnable jar found in: $target_dir"
  fi

  IFS=$'\n' sorted=($(ls -t "${files[@]}"))
  printf '%s\n' "${sorted[0]}"
}

restart_remote_compose() {
  local remote_host="$1"
  local user="$2"
  local password="$3"
  local compose_root="$4"
  local cmd

  cmd="set -e; cd $compose_root; docker-compose down; docker-compose build; docker-compose up -d"
  invoke_remote_command "$remote_host" "$user" "$password" "$cmd"
}

run_backend_pipeline() {
  local services=("$@")
  local service
  local module_dir
  local module_path
  local jar_path
  local remote_host
  local deploy_user
  local deploy_password
  local jar_target_dir
  local compose_root
  local parent_pom_args=("-N" "install")
  local common_build_args=("clean" "install")
  local module_build_args=("clean" "package")

  write_step "Backend pipeline"

  if [[ "${#services[@]}" -eq 0 ]]; then
    echo "No backend service selected. Skip backend pipeline."
    return 0
  fi

  if [[ "$SKIP_BACKEND_BUILD" != "1" ]]; then
    if [[ -z "$MAVEN_REPO_LOCAL" ]]; then
      echo "Use Maven default local repository (from Maven settings)."
    else
      echo "Use custom Maven local repository: $MAVEN_REPO_LOCAL"
      parent_pom_args+=("-Dmaven.repo.local=$MAVEN_REPO_LOCAL")
      common_build_args+=("-Dmaven.repo.local=$MAVEN_REPO_LOCAL")
      module_build_args+=("-Dmaven.repo.local=$MAVEN_REPO_LOCAL")
    fi

    write_step "Install root parent pom"
    invoke_local_command "$REPO_ROOT" mvn "${parent_pom_args[@]}"

    write_step "Build xlinks-router-common"
    invoke_local_command "$REPO_ROOT/xlinks-router-common" mvn "${common_build_args[@]}"

    for service in "${services[@]}"; do
      module_dir="$(backend_module_dir "$service")"
      module_path="$REPO_ROOT/$module_dir"
      write_step "Build backend module: $service"
      invoke_local_command "$module_path" mvn "${module_build_args[@]}"
    done
  fi

  if [[ "$SKIP_BACKEND_DEPLOY" == "1" ]]; then
    echo "Skip backend deploy."
    return 0
  fi

  for service in "${services[@]}"; do
    module_dir="$(backend_module_dir "$service")"
    module_path="$REPO_ROOT/$module_dir"
    jar_path="$(get_latest_jar "$module_path")"
    remote_host="$(backend_deploy_host "$service")"
    deploy_user="$(backend_deploy_user "$service")"
    deploy_password="$(backend_deploy_password "$service")"
    jar_target_dir="$(backend_jar_target_dir "$service")"
    compose_root="$(backend_compose_root "$service")"

    write_step "Upload jar: $service -> $remote_host:$jar_target_dir"
    upload_file "$jar_path" "$remote_host" "$deploy_user" "$deploy_password" "$jar_target_dir"

    write_step "Restart docker compose: $service"
    restart_remote_compose "$remote_host" "$deploy_user" "$deploy_password" "$compose_root"
  done
}

run_frontend_pipeline() {
  local apps=("$@")
  local app
  local app_dir
  local app_path
  local dist_path
  local remote_host
  local deploy_user
  local deploy_password

  write_step "Frontend pipeline"

  if [[ "${#apps[@]}" -eq 0 ]]; then
    echo "No frontend app selected. Skip frontend pipeline."
    return 0
  fi

  for app in "${apps[@]}"; do
    app_dir="$(frontend_app_dir "$app")"
    app_path="$REPO_ROOT/$app_dir"

    if [[ "$SKIP_FRONTEND_BUILD" != "1" ]]; then
      write_step "Build frontend app: $app"
      invoke_local_command "$app_path" npm install
      invoke_local_command "$app_path" npm run build
    fi

    if [[ "$SKIP_FRONTEND_DEPLOY" == "1" ]]; then
      echo "Skip frontend deploy: $app"
      continue
    fi

    dist_path="$app_path/dist"
    if [[ ! -d "$dist_path" ]]; then
      if [[ "$DRY_RUN" == "1" ]]; then
        dist_path="$app_path/dist"
      else
      die "Frontend dist directory not found: $dist_path"
      fi
    fi

    remote_host="$(frontend_deploy_host "$app")"
    deploy_user="$(frontend_deploy_user "$app")"
    deploy_password="$(frontend_deploy_password "$app")"

    write_step "Deploy frontend '$app' -> $remote_host"
    invoke_remote_command "$remote_host" "$deploy_user" "$deploy_password" "set -e; cd $(frontend_docker_dir "$app"); rm -rf dist"
    upload_directory "$dist_path" "$remote_host" "$deploy_user" "$deploy_password" "$(frontend_docker_dir "$app")"
    restart_remote_compose "$remote_host" "$deploy_user" "$deploy_password" "$(frontend_compose_root "$app")"
  done
}

while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --scope|-Scope)
      [[ "$#" -ge 2 ]] || die "Missing value for $1"
      SCOPE="$(lower "$2")"
      shift 2
      ;;
    --scope=*)
      SCOPE="$(lower "${1#*=}")"
      shift
      ;;
    --backend-apps|-BackendApps)
      [[ "$#" -ge 2 ]] || die "Missing value for $1"
      append_name_list "backend" "$2"
      shift 2
      ;;
    --backend-apps=*)
      append_name_list "backend" "${1#*=}"
      shift
      ;;
    --frontend-apps|-FrontendApps)
      [[ "$#" -ge 2 ]] || die "Missing value for $1"
      append_name_list "frontend" "$2"
      shift 2
      ;;
    --frontend-apps=*)
      append_name_list "frontend" "${1#*=}"
      shift
      ;;
    --dry-run|-DryRun)
      DRY_RUN=1
      shift
      ;;
    --maven-repo-local|-MavenRepoLocal)
      [[ "$#" -ge 2 ]] || die "Missing value for $1"
      MAVEN_REPO_LOCAL="$2"
      shift 2
      ;;
    --maven-repo-local=*)
      MAVEN_REPO_LOCAL="${1#*=}"
      shift
      ;;
    --skip-backend-build|-SkipBackendBuild)
      SKIP_BACKEND_BUILD=1
      shift
      ;;
    --skip-backend-deploy|-SkipBackendDeploy)
      SKIP_BACKEND_DEPLOY=1
      shift
      ;;
    --skip-frontend-build|-SkipFrontendBuild)
      SKIP_FRONTEND_BUILD=1
      shift
      ;;
    --skip-frontend-deploy|-SkipFrontendDeploy)
      SKIP_FRONTEND_DEPLOY=1
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      die "Unknown argument: $1"
      ;;
  esac
done

case "$SCOPE" in
  all|backend|frontend)
    ;;
  *)
    die "Invalid scope: $SCOPE. Expected: all, backend, frontend"
    ;;
esac

validate_names "backend" "${SELECTED_BACKEND_NAMES[@]}"
validate_names "frontend" "${SELECTED_FRONTEND_NAMES[@]}"

SELECTED_BACKEND_SERVICES=("${SELECTED_BACKEND_NAMES[@]}")
SELECTED_FRONTEND_APPS=("${SELECTED_FRONTEND_NAMES[@]}")

if [[ "${#SELECTED_BACKEND_SERVICES[@]}" -eq 0 ]]; then
  SELECTED_BACKEND_SERVICES=("${DEFAULT_BACKEND_NAMES[@]}")
fi
if [[ "${#SELECTED_FRONTEND_APPS[@]}" -eq 0 ]]; then
  SELECTED_FRONTEND_APPS=("${DEFAULT_FRONTEND_NAMES[@]}")
fi

RUN_BACKEND=0
RUN_FRONTEND=0

if [[ "$SCOPE" == "all" || "$SCOPE" == "backend" ]]; then
  if [[ "${#SELECTED_BACKEND_SERVICES[@]}" -gt 0 ]]; then
    RUN_BACKEND=1
  fi
fi
if [[ "$SCOPE" == "all" || "$SCOPE" == "frontend" ]]; then
  if [[ "${#SELECTED_FRONTEND_APPS[@]}" -gt 0 ]]; then
    RUN_FRONTEND=1
  fi
fi

write_step "Preflight checks"

if [[ "$RUN_BACKEND" == "1" ]]; then
  ensure_command "mvn"
fi
if [[ "$RUN_FRONTEND" == "1" ]]; then
  ensure_command "npm"
fi

NEED_REMOTE_TOOLS=0
if [[ "$RUN_BACKEND" == "1" && "$SKIP_BACKEND_DEPLOY" != "1" ]]; then
  NEED_REMOTE_TOOLS=1
fi
if [[ "$RUN_FRONTEND" == "1" && "$SKIP_FRONTEND_DEPLOY" != "1" ]]; then
  NEED_REMOTE_TOOLS=1
fi

if [[ "$NEED_REMOTE_TOOLS" == "1" ]]; then
  ensure_command "ssh"
  ensure_command "scp"
  ensure_command "ssh-keyscan"
  ensure_command "ssh-keygen"
  ensure_command "expect"
fi

if [[ "$RUN_BACKEND" == "1" ]]; then
  echo "Selected backend apps : ${SELECTED_BACKEND_SERVICES[*]}"
else
  echo "No backend app selected under current scope, backend pipeline will be skipped."
fi

if [[ "$RUN_FRONTEND" == "1" ]]; then
  echo "Selected frontend apps: ${SELECTED_FRONTEND_APPS[*]}"
else
  echo "No frontend app selected under current scope, frontend pipeline will be skipped."
fi

if [[ "$RUN_BACKEND" == "1" ]]; then
  run_backend_pipeline "${SELECTED_BACKEND_SERVICES[@]}"
fi

if [[ "$RUN_FRONTEND" == "1" ]]; then
  run_frontend_pipeline "${SELECTED_FRONTEND_APPS[@]}"
fi

write_step "All done"
