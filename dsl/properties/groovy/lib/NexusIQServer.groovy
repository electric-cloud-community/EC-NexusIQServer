import com.cloudbees.flowpdf.*
import com.cloudbees.flowpdf.components.ComponentManager
import com.cloudbees.flowpdf.components.cli.*

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
        def reportId
        try {
            ExecutionResult result =cli.runCommand(cmd)
            String stdOut = result.getStdOut()
            log.info "command output:\n $stdOut"
            String stdError = result.getStdErr()
            log.info "command error:\n $stdError"
            if (!result.isSuccess()){
                sr.setJobStepOutcome('error')
            }
        } catch (Exception ex){
            ex.printStackTrace()
            sr.setJobStepOutcome('error')
            sr.setJobStepSummary(ex.getMessage())
        }
        reportId = "dummyId"
        if(reportId){
            restParams.put("reportId", reportId)
            Object response = rest.getReportDetails(restParams)
            log.info "Got response from server: $response"
        }
        
        //TODO step result output parameters 
        sr.apply()
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