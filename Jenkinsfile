#!/usr/bin/env groovy

node {
    stage 'Clone the project'
    git branch: 'main', url: 'https://github.com/Benvorth/pushr.git'

    stage("Compilation") {
        sh "mvn clean install -DskipTests"
    }

    stage("Deployment") {

        sh "pid=\$(lsof -i:8081 -t) || true; kill -TERM \$pid || kill -KILL \$pid || true"

        withEnv(['JENKINS_NODE_COOKIE=dontkill']) {
            sh 'nohup mvn spring-boot:run -Dserver.port=8081 &'
        }

    }
}