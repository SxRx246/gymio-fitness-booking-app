pipeline {
    agent any

    stages {
        stage('Build and Test') {
            steps {
                echo 'Building and testing Gymio microservices...'
                sh 'chmod +x mvnw'
                sh './mvnw clean verify'
            }
        }
    }
}
