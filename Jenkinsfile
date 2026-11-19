pipeline {
    agent any

    environment {
        JAVA_HOME = '/usr/lib/jvm/java-17-openjdk-amd64'
        PATH = "${JAVA_HOME}/bin:${env.PATH}"

        DB_URL = 'jdbc:postgresql://172.31.39.199:5432/gymio_db'
        DB_TEST_URL = 'jdbc:postgresql://172.31.39.199:5432/gymio_test'
        KAFKA_BOOTSTRAP_SERVERS = '172.31.39.10:9092'
    }

    stages {
        stage('Build and Test') {
            steps {
                echo 'Building and testing Gymio microservices...'

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
                    usernamePassword(
                        credentialsId: 'gymio-mail',
                        usernameVariable: 'MAIL_USERNAME',
                        passwordVariable: 'MAIL_PASSWORD'
                    ),
                    string(
                        credentialsId: 'gymio-jwt-expiration',
                        variable: 'JWT_EXPIRATION_MS'
                    ),
                    string(
                        credentialsId: 'gymio-public-url',
                        variable: 'PUBLIC_BASE_URL'
                    )
                ]) {
                    sh 'chmod +x mvnw'

                    // Run tests against the isolated test database.
                    withEnv(["DB_URL=${DB_TEST_URL}"]) {
                        sh './mvnw clean verify'
                    }
                }
            }
        }
    }
}
