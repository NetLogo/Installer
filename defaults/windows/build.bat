@echo off

if not exist build mkdir build

cl /Fo:build\defaults.obj /Fe:build\defaults.exe defaults.cpp /link shell32.lib advapi32.lib

if %errorlevel% neq 0 exit 1
