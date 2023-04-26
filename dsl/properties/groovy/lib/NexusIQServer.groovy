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
        def violations, reportUrl, reportId
        try {
            ExecutionResult result =cli.runCommand(cmd)
            String stdOut = result.getStdOut()
            log.info "command output:\n $stdOut"
            String stdError = result.getStdErr()
            log.info "command error:\n $stdError"
            if (!result.isSuccess()){
                sr.setJobStepOutcome('error')
            } else {
                violations = extractViolations(stdOut)
                reportUrl = extractReportUrl(stdOut)
                reportId = getReportIdFromReportUrl(reportUrl)
                sr.setJobStepOutcome('success')
                sr.setOutputParameter("Violation Summary", violations)
                sr.setOutputParameter("Build Report URL", reportUrl)
                violations.split(',').each {
                    def severity = it.trim().split(' ')[1].capitalize()
                    def count = it.trim().split(' ')[0]
                   sr.setOutputParameter("$severity Violation Count", count)
                }
            }
        } catch (Exception ex){
            ex.printStackTrace()
            sr.setJobStepOutcome('error')
            sr.setJobStepSummary(ex.getMessage())
        }

        if(reportId){
            restParams.put("reportId", reportId)
            Object response = rest.getReportDetails(restParams)
            log.info "Got response from server: $response"
        }

        //TODO step result output parameters 
        sr.apply()
    }

    def extractViolations(String stdOut) {
        //example: 94 critical, 42 severe, 2 moderate
        return findSingleMatch("((Summary\\sof\\spolicy\\sviolations:\\s*)|(Number\\sof\\sopen\\spolicy\\sviolations:\\s*))(.*)$", 4, stdOut)
    }

    def extractReportUrl(String stdOut) {
        return findSingleMatch("The\\sdetailed\\sreport\\scan\\sbe\\sviewed\\sonline\\sat\\s*(https?:\\/\\/.*?)$", 1, stdOut)
    }

    def getReportIdFromReportUrl(String reportUrl) {
        return findSingleMatch(".*\\/report\\/([a-zA-Z0-9\\-]+)\\/?\\s*$", 1, reportUrl)
    }

    def findSingleMatch(String pattern, int group, String stdOut) {
        def result
        def pattern = Pattern.compile(pattern, Pattern.MULTILINE)
        def matcher = pattern.matcher(stdOut)
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
    def genCommandOptions(clientJar, applicationId, serverURL, credential, applicationWarLocation) {
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
// === step ends ===

}