#!/usr/bin/env groovy

/**
 * Gymio - CI/CD pipeline
 *
 *   GitHub (poll SCM)
 *     -> build & test all five Maven modules
 *     -> build one Docker image per service
 *     -> push to Amazon ECR, tagged with the Git commit SHA
 *     -> deploy to AWS Elastic Beanstalk (multi-container Docker)
 *     -> verify the deployment with an HTTP health check
 *     -> roll back to the previous version on failure
 *
 * AWS authentication uses the IAM role attached to the Jenkins agent
 * (EC2 instance profile or ECS task role). No AWS access keys are stored
 * in Jenkins or in this file.
 *
 * Every secret is read from the Jenkins credentials store through
 * withCredentials, which also masks it in the build log.
 */

// "<service-directory>=<ecr-repository>" for each deployable service.
// Edit the right-hand side if your ECR repository names differ.
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
        buildDiscarder(logRotator(numToKeepStr: '25', daysToKeepStr: '30'))
        timeout(time: 60, unit: 'MINUTES')
    }

    // Poll GitHub for new commits every 2 minutes. Set up a webhook as well if
    // you want instant feedback - see deploy/README.md.
    triggers {
        pollSCM('H/2 * * * *')
    }

    parameters {
        booleanParam(
            name: 'SKIP_TESTS',
            defaultValue: false,
            description: 'Skip the Maven test phase (build only). Not recommended.'
        )
        booleanParam(
            name: 'ROLLBACK_ON_FAILURE',
            defaultValue: true,
            description: 'Re-deploy the previous Elastic Beanstalk version if the health check fails.'
        )
    }

    environment {
        // ---- AWS target -------------------------------------------------
        AWS_REGION     = 'eu-north-1'
        EB_APPLICATION = 'gymio'
        EB_ENVIRONMENT = 'gymio-env'
        // Defaults to elasticbeanstalk-<region>-<account-id> when left empty.
        EB_S3_BUCKET   = ''
        // Only this branch is deployed to Elastic Beanstalk.
        DEPLOY_BRANCH  = 'main'

        // ---- Jenkins agent ----------------------------------------------
        JAVA_HOME_DIR  = '/opt/java/openjdk'

        // ---- Datastores --------------------------------------------------
        // The EB instances must be able to reach both addresses.
        DB_URL                  = 'jdbc:postgresql://172.31.39.199:5432/gymio_db'
        DB_TEST_URL             = 'jdbc:postgresql://172.31.39.199:5432/gymio_test'
        KAFKA_BOOTSTRAP_SERVERS = '172.31.39.10:9092'

        // ---- Optional ----------------------------------------------------
        // Public base URL used in verification/reset e-mails. Defaults to the
        // Elastic Beanstalk CNAME, resolved at build time.
        PUBLIC_BASE_URL = ''
        NOTIFY_EMAIL    = ''   // set to enable e-mail notifications
    }

    stages {

        // ================================================================
        stage('Initialize') {
            steps {
                script {
                    sh '''
                        set -e
                        export PATH="/var/jenkins_home/.local/bin:$PATH"
                        export PATH="/var/jenkins_home/.local/bin:$PATH"
                        export PATH="$HOME/.local/bin:$PATH"
                        echo "Java   : $(java -version 2>&1 | head -n 1)"
                        docker --version
                        if ! command -v aws >/dev/null 2>&1; then
                            echo "==> Installing AWS CLI v2 into $HOME/.local ..."
                            mkdir -p "$HOME/.local/aws-cli" "$HOME/.local/bin"
                            curl -sSL "https://amazonaws.com(uname -m).zip" -o /tmp/awscliv2.zip
                            (cd /tmp && jar -xf /tmp/awscliv2.zip)
                            chmod +x /tmp/aws/install /tmp/aws/dist/aws /tmp/aws/dist/aws_completer 2>/dev/null || true
                            /tmp/aws/install -i "$HOME/.local/aws-cli" -b "$HOME/.local/bin" --update
                            rm -rf /tmp/aws /tmp/awscliv2.zip
                        fi
                        aws --version
                    '''

                    if (!env.GIT_COMMIT) {
                        error('GIT_COMMIT is not set - is this job configured to check out from GitHub?')
                    }

                    env.SERVICES    = SERVICE_LIST.join(' ')
                    env.IMAGE_TAG   = env.GIT_COMMIT
                    // GIT_BRANCH may arrive as "origin/main".
                    def rawBranch = env.GIT_BRANCH ?: sh(returnStdout: true, script: "git name-rev --name-only HEAD").trim()
                    env.BUILD_BRANCH = rawBranch.replaceFirst(/^origin\//, '').replaceFirst(/^remotes\/origin\//, '')
                    env.IS_DEPLOY_BRANCH = (env.BUILD_BRANCH == env.DEPLOY_BRANCH).toString()

                    env.AWS_ACCOUNT_ID = sh(
                        returnStdout: true,
                        script: 'aws sts get-caller-identity --query Account --output text'
                    ).trim()
                    env.ECR_REGISTRY = "${env.AWS_ACCOUNT_ID}.dkr.ecr.${env.AWS_REGION}.amazonaws.com"

                    if (!env.EB_S3_BUCKET) {
                        env.EB_S3_BUCKET = "elasticbeanstalk-${env.AWS_REGION}-${env.AWS_ACCOUNT_ID}"
                    }
                    sh "aws s3api head-bucket --bucket ${env.EB_S3_BUCKET} >/dev/null"

                    // Previous version (for rollback) and CNAME (for e-mail links).
                    def state = sh(
                        returnStdout: true,
                        script: "aws elasticbeanstalk describe-environments " +
                                "--application-name ${env.EB_APPLICATION} " +
                                "--environment-names ${env.EB_ENVIRONMENT} " +
                                "--query 'Environments[0].[VersionLabel,CNAME]' --output text 2>/dev/null || true"
                    ).trim()

                    env.PREVIOUS_VERSION = ''
                    env.EB_CNAME         = ''
                    if (state) {
                        def parts = state.split('\t')
                        if (parts.length > 0 && parts[0] && parts[0] != 'None') {
                            env.PREVIOUS_VERSION = parts[0]
                        }
                        if (parts.length > 1) {
                            env.EB_CNAME = parts[1] == 'None' ? '' : parts[1]
                        }
                    }

                    if (!env.PUBLIC_BASE_URL && env.EB_CNAME) {
                        env.PUBLIC_BASE_URL = "http://${env.EB_CNAME}"
                    }
                    if (!env.PUBLIC_BASE_URL) {
                        echo 'WARNING: PUBLIC_BASE_URL is empty - e-mails will link to localhost:8080'
                    }

                    currentBuild.description = "${env.IMAGE_TAG} -> ${env.EB_ENVIRONMENT}"
                    echo "commit=${env.IMAGE_TAG} branch=${env.BUILD_BRANCH} deploy=${env.IS_DEPLOY_BRANCH}"
                    echo "ecr=${env.ECR_REGISTRY}"
                    echo "eb=${env.EB_APPLICATION}/${env.EB_ENVIRONMENT} previous=${env.PREVIOUS_VERSION}"
                }
            }
        }

        // ================================================================
        stage('Build & Test') {
            steps {
                withCredentials([
                    usernamePassword(credentialsId: 'gymio-db',
                                     usernameVariable: 'DB_USERNAME',
                                     passwordVariable: 'DB_PASSWORD'),
                    string(credentialsId: 'gymio-jwt-secret',           variable: 'JWT_SECRET'),
                    string(credentialsId: 'gymio-internal-auth-secret', variable: 'INTERNAL_AUTH_SECRET'),
                    string(credentialsId: 'gymio-jwt-expiration',     variable: 'JWT_EXPIRATION_MS'),
                    usernamePassword(credentialsId: 'gymio-mail',
                                     usernameVariable: 'MAIL_USERNAME',
                                     passwordVariable: 'MAIL_PASSWORD')
                ]) {
                    withEnv(["DB_URL=${env.DB_TEST_URL}", "SPRING_DATASOURCE_URL=${env.DB_TEST_URL}"]) {
                        sh """
                            set -e
                        export PATH="/var/jenkins_home/.local/bin:$PATH"
                        export PATH="/var/jenkins_home/.local/bin:$PATH"
                            if [ -d "${env.JAVA_HOME_DIR}" ]; then export JAVA_HOME=${env.JAVA_HOME_DIR}; export PATH="\$JAVA_HOME/bin:\$PATH"; fi
                            java -version

                            chmod +x mvnw
                            if ./mvnw -v >/dev/null 2>&1; then MVN=./mvnw; else MVN=mvn; fi
                            echo "Using \$MVN"

                            \$MVN -B -ntp clean verify ${params.SKIP_TESTS ? '-DskipTests' : ''}
                        """
                    }
                }
            }
        }

        // ================================================================
        stage('Build Docker Images') {
            steps {
                sh '''
                    set -e
                        export PATH="/var/jenkins_home/.local/bin:$PATH"
                        export PATH="/var/jenkins_home/.local/bin:$PATH"
                    for pair in $SERVICES; do
                        dir=${pair%%=*}
                        repo=${pair#*=}
                        echo "==> docker build microservices/$dir -> $ECR_REGISTRY/$repo:$IMAGE_TAG"
                        docker build -t "$ECR_REGISTRY/$repo:$IMAGE_TAG" "microservices/$dir"
                    done
                '''
            }
        }

        // ================================================================
        stage('Push to Amazon ECR') {
            steps {
                sh '''
                    set -e
                        export PATH="/var/jenkins_home/.local/bin:$PATH"
                        export PATH="/var/jenkins_home/.local/bin:$PATH"
                    aws ecr get-login-password --region "$AWS_REGION" \
                        | docker login --username AWS --password-stdin "$ECR_REGISTRY"
                    trap 'docker logout "$ECR_REGISTRY" >/dev/null 2>&1 || true' EXIT

                    for pair in $SERVICES; do
                        repo=${pair#*=}
                        docker tag "$ECR_REGISTRY/$repo:$IMAGE_TAG" "$ECR_REGISTRY/$repo:latest"
                        docker push "$ECR_REGISTRY/$repo:$IMAGE_TAG"
                        docker push "$ECR_REGISTRY/$repo:latest"
                        echo "==> pushed $repo:$IMAGE_TAG"
                    done
                '''
            }
        }

        // ================================================================
        stage('Deploy to Elastic Beanstalk') {
            when { expression { env.IS_DEPLOY_BRANCH == 'true' } }
            steps {
                withCredentials([
                    usernamePassword(credentialsId: 'gymio-db',
                                     usernameVariable: 'DB_USERNAME',
                                     passwordVariable: 'DB_PASSWORD'),
                    string(credentialsId: 'gymio-jwt-secret',           variable: 'JWT_SECRET'),
                    string(credentialsId: 'gymio-internal-auth-secret', variable: 'INTERNAL_AUTH_SECRET'),
                    string(credentialsId: 'gymio-jwt-expiration',     variable: 'JWT_EXPIRATION_MS'),
                    usernamePassword(credentialsId: 'gymio-mail',
                                     usernameVariable: 'MAIL_USERNAME',
                                     passwordVariable: 'MAIL_PASSWORD')
                ]) {
                    sh """
                        set -e
                        export PATH="/var/jenkins_home/.local/bin:$PATH"
                        export PATH="/var/jenkins_home/.local/bin:$PATH"

                        rm -rf build
                        mkdir -p build

                        # Dockerrun.aws.json + .ebextensions form the EB source bundle.
                        bash deploy/render-compose.sh build/docker-compose.yml
                        cp -r deploy/.ebextensions build/
                        ( cd build && if command -v zip >/dev/null 2>&1; then zip -q -r "../gymio-${env.IMAGE_TAG}.zip" docker-compose.yml .ebextensions; else jar -cMf "../gymio-${env.IMAGE_TAG}.zip" docker-compose.yml .ebextensions; fi )

                        echo "==> Uploading bundle to s3://${env.EB_S3_BUCKET}/gymio/${env.IMAGE_TAG}.zip"
                        aws s3 cp "gymio-${env.IMAGE_TAG}.zip" "s3://${env.EB_S3_BUCKET}/gymio/${env.IMAGE_TAG}.zip"

                        # Re-deploying the same commit must be possible.
                        aws elasticbeanstalk delete-application-version \\
                            --application-name "${env.EB_APPLICATION}" \\
                            --version-label "${env.IMAGE_TAG}" \\
                            --delete-source-from-s3 false >/dev/null 2>&1 || true

                        echo "==> Creating application version ${env.IMAGE_TAG}"
                        aws elasticbeanstalk create-application-version \\
                            --application-name "${env.EB_APPLICATION}" \\
                            --version-label "${env.IMAGE_TAG}" \\
                            --source-bundle "S3Bucket=${env.EB_S3_BUCKET},S3Key=gymio/${env.IMAGE_TAG}.zip" \\
                            --process

                        echo "==> Updating environment ${env.EB_ENVIRONMENT}"
                        aws elasticbeanstalk update-environment \\
                            --application-name "${env.EB_APPLICATION}" \\
                            --environment-name "${env.EB_ENVIRONMENT}" \\
                            --version-label "${env.IMAGE_TAG}"
                    """
                    script { env.DEPLOY_STARTED = 'true' }
                }
            }
        }

        // ================================================================
        stage('Verify Deployment') {
            when { expression { env.IS_DEPLOY_BRANCH == 'true' } }
            steps {
                sh '''
                    set -e
                        export PATH="/var/jenkins_home/.local/bin:$PATH"
                        export PATH="/var/jenkins_home/.local/bin:$PATH"

                    echo "==> Waiting for version $IMAGE_TAG to become live on $EB_ENVIRONMENT"
                    ready=0
                    deadline=$(( $(date +%s) + 900 ))

                    while [ "$(date +%s)" -lt "$deadline" ]; do
                        state=$(aws elasticbeanstalk describe-environments \\
                            --application-name "$EB_APPLICATION" \\
                            --environment-names "$EB_ENVIRONMENT" \\
                            --query 'Environments[0].[VersionLabel,Status]' \\
                            --output text) || state=""
                        set -- $state
                        echo "  version=${1:-?} status=${2:-?}"
                        if [ "${1:-}" = "$IMAGE_TAG" ] && [ "${2:-}" = "Ready" ]; then
                            ready=1
                            break
                        fi
                        sleep 20
                    done

                    if [ "$ready" -ne 1 ]; then
                        echo "ERROR: timed out waiting for $IMAGE_TAG to become live"
                        exit 1
                    fi

                    state=$(aws elasticbeanstalk describe-environments \\
                        --application-name "$EB_APPLICATION" \\
                        --environment-names "$EB_ENVIRONMENT" \\
                        --query 'Environments[0].[Health,CNAME]' --output text)
                    set -- $state
                    health="${1:-}"; cname="${2:-}"
                    echo "Elastic Beanstalk health=$health url=http://$cname"

                    if [ "$health" = "Red" ]; then
                        echo "ERROR: Elastic Beanstalk reports Red health"
                        exit 1
                    fi

                    echo "==> Polling http://$cname/actuator/health"
                    ok=0; attempt=1; max=60
                    while [ "$attempt" -le "$max" ]; do
                        code=$(curl -s -o /tmp/gymio-health -w '%{http_code}' --max-time 5 \\
                            "http://$cname/actuator/health" || true)
                        echo "  attempt $attempt/$max -> HTTP $code"
                        if [ "$code" = "200" ] && grep -q '"status":"UP"' /tmp/gymio-health 2>/dev/null; then
                            ok=1
                            break
                        fi
                        attempt=$((attempt + 1))
                        sleep 5
                    done
                    rm -f /tmp/gymio-health

                    if [ "$ok" -ne 1 ]; then
                        echo "ERROR: /actuator/health did not return {\"status\":\"UP\"}"
                        exit 1
                    fi

                    echo "Health check passed: {\"status\":\"UP\"}"
                '''
            }
        }
    }

    // ====================================================================
    post {
        success {
            script {
                echo '============================================================'
                echo ' PIPELINE SUCCEEDED'
                echo "   commit    : ${env.GIT_COMMIT}"
                echo "   image tag : ${env.IMAGE_TAG}"
                echo "   ecr       : ${env.ECR_REGISTRY}"
                echo "   eb env    : ${env.EB_APPLICATION}/${env.EB_ENVIRONMENT}"
                echo '============================================================'
                if (env.NOTIFY_EMAIL) {
                    emailext(
                        subject: "[Gymio] deploy ${env.IMAGE_TAG} succeeded",
                        body: "Commit ${env.GIT_COMMIT} was deployed to ${env.EB_ENVIRONMENT}.",
                        to: env.NOTIFY_EMAIL
                    )
                }
            }
        }

        failure {
            script {
                echo '============================================================'
                echo ' PIPELINE FAILED'
                echo "   commit    : ${env.GIT_COMMIT}"
                echo "   branch    : ${env.BUILD_BRANCH}"
                echo '============================================================'
                if (env.NOTIFY_EMAIL) {
                    emailext(
                        subject: "[Gymio] pipeline failed (${env.IMAGE_TAG})",
                        body: "Build ${env.BUILD_NUMBER} failed. See ${env.BUILD_URL}",
                        to: env.NOTIFY_EMAIL
                    )
                }

                if (params.ROLLBACK_ON_FAILURE &&
                    env.DEPLOY_STARTED == 'true' &&
                    env.PREVIOUS_VERSION) {
                    echo "Rolling ${env.EB_ENVIRONMENT} back to ${env.PREVIOUS_VERSION}"
                    sh """
                        aws elasticbeanstalk update-environment \\
                            --application-name "${env.EB_APPLICATION}" \\
                            --environment-name "${env.EB_ENVIRONMENT}" \\
                            --version-label "${env.PREVIOUS_VERSION}"
                    """
                }
            }
        }

        always {
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
            archiveArtifacts artifacts: 'gymio-*.zip', allowEmptyArchive: true
        }
    }
}