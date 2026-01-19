#!/bin/bash

# Hotel Booking Agent Backend Startup Script
# This script starts the agent backend with proper Google Cloud configuration

echo "=========================================="
echo "Hotel Booking Agent Backend Startup"
echo "=========================================="
echo ""

# Check if environment variables are set
if [ -z "$GOOGLE_CLOUD_PROJECT" ]; then
    echo "ERROR: GOOGLE_CLOUD_PROJECT environment variable is not set"
    echo ""
    echo "Please set it with:"
    echo "  export GOOGLE_CLOUD_PROJECT='your-project-id'"
    echo ""
    exit 1
fi

if [ -z "$GOOGLE_CLOUD_LOCATION" ]; then
    echo "WARNING: GOOGLE_CLOUD_LOCATION not set, using default: us-central1"
    export GOOGLE_CLOUD_LOCATION="us-central1"
fi

# Check if credentials file exists
if [ ! -f "$HOME/.config/gcloud/application_default_credentials.json" ]; then
    echo "ERROR: Google Cloud credentials not found"
    echo ""
    echo "Please run ./auth.sh first to authenticate"
    echo ""
    exit 1
fi

export GOOGLE_APPLICATION_CREDENTIALS="$HOME/.config/gcloud/application_default_credentials.json"

echo "Configuration:"
echo "  Project: $GOOGLE_CLOUD_PROJECT"
echo "  Location: $GOOGLE_CLOUD_LOCATION"
echo "  Credentials: $GOOGLE_APPLICATION_CREDENTIALS"
echo ""
echo "Starting agent backend..."
echo "=========================================="
echo ""

# Start the agent backend
mvn spring-boot:run
