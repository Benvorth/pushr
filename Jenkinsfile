#!/usr/bin/env groovy

node {
    stage("Stop running instance") {
        sh "pid=\$(lsof -i:8081 -t) || true; kill -TERM \$pid || kill -KILL \$pid || true"
        cleanWs()
    }


    dir('pushr-fe') {

        stage('Clone the frontend') {
            git branch: 'main', url: 'https://github.com/Benvorth/pushr-fe.git'
        }

        stage('Build the frontend') {
            sh 'npm install'
            sh 'npm run build'
        }
    }



    stage ('Clone the backend') {
        git branch: 'main', url: 'https://github.com/Benvorth/pushr.git'
    }

    stage('Merge frontend and backend') {
        // sh 'chown -R root:jenkins src/main/resources/static'
        sh 'rm -r src/main/resources/static'
        sh 'cp -rf pushr-fe/build/* src/main/resources/static'
    }

    stage("Build the backend") {
        sh 'mvn clean install -DskipTests'
    }



    stage("Deploy new version") {
        withEnv(['JENKINS_NODE_COOKIE=dontkill']) {
            sh 'nohup mvn spring-boot:run -Dserver.port=8081 &'
        }

    }
}