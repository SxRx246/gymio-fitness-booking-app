pipeline {
    agent any

    environment {
        DB_URL = 'jdbc:postgresql://gymio-postgres:5432/gymio'
        DB_TEST_URL = 'jdbc:postgresql://gymio-postgres:5432/gymio_test'
        KAFKA_BOOTSTRAP_SERVERS = 'kafka:29092'
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
                    sh './mvnw clean verify'
                }
            }
        }
    }
}
