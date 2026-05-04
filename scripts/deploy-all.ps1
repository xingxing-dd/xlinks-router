param(
    [ValidateSet("all", "backend", "frontend")]
    [string]$Scope = "all",
    [switch]$DryRun,
    [string]$MavenRepoLocal = "",
    [string[]]$BackendApps = @(),
    [string[]]$BackendHosts = @(),
    [string[]]$FrontendApps = @(),
    [switch]$SkipBackendBuild,
    [switch]$SkipBackendDeploy,
    [switch]$SkipFrontendBuild,
    [switch]$SkipFrontendDeploy
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

# Deployment configuration
$BackendServices = @(
    @{
        Name = "api"
        ModuleDir = "xlinks-router-api"
        DeployTargets = @(
            @{
                Name = "api-prod-1"
                Host = "101.35.218.196"
                User = "root"
                Password = "132311aA."
                JarTargetDir = "/app/x-links-api/docker/target"
                ComposeRoot = "/app/x-links-api"
            },
            @{
                Name = "api-prod-2"
                Host = "47.101.46.196"
                User = "root"
                Password = "132311aA."
                JarTargetDir = "/app/x-links-api/docker/target"
                ComposeRoot = "/app/x-links-api"
            }
        )
    },
    @{
        Name = "api-test"
        ModuleDir = "xlinks-router-api"
        DeployTargets = @(
            @{
                Name = "api-test"
                Host = "119.28.150.166"
                User = "root"
                Password = "132311aA."
                JarTargetDir = "/app/x-links-api/docker/target"
                ComposeRoot = "/app/x-links-api"
            }
        )
    },
    @{
        Name = "client"
        ModuleDir = "xlinks-router-client"
        DeployTargets = @(
            @{
                Name = "client"
                Host = "106.14.134.62"
                User = "root"
                Password = "132311aA."
                JarTargetDir = "/app/x-links-client/docker/target"
                ComposeRoot = "/app/x-links-client"
            }
        )
    },
    @{
        Name = "admin"
        ModuleDir = "xlinks-router-admin"
        DeployTargets = @(
            @{
                Name = "admin"
                Host = "106.14.134.62"
                User = "root"
                Password = "132311aA."
                JarTargetDir = "/app/x-links-admin/docker/target"
                ComposeRoot = "/app/x-links-admin"
            }
        )
    }
)

$FrontendAppConfigs = @(
    @{
        Name = "client"
        AppDir = "xlinks-router-web/xlinks-router-client"
        DistDir = "dist"
        Deployments = @(
            @{
                Host = "101.35.218.196"
                User = "root"
                Password = "132311aA."
                DockerDir = "/app/simple-nginx/docker"
                ComposeRoot = "/app/simple-nginx"
            }
        )
    },
    @{
        Name = "admin"
        AppDir = "xlinks-router-web/xlinks-router-admin"
        DistDir = "dist"
        Deployments = @(
            @{
                Host = "123.60.29.123"
                User = "root"
                Password = "132311aA."
                DockerDir = "/app/simple-nginx/docker"
                ComposeRoot = "/app/simple-nginx"
            }
        )
    }
)

function Get-HostKeyFingerprint {
    param([string]$RemoteHost)
    switch ($RemoteHost) {
        "101.35.218.196" { return "ssh-ed25519 255 SHA256:PfiPY69KeproT34U+fYlVw2A3URhqcCI0sKC6eguGJc" }
        "47.101.46.196" { return "ssh-ed25519 255 SHA256:ouNs2XcECtFANmnrR/HjEBwEXu9C/bvJEv1h9dpJagY" }
        "119.28.150.166" { return "ssh-ed25519 255 SHA256:/s5Kmh1ukYkGpz/d7r0EvGG3gxtSnCVcriSOqvZSNhw" }
        "106.14.134.62" { return "ssh-ed25519 255 SHA256:Y0bjWgodEzH/lJSp5J+uDcaX9T8Q+Ud2BoIunmpELps" }
        "123.60.29.123" { return "ssh-ed25519 255 SHA256:ggDyPPYBU3G0NgqWzwJ4TK0F9gxc4RYlg+R/g357E+I" }
        default { return $null }
    }
}

function Write-Step {
    param([string]$Message)
    Write-Host "`n==== $Message ====" -ForegroundColor Cyan
}

function Ensure-Command {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command not found: $Name"
    }
}

function Normalize-NameList {
    param([object[]]$Values)

    $result = @()
    if ($null -eq $Values) {
        return $result
    }

    foreach ($value in $Values) {
        if ($null -eq $value) {
            continue
        }

        $parts = $value.ToString().Split(",", [System.StringSplitOptions]::RemoveEmptyEntries)
        foreach ($part in $parts) {
            $name = $part.Trim().ToLowerInvariant()
            if (-not [string]::IsNullOrWhiteSpace($name) -and ($result -notcontains $name)) {
                $result += $name
            }
        }
    }

    return $result
}

function Get-BackendHostSelectors {
    param([object[]]$Services)

    $selectors = @()
    foreach ($service in $Services) {
        $targets = @($service.DeployTargets)
        foreach ($target in $targets) {
            if ($null -ne $target.Name) {
                $normalized = $target.Name.ToString().Trim().ToLowerInvariant()
                if (-not [string]::IsNullOrWhiteSpace($normalized) -and ($selectors -notcontains $normalized)) {
                    $selectors += $normalized
                }
            }
            if ($null -ne $target.Host) {
                $normalized = $target.Host.ToString().Trim().ToLowerInvariant()
                if (-not [string]::IsNullOrWhiteSpace($normalized) -and ($selectors -notcontains $normalized)) {
                    $selectors += $normalized
                }
            }
        }
    }
    return $selectors
}

function Validate-BackendHostSelectors {
    param(
        [string[]]$SelectedSelectors,
        [object[]]$Services
    )

    if (-not $SelectedSelectors -or $SelectedSelectors.Count -eq 0) {
        return
    }

    $availableSelectors = @(Get-BackendHostSelectors -Services $Services)
    $unknown = @($SelectedSelectors | Where-Object { $availableSelectors -notcontains $_ })
    if ($unknown.Count -gt 0) {
        $expected = ($availableSelectors | Sort-Object) -join ", "
        throw "Unknown backend host selector: $($unknown -join ', '). Available: $expected"
    }
}

function Resolve-BackendTargets {
    param(
        [object]$Service,
        [string[]]$HostSelectors
    )

    $targets = @($Service.DeployTargets)
    if (-not $HostSelectors -or $HostSelectors.Count -eq 0) {
        return $targets
    }

    $filtered = @($targets | Where-Object {
        $targetName = if ($null -eq $_.Name) { "" } else { $_.Name.ToString().Trim().ToLowerInvariant() }
        $targetHost = if ($null -eq $_.Host) { "" } else { $_.Host.ToString().Trim().ToLowerInvariant() }
        $HostSelectors -contains $targetName -or $HostSelectors -contains $targetHost
    })

    return $filtered
}

function Select-ConfiguredItems {
    param(
        [object[]]$Items,
        [string[]]$Names,
        [string]$ItemType
    )

    if (-not $Items -or $Items.Count -eq 0) {
        return @()
    }

    if (-not $Names -or $Names.Count -eq 0) {
        return $Items
    }

    $availableNames = $Items | ForEach-Object { $_.Name.ToLowerInvariant() }
    $unknown = @($Names | Where-Object { $availableNames -notcontains $_ })
    if ($unknown.Count -gt 0) {
        $expected = ($availableNames | Sort-Object) -join ", "
        throw "Unknown ${ItemType}: $($unknown -join ', '). Available: $expected"
    }

    return @($Items | Where-Object { $Names -contains $_.Name.ToLowerInvariant() })
}

function Resolve-PuttyTool {
    param([string]$ToolName)

    $command = Get-Command $ToolName -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $candidatePaths = @(
        (Join-Path $RepoRoot "tools/putty/$ToolName.exe"),
        "C:\\Program Files\\PuTTY\\$ToolName.exe",
        "C:\\Program Files (x86)\\PuTTY\\$ToolName.exe"
    )

    foreach ($candidate in $candidatePaths) {
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    throw "Cannot find $ToolName. Install PuTTY and ensure $ToolName(.exe) is available in PATH."
}

function Invoke-LocalCommand {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [string]$WorkingDirectory
    )

    $argLine = ($Arguments | ForEach-Object {
        if ($_ -match "\s") { '"{0}"' -f $_ } else { $_ }
    }) -join " "

    Write-Host ">> [$WorkingDirectory] $FilePath $argLine" -ForegroundColor DarkGray
    if ($DryRun) {
        return
    }

    Push-Location $WorkingDirectory
    try {
        & $FilePath @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "Command failed with exit code ${LASTEXITCODE}: $FilePath $argLine"
        }
    }
    finally {
        Pop-Location
    }
}

function Invoke-RemoteCommand {
    param(
        [string]$PlinkPath,
        [string]$RemoteHost,
        [string]$User,
        [string]$Password,
        [string]$Command
    )

    $args = @("-batch", "-ssh")
    $hostKey = Get-HostKeyFingerprint -RemoteHost $RemoteHost
    if (-not [string]::IsNullOrWhiteSpace($hostKey)) {
        $args += @("-hostkey", $hostKey)
    }
    $args += @("-pw", $Password, "${User}@${RemoteHost}", $Command)
    Invoke-LocalCommand -FilePath $PlinkPath -Arguments $args -WorkingDirectory $RepoRoot
}

function Upload-File {
    param(
        [string]$PscpPath,
        [string]$LocalPath,
        [string]$RemoteHost,
        [string]$User,
        [string]$Password,
        [string]$RemoteDir
    )

    $args = @("-batch")
    $hostKey = Get-HostKeyFingerprint -RemoteHost $RemoteHost
    if (-not [string]::IsNullOrWhiteSpace($hostKey)) {
        $args += @("-hostkey", $hostKey)
    }
    $args += @("-pw", $Password, $LocalPath, "${User}@${RemoteHost}:$RemoteDir/")
    Invoke-LocalCommand -FilePath $PscpPath -Arguments $args -WorkingDirectory $RepoRoot
}

function Upload-Directory {
    param(
        [string]$PscpPath,
        [string]$LocalPath,
        [string]$RemoteHost,
        [string]$User,
        [string]$Password,
        [string]$RemoteDir
    )

    $args = @("-batch", "-r")
    $hostKey = Get-HostKeyFingerprint -RemoteHost $RemoteHost
    if (-not [string]::IsNullOrWhiteSpace($hostKey)) {
        $args += @("-hostkey", $hostKey)
    }
    $args += @("-pw", $Password, $LocalPath, "${User}@${RemoteHost}:$RemoteDir")
    Invoke-LocalCommand -FilePath $PscpPath -Arguments $args -WorkingDirectory $RepoRoot
}

function Get-LatestJar {
    param([string]$ModuleAbsolutePath)

    $targetDir = Join-Path $ModuleAbsolutePath "target"
    if (-not (Test-Path $targetDir)) {
        throw "Target directory not found: $targetDir"
    }

    $jar = Get-ChildItem -Path $targetDir -Filter "*.jar" -File |
        Where-Object { $_.Name -notmatch "(sources|javadoc|original)" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if (-not $jar) {
        throw "No runnable jar found in: $targetDir"
    }

    return $jar.FullName
}

function Restart-RemoteCompose {
    param(
        [string]$PlinkPath,
        [string]$RemoteHost,
        [string]$User,
        [string]$Password,
        [string]$ComposeRoot
    )

    $cmd = "set -e; cd $ComposeRoot; docker-compose down; docker-compose build; docker-compose up -d"
    Invoke-RemoteCommand -PlinkPath $PlinkPath -RemoteHost $RemoteHost -User $User -Password $Password -Command $cmd
}

function Run-BackendPipeline {
    param(
        [string]$PlinkPath,
        [string]$PscpPath,
        [object[]]$Services,
        [string[]]$BackendHostSelectors
    )

    Write-Step "Backend pipeline"

    if (-not $Services -or $Services.Count -eq 0) {
        Write-Host "No backend service selected. Skip backend pipeline." -ForegroundColor Yellow
        return
    }

    if (-not $SkipBackendBuild) {
        if ([string]::IsNullOrWhiteSpace($MavenRepoLocal)) {
            Write-Host "Use Maven default local repository (from Maven settings)." -ForegroundColor DarkGray
        } else {
            Write-Host "Use custom Maven local repository: $MavenRepoLocal" -ForegroundColor DarkGray
        }

        $parentPomArgs = @("-N", "install")
        $commonBuildArgs = @("clean", "install")
        $moduleBuildArgs = @("clean", "package")
        if (-not [string]::IsNullOrWhiteSpace($MavenRepoLocal)) {
            $repoArg = "-Dmaven.repo.local=$MavenRepoLocal"
            $parentPomArgs += $repoArg
            $commonBuildArgs += $repoArg
            $moduleBuildArgs += $repoArg
        }

        Write-Step "Install root parent pom"
        Invoke-LocalCommand -FilePath "mvn" -Arguments $parentPomArgs -WorkingDirectory $RepoRoot

        Write-Step "Build xlinks-router-common"
        $commonDir = Join-Path $RepoRoot "xlinks-router-common"
        Invoke-LocalCommand -FilePath "mvn" -Arguments $commonBuildArgs -WorkingDirectory $commonDir

        foreach ($svc in $Services) {
            $modulePath = Join-Path $RepoRoot $svc.ModuleDir
            Write-Step "Build backend module: $($svc.Name)"
            Invoke-LocalCommand -FilePath "mvn" -Arguments $moduleBuildArgs -WorkingDirectory $modulePath
        }
    }

    if ($SkipBackendDeploy) {
        Write-Host "Skip backend deploy." -ForegroundColor Yellow
        return
    }

    $deployedTargetCount = 0
    foreach ($svc in $Services) {
        $modulePath = Join-Path $RepoRoot $svc.ModuleDir
        $jarPath = Get-LatestJar -ModuleAbsolutePath $modulePath
        $targets = @(Resolve-BackendTargets -Service $svc -HostSelectors $BackendHostSelectors)
        if ($targets.Count -eq 0) {
            Write-Host "Skip backend deploy for '$($svc.Name)': no target matched current -BackendHosts filter." -ForegroundColor Yellow
            continue
        }

        foreach ($target in $targets) {
            Write-Step "Upload jar: $($svc.Name)@$($target.Name) -> $($target.Host):$($target.JarTargetDir)"
            Upload-File -PscpPath $PscpPath -LocalPath $jarPath -RemoteHost $target.Host -User $target.User -Password $target.Password -RemoteDir $target.JarTargetDir

            Write-Step "Restart docker compose: $($svc.Name)@$($target.Name)"
            Restart-RemoteCompose -PlinkPath $PlinkPath -RemoteHost $target.Host -User $target.User -Password $target.Password -ComposeRoot $target.ComposeRoot
            $deployedTargetCount++
        }
    }

    if ($deployedTargetCount -eq 0) {
        throw "No backend deploy target matched the selected apps and backend host selectors."
    }
}

function Run-FrontendPipeline {
    param(
        [string]$PlinkPath,
        [string]$PscpPath,
        [object[]]$Apps
    )

    Write-Step "Frontend pipeline"

    if (-not $Apps -or $Apps.Count -eq 0) {
        Write-Host "No frontend app selected. Skip frontend pipeline." -ForegroundColor Yellow
        return
    }

    foreach ($app in $Apps) {
        $appPath = Join-Path $RepoRoot $app.AppDir

        if (-not $SkipFrontendBuild) {
            Write-Step "Build frontend app: $($app.Name)"
            Invoke-LocalCommand -FilePath "npm.cmd" -Arguments @("install") -WorkingDirectory $appPath
            Invoke-LocalCommand -FilePath "npm.cmd" -Arguments @("run", "build") -WorkingDirectory $appPath
        }

        if ($SkipFrontendDeploy) {
            Write-Host "Skip frontend deploy: $($app.Name)" -ForegroundColor Yellow
            continue
        }

        if (-not $app.Deployments -or $app.Deployments.Count -eq 0) {
            Write-Host "No deploy target configured for frontend app '$($app.Name)'. Build only." -ForegroundColor Yellow
            continue
        }

        $distPath = Join-Path $appPath $app.DistDir
        if (-not (Test-Path $distPath)) {
            throw "Frontend dist directory not found: $distPath"
        }

        foreach ($target in $app.Deployments) {
            Write-Step "Deploy frontend '$($app.Name)' -> $($target.Host)"

            $removeCmd = "set -e; cd $($target.DockerDir); rm -rf dist"
            Invoke-RemoteCommand -PlinkPath $PlinkPath -RemoteHost $target.Host -User $target.User -Password $target.Password -Command $removeCmd

            Upload-Directory -PscpPath $PscpPath -LocalPath $distPath -RemoteHost $target.Host -User $target.User -Password $target.Password -RemoteDir $target.DockerDir

            Restart-RemoteCompose -PlinkPath $PlinkPath -RemoteHost $target.Host -User $target.User -Password $target.Password -ComposeRoot $target.ComposeRoot
        }
    }
}

try {
    Write-Step "Preflight checks"
    Ensure-Command -Name "mvn"
    Ensure-Command -Name "npm.cmd"

    $selectedBackendNames = @(Normalize-NameList -Values $BackendApps)
    if ($selectedBackendNames.Count -eq 0) {
        $selectedBackendNames = @("api", "client", "admin")
    }
    $selectedBackendHostSelectors = @(Normalize-NameList -Values $BackendHosts)

    $selectedFrontendNames = @(Normalize-NameList -Values $FrontendApps)
    if ($selectedFrontendNames.Count -eq 0) {
        $selectedFrontendNames = @("client", "admin")
    }

    $selectedBackendServices = Select-ConfiguredItems -Items $BackendServices -Names $selectedBackendNames -ItemType "backend app"
    Validate-BackendHostSelectors -SelectedSelectors $selectedBackendHostSelectors -Services $BackendServices
    $selectedFrontendApps = Select-ConfiguredItems -Items $FrontendAppConfigs -Names $selectedFrontendNames -ItemType "frontend app"

    $runBackend = ($Scope -eq "all" -or $Scope -eq "backend") -and $selectedBackendServices.Count -gt 0
    $runFrontend = ($Scope -eq "all" -or $Scope -eq "frontend") -and $selectedFrontendApps.Count -gt 0

    if (-not $runBackend -and ($Scope -eq "all" -or $Scope -eq "backend")) {
        Write-Host "No backend app selected under current scope, backend pipeline will be skipped." -ForegroundColor Yellow
    }
    if (-not $runFrontend -and ($Scope -eq "all" -or $Scope -eq "frontend")) {
        Write-Host "No frontend app selected under current scope, frontend pipeline will be skipped." -ForegroundColor Yellow
    }

    if ($runBackend) {
        Write-Host ("Selected backend apps : " + (($selectedBackendServices | ForEach-Object { $_.Name }) -join ", "))
        if ($selectedBackendHostSelectors.Count -gt 0) {
            Write-Host ("Selected backend hosts: " + ($selectedBackendHostSelectors -join ", "))
        }
        else {
            Write-Host "Selected backend hosts: all configured nodes"
        }
    }
    if ($runFrontend) {
        Write-Host ("Selected frontend apps: " + (($selectedFrontendApps | ForEach-Object { $_.Name }) -join ", "))
    }

    $needBackendRemote = $runBackend -and (-not $SkipBackendDeploy)
    $needFrontendRemote = $runFrontend -and (-not $SkipFrontendDeploy)
    $needRemoteTools = $needBackendRemote -or $needFrontendRemote

    $plinkPath = $null
    $pscpPath = $null
    if ($needRemoteTools) {
        try {
            $plinkPath = Resolve-PuttyTool -ToolName "plink"
            $pscpPath = Resolve-PuttyTool -ToolName "pscp"
        }
        catch {
            if ($DryRun) {
                $plinkPath = "plink"
                $pscpPath = "pscp"
                Write-Host "PuTTY tools not found, but continuing in DryRun mode." -ForegroundColor Yellow
            }
            else {
                throw
            }
        }
    }

    if ($needRemoteTools) {
        Write-Host "Using plink: $plinkPath"
        Write-Host "Using pscp : $pscpPath"
    }

    if ($runBackend) {
        Run-BackendPipeline -PlinkPath $plinkPath -PscpPath $pscpPath -Services $selectedBackendServices -BackendHostSelectors $selectedBackendHostSelectors
    }

    if ($runFrontend) {
        Run-FrontendPipeline -PlinkPath $plinkPath -PscpPath $pscpPath -Apps $selectedFrontendApps
    }

    Write-Step "All done"
}
catch {
    Write-Host "`nFAILED: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
