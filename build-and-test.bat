@echo off
cd /d c:\Users\dell\IdeaProjects\JetStream
echo Compiling JetStream project...
call mvn clean compile -DskipTests=true
if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    pause
    exit /b 1
)

echo.
echo Testing Render PostgreSQL connection...
java -cp "target/classes" com.jetstream.database.RenderConnectionTest

pause
