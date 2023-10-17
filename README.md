EC-NexusIQServer Plugin

EC-NexusIQServer



Plugin version 2.1.3

Revised on Tue Oct 17 10:31:52 ICT 2023


* * *


Contents



*   [Overview](#overview)
*   [Plugin Configurations](#plugin-configurations)
*   [Plugin Procedures](#plugin-procedures)
    *   [Create Build Report](#create-build-report)
    *   [Get Latest Report Details](#get-latest-report-details)

## Overview


Integrates witn SonaType Nexus IQ Server reimplemented with PDK.




## Plugin Configurations

Plugin configurations are sets of parameters that can be applied across some, or all, of the plugin procedures. They can reduce the repetition of common values, create predefined parameter sets, and securely store credentials. Each configuration is given a unique name that is entered in the designated parameter for the plugin procedures that use them.  

### Creating Plugin Configurations

*   To create plugin configurations in CloudBees CD/RO, complete the following steps:
*   Navigate to DevOps Essentials  Plugin Management  Plugin configurations.
*   Select Add plugin configuration to create a new configuration.
*   In the New Configuration window, specify a Name for the configuration.
*   Select the Project that the configuration belongs to.
*   Optionally, add a Description for the configuration.
*   Select the appropriate Plugin for the configuration.
*   Configure the parameters per the descriptions below.

Configuration Parameters

| Parameter | Description |
| --- | --- |
| **Configuration Name** | Unique name for the configuration |
| Description | Configuration description |
| **Nexus IQ Server URL** | REST API Endpoint |
| Check Connection? | If checked, a connection endpoint and credentials will be tested before save. The configuration will not be saved if the test fails. |
| Debug Level | This option sets debug level for logs. If info is selected, only summary information will be shown, for debug, there will be some debug information and for trace the whole requests and responses will be shown. |

## Plugin Procedures

**IMPORTANT** Note that the names of Required parameters are marked in **_bold italics_** in the parameter description table for each procedure.




## Create Build Report

This procedure creates a build report using the Nexus CLI.

### Create Build Report Parameters

| Parameter | Description |
| --- | --- |
| **Configuration Name** | Previously defined configuration for the plugin |
| **nexusApplicationId** | The name of the Application for which Build Report needs to be created. For example sandbox-application. |
| **nexusApplicationWarLocation** | Specify the absolute path to the Application WAR file. For example /opt/DevSecOps/struts2-rest-showcase-*.war |


#### Output Parameters

| Parameter | Description |
| --- | --- |
| Violation Summary | Something like this "Summary of policy violations: 7 critical, 3 severe, 0 moderate" |
| Build Report URL | Path to Build Report URL. Something like this http://nexus/ui/links/application/sandbox-application/report/806fbab9e4ed426e85e1a7ee721b0e6f |
| Critical Violation Count | Number of Critical Violations, for example 7 |
| Severe Violation Count | Number of Severe Violations, for example 3 |
| Moderate Violation Count | Number of Moderate Violations, for example 0 |
| Component Count | Number of Components Identified, for example 28 |
| Security Issue Count | Number of Security Issues Identified, for example 27 |
| License Issue Count | Number of License Issues Identified, for example 6 |



## Get Latest Report Details

This procedure retrieve the latest report details.

### Get Latest Report Details Parameters

| Parameter | Description |
| --- | --- |
| **Configuration Name** | Previously defined configuration for the plugin |
| **nexusApplicationId** | The name of the Application for which Build Report needs to be created. For example sandbox-application. |
| resultPropertySheet | The result propert sheet to store the report details. |


#### Output Parameters

| Parameter | Description |
| --- | --- |
| Component Count | Number of Components Identified, for example 28 |
| Critical Component Count | Number of Critical Component Count Identified, for example 27 |
| Severe Component Count | Number of Severe Component Count Identified, for example 27 |
| Moderate Component Count | Number of Moderate Component Count Identified, for example 27 |
| Critical Policy Violation Count | Number of Critical Policy Violation Count Identified, for example 27 |
| Severe Policy Violation Count | Number of Severe Policy Violation Count Identified, for example 27 |
| Moderate Policy Violation Count | Number of Moderate Policy Violation Count Identified, for example 27 |
| Grandfathered Policy Violation Count | Number of Grandfathered Policy Violation Count Identified, for example 27 |
| Evaluation Date | Evaluation Date of the last scan |
| Scan Id | Scan Id of the last scan |
| Stage | Stage of the last scan |
| Report Pdf Url | Report Pdf Url |
