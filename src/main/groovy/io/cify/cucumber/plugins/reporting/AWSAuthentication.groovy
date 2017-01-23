package io.cify.cucumber.plugins.reporting

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

class AWSAuthentication {

    private static final PARAM_CIFY_AWS_AUTH_API = "cifyAuthApi"
    private static String apiStage = "test"
    private static def authData

    public static def getAuthData(String username, String password, String awsRegion){
        if(hasInformation(authData)){
            return authData
        }

        String awsAuthAPI = ReportManager.getParameter(PARAM_CIFY_AWS_AUTH_API)
        awsAuthAPI?: {throw new Exception("AWS authentication API not provided.")}

        String apiHostname = "${awsAuthAPI}.execute-api.${awsRegion}.amazonaws.com"
        String postData = "{\n" +
                "  \"params\": {\n" +
                "    \"login\": {\n" +
                "      \"username\": \"$username\",\n" +
                "      \"password\": \"$password\"\n" +
                "    }\n" +
                "  }\n" +
                "}"

        String result = httpsRequest(apiHostname,apiStage,postData)
        authData = new JsonSlurper().parseText(result)
        if(hasInformation(authData)){
            return authData
        }
        return null
    }

    private static boolean hasInformation(def authData){
        if(authData.awsAccessKey && authData.secretKey && authData.sessionToken
                && authData.identityId && authData.idToken && authData.accessToken && authData.company){
            return true
        }
        return false
    }

    private static String httpsRequest(String hostName, String resource, String postData ){

        HttpHost target = new HttpHost(hostName, 443, "https");

        SSLContext sslContext = SSLContexts.createSystemDefault();
        String[] supportedProtocols = ["TLSv1", "SSLv3"]
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext, supportedProtocols, null, SSLConnectionSocketFactory.getDefaultHostnameVerifier());

        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", sslConnectionSocketFactory)
                .build();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(sslConnectionSocketFactory)
                .setConnectionManager(cm)
                .build();

        HttpPost httpPost = new HttpPost("/$resource");
        ByteArrayEntity postDataEntity = new ByteArrayEntity(postData.getBytes());
        httpPost.setEntity(postDataEntity);
        CloseableHttpResponse response = httpClient.execute(target, httpPost);

        String result = ""
        try {
            HttpEntity entity = response.getEntity();
            result = EntityUtils.toString(entity);
            EntityUtils.consume(entity);
        } finally {
            response.close();
        }

        return result
    }

}
