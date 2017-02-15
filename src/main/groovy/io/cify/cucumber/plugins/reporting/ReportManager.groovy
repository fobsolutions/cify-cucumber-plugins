package io.cify.cucumber.plugins.reporting

import java.nio.file.Paths

import static java.util.UUID.randomUUID

/**
 * Created by FOB Solutions
 *
 * This class is responsible for reporting management
 */
class ReportManager {

    private static final String DEFAULT_REPORTING_DIR = "reporting"
    private static final String PARAM_REPORTING_DIR = "reportingDir"
    private static final String PARAM_VIDEO_DIR = "videoDir"
    private static final String SCREENSHOTS_SUB_DIR = "screenshots"
    private static final String DEFAULT_SCREENSHOTS_DIR = "build/cify/videos/screenshots"

    /**
     * Exports report. In case of error, saves data to local file in reporting directory.
     * @param data
     */
    public static void report(String data) {
        if (data) {
            try {
                if (export(data) == null) {
                    throw new Exception("Data export failed")
                }
            } catch (all) {
                println("Report failed. Error: " + all.message)
                String filepath = Paths.get(getReportingDir(), generateId() + ".json")
                FileReport.saveReportToFile(data, filepath)
                println("Report saved to: " + filepath)
            }
        }
    }

    /**
     * Export json data to AWS Firehose stream, returns AWS record ID
     * @param data
     * @return String
     */
    private static String export(String data) {
        return AWSKinesisStream.exportToAwsKinesisStream(data)
    }

    /**
     * Uploads test scenario screenshots to AWS S3
     */
    public static void uploadScreenshots(String scenarioId) {
        try {
            AWSS3.uploadScreenshotsInParallel(getScreenshotsDir() + "/" + scenarioId)
        } catch (all) {
            println("Screenshots upload operation fail. Error: " + all.message)
        }
    }

    /**
     * Returns reporting directory parameter, if 'reportingDir' parameter not specified uses default value
     * @return String
     */
    private static String getReportingDir() {
        getParameter(PARAM_REPORTING_DIR) ?: DEFAULT_REPORTING_DIR
    }

    /**
     * Returns screenshots directory parameter
     * @return String
     */
    private static String getScreenshotsDir() {
        String baseDir = getParameter(PARAM_VIDEO_DIR)
        baseDir ? baseDir + SCREENSHOTS_SUB_DIR : DEFAULT_SCREENSHOTS_DIR
    }

    /**
     * Gets parameter from system
     * @param parameter
     * @return String
     */
    public static String getParameter(String parameter) {
        if (System.getenv(parameter)) {
            return System.getenv(parameter)
        } else if (System.getProperty(parameter)) {
            return System.getProperty(parameter)
        } else {
            return null
        }
    }

    /**
     * Generates unique id
     * @return string
     */
    private static String generateId() {
        return (System.currentTimeMillis() + "-" + randomUUID()) as String
    }
}
