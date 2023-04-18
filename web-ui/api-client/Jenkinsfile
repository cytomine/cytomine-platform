node {
    stage 'Retrieve sources'
    checkout([
        $class: 'GitSCM',
        branches: [[name: 'refs/heads/'+env.BRANCH_NAME]],
        extensions: [[$class: 'CloneOption', noTags: false, shallow: false, depth: 0, reference: '']],
        userRemoteConfigs: scm.userRemoteConfigs,
    ])

    stage 'Clean'
    sh 'rm -rf ./ci'
    sh 'mkdir -p ./ci'

    stage 'Compute version name'
    sh 'scripts/ciBuildVersion.sh ${BRANCH_NAME}'

    stage 'Download and cache dependencies'
    sh 'scripts/ciDownloadDependencies.sh'

    lock('cytomine-instance-test') {
      stage 'Run cytomine instance'
      catchError {
          sh 'docker-compose -f scripts/docker-compose.yml down -v'
      }
      sh 'docker-compose -f scripts/docker-compose.yml up -d'

      sleep(time:30,unit:"SECONDS") // wait for cytomine, TODO: we should wait in the js code (retry connection).

      stage 'Build and test'
      catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
          sh 'scripts/ciTest.sh'
      }
      stage 'Publish test'
      step([$class: 'JUnitResultArchiver', testResults: 'ci/test-reports/*.xml'])

      stage 'Clear cytomine instance'
      catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS') {
          sh 'docker-compose -f scripts/docker-compose.yml down -v'
      }
    }
    stage 'Publish if official release'


    withFolderProperties{
        // if PRIVATE is define in jenkins, the war and the docker image are send to the private cytomine repository.
        // otherwise, public repo for war and public dockerhub repo for docker image
        echo("Private: ${env.PRIVATE}")

        if (env.PRIVATE && env.PRIVATE.equals("true")) {
            stage 'Publish package (private)'
            withCredentials(
                [
                    string(credentialsId: 'NPM_TOKEN', variable: 'NPM_TOKEN')
                ]
                ) {
                    sh 'scripts/ciPublishPrivate.sh'
                }
        } else {
            stage 'Publish package (public)'
            withCredentials(
                [
                    string(credentialsId: 'NPM_TOKEN', variable: 'NPM_TOKEN')
                ]
                ) {
                    sh 'scripts/ciPublishPublic.sh'
                }
        }
    }









}
