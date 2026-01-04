@echo off
setlocal
call "%~dp0..\mvnw.cmd" -q test
if errorlevel 1 exit /b 1
