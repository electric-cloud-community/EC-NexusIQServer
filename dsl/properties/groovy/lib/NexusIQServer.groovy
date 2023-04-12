import com.cloudbees.flowpdf.*

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
    * createBuildReport - Create Build Report/Create Build Report
    * Add your code into this method and it will be called when the step runs
    * @param config (required: true)
    * @param nexusApplicationId (required: true)
    * @param nexusApplicationWarLocation (required: true)
    
    */
    def createBuildReport(StepParameters p, StepResult sr) {
        // Use this parameters wrapper for convenient access to your parameters
        CreateBuildReportParameters sp = CreateBuildReportParameters.initParameters(p)

        // Calling logger:
        log.info p.asMap.get('config')
        log.info p.asMap.get('nexusApplicationId')
        log.info p.asMap.get('nexusApplicationWarLocation')
        

        // Setting job step summary to the config name
        sr.setJobStepSummary(p.getParameter('config')?.getValue() ?: 'null')

        sr.setReportUrl("Sample Report", 'https://cloudbees.com')
        sr.apply()
        log.info("step Create Build Report has been finished")
    }

// === step ends ===

}