pipeline {
  agent any

  options {
    disableConcurrentBuilds()
    skipDefaultCheckout(true)
    timestamps()
  }

  environment {
    REGISTRY = 'docker.io'

    // Docker Hub repository:
    // docker.io/tennguoi2/kendy-backend
    IMAGE_NAME = 'tennguoi2/kendy-backend'

    // ID credential trong Jenkins
    DOCKERHUB_CREDENTIALS = 'dockerhub-push-credentials'

    // Thư mục triển khai
    APP_DIR_LINUX = '/opt/kendy'
    APP_DIR_WIN   = 'C:/Kendy-deploy'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm

        script {
          // Hiện tại dùng nhánh dev
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
          Image        : ${env.BACKEND_IMAGE}
          APP_DIR      : ${env.APP_DIR}
          Trivy cache  : ${env.TRIVY_CACHE_DIR}
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
              mvnw.cmd test -Dspring.profiles.active=test
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
            powershell '''
              $ErrorActionPreference = 'Stop'

              docker build `
                --tag $env:BACKEND_IMAGE `
                .

              if ($LASTEXITCODE -ne 0) {
                throw "Docker build thất bại, exit code: $LASTEXITCODE"
              }
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
              TRIVY_CACHE_DIR="$TRIVY_CACHE_DIR" \
              trivy image \
                --exit-code 1 \
                --severity HIGH,CRITICAL \
                --timeout 20m \
                --scanners vuln \
                "$BACKEND_IMAGE"
            '''
          } else {
            powershell '''
              $ErrorActionPreference = 'Stop'

              trivy image `
                --exit-code 1 `
                --severity HIGH,CRITICAL `
                --timeout 20m `
                --scanners vuln `
                $env:BACKEND_IMAGE

              if ($LASTEXITCODE -ne 0) {
                throw "Trivy scan thất bại hoặc phát hiện lỗ hổng HIGH/CRITICAL."
              }
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
              powershell '''
                $ErrorActionPreference = 'Stop'

                $dockerUser = $env:REGISTRY_USER.Trim()
                $dockerToken = $env:REGISTRY_PASSWORD.Trim()

                if ([string]::IsNullOrWhiteSpace($dockerUser)) {
                  throw 'Docker Hub username đang bị trống.'
                }

                if ([string]::IsNullOrWhiteSpace($dockerToken)) {
                  throw 'Docker Hub token đang bị trống.'
                }

                Write-Host "Đăng nhập Docker Hub với user: $dockerUser"

                $startInfo =
                  New-Object System.Diagnostics.ProcessStartInfo

                $startInfo.FileName = 'docker.exe'

                $startInfo.Arguments =
                  "login docker.io --username `"$dockerUser`" --password-stdin"

                $startInfo.UseShellExecute = $false
                $startInfo.RedirectStandardInput = $true
                $startInfo.RedirectStandardOutput = $true
                $startInfo.RedirectStandardError = $true
                $startInfo.CreateNoWindow = $true

                $dockerProcess =
                  New-Object System.Diagnostics.Process

                $dockerProcess.StartInfo = $startInfo

                try {
                  $null = $dockerProcess.Start()

                  // Gửi token trực tiếp vào stdin của docker.exe
                  $dockerProcess.StandardInput.Write($dockerToken)
                  $dockerProcess.StandardInput.Close()

                  $standardOutput =
                    $dockerProcess.StandardOutput.ReadToEnd()

                  $standardError =
                    $dockerProcess.StandardError.ReadToEnd()

                  $dockerProcess.WaitForExit()

                  if (
                    -not [string]::IsNullOrWhiteSpace(
                      $standardOutput
                    )
                  ) {
                    Write-Host $standardOutput.Trim()
                  }

                  if (
                    -not [string]::IsNullOrWhiteSpace(
                      $standardError
                    )
                  ) {
                    Write-Host $standardError.Trim()
                  }

                  if ($dockerProcess.ExitCode -ne 0) {
                    throw "Docker Hub login thất bại, exit code: $($dockerProcess.ExitCode)"
                  }
                }
                finally {
                  $dockerProcess.Dispose()
                  $dockerToken = $null
                }

                Write-Host "Đang push image: $env:BACKEND_IMAGE"

                docker push $env:BACKEND_IMAGE

                if ($LASTEXITCODE -ne 0) {
                  throw "Docker push thất bại, exit code: $LASTEXITCODE"
                }
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
              BACKEND_IMAGE="$BACKEND_IMAGE" \
              APP_DIR="$APP_DIR" \
              "$APP_DIR/deploy.sh"
            '''
          } else {
            powershell '''
              $ErrorActionPreference = 'Stop'

              $deployScript =
                Join-Path $env:APP_DIR 'deploy.ps1'

              if (-not (Test-Path $deployScript)) {
                throw "Không tìm thấy file deploy: $deployScript"
              }

              Write-Host "Deploy image: $env:BACKEND_IMAGE"
              Write-Host "Deploy folder: $env:APP_DIR"

              & powershell.exe `
                -NoProfile `
                -ExecutionPolicy Bypass `
                -File $deployScript

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
          sh '''
            docker logout docker.io || true
          '''
        } else {
          powershell '''
            docker logout docker.io 2>$null |
              Out-Null

            exit 0
          '''
        }
      }
    }

    success {
      echo '✅ Pipeline hoàn thành thành công!'
    }

    failure {
      echo '❌ Pipeline thất bại. Kiểm tra Console Output.'
    }
  }
}