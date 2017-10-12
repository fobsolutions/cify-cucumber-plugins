package io.cify.cucumber.plugins

import gherkin.formatter.JSONFormatter
import gherkin.formatter.model.*
import io.cify.framework.core.DeviceCategory
import io.cify.framework.core.DeviceManager
import org.openqa.selenium.remote.DesiredCapabilities

/**
 * Created by FOB Solutions
 */
class CifyJSONFormatter extends JSONFormatter {

    private boolean inScenarioOutline = false

    private static final CAPABILITY = "capability"
    private static final NAME = "name"

    CifyJSONFormatter(Appendable out) {
        super(out)
    }

    @Override
    void scenarioOutline(ScenarioOutline scenarioOutline) {
        inScenarioOutline = true
    }

    @Override
    void examples(Examples examples) {
        // NoOp
    }

    @Override
    void startOfScenarioLifeCycle(Scenario scenario) {
        inScenarioOutline = false
        super.startOfScenarioLifeCycle(scenario)
    }

    @Override
    void step(Step step) {
        if (!inScenarioOutline) {
            super.step(step)
        }
    }

    @Override
    void feature(Feature feature) {

        StringBuilder stringBuilder = new StringBuilder(" [")
        DeviceCategory.values().each {
            DesiredCapabilities desiredCapabilities = DeviceManager.getInstance().getCapabilities().toDesiredCapabilities(it)
            if (desiredCapabilities.getCapability(CAPABILITY)) {
                String capabilityName = desiredCapabilities.getCapability(NAME) != null && !(desiredCapabilities.getCapability(NAME) as String).isEmpty() ? desiredCapabilities.getCapability(NAME) : desiredCapabilities.getCapability(CAPABILITY)
                stringBuilder.append(capabilityName).append(",")
            }
        }
        if (stringBuilder.toString().endsWith(",")) {
            stringBuilder.setLength(stringBuilder.toString().length() - 1)
        }
        stringBuilder.append("]")

        Feature updatedFeature = new Feature(feature.getComments(), feature.getTags(), feature.getKeyword(), feature.getName() + stringBuilder as String, feature.getDescription(), feature.getLine(), feature.getId())
        super.feature(updatedFeature)
    }

}
