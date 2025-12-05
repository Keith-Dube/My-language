@echo off
title Run Program
setlocal

if "%1"=="" (
    echo Usage: run file.k
    goto end
)

REM Establish root
set ROOT=%~dp0
cd /d "%ROOT%\.."
set ROOT=%CD%
cd /d "%~dp0"

set INPUT=%ROOT%\programs\%1
set BUILD=%ROOT%\build

if not exist "%INPUT%" (
    echo ERROR: Program not found:
    echo %INPUT%
    goto end
)

if not exist "%BUILD%\Parser.class" (
    echo ERROR: No compiled parser found. Run build.bat first.
    goto end
)

echo Running %1 ...
pushd "%BUILD%"
java K "%INPUT%"
popd

:end
echo.
pause
