pipeline {
  agent any

  options {
    disableConcurrentBuilds()
    skipDefaultCheckout(true)
    timestamps()
  }

  environment {
    REGISTRY = 'docker.io'
    IMAGE_NAME = 'tennguoi/kendy-backend'

    DOCKERHUB_CREDENTIALS = 'docker'

    APP_DIR_LINUX = '/home/minh/kendy'
    APP_DIR_WIN = 'C:/Kendy-deploy'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm

        script {
          env.IMAGE_TAG = "dev-${env.BUILD_NUMBER}"

          env.BACKEND_IMAGE =
            "${env.REGISTRY}/${env.IMAGE_NAME}:${env.IMAGE_TAG}"

          env.APP_DIR = isUnix()
            ? env.APP_DIR_LINUX
            : env.APP_DIR_WIN

          env.TRIVY_CACHE_DIR =
            "${env.WORKSPACE}/.trivy-cache"

          echo """
Hệ điều hành : ${isUnix() ? 'Linux' : 'Windows'}
Docker image : ${env.BACKEND_IMAGE}
Deploy folder: ${env.APP_DIR}
""".stripIndent()

          if (isUnix()) {
            sh '''
              mkdir -p "$TRIVY_CACHE_DIR"
            '''
          } else {
            powershell '''
              $ErrorActionPreference = 'Stop'

              New-Item `
                -ItemType Directory `
                -Force `
                -Path $env:TRIVY_CACHE_DIR |
                Out-Null
            '''
          }
        }
      }
    }

    stage('Test') {
      steps {
        script {
          if (isUnix()) {
            sh '''
              chmod +x mvnw

              ./mvnw test \
                -Dspring.profiles.active=test
            '''
          } else {
            bat '''
              @echo off
              call mvnw.cmd test -Dspring.profiles.active=test
            '''
          }
        }
      }
    }

    stage('Build') {
      steps {
        script {
          if (isUnix()) {
            sh '''
              chmod +x mvnw

              ./mvnw package \
                -DskipTests
            '''
          } else {
            bat '''
              @echo off
              call mvnw.cmd package -DskipTests
            '''
          }
        }
      }
    }

    stage('Build Image') {
      steps {
        script {
          if (isUnix()) {
            sh '''
              docker build \
                --tag "$BACKEND_IMAGE" \
                .
            '''
          } else {
            bat '''
              @echo off
              docker build --tag "%BACKEND_IMAGE%" .
            '''
          }
        }
      }
    }

    stage('Scan Image') {
      steps {
        script {
          if (isUnix()) {
            sh '''
              if ! command -v trivy >/dev/null 2>&1; then
                echo "⚠️ Cảnh báo: Trivy chưa được cài đặt trên máy Jenkins."
                echo "ℹ️ Bỏ qua bước quét bảo mật để tiếp tục deploy."
                exit 0
              fi

              trivy image \
                --cache-dir "$TRIVY_CACHE_DIR" \
                --pkg-types os \
                --exit-code 0 \
                --severity HIGH,CRITICAL \
                --timeout 20m \
                --scanners vuln \
                "$BACKEND_IMAGE"
            '''
          } else {
            bat '''
              @echo off
              where trivy >nul 2>&1
              if errorlevel 1 (
                echo [CANH BAO] Trivy chua duoc cai dat tren may Jenkins.
                echo [INFO] Bo qua buoc quet bao mat de tiep tuc deploy.
                exit /b 0
              )

              trivy image ^
                --cache-dir "%TRIVY_CACHE_DIR%" ^
                --pkg-types os ^
                --exit-code 0 ^
                --severity HIGH,CRITICAL ^
                --timeout 20m ^
                --scanners vuln ^
                "%BACKEND_IMAGE%"
            '''
          }
        }
      }
    }

    stage('Push Image') {
      steps {
        withCredentials([
          usernamePassword(
            credentialsId: env.DOCKERHUB_CREDENTIALS,
            usernameVariable: 'REGISTRY_USER',
            passwordVariable: 'REGISTRY_PASSWORD'
          )
        ]) {
          script {
            if (isUnix()) {
              sh '''
                set -eu

                echo "Dang dang nhap Docker Hub..."
                retry_count=0
                until printf '%s' "$REGISTRY_PASSWORD" | docker login "$REGISTRY" --username "$REGISTRY_USER" --password-stdin; do
                  retry_count=$((retry_count + 1))
                  if [ "$retry_count" -ge 5 ]; then
                    echo "Dang nhap Docker Hub that bai sau 5 lan thu do timeout mang."
                    exit 1
                  fi
                  echo "Loi ket noi Docker Hub, dang thu lai sau 10 giay (lan $retry_count/5)..."
                  sleep 10
                done

                echo "Dang push image $BACKEND_IMAGE..."
                retry_count=0
                until docker push "$BACKEND_IMAGE"; do
                  retry_count=$((retry_count + 1))
                  if [ "$retry_count" -ge 5 ]; then
                    echo "Push image that bai sau 5 lan thu do timeout mang."
                    exit 1
                  fi
                  echo "Loi push image, dang thu lai sau 10 giay (lan $retry_count/5)..."
                  sleep 10
                done
              '''
            } else {
              bat '''
                @echo off

                echo Dang dang nhap Docker Hub voi user %REGISTRY_USER%...

                <nul set /p "=%REGISTRY_PASSWORD%" | docker login "%REGISTRY%" ^
                  --username "%REGISTRY_USER%" ^
                  --password-stdin

                if errorlevel 1 (
                  echo Docker Hub login that bai.
                  exit /b 1
                )

                echo Dang push image %BACKEND_IMAGE%...

                docker push "%BACKEND_IMAGE%"

                if errorlevel 1 (
                  echo Docker push that bai.
                  exit /b 1
                )
              '''
            }
          }
        }
      }
    }

    stage('Deploy') {
      steps {
        script {
          if (isUnix()) {
            sh '''
              set -eu

              if [ ! -d "$APP_DIR" ]; then
                echo "Lỗi: Không tìm thấy thư mục deploy: $APP_DIR"
                exit 1
              fi

              DEPLOY_FILE="$APP_DIR/deploy.sh"
              if [ ! -f "$DEPLOY_FILE" ]; then
                echo "Lỗi: Không tìm thấy file: $DEPLOY_FILE"
                exit 1
              fi

              # Đảm bảo quyền thực thi deploy.sh và quyền đọc cho .env
              chmod +x "$DEPLOY_FILE" 2>/dev/null || true
              if [ -f "$APP_DIR/.env" ]; then
                chmod 664 "$APP_DIR/.env" 2>/dev/null || chmod +r "$APP_DIR/.env" 2>/dev/null || true
                if [ ! -r "$APP_DIR/.env" ]; then
                  echo "Lỗi: Không có quyền đọc file $APP_DIR/.env (Permission denied)."
                  echo "Vui lòng chạy trên máy chủ: chmod 664 $APP_DIR/.env"
                  exit 1
                fi
              fi

              echo "Deploy backend image: $BACKEND_IMAGE"
              echo "Deploy folder: $APP_DIR"

              BACKEND_IMAGE="$BACKEND_IMAGE" \
              APP_DIR="$APP_DIR" \
              sh "$DEPLOY_FILE"
            '''
          } else {
            powershell '''
              $ErrorActionPreference = 'Stop'

              $deployFile =
                Join-Path $env:APP_DIR 'deploy.ps1'

              if (-not (Test-Path $deployFile)) {
                throw "Không tìm thấy file: $deployFile"
              }

              Write-Host "Deploy image: $env:BACKEND_IMAGE"
              Write-Host "Deploy folder: $env:APP_DIR"

              & $deployFile

              if ($LASTEXITCODE -ne 0) {
                throw "Deploy thất bại, exit code: $LASTEXITCODE"
              }
            '''
          }
        }
      }
    }
  }

  post {
    always {
      script {
        if (isUnix()) {
          sh(
            returnStatus: true,
            script: '''
              docker logout docker.io >/dev/null 2>&1 || true
            '''
          )
        } else {
          bat(
            returnStatus: true,
            script: '''
              @echo off
              docker logout docker.io >nul 2>&1
              exit /b 0
            '''
          )
        }
      }
    }

    success {
      echo '✅ Pipeline hoàn thành thành công!'
    }

    failure {
      echo '❌ Pipeline thất bại. Kiểm tra stage màu đỏ.'
    }
  }
}