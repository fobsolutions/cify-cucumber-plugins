package io.cify.cucumber.plugins.reporting

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.ObjectTagging
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.model.PutObjectResult
import com.amazonaws.services.s3.model.Tag
import groovy.io.FileType

import javax.xml.bind.DatatypeConverter
import java.security.MessageDigest

/**
 * Created by FOB Solutions
 *
 * This class is responsible for uploading data to AWS S3
 */
class AWSS3 {

    private static final String BUCKET_NAME = "cify-reporting-screenshots"
    private static final int TAG_MAX_SIZE = 255
    private static boolean includeToken = false

    /**
     * Uploads files from given directory to S3 bucket in parallel using temporary AWS credentials
     *
     * @param screenshotsDir
     */
    public static void uploadScreenshotsInParallel(String screenshotsDir) {

        if (AWSAuthentication.getAuthData()) {
            println("Authenticated ok.")
        } else {
            throw new Exception("Authentication failed")
        }

        def credentials = AWSAuthentication.credentials
        String company = AWSAuthentication.company
        String token = AWSAuthentication.getAuthData()?.idToken

        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(AWSAuthentication.awsRegion)
                .build()

        println("S3 client created.")

        long uploadStarted = System.currentTimeMillis()
        def fileList = []
        def dir = new File(screenshotsDir)
        double volume = dir.directorySize() / 1024
        dir.eachFileRecurse(FileType.FILES) { file ->
            fileList << file
        }

        def filteredFileList = filterDuplicateScreenshots(fileList)
        def uploaded = []
        def threadList = []
        filteredFileList.each {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    String keyName = company + "/" + "widthxheight" + "_" + it.name
                    List<Tag> tags = getTags(company, token)
                    boolean success = false
                    int attempts = 0
                    while (!success) {
                        attempts++
                        try {
                            PutObjectRequest por = new PutObjectRequest(BUCKET_NAME, keyName, it as File)
                            por.setTagging(new ObjectTagging(tags));
                            PutObjectResult res = s3Client.putObject(por)
                            if (res) {
                                uploaded.add(it.name)
                                it.delete()
                                success = true
                            }
                        } catch (AmazonServiceException ase) {
                            println("Screenshot upload AmazonServiceException: " + ase.getMessage())
                        } catch (AmazonClientException ase) {
                            println("Screenshot upload AmazonClientException: " + ase.getMessage())
                        } catch (Exception e) {
                            println("Screenshot upload Exception: " + e.getMessage())
                        }
                        if (attempts == 3) {
                            break
                        }
                    }
                }
            })
            t.start()
            threadList.add(t)
        }
        threadList.each { it.join() }

        println("uploaded volume $volume kb, time " + (System.currentTimeMillis() - uploadStarted) + " ms")
        if (dir.list().length == 0) {
            dir.delete()
        }
    }

    /**
     * Returns list of Tags with screenshot information
     *
     * @param company
     * @param token
     * @param filepath
     * @return List < Tag >
     */
    private static List<Tag> getTags(String company, String token) {
        List<Tag> tags = new ArrayList<Tag>()
        tags.add(new Tag("company", company))
        if (includeToken) {
            token.split("(?<=\\G.{$TAG_MAX_SIZE})").eachWithIndex { item, index ->
                tags.add(new Tag(index + "_idtokenpart", item))
            }
        }
        return tags
    }

    /**
     * Iterates list, removes file if MD5 hash is equal to previous file hash
     *
     * @param fileList
     * @return list of filtered files
     */
    private static List filterDuplicateScreenshots(List fileList) {
        def filteredFileList = []
        String hash = ""
        fileList.eachWithIndex { item, index ->
            if (index == 0) {
                hash = getFileMD5Hash(item as File)
                filteredFileList.add(item)
            } else {
                println("current screenshot hash:$hash")
                String nextHash = getFileMD5Hash(item as File)
                if (hash.equalsIgnoreCase(nextHash)) {
                    item.delete()
                    println("next screenshot hash:$nextHash" + " delete duplicated file:" + item.name)
                } else {
                    filteredFileList.add(item)
                    println("next screenshot hash:$nextHash" + " file will be uploaded:" + item.name)
                    hash = nextHash
                }
            }
        }
        return filteredFileList
    }

    /**
     * Returns MD5 hash of given file
     *
     * @param file
     * @return String MD5 hash
     */
    private static String getFileMD5Hash(File file) {
        byte[] hash = MessageDigest.getInstance("MD5").digest(file.bytes)
        return DatatypeConverter.printHexBinary(hash)
    }
}
