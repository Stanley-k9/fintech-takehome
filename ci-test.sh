#!/bin/bash

# CI/CD Test Script for FinTech Microservices
# This script runs tests with PostgreSQL configuration

set -e

echo "=== Starting CI/CD Test Script ==="

# Set environment variables for PostgreSQL
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/fintech"
export SPRING_DATASOURCE_USERNAME="postgres"
export SPRING_DATASOURCE_PASSWORD="postgres"
export SPRING_JPA_HIBERNATE_DDL_AUTO="create-drop"
export LEDGER_SERVICE_URL="http://localhost:8081"

echo "Environment variables set:"
echo "SPRING_DATASOURCE_URL=$SPRING_DATASOURCE_URL"
echo "SPRING_DATASOURCE_USERNAME=$SPRING_DATASOURCE_USERNAME"
echo "SPRING_JPA_HIBERNATE_DDL_AUTO=$SPRING_JPA_HIBERNATE_DDL_AUTO"

# Test Ledger Service
echo "=== Testing Ledger Service ==="
cd ledger-service
./mvnw clean test -Dspring.profiles.active=ci
echo "Ledger Service tests completed successfully"

# Test Transfer Service
echo "=== Testing Transfer Service ==="
cd ../transfer-service
./mvnw clean test -Dspring.profiles.active=ci
echo "Transfer Service tests completed successfully"

# Build both services
echo "=== Building Services ==="
cd ../ledger-service
./mvnw clean package -DskipTests -Dspring.profiles.active=ci
echo "Ledger Service built successfully"

cd ../transfer-service
./mvnw clean package -DskipTests -Dspring.profiles.active=ci
echo "Transfer Service built successfully"

echo "=== CI/CD Test Script Completed Successfully ==="
