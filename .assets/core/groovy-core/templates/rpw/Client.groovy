// === rest client imports starts ===
// === rest client imports template ===
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
// === rest client imports template ends ===
// === rest client imports ends ===
// Place for the custom user imports, e.g. import groovy.xml.*
// === rest client starts ===
// === rest client template ===


@InheritConstructors
class InvalidRestClientException extends Exception {

}


class ProxyConfig {
    String url
    String userName
    String password
}


class {{restClassName}} {


    private static String BEARER_PREFIX = '{% if rest.bearerPrefix %}{{rest.bearerPrefix}}{% else %}Bearer{% endif %}'
    private static String USER_AGENT = '{% if rest.userAgent %}{{rest.userAgent}}{% else %}{{restClassName}} REST Client{% endif %}'
    private static String CONTENT_TYPE = '{% if rest.contentType %}{{rest.contentType}}{% else %}application/json{% endif %}'
    private static OAUTH1_SIGNATURE_METHOD = '{% if rest.oauthSignatureMethod %}{{rest.oauthSignatureMethod}}{% else %}RSA-SHA1{% endif %}'

    String endpoint
    String procedureName
    Map<String, String> procedureParameters

    private Log log
    private REST client
    private ProxyConfig proxyConfig

    {{restClassName}}(String endpoint, RESTConfig restConfig, FlowPlugin plugin) {
        this.endpoint = endpoint
        this.log = plugin.log
        this.client = new REST(restConfig)
    }

    /**
     * Will create a {{restClassName}} object from the plugin Config object.
     * Convenient as it can use pre-defined configuration fields.
     */
    static {{restClassName}} fromConfig(Config config, FlowPlugin plugin) {
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

        return new {{restClassName}}(endpoint, restConfig, plugin)
    }

    // Handles templates like {{ value }}, taking values from the params
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

    {% for endpoint in rest.endpoints %}

    /** Generated code for the endpoint {{endpoint.urlTemplate}}
    * Do not change this code
    {%- for p in endpoint.parameters %}
    * {{p.parameterName}}: in {{p.placement}}
    {%- endfor %}
    */
    def {{endpoint.methodName}}(Map<String, Object> params) {
        this.procedureName = '{{endpoint.methodName}}'
        this.procedureParameters = params

        String uri = '{{endpoint.urlTemplate}}'
        log.debug("URI template $uri")
        uri = renderOneLineTemplate(uri, params)

        Map query = [:]
        {% for p in endpoint.parameters %}
        {% if p.placement == 'query' %}
        query.put('{{p.parameterName}}', params.get('{{p.parameterName}}'))
        {% endif %}
        {% endfor %}
        log.debug "Query: ${query}"

        Object payload
        {%- for p in endpoint.parameters %}
        {%- if p.placement == 'body' %}
        {%- set isMap = "true" %}
        {#- the object must be initialized, but it also may be a List #}
        {%- endif %}
        {%- endfor %}
        {% if isMap %}payload = [:]{%- endif %}
        {% for p in endpoint.parameters %}
        {% if p.placement == 'raw body' %}
        payload = new JsonSlurper().parseText(params.get('{{p.parameterName}}'))
        log.debug "Raw body payload: $payload"
        {% endif %}
        {% if p.placement == 'body' %}
        payload.put('{{p.parameterName}}', params.get('{{p.parameterName}}'))
        {% endif %}
        {% endfor %}
        String jsonTemplate = '''{{endpoint.jsonTemplate}}'''
        if (jsonTemplate) {
            payload = payloadFromTemplate(jsonTemplate, params)
            log.debug("Payload from template: $payload")
        }
        //TODO clean empty fields
        Map headers = [:]
        {%- for p in endpoint.parameters %}
        {%- if p.placement == 'header' %}
        headers.put('{{p.parameterName}}', params.get('{{p.parameterName}}'))
        {%- endif %}
        {%- endfor %}
        {%- for headerName, headerValue in endpoint.headers %}
        headers.put('{{headerName}}', renderOneLineTemplate('{{headerValue}}', params))
        {%- endfor %}
        return makeRequest('{{endpoint.HTTPMethod}}', uri, query, payload, headers)
    }
{% endfor %}

// === rest client template ends ===
// === rest client ends ===
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
        return body
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

