#!/bin/bash
# Tumar Market — Startup Script
set -e

echo "=============================="
echo "  Tumar Market — Starting Up  "
echo "=============================="

# Config (override via environment)
export DB_HOST=${DB_HOST:-localhost}
export DB_PORT=${DB_PORT:-3306}
export DB_USER=${DB_USER:-root}
export DB_PASSWORD=${DB_PASSWORD:-}
export DB_NAME=${DB_NAME:-tumar_market}
export JWT_SECRET=${JWT_SECRET:-tumar-market-secret-2024}

echo ""
echo "Database: $DB_USER@$DB_HOST:$DB_PORT/$DB_NAME"

# 1. Python setup (optional, Go server does its own migration)
if command -v python3 &>/dev/null; then
  echo ""
  echo "--- Running Python DB setup ---"
  cd python
  pip install -r requirements.txt -q 2>/dev/null || true
  python3 setup_db.py || echo "  DB setup skipped (MySQL may not be available yet)"
  python3 seed_data.py || echo "  Seed skipped"
  cd ..
fi

# 2. Start Go server
echo ""
echo "--- Starting Go server ---"
cd go

if ! command -v go &>/dev/null; then
  echo "ERROR: Go is not installed. Please install Go 1.21+"
  exit 1
fi

go mod tidy
go build -o ../tumar-market-server .
cd ..

echo ""
echo "Server starting on http://localhost:8080"
echo "Press Ctrl+C to stop"
echo ""
./tumar-market-server
