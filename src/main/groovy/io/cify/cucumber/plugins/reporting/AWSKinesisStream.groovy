package io.cify.cucumber.plugins.reporting

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder
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

    /**
     * Initializing AWS parameters and put data to AWS Kinesis stream
     * @param data
     * @return String
     */
    public static String exportToAwsKinesisStream(String data) {
        if (!AWSAuthentication.getAuthData()) {
            throw new Exception("Authentication failed")
        }
        String token = AWSAuthentication.getAuthData()?.idToken
        def result = new JsonSlurper().parseText(data) as Map
        String partitionKey = result?.keySet()[0].toString()
        if (partitionKey) {
            def json = new JsonBuilder()
            json.data(
                    idToken: token,
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
        String awsKinesisStream = AWSAuthentication.authData.stream
        String newPartitionKey = "<companyId>$AWSAuthentication.authData.companyId<companyId><partition>$partitionKey<partition>"
        AmazonKinesis kinesisClient = AmazonKinesisClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(AWSAuthentication.credentials))
                .withRegion(AWSAuthentication.awsRegion)
                .build()

        PutRecordRequest putRecordRequest = new PutRecordRequest()
        putRecordRequest.setData(ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8)))
        putRecordRequest.setPartitionKey(newPartitionKey)
        putRecordRequest.setStreamName(awsKinesisStream)

        PutRecordResult result = kinesisClient.putRecord(putRecordRequest)
        return result.getSequenceNumber()
    }

}
