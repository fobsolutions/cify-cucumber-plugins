package io.cify.cucumber.plugins

import com.google.gson.JsonObject
import com.saucelabs.saucerest.SauceREST
import gherkin.formatter.Formatter
import gherkin.formatter.Reporter
import gherkin.formatter.model.*
import io.cify.framework.core.CifyFrameworkException
import io.cify.framework.core.DeviceCategory
import io.cify.framework.core.DeviceManager
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Marker
import org.apache.logging.log4j.MarkerManager
import org.apache.logging.log4j.core.Logger
import org.openqa.selenium.remote.RemoteWebDriver

import static io.cify.cucumber.plugins.SauceLabsPlugin.Status.SKIPPED

/**
 * Created by FOB Solutions
 *
 * This class is responsible for connecting with SauceLabs
 */
class SauceLabsPlugin implements Formatter, Reporter {

    static enum Status {
        PASSED,
        FAILED,
        SKIPPED
    }

    private static final String SAUCELABS_USERNAME = getParameter("SAUCELABS_USERNAME")
    private static final String SAUCELABS_ACCESSKEY = getParameter("SAUCELABS_ACCESSKEY")
    private static
    final String SAUCELABS_URL = "http://$SAUCELABS_USERNAME:$SAUCELABS_ACCESSKEY@ondemand.saucelabs.com:80/wd/hub"
    private SauceREST sauceREST = new SauceREST(SAUCELABS_USERNAME, SAUCELABS_ACCESSKEY)

    private static final Logger LOG = LogManager.getLogger(this.class) as Logger
    private static final Marker MARKER = MarkerManager.getMarker('SAUCELABSPLUGIN') as Marker

    private List<String> sessionIds = new ArrayList<>()
    private List<Result> results = new ArrayList<>()
    private String error = ""
    private String featureName
    private String scenarioName

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

    }

    /**
     * Called after the uri, but before the actual feature execution.
     *
     * @param feature the to be executed {@linkplain Feature}
     */
    @Override
    void feature(Feature feature) {
        LOG.debug(MARKER, "Feature started: $feature.name")

        featureName = feature.getName()

        DeviceCategory.values().each {
            if (!DeviceManager.getInstance().getCapabilities().toDesiredCapabilities(it).getCapability("remote")) {
                LOG.debug(MARKER, "Remote capability is not present, adding SauceLabs URL")
                DeviceManager.getInstance().getCapabilities().addToDesiredCapabilities(it, "remote", SAUCELABS_URL)
            }
        }
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
        LOG.debug(MARKER, "Scenario started: $scenario.name")
        scenarioName = scenario.getName()

        DeviceCategory.values().each {
            if (!DeviceManager.getInstance().getCapabilities().toDesiredCapabilities(it).getCapability("name")) {
                LOG.debug(MARKER, "Name capability is not present, will add scenario name")
                DeviceManager.getInstance().getCapabilities().addToDesiredCapabilities(it, "name", scenario.name)
            }
        }
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

    }

    /**
     * Is called for each step of a step container. <b>Attention:</b> All steps are iterated through
     * this method before any step is actually executed.
     *
     * @param step the {@link Step} to be executed
     */
    @Override
    void step(Step step) {

    }

    /**
     * Is called at the end of the scenario life cycle, meaning after the last "after" hook.
     * * @param scenario the {@link Scenario} of the current lifecycle
     */
    @Override
    void endOfScenarioLifeCycle(Scenario scenario) {
        sessionIds.each {

            Result failedResult = results.find {
                it.status == "failed"
            }

            try {
                failedResult ? setStatus(Status.FAILED, it) : setStatus(Status.PASSED, it)
                addDataToJob(featureName, scenarioName, error, it)
            } catch (all) {
                LOG.debug(MARKER, "Failed to set job status cause: " + all.message)
            }
        }
        sessionIds.clear()
        results.clear()
    }

    /**
     * Indicates that the last file has been processed. This should print out any closing output,
     * such as completing the JSON string, but it should *not* close any underlying streams/writers.
     */
    @Override
    void done() {

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

        DeviceManager.getInstance().getAllActiveDevices().each {
            try {
                String sessionId = (it.getDriver() as RemoteWebDriver).getSessionId() as String

                if (sessionIds.isEmpty()) {
                    it.setCapability("video", sauceREST.getPublicJobLink(sessionId))
                }
                sessionIds.add(sessionId)
                results.add(result)
            } catch (all) {
                LOG.debug(MARKER, "Failed to set public job link to capabilities cause: " + all.message)
            }
        }
    }

    @Override
    void after(Match match, Result result) {
        error = result.getErrorMessage()
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
     * Set job status
     * @param status
     * @param runId
     */
    private void setStatus(Status status, String runId) {

        LOG.debug(MARKER, "Setting SauceLabs test status to $status for run with id $runId")
        switch (status) {
            case Status.PASSED:
                sauceREST.jobPassed(runId)
                break
            case Status.FAILED:
                sauceREST.jobFailed(runId)
                break
            case SKIPPED:
                sauceREST.stopJob(runId)
                break
        }
    }

    /**
     * Add data to SauceLabs job
     * @param featureName
     * @param scenarioName
     * @param errorMessage
     * @param runId
     */
    private void addDataToJob(String featureName, String scenarioName, String errorMessage, String runId) {

        Map<String, Object> data = new HashMap<>()
        JsonObject json = new JsonObject()
        json.addProperty("feature", featureName)
        json.addProperty("scenario", scenarioName)
        json.addProperty("error", errorMessage)

        data.put("custom-data", json)
        LOG.debug(MARKER, "Adding data to job: $json")
        sauceREST.updateJobInfo(runId, data)

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
