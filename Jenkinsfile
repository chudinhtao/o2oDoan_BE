pipeline {
    agent any

    environment {
        DOCKER_CREDS = credentials('dockerhub-credentials')
        DOCKERHUB_USERNAME = "${DOCKER_CREDS_USR}"

        SSH_CREDENTIAL_ID = 'ssh-server-key'
        REMOTE_USER = 'cdt'
        REMOTE_HOST = '192.168.0.107'
        REMOTE_DIR  = '/home/cdt/fnb-project/backend'
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

        // ✅ Test SSH
        stage('Test SSH') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'ssh-server-key', keyFileVariable: 'SSH_KEY')]) {
                    sh """
                        ssh -i $SSH_KEY -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST} 'echo OK'
                    """
                }
            }
        }

        stage('Deploy to Server') {
            steps {
                script {
                    withCredentials([sshUserPrivateKey(credentialsId: 'ssh-server-key', keyFileVariable: 'SSH_KEY')]) {
                        sh """
                            ssh -i $SSH_KEY -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST} "
                                mkdir -p ${REMOTE_DIR}
                            "
                        """

                        sh """
                            scp -i $SSH_KEY -o StrictHostKeyChecking=no backend/docker-compose.prod.yml ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_DIR}/
                        """

                        sh """
                            scp -i $SSH_KEY -o StrictHostKeyChecking=no backend/.env ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_DIR}/
                        """

                        sh """
                            ssh -i $SSH_KEY -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST} "
                                cd ${REMOTE_DIR} &&

                                echo '📥 Pull images...'
                                docker compose -f docker-compose.prod.yml pull &&

                                echo '🚀 Restart services...'
                                docker compose -f docker-compose.prod.yml up -d
                            "
                        """
                    }
                }
            }
        }
    }

    post {
        always {
            sh 'docker logout || true'
        }
        success {
            echo "✅ Build + Deploy thành công!"
        }
        failure {
            echo "❌ Pipeline thất bại!"
        }
    }
}