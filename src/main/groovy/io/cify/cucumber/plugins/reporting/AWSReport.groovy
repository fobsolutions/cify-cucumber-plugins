package io.cify.cucumber.plugins.reporting

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.Region
import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClient
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest
import com.amazonaws.services.kinesisfirehose.model.PutRecordResult
import com.amazonaws.services.kinesisfirehose.model.Record

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * Created by FOB Solutions
 *
 * This class is responsible for reporting data to AWS
 */
class AWSReport {

    private static String awsSecretKey
    private static String awsAccessKey
    private static AWSCredentials credentials
    private static String awsRegion
    private static String awsFirehoseStream
    private static String defaultAwsRegion = "us-west-2"
    private static String defaultAwsFirehoseStream = "reporting-stream-test"

    private static final String PARAM_AWS_ACCESS_KEY = "awsAccessKey"
    private static final String PARAM_AWS_SECRET_KEY = "awsSecretKey"
    private static final String PARAM_AWS_FIREHOSE_STREAM = "awsFirehoseStream"
    private static final String PARAM_AWS_REGION = "awsRegion"

    /**
     * Initializing AWS parameters and put data to AWS Firehose stream
     * @param data
     * @return String
     */
    public static String exportToAwsFirehoseStream(String data) {
        initParameters()
        return putFirehoseRecord(data)
    }

    /**
     * Initializing AWS parameters
     */
    private static void initParameters() {
        getAwsCredentials() ?: {
            throw new Exception("AWS credentials not provided.")
        }
        awsFirehoseStream = ReportManager.getParameter(PARAM_AWS_FIREHOSE_STREAM) ?: defaultAwsFirehoseStream
        println("AWS Firehose stream: " + awsFirehoseStream)
        awsRegion = ReportManager.getParameter(PARAM_AWS_REGION) ?: defaultAwsRegion
        println("AWS region: " + awsRegion)
    }

    /**
     * Put data record to AWS Firehose stream and returns record ID
     * @param data
     * @return String
     */
    private static String putFirehoseRecord(String data) {
        AmazonKinesisFirehoseClient firehoseClient = new AmazonKinesisFirehoseClient(credentials)
        Region region = RegionUtils.getRegion(awsRegion)
        firehoseClient.setRegion(region)
        Record record = new Record()
        record.setData(ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8)))
        PutRecordRequest putRecordRequest = new PutRecordRequest().withDeliveryStreamName(awsFirehoseStream).withRecord(record)
        putRecordRequest.setRecord(record)
        PutRecordResult result = firehoseClient.putRecord(putRecordRequest)
        println("AWS Record ID: " + result.getRecordId())
        return result.getRecordId()
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
