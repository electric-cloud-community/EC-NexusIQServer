
// DO NOT EDIT THIS BLOCK BELOW=== Parameters starts ===
// PLEASE DO NOT EDIT THIS FILE

import com.cloudbees.flowpdf.StepParameters

class CreateBuildReportParameters {
    /**
    * Label: Application ID, type: entry
    */
    String nexusApplicationId
    /**
    * Label: Application WAR Location, type: entry
    */
    String nexusApplicationWarLocation

    static CreateBuildReportParameters initParameters(StepParameters sp) {
        CreateBuildReportParameters parameters = new CreateBuildReportParameters()

        def nexusApplicationId = sp.getRequiredParameter('nexusApplicationId').value
        parameters.nexusApplicationId = nexusApplicationId

        def nexusApplicationWarLocation = sp.getRequiredParameter('nexusApplicationWarLocation').value
        parameters.nexusApplicationWarLocation = nexusApplicationWarLocation

        return parameters
    }
}
// DO NOT EDIT THIS BLOCK ABOVE ^^^=== Parameters ends, checksum: 88c659dc07e0144d22a369c5afef4f90 ===
