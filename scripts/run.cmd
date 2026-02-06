@echo off
setlocal enabledelayedexpansion

REM Run CLI uber-jar
java -jar "%~dp0..\cli\target\apr-cli.jar" %*