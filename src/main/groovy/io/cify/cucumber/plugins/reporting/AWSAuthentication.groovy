package io.cify.cucumber.plugins.reporting

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.BasicSessionCredentials
import groovy.json.JsonSlurper
import org.apache.http.HttpEntity
import org.apache.http.HttpHost
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.config.Registry
import org.apache.http.config.RegistryBuilder
import org.apache.http.conn.socket.ConnectionSocketFactory
import org.apache.http.conn.socket.PlainConnectionSocketFactory
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.ssl.SSLContexts
import org.apache.http.util.EntityUtils

import javax.net.ssl.SSLContext

/**
 * Created by FOB Solutions
 *
 * This class is responsible for user authentication via AWS service
 */
class AWSAuthentication {

    private static final String PARAM_ACCESS_KEY = "accessKey"
    private static final PARAM_CIFY_AWS_AUTH_SERVICE = "authService"
    private static String awsAuthService
    private static final PARAM_CIFY_AWS_AUTH_SERVICE_STAGE = "serviceStage"
    private static String authServiceStage
    private static final String PARAM_SERVICE_REGION = "serviceRegion"
    public static String awsRegion
    private static def authData
    public static AWSCredentials credentials

    /**
     * Returns current authentication data
     *
     * @return json object
     */
    public static def getAuthData() {
        if (hasInformation(authData)) {
            return authData
        } else {
            initParameters()
        }
        return authData
    }

    /**
     * Initializing AWS parameters
     */
    private static void initParameters() {
        awsRegion = ReportManager.getParameter(PARAM_SERVICE_REGION)
        if (!awsRegion) {
            throw new Exception("Service region is not provided.")
        }
        if (!getAwsCredentials()) {
            throw new Exception("Reporter credentials not provided.")
        }
    }

    /**
     * Creates and returns AWS credentials
     * @return AWSCredentials
     */
    private static AWSCredentials getAwsCredentials() {
        String cifyAccessKey = ReportManager.getParameter(PARAM_ACCESS_KEY)
        if (cifyAccessKey) {
            def authData = requestAuthData(cifyAccessKey, awsRegion)
            credentials = new BasicSessionCredentials(authData?.awsAccessKey, authData?.secretKey, authData?.sessionToken)
            if (credentials) {
                return credentials
            } else {
                return null
            }
        }
    }

    /**
     * Sends request to AWS service and return authentication data
     *
     * @param cifyAccessKey
     * @param awsRegion
     * @return json object
     */
    private static def requestAuthData(String cifyAccessKey, String awsRegion) {
        if (!cifyAccessKey || !awsRegion) {
            return null
        }

        awsAuthService = ReportManager.getParameter(PARAM_CIFY_AWS_AUTH_SERVICE)
        authServiceStage = ReportManager.getParameter(PARAM_CIFY_AWS_AUTH_SERVICE_STAGE)
        if (!awsAuthService || !authServiceStage) {
            throw new Exception("AWS authentication service info not provided.")
        }

        String apiHostname = "${awsAuthService}.execute-api.${awsRegion}.amazonaws.com"
        String postData = "{\n" +
                "  \"params\": {\n" +
                "    \"login\": {\n" +
                "      \"cifyAccessKey\": \"$cifyAccessKey\"\n" +
                "    }\n" +
                "  }\n" +
                "}"

        String result = httpsRequest(apiHostname, authServiceStage, postData)
        authData = new JsonSlurper().parseText(result)
        if (hasInformation(authData)) {
            return authData
        } else {
            return null
        }
    }

    private static boolean hasInformation(def authData) {
        if (authData && authData.awsAccessKey && authData.secretKey && authData.sessionToken
                && authData.idToken && authData.companyId && authData.stream
                && authData.bucket) {
            return true
        }
        return false
    }

    private static String httpsRequest(String hostName, String resource, String postData) {
        HttpHost target = new HttpHost(hostName, 443, "https")
        SSLContext sslContext = SSLContexts.createSystemDefault()
        String[] supportedProtocols = ["TLSv1", "SSLv3"]
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext, supportedProtocols, null, SSLConnectionSocketFactory.getDefaultHostnameVerifier())

        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", sslConnectionSocketFactory)
                .build()
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry)

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(sslConnectionSocketFactory)
                .setConnectionManager(cm)
                .build()

        HttpPost httpPost = new HttpPost("/$resource")
        ByteArrayEntity postDataEntity = new ByteArrayEntity(postData.getBytes())
        httpPost.setEntity(postDataEntity)
        CloseableHttpResponse response = httpClient.execute(target, httpPost)

        String result = ""
        try {
            HttpEntity entity = response.getEntity()
            result = EntityUtils.toString(entity)
            EntityUtils.consume(entity)
        } finally {
            response.close()
        }
        return result
    }

}
