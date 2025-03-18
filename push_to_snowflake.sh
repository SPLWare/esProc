#!/bin/bash

# Input arguments with default fallbacks
SNOWFLAKE_ACCOUNT=${1:-$SNOWFLAKE_ACCOUNT}
REPOSITORY_NAME=${2:-$REPOSITORY_NAME}

# Validate inputs
if [ -z "$SNOWFLAKE_ACCOUNT" ]; then
    echo "Error: Snowflake account not specified."
    echo "Usage: $0 <snowflake_account> [repository_name] or set SNOWFLAKE_ACCOUNT environment variable."
    exit 1
fi

if [ -z "$REPOSITORY_NAME" ]; then
    REPOSITORY_NAME="tutorial_db/data_schema/tutorial_repository"
    echo "Repository not specified. Using default: $REPOSITORY_NAME"
fi

# Construct dynamic variables
IMAGE_HOST="$SNOWFLAKE_ACCOUNT.registry.snowflakecomputing.com"
REPOSITORY="$IMAGE_HOST/$REPOSITORY_NAME"
IMAGE_NAME="$REPOSITORY/spl_service:latest"

# Docker build and push
echo "Building Docker image..."
docker build --rm --platform linux/amd64 -t "$IMAGE_NAME" . || { echo "Docker build failed! Exiting."; exit 1; }

echo "Pushing Docker image..."
docker push "$IMAGE_NAME" || { echo "Docker push failed! Exiting."; exit 1; }

echo "Docker image pushed successfully: $IMAGE_NAME"
