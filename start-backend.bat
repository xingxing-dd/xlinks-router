@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "ROOT=%~dp0"
set "BACKENDS=xlinks-router-admin xlinks-router-api xlinks-router-client"

where mvn >nul 2>nul
if errorlevel 1 (
  echo [ERROR] mvn not found. Please install Maven and set PATH first.
  pause
  exit /b 1
)

if /I "%~1"=="all" goto RUN_ALL
if not "%~1"=="" (
  call :RUN_ONE "%~1"
  goto END
)

echo.
echo ===== Backend Modules =====
echo 1. admin  ^(xlinks-router-admin^)
echo 2. api    ^(xlinks-router-api^)
echo 3. client ^(xlinks-router-client^)
echo A. Run all ^(new windows^)
echo.
set /p "choice=Input index / short name / full name / A: "

if /I "%choice%"=="A" goto RUN_ALL
if "%choice%"=="1" set "choice=admin"
if "%choice%"=="2" set "choice=api"
if "%choice%"=="3" set "choice=client"

if not defined choice (
  echo [INFO] Empty input. Exit.
  goto END
)

call :RUN_ONE "%choice%"
goto END

:RUN_ONE
set "module=%~1"
call :RESOLVE_BACKEND "%module%"
if not exist "%ROOT%%module%\pom.xml" (
  echo [ERROR] Backend module not found: %module%
  echo Available short names: admin api client
  echo Available full names : %BACKENDS%
  goto :eof
)

echo [START] %module%
cd /d "%ROOT%%module%"
call mvn clean package spring-boot:run
goto :eof

:RESOLVE_BACKEND
set "input=%~1"
if /I "%input%"=="admin" set "module=xlinks-router-admin"
if /I "%input%"=="api" set "module=xlinks-router-api"
if /I "%input%"=="client" set "module=xlinks-router-client"
goto :eof

:RUN_ALL
for %%m in (%BACKENDS%) do (
  if exist "%ROOT%%%m\pom.xml" (
    start "backend-%%m" cmd /k "cd /d ""%ROOT%%%m"" && mvn clean package spring-boot:run"
  ) else (
    echo [SKIP] %%m\pom.xml not found
  )
)
goto END

:END
endlocal
