pipeline {
    agent any

    environment {
        DOCKER_CREDS = credentials('dockerhub-credentials')
        DOCKERHUB_USERNAME = "${DOCKER_CREDS_USR}"

        SSH_CREDENTIAL_ID = 'ssh-server-key'
        REMOTE_USER = 'cdt'
        REMOTE_HOST = '192.168.0.105'
        REMOTE_DIR  = '/home/cdt/your-repo' // thư mục chứa docker-compose.yml trên server
    }

    stages {
        stage('Checkout Code') {
            steps {
                checkout scm
            }
        }

        stage('Docker Login') {
            steps {
                sh 'echo $DOCKER_CREDS_PSW | docker login -u $DOCKER_CREDS_USR --password-stdin'
            }
        }

        stage('Build & Push Microservices') {
            steps {
                script {
                    def services = [
                        "eureka-server",
                        "api-gateway",
                        "auth-service",
                        "menu-service",
                        "order-service",
                        "kds-service",
                        "report-service",
                        "notification-service",
                        "ai-service"
                    ]

                    for (service in services) {

                        def imageName = "${DOCKERHUB_USERNAME}/fnb-${service}:latest"

                        echo "🚀 BUILDING: ${service}"

                        sh "docker build -f ./${service}/Dockerfile -t ${imageName} ."
                        sh "docker push ${imageName}"
                    }
                }
            }
        }

        stage('Deploy to Server') {
            steps {
                sshagent([env.SSH_CREDENTIAL_ID]) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ${env.REMOTE_USER}@${env.REMOTE_HOST} '
                            cd ${env.REMOTE_DIR} &&
                            git pull &&
                            docker-compose pull &&
                            docker-compose up -d
                        '
                    """
                }
            }
        }
    }

    post {
        always {
            sh 'docker logout || true'
            echo "🧹 Cleaned Docker session"
        }
        success {
            echo "✅ Build + Deploy thành công!"
        }
        failure {
            echo "❌ Pipeline thất bại!"
        }
    }
}