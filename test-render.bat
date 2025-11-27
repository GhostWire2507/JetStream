@echo off
cd /d c:\Users\dell\IdeaProjects\JetStream
mvn clean compile -q
java -cp "target/classes;%USERPROFILE%\.m2\repository\org\postgresql\postgresql\42.7.3\postgresql-42.7.3.jar" com.jetstream.database.RenderConnectionTest
pause
