// DO NOT EDIT THIS BLOCK BELOW=== rest client imports starts ===
import com.cloudbees.flowpdf.*
import com.cloudbees.flowpdf.client.Constants
import com.cloudbees.flowpdf.client.RESTRequest
import com.cloudbees.flowpdf.client.REST
import com.cloudbees.flowpdf.client.RESTConfig
import com.cloudbees.flowpdf.client.RESTResponse
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.InheritConstructors
import sun.reflect.generics.reflectiveObjects.NotImplementedException
// DO NOT EDIT THIS BLOCK ABOVE ^^^=== rest client imports ends, checksum: 13f768133315b40c755fd1b028e2f22b ===
// Place for the custom user imports, e.g. import groovy.xml.*
// DO NOT EDIT THIS BLOCK BELOW=== rest client starts ===
@InheritConstructors
class InvalidRestClientException extends Exception {

}

class ProxyConfig {
    String url
    String userName
    String password
}

class ECNexusIQServerRESTClient {

    private static String BEARER_PREFIX = 'Bearer'
    private static String USER_AGENT = 'My Nexus IQ Client'
    private static String CONTENT_TYPE = 'application/json'
    private static OAUTH1_SIGNATURE_METHOD = 'RSA-SHA1'

    String endpoint
    String procedureName
    Map<String, String> procedureParameters

    private Log log
    private REST client
    private ProxyConfig proxyConfig

    ECNexusIQServerRESTClient(String endpoint, RESTConfig restConfig, FlowPlugin plugin) {
        this.endpoint = endpoint
        this.log = plugin.log
        this.client = new REST(restConfig)
    }

    /**
     * Will create a ECNexusIQServerRESTClient object from the plugin Config object.
     * Convenient as it can use pre-defined configuration fields.
     */
    static ECNexusIQServerRESTClient fromConfig(Config config, FlowPlugin plugin) {
        Map params = [:]
        String endpoint = config.getRequiredParameter('endpoint').value.toString()
        Log log = plugin.log
        Credential credential
        RESTConfig restConfig = new RESTConfig()
            .withEndpoint(endpoint)
        if ((credential = config.getCredential('bearer_credential')) && credential.secretValue) {
            if (!credential.userName)
                credential.userName = BEARER_PREFIX
            restConfig.withCredentialForScheme('bearer', credential)
            log.debug "Using bearer credential in REST Client"
        } else if ((credential = config.getCredential('basic_credential'))) {
            restConfig.withCredentialForScheme('basic', credential)
        } else if (config.isParameterHasValue('authScheme') && config.getParameter('authScheme').value == 'anonymous') {
            log.debug "Using anonymous auth scheme"
            restConfig.withAuthScheme('anonymous')
        } else {
            restConfig.withAuthScheme('anonymous')
        }

        if (config.isParameterHasValue('httpProxyUrl')) {
            restConfig.withProxyParameters(config.getParameter('httpProxyUrl').value, config.getCredential('proxy_credential'))
        }

        return new ECNexusIQServerRESTClient(endpoint, restConfig, plugin)
    }

    // Handles templates like , taking values from the params
    private static String renderOneLineTemplate(String uri, Map params) {
        for (String key in params.keySet()) {
            Object value = params.get(key)
            if (uri =~ /\{\{$key\}\}/) {
                if (value) {
                    uri = uri.replaceAll(/\{\{$key\}\}/, value as String)
                } else {
                    throw new InvalidRestClientException("A field $key is empty in params but required in the template")
                }
            }
        }
        return uri
    }

    /**
     * This is the main request method
     * requestMethod - GET|POST|PUT - request method
     * pathUrl - uri path (without the endpoint)
     * queries - uri.query
     * payload - payload for POST/PUT requests
     * headers - headers for the request
     */
    Object makeRequest(String requestMethod, String pathUrl, Map queries, def payload, Map headers) {

        RESTRequest restRequest = new RESTRequest()
            .withRequestMethod(requestMethod)
            .withPathUrl(pathUrl)
            .withQueries(queries)
            .withHeaders(headers) as RESTRequest

        if (payload) {
            if (payload instanceof byte[]) {
                restRequest.withContentBytes(payload)
            } else {
                restRequest.withContentString(encodePayload(payload))
            }
        }

        if ((restRequest.requestMethod == "POST") || (restRequest.requestMethod == "PUT") || (restRequest.requestMethod == "PATCH")) {
            if (!restRequest.requestContentType) {
                restRequest.requestContentType = Constants.CT_JSON
            }
        }

        RESTResponse restResponse = client.doRequest(augmentRequest(client.prepareRequest(restRequest)), true)

        Object body = restResponse.content

        Object processedResponse = processResponse(restResponse, body)
        if (processedResponse) {
            return processedResponse
        }
        Object parsed = parseResponse(restResponse, body)
        return parsed
    }

    private static payloadFromTemplate(String template, Map params) {
        Object object = new JsonSlurper().parseText(template)
        object = fillFields(object, params)
        return object
    }

    private static def fillFields(def o, Map params) {
        def retval
        if (o instanceof Map) {
            retval = [:]
            for (String key in o.keySet()) {
                key = renderOneLineTemplate(key, params)
                def value = o.get(key)
                if (value instanceof String) {
                    value = renderOneLineTemplate(value, params)
                } else {
                    value = fillFields(value, params)
                }
                retval.put(key, value)
            }
        } else if (o instanceof List) {
            retval = []
            for (def i in o) {
                i = fillFields(i, params)
                retval.add(i)
            }
        } else if (o instanceof String) {
            o = renderOneLineTemplate(o, params)
            retval = o
        } else if (o instanceof Integer || o instanceof Boolean) {
            retval = o
        } else {
            throw new NotImplementedException()
        }
        return retval
    }

    /** Generated code for the endpoint /api/v2/applications/{{applicationId}}/reports/{{reportId}}
    * Do not change this code
    * applicationId: in path
    * reportId: in path
    */
    def getReportDetails(Map<String, Object> params) {
        this.procedureName = 'getReportDetails'
        this.procedureParameters = params

        String uri = '/api/v2/applications/{{applicationId}}/reports/{{reportId}}'
        log.debug("URI template $uri")
        uri = renderOneLineTemplate(uri, params)

        Map query = [:]

        log.debug "Query: ${query}"

        Object payload

        String jsonTemplate = ''''''
        if (jsonTemplate) {
            payload = payloadFromTemplate(jsonTemplate, params)
            log.debug("Payload from template: $payload")
        }
        //TODO clean empty fields
        Map headers = [:]
        return makeRequest('GET', uri, query, payload, headers)
    }

    /** Generated code for the endpoint /api/v2/applications
    * Do not change this code
    * publicId: in query
    */
    def getApplicationInfo(Map<String, Object> params) {
        this.procedureName = 'getApplicationInfo'
        this.procedureParameters = params

        String uri = '/api/v2/applications'
        log.debug("URI template $uri")
        uri = renderOneLineTemplate(uri, params)

        Map query = [:]

        query.put('publicId', params.get('publicId'))

        log.debug "Query: ${query}"

        Object payload

        String jsonTemplate = ''''''
        if (jsonTemplate) {
            payload = payloadFromTemplate(jsonTemplate, params)
            log.debug("Payload from template: $payload")
        }
        //TODO clean empty fields
        Map headers = [:]
        return makeRequest('GET', uri, query, payload, headers)
    }

    /** Generated code for the endpoint /api/v2/reports/applications/{{applicationId}}/history
    * Do not change this code
    * applicationId: in path
    */
    def getApplicationScanHistory(Map<String, Object> params) {
        this.procedureName = 'getApplicationScanHistory'
        this.procedureParameters = params

        String uri = '/api/v2/reports/applications/{{applicationId}}/history'
        log.debug("URI template $uri")
        uri = renderOneLineTemplate(uri, params)

        Map query = [:]

        log.debug "Query: ${query}"

        Object payload

        String jsonTemplate = ''''''
        if (jsonTemplate) {
            payload = payloadFromTemplate(jsonTemplate, params)
            log.debug("Payload from template: $payload")
        }
        //TODO clean empty fields
        Map headers = [:]
        return makeRequest('GET', uri, query, payload, headers)
    }
// DO NOT EDIT THIS BLOCK ABOVE ^^^=== rest client ends, checksum: 8b56cd691c3af46607dd80337bf921f2 ===
    /**
     * Use this method for any request pre-processing: adding custom headers, binary files, etc.
     */
    RESTRequest augmentRequest(RESTRequest request) {
        return request
    }

    /**
     * Use this method to provide a custom encoding for you payload (XML, yaml etc)
     */
    String encodePayload(def payload) {
        return JsonOutput.toJson(payload)
    }

    /**
     * Use this method to parse/alter response from the server
     */
    def parseResponse(RESTResponse restResponse, Object body) {
        //Relying on http builder content type processing
        def jsonSlurper = new JsonSlurper()
        return jsonSlurper.parseText(body)
    }

    /**
     * Use this method to alter default server response processing.
     * The response from this method will be returned as is, if any.
     * To disable response, just return null.
     */
    def processResponse(RESTResponse restResponse, Object body) {
        return null
    }

}