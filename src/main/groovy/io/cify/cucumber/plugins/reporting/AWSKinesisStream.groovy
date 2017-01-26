package io.cify.cucumber.plugins.reporting

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

    private final static String STREAM_POSTFIX = "-stream"

    /**
     * Initializing AWS parameters and put data to AWS Kinesis stream
     * @param data
     * @return String
     */
    public static String exportToAwsKinesisStream(String data) {
        def result = new JsonSlurper().parseText(data) as Map
        String partitionKey = result?.keySet()[0].toString()
        if (partitionKey) {
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
     * Put data record with partition key to AWS Kinesis stream and returns record sequence number
     * @param data
     * @param partition key
     * @return String
     */
    private static String putKinesisStreamRecord(String data, String partitionKey) {
        String awsKinesisStream = AWSAuthentication.company + STREAM_POSTFIX
        println("Stream: " + awsKinesisStream)
        String newPartitionKey = "<company>$AWSAuthentication.company<company><partition>$partitionKey<partition>"
        AmazonKinesisClient kinesisClient = new AmazonKinesisClient(AWSAuthentication.credentials)
        Region region = RegionUtils.getRegion(AWSAuthentication.awsRegion)
        kinesisClient.setRegion(region)

        PutRecordRequest putRecordRequest = new PutRecordRequest()
        putRecordRequest.setData(ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8)))
        putRecordRequest.setPartitionKey(newPartitionKey)
        putRecordRequest.setStreamName(awsKinesisStream)

        PutRecordResult result = kinesisClient.putRecord(putRecordRequest)
        println("Record number: " + result.getSequenceNumber())
        return result.getSequenceNumber()
    }

}
