#!/bin/bash

# Script to wait for application to be ready
# Usage: ./wait-for-app.sh <base_url>
#
# Returns: 0 if app is ready, 1 if timeout

BASE_URL="$1"

echo "Waiting for application to be ready..."
sleep 15

MAX_RETRIES=20
RETRY_COUNT=0
WAIT_TIME=10

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
  echo "Attempt $((RETRY_COUNT + 1))/$MAX_RETRIES: Checking if application is ready..."
  
  # Check if server responds (any HTTP response confirms app is running)
  if curl -s -o /dev/null -w "%{http_code}" "$BASE_URL" | grep -q "[0-9]"; then
    echo "Application is ready!"
    exit 0
  else
    echo "Application not ready yet. Waiting ${WAIT_TIME} seconds..."
    sleep $WAIT_TIME
    RETRY_COUNT=$((RETRY_COUNT + 1))
  fi
done

echo "Application did not become ready after $((MAX_RETRIES * WAIT_TIME + 30)) seconds"
exit 1
