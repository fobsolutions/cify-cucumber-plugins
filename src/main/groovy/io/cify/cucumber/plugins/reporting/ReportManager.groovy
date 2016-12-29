package io.cify.cucumber.plugins.reporting

class ReportManager {

    private static final String DEFAULT_REPORTING_DIR = "build/cify/reporting/"
    private static final String PARAM_REPORTING_DIR = "reportingDir"
    private static final String PROCESSED = "-processed"

    public static void report(String json) {
        if (json) {
            try {
                File file = FileReport.saveReportToFile(json, getReportingDir())
                AWSReport.exportToAwsFirehoseStream(json)
                FileReport.moveFileToDir(file, getReportingProcessedDir())
            } catch (all) {
                println("Report failed. Error: " + all.message)
            }
        }
    }

    private static String getReportingDir() {
        getParameter(PARAM_REPORTING_DIR) ?: DEFAULT_REPORTING_DIR
    }

    private static String getReportingProcessedDir() {
        getReportingDir() + PROCESSED
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
}
