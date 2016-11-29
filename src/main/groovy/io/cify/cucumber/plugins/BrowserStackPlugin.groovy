package io.cify.cucumber.plugins

import gherkin.formatter.Formatter
import gherkin.formatter.model.*
import io.cify.framework.core.CifyFrameworkException
import io.cify.framework.core.DeviceCategory
import io.cify.framework.core.DeviceManager

/**
 * Created by FOB Solutions
 *
 * This class is responsible for managing browser stack connections.
 */
class BrowserStackPlugin implements Formatter {

    public static final String BROWSERSTACK_USERNAME = getParameter("BROWSERSTACK_USERNAME")
    public static final String BROWSERSTACK_AUTOMATE_KEY = getParameter("BROWSERSTACK_AUTOMATE_KEY")
    public static
    final String BROWSERSTACK_URL = "https://" + BROWSERSTACK_USERNAME + ":" + BROWSERSTACK_AUTOMATE_KEY + "@hub-cloud.browserstack.com/wd/hub";

    /**
     * Called before feature execution
     * */
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
     * Executed before actual execution of a feature
     *
     * Adds remote URL
     * @param feature
     */
    @Override
    public void feature(Feature feature) {
        DeviceCategory.values().each {
            DeviceManager.getInstance().getCapabilities().addToDesiredCapabilities(it, "remote", BROWSERSTACK_URL)
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
     * the scenarioOutline(gherkin.formatter.model.ScenarioOutline),
     * but before any actual scenario example.
     *
     * @param examples the to be executed
     */
    @Override
    void examples(Examples examples) {

    }

    /**
     * Is called at the beginning of the scenario life cycle, meaning before the first "before" hook.
     * @param scenario the of the current lifecycle
     */
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
            throw new CifyFrameworkException("User did not pass parameter for $parameter please add it to system environment variable or system property!")
        }
    }
}
