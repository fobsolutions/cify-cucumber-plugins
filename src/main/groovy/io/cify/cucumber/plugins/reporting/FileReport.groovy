package io.cify.cucumber.plugins.reporting

/**
 * Created by FOB Solutions
 *
 * This class is responsible for reporting file operations
 */
public class FileReport {

    /**
     * Saves given content to provided file path
     * @param content
     * @param filepath
     * @return File
     */
    static File saveReportToFile(String content, String filepath) {
        def file = new File(filepath)
        file.getParentFile().mkdirs()
        file.write(content)
        return file
    }
}
