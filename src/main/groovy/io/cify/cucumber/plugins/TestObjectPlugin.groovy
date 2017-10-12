package io.cify.cucumber.plugins

import gherkin.formatter.Formatter
import gherkin.formatter.Reporter
import gherkin.formatter.model.*
import io.cify.cucumber.PluginHelper
import io.cify.framework.core.Capabilities
import io.cify.framework.core.DeviceCategory
import io.cify.framework.core.DeviceManager
import org.openqa.selenium.remote.RemoteWebDriver
import org.testobject.rest.api.RestClient
import org.testobject.rest.api.appium.common.TestObjectCapabilities
import org.testobject.rest.api.resource.AppiumResource

/**
 * Created by FOB Solutions
 *
 * This class is responsible for connecting with SauceLabs
 */
class TestObjectPlugin extends PluginHelper implements Formatter, Reporter {

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
            DeviceManager.getInstance().getCapabilities().addToDesiredCapabilities(it, TestObjectCapabilities.TESTOBJECT_SUITE_NAME, feature.name)
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
            DeviceManager.getInstance().getCapabilities().addToDesiredCapabilities(it, TestObjectCapabilities.TESTOBJECT_TEST_NAME, scenario.name)
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
                String apiKey = it.getCapabilities().getCapability(TestObjectCapabilities.TESTOBJECT_API_KEY)
                if (apiKey) {
                    String sessionId = (it.getDriver() as RemoteWebDriver).getSessionId() as String
                    getRestClient(apiKey).updateTestReportStatus(sessionId, result.getStatus() != "failed")
                }
            } catch (ignored) {
                // Not a Remote session
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

    /**
     * Init AppiumResource client
     * @param apiKey - TestObject apiKey
     * @return AppiumResource
     */
    private static AppiumResource getRestClient(String apiKey) {
        new AppiumResource(RestClient.Builder.createClient()
                .withUrl(TestObjectCapabilities.TESTOBJECT_API_ENDPOINT.toString())
                .withToken(apiKey)
                .path(RestClient.REST_APPIUM_PATH)
                .build())
    }
}

