pipeline {
    // Chạy trên bất kỳ máy Jenkins (Node) nào có cài Docker
    agent any

    environment {
        // Tên tài khoản Docker Hub mặc định (Thay thế nếu cần)
        // Lưu ý: Bạn cần tạo Credentials trong Jenkins loại "Username with password" có ID là 'dockerhub-credentials'
        DOCKER_CREDS = credentials('dockerhub-credentials')
        DOCKERHUB_USERNAME = "${DOCKER_CREDS_USR}"
    }

    stages {
        stage('Checkout Code') {
            steps {
                // Kéo code mới nhất từ Git về
                checkout scm
            }
        }

        stage('Docker Login') {
            steps {
                // Đăng nhập vào Docker Hub để chuẩn bị đẩy Image
                // Lưu ý: Nếu Jenkins cài trên Windows, hãy đổi 'sh' thành 'bat'
                sh 'echo $DOCKER_CREDS_PSW | docker login -u $DOCKER_CREDS_USR --password-stdin'
            }
        }

        stage('Build & Push Microservices') {
            steps {
                // Chui vào thư mục backend
                dir('backend') {
                    script {
                        // Danh sách 9 services
                        def services = ["eureka-server", "api-gateway", "auth-service", "menu-service", "order-service", "kds-service", "report-service", "notification-service", "ai-service"]
                        
                        for (int i = 0; i < services.size(); ++i) {
                            def service = services[i]
                            
                            echo "=================================================="
                            echo "🚀 BUILDING AND PUSHING: ${service}"
                            echo "=================================================="
                            
                            def imageName = "${DOCKERHUB_USERNAME}/fnb-${service}:latest"
                            
                            // Build Image (Đổi 'sh' thành 'bat' nếu dùng Windows)
                            sh "docker build -f ./${service}/Dockerfile -t ${imageName} ."
                            
                            // Đẩy lên Docker Hub
                            sh "docker push ${imageName}"
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            // Luôn luôn đăng xuất Docker sau khi chạy xong để bảo mật máy chủ CI
            echo "🧹 Dọn dẹp phiên làm việc Docker..."
            sh 'docker logout'
        }
        success {
            echo "✅ Toàn bộ Backend đã được Build và Push thành công lên mây!"
        }
        failure {
            echo "❌ Quá trình CI/CD thất bại. Hãy kiểm tra lại log."
        }
    }
}
