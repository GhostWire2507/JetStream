@echo off
echo Running JetStream with debug output...
echo.

mvn clean compile javafx:run 2>&1 | tee run-output.txt

echo.
echo Output saved to run-output.txt
pause

