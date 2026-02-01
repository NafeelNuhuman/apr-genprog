@echo off
setlocal enabledelayedexpansion

@REM REM Build (uses Maven Wrapper)
@REM call "%~dp0..\mvnw.cmd" -q clean package
@REM if errorlevel 1 exit /b 1

REM Run CLI uber-jar
java -jar "%~dp0..\cli\target\apr-cli.jar" %*