package io.cify.cucumber.plugins.reporting

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.BasicSessionCredentials
import com.amazonaws.regions.Region
import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.kinesis.AmazonKinesisClient
import com.amazonaws.services.kinesis.model.PutRecordRequest
import com.amazonaws.services.kinesis.model.PutRecordResult
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * Created by FOB Solutions
 *
 * This class is responsible for reporting data to AWS
 */
class AWSKinesisStream {

    private static AWSCredentials credentials
    private static String awsRegion
    private static String awsKinesisStream
    private static String defaultAwsRegion = "us-west-2"

    private static final String PARAM_CIFY_ACCESS_KEY = "cifyAccessKey"
    private static final String PARAM_CIFY_SECRET_KEY = "cifySecretKey"
    private static final String PARAM_AWS_REGION = "awsRegion"

    private static String company
    private static String awsKinesisStreamPostfix = "-stream"

    /**
     * Initializing AWS parameters and put data to AWS Kinesis stream
     * @param data
     * @return String
     */
    public static String exportToAwsKinesisStream(String data) {
        def result = new JsonSlurper().parseText(data) as Map
        String partitionKey = result?.keySet()[0].toString()
        if (partitionKey) {
            initParameters()
            def json = new JsonBuilder()
            json.data(
                    idToken: AWSAuthentication.getAuthData()?.idToken,
                    report: data
            )
            return putKinesisStreamRecord(json.toString(), partitionKey)
        } else {
            return null
        }
    }

    /**
     * Initializing AWS parameters
     */
    private static void initParameters() {
        awsRegion = ReportManager.getParameter(PARAM_AWS_REGION) ?: defaultAwsRegion
        println("AWS region: " + awsRegion)

        if (!getAwsCredentials()) {
            throw new Exception("AWS credentials not provided.")
        }

        awsKinesisStream = company + awsKinesisStreamPostfix
        println("AWS Kinesis stream: " + awsKinesisStream)
    }

    /**
     * Put data record with partition key to AWS Kinesis stream and returns record sequence number
     * @param data
     * @param partition key
     * @return String
     */
    private static String putKinesisStreamRecord(String data, String partitionKey) {
        String newPartitionKey = "<company>$company<company><partition>$partitionKey<partition>"
        AmazonKinesisClient kinesisClient = new AmazonKinesisClient(credentials)
        Region region = RegionUtils.getRegion(awsRegion)
        kinesisClient.setRegion(region)

        PutRecordRequest putRecordRequest = new PutRecordRequest()
        putRecordRequest.setData(ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8)))
        putRecordRequest.setPartitionKey(newPartitionKey)
        putRecordRequest.setStreamName(awsKinesisStream)

        PutRecordResult result = kinesisClient.putRecord(putRecordRequest)
        println("AWS Record sequence number: " + result.getSequenceNumber())
        return result.getSequenceNumber()
    }

    /**
     * Creates and returns AWS credentials
     * @return AWSCredentials
     */
    private static AWSCredentials getAwsCredentials() {
        String cifyAccessKey = ReportManager.getParameter(PARAM_CIFY_ACCESS_KEY)
        String cifySecretKey = ReportManager.getParameter(PARAM_CIFY_SECRET_KEY)
        if (cifyAccessKey && cifySecretKey) {
            println("Using provided cify parameters to get temporary AWS credentials.")
            def authData = AWSAuthentication.getAuthData(cifyAccessKey, cifySecretKey, awsRegion)
            credentials = new BasicSessionCredentials(authData?.awsAccessKey, authData?.secretKey, authData?.sessionToken)
            if (credentials) {
                company = authData?.company
                return credentials
            }
        }
        return null
    }
}
