# Payments System

A simple payments system that handles money transfers between accounts safely and efficiently.

## What It Does

This system lets you:
- Create accounts with starting balances
- Transfer money between accounts
- Process multiple transfers at once
- Handle failures gracefully
- Prevent duplicate transfers

## How It Works

The system has two main services:

1. **Ledger Service** (Port 8081)
   - Stores account balances
   - Handles the actual money movement
   - Keeps a permanent record of all transfers

2. **Transfer Service** (Port 8080)
   - Provides the public API
   - Handles transfer requests from clients
   - Manages retries and failures

## Key Features

- **Safe Transfers**: Money is never lost or duplicated
- **Duplicate Prevention**: Same transfer can't be processed twice
- **Concurrent Safety**: Multiple transfers can happen at the same time
- **Fault Tolerance**: System keeps working even if services fail
- **Batch Processing**: Process up to 20 transfers at once
- **Complete History**: Every transfer is permanently recorded
- **Easy Setup**: Run with Docker or Maven

## Getting Started

### What You Need

- Java 17 or higher
- Maven 3.6 or higher
- Docker (optional, but recommended)

### Running the System

**Option 1: With Docker (Easiest)**
```bash
docker-compose up --build
```

**Option 2: With Maven**
```bash
# Terminal 1 - Start Ledger Service
cd ledger-service
./mvnw spring-boot:run

# Terminal 2 - Start Transfer Service  
cd transfer-service
./mvnw spring-boot:run
```

That's it! Both services will be running and ready to use.

## API Endpoints

### Ledger Service (http://localhost:8081)
- `POST /accounts` - Create new account
- `GET /accounts/{id}` - Get account details
- `POST /ledger/transfer` - Process transfer (internal use)
- `GET /health` - Check if service is running
- `GET /swagger-ui.html` - API documentation

### Transfer Service (http://localhost:8080)
- `POST /transfers` - Create transfer (needs Idempotency-Key header)
- `GET /transfers/{id}` - Check transfer status
- `POST /transfers/batch` - Process up to 20 transfers at once
- `GET /actuator/health` - Check if service is running
- `GET /swagger-ui.html` - API documentation

## Example Usage

### 1. Create Accounts
```bash
# Create account 1 with 1000 Rands
curl -X POST http://localhost:8081/accounts \
  -H "Content-Type: application/json" \
  -d '{"initialBalance": 1000.00}'

# Create account 2 with 500 Rands
curl -X POST http://localhost:8081/accounts \
  -H "Content-Type: application/json" \
  -d '{"initialBalance": 500.00}'
```

### 2. Transfer Money
```bash
# Transfer 100 Rands from account 1 to account 2
curl -X POST http://localhost:8080/transfers \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: transfer-123" \
  -H "X-Request-ID: req-456" \
  -d '{
    "fromAccountId": 1,
    "toAccountId": 2,
    "amount": 100.00
  }'
```

### 3. Check Transfer Status
```bash
curl http://localhost:8080/transfers/{transferId}
```

### 4. Batch Transfer
```bash
curl -X POST http://localhost:8080/transfers/batch \
  -H "Content-Type: application/json" \
  -H "X-Request-ID: batch-789" \
  -d '{
    "transfers": [
      {
        "idempotencyKey": "batch-1",
        "fromAccountId": 1,
        "toAccountId": 2,
        "amount": 50.00
      },
      {
        "idempotencyKey": "batch-2",
        "fromAccountId": 2,
        "toAccountId": 1,
        "amount": 25.00
      }
    ]
  }'
```

## Testing

Run the tests to make sure everything works:

```bash
# Test Ledger Service
cd ledger-service
./mvnw test

# Test Transfer Service
cd transfer-service
./mvnw test
```

## Database

- **Development**: Uses H2 in-memory database (no setup needed)
- **Production**: Uses PostgreSQL (configured in docker-compose)
- **Schema**: Automatically created from the code

### View Database (Development)
- Ledger Service: http://localhost:8081/h2-console
- Transfer Service: http://localhost:8080/h2-console

## Monitoring

### Health Checks
- Ledger Service: http://localhost:8081/health
- Transfer Service: http://localhost:8080/actuator/health

### API Documentation
- Ledger Service: http://localhost:8081/swagger-ui.html
- Transfer Service: http://localhost:8080/swagger-ui.html

## Troubleshooting

**Port conflicts**: Make sure ports 8080 and 8081 are available

**Database issues**: If using Docker, make sure PostgreSQL is running

**View logs**:
```bash
docker-compose logs -f ledger-service
docker-compose logs -f transfer-service
```

## Project Structure
```
fintech-takehome/
├── ledger-service/          # Handles money movement
├── transfer-service/        # Public API
├── docker-compose.yml       # Easy deployment
└── README.md
```
