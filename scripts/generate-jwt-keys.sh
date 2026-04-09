#!/usr/bin/env bash
set -euo pipefail

OUTPUT_DIR="${1:-./secrets/jwt}"

mkdir -p "$OUTPUT_DIR"

echo "Generating RSA 2048-bit key pair..."
openssl genrsa 2048 2>/dev/null \
  | openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt \
  > "$OUTPUT_DIR/app.key"

openssl rsa -in "$OUTPUT_DIR/app.key" -pubout -out "$OUTPUT_DIR/app.pub" 2>/dev/null

echo "Keys written to:"
echo "  Private: $OUTPUT_DIR/app.key"
echo "  Public:  $OUTPUT_DIR/app.pub"
