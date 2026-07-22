pipeline {
  agent any

  options {
    disableConcurrentBuilds()
    skipDefaultCheckout(true)
    timestamps()
  }

  environment {
    REGISTRY = 'docker.io'
    IMAGE_NAME = 'tennguoi2/kendy-backend'

    DOCKERHUB_CREDENTIALS = 'dockerhub-push-credentials'

    APP_DIR_LINUX = '/opt/Kendy-deploy'
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
              trivy image \
                --cache-dir "$TRIVY_CACHE_DIR" \
                --exit-code 1 \
                --severity HIGH,CRITICAL \
                --timeout 20m \
                --scanners vuln \
                "$BACKEND_IMAGE"
            '''
          } else {
            bat '''
              @echo off

              trivy image ^
                --cache-dir "%TRIVY_CACHE_DIR%" ^
                --exit-code 1 ^
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

                printf '%s' "$REGISTRY_PASSWORD" |
                  docker login "$REGISTRY" \
                    --username "$REGISTRY_USER" \
                    --password-stdin

                docker push "$BACKEND_IMAGE"
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
              if [ ! -f "$APP_DIR/deploy.sh" ]; then
                echo "Không tìm thấy $APP_DIR/deploy.sh"
                exit 1
              fi

              BACKEND_IMAGE="$BACKEND_IMAGE" \
              APP_DIR="$APP_DIR" \
              sh "$APP_DIR/deploy.sh"
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