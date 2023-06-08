pipeline  {
    agent any

    tools {
        jdk 'OpenJDK11'
        maven 'Maven 3.9.2'
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
        skipDefaultCheckout()
    }

    stages {
    	stage('Clean Workspace') {
            steps {
                // Cleanup before starting the stage
                cleanWs()
            }
        }
    	stage('Checkout') {
            steps {
                // Checkout the repository
                checkout scm 
            }
        }
        stage('Main branch release') {
            when { 
                branch 'main' 
            }
            steps {
                echo "I am building on ${env.BRANCH_NAME}"
                sh "./gradlew clean build release -Drelease.dir=$JENKINS_HOME/repo.gecko/release/org.gecko.jaxrs.whiteboard --info --stacktrace -Dmaven.repo.local=${WORKSPACE}/.m2"
            }
        }
        stage('Snapshot branch release') {
            when { 
                branch 'snapshot'
            }
            steps  {
                echo "I am building on ${env.JOB_NAME}"
                sh "./gradlew clean release --info --stacktrace -x testOSGi -Dmaven.repo.local=${WORKSPACE}/.m2"
                sh "mkdir -p $JENKINS_HOME/repo.gecko/snapshot/org.gecko.jaxrs.whiteboard"
                sh "rm -rf $JENKINS_HOME/repo.gecko/snapshot/org.gecko.jaxrs.whiteboard/*"
                sh "cp -r cnf/release/* $JENKINS_HOME/repo.gecko/snapshot/org.gecko.jaxrs.whiteboard"
            }
        }
        stage('Jakarta branch snapshot') {
            when { 
                branch 'jakarta'
            }
            steps  {
                echo "I am building on ${env.JOB_NAME}"
                sh "./gradlew clean release --info --stacktrace -Dmaven.repo.local=${WORKSPACE}/.m2"
                sh "mkdir -p $JENKINS_HOME/repo.gecko/jakarta/org.gecko.jakarta.whiteboard"
                sh "rm -rf $JENKINS_HOME/repo.gecko/jakarta/org.gecko.jakarta.whiteboard/*"
                sh "cp -r cnf/release/* $JENKINS_HOME/repo.gecko/jakarta/org.gecko.jakarta.whiteboard"
            }
        }
        stage('Jakarta branch release') {
            when { 
                branch 'jakarta-main'
            }
            steps  {
                echo "I am building on ${env.JOB_NAME}"
                sh "./gradlew clean build release -Drelease.dir=$JENKINS_HOME/repo.gecko/release/org.gecko.jakarta.whiteboard -x testOSGi --info --stacktrace -Dmaven.repo.local=${WORKSPACE}/.m2"
            }
        }
        stage('Jakarta-Maven branch release') {
            when { 
                branch 'jakarta-maven'
            }
            steps  {
                echo "I am building on ${env.JOB_NAME}"
                sh "mvn clean install -Dmaven.repo.local=${WORKSPACE}/.m2"
            }
            post {
                success {
                    junit 'target/surefire-reports/**/*.xml' 
                }
            }
        }
    }
}
