pipeline {
  agent any

  environment {
    REGISTRY             = 'docker.io'
    IMAGE_NAME           = 'tennguoi2/kendy-backend'
    IMAGE_TAG            = "dev-${env.BUILD_NUMBER}"
    DOCKERHUB_CREDENTIALS = 'dockerhub-credentials'
    // Đường dẫn thư mục deploy trên Máy Jenkins/Deploy
    APP_DIR_LINUX        = '/Kendy-deploy'
    APP_DIR_WIN          = 'C:/Kendy-deploy'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
        script {
          env.BACKEND_IMAGE = "${env.REGISTRY}/${env.IMAGE_NAME}:${env.IMAGE_TAG}"
          // Tự detect OS, set APP_DIR phù hợp
          env.APP_DIR = isUnix() ? env.APP_DIR_LINUX : env.APP_DIR_WIN
          echo "Đang chạy trên: ${isUnix() ? 'Linux' : 'Windows'} | APP_DIR = ${env.APP_DIR}"
        }
      }
    }

    stage('Test') {
      steps {
        script {
          if (isUnix()) {
            sh './mvnw test'
          } else {
            bat 'mvnw.cmd test'
          }
        }
      }
    }

    stage('Build Image') {
      steps {
        script {
          if (isUnix()) {
            sh 'docker build -t "$BACKEND_IMAGE" .'
          } else {
            bat "docker build -t %BACKEND_IMAGE% ."
          }
        }
      }
    }

    stage('Push Image') {
      steps {
        withCredentials([usernamePassword(credentialsId: env.DOCKERHUB_CREDENTIALS, usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_PASSWORD')]) {
          script {
            if (isUnix()) {
              sh 'echo "$REGISTRY_PASSWORD" | docker login "$REGISTRY" -u "$REGISTRY_USER" --password-stdin'
              sh 'docker push "$BACKEND_IMAGE"'
            } else {
              bat "echo %REGISTRY_PASSWORD% | docker login %REGISTRY% -u %REGISTRY_USER% --password-stdin"
              bat "docker push %BACKEND_IMAGE%"
            }
          }
        }
      }
    }

    stage('Deploy') {
      steps {
        script {
          if (isUnix()) {
            sh """
              BACKEND_IMAGE="\${BACKEND_IMAGE}" APP_DIR="\${APP_DIR}" "\${APP_DIR}/deploy.sh"
            """
          } else {
            // Windows: gọi PowerShell script deploy
            bat """
              set BACKEND_IMAGE=%BACKEND_IMAGE%
              set APP_DIR=%APP_DIR%
              powershell -ExecutionPolicy Bypass -File "%APP_DIR%\\deploy.ps1"
            """
          }
        }
      }
    }
  }

  post {
    always {
      script {
        if (isUnix()) {
          sh 'docker logout || true'
        } else {
          bat 'docker logout'
        }
      }
    }
    success { echo '✅ Pipeline hoàn thành thành công!' }
    failure { echo '❌ Pipeline thất bại. Kiểm tra Console Output để biết thêm chi tiết.' }
  }
}

