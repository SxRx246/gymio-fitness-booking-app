pipeline {
    agent any

    environment {
        DB_URL = 'jdbc:postgresql://gymio-postgres:5432/gymio'
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
                    )
                ]) {
                    sh 'chmod +x mvnw'
                    sh './mvnw clean verify'
                }
            }
        }
    }
}
