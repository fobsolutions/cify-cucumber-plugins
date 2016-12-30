package io.cify.cucumber.plugins.reporting

public class FileReport {

    static File saveReportToFile(String content, String filepath) {
        def file = new File(filepath)
        file.getParentFile().mkdirs()
        file.write(content)
        return file
    }
}
