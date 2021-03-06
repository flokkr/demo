#!/usr/bin/env bash
SSL_DIR=${SSL_DIR:-/etc/ssl}
KEYSTORE_DIR=${KEYSTORE_DIR:-/etc/keystore}
openssl pkcs12 -export \
               -in $SSL_DIR/tls.crt \
               -inkey $SSL_DIR/tls.key \
               -out $KEYSTORE_DIR/keystore.p12 \
               -name jetty \
               -CAfile $SSL_DIR/ca.crt \
               -caname ca \
               -passout pass:Welcome1
keytool -importkeystore \
        -deststorepass Welcome1 \
        -destkeypass Welcome1 \
        -destkeystore $KEYSTORE_DIR/keystore \
        -srckeystore $KEYSTORE_DIR/keystore.p12 \
        -srcstoretype PKCS12 \
        -srcstorepass Welcome1 \
        -alias jetty
