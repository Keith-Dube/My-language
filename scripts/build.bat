@echo off
title Building Grammar
setlocal enabledelayedexpansion

REM --------------------------------------------
REM Check argument
REM --------------------------------------------
set GRAM=%1
if "%GRAM%"=="" (
    echo Usage: build GrammarName
    goto end
)

REM --------------------------------------------
REM Establish ROOT as /Kai folder
REM --------------------------------------------
set ROOT=%~dp0
cd /d "%ROOT%\.."
set ROOT=%CD%
cd /d "%~dp0"

REM --------------------------------------------
REM Paths
REM --------------------------------------------
set GRAMMAR_FILE=%ROOT%\K\%GRAM%.atg
set SRC_DIR=%ROOT%\src
set BUILD_DIR=%ROOT%\build
set COCO=%ROOT%\tools\Coco.jar
set ERRORS=%ROOT%\build_errors.txt

echo.
echo --------------------------------------
echo   BUILDING GRAMMAR WITH COCO/R
echo --------------------------------------
echo.

if not exist "%GRAMMAR_FILE%" (
    echo ERROR: Grammar file not found:
    echo %GRAMMAR_FILE%
    goto end
)

if not exist "%SRC_DIR%" mkdir "%SRC_DIR%"
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"

REM --------------------------------------------
REM Generate scanner, parser, driver
REM --------------------------------------------
echo Running Coco/R...
java -jar "%COCO%" -m "%GRAMMAR_FILE%"
if errorlevel 1 (
    echo ERROR: Coco/R failed.
    goto end
)

REM --------------------------------------------
REM Move generated Java files to src
REM --------------------------------------------
echo Moving generated Java files to /src ...
move /Y "%ROOT%\K\Parser.java" "%SRC_DIR%" >nul 2>nul
move /Y "%ROOT%\K\Scanner.java" "%SRC_DIR%" >nul 2>nul

REM --------------------------------------------
REM Compile Java files in dependency order
REM --------------------------------------------
echo Compiling Java files in dependency order...

REM Clear previous errors
if exist "%ERRORS%" del "%ERRORS%"

REM Compile FatalError first
echo Compiling FatalError.java ...
javac -d "%BUILD_DIR%" "%SRC_DIR%\FatalError.java" 2>> "%ERRORS%"
if errorlevel 1 (
    echo ERROR: Compilation failed on FatalError.java — see build_errors.txt
    goto end
)

REM Compile remaining files using -cp to see already compiled classes
for %%f in (
    Buffer.java
    UTF8Buffer.java
    Token.java
    StartStates.java
    Scanner.java
    Parser.java
    %GRAM%.java
) do (
    echo Compiling %%f ...
    javac -cp "%BUILD_DIR%" -d "%BUILD_DIR%" "%SRC_DIR%\%%f" 2>> "%ERRORS%"
    if errorlevel 1 (
        echo ERROR: Compilation failed on %%f — see build_errors.txt
        goto end
    )
)

echo.
echo BUILD SUCCESSFUL 
echo.

:end


