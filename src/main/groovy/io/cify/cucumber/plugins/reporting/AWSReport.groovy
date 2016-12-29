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
import io.cify.framework.core.CifyFrameworkException

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

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

    public static void exportToAwsFirehoseStream(String data) {
        initParameters()
        putFirehoseRecord(data)
    }

    private static void initParameters() {
        getAwsCredentials() ?: {
            throw new CifyFrameworkException("AWS credentials not provided.")
        }
        awsFirehoseStream = getParameter(PARAM_AWS_FIREHOSE_STREAM) ?: defaultAwsFirehoseStream
        //remove next line
        awsFirehoseStream = "fob-reporting-test-stream"
        awsRegion = getParameter(PARAM_AWS_REGION) ?: defaultAwsRegion
    }

    /**
     * Gets parameter from system
     * @param parameter
     * @return String
     */
    private static String getParameter(String parameter) {
        if (System.getenv(parameter)) {
            return System.getenv(parameter)
        } else if (System.getProperty(parameter)) {
            return System.getProperty(parameter)
        } else {
            return null
        }
    }

    private static String putFirehoseRecord(String data) {
        AmazonKinesisFirehoseClient firehoseClient = new AmazonKinesisFirehoseClient(credentials)
        Region region = RegionUtils.getRegion(awsRegion)
        firehoseClient.setRegion(region)
        Record record = new Record()
        record.setData(ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8)))
        PutRecordRequest putRecordRequest = new PutRecordRequest().withDeliveryStreamName(awsFirehoseStream).withRecord(record)
        putRecordRequest.setRecord(record)
        PutRecordResult result = firehoseClient.putRecord(putRecordRequest)
        return result.getRecordId()
    }

    private static AWSCredentials getAwsCredentials() {
        awsAccessKey = getParameter(PARAM_AWS_ACCESS_KEY)
        awsSecretKey = getParameter(PARAM_AWS_SECRET_KEY)
        if (awsAccessKey && awsSecretKey) {
            credentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey)
        } else {
            credentials = new ProfileCredentialsProvider().getCredentials()
        }
        return credentials
    }
}
