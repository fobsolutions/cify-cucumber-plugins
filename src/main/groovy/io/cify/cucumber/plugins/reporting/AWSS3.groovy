package io.cify.cucumber.plugins.reporting

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.regions.Region
import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectTagging
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.model.PutObjectResult
import com.amazonaws.services.s3.model.Tag
import groovy.io.FileType

import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.stream.FileImageInputStream
import javax.imageio.stream.ImageInputStream
import java.awt.*
import java.util.List

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
        Region region = RegionUtils.getRegion(AWSAuthentication.awsRegion)
        String company = AWSAuthentication.company
        String token = AWSAuthentication.getAuthData()?.idToken

        AmazonS3Client s3Client = new AmazonS3Client(credentials)
        s3Client.setRegion(region)
        println("S3 client created.")

        long uploadStarted = System.currentTimeMillis()
        def fileList = []
        def dir = new File(screenshotsDir)
        double volume = dir.directorySize() / 1024
        dir.eachFileRecurse(FileType.FILES) { file ->
            fileList << file
        }

        def uploaded = []
        def threadList = []
        fileList.each {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    def dimensions = getImageDimension(it as File)
                    String keyName = company + "/" + dimensions.width + "x" + dimensions.height + "_" + it.name
                    List<Tag> tags = getTags(company, token)
                    try {
                        PutObjectRequest por = new PutObjectRequest(BUCKET_NAME, keyName, it as File)
                        por.setTagging(new ObjectTagging(tags));
                        PutObjectResult res = s3Client.putObject(por)
                        if (res) {
                            uploaded.add(it.name)
                            it.delete()
                        }
                    } catch (AmazonServiceException ase) {
                        println("Screenshot upload AmazonServiceException: " + ase.getMessage())
                    } catch (AmazonClientException ase) {
                        println("Screenshot upload AmazonClientException: " + ase.getMessage())
                    } catch (Exception e) {
                        println("Screenshot upload Exception: " + e.getMessage())
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
        if(includeToken) {
            token.split("(?<=\\G.{$TAG_MAX_SIZE})").eachWithIndex { item, index ->
                tags.add(new Tag(index + "_idtokenpart", item))
            }
        }
        return tags
    }

    /**
     * Gets image dimensions for given file
     *
     * @param imgFile image file
     * @return dimensions of image
     */
    private static Dimension getImageDimension(File imgFile) {
        int pos = imgFile.getName().lastIndexOf(".")
        if (pos == -1)
            throw new IOException("No extension for file: " + imgFile.getAbsolutePath())
        String suffix = imgFile.getName().substring(pos + 1)
        Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(suffix)
        while (iter.hasNext()) {
            ImageReader reader = iter.next()
            try {
                ImageInputStream stream = new FileImageInputStream(imgFile)
                reader.setInput(stream)
                int width = reader.getWidth(reader.getMinIndex())
                int height = reader.getHeight(reader.getMinIndex())
                return new Dimension(width, height)
            } catch (IOException e) {
                println("Error reading: " + imgFile.getAbsolutePath() + " message:" + e.getMessage())
            } finally {
                reader.dispose()
            }
        }
        throw new Exception("Cannot get dimensions. Unknown image file: " + imgFile.getAbsolutePath())
    }

}
