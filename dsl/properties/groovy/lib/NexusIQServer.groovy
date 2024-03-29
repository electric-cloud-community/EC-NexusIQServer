import com.cloudbees.flowpdf.*
import com.cloudbees.flowpdf.components.ComponentManager
import com.cloudbees.flowpdf.components.cli.*
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* NexusIQServer
*/
class NexusIQServer extends FlowPlugin {

    @Override
    Map<String, Object> pluginInfo() {
        return [
                pluginName     : '@PLUGIN_KEY@',
                pluginVersion  : '@PLUGIN_VERSION@',
                configFields   : ['config'],
                configLocations: ['ec_plugin_cfgs'],
                defaultConfigValues: [:]
        ]
    }
// === check connection ends ===
/**
     * Auto-generated method for the procedure Create Build Report/Create Build Report
     * Add your code into this method and it will be called when step runs* Parameter: config* Parameter: nexusApplicationId* Parameter: nexusApplicationWarLocation
     */
    def createBuildReport(StepParameters p, StepResult sr) {
        CreateBuildReportParameters sp = CreateBuildReportParameters.initParameters(p)
        ECNexusIQServerRESTClient rest = genECNexusIQServerRESTClient()
        Map restParams = [:]
        Map requestParams = p.asMap
        log.info "requestParams: $requestParams"
        restParams.put("javaLocation", requestParams.get("nexusJavaLocation")?:"java")
        restParams.put("CLILocation", requestParams.get("nexusCLILocation"))
        restParams.put("credential", requestParams.get("basic_credential"))
        restParams.put("IQServerURL", requestParams.get("endpoint"))
        restParams.put("applicationId", requestParams.get("nexusApplicationId"))
        restParams.put("applicationWarLocation", requestParams.get("nexusApplicationWarLocation"))
        //log.info getContext().getConfigValues()
        def commandOptions = genCommandOptions(restParams.get("CLILocation"), restParams.get("applicationId"), restParams.get("IQServerURL"), restParams.get("credential"), restParams.get("applicationWarLocation"))

        def workspaceDir = System.getProperty('user.dir')
        /** Instantiate CLI component with a ComponentManager */
        CLI cli = (CLI) ComponentManager.loadComponent(CLI.class, [workingDirectory: workspaceDir], this)

        /** Create a Command Instance */
        Command cmd = cli.newCommand(restParams.get("javaLocation"), commandOptions)
        def violations, reportUrl, reportId, componentsIdentifiedCount = 0, licenseIssuesCount = 0, securityIssuesCount = 0
        try {
            ExecutionResult result =cli.runCommand(cmd)
            String stdOut = result.getStdOut()
            log.info "command output:\n $stdOut"
            String stdError = result.getStdErr()
            log.info "command error:\n $stdError"
            log.info "command success:" + result.isSuccess()

            /*
            stdOut = """\
            14:40:13 [INFO] 14:40:13 [INFO] 14:40:13 [INFO] ********************************************************************************************* 
            14:40:13 [INFO] Policy Action: Failure 
            14:40:13 [INFO] Stage: release 
            14:40:13 [INFO] Number of components affected: 76 critical, 22 severe, 0 moderate 
            14:40:13 [INFO] Number of open policy violations: 94 critical, 42 severe, 2 moderate 
            14:40:13 [INFO] Number of grandfathered policy violations: 0 
            14:40:13 [INFO] Number of components: 2676 
            14:40:13 [INFO] The detailed report can be viewed online at https://xxxx:8443/ui/links/application/appid/report/97cb3a8c6a4 
            14:40:13 [INFO]""".stripIndent()
            */
            violations = extractViolations(stdOut)
            reportUrl = extractReportUrl(stdOut)
            componentsIdentifiedCount = extractComponentsIdentifiedCount(stdOut)
            reportId = getReportIdFromReportUrl(reportUrl)
            sr.setOutputParameter("Violation Summary", violations)
            sr.setOutputParameter("Build Report URL", reportUrl)
            violations.split(',').each {
                def severity = it.trim().split(' ')[1].capitalize()
                def count = it.trim().split(' ')[0]
                log.info "severity: $severity, count: $count"
                sr.setOutputParameter("$severity Violation Count", count)
            }
        } catch (Exception ex){
            ex.printStackTrace()
            sr.setJobStepOutcome('error')
            sr.setJobStepSummary(ex.getMessage())
        }

        if(reportId){
            restParams.put("reportId", reportId)
            Object response = rest.getReportDetails(restParams)
            /*
            Object response = """\
            {
                "components": [
                    {
                        "hash": "1249e25aebb15358bedd",
                        "componentIdentifier": {
                            "format": "maven",
                            "coordinates": {
                            "artifactId": "tomcat-util",
                            "groupId": "tomcat",
                            "version": "5.5.23",
                            "extension": "jar",
                            "classifier": ""
                            },
                        },
                        "packageUrl": "pkg:maven/tomcat/tomcat-util@5.5.23?type=jar",	
                        "proprietary": false,
                        "matchState": "exact",
                        "pathnames": [
                            "sample-application.zip/tomcat-util-5.5.23.jar"
                        ],
                        "licenseData": {
                            "declaredLicenses": [
                            {
                                "licenseId": "Apache-2.0",
                                "licenseName": "Apache-2.0"
                            }
                            ],
                            "observedLicenses": [
                            {
                                "licenseId": "No-Sources",
                                "licenseName": "No Sources"
                            }
                            ],
                            "effectiveLicenses": [
                            {
                                "licenseId": "Apache-2.0",
                                "licenseName": "Apache-2.0"
                            }
                            ],
                            "overriddenLicenses": [

                            ],
                            "status": "Open"
                        },
                        "securityData": {
                            "securityIssues": [
                            {
                                "source": "cve",
                                "reference": "CVE-2007-3385",
                                "severity": 4.3,
                                "status": "Open",
                                "url": "http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2007-3385",
                                "threatCategory": "severe"
                            }
                            ]
                        }
                    }
                ]
            }""".stripIndent()
            */
            log.info "Got response from server: $response"
            //response = new JsonSlurper().parseText(response)

            if(response.matchSummary){
                componentsIdentifiedCount = response.matchSummary.totalComponentCount
            }

            response.components.each{ comp ->
                comp.licenseData?.effectiveLicenseThreats?.each{ threat ->
                    if (threat.licenseThreatGroupCategory != 'no-threat'){
                        licenseIssuesCount++
                    }
                }
                if(comp.securityData?.securityIssues?.size() > 0){
                    securityIssuesCount += comp.securityData.securityIssues.size()
                }
            }
            sr.setOutputParameter("Component Count", componentsIdentifiedCount.toString())
            sr.setOutputParameter("License Issue Count", licenseIssuesCount.toString())
            sr.setOutputParameter("Security Issue Count", securityIssuesCount.toString())

            def summary = """\
            <html>
            Components found: $componentsIdentifiedCount <br />
            Policy Alerts: $violations
            Security Alerts: $securityIssuesCount
            License Alerts: $licenseIssuesCount
            <br />
            Build Report Location: <a target="_BLANK" href="$reportUrl">$reportUrl</a></html>
            """.stripIndent()
            sr.setPipelineSummary('Nexus IQ Scan Summary', summary)
            sr.setReportUrl('Nexus IQ Scan URL', reportUrl)
            sr.setJobSummary(summary)

        } else {
            sr.setJobStepOutcome('error')
        }

        sr.apply()
    }

    private def extractViolations(String stdOut) {
        //example: 94 critical, 42 severe, 2 moderate
        return findSingleMatch("((Summary\\sof\\spolicy\\sviolations:\\s*)|(Number\\sof\\sopen\\spolicy\\sviolations:\\s*))(.*)\$", 4, stdOut)
    }

    private def extractReportUrl(String stdOut) {
        return findSingleMatch("The\\sdetailed\\sreport\\scan\\sbe\\sviewed\\sonline\\sat\\s*(https?:\\/\\/.*?)\$", 1, stdOut)
    }

    private def extractComponentsIdentifiedCount(String stdOut) {
        return findSingleMatch("Number\\sof\\scomponents:\\s*(\\d*)\\s*\$", 1, stdOut)
    }

    private def getReportIdFromReportUrl(String reportUrl) {
        return findSingleMatch(".*\\/report\\/([a-zA-Z0-9\\-]+)\\/?\\s*\$", 1, reportUrl)
    }

    private def findSingleMatch(String pattern, int group, String stdOut) {
        def result
        def myPattern = Pattern.compile(pattern, Pattern.MULTILINE)
        def matcher = myPattern.matcher(stdOut)
        while (matcher.find()) {
            result = matcher.group(group)
        }
        return result
    }

    /**
        # 1 - java path
        # 2 - client.jar
        # 3 - id
        # 4 - server url
        # 5 - credenrials username:password
        # 6 - app path
    **/
    private def genCommandOptions(clientJar, applicationId, serverURL, credential, applicationWarLocation) {
        def userName = credential.userName
        def password = credential.secretValue

        def cmdOptions = [ "-jar" , clientJar , "-i", applicationId, "-s", serverURL , "-a" , userName + ":" + password ,  applicationWarLocation]

        return cmdOptions
    }

/**
     * This method returns REST Client object
     */
    ECNexusIQServerRESTClient genECNexusIQServerRESTClient() {
        Context context = getContext()
        ECNexusIQServerRESTClient rest = ECNexusIQServerRESTClient.fromConfig(context.getConfigValues(), this)
        return rest
    }

/**
    * getLatestReportDetails - Get Latest Report Details/Get Latest Report Details
    * Add your code into this method and it will be called when the step runs
    * @param config (required: true)
    * @param nexusApplicationId (required: true)

    */
    def getLatestReportDetails(StepParameters p, StepResult sr) {
        // Use this parameters wrapper for convenient access to your parameters
        GetLatestReportDetailsParameters sp = GetLatestReportDetailsParameters.initParameters(p)

        ECNexusIQServerRESTClient rest = genECNexusIQServerRESTClient()
        Map restParams = [:]
        Map requestParams = p.asMap
        def baseUrl = requestParams.get("endpoint")
        log.info "requestParams: $requestParams"
        restParams.put("publicId", requestParams.get("nexusApplicationId"))

        Object response = rest.getApplicationInfo(restParams)
        log.info "Got rest.getApplicationInfo response from server: ${JsonOutput.toJson(response)}"
        Map restParams1 = [:]
        restParams1.put("applicationId", response.applications[0].id)

        response = rest.getApplicationScanHistory(restParams1)
        log.info "Got rest.getApplicationScanHistory response from server: ${JsonOutput.toJson(response)}"
        def lastReport = response.reports[0]
        log.debug "lastReport: ${JsonOutput.toJson(lastReport)}"
        if(lastReport){
            def componentsIdentifiedCount = lastReport.policyEvaluationResult.totalComponentCount
            sr.setOutputParameter("Component Count", componentsIdentifiedCount.toString())
            sr.setOutputParameter("Critical Component Count", lastReport.policyEvaluationResult.criticalComponentCount.toString())
            sr.setOutputParameter("Severe Component Count", lastReport.policyEvaluationResult.severeComponentCount.toString())
            sr.setOutputParameter("Moderate Component Count", lastReport.policyEvaluationResult.moderateComponentCount.toString())
            sr.setOutputParameter("Critical Policy Violation Count", lastReport.policyEvaluationResult.criticalPolicyViolationCount.toString())
            sr.setOutputParameter("Severe Policy Violation Count", lastReport.policyEvaluationResult.severePolicyViolationCount.toString())
            sr.setOutputParameter("Moderate Policy Violation Count", lastReport.policyEvaluationResult.moderatePolicyViolationCount.toString())
            sr.setOutputParameter("Grandfathered Policy Violation Count", lastReport.policyEvaluationResult.grandfatheredPolicyViolationCount.toString())
            sr.setOutputParameter("Evaluation Date", lastReport.evaluationDate.toString())

            def reportUrl = "${baseUrl}/${lastReport.reportHtmlUrl}"
            def reportPdfUrl = "${baseUrl}/${lastReport.reportPdfUrl}"
            sr.setReportUrl('Nexus IQ Scan URL', reportUrl)
            sr.setOutputParameter("Report Pdf Url", "<html><a href=\"$reportPdfUrl\">Download Report Pdf</a></html>")
            sr.setOutputParameter("Report Url", "<html><a href=\"$reportUrl\">View Report</a></html>")
            sr.setOutputParameter("Scan Id", lastReport.scanId.toString())
            sr.setOutputParameter("Stage", lastReport.stage.toString())

            def resultPropertySheet = requestParams.get("resultPropertySheet")
            if(resultPropertySheet){
                sr.setOutcomeProperty(resultPropertySheet + "/Component Count", componentsIdentifiedCount.toString())
                sr.setOutcomeProperty(resultPropertySheet + "/Critical Component Count", lastReport.policyEvaluationResult.criticalComponentCount.toString())
                sr.setOutcomeProperty(resultPropertySheet + "/Severe Component Count", lastReport.policyEvaluationResult.severeComponentCount.toString())
                sr.setOutcomeProperty(resultPropertySheet + "/Moderate Component Count", lastReport.policyEvaluationResult.moderateComponentCount.toString())
                sr.setOutcomeProperty(resultPropertySheet + "/Critical Policy Violation Count", lastReport.policyEvaluationResult.criticalPolicyViolationCount.toString())
                sr.setOutcomeProperty(resultPropertySheet + "/Severe Policy Violation Count", lastReport.policyEvaluationResult.severePolicyViolationCount.toString())
                sr.setOutcomeProperty(resultPropertySheet + "/Moderate Policy Violation Count", lastReport.policyEvaluationResult.moderatePolicyViolationCount.toString())
                sr.setOutcomeProperty(resultPropertySheet + "/Grandfathered Policy Violation Count", lastReport.policyEvaluationResult.grandfatheredPolicyViolationCount.toString())
                sr.setOutcomeProperty(resultPropertySheet + "/Nexus IQ Scan URL", reportUrl)
                sr.setOutcomeProperty(resultPropertySheet + "/Evaluation Date", lastReport.evaluationDate.toString())
                sr.setOutcomeProperty(resultPropertySheet + "/Scan Id", lastReport.scanId.toString())
                sr.setOutcomeProperty(resultPropertySheet + "/Stage", lastReport.stage.toString())
                sr.setOutcomeProperty(resultPropertySheet + "/Report Pdf Url", reportPdfUrl)
                sr.setOutcomeProperty(resultPropertySheet + "/Report Url", reportUrl)
            }
        } else {
            sr.setPipelineSummary('Nexus IQ Scan Summary', 'No report found')
            sr.setReportUrl('Nexus IQ Scan URL', 'No report found')
            sr.setJobSummary('No report found')
            sr.setJobStepOutcome('error')
        }

        sr.apply()
        log.info("step Get Latest Report Details has been finished")
    }

// === step ends ===

}