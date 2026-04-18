# Deploy Matrix

## Source Of Truth

- `scripts/DEPLOY.md`
- `scripts/deploy-all.ps1`

Always prefer those files if current repo behavior and this reference diverge.

## Supported Parameters

- `-Scope all|backend|frontend`
- `-BackendApps <name[,name...]>`
- `-FrontendApps <name[,name...]>`
- `-DryRun`
- `-SkipBackendBuild`
- `-SkipBackendDeploy`
- `-SkipFrontendBuild`
- `-SkipFrontendDeploy`
- `-MavenRepoLocal <path>`

## Backend App Mapping

- `api` -> module `xlinks-router-api`
- `client` -> module `xlinks-router-client`
- `admin` -> module `xlinks-router-admin`

## Frontend App Mapping

- `client` -> app dir `xlinks-router-web/xlinks-router-client`
- `admin` -> app dir `xlinks-router-web/xlinks-router-admin`

## Ambiguous Phrases That Require Confirmation

- `publish client`
- `publish admin`
- `publish web`
- `publish one module`
- `publish xlinks`
- `publish all`

## Safe Resolution Strategy

1. Determine whether the user wants execution or only a command preview.
2. Determine whether the target is backend, frontend, or all.
3. Determine exact app names.
4. Repeat the resolved command in plain language.
5. Execute only after the scope is unambiguous.

## Example Resolutions

- `publish all`
  - Needs confirmation: direct execution or `-DryRun` first.
  - Likely command: `-Scope all`

- `publish api`
  - Clear backend target.
  - Command: `-Scope backend -BackendApps api`

- `publish client`
  - Ambiguous: backend `client` vs frontend `client`
  - Must confirm.

- `publish frontend admin only`
  - Command: `-Scope frontend -FrontendApps admin`

- `build frontend client without deploy`
  - Command: `-Scope frontend -FrontendApps client -SkipFrontendDeploy`
