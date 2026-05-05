@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "ROOT=%~dp0"
set "BACKENDS=xlinks-router-admin xlinks-router-api xlinks-router-client"
set "FORCE_COMMON_INSTALL=0"
set "TARGET="

for %%a in (%*) do (
  if /I "%%~a"=="--common-install" set "FORCE_COMMON_INSTALL=1"
  if /I "%%~a"=="-ci" set "FORCE_COMMON_INSTALL=1"
  if /I "%%~a"=="--ci" set "FORCE_COMMON_INSTALL=1"
  if /I not "%%~a"=="--common-install" if /I not "%%~a"=="-ci" if /I not "%%~a"=="--ci" (
    if not defined TARGET set "TARGET=%%~a"
  )
)

where mvn >nul 2>nul
if errorlevel 1 (
  echo [ERROR] mvn not found. Please install Maven and set PATH first.
  pause
  exit /b 1
)

if /I "%TARGET%"=="all" goto RUN_ALL
if not "%TARGET%"=="" (
  call :RUN_ONE "%TARGET%"
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

call :PREPARE_COMMON_INSTALL
if errorlevel 1 goto :eof

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
call :PREPARE_COMMON_INSTALL
if errorlevel 1 goto END

for %%m in (%BACKENDS%) do (
  if exist "%ROOT%%%m\pom.xml" (
    start "backend-%%m" cmd /k "cd /d ""%ROOT%%%m"" && mvn clean package spring-boot:run"
  ) else (
    echo [SKIP] %%m\pom.xml not found
  )
)
goto END

:PREPARE_COMMON_INSTALL
if not "%FORCE_COMMON_INSTALL%"=="1" goto :eof
if not exist "%ROOT%xlinks-router-common\pom.xml" (
  echo [ERROR] xlinks-router-common\pom.xml not found
  exit /b 1
)
echo [PREPARE] Running mvn clean install for xlinks-router-common
cd /d "%ROOT%xlinks-router-common"
call mvn clean install
if errorlevel 1 (
  echo [ERROR] Failed to install xlinks-router-common
  exit /b 1
)
goto :eof

:END
endlocal
