#!/bin/bash
echo "========================================="
echo "Running JetStream with Debug Output"
echo "========================================="
./mvnw clean compile javafx:run 2>&1 | tee run-output.log

