pipeline  {
	agent any
	
	tools {
        jdk 'OpenJDK8'
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
      	gitLabConnection('gitlab.com')
      	gitlabBuilds(builds: ['Jenkins Build', 'Release', 'Post-Build'])
    }
    triggers {
        gitlab(triggerOnPush: true, triggerOnMergeRequest: true, branchFilterType: 'All')
    }
	post {
      failure {
        updateGitlabCommitStatus name: 'Post-Build', state: 'failed'
      }
      success {
        updateGitlabCommitStatus name: 'Post-Build', state: 'success'
      }
    }
	
    stages {
        stage('Build') {
            steps {
                echo "I am building on ${env.BRANCH_NAME}"
                updateGitlabCommitStatus name: 'Jenkins Build', state: 'pending'
				sh "./gradlew clean build --info"
				junit '**/generated/test-reports/**/*.xml'
				updateGitlabCommitStatus name: 'Jenkins Build', state: 'success'
		    }
		}
        stage('Master branch release') {
            when { 
            	branch 'master' 
            }
            steps {
	
                echo "I am building on ${env.BRANCH_NAME}"
                updateGitlabCommitStatus name: 'Release', state: 'pending'
				sh "./gradlew clean build release -Drelease.dir=$JENKINS_HOME/repo.gecko/release/geckoREST/ -x test -x testOSGi --info"
                updateGitlabCommitStatus name: 'Release', state: 'success'
		    }
		}
        stage('Snapshot branch release') {
            when { 
            	branch 'develop'
            }
            steps  {
                echo "I am building on ${env.JOB_NAME}"
                updateGitlabCommitStatus name: 'Release', state: 'pending'
				sh "./gradlew release --info"
				sh "mkdir -p $JENKINS_HOME/repo.gecko/snapshot/geckoREST"
				sh "rm -rf $JENKINS_HOME/repo.gecko/snapshot/geckoREST/*"
				sh "cp -r cnf/release/* $JENKINS_HOME/repo.gecko/snapshot/geckoREST"
                updateGitlabCommitStatus name: 'Release', state: 'success'
		    }
		}
		
    }

}
