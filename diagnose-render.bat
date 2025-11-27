@echo off
echo Diagnosing Render PostgreSQL Connection...
echo.
echo Step 1: Testing DNS resolution
nslookup dpg-d45e5v75r7bs73ag245g-a.postgres.render.com
echo.
echo Step 2: Current config.properties content:
type config.properties
echo.
echo Please verify the hostname in Render dashboard and update config.properties if needed.
pause
