pipeline {
  agent any

  environment {
    REGISTRY = credentials('kendy-registry-url')
    REGISTRY_CREDENTIALS = 'kendy-registry-credentials'
    IMAGE_NAME = 'kendy/backend'
    DEPLOY_HOSTS = credentials('kendy-deploy-hosts')
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
        script {
          env.IMAGE_TAG = sh(script: 'git rev-parse --short=12 HEAD', returnStdout: true).trim()
          env.BACKEND_IMAGE = "${env.REGISTRY}/${env.IMAGE_NAME}:${env.IMAGE_TAG}"
        }
      }
    }

    stage('Test') {
      steps {
        sh './mvnw test'
      }
    }

    stage('Build Image') {
      steps {
        sh 'docker build -t "$BACKEND_IMAGE" .'
      }
    }

    stage('Scan Image') {
      steps {
        sh 'trivy image --exit-code 1 --severity HIGH,CRITICAL "$BACKEND_IMAGE"'
      }
    }

    stage('Push Image') {
      steps {
        withCredentials([usernamePassword(credentialsId: env.REGISTRY_CREDENTIALS, usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_PASSWORD')]) {
          sh 'echo "$REGISTRY_PASSWORD" | docker login "$REGISTRY" -u "$REGISTRY_USER" --password-stdin'
          sh 'docker push "$BACKEND_IMAGE"'
        }
      }
    }

    stage('Deploy') {
      when {
        branch 'main'
      }
      steps {
        sshagent(credentials: ['kendy-deploy-ssh-key']) {
          sh '''
            for host in $DEPLOY_HOSTS; do
              ssh -o StrictHostKeyChecking=no "$host" "BACKEND_IMAGE='$BACKEND_IMAGE' APP_DIR=/opt/kendy /opt/kendy/deploy.sh"
            done
          '''
        }
      }
    }
  }
}
