@echo off
title Run Program
setlocal

REM --------------------------------------------
REM Check input file argument
REM --------------------------------------------
if "%1"=="" (
    echo Usage: run file.k [program arguments...]
    goto end
)

REM --------------------------------------------
REM Establish root
REM --------------------------------------------
set ROOT=%~dp0
cd /d "%ROOT%\.."
set ROOT=%CD%
cd /d "%~dp0"

set INPUT=%ROOT%\programs\%1
set BUILD=%ROOT%\build

REM --------------------------------------------
REM Verify input file exists
REM --------------------------------------------
if not exist "%INPUT%" (
    echo ERROR: Program not found:
    echo %INPUT%
    goto end
)

REM --------------------------------------------
REM Verify compiled parser exists
REM --------------------------------------------
if not exist "%BUILD%\Parser.class" (
    echo ERROR: No compiled parser found. Run build.bat first.
    goto end
)

REM --------------------------------------------
REM Run program
REM --------------------------------------------
echo Running %1 ...
pushd "%BUILD%"
java K "%INPUT%" %*
popd

:end
