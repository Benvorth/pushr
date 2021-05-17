#!/usr/bin/env groovy

node {
    stage 'Clone the project'
    git credentialsId: '7b99a74c-80b0-424a-a760-259865f53f55', url: 'https://github.com/Benvorth/pushr.git'

    dir('spring-jenkins-pipeline') {
        stage("Compilation and Analysis") {
            parallel 'Compilation': {
                sh "./mvnw clean install -DskipTests"
            }, 'Static Analysis': {
                stage("Checkstyle") {
                    sh "./mvnw checkstyle:checkstyle"

                    step([$class                   : 'CheckStylePublisher',
                          canRunOnFailed           : true,
                          defaultEncoding          : '',
                          healthy                  : '100',
                          pattern                  : '**/target/checkstyle-result.xml',
                          unHealthy                : '90',
                          useStableBuildAsReference: true
                    ])
                }
            }
        }

        stage("Tests and Deployment") {
            parallel 'Unit tests': {
                stage("Runing unit tests") {
                    try {
                        sh "./mvnw test -Punit"
                    } catch (err) {
                        step([$class: 'JUnitResultArchiver', testResults:
                                '**/target/surefire-reports/TEST-*UnitTest.xml'])
                        throw err
                    }
                    step([$class: 'JUnitResultArchiver', testResults:
                            '**/target/surefire-reports/TEST-*UnitTest.xml'])
                }
            }, 'Integration tests': {
                stage("Runing integration tests") {
                    try {
                        sh "./mvnw test -Pintegration"
                    } catch (err) {
                        step([$class: 'JUnitResultArchiver', testResults:
                                '**/target/surefire-reports/TEST-'
                                        + '*IntegrationTest.xml'])
                        throw err
                    }
                    step([$class: 'JUnitResultArchiver', testResults:
                            '**/target/surefire-reports/TEST-'
                                    + '*IntegrationTest.xml'])
                }
            }

            stage("Staging") {
                sh "pid=\$(lsof -i:8081 -t); kill -TERM \$pid "
                +"|| kill -KILL \$pid"
                withEnv(['JENKINS_NODE_COOKIE=dontkill']) {
                    sh 'nohup ./mvnw spring-boot:run -Dserver.port=8081 &'
                }
            }
        }
    }
}