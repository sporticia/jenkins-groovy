package com.worldremit.jenkins.steps

import groovy.json.JsonOutput
import groovy.json.JsonSlurperClassic;

class SonarQube {
    static def prepareSonarqubeScannerParams(context, projectName, branchName = null, pullRequestKey = null, version = null) {
        def sonarqubeScannerParamsOutput
        context.withSonarQubeEnv(credentialsId: 'sonarqube-token') {
            sonarqubeScannerParamsOutput = context.sh(script: 'echo $SONARQUBE_SCANNER_PARAMS', returnStdout: true)
        }

        def jsonSlurper = new JsonSlurperClassic()
        def sonarqubeScannerParams = jsonSlurper.parseText(sonarqubeScannerParamsOutput)

        sonarqubeScannerParams['sonar.projectKey'] = projectName
        sonarqubeScannerParams['sonar.projectName'] = projectName

        // SCM intergration
        // https://sonarqube.cloud.worldremit.com/documentation/analysis/scm-integration/
        sonarqubeScannerParams['sonar.scm.provider'] = 'git'
        sonarqubeScannerParams['sonar.scm.revision'] = version

        // Branch integration
        // https://sonarqube.cloud.worldremit.com/documentation/branches/overview/
        if (branchName && !pullRequestKey) {
            if (branchName != 'master') {
                sonarqubeScannerParams['sonar.branch.name'] = branchName
                sonarqubeScannerParams['sonar.branch.target'] = 'master'
            }
        }

        // PR integration
        // https://sonarqube.cloud.worldremit.com/documentation/analysis/pull-request/
        if (pullRequestKey) {
            sonarqubeScannerParams['sonar.pullrequest.key'] = pullRequestKey
            sonarqubeScannerParams['sonar.pullrequest.branch'] = branchName
            sonarqubeScannerParams['sonar.pullrequest.base'] = 'master'

            // PR decoration
            sonarqubeScannerParams['sonar.pullrequest.github.repository'] = "Worldremit/${projectName}"
        }

        return JsonOutput.toJson(sonarqubeScannerParams)
    }
}
