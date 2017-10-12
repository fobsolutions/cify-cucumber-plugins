package io.cify.cucumber.reporting

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.BasicSessionCredentials
import groovy.json.JsonSlurper
import org.apache.http.HttpEntity
import org.apache.http.HttpHost
import org.apache.http.StatusLine
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
    private static def authData
    public static String region
    public static AWSCredentials credentials

    /**
     * Returns current authentication data
     *
     * @return json object
     */
    static def getAuthData() {
        if (validateAuthData(authData) && credentials) {
            return authData
        } else {
            setAwsCredentials()
            return authData
        }

    }

    /**
     * Creates and returns AWS credentials
     * @return AWSCredentials
     */
    private static setAwsCredentials() {
        String cifyAccessKey = ReportManager.getParameter(PARAM_ACCESS_KEY)
        if (cifyAccessKey) {
            def authData = requestAuthData(cifyAccessKey)
            region = authData?.region
            credentials = new BasicSessionCredentials(authData?.awsAccessKey, authData?.secretKey, authData?.sessionToken)
            if (!credentials || !region) {
                println("Authentication failed. Unable to get credentials")
                System.exit(0)
            }
        } else {
            println("Authentication failed. Access key is not provided")
            System.exit(0)
        }
    }

    /**
     * Sends request to AWS service and return authentication data
     *
     * @param cifyAccessKey
     * @return json object
     */
    private static def requestAuthData(String cifyAccessKey) {
        awsAuthService = ReportManager.getParameter(PARAM_CIFY_AWS_AUTH_SERVICE)
        if (!awsAuthService) {
            println("Authentication service info not provided.")
            System.exit(0)
        }

        String postData = "{\n" +
                "  \"params\": {\n" +
                "    \"login\": {\n" +
                "      \"cifyAccessKey\": \"$cifyAccessKey\"\n" +
                "    }\n" +
                "  }\n" +
                "}"

        authData = httpsRequest(awsAuthService, postData)
        return validateAuthData(authData)
    }

    private static def validateAuthData(def authData) {
        if (authData && authData.awsAccessKey && authData.secretKey && authData.sessionToken
                && authData.idToken && authData.companyId && authData.stream
                && authData.bucket) {
            return authData
        }
        return null
    }

    private static def httpsRequest(String hostName, String postData) {
        def statusCode = null
        def json = null
        boolean failed = false
        CloseableHttpResponse response

        try {
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

            HttpPost httpPost = new HttpPost("/")
            ByteArrayEntity postDataEntity = new ByteArrayEntity(postData.getBytes())
            httpPost.setEntity(postDataEntity)
            response = httpClient.execute(target, httpPost)
            StatusLine sl = response.getStatusLine()
            statusCode = sl.getStatusCode()
            HttpEntity entity = response.getEntity()
            String result = EntityUtils.toString(entity)
            EntityUtils.consume(entity)
            json = new JsonSlurper().parseText(result)
        }
        catch (ignore) {
            failed = true
        }
        finally {
            if (response)
                response.close()
        }

        if (statusCode != 200 || failed) {
            if (json && json.message) {
                println("Reporter: " + json.message)
            } else {
                println("Reporter: authentication failed")
            }
            System.exit(0)
        }
        return json
    }

}
