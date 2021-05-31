#!/usr/bin/env groovy

pipeline {
    agent any

    environment {
        MARIADB_USER = credentials('MARIADB_USER')
        MARIADB_PASSWORD = credentials('MARIADB_PASSWORD')
    }

    stages {
        stage("Stop server and clean workspace") {
            steps {
                sh "chmod -R 777 ."
                sh "pid=\$(lsof -i:8081 -t) || true; kill -TERM \$pid || kill -KILL \$pid || true"
                sh 'rm -rf *'
            }
        }


        stage('Clone the frontend') {
            steps {
                dir('pushr-fe') {
                    git branch: 'main', url: 'https://github.com/Benvorth/pushr-fe.git'
                }
            }
        }
        stage('Build the frontend') {
            steps {
                dir('pushr-fe') {
                    sh 'npm install'
                    sh 'npm run build'
                }
            }
        }


        stage('Clone the backend') {
            steps {
                git branch: 'main', url: 'https://github.com/Benvorth/pushr.git'
            }
        }

        stage('Merge frontend and backend') {
            steps {
                sh 'cp -rf pushr-fe/build src/main/resources/static'
            }
        }

        stage("Build the backend") {
            steps {
                sh 'mvn clean install -DskipTests'
            }
        }

        stage("Deploy new version") {
            steps {
                withEnv(['JENKINS_NODE_COOKIE=dontkill']) {
                    sh 'nohup mvn -Dspring.profiles.active=prod -Dspring.datasource.username=root -Dspring.datasource.password=${MARIADB_PASSWORD} -Dserver.port=8081 spring-boot:run &'
                }
            }
        }
    }
}