#!/usr/bin/env bash

set -euo pipefail
umask 077

SECRETS_DIR="/var/lib/gymio/secrets"

required_vars=(
    DB_URL
    DB_USERNAME
    DB_PASSWORD
    JWT_SECRET
    JWT_EXPIRATION_MS
    INTERNAL_AUTH_SECRET
    KAFKA_BOOTSTRAP_SERVERS
    MAIL_USERNAME
    MAIL_PASSWORD
)

for var in "${required_vars[@]}"; do
    if [[ -z "${!var:-}" ]]; then
        echo "ERROR: Required environment variable $var is missing." >&2
        exit 1
    fi
done

install -d -o root -g root -m 700 "$SECRETS_DIR"

write_env() {
    local file="$1"
    shift

    local tmp_file
    tmp_file="$(mktemp "${file}.tmp.XXXXXX")"

    while (( "$#" )); do
        local key="$1"
        local value="$2"
        shift 2

        if [[ "$value" == *$'\n'* || "$value" == *$'\r'* ]]; then
            echo "ERROR: Multiline value is not supported for $key." >&2
            rm -f "$tmp_file"
            return 1
        fi

        value="${value//\'/\\\'}"
        printf "%s='%s'\n" "$key" "$value" >> "$tmp_file"
    done

    chown root:root "$tmp_file"
    chmod 600 "$tmp_file"

    mv -f -- "$tmp_file" "$file"
}

write_env "$SECRETS_DIR/api-gateway.env" \
    INTERNAL_AUTH_SECRET "$INTERNAL_AUTH_SECRET"

write_env "$SECRETS_DIR/user-service.env" \
    SPRING_DATASOURCE_URL "$DB_URL" \
    DB_URL "$DB_URL" \
    DB_USERNAME "$DB_USERNAME" \
    DB_PASSWORD "$DB_PASSWORD" \
    JWT_SECRET "$JWT_SECRET" \
    JWT_EXPIRATION_MS "$JWT_EXPIRATION_MS" \
    INTERNAL_AUTH_SECRET "$INTERNAL_AUTH_SECRET" \
    KAFKA_BOOTSTRAP_SERVERS "$KAFKA_BOOTSTRAP_SERVERS"

write_env "$SECRETS_DIR/fitness-booking-service.env" \
    DB_URL "$DB_URL" \
    DB_USERNAME "$DB_USERNAME" \
    DB_PASSWORD "$DB_PASSWORD" \
    JWT_SECRET "$JWT_SECRET" \
    JWT_EXPIRATION_MS "$JWT_EXPIRATION_MS" \
    APP_INTERNAL_AUTH_SECRET "$INTERNAL_AUTH_SECRET" \
    KAFKA_BOOTSTRAP_SERVERS "$KAFKA_BOOTSTRAP_SERVERS"

write_env "$SECRETS_DIR/membership-payment-service.env" \
    DB_URL "$DB_URL" \
    DB_USERNAME "$DB_USERNAME" \
    DB_PASSWORD "$DB_PASSWORD" \
    INTERNAL_AUTH_SECRET "$INTERNAL_AUTH_SECRET" \
    KAFKA_BOOTSTRAP_SERVERS "$KAFKA_BOOTSTRAP_SERVERS"

write_env "$SECRETS_DIR/notification-service.env" \
    MAIL_USERNAME "$MAIL_USERNAME" \
    MAIL_PASSWORD "$MAIL_PASSWORD" \
    INTERNAL_AUTH_SECRET "$INTERNAL_AUTH_SECRET" \
    KAFKA_BOOTSTRAP_SERVERS "$KAFKA_BOOTSTRAP_SERVERS"

echo "Gymio service environment files created successfully."
