@echo off
title Clean Project
setlocal

set ROOT=%~dp0
cd /d "%ROOT%\.."
set ROOT=%CD%
cd /d "%~dp0"

echo Cleaning project...

REM Remove build folder contents
if exist "%ROOT%\build" (
    del /Q "%ROOT%\build\*.class" 2>nul
)

REM Remove generated scanner/parser/driver in src
del /Q "%ROOT%\src\Parser.java" 2>nul
del /Q "%ROOT%\src\Scanner.java" 2>nul
del /Q "%ROOT%\src\K.java" 2>nul

REM Remove error log
del /Q "%ROOT%\build_errors.txt" 2>nul

echo ✔ Clean complete.
pause
