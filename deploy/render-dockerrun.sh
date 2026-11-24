#!/usr/bin/env bash
#
# Renders Dockerrun.aws.json (v2) for the Elastic Beanstalk multi-container
# Docker platform.
#
# Every image is pinned to $ECR_REGISTRY/<ecr-repo>:$IMAGE_TAG, so a deploy is
# always traceable to one Git commit.
#
# Networking note: the EB multi-container platform runs the ECS task in *bridge*
# mode (it drops networkMode from the generated task definition), so containers
# find each other by container name through `links` - NOT through localhost.
# The gateway therefore talks to http://user-service:8081 and so on.
#
# Required environment
#   ECR_REGISTRY   e.g. 123456789012.dkr.ecr.eu-west-1.amazonaws.com
#   IMAGE_TAG      image tag (the Git commit SHA)
#   SERVICES       space separated "<service-dir>=<ecr-repo>" pairs
#
# Secrets are read from the environment. Jenkins supplies them through
# withCredentials so they never appear in the repository or in build logs.
#
# Usage
#   bash render-dockerrun.sh <output.json>
# --------------------------------------------------------------------------------

set -euo pipefail

OUT="${1:?usage: render-dockerrun.sh <output.json>}"

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

# --- helpers ---------------------------------------------------------------

# Escapes a value for embedding in a JSON string.
json_escape() {
    printf '%s' "${1:-}" \
        | tr -d '\r\n' \
        | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g'
}

# Turns "KEY=value" arguments into a JSON array of {"name","value"} objects.
env_json() {
    local out="" pair
    for pair in "$@"; do
        out="${out}{\"name\":\"$(json_escape "${pair%%=*}")\",\"value\":\"$(json_escape "${pair#*=}")\"},"
    done
    printf '[%s]' "${out%,}"
}

memory_for() {
    case "$1" in
        notification-service) printf '256' ;;
        *)                    printf '512' ;;
    esac
}

# Only the gateway is published; the other services are reachable over links.
ports_for() {
    case "$1" in
        api-gateway) printf '[{"containerPort":8080,"hostPort":80}]' ;;
        *)           printf '[]' ;;
    esac
}

links_for() {
    case "$1" in
        api-gateway)
            printf '["user-service","fitness-booking-service","membership-payment-service","notification-service"]'
            ;;
        fitness-booking-service)
            printf '["user-service"]'
            ;;
        *)
            printf '[]'
            ;;
    esac
}

env_for() {
    case "$1" in
        api-gateway)
            env_json \
                "PORT=8080" \
                "SPRING_PROFILES_ACTIVE=docker" \
                "INTERNAL_AUTH_SECRET=${INTERNAL_AUTH_SECRET}" \
                "USER_SERVICE_VALIDATION_URL=http://user-service:8081/internal/auth/validate" \
                "USER_SERVICE_URL=http://user-service:8081" \
                "FITNESS_SERVICE_URL=http://fitness-booking-service:8082" \
                "MEMBERSHIP_PAYMENT_SERVICE_URL=http://membership-payment-service:8083" \
                "NOTIFICATION_SERVICE_URL=http://notification-service:8084"
            ;;

        user-service)
            env_json \
                "PORT=8081" \
                "SPRING_PROFILES_ACTIVE=docker" \
                "DB_URL=${DB_URL}" \
                "DB_USERNAME=${DB_USERNAME}" \
                "DB_PASSWORD=${DB_PASSWORD}" \
                "JWT_SECRET=${JWT_SECRET}" \
                "JWT_EXPIRATION_MS=${JWT_EXPIRATION_MS:-86400000}" \
                "INTERNAL_AUTH_SECRET=${INTERNAL_AUTH_SECRET}" \
                "KAFKA_BOOTSTRAP_SERVERS=${KAFKA_BOOTSTRAP_SERVERS}"
            ;;

        fitness-booking-service)
            env_json \
                "PORT=8082" \
                "SPRING_PROFILES_ACTIVE=docker" \
                "DB_URL=${DB_URL}" \
                "DB_USERNAME=${DB_USERNAME}" \
                "DB_PASSWORD=${DB_PASSWORD}" \
                "JWT_SECRET=${JWT_SECRET}" \
                "JWT_EXPIRATION_MS=${JWT_EXPIRATION_MS:-86400000}" \
                "KAFKA_BOOTSTRAP_SERVERS=${KAFKA_BOOTSTRAP_SERVERS}" \
                "APP_INTERNAL_AUTH_SECRET=${INTERNAL_AUTH_SECRET}" \
                "APP_USER_SERVICE_BASE_URL=http://user-service:8081"
            ;;

        membership-payment-service)
            # NOTE: this service reads its port from MEMBERSHIP_PAYMENT_SERVICE_PORT,
            # not from PORT - see its application.properties.
            env_json \
                "MEMBERSHIP_PAYMENT_SERVICE_PORT=8083" \
                "SPRING_PROFILES_ACTIVE=docker" \
                "DB_URL=${DB_URL}" \
                "DB_USERNAME=${DB_USERNAME}" \
                "DB_PASSWORD=${DB_PASSWORD}" \
                "KAFKA_BOOTSTRAP_SERVERS=${KAFKA_BOOTSTRAP_SERVERS}" \
                "INTERNAL_AUTH_SECRET=${INTERNAL_AUTH_SECRET}"
            ;;

        notification-service)
            env_json \
                "PORT=8084" \
                "SPRING_PROFILES_ACTIVE=docker" \
                "MAIL_USERNAME=${MAIL_USERNAME}" \
                "MAIL_PASSWORD=${MAIL_PASSWORD}" \
                "PUBLIC_BASE_URL=${PUBLIC_BASE_URL:-http://localhost:8080}" \
                "KAFKA_BOOTSTRAP_SERVERS=${KAFKA_BOOTSTRAP_SERVERS}" \
                "INTERNAL_AUTH_SECRET=${INTERNAL_AUTH_SECRET}"
            ;;

        *)
            echo "ERROR: no container definition for service '$1'" >&2
            exit 1
            ;;
    esac
}

emit_container() {
    printf '    {\n'
    printf '      "name": "%s",\n' "$(json_escape "$1")"
    printf '      "image": "%s",\n' "$(json_escape "$2")"
    printf '      "essential": true,\n'
    printf '      "memory": %s,\n' "$(memory_for "$1")"
    printf '      "portMappings": %s,\n' "$(ports_for "$1")"
    printf '      "links": %s,\n' "$(links_for "$1")"
    printf '      "environment": %s\n' "$(env_for "$1")"
    printf '    }'
}

# --- render ----------------------------------------------------------------

read -r -a pairs <<< "$SERVICES"

{
    printf '{\n'
    printf '  "AWSEBDockerrunVersion": 2,\n'
    printf '  "containerDefinitions": [\n'

    first=1
    for pair in "${pairs[@]}"; do
        if [ "$first" -eq 1 ]; then
            first=0
        else
            printf ',\n'
        fi
        emit_container "${pair%%=*}" "$ECR_REGISTRY/${pair#*=}:$IMAGE_TAG"
    done

    printf '\n  ]\n}\n'
} > "$OUT"

echo "Wrote $OUT with ${#pairs[@]} container definitions (tag $IMAGE_TAG)"