# Deploy Script

## Files
- `scripts/deploy-all.ps1`: build + deploy pipeline
- `scripts/deploy-all.bat`: Windows wrapper to call the PowerShell script
- `scripts/deploy-all.sh`: macOS/Linux shell entrypoint

## Prerequisites
- Maven (`mvn`)
- Node.js + npm
- Windows:
  - PowerShell
  - PuTTY tools (`plink.exe`, `pscp.exe`) in one of:
    - `tools/putty/` (inside this repo)
    - PATH
    - or installed at:
      - `C:\Program Files\PuTTY\`
      - `C:\Program Files (x86)\PuTTY\`
- macOS/Linux:
  - `ssh`
  - `scp`
  - `ssh-keyscan`
  - `ssh-keygen`
  - `expect`

## Quick start
From repository root:

Windows:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\deploy-all.ps1
```

or:

```bat
scripts\deploy-all.bat
```

macOS / Linux:

```bash
./scripts/deploy-all.sh
```

## Options
- `-Scope all|backend|frontend` (default: `all`)
- `-BackendApps <name[,name...]>` 指定后端应用（可选值：`api`,`api-test`,`client`,`admin`）
- `-FrontendApps <name[,name...]>` 指定前端应用（可选值：`client`,`admin`）
- `-DryRun` print commands only, do not execute
- `-SkipBackendBuild`
- `-SkipBackendDeploy`
- `-SkipFrontendBuild`
- `-SkipFrontendDeploy`
- `-MavenRepoLocal <path>` (optional, default: use Maven's own local repository from your Maven settings)

Examples:

```powershell
# Full dry run
powershell -File .\scripts\deploy-all.ps1 -DryRun

# Deploy api test env
powershell -File .\scripts\deploy-all.ps1 -Scope backend -BackendApps api-test

# Deploy backend only
powershell -File .\scripts\deploy-all.ps1 -Scope backend

# Deploy only backend api + admin
powershell -File .\scripts\deploy-all.ps1 -Scope backend -BackendApps api,admin

# Deploy only frontend admin
powershell -File .\scripts\deploy-all.ps1 -Scope frontend -FrontendApps admin

# Deploy mixed target: backend api + frontend admin
powershell -File .\scripts\deploy-all.ps1 -Scope all -BackendApps api -FrontendApps admin

# Build frontend only, no deploy
powershell -File .\scripts\deploy-all.ps1 -Scope frontend -SkipFrontendDeploy
```

```bash
# Full dry run
./scripts/deploy-all.sh --dry-run

# Deploy api test env
./scripts/deploy-all.sh --scope backend --backend-apps api-test

# Deploy backend only
./scripts/deploy-all.sh --scope backend

# Deploy only backend api + admin
./scripts/deploy-all.sh --scope backend --backend-apps api,admin

# Deploy only frontend admin
./scripts/deploy-all.sh --scope frontend --frontend-apps admin

# Deploy mixed target: backend api + frontend admin
./scripts/deploy-all.sh --scope all --backend-apps api --frontend-apps admin

# Build frontend only, no deploy
./scripts/deploy-all.sh --scope frontend --skip-frontend-deploy
```

## Notes
- Backend deploy uploads the newest runnable jar in each module `target` directory.
- Backend build installs root parent POM first (`mvn -N install`) and by default uses Maven's configured local repository.
- Frontend client deploy removes remote `dist` first, then uploads local `dist`.
- Frontend deploy targets:
  - client -> `101.35.218.196:/app/simple-nginx/docker`
  - admin -> `123.60.29.123:/app/simple-nginx/docker`
- Backend deploy targets:
  - api -> `101.35.218.196:/app/x-links-api/docker/target` (prod)
  - api-test -> `119.28.150.166:/app/x-links-api/docker/target` (test)
- `api-test` 当前复用和 `api` 相同的远端目录与 `docker-compose` 根目录；如果测试环境目录不同，请同步修改两个脚本。
- SSH host keys are pinned in script for configured servers.
- Server addresses and passwords are currently in-script constants. Adjust in `scripts/deploy-all.ps1` or `scripts/deploy-all.sh` if needed.
