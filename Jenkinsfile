#!/usr/bin/env groovy


node {
    stage 'Clone the project'
    git branch: 'main', url: 'https://github.com/Benvorth/pushr.git'

    pwd()

    stage("Compilation and Analysis") {
        sh "mvn clean install -DskipTests"
    }

    stage("Tests and Deployment") {
        stage("Staging") {
            sh "pid=\$(lsof -i:8081 -t); kill -TERM \$pid "
            +"|| kill -KILL \$pid"
            withEnv(['JENKINS_NODE_COOKIE=dontkill']) {
                sh 'nohup mvn spring-boot:run -Dserver.port=8081 &'
            }
        }
    }
}