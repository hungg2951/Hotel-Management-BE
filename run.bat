@echo off
title Hotel Management Backend
cls
echo ====================================================
echo   KHOI DONG HOTEL MANAGEMENT BACKEND (PORT 8080)
echo ====================================================
echo.

call .\mvnw.cmd spring-boot:run
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Co loi xay ra khi khoi dong ung dung.
    pause
)
