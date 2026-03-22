@echo off
setlocal enabledelayedexpansion

rem =========================================================
rem xlinks-router root launcher
rem
rem Usage:
rem   run.bat client
rem   run.bat web
rem   run.bat client -- <extra args passed to underlying cmd>
rem   run.bat web -- <extra args passed to underlying cmd>
rem
rem Notes:
rem - client: cd to xlinks-router-client and run mvn clean package spring-boot:run
rem - api:    cd to xlinks-router-api and run mvn clean package spring-boot:run
rem - admin:  cd to xlinks-router-admin and run mvn clean package spring-boot:run
rem - web:    cd to xlinks-router-web\xlinks-router-web-vue and run npm run dev
rem =========================================================

set "ROOT_DIR=%~dp0"
set "TARGET=%~1"

if "%TARGET%"=="" goto :help
if /I "%TARGET%"=="-h" goto :help
if /I "%TARGET%"=="--help" goto :help
if /I "%TARGET%"=="help" goto :help

rem Shift off target
shift

rem Optional "--" separator; everything after is forwarded.
set "FORWARD_ARGS=%*"
if defined FORWARD_ARGS (
  if "!FORWARD_ARGS:~0,2!"=="--" set "FORWARD_ARGS=!FORWARD_ARGS:~2!"
  rem trim one leading space after removing "--"
  if "!FORWARD_ARGS:~0,1!"==" " set "FORWARD_ARGS=!FORWARD_ARGS:~1!"
)

if /I "%TARGET%"=="client" goto :client
if /I "%TARGET%"=="api" goto :api
if /I "%TARGET%"=="admin" goto :admin
if /I "%TARGET%"=="web" goto :web

echo [ERROR] Unknown target: %TARGET%
echo.
goto :help

:client
set "CLIENT_DIR=%ROOT_DIR%xlinks-router-client"
if not exist "%CLIENT_DIR%\pom.xml" (
  echo [ERROR] client module not found: "%CLIENT_DIR%" (missing pom.xml)
  exit /b 2
)

pushd "%CLIENT_DIR%" >nul
echo [INFO] Running client in: %CD%
if defined FORWARD_ARGS (
  echo [INFO] Extra args: %FORWARD_ARGS%
)
call mvn clean package spring-boot:run %FORWARD_ARGS%
set "ERR=%ERRORLEVEL%"
popd >nul
exit /b %ERR%

:api
set "API_DIR=%ROOT_DIR%xlinks-router-api"
if not exist "%API_DIR%\pom.xml" (
  echo [ERROR] api module not found: "%API_DIR%" (missing pom.xml)
  exit /b 2
)

pushd "%API_DIR%" >nul
echo [INFO] Running api in: %CD%
if defined FORWARD_ARGS (
  echo [INFO] Extra args: %FORWARD_ARGS%
)
call mvn clean package spring-boot:run %FORWARD_ARGS%
set "ERR=%ERRORLEVEL%"
popd >nul
exit /b %ERR%

:admin
set "ADMIN_DIR=%ROOT_DIR%xlinks-router-admin"
if not exist "%ADMIN_DIR%\pom.xml" (
  echo [ERROR] admin module not found: "%ADMIN_DIR%" (missing pom.xml)
  exit /b 2
)

pushd "%ADMIN_DIR%" >nul
echo [INFO] Running admin in: %CD%
if defined FORWARD_ARGS (
  echo [INFO] Extra args: %FORWARD_ARGS%
)
call mvn clean package spring-boot:run %FORWARD_ARGS%
set "ERR=%ERRORLEVEL%"
popd >nul
exit /b %ERR%

:web
set "WEB_DIR=%ROOT_DIR%xlinks-router-web\xlinks-router-web-vue"
if not exist "%WEB_DIR%\package.json" (
  echo [ERROR] web(vue) module not found: "%WEB_DIR%" (missing package.json)
  exit /b 2
)

pushd "%WEB_DIR%" >nul
echo [INFO] Running web(vue) in: %CD%
if defined FORWARD_ARGS (
  echo [INFO] Extra args: %FORWARD_ARGS%
)
call npm run dev %FORWARD_ARGS%
set "ERR=%ERRORLEVEL%"
popd >nul
exit /b %ERR%

:help
echo Usage:
echo   %~nx0 client
echo   %~nx0 api
echo   %~nx0 admin
echo   %~nx0 web
echo.
echo Examples:
echo   %~nx0 client
echo   %~nx0 api
echo   %~nx0 admin
echo   %~nx0 web
echo.
echo Forward args (optional):
echo   %~nx0 client -- -DskipTests
echo   %~nx0 web -- --host 0.0.0.0 --port 5173
exit /b 1
