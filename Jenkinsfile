#!/usr/bin/env groovy


node {
    stage("Stop server and clean workspace") {
        sh "chmod -R 777 ."
        sh "pid=\$(lsof -i:8081 -t) || true; kill -TERM \$pid || kill -KILL \$pid || true"
        sh 'rm -rf *'
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
        sh 'cp -rf pushr-fe/build src/main/resources/static'
    }

    stage("Build the backend") {
        sh 'mvn clean install -DskipTests'
    }

    stage("Deploy new version") {
        withEnv(['JENKINS_NODE_COOKIE=dontkill']) {
            environment {
                MARIADB_USER        = credentials('MARIADB_USER')
                MARIADB_PASSWORD    = credentials('MARIADB_PASSWORD')
            }
            sh 'echo $MARIADB_USER'

            sh 'nohup mvn -Dspring.profiles.active=prod -Dserver.port=8081 -Dspring.datasource.username=$MARIADB_USER -Dspring.datasource.password=$MARIADB_PASSWORD spring-boot:run &'
        }

    }
}