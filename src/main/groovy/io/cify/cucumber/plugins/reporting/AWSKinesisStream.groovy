package io.cify.cucumber.plugins.reporting

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.profile.ProfileCredentialsProvider
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

    private static String awsSecretKey
    private static String awsAccessKey
    private static AWSCredentials credentials
    private static String awsRegion
    private static String awsKinesisStream
    private static String defaultAwsRegion = "us-west-2"
    private static String defaultAwsKinesisStream = "cify-reporting-test-stream"

    private static final String PARAM_AWS_ACCESS_KEY = "awsAccessKey"
    private static final String PARAM_AWS_SECRET_KEY = "awsSecretKey"
    private static final String PARAM_AWS_KINESIS_STREAM = "awsKinesisStream"
    private static final String PARAM_AWS_REGION = "awsRegion"

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
            data = new JsonBuilder(result.get(partitionKey)).toString()
            return putKinesisStreamRecord(data, partitionKey)
        } else {
            return null
        }
    }

    /**
     * Initializing AWS parameters
     */
    private static void initParameters() {
        getAwsCredentials() ?: {
            throw new Exception("AWS credentials not provided.")
        }
        awsKinesisStream = ReportManager.getParameter(PARAM_AWS_KINESIS_STREAM) ?: defaultAwsKinesisStream
        println("AWS Kinesis stream: " + awsKinesisStream)
        awsRegion = ReportManager.getParameter(PARAM_AWS_REGION) ?: defaultAwsRegion
        println("AWS region: " + awsRegion)
    }

    /**
     * Put data record with partition key to AWS Kinesis stream and returns record sequence number
     * @param data
     * @param partition key
     * @return String
     */
    private static String putKinesisStreamRecord(String data, String partitionKey) {
        AmazonKinesisClient kinesisClient = new AmazonKinesisClient(credentials)
        Region region = RegionUtils.getRegion(awsRegion)
        kinesisClient.setRegion(region)

        PutRecordRequest putRecordRequest = new PutRecordRequest()
        putRecordRequest.setData(ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8)))
        putRecordRequest.setPartitionKey(partitionKey)
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
        awsAccessKey = ReportManager.getParameter(PARAM_AWS_ACCESS_KEY)
        awsSecretKey = ReportManager.getParameter(PARAM_AWS_SECRET_KEY)
        if (awsAccessKey && awsSecretKey) {
            println("Using AWS credentials from parameters.")
            credentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey)
        } else {
            println("Using AWS credentials provider.")
            credentials = new ProfileCredentialsProvider().getCredentials()
        }
        return credentials
    }
}
