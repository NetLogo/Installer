@echo off

if "%1" == "" if "%2" == "" exit /b 1

taskkill /f /pid %1 || exit /b 1

del /s /q "C:\Program Files\NetLogo Installer" || exit /b 1
xcopy /s /i /y /q "%2" "C:\Program Files\NetLogo Installer" || exit /b 1
del /s /q "%2" || exit /b 1

cmd /c "C:\Program Files\NetLogo Installer\NetLogo Installer.exe" || exit /b 1
