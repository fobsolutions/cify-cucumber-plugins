package io.cify.cucumber.plugins

import com.saucelabs.saucerest.SauceREST
import gherkin.formatter.Formatter
import gherkin.formatter.Reporter
import gherkin.formatter.model.*
import io.cify.cucumber.PluginHelper
import io.cify.framework.core.CifyFrameworkException
import io.cify.framework.core.DeviceCategory
import io.cify.framework.core.DeviceManager
import org.openqa.selenium.remote.RemoteWebDriver

/**
 * Created by FOB Solutions
 *
 * This class is responsible for connecting with SauceLabs
 */
class SauceLabsPlugin extends PluginHelper implements Formatter, Reporter {

    private static final String SAUCELABS_USERNAME = getParameter("SAUCELABS_USERNAME")
    private static final String SAUCELABS_ACCESSKEY = getParameter("SAUCELABS_ACCESSKEY")
    private static
    final String SAUCELABS_URL = "http://$SAUCELABS_USERNAME:$SAUCELABS_ACCESSKEY@ondemand.saucelabs.com:80/wd/hub"
    private SauceREST sauceREST = new SauceREST(SAUCELABS_USERNAME, SAUCELABS_ACCESSKEY)

    private List<String> sessionIds = new ArrayList<>()
    private List<Result> results = new ArrayList<>()

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
        DeviceCategory.values().each {
            if (!DeviceManager.getInstance().getCapabilities().toDesiredCapabilities(it).getCapability("remote")) {
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
        DeviceCategory.values().each {
            if (!DeviceManager.getInstance().getCapabilities().toDesiredCapabilities(it).getCapability("name")) {
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

            if (failedResult) {
                sauceREST.jobFailed(it)
            } else {
                sauceREST.jobPassed(it)
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
            } catch (ignored) {
                // Not a SauceLabs session
            }
        }
    }

    @Override
    void after(Match match, Result result) {

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
}
