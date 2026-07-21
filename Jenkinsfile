pipeline {
  agent any

  environment {
    REGISTRY              = 'docker.io'
    IMAGE_NAME            = 'tennguoi2/kendy-backend'
    IMAGE_TAG             = "dev-${env.BUILD_NUMBER}"
    DOCKERHUB_CREDENTIALS = 'dockerhub-credentials'
    APP_DIR_LINUX         = '/Kendy-deploy'
    APP_DIR_WIN           = 'C:/Kendy-deploy'
    // Cache riêng cho Trivy
    TRIVY_CACHE_DIR       = "${WORKSPACE}/.trivy-cache"
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
        script {
          env.BACKEND_IMAGE = "${env.REGISTRY}/${env.IMAGE_NAME}:${env.IMAGE_TAG}"
          env.APP_DIR = isUnix() ? env.APP_DIR_LINUX : env.APP_DIR_WIN
          echo "Đang chạy trên: ${isUnix() ? 'Linux' : 'Windows'} | APP_DIR = ${env.APP_DIR}"
          if (isUnix()) {
            sh "mkdir -p ${env.TRIVY_CACHE_DIR}"
          } else {
            bat "if not exist ${env.TRIVY_CACHE_DIR} mkdir ${env.TRIVY_CACHE_DIR}"
          }
        }
      }
    }

    stage('Test') {
      steps {
        script {
          if (isUnix()) {
            sh './mvnw test -Dspring.profiles.active=test'
          } else {
            bat 'mvnw.cmd test -Dspring.profiles.active=test'
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

    stage('Scan Image') {
      steps {
        script {
          if (isUnix()) {
            sh """
              TRIVY_CACHE_DIR="${TRIVY_CACHE_DIR}" trivy image \
                --exit-code 1 \
                --severity HIGH,CRITICAL \
                --timeout 10m \
                --scanners vuln \
                "$BACKEND_IMAGE"
            """
          } else {
            bat """
              set TRIVY_CACHE_DIR=${TRIVY_CACHE_DIR}
              trivy image --exit-code 1 --severity HIGH,CRITICAL --timeout 10m --scanners vuln %BACKEND_IMAGE%
            """
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
