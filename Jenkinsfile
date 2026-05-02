pipeline {
    agent any

    environment {
        DOCKER_CREDS = credentials('dockerhub-credentials')
        DOCKERHUB_USERNAME = "${DOCKER_CREDS_USR}"

        SSH_CREDENTIAL_ID = 'ssh-server-key'
        REMOTE_USER = 'cdt'
        REMOTE_HOST = '192.168.0.107'
        REMOTE_DIR  = '/home/cdt/fnb-project/backend' // thư mục chứa docker-compose.yml trên server
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
                    // 1. Tạo thư mục từ xa nếu chưa có
                    sh "ssh -o StrictHostKeyChecking=no ${env.REMOTE_USER}@${env.REMOTE_HOST} 'mkdir -p ${env.REMOTE_DIR}'"
                    
                    // 2. Đẩy file docker-compose.prod.yml và .env lên server
                    // Lưu ý: file docker-compose.prod.yml nằm trong thư mục backend
                    sh "scp -o StrictHostKeyChecking=no backend/docker-compose.prod.yml ${env.REMOTE_USER}@${env.REMOTE_HOST}:${env.REMOTE_DIR}/"
                    sh "scp -o StrictHostKeyChecking=no backend/.env ${env.REMOTE_USER}@${env.REMOTE_HOST}:${env.REMOTE_DIR}/"

                    // 3. Thực hiện pull và up
                    sh """
                        ssh -o StrictHostKeyChecking=no ${env.REMOTE_USER}@${env.REMOTE_HOST} "
                            cd ${env.REMOTE_DIR} &&
                            docker-compose -f docker-compose.prod.yml pull &&
                            docker-compose -f docker-compose.prod.yml up -d
                        "
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