#!/bin/bash
# =============================================================================
# Certificate chain generator for Cinema Booking System
# Chain: Root CA -> Intermediate CA -> Server
# =============================================================================

set -e

export MSYS_NO_PATHCONV=1

STUDENT_ID="${STUDENT_ID:-XXXXXXXX}"
KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD:-changeit}"
VALIDITY_ROOT=3650
VALIDITY_INTERMEDIATE=1825
VALIDITY_SERVER=365

OUTPUT_DIR="certs"
mkdir -p "$OUTPUT_DIR"

echo "=== [1/5] Generating Root CA ==="
openssl genrsa -out "$OUTPUT_DIR/cinema-root-ca.key" 4096

openssl req -new -x509 \
  -days $VALIDITY_ROOT \
  -key "$OUTPUT_DIR/cinema-root-ca.key" \
  -out "$OUTPUT_DIR/cinema-root-ca.crt" \
  -subj "/C=RU/ST=Moscow/L=Moscow/O=CinemaBookingSystem/OU=Student-${STUDENT_ID}/CN=Cinema-RootCA"

echo "=== [2/5] Generating Intermediate CA ==="
openssl genrsa -out "$OUTPUT_DIR/cinema-intermediate-ca.key" 4096

openssl req -new \
  -key "$OUTPUT_DIR/cinema-intermediate-ca.key" \
  -out "$OUTPUT_DIR/cinema-intermediate-ca.csr" \
  -subj "/C=RU/ST=Moscow/L=Moscow/O=CinemaBookingSystem/OU=Student-${STUDENT_ID}/CN=Cinema-IntermediateCA"

cat > "$OUTPUT_DIR/intermediate-ext.cnf" << EOF
basicConstraints=CA:TRUE,pathlen:0
keyUsage=critical,keyCertSign,cRLSign
subjectKeyIdentifier=hash
authorityKeyIdentifier=keyid,issuer
EOF

openssl x509 -req \
  -days $VALIDITY_INTERMEDIATE \
  -in "$OUTPUT_DIR/cinema-intermediate-ca.csr" \
  -CA "$OUTPUT_DIR/cinema-root-ca.crt" \
  -CAkey "$OUTPUT_DIR/cinema-root-ca.key" \
  -CAcreateserial \
  -out "$OUTPUT_DIR/cinema-intermediate-ca.crt" \
  -extfile "$OUTPUT_DIR/intermediate-ext.cnf"

echo "=== [3/5] Generating server certificate ==="
openssl genrsa -out "$OUTPUT_DIR/cinema-server.key" 2048

openssl req -new \
  -key "$OUTPUT_DIR/cinema-server.key" \
  -out "$OUTPUT_DIR/cinema-server.csr" \
  -subj "/C=RU/ST=Moscow/L=Moscow/O=CinemaBookingSystem/OU=Student-${STUDENT_ID}/CN=localhost"

cat > "$OUTPUT_DIR/server-ext.cnf" << EOF
basicConstraints=CA:FALSE
keyUsage=critical,digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth
subjectAltName=DNS:localhost,IP:127.0.0.1
EOF

openssl x509 -req \
  -days $VALIDITY_SERVER \
  -in "$OUTPUT_DIR/cinema-server.csr" \
  -CA "$OUTPUT_DIR/cinema-intermediate-ca.crt" \
  -CAkey "$OUTPUT_DIR/cinema-intermediate-ca.key" \
  -CAcreateserial \
  -out "$OUTPUT_DIR/cinema-server.crt" \
  -extfile "$OUTPUT_DIR/server-ext.cnf"

echo "=== [4/5] Building PKCS12 keystore ==="
cat "$OUTPUT_DIR/cinema-server.crt" \
    "$OUTPUT_DIR/cinema-intermediate-ca.crt" \
    "$OUTPUT_DIR/cinema-root-ca.crt" > "$OUTPUT_DIR/cinema-chain.crt"

openssl pkcs12 -export \
  -in "$OUTPUT_DIR/cinema-server.crt" \
  -inkey "$OUTPUT_DIR/cinema-server.key" \
  -certfile "$OUTPUT_DIR/cinema-chain.crt" \
  -name server \
  -out src/main/resources/keystore.p12 \
  -passout "pass:${KEYSTORE_PASSWORD}"

echo "=== [5/5] Verifying certificate chain ==="
openssl verify \
  -CAfile "$OUTPUT_DIR/cinema-root-ca.crt" \
  -untrusted "$OUTPUT_DIR/cinema-intermediate-ca.crt" \
  "$OUTPUT_DIR/cinema-server.crt"

echo ""
echo "Certificates are ready in: $OUTPUT_DIR/"
echo "Keystore: src/main/resources/keystore.p12"
echo ""
echo "Run with HTTPS:"
echo "  SSL_ENABLED=true SSL_KEY_STORE_PASSWORD=${KEYSTORE_PASSWORD} SERVER_PORT=8443 mvn spring-boot:run"
