package io.cify.cucumber.plugins.reporting

import java.nio.file.Paths

public class FileReport {

    static File saveReportToFile(String content, String targetDirectory) {
        def file = new File(targetDirectory)
        file.getParentFile().mkdirs()
        file.write(content)
        return file
    }

    static void moveFileToDir(File file, String targetDirectory) {
        new File(targetDirectory).mkdirs()
        println("File $file moved to $targetDirectory")
        String newFilepath = Paths.get(targetDirectory, file.getName())
        file.renameTo(newFilepath)
    }

}
