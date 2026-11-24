#!/usr/bin/env bash
#
# Renders a production docker-compose.yml for Elastic Beanstalk on
# Amazon Linux 2023 ("64bit Amazon Linux 2023 running Docker").

set -euo pipefail

OUT="${1:?usage: render-compose.sh <output-compose.yml>}"

: "${ECR_REGISTRY:?ECR_REGISTRY is not set}"
: "${IMAGE_TAG:?IMAGE_TAG is not set}"
: "${SERVICES:?SERVICES is not set}"

require() {
    if [ -z "${1:-}" ]; then
        echo "ERROR: required variable '$2' is empty" >&2
        exit 1
    fi
}

require "${INTERNAL_AUTH_SECRET:-}"  INTERNAL_AUTH_SECRET
require "${JWT_SECRET:-}"            JWT_SECRET
require "${DB_URL:-}"                DB_URL
require "${DB_USERNAME:-}"           DB_USERNAME
require "${DB_PASSWORD:-}"           DB_PASSWORD
require "${KAFKA_BOOTSTRAP_SERVERS:-}" KAFKA_BOOTSTRAP_SERVERS
require "${MAIL_USERNAME:-}"         MAIL_USERNAME
require "${MAIL_PASSWORD:-}"         MAIL_PASSWORD

yaml_sq() {
    printf "'%s'" "$(printf '%s' "${1:-}" | tr -d '\r\n' | sed "s/'/''/g")"
}

declare -A REPO_MAP
read -r -a pairs <<< "$SERVICES"
for pair in "${pairs[@]}"; do
    dir="${pair%%=*}"
    repo="${pair#*=}"
    REPO_MAP["$dir"]="$repo"
done

img_for() {
    local repo="${REPO_MAP[$1]:?missing repo mapping for $1}"
    printf '%s/%s:%s' "$ECR_REGISTRY" "$repo" "$IMAGE_TAG"
}

cat > "$OUT" <<YAML
services:
  api-gateway:
    image: $(img_for api-gateway)
    container_name: gymio-api-gateway
    restart: unless-stopped
    ports:
      - "80:8080"
    environment:
      PORT: "8080"
      SPRING_PROFILES_ACTIVE: "docker"
      INTERNAL_AUTH_SECRET: $(yaml_sq "$INTERNAL_AUTH_SECRET")
      USER_SERVICE_VALIDATION_URL: "http://user-service:8081/internal/auth/validate"
      USER_SERVICE_URL: "http://user-service:8081"
      FITNESS_SERVICE_URL: "http://fitness-booking-service:8082"
      MEMBERSHIP_PAYMENT_SERVICE_URL: "http://membership-payment-service:8083"
      NOTIFICATION_SERVICE_URL: "http://notification-service:8084"
    depends_on:
      - user-service
      - fitness-booking-service
      - membership-payment-service
      - notification-service
    networks:
      - gymio-network

  user-service:
    image: $(img_for user-service)
    container_name: gymio-user-service
    restart: unless-stopped
    environment:
      PORT: "8081"
      SPRING_PROFILES_ACTIVE: "docker"
      SPRING_DATASOURCE_URL: $(yaml_sq "$DB_URL")
      DB_URL: $(yaml_sq "$DB_URL")
      DB_USERNAME: $(yaml_sq "$DB_USERNAME")
      DB_PASSWORD: $(yaml_sq "$DB_PASSWORD")
      JWT_SECRET: $(yaml_sq "$JWT_SECRET")
      JWT_EXPIRATION_MS: $(yaml_sq "${JWT_EXPIRATION_MS:-86400000}")
      INTERNAL_AUTH_SECRET: $(yaml_sq "$INTERNAL_AUTH_SECRET")
      KAFKA_BOOTSTRAP_SERVERS: $(yaml_sq "$KAFKA_BOOTSTRAP_SERVERS")
    networks:
      - gymio-network

  fitness-booking-service:
    image: $(img_for fitness-booking-service)
    container_name: gymio-fitness-booking-service
    restart: unless-stopped
    environment:
      PORT: "8082"
      SPRING_PROFILES_ACTIVE: "docker"
      DB_URL: $(yaml_sq "$DB_URL")
      DB_USERNAME: $(yaml_sq "$DB_USERNAME")
      DB_PASSWORD: $(yaml_sq "$DB_PASSWORD")
      JWT_SECRET: $(yaml_sq "$JWT_SECRET")
      JWT_EXPIRATION_MS: $(yaml_sq "${JWT_EXPIRATION_MS:-86400000}")
      KAFKA_BOOTSTRAP_SERVERS: $(yaml_sq "$KAFKA_BOOTSTRAP_SERVERS")
      APP_INTERNAL_AUTH_SECRET: $(yaml_sq "$INTERNAL_AUTH_SECRET")
      APP_USER_SERVICE_BASE_URL: "http://user-service:8081"
    depends_on:
      - user-service
    networks:
      - gymio-network

  membership-payment-service:
    image: $(img_for membership-payment-service)
    container_name: gymio-membership-payment-service
    restart: unless-stopped
    environment:
      MEMBERSHIP_PAYMENT_SERVICE_PORT: "8083"
      SPRING_PROFILES_ACTIVE: "docker"
      DB_URL: $(yaml_sq "$DB_URL")
      DB_USERNAME: $(yaml_sq "$DB_USERNAME")
      DB_PASSWORD: $(yaml_sq "$DB_PASSWORD")
      KAFKA_BOOTSTRAP_SERVERS: $(yaml_sq "$KAFKA_BOOTSTRAP_SERVERS")
      INTERNAL_AUTH_SECRET: $(yaml_sq "$INTERNAL_AUTH_SECRET")
    networks:
      - gymio-network

  notification-service:
    image: $(img_for notification-service)
    container_name: gymio-notification-service
    restart: unless-stopped
    environment:
      PORT: "8084"
      SPRING_PROFILES_ACTIVE: "docker"
      MAIL_USERNAME: $(yaml_sq "$MAIL_USERNAME")
      MAIL_PASSWORD: $(yaml_sq "$MAIL_PASSWORD")
      PUBLIC_BASE_URL: $(yaml_sq "${PUBLIC_BASE_URL:-http://localhost:8080}")
      KAFKA_BOOTSTRAP_SERVERS: $(yaml_sq "$KAFKA_BOOTSTRAP_SERVERS")
      INTERNAL_AUTH_SECRET: $(yaml_sq "$INTERNAL_AUTH_SECRET")
    networks:
      - gymio-network

networks:
  gymio-network:
    driver: bridge
YAML

echo "Wrote $OUT for tag $IMAGE_TAG"
