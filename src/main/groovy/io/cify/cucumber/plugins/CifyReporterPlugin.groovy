package io.cify.cucumber.plugins

import gherkin.formatter.Formatter
import gherkin.formatter.Reporter
import gherkin.formatter.model.*
import groovy.json.JsonBuilder

import io.cify.cucumber.reporting.AWSAuthentication
import io.cify.cucumber.reporting.ReportManager
import io.cify.framework.core.CifyFrameworkException
import io.cify.framework.reporting.TestReportManager

/**
 * Created by FOB Solutions
 *
 * This class is responsible for providing cucumber run information to cify framework
 * and reporting test results
 */
class CifyReporterPlugin implements Formatter, Reporter {

    private TestReportManager trm
    private static final long NANO_TO_MILLI_DIVIDER = 1000000L

    /**
     * Is called at the beginning of the scenario life cycle, meaning before the first "before" hook.
     * @param scenario the {@link Scenario} of the current lifecycle
     */
    /**
     * Is called in case any syntax error was detected during the parsing of the feature files.
     *
     * @param state the current state of the parser machine
     * @param event detected event
     * @param legalEvents expected event
     * @param uri the URI of the feature file
     * @param line the line number of the event
     */
    @Override
    void syntaxError(String state, String event, List<String> legalEvents, String uri, Integer line) {

    }

    /**
     * Called at the beginning of each feature.
     *
     * @param uri the feature's URI
     */
    @Override
    void uri(String uri) {
        if (getParameter("suiteFinished") == 'true') {
            suiteCompletedReport()
            System.exit(0)
        }
    }

    /**
     * Called after the uri, but before the actual feature execution.
     *
     * @param feature the to be executed {@linkplain Feature}
     */
    @Override
    void feature(Feature feature) {
        String cucumberRunId = getParameter("runId")
        AWSAuthentication.getAuthData()
        trm = TestReportManager.getTestReportManager()
        trm.testRunStarted(feature.name, cucumberRunId, feature.id)
    }

    /**
     * Called before the actual execution of the scenario outline step container.
     *
     * @param scenarioOutline the to be executed {@link ScenarioOutline}
     */
    @Override
    void scenarioOutline(ScenarioOutline scenarioOutline) {

    }

    /**
     * Called before the actual execution of the scenario examples. This is called after
     * the scenarioOutline,
     * but before any actual scenario example.
     *
     * @param examples the to be executed
     */
    @Override
    void examples(Examples examples) {

    }

    @Override
    void startOfScenarioLifeCycle(Scenario scenario) {

    }

    /**
     * Called before the actual execution of the background step container.
     *
     * @param background the to be executed {@link Background}
     */
    @Override
    void background(Background background) {

    }

    /**
     * Called before the actual execution of the scenario step container.
     *
     * @param scenario the to be executed {@link Scenario}
     */
    @Override
    void scenario(Scenario scenario) {
        trm.scenarioStarted(scenario.name, scenario.id)
    }

    /**
     * Is called for each step of a step container. <b>Attention:</b> All steps are iterated through
     * this method before any step is actually executed.
     *
     * @param step the {@link Step} to be executed
     */
    @Override
    void step(Step step) {
        trm.stepStarted("$step.keyword$step.name")
    }

    /**
     * Is called at the end of the scenario life cycle, meaning after the last "after" hook.
     * * @param scenario the {@link Scenario} of the current lifecycle
     */
    @Override
    void endOfScenarioLifeCycle(Scenario scenario) {

    }

    /**
     * Indicates that the last file has been processed. This should print out any closing output,
     * such as completing the JSON string, but it should *not* close any underlying streams/writers.
     */
    @Override
    void done() {
        ReportManager.report(trm?.testRunFinished())
    }

    /**
     * Closes all underlying streams.
     */
    @Override
    void close() {
    }

    /**
     * Indicates the End-Of-File for a Gherkin document (.feature file)
     */
    @Override
    void eof() {
    }

    @Override
    void before(Match match, Result result) {

    }

    /**
     * Result step for cucumber, called after every step
     * @param result
     */
    @Override
    void result(Result result) {
        long durationInMilliseconds = result.duration ? result.duration / NANO_TO_MILLI_DIVIDER : 0
        ReportManager.report(trm?.stepFinished(result.status, durationInMilliseconds, result.errorMessage))
    }

    @Override
    void after(Match match, Result result) {
        String scenarioId = trm?.getActiveScenario()?.scenarioId
        ReportManager.uploadScreenshots(scenarioId)
        ReportManager.report(trm?.scenarioFinished(result.status, result.errorMessage))
    }

    @Override
    void match(Match match) {

    }

    @Override
    void embedding(String mimeType, byte[] data) {

    }

    @Override
    void write(String text) {

    }

    /**
     * Report about testsuite end
     */
    private static void suiteCompletedReport() {
        String projectName = getParameter("projectName")
        String suiteName = getParameter("suiteName")
        String testsuiteId = getParameter("runId")
        def jsonBuilder = new JsonBuilder()
        jsonBuilder.suite(
                projectName: projectName,
                testsuiteId: testsuiteId,
                suiteName: suiteName,
                status: "completed"
        )
        ReportManager.report(jsonBuilder.toString())
    }

    /**
     * Gets parameter from system
     * @param parameter
     * @return String
     */
    static String getParameter(String parameter) {
        if (System.getenv(parameter)) {
            return System.getenv(parameter)
        } else if (System.getProperty(parameter)) {
            return System.getProperty(parameter)
        } else {
            throw new CifyFrameworkException("User did not pass parameter for $parameter please add it to system environment variable or system property!")
        }
    }
}
