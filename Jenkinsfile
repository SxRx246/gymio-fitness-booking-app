#!/usr/bin/env groovy

/**
 * Gymio - CI/CD Pipeline
 *
 * GitHub (poll SCM)
 *   -> Build and test five Maven modules
 *   -> Build Docker images
 *   -> Push images to Amazon ECR
 *   -> Deploy to AWS Elastic Beanstalk
 *   -> Verify deployment health
 *   -> Roll back to previous version on failure
 *
 * AWS authentication uses the IAM role attached to the Jenkins agent.
 * No static AWS access keys are stored in Jenkins or this file.
 */

def SERVICE_LIST = [
    'api-gateway=gymio-api-gateway',
    'user-service=gymio-user-service',
    'fitness-booking-service=gymio-fitness-booking-service',
    'membership-payment-service=gymio-membership-payment-service',
    'notification-service=gymio-notification-service'
]

pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(
            numToKeepStr: '25',
            daysToKeepStr: '30'
        ))
        timeout(time: 60, unit: 'MINUTES')
    }

    triggers {
        pollSCM('H/2 * * * *')
    }

    parameters {
        booleanParam(
            name: 'SKIP_TESTS',
            defaultValue: false,
            description: 'Skip Maven tests. Not recommended.'
        )

        booleanParam(
            name: 'ROLLBACK_ON_FAILURE',
            defaultValue: true,
            description: 'Roll back to the previous Elastic Beanstalk version if deployment verification fails.'
        )
    }

    environment {
        // Makes AWS CLI available in every Jenkins shell step.
        PATH+LOCAL = '/var/jenkins_home/.local/bin'

        // AWS target
        AWS_REGION = 'eu-north-1'
        EB_APPLICATION = 'gymio'
        EB_ENVIRONMENT = 'gymio-env'
        EB_S3_BUCKET = ''
        DEPLOY_BRANCH = 'main'

        // Jenkins agent
        JAVA_HOME_DIR = '/opt/java/openjdk'

        // Datastores
        DB_URL = 'jdbc:postgresql://172.31.39.199:5432/gymio_db'
        DB_TEST_URL = 'jdbc:postgresql://172.31.39.199:5432/gymio_test'

        // Optional
        PUBLIC_BASE_URL = ''
        NOTIFY_EMAIL = ''
    }

    stages {

        stage('Initialize') {
            steps {
                script {
                    sh '''
                        set -e

                        echo "Java: $(java -version 2>&1 | head -n 1)"
                        docker --version

                        if ! command -v aws >/dev/null 2>&1; then
                            echo "Installing AWS CLI v2..."

                            mkdir -p "$HOME/.local/aws-cli" "$HOME/.local/bin"

                            ARCH=$(uname -m)

                            case "$ARCH" in
                                x86_64)
                                    AWS_ARCH="x86_64"
                                    ;;
                                aarch64|arm64)
                                    AWS_ARCH="aarch64"
                                    ;;
                                *)
                                    echo "Unsupported architecture: $ARCH"
                                    exit 1
                                    ;;
                            esac

                            curl -fsSL \
                                "https://awscli.amazonaws.com/awscli-exe-linux-${AWS_ARCH}.zip" \
                                -o /tmp/awscliv2.zip

                            (cd /tmp && jar -xf /tmp/awscliv2.zip)

                            /tmp/aws/install \
                                -i "$HOME/.local/aws-cli" \
                                -b "$HOME/.local/bin" \
                                --update

                            rm -rf /tmp/aws /tmp/awscliv2.zip
                        fi

                        aws --version
                    '''

                    if (!env.GIT_COMMIT) {
                        error('GIT_COMMIT is not set. Check the Jenkins GitHub SCM configuration.')
                    }

                    env.SERVICES = SERVICE_LIST.join(' ')

                    // Unique image and EB version label for every Jenkins build.
                    env.IMAGE_TAG = "${env.GIT_COMMIT.take(12)}-${env.BUILD_NUMBER}"

                    def rawBranch = env.GIT_BRANCH ?:
                        sh(
                            returnStdout: true,
                            script: 'git name-rev --name-only HEAD'
                        ).trim()

                    env.BUILD_BRANCH = rawBranch
                        .replaceFirst(/^remotes\/origin\//, '')
                        .replaceFirst(/^origin\//, '')

                    env.IS_DEPLOY_BRANCH =
                        (env.BUILD_BRANCH == env.DEPLOY_BRANCH).toString()

                    env.AWS_ACCOUNT_ID = sh(
                        returnStdout: true,
                        script: 'aws sts get-caller-identity --query Account --output text'
                    ).trim()

                    env.ECR_REGISTRY =
                        "${env.AWS_ACCOUNT_ID}.dkr.ecr.${env.AWS_REGION}.amazonaws.com"

                    if (!env.EB_S3_BUCKET) {
                        env.EB_S3_BUCKET =
                            "elasticbeanstalk-${env.AWS_REGION}-${env.AWS_ACCOUNT_ID}"
                    }

                    sh """
                        aws s3api head-bucket \
                            --bucket "${env.EB_S3_BUCKET}" >/dev/null
                    """

                    // Retrieve current EB version and CNAME for rollback and links.
                    def state = sh(
                        returnStdout: true,
                        script: """
                            aws elasticbeanstalk describe-environments \
                                --application-name "${env.EB_APPLICATION}" \
                                --environment-names "${env.EB_ENVIRONMENT}" \
                                --query 'Environments[0].[VersionLabel,CNAME]' \
                                --output text 2>/dev/null || true
                        """
                    ).trim()

                    env.PREVIOUS_VERSION = ''
                    env.EB_CNAME = ''

                    if (state) {
                        def parts = state.tokenize()

                        if (parts.size() > 0 && parts[0] != 'None') {
                            env.PREVIOUS_VERSION = parts[0]
                        }

                        if (parts.size() > 1 && parts[1] != 'None') {
                            env.EB_CNAME = parts[1]
                        }
                    }

                    if (!env.PUBLIC_BASE_URL && env.EB_CNAME) {
                        env.PUBLIC_BASE_URL = "http://${env.EB_CNAME}"
                    }

                    if (!env.PUBLIC_BASE_URL) {
                        echo 'WARNING: PUBLIC_BASE_URL is empty.'
                    }

                    env.DEPLOY_STARTED = 'false'

                    currentBuild.description =
                        "${env.IMAGE_TAG} -> ${env.EB_ENVIRONMENT}"

                    echo "Commit: ${env.GIT_COMMIT}"
                    echo "Branch: ${env.BUILD_BRANCH}"
                    echo "Deploy branch: ${env.IS_DEPLOY_BRANCH}"
                    echo "ECR: ${env.ECR_REGISTRY}"
                    echo "EB: ${env.EB_APPLICATION}/${env.EB_ENVIRONMENT}"
                    echo "Previous version: ${env.PREVIOUS_VERSION}"
                }
            }
        }

        stage('Build & Test') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'gymio-db',
                        usernameVariable: 'DB_USERNAME',
                        passwordVariable: 'DB_PASSWORD'
                    ),
                    string(
                        credentialsId: 'gymio-jwt-secret',
                        variable: 'JWT_SECRET'
                    ),
                    string(
                        credentialsId: 'gymio-internal-auth-secret',
                        variable: 'INTERNAL_AUTH_SECRET'
                    ),
                    string(
                        credentialsId: 'gymio-jwt-expiration',
                        variable: 'JWT_EXPIRATION_MS'
                    ),
                    usernamePassword(
                        credentialsId: 'gymio-mail',
                        usernameVariable: 'MAIL_USERNAME',
                        passwordVariable: 'MAIL_PASSWORD'
                    )
                ]) {
                    withEnv([
                        "DB_URL=${env.DB_TEST_URL}",
                        "SPRING_DATASOURCE_URL=${env.DB_TEST_URL}"
                    ]) {
                        sh """
                            set -e

                            if [ -d "${env.JAVA_HOME_DIR}" ]; then
                                export JAVA_HOME="${env.JAVA_HOME_DIR}"
                                export PATH="\$JAVA_HOME/bin:\$PATH"
                            fi

                            java -version

                            chmod +x mvnw

                            if ./mvnw -v >/dev/null 2>&1; then
                                MVN=./mvnw
                            else
                                MVN=mvn
                            fi

                            echo "Using \$MVN"

                            \$MVN -B -ntp clean verify ${
                                params.SKIP_TESTS ? '-DskipTests' : ''
                            }
                        """
                    }
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                sh '''
                    set -e

                    for pair in $SERVICES; do
                        dir=${pair%%=*}
                        repo=${pair#*=}

                        echo "Building microservices/$dir"

                        docker build \
                            -t "$ECR_REGISTRY/$repo:$IMAGE_TAG" \
                            "microservices/$dir"
                    done
                '''
            }
        }

        stage('Push to Amazon ECR') {
            steps {
                sh '''
                    set -e

                    aws ecr get-login-password --region "$AWS_REGION" |
                        docker login \
                            --username AWS \
                            --password-stdin "$ECR_REGISTRY"

                    trap 'docker logout "$ECR_REGISTRY" >/dev/null 2>&1 || true' EXIT

                    for pair in $SERVICES; do
                        repo=${pair#*=}

                        docker tag \
                            "$ECR_REGISTRY/$repo:$IMAGE_TAG" \
                            "$ECR_REGISTRY/$repo:latest"

                        docker push "$ECR_REGISTRY/$repo:$IMAGE_TAG"
                        docker push "$ECR_REGISTRY/$repo:latest"

                        echo "Pushed $repo:$IMAGE_TAG"
                    done
                '''
            }
        }


        stage('Deploy to Elastic Beanstalk') {
            when {
                expression {
                    env.IS_DEPLOY_BRANCH == 'true'
                }
            }

            steps {
                sh '''
                    set -e

                    rm -rf build
                    mkdir -p build

                    BUNDLE="gymio-${IMAGE_TAG}.zip"
                    trap 'rm -f "$BUNDLE"' EXIT

                    # Generate Docker Compose configuration.
                    # Production secrets are retrieved by Elastic Beanstalk.
                    bash deploy/render-compose.sh build/docker-compose.yml

                    cp -r deploy/.ebextensions build/
                    cp -r .platform build/

                    (
                        cd build

                        if command -v zip >/dev/null 2>&1; then
                            zip -q -r "../$BUNDLE" \
                                docker-compose.yml .ebextensions .platform
                        else
                            jar -cMf "../$BUNDLE" \
                                docker-compose.yml .ebextensions .platform
                        fi
                    )

                    echo "Uploading deployment bundle..."

                    aws s3 cp "$BUNDLE" \
                        "s3://${EB_S3_BUCKET}/gymio/${IMAGE_TAG}.zip"

                    echo "Creating Elastic Beanstalk application version..."

                    aws elasticbeanstalk create-application-version \
                        --application-name "$EB_APPLICATION" \
                        --version-label "$IMAGE_TAG" \
                        --source-bundle \
                        "S3Bucket=${EB_S3_BUCKET},S3Key=gymio/${IMAGE_TAG}.zip" \
                        --process
                '''

                script {
                    // Enable rollback only once an update is about to be requested.
                    env.DEPLOY_STARTED = 'true'
                }

                sh '''
                    set -e

                    echo "Updating Elastic Beanstalk environment..."

                    aws elasticbeanstalk update-environment \
                        --application-name "$EB_APPLICATION" \
                        --environment-name "$EB_ENVIRONMENT" \
                        --version-label "$IMAGE_TAG"
                '''
            }
        }


        stage('Verify Deployment') {
            when {
                expression {
                    env.IS_DEPLOY_BRANCH == 'true'
                }
            }

            steps {
                sh '''
                    set -e

                    echo "Waiting for version $IMAGE_TAG on $EB_ENVIRONMENT"

                    ready=0
                    deadline=$(( $(date +%s) + 900 ))

                    while [ "$(date +%s)" -lt "$deadline" ]; do

                        state=$(aws elasticbeanstalk describe-environments \
                            --application-name "$EB_APPLICATION" \
                            --environment-names "$EB_ENVIRONMENT" \
                            --query 'Environments[0].[VersionLabel,Status]' \
                            --output text) || state=""

                        set -- $state

                        echo "Version=${1:-?} Status=${2:-?}"

                        if [ "${1:-}" = "$IMAGE_TAG" ] &&
                           [ "${2:-}" = "Ready" ]; then
                            ready=1
                            break
                        fi

                        sleep 20
                    done

                    if [ "$ready" -ne 1 ]; then
                        echo "ERROR: Timed out waiting for deployment."
                        exit 1
                    fi

                    state=$(aws elasticbeanstalk describe-environments \
                        --application-name "$EB_APPLICATION" \
                        --environment-names "$EB_ENVIRONMENT" \
                        --query 'Environments[0].[Health,CNAME]' \
                        --output text)

                    set -- $state

                    health="${1:-}"
                    cname="${2:-}"

                    echo "Elastic Beanstalk health=$health"
                    echo "Environment URL=http://$cname"

                    if [ "$health" = "Red" ]; then
                        echo "ERROR: Elastic Beanstalk reports Red health."
                        exit 1
                    fi

                    echo "Checking application health endpoint..."

                    ok=0
                    attempt=1
                    max=60

                    while [ "$attempt" -le "$max" ]; do

                        code=$(curl -s \
                            -o /tmp/gymio-health \
                            -w '%{http_code}' \
                            --max-time 5 \
                            "http://$cname/actuator/health" || true)

                        echo "Attempt $attempt/$max -> HTTP $code"

                        if [ "$code" = "200" ] &&
                           grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"' \
                               /tmp/gymio-health 2>/dev/null; then
                            ok=1
                            break
                        fi

                        attempt=$((attempt + 1))
                        sleep 5
                    done

                    rm -f /tmp/gymio-health

                    if [ "$ok" -ne 1 ]; then
                        echo "ERROR: Application health check failed."
                        exit 1
                    fi

                    echo "Health check passed."
                '''
            }
        }
    }

    post {
        success {
            script {
                echo '============================================================'
                echo ' GYMIO PIPELINE SUCCEEDED'
                echo " Commit: ${env.GIT_COMMIT}"
                echo " Image: ${env.IMAGE_TAG}"
                echo " ECR: ${env.ECR_REGISTRY}"
                echo " EB: ${env.EB_APPLICATION}/${env.EB_ENVIRONMENT}"
                echo '============================================================'

                if (env.NOTIFY_EMAIL) {
                    emailext(
                        subject: "[Gymio] Deployment ${env.IMAGE_TAG} succeeded",
                        body: "Commit ${env.GIT_COMMIT} was deployed to ${env.EB_ENVIRONMENT}.",
                        to: env.NOTIFY_EMAIL
                    )
                }
            }
        }

        failure {
            script {
                echo '============================================================'
                echo ' GYMIO PIPELINE FAILED'
                echo " Commit: ${env.GIT_COMMIT}"
                echo " Branch: ${env.BUILD_BRANCH}"
                echo '============================================================'

                if (env.NOTIFY_EMAIL) {
                    emailext(
                        subject: "[Gymio] Pipeline failed (${env.IMAGE_TAG})",
                        body: "Build ${env.BUILD_NUMBER} failed. See ${env.BUILD_URL}",
                        to: env.NOTIFY_EMAIL
                    )
                }

                if (params.ROLLBACK_ON_FAILURE &&
                    env.DEPLOY_STARTED == 'true' &&
                    env.PREVIOUS_VERSION) {

                    echo "Rolling back to ${env.PREVIOUS_VERSION}"

                    sh '''
                        aws elasticbeanstalk update-environment \
                            --application-name "$EB_APPLICATION" \
                            --environment-name "$EB_ENVIRONMENT" \
                            --version-label "$PREVIOUS_VERSION"
                    '''
                } else {
                    echo 'Rollback skipped: no deployment rollback is required or no previous version exists.'
                }
            }
        }

        always {
            junit(
                allowEmptyResults: true,
                testResults: '**/target/surefire-reports/*.xml'
            )
        }
    }
}
