@echo off
setlocal enabledelayedexpansion

REM Build (uses Maven Wrapper)
call "%~dp0..\mvnw.cmd" -q clean package
if errorlevel 1 exit /b 1

REM Run CLI uber-jar
java -jar "%~dp0..\cli\target\apr-cli.jar" %*