---
name: xlinks-router-deploy
description: Deploy xlinks-router by using the repository release scripts in `scripts/`. Use when the user wants to publish the whole xlinks-router system, deploy one backend app (`api`,`client`,`admin`), deploy one frontend app (`client`,`admin`), run a dry run, or build without deploy. Before executing, resolve ambiguous publish intent with the user when names like `client`, `admin`, `web`, `backend`, `frontend`, or `module` do not uniquely map to a script target.
---

# Xlinks Router Deploy

## Overview

Use the repo deployment entrypoint `scripts/deploy-all.ps1` from the repository root. Do not invent alternate release commands when the existing script already supports the requested target.

Read [references/deploy-matrix.md](references/deploy-matrix.md) before composing the final command if the request involves target selection, skip flags, or confirmation rules.

## Workflow

1. Read `scripts/DEPLOY.md` and `scripts/deploy-all.ps1` if the repo may have changed since the skill was written.
2. Map the user request to script parameters:
   - Full publish: `-Scope all`
   - Backend only: `-Scope backend`
   - Frontend only: `-Scope frontend`
   - Specific backend apps: `-BackendApps api|client|admin`
   - Specific frontend apps: `-FrontendApps client|admin`
   - Preview only: `-DryRun`
   - Build only: combine scope with `-SkipBackendDeploy` and/or `-SkipFrontendDeploy`
3. Pause for human confirmation when the request is ambiguous or materially risky.
4. Echo the resolved scope in plain language before execution.
5. Run the PowerShell entrypoint from repo root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\deploy-all.ps1 ...
```

6. Report which backend apps and frontend apps were selected, and summarize success or failure.

## Confirmation Rules

Ask the user to confirm before execution when any of these are true:

- The request says `publish xlinks-router` or `publish all` but it is not clear whether they want a dry run first.
- The request mentions `client` or `admin` without saying backend or frontend.
- The request says `publish one module` or `publish app` but does not name the exact target.
- The request mixes natural-language targets that can map to multiple script flags.
- The request would deploy to production and they have not clearly approved execution yet.

Use short, concrete confirmation prompts. Good examples:

- `I currently understand this as a full release (backend api/client/admin plus frontend client/admin). Do you want direct execution or a DryRun first?`
- `Does client mean backend xlinks-router-client or frontend xlinks-router-web/xlinks-router-client?`
- `Do you want backend admin or frontend admin?`

## Mapping Rules

- `xlinks-router-api` -> backend `api`
- `xlinks-router-client` -> backend `client`
- `xlinks-router-admin` -> backend `admin`
- `xlinks-router-web/xlinks-router-client` -> frontend `client`
- `xlinks-router-web/xlinks-router-admin` -> frontend `admin`

Treat these words as ambiguous until clarified:

- `client`
- `admin`
- `web`
- `module`

## Execution Guidance

- Prefer the PowerShell script over the `.bat` wrapper because the script exposes the full option surface clearly.
- Run from repository root `D:\project\xlinks-router`.
- Do not hardcode hostnames or passwords in the response unless the user explicitly asks; rely on the script.
- If the user only wants a plan or command preview, stop after composing the exact command.
- If the script fails in preflight, surface the failing prerequisite directly.

## Common Commands

```powershell
# Full release
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\deploy-all.ps1 -Scope all

# Full dry run
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\deploy-all.ps1 -DryRun

# Backend api only
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\deploy-all.ps1 -Scope backend -BackendApps api

# Frontend client only
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\deploy-all.ps1 -Scope frontend -FrontendApps client

# Mixed target
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\deploy-all.ps1 -Scope all -BackendApps api -FrontendApps admin

# Build frontend only without deploy
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\deploy-all.ps1 -Scope frontend -SkipFrontendDeploy
```
