@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "ROOT=%~dp0"
set "FRONT_ROOT=%ROOT%xlinks-router-web\"
set "FRONTENDS=Tokenrouter xlinks-router-admin xlinks-router-client"

where npm >nul 2>nul
if errorlevel 1 (
  echo [ERROR] npm not found. Please install Node.js and set PATH first.
  pause
  exit /b 1
)

if /I "%~1"=="all" goto RUN_ALL
if not "%~1"=="" (
  call :RUN_ONE "%~1"
  goto END
)

echo.
echo ===== Frontend Modules =====
echo 1. token  ^(Tokenrouter^)
echo 2. admin  ^(xlinks-router-admin^)
echo 3. client ^(xlinks-router-client^)
echo A. Run all ^(new windows^)
echo.
set /p "choice=Input index / short name / full name / A: "

if /I "%choice%"=="A" goto RUN_ALL
if "%choice%"=="1" set "choice=token"
if "%choice%"=="2" set "choice=admin"
if "%choice%"=="3" set "choice=client"

if not defined choice (
  echo [INFO] Empty input. Exit.
  goto END
)

call :RUN_ONE "%choice%"
goto END

:RUN_ONE
set "module=%~1"
call :RESOLVE_FRONTEND "%module%"
if not exist "%FRONT_ROOT%%module%\package.json" (
  echo [ERROR] Frontend module not found: %module%
  echo Available short names: token admin client
  echo Available full names : %FRONTENDS%
  goto :eof
)

echo [START] %module%
cd /d "%FRONT_ROOT%%module%"
call npm run dev
goto :eof

:RESOLVE_FRONTEND
set "input=%~1"
if /I "%input%"=="token" set "module=Tokenrouter"
if /I "%input%"=="tokenrouter" set "module=Tokenrouter"
if /I "%input%"=="admin" set "module=xlinks-router-admin"
if /I "%input%"=="client" set "module=xlinks-router-client"
goto :eof

:RUN_ALL
for %%m in (%FRONTENDS%) do (
  if exist "%FRONT_ROOT%%%m\package.json" (
    start "frontend-%%m" cmd /k "cd /d ""%FRONT_ROOT%%%m"" && npm run dev"
  ) else (
    echo [SKIP] %%m\package.json not found
  )
)
goto END

:END
endlocal
