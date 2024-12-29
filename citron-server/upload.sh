#!/bin/bash

if [ $# -ne 1 ]; then
  echo "Missing VERSION parameter!"
  echo "Usage: ./upload.sh <VERSION>"
  exit 1
fi

VERSION=$1

JAR_NAME="citron-$VERSION-runner.jar"

RESPONSE=$(curl -F "file=@target/$JAR_NAME" https://tmpfiles.org/api/v1/upload)
echo "Response: $RESPONSE"

LINK=$(echo "$RESPONSE" | grep -o '"url":"[^"]*"' | cut -d'"' -f4 | sed 's|tmpfiles.org|tmpfiles.org/dl|')
echo "Uploaded $JAR_NAME. Download link: $LINK"
