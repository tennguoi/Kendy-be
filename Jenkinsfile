pipeline {
  agent any

  options {
    disableConcurrentBuilds()
  }

  environment {
    REGISTRY              = 'docker.io'
    IMAGE_NAME            = 'tennguoi2/kendy-backend'
    IMAGE_TAG             = "dev-${env.BUILD_NUMBER}"
    DOCKERHUB_CREDENTIALS = 'dockerhub-push-credentials'
    DOCKERHUB_USER        = 'tennguoi2'
    APP_DIR_LINUX         = '/Kendy-deploy'
    APP_DIR_WIN           = 'C:/Kendy-deploy'
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
          // Tạo thư mục cache - dùng PowerShell cho Windows
          if (isUnix()) {
            sh "mkdir -p ${env.TRIVY_CACHE_DIR}"
          } else {
            powershell "New-Item -ItemType Directory -Force -Path ${env.TRIVY_CACHE_DIR}"
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
                --timeout 20m \
                --scanners vuln \
                "$BACKEND_IMAGE"
            """
          } else {
            bat """
              set TRIVY_CACHE_DIR=${TRIVY_CACHE_DIR}
              trivy image --exit-code 1 --severity HIGH,CRITICAL --timeout 20m --scanners vuln %BACKEND_IMAGE%
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
              sh 'printf "%s" "$REGISTRY_PASSWORD" | docker login "$REGISTRY" -u "$DOCKERHUB_USER" --password-stdin'
              sh 'docker push "$BACKEND_IMAGE"'
            } else {
              powershell '''
                $ErrorActionPreference = 'Stop'
                $dockerPassword = $env:REGISTRY_PASSWORD.Trim()
                if ([string]::IsNullOrWhiteSpace($dockerPassword)) {
                  throw 'Docker Hub credential password is empty.'
                }

                $sha = [System.Security.Cryptography.SHA256]::Create()
                try {
                  $tokenBytes = [System.Text.Encoding]::UTF8.GetBytes($dockerPassword)
                  $hashBytes = $sha.ComputeHash($tokenBytes)
                  $tokenHash = ($hashBytes | ForEach-Object { $_.ToString('x2') }) -join ''
                } finally {
                  $sha.Dispose()
                  if ($tokenBytes) {
                    [Array]::Clear($tokenBytes, 0, $tokenBytes.Length)
                  }
                }

                Write-Host "Jenkins token SHA256: $tokenHash"
                Write-Host "Docker credential user from Jenkins: $env:REGISTRY_USER"
                Write-Host "Docker login forced user: $env:DOCKERHUB_USER"
                Write-Host "Docker token length after trim: $($dockerPassword.Length)"

                $startInfo = New-Object System.Diagnostics.ProcessStartInfo
                $startInfo.FileName = 'docker.exe'
                $startInfo.Arguments = "login $env:REGISTRY --username $env:DOCKERHUB_USER --password-stdin"
                $startInfo.UseShellExecute = $false
                $startInfo.RedirectStandardInput = $true
                $startInfo.RedirectStandardOutput = $true
                $startInfo.RedirectStandardError = $true

                $dockerProcess = New-Object System.Diagnostics.Process
                $dockerProcess.StartInfo = $startInfo
                $null = $dockerProcess.Start()
                $dockerProcess.StandardInput.Write($dockerPassword)
                $dockerProcess.StandardInput.Close()
                $standardOutput = $dockerProcess.StandardOutput.ReadToEnd()
                $standardError = $dockerProcess.StandardError.ReadToEnd()
                $dockerProcess.WaitForExit()

                if ($standardOutput) { Write-Host $standardOutput.TrimEnd() }
                if ($standardError) { Write-Host $standardError.TrimEnd() }
                if ($dockerProcess.ExitCode -ne 0) { exit $dockerProcess.ExitCode }
              '''
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
