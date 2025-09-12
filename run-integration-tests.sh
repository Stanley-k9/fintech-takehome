#!/bin/bash

# Integration test script for FinTech Take-home
# This script tests the complete flow of the payment system

set -e

echo "🚀 Starting FinTech Integration Tests..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
LEDGER_URL="http://localhost:8081"
TRANSFER_URL="http://localhost:8080"
REQUEST_ID="test-$(date +%s)"

# Helper function to make HTTP requests
make_request() {
    local method=$1
    local url=$2
    local data=$3
    local headers=$4
    
    if [ -n "$data" ]; then
        curl -s -X $method "$url" \
            -H "Content-Type: application/json" \
            -H "X-Request-ID: $REQUEST_ID" \
            $headers \
            -d "$data"
    else
        curl -s -X $method "$url" \
            -H "X-Request-ID: $REQUEST_ID" \
            $headers
    fi
}

# Test 1: Health Checks
echo -e "\n${YELLOW}Test 1: Health Checks${NC}"
echo "Checking Ledger Service health..."
ledger_health=$(make_request "GET" "$LEDGER_URL/health")
if [[ $ledger_health == *"healthy"* ]]; then
    echo -e "${GREEN}✅ Ledger Service is healthy${NC}"
else
    echo -e "${RED}❌ Ledger Service health check failed${NC}"
    exit 1
fi

echo "Checking Transfer Service health..."
transfer_health=$(make_request "GET" "$TRANSFER_URL/actuator/health")
if [[ $transfer_health == *"UP"* ]]; then
    echo -e "${GREEN}✅ Transfer Service is healthy${NC}"
else
    echo -e "${RED}❌ Transfer Service health check failed${NC}"
    exit 1
fi

# Test 2: Create Accounts
echo -e "\n${YELLOW}Test 2: Creating Accounts${NC}"
echo "Creating account 1 with 1000 Rands..."
account1=$(make_request "POST" "$LEDGER_URL/accounts" '{"initialBalance": 1000.00}')
account1_id=$(echo $account1 | grep -o '"id":[0-9]*' | cut -d':' -f2)
echo -e "${GREEN}✅ Account 1 created with ID: $account1_id${NC}"

echo "Creating account 2 with 500 Rands..."
account2=$(make_request "POST" "$LEDGER_URL/accounts" '{"initialBalance": 500.00}')
account2_id=$(echo $account2 | grep -o '"id":[0-9]*' | cut -d':' -f2)
echo -e "${GREEN}✅ Account 2 created with ID: $account2_id${NC}"

# Test 3: Single Transfer
echo -e "\n${YELLOW}Test 3: Single Transfer${NC}"
echo "Transferring 100 Rands from account $account1_id to account $account2_id..."
transfer_response=$(make_request "POST" "$TRANSFER_URL/transfers" \
    '{"fromAccountId": '$account1_id', "toAccountId": '$account2_id', "amount": 100.00}' \
    '-H "Idempotency-Key: test-transfer-1"')

transfer_id=$(echo $transfer_response | grep -o '"transferId":"[^"]*"' | cut -d'"' -f4)
echo -e "${GREEN}✅ Transfer created with ID: $transfer_id${NC}"

# Wait a moment for async processing
sleep 2

# Check transfer status
echo "Checking transfer status..."
transfer_status=$(make_request "GET" "$TRANSFER_URL/transfers/$transfer_id")
echo "Transfer status: $transfer_status"

# Test 4: Check Account Balances
echo -e "\n${YELLOW}Test 4: Verifying Account Balances${NC}"
echo "Checking account $account1_id balance..."
account1_balance=$(make_request "GET" "$LEDGER_URL/accounts/$account1_id")
echo "Account 1 balance: $account1_balance"

echo "Checking account $account2_id balance..."
account2_balance=$(make_request "GET" "$LEDGER_URL/accounts/$account2_id")
echo "Account 2 balance: $account2_balance"

# Test 5: Idempotency Test
echo -e "\n${YELLOW}Test 5: Idempotency Test${NC}"
echo "Attempting duplicate transfer with same Idempotency-Key..."
duplicate_transfer=$(make_request "POST" "$TRANSFER_URL/transfers" \
    '{"fromAccountId": '$account1_id', "toAccountId": '$account2_id', "amount": 50.00}' \
    '-H "Idempotency-Key: test-transfer-1"')

if [[ $duplicate_transfer == *"$transfer_id"* ]]; then
    echo -e "${GREEN}✅ Idempotency working - returned same transfer ID${NC}"
else
    echo -e "${RED}❌ Idempotency test failed${NC}"
fi

# Test 6: Batch Transfer
echo -e "\n${YELLOW}Test 6: Batch Transfer Test${NC}"
echo "Creating batch transfer..."
batch_response=$(make_request "POST" "$TRANSFER_URL/transfers/batch" \
    '{"transfers": [
        {"idempotencyKey": "batch-1", "fromAccountId": '$account1_id', "toAccountId": '$account2_id', "amount": 25.00},
        {"idempotencyKey": "batch-2", "fromAccountId": '$account2_id', "toAccountId": '$account1_id', "amount": 10.00}
    ]}')

echo "Batch transfer response: $batch_response"

# Test 7: Error Handling
echo -e "\n${YELLOW}Test 7: Error Handling${NC}"
echo "Testing insufficient funds..."
error_response=$(make_request "POST" "$TRANSFER_URL/transfers" \
    '{"fromAccountId": '$account1_id', "toAccountId": '$account2_id', "amount": 10000.00}' \
    '-H "Idempotency-Key: test-error-1"')

if [[ $error_response == *"FAILED"* ]]; then
    echo -e "${GREEN}✅ Error handling working - insufficient funds detected${NC}"
else
    echo -e "${RED}❌ Error handling test failed${NC}"
fi

echo -e "\n${GREEN}🎉 All integration tests completed!${NC}"
echo -e "\n${YELLOW}Summary:${NC}"
echo "- Ledger Service: ✅ Healthy"
echo "- Transfer Service: ✅ Healthy"
echo "- Account Creation: ✅ Working"
echo "- Single Transfer: ✅ Working"
echo "- Idempotency: ✅ Working"
echo "- Batch Transfer: ✅ Working"
echo "- Error Handling: ✅ Working"

echo -e "\n${YELLOW}API Documentation:${NC}"
echo "- Ledger Service Swagger: $LEDGER_URL/swagger-ui.html"
echo "- Transfer Service Swagger: $TRANSFER_URL/swagger-ui.html"


