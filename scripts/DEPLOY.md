# Deploy Script (Windows)

## Files
- `scripts/deploy-all.ps1`: build + deploy pipeline
- `scripts/deploy-all.bat`: wrapper to call the PowerShell script

## Prerequisites
- Maven (`mvn`)
- Node.js + npm (`npm.cmd`)
- PuTTY tools (`plink.exe`, `pscp.exe`) in one of:
  - `tools/putty/` (inside this repo)
  - PATH
  - or installed at:
    - `C:\Program Files\PuTTY\`
    - `C:\Program Files (x86)\PuTTY\`

## Quick start
From repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\deploy-all.ps1
```

Or:

```bat
scripts\deploy-all.bat
```

## Options
- `-Scope all|backend|frontend` (default: `all`)
- `-BackendApps <name[,name...]>` 指定后端应用（可选值：`api`,`client`,`admin`）
- `-FrontendApps <name[,name...]>` 指定前端应用（可选值：`client`,`admin`）
- `-DryRun` print commands only, do not execute
- `-SkipBackendBuild`
- `-SkipBackendDeploy`
- `-SkipFrontendBuild`
- `-SkipFrontendDeploy`
- `-MavenRepoLocal <path>` (optional, default: `.\.m2repo`)

Examples:

```powershell
# Full dry run
powershell -File .\scripts\deploy-all.ps1 -DryRun

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

## Notes
- Backend deploy uploads the newest runnable jar in each module `target` directory.
- Backend build installs root parent POM first (`mvn -N install`) and uses local repo path `.\.m2repo` by default.
- Frontend client deploy removes remote `dist` first, then uploads local `dist`.
- Frontend deploy targets:
  - client -> `101.35.218.196:/app/simple-nginx/docker`
  - admin -> `123.60.29.123:/app/simple-nginx/docker`
- SSH host keys are pinned in script for configured servers.
- Server addresses and passwords are currently in-script constants. Adjust in `scripts/deploy-all.ps1` if needed.
