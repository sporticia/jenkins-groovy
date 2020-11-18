if(qualityGate) {
    stage('SonarQube QualityGate')
    {
        // Get sonarqube-token from credentials
        withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONARQUBE_TOKEN'),]){
            // Prepare HTTP URL access token for use
            def encoded_SONARQUBE_TOKEN = "$SONARQUBE_TOKEN:".bytes.encodeBase64().toString()
            // Need to get taskId from <work_dir>/report-task.txt to use to get analysisId from SonarQube URL to get QualityGate result
            def props = readProperties file: "test-artifacts/report-task.txt"
            // Output all props for debugging (turn off once this is working)
            //println "sonarReportProperties=${props}"
            // Get server URL
            def sonarServerUrl = "${props.serverUrl}"
            // Get dashboard URL for failure review
            def sonarDashURL = "${props.dashboardUrl}"
            // Get the URL for the taskId of this scan
            def sonarTaskUrl = "${props.ceTaskUrl}"
            // Set vars to initial null value before loop
            def analysisStatus = null
            def analysisId = null
            def taskResponseParsed = null

            // Timeout after one minute
            timeout(time: 60, unit: 'SECONDS') {
                // Keep checking for the status of the taskId scan until it completes and return 'SUCCESS' or timeout and fail after 1 mins
                while (analysisStatus != ("SUCCESS")) {
                    def taskUrl = new URL(sonarTaskUrl).openConnection()
                    taskUrl.setRequestProperty("Authorization", "Basic ${encoded_SONARQUBE_TOKEN}")
                    taskResponse = taskUrl.getInputStream().getText()
                    println "taskResponse is ${taskResponse}"
                    // Get the analysisId from the taskResponse results output
                    def jsonSlurper = new JsonSlurperClassic()
                    taskResponseParsed = jsonSlurper.parseText(taskResponse)
                    analysisStatus = taskResponseParsed.task.status
                    println "analysisStatus is ${analysisStatus}"
                    // Clear non serializable values to stop Jenkins borking out
                    taskUrl = null
                    jsonSlurper = null
                    // If task scan has not completed result will not be SUCCESS, wait 5 seconds before retrying
                    if (analysisStatus != ("SUCCESS")) {
                        sleep(5)
                    }
                }
                // Once taskId scan status is SUCCESS, grab the analysisId
                analysisId = (taskResponseParsed.task.analysisId)
                println "analysisId is ${analysisId}"
            }

            // Check the quality gate result using the analysisId
            def sonarQGateUrl = sonarServerUrl + "/api/qualitygates/project_status?analysisId=${analysisId}"
            def SQGateUrl = new URL(sonarQGateUrl).openConnection()
            SQGateUrl.setRequestProperty("Authorization", "Basic ${encoded_SONARQUBE_TOKEN}")
            SQGateResponse = SQGateUrl.getInputStream().getText()

            // Get the quality gate result from the quality gate results output
            jsonSlurper = new JsonSlurperClassic()
            SQGateResponseParsed = jsonSlurper.parseText(SQGateResponse)
            SQGateResult = SQGateResponseParsed.projectStatus.status

            // Echo out quality gate result
            println "SonarQube quality gate result is ${SQGateResult}"

            // Clear non serializable values to stop Jenkins borking out (again)
            SQGateUrl = null
            jsonSlurper = null

            if (SQGateResult == ("ERROR")) {
                // If SQGateResult is ERROR bomb out
                error "Pipeline aborted due to SonarQube qualitygate failure: ${SQGateResult} \n\nPlease review ${sonarDashURL}\n\n"
            } else if (SQGateResult == ("OK")) {
                // If SQGateResult is OK carry on
                println "SonarQube quality gate passed."
            } else {
                // If SQGateResult is not OK or ERROR bomb out
                error "SonarQube quality gate result undefined: ${SQGateResult} \n\n Please review ${sonarDashURL}"
            }
        }
    }
}