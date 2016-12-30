package io.cify.cucumber.plugins.reporting

import java.nio.file.Paths

import static java.util.UUID.randomUUID

class ReportManager {

    private static final String DEFAULT_REPORTING_DIR = "reporting"
    private static final String PARAM_REPORTING_DIR = "reportingDir"

    public static void report(String json) {
        if (json) {
            try {
                AWSReport.exportToAwsFirehoseStream(json)
            } catch (all) {
                println("Report failed. Error: " + all.message)
                String filepath = Paths.get(getReportingDir(), generateId() + ".json")
                FileReport.saveReportToFile(json, filepath)
                println("Report saved to: " + filepath)
            }
        }
    }

    private static String getReportingDir() {
        getParameter(PARAM_REPORTING_DIR) ?: DEFAULT_REPORTING_DIR
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

    /**
     * Generates unique id
     * @return string
     */
    private static String generateId() {
        return (System.currentTimeMillis() + "-" + randomUUID()) as String
    }
}
