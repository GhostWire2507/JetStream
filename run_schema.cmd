@echo off
cd /d C:\Users\dell\IdeaProjects\JetStream
mvn exec:java -Dexec.mainClass=com.jetstream.database.SQLiteSchemaRunner -Dexec.cleanupDaemonThreads=false
