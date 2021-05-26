#!/usr/bin/env groovy

node {

    dir('pushr-fe') {

        stage('Clone the frontend') {
            git branch: 'main', url: 'https://github.com/Benvorth/pushr-fe.git'
        }

        stage('Build the frontend') {
            sh "npm run package"
        }
    }


    stage ('Clone the backend') {
        git branch: 'main', url: 'https://github.com/Benvorth/pushr.git'
    }

    stage('Merge frontend and backend') {
        sh "cp -r pushr-fe/build src/main/resources/static"
    }

    stage("Build the backend") {
        sh "mvn clean install -DskipTests"
    }

    stage("Stop running instance") {
        sh "pid=\$(lsof -i:8081 -t) || true; kill -TERM \$pid || kill -KILL \$pid || true"
    }

    stage("Deploy new version") {
        withEnv(['JENKINS_NODE_COOKIE=dontkill']) {
            sh 'nohup mvn spring-boot:run -Dserver.port=8081 &'
        }

    }
}