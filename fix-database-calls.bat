@echo off
setlocal enabledelayedexpansion
cd /d c:\Users\dell\IdeaProjects\JetStream

REM Replace all getActiveConnection() with getConnection()
for /r src\main\java %%F in (*.java) do (
    powershell -Command "(Get-Content '%%F') -replace 'DatabaseConnection\.getActiveConnection\(\)', 'DatabaseConnection.getConnection()' -replace 'DatabaseConnection\.getPostgres\(\)', 'DatabaseConnection.getConnection()' | Set-Content '%%F'"
)

echo Replacement complete!
